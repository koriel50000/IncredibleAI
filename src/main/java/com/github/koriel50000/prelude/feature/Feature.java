package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.Reversi;

import java.util.List;

public interface Feature {

    Reversi.Move evaluate(Reversi reversi, List<Reversi.Move> moves, Reversi.Turn turn);
}
