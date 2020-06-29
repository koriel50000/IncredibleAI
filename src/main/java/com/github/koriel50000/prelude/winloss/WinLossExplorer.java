package com.github.koriel50000.prelude.winloss;

import com.github.koriel50000.prelude.reversi.Board;

import java.util.List;

public class WinLossExplorer {

    private Board board;

    public WinLossExplorer(Board board) {
        this.board = board;
    }

    public boolean notPossible() {
        // 勝敗探索ができないならばtrue
        // falseの場合も、制限時間内に勝敗が判定するかは未確定
        // FIXME
        return board.getTurnCount() < 50;
    }

    private Board.Color currentColor;

    public long explore(long playerBoard, long opponentBoard, long moves) {
        currentColor = board.getCurrentColor();
        long optimumMove = 0;

        int maxValue = Integer.MIN_VALUE;
        while (moves != 0) {
            long coord = moves & -moves;  // 一番右のビットのみ取り出す

            Board nextBoard = board.tryMove(coord);
            int value = negamax(nextBoard, 1);
            if (value > maxValue) {
                maxValue = value;
                optimumMove = coord;
            }

            moves ^= coord;  // 一番右のビットを0にする
        }
        return optimumMove;
    }

    private int negamax(Board board, int color) {
        if (board.hasCompleted()) {
            Board.Score score = board.getScore();
            if (color > 0) { // FIXME 手番の判定
                return score.getBlackStones() - score.getWhiteStones();
            } else {
                return score.getWhiteStones() - score.getBlackStones();
            }
        }
        board.nextTurn();

        List<Board.Coord> moves = board.availableMoves();
        if (moves.size() == 0) {
            board.nextTurn(); // FIXME 終了判定は？
            return -negamax(board, -color);
        }

        int maxValue = Integer.MIN_VALUE;
        for (Board.Coord move : moves) {
            Board nextBoard = board.tryMove(0); // FIXME
            int value = -negamax(nextBoard, -color);
            if (value > maxValue) {
                maxValue = value;
            }
        }
        return maxValue;
    }
}
