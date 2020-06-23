package com.github.koriel50000.prelude.book;

import com.github.koriel50000.prelude.reversi.Board;

import java.util.List;

public class BookSearch {

    private Board board;

    public BookSearch(Board board) {
        this.board = board;
    }

    public boolean notExists() {
        // FIXME
        // 少なくともブックに存在しないならばtrue
        // falseの場合は、ブックに存在するかは未確定
        return true;
    }

    public Board.Coord search(List<Board.Coord> moves) {
        // TODO
        // ブックから着手を検索
        return null;
    }
}
