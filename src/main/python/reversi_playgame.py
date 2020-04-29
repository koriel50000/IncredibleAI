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
def evaluate_moves(sess, coords):
    for coord in coords:
        move = reversi.coord2move(coord)
        state = converter.convert_state(reversi, move)
        predicted_value, probs = cnn_model.calculate_predicted_value(sess, state)
        #probs = np.r_[pprobs, nprobs]
        view_probs = ["{0:.2f}".format(x) for x in probs]
        print("move:{0} predicted:{1} {2}".format(move, predicted_value, view_probs))


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


def manual_feature(sess, coords):
    move = input("move> ")
    return converter.move_to_coord(move.upper())


#
# ゲームを実行する
#
def play(sess, black_feature, white_feature):
    reversi.initialize()
    
    while True:
        turn = reversi.get_current_turn()

        reversi.print_board()
        reversi.print_status()

        coords = reversi.available_moves(turn)
        if len(coords) > 0:
            if turn == reversi.BLACK:
                coord = black_feature(sess, coords)
            else:
                coord = white_feature(sess, coords)
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
    sess = tf.Session()
    
    saver = tf.train.Saver()
    saver.restore(sess, "../resources/checkpoint/model.ckpt-1")
    
    play(sess, prelude_feature, manual_feature)
    
    sess.close()
    
    return 0


if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)
