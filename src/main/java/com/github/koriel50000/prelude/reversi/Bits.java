package com.github.koriel50000.prelude.reversi;

public final class Bits {

    /**
     * 立っている("1"の)ビットの数を返す
     */
    public static int populationCount(long bits) {
        bits = (bits & 0x5555555555555555L) + ((bits >>> 1) & 0x5555555555555555L);
        bits = (bits & 0x3333333333333333L) + ((bits >>> 2) & 0x3333333333333333L);
        bits = (bits & 0x0f0f0f0f0f0f0f0fL) + ((bits >>> 4) & 0x0f0f0f0f0f0f0f0fL);
        bits = (bits & 0x00ff00ff00ff00ffL) + ((bits >>> 8) & 0x00ff00ff00ff00ffL);
        bits = (bits & 0x0000ffff0000ffffL) + ((bits >>> 16) & 0x0000ffff0000ffffL);
        bits = (bits & 0x00000000ffffffffL) + ((bits >>> 32) & 0x00000000ffffffffL);
        return (int)bits;
    }

    /**
     * 最上位ビット(MSB: Most Significant Bit)から連続する"0"のビットの数を返す
     */
    public static int countLeadingZeros(long bits) {
        bits |= bits >>> 1;
        bits |= bits >>> 2;
        bits |= bits >>> 4;
        bits |= bits >>> 8;
        bits |= bits >>> 16;
        bits |= bits >>> 32;
        return populationCount(~bits);
    }

    /**
     * 最下位ビット(LSB: Least Significant Bit)から連続する"0"のビットの数を返す
     */
    public static int countTrailingZeros(long bits) {
        return populationCount(bits - 1);
    }

    /**
     *
     */
    public static long getRightmostBit(long bits) {
        return bits & -bits;
    }

    /**
     *
     */
    public static long coordAt(int x, int y) {
        return 0x8000000000000000L >>> (y * 8 + x);
    }
}
