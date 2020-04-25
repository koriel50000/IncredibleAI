package com.github.koriel50000.prelude.learning;

import com.github.koriel50000.prelude.Reversi;
import org.tensorflow.Tensor;
import org.tensorflow.types.UInt8;

import java.nio.ByteBuffer;

public class PreludeConverter {

    private static final int ROWS = 8;
    private static final int COLUMS = 8;
    private static final int CHANNEL = 16;

    private static final int AREA_EMPTY = 0;
    private static final int AREA_ODD = 1;
    private static final int AREA_EVEN = 2;
    private static final int AREA_UNKNOWN = 3;
    private static final int AREA_NOT_EMPTY = 4;

    private static final int[][] REGION = new int[][] {
            new int[] { 8, 0, 0, 0, 1, 1, 1,10 },
            new int[] { 4, 8, 0, 0, 1, 1,10, 5 },
            new int[] { 4, 4, 8, 0, 1,10, 5, 5 },
            new int[] { 4, 4, 4, 8,10, 5, 5, 5 },
            new int[] { 6, 6, 6,12,14, 7, 7, 7 },
            new int[] { 6, 6,12, 2, 3,14, 7, 7 },
            new int[] { 6,12, 2, 2, 3, 3,14, 7 },
            new int[] {12, 2, 2, 2, 3, 3, 3,14 }
    };

