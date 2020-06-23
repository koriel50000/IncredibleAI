package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.feature.Feature;
import com.github.koriel50000.prelude.feature.RandomFeature;
import com.github.koriel50000.prelude.feature.ReferenceFeature;

import java.util.List;

public class AutoPlayMain {

    public static void main(String[] args) {
        AutoPlayMain main = new AutoPlayMain();
        main.autoplay();
    }

    private void autoplay() {
        Reversi reversi = new Reversi();

        Feature referenceFeagure = new ReferenceFeature(reversi);
        Feature randomFeature = new RandomFeature();
        referenceFeagure.init();
        randomFeature.init();

        int win = 0;
        int loss = 0;
        int draw = 0;
        int winStones = 0;
        int lossStones = 0;
        Reversi.Score score;

        for (int i = 0; i < 50; i++) {
            score = play(reversi, referenceFeagure, randomFeature);

            switch (score.getWinner()) {
                case Black:
                    win += 1;
                    winStones += score.getBlackStones();
                    lossStones += score.getWhiteStones();
                    break;
                case White:
                    loss += 1;
                    winStones += score.getWhiteStones();
                    lossStones += score.getBlackStones();
                    break;
                case Draw:
                    draw += 1;
                    winStones += score.getBlackStones();
                    lossStones += score.getWhiteStones();
                    break;
            }

            score = play(reversi, randomFeature, referenceFeagure);

            switch (score.getWinner()) {
                case Black:
                    loss += 1;
                    winStones += score.getBlackStones();
                    lossStones += score.getWhiteStones();
                    break;
                case White:
                    win += 1;
                    winStones += score.getWhiteStones();
                    lossStones += score.getBlackStones();
                    break;
                case Draw:
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
    private Reversi.Score play(Reversi reversi, Feature blackFeature, Feature whiteFeature) {
        reversi.initialize();

        while (true) {
            Reversi.Turn turn = reversi.getCurrentTurn();

            List<Reversi.Coord> moves = reversi.availableMoves();
            if (moves.size() > 0) {
                Reversi.Coord move;
                if (turn == Reversi.Turn.Black) {
                    move = blackFeature.evaluate(moves);
                } else {
                    move = whiteFeature.evaluate(moves);
                }
                reversi.makeMove(move);
            }

            // ゲーム終了を判定
            if (reversi.hasCompleted()) {
                break;
            }
            reversi.nextTurn();
        }

        return reversi.getScore();
    }
}
