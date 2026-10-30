package industries.angelic.biomic.world.terrain;

import java.util.BitSet;

import industries.angelic.biomic.biome.VerticalBiomeSampler;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.world.WorldgenContext;

/** Facade over Biomic's independent density, river, carving, and surface-rule stages. */
public final class BiomicTerrainSampler implements HostedTerrainAccess {

    public static final int CHUNK_WIDTH = 16;
    public static final int WORLD_HEIGHT = 256;
    public static final int BLOCK_COUNT = CHUNK_WIDTH * CHUNK_WIDTH * WORLD_HEIGHT;
    public static final byte AIR = 0;
    public static final byte BEDROCK = 1;
    public static final byte STONE = 2;
    public static final byte DIRT = 3;
    public static final byte GRASS = 4;
    public static final byte WATER = 5;
    public static final byte SAND = 6;
    public static final byte GRAVEL = 7;
    public static final byte MOSS = 8;
    public static final byte TERRACOTTA = 9;
    public static final byte BASALT = 10;
    public static final byte MAGMA = 11;
    public static final byte SNOW = 12;
    public static final byte MYCELIUM = 13;
    public static final byte ICE = 14;
    public static final byte CHALK = 15;
    public static final byte TUFF = 16;
    public static final byte PODZOL = 17;
    public static final byte MUD = 18;
    public static final byte RED_TERRACOTTA = 19;
    public static final byte ORANGE_TERRACOTTA = 20;
    public static final byte BROWN_TERRACOTTA = 21;
    public static final byte COARSE_DIRT = 22;
    public static final byte PACKED_MUD = 23;
    public static final byte RED_CONCRETE = 24;
    public static final byte BROWN_CONCRETE = 25;
    public static final byte LIGHT_GRAY_TERRACOTTA = 26;
    public static final byte PINK_TERRACOTTA = 27;
    public static final byte GRANITE = 28;
    public static final byte SANDSTONE = 29;
    public static final byte ANDESITE = 30;
    public static final byte BLACKSTONE = 31;
    public static final byte BLUE_ICE = 32;
    public static final byte BLUE_TERRACOTTA = 33;
    public static final byte CALCITE = 34;
    public static final byte CLAY = 35;
    public static final byte CYAN_TERRACOTTA = 36;
    public static final byte DEAD_BRAIN_CORAL = 37;
    public static final byte DEAD_BUBBLE_CORAL = 38;
    public static final byte DEAD_HORN_CORAL = 39;
    public static final byte DEEPSLATE = 40;
    public static final byte DIORITE = 41;
    public static final byte DRIPSTONE_BLOCK = 42;
    public static final byte FARMLAND_MOIST = 43;
    public static final byte LAVA = 44;
    public static final byte LIGHT_BLUE_TERRACOTTA = 45;
    public static final byte LIME_TERRACOTTA = 46;
    public static final byte MOSSY_COBBLESTONE = 47;
    public static final byte OBSIDIAN = 48;
    public static final byte POWDER_SNOW = 49;
    public static final byte RED_SAND = 50;
    public static final byte RED_SANDSTONE = 51;
    public static final byte ROOTED_DIRT = 52;
    public static final byte SMOOTH_BASALT = 53;
    public static final byte SMOOTH_SANDSTONE = 54;
    public static final byte WHITE_CONCRETE_POWDER = 55;
    public static final byte WHITE_TERRACOTTA = 56;
    public static final byte YELLOW_CONCRETE = 57;
    public static final byte YELLOW_TERRACOTTA = 58;
    public static final byte COBBLESTONE = 59;
    public static final byte CHALK_GRASS = 60;
    public static final byte PEAT_GRASS = 61;
    public static final byte PEAT_DIRT = 62;
    public static final byte SILT_GRASS = 63;
    public static final byte SILT_DIRT = 64;
    public static final byte ASHEN_DIRT = 65;
    public static final byte ASH = 66;
    public static final byte ARGILLITE = 67;
    public static final byte ARGILLITE_GRASS = 68;
    public static final byte VIRIDESCENT_NYLIUM = 69;
    public static final byte DEEPSLATE_VIRIDESCENT_NYLIUM = 70;
    public static final byte PRISMOSS = 71;
    public static final byte DEEPSLATE_PRISMOSS = 72;
    public static final byte BRIMSPROUT_NYLIUM = 74;
    public static final byte COBALT_NYLIUM = 75;
    public static final byte GLISTERING_NYLIUM = 76;
    public static final byte MYCOTOXIC_MOSS = 77;
    public static final byte PEAT_MUD = 78;
    public static final byte SILT_MUD = 79;
    public static final byte PEAT_PODZOL = 80;
    public static final byte SILT_PODZOL = 81;
    public static final byte FORLORN_GUANOSTONE = 82;
    public static final byte FORLORN_COPROLITH = 83;
    public static final byte PRIMORDIAL_LIMESTONE = 84;
    public static final byte ABYSSMARINE = 85;
    public static final byte ABYSSAL_MUCK = 86;
    public static final byte PRIMORDIAL_FLOOD_BASALT = 87;
    public static final byte PRIMORDIAL_MAGMA = 88;
    public static final byte PRIMORDIAL_MAGMA_ACTIVE = 89;
    public static final byte PRIMORDIAL_VOLCANIC_CORE = 90;
    public static final byte PRIMORDIAL_MOSS_CARPET = 91;
    public static final byte PRIMORDIAL_MOSS_BLOCK = 92;
    /** Opaque ice for terrain; vanilla's ice is translucent and shows its inner faces in mixes. */
    public static final byte PACKED_ICE = 93;
    public static final byte MAX_MATERIAL = PACKED_ICE;

