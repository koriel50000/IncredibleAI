# -*- coding: utf-8 -*-

import sys
import torch
import onnx
from brevitas.export import FINNManager
from finn.core.modelwrapper import ModelWrapper

from models.CNV import cnv


def load_checkpoint(model, path):
    print('Loading model checkpoint at: {}'.format(path))
    package = torch.load(path, map_location='cpu')
    model_state_dict = package['state_dict']
    model.load_state_dict(model_state_dict)


def export(model, path):
    FINNManager.export(model, input_shape=(1, 3, 32, 32), export_path=path)


def main():
    model = cnv()
    load_checkpoint(model, '../../resources/brevitas/experiments/CNV-W1A1/checkpoints/best.tar')
    export(model, '../../resources/brevitas/experiments/1_cnv-w1a1.onnx')

    onnx_model = onnx.load('../../resources/brevitas/experiments/1_cnv-w1a1.onnx')
    finn_model = ModelWrapper(onnx_model)
    print(finn_model.graph)

    #export(finn_model, '../../resources/brevitas/experiments/2_cnv-w1a1.onnx')

    return 0


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)
