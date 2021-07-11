# -*- coding: utf-8 -*-

import sys
import random

import reversi
import oddeven_feature as feature
import cnn_model as model

random.seed()


def optimum_choice(evals):
    evals.sort(key=lambda x: x['value'], reverse=True)

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
def prelude_operator(coords):
    ndigits = 3  # 評価値は小数点第三位を四捨五入
    evals = []
    for coord in coords:
        state = feature.convert_state(reversi, coord)
        value = round(model.calculate_predicted_value(state), ndigits)
        evals.append({'coord': coord, 'value': value})

    return optimum_choice(evals)


def random_operator(coords):
    return random.choice(coords)


def manual_operator(coords):
    while True:
        move = input("move> ")
        coord = feature.move_to_coord(move.upper())
        if coord in coords:
            return coord


#
# ゲームを実行する
#
def play(black_operator, white_operator):
    reversi.clear()
    feature.clear()
    
    while True:
        reversi.print_board()
        reversi.print_status()

        coords = reversi.available_moves()
        if len(coords) == 0:
            print("Pass!")
            passed = True
        else:
            if reversi.current_color == reversi.BLACK:
                coord = black_operator(coords)
            else:
                coord = white_operator(coords)

            flipped = reversi.make_move(coord)
            feature.increase_flipped(coord, flipped)
            passed = False

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
    model.load_model("../resources/model/")
    # step = 2
    # epoch = 10
    # cnn_model.load_checkpoint("../resources/checkpoint/", step, epoch)

    # TODO 先手・後手を選択

    # play(prelude_operator, manual_operator)
    play(prelude_operator, random_operator)

    return 0


if __name__ == "__main__":
    exit_code = main(sys.argv)
    sys.exit(exit_code)
