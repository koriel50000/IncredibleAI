# -*- coding: utf-8 -*-

import sys
from datetime import datetime

import cnn_model

import tensorflow.compat.v1 as tf
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
        cnn_model.training_model(sess, 'train1k{:02}'.format(i))
        saver.save(sess, "../resources/checkpoint/model.ckpt", global_step=i)
        print(datetime.now())
    
    sess.close()
    print('end:', datetime.now())
    
    return 0


if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)
