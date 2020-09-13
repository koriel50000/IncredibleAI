# -*- coding: utf-8 -*-

import numpy as np
import statistics as stat

# 定数宣言
COLUMNS = 8
ROWS = 8
CHANNELS = 16

BOARD_EMPTY = 0
BOARD_BORDER = 3

AREA_EMPTY = 0
AREA_ODD = 1
AREA_EVEN = 2
AREA_UNKNOWN = 3
AREA_NOT_EMPTY = 4

REGION = ((8, 0, 0, 0, 2, 2, 2, 10),
          (1, 8, 0, 0, 2, 2, 10, 3),
          (1, 1, 8, 0, 2, 10, 3, 3),
          (1, 1, 1, 8, 10, 3, 3, 3),
          (5, 5, 5, 12, 14, 7, 7, 7),
          (5, 5, 12, 4, 6, 14, 7, 7),
          (5, 12, 4, 4, 6, 6, 14, 7),
          (12, 4, 4, 4, 6, 6, 6, 14))

# global変数宣言
region = 0
oddeven_area = [[0 for x_ in range(COLUMNS + 2)] for y_ in range(ROWS + 2)]
odd_count = 0
even_count = 0
empty_count = 0
early_turn = True
flipped_board = [[0 for x_ in range(COLUMNS + 2)] for y_ in range(ROWS + 2)]


def clear():
    global region, odd_count, even_count, empty_count, early_turn, flipped_board

    region = 0
    odd_count = 0
    even_count = 1
    empty_count = 60
    early_turn = True
    for y in range(1, ROWS + 1):
        for x in range(1, COLUMNS + 1):
            flipped_board[y][x] = 0
    flipped_board[4][4] = 1
    flipped_board[4][5] = 1
    flipped_board[5][4] = 1
    flipped_board[5][5] = 1


#
# 対角位置の対称変換が必要か
#
def is_symmetric(diagonal, board, color):
    for y in range(1, ROWS + 1):
        for x in range(y + 1, COLUMNS + 1):
            x_, y_ = x, y
            if diagonal == 8:
                x_, y_ = x, y  # 変換なし
            elif diagonal == 10:
                x_, y_ = 9 - x, y  # 左右反転
            elif diagonal == 12:
                x_, y_ = x, 9 - y  # 上下反転
            elif diagonal == 14:
                x_, y_ = 9 - x, 9 - y  # 上下左右反転
            # TODO 座標変換ではなくboard側を返還しなければならない

            if board[y_][x_] != board[x_][y_]:
                # FIXME
                return board[y_][x_] != color
    return False


#
# 領域を判定する
#
def check_region(board, coord, color):
    global region

    x, y = coord
    region = REGION[y - 1][x - 1]

    if region >= 8 and is_symmetric(region, board, color):
        region += 1


#
# 反転数を増分する
#
def increase_flipped(flipped, coord):
    global flipped_board

    for coord_ in flipped:
        x, y = coord_
        flipped_board[y][x] += 1
    x, y = coord
    flipped_board[y][x] += 1


#
# 再帰的にたどって領域を分割する
#
def partition_recursive(area, x, y, count):
    area[y][x] = AREA_UNKNOWN
    count += 1
    for direction in ((-1, 0), (0, -1), (1, 0), (0, 1)):
        dx, dy = direction
        x_ = x + dx
        y_ = y + dy
        if area[y_][x_] == AREA_EMPTY:
            count = partition_recursive(area, x_, y_, count)
    return count


#
# 領域を分割する
#
def partition(area, x, y):
    if area[y][x] == AREA_ODD or area[y][x] == AREA_EVEN:
        return -1  # すでに分類済み

    count = partition_recursive(area, x, y, 0)
    oddeven = AREA_ODD if count % 2 == 1 else AREA_EVEN

    for y_ in range(1, ROWS + 1):
        for x_ in range(1, COLUMNS + 1):
            if area[y_][x_] == AREA_UNKNOWN:
                area[y_][x_] = oddeven

    return oddeven


