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
    public void init(Reversi reversi) {
    }

    @Override
    public void destroy(Reversi reversi) {
    }

    @Override
    public Reversi.Move evaluate(Reversi reversi, List<Reversi.Move> moves, Reversi.Turn turn) {
        Reversi.Move move = moves.get(random.nextInt(moves.size()));
        return move;
    }
}
