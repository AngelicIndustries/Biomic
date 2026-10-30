package industries.angelic.biomic.compat.biome;

import static org.junit.Assert.*;

import org.junit.Test;

public class SaltyBrickmakerCampTest {

    @Test
    public void captureEnvelopeIsBoundedAndOverflowSafe() {
        assertTrue(SaltyBrickmakerCamp.insideHorizontalEnvelope(100, -100, 60, -140));
        assertTrue(SaltyBrickmakerCamp.insideHorizontalEnvelope(100, -100, 140, -60));
        assertFalse(SaltyBrickmakerCamp.insideHorizontalEnvelope(100, -100, 59, -100));
        assertFalse(SaltyBrickmakerCamp.insideHorizontalEnvelope(Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0));
    }

    @Test
    public void planDefensivelyCopiesItsPlacementMap() {
        var source = new java.util.LinkedHashMap<SaltyBrickmakerCamp.Position, SaltyBrickmakerCamp.Placement>();
        source.put(new SaltyBrickmakerCamp.Position(1, 2, 3), new SaltyBrickmakerCamp.Placement("minecraft:air", null));
        var plan = new SaltyBrickmakerCamp.Plan(source);
        source.clear();
        assertEquals(
            1,
            plan.placements()
                .size());
        assertThrows(
            UnsupportedOperationException.class,
            () -> plan.placements()
                .clear());
    }

    @Test
    public void nativeVanillaPaletteKeepsEveryEmittedMetadataValue() {
        assertPalette("minecraft:wool", 0, 7, 8, 12, 15);
        assertPalette("minecraft:bed", 0, 1, 2, 3, 8, 9, 10, 11);
        assertPalette("minecraft:chest", 2, 3, 4, 5);
        assertPalette("minecraft:fence_gate", 0, 1, 2, 3);
        assertPalette("minecraft:hay_block", 0, 4, 8);
        assertPalette("minecraft:wooden_slab", 0);
    }

    @Test
    public void nativeVanillaPaletteRejectsStatesTheCampCannotEmit() {
        assertNull(SaltyBrickmakerCamp.encodeVanillaCampState("minecraft:wool", 1));
        assertNull(SaltyBrickmakerCamp.encodeVanillaCampState("minecraft:bed", 4));
        assertNull(SaltyBrickmakerCamp.encodeVanillaCampState("minecraft:chest", 1));
        assertNull(SaltyBrickmakerCamp.encodeVanillaCampState("minecraft:fence_gate", 4));
        assertNull(SaltyBrickmakerCamp.encodeVanillaCampState("minecraft:hay_block", 1));
        assertNull(SaltyBrickmakerCamp.encodeVanillaCampState("minecraft:wooden_slab", 8));
        assertNull(SaltyBrickmakerCamp.encodeVanillaCampState("minecraft:fence", 0));
        assertNull(SaltyBrickmakerCamp.encodeVanillaCampState(null, 0));
    }

    private static void assertPalette(String registry, int... metadata) {
        for (int value : metadata) assertEquals(
            registry + "[legacy_meta=" + value + "]",
            SaltyBrickmakerCamp.encodeVanillaCampState(registry, value));
    }
}
