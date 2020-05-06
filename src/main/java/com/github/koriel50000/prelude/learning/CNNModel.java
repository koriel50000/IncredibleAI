package com.github.koriel50000.prelude.learning;

import org.tensorflow.*;

import java.nio.FloatBuffer;
import java.util.Iterator;

public class CNNModel {

    private static final int ROWS = 8;
    private static final int COLUMNS = 8;
    private static final int CHANNEL = 16;

    private Graph graph;
    private Session session;

    public void init() {
        // FIXME 学習済みモデルが利用できない。
        // Graph Transform Toolやservingなど。Graphで表現するされたモデルが理解できていない。
        // 「TensorFlow のモデルを Graph Transform Tool で最適化してサービングする」
        // https://tech.mercari.com/entry/2019/12/09/200514
        SavedModelBundle bundle = SavedModelBundle.loader("./build/resources/main/model/")
                .withTags("serve")
                .load();
        graph = bundle.graph();
        session = bundle.session();
        Iterator<Operation> iter = graph.operations();
        while (iter.hasNext()) {
            Operation op = iter.next();
            String name = op.name();
            String type = op.type();
            System.out.println(name + " : " + type);
        }
    }

    public void destroy() {
        session.close();
        graph.close();
    }

    public float calculatePredicatedValue(FloatBuffer stateBuffer) {
        try (Tensor<Float> state = Tensor.create(new long[]{ROWS * COLUMNS * CHANNEL}, stateBuffer);
             Tensor predictions = session.runner()
                     .feed("inputs", state)
                     .fetch("outputs")
                     .run().get(0)) {
            return predictions.floatValue();
        }
    }
}
