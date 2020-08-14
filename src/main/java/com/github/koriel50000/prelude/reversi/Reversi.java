package com.github.koriel50000.prelude.reversi;

import com.github.koriel50000.prelude.learning.PreludeConverter;

import java.nio.FloatBuffer;
import java.util.*;

public class Reversi {

    public static final int COLUMNS = 8;
    public static final int ROWS = 8;
    public static final int CHANNELS = 16;

    private Board board;
    private Color currentColor;
    private int turnCount;
    private boolean passedBefore;
    private Score score;

    private PreludeConverter converter;

    public Reversi() {
        board = new Board();
        converter = new PreludeConverter();
    }

    /**
     * 盤面を初期化する
     */
    public void clear() {
        board.clear();
        currentColor = Color.Black;
        turnCount = 1;
        passedBefore = false;
        score = null;
        converter.clear();
    }

    /**
     * 方向を指定して石が打てるかを判定する
     */
    private boolean canMoveDirection(Coord coord, Direction dir) {
        Coord coord_ = coord.move(dir);
        while (board.get(coord_) == currentColor.opponentValue()) { // 相手石ならば継続
            coord_ = coord_.move(dir);
            if (board.get(coord_) == currentColor.value()) { // 自石ならば終了
                return true;
            }
        }
        return false;
    }

