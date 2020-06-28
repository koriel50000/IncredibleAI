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

    private long evaluate(long moves) {
        List<Long> moveList = new ArrayList<>();
        while (moves != 0) {
            long coord = moves & -moves;  // 一番右のビットのみ取り出す
            moveList.add(coord);
            moves ^= coord;  // 一番右のビットを0にする
        }
        long move = moveList.get(random.nextInt(moveList.size()));
        return move;
    }

    /**
     * ゲームを開始する
     */
    private void play(BitBoard board, Feature blackFeature, Feature whiteFeature) {
        board.initialize();

        while (true) {
            board.printBoard();
            board.printStatus();

            long moves = board.availableMoves();
            boolean passed = false;
            if (moves != 0) {
                long move;
                if (board.currentColor == BitBoard.BLACK) {
                    move = evaluate(moves); //blackFeature.evaluate(moves);
                } else {
                    move = evaluate(moves); //whiteFeature.evaluate(moves);
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
