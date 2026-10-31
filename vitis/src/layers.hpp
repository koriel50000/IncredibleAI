#pragma once
#include "types.hpp"
#include "bitarith.hpp"
#include "window_buffer.hpp"

constexpr int WIDTH = 80;
constexpr int HEIGHT = 80;
constexpr int CHANNEL = 64;
constexpr int FILTER = 64;

constexpr int KERNEL = 3;
constexpr int THRESHOLD = 14;

constexpr int FEAT_WIDTH = WIDTH / 4;
constexpr int FEAT_HEIGHT = HEIGHT / 4;

using data_t = int_t<CHANNEL>;
using block_data_t = data_t[HEIGHT][WIDTH];
using block_feat_t = data_t[FEAT_HEIGHT][FEAT_WIDTH];
using block_conv_t = data_t[FILTER][KERNEL * KERNEL];
using block_thr_t = int16_t[FILTER][THRESHOLD];

enum ConvOp {
	SpatialOp,
	DepthwiseOp,
	DepthwiseBrOp,
	DepthwiseHeadOp,
	SingleChannelOp,
	PointwiseOp,
	PointwiseHeadOp,
	FuseTopDownOp,
};

struct ConvMode {
	ConvOp op;
	int stride;
	int input;
	bool depthwise;
	bool relu;
};

static constexpr ConvMode Spatial = { SpatialOp, 2, 9, false, true };
static constexpr ConvMode Depthwise = { DepthwiseOp, 1, 10, true, true };
static constexpr ConvMode DepthwiseBr = { DepthwiseBrOp, 1, 10, true, true };
static constexpr ConvMode DepthwiseHead = { DepthwiseHeadOp, 1, 10, true, true };
static constexpr ConvMode SingleChannel = { SingleChannelOp, 1, 10, true, false };
static constexpr ConvMode Pointwise = { PointwiseOp, 0, 1, false, false };
static constexpr ConvMode PointwiseHead = { PointwiseHeadOp, 0, 1, false, false };
static constexpr ConvMode FuseTopDown = { FuseTopDownOp, 0, 2, false, false };

namespace Conv2D {
	void windowize(const int h, const int w, const int st, const bool depthwise,
		block_data_t& inb, fifo<data_t>& outs);

	void compute(const int h, const int w, const int f, const ConvMode& type,
		const block_conv_t& wi, const block_thr_t& thr, fifo<data_t>& ins, fifo<data_t>& outs);
}

namespace MaxPool2x2 {
	void compute_h(const int ih, const int iw, block_data_t& inb, block_data_t& outb);

	void compute_v(const int oh, const int ow, block_data_t& inb, block_data_t& outb);
}
