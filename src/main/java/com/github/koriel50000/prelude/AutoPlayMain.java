package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.feature.Feature;
import com.github.koriel50000.prelude.feature.RandomFeature;
import com.github.koriel50000.prelude.feature.ReferenceFeature;
import com.github.koriel50000.prelude.reversi.BitBoard;

public class AutoPlayMain {

    public static void main(String[] args) {
        AutoPlayMain main = new AutoPlayMain();
        main.autoplay();
    }

    private void autoplay() {
        BitBoard board = new BitBoard();

        Feature referenceFeagure = new ReferenceFeature(board);
        Feature randomFeature = new RandomFeature();
        referenceFeagure.init();
        randomFeature.init();

        int win = 0;
        int loss = 0;
        int draw = 0;
        int winStones = 0;
        int lossStones = 0;
        BitBoard.Score score;

        for (int i = 0; i < 50; i++) {
            score = play(board, referenceFeagure, randomFeature);

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

            score = play(board, randomFeature, referenceFeagure);

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
    private BitBoard.Score play(BitBoard board, Feature blackFeature, Feature whiteFeature) {
        board.initialize();

        while (true) {
            boolean passed = false;
            if (board.currentColor == BitBoard.BLACK) {
                long moves = board.availableMoves(board.blackBoard, board.whiteBoard);
                if (moves != 0) {
                    long move = blackFeature.evaluate(board.blackBoard, board.whiteBoard, moves);
                    board.makeMove(board.blackBoard, board.whiteBoard, move);
                } else {
                    passed = true;
                }
            } else {
                long moves = board.availableMoves(board.whiteBoard, board.blackBoard);
                if (moves != 0) {
                    long move = blackFeature.evaluate(board.whiteBoard, board.blackBoard, moves);
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
