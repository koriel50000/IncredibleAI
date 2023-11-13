# -*- coding: utf-8 -*-

import sys
import pynq
import numpy as np

import models


accel = models.cnn_2w2a_kifu()
print("Expected input shape and datatype: %s %s" % (str(accel.ishape_normal()), str(accel.idt())))
print("Expected output shape and datatype: %s %s" % (str(accel.oshape_normal()), str(accel.odt())))


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
    state = np.transpose(state.reshape(16, 8, 8), [1, 2, 0])
    ibuf_normal = state.reshape(accel.ishape_normal()) * 255
    obuf_normal = accel.execute(ibuf_normal)
    return obuf_normal.flatten()[0]
