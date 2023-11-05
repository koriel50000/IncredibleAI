# -*- coding: utf-8 -*-

import sys

import bnn_pynq_train


def main():
    network = 'CNN_2W2A'
    resume_path = '../../resources/brevitas/experiments/{}/checkpoints/best.tar'.format(network)
    args = ['--evaluate',
            '--network', network,
            '--datadir', '../../resources/brevitas/data',
            '--resume', resume_path,
            '--gpus', None,
            '--num_workers', '2']

    avg = bnn_pynq_train.launch(args)
    print(avg)

    return 0


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)
