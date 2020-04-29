package com.github.koriel50000.prelude;

import org.apache.commons.lang3.SerializationUtils;

import java.util.ArrayList;
import java.util.List;

public class Reversi {

    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;
    public static final int BORDER = 3;

    private int[][] board = new int[10][10];
    private int[][] reverse = new int[10][10];
    private int[] stones = new int[3];
    private int[][] tempBoard;
    private int[][] tempReverse;
    private int[] tempStones;

    private Turn currentTurn;
    private int turnCount;

    /**
     * 盤面を初期化する
     */
    public void initialize() {
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (1 <= x && x <= 8 && 1 <= y && y <= 8) {
                    board[y][x] = EMPTY;
                } else {
                    board[y][x] = BORDER;
                }
            }
        }
        board[4][4] = WHITE;
        board[4][5] = BLACK;
        board[5][4] = BLACK;
        board[5][5] = WHITE;
        reverse[4][4] = 1;
        reverse[4][5] = 1;
        reverse[5][4] = 1;
        reverse[5][5] = 1;
        stones[EMPTY] = 60;
        stones[BLACK] = 2;
        stones[WHITE] = 2;
        currentTurn = Turn.Black;
        turnCount = 1;
    }

    /**
     * 方向を指定して石が打てるかを判定する
     */
    private boolean canMoveDirection(Turn turn, Coord coord, Direction dir) {
        int x = coord.x + dir.dx;
        int y = coord.y + dir.dy;
        while (board[y][x] == turn.opponentBoardValue()) { // 相手石ならば継続
            x += dir.dx;
            y += dir.dy;
            if (board[y][x] == turn.boardValue()) { // 自石ならば終了
                return true;
            }
        }
        return false;
    }

    /**
     * 指定した位置に石が打てるかを判定する
     */
    private boolean canMove(Turn turn, Coord coord) {
        if (board[coord.y][coord.x] == EMPTY) {
            for (Direction dir : Direction.values()) {
                if (canMoveDirection(turn, coord, dir)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 着手可能なリストを返す
     */
    public List<Coord> availableMoves() {
        return availableMoves(currentTurn);
    }

    private List<Coord> availableMoves(Turn turn) {
        List<Coord> coords = new ArrayList<>();
        for (Coord coord : Coord.values()) {
            if (canMove(turn, coord)) {
                coords.add(coord);
            }
        }
        return coords;
    }

    /**
     * 指定された場所に石を打つ
     */
    public void makeMove(Coord coord) {
        tempBoard = SerializationUtils.clone(board);
        tempReverse = SerializationUtils.clone(reverse);
        tempStones = SerializationUtils.clone(stones);

        Turn turn = currentTurn;
        int x = coord.x;
        int y = coord.y;

        board[y][x] = turn.boardValue(); // 石を打つ
        reverse[y][x] += 1; // 反転数+1
        stones[turn.boardValue()] += 1; // 自石を増やす
        stones[EMPTY] -= 1; // 空白を減らす

        for (Direction dir : Direction.values()) {
            if (!canMoveDirection(turn, coord, dir)) {
                continue;
            }
            x = coord.x + dir.dx;
            y = coord.y + dir.dy;
            while (board[y][x] == turn.opponentBoardValue()) { // 相手石ならば継続
                board[y][x] = turn.boardValue(); // 石を反転
                reverse[y][x] += 1; // 反転数+1
                stones[turn.boardValue()] += 1; // 自石を増やす
                stones[turn.opponentBoardValue()] -= 1; // 相手石を減らす
                x += dir.dx;
                y += dir.dy;
            }
        }
    }

    /**
     * ゲームの終了を判定する
     */
    public boolean hasCompleted() {
        if (stones[EMPTY] == 0) {
            // 空白がなくなったら終了
            return true;
        } else if (stones[BLACK] == 0 || stones[WHITE] == 0) {
            // 先手・後手どちらかが完勝したら終了
            return true;
        } else if (availableMoves(Turn.Black).size() == 0 && availableMoves(Turn.White).size() == 0) {
            // 先手・後手両方パスで終了
            return true;
        }
        return false; // 上記以外はゲーム続行
    }

    /**
     * 手番を進める
     */
    public void nextTurn() {
        currentTurn = currentTurn.opponentTurn(); // 手番を変更
        turnCount += 1;
    }

    /**
     * 盤面を戻す
     */
    public void undoBoard() {
        board = SerializationUtils.clone(tempBoard);
        reverse = SerializationUtils.clone(tempReverse);
        stones = SerializationUtils.clone(tempStones);
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
                System.out.print(" " + STONES[board[y][x]]);
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
        if (currentTurn == Reversi.Turn.Black) {
            turn = "black";
            stone = STONES[Reversi.Turn.Black.boardValue()];
        } else {
            turn = "white";
            stone = STONES[Reversi.Turn.White.boardValue()];
        }
        System.out.print(String.format("move count:%d ", turnCount));
        System.out.println(String.format("move:%s(%s)", turn, stone));
        System.out.println(String.format("black:%d white:%d", stones[BLACK], stones[WHITE]));
        System.out.println();
    }

    /**
     * スコアを表示する
     */
    public void printScore() {
        System.out.print(String.format("move count:%d ", turnCount));
        String winner;
        if (stones[BLACK] > stones[WHITE]) {
            winner = "black";
            stones[BLACK] += stones[EMPTY];
        } else if (stones[BLACK] < stones[WHITE]) {
            winner = "white";
            stones[WHITE] += stones[EMPTY];
        } else {
            winner = "draw";
        }
        System.out.println(String.format("winner:%s", winner));
        System.out.println(String.format("black:%d white:%d", stones[BLACK], stones[WHITE]));
        System.out.println();
    }

    public Turn getCurrentTurn() {
        return currentTurn;
    }

    public int[][] getBoard() {
        return board;
    }

    public int[][] getTempBoard() {
        return tempBoard;
    }

    public int[][] getTempReverse() {
        return tempReverse;
    }

    public enum Turn {
        Black(BLACK), White(WHITE);

        private int boardValue;

        Turn(int boardValue) {
            this.boardValue = boardValue;
        }

        /**
         * 相手の手番を返す
         * Black->White、White->Black
         */
        Turn opponentTurn() {
            return this == Black ? White : Black;
        }

        public int boardValue() {
            return boardValue;
        }

        public int opponentBoardValue() {
            return opponentTurn().boardValue();
        }
    }

    public static class Coord {

        public final int x;
        public final int y;
        public final String symbol;

        private Coord(int x, int y, String symbol) {
            this.x = x;
            this.y = y;
            this.symbol = symbol;
        }

        private static Coord[] values;

        static {
            values = new Coord[8 * 8];
            int i = 0;
            for (int y = 1; y <= 8; y++) {
                for (int x = 1; x <= 8; x++) {
                    String symbol = String.format("%c%d", 'A' + (x - 1), y);
                    values[i++] = new Coord(x, y, symbol);
                }
            }
        }

        public static Coord[] values() {
            return values;
        }
        public static Coord valueOf(int x, int y) {
            return values[(y - 1) * 8 + (x - 1)];
        }

        public static Coord valueOf(String symbol) {
            return null; // TODO
        }
    }

    public static class Direction {

        public final int dx;
        public final int dy;

        private Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        private static Direction[] values;
        private static Direction[] crossValues;

        static {
            values = new Direction[] {
                    new Direction(-1, 0),
                    new Direction(-1, -1),
                    new Direction(0, -1),
                    new Direction(1, -1),
                    new Direction(1, 0),
                    new Direction(1, 1),
                    new Direction(0, 1),
                    new Direction(-1, 1),
            };
            crossValues = new Direction[] {
                    values[0],
                    values[2],
                    values[4],
                    values[6]
            };
        }

        public static Direction[] values() {
            return values;
        }

        public static Direction[] corssValues() {
            return crossValues;
        }
    }
}
