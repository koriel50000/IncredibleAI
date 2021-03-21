package com.github.koriel50000.prelude.learning;

import com.github.koriel50000.prelude.reversi.Bits;

public class BitFeature {

    private static final int[] REGION = new int[]{
            8, 0, 0, 0, 2, 2, 2, 10,
            1, 8, 0, 0, 2, 2, 10, 3,
            1, 1, 8, 0, 2, 10, 3, 3,
            1, 1, 1, 8, 10, 3, 3, 3,
            5, 5, 5, 12, 14, 7, 7, 7,
            5, 5, 12, 4, 6, 14, 7, 7,
            5, 12, 4, 4, 6, 6, 14, 7,
            12, 4, 4, 4, 6, 6, 6, 14
    };

    private BitState currentState;

    public void clear() {
        currentState = new BitState();
    }

    public void setState(BitState state) {
        enumerateOddEven(state, state.coord);
        increaseFlipped(state, state.flipped | state.coord);
        this.currentState = state;
    }

    /**
     * 対角位置の対称変換が必要か
     */
    private boolean isSymmetric(int diagonal, long player, long opponent) {
        // 着手が左上部(0,8)領域となるように盤面を反転する
        long player_;
        long opponent_;
        switch (diagonal) {
            case 8:
                // 変換なし
                player_ = player;
                opponent_ = opponent;
                break;
            case 10:
                // 左右反転
                player_ = Bits.flipLtRt(player);
                opponent_ = Bits.flipLtRt(opponent);
                break;
            case 12:
                // 上下反転
                player_ = Bits.flipUpDn(player);
                opponent_ = Bits.flipUpDn(opponent);
                break;
            case 14:
                // 上下左右反転
                player_ = Bits.flip(player);
                opponent_ = Bits.flip(opponent);
                break;
            default:
                throw new IllegalArgumentException("no match: " + diagonal);
        }

        // 右上三角領域を左上から転置行列と比較して差分を求める
        long mask = 0x7F3F1F0F07030100L;
        long tPlayer_ = Bits.transposed(player_);
        long tOpponent_ = Bits.transposed(opponent_);

        long pDiff = (player_ & mask) ^ (tPlayer_ & mask);
        long oDiff = (opponent_ & mask) ^ (tOpponent_ & mask);
        int pPos = Bits.countLeadingZeros(pDiff);
        int oPos = Bits.countLeadingZeros(oDiff);

        // 最初の差分が自石のときtrue、それ以外はfalse
        return pPos <= oPos && (player_ & Bits.coordAt(pPos)) != 0;
    }

    /**
     * 領域を判定する
     */
    private int checkRegion(long player, long opponent, int index) {
        int region = REGION[index];
        if (region >= 8 && isSymmetric(region, player, opponent)) {
            region += 1; // 対角線の領域を補正
        }
        return region;
    }

    /**
     * 反転数を増分する
     */
    private void increaseFlipped(BitState state, long flipped) {
        long first = state.flippedBoard2 | state.flippedBoard3;
        long second = state.flippedBoard4 | state.flippedBoard5;
        long all = state.flippedBoard1 | first | second | state.flippedBoard6;

        while (flipped != 0) {
            long coord = Bits.getRightmostBit(flipped);

            if ((all & coord) == 0) {
                state.flippedBoard1 ^= coord;
            } else if ((state.flippedBoard1 & coord) != 0) {
                state.flippedBoard1 ^= coord;
                state.flippedBoard2 ^= coord;
            } else if ((first & coord) != 0) {
                if ((state.flippedBoard2 & coord) != 0) {
                    state.flippedBoard2 ^= coord;
                    state.flippedBoard3 ^= coord;
                } else {
                    state.flippedBoard3 ^= coord;
                    state.flippedBoard4 ^= coord;
                }
            } else if ((second & coord) != 0) {
                if ((state.flippedBoard4 & coord) != 0) {
                    state.flippedBoard4 ^= coord;
                    state.flippedBoard5 ^= coord;
                } else {
                    state.flippedBoard5 ^= coord;
                    state.flippedBoard6 ^= coord;
                }
            }

            flipped ^= coord;
        }
    }

