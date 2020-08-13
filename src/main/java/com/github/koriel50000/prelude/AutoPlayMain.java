package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.feature.Feature;
import com.github.koriel50000.prelude.feature.RandomFeature;
import com.github.koriel50000.prelude.feature.ReferenceFeature;
import com.github.koriel50000.prelude.reversi.*;

import static com.github.koriel50000.prelude.reversi.Reversi.*;

public class AutoPlayMain {

    private BitBoard bitBoard;
    private Reversi reversi;

    private AutoPlayMain() {
        bitBoard = new BitBoard();
        reversi = new Reversi();
    }

    private void autoplay() {
        long seed = System.currentTimeMillis();
        Feature referenceFeature = new ReferenceFeature(bitBoard, reversi, seed);
        Feature randomFeature = new RandomFeature(seed);
        referenceFeature.init();
        randomFeature.init();

        int win = 0;
        int loss = 0;
        int draw = 0;
        int winStones = 0;
        int lossStones = 0;
        Score score;

        for (int i = 0; i < 50; i++) {
            score = play(referenceFeature, randomFeature);

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

            score = play(randomFeature, referenceFeature);

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

        referenceFeature.destroy();
        randomFeature.destroy();
    }

    /**
     * ゲームを開始する
     */
    private Score play(Feature blackFeature, Feature whiteFeature) {
        bitBoard.clear();
        reversi.clear();

        LineBuffer buffer = new LineBuffer();

        while (true) {
            boolean passed = false;
            if (bitBoard.currentColor == BitBoard.BLACK) {
                long coords = bitBoard.availableMoves(bitBoard.blackBoard, bitBoard.whiteBoard);
                if (coords != 0) {
                    long coord = blackFeature.evaluate(bitBoard.blackBoard, bitBoard.whiteBoard, coords);
                    bitBoard.makeMove(bitBoard.blackBoard, bitBoard.whiteBoard, coord);
                    reversi.makeMove(Coord.valueOf(coord));
                } else {
                    passed = true;
                }
            } else {
                long coords = bitBoard.availableMoves(bitBoard.whiteBoard, bitBoard.blackBoard);
                if (coords != 0) {
                    long coord = whiteFeature.evaluate(bitBoard.whiteBoard, bitBoard.blackBoard, coords);
                    bitBoard.makeMove(bitBoard.whiteBoard, bitBoard.blackBoard, coord);
                    reversi.makeMove(Coord.valueOf(coord));
                } else {
                    passed = true;
                }
            }

            // ゲーム終了を判定
            boolean dummy = reversi.hasCompleted(passed);
            if (bitBoard.hasCompleted(passed)) {
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

        return bitBoard.getScore();
    }

    public static void main(String[] args) {
        AutoPlayMain main = new AutoPlayMain();
        main.autoplay();
    }
}
