package com.github.koriel50000.prelude.reversi;

public class Score {

    private String winner;
    private int blackStones;
    private int whiteStones;

    public Score(String winner, int blackStones, int whiteStones) {
        this.winner = winner;
        this.blackStones = blackStones;
        this.whiteStones = whiteStones;
    }

    public String getWinner() {
        return winner;
    }

    public int getBlackStones() {
        return blackStones;
    }

    public int getWhiteStones() {
        return whiteStones;
    }
}
