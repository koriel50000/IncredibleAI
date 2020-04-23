package com.github.koriel50000.prelude;

import java.util.List;
import java.util.Random;

public class ReversiMain {

    private String[] STONES = { ".", "@", "O" };

    public static void main(String[] args) {
        ReversiMain main = new ReversiMain();
        main.play();
    }

    /**
     * ゲームを開始する
     */
    private void play() {
        Reversi reversi = new Reversi();
        Random rand = new Random(System.currentTimeMillis());

        reversi.initialize();

        while (true) {
            printBoard(reversi);
            printStatus(reversi);

            List<Reversi.Move> moves = reversi.availableMoves();
            if (moves.size() > 0) {
                Reversi.Move move = moves.get(rand.nextInt(moves.size()));
                reversi.makeMove(move);
            } else{
                System.out.println("Pass!");
            }

            // ゲーム終了を判定
            if (reversi.hasCompleted()) {
                printBoard(reversi);
                printScore(reversi);
                break;
            }

            reversi.nextTurn();
        }
    }

    /**
     * 盤面を表示する
     */
    private void printBoard(Reversi reversi) {
        System.out.println();
        System.out.println("  A B C D E F G H");
        for (int y = 1; y <= 8; y++) {
            System.out.print(y);
            for (int x = 1; x <= 8; x++) {
                System.out.print(" " + STONES[reversi.boardValue(x, y)]);
            }
            System.out.println();
        }
    }

    /**
     * 現在の状態を表示する
     */
    private void printStatus(Reversi reversi) {
        int turnCount = reversi.getTurnCount();
        int blackCount = reversi.getBlackCount();
        int whiteCount = reversi.getWhiteCount();
        String turn;
        String stone;
        if (reversi.getCurrentTurn() == Reversi.Turn.Black) {
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
    private void printScore(Reversi reversi) {
        int turnCount = reversi.getTurnCount();
        int blackCount = reversi.getBlackCount();
        int whiteCount = reversi.getWhiteCount();
        int emptyCount = reversi.getEmptyCount();
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