    private final TerrainColumnSampler columnSampler;
    private final TerrainDensitySampler densitySampler;
    private final OrigenDensityField exactOrigenField;
    private final SurfaceRuleApplier exteriorSurfaceRules;
    private final VerticalBiomeSampler verticalBiomeSampler;
    private final boolean strictOrigenBaseline;
    private final HostedOceanDepth oceanDepth;
    private final HostedCaveExpansion caveExpansion;
    private final AbyssalTrenches trenches;
    private final HostedMarshTerrain marshTerrain;
    private final HostedGardenCoastTerrain gardenCoastTerrain;
    private final HostedBopMarineTerrain bopMarineTerrain;
    private final java.util.List<String> nativeBopHabitats;
    private volatile SkyIslandHabitats skyHabitats;
    private final RegionalHabitatSampler nativeSandRegions;

    @Override
    public industries.angelic.biomic.api.ChunkVerticalLayout layout() {
        return industries.angelic.biomic.api.ChunkVerticalLayout.LEGACY_256;
    }

    @Override
    public int minimumEcologyY() {
        return 0;
    }

    @Override
    public AuthoredVerticalProjection projection() {
        return AuthoredVerticalProjections.origenCompressed256();
    }

    public BiomicTerrainSampler(WorldgenContext context, MacroTerrainSampler macroSampler,
        VerticalBiomeSampler verticalBiomeSampler) {
        if (context == null || macroSampler == null || verticalBiomeSampler == null) {
            throw new IllegalArgumentException("Worldgen context and terrain/biome samplers cannot be null");
        }
        OrigenTerrainGraph terrainGraph = new OrigenTerrainGraph(
            context,
            verticalBiomeSampler.getFloatingIslandSampler());
        columnSampler = new TerrainColumnSampler(macroSampler, terrainGraph);
        densitySampler = new TerrainDensitySampler(context, columnSampler, terrainGraph);
        exactOrigenField = OrigenDensityField.shared(context);
        this.verticalBiomeSampler = verticalBiomeSampler;
        exteriorSurfaceRules = new SurfaceRuleApplier(context, densitySampler);
        strictOrigenBaseline = context.getConfig()
            .isStrictOrigenBaseline();
        oceanDepth = new HostedOceanDepth(context);
        marshTerrain = context.getNativeHabitats()
            .fullMarsh()
            || context.getNativeHabitats()
                .bopHabitats()
                .stream()
                .anyMatch(industries.angelic.biomic.compat.biome.BopNativeBiomes::lowlandWetland)
                    ? new HostedMarshTerrain(context)
                    : null;
        gardenCoastTerrain = context.getNativeHabitats()
            .bopHabitats()
            .contains(industries.angelic.biomic.compat.biome.BopNativeBiomes.GARDEN)
                ? new HostedGardenCoastTerrain(context)
                : null;
        nativeBopHabitats = context.getNativeHabitats()
            .bopHabitats();
        if (verticalBiomeSampler.getFloatingIslandSampler() != null) {
            skyHabitats = new SkyIslandHabitats(
                verticalBiomeSampler.getFloatingIslandSampler(),
                IslandBiomeRules.DEFAULT,
                nativeBopHabitats);
            densitySampler.setIslandKinds((x, z) -> IslandKind.of(skyHabitats.at(x, z)));
        }
        bopMarineTerrain = nativeBopHabitats.stream()
            .anyMatch(HostedBopMarineTerrain::marine) ? new HostedBopMarineTerrain(context) : null;
        nativeSandRegions = nativeBopHabitats.stream()
            .anyMatch(HostedSandBase::ownsHabitat) ? new RegionalHabitatSampler(context) : null;
        caveExpansion = context.getCaveExpansion()
            .enabled()
                ? new HostedCaveExpansion(
                    context.getSettings()
                        .getWorldSeed(),
                    context.getCaveExpansion(),
                    (cx, cz) -> createUnexpanded(cx, cz, verticalBiomeSampler.createVolume(cx, cz)))
                : null;
        if (caveExpansion != null) {
            // The 256 planner's rows are physical: the pocket's authored heights compressed into them.
            CavePockets pockets = new CavePockets(
                context.getSettings()
                    .getWorldSeed());
            caveExpansion.setPockets((x, z) -> {
                int range = pockets.range(x, z);
                if (range < 0) return -1;
                int low = (int) Math.floor(OrigenVerticalTransform.toPhysicalY(CavePockets.low(range)));
                int high = (int) Math.ceil(OrigenVerticalTransform.toPhysicalY(CavePockets.high(range)));
                return (low + 1024) << 16 | (high + 1024);
            });
        }
        // The Abyssal Chasm is part of the terrain caves are planned around: they keep clear of its water.
        trenches = context.getAlexCaves()
            .abyssalEnabled()
                ? AbyssalTrenches.legacy256(
                    context,
                    (cx, cz) -> createUntrenched(cx, cz, verticalBiomeSampler.createVolume(cx, cz)).materials())
                : null;
    }

