import shutil
from qonnx.core.modelwrapper import ModelWrapper
from brevitas.export import FINNManager

from qonnx.transformation.general import GiveReadableTensorNames, GiveUniqueNodeNames, RemoveStaticGraphInputs
from qonnx.transformation.infer_shapes import InferShapes
from qonnx.transformation.infer_datatypes import InferDataTypes
from qonnx.transformation.fold_constants import FoldConstants

from finn.util.pytorch import ToTensor
from qonnx.transformation.merge_onnx_models import MergeONNXModels
from qonnx.core.datatype import DataType

from qonnx.transformation.insert_topk import InsertTopK

from finn.transformation.streamline import Streamline
from finn.transformation.streamline.reorder import MoveScalarLinearPastInvariants
import finn.transformation.streamline.absorb as absorb

from qonnx.transformation.bipolar_to_xnor import ConvertBipolarMatMulToXnorPopcount
from finn.transformation.streamline.round_thresholds import RoundAndClipThresholds
from qonnx.transformation.infer_data_layouts import InferDataLayouts
from qonnx.transformation.general import RemoveUnusedTensors


def main():
    network = 'CNN_2W2A'
    brevitas_path = '../../resources/brevitas/experiments'
    finn_build_path = '../../resources/finn-examples/build/bnn-pynq/models'

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

    # # move initial Mul (from preproc) past the Reshape
    # model = model.transform(MoveScalarLinearPastInvariants())
    # # streamline
    # model = model.transform(Streamline())
    # model.save('{}/{}_streamlined.onnx'.format(brevitas_path, network))
    #
    # model = model.transform(ConvertBipolarMatMulToXnorPopcount())
    # model = model.transform(absorb.AbsorbAddIntoMultiThreshold())
    # model = model.transform(absorb.AbsorbMulIntoMultiThreshold())
    # # absorb final add-mul nodes into TopK
    # model = model.transform(absorb.AbsorbScalarMulAddIntoTopK())
    # model = model.transform(RoundAndClipThresholds())
    #
    # # bit of tidy-up
    # model = model.transform(InferDataLayouts())
    # model = model.transform(RemoveUnusedTensors())
    #
    # model.save('{}/{}_ready_for_hls_conversion.onnx'.format(brevitas_path, network))


if __name__ == "__main__":
    main()
