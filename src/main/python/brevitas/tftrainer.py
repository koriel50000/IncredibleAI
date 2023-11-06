# Copyright (C) 2023, Advanced Micro Devices, Inc. All rights reserved.
# SPDX-License-Identifier: BSD-3-Clause

from datetime import datetime
from hashlib import sha256
import os
import random
import time

import numpy as np
import torch
from torch import nn
import torch.optim as optim
from torch.optim.lr_scheduler import MultiStepLR
from torch.utils.data import DataLoader
import torchvision
from torchvision import transforms
from torchvision.datasets import CIFAR10
from torchvision.datasets import MNIST
import tensorflow as tf

from logger import EvalEpochMeters
from logger import Logger
from logger import TrainingEpochMeters
from models import model_with_cfg
from models.losses import SqrHingeLoss


ROWS = 8
COLUMNS = 8
CHANNELS = 16


def parse_function(example_proto):
    feature_description = {
        'x': tf.io.FixedLenFeature([], tf.string),
        'y': tf.io.FixedLenFeature([], tf.string),
    }
    features = tf.io.parse_single_example(example_proto, feature_description)
    x = tf.io.decode_raw(features['x'], tf.uint8)
    y = tf.io.decode_raw(features['y'], tf.uint8)
    x_ = tf.reshape(x, [CHANNELS, ROWS, COLUMNS])
    y_ = tf.reshape(y, [1])
    return x_, y_


def accuracy(output, target, topk=(1,)):
    """Computes the precision@k for the specified values of k"""
    maxk = max(topk)
    batch_size = target.size(0)

    _, pred = output.topk(maxk, 1, True, True)
    pred = pred.t()
    correct = pred.eq(target.view(1, -1).expand_as(pred))

    res = []
    for k in topk:
        correct_k = correct[:k].flatten().float().sum(0)
        res.append(correct_k.mul_(100.0 / batch_size))
    return res


