# Copyright (C) 2023, Advanced Micro Devices, Inc. All rights reserved.
# SPDX-License-Identifier: BSD-3-Clause

import torch
import torch.nn as nn
import brevitas.nn as qnn

from .common import Int4ActQuant, Int4WeightQuant

KERNEL_SIZE = 3


class CNN(nn.Module):

    def __init__(self, weight_bit_width, act_bit_width, in_bit_width, out_bit_width, in_ch):
        super(CNN, self).__init__()

        # self.quant0 = qnn.QuantIdentity(
        #     act_quant=Int4ActQuant,
        #     act_bit_width=in_bit_width)

        self.conv1 = qnn.QuantConv2d(
            kernel_size=KERNEL_SIZE,
            in_channels=in_ch,
            out_channels=32,
            bias=False,
            weight_quant=Int4WeightQuant,
            weight_bit_width=weight_bit_width)
        self.bn1 = nn.BatchNorm2d(32)
        self.quant1 = qnn.QuantIdentity(
            act_quant=Int4ActQuant,
            act_bit_width=act_bit_width)

        self.conv2 = qnn.QuantConv2d(
            kernel_size=KERNEL_SIZE,
            in_channels=32,
            out_channels=32,
            bias=False,
            weight_quant=Int4WeightQuant,
            weight_bit_width=weight_bit_width)
        self.bn2 = nn.BatchNorm2d(32)
        self.quant2 = qnn.QuantIdentity(
            act_quant=Int4ActQuant,
            act_bit_width=act_bit_width)

        self.conv3 = qnn.QuantConv2d(
            kernel_size=KERNEL_SIZE,
            in_channels=32,
            out_channels=64,
            bias=False,
            weight_quant=Int4WeightQuant,
            weight_bit_width=weight_bit_width)
        self.bn3 = nn.BatchNorm2d(64)
        self.quant3 = qnn.QuantIdentity(
            act_quant=Int4ActQuant,
            act_bit_width=act_bit_width)

        self.fc4 = qnn.QuantLinear(
            in_features=256,
            out_features=256,
            bias=False,
            weight_quant=Int4WeightQuant,
            weight_bit_width=weight_bit_width)
        self.bn4 = nn.BatchNorm1d(256)
        self.quant4 = qnn.QuantIdentity(
            act_quant=Int4ActQuant,
            act_bit_width=act_bit_width)

        self.fc5 = qnn.QuantLinear(
            in_features=256,
            out_features=1,
            bias=False,
            weight_quant=Int4WeightQuant,
            weight_bit_width=out_bit_width)

        for m in self.modules():
            if isinstance(m, qnn.QuantConv2d) or isinstance(m, qnn.QuantLinear):
                torch.nn.init.uniform_(m.weight.data, -1, 1)

    # def clip_weights(self, min_val, max_val):
    #     for mod in self.conv_features:
    #         if isinstance(mod, qnn.QuantConv2d):
    #             mod.weight.data.clamp_(min_val, max_val)
    #     for mod in self.linear_features:
    #         if isinstance(mod, qnn.QuantLinear):
    #             mod.weight.data.clamp_(min_val, max_val)

    def forward(self, x):
        x = 2.0 * x - torch.tensor([1.0], device=x.device)
        # x = self.quant0(x)
        x = self.conv1(x)
        x = self.bn1(x)
        x = self.quant1(x)
        x = self.conv2(x)
        x = self.bn2(x)
        x = self.quant2(x)
        x = self.conv3(x)
        x = self.bn3(x)
        x = self.quant3(x)
        x = x.view(x.shape[0], -1)
        x = self.fc4(x)
        x = self.bn4(x)
        x = self.quant4(x)
        x = self.fc5(x)
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
