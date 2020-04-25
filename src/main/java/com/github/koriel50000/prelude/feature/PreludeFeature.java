package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.Reversi;
import com.github.koriel50000.prelude.learning.CNNModel;
import com.github.koriel50000.prelude.learning.PreludeConverter;
import org.tensorflow.Tensor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PreludeFeature implements Feature {

    private PreludeConverter converter;
    private CNNModel model;
    private Random random;

    public PreludeFeature() {
        converter = new PreludeConverter();
        model = new CNNModel();
        random = new Random(System.currentTimeMillis());
    }

    @Override
    public void init(Reversi reversi) {
        model.init();
    }

    @Override
    public void destroy(Reversi reversi) {
        model.destroy();
    }

    @Override
    public Reversi.Move evaluate(Reversi reversi, List<Reversi.Move> moves, Reversi.Turn turn) {
        List<Eval> evals = clearEvals(moves);
        for (Eval eval : evals) {
            Reversi.Move move = eval.getMove();
            Tensor state = converter.convertState(reversi, move);
            float value = model.calculatePredicatedValue(state);
            eval.setValue(value);
        }

        if (evals.size() > 0) {
            Reversi.Move actualMove = optimumChoice(evals);
            return actualMove;
        } else {
            throw new IllegalArgumentException("not found");
        }
    }

    private List<Eval> clearEvals(List<Reversi.Move> moves) {
        List<Eval> evals = new ArrayList<>();
        for (Reversi.Move move : moves) {
            evals.add(new Eval(move, 0.0f)); // valueはダミー
        }
        return evals;
    }

    private Reversi.Move optimumChoice(List<Eval> evals) {
        Collections.sort(evals, Collections.reverseOrder()); // 評価値で降順

        List<Reversi.Move> moves = new ArrayList<>();
        float maximumValue = Float.MIN_VALUE;
        float delta = 0.001f; // 近い値を考慮
        for (Eval evel : evals) {
            Reversi.Move move = evel.getMove();
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

        private Reversi.Move move;
        private float value;

        Eval(Reversi.Move move, float value) {
            this.move = move;
            this.value = value;
        }

        Reversi.Move getMove() {
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
