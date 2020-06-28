package com.github.koriel50000.prelude.reversi;

public class BitBoard {

    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;

    public long blackBoard;
    public long whiteBoard;
    public int currentColor;

    private int turnCount;
    private boolean passedBefore;
    private int depth;
    private Score score;

    public void initialize() {
        blackBoard = 0x0000000810000000L;
        whiteBoard = 0x0000001008000000L;
        currentColor = BLACK;
        turnCount = 1;
        passedBefore = false;
        depth = 60;
        score = null;
    }

    private int stoneIndex(int x, int y) {
        long coord = 0x8000000000000000L >>> ((y - 1) * 8 + (x - 1));
        if ((blackBoard & coord) != 0) {
            return BLACK;
        } else if ((whiteBoard & coord) != 0) {
            return WHITE;
        } else {
            return EMPTY;
        }
    }

    private int getStoneCount(long board) {
        board = (board & 0x5555555555555555L) + ((board >>> 1) & 0x5555555555555555L);
        board = (board & 0x3333333333333333L) + ((board >>> 2) & 0x3333333333333333L);
        board = (board & 0x0f0f0f0f0f0f0f0fL) + ((board >>> 4) & 0x0f0f0f0f0f0f0f0fL);
        board = (board & 0x00ff00ff00ff00ffL) + ((board >>> 8) & 0x00ff00ff00ff00ffL);
        board = (board & 0x0000ffff0000ffffL) + ((board >>> 16) & 0x0000ffff0000ffffL);
        board = (board & 0x00000000ffffffffL) + ((board >>> 32) & 0x00000000ffffffffL);
        return (int)board;
    }

    private int getEmptyCount() {
        long board = ~(blackBoard | whiteBoard);
        return getStoneCount(board);
    }

    private long getReverseBoard() {
        long emptyBoard = ~(blackBoard | whiteBoard);
        while (emptyBoard != 0) {
            long coord = emptyBoard & -emptyBoard;  // 一番右のビットのみ取り出す

            emptyBoard ^= coord;  // 一番右のビットを0にする
        }
        return 0L;
    }

    /**
     * 一番右端の立っているビット位置を返す
     */
    private int getRightmostBit(long coord) {
        return getStoneCount(coord - 1);
    }

    /**
     * 最上位ビットから連続する 0 のビットの数を返す
     */
    private int clz(long board) {
        board = board | ( board >>>  1 );
        board = board | ( board >>>  2 );
        board = board | ( board >>>  4 );
        board = board | ( board >>>  8 );
        board = board | ( board >>> 16 );
        board = board | ( board >>> 32 );
        return getStoneCount(~board);
    }

    private boolean first = true;

    /**
     * 着手可能なリストを返す
     */
    public long availableMoves() {
        if (first) {
            first = false;
            return 0x0000102004080000L;
        } else {
            return 0L;
        }
    }

