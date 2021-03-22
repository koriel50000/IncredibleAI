package com.github.koriel50000.prelude.reversi;

import com.github.koriel50000.prelude.learning.BitFeature;
import com.github.koriel50000.prelude.learning.BitState;

import java.util.Arrays;

public class BitBoard {

    public static final int COLUMNS = 8;
    public static final int ROWS = 8;
    public static final int CHANNELS = 16;

    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;

    public long blackBoard;
    public long whiteBoard;
    public int currentColor;
    public int depth;

    private boolean passedBefore;
    private Score score;

    public BitBoard() {
    }

    /**
     * 盤面を初期化する
     */
    public void clear() {
        blackBoard = 0x0000000810000000L;
        whiteBoard = 0x0000001008000000L;
        currentColor = BLACK;
        depth = 60;
        passedBefore = false;
        score = null;
    }

    /**
     * 反転する石の位置を返す
     *
     * @see <a href="http://primenumber.hatenadiary.jp/entry/2016/12/26/063226"/>
     */
    public long computeFlipped(long player, long opponent, int index) {
        // OM.x = O;
        // OM.yzw = O & 0x7e7e7e7e7e7e7e7eUL;
        // mask = (ulong4) (0x0080808080808080UL, 0x7f00000000000000UL, 0x0102040810204000UL, 0x0040201008040201UL) >> (63 - pos);
        // outflank = (0x8000000000000000UL >> clz(~OM & mask)) & P;
        // flipped  = (-outflank * 2) & mask;
        // mask = (ulong4) (0x0101010101010100UL, 0x00000000000000feUL, 0x0002040810204080UL, 0x8040201008040200UL) << pos;
        // outflank = mask & ((OM | ~mask) + 1) & P;
        // flipped |= (outflank - (outflank != 0)) & mask;
        // return flipped.x | flipped.y | flipped.z | flipped.w;

        long opponentBoardMasked = opponent & 0x7e7e7e7e7e7e7e7eL;

        // 下
        long maskDn = 0x0080808080808080L >>> index;
        int clzDn = Bits.countLeadingZeros(~opponent & maskDn);
        long outflankDn = (0x8000000000000000L >>> clzDn) & player;
        long flipped = (-outflankDn * 2) & maskDn;

        // 右
        long maskRt = 0x7f00000000000000L >>> index;
        int clzRt = Bits.countLeadingZeros(~opponentBoardMasked & maskRt);
        long outflankRt = (0x8000000000000000L >>> clzRt) & player;
        flipped |= (-outflankRt * 2) & maskRt;

        // 左下
        long maskLtDn = 0x0102040810204000L >>> index;
        int clzLtDn = Bits.countLeadingZeros(~opponentBoardMasked & maskLtDn);
        long outflankLtDn = (0x8000000000000000L >>> clzLtDn) & player;
        flipped |= (-outflankLtDn * 2) & maskLtDn;

        // 右下
        long maskRtDn = 0x0040201008040201L >>> index;
        int clzRtDn = Bits.countLeadingZeros(~opponentBoardMasked & maskRtDn);
        long outflankRtDn = (0x8000000000000000L >>> clzRtDn) & player;
        flipped |= (-outflankRtDn * 2) & maskRtDn;

        // 上
        long maskUp = 0x0101010101010100L << (63 - index);
        long outflankUp = maskUp & ((opponent | ~maskUp) + 1) & player;
        flipped |= (outflankUp - ((outflankUp | -outflankUp) >>> 63)) & maskUp;

        // 左
        long maskLt = 0x00000000000000feL << (63 - index);
        long outflankLt = maskLt & ((opponentBoardMasked | ~maskLt) + 1) & player;
        flipped |= (outflankLt - ((outflankLt | -outflankLt) >>> 63)) & maskLt;

        // 右上
        long maskRtUp = 0x0002040810204080L << (63 - index);
        long outflankRtUp = maskRtUp & ((opponentBoardMasked | ~maskRtUp) + 1) & player;
        flipped |= (outflankRtUp - ((outflankRtUp | -outflankRtUp) >>> 63)) & maskRtUp;

        // 左上
        long maskLtUp = 0x8040201008040200L << (63 - index);
        long outflankLtUp = maskLtUp & ((opponentBoardMasked | ~maskLtUp) + 1) & player;
        flipped |= (outflankLtUp - ((outflankLtUp | -outflankLtUp) >>> 63)) & maskLtUp;

        return flipped;
    }

