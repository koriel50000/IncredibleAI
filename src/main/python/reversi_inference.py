# -*- coding: utf-8 -*-

import sys
import glob
import os.path
import zipfile

import reversi
import converter
import cnn_model

import tensorflow.compat.v1 as tf

tf.disable_v2_behavior()

# global変数宣言
num_total = 0
num_correct = 0


#
# 棋譜を評価する
#
def evaluate_records(sess, move_record, eval_record):
    global num_total, num_correct

    reversi.init()

    for index, actual_move in enumerate(converter.convert_moves(move_record)):
        # print "count:{0} move:{1}".format(index + 1, actual_move)
        values = []
        predicted_values = []
        evals = converter.convert_evals(eval_record[index])
        for entry in evals:
            coord = entry['coord']
            value = entry['value']
            state = converter.convert_state(reversi, coord)
            predicted_value = cnn_model.calculate_predicted_value(sess, state)
            values.append(value)
            predicted_values.append(predicted_value)
            # probs = np.r_[pprobs, nprobs]
            # view_probs = ["{0:.2f}".format(x) for x in probs]
        for value in values:
            print(' {0:.2f}'.format(value), end='')
        print()
        for value in predicted_values:
            print(' {0:.2f}'.format(value), end='')
        print()
        print()
        num_total += 1
        if predicted_value == value:
            num_correct += 1

        coord = converter.move_to_coord(actual_move)
        reversi.make_move(coord)
        reversi.next_turn()


#
# 学習モデルを評価する
#
def evaluating_model(sess, path, filenames, begin, end):
    for i in range(begin, end):
        file = os.path.join(path, filenames[i])
        with open(file, "r") as fin:
            move_record = fin.readline().strip()
            eval_record = [x.strip() for x in fin.readlines()]

        evaluate_records(sess, move_record, eval_record)

        if i % 10 == 0:
            print("{0}: {1}".format(i, file))
            print(float(num_correct) / num_total)


#
# メイン
#
def main(args):
    sess = tf.Session()

    saver = tf.train.Saver()
    saver.restore(sess, "../resources/checkpoint/model.ckpt-1")

    path = "../resources/records/"
    with zipfile.ZipFile(os.path.join(path, "kifu102245.zip")) as records_zip:
        offset = 100000
        size = 1
        filenames = records_zip.namelist()[offset: offset + size]
        records_zip.extractall(path, filenames)

        evaluating_model(sess, path, filenames, 0, size)

        for file in glob.glob(os.path.join(path, "kifu*.txt")):
            os.remove(file)

    sess.close()

    return 0


if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)
