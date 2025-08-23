package org.roaringbitmap.art;

import org.roaringbitmap.longlong.HighLowContainer;
import org.roaringbitmap.longlong.Roaring64Bitmap;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

public class TreeStructure {
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

    public static Art getArt(Roaring64Bitmap bitmap) {
        return assertDoesNotThrow( () ->
                (Art) artField.get(getHighLowContainer(bitmap))
        );
    }
    public static HighLowContainer getHighLowContainer(Roaring64Bitmap bitmap) {
        return assertDoesNotThrow( () ->
                (HighLowContainer)highLowContainerField.get(bitmap)
        );
    }
    public static String checkAndDumpTree(Roaring64Bitmap bitmap) {
        Art art = getArt(bitmap);
        if (art == null) {
            return "No ART tree";
        }
        HighLowContainer highLowContainer = getHighLowContainer(bitmap);
        StringBuilder sb = new StringBuilder();
        checkAndDumpTree(highLowContainer, (byte)0, art.getRoot(), sb, "",-1);
        String result = sb.toString();
        if (result.contains("ERROR")) {
            fail("Tree dump contains errors:\n" + result);
            return null; // appeasing the compiler: this line will never be executed.
        } else {
            return result;
        }
    }

    private static void checkAndDumpTree(HighLowContainer highLowContainer, byte position, Node node, StringBuilder sb, String indentRaw, int depth) {
        if (node == null) {
            sb.append(indentRaw).append("null\n");
            return;
        }
        String index = depth == 0? "": String.format(" [%02x] ", position);
        String indexPad = "                ".substring(0, index.length());

        String indent1 = indentRaw + index;
        String indent2 = indentRaw + indexPad;


        if (node instanceof LeafNode) {
            LeafNode leafNode = (LeafNode) node;
            sb.append(String.format("%sLeaf key=0x%012x\n", indent1, leafNode.getKey()));
            sb.append(String.format("%s  content=%s\n", indent2, highLowContainer.getContainer(leafNode.getContainerIdx()).toString()));
        } else {
            BranchNode branchNode = (BranchNode) node;
            depth = depth + 1 + branchNode.prefix.length;
            sb.append(String.format("%s%s count=%d prefix=%s\n", indent1, branchNode.getClass().getSimpleName(),
                    branchNode.count, Arrays.toString(branchNode.prefix)));
            if (depth > 5) {
                //leaves are level 6, no no nodes should be > 5
                sb.append(String.format("%s ERROR DEPTH %d\n", indent2, depth));
            }
            if (node instanceof Node4) {
                Node4 node4 = (Node4) node;
                sb.append(String.format("%s  key=0x%08x\n", indent2, node4.key));
                checkInvariants(node4, sb, indent2);
                for (int i = 0; i < node4.count; i++) {
                    byte key = node4.getChildKey(i);
                    checkAndDumpTree(highLowContainer, key, node4.children[i], sb, indent2 + "    ", depth);
                }

            } else if (node instanceof Node16) {
                Node16 node16 = (Node16) node;
                sb.append(String.format("%s  firstV=0x%016x secondV=0x%016x \n", indent2, node16.firstV, node16.secondV));
                checkInvariants(node16, sb, indent2);
                for (int i = 0; i < node16.count; i++) {
                    byte key = node16.getChildKey(i);
                    checkAndDumpTree(highLowContainer, key, node16.children[i], sb, indent2 + "    ", depth);
                }
            } else if (node instanceof Node48) {
                Node48 node48 = (Node48) node;
                checkInvariants(node48, sb, indent2);
                for (int i = 0; i < node48.count; i++) {
                    byte key = node48.getChildKey(i);
                    checkAndDumpTree(highLowContainer, key, node48.children[i], sb, indent2 + "    ", depth);
                }
            } else if (node instanceof Node256) {
                Node256 node256 = (Node256) node;
                checkInvariants(node256, sb, indent2);
                for (int key = 0; key < 256; key++) {
                    if (node256.children[key] != null) {
                        checkAndDumpTree(highLowContainer, key, node256.children[i], sb, indent2 + "    ", depth);
                    }
                }
            }
        }
    }