    /**
     * 着手可能な石の位置を返す
     */
    public long availableMoves(long player, long opponent) {
        long emptyBoard = ~(player | opponent);
        long maskLtRt = opponent & 0x7e7e7e7e7e7e7e7eL;
        long maskUpDn = opponent & 0x00ffffffffffff00L;
        long maskDiag = opponent & 0x007e7e7e7e7e7e00L;

        // 右
        long tmpRt = player << 1;
        long mobility = (maskLtRt + tmpRt) & emptyBoard & ~tmpRt;

        // 左
        long tmpLt = player >>> 1 & maskLtRt;
        tmpLt |= tmpLt >>> 1 & maskLtRt;
        tmpLt |= tmpLt >>> 1 & maskLtRt;
        tmpLt |= tmpLt >>> 1 & maskLtRt;
        tmpLt |= tmpLt >>> 1 & maskLtRt;
        tmpLt |= tmpLt >>> 1 & maskLtRt;
        mobility |= tmpLt >>> 1 & emptyBoard;

        // 下
        long tmpDn = player << 8 & maskUpDn;
        tmpDn |= tmpDn << 8 & maskUpDn;
        tmpDn |= tmpDn << 8 & maskUpDn;
        tmpDn |= tmpDn << 8 & maskUpDn;
        tmpDn |= tmpDn << 8 & maskUpDn;
        tmpDn |= tmpDn << 8 & maskUpDn;
        mobility |= tmpDn << 8 & emptyBoard;

        // 上
        long tmpUp = player >>> 8 & maskUpDn;
        tmpUp |= tmpUp >>> 8 & maskUpDn;
        tmpUp |= tmpUp >>> 8 & maskUpDn;
        tmpUp |= tmpUp >>> 8 & maskUpDn;
        tmpUp |= tmpUp >>> 8 & maskUpDn;
        tmpUp |= tmpUp >>> 8 & maskUpDn;
        mobility |= tmpUp >>> 8 & emptyBoard;

        // 右下
        long tmpRtDn = player << 9 & maskDiag;
        tmpRtDn |= tmpRtDn << 9 & maskDiag;
        tmpRtDn |= tmpRtDn << 9 & maskDiag;
        tmpRtDn |= tmpRtDn << 9 & maskDiag;
        tmpRtDn |= tmpRtDn << 9 & maskDiag;
        tmpRtDn |= tmpRtDn << 9 & maskDiag;
        mobility |= tmpRtDn << 9 & emptyBoard;

        // 左上
        long tmpLtUp = player >>> 9 & maskDiag;
        tmpLtUp |= tmpLtUp >>> 9 & maskDiag;
        tmpLtUp |= tmpLtUp >>> 9 & maskDiag;
        tmpLtUp |= tmpLtUp >>> 9 & maskDiag;
        tmpLtUp |= tmpLtUp >>> 9 & maskDiag;
        tmpLtUp |= tmpLtUp >>> 9 & maskDiag;
        mobility |= tmpLtUp >>> 9 & emptyBoard;

        // 左下
        long tmpLtDn = player << 7 & maskDiag;
        tmpLtDn |= tmpLtDn << 7 & maskDiag;
        tmpLtDn |= tmpLtDn << 7 & maskDiag;
        tmpLtDn |= tmpLtDn << 7 & maskDiag;
        tmpLtDn |= tmpLtDn << 7 & maskDiag;
        tmpLtDn |= tmpLtDn << 7 & maskDiag;
        mobility |= tmpLtDn << 7 & emptyBoard;

        // 右上
        long tmpRtUp = player >>> 7 & maskDiag;
        tmpRtUp |= tmpRtUp >>> 7 & maskDiag;
        tmpRtUp |= tmpRtUp >>> 7 & maskDiag;
        tmpRtUp |= tmpRtUp >>> 7 & maskDiag;
        tmpRtUp |= tmpRtUp >>> 7 & maskDiag;
        tmpRtUp |= tmpRtUp >>> 7 & maskDiag;
        mobility |= tmpRtUp >>> 7 & emptyBoard;

        return mobility;
    }

