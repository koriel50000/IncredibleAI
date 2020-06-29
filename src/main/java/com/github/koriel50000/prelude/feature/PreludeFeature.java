package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.reversi.Board;
import com.github.koriel50000.prelude.learning.CNNModel;
import com.github.koriel50000.prelude.learning.PreludeConverter;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PreludeFeature implements Feature {

    private Board board;
    private PreludeConverter converter;
    private CNNModel model;
    private Random random;

    public PreludeFeature(Board board) {
        this.board = board;
        converter = new PreludeConverter();
        model = new CNNModel();
        random = new Random(System.currentTimeMillis());
    }

    @Override
    public void init() {
        model.init();
    }

    @Override
    public void destroy() {
        model.destroy();
    }

    @Override
    public long evaluate(long playerBoard, long opponentBoard, long moves) {
        List<Eval> evals = new ArrayList<>();
        while (moves != 0) {
            long coord = moves & -moves;  // 一番右のビットのみ取り出す
            FloatBuffer state = converter.convertState(board, coord);
            float value = model.calculatePredicatedValue(state);
            evals.add(new Eval(coord, value));
        }

        return optimumChoice(evals);
    }

    private long optimumChoice(List<Eval> evals) {
        Collections.sort(evals, Collections.reverseOrder()); // 評価値で降順

        List<Long> moves = new ArrayList<>();
        float maximumValue = Float.NEGATIVE_INFINITY;
        float delta = 0.001f; // FIXME 近い値を考慮
        for (Eval evel : evals) {
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

    private static class Eval implements Comparable<Eval> {

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
        public int compareTo(Eval eval) {
            return Float.compare(value, eval.value);
        }
    }
}
