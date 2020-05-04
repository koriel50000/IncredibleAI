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
    evals = []
    for coord in coords:
        state = converter.convert_state(reversi, coord, dtype=np.float32)
        value = calculate_predicted_value(state)
        evals.append({'coord': coord, 'value': value})

    return optimum_choice(evals)


def random_feature(coords):
    return random.choice(coords)


def manual_feature(coords):
    move = input("move> ")
    return converter.move_to_coord(move.upper())


#
# ゲームを実行する
#
def play(black_feature, white_feature):
    reversi.initialize()
    
    while True:
        turn = reversi.get_current_turn()

        reversi.print_board()
        reversi.print_status()

        coords = reversi.available_moves(turn)
        if len(coords) > 0:
            if turn == reversi.BLACK:
                coord = black_feature(coords)
            else:
                coord = white_feature(coords)
            reversi.make_move(coord)
        else:
            print("Pass!")
        
        # ゲーム終了を判定
        if reversi.has_completed():
            break
        reversi.next_turn()

    reversi.print_board()
    reversi.print_score()


#
# メイン
#
def main(args):
    # TODO 先手・後手を選択

    play(prelude_feature, manual_feature)

    return 0


if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)