    private static class Segment {

        int y;
        long line;

        Segment(int y, long line) {
            this.y = y;
            this.line = line;
        }
    }

    /**
     * 塗りつぶしで領域を分割する
     *
     * @see <a href="https://www.hiramine.com/programming/graphics/2d_seedfill.html"/>
     */
    private long partitionScan(long area, long coord) {
        Segment[] segments = new Segment[32]; // たぶん十分なはず
        int pos = 0;
        int y = Bits.indexOf(coord) & 0x38;
        long part = Bits.scanLine(area & 0xff00000000000000L >>> y, coord, false);
        area ^= part;
        segments[pos++] = new Segment(y, part);

        while (pos > 0) {
            Segment seg = segments[--pos];
            long areaUp = area & 0xffL << (64 - seg.y);
            long areaDn = area & 0xff00000000000000L >>> (seg.y + 8);
            boolean rightmost;
            // 上側
            rightmost = false;
            long segUp = area & (seg.line << 8);
            while (segUp != 0) {
                long seed = Bits.getRightmostBit(segUp);
                long line = Bits.scanLine(areaUp, seed, rightmost);
                rightmost = true;
                part |= line;
                area ^= line;
                segments[pos++] = new Segment(seg.y - 8, line);
                segUp ^= line;
            }
            // 下側
            rightmost = false;
            long segDn = area & (seg.line >>> 8);
            while (segDn != 0) {
                long seed = Bits.getRightmostBit(segDn);
                long line = Bits.scanLine(areaDn, seed, rightmost);
                rightmost = true;
                part |= line;
                area ^= line;
                segments[pos++] = new Segment(seg.y + 8, line);
                segDn ^= line;
            }
        }
        return part;
    }

    /**
     * 領域を分割する
     */
    private long partition(BitState state, long area, long coord) {
        if ((area & coord) != 0) {
            long part = partitionScan(area, coord);
            if (Bits.populationCount(part) % 2 == 0) {
                state.oddArea &= ~part;
                state.evenArea |= part;
                state.evenCount++;
            } else {
                state.oddArea |= part;
                state.evenArea &= ~part;
                state.oddCount++;
            }
            area ^= part;
        }
        return area;
    }

    /**
     * 偶数領域・奇数領域を集計する
     */
    private void enumerateOddEven(BitState state, long coord) {
        state.earlyTurn = state.earlyTurn && (coord & 0xff818181818181ffL) == 0;
        if ((state.oddArea & coord) != 0) {
            // 奇数領域に着手
            state.oddArea ^= coord;
            --state.oddCount;
            // 上
            long area = state.oddArea;
            area = partition(state, area, coord << 8);
            // 下
            area = partition(state, area, coord >>> 8);
            // 左
            area = partition(state, area, (coord & 0x7f7f7f7f7f7f7f7fL) << 1);
            // 右
            partition(state, area, (coord >>> 1) & 0x7f7f7f7f7f7f7f7fL);
        } else {
            // 偶数領域に着手
            state.evenArea ^= coord;
            --state.evenCount;
            // 上
            long area = state.evenArea;
            area = partition(state, area, coord << 8);
            // 下
            area = partition(state, area, coord >>> 8);
            // 左
            area = partition(state, area, (coord & 0x7f7f7f7f7f7f7f7fL) << 1);
            // 右
            partition(state, area, (coord >>> 1) & 0x7f7f7f7f7f7f7f7fL);
        }
    }

    /**
     * 石を置いたときの特徴量を返す
     */
    public BitState convertState(long player, long opponent, long flipped, long coord, int index, int depth) {
        BitState state = new BitState(currentState);
        state.region = checkRegion(player, opponent, index);
        state.emptyCount = depth;
        state.player = player;
        state.opponent = opponent;
        state.flipped = flipped;
        state.coord = coord;

        state.convertBuffer();
        return state;
    }
}
