import sys
import pynq
import numpy as np

#from dataset_loading import mnist
from dataset_loading import cifar
from finn_examples import models

def main(args):
    #trainx, trainy, testx, testy, valx, valy = mnist.load_mnist_data("../../resources/finn-examples/data/mnist", download=False, one_hot=False)
    trainx, trainy, testx, testy, valx, valy = cifar.load_cifar_data("../../resources/finn-examples/data", download=False, one_hot=False)

    #accel = models.tfc_w2a2_mnist()
    accel = models.cnv_w2a2_cifar10()

    print("Expected input shape and datatype: %s %s" % (str(accel.ishape_normal()), str(accel.idt())))
    print("Expected output shape and datatype: %s %s" % (str(accel.oshape_normal()), str(accel.odt())))

    batch_size = 1000
    total = testx.shape[0]
    accel.batch_size = batch_size
    n_batches = int(total / batch_size)

    batch_imgs = testx.reshape(n_batches, batch_size, -1)
    batch_labels = testy.reshape(n_batches, batch_size)
    obuf_normal = np.empty_like(accel.obuf_packed_device)
    print("Ready to run validation, test images tensor has shape %s" % str(batch_imgs.shape))
    print("Accelerator buffer shapes are %s for input, %s for output" % (str(accel.ishape_packed()), str(accel.oshape_packed())) )

    ok = 0
    nok = 0
    for i in range(n_batches):
        ibuf_normal = batch_imgs[i].reshape(accel.ishape_normal())
        exp = batch_labels[i]
        obuf_normal = accel.execute(ibuf_normal)
        ret = np.bincount(obuf_normal.flatten() == exp.flatten())
        nok += ret[0]
        ok += ret[1]
        print("batch %d / %d : total OK %d NOK %d" % (i, n_batches, ok, nok))

    acc = 100.0 * ok / (total)
    print("Final accuracy: {}%".format(acc))

    return 0


if __name__ == "__main__":
    exit_code = main(sys.argv)
    sys.exit(exit_code)
