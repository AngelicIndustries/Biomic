package industries.angelic.biomic.biome;

import industries.angelic.biomic.storage.BiomeVolume;

/** Block-coordinate lookup helpers for consumers of one authoritative chunk biome volume. */
public final class VerticalBiomeVolumeAccess {

    private VerticalBiomeVolumeAccess() {}

    public static String getKey(BiomeVolume volume, int localBlockX, int blockY, int localBlockZ) {
        requireCoordinates(volume, localBlockX, blockY, localBlockZ);
        return volume.getBiomeKey(localBlockX >> 2, blockY >> 2, localBlockZ >> 2);
    }

    private static void requireCoordinates(BiomeVolume volume, int localBlockX, int blockY, int localBlockZ) {
        if (volume == null) {
            throw new IllegalArgumentException("Biome volume cannot be null");
        }
        if (localBlockX < 0 || localBlockX >= 16
            || localBlockZ < 0
            || localBlockZ >= 16
            || !volume.containsBlockY(blockY)) {
            throw new IndexOutOfBoundsException(
                "Biome-volume block coordinates must be x/z in [0, 15] and y in [0, " + (volume.getBlockHeight() - 1)
                    + "]");
        }
    }
}
