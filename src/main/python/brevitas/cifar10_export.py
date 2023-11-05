# -*- coding: utf-8 -*-

import sys
import torch
from models import model_with_cfg
from brevitas.export import FINNManager


def load_checkpoint(model, path):
    print('Loading model checkpoint at: {}'.format(path))
    package = torch.load(path, map_location='cpu')
    model_state_dict = package['state_dict']
    model.load_state_dict(model_state_dict)


def export(model, path):
    FINNManager.export(model, input_shape=(1, 3, 32, 32), export_path=path)


def main():
    network = 'CNV_2W2A'
    resume_path = '../../resources/brevitas/experiments/{}/checkpoints/best.tar'.format(network)

    model, cfg = model_with_cfg(network, False)
    load_checkpoint(model, resume_path)
    export_path = '../../resources/brevitas/experiments/{}.onnx'.format(network)
    export(model, export_path)

    return 0


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)
