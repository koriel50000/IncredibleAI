package com.github.koriel50000.prelude.learning;

import com.github.koriel50000.prelude.reversi.Bits;

import java.nio.FloatBuffer;
import java.util.Arrays;

public class BitConverter {

    private static final int ROWS = 8;
    private static final int COLUMS = 8;
    private static final int CHANNEL = 16;

    private static final int[] REGION = new int[]{
            8, 0, 0, 0, 1, 1, 1, 10,
            4, 8, 0, 0, 1, 1, 10, 5,
            4, 4, 8, 0, 1, 10, 5, 5,
            4, 4, 4, 8, 10, 5, 5, 5,
            6, 6, 6, 12, 14, 7, 7, 7,
            6, 6, 12, 2, 3, 14, 7, 7,
            6, 12, 2, 2, 3, 3, 14, 7,
            12, 2, 2, 2, 3, 3, 3, 14
    };

    /**
     * 対角位置の対称変換が必要か
     */
    private boolean isSymmetric(int diagonal, long player, long opponent) {
        for (int y = 0; y < ROWS; y++) {
            for (int x = y + 1; x < COLUMS; x++) {
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
                        x_ = 8 - x;
                        y_ = y;
                        break;
                    case 12:
                        // 上下反転
                        x_ = x;
                        y_ = 8 - y;
                        break;
                    case 14:
                        // 上下左右反転
                        x_ = 8 - x;
                        y_ = 8 - y;
                        break;
                    default:
                        throw new IllegalArgumentException("no match: " + diagonal);
                }
                // FIXME 説明を記載
                long coord = Bits.coordAt(x_, y_);
                long transCoord = Bits.coordAt(y_, x_);
                if ((player & coord) != (player & transCoord)) {
                    return (player & coord) == 0;
                } else if ((opponent & coord) != (opponent & transCoord)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 領域を判定する
     */
    private int checkRegion(long player, long opponent, int index) {
        int region = REGION[index];
        if (region >= 8 && isSymmetric(region, player, opponent)) {
            region += 1;
        }
        return region;
    }

    /**
     * 着手をプロットする
     */
    private void putState(FloatBuffer state, int region, long coord, int channel) {
        // FIXME
        int index = Bits.indexOf(coord);
        int x = index % 8;
        int y = index / 8;
        int x_;
        int y_;
        switch (region) {
            case 0:
            case 8:
                // 変換なし
                x_ = x;
                y_ = y;
                break;
            case 1:
            case 10:
                // 左右反転
                x_ = 7 - x;
                y_ = y;
                break;
            case 2:
            case 12:
                // 上下反転
                x_ = x;
                y_ = 7 - y;
                break;
            case 3:
            case 14:
                // 上下左右反転
                x_ = 7 - x;
                y_ = 7 - y;
                break;
            case 4:
            case 9:
                // 対称反転
                x_ = y;
                y_ = x;
                break;
            case 5:
            case 11:
                // 左右対称反転
                x_ = y;
                y_ = 7 - x;
                break;
            case 6:
            case 13:
                // 上下対称反転
                x_ = 7 - y;
                y_ = x;
                break;
            case 7:
            case 15:
                // 上下左右対称反転
                x_ = 7 - y;
                y_ = 7 - x;
                break;
            default:
                throw new IllegalArgumentException("no match: " + region);
        }
        int pos = channel * ROWS * COLUMS + y_ * COLUMS + x_;
        state.put(pos, 1);
    }

    private void fillState(FloatBuffer state, int channel) {
        int offset = channel * ROWS * COLUMS;
        for (int i = 0; i < ROWS * COLUMS; i++) {
            state.put(offset + i, 1);
        }
    }

    private long oddArea;
    private long evenArea;
    private int oddCount;
    private int evenCount;
    private boolean earlyTurn;
    private int[] flippedBoard;

    public void initialize() {
        oddArea = 0x0000000000000000L;
        evenArea = 0xffffffe7e7ffffffL;
        oddCount = 0;
        evenCount = 1;
        earlyTurn = true;
        flippedBoard = new int[] {
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 1, 1, 0, 0, 0,
                0, 0, 0, 1, 1, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0
        };
    }

    private void increaseFlipped(long flipped) {
        while (flipped != 0) {
            long coord = Bits.getRightmostBit(flipped);

            int index = Bits.indexOf(coord);
            if (flippedBoard[index] < 6) { // 6以上は6プレーン目とする
                flippedBoard[index]++;
            }

            flipped ^= coord;
        }
    }

    /**
     * 領域を再帰的にたどって反転領域を返す
     */
    private long flippedArea(long area, long coord, long flipped) {
        if ((area & coord) == 0) {
            return flipped;
        }
        flipped ^= coord;
        // 上
        flipped = flippedArea(area ^ flipped, coord << 8, flipped);
        // 下
        flipped = flippedArea(area ^ flipped, coord >>> 8, flipped);
        // 左
        flipped = flippedArea(area ^ flipped, coord << 1 & 0xfefefefefefefefeL, flipped);
        // 右
        flipped = flippedArea(area ^ flipped, coord >>> 1 & 0x7f7f7f7f7f7f7f7fL, flipped);
        return flipped;
    }

    /**
     * 偶数領域・奇数領域を集計する
     */
    private void calculateArea(long area, long coord) {
        if ((area & coord) != 0) {
            long flipped = flippedArea(area, coord, 0L);
            int oddeven = Bits.populationCount(flipped);
            if (oddeven % 2 == 0) {
                oddArea &= ~flipped;
                evenArea |= flipped;
                evenCount++;
            } else {
                oddArea |= flipped;
                evenArea &= ~flipped;
                oddCount++;
            }
        }
    }

    /**
     * 空白領域を偶数領域・奇数領域に分けて数え上げる
     */
    private void enumerateOddEven(long coord) {
        if (earlyTurn) {
            // まだ辺に接していない
            if ((oddArea & coord) != 0) {
                // 奇数領域に着手
                evenArea |= oddArea ^ coord;
                oddArea = 0x0000000000000000L;
                evenCount = 1;
                oddCount = 0;
            } else {
                // 偶数領域に着手
                oddArea |= evenArea ^ coord;
                evenArea = 0x0000000000000000L;
                oddCount = 1;
                evenCount = 0;
            }
            earlyTurn = (coord & 0xff818181818181ffL) == 0;
        } else {
            // すでに辺に接している
            if ((oddArea & coord) != 0) {
                // 奇数領域に着手
                oddArea ^= coord;
                --oddCount;
                // 上
                calculateArea(oddArea, coord << 8);
                // 下
                calculateArea(oddArea, coord >>> 8);
                // 左
                calculateArea(oddArea, coord << 1 & 0xfefefefefefefefeL);
                // 右
                calculateArea(oddArea, coord >>> 1 & 0x7f7f7f7f7f7f7f7fL);
            } else {
                // 偶数領域に着手
                evenArea ^= coord;
                --evenCount;
                // 上
                calculateArea(evenArea, coord << 8);
                // 下
                calculateArea(evenArea, coord >>> 8);
                // 左
                calculateArea(evenArea, coord << 1 & 0xfefefefefefefefeL);
                // 右
                calculateArea(evenArea, coord >>> 1 & 0x7f7f7f7f7f7f7f7fL);
            }
        }
    }

    /**
     * 石を置いたときの状態を返す
     */
    public FloatBuffer convertState(long player, long opponent, long flipped, long coord, int pos) {
        long newPlayer = player | coord | flipped;
        long newOpponent = opponent ^ flipped;
        int region = checkRegion(newPlayer, newOpponent, pos);

        enumerateOddEven(coord);
        increaseFlipped(flipped);

        FloatBuffer state = FloatBuffer.allocate(ROWS * COLUMS * CHANNEL);
        state.clear();

        long coord_ = 0x8000000000000000L;
        for (int i = 0; i < 64; i++) {
            if ((player & coord_) != 0) {
                putState(state, region, coord_, 0); // 着手前に自石
            } else if ((opponent & coord_) != 0) {
                putState(state, region, coord_, 1); // 着手前に相手石
            } else {
                putState(state, region, coord_, 2); // 着手前に空白
            }
            if (coord == coord_) {
                putState(state, region, coord_, 3); // 着手
                putState(state, region, coord_, 4); // 変化した石
            } else if ((flipped & coord_) != 0) {
                putState(state, region, coord_, 4); // 変化した石
            }
            if ((oddArea & coord_) != 0) {
                putState(state, region, coord_, 5); // 奇数領域
            } else if ((evenArea & coord_) != 0) {
                putState(state, region, coord_, 6); // 偶数領域
            }
            if (flippedBoard[i] > 0) {
                putState(state, region, coord_, 9 + flippedBoard[i]); // 反転数
            }
            coord_ >>>= 1;
        }
        if (!earlyTurn) {
            fillState(state, 7); // 序盤でない
        }
        int emptyCount = Bits.populationCount(~(newPlayer | newOpponent));
        if (emptyCount % 2 == 1) {
            fillState(state, 8); // 空白数が奇数
        }
        if (oddCount == 1 || oddCount % 2 == 0) {
            fillState(state, 9); // 奇数領域が1個または偶数
        }

        return state;
    }
}
