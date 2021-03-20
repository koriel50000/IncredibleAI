# -*- coding: utf-8 -*-

import sys
from datetime import datetime

import oddeven_feature as feature
import cnn_model as model

# 定数宣言
BATCH_SIZE = 16
EPOCH_SIZE = 10


#
# メイン
#
def main(args):
    print('start:', datetime.now())

    start_step = 0
    end_step = 1
    initial_epoch = 0

    # チェックポイントから学習を再開する
    if start_step != 0 or initial_epoch != 0:
        checkpoint_prefix = "../resources/checkpoint/train1k{:02d}".format(start_step)
        model.load_checkpoint(checkpoint_prefix, initial_epoch)

    if initial_epoch == EPOCH_SIZE:
        start_step += 1
        initial_epoch = 0

    for step in range(start_step, end_step):
        dataset_path = "../resources/REVERSI_data/train1k{:02d}.tfrecords.gz".format(step)
        checkpoint_prefix = "../resources/checkpoint/train1k{:02d}".format(step)

        dataset = model.load_dataset(dataset_path, feature.parse_function)

        model.training_model(dataset,
                             checkpoint_prefix,
                             BATCH_SIZE,
                             initial_epoch,
                             EPOCH_SIZE)

        initial_epoch = 0
        print(datetime.now())

    model.save_model('../resources/model/')

    print('end:', datetime.now())
    
    return 0


if __name__ == "__main__":
    exit_code = main(sys.argv)
    sys.exit(exit_code)
