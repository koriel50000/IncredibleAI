# Copyright (C) 2023, Advanced Micro Devices, Inc. All rights reserved.
# SPDX-License-Identifier: BSD-3-Clause

import torch
import torch.nn as nn
import brevitas.nn as qnn

from .common import Int4ActQuant, Int4WeightQuant

CNV_OUT_CH_POOL = [(32, False), (32, False), (64, False)]
INTERMEDIATE_FC_FEATURES = [(256, 256)]
LAST_FC_IN_FEATURES = 256
LAST_FC_OUT_FEATURES = 1
POOL_SIZE = 2
KERNEL_SIZE = 3


class CNN(nn.Module):

    def __init__(self, weight_bit_width, act_bit_width, in_bit_width, out_bit_width, in_ch):
        super(CNN, self).__init__()

        self.conv_features = nn.ModuleList()
        self.linear_features = nn.ModuleList()

        self.conv_features.append(qnn.QuantIdentity(act_quant=Int4ActQuant))

        for out_ch, is_pool_enabled in CNV_OUT_CH_POOL:
            self.conv_features.append(
                qnn.QuantConv2d(
                    kernel_size=KERNEL_SIZE,
                    in_channels=in_ch,
                    out_channels=out_ch,
                    bias=False,
                    weight_quant=Int4WeightQuant,
                    weight_bit_width=weight_bit_width))
            in_ch = out_ch
            self.conv_features.append(nn.BatchNorm2d(in_ch))
            self.conv_features.append(
                qnn.QuantIdentity(act_quant=Int4ActQuant))
            if is_pool_enabled:
                self.conv_features.append(nn.MaxPool2d(kernel_size=POOL_SIZE))

        for in_features, out_features in INTERMEDIATE_FC_FEATURES:
            self.linear_features.append(
                qnn.QuantLinear(
                    in_features=in_features,
                    out_features=out_features,
                    bias=False,
                    weight_quant=Int4WeightQuant,
                    weight_bit_width=weight_bit_width))
            self.linear_features.append(nn.BatchNorm1d(out_features))
            self.linear_features.append(
                qnn.QuantIdentity(act_quant=Int4ActQuant))

        self.linear_features.append(
            qnn.QuantLinear(
                in_features=LAST_FC_IN_FEATURES,
                out_features=LAST_FC_OUT_FEATURES,
                bias=False,
                weight_quant=Int4WeightQuant,
                weight_bit_width=out_bit_width))

        for m in self.modules():
            if isinstance(m, qnn.QuantConv2d) or isinstance(m, qnn.QuantLinear):
                torch.nn.init.uniform_(m.weight.data, -1, 1)

    def clip_weights(self, min_val, max_val):
        for mod in self.conv_features:
            if isinstance(mod, qnn.QuantConv2d):
                mod.weight.data.clamp_(min_val, max_val)
        for mod in self.linear_features:
            if isinstance(mod, qnn.QuantLinear):
                mod.weight.data.clamp_(min_val, max_val)

    def forward(self, x):
        x = 2.0 * x - torch.tensor([1.0], device=x.device)
        for mod in self.conv_features:
            x = mod(x)
        x = x.view(x.shape[0], -1)
        for mod in self.linear_features:
            x = mod(x)
        return x


def cnn(cfg):
    weight_bit_width = cfg.getint('QUANT', 'WEIGHT_BIT_WIDTH')
    act_bit_width = cfg.getint('QUANT', 'ACT_BIT_WIDTH')
    in_bit_width = cfg.getint('QUANT', 'IN_BIT_WIDTH')
    out_bit_width = cfg.getint('MODEL', 'OUT_BIT_WIDTH')
    in_channels = cfg.getint('MODEL', 'IN_CHANNELS')
    net = CNN(
        weight_bit_width=weight_bit_width,
        act_bit_width=act_bit_width,
        in_bit_width=in_bit_width,
        out_bit_width=out_bit_width,
        in_ch=in_channels)
    return net
