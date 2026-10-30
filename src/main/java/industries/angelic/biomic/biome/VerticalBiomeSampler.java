package industries.angelic.biomic.biome;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.terrain.AuthoredVerticalProjection;
import industries.angelic.biomic.world.terrain.AuthoredVerticalProjections;
import industries.angelic.biomic.world.terrain.FloatingIslandArchetype;
import industries.angelic.biomic.world.terrain.FloatingIslandDensitySampler;
import industries.angelic.biomic.world.terrain.OrigenVerticalProfileSampler;

/** Quart-resolution storage view of Origen's exact 3D biome extrusion and optional Biomic floating islands. */
public final class VerticalBiomeSampler {

    private final int generatorVersion;
    private final FloatingIslandDensitySampler floatingIslands;
    private final OrigenVerticalProfileSampler origenProfiles;
    private final AuthoredVerticalProjection projection;

    public VerticalBiomeSampler(WorldgenContext context) {
        this(
            context,
            context.getConfig()
                .isFloatingIslandsEnabled() ? new FloatingIslandDensitySampler(context) : null);
    }

    public VerticalBiomeSampler(WorldgenContext context, FloatingIslandDensitySampler floatingIslands) {
        if (context == null) {
            throw new IllegalArgumentException("Worldgen context cannot be null");
        }
        this.generatorVersion = context.getGeneratorVersion();
        this.floatingIslands = floatingIslands;
        this.origenProfiles = new OrigenVerticalProfileSampler(context);
        this.projection = null;
    }

    /** Explicit projection path. It is never selected from physical height or another ambient runtime property. */
    public VerticalBiomeSampler(WorldgenContext context, AuthoredVerticalProjection projection) {
        this(
            context,
            context != null && context.getConfig()
                .isFloatingIslandsEnabled() ? new FloatingIslandDensitySampler(context) : null,
            projection);
    }

    public VerticalBiomeSampler(WorldgenContext context, FloatingIslandDensitySampler floatingIslands,
        AuthoredVerticalProjection projection) {
        if (context == null) throw new IllegalArgumentException("Worldgen context cannot be null");
        if (projection == null) throw new IllegalArgumentException("Origen vertical projection cannot be null");
        if (projection != AuthoredVerticalProjections.origenCompressed256()
            && projection != AuthoredVerticalProjections.shifted512()) {
            throw new IllegalArgumentException("Unsupported Origen vertical projection: " + projection.revision());
        }
        if (projection == AuthoredVerticalProjections.shifted512() && floatingIslands != null) {
            throw new IllegalArgumentException(
                "Shifted Origen biome volumes require a saved floating-island placement revision");
        }
        this.generatorVersion = context.getGeneratorVersion();
        this.floatingIslands = floatingIslands;
        this.origenProfiles = new OrigenVerticalProfileSampler(context, projection);
        this.projection = projection;
    }

    public BiomeVolume createVolume(int chunkX, int chunkZ) {
        if (projection == AuthoredVerticalProjections.shifted512()) return createShiftedVolume(chunkX, chunkZ);
        short[] cells = new short[BiomeVolume.CELL_COUNT];
        List<String> palette = new ArrayList<>();
        Map<String, Short> paletteIndices = new LinkedHashMap<>();
        palette.add(BiomeVolume.SURFACE_KEY);
        paletteIndices.put(BiomeVolume.SURFACE_KEY, (short) 0);
        int startQuartX = chunkX * BiomeVolume.QUART_WIDTH;
        int startQuartZ = chunkZ * BiomeVolume.QUART_WIDTH;
        OrigenBiomeProfile[] surfaceProfiles = new OrigenBiomeProfile[BiomeVolume.QUART_WIDTH
            * BiomeVolume.QUART_WIDTH];
        for (int localQuartZ = 0; localQuartZ < BiomeVolume.QUART_WIDTH; localQuartZ++) {
            int blockZ = (startQuartZ + localQuartZ) * 4 + 2;
            for (int localQuartX = 0; localQuartX < BiomeVolume.QUART_WIDTH; localQuartX++) {
                int blockX = (startQuartX + localQuartX) * 4 + 2;
                surfaceProfiles[localQuartZ * BiomeVolume.QUART_WIDTH + localQuartX] = origenProfiles
                    .sampleSurface(blockX, blockZ);
            }
        }
        for (int quartY = 0; quartY < BiomeVolume.QUART_HEIGHT; quartY++) {
            for (int localQuartZ = 0; localQuartZ < BiomeVolume.QUART_WIDTH; localQuartZ++) {
                for (int localQuartX = 0; localQuartX < BiomeVolume.QUART_WIDTH; localQuartX++) {
                    int worldQuartX = startQuartX + localQuartX;
                    int worldQuartZ = startQuartZ + localQuartZ;
                    OrigenBiomeProfile surfaceProfile = surfaceProfiles[localQuartZ * BiomeVolume.QUART_WIDTH
                        + localQuartX];
                    String key = sampleDefinition(worldQuartX, quartY, worldQuartZ, surfaceProfile).getKey();
                    Short paletteIndex = paletteIndices.get(key);
                    if (paletteIndex == null) {
                        paletteIndex = (short) palette.size();
                        palette.add(key);
                        paletteIndices.put(key, paletteIndex);
                    }
                    cells[index(localQuartX, quartY, localQuartZ)] = paletteIndex;
                }
            }
        }
        if (palette.size() == 1) {
            return BiomeVolume.surfaceOnly(generatorVersion);
        }
        return BiomeVolume.create(generatorVersion, palette, cells);
    }

