package industries.angelic.biomic.compat.biome;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

public class NativeWeightedTableTest {

    @Test
    public void profileFingerprintIsStableAndNamespaced() {
        var profiles = new LinkedHashMap<String, String>();
        profiles.put("b", "second");
        profiles.put("a", "first");
        assertEquals(
            BopNativeBiomes.fingerprint(profiles),
            BopNativeBiomes.fingerprint(Map.of("a", "first", "b", "second")));
        assertNotEquals(
            BopNativeBiomes.fingerprint(profiles),
            BopNativeBiomes.fingerprint(Map.of("a", "changed", "b", "second")));
        assertNotEquals(BopNativeBiomes.fingerprint(profiles), BopNativeBiomes.fingerprint(Map.of("a", "first")));
        assertEquals("", BopNativeBiomes.fingerprint(Map.of()));
    }

    @Test
    public void seededChoiceDoesNotDependOnMapInsertionOrder() {
        var entries = List.of(
            new NativeWeightedTable.Entry<>("c", 2.5, "c"),
            new NativeWeightedTable.Entry<>("a", 1, "a"),
            new NativeWeightedTable.Entry<>("b", .5, "b"));
        var reverse = new ArrayList<>(entries);
        Collections.reverse(reverse);
        var one = new NativeWeightedTable<>(entries);
        var two = new NativeWeightedTable<>(reverse);
        var first = new Random(42);
        var second = new Random(42);
        var counts = new HashMap<String, Integer>();
        for (int i = 0; i < 10000; i++) {
            String value = one.choose(first);
            assertEquals(value, two.choose(second));
            counts.merge(value, 1, Integer::sum);
        }
        assertTrue(counts.get("c") > counts.get("a"));
        assertTrue(counts.get("a") > counts.get("b"));
        assertThrows(
            UnsupportedOperationException.class,
            () -> one.entries()
                .clear());
    }

    @Test
    public void invalidTablesFailClosed() {
        for (double bad : new double[] { 0, -1, Double.NaN, Double.POSITIVE_INFINITY, 1_000_001 }) assertThrows(
            IllegalArgumentException.class,
            () -> new NativeWeightedTable<>(List.of(new NativeWeightedTable.Entry<>("a", bad, "a"))));
        assertThrows(
            IllegalArgumentException.class,
            () -> new NativeWeightedTable<>(List.of(new NativeWeightedTable.Entry<String>(null, 1, "a"))));
        assertThrows(
            IllegalArgumentException.class,
            () -> new NativeWeightedTable<>(
                List.of(new NativeWeightedTable.Entry<>("a", 1, "a"), new NativeWeightedTable.Entry<>("a", 2, "b"))));
        assertThrows(IllegalStateException.class, () -> new NativeWeightedTable<>(List.of()).choose(new Random()));
    }
}
