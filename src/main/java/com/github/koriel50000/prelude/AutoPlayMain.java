package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.feature.Feature;
import com.github.koriel50000.prelude.feature.RandomFeature;
import com.github.koriel50000.prelude.feature.ReferenceFeature;
import com.github.koriel50000.prelude.reversi.BitBoard;
import com.github.koriel50000.prelude.reversi.Score;

public class AutoPlayMain {

    public static void main(String[] args) {
        AutoPlayMain main = new AutoPlayMain();
        main.autoplay();
    }

    private void autoplay() {
        BitBoard board = new BitBoard();

        long seed = System.currentTimeMillis();
        Feature referenceFeagure = new ReferenceFeature(board, seed);
        Feature randomFeature = new RandomFeature(seed);
        referenceFeagure.init();
        randomFeature.init();

        int win = 0;
        int loss = 0;
        int draw = 0;
        int winStones = 0;
        int lossStones = 0;
        Score score;

        for (int i = 0; i < 50; i++) {
            score = play(board, randomFeature, randomFeature);

            switch (score.getWinner()) {
                case "black":
                    win += 1;
                    winStones += score.getBlackStones();
                    lossStones += score.getWhiteStones();
                    break;
                case "white":
                    loss += 1;
                    winStones += score.getWhiteStones();
                    lossStones += score.getBlackStones();
                    break;
                case "draw":
                    draw += 1;
                    winStones += score.getBlackStones();
                    lossStones += score.getWhiteStones();
                    break;
            }

            score = play(board, randomFeature, randomFeature);

            switch (score.getWinner()) {
                case "black":
                    loss += 1;
                    winStones += score.getBlackStones();
                    lossStones += score.getWhiteStones();
                    break;
                case "white":
                    win += 1;
                    winStones += score.getWhiteStones();
                    lossStones += score.getBlackStones();
                    break;
                case "draw":
                    draw += 1;
                    winStones += score.getWhiteStones();
                    lossStones += score.getBlackStones();
                    break;
            }
        }

        System.out.println(String.format("win:%d loss:%d draw:%d", win, loss, draw));
        System.out.println(String.format("win_stone:%d loss_stone:%d", winStones, lossStones));

        referenceFeagure.destroy();
        randomFeature.destroy();
    }

    /**
     * ゲームを開始する
     */
    private Score play(BitBoard board, Feature blackFeature, Feature whiteFeature) {
        board.initialize();

        while (true) {
            boolean passed = false;
            if (board.currentColor == BitBoard.BLACK) {
                long coords = board.availableMoves(board.blackBoard, board.whiteBoard);
                if (coords != 0) {
                    long move = blackFeature.evaluate(board.blackBoard, board.whiteBoard, coords);
                    long flipped = board.makeMove(board.blackBoard, board.whiteBoard, move);
                } else {
                    passed = true;
                }
            } else {
                long coords = board.availableMoves(board.whiteBoard, board.blackBoard);
                if (coords != 0) {
                    long move = whiteFeature.evaluate(board.whiteBoard, board.blackBoard, coords);
                    board.makeMove(board.whiteBoard, board.blackBoard, move);
                } else {
                    passed = true;
                }
            }

            // ゲーム終了を判定
            if (board.hasCompleted(passed)) {
                break;
            }
            board.nextTurn(passed);
        }

        return board.getScore();
    }
}
