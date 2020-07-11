package com.github.koriel50000.prelude.book;

import com.github.koriel50000.prelude.reversi.BitBoard;
import com.github.koriel50000.prelude.reversi.Board;

import java.util.List;

public class BookSearch {

    private BitBoard board;

    public BookSearch(BitBoard board) {
        this.board = board;
    }

    public boolean notExists() {
        // FIXME
        // 少なくともブックに存在しないならばtrue
        // falseの場合は、ブックに存在するかは未確定
        return true;
    }

    public long search(long playerBoard, long opponentBoard, long moves) {
        // TODO
        // ブックから着手を検索
        return 0;
    }
}
