from functools import reduce
from operator import mul

from torch.nn import Module, ModuleList, BatchNorm1d, Dropout
import torch

from brevitas.nn import QuantIdentity, QuantLinear
from .common import CommonWeightQuant, CommonActQuant
from .tensor_norm import TensorNorm

DROPOUT = 0.2


class FC(Module):

    def __init__(
            self,
            num_classes,
            weight_bit_width,
            act_bit_width,
            in_bit_width,
            in_channels,
            out_features,
            in_features=(28, 28)):
        super(FC, self).__init__()

        self.features = ModuleList()
        self.features.append(QuantIdentity(act_quant=CommonActQuant, bit_width=in_bit_width))
        self.features.append(Dropout(p=DROPOUT))
        in_features = reduce(mul, in_features)
        for out_features in out_features:
            self.features.append(QuantLinear(
                in_features=in_features,
                out_features=out_features,
                bias=False,
                weight_bit_width=weight_bit_width,
                weight_quant=CommonWeightQuant))
            in_features = out_features
            self.features.append(BatchNorm1d(num_features=in_features))
            self.features.append(QuantIdentity(act_quant=CommonActQuant, bit_width=act_bit_width))
            self.features.append(Dropout(p=DROPOUT))
        self.features.append(QuantLinear(
                in_features=in_features,
                out_features=num_classes,
                bias=False,
                weight_bit_width=weight_bit_width,
                weight_quant=CommonWeightQuant))
        self.features.append(TensorNorm())

        for m in self.modules():
          if isinstance(m, QuantLinear):
            torch.nn.init.uniform_(m.weight.data, -1, 1)

    def clip_weights(self, min_val, max_val):
        for mod in self.features:
            if isinstance(mod, QuantLinear):
                mod.weight.data.clamp_(min_val, max_val)
    
    def forward(self, x):
        x = x.view(x.shape[0], -1)
        x = 2.0 * x - torch.tensor([1.0], device=x.device)
        for mod in self.features:
            x = mod(x)
        return x


def fc():
    net = FC(
        weight_bit_width=1,
        act_bit_width=1,
        in_bit_width=1,
        in_channels=1,
        out_features=[64, 64, 64],
        num_classes=10)
    return net
