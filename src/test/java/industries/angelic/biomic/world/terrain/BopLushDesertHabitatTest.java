package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import industries.angelic.biomic.compat.biome.BopGroundCoverSupport;
import industries.angelic.biomic.compat.biome.BopNativeBiomes;

public class BopLushDesertHabitatTest {

    @Test
    public void nativeClayGrassIntervalsHaveNoPreviousColumnState() {
        String key = BopNativeBiomes.LUSH_DESERT;
        for (int repeat = 0; repeat < 10; repeat++)
            for (double n : new double[] { 5, -5, 1.75, 1.750001, -.5, -.49999, 0 }) {
                assertEquals(
                    n > 1.75 ? "minecraft:grass_block[snowy=false]" : "minecraft:terracotta",
                    BopNativeBiomes.soil(key, 0, n));
                assertEquals(n > 1.75 ? "minecraft:dirt" : "minecraft:terracotta", BopNativeBiomes.soil(key, 1, n));
            }
        assertTrue(BopNativeBiomes.usesSurfaceNoise(key));
        assertTrue(BopNativeBiomes.reservesOreSubstrate(key));
        for (int d = -1; d <= 9; d++) assertEquals(d >= 0 && d < 8, BopNativeBiomes.ownsOreSubstrate(key, d, 3));
        assertEquals(8, HostedVegetationPolicy.foundationDepth("biomic:native_bop/" + key + "/oasesPerChunk"));
        assertEquals(8, HostedVegetationPolicy.foundationDepth("biomic:native_bop/" + key + "/waterLakesPerChunk"));
        assertEquals(
            Boolean.FALSE,
            BopGroundCoverSupport.test("biomesoplenty:plants[legacy_meta=1]", "minecraft:grass_block"));
        assertEquals(
            Boolean.TRUE,
            BopGroundCoverSupport.test("biomesoplenty:plants[legacy_meta=1]", "minecraft:terracotta"));
    }

    @Test
    public void warmHostAdmissionRemainsSaveOptIn() {
        String key = BopNativeBiomes.LUSH_DESERT;
        var hits = new HashSet<Integer>();
        var allowed = Set.of(-1, 3, 12, 32);
        var old = BopNativeBiomes.supportedKeys()
            .stream()
            .filter(k -> !k.equals(key))
            .toList();
        for (int family = -1; family <= 40; family++) for (int anchor = 0; anchor < 500; anchor++) {
            var selected = NativeBopHabitatSelector.select(42, anchor, family, List.of(key));
            if (allowed.contains(family)) {
                if (selected != null) hits.add(family);
            } else assertNull(selected);
            assertNotEquals(key, NativeBopHabitatSelector.select(42, anchor, family, old));
        }
        assertEquals(allowed, hits);
        assertTrue(HostedVegetationPolicy.hasWetSurface(key));
        assertEquals(12, HostedVegetationPolicy.waterDepthLimit(key));
    }
}
