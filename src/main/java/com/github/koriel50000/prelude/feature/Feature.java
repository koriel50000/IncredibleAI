package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.Reversi;

import java.util.List;

public interface Feature {

    void init(Reversi reversi);

    void destroy(Reversi reversi);

    Reversi.Coord evaluate(Reversi reversi, List<Reversi.Coord> moves, Reversi.Turn turn);
}
