# -*- coding: utf-8 -*-

import sys
from datetime import datetime
import tensorflow.compat.v1 as tf

import datasets
import cnn_model

tf.disable_v2_behavior()


#
# メイン
#
def main(args):
    print('start:', datetime.now())
    sess = tf.Session()
    
    init = tf.initialize_all_variables()
    sess.run(init)
    
    saver = tf.train.Saver(max_to_keep=None)
    
    for i in range(1, 2):
        prefix = 'train1k{:02}'.format(i)
        reversi_data = datasets.read_data_sets('../resources/REVERSI_data/', prefix)

        cnn_model.training_model(sess, reversi_data)

        saver.save(sess, "../resources/checkpoint/model.ckpt", global_step=i)
        print(datetime.now())
    
    sess.close()
    print('end:', datetime.now())
    
    return 0


if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)
