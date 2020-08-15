package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.feature.Feature;
import com.github.koriel50000.prelude.feature.PreludeFeature;
import com.github.koriel50000.prelude.feature.RandomFeature;
import com.github.koriel50000.prelude.reversi.BitBoard;
import com.github.koriel50000.prelude.reversi.Bits;
import com.github.koriel50000.prelude.reversi.LineBuffer;
import com.github.koriel50000.prelude.reversi.Reversi;

import static com.github.koriel50000.prelude.reversi.Reversi.Color;
import static com.github.koriel50000.prelude.reversi.Reversi.Coord;

public class PlayMain {

    private BitBoard bitBoard;
    private Reversi reversi;

    private PlayMain() {
        bitBoard = new BitBoard();
        reversi = new Reversi();
    }

    private void oneplay() {
        long seed = System.currentTimeMillis();
        Feature preludeFeature = new PreludeFeature(bitBoard, reversi, seed);
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
        bitBoard.clear();
        reversi.clear();

        LineBuffer buffer = new LineBuffer();

        while (true) {
            bitBoard.printBoard(buffer.offset(0));
            bitBoard.printStatus(buffer.offset(0));
            reversi.printBoard(buffer.offset(30));
            reversi.printStatus(buffer.offset(30));
            buffer.flush();

            boolean passed = false;
            // Assert
            boolean blackTurn = bitBoard.currentColor == BitBoard.BLACK;
            boolean expectedBlackTurn = reversi.getCurrentColor() == Color.Black;
            assertEquals(expectedBlackTurn, blackTurn, "currentColor");

            if (blackTurn) {
                // Assert
                long coords = bitBoard.availableMoves(bitBoard.blackBoard, bitBoard.whiteBoard);
                long expectedCoords = Coord.toCoords(reversi.availableMoves());
                try {
                    assertEquals(expectedCoords, coords, "availableMoves");
                } catch (AssertionError e) {
                    Bits.printMatrix(expectedCoords);
                    Bits.printMatrix(coords);
                    throw e;
                }

                if (coords != 0) {
                    long coord = blackFeature.evaluate(bitBoard.blackBoard, bitBoard.whiteBoard, coords);

                    bitBoard.makeMove(bitBoard.blackBoard, bitBoard.whiteBoard, coord);
                    reversi.makeMove(Coord.valueOf(coord));
                } else {
                    System.out.println("Pass!");
                    passed = true;
                }
            } else {
                // Assert
                long coords = bitBoard.availableMoves(bitBoard.whiteBoard, bitBoard.blackBoard);
                long expectedCoords = Coord.toCoords(reversi.availableMoves());
                try {
                    assertEquals(expectedCoords, coords, "availableMoves");
                } catch (AssertionError e) {
                    Bits.printMatrix(expectedCoords);
                    Bits.printMatrix(coords);
                    throw e;
                }

                if (coords != 0) {
                    long coord = whiteFeature.evaluate(bitBoard.whiteBoard, bitBoard.blackBoard, coords);

                    bitBoard.makeMove(bitBoard.whiteBoard, bitBoard.blackBoard, coord);
                    reversi.convertState(Coord.valueOf(coord));
                    reversi.makeMove(Coord.valueOf(coord));
                } else {
                    System.out.println("Pass!");
                    passed = true;
                }
            }

            // Assert
            boolean completed = bitBoard.hasCompleted(passed);
            boolean expectedCompleted = reversi.hasCompleted(passed);
            assertEquals(expectedCompleted, completed, "hasCompleted");

            // ゲーム終了を判定
            if (completed) {
                break;
            }

            bitBoard.nextTurn(passed);
            reversi.nextTurn(passed);
        }

        bitBoard.printBoard(buffer.offset(0));
        bitBoard.printScore(buffer.offset(0));
        reversi.printBoard(buffer.offset(30));
        reversi.printScore(buffer.offset(30));
        buffer.flush();
    }

    private void assertEquals(Object expected, Object actual, String message) {
        assert expected.equals(actual) : String.format("'%s' not match. %s %s", message, expected, actual);
    }

    public static void main(String[] args) {
        PlayMain main = new PlayMain();
        main.oneplay();
    }
}
