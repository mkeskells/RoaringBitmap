package org.roaringbitmap.art;

import org.roaringbitmap.longlong.IntegerUtil;
import org.roaringbitmap.longlong.LongUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class Node4 extends BranchNode {

  @Override
  public Node4 withPrefix(byte compressedPrefixSize, long prefix) {
    Node4 result = create(compressedPrefixSize, prefix);
    result.key = this.key;
    result.children = this.children;
    result.count = this.count;
    return result;
  }

  public Node4 withPrefix(byte compressedPrefixSize, byte[] prefix) {
    if (compressedPrefixSize != prefix.length) throw new IllegalStateException();
    return withPrefix(compressedPrefixSize, OnlyDuringMigration.prefixToLong(prefix));
  }
  public static Node4 create(int compressedPrefixSize) {
    
  }
  public static Node4 create(int compressedPrefixSize, long prefix) {
    switch(compressedPrefixSize) {
      case 0: return new Prefix0();
      case 1: return new Prefix1(prefix);
      case 2: return new Prefix2(prefix);
      case 3: return new Prefix3(prefix);
      case 4: return new Prefix4(prefix);
      case 5: return new Prefix5(prefix);
      default:throw new IllegalArgumentException();
    }

  }
  private static class Prefix0 extends Node4{
    @Override
    public byte prefixLength() {
      return 0;
    }

    @Override
    public byte[] prefix() {
      return new byte[0];
    }

    @Override
    public long prefixAsLong() {
      return 0L;
    }
  }
  private static class Prefix1 extends Node4{
    private final byte prefix;
    Prefix1(long prefix) {
      this.prefix = (byte) prefix;
    }
    @Override
    public byte prefixLength() {
      return 1;
    }

    @Override
    public byte[] prefix() {
      return OnlyDuringMigration.longToPrefix(prefixLength(),prefix);
    }

    @Override
    public long prefixAsLong() {
      return prefix;
    }
  }
  private static class Prefix2 extends Node4{
    private final short prefix;
    Prefix2(long prefix) {
      this.prefix = (short) prefix;
    }
    @Override
    public byte prefixLength() {
      return 2;
    }

    @Override
    public byte[] prefix() {
      return OnlyDuringMigration.longToPrefix(prefixLength(),prefix);
    }

    @Override
    public long prefixAsLong() {
      return prefix;
    }
  }
  private static class Prefix3 extends Node4{
    private final int prefix;
    Prefix3(long prefix) {
      this.prefix = (int) prefix;
    }
    @Override
    public byte prefixLength() {
      return 4;
    }

    @Override
    public byte[] prefix() {
      return OnlyDuringMigration.longToPrefix(prefixLength(),prefix);
    }

    @Override
    public long prefixAsLong() {
      return prefix;
    }
  }
  private static class Prefix4 extends Node4{
    private final int prefix;
    Prefix4(long prefix) {
      this.prefix = (int) prefix;
    }
    @Override
    public byte prefixLength() {
      return 4;
    }

    @Override
    public byte[] prefix() {
      return OnlyDuringMigration.longToPrefix(prefixLength(),prefix);
    }

    @Override
    public long prefixAsLong() {
      return prefix;
    }
  }
  private static class Prefix5 extends Node4{
    private final long prefix;
    Prefix5(long prefix) {
      this.prefix = prefix;
    }
    @Override
    public byte prefixLength() {
      return 5;
    }

    @Override
    public byte[] prefix() {
      return OnlyDuringMigration.longToPrefix(prefixLength(),prefix);
    }

    @Override
    public long prefixAsLong() {
      return prefix;
    }
  }
  int key = 0;
  Node[] children = new Node[4];

  private Node4() {
    super(NodeType.NODE4);
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
   * insert the child node into the node4 with the key byte
   *
   * @param node the node4 to insert into
   * @param childNode the child node
   * @param key the key byte
   * @return the input node4 or an adaptive generated node16
   */
  public static BranchNode insert(BranchNode node, Node childNode, byte key) {
    Node4 current = (Node4) node;
    if (current.count < 4) {
      // insert leaf into current node
      current.key = IntegerUtil.setByte(current.key, key, current.count);
      current.children[current.count] = childNode;
      current.count++;
      insertionSort(current);
      return current;
    } else {
      // grow to Node16
      Node16 node16 = Node16.create(current.prefixLength());
      node16.count = 4;
      node16.firstV = LongUtils.initWithFirst4Byte(current.key);
      System.arraycopy(current.children, 0, node16.children, 0, 4);
      copyPrefix(current, node16);
      BranchNode freshOne = Node16.insert(node16, childNode, key);
      return freshOne;
    }
  }

  @Override
  public Node remove(int pos) {
    assert pos < count;
    children[pos] = null;
    count--;
    key = IntegerUtil.shiftLeftFromSpecifiedPosition(key, pos, (4 - pos - 1));
    for (; pos < count; pos++) {
      children[pos] = children[pos + 1];
    }
    if (count == 1) {
      // shrink to the child node
      Node childNode = children[0];
      if (childNode instanceof BranchNode) {
        BranchNode child = (BranchNode) childNode;

        long newPrefixAsLong = this.prefixAsLong();
        newPrefixAsLong = (newPrefixAsLong << 8) | IntegerUtil.firstByte(key);
        newPrefixAsLong = (newPrefixAsLong << (8* (child.prefixLength()))) | child.prefixAsLong();

        //TODO remove start
        byte newLength = (byte) (child.prefixLength() + this.prefixLength() + 1);
        byte[] newPrefix = new byte[newLength];
        System.arraycopy(this.prefix(), 0, newPrefix, 0, this.prefixLength());
        newPrefix[this.prefixLength()] = IntegerUtil.firstByte(key);
        System.arraycopy(child.prefix(), 0, newPrefix, this.prefixLength() + 1, child.prefixLength());
        long prefixLong = OnlyDuringMigration.prefixToLong(newPrefix);
        if (prefixLong != newPrefixAsLong) throw new IllegalStateException();
        //TODO remove end

        return child.withPrefix(newLength, prefixLong);
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
