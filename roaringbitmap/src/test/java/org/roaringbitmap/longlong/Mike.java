package org.roaringbitmap.longlong;

import java.util.Random;

public class Mike {
    public static void main(String[] args) {
        for (int i = 0 ; i < 10; ++i) {
            long start = System.nanoTime();
           new Mike().run();
            long stop = System.nanoTime();
            System.out.println("Iteration " + i + " took " + (stop - start)/1000/1000.0 + " ms");
        }

    }

    private final void run() {
        Roaring64Bitmap tst = new Roaring64Bitmap();
        Random r = new Random(0x1234567890abcdefL);


        for (long i = 0; i < 1000000; ++i) {
            tst.addLong(r.nextLong());
        }
    }
}
