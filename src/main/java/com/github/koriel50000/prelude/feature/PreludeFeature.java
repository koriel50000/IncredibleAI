package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.learning.BitConverter;
import com.github.koriel50000.prelude.reversi.BitBoard;
import com.github.koriel50000.prelude.reversi.Bits;
import com.github.koriel50000.prelude.reversi.Board;
import com.github.koriel50000.prelude.learning.CNNModel;
import com.github.koriel50000.prelude.learning.PreludeConverter;
import com.github.koriel50000.prelude.reversi.LineBuffer;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PreludeFeature implements Feature {

    private BitBoard bitBoard;
    private Board board;
    private BitConverter bitConverter;
    private PreludeConverter converter;
    private CNNModel model;
    private Random random;

    public PreludeFeature(BitBoard bitBoard, Board board, long seed) {
        this.bitBoard = bitBoard;
        this.board = board;
        bitConverter = new BitConverter();
        converter = new PreludeConverter();
        model = new CNNModel();
        random = new Random(seed);
    }

    @Override
    public void init() {
        model.init();
        bitConverter.initialize();
    }

    @Override
    public void destroy() {
        model.destroy();
    }

    @Override
    public long evaluate(long player, long opponent, long coords) {
        List<Eval> evals = new ArrayList<>();

        while (coords != 0) {
            long coord = Bits.getRightmostBit(coords);  // 一番右のビットのみ取り出す
            int index = Bits.indexOf(coord);

            long flipped = bitBoard.computeFlipped(player, opponent, index);

            // Assert
            FloatBuffer state = bitConverter.convertState(player, opponent, flipped, coord, index);
            FloatBuffer expectedState = converter.convertState(board, Board.Coord.valueOf(index));
            int region = bitConverter.region;
            int expectedRegion = converter.region;
            try {
                assertEquals(expectedRegion, region, "regin");
                assertEquals(expectedState, state, "convertState");
            } catch (AssertionError e) {
                throw e;
            }

            float value = model.calculatePredicatedValue(state);
            evals.add(new Eval(coord, value));

            coords ^= coord;  // 一番右のビットを0にする
        }

        return optimumChoice(evals);
    }

    private long optimumChoice(List<Eval> evals) {
        Collections.sort(evals, Collections.reverseOrder()); // 評価値で降順

        List<Long> coords = new ArrayList<>();
        float maximumValue = Float.NEGATIVE_INFINITY;
        float delta = 0.001f; // FIXME 近い値を考慮
        for (Eval evel : evals) {
            long coord = evel.getCoord();
            float value = evel.getValue();
            if (value + delta < maximumValue) {
                break;
            }
            coords.add(coord);
            maximumValue = value;
        }

        return coords.get(random.nextInt(coords.size()));
    }

    private void assertEquals(FloatBuffer expected, FloatBuffer actual, String message) {
        FloatBuffer expected_ = expected.duplicate();
        FloatBuffer actual_ = actual.duplicate();
        float[] matrix = new float[8 * 8];
        for (int channel = 0; channel < 16; channel++) {
            expected_.get(matrix);
            long expectedMatrix = matrixTo(matrix);
            actual_.get(matrix);
            long actualMatrix = matrixTo(matrix);
            if (expectedMatrix != actualMatrix) {
                Bits.printMatrix(expectedMatrix);
                Bits.printMatrix(actualMatrix);
                assert (expectedMatrix == actualMatrix) : String.format("'%s' not match. channel=%d", message, channel);
            }
        }
    }

    private long matrixTo(float[] matrix) {
        long bits = 0L;
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] > 0.0) {
                bits |= Bits.coordAt(i);
            }
        }
        return bits;
    }

    private void assertEquals(Object expected, Object actual, String message) {
        assert expected.equals(actual) : String.format("'%s' not match. %s %s", message, expected, actual);
    }

    private static class Eval implements Comparable<Eval> {

        private long coord;
        private float value;

        Eval(long coord, float value) {
            this.coord = coord;
            this.value = value;
        }

        long getCoord() {
            return coord;
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
