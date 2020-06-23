package com.github.koriel50000.prelude.book;

import com.github.koriel50000.prelude.Reversi;

import java.util.List;

public class BookSearch {

    private Reversi reversi;

    public BookSearch(Reversi reversi) {
        this.reversi = reversi;
    }

    public boolean notExists() {
        // FIXME
        // 少なくともブックに存在しないならばtrue
        // falseの場合は、ブックに存在するかは未確定
        return true;
    }

    public Reversi.Coord search(List<Reversi.Coord> moves) {
        // TODO
        // ブックから着手を検索
        return null;
    }
}
