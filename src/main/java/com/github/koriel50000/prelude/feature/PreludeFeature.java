package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.Reversi;
import com.github.koriel50000.prelude.learning.CNNModel;
import com.github.koriel50000.prelude.learning.PreludeConverter;

import java.nio.FloatBuffer;
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
    public void init() {
        model.init();
    }

    @Override
    public void destroy() {
        model.destroy();
    }

    @Override
    public Reversi.Coord evaluate(Reversi reversi, List<Reversi.Coord> moves) {
        List<Eval> evals = new ArrayList<>();
        for (Reversi.Coord move : moves) {
            FloatBuffer state = converter.convertState(reversi, move);
            float value = model.calculatePredicatedValue(state);
            evals.add(new Eval(move, value));
        }

        return optimumChoice(evals);
    }

    private Reversi.Coord optimumChoice(List<Eval> evals) {
        Collections.sort(evals, Collections.reverseOrder()); // 評価値で降順

        List<Reversi.Coord> moves = new ArrayList<>();
        float maximumValue = Float.NEGATIVE_INFINITY;
        float delta = 0.001f; // FIXME 近い値を考慮
        for (Eval evel : evals) {
            Reversi.Coord move = evel.getMove();
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

        private Reversi.Coord move;
        private float value;

        Eval(Reversi.Coord move, float value) {
            this.move = move;
            this.value = value;
        }

        Reversi.Coord getMove() {
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
