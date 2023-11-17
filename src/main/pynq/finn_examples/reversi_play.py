# -*- coding: utf-8 -*-

import random
import requests
import time

import reversi
import oddeven_feature as feature
import cnn_model as model


RECORD_ENDPOINT = 'http://192.168.0.18:8080/othello/api/record'
GET_API_INTERVAL = 0.5
POST_API_INTERVAL = 0.5

random.seed()


def post_record(color, coord):
    url = RECORD_ENDPOINT + '/' + str(color) + '/'
    payload = { 'x': coord[0] - 1, 'y': coord[1] -1 }
    success = False
    print('post_record:', url, payload)

    while not success:
        req = requests.post(url, params=payload)
        result = req.text
        print('post_record:', result)
        if not result or result == '400':
            time.sleep(POST_API_INTERVAL)
        elif result == '500':
            raise RuntimeError(req.raw)
        else:
            success = True

    return coord


def get_record(color):
    url = RECORD_ENDPOINT + '/' + str(color) + '/'
    success = False
    print('get_record:', url)

    while not success:
        req = requests.get(url)
        result = req.text
        print('get_record:', result)
        if not result or result == '401' or result == '402':
            time.sleep(GET_API_INTERVAL)
        elif result == '500':
            raise RuntimeError(req.raw)
        else:
            success = True
            coord = result.split(',')[1:3]
            coord = tuple([int(x) + 1 for x in coord])

    return coord


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

    print(coords)
    return random.choice(coords)


#
# 差し手を評価する
#
def prelude_operator(color, coords, passed):
    if passed:
        coord = (-2, -2) # originの違いを補正
        post_record(color, coord)
        return

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

    coord = optimum_choice(evals)
    return post_record(color, coord)


def external_operator(color, coords, passed):
    return get_record(color)


#
# ゲームを実行する
#
def play(black_operator, white_operator):
    reversi.clear()
    feature.clear()

    while True:
        reversi.print_board()
        coords = reversi.available_moves()
        passed = True if len(coords) == 0 else False

        if reversi.current_color == reversi.BLACK:
            coord = black_operator(1, coords, passed)
        else:
            coord = white_operator(2, coords, passed)

        if not passed:
            flipped = reversi.make_move(coord)
            feature.increase_flipped(coord, flipped)

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
    win = 0
    loss = 0
    draw = 0
    win_stone = 0
    loss_stone = 0

    winner, black, white = play(prelude_operator, external_operator)
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

    print("win:{0} loss:{1} draw:{2}".format(win, loss, draw))
    print("win_stone:{0} loss_stone:{1}".format(win_stone, loss_stone))


if __name__ == "__main__":
    main()
