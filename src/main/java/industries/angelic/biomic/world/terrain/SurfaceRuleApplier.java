package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.biome.VerticalBiomeCatalogue;
import industries.angelic.biomic.biome.VerticalBiomeDefinition;
import industries.angelic.biomic.biome.VerticalBiomeVolumeAccess;
import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.world.WorldgenContext;

/** Exterior surface, cliff, beach and seabed rules applied after base density and carving. */
final class SurfaceRuleApplier {

    private final OrigenSurfaceRuntime origenSurfaceRuntime;

    SurfaceRuleApplier(WorldgenContext context, TerrainDensitySampler densitySampler) {
        if (context == null || densitySampler == null) {
            throw new IllegalArgumentException("Worldgen context and density sampler cannot be null");
        }
        origenSurfaceRuntime = OrigenSurfaceRuntime.shared(context);
    }

    void apply(byte[] materials, byte[] solidOrigins, TerrainColumn[][] neighborhood, int chunkX, int chunkZ,
        BiomeVolume biomeVolume) {
        if (biomeVolume == null) {
            throw new IllegalArgumentException("Biome volume cannot be null");
        }
        origenSurfaceRuntime.apply(materials, solidOrigins, chunkX, chunkZ);
        for (int localX = 0; localX < BiomicTerrainSampler.CHUNK_WIDTH; localX++) {
            for (int localZ = 0; localZ < BiomicTerrainSampler.CHUNK_WIDTH; localZ++) {
                applyFloatingIslandSurfaces(
                    materials,
                    solidOrigins,
                    localX,
                    localZ,
                    neighborhood[localX + 2][localZ + 2],
                    biomeVolume);
            }
        }
    }

    /** Executes only Origen's exact palette/carver projection; strict mode has no RU or floating surfaces. */
    void applyStrictOrigen(byte[] materials, int chunkX, int chunkZ) {
        origenSurfaceRuntime.applyStrict(materials, chunkX, chunkZ);
    }

    byte[] cachedAuthoredChunk(int chunkX, int chunkZ) {
        return origenSurfaceRuntime.cachedAuthoredChunk(chunkX, chunkZ);
    }

    private void applyFloatingIslandSurfaces(byte[] materials, byte[] solidOrigins, int localX, int localZ,
        TerrainColumn surfaceColumn, BiomeVolume biomeVolume) {
        for (int y = BiomicTerrainSampler.WORLD_HEIGHT - 2; y >= FloatingIslandDensitySampler.MIN_Y; y--) {
            int index = BiomicTerrainSampler.index(localX, y, localZ);
            if (materials[index] != BiomicTerrainSampler.STONE
                || (solidOrigins[index] & TerrainDensitySampler.FLOATING_ORIGIN) == 0
                || (solidOrigins[index] & TerrainDensitySampler.GROUND_ORIGIN) != 0
                || materials[BiomicTerrainSampler.index(localX, y + 1, localZ)] != BiomicTerrainSampler.AIR) {
                continue;
            }
            String storedKey = VerticalBiomeVolumeAccess.getKey(biomeVolume, localX, y, localZ);
            VerticalBiomeDefinition definition = VerticalBiomeCatalogue.behaviorFor(storedKey);
            SurfacePalette palette;
            if (definition.getFloatingSurface() == VerticalBiomeDefinition.FloatingSurface.TEMPERATE_GRASS) {
                palette = SurfacePalette.GRASS;
            } else {
                OrigenBiomeProfile profile = VerticalBiomeCatalogue.getFloatingIslandProfile(storedKey);
                if (profile == null) {
                    // The surface sentinel deliberately inherits the ordinary column's surface behavior.
                    profile = surfaceColumn.getPrimaryProfile();
                }
                palette = palette(profile);
            }
            replaceFloatingTopStone(materials, solidOrigins, localX, localZ, y - 1, 2, palette.filler);
            materials[index] = palette.top;
        }
    }

    private static SurfacePalette palette(OrigenBiomeProfile profile) {
        switch (profile) {
            case ABYSSAL_ALLEYS:
                return SurfacePalette.GRAVEL;
            case STONEGATE_SEAS:
            case ARCTIC_ARCHES:
            case MARINE_MONOLITHS:
                return SurfacePalette.STONE;
            case ARID_ARBORETUM:
            case BADLANDS_BALCONIES:
            case CARVING_CREAKS:
            case MESA_MONUMENTS:
            case SANDY_SPLITS:
                return SurfacePalette.TERRACOTTA;
            case OASIS:
                return SurfacePalette.SAND;
            case ICY_INCISIONS:
            case FROSTY_FINGERS:
            case MOUNTAIN_MIRRORS:
            case ALPINE_ASCENDANCY:
            case SNOWY_SPIRES:
            case TUNDRA_TRACKS:
            case BARE_BOULDERFIELDS:
                return SurfacePalette.SNOW;
            case FROZEN_FUNGI:
                return SurfacePalette.MYCELIUM;
            case MURKY_MARSHLANDS:
            case SECLUDED_VALLEY:
            case SINKHOLE_FOREST:
            case SINKHOLE_JUNGLE:
                return SurfacePalette.MOSS;
            case SINKHOLE_FROZEN:
                return SurfacePalette.SNOW;
            case WATERY_WILDS:
            case WHITE_WALLOWS:
                return SurfacePalette.STONE_GRASS;
            default:
                return SurfacePalette.GRASS;
        }
    }

    private static void replaceFloatingTopStone(byte[] materials, byte[] solidOrigins, int localX, int localZ, int topY,
        int depth, byte replacement) {
        int replaced = 0;
        for (int y = topY; y >= 1 && replaced < depth; y--) {
            int index = BiomicTerrainSampler.index(localX, y, localZ);
            if (materials[index] != BiomicTerrainSampler.STONE
                || (solidOrigins[index] & TerrainDensitySampler.FLOATING_ORIGIN) == 0
                || (solidOrigins[index] & TerrainDensitySampler.GROUND_ORIGIN) != 0) {
                break;
            }
            materials[index] = replacement;
            replaced++;
        }
    }

    private enum SurfacePalette {

        GRASS(BiomicTerrainSampler.GRASS, BiomicTerrainSampler.DIRT),
        STONE_GRASS(BiomicTerrainSampler.GRASS, BiomicTerrainSampler.STONE),
        STONE(BiomicTerrainSampler.STONE, BiomicTerrainSampler.STONE),
        GRAVEL(BiomicTerrainSampler.GRAVEL, BiomicTerrainSampler.GRAVEL),
        SAND(BiomicTerrainSampler.SAND, BiomicTerrainSampler.SAND),
        TERRACOTTA(BiomicTerrainSampler.TERRACOTTA, BiomicTerrainSampler.TERRACOTTA),
        SNOW(BiomicTerrainSampler.SNOW, BiomicTerrainSampler.DIRT),
        MYCELIUM(BiomicTerrainSampler.MYCELIUM, BiomicTerrainSampler.DIRT),
        MOSS(BiomicTerrainSampler.MOSS, BiomicTerrainSampler.DIRT);

        private final byte top;
        private final byte filler;

        SurfacePalette(byte top, byte filler) {
            this.top = top;
            this.filler = filler;
        }
    }
}
