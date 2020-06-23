package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.reversi.Board;
import com.github.koriel50000.prelude.book.BookSearch;
import com.github.koriel50000.prelude.rollout.RolloutPolicy;
import com.github.koriel50000.prelude.winloss.WinLossExplorer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ReferenceFeature implements Feature {

    private static final long TIME_LIMIT = 1000; // 制限時間 1000(ms)

    private BookSearch bookSearch;
    private RolloutPolicy rolloutPolicy;
    private WinLossExplorer winLossExplorer;

    private ExecutorService executor;
    private List<EvaluateTask> evaluateTasks;

    public ReferenceFeature(Board board) {
        bookSearch = new BookSearch(board);
        rolloutPolicy = new RolloutPolicy(board);
        winLossExplorer = new WinLossExplorer(board);

        evaluateTasks = new ArrayList<>();
        evaluateTasks.add(new EvaluateTask() {
            @Override
            protected Board.Coord evaluate(List<Board.Coord> moves) throws Exception {
                return search(moves);
            }
        });
        evaluateTasks.add(new EvaluateTask() {
            @Override
            protected Board.Coord evaluate(List<Board.Coord> moves) throws Exception {
                return rollout(moves);
            }
        });
        evaluateTasks.add(new EvaluateTask() {
            @Override
            protected Board.Coord evaluate(List<Board.Coord> moves) throws Exception {
                return explore(moves);
            }
        });

        executor = Executors.newFixedThreadPool(evaluateTasks.size());
    }

    @Override
    public void init() {
        rolloutPolicy.init();
    }

    @Override
    public void destroy() {
        executor.shutdown();

        rolloutPolicy.destroy();
    }

    @Override
    public Board.Coord evaluate(List<Board.Coord> moves) {
        for (EvaluateTask evaluateTask : evaluateTasks) {
            evaluateTask.setMoves(moves);
        }

        Board.Coord coord;
        try {
            coord = executor.invokeAny(evaluateTasks, TIME_LIMIT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // 時間切れの場合は、ロールアウトから最後の着手を取得
            coord = rolloutPolicy.getLastCoord();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return coord;
    }

    private Board.Coord search(List<Board.Coord> moves) {
        if (bookSearch.notExists()) {
            throw new CancellationException("not exists");
        }

        return bookSearch.search(moves);
    }

    private Board.Coord rollout(List<Board.Coord> moves) {
        return rolloutPolicy.rollout(moves);
    }

    private Board.Coord explore(List<Board.Coord> moves) {
        if (winLossExplorer.notPossible()) {
            throw new CancellationException("not possible");
        }

        return winLossExplorer.explore(moves);
    }

    private static abstract class EvaluateTask implements Callable<Board.Coord> {

        private List<Board.Coord> moves;

        private void setMoves(List<Board.Coord> moves) {
            this.moves = moves;
        }

        @Override
        public Board.Coord call() throws Exception {
            return evaluate(moves);
        }

        protected abstract Board.Coord evaluate(List<Board.Coord> moves) throws Exception;
    }
}
