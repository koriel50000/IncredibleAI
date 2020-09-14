# -*- coding: utf-8 -*-

import sys
import glob
import os.path
import struct
import zipfile
import gzip
import shutil
import numpy as np

import reversi
import converter


def write8(file, value):
    file.write(struct.pack("B", value))


def write32(file, value):
    file.write(struct.pack(">I", value))


def write_magic(states_file, labels_file):
    magic = 0x0803
    items = 60  # dummy
    write32(states_file, magic)
    write32(states_file, items)
    write32(states_file, reversi.COLUMNS)
    write32(states_file, reversi.ROWS)
    write32(states_file, reversi.CHANNELS)
    
    magic = 0x0801
    write32(labels_file, magic)
    write32(labels_file, items)


def write_items(states_file, labels_file, total):
    states_file.seek(4)
    write32(states_file, total)
    labels_file.seek(4)
    write32(labels_file, total)


def write_data(states_file, labels_file, state, value):
    # FIXME NHWC or NCHWとは？ NHWC(channels_last)になっていないのになぜこれで結果が出る？
    for channel in range(0, reversi.CHANNELS):
        for y in range(0, reversi.ROWS):
            for x in range(0, reversi.COLUMNS):
                write8(states_file, state[channel, y, x])
    # Convert from [-1.0, 0.0, 1.0] -> [1, 255].
    value = int(round(value * 127)) + 128
    write8(labels_file, value)


#
# 棋譜を学習データに変換する
#
def write_records(states_file, labels_file, move_record, eval_record):
    reversi.clear()
    
    count = 0
    for index, actual_move in enumerate(converter.convert_moves(move_record)):
        # print("index:{0} move:{1}".format(index, actual_move))
        while True:
            if len(reversi.available_moves()) > 0:
                break
            print("Pass!")
            if reversi.has_completed(True):
                raise Exception("Error!")  # 先手・後手両方パスで終了は考慮しない
            reversi.next_turn(True)

        evals = converter.convert_evals(eval_record[index])
        for entry in evals:
            coord = entry['coord']
            value = entry['value']
            state = reversi.convert_state(coord, dtype=np.uint8)
            write_data(states_file, labels_file, state, value)
            count += 1
        coord = converter.move_to_coord(actual_move)
        reversi.make_move(coord)
        reversi.next_turn(False)
    
    return count


def compress(filename):
    with open(filename, 'rb') as fin:
        with gzip.open(filename + '.gz', 'wb') as fout:
            shutil.copyfileobj(fin, fout)
    os.remove(filename)


#
# 指定した範囲の棋譜を学習データファイルに保存する
#
def save_datafile(prefix, path, filenames):
    states_filename = '{0}-states-idx4-ubyte'.format(prefix)
    labels_filename = '{0}-labels-idx1-ubyte'.format(prefix)
    
    with open(states_filename, 'wb') as states_file, \
            open(labels_filename, 'wb') as labels_file:
        
        write_magic(states_file, labels_file)
        
        total = 0
        for i, filename in enumerate(filenames):
            file = os.path.join(path, filename)
            if i % 100 == 0:
                print("{0}: {1}".format(i, filename))

            with open(file, "r") as file:
                move_record = file.readline().strip()
                eval_record = [x.strip() for x in file.readlines()]
            
            count = write_records(states_file, labels_file, move_record, eval_record)
            total += count
            
            states_file.flush()
            labels_file.flush()
        
        write_items(states_file, labels_file, total)
    
    compress(states_filename)
    compress(labels_filename)


#
# メイン
#
def main(args):
    path = "../resources/records/"
    with zipfile.ZipFile(os.path.join(path, "kifu102245.zip")) as records_zip:

        namelist = records_zip.namelist()
        step = 1000
        for i, pos in enumerate(range(0, 102000, step)):
            filenames = namelist[pos: pos + step]
            records_zip.extractall(path, filenames)

            prefix = "../resources/REVERSI_data/train1k{:02d}".format(i)
            save_datafile(prefix, path, filenames)

            for file in glob.glob(os.path.join(path, "kifu*.txt")):
                os.remove(file)

    return 0


if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)
