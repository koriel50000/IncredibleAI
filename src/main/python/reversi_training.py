# -*- coding: utf-8 -*-

import sys
from datetime import datetime

import cnn_model


#
# メイン
#
def main(args):
    print('start:', datetime.now())

    for i in range(1, 3):
        prefix = 'train1k{:02d}'.format(i)
        cnn_model.training_model('../resources/REVERSI_data/',
                                 '../resources/checkpoint/',
                                 prefix)
        print(datetime.now())

    cnn_model.save_model('../resources/model/')

    print('end:', datetime.now())
    
    return 0


if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)
