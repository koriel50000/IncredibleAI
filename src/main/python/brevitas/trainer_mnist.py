import sys
import torch
from models import model_with_cfg
from brevitas.export import FINNManager
brevitas.onnx as bo

def main(network, resume):

    model, cfg = model_with_cfg(network, True)

    # print('Loading model checkpoint at: {}'.format(resume))
    # package = torch.load(resume, map_location='cpu')
    # model_state_dict = package['state_dict']
    # model.load_state_dict(model_state_dict)

    FINNManager.export(model, input_shape=(1, 28, 28), export_path='../../resources/brevitas/experiments/tfc-w1a1.onnx')
    return 0


if __name__ == "__main__":
    exit_code = main('tfc_1w1a', '../../resources/brevitas/experiments/TFC_1W1A_1W1A_20210712_170600/checkpoints/best.tar')
    sys.exit(exit_code)
