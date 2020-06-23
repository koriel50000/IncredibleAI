package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.reversi.Board;

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
    public Board.Coord evaluate(List<Board.Coord> moves) {
        Board.Coord coord = moves.get(random.nextInt(moves.size()));
        return coord;
    }
}
