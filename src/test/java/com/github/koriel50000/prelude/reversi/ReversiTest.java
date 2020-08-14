package com.github.koriel50000.prelude.reversi;

import org.junit.jupiter.api.Test;

import static com.github.koriel50000.prelude.reversi.Reversi.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReversiTest {

    @Test
    void testStone() {
        assertAll(
                () -> assertEquals(".", Stone.EMPTY.toString()),
                () -> assertEquals("@", Stone.BLACK.toString()),
                () -> assertEquals("O", Stone.WHITE.toString()),
                () -> assertEquals(0, Stone.EMPTY.ordinal()),
                () -> assertEquals(1, Stone.BLACK.ordinal()),
                () -> assertEquals(2, Stone.WHITE.ordinal()),
                () -> assertEquals(3, Stone.BORDER.ordinal())
        );
    }

    @Test
    void testColor() {
        assertAll(
                () -> assertEquals(Stone.BLACK, Color.Black.value()),
                () -> assertEquals(Stone.WHITE, Color.White.value()),
                () -> assertEquals(Stone.WHITE, Color.Black.opponentValue()),
                () -> assertEquals(Stone.BLACK, Color.White.opponentValue()),
                () -> assertEquals("black", Color.Black.toString()),
                () -> assertEquals("white", Color.White.toString())
        );
    }

    @Test
    void testCoord() {
        int expectedIndex = 0;
        long expectedCoord = 0x8000000000000000L;
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                Coord actual1 = Coord.valueOf(x, y);
                if (x == 0 || x == 9 || y == 0 | y == 9) {
                    assertEquals(Coord.OUT_OF_BOUNDS, actual1);
                } else {
                    assertEquals(x, actual1.x);
                    assertEquals(y, actual1.y);
                    assertEquals(expectedIndex, actual1.index());
                    assertEquals(x == 1 || x == 8 || y == 1 || y == 8, actual1.isCircum());
                    Coord actual2 = Coord.valueOf(expectedIndex);
                    Coord actual3 = Coord.valueOf(expectedCoord);
                    assertEquals(actual1, actual2);
                    assertEquals(actual1, actual3);
                    expectedIndex++;
                    expectedCoord >>>= 1;
                }
            }
        }

        Direction[] dirs = Direction.values();
        Coord coord = Coord.valueOf(1, 1);
        assertAll(
                () -> assertEquals(Coord.OUT_OF_BOUNDS, coord.move(dirs[0])),
                () -> assertEquals(Coord.OUT_OF_BOUNDS, coord.move(dirs[1])),
                () -> assertEquals(Coord.OUT_OF_BOUNDS, coord.move(dirs[2])),
                () -> assertEquals(Coord.OUT_OF_BOUNDS, coord.move(dirs[3])),
                () -> assertEquals(Coord.valueOf(2, 1), coord.move(dirs[4])),
                () -> assertEquals(Coord.valueOf(2, 2), coord.move(dirs[5])),
                () -> assertEquals(Coord.valueOf(1, 2), coord.move(dirs[6])),
                () -> assertEquals(Coord.OUT_OF_BOUNDS, coord.move(dirs[7]))
        );
    }

    @Test
    void testDirection() {
        Direction[] dirs = Direction.values();
        assertEquals(8, dirs.length);
        assertAll(
                () -> assertEquals(-1, dirs[0].dx),
                () -> assertEquals(0, dirs[0].dy),
                () -> assertEquals(-1, dirs[1].dx),
                () -> assertEquals(-1, dirs[1].dy),
                () -> assertEquals(0, dirs[2].dx),
                () -> assertEquals(-1, dirs[2].dy),
                () -> assertEquals(1, dirs[3].dx),
                () -> assertEquals(-1, dirs[3].dy),
                () -> assertEquals(1, dirs[4].dx),
                () -> assertEquals(0, dirs[4].dy),
                () -> assertEquals(1, dirs[5].dx),
                () -> assertEquals(1, dirs[5].dy),
                () -> assertEquals(0, dirs[6].dx),
                () -> assertEquals(1, dirs[6].dy),
                () -> assertEquals(-1, dirs[7].dx),
                () -> assertEquals(1, dirs[7].dy)
        );
    }
}
