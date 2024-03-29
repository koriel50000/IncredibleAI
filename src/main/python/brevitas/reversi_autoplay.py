# -*- coding: utf-8 -*-

import sys
import random

import torch

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
    states = []
    for coord in coords:
        state = feature.convert_state(reversi, coord)
        states.append(state)

    states_ = feature.convert_states(states)
    values = model.calculate_predicted_values(states_)

    evals = []
    for coord, value in zip(coords, values):
        ndigits = 3  # 評価値は小数点第三位を四捨五入
        value = round(value, ndigits)
        evals.append({'coord': coord, 'value': value})

    return optimum_choice(evals)


def random_operator(coords):
    return random.choice(coords)


#
# ゲームを実行する
#
def play(black_operator, white_operator):
    reversi.clear()
    feature.clear()

    while True:
        coords = reversi.available_moves()
        if len(coords) == 0:
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
    winner, black, white = reversi.print_score()

    return winner, black, white


#
# メイン
#
def main():
    network = 'CNN_2W2A'
    resume = '../../resources/brevitas/experiments/{}/checkpoints/checkpoint.tar'.format(network)
    model.load_model(resume)

    win = 0
    loss = 0
    draw = 0
    win_stone = 0
    loss_stone = 0
    for i in range(50):
        with torch.no_grad():
            winner, black, white = play(prelude_operator, random_operator)
        if winner == "black":
            win += 1
            win_stone += black
            loss_stone += white
        elif winner == "white":
            loss += 1
            win_stone += white
            loss_stone += black
        else:
            draw += 1
            win_stone += black
            loss_stone += white
        with torch.no_grad():
            winner, black, white = play(random_operator, prelude_operator)
        if winner == "black":
            loss += 1
            win_stone += black
            loss_stone += white
        elif winner == "white":
            win += 1
            win_stone += white
            loss_stone += black
        else:
            draw += 1
            win_stone += white
            loss_stone += black

    print("win:{0} loss:{1} draw:{2}".format(win, loss, draw))
    print("win_stone:{0} loss_stone:{1}".format(win_stone, loss_stone))


if __name__ == "__main__":
    main()
