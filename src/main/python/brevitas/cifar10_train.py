# -*- coding: utf-8 -*-

import sys

import bnn_pynq_train


def main():
    args = ['--network', 'CNV-W1A1',
            '--datadir', '../../resources/brevitas/data',
            '--experiments', '../../resources/brevitas/experiments',
            '--gpus', None,
            '--num_workers', '2',
            '--epochs', '2']

    bnn_pynq_train.launch(args)

    return 0


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)
