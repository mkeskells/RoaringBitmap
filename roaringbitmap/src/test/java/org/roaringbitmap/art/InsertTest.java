package org.roaringbitmap.art;

import org.junit.jupiter.api.Test;
import org.roaringbitmap.longlong.HighLowContainer;
import org.roaringbitmap.longlong.Roaring64Bitmap;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.roaringbitmap.art.TreeStructure.checkAndDumpTree;

public class InsertTest {
    @Test
    void basicAddLeaf() {
        Roaring64Bitmap bitmap = new Roaring64Bitmap();
        bitmap.add(1L);
        bitmap.add(2L);
        bitmap.add(3L);
        bitmap.add(4L);
        bitmap.add(5L);
        bitmap.add(6L);
        bitmap.add(7L);
        bitmap.add(8L);
        bitmap.add(9L);
        bitmap.add(10L);

        assertTrue(bitmap.contains(1L));
        assertTrue(bitmap.contains(2L));
        assertTrue(bitmap.contains(3L));
        assertTrue(bitmap.contains(4L));
        assertTrue(bitmap.contains(5L));
        assertTrue(bitmap.contains(6L));
        assertTrue(bitmap.contains(7L));
        assertTrue(bitmap.contains(8L));
        assertTrue(bitmap.contains(9L));
        assertTrue(bitmap.contains(10L));
        assertFalse(bitmap.contains(11L));
        assertEquals(10, bitmap.getLongCardinality());
        assertFalse(bitmap.isEmpty());

        assertEquals(//
                "Leaf key=0x000000000000\n" +
                        "  content={1,2,3,4,5,6,7,8,9,10}\n",
                checkAndDumpTree(bitmap));

    }

    @Test
    void basicAdd2Leaves() {
        Roaring64Bitmap bitmap = new Roaring64Bitmap();
        bitmap.add(0x1L);
        bitmap.add(0x10000L);

        assertEquals(//
                "Node4 count=2 prefix=[0, 0, 0, 0, 0]\n" +
                        "  key=0x00010000\n" +
                        "    Leaf key=0x000000000000\n" +
                        "      content={1}\n" +
                        "    Leaf key=0x000000000001\n" +
                        "      content={0}\n",
                checkAndDumpTree(bitmap));

        assertTrue(bitmap.contains(0x1L));
        assertTrue(bitmap.contains(0x10000L));
        assertEquals(2, bitmap.getLongCardinality());
        assertFalse(bitmap.isEmpty());

    }

    @Test
    void basicAdd3Levels1() {
        Roaring64Bitmap bitmap = new Roaring64Bitmap();
        bitmap.add(0x1L);
        bitmap.add(0x10000L);
        bitmap.add(0x1000000L);

        assertEquals(//
                "Node4 count=2 prefix=[0, 0, 0, 0]\n" +
                        "  key=0x00010000\n" +
                        "    Node4 count=2 prefix=[]\n" +
                        "      key=0x00010000\n" +
                        "        Leaf key=0x000000000000\n" +
                        "          content={1}\n" +
                        "        Leaf key=0x000000000001\n" +
                        "          content={0}\n" +
                        "    Leaf key=0x000000000100\n" +
                        "      content={0}\n",
                checkAndDumpTree(bitmap));

        assertTrue(bitmap.contains(0x1L));
        assertTrue(bitmap.contains(0x10000L));
        assertTrue(bitmap.contains(0x1000000L));
        assertEquals(3, bitmap.getLongCardinality());
        assertFalse(bitmap.isEmpty());

    }

    @Test
    void basicAdd3Levels2() {
        Roaring64Bitmap bitmap = new Roaring64Bitmap();
        bitmap.add(0x1L);
        bitmap.add(0x10000L);
        bitmap.add(0x100000000L);

        assertEquals(//
                "Node4 count=2 prefix=[0, 0, 0]\n" +
                        "  key=0x00010000\n" +
                        "    Node4 count=2 prefix=[0]\n" +
                        "      key=0x00010000\n" +
                        "        Leaf key=0x000000000000\n" +
                        "          content={1}\n" +
                        "        Leaf key=0x000000000001\n" +
                        "          content={0}\n" +
                        "    Leaf key=0x000000010000\n" +
                        "      content={0}\n",
                checkAndDumpTree(bitmap));

    }

}