    /**
     * 対角位置の対称変換が必要か
     */
    private boolean isSymmetric(int diagonal, int[][] board, Reversi.Turn turn) {
        for (int y = 1; y <= 8; y++) {
            for (int x = y + 1; x <= 8; x++) {
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
                        x_ = 9 - x;
                        y_ = y;
                        break;
                    case 12:
                        // 上下反転
                        x_ = x;
                        y_ = 9 - y;
                        break;
                    case 14:
                        // 上下左右反転
                        x_ = 9 - x;
                        y_ = 9 - y;
                        break;
                    default:
                        throw new IllegalArgumentException("no match: " + diagonal);
                }
                // FIXME 説明を記載
                if (board[y_][x_] != board[x_][y_]) {
                    return board[y_][x_] != turn.boardValue();
                }
            }
        }
        return false;
    }

    /**
     * 領域を判定する
     */
    private int checkRegion(int[][] board, int x, int y, Reversi.Turn turn) {
        int region = REGION[y - 1][x - 1];
        if (region >= 8 && isSymmetric(region, board, turn)) {
            region += 1;
        }
        return region;
    }

    /**
     *
     */
    private int groupingAreaRecursive(int x, int y, int count) {
        oddevenArea[y][x] = AREA_UNKNOWN;
        count += 1;
        for (Reversi.Direction dir : Reversi.Direction.corssValues()) {
            int x_ = x + dir.dx;
            int y_ = y + dir.dy;
            if (oddevenArea[y_][x_] == AREA_EMPTY) {
                count = groupingAreaRecursive(x_, y_, count);
            }
        }
        return count;
    }

    /**
     * 空白領域を偶数領域と奇数領域に分割する
     */
    private int groupingArea(int x, int y) {
        if (oddevenArea[y][x] == AREA_ODD || oddevenArea[y][x] == AREA_EVEN) {
            return 0; // すでに分割済み
        }

        int count = groupingAreaRecursive(x, y, 0);
        int oddeven = (count % 2 == 1) ? AREA_ODD : AREA_EVEN;

        for (Reversi.Move move : Reversi.Move.values()) {
            int x_ = move.x;
            int y_ = move.y;
            if (oddevenArea[y_][x_] == AREA_UNKNOWN) {
                oddevenArea[y_][x_] = oddeven;
            }
        }
        return oddeven;
    }

    private boolean earlyStage;
    private int[][] oddevenArea = new int[10][10];
    private int emptyCount;
    private int oddCount;
    private int evenCount;

    private void clearArea(int[][] board) {
        earlyStage = true;
        emptyCount = 0;
        oddCount = 0;
        evenCount = 0;
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (board[y][x] == Reversi.EMPTY) {
                    oddevenArea[y][x] = AREA_EMPTY;
                } else if (board[y][x] == Reversi.BORDER) {
                    oddevenArea[y][x] = AREA_NOT_EMPTY;
                } else {
                    oddevenArea[y][x] = AREA_NOT_EMPTY;
                    if (x == 1 || x == 8 || y == 1 || y == 8) {
                        earlyStage = false;
                    }
                }
            }
        }
    }

    private void countingArea() {
        for (Reversi.Move move : Reversi.Move.values()) {
            int x = move.x;
            int y = move.y;
            if (oddevenArea[y][x] != AREA_NOT_EMPTY) {
                emptyCount += 1;
                int oddeven = groupingArea(x, y);
                if (oddeven == AREA_ODD) {
                    oddCount += 1;
                } else if (oddeven == AREA_EVEN) {
                    evenCount += 1;
                }
            }
        }
    }

    /**
     * 着手をプロットする
     */
    private void putState(ByteBuffer state, int region, int x, int y, int channel) {
        int x_;
        int y_;
        switch (region) {
            case 0: case 8:
                // 変換なし
                x_ = x - 1;
                y_ = y - 1;
                break;
            case 1: case 10:
                // 左右反転
                x_ = 8 - x;
                y_ = y - 1;
                break;
            case 2: case 12:
                // 上下反転
                x_ = x - 1;
                y_ = 8 - y;
                break;
            case 3: case 14:
                // 上下左右反転
                x_ = 8 - x;
                y_ = 8 - y;
                break;
            case 4: case 9:
                // 対称反転
                x_ = y - 1;
                y_ = x - 1;
                break;
            case 5: case 11:
                // 左右対称反転
                x_ = y - 1;
                y_ = 8 - x;
                break;
            case 6: case 13:
                // 上下対称反転
                x_ = 8 - y;
                y_ = x - 1;
                break;
            case 7: case 15:
                // 上下左右対称反転
                x_ = 8 - y;
                y_ = 8 - x;
                break;
            default:
                throw new IllegalArgumentException("no match: " + region);
        }
        int index = channel * ROWS * COLUMS + y_ * COLUMS + x_;
        state.put(index, (byte)1);
    }

    /**
     * 石を置いたときの状態を返す
     */
    public Tensor convertState(Reversi reversi, Reversi.Move newMove) {
        ByteBuffer state = ByteBuffer.allocate(COLUMS * ROWS * CHANNEL);

        reversi.makeMove(newMove);

        int[][] board = reversi.getTempBoard();
        int[][] nextBoard = reversi.getBoard();
        int[][] reverse = reversi.getTempReverse();
        Reversi.Turn turn = reversi.getCurrentTurn();

        int x = newMove.x;
        int y = newMove.y;
        int region = checkRegion(nextBoard, x, y, turn);

        clearArea(board);
        countingArea();

        putState(state, region, x, y, 3); // 着手

        for (Reversi.Move move : Reversi.Move.values()) {
            int x_ = move.x;
            int y_ = move.y;
            if (board[y_][x_] == turn.boardValue()) {
                putState(state, region, x_, y_, 0); // 着手前に自石
            } else if (board[y_][x_] == turn.opponentBoardValue()) {
                putState(state, region, x_, y_, 1); // 着手前に相手石
            } else {
                putState(state, region, x_, y_, 2); // 着手前に空白
            }
            if (board[y_][x_] != nextBoard[y_][x_]) {
                putState(state, region, x_, y_, 4); // 変化した石
            }
            if (oddevenArea[y_][x_] == AREA_ODD) {
                putState(state, region, x_, y_, 5); // 奇数領域
            } else if (oddevenArea[y_][x_] == AREA_EVEN) {
                putState(state, region, x_, y_, 6); // 偶数領域
            }
            if (!earlyStage) {
                putState(state, region, x_, y_, 7); // 序盤でない
            }
            if (emptyCount % 2 == 1) {
                putState(state, region, x_, y_, 8); // 空白数が奇数
            }
            if (oddCount == 1 || oddCount % 2 == 0) {
                putState(state, region, x_, y_, 9); // 奇数領域が1個または偶数
            }
            int reverseCount = (reverse[y_][x_] < 6) ? reverse[y_][x_] : 6; // 6以上は6プレーン目とする
            if (reverseCount > 0) {
                putState(state, region, x_, y_, 9 + reverseCount); // 反転数
            }
        }

        reversi.undoBoard();

        return Tensor.create(UInt8.class, new long[] { COLUMS, ROWS, CHANNEL }, state);
    }
}
