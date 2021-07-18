# -*- coding: utf-8 -*-

import sys
import torch
import onnx
from brevitas.export import FINNManager
from finn.core.modelwrapper import ModelWrapper

from models.FC import fc


def load_checkpoint(model, path):
    print('Loading model checkpoint at: {}'.format(path))
    package = torch.load(path, map_location='cpu')
    model_state_dict = package['state_dict']
    model.load_state_dict(model_state_dict)


def export(model, path):
    FINNManager.export(model, input_shape=(1, 28, 28), export_path=path)


def main():
    model = fc()
    load_checkpoint(model, '../../resources/brevitas/experiments/TFC-W1A1/checkpoints/best.tar')
    export(model, '../../resources/brevitas/experiments/1_tfc-w1a1.onnx')

    onnx_model = onnx.load('../../resources/brevitas/experiments/1_tfc-w1a1.onnx')
    finn_model = ModelWrapper(onnx_model)
    print(finn_model.graph)

    #export(finn_model, '../../resources/brevitas/experiments/2_tfc-w1a1.onnx')

    return 0


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)
