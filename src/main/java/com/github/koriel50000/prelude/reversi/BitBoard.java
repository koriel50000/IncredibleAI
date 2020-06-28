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
    public void makeMove(long coord) {
        long reverseBoard = 0x0000000008000000L;

        blackBoard ^= coord | reverseBoard;
        whiteBoard ^= reverseBoard;
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
