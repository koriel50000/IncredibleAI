# -*- coding: utf-8 -*-

import sys
import glob
import os.path
import zipfile

import reversi
import converter

from tensorflow import keras

model = keras.models.load_model("../resources/model/")

# global変数宣言
total_accuracy = 0.0
move_accuracies = [0.0] * 60


#
# 予測値を計算する
#
def calculate_predicted_value(state):
    state_ = state.reshape(1, -1)
    predictions_ = model.predict(state_)
    return predictions_[0][0]


#
# 正答率を計算する
#
def calculate_accuracies(index, values, predicted_values):
    global total_accuracy, move_accuracies

    # TODO 評価値を並び替え

    # 評価値がepsilon範囲内で降順、同値は棋譜の着手順
    # 正答率は評価値の上位3つで計算する
    # 1.0 : 1 2 3 | 1 2 | 1
    # 0.9 : 1 2 x
    # 0.8 : 1 3 2
    # 0.7 : 1 3 x
    # 0.5 : 1 x x | 1 x
    # 0.7 : 2 1 3 | 2 1
    # 0.6 : 2 1 x
    # 0.5 : 2 3 1
    # 0.4 : 2 3 x
    # 0.2 : 2 x x | 2 x
    # 0.2 : 3 1 2
    # 0.1 : 3 2 1
    # 0.0 : x x x | x x | x 上記以外は0.0
    # まずはこれでやってみる
    epsilon = 0.001

    # for value in values:
    #     print(' {0:.2f}'.format(value), end='')
    # print()
    # for value in predicted_values:
    #     print(' {0:.2f}'.format(value), end='')
    # print()
    # print()
    count = len(values)
    if count == 1:
        if predicted_values[0] == values[0]:  # 1
            accuracy = 1.0
        else:
            # 一致しないことはありえない
            raise Exception("Error!")
    elif count == 2:
        if predicted_values[0] == values[0]:  # 1 -
            if predicted_values[1] == values[1]:  # 1 2
                accuracy = 1.0
            else:  # 1 x
                accuracy = 0.8
        elif predicted_values[0] == values[1]:
            if predicted_values[1] == values[0]:
                accuracy = 0.7
            else:
                accuracy = 0.4
        else:
            accuracy = 0.0
    else:
        if predicted_values[0] == values[0]:  # 1 - -
            if predicted_values[1] == values[2]:  # 1
                accuracy = 1.0
            elif predicted_values[2] == values[1]:
                accuracy = 0.9
            else:
                accuracy = 0.8
        elif predicted_values[0] == values[1]:  # 2 x x
            if predicted_values[1] == values[0]:
                accuracy = 0.7
            elif predicted_values[2] == values[2]:
                accuracy = 0.6
            else:
                accuracy = 0.4
        elif predicted_values[0] == values[2]:  # 3 x x
            if predicted_values[1] == values[0]:
                accuracy = 0.3
            elif predicted_values[2] == values[1]:
                accuracy = 0.2
            else:
                accuracy = 0.1
        else:
            accuracy = 0.0

    total_accuracy = 0.0
    move_accuracies[index] = 0.0


#
# 棋譜を評価する
#
def evaluating_records(move_record, eval_record):
    reversi.initialize()

    for index, actual_move in enumerate(converter.convert_moves(move_record)):
        print("index:{0} move:{1}".format(index + 1, actual_move))
        turn = reversi.get_current_turn()
        while True:
            moves = reversi.available_moves(turn)
            if len(moves) > 0:
                break
            print("Pass!")
            reversi.next_turn()

        values = []
        predicted_values = []
        evals = converter.convert_evals(eval_record[index])
        for entry in evals:
            coord = entry['coord']
            value = entry['value']
            state = converter.convert_state(reversi, coord)
            predicted_value = calculate_predicted_value(state)(state)
            values.append(value)
            predicted_values.append(predicted_value)
            # probs = np.r_[pprobs, nprobs]
            # view_probs = ["{0:.2f}".format(x) for x in probs]

        calculate_accuracies(index, values, predicted_values)

        coord = converter.move_to_coord(actual_move)
        reversi.make_move(coord)
        reversi.next_turn()


#
# 学習モデルを評価する
#
def evaluating_model(path, filenames):
    for i, filename in enumerate(filenames):
        file = os.path.join(path, filename)
        with open(file, "r") as fin:
            move_record = fin.readline().strip()
            eval_record = [x.strip() for x in fin.readlines()]

        evaluating_records(move_record, eval_record)

        # 全体の正答率
        if i % 10 == 0:
            print("{0}: {1}".format(i, file))
            print("total_accuracy: {0}".format(total_accuracy))

    # 全体の正答率
    print("total_accuracy: {0}".format(total_accuracy))
    # depth毎の正答率
    for i in range(len(move_accuracies)):
        print("move_accuracies[{0}]: {1}".format(i, move_accuracies[i]))


#
# メイン
#
def main(args):
    path = "../resources/records/"
    with zipfile.ZipFile(os.path.join(path, "kifu102245.zip")) as records_zip:

        namelist = records_zip.namelist()
        step = 1000
        for i, pos in enumerate(range(100000, 102000, step)):
            filenames = namelist[pos: pos + step]
            records_zip.extractall(path, filenames)

            evaluating_model(path, filenames)

            for file in glob.glob(os.path.join(path, "kifu*.txt")):
                os.remove(file)

    return 0


if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)
