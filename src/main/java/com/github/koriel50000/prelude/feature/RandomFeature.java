package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.Reversi;

import java.util.List;
import java.util.Random;

public class RandomFeature implements Feature {

    private Random random;

    public RandomFeature() {
        random = new Random(System.currentTimeMillis());
    }

    @Override
    public void init() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public Reversi.Coord evaluate(List<Reversi.Coord> moves) {
        Reversi.Coord coord = moves.get(random.nextInt(moves.size()));
        return coord;
    }
}
