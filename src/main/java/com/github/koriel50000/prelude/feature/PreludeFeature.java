package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.Reversi;
import com.github.koriel50000.prelude.learning.CNNModel;
import com.github.koriel50000.prelude.learning.PreludeConverter;
import org.tensorflow.Tensor;

import java.util.ArrayList;
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

    public void init() {
        model.init();
    }

    public void destroy() {
        model.destroy();
    }

    @Override
    public Reversi.Move evaluate(Reversi reversi, List<Reversi.Move> moves, Reversi.Turn turn) {
        List<Eval> evals = availablesToEvals(moves);
        for (Eval eval : evals) {
            Reversi.Move move = eval.getMove();
            Tensor state = converter.convertState(reversi, move);
            float value = model.calculatePredicatedValue(state);
            eval.setValue(value);
        }

        if (evals.size() > 0) {
            Reversi.Move actualMove = optimumChoice(evals);
            reversi.makeMove(actualMove);
            reversi.nextTurn();
            return actualMove;
        } else {
            reversi.nextTurn();
            return null;
        }
    }

    private List<Eval> availablesToEvals(List<Reversi.Move> moves) {
        List<Eval> evals = new ArrayList<>();
        for (Reversi.Move move : moves) {
            evals.add(new Eval(move, 0.0f)); // valueはダミー
        }
        return evals;
    }

    private Reversi.Move optimumChoice(List<Eval> evals) {
        evals.sort(null); // TODO valueで降順
        //evals.sort(key = lambda x:x['value'], reverse = True);

        List<Reversi.Move> moves = new ArrayList<>();
        float maximumValue = Float.MIN_VALUE;
        for (Eval entry : evals) {
            Reversi.Move move = entry.getMove();
            float value = entry.getValue();
            if (value < maximumValue) {
                break;
            }
            moves.add(move);
            maximumValue = value;
        }

        return moves.get(random.nextInt(moves.size()));
    }

    private static class Eval {
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
    }
}
