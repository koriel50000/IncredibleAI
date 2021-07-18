import pkg_resources as pk

import os
import sys
import platform
import pynq
import numpy as np
from dataset_loading import mnist

from finn.core.datatype import DataType
from finn_examples.driver import FINNExampleOverlay

_mnist_fc_io_shape_dict = {
    "idt": DataType.UINT8,
    "odt": DataType.UINT8,
    "ishape_normal": (1, 784),
    "oshape_normal": (1, 1),
    "ishape_folded": (1, 1, 784),
    "oshape_folded": (1, 1, 1),
    "ishape_packed": (1, 1, 784),
    "oshape_packed": (1, 1, 1),
}

def get_edge_or_pcie():
    cpu = platform.processor()
    if cpu in ["armv7l", "aarch64"]:
        return "edge"
    elif cpu in ["x86_64"]:
        return "pcie"
    else:
        raise OSError("Platform is not supported.")


def find_bitfile(model_name, target_platform):
    bitfile_exts = {"edge": "bit", "pcie": "xclbin"}
    bitfile_ext = bitfile_exts[get_edge_or_pcie()]
    bitfile_name = "%s.%s" % (model_name, bitfile_ext)
    bitfile_candidates = [
        pk.resource_filename(
            "finn_examples", "bitfiles/%s/%s" % (target_platform, bitfile_name)
        ),
        pk.resource_filename(
            "finn_examples",
            "bitfiles/bitfiles.zip.d/%s/%s" % (target_platform, bitfile_name),
        ),
    ]
    for candidate in bitfile_candidates:
        if os.path.isfile(candidate):
            return candidate
    raise Exception(
        "Bitfile for model = %s target platform = %s not found. Looked in: %s"
        % (model_name, target_platform, str(bitfile_candidates))
    )


def get_driver_mode():
    driver_modes = {"edge": "zynq-iodma", "pcie": "alveo"}
    return driver_modes[get_edge_or_pcie()]


def resolve_target_platform(target_platform):
    if target_platform is None:
        return pynq.Device.active_device.name
    else:
        assert target_platform in [x.name for x in pynq.Device.devices]
        return target_platform


def tfc_w1a1_mnist(target_platform=None):
    target_platform = resolve_target_platform(target_platform)
    driver_mode = get_driver_mode()
    model_name = "tfc-w1a1"
    filename = "./bitfiles/Pynq-Z1/finn-accel.bit"
    return FINNExampleOverlay(filename, driver_mode, _mnist_fc_io_shape_dict)

def main(args):
    trainx, trainy, testx, testy, valx, valy = mnist.load_mnist_data("./data", download=False, one_hot=False)

    accel = tfc_w1a1_mnist()

    print("Expected input shape and datatype: %s %s" % (str(accel.ishape_normal), str(accel.idt)))
    print("Expected output shape and datatype: %s %s" % (str(accel.oshape_normal), str(accel.odt)))

    batch_size = 1000
    total = testx.shape[0]
    accel.batch_size = batch_size
    n_batches = int(total / batch_size)

    batch_imgs = testx.reshape(n_batches, batch_size, -1)
    batch_labels = testy.reshape(n_batches, batch_size)
    obuf_normal = np.empty_like(accel.obuf_packed_device)
    print("Ready to run validation, test images tensor has shape %s" % str(batch_imgs.shape))
    print("Accelerator buffer shapes are %s for input, %s for output" % (str(accel.ishape_packed), str(accel.oshape_packed)) )

    ok = 0
    nok = 0
    for i in range(n_batches):
        ibuf_normal = batch_imgs[i].reshape(accel.ishape_normal)
        exp = batch_labels[i]
        obuf_normal = accel.execute(ibuf_normal)
        ret = np.bincount(obuf_normal.flatten() == exp.flatten())
        nok += ret[0]
        ok += ret[1]
        print("batch %d / %d : total OK %d NOK %d" % (i, n_batches, ok, nok))

    acc = 100.0 * ok / (total)
    print("Final accuracy: {}%".format(acc))

if __name__ == "__main__":
    exitCode = main(sys.argv)
    sys.exit(exitCode)