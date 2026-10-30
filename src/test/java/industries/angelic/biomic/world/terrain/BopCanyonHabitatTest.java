package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import industries.angelic.biomic.compat.biome.BopNativeBiomes;
import industries.angelic.biomic.compat.biome.BopSubBiomePolicy;

public class BopCanyonHabitatTest {

    @Test
    public void dryNativeSkinAndReedAdmissionAreSeparateFromLowlandFlattening() {
        for (String key : List.of(BopNativeBiomes.CANYON, BopNativeBiomes.CANYON_RAVINE)) {
            assertTrue(BopNativeBiomes.handles(key));
            assertTrue(BopNativeBiomes.canyon(key));
            assertFalse(BopNativeBiomes.lowlandWetland(key));
            assertFalse(BopNativeBiomes.loamy(key));
            assertTrue(HostedVegetationPolicy.hasWetSurface(key));
            for (double noise : new double[] { -5, 0, 5 }) for (int depth = 0; depth < 3; depth++) {
                assertEquals(BopNativeBiomes.HARD_DIRT, BopNativeBiomes.soil(key, depth, noise));
                assertTrue(BopNativeBiomes.ownsOreSubstrate(key, depth, 3));
            }
            assertFalse(BopNativeBiomes.ownsOreSubstrate(key, 3, 3));
        }
    }

    @Test
    public void nativeRavineNeedsItsCanyonParentAndOnlyUsesAdmittedWholeLandforms() {
        String parent = BopNativeBiomes.CANYON, child = BopNativeBiomes.CANYON_RAVINE;
        assertEquals(
            parent,
            BopSubBiomePolicy.rule(child)
                .parent());
        assertFalse(BopSubBiomePolicy.validSelection(List.of(child)));
        var seen = new HashMap<String, Set<Integer>>();
        for (int family = -1; family < 40; family++) for (int i = 0; i < 512; i++) {
            assertNull(NativeBopHabitatSelector.select(42, i, family, List.of(child)));
            String key = NativeBopHabitatSelector.select(42, i, family, List.of(parent, child));
            if (key != null) seen.computeIfAbsent(key, k -> new HashSet<>())
                .add(family);
        }
        assertEquals(Map.of(parent, Set.of(-1, 12, 32), child, Set.of(-1, 12, 32)), seen);
    }
}
