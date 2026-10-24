# Copyright (C) 2023, Advanced Micro Devices, Inc. All rights reserved.
# SPDX-License-Identifier: BSD-3-Clause

from dependencies import value

from brevitas.core.bit_width import BitWidthImplType
from brevitas.core.quant import QuantType
from brevitas.core.restrict_val import FloatToIntImplType, RestrictValueType
from brevitas.core.scaling import ScalingImplType, ScalingPerOutputType
from brevitas.core.zero_point import ZeroZeroPoint
from brevitas.inject import ExtendedInjector
from brevitas.quant.solver import ActQuantSolver, WeightQuantSolver
from brevitas.quant.experimental.float_base import ScaledFloatWeightBase, FloatActBase, Fp4e2m1Mixin


class Fp4e3m0Mixin(ExtendedInjector):
    bit_width = 4
    exponent_bit_width = 3
    mantissa_bit_width = 0
    saturating = True


class Int4WeightQuant(Fp4e3m0Mixin,
                      ScaledFloatWeightBase):
    scaling_per_output_type = ScalingPerOutputType.TENSOR

    @value
    def exponent_bias(exponent_bit_width):
        return 1


class Int4ActQuant(Fp4e2m1Mixin,
                   FloatActBase,
                   ActQuantSolver):
    scaling_impl_type = ScalingImplType.CONST
    scaling_per_output_channel = False
    restrict_scaling_type = RestrictValueType.FP
    scaling_const = 1.0
    max_val = 0.5
    min_val = -0.5


class CommonQuant(ExtendedInjector):
    bit_width_impl_type = BitWidthImplType.CONST
    scaling_impl_type = ScalingImplType.CONST
    restrict_scaling_type = RestrictValueType.FP
    zero_point_impl = ZeroZeroPoint
    float_to_int_impl_type = FloatToIntImplType.ROUND
    scaling_per_output_channel = False
    narrow_range = True
    signed = True

    @value
    def quant_type(bit_width):
        if bit_width is None:
            return QuantType.FP
        elif bit_width == 1:
            return QuantType.BINARY
        else:
            return QuantType.INT


class CommonWeightQuant(CommonQuant, WeightQuantSolver):
    scaling_const = 1.0


class CommonActQuant(CommonQuant, ActQuantSolver):
    min_val = -1.0
    max_val = 1.0
