package industries.angelic.biomic.world.terrain;

import com.dfsek.seismic.type.sampler.Sampler;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.biome.origen.OrigenTerrainFamily;

/**
 * Where a column looks for the biome that skins it (terrain study 3.4): up to four blocks away on a 1/24 noise, so a
 * palette border follows an organic curve instead of the biome map's 4-block steps. Only the skin moves; biome
 * identity, terrain, fluids and features stay with the column's own biome.
 */
final class SkinJitter {

    private static final double AMPLITUDE = 4.0D;
    private static final Sampler NOISE = new Terra64OpenSimplex2Sampler(1.0D / 24.0D, 0L);

    private SkinJitter() {}

    static int x(long seed, int x, int z) {
        return (int) Math.floor(x + NOISE.getSample(seed + 31L, x, z) * AMPLITUDE);
    }

    static int z(long seed, int x, int z) {
        return (int) Math.floor(z + NOISE.getSample(seed + 32L, x, z) * AMPLITUDE);
    }

    /**
     * Origen ground whose skin may cross a border: land. Canyons, sinkholes, rivers, coasts, seas and caves keep their
     * own: a jittered canyon wall would take the rim's grass, and a beach's gravel and dead coral would reach under a
     * hosted habitat that re-skins only soil.
     */
    static boolean origenLand(OrigenBiomeProfile profile) {
        return profile.getKind() == OrigenBiomeProfile.Kind.LAND
            && profile.getTerrainFamily() != OrigenTerrainFamily.RIVER_CHANNEL;
    }
}
