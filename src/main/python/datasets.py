import gzip
import collections

import numpy
import tensorflow as tf

Datasets = collections.namedtuple('Datasets', ['train'])


def _read32(bytestream):
    dt = numpy.dtype(numpy.uint32).newbyteorder('>')
    return numpy.frombuffer(bytestream.read(4), dtype=dt)[0]


def extract_images(f):
    """Extract the images into a 4D uint8 numpy array [index, y, x, depth].

    Args:
      f: A file object that can be passed into a gzip reader.

    Returns:
      data: A 4D uint8 numpy array [index, y, x, depth].

    Raises:
      ValueError: If the bytestream does not start with 2051.
    """
    print('Extracting', f.name)
    with gzip.GzipFile(fileobj=f) as bytestream:
        magic = _read32(bytestream)
        if magic != 2051:
            raise ValueError('Invalid magic number %d in REVERSI image file: %s' %
                             (magic, f.name))
        num_images = _read32(bytestream)
        rows = _read32(bytestream)
        cols = _read32(bytestream)
        depth = _read32(bytestream)
        buf = bytestream.read(num_images * rows * cols * depth)
        data = numpy.frombuffer(buf, dtype=numpy.uint8)
        data = data.reshape(num_images, rows, cols, depth)
        return data


def extract_labels(f):
    """Extract the labels into a 1D uint8 numpy array [index].

    Args:
      f: A file object that can be passed into a gzip reader.

    Returns:
      labels: a 1D uint8 numpy array.

    Raises:
      ValueError: If the bystream doesn't start with 2049.
    """
    print('Extracting', f.name)
    with gzip.GzipFile(fileobj=f) as bytestream:
        magic = _read32(bytestream)
        if magic != 2049:
            raise ValueError('Invalid magic number %d in REVERSI label file: %s' %
                             (magic, f.name))
        num_items = _read32(bytestream)
        buf = bytestream.read(num_items)
        labels = numpy.frombuffer(buf, dtype=numpy.uint8)
        return labels


class DataSet(object):

    def __init__(self,
                 images,
                 labels,
                 dtype=tf.dtypes.float32,
                 reshape=True):
        """Construct a DataSet.
        one_hot arg is used only if fake_data is true.  `dtype` can be either
        `uint8` to leave the input as `[0, 255]`, or `float32` to rescale into
        `[0, 1]`.  Seed arg provides for convenient deterministic testing.
        """
        dtype = tf.dtypes.as_dtype(dtype).base_dtype
        if dtype not in (tf.dtypes.uint8, tf.dtypes.float32):
            raise TypeError('Invalid image dtype %r, expected int8 or float32' %
                            dtype)
        assert images.shape[0] == labels.shape[0], (
                'images.shape: %s labels.shape: %s' % (images.shape, labels.shape))
        self._num_examples = images.shape[0]

        # Convert shape from [num examples, rows, columns, depth]
        # to [num examples, rows*columns*depth] (assuming depth == 1)
        if reshape:
            # assert images.shape[3] == 1
            images = images.reshape(images.shape[0],
                                    images.shape[1] * images.shape[2] * images.shape[3])
        if dtype == tf.dtypes.float32:
            # Convert from [1, 255] -> [-1.0, 0.0, 1.0]
            labels = numpy.subtract(numpy.multiply(labels, 2.0 / 255.0), 1.0)
        self._images = images
        self._labels = labels
        self._epochs_completed = 0
        self._index_in_epoch = 0

    @property
    def images(self):
        return self._images

    @property
    def labels(self):
        return self._labels

    @property
    def num_examples(self):
        return self._num_examples

    @property
    def epochs_completed(self):
        return self._epochs_completed

    def next_batch(self, batch_size):
        """Return the next `batch_size` examples from this data set."""
        start = self._index_in_epoch
        # Go to the next epoch
        if start + batch_size > self._num_examples:
            # Finished epoch
            self._epochs_completed += 1
            # Get the rest examples in this epoch
            rest_num_examples = self._num_examples - start
            images_rest_part = self._images[start:self._num_examples]
            labels_rest_part = self._labels[start:self._num_examples]
            # Start next epoch
            start = 0
            self._index_in_epoch = batch_size - rest_num_examples
            end = self._index_in_epoch
            images_new_part = self._images[start:end]
            labels_new_part = self._labels[start:end]
            return numpy.concatenate((images_rest_part, images_new_part), axis=0), numpy.concatenate(
                (labels_rest_part, labels_new_part), axis=0)
        else:
            self._index_in_epoch += batch_size
            end = self._index_in_epoch
            return self._images[start:end], self._labels[start:end]


def read_data_sets(train_dir,
                   train_prefix,
                   dtype=tf.dtypes.float32,
                   reshape=True):
    TRAIN_IMAGES = train_prefix + '-states-idx4-ubyte.gz'
    TRAIN_LABELS = train_prefix + '-labels-idx1-ubyte.gz'

    local_file = train_dir + TRAIN_IMAGES
    with open(local_file, 'rb') as f:
        train_images = extract_images(f)

    local_file = train_dir + TRAIN_LABELS
    with open(local_file, 'rb') as f:
        train_labels = extract_labels(f)

    options = dict(dtype=dtype, reshape=reshape)

    train = DataSet(train_images, train_labels, **options)

    return Datasets(train=train)
