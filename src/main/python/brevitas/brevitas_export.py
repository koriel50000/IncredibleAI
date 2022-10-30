import sys
import torch
from models import model_with_cfg
import brevitas.onnx as bo


def main(args):
    network = 'lfc_1w1a' #args[1]
    resume = '../../resources/brevitas/experiments/LFC_1W1A_1W1A_20221030_135656/checkpoints/best.tar' #args[2]

    model, cfg = model_with_cfg(network, False)

    print('Loading model checkpoint at: {}'.format(resume))
    package = torch.load(resume, map_location='cpu')
    model_state_dict = package['state_dict']
    model.load_state_dict(model_state_dict)

    export_path = '../../resources/brevitas/experiments/' + network + '.onnx'
    bo.export_finn_onnx(model, (1, 1, 28, 28), export_path)


if __name__ == "__main__":
    main(sys.argv)