    /** Returns the highest solid crossing in the terrain graph, not its two-dimensional offset input. */
    public int sampleSurfaceHeight(int blockX, int blockZ) {
        return densitySampler.sampleSurfaceHeight(blockX, blockZ, false);
    }

    /**
     * Highest ground block left by Origen's authored density and carver fields, excluding Biomic's sky layer.
     * Structure admission must use this instead of a broad height offset or a floating mass overhead.
     */
    public int sampleCarvedGroundSurfaceHeight(int blockX, int blockZ) {
        long key = (long) blockX << 32 ^ (blockZ & 0xFFFF_FFFFL);
        synchronized (carvedGroundHeights) {
            int known = carvedGroundHeights.getAndMoveToLast(key);
            if (known != Integer.MIN_VALUE) return known;
        }
        int height = computeCarvedGroundSurfaceHeight(blockX, blockZ);
        synchronized (carvedGroundHeights) {
            carvedGroundHeights.putAndMoveToLast(key, height);
            if (carvedGroundHeights.size() > CARVED_GROUND_HEIGHT_CACHE_SIZE) carvedGroundHeights.removeFirstInt();
        }
        return height;
    }

    /**
     * Column heights are pure functions of seed and position; neighbouring chunks query the same columns repeatedly.
     */
    private static final int CARVED_GROUND_HEIGHT_CACHE_SIZE = 1 << 16;
    private final it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap carvedGroundHeights = newCarvedGroundHeightCache();
    private final it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap authoredGroundHeights = newCarvedGroundHeightCache();

