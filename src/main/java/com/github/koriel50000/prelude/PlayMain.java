package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.feature.Feature;
import com.github.koriel50000.prelude.feature.PreludeFeature;
import com.github.koriel50000.prelude.feature.RandomFeature;

import java.util.List;

public class PlayMain {

    private String[] STONES = { ".", "@", "O" };

    public static void main(String[] args) {
        PlayMain main = new PlayMain();
        Feature blackFeature = new PreludeFeature();
        Feature whiteFeature = new RandomFeature();
        main.play(blackFeature, whiteFeature);
    }

    /**
     * ゲームを開始する
     */
    private void play(Feature blackFeature, Feature whiteFeature) {
        Reversi reversi = new Reversi();

        reversi.initialize();

        while (true) {
            printBoard(reversi.getBoard());
            printStatus(reversi.getTurnCount(),
                    reversi.getBlackCount(),
                    reversi.getWhiteCount(),
                    reversi.getCurrentTurn());

            List<Reversi.Move> moves = reversi.availableMoves();
            if (moves.size() > 0) {
                Reversi.Turn turn = reversi.getCurrentTurn();
                Reversi.Move move = (turn == Reversi.Turn.Black)
                        ? blackFeature.evaluate(reversi, moves, turn)
                        : whiteFeature.evaluate(reversi, moves, turn);
                reversi.makeMove(move);
            } else{
                System.out.println("Pass!");
            }

            // ゲーム終了を判定
            if (reversi.hasCompleted()) {
                printBoard(reversi.getBoard());
                printScore(reversi.getTurnCount(),
                        reversi.getBlackCount(),
                        reversi.getWhiteCount(),
                        reversi.getEmptyCount());
                break;
            }

            reversi.nextTurn();
        }
    }

    /**
     * 盤面を表示する
     */
    private void printBoard(int[][] board) {
        System.out.println();
        System.out.println("  A B C D E F G H");
        for (int y = 1; y <= 8; y++) {
            System.out.print(y);
            for (int x = 1; x <= 8; x++) {
                System.out.print(" " + STONES[board[y][x]]);
            }
            System.out.println();
        }
    }

    /**
     * 現在の状態を表示する
     */
    private void printStatus(int turnCount, int blackCount, int whiteCount, Reversi.Turn currentTurn) {
        String turn;
        String stone;
        if (currentTurn == Reversi.Turn.Black) {
            turn = "black";
            stone = STONES[Reversi.Turn.Black.boardValue()];
        } else {
            turn = "white";
            stone = STONES[Reversi.Turn.White.boardValue()];
        }
        System.out.print(String.format("move count:%d ", turnCount));
        System.out.println(String.format("move:%s(%s)", turn, stone));
        System.out.println(String.format("black:%d white:%d", blackCount, whiteCount));
        System.out.println();
    }

    /**
     * スコアを表示する
     */
    private void printScore(int turnCount, int blackCount, int whiteCount, int emptyCount) {
        System.out.print(String.format("move count:%d ", turnCount));
        String winner;
        if (blackCount > whiteCount) {
            winner = "black";
            blackCount += emptyCount;
        } else if (blackCount < whiteCount) {
            winner = "white";
            whiteCount += emptyCount;
        } else {
            winner = "draw";
        }
        System.out.println(String.format("winner:%s", winner));
        System.out.println(String.format("black:%d white:%d", blackCount, whiteCount));
        System.out.println();
    }
}
