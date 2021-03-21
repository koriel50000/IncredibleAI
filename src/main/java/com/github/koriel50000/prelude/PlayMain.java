package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.learning.PreludeFeature;
import com.github.koriel50000.prelude.op.Operator;
import com.github.koriel50000.prelude.op.PreludeOperator;
import com.github.koriel50000.prelude.op.RandomOperator;
import com.github.koriel50000.prelude.reversi.BitBoard;
import com.github.koriel50000.prelude.reversi.Bits;
import com.github.koriel50000.prelude.reversi.LineBuffer;
import com.github.koriel50000.prelude.reversi.Reversi;

import java.util.List;

import static com.github.koriel50000.prelude.reversi.Reversi.Color;
import static com.github.koriel50000.prelude.reversi.Reversi.Coord;

public class PlayMain {

    private BitBoard bitBoard;
    private Reversi reversi;
    private PreludeFeature feature;

    private PlayMain() {
        bitBoard = new BitBoard();
        reversi = new Reversi();
        feature = new PreludeFeature();
    }

    private void oneplay() {
        long seed = System.currentTimeMillis();
        Operator preludeOperator = new PreludeOperator(
                bitBoard, reversi, feature, seed);
        Operator randomOperator = new RandomOperator(seed);
        preludeOperator.init();
        randomOperator.init();

        play(preludeOperator, randomOperator);

        preludeOperator.destroy();
        randomOperator.destroy();
    }

    /**
     * ゲームを開始する
     */
    private void play(Operator blackOperator, Operator whiteOperator) {
        bitBoard.clear();
        reversi.clear();
        feature.clear();

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

            long playerBoard = blackTurn ? bitBoard.blackBoard : bitBoard.whiteBoard;
            long opponentBoard = blackTurn ? bitBoard.whiteBoard : bitBoard.blackBoard;
            Operator operator = blackTurn ? blackOperator : whiteOperator;

            // Assert
            long coords = bitBoard.availableMoves(playerBoard, opponentBoard);
            long expectedCoords = Coord.toCoords(reversi.availableMoves());
            try {
                assertEquals(expectedCoords, coords, "availableMoves");
            } catch (AssertionError e) {
                LineBuffer errorBuffer = new LineBuffer();
                errorBuffer.offset(0);
                errorBuffer.println("expected");
                Bits.printMatrix(errorBuffer, expectedCoords);
                errorBuffer.offset(15);
                errorBuffer.println("actual");
                Bits.printMatrix(errorBuffer, coords);
                errorBuffer.flush();
                throw e;
            }

            if (coords == 0) {
                System.out.println("Pass!");
                passed = true;
            } else {
                long coord = operator.evaluate(playerBoard, opponentBoard, coords);
                bitBoard.makeMove(playerBoard, opponentBoard, coord);
                Coord coord_ = Coord.valueOf(coord);
                List<Coord> flipped = reversi.makeMove(coord_);
                feature.increaseFlipped(coord_, flipped);
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