    /**
     * Highest block of Origen's authored density alone: no carvers, ocean floors or sky layer. A tenth of
     * {@link #sampleCarvedGroundSurfaceHeight}'s cost and the same height on 97% of land columns, for planning
     * structures over wide areas (the shifted512 path's {@code ShiftedGroundHeights}, in 256 physical Y).
     */
    public int sampleAuthoredGroundHeight(int blockX, int blockZ) {
        long key = (long) blockX << 32 ^ (blockZ & 0xFFFF_FFFFL);
        synchronized (authoredGroundHeights) {
            int known = authoredGroundHeights.getAndMoveToLast(key);
            if (known != Integer.MIN_VALUE) return known;
        }
        int y = WORLD_HEIGHT - 1;
        while (y > 0 && exactOrigenField.sampleScaledDensity(blockX, y, blockZ) <= 0.0D) y -= 4;
        while (y > 0 && y < WORLD_HEIGHT - 1 && exactOrigenField.sampleScaledDensity(blockX, y + 1, blockZ) > 0.0D) y++;
        int height = Math.max(1, y);
        synchronized (authoredGroundHeights) {
            authoredGroundHeights.putAndMoveToLast(key, height);
            if (authoredGroundHeights.size() > CARVED_GROUND_HEIGHT_CACHE_SIZE) authoredGroundHeights.removeFirstInt();
        }
        return height;
    }

    private static it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap newCarvedGroundHeightCache() {
        var cache = new it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap(CARVED_GROUND_HEIGHT_CACHE_SIZE + 1);
        cache.defaultReturnValue(Integer.MIN_VALUE);
        return cache;
    }

    private int computeCarvedGroundSurfaceHeight(int blockX, int blockZ) {
        int upperBound = densitySampler.sampleSurfaceHeight(blockX, blockZ, true);
        for (int blockY = upperBound; blockY > 0; blockY--) {
            if (exactOrigenField.sampleScaledDensity(blockX, blockY, blockZ) > 0.0D && exactOrigenField
                .sampleUpstreamCarverInterpolated(blockX, OrigenDensityField.toUpstreamY(blockY), blockZ) <= 0.0D) {
                int floor = oceanDepth.loweredFloor(blockX, blockZ, blockY);
                if (gardenCoastTerrain != null) {
                    int nominal = gardenCoastTerrain.floor(blockX, blockZ, floor);
                    if (nominal < floor) floor = gardenSupportedFloor(blockX, blockZ, floor, nominal);
                }
                return bopMarineTerrain == null ? floor : bopMarineTerrain.floor(blockX, blockZ, floor);
            }
        }
        return 1;
    }

    /** Mirrors HostedGardenCoastTerrain's retained three-block substrate check without generating a chunk. */
    private int gardenSupportedFloor(int x, int z, int old, int nominal) {
        int run = 0;
        for (int y = nominal; y > 0; y--) {
            boolean solid = exactOrigenField.sampleScaledDensity(x, y, z) > 0.0D
                && exactOrigenField.sampleUpstreamCarverInterpolated(x, OrigenDensityField.toUpstreamY(y), z) <= 0.0D;
            if (solid) {
                if (++run == 3) return y + 2;
            } else run = 0;
        }
        return old;
    }

    public double sampleRiverStrength(int blockX, int blockZ) {
        return columnSampler.sample(blockX, blockZ)
            .getRiverStrength();
    }

    public double sampleDensity(int blockX, int blockY, int blockZ) {
        return densitySampler.sampleDensity(blockX, blockY, blockZ);
    }

    /** The floating layer in one column: {@code bottom << 16 | top}, or -1 where it has none. */
    public int floatingSpan(int x, int z) {
        return densitySampler.floatingSpans(x >> 4, z >> 4)[(x & 15) * CHUNK_WIDTH + (z & 15)];
    }

    /** Origen's authored Y for a physical Y of this compressed layout. */
    public static int authoredY(int physicalY) {
        return (int) Math.floor(OrigenDensityField.toUpstreamY(physicalY));
    }

    /** A cheap estimate of the floating layer's top in a column, or -1 (see FloatingIslandDensitySampler). */
    public int floatingTopEstimate(int x, int z) {
        FloatingIslandDensitySampler islands = verticalBiomeSampler.getFloatingIslandSampler();
        return islands == null ? -1 : islands.estimateTop(x, z);
    }

