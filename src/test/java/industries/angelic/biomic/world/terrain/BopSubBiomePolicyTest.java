package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.junit.Test;

import industries.angelic.biomic.compat.biome.BopNativeBiomes;
import industries.angelic.biomic.compat.biome.BopSubBiomePolicy;
import industries.angelic.biomic.compat.biome.NativeHabitatSettings;

public class BopSubBiomePolicyTest {

    @Test
    public void variantsRequireTheirNativeParentAndNeverBecomeIndependentPoolEntries() {
        assertFalse(BopSubBiomePolicy.isChild(null));
        assertNull(BopSubBiomePolicy.rule(BopNativeBiomes.GROVE));
        for (String child : List.of(
            BopNativeBiomes.ALPS_FOREST,
            BopNativeBiomes.MEADOW_FOREST,
            BopNativeBiomes.SEASONAL_CLEARING,
            BopNativeBiomes.GLACIER)) {
            var rule = BopSubBiomePolicy.rule(child);
            assertNotNull(rule);
            assertFalse(BopSubBiomePolicy.validSelection(List.of(child)));
            assertTrue(BopSubBiomePolicy.validSelection(List.of(rule.parent(), child)));
            assertThrows(
                IllegalArgumentException.class,
                () -> NativeHabitatSettings.OFF.withBop("a".repeat(64), List.of(child)));
            int childCount = 0, parentCount = 0;
            for (int i = 0; i < 10000; i++) {
                for (int family : new int[] { -1, 1, 2, 3, 4, 10, 11, 14, 15, 16, 17, 20, 22, 30, 31, 32, 38 })
                    assertNull(NativeBopHabitatSelector.select(42, i, family, List.of(child)));
                String selected = BopSubBiomePolicy.select(rule.parent(), i, List.of(rule.parent(), child));
                if (selected.equals(child)) childCount++;
                else {
                    assertEquals(rule.parent(), selected);
                    parentCount++;
                }
                assertEquals(rule.parent(), BopSubBiomePolicy.select(rule.parent(), i, List.of(rule.parent())));
                assertEquals(selected, BopSubBiomePolicy.select(rule.parent(), i, List.of(child, rule.parent())));
            }
            assertTrue(childCount > 2000 && childCount < 3000);
            assertEquals(10000, childCount + parentCount);
        }
    }

    @Test
    public void everyNewChoiceIsAVariantOfTheSamePreviouslySelectedParent() {
        var old = BopNativeBiomes.supportedKeys()
            .stream()
            .filter(k -> !BopSubBiomePolicy.isChild(k))
            .toList();
        var all = BopNativeBiomes.supportedKeys()
            .stream()
            .filter(k -> !BopSubBiomePolicy.externalParent(k))
            .toList();
        var found = new HashSet<String>();
        for (int family : new int[] { -1, 1, 2, 3, 4, 10, 11, 14, 15, 16, 17, 20, 22, 24, 25, 28, 30, 31, 32, 38 })
            for (int i = 0; i < 10000; i++) {
                String before = NativeBopHabitatSelector.select(42, i, family, old),
                    after = NativeBopHabitatSelector.select(42, i, family, all);
                if (!Objects.equals(before, after)) {
                    var rule = BopSubBiomePolicy.rule(after);
                    assertNotNull(rule);
                    assertEquals(before, rule.parent());
                    found.add(after);
                }
            }
        assertEquals(
            all.stream()
                .filter(BopSubBiomePolicy::isChild)
                .collect(java.util.stream.Collectors.toSet()),
            found);
    }

    @Test
    public void sludgepitSiblingQuartersAreDisjointAndMissingSiblingsDoNotStealBuckets() {
        String parent = BopNativeBiomes.SLUDGEPIT, a = BopNativeBiomes.QUAGMIRE, b = BopNativeBiomes.SILKGLADES;
        var counts = new HashMap<String, Integer>();
        for (int i = 0; i < 10000; i++) {
            String both = BopSubBiomePolicy.select(parent, i, List.of(parent, a, b));
            counts.merge(both, 1, Integer::sum);
            assertEquals(both, BopSubBiomePolicy.select(parent, i, List.of(b, a, parent)));
            assertEquals(both.equals(a) ? a : parent, BopSubBiomePolicy.select(parent, i, List.of(parent, a)));
            assertEquals(both.equals(b) ? b : parent, BopSubBiomePolicy.select(parent, i, List.of(parent, b)));
            assertEquals(parent, BopSubBiomePolicy.select(parent, i, List.of(parent)));
            for (String older : List.of(
                BopNativeBiomes.ALPS_FOREST,
                BopNativeBiomes.MEADOW_FOREST,
                BopNativeBiomes.SEASONAL_CLEARING,
                BopNativeBiomes.GLACIER)) {
                var rule = BopSubBiomePolicy.rule(older);
                long hash = industries.angelic.biomic.world.WorldgenContext
                    .mix64(i ^ older.hashCode() ^ 0x424f505355423031L);
                assertEquals(
                    Math.floorMod(hash, 4) == 0 ? older : rule.parent(),
                    BopSubBiomePolicy.select(rule.parent(), i, List.of(rule.parent(), older, a, b)));
            }
        }
        for (String child : List.of(a, b)) {
            assertTrue(counts.get(child) > 2000 && counts.get(child) < 3000);
            assertFalse(BopSubBiomePolicy.validSelection(List.of(child)));
        }
        assertTrue(counts.get(parent) > 4500 && counts.get(parent) < 5500);
    }
}
