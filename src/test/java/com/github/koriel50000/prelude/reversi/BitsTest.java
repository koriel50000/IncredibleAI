package com.github.koriel50000.prelude.reversi;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;

import static java.lang.Long.parseLong;
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
    void indexOf() {
        assertAll(
                () -> assertEquals(0, Bits.indexOf(0x8000000000000000L)),
                () -> assertEquals(1, Bits.indexOf(0x4000000000000000L)),
                () -> assertEquals(63, Bits.indexOf(0x0000000000000001L))
        );
    }

    @Test
    void transpose() {
        long test1 = parseLong("" +
                "10101010" +
                "00010101" +
                "00101010" +
                "00000101" +
                "00001010" +
                "00000001" +
                "00000010" +
                "00000000", 2);
        printMatrix(test1);
        long expected1 = matrix("",
                "00000000",
                "00000000",
                "00000000",
                "00000000",
                "00000000",
                "00000000",
                "00000000",
                "00000000");
        printMatrix(expected1);
        assertEquals(expected1, Bits.transpose(test1));
    }

    private static long matrix(String... bits) {
        return Long.parseLong(String.join("", bits), 2);
    }

    private static void printMatrix(long matrix) {
        for (int i = 0; i < 8; i++) {
            long row = matrix >>> (i * 8) & 0xff;
            String bits = Long.toBinaryString(row);
            String line = StringUtils.leftPad("", 8, '0');
            System.out.println(line);
        }
    }
}