    private static void checkInvariants(Node4 node4, StringBuilder sb, String indent) {
        checkCommonInvariants(node4, 2, 4, node4.children, sb, indent);
    }
    private static void checkInvariants(Node16 node4, StringBuilder sb, String indent) {
        checkCommonInvariants(node4, 3, 16, node4.children, sb, indent);
    }
    private static void checkInvariants(Node48 node4, StringBuilder sb, String indent) {
        checkCommonInvariants(node4, 12, 48, node4.children, sb, indent);
    }
    private static void checkInvariants(Node256 node4, StringBuilder sb, String indent) {
        checkCommonInvariants(node4, 40, 256, node4.children, sb, indent);
    }
    private static void checkCommonInvariants(BranchNode node, int minCount, int maxCount, Node[] childrenArray, StringBuilder sb, String indent) {
        if (node.count < minCount || node.count > maxCount) {
            sb.append(String.format("%s  ERROR count %d - range[%d-%d]\n", indent, node.count, minCount, maxCount));
        }
        //check the mappedChildren match the keys. We cant check that they are correct, but we can see if then are consistent
        Map<Integer, Node> posMapped = new HashMap<>();
        Map<Node, Integer> mappedChildren = new IdentityHashMap<>();
        for (int key = 0; key < 256; key++) {
            int pos = node.getChildPos((byte)key);
            Node test = node.getChildAtKey((byte) key);
            if (pos == BranchNode.ILLEGAL_IDX) {
                if (test != null) {
                    sb.append(String.format("%s  ERROR child at key[%d] %s is not null, but getChildPos was ILLEGAL_IDX\n", indent, key, test.getClass().getSimpleName()));
                }
            } else {
                Node child = node.getChild(pos);
                if (child == null) {
                    sb.append(String.format("%s  ERROR child at key[%d] %s is null, but getChildPos was %d\n", indent, key, pos));
                }
                if (child != test) {
                    sb.append(String.format("%s  ERROR getChild(getChildPos(%d)) != getChildAtKey(%d). getChild:%s vs getChildAtKey:%s\n", indent, key, child, test));
                }
                posMapped.put(key, child);
                if (mappedChildren.containsKey(child)) {
                    sb.append(String.format("%s  ERROR child %s is already used at key %d\n", indent, child.getClass().getSimpleName(), mappedChildren.get(child)));
                } else {
                    mappedChildren.put(child, key);
                }
            }
        }
        //check the count matches the number of mappedChildren
        if (node.count != posMapped.size()) {
            sb.append(String.format("%s  ERROR count %d != posMapped.size() %d\n", indent, node.count, posMapped.size()));
        }
        if (node.count != mappedChildren.size()) {
            sb.append(String.format("%s  ERROR count %d != mappedChildren.size() %d\n", indent, node.count, mappedChildren.size()));
        }

        // We have OK so the mappedChildren seem to tie up. Check that rest of the slots for mappedChildren are null

        List<Node> structuralChildren = Arrays.stream(mappedChildren)
                .filter(Objects::nonNull)
                .toList();
        //reasonable to assume that if the counts match, the values match, and we dont want to reduce to a set for obvious reasons
        if (structuralChildren.size() != mappedChildren.size()) {
            sb.append(String.format("%s  ERROR structuralChildren.size() %d != mappedChildren.size() %d\n", indent, structuralChildren.size(), node.count));
            if (structuralChildren.size() < node.count) {
                //something wrong in the checking code. We found mappedChildren that are not in the structure, so where did they come from?
                sb.append(String.format("%s  CODE CHECK ERROR structuralChildren.size() %d < mappedChildren.size() %d\n", indent, structuralChildren.size(), node.count));
            } else {
                //we either have a duplicate node or a node that isnt a child - both probably because some pointer in the mappedChildren wasnt cleaned up
                Map<Node, List<Integer>> inStructure = new IdentityHashMap<>();
                for (int i = 0; i < childrenArray.length; i++) {
                    Node child = childrenArray[i];
                    if (child != null) {
                        inStructure.computeIfAbsent(child, ArrayList::new).add(i);
                    }
                }
                //duplicates
                inStructure.forEach((node, structuralLocations) -> {
                    if (structuralLocations.size() > 1) {
                        sb.append(String.format("%s  ERROR duplicate child %s at locations %s\n", indent, node.getClass().getSimpleName(), structuralLocations));
                    }
                    if (!mappedChildren.containsKey(node)) {
                        sb.append(String.format("%s  ERROR child %s at locations %s is not in mappedChildren\n", indent, node.getClass().getSimpleName(), structuralLocations));
                    }
                });
            }
        }
    }
}
