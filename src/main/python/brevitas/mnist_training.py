import sys
import argparse
import torch
from brevitas.export import FINNManager

from trainer import Trainer
from models.FC import fc


def add_bool_arg(parser, name, default):
    group = parser.add_mutually_exclusive_group(required=False)
    group.add_argument("--" + name, dest=name, action="store_true")
    group.add_argument("--no_" + name, dest=name, action="store_false")
    parser.set_defaults(**{name: default})


def none_or_str(value):
    if value == "None":
        return None
    return value


def none_or_int(value):
    if value == "None":
        return None
    return int(value)


def parse_args(args):
    parser = argparse.ArgumentParser(description="PyTorch MNIST/CIFAR10 Training")
    # I/O
    parser.add_argument("--datadir", default="./data/", help="Dataset location")
    parser.add_argument("--experiments", default="./experiments", help="Path to experiments folder")
    parser.add_argument("--dry_run", action="store_true", help="Disable output files generation")
    parser.add_argument("--log_freq", type=int, default=10)
    # Execution modes
    parser.add_argument("--evaluate", dest="evaluate", action="store_true", help="evaluate model on validation set")
    parser.add_argument("--resume", dest="resume", type=none_or_str,
                        help="Resume from checkpoint. Overrides --pretrained flag.")
    add_bool_arg(parser, "detect_nan", default=False)
    # Compute resources
    parser.add_argument("--num_workers", default=4, type=int, help="Number of workers")
    parser.add_argument("--gpus", type=none_or_str, default="0", help="Comma separated GPUs")
    # Optimizer hyperparams
    parser.add_argument("--batch_size", default=100, type=int, help="batch size")
    parser.add_argument("--lr", default=0.02, type=float, help="Learning rate")
    parser.add_argument("--optim", type=none_or_str, default="ADAM", help="Optimizer to use")
    parser.add_argument("--loss", type=none_or_str, default="SqrHinge", help="Loss function to use")
    parser.add_argument("--scheduler", default="FIXED", type=none_or_str, help="LR Scheduler")
    parser.add_argument("--milestones", type=none_or_str, default='100,150,200,250', help="Scheduler milestones")
    parser.add_argument("--momentum", default=0.9, type=float, help="Momentum")
    parser.add_argument("--weight_decay", default=0, type=float, help="Weight decay")
    parser.add_argument("--epochs", default=1000, type=int, help="Number of epochs")
    parser.add_argument("--random_seed", default=1, type=int, help="Random seed")
    # Neural network Architecture
    parser.add_argument("--network", default="LFC_1W1A", type=str, help="neural network")
    parser.add_argument("--pretrained", action='store_true', help="Load pretrained model")
    return parser.parse_args(args)


def resume():
    print()
    # print('Loading model checkpoint at: {}'.format(resume))
    # package = torch.load(resume, map_location='cpu')
    # model_state_dict = package['state_dict']
    # model.load_state_dict(model_state_dict)


def export(model, path):
    FINNManager.export(model, input_shape=(1, 28, 28), export_path=path)


def main():
    cmd_args = ['--network', 'TFC',
            '--datadir', '../../resources/brevitas/data',
            '--experiments', '../../resources/brevitas/experiments',
            '--gpus', None,
            '--num_workers', '2',
            '--epochs', '2']
    args = parse_args(cmd_args)

    model = fc()

    trainer = Trainer(model, args)

    trainer.train_model()

    export(model, '../../resources/brevitas/experiments/tfc-w1a1.onnx')

    return 0


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)
