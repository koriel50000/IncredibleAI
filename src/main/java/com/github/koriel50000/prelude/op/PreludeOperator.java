package com.github.koriel50000.prelude.op;

import com.github.koriel50000.prelude.learning.BitFeature;
import com.github.koriel50000.prelude.learning.CNNModel;
import com.github.koriel50000.prelude.learning.PreludeFeature;
import com.github.koriel50000.prelude.reversi.BitBoard;
import com.github.koriel50000.prelude.reversi.Reversi;
import com.github.koriel50000.prelude.util.Bits;
import com.github.koriel50000.prelude.util.LineBuffer;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.github.koriel50000.prelude.learning.PreludeFeature.COLUMNS;
import static com.github.koriel50000.prelude.learning.PreludeFeature.ROWS;
import static com.github.koriel50000.prelude.reversi.Reversi.Coord;

public class PreludeOperator implements Operator {

    private BitBoard bitBoard;
    private BitFeature bitFeature;
    private Reversi reversi;
    private PreludeFeature feature;

    private CNNModel model;
    private Random random;

    public PreludeOperator(BitBoard bitBoard, BitFeature bitFeature,
                           Reversi reversi, PreludeFeature feature, long seed) {
        this.bitBoard = bitBoard;
        this.bitFeature = bitFeature;
        this.reversi = reversi;
        this.feature = feature;
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
    public long evaluate(long player, long opponent, long coords) {
        List<Eval> evals = new ArrayList<>();

        while (coords != 0) {
            long coord = Bits.getRightmostBit(coords);  // 一番右のビットのみ取り出す

            // Assert
            int index = Bits.indexOf(coord);
            long flipped = bitBoard.computeFlipped(player, opponent, index);
            float[] buffer = bitFeature.getStateBuffer(player, opponent, flipped, coord, index);
            float[] expectedBuffer = feature.getStateBuffer(reversi, Coord.valueOf(coord));
            try {
                assertEquals(expectedBuffer, buffer, "buffer");
            } catch (AssertionError e) {
                throw e;
            }

            float value = model.calculatePredicatedValue(buffer);
            evals.add(new Eval(coord, value));

            coords ^= coord;  // 一番右のビットを0にする
        }

        return optimumChoice(evals);
    }

    private long optimumChoice(List<Eval> evals) {
        Collections.sort(evals, Collections.reverseOrder()); // 評価値で降順

        List<Long> coords = new ArrayList<>();
        float maximumValue = Float.NEGATIVE_INFINITY;
        float delta = 0.01f; // FIXME 近い値を考慮
        for (Eval evel : evals) {
            long coord = evel.getCoord();
            float value = evel.getValue();
            if (value + delta < maximumValue) {
                break;
            }
            coords.add(coord);
            maximumValue = value;
        }

//        printEvals(evals);
//        printCoords(coords);

        return coords.get(random.nextInt(coords.size()));
    }

    private void printEvals(List<Eval> evals) {
        System.out.println(ArrayUtils.toString(evals));
    }

    private void printCoords(List<Long> coords) {
        List<Coord> coords_ = new ArrayList<>();
        for (long coord : coords) {
            coords_.add(Coord.valueOf(coord));
        }
        System.out.println(ArrayUtils.toString(coords_));
    }

    private void assertEquals(float[] expected, float[] actual, String message) {
        float[] matrix = new float[ROWS * COLUMNS];
        for (int channel = 0; channel < 16; channel++) {
            int offset = ROWS * COLUMNS * channel;
            System.arraycopy(expected, offset, matrix, 0, matrix.length);
            long expectedMatrix = matrixTo(matrix);
            System.arraycopy(actual, offset, matrix, 0, matrix.length);
            long actualMatrix = matrixTo(matrix);
            if (expectedMatrix != actualMatrix) {
                LineBuffer buffer = new LineBuffer();
                buffer.offset(0);
                buffer.println("expected");
                Bits.printMatrix(buffer, expectedMatrix);
                buffer.offset(15);
                buffer.println("actual");
                Bits.printMatrix(buffer, actualMatrix);
                buffer.flush();
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

        @Override
        public String toString() {
            return String.format("%s:%.2f", Coord.valueOf(coord), value);
        }
    }
}
