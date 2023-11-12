# -*- coding: utf-8 -*-

import os
import random
import time

import numpy as np
import tensorflow as tf
import torch
from torch import nn
import torch.optim as optim

from logger import Logger
from logger import TrainingEpochMeters
from models import model_with_cfg


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
    y = tf.io.decode_raw(features['y'], tf.int8)
    x_ = tf.reshape(x, [CHANNELS, ROWS, COLUMNS])
    y_ = tf.reshape(tf.divide(tf.subtract(y, 128), 127), [1])
    return x_, y_


def accuracy(output, target, topk=(1,)):
    # """Computes the precision@k for the specified values of k"""
    # maxk = max(topk)
    # batch_size = target.size(0)
    #
    # _, pred = output.topk(maxk, 1, True, True)
    # pred = pred.t()
    # correct = pred.eq(target.view(1, -1).expand_as(pred))
    #
    res = [torch.tensor(0), torch.tensor(0)]
    # for k in topk:
    #     correct_k = correct[:k].flatten().float().sum(0)
    #     res.append(correct_k.mul_(100.0 / batch_size))
    return res


class Trainer(object):

    def __init__(self, network, datadir, experiments, resume, gpus):

        model, cfg = model_with_cfg(network, False)

        # Init arguments
        self.datadir = datadir
        experiment_name = '{}'.format(network)
        self.output_dir_path = os.path.join(experiments, experiment_name)

        if resume:
            self.output_dir_path, _ = os.path.split(resume)
            self.output_dir_path, _ = os.path.split(self.output_dir_path)

        self.checkpoints_dir_path = os.path.join(self.output_dir_path, 'checkpoints')
        if not resume:
            os.mkdir(self.output_dir_path)
            os.mkdir(self.checkpoints_dir_path)
        self.logger = Logger(self.output_dir_path, False)

        # Randomness
        random_seed = 1
        random.seed(random_seed)
        torch.manual_seed(random_seed)
        torch.cuda.manual_seed_all(random_seed)

        # Init starting values
        self.loader_len = 32768
        self.starting_epoch = 1
        self.best_val_acc = 0

        # Setup device
        if gpus is not None:
            gpus = [int(i) for i in gpus.split(',')]
            self.device = 'cuda:' + str(gpus[0])
            torch.backends.cudnn.benchmark = True
        else:
            self.device = 'cpu'
        self.device = torch.device(self.device)

        # Resume checkpoint, if any
        if resume:
            print('Loading model checkpoint at: {}'.format(resume))
            package = torch.load(resume, map_location='cpu')
            model_state_dict = package['state_dict']
            model.load_state_dict(model_state_dict, strict=False)

        if gpus is not None and len(gpus) == 1:
            model = model.to(device=self.device)
        if gpus is not None and len(gpus) > 1:
            model = nn.DataParallel(model, gpus)
        self.model = model

        # Loss function CrossEntropy only
        self.criterion = nn.MSELoss()
        self.criterion = self.criterion.to(device=self.device)

        # Init optimizer
        self.optimizer = optim.Adam(
            self.model.parameters(), lr=0.02, weight_decay=0)

        # Resume optimizer, if any
        if resume:
            self.logger.log.info("Loading optimizer checkpoint")
            if 'optim_dict' in package.keys():
                self.optimizer.load_state_dict(package['optim_dict'])
            if 'epoch' in package.keys():
                self.starting_epoch = package['epoch']
            if 'best_val_acc' in package.keys():
                self.best_val_acc = package['best_val_acc']

        # LR scheduler
        self.scheduler = None

    def checkpoint_best(self, epoch, name):
        best_path = os.path.join(self.checkpoints_dir_path, name)
        self.logger.info("Saving checkpoint model to {}".format(best_path))
        torch.save({
            'state_dict': self.model.state_dict(),
            'optim_dict': self.optimizer.state_dict(),
            'epoch': epoch + 1,
            'best_val_acc': self.best_val_acc,},
            best_path)

    def train_model(self, batch_size, epochs, log_freq):

        # training starts
        #if self.detect_nan:
        #    torch.autograd.set_detect_anomaly(True)

        for epoch in range(self.starting_epoch, epochs):
            # Set to training mode
            self.model.train()
            self.criterion.train()

            # Init metrics
            epoch_meters = TrainingEpochMeters()
            start_data_loading = time.time()

            start_step = 0
            end_step = 5
            for step in range(start_step, end_step):
                train_filename = os.path.join(self.datadir, 'kifu1k{:02d}.tfrecords.gz').format(step)
                print(train_filename)
                raw_dataset = tf.data.TFRecordDataset([train_filename], compression_type="GZIP")
                train_loader = raw_dataset.map(parse_function).batch(batch_size)

                for i, data in enumerate(train_loader.as_numpy_iterator()):
                    (raw_input, raw_target) = data
                    input = torch.from_numpy(raw_input).clone()
                    target = torch.from_numpy(raw_target.astype(np.float32)).clone()
                    input = input.to(self.device, non_blocking=True)
                    target = target.to(self.device, non_blocking=True)

                    target_var = target.squeeze()
                    # pytorch error: multi-target not supported in CrossEntropyLoss()
                    # @see https://stackoverflow.com/questions/49206550/pytorch-error-multi-target-not-supported-in-crossentropyloss/49209628

                    # measure data loading time
                    epoch_meters.data_time.update(time.time() - start_data_loading)

                    # Training batch starts
                    start_batch = time.time()
                    output = self.model(input)
                    loss = self.criterion(torch.flatten(output), target_var)

                    # compute gradient and do SGD step
                    self.optimizer.zero_grad()
                    loss.backward()
                    self.optimizer.step()

                    if hasattr(self.model, 'clip_weights'):
                        self.model.clip_weights(-1, 1)

                    # measure elapsed time
                    epoch_meters.batch_time.update(time.time() - start_batch)

                    if i % int(log_freq) == 0 or i == self.loader_len - 1:
                        prec1, prec5 = accuracy(output.detach(), target, topk=(1, 5))
                        epoch_meters.losses.update(loss.item(), input.size(0))
                        epoch_meters.top1.update(prec1.item(), input.size(0))
                        epoch_meters.top5.update(prec5.item(), input.size(0))
                        self.logger.training_batch_cli_log(
                            epoch_meters, epoch, i, self.loader_len)

                    # training batch ends
                    start_data_loading = time.time()

            # Set the learning rate
            if epoch % 40 == 0:
                self.optimizer.param_groups[0]['lr'] *= 0.5

            # checkpoint
            self.checkpoint_best(epoch, "checkpoint.tar")


def main():
    network = 'CNN_2W2A'
    datadir = '../../resources/REVERSI_data'
    experiments = '../../resources/brevitas/experiments'
    resume = None
    gpus = '0'

    batch_size = 64
    epochs = 2
    log_freq = 10

    trainer = Trainer(network, datadir, experiments, resume, gpus)
    trainer.train_model(batch_size, epochs, log_freq)


if __name__ == "__main__":
    main()
