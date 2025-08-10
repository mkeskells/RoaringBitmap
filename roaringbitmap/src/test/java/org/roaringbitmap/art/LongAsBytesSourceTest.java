package org.roaringbitmap.art;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LongAsBytesSourceTest {

    public static Stream<Arguments> onlyHighPartTestData() {
        return Stream.of(
                Arguments.of(0x1234567890ABCDEFL, 0x1234567890AB0000L),
                Arguments.of(0x0000000000000000L, 0x0000000000000000L),
                Arguments.of(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFF0000L),
                Arguments.of(0x7FFFFFFFFFFFFFFFL, 0x7FFFFFFFFFFF0000L),
                Arguments.of(0x8000000000000000L, 0x8000000000000000L)
        );
    }

    @ParameterizedTest
    @MethodSource("onlyHighPartTestData")
    void onlyHighPartTest(long expected, long test) {
        long actual = Art.onlyHighPart(test)
        assertEquals(expected, actual,
                "Expected high part of " + Long.toHexString(test) + " to be " + Long.toHexString(expected)););
    }
    public static Stream<Arguments> getByteTestData() {
        return Stream.of(
                Arguments.of(0x12, 0x01234567890ABCDEFL, 0),
                Arguments.of(0x34, 0x1234567890ABCDEFL, 1),
                Arguments.of(0x56, 0x1234567890ABCDEFL, 2),
                Arguments.of(0x78, 0x1234567890ABCDEFL, 3),
                Arguments.of(0x90, 0x1234567890ABCDEFL, 4),
                Arguments.of(0xab, 0x1234567890ABCDEFL, 5),

                Arguments.of(0x00, 0x0000000000000000L, 0),
                Arguments.of(0x00, 0x0000000000000000L, 1),
                Arguments.of(0x00, 0x0000000000000000L, 2),
                Arguments.of(0x00, 0x0000000000000000L, 3),
                Arguments.of(0x00, 0x0000000000000000L, 4),
                Arguments.of(0x00, 0x0000000000000000L, 5),

                Arguments.of(0xff, 0xFFFFFFFFFFFFFFFFL, 0),
                Arguments.of(0xff, 0xFFFFFFFFFFFFFFFFL, 1),
                Arguments.of(0xff, 0xFFFFFFFFFFFFFFFFL, 2),
                Arguments.of(0xff, 0xFFFFFFFFFFFFFFFFL, 3),
                Arguments.of(0xff, 0xFFFFFFFFFFFFFFFFL, 4),
                Arguments.of(0xff, 0xFFFFFFFFFFFFFFFFL, 5),


                Arguments.of(0x7f, 0x7FFFFFFFFFFFFFFFL, 0),
                Arguments.of(0xff, 0x7FFFFFFFFFFFFFFFL, 1),
                Arguments.of(0xff, 0x7FFFFFFFFFFFFFFFL, 2),
                Arguments.of(0xff, 0x7FFFFFFFFFFFFFFFL, 3),
                Arguments.of(0xff, 0x7FFFFFFFFFFFFFFFL, 4),
                Arguments.of(0xff, 0x7FFFFFFFFFFFFFFFL, 5),

                Arguments.of(0x80, 0x8000000000000000L, 0),
                Arguments.of(0x00, 0x8000000000000000L, 1),
                Arguments.of(0x00, 0x8000000000000000L, 2),
                Arguments.of(0x00, 0x8000000000000000L, 3),
                Arguments.of(0x00, 0x8000000000000000L, 4),
                Arguments.of(0x00, 0x8000000000000000L, 5),

                );
    }
    @ParameterizedTest
    @MethodSource("getByteTestData")
    void getByteTest(byte expected, long source, int byteIndex) {
        byte actual = Art.getByte(source, byteIndex);
        assertEquals(expected, actual,
                "Expected byte at index " + byteIndex + " of " + Long.toHexString(source) + " to be " + Integer.toHexString(Byte.toUnsignedInt(expected)));
    }

}