    private BiomeVolume createShiftedVolume(int chunkX, int chunkZ) {
        short[] cells = new short[BiomeVolume.MAX_CELL_COUNT];
        List<String> palette = new ArrayList<>();
        Map<String, Short> paletteIndices = new LinkedHashMap<>();
        palette.add(BiomeVolume.SURFACE_KEY);
        paletteIndices.put(BiomeVolume.SURFACE_KEY, (short) 0);
        int startQuartX = chunkX * BiomeVolume.QUART_WIDTH;
        int startQuartZ = chunkZ * BiomeVolume.QUART_WIDTH;
        OrigenBiomeProfile[] surfaceProfiles = new OrigenBiomeProfile[BiomeVolume.QUART_WIDTH
            * BiomeVolume.QUART_WIDTH];
        for (int localQuartZ = 0; localQuartZ < BiomeVolume.QUART_WIDTH; localQuartZ++) {
            int blockZ = (startQuartZ + localQuartZ) * 4 + 2;
            for (int localQuartX = 0; localQuartX < BiomeVolume.QUART_WIDTH; localQuartX++) {
                int blockX = (startQuartX + localQuartX) * 4 + 2;
                surfaceProfiles[localQuartZ * BiomeVolume.QUART_WIDTH + localQuartX] = origenProfiles
                    .sampleSurface(blockX, blockZ);
            }
        }
        for (int quartY = 0; quartY < BiomeVolume.FULL_QUART_HEIGHT; quartY++) {
            int physicalBlockY = quartY * 4 + 2;
            if (!projection.containsPhysicalBlockY(physicalBlockY)) continue;
            for (int localQuartZ = 0; localQuartZ < BiomeVolume.QUART_WIDTH; localQuartZ++) {
                for (int localQuartX = 0; localQuartX < BiomeVolume.QUART_WIDTH; localQuartX++) {
                    int worldQuartX = startQuartX + localQuartX;
                    int worldQuartZ = startQuartZ + localQuartZ;
                    OrigenBiomeProfile surfaceProfile = surfaceProfiles[localQuartZ * BiomeVolume.QUART_WIDTH
                        + localQuartX];
                    String key = sampleDefinition(worldQuartX, quartY, worldQuartZ, surfaceProfile).getKey();
                    Short paletteIndex = paletteIndices.get(key);
                    if (paletteIndex == null) {
                        paletteIndex = (short) palette.size();
                        palette.add(key);
                        paletteIndices.put(key, paletteIndex);
                    }
                    cells[index(localQuartX, quartY, localQuartZ)] = paletteIndex;
                }
            }
        }
        if (palette.size() == 1) return BiomeVolume.surfaceOnly(generatorVersion, BiomeVolume.FULL_BLOCK_HEIGHT);
        return BiomeVolume.create(generatorVersion, BiomeVolume.FULL_BLOCK_HEIGHT, palette, cells);
    }

    public FloatingIslandDensitySampler getFloatingIslandSampler() {
        return floatingIslands;
    }

    private VerticalBiomeDefinition sampleDefinition(int worldQuartX, int quartY, int worldQuartZ,
        OrigenBiomeProfile surfaceProfile) {
        int blockX = worldQuartX * 4 + 2;
        int blockY = quartY * 4 + 2;
        int blockZ = worldQuartZ * 4 + 2;
        if (floatingIslands != null) {
            VerticalBiomeDefinition islandDefinition = floatingDefinition(blockX, blockY, blockZ);
            if (islandDefinition != null) {
                return islandDefinition;
            }
        }
        OrigenBiomeProfile profile = origenProfiles.extrude(surfaceProfile, blockX, blockY, blockZ);
        VerticalBiomeDefinition caveDefinition = VerticalBiomeCatalogue.getOrigenCave(profile);
        return resolveCave(caveDefinition);
    }

    private VerticalBiomeDefinition resolveCave(VerticalBiomeDefinition fallback) {
        return fallback == null ? VerticalBiomeCatalogue.SURFACE : fallback;
    }

    private VerticalBiomeDefinition floatingDefinition(int blockX, int blockY, int blockZ) {
        FloatingIslandArchetype archetype = floatingIslands.sampleArchetype(blockX, blockY, blockZ);
        if (archetype == FloatingIslandArchetype.RING_BASIN) {
            return VerticalBiomeCatalogue.SKY_DONUT;
        }
        if (archetype == FloatingIslandArchetype.AEROPELAGO) {
            VerticalBiomeDefinition hosted = VerticalBiomeCatalogue
                .getAeropelagoHost(floatingIslands.sampleProfile(blockX, blockY, blockZ));
            return hosted == null ? VerticalBiomeCatalogue.AEROPELAGO : hosted;
        }
        return VerticalBiomeCatalogue.getFloatingIsland(floatingIslands.sampleProfile(blockX, blockY, blockZ));
    }

    private static int index(int quartX, int quartY, int quartZ) {
        return (quartY * BiomeVolume.QUART_WIDTH + quartZ) * BiomeVolume.QUART_WIDTH + quartX;
    }
}
