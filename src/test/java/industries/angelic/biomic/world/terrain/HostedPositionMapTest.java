package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;

public class HostedPositionMapTest {

    @Test
    public void fullCoordinatesReplacementGrowthAndPublicationOrderMatchLinkedHashMap() {
        var expected = new LinkedHashMap<Pos, String>();
        var actual = new HostedPositionMap();
        Random random = new Random(4721);
        List<Pos> probes = new ArrayList<>();
        for (int x : new int[] { Integer.MIN_VALUE, -30000001, -17, -16, -1, 0, 15, 16, 30000001, Integer.MAX_VALUE })
            for (int y : new int[] { Integer.MIN_VALUE, -64, 0, 255, 256, 511, 512, Integer.MAX_VALUE })
                probes.add(new Pos(x, y, ~x));
        for (int i = 0; i < 20000; i++) {
            Pos p = i < probes.size() ? probes.get(i) : probes.get(random.nextInt(probes.size()));
            String value = i % 7 == 0 ? null : "state-" + random.nextInt(100);
            assertEquals(expected.put(p, value), actual.put(p, value));
            assertEquals(expected.containsKey(p), actual.containsKey(p.x, p.y, p.z));
            assertEquals(expected.get(p), actual.get(p.x, p.y, p.z));
            assertFalse(actual.containsKey(p.x, p.y, p.z ^ 1));
            if (i % 73 == 0) assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(actual.entrySet()));
        }
        assertEquals(expected, actual);
        assertEquals(expected.hashCode(), actual.hashCode());
        assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.keySet()));
    }

    @Test
    public void mapViewsRemovalClearAndEntryUpdatesRemainConsistent() {
        var expected = new LinkedHashMap<Pos, String>();
        var actual = new HostedPositionMap();
        for (int i = 0; i < 500; i++) {
            Pos p = new Pos(i, 511, -i);
            expected.put(p, "state" + i);
            actual.put(p, "state" + i);
        }
        var a = actual.entrySet()
            .iterator();
        var b = expected.entrySet()
            .iterator();
        while (a.hasNext()) {
            var ae = a.next();
            var be = b.next();
            if (ae.getKey().x % 5 == 0) {
                a.remove();
                b.remove();
            } else if (ae.getKey().x % 7 == 0) {
                ae.setValue(null);
                be.setValue(null);
            }
        }
        assertEquals(expected, actual);
        for (int i = 0; i < 500; i++) assertEquals(expected.get(new Pos(i, 511, -i)), actual.get(i, 511, -i));
        for (int i = 0; i < 500; i += 11)
            assertEquals(expected.remove(new Pos(i, 511, -i)), actual.remove(new Pos(i, 511, -i)));
        assertEquals(expected, actual);
        actual.values()
            .remove("state9");
        expected.values()
            .remove("state9");
        assertEquals(expected, actual);
        actual.keySet()
            .clear();
        assertEquals(0, actual.size());
        assertNull(actual.get(1, 511, -1));
        actual.put(new Pos(Integer.MAX_VALUE, Integer.MIN_VALUE, -1), "later");
        assertEquals("later", actual.get(Integer.MAX_VALUE, Integer.MIN_VALUE, -1));
    }

    @Test
    public void nestedNewestPresentNullShadowsParentsAndPutAllRetainsOriginalOrder() {
        var parent = new HostedPositionMap();
        var child = new HostedPositionMap();
        var accepted = new HostedPositionMap();
        Pos root = new Pos(-17, 300, 16), other = new Pos(-16, 301, 15);
        parent.put(root, "parent");
        parent.put(other, "second");
        child.put(root, null);
        var maps = new ArrayDeque<HostedPositionMap>();
        maps.push(parent);
        maps.push(child);
        assertSame(child, HostedPositionMap.containing(maps, -17, 300, 16));
        assertNull(
            HostedPositionMap.containing(maps, -17, 300, 16)
                .get(-17, 300, 16));
        assertSame(parent, HostedPositionMap.containing(maps, -16, 301, 15));
        assertNull(HostedPositionMap.containing(maps, Integer.MIN_VALUE, 300, 16));
        maps.pop();
        parent.putAll(child);
        accepted.putAll(parent);
        assertEquals(Arrays.asList(root, other), new ArrayList<>(accepted.keySet()));
        assertTrue(accepted.containsKey(-17, 300, 16));
        assertNull(accepted.get(-17, 300, 16));
        maps.pop();
        assertNull(HostedPositionMap.containing(maps, -17, 300, 16));
    }

    @Test
    public void seededNestedPopulationTraceKeepsReadsRollbackCommitAndRngIdentical() {
        Random rng = new Random(8271);
        var expectedAccepted = new LinkedHashMap<Pos, String>();
        var accepted = new HostedPositionMap();
        var expectedStack = new ArrayDeque<LinkedHashMap<Pos, String>>();
        var stack = new ArrayDeque<HostedPositionMap>();
        for (int action = 0; action < 20000; action++) {
            int decision = rng.nextInt(8);
            if (stack.isEmpty() || decision == 0) {
                expectedStack.push(new LinkedHashMap<>());
                stack.push(new HostedPositionMap());
            } else if (decision == 1 || decision == 2) {
                var e = expectedStack.pop();
                var a = stack.pop();
                assertEquals(new ArrayList<>(e.entrySet()), new ArrayList<>(a.entrySet()));
                if (decision == 1) {
                    if (stack.isEmpty()) {
                        expectedAccepted.putAll(e);
                        accepted.putAll(a);
                    } else {
                        expectedStack.peek()
                            .putAll(e);
                        stack.peek()
                            .putAll(a);
                    }
                } // decision 2 rolls back all writes; ancestors and accepted state remain intact.
            } else {
                int x = rng.nextInt(65) - 32, y = 250 + rng.nextInt(20), z = rng.nextInt(65) - 32;
                Pos p = new Pos(x, y, z);
                if (decision < 5) {
                    String value = rng.nextInt(5) == 0 ? null : "state" + rng.nextInt(13);
                    expectedStack.peek()
                        .put(p, value);
                    stack.peek()
                        .put(p, value);
                } else {
                    Map<Pos, String> expected = null;
                    for (var candidate : expectedStack) if (candidate.containsKey(p)) {
                        expected = candidate;
                        break;
                    }
                    HostedPositionMap actual = HostedPositionMap.containing(stack, x, y, z);
                    assertEquals(expected != null, actual != null);
                    assertEquals(
                        expected == null ? expectedAccepted.get(p) : expected.get(p),
                        actual == null ? accepted.get(x, y, z) : actual.get(x, y, z));
                }
            }
        }
        assertEquals(new ArrayList<>(expectedAccepted.entrySet()), new ArrayList<>(accepted.entrySet()));
    }

    @Test
    public void allocationMicrobench() {
        var bean = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        Map<Pos, String> baseline = new LinkedHashMap<>();
        var optimized = new HostedPositionMap();
        for (int i = 0; i < 1024; i++) {
            Pos p = new Pos((i & 31) - 16, 190 + (i & 63), (i >> 5) - 16);
            baseline.put(p, "stone");
            optimized.put(p, "stone");
        }
        for (int i = 0; i < 5; i++) {
            read(baseline, optimized, false, 200000);
            read(baseline, optimized, true, 200000);
        }
        long before = bean.getThreadAllocatedBytes(
            Thread.currentThread()
                .getId()),
            start = System.nanoTime();
        long oldChecksum = read(baseline, optimized, false, 4000000);
        long oldNanos = System.nanoTime() - start;
        long oldBytes = bean.getThreadAllocatedBytes(
            Thread.currentThread()
                .getId())
            - before;
        before = bean.getThreadAllocatedBytes(
            Thread.currentThread()
                .getId());
        start = System.nanoTime();
        long newChecksum = read(baseline, optimized, true, 4000000);
        long newNanos = System.nanoTime() - start;
        long newBytes = bean.getThreadAllocatedBytes(
            Thread.currentThread()
                .getId())
            - before;
        assertEquals(oldChecksum, newChecksum);
        System.out.println(
            "HostedPositionMap 4M mixed read probes: baseline_ns=" + oldNanos
                + " optimized_ns="
                + newNanos
                + " baseline_bytes="
                + oldBytes
                + " optimized_bytes="
                + newBytes
                + " checksum="
                + newChecksum);
        assertTrue("coordinate probes must not allocate", newBytes < 1024);
    }

    private static long read(Map<Pos, String> baseline, HostedPositionMap optimized, boolean fast, int count) {
        long sum = 0;
        for (int i = 0; i < count; i++) {
            int k = i & 2047, x = (k & 31) - 16, y = 190 + (k & 63), z = (k >> 5) - 16;
            String state = fast ? optimized.get(x, y, z) : baseline.get(new Pos(x, y, z));
            if (state != null) sum += state.length();
        }
        return sum;
    }
}
