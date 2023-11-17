# -*- coding: utf-8 -*-

import bnn_pynq_train


def main():
    network = 'TFC_2W2A'
    resume_path = '../../resources/brevitas/experiments/{}/checkpoints/best.tar'.format(network)
    args = ['--evaluate',
            '--network', network,
            '--datadir', '../../resources/brevitas/data',
            '--resume', resume_path,
            '--gpus', '0',
            '--num_workers', '2']

    bnn_pynq_train.launch(args)


if __name__ == "__main__":
    main()
