# -*- coding: utf-8 -*-

import shutil
import torch

from models import model_with_cfg

from brevitas.export import FINNManager
from qonnx.core.modelwrapper import ModelWrapper
from qonnx.core.datatype import DataType

from qonnx.transformation.general import GiveReadableTensorNames, GiveUniqueNodeNames, RemoveStaticGraphInputs
from qonnx.transformation.infer_shapes import InferShapes
from qonnx.transformation.infer_datatypes import InferDataTypes
from qonnx.transformation.fold_constants import FoldConstants


def load_checkpoint(model, path):
    print('Loading model checkpoint at: {}'.format(path))
    package = torch.load(path, map_location='cpu')
    model_state_dict = package['state_dict']
    model.load_state_dict(model_state_dict, strict=False)


def export(model, path):
    FINNManager.export(model, input_shape=(1, 16, 8, 8), export_path=path)


def network_preparation(network, brevitas_path, finn_build_path):
    model = ModelWrapper('{}/{}.onnx'.format(brevitas_path, network))

    # add input quantization annotation: UINT8 for all BNN-PYNQ models
    global_inp_name = model.graph.input[0].name
    model.set_tensor_datatype(global_inp_name, DataType["UINT8"])
    global_outp_name = model.graph.output[0].name
    model.set_tensor_datatype(global_outp_name, DataType["INT8"])

    # tidy-up
    model = model.transform(InferShapes())
    model = model.transform(FoldConstants())
    model = model.transform(GiveUniqueNodeNames())
    model = model.transform(GiveReadableTensorNames())
    model = model.transform(InferDataTypes())
    model = model.transform(RemoveStaticGraphInputs())


    chkpt_name = '{}/{}_tidy.onnx'.format(brevitas_path, network)
    model.save(chkpt_name)

    shutil.copy(chkpt_name, '{}/{}.onnx'.format(finn_build_path, network))


def main():
    network = 'CNN_2W2A'
    brevitas_path = '../../resources/brevitas/experiments'
    resume_path = '{}/{}/checkpoints/checkpoint.tar'.format(brevitas_path, network)
    finn_build_path = '../../resources/finn-examples/build/bnn-pynq/models'

    model, cfg = model_with_cfg(network, False)
    load_checkpoint(model, resume_path)
    export_path = '{}/{}.onnx'.format(brevitas_path, network)
    export(model, export_path)
    network_preparation(network, brevitas_path, finn_build_path)


if __name__ == "__main__":
    main()
