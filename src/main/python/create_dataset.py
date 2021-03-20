# -*- coding: utf-8 -*-

import sys
import os
import zipfile
import gzip
import shutil
import tensorflow as tf

import reversi
import oddeven_feature as feature


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
            state = feature.convert_state(reversi, coord)
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
    dataset_file = "{0}.tfrecords".format(output_path)  # NHWC float32

    with tf.io.TFRecordWriter(dataset_file) as fout:
        for i, filename in enumerate(filenames):
            if i % 100 == 0:
                print("{0}: {1}".format(i, filename))

            file = os.path.join(input_path, filename)
            with open(file, "r") as file:
                move_line = file.readline().strip()
                eval_lines = [x.strip() for x in file.readlines()]
            write_record(fout, move_line, eval_lines)

    compress(dataset_file)


def main(args):
    input_path = "../resources/records/"

    with zipfile.ZipFile(os.path.join(input_path, "kifu102245.zip")) as records_zip:
        namelist = records_zip.namelist()

        step = 1000
        for i, pos in enumerate(range(101000, 102000, step)):
            filenames = namelist[pos: pos + step]
            records_zip.extractall(input_path, filenames)

            output_path = "../resources/REVERSI_data/train1k{:02d}".format(i)
            output_dataset(input_path, output_path, filenames)

            for filename in filenames:
                os.remove(os.path.join(input_path, filename))

    return 0


if __name__ == "__main__":
    exit_code = main(sys.argv)
    sys.exit(exit_code)
