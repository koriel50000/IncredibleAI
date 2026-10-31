# -*- coding: utf-8 -*-

import sys
import glob
import os.path
import zipfile
from collections import deque
import math

import torch
import torch.nn as nn
import brevitas.nn as qnn

import reversi
import oddeven_feature as feature
import cnn_model as model


#
# 棋譜を評価する
#
def evaluating_records(move_record, eval_records):
    global total_count

    reversi.clear()
    feature.clear()

    payload = []
    states = []

    for index, actual_move in enumerate(feature.convert_moves(move_record)):
        # print("index:{0} move:{1}".format(index, actual_move))
        if len(reversi.available_moves()) == 0:
            reversi.next_turn(True)
            if len(reversi.available_moves()) == 0:
                raise Exception("Error!")  # 先手・後手両方パスで終了は考慮しない

        actual_evals = feature.convert_evals(eval_records[index])
        payload.append(actual_evals)

        for entry in actual_evals:
            coord = entry['coord']
            state = feature.convert_state(reversi, coord)
            states.append(state)

        coord = feature.move_to_coord(actual_move)
        flipped = reversi.make_move(coord)
        feature.increase_flipped(coord, flipped)
        reversi.next_turn(False)

    states_ = feature.convert_states(states)
    values_ = model.calculate_predicted_values(states_)
    values = deque(values_)

    for index, actual_evals in enumerate(payload):
        predicted_evals = []
        for entry in actual_evals:
            coord = entry['coord']
            ndigits = 3  # 評価値は小数点第三位を四捨五入
            value = entry['value']
            entry['value'] = round(value, ndigits)
            value = values.popleft()
            predicted_value = round(value, ndigits)
            predicted_evals.append({'coord': coord, 'value': predicted_value})

        for entry in actual_evals:
            move = feature.coord_to_move(entry['coord'])
            value = entry['value']
            print(' {0}:{1:.2f}'.format(move, value), end='')
        print()
        for entry in predicted_evals:
            move = feature.coord_to_move(entry['coord'])
            value = entry['value']
            print(' {0}:{1:.2f}'.format(move, value), end='')
        print()
        print()

#
# 学習モデルを評価する
#
def evaluating_model(path, filenames):
    for i, filename in enumerate(filenames):
        file = os.path.join(path, filename)
        with open(file, "r") as fin:
            move_record = fin.readline().strip()
            eval_records = [x.strip() for x in fin.readlines()]

        evaluating_records(move_record, eval_records)

        # 全体の正答率
        print("{0}: {1}".format(i, file))


def convert(kifu_trained):
    state_dict = kifu_trained.state_dict()
    # print(state_dict.keys())

    scale = 0.0
    scale_model0 = 0.0
    gamma = 0.0
    beta = 0.0
    mean = 0.0
    var = 0.0
    eps = 1e-5
    for name, m in (kifu_trained.named_modules()):
        # print(name)
        if isinstance(m, qnn.QuantConv2d):
            scale = m.quant_weight().scale
            scale = scale.view(-1)
            weight = torch.floor(m.quant_weight() / m.quant_weight().scale)
            print_conv_weight(name, m, weight)
            # print(f'{name} {m.quant_weight().scale.shape}')
        elif isinstance(m, qnn.QuantLinear):
            scale = m.quant_weight().scale
            scale = scale.view(-1)
            weight = torch.floor(m.quant_weight() / m.quant_weight().scale)
            print_linear_weight(name, m, weight)
            # print(f'{name} {m.quant_weight().scale.shape}')
        elif isinstance(m, nn.BatchNorm2d):
            params = m.state_dict()
            gamma = params['weight']
            beta = params['bias']
            mean = params['running_mean']
            var = params['running_var']
            # state_dict[name + '.weight'] = gamma
            # state_dict[name + '.bias'] = beta
            # state_dict[name + '.running_mean'] = mean
            # state_dict[name + '.running_var'] = var
            # state_dict[name + '.scale'] = scale_model0 if name == 'backbone.model0.bn1' else scale
            print(f'{name} {scale.shape}')
        elif isinstance(m, qnn.QuantReLU):
            # y = gamma * (x - mean) / math.sqrt(var + eps) + beta
            y = torch.tensor([0.125, 0.375, 0.625, 0.875, 1.25, 1.75, 2.5])
            std = torch.sqrt(var + eps)
            x = (y - beta[:, None]) / gamma[:, None] * std[:, None] + mean[:, None]
            if name == 'backbone.model0.relu1':
                scale = scale_model0
            threshold = torch.ceil(x / scale[:, None] * 4.0)
            print_threshold(name, 7, threshold)
            # print(f'{name}.threshold {threshold.shape}')
        elif isinstance(m, qnn.QuantIdentity):
            # y = gamma * (x - mean) / math.sqrt(var + eps) + beta
            y = torch.tensor([-2.5, -1.75, -1.25, -0.875, -0.625, -0.375, -0.125, 0.125, 0.375, 0.625, 0.875, 1.25, 1.75, 2.5])
            std = torch.sqrt(var + eps)
            x = (y[None, :] - beta[:, None]) / gamma[:, None] * std[:, None] + mean[:, None]
            threshold = torch.ceil(x / scale[:, None] * 4.0)
            print_threshold(name, 14, threshold)
            # print(f'{name} threshold={threshold.shape}')


