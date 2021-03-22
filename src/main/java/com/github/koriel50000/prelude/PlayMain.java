package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.learning.BitFeature;
import com.github.koriel50000.prelude.learning.PreludeFeature;
import com.github.koriel50000.prelude.op.Operator;
import com.github.koriel50000.prelude.op.PreludeOperator;
import com.github.koriel50000.prelude.op.RandomOperator;
import com.github.koriel50000.prelude.reversi.BitBoard;
import com.github.koriel50000.prelude.util.Bits;
import com.github.koriel50000.prelude.util.LineBuffer;
import com.github.koriel50000.prelude.reversi.Reversi;

import java.util.List;

import static com.github.koriel50000.prelude.reversi.Reversi.Color;
import static com.github.koriel50000.prelude.reversi.Reversi.Coord;

public class PlayMain {

    private BitBoard bitBoard;
    private BitFeature bitFeature;
    private Reversi reversi;
    private PreludeFeature feature;

    private PlayMain() {
        bitBoard = new BitBoard();
        bitFeature = new BitFeature();
        reversi = new Reversi();
        feature = new PreludeFeature();
    }

    private void oneplay() {
        long seed = System.currentTimeMillis();
        Operator preludeOperator = new PreludeOperator(
                bitBoard, bitFeature, reversi, feature, seed);
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
        bitFeature.clear();
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

            long player = blackTurn ? bitBoard.blackBoard : bitBoard.whiteBoard;
            long opponent = blackTurn ? bitBoard.whiteBoard : bitBoard.blackBoard;
            Operator operator = blackTurn ? blackOperator : whiteOperator;

            // Assert
            long coords = bitBoard.availableMoves(player, opponent);
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
                long coord = operator.evaluate(player, opponent, coords);

                int index = Bits.indexOf(coord);
                long flipped = bitBoard.computeFlipped(player, opponent, index);
                bitBoard.makeMove(flipped, coord);
                bitFeature.setState(player, opponent, flipped, coord, index);

                Coord coord_ = Coord.valueOf(coord);
                List<Coord> flipped_ = reversi.makeMove(coord_);
                feature.increaseFlipped(coord_, flipped_);
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
