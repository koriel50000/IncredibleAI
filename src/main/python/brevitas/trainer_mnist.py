import sys
import torch
from models import model_with_cfg
from brevitas.export import FINNManager


def main(network, resume):

    model, cfg = model_with_cfg(network, False)

    print('Loading model checkpoint at: {}'.format(resume))
    package = torch.load(resume, map_location='cpu')
    model_state_dict = package['state_dict']
    model.load_state_dict(model_state_dict)

    FINNManager.export(model, input_shape=(1, 28, 28), export_path='../../resources/brevitas/experiments/lfc_1w1a.onnx')
    return 0


if __name__ == "__main__":
    exit_code = main('lfc_1w1a', '../../resources/brevitas/experiments/LFC_1W1A_1W1A_20210711_204713/checkpoints/best.tar')
    sys.exit(exit_code)
