package com.github.koriel50000.prelude.learning;

import com.github.koriel50000.prelude.reversi.Bits;

import java.nio.FloatBuffer;
import java.util.Arrays;

import static com.github.koriel50000.prelude.reversi.BitBoard.*;


public class BitState {

    public int region;
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

    private float[] buffer;

    BitState() {
        region = 0;
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

        buffer = new float[COLUMNS * ROWS * CHANNELS];
    }

    BitState(BitState state) {
        this.region = state.region;
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

        buffer = new float[COLUMNS * ROWS * CHANNELS];
    }

    public FloatBuffer getBuffer() {
        return FloatBuffer.wrap(buffer);
    }

    private void put(int channel, long coords) {
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

        int offset = channel * ROWS * COLUMNS;
        while (coords_ != 0) {
            long coord = Bits.getRightmostBit(coords_);
            int index = Bits.indexOf(coord);
            buffer[offset + index] = 1;
            coords_ ^= coord;
        }
    }

    private void fill(int channel) {
        int offset = channel * ROWS * COLUMNS;
        Arrays.fill(buffer, offset,  offset + ROWS * COLUMNS, 1);
    }

    /**
     * 盤面を特徴量に変換する
     */
    void convertBuffer() {
        put(0, player); // 着手前に自石
        put(1, opponent); // 着手前に相手石
        put(2, ~(player | opponent)); // 着手前に空白
        put(3, coord); // 着手
        put(4, flipped | coord); // 変化した石
        if (oddArea != 0) {
            put(5, oddArea); // 奇数領域
        }
        if (evenArea != 0) {
            put(6, evenArea); // 偶数領域
        }
        if (!earlyTurn) {
            fill(7); // 序盤でない
        }
        if (emptyCount % 2 == 1) {
            fill(8); // 空白数が奇数
        }
        if (oddCount == 1 || oddCount % 2 == 0) {
            fill(9); // 奇数領域が1個または偶数
        }
        if (flippedBoard1 != 0) {
            put(10, flippedBoard1); // 反転数1
        }
        if (flippedBoard2 != 0) {
            put(11, flippedBoard2); // 反転数2
        }
        if (flippedBoard3 != 0) {
            put(12, flippedBoard3); // 反転数3
        }
        if (flippedBoard4 != 0) {
            put(13, flippedBoard4); // 反転数4
        }
        if (flippedBoard5 != 0) {
            put(14, flippedBoard5); // 反転数5
        }
        if (flippedBoard6 != 0) {
            put(15, flippedBoard6); // 反転数6
        }
    }
}
