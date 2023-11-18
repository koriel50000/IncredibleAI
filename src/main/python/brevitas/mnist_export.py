# -*- coding: utf-8 -*-

import shutil
import torch

from models import model_with_cfg

from brevitas.export import FINNManager
from qonnx.core.modelwrapper import ModelWrapper

from qonnx.transformation.general import GiveReadableTensorNames, GiveUniqueNodeNames, RemoveStaticGraphInputs
from qonnx.transformation.infer_shapes import InferShapes
from qonnx.transformation.infer_datatypes import InferDataTypes
from qonnx.transformation.fold_constants import FoldConstants

from finn.util.pytorch import ToTensor
from qonnx.transformation.merge_onnx_models import MergeONNXModels
from qonnx.core.datatype import DataType

from qonnx.transformation.insert_topk import InsertTopK


def load_checkpoint(model, path):
    print('Loading model checkpoint at: {}'.format(path))
    package = torch.load(path, map_location='cpu')
    model_state_dict = package['state_dict']
    model.load_state_dict(model_state_dict, strict=False)


def export(model, path):
    FINNManager.export(model, input_shape=(1, 1, 28, 28), export_path=path)


def network_preparation(network, brevitas_path, finn_build_path):
    model = ModelWrapper('{}/{}.onnx'.format(brevitas_path, network))

    model = model.transform(InferShapes())
    model = model.transform(FoldConstants())
    model = model.transform(GiveUniqueNodeNames())
    model = model.transform(GiveReadableTensorNames())
    model = model.transform(InferDataTypes())
    model = model.transform(RemoveStaticGraphInputs())

    model.save('{}/{}_tidy.onnx'.format(brevitas_path, network))

    global_inp_name = model.graph.input[0].name
    ishape = model.get_tensor_shape(global_inp_name)
    # preprocessing: torchvision's ToTensor divides uint8 inputs by 255
    totensor_pyt = ToTensor()
    chkpt_preproc_name = '{}/{}_preproc.onnx'.format(brevitas_path, network)
    #bo.export_finn_onnx(totensor_pyt, ishape, chkpt_preproc_name)
    FINNManager.export(totensor_pyt, input_shape=ishape, export_path=chkpt_preproc_name)

    # join preprocessing and core model
    pre_model = ModelWrapper(chkpt_preproc_name)
    model = model.transform(MergeONNXModels(pre_model))
    # add input quantization annotation: UINT8 for all BNN-PYNQ models
    global_inp_name = model.graph.input[0].name
    model.set_tensor_datatype(global_inp_name, DataType["UINT8"])

    model.save('{}/{}_with_preproc.onnx'.format(brevitas_path, network))

    # postprocessing: insert Top-1 node at the end
    model = model.transform(InsertTopK(k=1))
    chkpt_name = '{}/{}_pre_post.onnx'.format(brevitas_path, network)
    # tidy-up again
    model = model.transform(InferShapes())
    model = model.transform(FoldConstants())
    model = model.transform(GiveUniqueNodeNames())
    model = model.transform(GiveReadableTensorNames())
    model = model.transform(InferDataTypes())
    model = model.transform(RemoveStaticGraphInputs())
    model.save(chkpt_name)

    shutil.copy(chkpt_name, '{}/{}.onnx'.format(finn_build_path, network))


def main():
    network = 'TFC_2W2A'
    brevitas_path = '../../resources/brevitas/experiments'
    resume_path = '{}/{}/checkpoints/best.tar'.format(brevitas_path, network)
    finn_build_path = '../../resources/finn-examples/build/bnn-pynq/models'

    model, cfg = model_with_cfg(network, False)
    load_checkpoint(model, resume_path)
    export_path = '{}/{}.onnx'.format(brevitas_path, network)
    export(model, export_path)
    network_preparation(network, brevitas_path, finn_build_path)


if __name__ == "__main__":
    main()
