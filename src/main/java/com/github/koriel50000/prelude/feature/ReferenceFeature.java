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
            protected long evaluate(long playerBoard, long opponentBoard, long moves) throws Exception {
                return search(playerBoard, opponentBoard, moves);
            }
        });
        evaluateTasks.add(new EvaluateTask() {
            @Override
            protected long evaluate(long playerBoard, long opponentBoard, long moves) throws Exception {
                return rollout(playerBoard, opponentBoard, moves);
            }
        });
        evaluateTasks.add(new EvaluateTask() {
            @Override
            protected long evaluate(long playerBoard, long opponentBoard, long moves) throws Exception {
                return explore(playerBoard, opponentBoard, moves);
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
    public long evaluate(long playerBoard, long opponentBoard, long moves) {
        for (EvaluateTask evaluateTask : evaluateTasks) {
            evaluateTask.setMoves(playerBoard, opponentBoard, moves);
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

    private long search(long playerBoard, long opponentBoard, long moves) {
        if (bookSearch.notExists()) {
            throw new CancellationException("not exists");
        }

        return bookSearch.search(playerBoard, opponentBoard, moves);
    }

    private long rollout(long playerBoard, long opponentBoard, long moves) {
        return rolloutPolicy.rollout(playerBoard, opponentBoard, moves);
    }

    private long explore(long playerBoard, long opponentBoard, long moves) {
        if (winLossExplorer.notPossible()) {
            throw new CancellationException("not possible");
        }

        return winLossExplorer.explore(playerBoard, opponentBoard, moves);
    }

    private static abstract class EvaluateTask implements Callable<Long> {

        private long playerBoard;
        private long opponentBoard;
        private long moves;

        private void setMoves(long playerBoard, long opponentBoard, long moves) {
            this.playerBoard = playerBoard;
            this.opponentBoard = opponentBoard;
            this.moves = moves;
        }

        @Override
        public Long call() throws Exception {
            return evaluate(playerBoard, opponentBoard, moves);
        }

        protected abstract long evaluate(long playerBoard, long opponentBoard, long moves) throws Exception;
    }
}