def print_conv_weight(name, m, weight):
    weight = weight.int()
    n, c, h, w = weight.shape
    if m.groups > 1 or (m.groups == 1 and c == 1):
        c = h * w
        h = 1
        w = 1
        weight = weight.reshape(n, h, w, c)
    else:
        weight = weight.permute(0, 2, 3, 1)
        n, h, w, c = weight.shape
    var = f'{name}.weight'.replace('.', '_')
    assert c in [16, 32, 64]
    print(f'{var} = [')
    print('#', weight.shape)
    for f in range(n):
        for y in range(h):
            for x in range(w):
                print('0x', end='')
                for z in reversed(range(c)):
                    channel = weight[f, y, x, z].item()
                    if channel > 0:
                        channel = int(math.log2(channel)) + 1
                    elif channel < 0:
                        channel = int(math.log2(-channel)) + 0x9
                    else:
                        channel = 0
                    if c >= 32 and z % 16 == 0:
                        print(f'{channel:x}', end='' if z == 0 else ',0x')
                    else:
                        print(f'{channel:x}', end='')
                print(', ', end='')
        print()
    print(']')
    print()


def print_linear_weight(name, m, weight):
    weight = weight.int()
    n, c = weight.shape
    var = f'{name}.weight'.replace('.', '_')
    assert c in [256]
    print(f'{var} = [')
    print('#', weight.shape)
    for f in range(n):
        print('0x', end='')
        for z in reversed(range(c)):
            channel = weight[f, z].item()
            if channel > 0:
                channel = int(math.log2(channel)) + 1
            elif channel < 0:
                channel = int(math.log2(-channel)) + 0x9
            else:
                channel = 0
            if c >= 32 and z % 16 == 0:
                print(f'{channel:x}', end='' if z == 0 else ',0x')
            else:
                print(f'{channel:x}', end='')
        print(', ', end='')
        print()
    print(']')
    print()


def print_threshold(name, m, threshold):
    threshold = threshold.int()
    n, m = threshold.shape
    var = f'{name}.threshold'.replace('.', '_')
    print(f'{var} = [')
    print('#', threshold.shape)
    for f in range(n):
        row = threshold[f, :].tolist()
        print(f'{", ".join(map(str, row))},')
    print(']')
    print()


#
# メイン
#
def main():
    network = 'CNN_4W4A'
    resume = '../../resources/brevitas/experiments/{}/checkpoints/checkpoint.tar'.format(network)
    model.load_model(resume)

    convert(model.model)
    # path = "../../resources/records/"
    # with zipfile.ZipFile(os.path.join(path, "kifu102245.zip")) as records_zip:
    #
    #     namelist = records_zip.namelist()
    #
    #     filenames = namelist[100000: 100001]
    #     records_zip.extractall(path, filenames)
    #
    #     with torch.no_grad():
    #         evaluating_model(path, filenames)
    #
    #     for file in glob.glob(os.path.join(path, "kifu*.txt")):
    #         os.remove(file)


if __name__ == "__main__":
    main()