    /**
     * 指定した位置に石が打てるかを判定する
     */
    private boolean canMove(Coord coord) {
        if (board.get(coord) == Stone.EMPTY) {
            for (Direction dir : Direction.values()) {
                if (canMoveDirection(coord, dir)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Coord> computeFlipped(Coord coord) {
        List<Coord> flipped = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (!canMoveDirection(coord, dir)) {
                continue;
            }
            Coord coord_ = coord.move(dir);
            while (board.get(coord_) == currentColor.opponentValue()) { // 相手石ならば継続
                flipped.add(coord_); // 反転位置を追加
                coord_ = coord_.move(dir);
            }
        }
        return Collections.unmodifiableList(flipped);
    }

    /**
     * 着手可能なリストを返す
     */
    public List<Coord> availableMoves() {
        List<Coord> coords = new ArrayList<>();
        for (Coord coord : Coord.values()) {
            if (canMove(coord)) {
                coords.add(coord);
            }
        }
        return Collections.unmodifiableList(coords);
    }

    public int region;

    public FloatBuffer convertState(Coord coord) {
        List<Coord> flipped = computeFlipped(coord);
        FloatBuffer buffer = converter.convertState(board, flipped, coord, currentColor);
        region = converter.region;
        return buffer;
    }

    /**
     * 指定された場所に石を打つ
     */
    public void makeMove(Coord coord) {
        List<Coord> flipped = computeFlipped(coord);
        converter.setFlipped(flipped, coord);

        board.put(coord, currentColor); // 石を打つ
        for (Coord coord_ : flipped) {
            board.put(coord_, currentColor); // 石を反転
        }
    }

    /**
     * ゲームの終了を判定する
     */
    public boolean hasCompleted(boolean passed) {
        // 空白がなくなったら終了
        // 先手・後手両方パスで終了
        // 先手・後手どちらかが完勝したら終了
        if (board.getEmptyStones() == 0 || (passed && passedBefore) ||
                board.getWhiteStones() == 0 || board.getBlackStones() == 0) {
            String winner;
            int emptyCount = board.getEmptyStones();
            int blackCount = board.getBlackStones();
            int whiteCount = board.getWhiteStones();
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
        currentColor = currentColor.opponentColor(); // 手番を変更
        turnCount++;
        passedBefore = passed;
    }

    /**
     * 盤面を表示する
     */
    public void printBoard(LineBuffer buffer) {
        buffer.println();
        buffer.println("  A B C D E F G H");
        for (int y = 1; y <= ROWS; y++) {
            buffer.print(y);
            for (int x = 1; x <= COLUMNS; x++) {
                buffer.print(" " + board.get(x, y).toString());
            }
            buffer.println();
        }
    }

    /**
     * 現在の状態を表示する
     */
    public void printStatus(LineBuffer buffer) {
        String turn;
        String stone;
        if (currentColor == Color.Black) {
            turn = "black";
            stone = Stone.BLACK.toString();
        } else {
            turn = "white";
            stone = Stone.WHITE.toString();
        }
        buffer.print(String.format("move count:%d ", turnCount));
        buffer.println(String.format("move:%s(%s)", turn, stone));
        buffer.println(String.format("black:%d white:%d",
                board.getBlackStones(), board.getWhiteStones()));
        buffer.println();
    }

    /**
     * スコアを表示する
     */
    public void printScore(LineBuffer buffer) {
        buffer.print(String.format("move count:%d ", turnCount));
        buffer.println(String.format("winner:%s", score.getWinner()));
        buffer.println(String.format("black:%d white:%d", score.getBlackStones(), score.getWhiteStones()));
        buffer.println();
    }

    public Color getCurrentColor() {
        return currentColor;
    }

    public Score getScore() {
        return score;
    }

    public static final class Board {

        private Stone[] board;
        private int[] stones;

        Board() {
            board = new Stone[COLUMNS * ROWS];
            stones = new int[3];
        }

        void clear() {
            for (Coord coord : Coord.values()) {
                board[coord.index()] = Stone.EMPTY;
            }
            board[Coord.valueOf(4, 4).index()] = Stone.WHITE;
            board[Coord.valueOf(5, 4).index()] = Stone.BLACK;
            board[Coord.valueOf(4, 5).index()] = Stone.BLACK;
            board[Coord.valueOf(5, 5).index()] = Stone.WHITE;
            stones[Stone.EMPTY.ordinal()] = 60;
            stones[Stone.BLACK.ordinal()] = 2;
            stones[Stone.WHITE.ordinal()] = 2;
        }

        void put(Coord coord, Color color) {
            if (get(coord) == Stone.EMPTY) {
                --stones[Stone.EMPTY.ordinal()]; // 空白を減らす
            } else {
                --stones[color.opponentValue().ordinal()]; // 相手石を減らす
            }
            board[coord.index()] = color.value();
            stones[color.value().ordinal()]++; // 自石を増やす
        }

        public int getEmptyStones() {
            return stones[Stone.EMPTY.ordinal()];
        }

        public int getBlackStones() {
            return stones[Stone.BLACK.ordinal()];
        }

        public int getWhiteStones() {
            return stones[Stone.WHITE.ordinal()];
        }

        public Stone get(int x, int y) {
            return get(Coord.valueOf(x, y));
        }

        public Stone get(Coord coord) {
            if (coord == Coord.OUT_OF_BOUNDS) {
                return Stone.BORDER;
            }
            return board[coord.index()];
        }
    }

    public enum Stone {
        EMPTY("."),
        BLACK("@"),
        WHITE("O"),
        BORDER("#");

        private final String display;

        Stone(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    public enum Color {
        Black(Stone.BLACK, "black"),
        White(Stone.WHITE, "white");

        private final Stone value;
        private final String display;

        Color(Stone value, String display) {
            this.value = value;
            this.display = display;
        }

        /**
         * 相手の手番を返す
         * Black->White、White->Black
         */
        private Color opponentColor() {
            return this == Black ? White : Black;
        }

        public Stone value() {
            return value;
        }

        public Stone opponentValue() {
            return opponentColor().value();
        }

        @Override
        public String toString() {
            return display;
        }
    }

    public static final class Coord {

        public static final Coord OUT_OF_BOUNDS;

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
            OUT_OF_BOUNDS = new Coord(-1, -1, "OB");
            values = new Coord[COLUMNS * ROWS];
            int i = 0;
            for (int y = 1; y <= 8; y++) {
                for (int x = 1; x <= 8; x++) {
                    String symbol = String.format("%c%d", 'A' + (x - 1), y);
                    values[i++] = new Coord(x, y, symbol);
                }
            }
        }

        public int index() {
            if (this == OUT_OF_BOUNDS) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return (y - 1) * 8 + (x - 1);
        }

        public Coord move(Direction dir) {
            if (this == OUT_OF_BOUNDS) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return valueOf(x + dir.dx, y + dir.dy);
        }

        public Coord flipUpDn() {
            if (this == OUT_OF_BOUNDS) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return valueOf(x, 9 - y);
        }

        public Coord flipLtRt() {
            if (this == OUT_OF_BOUNDS) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return valueOf(9 - x, y);
        }

        public Coord flip() {
            if (this == OUT_OF_BOUNDS) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return valueOf(9 - x, 9 - y);
        }

        public Coord transposed() {
            if (this == OUT_OF_BOUNDS) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return valueOf(y, x);
        }

        public Coord flipLtRtTransposed() {
            if (this == OUT_OF_BOUNDS) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return valueOf(y, 9 - x);
        }

        public Coord flipUpDnTransposed() {
            if (this == OUT_OF_BOUNDS) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return valueOf(9 - y, x);
        }

        public Coord flipTransposed() {
            if (this == OUT_OF_BOUNDS) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return valueOf(9 - y, 9 - x);
        }

        public boolean isCircum() {
            return x == 1 || x == 8 || y == 1 || y == 8;
        }

        public boolean idEdge() {
            return false; // TODO
        }

        public boolean isCorner() {
            return false; // TODO
        }

        public static Coord[] values() {
            return values;
        }

        public static Coord valueOf(int x, int y) {
            if (x < 1 || COLUMNS < x || y < 1 || ROWS < y) {
                return OUT_OF_BOUNDS;
            }
            return values[(y - 1) * 8 + (x - 1)];
        }

        public static Coord valueOf(int index) {
            if (0 <= index && index < values.length) {
                return values[index];
            }
            throw new ArrayIndexOutOfBoundsException();
        }

        public static Coord valueOf(long coord) {
            int index = Bits.indexOf(coord);
            return valueOf(index);
        }

        public static Coord valueOf(String symbol) {
            return null; // TODO
        }

        public static long toCoords(List<Coord> moves) {
            long coords = 0L;
            for (Coord move : moves) {
                coords |= Bits.coordAt(move.x - 1, move.y - 1);
            }
            return coords;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    public static final class Direction {

        public final int dx;
        public final int dy;

        private Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        private static Direction[] values;
        private static Direction[] crossValues;

        static {
            values = new Direction[]{
                    new Direction(-1, 0),
                    new Direction(-1, -1),
                    new Direction(0, -1),
                    new Direction(1, -1),
                    new Direction(1, 0),
                    new Direction(1, 1),
                    new Direction(0, 1),
                    new Direction(-1, 1),
            };
            crossValues = new Direction[]{
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
