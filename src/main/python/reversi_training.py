# -*- coding: utf-8 -*-

import sys
from datetime import datetime

import datasets
import cnn_model


#
# メイン
#
def main(args):
    print('start:', datetime.now())

    for i in range(1, 2):
        prefix = 'train1k{:02}'.format(i)
        reversi_data = datasets.read_data_sets('../resources/REVERSI_data/', prefix)

        cnn_model.training_model(reversi_data)

        #cnn_model.save_checkpoint('../resources/checkpoint/cnn_model', i)
        print(datetime.now())

    #cnn_model.save_model("../resources/model/")
    print('end:', datetime.now())
    
    return 0


if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)
