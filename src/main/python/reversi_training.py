# -*- coding: utf-8 -*-

import sys
from datetime import datetime

import cnn_model


#
# メイン
#
def main(args):
    print('start:', datetime.now())

    start_step = 0
    end_step = 100
    initial_epoch = 0
    epoch_size = 10

    if start_step != 0 or initial_epoch != 0:
        cnn_model.load_checkpoint("../resources/checkpoint/",
                                  start_step,
                                  initial_epoch)
    if initial_epoch == 10:
        start_step += 1
        initial_epoch = 0

    for step in range(start_step, end_step):
        cnn_model.training_model('../resources/REVERSI_data/',
                                 '../resources/checkpoint/',
                                 step,
                                 initial_epoch,
                                 epoch_size)
        initial_epoch = 0
        print(datetime.now())

    cnn_model.save_model('../resources/model/')

    print('end:', datetime.now())
    
    return 0


if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)
