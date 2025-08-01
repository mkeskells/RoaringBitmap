package org.roaringbitmap.art;

public class OnlyDuringMigration {

    private static long extractByte(int index, byte[] prefix) {
        long byteValue = ((long)prefix[index] & 0xff);
        return byteValue << (index * 8);
    }
    private static byte extractByte(int index, long prefix) {
        return (byte) (prefix >> (index * 8));
    }
    public static long prefixToLong(byte[] prefix) {
        long result = 0L;
        for (int i = 0; i < prefix.length; i++) {
            result |= extractByte(i,prefix);
        }
        return result;
    }
    public static byte[] longToPrefix(int length, long prefix) {
        byte[]  result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = extractByte(1,prefix);
        }
        return result;
    }
}
