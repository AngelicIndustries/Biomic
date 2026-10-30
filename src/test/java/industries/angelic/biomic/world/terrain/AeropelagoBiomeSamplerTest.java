package industries.angelic.biomic.world.terrain;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;

public class AeropelagoBiomeSamplerTest {

    @Test
    public void broadMosaicContainsMeadowsAndEveryHostedOrigenHabitat() {
        AeropelagoBiomeSampler sampler = new AeropelagoBiomeSampler(42L);
        Set<OrigenBiomeProfile> profiles = EnumSet.noneOf(OrigenBiomeProfile.class);
        boolean meadow = false;
        for (int z = -512; z <= 512; z += 8) {
            for (int x = -512; x <= 512; x += 8) {
                OrigenBiomeProfile profile = sampler.sample(0x1234_5678_9ABC_DEF0L, x, z);
                meadow |= profile == null;
                if (profile != null) profiles.add(profile);
            }
        }

        Assert.assertTrue("the hosted mosaic has no open Aeropelago meadow", meadow);
        Assert.assertEquals(
            EnumSet.of(
                OrigenBiomeProfile.LUSH_LOOPS,
                OrigenBiomeProfile.SAKURA_STREAMS,
                OrigenBiomeProfile.FOLIAGE_FORTRESS_INNER,
                OrigenBiomeProfile.TALL_TIMBERLAND),
            profiles);
    }

    @Test
    public void habitatsAreBroadRatherThanChunkScaleNoise() {
        AeropelagoBiomeSampler sampler = new AeropelagoBiomeSampler(42L);
        long cluster = 0x1234_5678_9ABC_DEF0L;
        int agreements = 0;
        int comparisons = 0;
        for (int z = -384; z <= 384; z += 16) {
            for (int x = -384; x <= 384; x += 16) {
                OrigenBiomeProfile center = sampler.sample(cluster, x, z);
                OrigenBiomeProfile east = sampler.sample(cluster, x + 16, z);
                OrigenBiomeProfile south = sampler.sample(cluster, x, z + 16);
                if (center == east) agreements++;
                if (center == south) agreements++;
                comparisons += 2;
            }
        }
        Assert.assertTrue("hosted habitats fragment at chunk scale", agreements >= comparisons * 0.82D);
    }
}
