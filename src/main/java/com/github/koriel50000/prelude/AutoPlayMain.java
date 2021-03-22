package com.github.koriel50000.prelude;

import com.github.koriel50000.prelude.learning.BitFeature;
import com.github.koriel50000.prelude.learning.PreludeFeature;
import com.github.koriel50000.prelude.op.Operator;
import com.github.koriel50000.prelude.op.PreludeOperator;
import com.github.koriel50000.prelude.op.RandomOperator;
import com.github.koriel50000.prelude.op.ReferenceOperator;
import com.github.koriel50000.prelude.reversi.*;

import java.util.List;

import static com.github.koriel50000.prelude.reversi.Reversi.Coord;

public class AutoPlayMain {

    private BitBoard bitBoard;
    private BitFeature bitFeature;
    private Reversi reversi;
    private PreludeFeature feature;

    private AutoPlayMain() {
        bitBoard = new BitBoard();
        bitFeature = new BitFeature();
        reversi = new Reversi();
        feature = new PreludeFeature();
    }

    private void autoplay() {
        long seed = System.currentTimeMillis();
        Operator referenceOperator = new ReferenceOperator(
                bitBoard, bitFeature, reversi, feature, seed);
        Operator preludeOperator = new PreludeOperator(
                bitBoard, bitFeature, reversi, feature, seed);
        Operator randomOperator = new RandomOperator(seed);
        preludeOperator.init();
        randomOperator.init();

        int win = 0;
        int loss = 0;
        int draw = 0;
        int winStones = 0;
        int lossStones = 0;
        Score score;

        for (int i = 0; i < 50; i++) {
            score = play(preludeOperator, randomOperator);

            switch (score.getWinner()) {
                case "black":
                    win += 1;
                    winStones += score.getBlackStones();
                    lossStones += score.getWhiteStones();
                    break;
                case "white":
                    loss += 1;
                    winStones += score.getWhiteStones();
                    lossStones += score.getBlackStones();
                    break;
                case "draw":
                    draw += 1;
                    winStones += score.getBlackStones();
                    lossStones += score.getWhiteStones();
                    break;
            }

            score = play(randomOperator, preludeOperator);

            switch (score.getWinner()) {
                case "black":
                    loss += 1;
                    winStones += score.getBlackStones();
                    lossStones += score.getWhiteStones();
                    break;
                case "white":
                    win += 1;
                    winStones += score.getWhiteStones();
                    lossStones += score.getBlackStones();
                    break;
                case "draw":
                    draw += 1;
                    winStones += score.getWhiteStones();
                    lossStones += score.getBlackStones();
                    break;
            }
        }

        System.out.println(String.format("win:%d loss:%d draw:%d", win, loss, draw));
        System.out.println(String.format("win_stone:%d loss_stone:%d", winStones, lossStones));

        preludeOperator.destroy();
        randomOperator.destroy();
    }

    /**
     * ゲームを開始する
     */
    private Score play(Operator blackOperator, Operator whiteOperator) {
        bitBoard.clear();
        bitFeature.clear();
        reversi.clear();
        feature.clear();

        LineBuffer buffer = new LineBuffer();

        while (true) {
            boolean passed = false;
            boolean blackTurn = bitBoard.currentColor == BitBoard.BLACK;
            long player =  blackTurn ? bitBoard.blackBoard : bitBoard.whiteBoard;
            long opponent = blackTurn ? bitBoard.whiteBoard : bitBoard.blackBoard;
            Operator operator = blackTurn ? blackOperator : whiteOperator;

            long coords = bitBoard.availableMoves(player, opponent);
            if (coords == 0) {
                passed = true;
            } else {
                long coord = operator.evaluate(player, opponent, coords);

                int index = Bits.indexOf(coord);
                long flipped = bitBoard.computeFlipped(player, opponent, index);
                bitBoard.makeMove(flipped, coord);
                bitFeature.setState(player, opponent, flipped, coord, index);

                Coord coord_ = Coord.valueOf(coord);
                List<Coord> flipped_ = reversi.makeMove(coord_);
                feature.increaseFlipped(coord_, flipped_);
            }

            // ゲーム終了を判定
            boolean dummy = reversi.hasCompleted(passed);
            if (bitBoard.hasCompleted(passed)) {
                break;
            }
            bitBoard.nextTurn(passed);
            reversi.nextTurn(passed);
        }

        bitBoard.printBoard(buffer.offset(0));
        bitBoard.printScore(buffer.offset(0));
        reversi.printBoard(buffer.offset(30));
        reversi.printScore(buffer.offset(30));
        buffer.flush();

        return bitBoard.getScore();
    }

    public static void main(String[] args) {
        AutoPlayMain main = new AutoPlayMain();
        main.autoplay();
    }
}
