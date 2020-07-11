package com.github.koriel50000.prelude.rollout;

import com.github.koriel50000.prelude.learning.BitConverter;
import com.github.koriel50000.prelude.reversi.BitBoard;
import com.github.koriel50000.prelude.reversi.Board;
import com.github.koriel50000.prelude.learning.CNNModel;
import com.github.koriel50000.prelude.learning.PreludeConverter;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RolloutPolicy {

    private BitBoard board;
    private BitConverter converter;
    private CNNModel model;
    private Random random;

    private volatile long lastCoord;

    public RolloutPolicy(BitBoard board) {
        this.board = board;
        converter = new BitConverter();
        model = new CNNModel();
        random = new Random(System.currentTimeMillis());
    }

    public void init() {
        model.init();
    }

    public void destroy() {
        model.destroy();
    }

    public long getLastCoord() {
        return lastCoord;
    }

    public long rollout(long playerBoard, long opponentBoard, long moves) {
        // FIXME 仮にPreludeの実装を流用する
        List<RolloutPolicy.Eval> evals = new ArrayList<>();
        while (moves != 0) {
            long coord = moves & -moves;  // 一番右のビットのみ取り出す

            FloatBuffer state = converter.convertState(board, coord);
            float value = model.calculatePredicatedValue(state);
            evals.add(new RolloutPolicy.Eval(coord, value));

            moves ^= coord;  // 一番右のビットを0にする
        }

        // FIXME 制限時間に返せる着手を１つは用意する
        long coord = optimumChoice(evals);
        lastCoord = coord;
        return coord;
    }

    private long optimumChoice(List<RolloutPolicy.Eval> evals) {
        Collections.sort(evals, Collections.reverseOrder()); // 評価値で降順

        List<Long> moves = new ArrayList<>();
        float maximumValue = Float.NEGATIVE_INFINITY;
        float delta = 0.001f; // FIXME 近い値を考慮
        for (RolloutPolicy.Eval evel : evals) {
            long move = evel.getMove();
            float value = evel.getValue();
            if (value + delta < maximumValue) {
                break;
            }
            moves.add(move);
            maximumValue = value;
        }

        return moves.get(random.nextInt(moves.size()));
    }

    private static class Eval implements Comparable<RolloutPolicy.Eval> {

        private long move;
        private float value;

        Eval(long move, float value) {
            this.move = move;
            this.value = value;
        }

        long getMove() {
            return move;
        }

        float getValue() {
            return value;
        }

        void setValue(float value) {
            this.value = value;
        }

        @Override
        public int compareTo(RolloutPolicy.Eval eval) {
            return Float.compare(value, eval.value);
        }
    }
}
