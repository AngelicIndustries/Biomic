package industries.angelic.biomic.world.terrain;

/** The habitat owns an exposed soil skin, not the geological cross-section of a landmark. */
final class HostedSurfaceSkin {

    private HostedSurfaceSkin() {}

    static int layers(byte[] column, DecorationSurfaceColumn.Surface surface) {
        return layers(column, surface, false, false);
    }

    static int layers(byte[] column, DecorationSurfaceColumn.Surface surface, boolean landmark, boolean wetland) {
        if (surface.fluidDepth() != 0 && !wetland) return 0;
        int layers = 0;
        for (int y = surface.floorY; y >= surface.bottomY && layers < 3; y--) {
            if (landmark && !soil(column[y])) break;
            layers++;
        }
        return layers;
    }

    static boolean soil(byte material) {
        return switch (material) {
            case BiomicTerrainSampler.GRASS, BiomicTerrainSampler.DIRT, BiomicTerrainSampler.MOSS, BiomicTerrainSampler.MYCELIUM, BiomicTerrainSampler.PODZOL, BiomicTerrainSampler.COARSE_DIRT, BiomicTerrainSampler.ROOTED_DIRT, BiomicTerrainSampler.MUD, BiomicTerrainSampler.SAND, BiomicTerrainSampler.RED_SAND -> true;
            default -> false;
        };
    }

    /** A thin authored snow cap over real soil is a winter surface, not an ice mountain's geology. */
    static int nativeWinterLayers(byte[] column, DecorationSurfaceColumn.Surface surface, int ordinary) {
        if (surface.fluidDepth() != 0 || column[surface.floorY] != BiomicTerrainSampler.SNOW) return ordinary;
        int y = surface.floorY, snow = 0;
        while (y >= surface.bottomY && column[y] == BiomicTerrainSampler.SNOW && snow < 3) {
            y--;
            snow++;
        }
        if (snow > 2 || y < surface.bottomY || !soil(column[y])) return ordinary;
        int layers = snow;
        while (y >= surface.bottomY && layers < 3 && soil(column[y])) {
            layers++;
            y--;
        }
        return layers;
    }

    static int geologicalLayers(byte[] column, DecorationSurfaceColumn.Surface surface, int maximum) {
        return geologicalLayers(column, surface, maximum, false);
    }

    static int coastalLayers(byte[] column, DecorationSurfaceColumn.Surface surface, int maximum, String biome) {
        // SHALE_BEACH uses dead coral as a rock texture. RU Gravel Beach owns its dry
        // surface instead; do not mistake that source palette for a protected structure.
        return geologicalLayers(
            column,
            surface,
            maximum,
            surface.fluidDepth() == 0 && "regions_unexplored:gravel_beach".equals(biome));
    }

    private static int geologicalLayers(byte[] column, DecorationSurfaceColumn.Surface surface, int maximum,
        boolean dryShale) {
        int layers = 0;
        for (int y = surface.floorY; y >= surface.bottomY && layers < maximum; y--) {
            byte material = column[y];
            if (!soil(material) && material != BiomicTerrainSampler.STONE
                && material != BiomicTerrainSampler.SNOW
                && material != BiomicTerrainSampler.ICE
                && material != BiomicTerrainSampler.BLUE_ICE
                && material != BiomicTerrainSampler.PACKED_ICE
                && material != BiomicTerrainSampler.CHALK
                && material != BiomicTerrainSampler.CHALK_GRASS
                && material != BiomicTerrainSampler.GRAVEL
                && material != BiomicTerrainSampler.SANDSTONE
                && material != BiomicTerrainSampler.ANDESITE
                && material != BiomicTerrainSampler.DIORITE
                && material != BiomicTerrainSampler.GRANITE
                && !(dryShale && (material == BiomicTerrainSampler.DEAD_BRAIN_CORAL
                    || material == BiomicTerrainSampler.DEAD_BUBBLE_CORAL
                    || material == BiomicTerrainSampler.DEAD_HORN_CORAL)))
                break;
            layers++;
        }
        return layers;
    }
}
