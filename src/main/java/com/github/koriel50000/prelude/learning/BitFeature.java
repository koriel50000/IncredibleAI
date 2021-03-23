package com.github.koriel50000.prelude.learning;

import com.github.koriel50000.prelude.util.Bits;

import java.util.Arrays;

public class BitFeature {

    public static final int ROWS = 8;
    public static final int COLUMNS = 8;
    public static final int CHANNELS = 16;

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

    public BitFeature() {
    }

    public void clear() {
        currentState = new BitState();
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

    private BitState createState(long player, long opponent, long flipped, long coord, int index) {
        BitState state = new BitState(currentState);
        state.player = player;
        state.opponent = opponent;
        state.flipped = flipped;
        state.coord = coord;
        state.region = checkRegion(player, opponent, index); // 新しい盤面で領域を確認
        return state;
    }

    /**
     * 石を置いたときの特徴量を返す
     */
    public float[] getStateBuffer(long player, long opponent, long flipped, long coord, int index) {
        BitState state = createState(player, opponent, flipped, coord, index);
        return state.convertBuffer();
    }

    public void setState(long player, long opponent, long flipped, long coord, int index) {
        BitState state = createState(player, opponent, flipped, coord, index);
        --state.emptyCount;  // 空白は必ず1つずつ減る
        state.earlyTurn = state.earlyTurn && (coord & 0xff818181818181ffL) == 0;  // 周囲に着手するまでは序盤
        enumerateOddEven(state, coord);  // 偶数領域・奇数領域を集計する
        increaseFlipped(state, flipped | coord);  // 反転数を増分する
        this.currentState = state;
    }

    private static class BitState {

        long oddArea;
        long evenArea;
        int oddCount;
        int evenCount;
        int emptyCount;
        boolean earlyTurn;
        long flippedBoard1;
        long flippedBoard2;
        long flippedBoard3;
        long flippedBoard4;
        long flippedBoard5;
        long flippedBoard6;

        long player;
        long opponent;
        long flipped;
        long coord;
        int region;

        BitState() {
            oddArea = 0x0000000000000000L;
            evenArea = 0xffffffe7e7ffffffL;
            oddCount = 0;
            evenCount = 1;
            emptyCount = 60;
            earlyTurn = true;
            flippedBoard1 = 0x0000001818000000L;
            flippedBoard2 = 0x0000000000000000L;
            flippedBoard3 = 0x0000000000000000L;
            flippedBoard4 = 0x0000000000000000L;
            flippedBoard5 = 0x0000000000000000L;
            flippedBoard6 = 0x0000000000000000L;
        }

        BitState(BitState state) {
            this.oddArea = state.oddArea;
            this.evenArea = state.evenArea;
            this.oddCount = state.oddCount;
            this.evenCount = state.evenCount;
            this.emptyCount = state.emptyCount;
            this.earlyTurn = state.earlyTurn;
            this.flippedBoard1 = state.flippedBoard1;
            this.flippedBoard2 = state.flippedBoard2;
            this.flippedBoard3 = state.flippedBoard3;
            this.flippedBoard4 = state.flippedBoard4;
            this.flippedBoard5 = state.flippedBoard5;
            this.flippedBoard6 = state.flippedBoard6;
        }

        private void put(float[] buffer, int channel, long coords) {
            long coords_;
            switch (region & 0x06) {
                case 0:
                    // 変換なし
                    coords_ = coords;
                    break;
                case 2:
                    // 左右反転
                    coords_ = Bits.flipLtRt(coords);
                    break;
                case 4:
                    // 上下反転
                    coords_ = Bits.flipUpDn(coords);
                    break;
                case 6:
                    // 上下左右反転
                    coords_ = Bits.flip(coords);
                    break;
                default:
                    throw new IllegalArgumentException("no match: " + region);
            }
            if ((region & 0x01) != 0) {
                coords_ = Bits.transposed(coords_);
            }

            int offset = ROWS * COLUMNS * channel;
            while (coords_ != 0) {
                long coord = Bits.getRightmostBit(coords_);
                int index = Bits.indexOf(coord);
                buffer[offset + index] = 1;
                coords_ ^= coord;
            }
        }

        private void fill(float[] buffer, int channel) {
            int offset = ROWS * COLUMNS * channel;
            Arrays.fill(buffer, offset, offset + ROWS * COLUMNS, 1);
        }

        /**
         * 盤面を特徴量に変換する
         */
        float[] convertBuffer() {
            float[] buffer = new float[ROWS * COLUMNS * CHANNELS];

            put(buffer, 0, player); // 着手前に自石
            put(buffer, 1, opponent); // 着手前に相手石
            put(buffer, 2, ~(player | opponent)); // 着手前に空白
            put(buffer, 3, coord); // 着手
            put(buffer, 4, flipped | coord); // 変化した石
            if (oddArea != 0) {
                put(buffer, 5, oddArea); // 奇数領域
            }
            if (evenArea != 0) {
                put(buffer, 6, evenArea); // 偶数領域
            }
            if (!earlyTurn) {
                fill(buffer, 7); // 序盤でない
            }
            if (emptyCount % 2 == 1) {
                fill(buffer, 8); // 空白数が奇数
            }
            if (oddCount == 1 || oddCount % 2 == 0) {
                fill(buffer, 9); // 奇数領域が1個または偶数
            }
            if (flippedBoard1 != 0) {
                put(buffer, 10, flippedBoard1); // 反転数1
            }
            if (flippedBoard2 != 0) {
                put(buffer, 11, flippedBoard2); // 反転数2
            }
            if (flippedBoard3 != 0) {
                put(buffer, 12, flippedBoard3); // 反転数3
            }
            if (flippedBoard4 != 0) {
                put(buffer, 13, flippedBoard4); // 反転数4
            }
            if (flippedBoard5 != 0) {
                put(buffer, 14, flippedBoard5); // 反転数5
            }
            if (flippedBoard6 != 0) {
                put(buffer, 15, flippedBoard6); // 反転数6
            }

            return buffer;
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
}