    /** The admitted island cluster of one cluster region, or null (see FloatingIslandDensitySampler). */
    public FloatingIslandDensitySampler.Cluster floatingCluster(int regionX, int regionZ) {
        FloatingIslandDensitySampler islands = verticalBiomeSampler.getFloatingIslandSampler();
        return islands == null ? null : islands.cluster(regionX, regionZ);
    }

    /** Physical provenance, independent of surface biome names and arbitrary altitude thresholds. */
    public FloatingIslandArchetype floatingArchetypeAt(int x, int y, int z) {
        FloatingIslandDensitySampler islands = verticalBiomeSampler.getFloatingIslandSampler();
        if (islands == null || y < FloatingIslandDensitySampler.MIN_Y
            || y > FloatingIslandDensitySampler.MAX_Y
            || islands.sampleDensity(x, y, z) <= 0
            || exactOrigenField.sampleScaledDensity(x, y, z) > 0) return null;
        return islands.sampleArchetype(x, y, z);
    }

    /** The habitat of the island at a cell (SkyIslandHabitats), or null. */
    public String floatingHabitatAt(int x, int y, int z) {
        return floatingArchetypeAt(x, y, z) == null ? null : skyHabitats.at(x, z);
    }

    /** The world's island habitat rules, and the mainland habitats islands follow when the rules say so. */
    public void setSkyIslandRules(IslandBiomeRules rules, RegionalHabitatSampler mainland) {
        FloatingIslandDensitySampler islands = verticalBiomeSampler.getFloatingIslandSampler();
        if (islands == null) return;
        SkyIslandHabitats habitats = new SkyIslandHabitats(islands, rules, nativeBopHabitats);
        habitats.setGround(mainland);
        skyHabitats = habitats;
    }

    /** The islets centred in a chunk that kept their island (see FloatingIslandDensitySampler#islets). */
    public java.util.List<int[]> floatingIslets(int chunkX, int chunkZ) {
        FloatingIslandDensitySampler islands = verticalBiomeSampler.getFloatingIslandSampler();
        if (islands == null) return java.util.List.of();
        java.util.List<int[]> islets = islands.islets(chunkX, chunkZ);
        islets.removeIf(islet -> floatingSpan(islet[0], islet[1]) < 0);
        return islets;
    }

    public byte[] createMaterialArray(int chunkX, int chunkZ) {
        return createMaterialArray(chunkX, chunkZ, verticalBiomeSampler.createVolume(chunkX, chunkZ));
    }

    /** Generates one chunk using the exact immutable volume that will be attached to that chunk. */
    public byte[] createMaterialArray(int chunkX, int chunkZ, BiomeVolume biomeVolume) {
        // Neighbouring chunks' cave halos usually computed this base already.
        HostedCaveExpansion.Base base = caveExpansion == null ? null : caveExpansion.peekBase(chunkX, chunkZ);
        if (base == null) base = createUnexpanded(chunkX, chunkZ, biomeVolume);
        return createPreForlorn(chunkX, chunkZ, biomeVolume, base);
    }

    private CaveThemes caveThemes;

    /** Cave rock skins are terrain: laid with the cave expansion, on the workers (CaveThemes). */
    public void setCaveThemes(CaveThemes themes) {
        caveThemes = themes;
    }

    private byte[] createPreForlorn(int chunkX, int chunkZ, BiomeVolume biomeVolume, HostedCaveExpansion.Base base) {
        byte[] result = caveExpansion == null ? base.materials() : caveExpansion.apply(chunkX, chunkZ, base);
        if (caveThemes != null) {
            if (result == base.materials()) result = result.clone();
            byte[] origins = base.origins();
            caveThemes.skin(
                chunkX,
                chunkZ,
                result,
                biomeVolume,
                i -> (origins[i] & TerrainDensitySampler.FLOATING_ORIGIN) != 0,
                null,
                0,
                0);
        }
        if (nativeSandRegions != null && !strictOrigenBaseline) {
            Boolean[] columns = new Boolean[256];
            HostedSandBase.apply(result, WORLD_HEIGHT, i -> {
                int column = i / WORLD_HEIGHT, y = i % WORLD_HEIGHT, x = column / 16, z = column % 16;
                // Mainland surface habitat only: no cave replacement, sky relabeling or old-save enrollment.
                if ((base.origins()[i] & TerrainDensitySampler.FLOATING_ORIGIN) != 0
                    || !BiomeVolume.SURFACE_KEY.equals(biomeVolume.getBiomeKey(x >> 2, y >> 2, z >> 2))) return false;
                if (columns[column] == null) columns[column] = HostedSandBase
                    .ownsHabitat(nativeSandRegions.sample(chunkX * 16 + x, chunkZ * 16 + z));
                return columns[column];
            });
        }
        return result;
    }

