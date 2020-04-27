#!/usr/bin/python
# -*- coding: utf-8 -*-

#import pychecker.checker
import struct
import numpy as np
import datasets

import tensorflow.compat.v1 as tf
tf.disable_v2_behavior()

# 定数宣言
input_rows = 8
input_cols = 8
input_channel = 16

# モデル定義
def weight_variable(shape):
  initial = tf.truncated_normal(shape, stddev=0.1)
  return tf.Variable(initial)

def bias_variable(shape):
  initial = tf.constant(0.1, shape=shape)
  return tf.Variable(initial)

def conv2d(x, W):
  return tf.nn.conv2d(x, W, strides=[1, 1, 1, 1], padding='SAME')

# 変数宣言
x = tf.placeholder(tf.float32, [None, input_rows*input_cols*input_channel])
y_ = tf.placeholder(tf.float32, [None])
x_image = tf.reshape(x, [-1, input_rows, input_cols, input_channel])

k_filter = 64

# 畳み込み層1
W_conv1 = weight_variable([5, 5, input_channel, k_filter])
h_conv1 = tf.nn.relu(conv2d(x_image, W_conv1))

# 畳み込み層2
W_conv2 = weight_variable([3, 3, k_filter, k_filter])
h_conv2 = tf.nn.relu(conv2d(h_conv1, W_conv2))

# 畳み込み層3
W_conv3 = weight_variable([3, 3, k_filter, k_filter])
h_conv3 = tf.nn.relu(conv2d(h_conv2, W_conv3))

# 畳み込み層4
W_conv4 = weight_variable([3, 3, k_filter, k_filter])
h_conv4 = tf.nn.relu(conv2d(h_conv3, W_conv4))

# 畳み込み層5
W_conv5 = weight_variable([3, 3, k_filter, k_filter])
h_conv5 = tf.nn.relu(conv2d(h_conv4, W_conv5))

# 畳み込み層6
W_conv6 = weight_variable([1, 1, k_filter, 1])
h_conv6 = tf.nn.relu(conv2d(h_conv5, W_conv6))

# 全結合層
W_fc1 = weight_variable([input_rows*input_cols*1, 256])
b_fc1 = bias_variable([256])

h_conv_flat = tf.reshape(h_conv6, [-1, input_rows*input_cols*1])
h_fc1 = tf.nn.relu(tf.matmul(h_conv_flat, W_fc1)+b_fc1)

# 出力層
keep_prob = tf.placeholder("float")
h_fc1_drop = tf.nn.dropout(h_fc1, keep_prob)

W_fc2 = weight_variable([256, 1])
b_fc2 = bias_variable([1])

y_conv = tf.nn.tanh(tf.matmul(h_fc1, W_fc2) + b_fc2)

loss = 0.5 * tf.reduce_sum((y_conv-y_) **2)

train_step = tf.train.AdamOptimizer(1e-5).minimize(loss)

#def minimize(i, dummy):
#    # 二乗和誤差
#    loss = 0.5 * tf.reduce_sum((y_conv-y_) **2)
#    
#    train_step = tf.train.AdamOptimizer(1e-5).minimize(loss)
#    return i + 1, loss
#
#loop = tf.while_loop(lambda i, dummy: i < 16, minimize, [0, 0.0])

#
# モデルを学習する
#
def training_model(sess, prefix):
    reversi_data = datasets.read_data_sets('../resources/REVERSI_data/', prefix, one_hot=False)
    print("images;", reversi_data.train.images.shape)
    print("labels:", reversi_data.train.labels.shape)
    i = 0
    while reversi_data.train.epochs_completed < 1: # 10:
        batch_xs, batch_ys = reversi_data.train.next_batch(1, shuffle=False)
        sess.run(train_step, feed_dict={x: batch_xs, y_: batch_ys, keep_prob: 0.99})
        #index, loss = sess.run(loop, feed_dict={x: batch_xs, y_: batch_ys, keep_prob: 0.99})
        i += 1
        if i % 10000 == 0:
            print(str(i) + ":", str(sess.run(loss, feed_dict={x: batch_xs, y_: batch_ys, keep_prob: 1.0})))

#
# 予測値を計算する
#
def calculate_predicted_value(sess, state):
    state_ = state.reshape(1, -1)
    y_ = sess.run(y_conv, feed_dict={x: state_, keep_prob: 1.0})
    return y_[0][0]
