package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.feature.Feature;
import com.github.koriel50000.prelude.feature.PreludeFeature;
import com.github.koriel50000.prelude.feature.RandomFeature;
import com.github.koriel50000.prelude.reversi.Board;

import java.util.List;

public class PlayMain {

    public static void main(String[] args) {
        PlayMain main = new PlayMain();
        main.oneplay();
    }

    private void oneplay() {
        Board board = new Board();

        Feature preludeFeature = new PreludeFeature(board);
        Feature randomFeature = new RandomFeature();
        preludeFeature.init();
        randomFeature.init();

        play(board, preludeFeature, randomFeature);

        preludeFeature.destroy();
        randomFeature.destroy();
    }

    /**
     * ゲームを開始する
     */
    private void play(Board board, Feature blackFeature, Feature whiteFeature) {
        board.initialize();

        while (true) {
            Board.Color color = board.getCurrentColor();

            board.printBoard();
            board.printStatus();

            List<Board.Coord> moves = board.availableMoves();
            if (moves.size() > 0) {
                Board.Coord move;
                if (color == Board.Color.Black) {
                    move = blackFeature.evaluate(moves);
                } else {
                    move = whiteFeature.evaluate(moves);
                }
                board.makeMove(move);
            } else{
                System.out.println("Pass!");
            }

            // ゲーム終了を判定
            if (board.hasCompleted()) {
                break;
            }

            board.nextTurn();
        }

        board.printBoard();
        board.printScore();
    }
}
