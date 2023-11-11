# -*- coding: utf-8 -*-

import sys

import bnn_pynq_train


def main():
    args = ['--network', 'TFC_2W2A',
            '--datadir', '../../resources/brevitas/data',
            '--experiments', '../../resources/brevitas/experiments',
            '--gpus', None,
            '--num_workers', '2',
            '--epochs', '5']

    bnn_pynq_train.launch(args)


if __name__ == "__main__":
    main()
