# -*- coding: utf-8 -*-

import numpy as np
import statistics as stat

import reversi

# 定数宣言
COLUMNS = 8
ROWS = 8
CHANNEL = 16

AREA_EMPTY = 0
AREA_ODD = 1
AREA_EVEN = 2
AREA_UNKNOWN = 3
AREA_NOT_EMPTY = 4

REGION = ((8, 0, 0, 0, 1, 1, 1, 10),
          (4, 8, 0, 0, 1, 1, 10, 5),
          (4, 4, 8, 0, 1, 10, 5, 5),
          (4, 4, 4, 8, 10, 5, 5, 5),
          (6, 6, 6, 12, 14, 7, 7, 7),
          (6, 6, 12, 2, 3, 14, 7, 7),
          (6, 12, 2, 2, 3, 3, 14, 7),
          (12, 2, 2, 2, 3, 3, 3, 14))


#
# 着手を位置に変換する
#
def move_to_coord(move):
    x = ord(move[:1]) - ord("A") + 1
    y = int(move[1:])
    return x, y


#
# 位置を着手に変換する
#
# def coord_to_move(coord):
#     x, y = coord
#     return "{0}{1}".format(chr(ord("A") + x - 1), y)


#
# 棋譜(着手)を着手リストに変換する
#
def convert_moves(move_record):
    moves = []

    for move in [move_record[i:i + 2] for i in range(0, len(move_record), 2)]:
        moves.append(move)

    return moves


#
# 対角位置の対称変換が必要か
#
def is_symmetric(diagonal, board, turn):
    for y in range(1, 9):
        for x in range(y + 1, 9):
            x_, y_ = x, y
            if diagonal == 8:
                x_, y_ = x, y  # 変換なし
            elif diagonal == 10:
                x_, y_ = 9 - x, y  # 左右反転
            elif diagonal == 12:
                x_, y_ = x, 9 - y  # 上下反転
            elif diagonal == 14:
                x_, y_ = 9 - x, 9 - y  # 上下左右反転

            if board[y_][x_] != board[x_][y_]:
                return board[y_][x_] != turn
    return False


#
# 領域を判定する
#
def check_region(board, coord, turn):
    x, y = coord
    region = REGION[y - 1][x - 1]

    if region >= 8 and is_symmetric(region, board, turn):
        region += 1

    return region


def calculate_oddeven_recursive(oddeven_area, x, y, count):
    oddeven_area[y][x] = AREA_UNKNOWN
    count += 1
    for dir in ((-1, 0), (0, -1), (1, 0), (0, 1)):
        dx, dy = dir
        x_ = x + dx
        y_ = y + dy
        if oddeven_area[y_][x_] == AREA_EMPTY:
            count = calculate_oddeven_recursive(oddeven_area, x_, y_, count)
    return count


#
# 空白領域が偶数か奇数かを分類する
#
def calculate_oddeven(oddeven_area, x, y):
    if oddeven_area[y][x] == AREA_ODD or oddeven_area[y][x] == AREA_EVEN:
        return -1  # すでに分類済み

    count = calculate_oddeven_recursive(oddeven_area, x, y, 0)
    oddeven = AREA_ODD if count % 2 == 1 else AREA_EVEN

    for y_ in range(1, 9):
        for x_ in range(1, 9):
            if oddeven_area[y_][x_] == AREA_UNKNOWN:
                oddeven_area[y_][x_] = oddeven

    return oddeven


#
# 偶数/奇数/空白領域を列挙する
#
def enumerate_area(board):
    early_stage = True
    empty_count = 0
    odd_count = 0
    even_count = 0
    oddeven_area = [[0 for x_ in range(10)] for y_ in range(10)]
    for y in range(10):
        for x in range(10):
            if board[y][x] == reversi.EMPTY:
                oddeven_area[y][x] = AREA_EMPTY
            elif board[y][x] == reversi.BORDER:
                oddeven_area[y][x] = AREA_NOT_EMPTY
            else:
                oddeven_area[y][x] = AREA_NOT_EMPTY
                if x == 1 or x == 8 or y == 1 or y == 8:
                    early_stage = False

    for y in range(1, 9):
        for x in range(1, 9):
            if oddeven_area[y][x] != AREA_NOT_EMPTY:
                empty_count += 1
                oddeven = calculate_oddeven(oddeven_area, x, y)
                if oddeven == AREA_ODD:
                    odd_count += 1
                elif oddeven == AREA_EVEN:
                    even_count += 1

    return oddeven_area, early_stage, empty_count, odd_count, even_count


