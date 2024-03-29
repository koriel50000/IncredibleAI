package com.github.koriel50000.prelude.op;

import com.github.koriel50000.prelude.book.BookSearch;
import com.github.koriel50000.prelude.learning.BitFeature;
import com.github.koriel50000.prelude.learning.PreludeFeature;
import com.github.koriel50000.prelude.reversi.BitBoard;
import com.github.koriel50000.prelude.reversi.Reversi;
import com.github.koriel50000.prelude.rollout.RolloutPolicy;
import com.github.koriel50000.prelude.winloss.WinLossExplorer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ReferenceOperator implements Operator {

    private static final long TIME_LIMIT = 1000; // 制限時間 1000(ms)

    private BookSearch bookSearch;
    private RolloutPolicy rolloutPolicy;
    private WinLossExplorer winLossExplorer;

    private ExecutorService executor;
    private List<EvaluateTask> evaluateTasks;

    public ReferenceOperator(BitBoard bitBoard, BitFeature bitFeature,
                             Reversi reversi, PreludeFeature feature, long seed) {
        bookSearch = new BookSearch(bitBoard);
        rolloutPolicy = new RolloutPolicy(bitBoard, bitFeature, reversi, feature, seed);
        winLossExplorer = new WinLossExplorer(bitBoard);

        evaluateTasks = new ArrayList<>();
        evaluateTasks.add(new EvaluateTask() {
            @Override
            protected long evaluate(long player, long opponent, long coords) {
                return search(player, opponent, coords);
            }
        });
        evaluateTasks.add(new EvaluateTask() {
            @Override
            protected long evaluate(long player, long opponent, long coords) {
                return rollout(player, opponent, coords);
            }
        });
        evaluateTasks.add(new EvaluateTask() {
            @Override
            protected long evaluate(long player, long opponent, long coords) {
                return explore(player, opponent, coords);
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
    public long evaluate(long player, long opponent, long coords) {
        for (EvaluateTask evaluateTask : evaluateTasks) {
            evaluateTask.setMoves(player, opponent, coords);
        }

        Long coord;
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

    private long search(long player, long opponent, long coords) {
        if (bookSearch.notExists()) {
            throw new CancellationException("not exists");
        }

        return bookSearch.search(player, opponent, coords);
    }

    private long rollout(long player, long opponent, long coords) {
        return rolloutPolicy.rollout(player, opponent, coords);
    }

    private long explore(long player, long opponent, long coords) {
        if (winLossExplorer.notPossible()) {
            throw new CancellationException("not possible");
        }

        return winLossExplorer.explore(player, opponent, coords);
    }

    private static abstract class EvaluateTask implements Callable<Long> {

        private long player;
        private long opponent;
        private long coords;

        private void setMoves(long player, long opponent, long coords) {
            this.player = player;
            this.opponent = opponent;
            this.coords = coords;
        }

        @Override
        public Long call() {
            return evaluate(player, opponent, coords);
        }

        protected abstract long evaluate(long player, long opponent, long coords);
    }
}
