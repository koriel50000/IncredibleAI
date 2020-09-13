# -*- coding: utf-8 -*-

import sys
import glob
import os.path
import zipfile
import math

import reversi
import converter

from tensorflow import keras

model = keras.models.load_model("../resources/model/")

# global変数宣言
total_count = 0
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
def calculate_accuracies(index, evals, predicted_evals):
    global total_count, total_accuracy, move_accuracies

    # 正答率は評価値の上位3つで計算する
    # 1.0 : 1 2 3 | 1 2 | 1
    # 0.9 : 1 2 x
    # 0.8 : 1 3 2
    # 0.7 : 1 3 x
    # 0.5 : 1 x x
    # 0.7 : 2 1 3
    # 0.6 : 2 1 x | 2 1
    # 0.5 : 2 3 1
    # 0.4 : 2 3 x
    # 0.2 : 2 x x
    # 0.2 : 3 1 2
    # 0.1 : 3 2 1
    # 0.0 : x x x | x x | x 上記以外は0.0
    # まずはこれでやってみる

    evals.sort(key=lambda x: x['value'], reverse=True)
    predicted_evals.sort(key=lambda x: x['value'], reverse=True)

    # for entry in evals:
    #     move = converter.coord_to_move(entry['coord'])
    #     value = entry['value']
    #     print(' {0}:{1:.2f}'.format(move, value), end='')
    # print()
    # for entry in predicted_evals:
    #     move = converter.coord_to_move(entry['coord'])
    #     value = entry['value']
    #     print(' {0}:{1:.2f}'.format(move, value), end='')
    # print()
    # print()

    count = len(evals)
    if count == 1:
        accuracy = 1.0

    elif count == 2:
        first = evals[0]['coord']
        predicted_first = predicted_evals[0]['coord']
        if predicted_first == first:  # 1 2
            accuracy = 1.0
        else:  # 2 1
            accuracy = 0.6

    else:
        first = evals[0]['coord']
        predicted_first = predicted_evals[0]['coord']
        second = evals[1]['coord']
        predicted_second = predicted_evals[1]['coord']
        third = evals[2]['coord']
        predicted_third = predicted_evals[2]['coord']

        if predicted_first == first:  # 1 - -
            if predicted_second == second:  # 1 2 -
                if predicted_third == third:  # 1 2 3
                    accuracy = 1.0
                else:  # 1 2 x
                    accuracy = 0.9
            elif predicted_second == third:  # 1 3 -
                if predicted_third == second:  # 1 3 2
                    accuracy = 0.8
                else:  # 1 3 x
                    accuracy = 0.7
            else:  # 1 x x
                accuracy = 0.5
        elif predicted_first == second:  # 2 - -
            if predicted_second == first:  # 2 1 -
                if predicted_third == third:  # 2 1 3
                    accuracy = 0.7
                else:  # 2 1 x
                    accuracy = 0.6
            elif predicted_second == third:  # 2 3 -
                if predicted_third == first:  # 2 3 1
                    accuracy = 0.5
                else:  # 2 3 x
                    accuracy = 0.4
            else:  # 2 x x
                accuracy = 0.2
        elif predicted_first == third:  # 3 - -
            if predicted_second == first and predicted_third == second:  # 3 1 2
                accuracy = 0.2
            elif predicted_second == second and predicted_third == first:  # 3 2 1
                accuracy = 0.1
            else:  # 上記以外は0.0
                accuracy = 0.0
        else:  # 上記以外は0.0
            accuracy = 0.0

    total_count += 1
    total_accuracy += accuracy
    move_accuracies[index] += accuracy


#
# 棋譜を評価する
#
def evaluating_records(move_record, eval_records):
    reversi.clear()

    for index, actual_move in enumerate(converter.convert_moves(move_record)):
        # print("index:{0} move:{1}".format(index, actual_move))
        while True:
            if len(reversi.available_moves()) > 0:
                break
            print("Pass!")
            if reversi.has_completed(True):
                raise Exception("Error!")  # 先手・後手両方パスで終了は考慮しない
            reversi.next_turn(True)

        evals = converter.convert_evals(eval_records[index])

        ndigits = 3  # 評価値は小数点第三位を四捨五入
        predicted_evals = []
        for entry in evals:
            coord = entry['coord']
            value = entry['value']
            entry['value'] = round(value, ndigits)
            state = reversi.convert_state(coord)
            predicted_value = round(calculate_predicted_value(state), ndigits)
            predicted_evals.append({'coord': coord, 'value': predicted_value})

        calculate_accuracies(index, evals, predicted_evals)

        coord = converter.move_to_coord(actual_move)
        reversi.make_move(coord)
        reversi.next_turn(False)


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
        print("total_accuracy: {0}".format(total_accuracy / total_count))

    # depth毎の正答率
    size = len(move_accuracies)
    for i in range(size):
        print("move_accuracies[{0}]: {1}".format(i, move_accuracies[i] * size / total_count))


#
# メイン
#
def main(args):
    path = "../resources/records/"
    with zipfile.ZipFile(os.path.join(path, "kifu102245.zip")) as records_zip:

        namelist = records_zip.namelist()

        filenames = namelist[100000: 100100]
        records_zip.extractall(path, filenames)

        evaluating_model(path, filenames)

        for file in glob.glob(os.path.join(path, "kifu*.txt")):
            os.remove(file)

    return 0


if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)
