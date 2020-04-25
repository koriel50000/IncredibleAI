package com.github.koriel50000.prelude.learning;

import org.tensorflow.*;

public class CNNModel {

    private static final int COLUMS = 8;
    private static final int ROWS = 8;
    private static final int CHANNEL = 16;

    private Graph graph;
    private Session session;

    public void init() {
        SavedModelBundle bundle = SavedModelBundle.loader("./checkpoint/model.ckpt-1").load();
        graph = bundle.graph();
        session = bundle.session();
    }

//    public void createModel() {
//        try (Graph g = new Graph()) {
//            Output input = null;
//            Output x = g.opBuilder("Placeholder", "x")
//                    .setAttr("dtype", DataType.FLOAT)
//                    .setAttr("shape", Shape.make(0, COLUMS * ROWS * CHANNEL))
//                    .addInput(input)
//                    .build()
//                    .output(0);
//        }
//    }

    public void destroy() {
        session.close();
        graph.close();
    }

    public float calculatePredicatedValue(Tensor state) {
        try (Tensor state_ = graph.opBuilder("Reshape", "state_")
             .setAttr("tensor", state)
             .setAttr("shape", Shape.make(1, -1))
             .build()
             .output(0)
             .tensor();
             Tensor keep_prob = Tensors.create(1.0f);
             Tensor y_ = session.runner()
                     .feed("x", state_)
                     .feed("keep_prob", keep_prob)
                     .fetch("y_conv")
                     .run().get(0))
        {
            return y_.floatValue();
        }
    }
}