#
# 着手を描画する
#
def put_state(state, region, x, y, channel):
    if region in (0, 8):
        x_, y_ = x - 1, y - 1  # 変換なし
    elif region in (1, 10):
        x_, y_ = 8 - x, y - 1  # 左右反転
    elif region in (2, 12):
        x_, y_ = x - 1, 8 - y  # 上下反転
    elif region in (3, 14):
        x_, y_ = 8 - x, 8 - y  # 上下左右反転
    elif region in (4, 9):
        x_, y_ = y - 1, x - 1  # 対称反転
    elif region in (5, 11):
        x_, y_ = y - 1, 8 - x  # 左右対称反転
    elif region in (6, 13):
        x_, y_ = 8 - y, x - 1  # 上下対称反転
    elif region in (7, 15):
        x_, y_ = 8 - y, 8 - x  # 上下左右対称反転

    state[channel, y_, x_] = 1


#
# 石を置いたときの状態を返す
#
def convert_state(reversi, coord, dtype=np.uint8):
    board, next_board, reverse, next_reverse, turn = reversi.make_move(coord)

    region = check_region(next_board, coord, turn)

    oddeven_area, early_stage, empty_count, odd_count, even_count = enumerate_area(board)

    state = np.zeros((CHANNEL, ROWS, COLUMNS), dtype=dtype)

    x_, y_ = coord
    put_state(state, region, x_, y_, 3)  # 着手

    for y in range(1, 9):
        for x in range(1, 9):
            if board[y][x] == turn:
                put_state(state, region, x, y, 0)  # 着手前に自石
            elif board[y][x] == reversi.opponent_turn(turn):
                put_state(state, region, x, y, 1)  # 着手前に相手石
            else:
                put_state(state, region, x, y, 2)  # 着手前に空白
            if board[y][x] != next_board[y][x]:
                put_state(state, region, x, y, 4)  # 変化した石
            if oddeven_area[y][x] == AREA_ODD:
                put_state(state, region, x, y, 5)  # 奇数領域
            elif oddeven_area[y][x] == AREA_EVEN:
                put_state(state, region, x, y, 6)  # 偶数領域
            if not early_stage:
                put_state(state, region, x, y, 7)  # 序盤でない
            if empty_count % 2 == 1:
                put_state(state, region, x, y, 8)  # 空白数が奇数
            if odd_count == 1 or odd_count % 2 == 0:
                put_state(state, region, x, y, 9)  # 奇数領域が1個または偶数
            reverse_count = reverse[y][x] if reverse[y][x] < 6 else 6  # 6以上は6プレーン目とする
            if reverse_count > 0:
                put_state(state, region, x, y, 9 + reverse_count)  # 反転数

    reversi.undo_board()

    return state


#
# 棋譜(評価値)を評価リストに変換する
#
def convert_evals(eval_record):
    evals = []
    values = []
    fields = [x.strip() for x in eval_record.split(",")]

    for sets in [fields[i:i + 3] for i in range(0, len(fields), 3)]:
        coord = move_to_coord(sets[0])
        if sets[1] == "Win":
            value = 1.0
        elif sets[1] == "Draw":
            value = 0.0
        elif sets[1] == "Loss":
            value = -1.0
        else:
            if sets[2] == "----":
                value = float(sets[1])
            else:
                value = float(sets[2])
            values.append(value)
        evals.append({'coord': coord, 'value': value})

    if len(values) > 0:
        # 評価値を標準偏差で補正してtanh関数で-1.0～0.0～1.0範囲に変換
        stdev = stat.stdev(values) if len(values) >= 2 else 0.0
        for entry in evals:
            value = entry['value']
            value = value - stat.mean(values)
            value = value / stdev if stdev > 0 else value
            value = np.tanh(value)
            entry['value'] = value

    return evals
