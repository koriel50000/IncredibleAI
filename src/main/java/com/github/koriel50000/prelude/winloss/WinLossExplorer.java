package com.github.koriel50000.prelude.winloss;

import com.github.koriel50000.prelude.reversi.BitBoard;
import com.github.koriel50000.prelude.reversi.Bits;

public class WinLossExplorer {

    private BitBoard board;

    public WinLossExplorer(BitBoard board) {
        this.board = board;
    }

    /**
     * 勝敗探索ができないならばtrue
     * falseの場合も、制限時間内に勝敗が判定するかは未確定
     */
    public boolean notPossible() {
        // FIXME
        return board.depth > 10; // 残り10マスから探索開始
    }

    public long explore(long player, long opponent, long moves) {
        long optimumMove = 0;

        int maxValue = Integer.MIN_VALUE;
        while (moves != 0) {
            long coord = Bits.getRightmostBit(moves);  // 一番右のビットのみ取り出す

            int depth = board.depth;
            int value = negamax(player, opponent, depth, false);
            if (value > maxValue) {
                maxValue = value;
                optimumMove = coord;
            }

            moves ^= coord;  // 一番右のビットを0にする
        }
        return optimumMove;
    }

    private int negamax(long player, long opponent, int depth, boolean passedBefore) {
        if (depth == 0) {
            return Bits.populationCount(player) - Bits.populationCount(opponent);
        }

        long coords = board.availableMoves(player, opponent);
        if (coords == 0) {
            if (passedBefore) {
                int maxValue = Bits.populationCount(player) - Bits.populationCount(opponent);
                return (maxValue > 0) ? maxValue + depth : maxValue - depth;
            }
            return -negamax(opponent, player, depth, true);
        }

        int maxValue = Integer.MIN_VALUE;
        while (coords != 0) {
            long coord = Bits.getRightmostBit(coords);  // 一番右のビットのみ取り出す
            int pos = Bits.countTrailingZeros(coords);

            long flipped = board.tryMove(player, opponent, pos);
            int value = -negamax(opponent ^ flipped, player | coord | flipped, depth - 1, false);
            if (value > maxValue) {
                maxValue = value;
            }

            coords ^= coord;  // 一番右のビットを0にする
        }
        return maxValue;
    }
}
