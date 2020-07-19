package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.reversi.Bits;
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

    public PreludeFeature(Board board, long seed) {
        this.board = board;
        converter = new PreludeConverter();
        model = new CNNModel();
        random = new Random(seed);
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
    public long evaluate(long playerDummy, long opponentDummy, long coordsDummy) {
        List<Board.Coord> moves = board.availableMoves();

        List<Eval> evals = new ArrayList<>();
        for (Board.Coord move : moves) {
            FloatBuffer state = converter.convertState(board, move);
            float value = model.calculatePredicatedValue(state);
            evals.add(new Eval(move, value));
        }

        Board.Coord move = optimumChoice(evals);
        return Bits.coordAt(move.x - 1, move.y - 1);
    }

    private Board.Coord optimumChoice(List<Eval> evals) {
        Collections.sort(evals, Collections.reverseOrder()); // 評価値で降順

        List<Board.Coord> moves = new ArrayList<>();
        float maximumValue = Float.NEGATIVE_INFINITY;
        float delta = 0.001f; // FIXME 近い値を考慮
        for (Eval evel : evals) {
            Board.Coord move = evel.getMove();
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

        private Board.Coord move;
        private float value;

        Eval(Board.Coord move, float value) {
            this.move = move;
            this.value = value;
        }

        Board.Coord getMove() {
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
