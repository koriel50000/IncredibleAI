package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.feature.Feature;
import com.github.koriel50000.prelude.feature.PreludeFeature;
import com.github.koriel50000.prelude.feature.RandomFeature;
import com.github.koriel50000.prelude.reversi.BitBoard;
import com.github.koriel50000.prelude.reversi.Board;

import java.util.List;
import java.util.Random;

public class PlayMain {

    public static void main(String[] args) {
        PlayMain main = new PlayMain();
        main.oneplay();
    }

    private void oneplay() {
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
        Random random = new Random(System.currentTimeMillis());

        board.initialize();

        while (true) {
            board.printBoard();
            board.printStatus();

            long[] moves = board.availableMoves();
            boolean passed = false;
            if (moves.length > 0) {
                long move;
                if (board.currentColor == BitBoard.BLACK) {
                    move = moves[random.nextInt(moves.length)]; //blackFeature.evaluate(moves);
                } else {
                    move = moves[random.nextInt(moves.length)]; //whiteFeature.evaluate(moves);
                }
                board.makeMove(move);
            } else{
                System.out.println("Pass!");
                passed = true;
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
