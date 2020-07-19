package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.feature.Feature;
import com.github.koriel50000.prelude.feature.PreludeFeature;
import com.github.koriel50000.prelude.feature.RandomFeature;
import com.github.koriel50000.prelude.feature.ReferenceFeature;
import com.github.koriel50000.prelude.reversi.BitBoard;
import com.github.koriel50000.prelude.reversi.Bits;
import com.github.koriel50000.prelude.reversi.Board;
import com.github.koriel50000.prelude.reversi.LineBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlayMain {

    private Board board;
    private BitBoard bitBoard;

    private PlayMain() {
        board = new Board();
        bitBoard = new BitBoard();
    }

    private void oneplay() {
        long seed = System.currentTimeMillis();
        Feature preludeFeature = new PreludeFeature(board, seed);
        Feature randomFeature = new RandomFeature(seed);
        preludeFeature.init();
        randomFeature.init();

        play(preludeFeature, randomFeature);

        preludeFeature.destroy();
        randomFeature.destroy();
    }

    /**
     * ゲームを開始する
     */
    private void play(Feature blackFeature, Feature whiteFeature) {
        board.initialize();
        bitBoard.initialize();

        LineBuffer buffer = new LineBuffer();

        while (true) {
            bitBoard.printBoard(buffer.offset(0));
            bitBoard.printStatus(buffer.offset(0));
            board.printBoard(buffer.offset(30));
            board.printStatus(buffer.offset(30));
            buffer.flush();

            boolean passed = false;
            // Assert
            boolean blackTurn = bitBoard.currentColor == BitBoard.BLACK;
            boolean expectedBlackTurn = board.getCurrentColor() == Board.Color.Black;
            assertEquals(expectedBlackTurn, blackTurn,  "currentColor");

            if (blackTurn) {
                // Assert
                long coords = bitBoard.availableMoves(bitBoard.blackBoard, bitBoard.whiteBoard);
                long expectedCoords = toCoords(board.availableMoves());
                try {
                    assertEquals(expectedCoords, coords, "availableMoves");
                } catch (AssertionError e) {
                    Bits.printMatrix(expectedCoords);
                    Bits.printMatrix(coords);
                    throw e;
                }

                if (coords != 0) {
                    long coord = blackFeature.evaluate(0L, 0L, coords);

                    bitBoard.makeMove(bitBoard.blackBoard, bitBoard.whiteBoard, coord);
                    board.makeMove(Board.Coord.valueOf(Bits.indexOf(coord)));
                } else {
                    System.out.println("Pass!");
                    passed = true;
                }
            } else {
                // Assert
                long coords = bitBoard.availableMoves(bitBoard.whiteBoard, bitBoard.blackBoard);
                long expectedCoords = toCoords(board.availableMoves());
                try {
                    assertEquals(expectedCoords, coords, "availableMoves");
                } catch (AssertionError e) {
                    Bits.printMatrix(expectedCoords);
                    Bits.printMatrix(coords);
                    throw e;
                }

                if (coords != 0) {
                    long coord = whiteFeature.evaluate(0L, 0L, coords);

                    bitBoard.makeMove(bitBoard.whiteBoard, bitBoard.blackBoard, coord);
                    board.makeMove(Board.Coord.valueOf(Bits.indexOf(coord)));
                } else {
                    System.out.println("Pass!");
                    passed = true;
                }
            }

            // Assert
            boolean completed = bitBoard.hasCompleted(passed);
            boolean expectedCompleted = board.hasCompleted();
            assertEquals(expectedCompleted, completed, "hasCompleted");

            // ゲーム終了を判定
            if (completed) {
                break;
            }

            bitBoard.nextTurn(passed);
            board.nextTurn();
        }

        bitBoard.printBoard(buffer.offset(0));
        bitBoard.printScore(buffer.offset(0));
        board.printBoard(buffer.offset(30));
        board.printScore(buffer.offset(30));
        buffer.flush();
    }

    private long toCoords(List<Board.Coord> moves) {
        long coords = 0L;
        for (Board.Coord move : moves) {
            coords |= Bits.coordAt(move.x - 1, move.y - 1);
        }
        return coords;
    }

    private void assertEquals(Object expected, Object actual, String message) {
        assert expected.equals(actual) : String.format("'%s' not match. %s %s", message, expected, actual);
    }

    public static void main(String[] args) {
        PlayMain main = new PlayMain();
        main.oneplay();
    }
}
