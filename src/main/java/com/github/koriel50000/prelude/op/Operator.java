package com.github.koriel50000.prelude.op;

public interface Operator {

    void init();

    void destroy();

    long evaluate(long player, long opponent, long coords);
}
