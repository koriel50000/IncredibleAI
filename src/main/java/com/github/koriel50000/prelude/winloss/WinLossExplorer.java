package com.github.koriel50000.prelude.winloss;

import com.github.koriel50000.prelude.Reversi;

import java.util.List;

public class WinLossExplorer {

    private Reversi reversi;

    public WinLossExplorer(Reversi reversi) {
        this.reversi = reversi;
    }

    public boolean notPossible() {
        // 勝敗探索ができないならばtrue
        // falseの場合も、制限時間内に勝敗が判定するかは未確定
        // FIXME
        return reversi.getTurnCount() < 50;
    }

    private Reversi.Turn currentTurn;

    public Reversi.Coord explore(List<Reversi.Coord> moves) {
        currentTurn = reversi.getCurrentTurn();
        Reversi.Coord optimumMove = null;

        int maxValue = Integer.MIN_VALUE;
        for (Reversi.Coord move : moves) {
            Reversi nextBoard = reversi.tryMove(move);
            int value = negamax(nextBoard, 1);
            if (value > maxValue) {
                maxValue = value;
                optimumMove = move;
            }
        }
        return optimumMove;
    }

    private int negamax(Reversi board, int color) {
        if (board.hasCompleted()) {
            Reversi.Score score = board.getScore();
            if (color > 0) {
                return score.getBlackStones() - score.getWhiteStones();
            } else {
                return score.getWhiteStones() - score.getBlackStones();
            }
        }
        board.nextTurn();

        List<Reversi.Coord> moves = board.availableMoves();
        if (moves.size() == 0) {
            board.nextTurn();
            return -negamax(board, -color);
        }

        int maxValue = Integer.MIN_VALUE;
        for (Reversi.Coord move : moves) {
            Reversi nextBoard = board.tryMove(move);
            int value = -negamax(nextBoard, -color);
            if (value > maxValue) {
                maxValue = value;
            }
        }
        return maxValue;
    }
}
