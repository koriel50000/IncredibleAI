package com.github.koriel50000.prelude.reversi;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

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
    void lastIndexOf() {
        assertAll(
                () -> assertEquals(0, Bits.lastIndexOf(0x0000000000000001L)),
                () -> assertEquals(1, Bits.lastIndexOf(0x0000000000000002L)),
                () -> assertEquals(63, Bits.lastIndexOf(0x8000000000000000L))
        );
    }
}