    public BiomeVolume projectAbyssal(BiomeVolume original, int chunkX, int chunkZ) {
        return trenches == null ? original : trenches.project(original, chunkX, chunkZ);
    }

    /** Saved source-biome gate used by placed features: a trench cell claims its 4x4x4 quart. */
    public boolean isAbyssalHabitat(int x, int y, int z) {
        return trenches != null && trenches.ownsQuart(x, y, z);
    }

    public HostedAbyssalRuntime abyssalRuntime() {
        return trenches == null ? null : trenches.runtime();
    }

    /** Just above the pre-trench sea floor: the source floor features descend from here through trench water. */
    public int abyssalSourceFloor(int x, int z) {
        return trenches == null ? 0 : trenches.populationFloor(x, z);
    }

    /** Pre-cave terrain, the chasm included. */
    private HostedCaveExpansion.Base createUnexpanded(int chunkX, int chunkZ, BiomeVolume biomeVolume) {
        HostedCaveExpansion.Base base = createUntrenched(chunkX, chunkZ, biomeVolume);
        if (trenches != null) trenches.apply(chunkX, chunkZ, base.materials(), null);
        return base;
    }

    private HostedCaveExpansion.Base createUntrenched(int chunkX, int chunkZ, BiomeVolume biomeVolume) {
        if (biomeVolume == null) {
            throw new IllegalArgumentException("Biome volume cannot be null");
        }
        byte[] materials = new byte[BLOCK_COUNT];
        if (strictOrigenBaseline) {
            // The authored surface pass owns the exact density and carver state needed to project this chunk.
            // Exact-baseline worlds therefore need neither a duplicate physical raster nor any RU/floating state.
            exteriorSurfaceRules.applyStrictOrigen(materials, chunkX, chunkZ);
            return new HostedCaveExpansion.Base(materials, null);
        }

        byte[] solidOrigins = new byte[BLOCK_COUNT];
        TerrainColumn[][] neighborhood = createColumnNeighborhood(chunkX, chunkZ);
        densitySampler.fillBaseTerrain(materials, solidOrigins, chunkX, chunkZ);
        // Composite modes retain the eager pass because their later custom carvers intentionally observe Origen's
        // caves while mutating the shared physical array. Strict mode fuses these decisions into its surface pass.
        applyExactOrigenCarving(materials, solidOrigins, chunkX, chunkZ);
        exteriorSurfaceRules.apply(materials, solidOrigins, neighborhood, chunkX, chunkZ, biomeVolume);
        oceanDepth.apply(materials, chunkX, chunkZ);
        if (marshTerrain != null) marshTerrain.apply(materials, solidOrigins, chunkX, chunkZ);
        if (gardenCoastTerrain != null) gardenCoastTerrain.apply(materials, solidOrigins, chunkX, chunkZ);
        if (bopMarineTerrain != null) bopMarineTerrain.apply(materials, solidOrigins, chunkX, chunkZ);
        return new HostedCaveExpansion.Base(materials, solidOrigins);
    }

    /** Returns the immutable authored-height surface data produced alongside the most recent terrain result. */
    public byte[] cachedAuthoredMaterialArray(int chunkX, int chunkZ) {
        return exteriorSurfaceRules.cachedAuthoredChunk(chunkX, chunkZ);
    }

    /** Returns only an already-computed cave projection; this accessor never builds a mask or reads a World. */
    public BitSet completedCaveMaskSnapshot(int chunkX, int chunkZ) {
        return caveExpansion == null ? null : caveExpansion.completedMaskSnapshot(chunkX, chunkZ);
    }

    /** Re-pins an exact worker mask in the existing bounded cave cache. */
    public void rememberCompletedCaveMask(int chunkX, int chunkZ, BitSet mask) {
        if (caveExpansion != null) caveExpansion.rememberCompletedMask(chunkX, chunkZ, mask);
    }

