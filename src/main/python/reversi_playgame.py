# -*- coding: utf-8 -*-

import sys
import random
import numpy as np
from tensorflow import keras

import converter
import reversi

random.seed()

model = keras.models.load_model("../resources/model/")


#
# 予測値を計算する
#
def calculate_predicted_value(state):
    state_ = state.reshape(1, -1)
    predictions_ = model.predict(state_)
    return predictions_[0][0]


def optimum_choice(evals):
    evals.sort(key=lambda x: x['value'], reverse=True)
    print(evals)

    coords = []
    maximum_value = float("-inf")
    for entry in evals:
        coord = entry['coord']
        value = entry['value']
        if value < maximum_value:
            break
        coords.append(coord)
        maximum_value = value

    return random.choice(coords)


#
# 差し手を評価する
#
def prelude_feature(coords):
    ndigits = 3  # 評価値は小数点第三位を四捨五入
    evals = []
    for coord in coords:
        state = reversi.convert_state(coord, dtype=np.float32)
        value = round(calculate_predicted_value(state), ndigits)
        evals.append({'coord': coord, 'value': value})

    return optimum_choice(evals)


def random_feature(coords):
    return random.choice(coords)


def manual_feature(coords):
    while True:
        move = input("move> ")
        coord = converter.move_to_coord(move.upper())
        if coord in coords:
            return coord


#
# ゲームを実行する
#
def play(black_feature, white_feature):
    reversi.clear()
    
    while True:
        reversi.print_board()
        reversi.print_status()

        passed = False
        if reversi.current_color == reversi.BLACK:
            coords = reversi.available_moves()
            if len(coords) > 0:
                coord = black_feature(coords)
                reversi.make_move(coord)
            else:
                print("Pass!")
                passed = True
        else:
            coords = reversi.available_moves()
            if len(coords) > 0:
                coord = white_feature(coords)
                reversi.make_move(coord)
            else:
                print("Pass!")
                passed = True

        # ゲーム終了を判定
        if reversi.has_completed(passed):
            break
        reversi.next_turn(passed)

    reversi.print_board()
    reversi.print_score()


#
# メイン
#
def main(args):
    # TODO 先手・後手を選択

    # play(prelude_feature, manual_feature)
    play(prelude_feature, random_feature)

    return 0


if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)
