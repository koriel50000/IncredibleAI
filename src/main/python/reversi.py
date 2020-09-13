# -*- coding: utf-8 -*-

import numpy as np

import converter

# 定数宣言
COLUMNS = 8
ROWS = 8
CHANNELS = 16

EMPTY, BLACK, WHITE, BORDER = (0, 1, 2, 3)
STONES = (".", "@", "O")
DIRECTIONS = ((0, -1), (1, -1), (1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0), (-1, -1))

# global変数宣言
board = [[0 for x_ in range(COLUMNS + 2)] for y_ in range(ROWS + 2)]
number_of_stones = [0, 0, 0]
current_color = 0
turn_count = 0
passedBefore = False


#
# 盤面を初期化する
#
def clear():
    global board, number_of_stones, current_color, turn_count, passedBefore

    for y in range(ROWS + 2):
        for x in range(COLUMNS + 2):
            if 1 <= x <= COLUMNS and 1 <= y <= ROWS:
                board[y][x] = EMPTY
            else:
                board[y][x] = BORDER
    board[4][4] = WHITE
    board[4][5] = BLACK
    board[5][4] = BLACK
    board[5][5] = WHITE
    number_of_stones[EMPTY] = 60
    number_of_stones[BLACK] = 2
    number_of_stones[WHITE] = 2
    current_color = BLACK
    turn_count = 1
    passedBefore = False
    converter.clear()


#
# 相手の手番を返す
# BLACK(1)->WHITE(2)、WHITE(2)->BLACK(1)
#
def opponent_turn(turn):
    return turn ^ 3


#
# 方向を指定して石が打てるかを判定する
#
def can_move_direction(coord, direction):
    x, y = coord
    dx, dy = direction
    x += dx
    y += dy
    while board[y][x] == opponent_turn(current_color):  # 相手石ならば継続
        x += dx
        y += dy
        if board[y][x] == current_color:  # 自石ならば終了
            return True
    return False


#
# 指定した位置に石が打てるかを判定する
#
def can_move(coord):
    x, y = coord
    if board[y][x] == EMPTY:
        for direction in DIRECTIONS:
            if can_move_direction(coord, direction):
                return True
    return False


#
# 反転する石の位置を返す
#
def compute_flipped(coord):
    flipped = []
    x, y = coord
    for direction in DIRECTIONS:
        if not can_move_direction(coord, direction):
            continue
        dx, dy = direction
        x += dx
        y += dy
        while board[y][x] == opponent_turn(current_color):  # 相手石ならば継続
            flipped.append((x, y))  # 反転位置を追加
            x += dx
            y += dy

    return flipped


#
# 着手可能なリストを返す
#
def available_moves():
    coords = []
    for y in range(1, ROWS + 1):
        for x in range(1, COLUMNS + 1):
            coord = (x, y)
            if can_move(coord):
                coords.append(coord)
    return coords


#
# 指定した着手から盤面の特徴量に変換する
#
def convert_state(coord, dtype=np.float32):
    flipped = compute_flipped(coord)
    state = converter.convert_state(board, flipped, coord, current_color, dtype)
    return state


#
# 指定された場所に石を打つ
#
def make_move(coord):
    global board, number_of_stones

    flipped = compute_flipped(coord)
    converter.increase_flipped(flipped, coord)

    x, y = coord
    board[y][x] = current_color  # 石を打つ
    number_of_stones[current_color] += 1  # 自石を増やす
    number_of_stones[EMPTY] -= 1  # 空白を減らす

    for coord_ in flipped:
        x, y = coord_
        board[y][x] = current_color  # 石を反転
        number_of_stones[current_color] += 1  # 自石を増やす
        number_of_stones[opponent_turn(current_color)] -= 1  # 相手石を減らす


#
# ゲームの終了を判定する
#
def has_completed(passed):
    # 空白がなくなったら終了
    # 先手・後手両方パスで終了
    # 先手・後手どちらかが完勝したら終了
    if number_of_stones[EMPTY] == 0 or (passed and passedBefore) \
            or number_of_stones[WHITE] == 0 or number_of_stones[BLACK] == 0:
        return True

    return False  # 上記以外はゲーム続行


#
# 手番を進める
#
def next_turn(passed):
    global current_color, turn_count, passedBefore

    current_color = opponent_turn(current_color)  # 手番を変更
    turn_count += 1
    passedBefore = passed


#
# 盤面を表示する
#
def print_board():
    print()
    print("  A B C D E F G H")
    for y in range(1, ROWS + 1):
        print(y, end='')
        for x in range(1, COLUMNS + 1):
            print(" " + STONES[board[y][x]], end='')
        print()


#
# 現在の状態を表示する
#
def print_status():
    print("move count:{0}".format(turn_count), end='')
    print(" move:{0}({1})".format("black" if current_color == BLACK else "white", STONES[current_color]))
    print("black:{0} white:{1}".format(number_of_stones[BLACK], number_of_stones[WHITE]))
    print()


#
# スコアを表示する
#
def print_score():
    print("move count:{0}".format(turn_count), end='')
    if number_of_stones[BLACK] > number_of_stones[WHITE]:
        winner = "black"
        number_of_stones[BLACK] += number_of_stones[EMPTY]
    elif number_of_stones[BLACK] < number_of_stones[WHITE]:
        winner = "white"
        number_of_stones[WHITE] += number_of_stones[EMPTY]
    else:
        winner = "draw"
    print(" winner:{0}".format(winner))
    print("black:{0} white:{1}".format(number_of_stones[BLACK], number_of_stones[WHITE]))
    print()

    return winner, number_of_stones[BLACK], number_of_stones[WHITE]
