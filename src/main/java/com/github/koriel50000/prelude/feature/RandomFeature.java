package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.reversi.Bits;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomFeature implements Feature {

    private Random random;

    public RandomFeature(long seed) {
        random = new Random(seed);
    }

    @Override
    public void init() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public long evaluate(long playerBoard, long opponentBoard, long coords) {
        List<Long> moveList = new ArrayList<>();
        while (coords != 0) {
            long coord = Bits.getRightmostBit(coords);  // 一番右のビットのみ取り出す
            moveList.add(coord);
            coords ^= coord;  // 一番右のビットを0にする
        }
        long move = moveList.get(random.nextInt(moveList.size()));
        return move;
    }
}
