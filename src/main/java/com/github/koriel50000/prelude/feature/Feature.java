package com.github.koriel50000.prelude.feature;

public interface Feature {

    void init();

    void destroy();

    long evaluate(long playerBoard, long opponentBoard, long moves);
}
