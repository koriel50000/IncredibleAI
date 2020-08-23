package com.github.koriel50000.prelude.learning;

import com.github.koriel50000.prelude.reversi.Bits;

import java.nio.FloatBuffer;
import java.util.List;

import static com.github.koriel50000.prelude.reversi.Reversi.*;

public class PreludeConverter {

    private static final int AREA_EMPTY = 0;
    private static final int AREA_ODD = 1;
    private static final int AREA_EVEN = 2;
    private static final int AREA_UNKNOWN = 3;
    private static final int AREA_NOT_EMPTY = 4;

    private static final int[] REGION = new int[]{
            8, 0, 0, 0, 2, 2, 2, 10,
            1, 8, 0, 0, 2, 2, 10, 3,
            1, 1, 8, 0, 2, 10, 3, 3,
            1, 1, 1, 8, 10, 3, 3, 3,
            5, 5, 5, 12, 14, 7, 7, 7,
            5, 5, 12, 4, 6, 14, 7, 7,
            5, 12, 4, 4, 6, 6, 14, 7,
            12, 4, 4, 4, 6, 6, 6, 14
    };

    public int region;
    private int[] oddevenArea;
    private int oddCount;
    private int evenCount;
    private int emptyCount;
    private boolean earlyTurn;
    private int[] reverse;

    public PreludeConverter() {
        oddevenArea = new int[COLUMNS * ROWS];
        reverse = new int[COLUMNS * ROWS];
    }

    public void clear() {
        region = 0;
        oddCount = 0;
        evenCount = 1;
        earlyTurn = true;
        for (Coord coord : Coord.values()) {
            reverse[coord.index()] = 0;
        }
        reverse[Coord.valueOf("D4").index()] = 1;
        reverse[Coord.valueOf("E4").index()] = 1;
        reverse[Coord.valueOf("D5").index()] = 1;
        reverse[Coord.valueOf("E5").index()] = 1;
    }

    /**
     * 対角位置の対称変換が必要か
     */
    private boolean isSymmetric(int diagonal, Board board, Color color) {
        Board board_;
        switch (diagonal) {
            case 8:
                // 変換なし
                board_ = board;
                break;
            case 10:
                // 左右反転
                board_ = board.flipLtRt();
                break;
            case 12:
                // 上下反転
                board_ = board.flipUpDn();
                break;
            case 14:
                // 上下左右反転
                board_ = board.flip();
                break;
            default:
                throw new IllegalArgumentException("no match: " + diagonal);
        }

        for (int y = 1; y <= ROWS; y++) {
            for (int x = y + 1; x <= COLUMNS; x++) {
                // 転置行列と左上から比較して、初めての違いが自石のときtrue、それ以外はfalse
                if (board_.get(x, y) != board_.get(y, x)) {
                    return board_.get(x, y) == color.value();
                }
            }
        }
        return false;
    }

    /**
     * 領域を判定する
     */
    private void checkRegion(Board board, Coord coord, Color color) {
        region = REGION[coord.index()];
        if (region >= 8 && isSymmetric(region, board, color)) {
            region += 1;
        }
    }

    /**
     * 空白を再帰的にたどって偶数領域か奇数領域かに分割する
     */
    private int calculateOddEvenRecursive(int[] area, Coord coord, int count) {
        area[coord.index()] = AREA_UNKNOWN;
        count += 1;
        for (Direction dir : Direction.corssValues()) {
            Coord coord_ = coord.move(dir);
            if (coord_ != Coord.OUT_OF_BOUNDS && area[coord_.index()] == AREA_EMPTY) {
                count = calculateOddEvenRecursive(area, coord_, count);
            }
        }
        return count;
    }

    /**
     * 空白を偶数領域か奇数領域かに分割する
     */
    private int calculateOddEven(int[] area, Coord coord) {
        if (area[coord.index()] == AREA_ODD || area[coord.index()] == AREA_EVEN) {
            return -1; // すでに分類済み
        }

        int count = calculateOddEvenRecursive(area, coord, 0);
        int oddeven = (count % 2 == 1) ? AREA_ODD : AREA_EVEN;

        for (Coord coord_ : Coord.values()) {
            if (area[coord_.index()] == AREA_UNKNOWN) {
                area[coord_.index()] = oddeven;
            }
        }
        return oddeven;
    }

