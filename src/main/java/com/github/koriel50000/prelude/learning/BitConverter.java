package com.github.koriel50000.prelude.learning;

import com.github.koriel50000.prelude.reversi.Bits;

public class BitConverter {

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

    private BitState currentState;

    public void clear() {
        currentState = new BitState();
    }

    public void setState(BitState state) {
        enumerateOddEven(state, state.coord);
        increaseFlipped(state, state.flipped | state.coord);
        this.currentState = state;
    }

    /**
     * 対角位置の対称変換が必要か
     */
    private boolean isSymmetric(int diagonal, long player, long opponent) {
        long player_;
        long opponent_;
        switch (diagonal) {
            case 8:
                // 変換なし
                player_ = player;
                opponent_ = opponent;
                break;
            case 10:
                // 左右反転
                player_ = Bits.flipLtRt(player);
                opponent_ = Bits.flipLtRt(opponent);
                break;
            case 12:
                // 上下反転
                player_ = Bits.flipUpDn(player);
                opponent_ = Bits.flipUpDn(opponent);
                break;
            case 14:
                // 上下左右反転
                player_ = Bits.flip(player);
                opponent_ = Bits.flip(opponent);
                break;
            default:
                throw new IllegalArgumentException("no match: " + diagonal);
        }

        long mask = 0x7F3F1F0F07030100L;
        long tPlayer_ = Bits.transposed(player_);
        long tOpponent_ = Bits.transposed(opponent_);

        long pDiff = (player_ & mask) ^ (tPlayer_ & mask);
        long oDiff = (opponent_ & mask) ^ (tOpponent_ & mask);
        int pPos = Bits.countLeadingZeros(pDiff);
        int oPos = Bits.countLeadingZeros(oDiff);

        return pPos <= oPos && (player_ & Bits.coordAt(pPos)) != 0;
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

    private void increaseFlipped(BitState state, long flipped) {
        long first = state.flippedBoard2 | state.flippedBoard3;
        long second = state.flippedBoard4 | state.flippedBoard5;
        long all = state.flippedBoard1 | first | second | state.flippedBoard6;

        while (flipped != 0) {
            long coord = Bits.getRightmostBit(flipped);

            if ((all & coord) == 0) {
                state.flippedBoard1 ^= coord;
            } else if ((state.flippedBoard1 & coord) != 0) {
                state.flippedBoard1 ^= coord;
                state.flippedBoard2 ^= coord;
            } else if ((first & coord) != 0) {
                if ((state.flippedBoard2 & coord) != 0) {
                    state.flippedBoard2 ^= coord;
                    state.flippedBoard3 ^= coord;
                } else {
                    state.flippedBoard3 ^= coord;
                    state.flippedBoard4 ^= coord;
                }
            } else if ((second & coord) != 0) {
                if ((state.flippedBoard4 & coord) != 0) {
                    state.flippedBoard4 ^= coord;
                    state.flippedBoard5 ^= coord;
                } else {
                    state.flippedBoard5 ^= coord;
                    state.flippedBoard6 ^= coord;
                }
            }

            flipped ^= coord;
        }
    }

    /**
     * 領域を再帰的にたどって反転領域を返す
     */
    private long flippedAreaRecursive(long area, long coord, long flipped) {
        area &= ~flipped;
        if ((area & coord) == 0) {
            return flipped;
        }
        flipped |= coord;
        // 上
        flipped = flippedAreaRecursive(area, coord << 8, flipped);
        // 下
        flipped = flippedAreaRecursive(area, coord >>> 8, flipped);
        // 左
        flipped = flippedAreaRecursive(area, (coord & 0x7f7f7f7f7f7f7f7fL) << 1, flipped);
        // 右
        flipped = flippedAreaRecursive(area, (coord >>> 1) & 0x7f7f7f7f7f7f7f7fL, flipped);
        return flipped;
    }

    private long flippedArea(BitState state, long area, long coord) {
        if ((area & coord) != 0) {
            long flipped = flippedAreaRecursive(area, coord, 0L);
            int oddeven = Bits.populationCount(flipped);
            if (oddeven % 2 == 0) {
                state.oddArea &= ~flipped;
                state.evenArea |= flipped;
                state.evenCount++;
            } else {
                state.oddArea |= flipped;
                state.evenArea &= ~flipped;
                state.oddCount++;
            }
            area ^= flipped;
        }
        return area;
    }

    /**
     * 偶数領域・奇数領域を集計する
     */
    private void calculateMultiArea(BitState state, long coord) {
        if ((state.oddArea & coord) != 0) {
            // 奇数領域に着手
            state.oddArea ^= coord;
            --state.oddCount;
            // 上
            long area = state.oddArea;
            area = flippedArea(state, area, coord << 8);
            // 下
            area = flippedArea(state, area, coord >>> 8);
            // 左
            area = flippedArea(state, area, (coord & 0x7f7f7f7f7f7f7f7fL) << 1);
            // 右
            flippedArea(state, area, (coord >>> 1) & 0x7f7f7f7f7f7f7f7fL);
        } else {
            // 偶数領域に着手
            state.evenArea ^= coord;
            --state.evenCount;
            // 上
            long area = state.evenArea;
            area = flippedArea(state, area, coord << 8);
            // 下
            area = flippedArea(state, area, coord >>> 8);
            // 左
            area = flippedArea(state, area, (coord & 0x7f7f7f7f7f7f7f7fL) << 1);
            // 右
            flippedArea(state, area, (coord >>> 1) & 0x7f7f7f7f7f7f7f7fL);
        }
    }

    private void calculateSingleArea(BitState state, long coord) {
        long emptyArea = state.oddArea | state.evenArea & ~coord;
        int index = Bits.indexOf(coord);

        long maskUpLt = 0x0000000000000103L;
        long maskDnRt = 0xc080000000000000L;
        long oneArea = 0L;

        // 上
        int pos = (index - 8) & 0x3f;
        long mask = maskDnRt >>> pos | maskUpLt << (63 - pos); // FIXME 端を越えたときの考慮は？
        long coord_ = coord << 8;
        if ((emptyArea & mask) == coord_) {
            oneArea |= coord_;
        }
        // 下
        pos = (index + 8) & 0x3f;
        mask = maskDnRt >>> pos | maskUpLt << (63 - pos);
        coord_ = coord >>> 8;
        if ((emptyArea & mask) == coord_) {
            oneArea |= coord_;
        }
        // 左
        pos = (index - 1) & 0x3f;
        mask = maskDnRt >>> pos | maskUpLt << (63 - pos);
        coord_ = coord << 8;
        if ((emptyArea & mask) == coord_) {
            oneArea |= coord_;
        }
        // 右
        pos = (index + 1) & 0x3f;
        mask = maskDnRt >>> pos | maskUpLt << (63 - pos);
        coord_ = coord << 8;
        if ((emptyArea & mask) == coord_) {
            oneArea |= coord_;
        }

        if (oneArea != 0) {
            // FIXME oneAreaが2つ以上あった場合は？
            System.out.println("oneArea");
            Bits.printMatrix(oneArea);
            if (Bits.populationCount(oneArea) > 1) {
                throw new ArrayIndexOutOfBoundsException();
            }
            if ((state.oddArea & coord) != 0) {
                // 奇数領域に着手
                state.oddArea ^= coord;
                state.evenArea = 0x0000000000000000L;
                state.oddCount = 2;
                state.evenCount = 0;
            } else {
                // 偶数領域に着手
                state.oddArea = oneArea;
                state.evenArea ^= coord | oneArea;
                state.oddCount = 1;
                state.evenCount = 1;
            }
        } else {
            if ((state.oddArea & coord) != 0) {
                // 奇数領域に着手
                state.evenArea |= state.oddArea ^ coord;
                state.oddArea = 0x0000000000000000L;
                state.evenCount = 1;
                state.oddCount = 0;
            } else {
                // 偶数領域に着手
                state.oddArea |= state.evenArea ^ coord;
                state.evenArea = 0x0000000000000000L;
                state.oddCount = 1;
                state.evenCount = 0;
            }
        }
    }

    /**
     * 空白領域を偶数領域・奇数領域に分けて数え上げる
     */
    private void enumerateOddEven(BitState state, long coord) {
        if (state.earlyTurn && (state.oddCount + state.evenCount) == 1) {
            calculateSingleArea(state, coord);
        } else {
            calculateMultiArea(state, coord);
        }
        state.earlyTurn = state.earlyTurn && (coord & 0xff818181818181ffL) == 0;
    }

    /**
     * 石を置いたときの状態を返す
     */
    public BitState convertState(long player, long opponent, long flipped, long coord, int index, int depth) {
        BitState state = new BitState(currentState);
        state.region = checkRegion(player, opponent, index);
        state.emptyCount = depth;
        state.player = player;
        state.opponent = opponent;
        state.flipped = flipped;
        state.coord = coord;

        state.convertBuffer();
        return state;
    }
}
