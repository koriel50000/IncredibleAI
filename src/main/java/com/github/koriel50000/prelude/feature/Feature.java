package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.reversi.Board;

import java.util.List;

public interface Feature {

    void init();

    void destroy();

    Board.Coord evaluate(List<Board.Coord> moves);
}
