package bench;

import org.roaringbitmap.longlong.Roaring64Bitmap;

import java.util.Arrays;
import java.util.Random;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.function.Supplier;

public class SimpleLookup {

  public static void main(String[] args) throws Exception {
    SimpleLookup lookup = new SimpleLookup();

//    lookup.runBenchmark(lookup::lookupPopulated, "lookupPopulated", 5);
//    lookup.runBenchmark(lookup::addSortedOld, "addSorted", 5);
//    lookup.runBenchmark(lookup::addSortedNew, "addSortedNew", 5);
//    lookup.runBenchmark(lookup::addUnsorted, "addUnsorted", 5);

//    lookup.runBenchmark(lookup::addSortedSmall, "addSortedSmall", 50);
//    lookup.runBenchmark(lookup::addUnsortedSmall, "addUnsortedSmall", 50);
  }

  private void runBenchmark(Supplier<Object> fn, String name, int runs) {
    long[] durations = new long[runs];
    long[] allocations = new long[runs];
    Object[] results = new Object[runs];
    long currentThreadId = Thread.currentThread().getId();
    ThreadMXBean threadMxBean = (ThreadMXBean)
            ManagementFactory.getThreadMXBean();

    for (int i = 0; i < 5; i++) {
      System.out.println("Running " + name + " benchmark warmup " + i);
      results[i] = fn.get();
    }

    for (int i = 0; i < runs; i++) {
      System.out.println("Running " + name + " benchmark iteration " + i);
      long allocBefore = threadMxBean.getThreadAllocatedBytes(currentThreadId);
      long startTime = System.nanoTime();
      Object result = fn.get();
      long endTime = System.nanoTime();
      long allocAfter = threadMxBean.getThreadAllocatedBytes(currentThreadId);
      durations[i] = endTime - startTime;
      allocations[i] = allocAfter - allocBefore;
      results[i] = result;
      System.out.println("Iteration " + i + ": Duration: " + durations[i] + " ns, Allocated: " + allocations[i] + " bytes, Result: " + results[i]);
    }
    System.out.println("Average Duration: " + Arrays.stream(durations).asDoubleStream().map(x -> x / 1000000).summaryStatistics() + " ms");
    System.out.println("Average Allocation: " + Arrays.stream(allocations).asDoubleStream().map(x -> x / 1024 / 1024).summaryStatistics() + " MB");

  }

  //  private Roaring64Bitmap testPopulated = new Roaring64Bitmap();
  private SimpleLookup() {
    //    for (long index : indexes) {
    //      testPopulated.addLong(index);
    //    }
  }

  private long[] indexes = new Random(0L).longs().limit(10000000).toArray();
  private long[] sortedIndexes = Arrays.stream(indexes).sorted().toArray();

  private long[] indexesSmall = new Random(0L).longs().limit(100000).toArray();
  private long[] sortedIndexesSmall = Arrays.stream(indexesSmall).sorted().toArray();


//  public int lookupPopulated() {
//    int result = 0;
//    for (long index : indexes) {
//      if (testPopulated.contains(index)) {;
//        result++;
//      }
//    }
//    return result;
////  }
//
//  public Boolean addSortedOld() {
//    Roaring64Bitmap test = new Roaring64Bitmap();
//    for (long index : sortedIndexes) {
//      test.addLongOld(index);
//    }
//    return test.isEmpty();
//  }
//  public Boolean addSortedNew() {
//    Roaring64Bitmap test = new Roaring64Bitmap();
//    for (long index : sortedIndexes) {
//      test.addLong(index);
//    }
//    return test.isEmpty();
//  }
  public Boolean addUnsorted() {
    Roaring64Bitmap test = new Roaring64Bitmap();
    for (long index : indexes) {
      test.addLong(index);
    }
    return test.isEmpty();
  }
  public Boolean addSortedSmall() {
    Roaring64Bitmap test = new Roaring64Bitmap();
    for (long index : sortedIndexesSmall) {
      test.addLong(index);
    }
    return test.isEmpty();
  }
  public Boolean addUnsortedSmall() {
    Roaring64Bitmap test = new Roaring64Bitmap();
    for (long index : indexesSmall) {
      test.addLong(index);
    }
    return test.isEmpty();
  }

}
