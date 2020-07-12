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
    private int checkRegion(long player, long opponent, int pos) {
        int region = REGION[63 - pos];
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
        int pos = Bits.countLeadingZeros(coord);
        int x = pos;
        int y = pos;
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
        int index = channel * ROWS * COLUMS + y_ * COLUMS + x_;
        state.put(index, 1);
    }

    private void fillState(FloatBuffer state, int channel) {
        int offset = channel * ROWS * COLUMS;
        float[] values = new float[ROWS * COLUMS];
        Arrays.fill(values, 1);
        state.put(values, offset, values.length);
    }

    private long oddArea;
    private long evenArea;
    private int oddCount;
    private int evenCount;
    private boolean earlyStage;
    private int[] flippedBoard = new int[] {
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 1, 1, 0, 0, 0,
            0, 0, 0, 1, 1, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
    };

    public void initialize() {
        oddArea = 0x0000000000000000L;
        evenArea = 0xffffffe7e7ffffffL;
        oddCount = 0;
        evenCount = 1;
        earlyStage = true;
    }

    pvivate void enumerateArea(long coord) {
        long cornerMask = 0x8100000000000081L;
        long edgeMask = 0x7e8181818181817eL;
        long crossMask = 0x0L;


        if ((coord & cornerMask) != 0) {
            // 左上
            long adjacentDn = 0L;
            long adjacentRt = 0L;
            if (adjacentDn == 0 || adjacentRt == 0) {
                // 領域2分割
                if ((oddArea | coord) != 0) {
                    // 奇数領域を埋める
                    if (Bits.populationCount(adjacentDn) % 2 == 0) {
                        // 奇数領域 - 1 => 偶数領域 + 偶数領域
                        evenCount += 2;
                        --oddCount;
                    } else {
                        // 奇数領域 - 1 => 奇数領域 + 奇数領域
                    }
                } else {
                    // 偶数領域を埋める
                    // 偶数領域 - 1 => 偶数領域 + 奇数領域
                }
            } else {
                // 分割なし
                if ((oddArea | coord) != 0) {
                    // 奇数領域を埋める

                } else {
                    // 偶数領域を埋める
                }
            }
        } else if ((coord & cornerMask) != 0) {
            // 右上
        } else if ((coord & cornerMask) != 0) {
            // 右下
        } else if ((coord & cornerMask) != 0) {
            // 左下
        } else if ((coord & edgeMask) != 0) {
            // 上
            long adjacentDn = 0L;
            long adjacentRt = 0L;
            long adjacentLt = 0L;
            if (divied) {

            } else {
                if ((oddArea | coord) != 0) {
                    // 奇数領域を埋める

                } else {
                    // 偶数領域を埋める
                }
            }
        } else {
            if ((oddArea | coord) != 0) {
                // 奇数領域を埋める
                int pos = 0; // FIXME
                if ((oddArea | (crossMask << pos)) == 0) {
                    // 1マス空きを埋める
                    --oddCount;
                    oddArea ^= coord;
                } else {
                    // 奇数領域が偶数領域に変わる
                    --oddCount;
                    evenCount++;
                    flippedArea(coord); // FIXME
                }
            } else {
                // 偶数領域を埋める
                --evenCount;
                oddCount++;
                flippedArea(coord); // FIXME
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

        enumerateArea(board);

        FloatBuffer state = FloatBuffer.allocate(ROWS * COLUMS * CHANNEL);
        state.clear();


        long coord_ = 0x8000000000000000L;
        while (coord_ != 0) {
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
            int reverseCount = (flippedBoard[pos] < 6) ? flippedBoard[pos] : 6; // 6以上は6プレーン目とする
            if (reverseCount > 0) {
                putState(state, region, coord_, 9 + reverseCount); // 反転数
            }
            coord_ >>>= 1;
        }
        if (!earlyStage) {
            fillState(state, 7); // 序盤でない
        }
        if (emptyCount % 2 == 1) {
            fillState(state, 8); // 空白数が奇数
        }
        if (oddCount == 1 || oddCount % 2 == 0) {
            fillState(state, 9); // 奇数領域が1個または偶数
        }

        return state;
    }
}
