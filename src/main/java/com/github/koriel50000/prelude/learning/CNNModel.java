package com.github.koriel50000.prelude.learning;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.tensorflow.*;
import org.tensorflow.op.Scope;
import org.tensorflow.op.core.Constant;
import org.tensorflow.op.core.Reshape;
import org.tensorflow.op.core.Shape;
import org.tensorflow.types.UInt8;

import java.nio.ByteBuffer;

public class CNNModel {

    private static final int COLUMNS = 8;
    private static final int ROWS = 8;
    private static final int CHANNEL = 16;

    private Graph graph;
    private Session session;

    public void init() {
        SavedModelBundle bundle = SavedModelBundle.load("./checkpoint/model.ckpt-1");
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

    public float calculatePredicatedValue(ByteBuffer stateBuffer) {
//        Scope scope = new Scope(graph);
//        Output<UInt8> state = graph.opBuilder("Const", "state")
//                .setAttr("dtype", DataType.UINT8)
//                .setAttr("value", Tensor.create(UInt8.class, new long[] { COLUMS, ROWS, CHANNEL }, stateBuffer))
//                .build()
//                .output(0);
//        Output<UInt8> state = Constant.create(scope, UInt8.class, new long[] { COLUMNS * ROWS * CHANNEL }, stateBuffer).asOutput();
//        System.out.println("state: " + state);
//        Output shape = Shape.create(scope, Constant.create(scope, new long[] { 1, -1 }, Long.class)).asOutput();
//        System.out.println("shape: " + shape);
//        Tensor state_ = Reshape.create(scope, state, shape).asOutput().tensor();
//        System.out.println("state_: " + state_);
//        try (Tensor<Float> keep_prob = Tensors.create(1.0f);
//             Tensor y_ = session.runner()
//                     .feed("x", state.tensor())
//                     .feed("keep_prob", keep_prob)
//                     .fetch("y_conv")
//                     .run().get(0))
//        {
//            //return y_.floatValue();
//        }
        return 1.0f;
    }
}
