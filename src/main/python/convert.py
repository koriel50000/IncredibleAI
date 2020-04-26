# -*- coding: utf-8 -*-

import math
import numpy as np
import statistics as stat

# 定数宣言
ROWS = 8
COLUMNS = 8
CHANNEL = 16

AREA = (( 8, 0, 0, 0, 1, 1, 1,10),
        ( 4, 8, 0, 0, 1, 1,10, 5),
        ( 4, 4, 8, 0, 1,10, 5, 5),
        ( 4, 4, 4, 8,10, 5, 5, 5),
        ( 6, 6, 6,12,14, 7, 7, 7),
        ( 6, 6,12, 2, 3,14, 7, 7),
        ( 6,12, 2, 2, 3, 3,14, 7),
        (12, 2, 2, 2, 3, 3, 3,14))

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
def coord_to_move(coord):
    x, y = coord
    return "{0}{1}".format(chr(ord("A") + x - 1), y)

#
# 対角位置を対称変換する
#
def symmetric_coord(diagonal, x, y):
    if diagonal == 8:
        return x, y         # 変換なし
    elif diagonal == 10:
        return 9 - x, y     # 左右反転
    elif diagonal == 12:
        return x, 9 - y     # 上下反転
    elif diagonal == 14:
        return 9 - x, 9 - y # 上下左右反転

#
# 対角位置の対称変換が必要か
#
def is_symmetric(diagonal, board, turn):
    for y_ in range(1, 9):
        for x_ in range(y_ + 1, 9):
            x, y = symmetric_coord(diagonal, x_, y_)
            if board[y][x] != board[x][y]:
                return board[y][x] != turn
    return False

#
# 領域を判定する
#
def calculate_area(board, coord, turn):
    x, y = coord
    area = AREA[y-1][x-1]
    
    if area >= 8 and is_symmetric(area, board, turn):
        area += 1
    
    return area

#
# 着手の領域から座標を変換する
#
def transform_coord(area, x, y):
    if area in (0, 8):
        x, y = x - 1, y - 1 # 変換なし
    elif area in (1, 10):
        x, y = 8 - x, y - 1 # 左右反転
    elif area in (2, 12):
        x, y = x - 1, 8 - y # 上下反転
    elif area in (3, 14):
        x, y = 8 - x, 8 - y # 上下左右反転
    elif area in (4, 9):
        x, y = y - 1, x - 1 # 対称反転
    elif area in (5, 11):
        x, y = y - 1, 8 - x # 左右対称反転
    elif area in (6, 13):
        x, y = 8 - y, x - 1 # 上下対称反転
    elif area in (7, 15):
        x, y = 8 - y, 8 - x # 上下左右対称反転
    
    return x, y

#
# 着手を描画する
#
def point_to_state(state, area, x, y, channel):
    x_, y_ = transform_coord(area, x, y)
    state[channel, y_, x_] = 1

#
# 棋譜(着手)を着手リストに変換する
#
def convert_moves(move_record):
    moves = []
    
    for move in [move_record[i:i+2] for i in range(0, len(move_record), 2)]:
        moves.append(move)
    
    return moves

#
# 棋譜(評価値)を評価リストに変換する
#
def convert_evals(eval_record):
    evals = []
    values = []
    fields = [x.strip() for x in eval_record.split(",")]
    
    for sets in [fields[i:i+3] for i in range(0, len(fields), 3)]:
        move = sets[0]        
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
        evals.append({'move': move, 'value': value})
    
    if len(values) > 0:
        # 評価値を標準偏差で補正してtanh関数で-1.0～0.0～1.0範囲に変換
        stdev = stat.stdev(values) if len(values) >= 2 else 0.0
        for entry in evals:
            value = entry['value']
            value = value - stat.mean(values)
            value = value / stdev if stdev > 0 else value
            value = np.tanh(value)
            entry['value'] = value;

    return evals

def divide_area_recursive(oddeven_board, x, y, number_of_empty):
    oddeven_board[y][x] = 3
    number_of_empty += 1
    for dir in ((-1, 0), (0, -1), (1, 0), (1, 1)):
        dx, dy = dir
        x_ = x + dx
        y_ = y + dy
        if oddeven_board[y_][x_] == 0:
            number_of_empty = divide_area_recursive(oddeven_board, x_, y_, number_of_empty)
    return number_of_empty

#
# 空白領域を偶数領域と奇数領域に分割する
#
def divide_area(oddeven_board, x, y):
    if oddeven_board[y][x] == 1 or oddeven_board[y][x] == 2:
        return 0 # すでに分割済み
    
    number_of_empty = divide_area_recursive(oddeven_board, x, y, 0)
    oddeven = 1 if number_of_empty % 2 == 1 else 2
    for y in range(1, 9):
        for x in range(1, 9):
            if oddeven_board[y][x] == 3:
                oddeven_board[y][x] == oddeven
    return oddeven

#
# 石を置いたときの状態を返す
#
def convert_state(reversi, move):
    coord = move_to_coord(move)
    x_, y_ = coord
    board, next_board, reverse, next_reverse, turn = reversi.make_a_move(coord)
    
    area = calculate_area(next_board, coord, turn)
    
    state = np.zeros((CHANNEL, COLUMNS, ROWS), dtype=np.uint8)
    
    early_stage = True
    oddeven_board = [[0 for x_ in range(10)] for y_ in range(10)]
    for y in range(10):
        for x in range(10):
            if board[y][x] == reversi.BORDER:
                oddeven_board[y][x] = 4
            elif board[y][x] != reversi.EMPTY:
                oddeven_board[y][x] = 4
                if (x == 1 or x == 8 or y == 1 or y == 8):
                    early_stage = False
    
    number_of_empty = 0
    number_of_odd = 0
    number_of_even = 0
    for y in range(1, 9):
        for x in range(1, 9):
            if board[y][x] == reversi.EMPTY:
                number_of_empty += 1
                oddeven = divide_area(oddeven_board, x, y)
                if oddeven == 1:
                    number_of_odd += 1
                elif oddeven == 2:
                    number_of_even += 1
    
    for y in range(1, 9):
        for x in range(1, 9):
            if board[y][x] == turn:
                point_to_state(state, area, x, y, 0) # 着手前に自石
            elif board[y][x] == reversi.opponent_turn(turn):
                point_to_state(state, area, x, y, 1) # 着手前に相手石
            else:
                point_to_state(state, area, x, y, 2) # 着手前に空白
            if board[y][x] != next_board[y][x]:
                point_to_state(state, area, x, y, 4) # 変化した石
            if oddeven_board[y][x] == 1:
                point_to_state(state, area, x, y, 5) # 奇数領域
            elif oddeven_board[y][x] == 2:
                point_to_state(state, area, x, y, 6) # 偶数領域
            if not early_stage:
                point_to_state(state, area, x, y, 7) # 序盤でない
            if number_of_empty % 2 == 1:
                point_to_state(state, area, x, y, 8) # 空白数が奇数
            if number_of_odd == 1 or number_of_odd % 2 == 0:
                point_to_state(state, area, x, y, 9) # 奇数領域が1個または偶数
            reverse_count = reverse[y][x] if reverse[y][x] < 6 else 6; # 6以上は6プレーン目とする
            if (reverse_count > 0):
                point_to_state(state, area, x, y, 9 + reverse_count) # 反転数
    
    x_, y_ = coord
    point_to_state(state, area, x_, y_, 3) # 着手
    
    reversi.undo_board()
    
    return state