#
# 偶数領域・奇数領域を集計する
#
def enumerate_oddeven(board):
    global oddeven_area, odd_count, even_count, empty_count, early_turn

    early_turn = True
    for y in range(10):
        for x in range(10):
            if board[y][x] == BOARD_EMPTY:
                oddeven_area[y][x] = AREA_EMPTY
            elif board[y][x] == BOARD_BORDER:
                oddeven_area[y][x] = AREA_NOT_EMPTY
            else:
                oddeven_area[y][x] = AREA_NOT_EMPTY
                if x == 1 or x == 8 or y == 1 or y == 8:
                    early_turn = False

    odd_count = 0
    even_count = 0
    empty_count = 0
    for y in range(1, ROWS + 1):
        for x in range(1, COLUMNS + 1):
            if oddeven_area[y][x] != AREA_NOT_EMPTY:
                empty_count += 1
                oddeven = partition(oddeven_area, x, y)
                if oddeven == AREA_ODD:
                    odd_count += 1
                elif oddeven == AREA_EVEN:
                    even_count += 1


def put(state, x, y, channel):
    if region in (0, 8):
        x_, y_ = x - 1, y - 1  # 変換なし
    elif region in (2, 10):
        x_, y_ = 8 - x, y - 1  # 左右反転
    elif region in (4, 12):
        x_, y_ = x - 1, 8 - y  # 上下反転
    elif region in (6, 14):
        x_, y_ = 8 - x, 8 - y  # 上下左右反転
    elif region in (1, 9):
        x_, y_ = y - 1, x - 1  # 対称反転
    elif region in (3, 11):
        x_, y_ = y - 1, 8 - x  # 左右対称反転
    elif region in (5, 13):
        x_, y_ = 8 - y, x - 1  # 上下対称反転
    elif region in (7, 15):
        x_, y_ = 8 - y, 8 - x  # 上下左右対称反転
    else:
        raise Exception("Error!")

    state[channel, y_, x_] = 1


def fill(state, channel):
    for y in range(ROWS):
        for x in range(COLUMNS):
            state[channel, y, x] = 1


#
# 石を置いたときの状態を返す
#
def convert_state(board, flipped, coord, color, dtype):
    check_region(board, coord, color)
    enumerate_oddeven(board)

    state = np.zeros((CHANNELS, ROWS, COLUMNS), dtype=dtype)

    for y in range(1, ROWS + 1):
        for x in range(1, COLUMNS + 1):
            if board[y][x] == color:
                put(state, x, y, 0)  # 着手前に自石
            elif board[y][x] == opponent_turn(color):
                put(state, x, y, 1)  # 着手前に相手石
            else:
                put(state, x, y, 2)  # 着手前に空白
            if (x, y) == coord:
                put(state, x, y, 3)  # 着手
                put(state, x, y, 4)  # 変化した石
            elif (x, y) in flipped:
                put(state, x, y, 4)  # 変化した石
            if oddeven_area[y][x] == AREA_ODD:
                put(state, x, y, 5)  # 奇数領域
            elif oddeven_area[y][x] == AREA_EVEN:
                put(state, x, y, 6)  # 偶数領域
            reverse_count = flipped_board[y][x]
            if reverse_count > 0:
                reverse_count = reverse_count if reverse_count < 6 else 6  # 6以上は6プレーン目とする
                put(state, x, y, 9 + reverse_count)  # 反転数

    if not early_turn:
        fill(state, 7)  # 序盤でない
    if empty_count % 2 == 1:
        fill(state, 8)  # 空白数が奇数
    if odd_count == 1 or odd_count % 2 == 0:
        fill(state, 9)  # 奇数領域が1個または偶数

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


#
# 相手の手番を返す
# BLACK(1)->WHITE(2)、WHITE(2)->BLACK(1)
#
def opponent_turn(turn):
    return turn ^ 3


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
# 棋譜(着手)を着手リストに変換する
#
def convert_moves(move_record):
    moves = []
    for move in [move_record[i:i + 2] for i in range(0, len(move_record), 2)]:
        moves.append(move)

    return moves