class TFTrainer(object):

    def __init__(self, args):

        model, cfg = model_with_cfg(args.network, args.pretrained)

        # Init arguments
        self.args = args
        experiment_name = '{}'.format(args.network)
        self.output_dir_path = os.path.join(args.experiments, experiment_name)

        if self.args.resume:
            self.output_dir_path, _ = os.path.split(args.resume)
            self.output_dir_path, _ = os.path.split(self.output_dir_path)

        if not args.dry_run:
            self.checkpoints_dir_path = os.path.join(self.output_dir_path, 'checkpoints')
            if not args.resume:
                os.mkdir(self.output_dir_path)
                os.mkdir(self.checkpoints_dir_path)
        self.logger = Logger(self.output_dir_path, args.dry_run)

        # Randomness
        random.seed(args.random_seed)
        torch.manual_seed(args.random_seed)
        torch.cuda.manual_seed_all(args.random_seed)

        dataset = cfg.get('MODEL', 'DATASET')
        self.num_classes = cfg.getint('MODEL', 'NUM_CLASSES')

        if dataset != 'KIFU':
            raise Exception("Dataset not supported: {}".format(args.dataset))

        train_filename = os.path.join('../../resources/REVERSI_data', 'kifu1k00.tfrecords.gz')
        raw_dataset = tf.data.TFRecordDataset([train_filename], compression_type="GZIP")
        self.train_loader = raw_dataset.map(parse_function).batch(args.batch_size)
        test_filename = os.path.join('../../resources/REVERSI_data', 'kifu1k100.tfrecords.gz')
        raw_dataset = tf.data.TFRecordDataset([test_filename], compression_type="GZIP")
        self.test_loader = raw_dataset.map(parse_function).batch(args.batch_size)
        self.loader_len = 50000

        # Init starting values
        self.starting_epoch = 1
        self.best_val_acc = 0

        # Setup device
        if args.gpus is not None:
            args.gpus = [int(i) for i in args.gpus.split(',')]
            self.device = 'cuda:' + str(args.gpus[0])
            torch.backends.cudnn.benchmark = True
        else:
            self.device = 'cpu'
        self.device = torch.device(self.device)

        # Resume checkpoint, if any
        if args.resume:
            print('Loading model checkpoint at: {}'.format(args.resume))
            package = torch.load(args.resume, map_location='cpu')
            model_state_dict = package['state_dict']
            model.load_state_dict(model_state_dict, strict=args.strict)

        if args.state_dict_to_pth:
            state_dict = model.state_dict()
            name = args.network.lower()
            path = os.path.join(self.checkpoints_dir_path, name)
            torch.save(state_dict, path)
            with open(path, "rb") as f:
                bytes = f.read()
                readable_hash = sha256(bytes).hexdigest()[:8]
            new_path = path + '-' + readable_hash + '.pth'
            os.rename(path, new_path)
            self.logger.info("Saving checkpoint model to {}".format(new_path))
            exit(0)

        if args.gpus is not None and len(args.gpus) == 1:
            model = model.to(device=self.device)
        if args.gpus is not None and len(args.gpus) > 1:
            model = nn.DataParallel(model, args.gpus)
        self.model = model

        # Loss function CrossEntropy only
        self.criterion = nn.CrossEntropyLoss()
        self.criterion = self.criterion.to(device=self.device)

        # Init optimizer
        if args.optim == 'ADAM':
            self.optimizer = optim.Adam(
                self.model.parameters(), lr=args.lr, weight_decay=args.weight_decay)
        elif args.optim == 'SGD':
            self.optimizer = optim.SGD(
                self.model.parameters(),
                lr=self.args.lr,
                momentum=self.args.momentum,
                weight_decay=self.args.weight_decay)

        # Resume optimizer, if any
        if args.resume and not args.evaluate:
            self.logger.log.info("Loading optimizer checkpoint")
            if 'optim_dict' in package.keys():
                self.optimizer.load_state_dict(package['optim_dict'])
            if 'epoch' in package.keys():
                self.starting_epoch = package['epoch']
            if 'best_val_acc' in package.keys():
                self.best_val_acc = package['best_val_acc']

        # LR scheduler
        if args.scheduler == 'STEP':
            milestones = [int(i) for i in args.milestones.split(',')]
            self.scheduler = MultiStepLR(optimizer=self.optimizer, milestones=milestones, gamma=0.1)
        elif args.scheduler == 'FIXED':
            self.scheduler = None
        else:
            raise Exception("Unrecognized scheduler {}".format(self.args.scheduler))

        # Resume scheduler, if any
        if args.resume and not args.evaluate and self.scheduler is not None:
            self.scheduler.last_epoch = package['epoch'] - 1

    def checkpoint_best(self, epoch, name):
        best_path = os.path.join(self.checkpoints_dir_path, name)
        self.logger.info("Saving checkpoint model to {}".format(best_path))
        torch.save({
            'state_dict': self.model.state_dict(),
            'optim_dict': self.optimizer.state_dict(),
            'epoch': epoch + 1,
            'best_val_acc': self.best_val_acc,},
                   best_path)

    def train_model(self):

        # training starts
        if self.args.detect_nan:
            torch.autograd.set_detect_anomaly(True)

        for epoch in range(self.starting_epoch, self.args.epochs):

            # Set to training mode
            self.model.train()
            self.criterion.train()

            # Init metrics
            epoch_meters = TrainingEpochMeters()
            start_data_loading = time.time()

            for i, data in enumerate(self.train_loader.as_numpy_iterator()):
                (raw_input, raw_target) = data
                input = torch.from_numpy(raw_input).clone()
                target = torch.from_numpy(raw_target.astype(np.int64)).clone()
                input = input.to(self.device, non_blocking=True)
                target = target.to(self.device, non_blocking=True)

                target_var = target.squeeze_()
                # pytorch error: multi-target not supported in CrossEntropyLoss()
                # @see https://stackoverflow.com/questions/49206550/pytorch-error-multi-target-not-supported-in-crossentropyloss/49209628

                # measure data loading time
                epoch_meters.data_time.update(time.time() - start_data_loading)

                # Training batch starts
                start_batch = time.time()
                output = self.model(input)
                loss = self.criterion(output, target_var)

                # compute gradient and do SGD step
                self.optimizer.zero_grad()
                loss.backward()
                self.optimizer.step()

                if hasattr(self.model, 'clip_weights'):
                    self.model.clip_weights(-1, 1)

                # measure elapsed time
                epoch_meters.batch_time.update(time.time() - start_batch)

                if i % int(self.args.log_freq) == 0 or i == self.loader_len - 1:
                    prec1, prec5 = accuracy(output.detach(), target, topk=(1, 5))
                    epoch_meters.losses.update(loss.item(), input.size(0))
                    epoch_meters.top1.update(prec1.item(), input.size(0))
                    epoch_meters.top5.update(prec5.item(), input.size(0))
                    self.logger.training_batch_cli_log(
                        epoch_meters, epoch, i, self.loader_len)

                # training batch ends
                start_data_loading = time.time()

            # Set the learning rate
            if self.scheduler is not None:
                self.scheduler.step(epoch)
            else:
                # Set the learning rate
                if epoch % 40 == 0:
                    self.optimizer.param_groups[0]['lr'] *= 0.5

            # Perform eval
            with torch.no_grad():
                top1avg = self.eval_model(epoch)

            # checkpoint
            if top1avg >= self.best_val_acc and not self.args.dry_run:
                self.best_val_acc = top1avg
                self.checkpoint_best(epoch, "best.tar")
            elif not self.args.dry_run:
                self.checkpoint_best(epoch, "checkpoint.tar")

        # training ends
        if not self.args.dry_run:
            return os.path.join(self.checkpoints_dir_path, "best.tar")

    def eval_model(self, epoch=None):
        eval_meters = EvalEpochMeters()

        # switch to evaluate mode
        self.model.eval()
        self.criterion.eval()

        for i, data in enumerate(self.test_loader.as_numpy_iterator()):

            end = time.time()
            (raw_input, raw_target) = data
            input = torch.from_numpy(raw_input).clone()
            target = torch.from_numpy(raw_target.astype(np.int64)).clone()

            input = input.to(self.device, non_blocking=True)
            target = target.to(self.device, non_blocking=True)

            target_var = target.squeeze_()

            # compute output
            output = self.model(input)

            # measure model elapsed time
            eval_meters.model_time.update(time.time() - end)
            end = time.time()

            # compute loss
            loss = self.criterion(output, target_var)
            eval_meters.loss_time.update(time.time() - end)

            pred = output.data.argmax(1, keepdim=True)
            correct = pred.eq(target.data.view_as(pred)).sum()
            prec1 = 100. * correct.float() / input.size(0)

            _, prec5 = accuracy(output, target, topk=(1, 5))
            eval_meters.losses.update(loss.item(), input.size(0))
            eval_meters.top1.update(prec1.item(), input.size(0))
            eval_meters.top5.update(prec5.item(), input.size(0))

            # Eval batch ends
            self.logger.eval_batch_cli_log(eval_meters, i, self.loader_len)

        return eval_meters.top1.avg
