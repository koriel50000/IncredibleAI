package com.github.koriel50000.prelude.learning;

import com.github.koriel50000.prelude.util.Bits;

import java.util.Arrays;

import static com.github.koriel50000.prelude.reversi.BitBoard.*;


public class BitState {

    public long oddArea;
    public long evenArea;
    public int oddCount;
    public int evenCount;
    public int emptyCount;
    public boolean earlyTurn;
    public long flippedBoard1;
    public long flippedBoard2;
    public long flippedBoard3;
    public long flippedBoard4;
    public long flippedBoard5;
    public long flippedBoard6;

    public long player;
    public long opponent;
    public long flipped;
    public long coord;
    public int region;

    BitState() {
        oddArea = 0x0000000000000000L;
        evenArea = 0xffffffe7e7ffffffL;
        oddCount = 0;
        evenCount = 1;
        emptyCount = 60;
        earlyTurn = true;
        flippedBoard1 = 0x0000001818000000L;
        flippedBoard2 = 0x0000000000000000L;
        flippedBoard3 = 0x0000000000000000L;
        flippedBoard4 = 0x0000000000000000L;
        flippedBoard5 = 0x0000000000000000L;
        flippedBoard6 = 0x0000000000000000L;
    }

    BitState(BitState state) {
        this.oddArea = state.oddArea;
        this.evenArea = state.evenArea;
        this.oddCount = state.oddCount;
        this.evenCount = state.evenCount;
        this.emptyCount = state.emptyCount;
        this.earlyTurn = state.earlyTurn;
        this.flippedBoard1 = state.flippedBoard1;
        this.flippedBoard2 = state.flippedBoard2;
        this.flippedBoard3 = state.flippedBoard3;
        this.flippedBoard4 = state.flippedBoard4;
        this.flippedBoard5 = state.flippedBoard5;
        this.flippedBoard6 = state.flippedBoard6;
    }

    private void put(float[] buffer, int channel, long coords) {
        long coords_;
        switch (region & 0x06) {
            case 0:
                // 変換なし
                coords_ = coords;
                break;
            case 2:
                // 左右反転
                coords_ = Bits.flipLtRt(coords);
                break;
            case 4:
                // 上下反転
                coords_ = Bits.flipUpDn(coords);
                break;
            case 6:
                // 上下左右反転
                coords_ = Bits.flip(coords);
                break;
            default:
                throw new IllegalArgumentException("no match: " + region);
        }
        if ((region & 0x01) != 0) {
            coords_ = Bits.transposed(coords_);
        }

        int offset = ROWS * COLUMNS * channel;
        while (coords_ != 0) {
            long coord = Bits.getRightmostBit(coords_);
            int index = Bits.indexOf(coord);
            buffer[offset + index] = 1;
            coords_ ^= coord;
        }
    }

    private void fill(float[] buffer, int channel) {
        int offset = ROWS * COLUMNS * channel;
        Arrays.fill(buffer, offset, offset + ROWS * COLUMNS, 1);
    }

    /**
     * 盤面を特徴量に変換する
     */
    float[] convertBuffer() {
        float[] buffer = new float[ROWS * COLUMNS * CHANNELS];

        put(buffer, 0, player); // 着手前に自石
        put(buffer, 1, opponent); // 着手前に相手石
        put(buffer, 2, ~(player | opponent)); // 着手前に空白
        put(buffer, 3, coord); // 着手
        put(buffer, 4, flipped | coord); // 変化した石
        if (oddArea != 0) {
            put(buffer, 5, oddArea); // 奇数領域
        }
        if (evenArea != 0) {
            put(buffer, 6, evenArea); // 偶数領域
        }
        if (!earlyTurn) {
            fill(buffer, 7); // 序盤でない
        }
        if (emptyCount % 2 == 1) {
            fill(buffer, 8); // 空白数が奇数
        }
        if (oddCount == 1 || oddCount % 2 == 0) {
            fill(buffer, 9); // 奇数領域が1個または偶数
        }
        if (flippedBoard1 != 0) {
            put(buffer, 10, flippedBoard1); // 反転数1
        }
        if (flippedBoard2 != 0) {
            put(buffer, 11, flippedBoard2); // 反転数2
        }
        if (flippedBoard3 != 0) {
            put(buffer, 12, flippedBoard3); // 反転数3
        }
        if (flippedBoard4 != 0) {
            put(buffer, 13, flippedBoard4); // 反転数4
        }
        if (flippedBoard5 != 0) {
            put(buffer, 14, flippedBoard5); // 反転数5
        }
        if (flippedBoard6 != 0) {
            put(buffer, 15, flippedBoard6); // 反転数6
        }

        return buffer;
    }
}
