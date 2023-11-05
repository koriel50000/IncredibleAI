# -*- coding: utf-8 -*-

import sys
import os
import glob
import zipfile
import gzip
import shutil
from typing import Any

import numpy as np
import tensorflow as tf
from torchvision.datasets.vision import VisionDataset
from torchvision.datasets import MNIST

import reversi
import oddeven_feature as feature


class KIFUDataset(MNIST):
    pass


def write_example(fout, state, value):
    # print(state)
    # print(value)
    serialized_example = feature.serialize_example(state, value)
    fout.write(serialized_example)


#
# 棋譜を学習データに変換する
#
def write_record(fout, move_record, eval_records):
    reversi.clear()
    feature.clear()

    for index, actual_move in enumerate(feature.convert_moves(move_record)):
        # print("index:{0} move:{1}".format(index, actual_move))
        if len(reversi.available_moves()) == 0:
            reversi.next_turn(True)
            if len(reversi.available_moves()) == 0:
                raise Exception("Error!")  # 先手・後手両方パスで終了は考慮しない

        evals = feature.convert_evals(eval_records[index])
        for entry in evals:
            coord = entry['coord']
            value = entry['value']
            # Convert from [-1.0, 0.0, 1.0] -> [1, 255].
            value = int(round(value * 127)) + 128
            state = feature.convert_state(reversi, coord, np.uint8)
            write_example(fout, state, value)

        coord = feature.move_to_coord(actual_move)
        flipped = reversi.make_move(coord)
        feature.increase_flipped(coord, flipped)
        reversi.next_turn(False)


def compress(filename):
    with open(filename, 'rb') as fin:
        with gzip.open(filename + '.gz', 'wb') as fout:
            shutil.copyfileobj(fin, fout)
    os.remove(filename)


def output_dataset(input_path, output_path, filenames):
    dataset_file = "{0}.tfrecords".format(output_path)  # NHWC uint8

    with tf.io.TFRecordWriter(dataset_file) as fout:
        for i, filename in enumerate(filenames):
            if i % 100 == 0:
                print("{0}: {1}".format(i, filename))

            file = os.path.join(input_path, filename)
            with open(file, "r") as file:
                move_record = file.readline().strip()
                eval_records = [x.strip() for x in file.readlines()]
            write_record(fout, move_record, eval_records)

    compress(dataset_file)


def main(args):
    input_path = "../resources/records/"

    with zipfile.ZipFile(os.path.join(input_path, "kifu102245.zip")) as records_zip:
        namelist = records_zip.namelist()

        step = 1000
        for i, pos in enumerate(range(0, 102000, step)):
            filenames = namelist[pos: pos + step]
            records_zip.extractall(input_path, filenames)

            output_path = "../resources/REVERSI_data/kifu1k{:02d}".format(i)
            output_dataset(input_path, output_path, filenames)

            for file in glob.glob(os.path.join(input_path, "kifu*.txt")):
                os.remove(file)

    return 0


if __name__ == "__main__":
    exit_code = main(sys.argv)
    sys.exit(exit_code)
