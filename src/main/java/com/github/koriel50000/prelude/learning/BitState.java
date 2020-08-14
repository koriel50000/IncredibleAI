package com.github.koriel50000.prelude.learning;

import com.github.koriel50000.prelude.reversi.Bits;

import static com.github.koriel50000.prelude.reversi.BitBoard.*;

import java.nio.FloatBuffer;
import java.util.Arrays;

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

    private FloatBuffer buffer;

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

        buffer = FloatBuffer.allocate(COLUMNS * ROWS * CHANNELS);
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

        buffer = FloatBuffer.allocate(COLUMNS * ROWS * CHANNELS);
    }

    public FloatBuffer getBuffer() {
        return this.buffer;
    }

    /**
     * 着手をプロットする
     */
    private void putBuffer(FloatBuffer buffer, long coord, int channel) {
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
        int pos = channel * ROWS * COLUMNS + y_ * COLUMNS + x_;
        buffer.put(pos, 1);
    }

    private void fillBuffer(FloatBuffer buffer, long fill, int channel) {
        long fill_;
        switch (region) {
            case 0:
            case 8:
                // 変換なし
                fill_ = fill;
                break;
            case 1:
            case 10:
                // 左右反転
                fill_ = Bits.flipLtRt(fill);
                break;
            case 2:
            case 12:
                // 上下反転
                fill_ = Bits.flipUpDn(fill);
                break;
            case 3:
            case 14:
                // 上下左右反転
                fill_ = Bits.flip(fill);
                break;
            case 4:
            case 9:
                // 対称反転
                fill_ = Bits.transposed(fill);
                break;
            case 5:
            case 11:
                // 左右対称反転
                fill_ = Bits.transposed(Bits.flipLtRt(fill));
                break;
            case 6:
            case 13:
                // 上下対称反転
                fill_ = Bits.transposed(Bits.flipUpDn(fill));
                break;
            case 7:
            case 15:
                // 上下左右対称反転
                fill_ = Bits.transposed(Bits.flip(fill));
                break;
            default:
                throw new IllegalArgumentException("no match: " + region);
        }

        float[] values = new float[ROWS * COLUMNS];
        while (fill_ != 0) {
            long coord = Bits.getRightmostBit(fill_);
            int index = Bits.indexOf(coord);
            values[index] = 1;
            fill_ ^= coord;
        }
        buffer.position(channel * ROWS * COLUMNS);
        buffer.put(values);
    }

    private void fillBuffer(FloatBuffer buffer, int channel) {
        float[] values = new float[ROWS * COLUMNS];
        Arrays.fill(values, 1);
        buffer.position(channel * ROWS * COLUMNS);
        buffer.put(values);
    }

    public void convertBuffer() {
        fillBuffer(buffer, player, 0); // 着手前に自石
        fillBuffer(buffer, opponent, 1); // 着手前に相手石
        fillBuffer(buffer, ~(player | opponent), 2); // 着手前に空白
        fillBuffer(buffer, coord, 3); // 着手
        fillBuffer(buffer, flipped | coord, 4); // 変化した石
//        if (oddArea != 0) {
//            fillBuffer(buffer, oddArea, 5); // 奇数領域
//        } else if (evenArea != 0) {
//            fillBuffer(buffer, evenArea, 6); // 偶数領域
//        }
        if (flippedBoard1 != 0) {
            fillBuffer(buffer, flippedBoard1,10); // 反転数1
        }
        if (flippedBoard2 != 0) {
            fillBuffer(buffer, flippedBoard2,11); // 反転数2
        }
        if (flippedBoard3 != 0) {
            fillBuffer(buffer, flippedBoard3,12); // 反転数3
        }
        if (flippedBoard4 != 0) {
            fillBuffer(buffer, flippedBoard4,13); // 反転数4
        }
        if (flippedBoard5 != 0) {
            fillBuffer(buffer, flippedBoard5,14); // 反転数5
        }
        if (flippedBoard6 != 0) {
            fillBuffer(buffer, flippedBoard6,15); // 反転数6
        }
//        if (!earlyTurn) {
//            fillBuffer(buffer, 7); // 序盤でない
//        }
        if (emptyCount % 2 == 1) {
            fillBuffer(buffer, 8); // 空白数が奇数
        }
//        if (oddCount == 1 || oddCount % 2 == 0) {
//            fillBuffer(buffer, 9); // 奇数領域が1個または偶数
//        }
        buffer.clear();
    }
}
