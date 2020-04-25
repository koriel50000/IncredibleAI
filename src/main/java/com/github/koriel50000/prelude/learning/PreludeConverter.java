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
     * 対角位置を対称変換する
     */
    private Reversi.Move symmetricMove(int diagonal, Reversi.Move move) {
        switch (diagonal) {
            case 8:
                return Reversi.Move.valueOf(move.x, move.y); // 変換なし
            case 10:
                return Reversi.Move.valueOf(9 - move.x, move.y); // 左右反転
            case 12:
                return Reversi.Move.valueOf(move.x,9 - move.y); // 上下反転
            case 14:
                return Reversi.Move.valueOf(9 - move.x,9 - move.y); // 上下左右反転
            default:
                throw new IllegalArgumentException("no match: " + diagonal);
        }
    }

    /**
     * 対角位置の対称変換が必要か
     */
    private boolean isSymmetric(int diagonal, int[][] board, Reversi.Turn turn) {
        for (int y = 1; y <= 8; y++) {
            for (int x = y + 1; x <= 8; x++) {
                Reversi.Move move = symmetricMove(diagonal, Reversi.Move.valueOf(x, y));
                // FIXME 説明を記載
                if (board[move.y][move.x] != board[move.x][move.y]) {
                    return board[move.y][move.x] != turn.boardValue();
                }
            }
        }
        return false;
    }

    /**
     * 領域を判定する
     */
    private int checkRegion(int[][] board, Reversi.Move move, Reversi.Turn turn) {
        int region = REGION[move.y - 1][move.x - 1];
        if (region >= 8 && isSymmetric(region, board, turn)) {
            region += 1;
        }
        return region;
    }

    /**
     * 着手の領域から座標を変換する
     */
    private Reversi.Move normalizeMove(int region, int x, int y) {
        Reversi.Move move;
        switch (region) {
            case 0: case 8:
                move = Reversi.Move.valueOf(x - 1, y - 1); // 変換なし
                break;
            case 1: case 10:
                move = Reversi.Move.valueOf(8 - x, y - 1); // 左右反転
                break;
            case 2: case 12:
                move = Reversi.Move.valueOf(x - 1, 8 - y); // 上下反転
                break;
            case 3: case 14:
                move = Reversi.Move.valueOf(8 - x, 8 - y); // 上下左右反転
                break;
            case 4: case 9:
                move = Reversi.Move.valueOf(y - 1, x - 1); // 対称反転
                break;
            case 5: case 11:
                move = Reversi.Move.valueOf(y - 1, 8 - x); // 左右対称反転
                break;
            case 6: case 13:
                move = Reversi.Move.valueOf(8 - y, x - 1); // 上下対称反転
                break;
            case 7: case 15:
                move = Reversi.Move.valueOf(8 - y, 8 - x); // 上下左右対称反転
                break;
            default:
                throw new IllegalArgumentException("no match: " + region);
        }

        return move;
    }

    /**
     *
     */
    private int groupingAreaRecursive(int x, int y, int count) {
        oddevenBoard[y][x] = AREA_UNKNOWN;
        count += 1;
        for (Reversi.Direction dir : Reversi.Direction.corssValues()) {
            int x_ = x + dir.dx;
            int y_ = y + dir.dy;
            if (oddevenBoard[y_][x_] == AREA_EMPTY) {
                count = groupingAreaRecursive(x_, y_, count);
            }
        }
        return count;
    }

    /**
     * 空白領域を偶数領域と奇数領域に分割する
     */
    private int groupingArea(int x, int y) {
        if (oddevenBoard[y][x] == AREA_ODD || oddevenBoard[y][x] == AREA_EVEN) {
            return 0; // すでに分割済み
        }

        int number_of_empty = groupingAreaRecursive(x, y, 0);
        int oddeven = (number_of_empty % 2 == 1) ? AREA_ODD : AREA_EVEN;

        for (Reversi.Move move : Reversi.Move.values()) {
            if (oddevenBoard[move.y][move.x] == AREA_UNKNOWN) {
                oddevenBoard[move.y][move.x] = oddeven;
            }
        }
        return oddeven;
    }

    private boolean earlyStage;
    private int[][] oddevenBoard = new int[10][10];
    private int emptyCount;
    private int oddCount;
    private int evenCount;

    private void clearArea(int[][] board) {
        emptyCount = 0;
        oddCount = 0;
        evenCount = 0;
        earlyStage = true;
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (board[y][x] == Reversi.EMPTY) {
                    oddevenBoard[y][x] = AREA_EMPTY;
                } else if (board[y][x] == Reversi.BORDER) {
                    oddevenBoard[y][x] = AREA_NOT_EMPTY;
                } else {
                    oddevenBoard[y][x] = AREA_NOT_EMPTY;
                    if (x == 1 || x == 8 || y == 1 || y == 8) {
                        earlyStage = false;
                    }
                }
            }
        }
    }

    private void countingArea() {
        for (Reversi.Move move : Reversi.Move.values()) {
            if (oddevenBoard[move.y][move.x] != AREA_NOT_EMPTY) {
                emptyCount += 1;
                int oddeven = groupingArea(move.x, move.y);
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
        Reversi.Move move = normalizeMove(region, x, y);
        int index = channel * ROWS * COLUMS + move.y * COLUMS + move.x;
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

        int region = checkRegion(nextBoard, newMove, turn);

        //
        clearArea(board);
        countingArea();

        for (Reversi.Move move : Reversi.Move.values()) {
            int x = move.x;
            int y = move.y;
            if (board[y][x] == turn.boardValue()) {
                putState(state, region, x, y, 0); // 着手前に自石
            } else if (board[y][x] == turn.opponentBoardValue()) {
                putState(state, region, x, y, 1); // 着手前に相手石
            } else {
                putState(state, region, x, y, 2); // 着手前に空白
            }
            if (board[y][x] != nextBoard[y][x]) {
                putState(state, region, x, y, 4); // 変化した石
            }
            if (oddevenBoard[y][x] == AREA_ODD) {
                putState(state, region, x, y, 5); // 奇数領域
            } else if (oddevenBoard[y][x] == AREA_EVEN) {
                putState(state, region, x, y, 6); // 偶数領域
            }
            if (!earlyStage) {
                putState(state, region, x, y, 7); // 序盤でない
            }
            if (emptyCount % 2 == 1) {
                putState(state, region, x, y, 8); // 空白数が奇数
            }
            if (oddCount == 1 || oddCount % 2 == 0) {
                putState(state, region, x, y, 9); // 奇数領域が1個または偶数
            }
            int reverse_count = (reverse[y][x] < 6) ? reverse[y][x] : 6; // 6以上は6プレーン目とする
            if (reverse_count > 0) {
                putState(state, region, x, y, 9 + reverse_count); // 反転数
            }
        }

        putState(state, region, newMove.x, newMove.y, 3); // 着手

        reversi.undoBoard();

        return Tensor.create(UInt8.class, new long[] {COLUMS, ROWS, CHANNEL}, state);
    }
}
