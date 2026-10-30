package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import org.junit.Test;

import industries.angelic.biomic.world.alexscaves.OceanTrenchPlanner;

public class AbyssalRegionSamplerTest {

    @Test
    public void startsStayOnSourceGridAndAreDeterministicAcrossNegativeRegions() {
        AbyssalRegionSampler sampler = new AbyssalRegionSampler(42, (x, z) -> 40);
        for (int rx = -3; rx <= 3; rx++) for (int rz = -3; rz <= 3; rz++) {
            HostedAbyssalRuntime.Start a = sampler.start(rx, rz), b = sampler.start(rx, rz);
            assertEquals(a, b);
            assertEquals(0, Math.floorMod(a.chunkX(), 2));
            assertEquals(0, Math.floorMod(a.chunkZ(), 2));
            assertEquals(rx, Math.floorDiv(a.chunkX(), 32));
            assertEquals(rz, Math.floorDiv(a.chunkZ(), 32));
        }
    }

    @Test
    public void dryOrShallowLandCanNeverOwnAnAbyssalCell() {
        AbyssalRegionSampler dry = new AbyssalRegionSampler(7, (x, z) -> 70);
        HostedAbyssalRuntime.Start start = dry.start(0, 0);
        assertEquals(
            HostedAbyssalRuntime.Identity.INCOMPATIBLE,
            dry.identity(start.x() + 8, OceanTrenchPlanner.SOURCE_CENTER_Y, start.z() + 8));
        assertTrue(
            dry.candidates(start.chunkX(), start.chunkZ())
                .isEmpty());
    }

    @Test
    public void physicalHeightDoesNotEnterSavedRegionalOwnership() {
        AbyssalRegionSampler ocean = new AbyssalRegionSampler(7, (x, z) -> 50);
        HostedAbyssalRuntime.Start start = ocean.start(-1, 2);
        assertEquals(
            HostedAbyssalRuntime.Identity.ABYSSAL,
            ocean.identity(start.x() + 8, OceanTrenchPlanner.SOURCE_CENTER_Y, start.z() + 8));
        assertFalse(
            ocean.candidates(start.chunkX(), start.chunkZ())
                .isEmpty());
    }
}
