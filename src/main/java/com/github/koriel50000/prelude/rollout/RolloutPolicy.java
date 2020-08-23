package com.github.koriel50000.prelude.rollout;

import com.github.koriel50000.prelude.learning.*;
import com.github.koriel50000.prelude.reversi.BitBoard;
import com.github.koriel50000.prelude.reversi.Bits;
import com.github.koriel50000.prelude.reversi.Reversi;
import com.github.koriel50000.prelude.reversi.LineBuffer;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RolloutPolicy {

    private BitBoard bitBoard;
    private CNNModel model;
    private Random random;

    private Reversi reversi;
    private PreludeConverter converter;

    private volatile long lastCoord;

    public RolloutPolicy(BitBoard bitBoard, Reversi reversi, long seed) {
        this.bitBoard = bitBoard;
        this.reversi = reversi;
        model = new CNNModel();
        random = new Random(seed);

        converter = new PreludeConverter();
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

    public long rollout(long player, long opponent, long coords) {
        // FIXME 仮にPreludeの実装を流用する
        List<RolloutPolicy.Eval> evals = new ArrayList<>();
        while (coords != 0) {
            long coord = Bits.getRightmostBit(coords);  // 一番右のビットのみ取り出す

            // Assert
            BitState state = bitBoard.convertState(player, opponent, coord);
            FloatBuffer expectedBuffer = reversi.convertState(Reversi.Coord.valueOf(coord));
            try {
                FloatBuffer buffer = state.getBuffer();
                assertEquals(expectedBuffer, buffer, "buffer");
            } catch (AssertionError e) {
                LineBuffer buffer = new LineBuffer();
                reversi.printBoard(buffer.offset(0));
                Bits.printMatrix(buffer.offset(30), coord);
                buffer.flush();
                throw e;
            }

            float value = model.calculatePredicatedValue(state.getBuffer());
            evals.add(new RolloutPolicy.Eval(coord, value));

            coords ^= coord;  // 一番右のビットを0にする
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

    private void assertEquals(Object expected, Object actual, String message) {
        assert expected.equals(actual) : String.format("'%s' not match. %s %s", message, expected, actual);
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
