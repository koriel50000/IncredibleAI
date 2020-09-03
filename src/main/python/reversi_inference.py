# -*- coding: utf-8 -*-

import sys
import os.path

import reversi
import converter

from tensorflow import keras

model = keras.models.load_model("../resources/model/")

# global変数宣言
num_total = 0
num_correct = 0


#
# 棋譜を評価する
#
def evaluate_records(sess, move_record, eval_record):
    global num_total, num_correct

    reversi.initialize()

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
        # 評価値がdelta範囲内で降順、同値は着手のindexで昇順に並び替え
        # 正答率は評価値の上位3つで計算する
        # 1 2 3 : 1.0
        # 1 3 2 : 0.9
        # 1 x x : 0.8
        # 2 1 3 : 0.7
        # 2 3 1 : 0.6
        # 2 x x : 0.4
        # 3 1 2 : 0.3
        # 3 2 1 : 0.2
        # 3 x x : 0.1
        # x x x : 0.0 上記以外は0.0
        # まずはこれでやってみる

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
        # 全体の正答率
        # depth毎の正答率
        if i % 10 == 0:
            print("{0}: {1}".format(i, file))
            print(float(num_correct) / num_total)


#
# メイン
#
def main(args):

    return 0


if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)
