package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.feature.Feature;
import com.github.koriel50000.prelude.feature.PreludeFeature;
import com.github.koriel50000.prelude.feature.RandomFeature;

import java.util.List;

public class AutoPlayMain {

    public static void main(String[] args) {
        AutoPlayMain main = new AutoPlayMain();
        main.autoplay();
    }

    private void autoplay() {
        Feature preludeFeature = new PreludeFeature();
        Feature randomFeature = new RandomFeature();
        preludeFeature.init();
        randomFeature.init();

        for (int i = 0; i < 50; i++) {
            play(preludeFeature, randomFeature);
            play(randomFeature, preludeFeature);
        }

        preludeFeature.destroy();
        randomFeature.destroy();
    }

    /**
     * ゲームを開始する
     */
    private void play(Feature blackFeature, Feature whiteFeature) {
        Reversi reversi = new Reversi();
        reversi.initialize();

        while (true) {
            Reversi.Turn turn = reversi.getCurrentTurn();

            List<Reversi.Coord> moves = reversi.availableMoves();
            if (moves.size() > 0) {
                Reversi.Coord move;
                if (turn == Reversi.Turn.Black) {
                    move = blackFeature.evaluate(reversi, moves, turn);
                } else {
                    move = whiteFeature.evaluate(reversi, moves, turn);
                }
                reversi.makeMove(move);
            }

            // ゲーム終了を判定
            if (reversi.hasCompleted()) {
                break;
            }
            reversi.nextTurn();
        }

        reversi.printBoard();
        reversi.printScore();
    }
}
