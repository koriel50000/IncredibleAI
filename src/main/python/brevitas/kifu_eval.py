# -*- coding: utf-8 -*-

import os
import random
import time

import numpy as np
import torch
from torch import nn
import torch.optim as optim
import tensorflow as tf

from logger import EvalEpochMeters
from logger import Logger
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
        res.append(correct_k.mul_(16.0 / batch_size))
    return res


class Trainer(object):

    def __init__(self, network, resume, gpus):

        model, cfg = model_with_cfg(network, False)

        # Init arguments
        self.output_dir_path, _ = os.path.split(resume)
        self.output_dir_path, _ = os.path.split(self.output_dir_path)

        self.logger = Logger(self.output_dir_path, True)

        # Randomness
        random_seed = 1
        random.seed(random_seed)
        torch.manual_seed(random_seed)
        torch.cuda.manual_seed_all(random_seed)

        dataset = cfg.get('MODEL', 'DATASET')
        self.num_classes = cfg.getint('MODEL', 'NUM_CLASSES')

        test_filename = os.path.join('../../resources/REVERSI_data', 'kifu1k100.tfrecords.gz')
        raw_dataset = tf.data.TFRecordDataset([test_filename], compression_type="GZIP")
        batch_size = 16
        self.test_loader = raw_dataset.map(parse_function).batch(batch_size)
        self.loader_len = 32768

        # Init starting values
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
        self.criterion = nn.CrossEntropyLoss()
        self.criterion = self.criterion.to(device=self.device)

        # Init optimizer
        self.optimizer = optim.Adam(
            self.model.parameters(), lr=0.02, weight_decay=0)

        # LR scheduler
        self.scheduler = None

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


def main():
    network = 'CNN_2W2A'
    resume = '../../resources/brevitas/experiments/{}/checkpoints/checkpoint.tar'.format(network)
    gpus = None
    trainer = Trainer(network, resume, gpus)

    with torch.no_grad():
        trainer.eval_model()


if __name__ == "__main__":
    main()
