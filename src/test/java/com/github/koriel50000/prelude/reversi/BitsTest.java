package com.github.koriel50000.prelude.reversi;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BitsTest {

    @BeforeAll
    static void setUpAll() {
        System.out.println("before all tests");
    }

    @BeforeEach
    void setUp() {
        System.out.println("before each test");
    }

    @AfterEach
    void tearDown() {
        System.out.println("after each test");
    }

    @AfterAll
    static void tearDownAll() {
        System.out.println("after all tests");
    }

    @Test
    void populationCount() {
        assertAll(
                () -> assertEquals(0, Bits.populationCount(0x0000000000000000L)),
                () -> assertEquals(1, Bits.populationCount(0x0000000100000000L)),
                () -> assertEquals(8, Bits.populationCount(0xf00000000000000fL))
        );
    }

    @Test
    void reverse() {
        assertAll(
                () -> assertEquals(0x0000000000000000L, Bits.reverse(0x0000000000000000L)),
                () -> assertEquals(0xaa00000000000008L, Bits.reverse(0x1000000000000055L)),
                () -> assertEquals(0x00005000a0005000L, Bits.reverse(0x000a0005000a0000L)),
                () -> assertEquals(0x0000000000000001L, Bits.reverse(0x8000000000000000L))
        );
    }

    @Test
    void rotate() {
        assertAll(
                () -> assertEquals(0x0000000000000000L, Bits.rotate(0x0000000000000000L, 0)),
                () -> assertEquals(0x1154000000000000L, Bits.rotate(0xaa00000000000008L, 7)),
                () -> assertEquals(0x4000a0000000a001L, Bits.rotate(0x00005000a0005000L, 31)),
                () -> assertEquals(0x0000000000000001L, Bits.rotate(0x8000000000000000L, 63))
        );
    }

    @Test
    void getRightmostBit() {
        assertAll(
                () -> assertEquals(0x0000000000000000L, Bits.getRightmostBit(0x0000000000000000L)),
                () -> assertEquals(0x0000000000000001L, Bits.getRightmostBit(0x1000000000000055L)),
                () -> assertEquals(0x0000000000020000L, Bits.getRightmostBit(0x000a0005000a0000L)),
                () -> assertEquals(0x8000000000000000L, Bits.getRightmostBit(0x8000000000000000L))
        );
    }

    @Test
    void countLeadingZeros() {
        assertAll(
                () -> assertEquals(64, Bits.countLeadingZeros(0x0000000000000000L)),
                () -> assertEquals(15, Bits.countLeadingZeros(0x0001000200040008L)),
                () -> assertEquals(0, Bits.countLeadingZeros(0x8000000080000000L))
        );
    }

    @Test
    void countTrailingZeros() {
        assertAll(
                () -> assertEquals(64, Bits.countTrailingZeros(0x0000000000000000L)),
                () -> assertEquals(31, Bits.countTrailingZeros(0x8000000080000000L)),
                () -> assertEquals(3, Bits.countTrailingZeros(0x0001000200040008L)),
                () -> assertEquals(0, Bits.countLeadingZeros(0x8000000080000001L))
        );
    }

    @Test
    void indexOf() {
        assertAll(
                () -> assertEquals(0, Bits.indexOf(0x8000000000000000L)),
                () -> assertEquals(1, Bits.indexOf(0x4000000000000000L)),
                () -> assertEquals(63, Bits.indexOf(0x0000000000000001L))
        );
    }

    @Test
    void coordAt() {
        assertAll(
                () -> assertEquals(0x8000000000000000L, Bits.coordAt(0)),
                () -> assertEquals(0x0000000020000000L, Bits.coordAt(34)),
                () -> assertEquals(0x0000000000000001L, Bits.coordAt(63))
        );
        assertAll(
                () -> assertEquals(0x8000000000000000L, Bits.coordAt(0, 0)),
                () -> assertEquals(0x0000000020000000L, Bits.coordAt(2, 4)),
                () -> assertEquals(0x0000000000000001L, Bits.coordAt(7, 7))
        );
    }

    @Test
    void transposed() {
        long test1 = parseMatrix("",
                "10101010",
                "00010101",
                "00101010",
                "00000101",
                "00001010",
                "00000001",
                "11110010",
                "11100000");
        Bits.printMatrix(test1);
        long expected1 = parseMatrix("",
                "10000011",
                "00000011",
                "10100011",
                "01000010",
                "10101000",
                "01010000",
                "10101010",
                "01010100");
        Bits.printMatrix(expected1);
        assertEquals(expected1, Bits.transposed(test1));
    }

    @Test
    void flipUpDn() {
        long test1 = parseMatrix("",
                "10101010",
                "00010101",
                "00101010",
                "00000101",
                "00001010",
                "00000001",
                "11110010",
                "11100000");
        Bits.printMatrix(test1);
        long expected1 = parseMatrix("",
                "11100000",
                "11110010",
                "00000001",
                "00001010",
                "00000101",
                "00101010",
                "00010101",
                "10101010");
        Bits.printMatrix(expected1);
        assertEquals(expected1, Bits.flipUpDn(test1));
    }

    @Test
    void flipLtRt() {
        long test1 = parseMatrix("",
                "10101010",
                "00010101",
                "00101010",
                "00000101",
                "00001010",
                "00000001",
                "11110010",
                "11100000");
        Bits.printMatrix(test1);
        long expected1 = parseMatrix("",
                "01010101",
                "10101000",
                "01010100",
                "10100000",
                "01010000",
                "10000000",
                "01001111",
                "00000111");
        Bits.printMatrix(expected1);
        assertEquals(expected1, Bits.flipLtRt(test1));
    }

    @Test
    void flip() {
        long test1 = parseMatrix("",
                "10101010",
                "00010101",
                "00101010",
                "00000101",
                "00001010",
                "00000001",
                "11110010",
                "11100000");
        Bits.printMatrix(test1);
        long expected1 = parseMatrix("",
                "00000111",
                "01001111",
                "10000000",
                "01010000",
                "10100000",
                "01010100",
                "10101000",
                "01010101");
        Bits.printMatrix(expected1);
        assertEquals(expected1, Bits.flip(test1));
    }

    private static long parseMatrix(String... binaries) {
        String bin = String.join("", binaries);
        long bits;
        if (bin.length() < 64) {
            bits = Long.parseLong(bin, 2);
        } else {
            bits = Long.parseLong(bin.substring(1), 2);
            if (bin.charAt(0) == '1') {
                bits |= 0x8000000000000000L;
            }
        }
        return bits;
    }
}
