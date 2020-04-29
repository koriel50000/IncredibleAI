package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.Reversi;

import java.util.List;

public interface Feature {

    void init();

    void destroy();

    Reversi.Coord evaluate(Reversi reversi, List<Reversi.Coord> moves, Reversi.Turn turn);
}
