package com.github.koriel50000.prelude.learning;

import com.github.koriel50000.prelude.reversi.Bits;
import com.github.koriel50000.prelude.reversi.Board;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

public class PreludeConverter {

    public static final int ROWS = 8;
    public static final int COLUMS = 8;
    public static final int CHANNEL = 16;

    public static final int AREA_EMPTY = 0;
    public static final int AREA_ODD = 1;
    public static final int AREA_EVEN = 2;
    public static final int AREA_UNKNOWN = 3;
    public static final int AREA_NOT_EMPTY = 4;

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
            int x = coord.x;
            int y = coord.y;
            state.reverse[y][x]++;
        }
    }

    /**
     * 空白を再帰的にたどって偶数領域か奇数領域かに分割する
     */
    private int calculateOddEvenRecursive(int[][] area, int x, int y, int count) {
        area[y][x] = AREA_UNKNOWN;
        count += 1;
        for (Board.Direction dir : Board.Direction.corssValues()) {
            int x_ = x + dir.dx;
            int y_ = y + dir.dy;
            if (area[y_][x_] == AREA_EMPTY) {
                count = calculateOddEvenRecursive(area, x_, y_, count);
            }
        }
        return count;
    }

    /**
     * 空白を偶数領域か奇数領域かに分割する
     */
    private int calculateOddEven(int[][] area, int x, int y) {
        if (area[y][x] == AREA_ODD || area[y][x] == AREA_EVEN) {
            return -1; // すでに分類済み
        }

        int count = calculateOddEvenRecursive(area, x, y, 0);
        int oddeven = (count % 2 == 1) ? AREA_ODD : AREA_EVEN;

        for (Board.Coord move : Board.Coord.values()) {
            int x_ = move.x;
            int y_ = move.y;
            if (area[y_][x_] == AREA_UNKNOWN) {
                area[y_][x_] = oddeven;
            }
        }
        return oddeven;
    }

    /**
     * 偶数/奇数/空白の領域を集計する
     */
    private void enumerateArea(State state, int[][] board) {
        state.earlyTurn = true;
        state.emptyCount = 0;
        state.oddCount = 0;
        state.evenCount = 0;
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (board[y][x] == Board.EMPTY) {
                    state.oddevenArea[y][x] = AREA_EMPTY;
                } else if (board[y][x] == Board.BORDER) {
                    state.oddevenArea[y][x] = AREA_NOT_EMPTY;
                } else {
                    state.oddevenArea[y][x] = AREA_NOT_EMPTY;
                    if (x == 1 || x == 8 || y == 1 || y == 8) {
                        state.earlyTurn = false;
                    }
                }
            }
        }

        for (Board.Coord coord : Board.Coord.values()) {
            int x = coord.x;
            int y = coord.y;
            if (state.oddevenArea[y][x] != AREA_NOT_EMPTY) {
                state.emptyCount += 1;
                int oddeven = calculateOddEven(state.oddevenArea, x, y);
                if (oddeven == AREA_ODD) {
                    state.oddCount += 1;
                } else if (oddeven == AREA_EVEN) {
                    state.evenCount += 1;
                }
            }
        }
    }

    /**
     * 石を置いたときの状態を返す
     */
    public State convertState(int[][] board, List<Board.Coord> flipped, Board.Coord coord, Board.Color color) {
        State state = new State(currentState, board, flipped, coord, color);
        state.region = checkRegion(board, coord.x, coord.y, color);

        enumerateArea(state, board);
        increaseFlipped(state, flipped);

        state.convertBuffer();

        return state;
    }
}
