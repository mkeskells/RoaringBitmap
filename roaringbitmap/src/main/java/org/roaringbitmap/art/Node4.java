package org.roaringbitmap.art;

import org.roaringbitmap.longlong.IntegerUtil;
import org.roaringbitmap.longlong.LongUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Node4 extends BranchNode {

  int key = 0;
  Node[] children = new Node[4];

  public Node4(int compressedPrefixSize) {
    super(compressedPrefixSize);
  }
  public static Node4 create(Node node1, Node node2,  byte node1Key, byte node2Key,
                             long prefixSource, byte prefixStart, byte prefixlength) {
    if ((char)node1Key < (char)node2Key) {
      return createOrdered(node1, node2, node1Key, node2Key, prefixSource, prefixStart, prefixlength);
    } else {
      return createOrdered(node2, node1, node2Key, node1Key, prefixSource, prefixStart, prefixlength);
    }
  }
  /**
   * create a Node4 with two child nodes and their keys.
   * It is required that node1Key < node2Key when conpared as unsigned bytes.
   * The child nodes must have the prefixes adjusted before callingt this method.
   * @param node1 the first node
   * @param node2 the second node
   * @param node1Key the key of the first node
   * @param node2Key the key of the second node
   * @param prefixSource the source of the prefix, in big endian order
   * @param prefixStart the start index of the prefix in the source
   * @param prefixLength the end index of the prefix in the source
   * @return a Node4 instance with the two nodes as children
   */
  public static Node4 createOrdered(Node node1, Node node2,  byte node1Key, byte node2Key,
                             long prefixSource, byte prefixStart, byte prefixLength) {
    Node4 node4 = new Node4(prefixLength);
    node4.count = 2;
    node4.key = IntegerUtil.init2Bytes(node1Key, node2Key);
    node4.children[0] = node1;
    node4.children[1] = node2;
    for (int i = prefixStart; i < prefixStart + prefixLength; i++) {
      node4.prefix[i - prefixStart] = LongUtils.getByte(prefixSource, i);
    }
    return node4;
  }


  @Override
  protected NodeType nodeType() {
    return NodeType.NODE4;
  }

  @Override
  public int getChildPos(byte k) {
    for (int i = 0; i < count; i++) {
      int shiftLeftLen = (3 - i) * 8;
      byte v = (byte) (key >> shiftLeftLen);
      if (v == k) {
        return i;
      }
    }
    return ILLEGAL_IDX;
  }

  @Override
  public SearchResult getNearestChildPos(byte k) {
    byte[] firstBytes = IntegerUtil.toBDBytes(key);
    return binarySearchWithResult(firstBytes, 0, count, k);
  }

  @Override
  public byte getChildKey(int pos) {
    int shiftLeftLen = (3 - pos) * 8;
    byte v = (byte) (key >> shiftLeftLen);
    return v;
  }

  @Override
  public Node getChild(int pos) {
    return children[pos];
  }

  @Override
  public Node getChildAtKey(byte key) {
    int pos = getChildPos(key);
    return (pos != ILLEGAL_IDX) ? children[pos] : null;
  }

  @Override
  public void replaceNode(int pos, Node freshOne) {
    children[pos] = freshOne;
  }

  @Override
  public int getMinPos() {
    return 0;
  }

  @Override
  public int getNextLargerPos(int pos) {
    if (pos == ILLEGAL_IDX) {
      return 0;
    }
    pos++;
    return pos < count ? pos : ILLEGAL_IDX;
  }

  @Override
  public int getMaxPos() {
    return count - 1;
  }

  @Override
  public int getNextSmallerPos(int pos) {
    if (pos == ILLEGAL_IDX) {
      return count - 1;
    }
    pos--;
    return pos >= 0 ? pos : ILLEGAL_IDX;
  }

  /**
   * insert the child node into this with the key byte
   *
   * @param childNode the child node
   * @param key the key byte
   * @return the input node4 or an adaptive generated node16
   */
  @Override
  protected BranchNode insert(Node childNode, byte key) {
    if (this.count < 4) {
      // insert leaf into current node
      this.key = IntegerUtil.setByte(this.key, key, this.count);
      this.children[this.count] = childNode;
      this.count++;
      insertionSort(this);
      return this;
    } else {
      // grow to Node16
      Node16 node16 = new Node16(this.prefixLength());
      node16.count = 4;
      node16.firstV = LongUtils.initWithFirst4Byte(this.key);
      System.arraycopy(children, 0, node16.children, 0, 4);
      copyPrefix(this, node16);
      BranchNode freshOne = node16.insert(childNode, key);
      return freshOne;
    }
  }

  @Override
  public Node remove(int pos) {
    assert pos < count;
    count--;
    key = IntegerUtil.shiftLeftFromSpecifiedPosition(key, pos, (4 - pos - 1));
    for (; pos < count; pos++) {
      children[pos] = children[pos + 1];
    }
    children[pos] = null;
    if (count == 1) {
      // shrink to the child node
      Node childNode = children[0];
      if (childNode instanceof BranchNode) {
        BranchNode child = (BranchNode) childNode;
        byte childPrefixLength = child.prefixLength();
        byte thisPrefixLength = this.prefixLength();
        byte newLength = (byte) (childPrefixLength + thisPrefixLength + 1);
        byte[] newPrefix = new byte[newLength];
        System.arraycopy(this.prefix, 0, newPrefix, 0,thisPrefixLength);
        newPrefix[thisPrefixLength] = IntegerUtil.firstByte(key);
        System.arraycopy(child.prefix, 0, newPrefix, thisPrefixLength + 1, childPrefixLength);
        child.prefix = newPrefix;
      }
      return childNode;
    }
    return this;
  }

  @Override
  public void serializeNodeBody(DataOutput dataOutput) throws IOException {
    dataOutput.writeInt(Integer.reverseBytes(key));
  }

  /**
   * serialize the node's body content
   */
  @Override
  public void serializeNodeBody(ByteBuffer byteBuffer) throws IOException {
    byteBuffer.putInt(key);
  }

  @Override
  public void deserializeNodeBody(DataInput dataInput) throws IOException {
    int v = dataInput.readInt();
    key = Integer.reverseBytes(v);
  }

  /**
   * deserialize the node's body content
   */
  @Override
  public void deserializeNodeBody(ByteBuffer byteBuffer) throws IOException {
    key = byteBuffer.getInt();
  }

  @Override
  public int serializeNodeBodySizeInBytes() {
    return 4;
  }

  @Override
  public void replaceChildren(Node[] children) {
    System.arraycopy(children, 0, this.children, 0, count);
  }

  /**
   * sort the key byte array of node4 type by the insertion sort algorithm.
   *
   * @param node4 node14 or node16
   */
  private static void insertionSort(Node4 node4) {
    byte[] key = IntegerUtil.toBDBytes(node4.key);
    byte[] sortedKey = sortSmallByteArray(key, node4.children, 0, node4.count - 1);
    node4.key = IntegerUtil.fromBDBytes(sortedKey);
  }
}
