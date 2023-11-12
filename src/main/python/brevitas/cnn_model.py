# -*- coding: utf-8 -*-

import torch

from models import model_with_cfg

model, _ = model_with_cfg('CNN_2W2A', False)
device = torch.device('cpu')


def load_model(resume):
    global model

    # Resume checkpoint
    print('Loading model checkpoint at: {}'.format(resume))
    package = torch.load(resume, map_location='cpu')
    model_state_dict = package['state_dict']
    model.load_state_dict(model_state_dict, strict=False)

    # switch to evaluate mode
    model.eval()


#
# 予測値を計算する
#
def calculate_predicted_values(states):
    predictions = []
    for state in states:
        predictions.append(calculate_predicted_value(state))
    return predictions


#
# 予測値を計算する
#
def calculate_predicted_value(state):
    input = torch.from_numpy(state.reshape(1, 16, 8, 8)).clone()
    input = input.to(device, non_blocking=True)
    predictions_ = model(input)
    #print(predictions_[0])
    return predictions_[0][0].item()
