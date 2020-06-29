package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.feature.Feature;
import com.github.koriel50000.prelude.feature.PreludeFeature;
import com.github.koriel50000.prelude.feature.RandomFeature;
import com.github.koriel50000.prelude.reversi.BitBoard;
import com.github.koriel50000.prelude.reversi.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlayMain {

    public static void main(String[] args) {
        PlayMain main = new PlayMain();
        main.oneplay();
    }

    private Random random;

    private void oneplay() {
        random = new Random(System.currentTimeMillis());

        BitBoard board = new BitBoard();

        Feature randomFeature = new RandomFeature();
        randomFeature.init();

        play(board, randomFeature, randomFeature);

        randomFeature.destroy();
    }

    /**
     * ゲームを開始する
     */
    private void play(BitBoard board, Feature blackFeature, Feature whiteFeature) {
        board.initialize();

        while (true) {
            board.printBoard();
            board.printStatus();

            boolean passed = false;
            if (board.currentColor == BitBoard.BLACK) {
                long moves = board.availableMoves(board.blackBoard, board.whiteBoard);
                if (moves != 0) {
                    long move = blackFeature.evaluate(board.blackBoard, board.whiteBoard, moves);
                    board.makeMove(board.blackBoard, board.whiteBoard, move);
                } else {
                    System.out.println("Pass!");
                    passed = true;
                }
            } else {
                long moves = board.availableMoves(board.whiteBoard, board.blackBoard);
                if (moves != 0) {
                    long move = whiteFeature.evaluate(board.whiteBoard, board.blackBoard, moves);
                    board.makeMove(board.whiteBoard, board.blackBoard, move);
                } else {
                    System.out.println("Pass!");
                    passed = true;
                }
            }

            // ゲーム終了を判定
            if (board.hasCompleted(passed)) {
                break;
            }

            board.nextTurn(passed);
        }

        board.printBoard();
        board.printScore();
    }
}
