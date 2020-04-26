# -*- coding: utf-8 -*-

import copy

# 定数宣言
EMPTY, BLACK, WHITE, BORDER = (0, 1, 2, 3)
STONES = (".", "@", "O")
DIRECTIONS = ((0, -1), (1, -1), (1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0), (-1, -1))

# global変数宣言
board = [[0 for x_ in range(10)] for y_ in range(10)]
temp_board = [[0 for x_ in range(10)] for y_ in range(10)]
reverse = [[0 for x_ in range(10)] for y_ in range(10)]
temp_reverse = [[0 for x_ in range(10)] for y_ in range(10)]
number_of_stones = [0, 0, 0]
temp_number_of_stones = [0, 0, 0]
current_turn = 0
turn_count = 0

#
# 盤面を初期化する
#
def init():
    global board, reverse, number_of_stones, current_turn, turn_count
    
    for y in range(10):
        for x in range(10):
            if (1 <= x and x <= 8) and (1 <= y and y <= 8):
                board[y][x] = EMPTY
            else:
                board[y][x] = BORDER
    board[4][4] = WHITE
    board[4][5] = BLACK
    board[5][4] = BLACK
    board[5][5] = WHITE
    reverse[4][4] = 1
    reverse[4][5] = 1
    reverse[5][4] = 1
    reverse[5][5] = 1
    number_of_stones[EMPTY] = 60
    number_of_stones[BLACK] = 2
    number_of_stones[WHITE] = 2
    current_turn = BLACK
    turn_count = 1

#
# 相手の手番を返す
# BLACK(1)->WHITE(2)、WHITE(2)->BLACK(1)
#
def opponent_turn(turn):
    return turn ^ 3

#
# 方向を指定して石が打てるかを判定する
#
def can_move_direction(turn, coord, dir):
    x, y = coord
    dx, dy = dir
    x += dx
    y += dy
    while board[y][x] == opponent_turn(turn): # 相手石ならば継続
        x += dx
        y += dy
        if board[y][x] == turn: #自石ならば終了
            return True
    return False

#
# 指定した位置に石が打てるかを判定する
#
def can_move(turn, coord):
    x, y = coord
    if board[y][x] == EMPTY:
        for dir in DIRECTIONS:
            if can_move_direction(turn, coord, dir):
                return True
    return False

#
# 着手可能なリストを返す
#
def available_moves(turn):
    moves = []
    for y in range(1, 8+1):
        for x in range(1, 8+1):
            coord = (x, y)
            if can_move(turn, coord):
                moves.append(coord_to_move(coord))
    return moves

#
# 指定された場所に石を打つ
#
def make_a_move(coord):
    global board, temp_board, reverse, temp_reverse, number_of_stones, temp_number_of_stones
    
    temp_board = copy.deepcopy(board)
    temp_reverse = copy.deepcopy(reverse)
    temp_number_of_stones = copy.deepcopy(number_of_stones)
    
    turn = current_turn
    x, y = coord
    board[y][x] = turn
    reverse[y][x] += 1 # 反転数+1
    number_of_stones[turn] += 1 # 自石を増やす
    number_of_stones[EMPTY] -= 1 # 空白を減らす
    for dir in DIRECTIONS:
        if not can_move_direction(turn, coord,  dir):
            continue
        x, y = coord
        dx, dy = dir
        x += dx
        y += dy
        while board[y][x] == opponent_turn(turn): # 相手石ならば継続
            board[y][x] = turn # 石を反転
            reverse[y][x] += 1 # 反転数+1
            number_of_stones[turn] += 1 # 自石を増やす
            number_of_stones[opponent_turn(turn)] -= 1 # 相手石を減らす
            x += dx
            y += dy
    
    return temp_board, board, temp_reverse, reverse, turn

#
# ゲームの終了を判定する
#
def has_completed():
    if number_of_stones[EMPTY] == 0: # 空白がなくなったら終了
        return True
    elif number_of_stones[BLACK] == 0 or number_of_stones[WHITE] == 0: # 先手・後手どちらかが完勝したら終了
        return True
    elif len(available_moves(BLACK)) == 0 and len(available_moves(WHITE)) == 0: # 先手・後手両方パスで終了
        return True
    else:
        return False # 上記以外はゲーム続行

#
# 手番を進める
#
def next_turn():
    global current_turn, turn_count
    
    current_turn = opponent_turn(current_turn) # 手番を変更
    turn_count += 1

#
# 盤面を戻す
#
def undo_board():
    global board, reverse, number_of_stones
    
    board = copy.deepcopy(temp_board)
    reverse = copy.deepcopy(temp_reverse)
    number_of_stones = copy.deepcopy(temp_number_of_stones)

#
# 盤面を表示する
#
def print_board():
    print
    print("  A B C D E F G H")
    for y in range(1, 8+1):
        print(y, end='')
        for x in range(1, 8+1):
            print(" " + STONES[board[y][x]], end='')
        print()

#
# 現在の状態を表示する
#
def print_status():
    turn = current_turn()
    print("move count:{0}".format(turn_count), end='')
    print("move:{0}({1})".format("black" if turn == BLACK else "white", STONES[turn]))
    print("black:{0} white:{1}".format(number_of_stones[BLACK], number_of_stones[WHITE]))
    print

#
# スコアを表示する
#
def print_score():
    print("move count:{0} ".format(turn_count), end='')
    if number_of_stones[BLACK] > number_of_stones[WHITE]:
        winner = "black"
        number_of_stones[BLACK] += number_of_stones[EMPTY]
    elif number_of_stones[BLACK] < number_of_stones[WHITE]:
        winner = "white"
        number_of_stones[WHITE] += number_of_stones[EMPTY]
    else:
        winner = "draw"
    print("winner:{0}".format(winner))
    print("black:{0} white:{1}".format(number_of_stones[BLACK], number_of_stones[WHITE]))
    print

