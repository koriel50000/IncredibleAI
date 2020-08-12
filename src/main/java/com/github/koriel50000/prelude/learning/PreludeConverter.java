package com.github.koriel50000.prelude.learning;

import com.github.koriel50000.prelude.reversi.Bits;
import com.github.koriel50000.prelude.reversi.Board;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

public class PreludeConverter {

    private static final int ROWS = 8;
    private static final int COLUMS = 8;
    private static final int CHANNEL = 16;

    private static final int AREA_EMPTY = 0;
    private static final int AREA_ODD = 1;
    private static final int AREA_EVEN = 2;
    private static final int AREA_UNKNOWN = 3;
    private static final int AREA_NOT_EMPTY = 4;

    private static final int[][] REGION = new int[][]{
            new int[]{8, 0, 0, 0, 1, 1, 1, 10},
            new int[]{4, 8, 0, 0, 1, 1, 10, 5},
            new int[]{4, 4, 8, 0, 1, 10, 5, 5},
            new int[]{4, 4, 4, 8, 10, 5, 5, 5},
            new int[]{6, 6, 6, 12, 14, 7, 7, 7},
            new int[]{6, 6, 12, 2, 3, 14, 7, 7},
            new int[]{6, 12, 2, 2, 3, 3, 14, 7},
            new int[]{12, 2, 2, 2, 3, 3, 3, 14}
    };

    private State currentState;

    public void clear() {
        currentState = new State();
    }

    public void setState(State state) {
        this.currentState = state;
    }

    /**
     * 対角位置の対称変換が必要か
     */
    private boolean isSymmetric(int diagonal, int[][] board, Board.Color color) {
        int[][] tmp = new int[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int x_;
                int y_;
                switch (diagonal) {
                    case 8:
                        // 変換なし
                        x_ = x;
                        y_ = y;
                        break;
                    case 10:
                        // 左右反転
                        x_ = 7 - x;
                        y_ = y;
                        break;
                    case 12:
                        // 上下反転
                        x_ = x;
                        y_ = 7 - y;
                        break;
                    case 14:
                        // 上下左右反転
                        x_ = 7 - x;
                        y_ = 7 - y;
                        break;
                    default:
                        throw new IllegalArgumentException("no match: " + diagonal);
                }
                tmp[y][x] = board[y_ + 1][x_ + 1];
            }
        }

        for (int y = 0; y < 8; y++) {
            for (int x = y + 1; x < 8; x++) {
                // 転置行列と左上から比較して、初めての違いが自石のときtrue、それ以外はfalse
                if (tmp[y][x] != tmp[x][y]) {
                    System.out.println(String.format("expected: %d", y * 8 + x));
                    Bits.printMatrix(tmp, false);
                    return tmp[y][x] == color.boardValue();
                }
            }
        }
        System.out.println(String.format("expected: %d", 64));
        return false;
    }

    /**
     * 領域を判定する
     */
    private int checkRegion(int[][] board, int x, int y, Board.Color color) {
        int region = REGION[y - 1][x - 1];
        if (region >= 8 && isSymmetric(region, board, color)) {
            region += 1;
        }
        return region;
    }

    private void increaseFlipped(State state, List<Board.Coord> flipped) {
        for (Board.Coord coord : flipped) {
            if ((state.flippedBoard1 & coord) != 0) {
                state.flippedBoard1 ^= coord;
                state.flippedBoard2 ^= coord;
            } else if ((state.flippedBoard2 & coord) != 0) {
                state.flippedBoard2 ^= coord;
                state.flippedBoard3 ^= coord;
            } else if ((state.flippedBoard3 & coord) != 0) {
                state.flippedBoard3 ^= coord;
                state.flippedBoard4 ^= coord;
            } else if ((state.flippedBoard4 & coord) != 0) {
                state.flippedBoard4 ^= coord;
                state.flippedBoard5 ^= coord;
            } else if ((state.flippedBoard5 & coord) != 0) {
                state.flippedBoard5 ^= coord;
                state.flippedBoard6 ^= coord;
            }
        }
    }

    /**
     * 空白を再帰的にたどって偶数領域か奇数領域かに分割する
     */
    private int calculateOddEvenRecursive(int x, int y, int count) {
        oddevenArea[y][x] = AREA_UNKNOWN;
        count += 1;
        for (Board.Direction dir : Board.Direction.corssValues()) {
            int x_ = x + dir.dx;
            int y_ = y + dir.dy;
            if (oddevenArea[y_][x_] == AREA_EMPTY) {
                count = calculateOddEvenRecursive(x_, y_, count);
            }
        }
        return count;
    }

    /**
     * 空白を偶数領域か奇数領域かに分割する
     */
    private int calculateOddEven(int x, int y) {
        if (oddevenArea[y][x] == AREA_ODD || oddevenArea[y][x] == AREA_EVEN) {
            return -1; // すでに分類済み
        }

        int count = calculateOddEvenRecursive(x, y, 0);
        int oddeven = (count % 2 == 1) ? AREA_ODD : AREA_EVEN;

        for (Board.Coord move : Board.Coord.values()) {
            int x_ = move.x;
            int y_ = move.y;
            if (oddevenArea[y_][x_] == AREA_UNKNOWN) {
                oddevenArea[y_][x_] = oddeven;
            }
        }
        return oddeven;
    }

    /**
     * 偶数/奇数/空白の領域を集計する
     */
    private void enumerateArea(int[][] board) {
        earlyStage = true;
        emptyCount = 0;
        oddCount = 0;
        evenCount = 0;
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (board[y][x] == Board.EMPTY) {
                    oddevenArea[y][x] = AREA_EMPTY;
                } else if (board[y][x] == Board.BORDER) {
                    oddevenArea[y][x] = AREA_NOT_EMPTY;
                } else {
                    oddevenArea[y][x] = AREA_NOT_EMPTY;
                    if (x == 1 || x == 8 || y == 1 || y == 8) {
                        earlyStage = false;
                    }
                }
            }
        }

        for (Board.Coord move : Board.Coord.values()) {
            int x = move.x;
            int y = move.y;
            if (oddevenArea[y][x] != AREA_NOT_EMPTY) {
                emptyCount += 1;
                int oddeven = calculateOddEven(x, y);
                if (oddeven == AREA_ODD) {
                    oddCount += 1;
                } else if (oddeven == AREA_EVEN) {
                    evenCount += 1;
                }
            }
        }
    }

    /**
     * 石を置いたときの状態を返す
     */
    public State convertState(int[][] board, List<Board.Coord> flipped, Board.Coord coord, Board.Color color) {
        State state = new State(currentState);
        state.board = board;
        state.flipped = flipped;
        state.coord = coord;
        state.region = checkRegion(board, x, y, color);

        enumerateArea(state, coord);
        increaseFlipped(state, flipped);

        state.convertBuffer();

        return state;
    }
}
