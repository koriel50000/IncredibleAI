package com.github.koriel50000.prelude.feature;

import com.github.koriel50000.prelude.Reversi;
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

    public ReferenceFeature(Reversi reversi) {
        bookSearch = new BookSearch(reversi);
        rolloutPolicy = new RolloutPolicy(reversi);
        winLossExplorer = new WinLossExplorer(reversi);

        evaluateTasks = new ArrayList<>();
        evaluateTasks.add(new EvaluateTask() {
            @Override
            protected Reversi.Coord evaluate(List<Reversi.Coord> moves) throws Exception {
                return search(moves);
            }
        });
        evaluateTasks.add(new EvaluateTask() {
            @Override
            protected Reversi.Coord evaluate(List<Reversi.Coord> moves) throws Exception {
                return rollout(moves);
            }
        });
        evaluateTasks.add(new EvaluateTask() {
            @Override
            protected Reversi.Coord evaluate(List<Reversi.Coord> moves) throws Exception {
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
    public Reversi.Coord evaluate(List<Reversi.Coord> moves) {
        for (EvaluateTask evaluateTask : evaluateTasks) {
            evaluateTask.setMoves(moves);
        }

        Reversi.Coord coord;
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

    private Reversi.Coord search(List<Reversi.Coord> moves) {
        if (bookSearch.notExists()) {
            throw new CancellationException("not exists");
        }

        return bookSearch.search(moves);
    }

    private Reversi.Coord rollout(List<Reversi.Coord> moves) {
        return rolloutPolicy.rollout(moves);
    }

    private Reversi.Coord explore(List<Reversi.Coord> moves) {
        if (winLossExplorer.notPossible()) {
            throw new CancellationException("not possible");
        }

        return winLossExplorer.explore(moves);
    }

    private static abstract class EvaluateTask implements Callable<Reversi.Coord> {

        private List<Reversi.Coord> moves;

        private void setMoves(List<Reversi.Coord> moves) {
            this.moves = moves;
        }

        @Override
        public Reversi.Coord call() throws Exception {
            return evaluate(moves);
        }

        protected abstract Reversi.Coord evaluate(List<Reversi.Coord> moves) throws Exception;
    }
}