    /** Cache-only diagnostics for bounded stress comparisons; absent when cave expansion is disabled. */
    public String caveCachePerformanceReport() {
        return caveExpansion == null ? null : caveExpansion.performanceReport();
    }

    public boolean isExpansionCavity(int x, int y, int z) {
        return caveExpansion != null && y >= 0
            && y < 256
            && caveExpansion.mask(x >> 4, z >> 4)
                .get(index(x & 15, y, z & 15));
    }

    boolean surfaceBoulderIntersectsExpansion(int x, int y, int z) {
        if (caveExpansion == null) return false;
        // Authored stone/andesite wall boulders: radius<2.5, warp<=1, squish>=.5.
        // Six physical Y blocks conservatively include every squeezed source sibling.
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++)
            for (int dy = -6; dy <= 6; dy++) if (isExpansionCavity(x + dx, y + dy, z + dz)) return true;
        return false;
    }

    byte[] oceanAuthoredView(byte[] authored, int blockX, int blockZ, java.util.function.Supplier<byte[]> physical) {
        return oceanAuthoredView(authored, blockX, blockZ, physical, null);
    }

    byte[] oceanAuthoredView(byte[] authored, int blockX, int blockZ, java.util.function.Supplier<byte[]> physical,
        BitSet completedCaveMask) {
        byte[] result = oceanDepth.authoredView(authored, blockX, blockZ, physical);
        if (marshTerrain != null) result = marshTerrain.authoredView(
            result,
            blockX,
            blockZ,
            physical,
            () -> createUnexpanded(
                blockX >> 4,
                blockZ >> 4,
                verticalBiomeSampler.createVolume(blockX >> 4, blockZ >> 4)));
        if (gardenCoastTerrain != null) result = gardenCoastTerrain.authoredView(
            result,
            blockX,
            blockZ,
            physical,
            () -> createUnexpanded(
                blockX >> 4,
                blockZ >> 4,
                verticalBiomeSampler.createVolume(blockX >> 4, blockZ >> 4)));
        if (bopMarineTerrain != null) result = bopMarineTerrain.authoredView(
            result,
            blockX,
            blockZ,
            physical,
            () -> createUnexpanded(
                blockX >> 4,
                blockZ >> 4,
                verticalBiomeSampler.createVolume(blockX >> 4, blockZ >> 4)));
        return caveExpansion == null ? result : caveExpansion.authoredView(result, blockX, blockZ, completedCaveMask);
    }

    private void applyExactOrigenCarving(byte[] materials, byte[] solidOrigins, int chunkX, int chunkZ) {
        int worldStartX = chunkX << 4;
        int worldStartZ = chunkZ << 4;
        for (int localX = 0; localX < CHUNK_WIDTH; localX++) {
            int worldX = worldStartX + localX;
            for (int localZ = 0; localZ < CHUNK_WIDTH; localZ++) {
                int worldZ = worldStartZ + localZ;
                for (int y = 0; y < WORLD_HEIGHT; y++) {
                    int index = index(localX, y, localZ);
                    if ((solidOrigins[index] & TerrainDensitySampler.GROUND_ORIGIN) != 0 && exactOrigenField
                        .sampleUpstreamCarverInterpolated(worldX, OrigenDensityField.toUpstreamY(y), worldZ) > 0.0D) {
                        materials[index] = AIR;
                    }
                }
            }
        }
    }

    public static int index(int localX, int y, int localZ) {
        return localX << 12 | localZ << 8 | y;
    }

    private TerrainColumn[][] createColumnNeighborhood(int chunkX, int chunkZ) {
        int worldStartX = chunkX << 4;
        int worldStartZ = chunkZ << 4;
        TerrainColumn[][] neighborhood = new TerrainColumn[20][20];
        for (int offsetX = -2; offsetX <= 17; offsetX++) {
            for (int offsetZ = -2; offsetZ <= 17; offsetZ++) {
                neighborhood[offsetX + 2][offsetZ + 2] = columnSampler
                    .sample(worldStartX + offsetX, worldStartZ + offsetZ);
            }
        }
        return neighborhood;
    }

}
