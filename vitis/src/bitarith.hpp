#pragma once
#include "types.hpp"

inline int16_t mul(const uint4_t v, const uint4_t w) {
	static const ap_int<5> v_lut[] = {
		0, 1, 2, 3, 4, 6, 8, 12,
		0, -1, -2, -3, -4, -6, -8, -12,
	};

    if (w == 0xf) {
        return 0;        
    } else {
    	bool_t sign = v[3] ^ w[3];
	    int16_t oval = v_lut[(sign, v(2, 0))];
        return oval << w(2, 0);
    }
}

template <int C>
int16_t muladd(const int_t<C> vi, const int_t<C> wi) {
	static int16_t t[C];
#pragma HLS array_partition variable=t complete

	mul: for (int i = 0; i < C; i++) {
#pragma HLS unroll
		t[i] = mul(vi[i], wi[i]);
	}

	add_outer : for (int d = 1; d < C; d *= 2) {
		add_inner : for (int i = 0; i < C; i += d * 2) {
			t[i] += t[i + d];
		}
	}
	return t[0];
}

inline uint4_t batch_norm_relu(const int16_t acc, const int16_t thr[]) {
    if (acc < thr[3]) {
        if (acc < thr[1]) {
            return (acc < thr[0]) ? 0 : 1;
        } else {
            return (acc < thr[2]) ? 2 : 3;
        }
    } else {
        if (acc < thr[5]) {
            return (acc < thr[4]) ? 4 : 5;
        } else {
            return (acc < thr[6]) ? 6 : 7;
        }
    }
}

inline uint4_t batch_norm(const int16_t acc, const int16_t thr[]) {
    if (acc < thr[7]) {
        if (acc < thr[3]) {
            if (acc < thr[1]) {
                return (acc < thr[0]) ? 15 : 14;
            } else {
                return (acc < thr[2]) ? 13 : 12;
            }
        } else {
            if (acc < thr[5]) {
                return (acc < thr[4]) ? 11 : 10;
            } else {
                return (acc < thr[6]) ? 9 : 0;
            }
        }
    } else {
        if (acc < thr[11]) {
            if (acc < thr[9]) {
                return (acc < thr[8]) ? 1 : 2;
            } else {
                return (acc < thr[10]) ? 3 : 4;
            }
        } else {
            if (acc < thr[13]) {
                return (acc < thr[12]) ? 5 : 6;
            } else {
                return 7;
            }
        }
    }
}
