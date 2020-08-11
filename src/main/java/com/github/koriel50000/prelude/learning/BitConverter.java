package com.github.koriel50000.prelude.learning;

import com.github.koriel50000.prelude.reversi.Bits;

import java.nio.FloatBuffer;

public class BitConverter {

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

    private BitState currentState;

    public void clear() {
        currentState = new BitState();
    }

    public void setState(BitState state) {
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
                player_ = Bits.horizontalMatrix(player);
                opponent_ = Bits.horizontalMatrix(opponent);
                break;
            case 12:
                // 上下反転
                player_ = Bits.verticalMatrix(player);
                opponent_ = Bits.verticalMatrix(opponent);
                break;
            case 14:
                // 上下左右反転
                player_ = Bits.verticalAndHorizontalMatrix(player);
                opponent_ = Bits.verticalAndHorizontalMatrix(opponent);
                break;
            default:
                throw new IllegalArgumentException("no match: " + diagonal);
        }

        long mask = 0x7F3F1F0F07030100L;
        long tPlayer_ = Bits.transposedMatrix(player_);
        long tOpponent_ = Bits.transposedMatrix(opponent_);

        long pDiff = (player_ & mask) ^ (tPlayer_ & mask);
        long oDiff = (opponent_ & mask) ^ (tOpponent_ & mask);
        int pPos = Bits.countLeadingZeros(pDiff);
        int oPos = Bits.countLeadingZeros(oDiff);
        System.out.println(String.format("actual: %d %d", pPos, oPos));
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
        while (flipped != 0) {
            long coord = Bits.getRightmostBit(flipped);

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
    private void calculateArea(BitState state, long area, long coord) {
        if ((area & coord) != 0) {
            long flipped = flippedArea(area, coord, 0L);
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
        }
    }

    /**
     * 空白領域を偶数領域・奇数領域に分けて数え上げる
     */
    private void enumerateOddEven(BitState state, long coord) {
        if (state.earlyTurn) {
            // まだ辺に接していない
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
            state.earlyTurn = (coord & 0xff818181818181ffL) == 0;
        } else {
            // すでに辺に接している
            if ((state.oddArea & coord) != 0) {
                // 奇数領域に着手
                state.oddArea ^= coord;
                --state.oddCount;
                // 上
                calculateArea(state, state.oddArea, coord << 8);
                // 下
                calculateArea(state, state.oddArea, coord >>> 8);
                // 左
                calculateArea(state, state.oddArea, coord << 1 & 0xfefefefefefefefeL);
                // 右
                calculateArea(state, state.oddArea, coord >>> 1 & 0x7f7f7f7f7f7f7f7fL);
            } else {
                // 偶数領域に着手
                state.evenArea ^= coord;
                --state.evenCount;
                // 上
                calculateArea(state, state.evenArea, coord << 8);
                // 下
                calculateArea(state, state.evenArea, coord >>> 8);
                // 左
                calculateArea(state, state.evenArea, coord << 1 & 0xfefefefefefefefeL);
                // 右
                calculateArea(state, state.evenArea, coord >>> 1 & 0x7f7f7f7f7f7f7f7fL);
            }
        }
    }

    /**
     * 石を置いたときの状態を返す
     */
    public BitState convertState(long player, long opponent, long flipped, long coord, int index) {
        BitState state = new BitState(currentState);
        state.player = player;
        state.opponent = opponent;
        state.flipped = flipped;
        state.coord = coord;
        state.region = checkRegion(player, opponent, index);

        enumerateOddEven(state, coord);
        increaseFlipped(state, flipped);

        state.convertBuffer();

        return state;
    }
}