    /**
     * 指定された位置に石を打つ
     */
    public void makeMove(long flipped, long coord) {
        blackBoard ^= (coord * (currentColor & 1)) | flipped;
        whiteBoard ^= (coord * (currentColor >> 1)) | flipped;
        --depth;
    }

    /**
     * ゲームの終了を判定する
     */
    public boolean hasCompleted(boolean passed) {
        // 空白がなくなったら終了
        // 先手・後手両方パスで終了
        // 先手・後手どちらかが完勝したら終了
        if (depth == 0 || (passed && passedBefore) ||
                Bits.populationCount(whiteBoard) == 0 || Bits.populationCount(blackBoard) == 0) {
            String winner;
            int blackCount = Bits.populationCount(blackBoard);
            int whiteCount = Bits.populationCount(whiteBoard);
            if (blackCount > whiteCount) {
                winner = "black";
                blackCount = 64 - whiteCount;
            } else if (blackCount < whiteCount) {
                winner = "white";
                whiteCount = 64 - blackCount;
            } else {
                winner = "draw";
            }
            score = new Score(winner, blackCount, whiteCount);
            return true;
        }

        return false; // 上記以外はゲーム続行
    }

    /**
     * 手番を進める
     */
    public void nextTurn(boolean passed) {
        currentColor = currentColor ^ 0x3; // 手番を変更
        passedBefore = passed;
    }

    private static final String[] STONES = {".", "@", "O"};

    private int stoneColor(int x, int y) {
        long coord = Bits.coordAt(x - 1, y - 1);
        if ((blackBoard & coord) != 0) {
            return BLACK;
        } else if ((whiteBoard & coord) != 0) {
            return WHITE;
        } else {
            return EMPTY;
        }
    }

    /**
     * 盤面を表示する
     */
    public void printBoard(LineBuffer buffer) {
        buffer.println();
        buffer.println("  A B C D E F G H");
        for (int y = 1; y <= 8; y++) {
            buffer.print(y);
            for (int x = 1; x <= 8; x++) {
                String stone = STONES[stoneColor(x, y)];
                buffer.print(" " + stone);
            }
            buffer.println();
        }
    }

    /**
     * 現在の状態を表示する
     */
    public void printStatus(LineBuffer buffer) {
        String color;
        String stone;
        if (currentColor == BLACK) {
            color = "black";
            stone = STONES[BLACK];
        } else {
            color = "white";
            stone = STONES[WHITE];
        }
        int blackCount = Bits.populationCount(blackBoard);
        int whiteCount = Bits.populationCount(whiteBoard);
        buffer.print(String.format("depth:%d ", depth));
        buffer.println(String.format("move:%s(%s)", color, stone));
        buffer.println(String.format("black:%d white:%d", blackCount, whiteCount));
        buffer.println();
    }

    /**
     * スコアを表示する
     */
    public void printScore(LineBuffer buffer) {
        buffer.print(String.format("depth:%d ", depth));
        buffer.println(String.format("winner:%s", score.getWinner()));
        buffer.println(String.format("black:%d white:%d", score.getBlackStones(), score.getWhiteStones()));
        buffer.println();
    }

    public Score getScore() {
        return score;
    }
}
