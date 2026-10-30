package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

/** Reference trace for the exact get/put/clear operations used by the two ownership caches. */
public class OwnershipCacheLookupTest {

    static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    @Test
    public void signedKeysReplacementMissClearAndEvictionMatchAccessOrderReference() {
        var reference = new LinkedHashMap<Long, Integer>(32, .75f, true) {

            protected boolean removeEldestEntry(Map.Entry<Long, Integer> e) {
                return size() > 32;
            }
        };
        var actual = new Long2ObjectLinkedOpenHashMap<Integer>(32);
        long[] edge = { 0, Long.MIN_VALUE, Long.MAX_VALUE, -1, key(Integer.MIN_VALUE, Integer.MAX_VALUE),
            key(Integer.MAX_VALUE, Integer.MIN_VALUE) };
        Random random = new Random(94511);
        for (int i = 0; i < 50000; i++) {
            long k = i % 11 < 6 ? edge[i % 11] : key(random.nextInt(13) - 6, random.nextInt(13) - 6);
            if (i % 101 == 0) {
                actual.clear();
                reference.clear();
            } else if (i % 3 == 0) {
                Integer v = i;
                assertEquals(reference.put(k, v), actual.putAndMoveToLast(k, v));
                if (actual.size() > 32) actual.removeFirst();
            } else assertEquals(reference.get(k), actual.getAndMoveToLast(k));
            assertEquals(new ArrayList<>(reference.keySet()), new ArrayList<>(actual.keySet()));
            assertEquals(reference, actual);
        }
    }

    @Test
    public void diagonalAndOppositeSignKeysKeepAllSixtyFourBits() {
        var map = new Long2ObjectLinkedOpenHashMap<Integer>(32);
        for (int i = -16; i < 16; i++) map.putAndMoveToLast(key(i, i), Integer.valueOf(i));
        for (int i = -16; i < 16; i++) assertEquals(Integer.valueOf(i), map.getAndMoveToLast(key(i, i)));
        assertEquals(32, map.size());
        assertNull(map.getAndMoveToLast(key(-16, 16)));
    }

    static volatile long checksum;

    public static void main(String[] args) {
        for (String shape : new String[] { "diagonal", "negative-window", "positive-window", "repeated-key",
            "cold-misses" }) {
            long[] keys = new long[32];
            for (int i = 0; i < 32; i++) keys[i] = shape.equals("diagonal") ? key(i - 16, i - 16)
                : key((shape.equals("negative-window") ? -320 : 320) + (i & 7), -96 + (i >> 3));
            var old = new LinkedHashMap<Long, Integer>(32, .75f, true);
            var fresh = new Long2ObjectLinkedOpenHashMap<Integer>(32);
            for (int i = 0; i < 32; i++) {
                old.put(keys[i], i);
                fresh.putAndMoveToLast(keys[i], Integer.valueOf(i));
            }
            if (shape.equals("repeated-key")) Arrays.fill(keys, key(-20, -90));
            if (shape.equals("repeated-key")) {
                old.put(keys[0], 31);
                fresh.putAndMoveToLast(keys[0], Integer.valueOf(31));
            }
            if (shape.equals("cold-misses")) for (int i = 0; i < 32; i++) keys[i] = key(-4000 + i, 9000);
            for (int run = 0; run < 6; run++) {
                long before = allocated(), start = System.nanoTime(), sum = 0;
                for (int i = 0; i < 4000000; i++) {
                    Integer value = old.get(keys[i & 31]);
                    sum += value == null ? -1 : value;
                }
                long elapsed = System.nanoTime() - start, bytes = allocated() - before;
                checksum = sum;
                long b2 = allocated(), s2 = System.nanoTime(), sum2 = 0;
                for (int i = 0; i < 4000000; i++) {
                    Integer value = fresh.getAndMoveToLast(keys[i & 31]);
                    sum2 += value == null ? -1 : value;
                }
                long e2 = System.nanoTime() - s2, bytes2 = allocated() - b2;
                checksum = sum2;
                if (sum != sum2) throw new AssertionError("checksum parity");
                if (run >= 3) System.out.printf(
                    Locale.ROOT,
                    "%s run%d boxed %.3fms %dB primitive %.3fms %dB checksum%d%n",
                    shape,
                    run,
                    elapsed / 1e6,
                    bytes,
                    e2 / 1e6,
                    bytes2,
                    sum);
            }
        }
    }

    static long allocated() {
        return ((com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean())
            .getThreadAllocatedBytes(
                Thread.currentThread()
                    .getId());
    }
}
