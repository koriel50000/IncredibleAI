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

    private ExecutorService executor;
    private List<EvaluateTask> evaluateTasks;

    private SearchTask searchTask;
    private RolloutTask rolloutTask;
    private WinLossTask winLossTask;

    @Override
    public void init() {
        searchTask = new SearchTask();
        rolloutTask = new RolloutTask();
        winLossTask = new WinLossTask();
        rolloutTask.init();

        evaluateTasks = new ArrayList<>();
        evaluateTasks.add(searchTask);
        evaluateTasks.add(rolloutTask);
        evaluateTasks.add(winLossTask);

        executor = Executors.newFixedThreadPool(evaluateTasks.size());
    }

    @Override
    public void destroy() {
        executor.shutdown();
        
        rolloutTask.destroy();
    }

    @Override
    public Reversi.Coord evaluate(Reversi reversi, List<Reversi.Coord> moves) {
        for (EvaluateTask evaluateTask : evaluateTasks) {
            evaluateTask.set(reversi, moves);
        }

        Reversi.Coord coord = null;
        try {
            coord = executor.invokeAny(evaluateTasks, TIME_LIMIT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // 時間切れの場合は、ロールアウトから最後の着手を取得
            coord = rolloutTask.getLastCoord();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return coord;
    }

    private static abstract class EvaluateTask implements Callable<Reversi.Coord> {

        private Reversi reversi;
        private List<Reversi.Coord> moves;

        private void set(Reversi reversi, List<Reversi.Coord> moves) {
            this.reversi = reversi;
            this.moves = moves;
        }

        @Override
        public Reversi.Coord call() throws Exception {
            return evaluate(reversi, moves);
        }

        protected abstract Reversi.Coord evaluate(Reversi reversi, List<Reversi.Coord> moves) throws Exception;
    }

    private static class SearchTask extends EvaluateTask {

        private BookSearch bookSearch;

        private SearchTask() {
            bookSearch = new BookSearch();
        }

        @Override
        protected Reversi.Coord evaluate(Reversi reversi, List<Reversi.Coord> moves) {
            if (bookSearch.notExists()) {
                throw new CancellationException("not exists");
            }

            return bookSearch.search();
        }
    }

    private static class RolloutTask extends EvaluateTask {

        private RolloutPolicy rolloutPolicy;

        private volatile Reversi.Coord lastCoord;

        private RolloutTask() {
            rolloutPolicy = new RolloutPolicy();
        }

        public void init() {
            rolloutPolicy.init();
        }

        public void destroy() {
            rolloutPolicy.destroy();
        }

        @Override
        protected Reversi.Coord evaluate(Reversi reversi, List<Reversi.Coord> moves) {
            // FIXME
            Reversi.Coord coord = rolloutPolicy.evaluate(reversi, moves); // この処理は制限時間内で終了すると仮定
            lastCoord = coord;

            // TODO ロールアウトでより良い着手を探す

            return coord;
        }

        public Reversi.Coord getLastCoord() {
            return lastCoord;
        }
    }

    private static class WinLossTask extends EvaluateTask {

        private WinLossExplorer winLossExplorer;

        private WinLossTask() {
            winLossExplorer = new WinLossExplorer();
        }

        @Override
        protected Reversi.Coord evaluate(Reversi reversi, List<Reversi.Coord> moves) {
            if (winLossExplorer.notPossible()) {
                throw new CancellationException("not possible");
            }

            return winLossExplorer.explore();
        }
    }
}
