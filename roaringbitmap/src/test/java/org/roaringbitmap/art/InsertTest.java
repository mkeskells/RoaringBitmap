package org.roaringbitmap.art;

import org.junit.jupiter.api.Test;
import org.roaringbitmap.longlong.HighLowContainer;
import org.roaringbitmap.longlong.Roaring64Bitmap;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

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

        Art art = getArt(bitmap);
        assertNotNull(art);
        assertNotNull(art.getRoot());
        assertEquals(LeafNode.class, art.getRoot().getClass());

        LeafNode leaf = (LeafNode) art.getRoot();
        assertArrayEquals(new byte[] {0,0,0,0,0,0}, leaf.getKeyBytes());
        assertEquals(0L, leaf.getKey());
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
                "    Leaf key=0x000000000001\n", dumpTree(bitmap));

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
                "        Leaf key=0x000000000001\n" +
                "    Leaf key=0x000000000100\n",
                dumpTree(bitmap));

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

        Art art = getArt(bitmap);
        assertNotNull(art);
        assertNotNull(art.getRoot());
        assertEquals(Node4.class, art.getRoot().getClass());

        Node4 root = (Node4) art.getRoot();
        assertArrayEquals(new byte[] {0,0,0}, root.prefix, Arrays.toString(root.prefix));
        assertEquals(2, root.count);

        Node4 child = (Node4) root.children[0];
        assertArrayEquals(new byte[] {0}, child.prefix, Arrays.toString(child.prefix));
        assertEquals(2, child.count);

        LeafNode l1 = (LeafNode) child.children[0];
        assertArrayEquals(new byte[] {0,0,0,0,0,0}, l1.getKeyBytes());
        assertEquals(0x0L, l1.getKey());

        LeafNode l2 = (LeafNode) child.children[1];
        assertArrayEquals(new byte[] {0,0,0,0,0,1}, l2.getKeyBytes());
        assertEquals(0x1L, l2.getKey());

        assertEquals(0x00_01_00_00, root.key);

        assertSame(l1, child.getChildAtKey((byte) 0));
        assertEquals(0, child.getChildPos((byte) 0));
        assertSame(l1, child.getChild(0));

        assertSame(l2, child.getChildAtKey((byte) 1));
        assertEquals(1, child.getChildPos((byte) 1));
        assertSame(l2, child.getChild(1));

        assertTrue(bitmap.contains(0x1L));
        assertTrue(bitmap.contains(0x10000L));
        assertEquals(3, bitmap.getLongCardinality());
        assertFalse(bitmap.isEmpty());

    }


    private final static java.lang.reflect.Field highLowContainerField;
    private final static java.lang.reflect.Field artField;
    static {
        try {
            artField = HighLowContainer.class.getDeclaredField("art");
            artField.setAccessible(true);
            highLowContainerField = Roaring64Bitmap.class.getDeclaredField("highLowContainer");
            highLowContainerField.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Art getArt(Roaring64Bitmap bitmap) {
        return assertDoesNotThrow( () -> {
            Object highLowContainer = highLowContainerField.get(bitmap);
            return (Art) artField.get(highLowContainer);
        });
    }
    private String dumpTree(Roaring64Bitmap bitmap) {
        Art art = getArt(bitmap);
        if (art == null) {
            return "No ART tree";
        }
        StringBuilder sb = new StringBuilder();
        dumpTree(art.getRoot(), sb, "");
        return sb.toString();
    }

    private void dumpTree(Node node, StringBuilder sb, String indent) {
        if (node == null) {
            sb.append(indent).append("null\n");
            return;
        }
        if (node instanceof LeafNode) {
            LeafNode leafNode = (LeafNode) node;
            sb.append(String.format("%sLeaf key=0x%012x\n", indent, leafNode.getKey()));
        } else {
            BranchNode branchNode = (BranchNode) node;
            sb.append(String.format("%s%s count=%d prefix=%s\n", indent, branchNode.getClass().getSimpleName(),
                    branchNode.count, Arrays.toString(branchNode.prefix)));
            if (node instanceof Node4) {
                Node4 node4 = (Node4) node;
                sb.append(String.format("%s  key=0x%08x\n", indent, node4.key));
                for (int i = 0; i < node4.count; i++) {
                    dumpTree(node4.children[i], sb, indent + "    ");
                }

                checkErrors(node4.count, 4, node4.children, sb, indent);

            } else if (node instanceof Node16) {
                Node16 node16 = (Node16) node;
                sb.append(String.format("%s  firstV=0x%016x secondV=0x%016x \n", indent, node16.firstV, node16.secondV));
                for (int i = 0; i < node16.count; i++) {
                    dumpTree(node16.children[i], sb, indent + "    ");
                }
                checkErrors(node16.count, 16, node16.children, sb, indent);
            } else if (node instanceof Node48) {
                Node48 node48 = (Node48) node;
                for (int i = 0; i < node48.count; i++) {
                    dumpTree(node48.children[i], sb, indent + "    ");
                }
                checkErrors(node48.count, 48, node48.children, sb, indent);
            } else if (node instanceof Node256) {
                Node256 node256 = (Node256) node;
                for (int i = 0; i < 256; i++) {
                    dumpTree(node256.children[i], sb, indent + "    ");
                }
            }
        }
    }

    private void checkErrors(short node4, int x, Node[] node41, StringBuilder sb, String indent) {
        for (int i = node4; i < x; i++) {
            if (node41[i] != null) {
                ;
                sb.append(String.format("%s  ERROR children[%d] %s\n", indent, i, node41[i].getClass().getSimpleName()));
                dumpTree(node41[i], sb, indent + "    ERROR ");
                fail(sb.toString());
            }
        }
    }
}