    /**
     * 指定された場所に石を打つ
     */
    public void makeMove(long playerBoard, long opponentBoard, long coord) {
        long reverseBoard = 0;
        int pos = getRightmostBit(coord);

        long outflankx, outflanky, outflankz, outflankw;

        long opponentBoardMasked = opponentBoard & 0x7e7e7e7e7e7e7e7eL;

        // 下
        long downMask = 0x0080808080808080L >>> (63 - pos);
        int clz1 = clz(~opponentBoard & downMask);
        outflankx = (0x8000000000000000L >> clz1) & playerBoard;
        reverseBoard |= (-outflankx << 1) & downMask;

        // 右
        long rightMask = 0x7f00000000000000L >>> (63 - pos);
        int clz2 = clz(~opponentBoardMasked & rightMask);
        outflanky = (0x8000000000000000L >> clz2) & playerBoard;
        reverseBoard |= (-outflanky << 1) & rightMask;

        // 左下
        long leftDownMask = 0x0102040810204000L >>> (63 - pos);
        int clz3 = clz(~opponentBoardMasked & leftDownMask);
        outflankz = (0x8000000000000000L >> clz3) & playerBoard;
        reverseBoard |= (-outflankz << 1) & leftDownMask;

        // 右下
        long rightDownMask = 0x0040201008040201L >>> (63 - pos);
        int clz4 = clz(~opponentBoardMasked & rightDownMask);
        outflankw = (0x8000000000000000L >> clz4) & playerBoard;
        reverseBoard |= (-outflankw << 1) & rightDownMask;

        // 上
        long upMask = 0x0101010101010100L << pos;
        outflankx = upMask & ((opponentBoard | ~upMask) + 1) & playerBoard;
        reverseBoard |= (outflankx - (outflankx != 0)) & upMask;

        // 左
        long leftMask = 0x00000000000000feL << pos;
        outflanky = leftMask & ((opponentBoardMasked | ~leftMask) + 1) & playerBoard;
        reverseBoard |= (outflanky - (outflanky != 0)) & leftMask;

        // 右上
        long rightUpMask = 0x0002040810204080L << pos;
        outflankz = rightUpMask & ((opponentBoardMasked | ~rightUpMask) + 1) & playerBoard;
        reverseBoard |= (outflankz - (outflankz != 0)) & rightUpMask;

        // 左上
        long leftUpMask = 0x8040201008040200L << pos;
        outflankw = leftUpMask & ((opponentBoardMasked | ~leftUpMask) + 1) & playerBoard;
        reverseBoard |= (outflankw - (outflankw != 0)) & leftUpMask;

        blackBoard ^= (coord * (currentColor & 0x01)) | reverseBoard;
        whiteBoard ^= (coord * (currentColor >> 1)) | reverseBoard;
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
                getStoneCount(whiteBoard) == 0 || getStoneCount(blackBoard) == 0)
        {
            String winner;
            int blackCount = getStoneCount(blackBoard);
            int whiteCount = getStoneCount(whiteBoard);
            int emptyCount = getEmptyCount();
            if (blackCount > whiteCount) {
                winner = "black";
                blackCount += emptyCount;
            } else if (blackCount < whiteCount) {
                winner = "white";
                whiteCount += emptyCount;
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
        turnCount += 1;
        passedBefore = passed;
    }

    private static final String[] STONES = { ".", "@", "O" };

    /**
     * 盤面を表示する
     */
    public void printBoard() {
        System.out.println();
        System.out.println("  A B C D E F G H");
        for (int y = 1; y <= 8; y++) {
            System.out.print(y);
            for (int x = 1; x <= 8; x++) {
                String stone = STONES[stoneIndex(x, y)];
                System.out.print(" " + stone);
            }
            System.out.println();
        }
    }

    /**
     * 現在の状態を表示する
     */
    public void printStatus() {
        String turn;
        String stone;
        if (currentColor == BLACK) {
            turn = "black";
            stone = STONES[BLACK];
        } else {
            turn = "white";
            stone = STONES[WHITE];
        }
        int blackCount = getStoneCount(blackBoard);
        int whiteCount = getStoneCount(whiteBoard);
        System.out.print(String.format("move count:%d ", turnCount));
        System.out.println(String.format("move:%s(%s)", turn, stone));
        System.out.println(String.format("black:%d white:%d", blackCount, whiteCount));
        System.out.println();
    }

    /**
     * スコアを表示する
     */
    public void printScore() {
        System.out.print(String.format("move count:%d ", turnCount));
        System.out.println(String.format("winner:%s", score.getWinner()));
        System.out.println(String.format("black:%d white:%d", score.getBlackStones(), score.getWhiteStones()));
        System.out.println();
    }

    public static class Score {

        private String winner;
        private int blackStones;
        private int whiteStones;

        private Score(String winner, int blackStones, int whiteStones) {
            this.winner = winner;
            this.blackStones = blackStones;
            this.whiteStones = whiteStones;
        }

        public String getWinner() {
            return winner;
        }

        public int getBlackStones() {
            return blackStones;
        }

        public int getWhiteStones() {
            return whiteStones;
        }
    }
}
