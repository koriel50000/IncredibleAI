package com.github.koriel50000.prelude.learning;

import com.github.koriel50000.prelude.reversi.Board;

import java.nio.FloatBuffer;
import java.util.List;

public class State {

    private static final int ROWS = 8;
    private static final int COLUMS = 8;
    private static final int CHANNEL = 16;

    private int region;
    private boolean earlyStage;
    private int[][] oddevenArea = new int[10][10];
    private int emptyCount;
    private int oddCount;
    private int evenCount;
    private int[][] reverse;

    private int[][] board;
    private List<Board.Coord> flipped;
    private Board.Coord coord;
    private Board.Color color;

    private FloatBuffer buffer;

    public State() {

        buffer = FloatBuffer.allocate(ROWS * COLUMS * CHANNEL);
    }

    public State(State state) {

        buffer = FloatBuffer.allocate(ROWS * COLUMS * CHANNEL);
    }

    public int getRegion() {
    }

    public FloatBuffer getBuffer() {
        return this.buffer;
    }

    /**
     * 着手をプロットする
     */
    private void putBuffer(FloatBuffer state, int region, int x, int y, int channel) {
        int x_;
        int y_;
        switch (region) {
            case 0:
            case 8:
                // 変換なし
                x_ = x - 1;
                y_ = y - 1;
                break;
            case 1:
            case 10:
                // 左右反転
                x_ = 8 - x;
                y_ = y - 1;
                break;
            case 2:
            case 12:
                // 上下反転
                x_ = x - 1;
                y_ = 8 - y;
                break;
            case 3:
            case 14:
                // 上下左右反転
                x_ = 8 - x;
                y_ = 8 - y;
                break;
            case 4:
            case 9:
                // 対称反転
                x_ = y - 1;
                y_ = x - 1;
                break;
            case 5:
            case 11:
                // 左右対称反転
                x_ = y - 1;
                y_ = 8 - x;
                break;
            case 6:
            case 13:
                // 上下対称反転
                x_ = 8 - y;
                y_ = x - 1;
                break;
            case 7:
            case 15:
                // 上下左右対称反転
                x_ = 8 - y;
                y_ = 8 - x;
                break;
            default:
                throw new IllegalArgumentException("no match: " + region);
        }
        int index = channel * ROWS * COLUMS + y_ * COLUMS + x_;
        state.put(index, 1);
    }

    private void fillBuffer(FloatBuffer state, int channel) {
        int offset = channel * ROWS * COLUMS;
        for (int i = 0; i < ROWS * COLUMS; i++) {
            state.put(offset + i, 1);
        }
    }

    public void convertBuffer() {
        putBuffer(buffer, region, x, y, 3); // 着手

        for (Board.Coord move : Board.Coord.values()) {
            int x_ = move.x;
            int y_ = move.y;
            if (board[y_][x_] == color.boardValue()) {
                putBuffer(buffer, region, x_, y_, 0); // 着手前に自石
            } else if (board[y_][x_] == color.opponentBoardValue()) {
                putBuffer(buffer, region, x_, y_, 1); // 着手前に相手石
            } else {
                putBuffer(buffer, region, x_, y_, 2); // 着手前に空白
            }
            if (board[y_][x_] != nextBoard[y_][x_]) {
                putBuffer(buffer, region, x_, y_, 4); // 変化した石
            }
            if (oddevenArea[y_][x_] == AREA_ODD) {
                putBuffer(buffer, region, x_, y_, 5); // 奇数領域
            } else if (oddevenArea[y_][x_] == AREA_EVEN) {
                putBuffer(buffer, region, x_, y_, 6); // 偶数領域
            }
            int reverseCount = (reverse[y_][x_] < 6) ? reverse[y_][x_] : 6; // 6以上は6プレーン目とする
            if (reverseCount > 0) {
                putBuffer(buffer, region, x_, y_, 9 + reverseCount); // 反転数
            }
        }
        if (!earlyStage) {
            fillBuffer(buffer, 7); // 序盤でない
        }
        if (emptyCount % 2 == 1) {
            fillBuffer(buffer, 8); // 空白数が奇数
        }
        if (oddCount == 1 || oddCount % 2 == 0) {
            fillBuffer(buffer, 9); // 奇数領域が1個または偶数
        }
    }
}