    /**
     * 偶数/奇数/空白の領域を集計する
     */
    private void enumerateArea(Board board) {
        earlyTurn = true;
        for (Coord coord : Coord.values()) {
            if (board.get(coord) == Stone.EMPTY) {
                oddevenArea[coord.index()] = AREA_EMPTY;
            } else {
                oddevenArea[coord.index()] = AREA_NOT_EMPTY;
                if (coord.isCircum()) {
                    earlyTurn = false;
                }
            }
        }

        oddCount = 0;
        evenCount = 0;
        emptyCount = board.getEmptyStones();
        for (Coord coord : Coord.values()) {
            if (oddevenArea[coord.index()] != AREA_NOT_EMPTY) {
                int oddeven = calculateOddEven(oddevenArea, coord);
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
    private void putBuffer(FloatBuffer buffer, Coord coord, int channel) {
        Coord coord_;
        switch (region) {
            case 0:
            case 8:
                // 変換なし
                coord_ = coord;
                break;
            case 2:
            case 10:
                // 左右反転
                coord_ = coord.flipLtRt();
                break;
            case 4:
            case 12:
                // 上下反転
                coord_ = coord.flipUpDn();
                break;
            case 6:
            case 14:
                // 上下左右反転
                coord_ = coord.flip();
                break;
            case 1:
            case 9:
                // 対称反転
                coord_ = coord.transposed();
                break;
            case 3:
            case 11:
                // 左右対称反転
                coord_ = coord.flipLtRtTransposed();
                break;
            case 5:
            case 13:
                // 上下対称反転
                coord_ = coord.flipUpDnTransposed();
                break;
            case 7:
            case 15:
                // 上下左右対称反転
                coord_ = coord.flipTransposed();
                break;
            default:
                throw new IllegalArgumentException("no match: " + region);
        }
        int offset = channel * ROWS * COLUMNS;
        buffer.put(offset + coord_.index(), 1);
    }

    private void fillBuffer(FloatBuffer buffer, int channel) {
        int offset = channel * ROWS * COLUMNS;
        for (int i = 0; i < ROWS * COLUMNS; i++) {
            buffer.put(offset + i, 1);
        }
    }

    /**
     * 石を置いたときの状態を返す
     */
    public FloatBuffer convertState(Board board, List<Coord> flipped, Coord coord, Color color) {
        checkRegion(board, coord, color);
        enumerateArea(board);

        FloatBuffer buffer = FloatBuffer.allocate(COLUMNS * ROWS * CHANNELS);

        for (Coord coord_ : Coord.values()) {
            if (board.get(coord_) == color.value()) {
                putBuffer(buffer, coord_, 0); // 着手前に自石
            } else if (board.get(coord_) == color.opponentValue()) {
                putBuffer(buffer, coord_, 1); // 着手前に相手石
            } else {
                putBuffer(buffer, coord_, 2); // 着手前に空白
            }
            if (coord_ == coord) {
                putBuffer(buffer, coord_, 3); // 着手
                putBuffer(buffer, coord_, 4); // 変化した石
            } else if (flipped.contains(coord_)) {
                putBuffer(buffer, coord_, 4); // 変化した石
            }
            if (oddevenArea[coord_.index()] == AREA_ODD) {
                putBuffer(buffer, coord_, 5); // 奇数領域
            } else if (oddevenArea[coord_.index()] == AREA_EVEN) {
                putBuffer(buffer, coord_, 6); // 偶数領域
            }
            int reverseCount = (reverse[coord_.index()] < 6) ? reverse[coord_.index()] : 6; // 6以上は6プレーン目とする
            if (reverseCount > 0) {
                putBuffer(buffer, coord_, 9 + reverseCount); // 反転数
            }
        }
        if (!earlyTurn) {
            fillBuffer(buffer, 7); // 序盤でない
        }
        if (emptyCount % 2 == 1) {
            fillBuffer(buffer, 8); // 空白数が奇数
        }
        if (oddCount == 1 || oddCount % 2 == 0) {
            fillBuffer(buffer, 9); // 奇数領域が1個または偶数
        }

        buffer.clear();
        System.out.println(String.format("expected empty:%d odd:%d even:%d", emptyCount, oddCount, evenCount));
        Bits.printMatrix(oddevenArea);
        return buffer;
    }

    /**
     * 反転数を加算する
     */
    public void increaseFlipped(List<Coord> flipped, Coord coord) {
        for (Coord coord_ : flipped) {
            reverse[coord_.index()]++;
        }
        reverse[coord.index()]++;
    }
}
