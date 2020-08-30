package com.github.koriel50000.prelude.learning;

import com.github.koriel50000.prelude.reversi.Bits;

import java.util.ArrayDeque;
import java.util.Deque;

public class BitConverter {

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

    /**
     * 再帰的にたどって領域を分割する
     */
    private long partitionRecursive(long area, long coord, long part) {
        area &= ~part;
        if ((area & coord) == 0) {
            return part;
        }
        part |= coord;
        // 上
        part = partitionRecursive(area, coord << 8, part);
        // 下
        part = partitionRecursive(area, coord >>> 8, part);
        // 左
        part = partitionRecursive(area, (coord & 0x7f7f7f7f7f7f7f7fL) << 1, part);
        // 右
        part = partitionRecursive(area, (coord >>> 1) & 0x7f7f7f7f7f7f7f7fL, part);
        return part;
    }

    /**
     * 塗りつぶしで領域を分割する
     *
     * @see <a href="https://www.hiramine.com/programming/graphics/2d_seedfill.html"/>
     */
    private long partitionScan(long area, long coord) {
        Deque<Long> segments = new ArrayDeque<>();
        long mask = 0xff00000000000000L >>> (Bits.indexOf(coord) & 0x07);
        long part = Bits.scanLine(area & mask, coord);
        area ^= part;
        segments.addFirst(part);

        while (segments.size() > 0) {
            long seg = segments.removeFirst();
            mask = 0xff00000000000000L >>> (Bits.indexOf(Bits.getRightmostBit(seg)) & 0x07);
            long areaUp = area & (mask << 8);
            long areaDn = area & (mask >>> 8);
            // 上側
            long segUp = area & (seg << 8);
            while (segUp != 0) {
                long seed = Bits.getRightmostBit(segUp);
                long fill = Bits.scanLine(areaUp, seed);
                part |= fill;
                area ^= fill;
                segments.addFirst(fill);
                segUp ^= fill;
            }
            // 下側
            long segDn = area & (seg >>> 8);
            while (segDn != 0) {
                long seed = Bits.getRightmostBit(segDn);
                long fill = Bits.scanLine(areaDn, seed);
                part |= fill;
                area ^= fill;
                segments.addFirst(fill);
                segDn ^= fill;
            }
        }
        return part;
    }

    /**
     * 領域を分割する
     */
    private long partition(BitState state, long area, long coord) {
        if ((area & coord) != 0) {
            //long part = partitionRecursive(area, coord, 0L);
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
     * 複数領域から偶数領域・奇数領域を集計する
     */
    private void enumerateMultiArea(BitState state, long coord) {
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
     * 単一領域から偶数領域・奇数領域を集計する
     */
    private void enumerateSingleArea(BitState state, long coord) {
        long emptyArea = (state.oddArea | state.evenArea) & ~coord;
        int index = Bits.indexOf(coord);

        long mask = Bits.rotate(0xc080000000000081L, index);
        long oneArea = 0L; // FIXME 2マス以上の閉領域は？

        // 上
        long coord_ = coord << 8;
        if ((emptyArea & (mask << 8) ^ coord_) == 0) {
            oneArea |= coord_;
        }
        // 下
        coord_ = coord >>> 8;
        if ((emptyArea & (mask >>> 8) ^ coord_) == 0) {
            oneArea |= coord_;
        }
        // 左
        coord_ = coord << 1;
        if ((emptyArea & (mask << 1 & 0xfefefefefefefefeL) ^ coord_) == 0) {
            oneArea |= coord_;
        }
        // 右
        coord_ = coord >>> 1;
        if ((emptyArea & (mask >>> 1 & 0x7f7f7f7f7f7f7f7fL) ^ coord_) == 0) {
            oneArea |= coord_;
        }

        if (oneArea != 0) {
            // FIXME oneAreaが2つ以上あった場合は？
            if ((state.oddArea & coord) != 0) {
                // 奇数領域に着手
                state.oddArea ^= coord;
                state.evenArea = 0x0000000000000000L;
                state.oddCount = 2;
                state.evenCount = 0;
            } else {
                // 偶数領域に着手
                state.oddArea = oneArea;
                state.evenArea ^= coord | oneArea;
                state.oddCount = 1;
                state.evenCount = 1;
            }
        } else {
            if ((state.oddArea & coord) != 0) {
                // 奇数領域に着手
                state.evenArea |= state.oddArea ^ coord;
                state.oddArea = 0x0000000000000000L;
                state.evenCount = 1;
                state.oddCount = 0;
            } else {
                // 偶数領域に着手
                state.oddArea |= state.evenArea ^ coord;
                state.evenArea = 0x0000000000000000L;
                state.oddCount = 1;
                state.evenCount = 0;
            }
        }
    }

    /**
     * 偶数領域・奇数領域を集計する
     */
    private void enumerateOddEven(BitState state, long coord) {
        state.earlyTurn = state.earlyTurn && (coord & 0xff818181818181ffL) == 0;
//        if (state.earlyTurn && (state.oddCount + state.evenCount) == 1) {
//            enumerateSingleArea(state, coord);
//        } else {
//            enumerateMultiArea(state, coord);
//        }
        // FIXME 単一領域の集計は考慮漏れがあるため使用しない
        enumerateMultiArea(state, coord);
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
