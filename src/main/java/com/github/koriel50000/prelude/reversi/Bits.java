package com.github.koriel50000.prelude.reversi;

import org.apache.commons.lang3.StringUtils;

public final class Bits {

    private static int[] indexTable;

    /**
     * 最上位ビットのインデックスを0として、ビット位置のハッシュテーブルを作成する
     */
    static {
        indexTable = new int[64];
        long onehot = 0x8000000000000000L;
        for (int index = 0; index < 64; index++) {
            int hash = (int) ((onehot * 0x03F566ED27179461L) >>> 58);
            indexTable[hash] = index;
            onehot >>>= 1;
        }
    }

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
        return (int) bits;
    }

    /**
     * もっとも右端の立っている("1"の)ビットを返す
     */
    public static long getRightmostBit(long bits) {
        return bits & -bits;
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
        if (bits == 0) {
            return 64;
        }
        long bit = getRightmostBit(bits);
        int hash = (int) ((bit * 0x03F566ED27179461L) >>> 58);
        return 63 - indexTable[hash];
    }

    /**
     *
     */
    public static int indexOf(long coord) {
        int hash = (int) ((coord * 0x03F566ED27179461L) >>> 58);
        return indexTable[hash];
    }

    /**
     *
     */
    public static long coordAt(int x, int y) {
        return 0x8000000000000000L >>> (y * 8 + x);
    }

    /**
     *
     */
    public static long transpose(long matrix) {
        return matrix & 0x8040201008040201L |
                (matrix & 0x0080402010080402L) << 7 |
                (matrix & 0x0000804020100804L) << 14 |
                (matrix & 0x0000008040201008L) << 21 |
                (matrix & 0x0000000080402010L) << 28 |
                (matrix & 0x0000000000804020L) << 35 |
                (matrix & 0x0000000000008040L) << 42 |
                (matrix & 0x0000000000000080L) << 49 |
                (matrix >>> 7) & 0x0080402010080402L |
                (matrix >>> 14) & 0x0000804020100804L |
                (matrix >>> 21) & 0x0000008040201008L |
                (matrix >>> 28) & 0x0000000080402010L |
                (matrix >>> 35) & 0x0000000000804020L |
                (matrix >>> 42) & 0x0000000000008040L |
                (matrix >>> 49) & 0x0000000000000080L;
    }


    public static void printMatrix(long matrix) {
        System.out.println(String.format("%016d", matrix));
        for (int i = 7; i >= 0; --i) {
            long row = matrix >>> (i * 8) & 0xff;
            String bits = Long.toBinaryString(row);
            String line = StringUtils.leftPad(bits, 8, '0');
            System.out.println(line);
        }
    }
}
