# -*- coding: utf-8 -*-

import sys
import random
import tensorflow as tf

import converter
import reversi
import cnn_model

random.seed()
tf.disable_v2_behavior()


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
def prelude_feature(sess, coords):
    evals = []
    for coord in coords:
        state = converter.convert_state(reversi, coord)
        value = cnn_model.calculate_predicted_value(sess, state)
        evals.append({'coord': coord, 'value': value})

    return optimum_choice(evals)


def random_feature(sess, coords):
    return random.choice(coords)


#
# ゲームを実行する
#
def play(sess, black_feature, white_feature):
    reversi.initialize()

    while True:
        turn = reversi.get_current_turn()

        coords = reversi.available_moves(turn)
        if len(coords) > 0:
            if turn == reversi.BLACK:
                coord = black_feature(sess, coords)
            else:
                coord = white_feature(sess, coords)
            reversi.make_move(coord)

        # ゲーム終了を判定
        if reversi.has_completed():
            break
        reversi.next_turn()

    reversi.print_board()
    winner, black, white = reversi.print_score()

    return winner, black, white


#
# メイン
#
def main(args):
    sess = tf.Session()

    saver = tf.train.Saver()
    saver.restore(sess, "../resources/checkpoint/model.ckpt-1")

    win = 0
    loss = 0
    draw = 0
    for i in range(50):
        winner, black, white = play(sess, prelude_feature, random_feature)
        if winner == "black":
            win += 1
        elif winner == "white":
            loss += 1
        else:
            draw += 1
        winner, black, white = play(sess, random_feature, prelude_feature)
        if winner == "black":
            loss += 1
        elif winner == "white":
            win += 1
        else:
            draw += 1

    print("win:{0} loss:{1} draw:{2}".format(win, loss, draw))

    sess.close()

    return 0


if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)
