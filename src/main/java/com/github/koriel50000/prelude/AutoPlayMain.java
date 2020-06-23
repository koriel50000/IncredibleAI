package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.feature.Feature;
import com.github.koriel50000.prelude.feature.RandomFeature;
import com.github.koriel50000.prelude.feature.ReferenceFeature;
import com.github.koriel50000.prelude.reversi.Board;

import java.util.List;

public class AutoPlayMain {

    public static void main(String[] args) {
        AutoPlayMain main = new AutoPlayMain();
        main.autoplay();
    }

    private void autoplay() {
        Board board = new Board();

        Feature referenceFeagure = new ReferenceFeature(board);
        Feature randomFeature = new RandomFeature();
        referenceFeagure.init();
        randomFeature.init();

        int win = 0;
        int loss = 0;
        int draw = 0;
        int winStones = 0;
        int lossStones = 0;
        Board.Score score;

        for (int i = 0; i < 50; i++) {
            score = play(board, referenceFeagure, randomFeature);

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

            score = play(board, randomFeature, referenceFeagure);

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
    private Board.Score play(Board board, Feature blackFeature, Feature whiteFeature) {
        board.initialize();

        while (true) {
            Board.Color color = board.getCurrentColor();

            List<Board.Coord> moves = board.availableMoves();
            if (moves.size() > 0) {
                Board.Coord move;
                if (color == Board.Color.Black) {
                    move = blackFeature.evaluate(moves);
                } else {
                    move = whiteFeature.evaluate(moves);
                }
                board.makeMove(move);
            }

            // ゲーム終了を判定
            if (board.hasCompleted()) {
                break;
            }
            board.nextTurn();
        }

        return board.getScore();
    }
}
