# -*- coding: utf-8 -*-

import sys

import bnn_pynq_train


def main():
    args = ['--evaluate',
            '--datadir', '../../resources/brevitas/data',
            '--network', 'TFC-W1A1',
            '--resume', '../../resources/brevitas/experiments/TFC-W1A1/checkpoints/best.tar',
            '--gpus', None,
            '--num_workers', '2']

    avg = bnn_pynq_train.launch(args)
    print(avg)

    return 0


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)
