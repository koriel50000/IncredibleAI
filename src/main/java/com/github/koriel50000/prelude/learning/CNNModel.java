package com.github.koriel50000.prelude.learning;

import org.tensorflow.Graph;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.nio.FloatBuffer;

public class CNNModel {

    private static final int COLUMNS = 8;
    private static final int ROWS = 8;
    private static final int CHANNELS = 16;

    private Graph graph;
    private Session session;

    public void init() {
        // FIXME 学習済みモデルがよくわかっていない
        // Graph Transform Toolやservingなど。Graphで表現するされたモデルが理解できていない。
        // 「TensorFlow のモデルを Graph Transform Tool で最適化してサービングする」
        // https://tech.mercari.com/entry/2019/12/09/200514
        SavedModelBundle bundle = SavedModelBundle.loader("./build/resources/main/model/")
                .withTags("serve")
                .load();
        graph = bundle.graph();
        session = bundle.session();
    }

    public void destroy() {
        session.close();
        graph.close();
    }

    public float calculatePredicatedValue(FloatBuffer stateBuffer) {
        // FIXME コマンドラインツールの使い方を理解する
        // TensorFlow > 学ぶ > TensorFlow Core > ガイド > SavedModel
        // Details of the SavedModel command line interface
        // https://www.tensorflow.org/guide/saved_model
        //
        // > saved_model_cli show --dir ..\resources\model
        // > saved_model_cli show --dir ..\resources\model --tag_set serve
        // > saved_model_cli show --dir ..\resources\model --tag_set serve --signature_def serving_default
        // The given SavedModel SignatureDef contains the following input(s):
        //   inputs['inputs'] tensor_info:
        //       dtype: DT_FLOAT
        //       shape: (-1, 1024)
        //       name: serving_default_inputs:0
        // The given SavedModel SignatureDef contains the following output(s):
        //   outputs['outputs'] tensor_info:
        //       dtype: DT_FLOAT
        //       shape: (-1, 1)
        //       name: StatefulPartitionedCall:0
        // Method name is: tensorflow/serving/predict
        try (Tensor<Float> state = Tensor.create(new long[]{1, ROWS * COLUMNS * CHANNELS}, stateBuffer);
             Tensor predictions = session.runner()
                     .feed("serving_default_inputs", state)
                     .fetch("StatefulPartitionedCall")
                     .run().get(0)) {
            float[][] matrix = new float[1][1];
            predictions.copyTo(matrix);
            return matrix[0][0];
        }
    }
}
