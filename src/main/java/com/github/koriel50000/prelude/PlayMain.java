package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.feature.Feature;
import com.github.koriel50000.prelude.feature.PreludeFeature;
import com.github.koriel50000.prelude.feature.RandomFeature;

import java.util.List;

public class PlayMain {

    public static void main(String[] args) {
        PlayMain main = new PlayMain();

        Feature preludeFeature = new PreludeFeature();
        Feature randomFeature = new RandomFeature();
        preludeFeature.init();
        randomFeature.init();

        main.play(preludeFeature, randomFeature);

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

            reversi.printBoard();
            reversi.printStatus();

            List<Reversi.Coord> moves = reversi.availableMoves();
            if (moves.size() > 0) {
                Reversi.Coord move;
                if (turn == Reversi.Turn.Black) {
                    move = blackFeature.evaluate(reversi, moves);
                } else {
                    move = whiteFeature.evaluate(reversi, moves);
                }
                reversi.makeMove(move);
            } else{
                System.out.println("Pass!");
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
