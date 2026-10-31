#include "layers.hpp"

namespace Conv2D {
	void windowize(const int h, const int w, const int st, const bool depthwise,
		block_data_t& inb, fifo<data_t>& outs)
	{
		Window<KERNEL, KERNEL, data_t, block_data_t> winb;

		windowize_h: for (int y = 0; y < HEIGHT * 2; y++) {
			// @see UG1399, Vitis HLS Coding Styles > Loops > Variable Loop Bounds
			if (y < h) {
				winb.reset_center_col();
				winb.insert_right_col(h, w, depthwise, inb, y, 0);
				windowize_w: for (int x = 0; x < WIDTH * 2; x++) {
#pragma HLS pipeline
					if (x < w) {
						// buffering
						winb.shift_pixels_left();
						winb.insert_right_col(h, w, depthwise, inb, y, x + 1);
			 			// output
						if (x % st == 0 && y % st == 0) {
							if (depthwise) {
								outs.write(data_t(0));
							}
							for (int k = 0; k < KERNEL * KERNEL; k++) {
								outs.write(winb[k]);
							}
						}
					}
				}
			}
		}
	}

	void compute(const int h, const int w, const int f, const ConvMode& mode,
		const block_conv_t& wi, const block_thr_t& thr, fifo<data_t>& ins, fifo<data_t>& outs)
	{
		compute_hw: for (int i = 0; i < HEIGHT * WIDTH; i++) {
			if (i < h * w) {
				// input
				data_t inputs[KERNEL * KERNEL + 1];
#pragma HLS array_partition variable=inputs complete

				compute_k1: for (int k = 0; k < KERNEL * KERNEL + 1; k++) {
#pragma HLS pipeline
					if (k < mode.input) {
						inputs[k] = ins.read();
					}
				}

				data_t oval = 0;
				compute_f: for (int j = 0; j < FILTER; j++) {
					if (j < f) {
						if (mode.depthwise) {
							compute_k2: for (int k = 1; k < KERNEL * KERNEL + 1; k++) {
#pragma HLS unroll
								inputs[0][k - 1] = inputs[k][j];
							}
						}

						// convolution
						int16_t acc = 0;
						if (mode.op == SpatialOp) {
							compute_k3: for (int k = 0; k < KERNEL * KERNEL; k++) {
#pragma HLS pipeline
								acc += muladd<CHANNEL>(inputs[k], wi[j][k]);
							}
						} else if (mode.op == FuseTopDownOp) {
							compute_k4: for (int k = 0; k < 2; k++) {
#pragma HLS pipeline
								acc += muladd<CHANNEL>(inputs[k], wi[j][0]);
							}
						} else {
							acc = muladd<CHANNEL>(inputs[0], wi[j][0]);							
						}

						// batch norm
						if (mode.relu) {
							oval[j] = batch_norm_relu(acc, thr[j]);
						} else {
							oval[j] = batch_norm(acc, thr[j]);
						}
					}
				}
				outs.write(oval);
			}
		}
	}
}

namespace MaxPool2x2 {
	void maxpool(const data_t v1, const data_t v2, data_t& ov) {
#pragma HLS inline off
		maxpool: for (int z = 0; z < CHANNEL; z++) {
#pragma HLS unroll
			ov[z] = (v1[z] > v2[z]) ? v1[z] : v2[z];
		}
	}

	void compute_h(const int ih, const int iw, block_data_t& inb, block_data_t& outb) {
		compute_h: for (int y = 0; y < HEIGHT; y++) {
			if (y < ih) {
				compute_w: for (int x = 0; x < WIDTH; x++) {
					if (x < iw) {
						data_t val1 = inb[y][x];
						x++;
						data_t val2 = inb[y][x];
						data_t oval;
						maxpool(val1, val2, oval);
						outb[y][x / 2] = oval;
					}
				}
			}
		}
	}

	void compute_v(const int oh, const int ow, block_data_t& inb, block_data_t& outb) {
		int y1 = 0;
		compute_h: for (int y = 0; y < HEIGHT; y++) {
			if (y < oh) {
				int y2 = y1 + 1;
				compute_w: for (int x = 0; x < WIDTH; x++) {
					if (x < ow) {
						data_t val1 = inb[y1][x];
						data_t val2 = inb[y2][x];
						data_t oval;
						maxpool(val1, val2, oval);
						outb[y][x] = oval;
					}
				}
				y1 = y2 + 1;
			}
		}
	}
}
