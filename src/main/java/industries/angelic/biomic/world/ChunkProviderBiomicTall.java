package industries.angelic.biomic.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;

import industries.angelic.biomic.api.BiomicBiomeApi;
import industries.angelic.biomic.api.ChunkVerticalLayout;
import industries.angelic.biomic.biome.VerticalBiomeSampler;
import industries.angelic.biomic.compat.endlessids.EndlessIdsBiomeStorage;
import industries.angelic.biomic.config.PerformanceConfig;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.storage.BiomeVolumeManager;
import industries.angelic.biomic.world.structure.BiomicMineshaftGenerator;
import industries.angelic.biomic.world.structure.BiomicStrongholdGenerator;
import industries.angelic.biomic.world.structure.BiomicVillageGenerator;
import industries.angelic.biomic.world.structure.StructureTerrain;
import industries.angelic.biomic.world.terrain.AbyssalTrenches;
import industries.angelic.biomic.world.terrain.AuthoredVerticalProjection;
import industries.angelic.biomic.world.terrain.AuthoredVerticalProjections;
import industries.angelic.biomic.world.terrain.FloatingIslandArchetype;
import industries.angelic.biomic.world.terrain.HostedCaveRuntime;
import industries.angelic.biomic.world.terrain.HostedIslandBiomes;
import industries.angelic.biomic.world.terrain.HostedIslandRuntime;
import industries.angelic.biomic.world.terrain.HostedTerrainAccess;
import industries.angelic.biomic.world.terrain.IntrinsicHostedTerrainAccess;
import industries.angelic.biomic.world.terrain.MacroTerrainSampler;
import industries.angelic.biomic.world.terrain.OrigenFeatureRuntime;
import industries.angelic.biomic.world.terrain.OrigenProjectedTerrainSampler;
import industries.angelic.biomic.world.terrain.ProjectedHostedTerrainAccess;
import industries.angelic.biomic.world.terrain.ShiftedCaveExpansion;
import industries.angelic.biomic.world.terrain.ShiftedNativeMarshTerrainAccess;
import industries.angelic.biomic.world.terrain.ShiftedSkyIslands;

/**
 * Hohen Angst overworld: Origen at its uncompressed authored height (shifted512: authored -64..320 sits at physical
 * 64..448, sea level 191) with RU mainland habitats on top. Pure terrain is computed ahead on worker threads; the
 * server thread only resolves blocks, surfaces habitats, builds the chunk and populates features.
 */
public final class ChunkProviderBiomicTall
    implements IChunkProvider, BenchmarkedChunkProvider, JournaledPopulation, TerrainPrefetching, BiomeLocating {

    private static final ChunkVerticalLayout LAYOUT = ChunkVerticalLayout.EXTENDED_512;

    private final World world;
    private final WorldgenContext context;
    private final OrigenProjectedTerrainSampler terrain;
    /** Physical terrain before habitats: projected Origen, plus the salt-marsh reshaping when it is enabled. */
    private final HostedTerrainAccess foundation;
    private final VerticalBiomeSampler biomes;
    private final OrigenFeatureRuntime features;
    /**
     * Temples, witch huts, igloos, ruined portals, dungeons, wells and island geodes (order-independent population
     * only).
     */
    private final industries.angelic.biomic.world.structure.BiomicScatteredFeatures scattered;
    private final HostedIslandRuntime mainland;
    private final ShiftedCaveExpansion caves;
    private final ShiftedSkyIslands skyIslands;
    private final AbyssalTrenches trenches;
    private final HostedIslandRuntime skyHabitats;
    private final industries.angelic.biomic.world.terrain.FloraLedger flora;
    private HostedTerrainAccess mainlandTerrain;
    private final HostedCaveRuntime caveHabitats;
    private final industries.angelic.biomic.world.terrain.CaveDecorPass caveDecor;
    private final industries.angelic.biomic.world.terrain.CaveThemes caveThemes;
    private final SlowSteps slowTerrain = new SlowSteps("terrain");
    private final SlowSteps slowPopulation = new SlowSteps("populate");
    /** The one physical composition behind both the workers' payloads and population's recomputed chunks. */
    private final FoundationView composition;
    private final boolean mapFeaturesEnabled;
    private final BiomicMineshaftGenerator mineshafts;
    private final BiomicVillageGenerator villages;
    private final BiomicStrongholdGenerator strongholds;
    private final TerrainPrecomputeScheduler terrainPrecompute;
    private final WorldgenPerformanceTracker performance = new WorldgenPerformanceTracker();

    public ChunkProviderBiomicTall(World world, WorldgenContext context) {
        if (world == null || context == null) throw new IllegalArgumentException("World and context are required");
        if (LiveChunkLayout.of(world)
            .height() != LAYOUT.height()) {
            throw new IllegalStateException("Tall Biomic terrain requires 512-block chunk storage (Hohen Angst)");
        }
        if (!context.getSettings()
            .getAuthoredHeightPolicy(LAYOUT.height(), BiomeVolumeManager.SUPPORTS_SCHEMA_TWO)
            .shifted512()) {
            throw new IllegalStateException("This world was created with compressed terrain; it cannot become tall");
        }
        this.world = world;
        this.context = context;
        terrain = new OrigenProjectedTerrainSampler(context, AuthoredVerticalProjections.shifted512(), LAYOUT);
        var pristine = new ProjectedHostedTerrainAccess(terrain);
        biomes = new VerticalBiomeSampler(context, AuthoredVerticalProjections.shifted512());
        HostedTerrainAccess hosted;
        ShiftedNativeMarshTerrainAccess marsh = null;
        if (context.getNativeHabitats()
            .shifted512()) {
            // BOP habitats and the salt marsh: native ecology on the (optionally marsh-reshaped) foundation.
            marsh = context.getNativeHabitats()
                .fullMarsh() ? new ShiftedNativeMarshTerrainAccess(context, pristine) : null;
        }
        foundation = marsh == null ? pristine : marsh;
        skyIslands = SkyIslandsWorldData.enabled(world)
            ? new ShiftedSkyIslands(context, SkyIslandsWorldData.biomes(world))
            : null;
        // Vanilla structures: underground pieces laid out in legacy Y and raised by 128 onto the shifted terrain.
        mapFeaturesEnabled = world.getWorldInfo()
            .isMapFeaturesEnabled();
        var structureTerrain = StructureTerrain.shifted512(context, biomes, skyIslands);
        MacroTerrainSampler macro = world.getWorldChunkManager() instanceof WorldChunkManagerBiomic manager
            ? manager.getMacroTerrainSampler()
            : new MacroTerrainSampler(context);
        mineshafts = new BiomicMineshaftGenerator(context, structureTerrain);
        villages = new BiomicVillageGenerator(context, macro, structureTerrain);
        strongholds = new BiomicStrongholdGenerator(structureTerrain);
        // The Abyssal Chasm is part of the terrain caves are planned around: they keep clear of its water.
        trenches = context.getAlexCaves()
            .abyssalShifted512() ? AbyssalTrenches.shifted512(context, foundation::createMaterialArray) : null;
        caves = context.getCaveExpansion()
            .shifted512()
                ? new ShiftedCaveExpansion(
                    context.getSettings()
                        .getWorldSeed(),
                    context.getCaveExpansion(),
                    this::trenchedFoundation,
                    terrain::groundMask)
                : null;
        caveThemes = context.getCaveExpansion()
            .nativeEcology() ? new industries.angelic.biomic.world.terrain.CaveThemes(context, true, LAYOUT.height())
                : null;
        // The pristine view caches copies for repeated hosted reads; a fresh chunk reads the sampler directly.
        ProjectedHostedTerrainAccess.ChunkSource base = foundation instanceof ProjectedHostedTerrainAccess
            ? terrain::createMaterialArray
            : foundation::createMaterialArray;
        composition = new FoundationView(
            world,
            foundation,
            base,
            trenches,
            caves,
            skyIslands,
            shapedVillages(),
            caveThemes,
            biomes);
        if (context.getNativeHabitats()
            .shifted512()) {
            features = OrigenFeatureRuntime.projectedNativeIntrinsicHosted(context, terrain, composition, marsh);
            hosted = composition;
        } else if (caves != null || skyIslands != null
            || trenches != null
            || shapedVillages() != null
            || caveThemes != null) {
                features = OrigenFeatureRuntime.projectedIntrinsicHosted(context, terrain, composition);
                hosted = composition;
            } else {
                features = OrigenFeatureRuntime.projectedHosted(context, terrain, pristine);
                hosted = pristine;
            }
        mainlandTerrain = hosted;
        mainland = new HostedIslandRuntime(context, hosted, true, this::peekHostedRawTerrain);
        scattered = mapFeaturesEnabled && OrigenFeatureRuntime.ORDER_INDEPENDENT
            ? new industries.angelic.biomic.world.structure.BiomicScatteredFeatures(
                context,
                structureTerrain,
                features.providedTerrain,
                new industries.angelic.biomic.world.structure.BiomicScatteredFeatures.Habitats() {

                    @Override
                    public String ground(int x, int z) {
                        return mainland.surfaceHabitat(x, z);
                    }

                    @Override
                    public String island(int x, int y, int z) {
                        return skyIslands == null ? null : skyIslands.habitatAt(x, y, z);
                    }
                })
            : null;
        flora = new industries.angelic.biomic.world.terrain.FloraLedger(
            context,
            mainland,
            AuthoredVerticalProjections.shifted512()
                .physicalSeaBoundary(),
            LAYOUT.height());
        // Sky islands carry their own habitats (RU island habitats or a native BOP one per cluster).
        skyHabitats = skyIslands != null ? new HostedIslandRuntime(context, hosted, false, this::peekHostedRawTerrain)
            : null;
        if (skyIslands != null) {
            skyIslands.setGround(mainland.groundHabitats());
            skyHabitats.setIslands(skyIslands);
            flora.setIslands(skyHabitats);
        }
        HostedIslandRuntime.DecorationGuard guard = new HostedIslandRuntime.DecorationGuard() {

            @Override
            public boolean blocks(String feature, boolean bulky, int x, int y, int z) {
                return !HostedIslandRuntime.snowCover(feature)
                    && (shapedVillages() != null && villages.guardsDecoration(world, feature, bulky, x, y, z)
                        || scattered != null && scattered.protects(x, y, z, bulky ? 8 : 2));
            }

            @Override
            public boolean near(int x, int y, int z, int reach) {
                return shapedVillages() != null && villages.nearDecoration(world, x, y, z, reach)
                    || scattered != null && scattered.protects(x, y, z, reach);
            }
        };
        if (scattered != null) scattered.setVillages(
            (x, y, z, reach) -> shapedVillages() != null && villages.nearDecoration(world, x, y, z, reach));
        if (shapedVillages() != null || scattered != null) {
            mainland.setVillageGuard(guard);
            if (skyHabitats != null) skyHabitats.setVillageGuard(guard);
            flora.setGuard(guard);
        }
        // RU cave biomes decorate Origen's and the expansion's cavities; they displace Origen's own cave ecology.
        caveHabitats = new HostedCaveRuntime(context, hosted, biomes);
        features.setHostedCaveOwner(caveHabitats::owns);
        // Origen's own sea-floor and cave dressing gives way to the chasm's ecology, as in the 256 blend.
        if (trenches != null) features.setHostedEnvironmentOwner(trenches::ownsQuart);
        caveDecor = caveThemes == null ? null
            : new industries.angelic.biomic.world.terrain.CaveDecorPass(mainland, caveThemes, LAYOUT.height(), true);
        features.setCaveLichenColonies(caveDecor != null);
        features.setLocatedHostedPlantSupport(mainland::groundCoverSupportAt);
        features.setHostedSurfaceOwner(mainland::ownsSurfaceDecorationAt);
        if (context.getNativeHabitats()
            .bopHabitats()
            .stream()
            .anyMatch(industries.angelic.biomic.compat.biome.BopNativeBiomes::reservesOreSubstrate)) {
            features.setHostedOreSubstrateOwner(mainland::ownsNativeShoreSoilAt);
        }
        // Vanilla structures: underground pieces laid out in legacy Y and raised by 128 onto the shifted terrain.
        features.setSurfaceProtection(new OrigenFeatureRuntime.SurfaceProtection() {

            @Override
            public boolean contains(int x, int y, int z, boolean bulky) {
                return guard.blocks("origen", bulky, x, y, z);
            }

            @Override
            public boolean near(int x, int y, int z, int reach) {
                return guard.near(x, y, z, reach);
            }
        });
        terrainPrecompute = new TerrainPrecomputeScheduler(new TerrainPrecomputeScheduler.Source() {

            @Override
            public TerrainPrecomputeScheduler.TerrainPayload compute(int chunkX, int chunkZ) {
                // Prefetched chunks too: their structure check asks for the sky plans around them when provided.
                warmScatteredPlans(chunkX, chunkZ);
                return computePureTerrain(chunkX, chunkZ);
            }

            @Override
            public void warm(int chunkX, int chunkZ, java.util.concurrent.Executor workers) {
                if (shapedVillages() != null) villages.warm(world, chunkX, chunkZ, workers);
                if (scattered != null) scattered.warm(chunkX, chunkZ, workers);
            }
        }, PerformanceConfig.terrainWorkerThreads, PerformanceConfig.terrainPrefetchTasks);
        if (caves != null) caves.setHaloWorkers(terrainPrecompute.workers());
        if (OrigenFeatureRuntime.ORDER_INDEPENDENT) features.enableJournaledPopulation(
            (cx, cz, m, b, meta) -> paintSkins(cx, cz, m, b, meta, biomes.createVolume(cx, cz)),
            terrainPrecompute::peekRawMaterials);
    }

    /** Worker-safe: Origen terrain, its authored companion and the unprojected biome volume. */
    private TerrainPrecomputeScheduler.TerrainPayload computePureTerrain(int chunkX, int chunkZ) {
        byte[] authored = terrain.copyAuthoredMaterialArray(chunkX, chunkZ);
        SlowSteps.lap("authored");
        BiomeVolume volume = biomes.createVolume(chunkX, chunkZ);
        SlowSteps.lap("biomes");
        java.util.BitSet[] caveMask = new java.util.BitSet[1];
        byte[] materials = composition.compose(chunkX, chunkZ, authored, volume, caveMask, true);
        // The hosted habitats' sites, from the materials the server will paint (cave surfacing only reads them).
        mainland.prepare(chunkX, chunkZ, materials);
        return new TerrainPrecomputeScheduler.TerrainPayload(volume, materials, authored, caveMask[0]);
    }

    private byte[] trenchedFoundation(int chunkX, int chunkZ) {
        byte[] materials = foundation.createMaterialArray(chunkX, chunkZ);
        if (trenches != null) trenches.apply(chunkX, chunkZ, materials, null);
        return materials;
    }

    /** Villages whose ground is shaped before they are built, or null when structures are off. */
    private BiomicVillageGenerator shapedVillages() {
        return mapFeaturesEnabled ? villages : null;
    }

    /** Re-publish a populated chunk's BOP tree sources when it is loaded back from disk. */
    public void nativeTreeSourceLoaded(Chunk chunk) {
        if (context.getNativeHabitats()
            .bop() && chunk.isTerrainPopulated) {
            features.nativeTreeSourceComplete(world, chunk.xPosition, chunk.zPosition);
        }
    }

    /**
     * The foundation viewed as a hosted environment whose intrinsic cells are the Abyssal Chasm, expansion caves and
     * sky islands. Caves are carved terrain, not a foreign environment: Origen's cave ecology may still decorate them.
     */
    private static final class FoundationView implements IntrinsicHostedTerrainAccess {

        private final World world;
        private final HostedTerrainAccess foundation;
        private final ProjectedHostedTerrainAccess.ChunkSource base;
        private final AbyssalTrenches trenches;
        private final ShiftedCaveExpansion caves;
        private final ShiftedSkyIslands islands;
        private final BiomicVillageGenerator villages;
        private final industries.angelic.biomic.world.terrain.CaveThemes themes;
        private final VerticalBiomeSampler biomes;

        FoundationView(World world, HostedTerrainAccess foundation, ProjectedHostedTerrainAccess.ChunkSource base,
            AbyssalTrenches trenches, ShiftedCaveExpansion caves, ShiftedSkyIslands islands,
            BiomicVillageGenerator villages, industries.angelic.biomic.world.terrain.CaveThemes themes,
            VerticalBiomeSampler biomes) {
            this.world = world;
            this.foundation = foundation;
            this.base = base;
            this.trenches = trenches;
            this.caves = caves;
            this.islands = islands;
            this.villages = villages;
            this.themes = themes;
            this.biomes = biomes;
        }

        @Override
        public ChunkVerticalLayout layout() {
            return foundation.layout();
        }

        @Override
        public int minimumEcologyY() {
            return foundation.minimumEcologyY();
        }

        @Override
        public AuthoredVerticalProjection projection() {
            return foundation.projection();
        }

        @Override
        public byte[] createMaterialArray(int chunkX, int chunkZ) {
            return compose(
                chunkX,
                chunkZ,
                null,
                themes == null ? null : biomes.createVolume(chunkX, chunkZ),
                null,
                false);
        }

        /**
         * The chunk's physical terrain. The workers' payload (with its authored companion and cave mask) and
         * population's recomputation of an evicted chunk both come from here: when the two derivations were separate,
         * the recomputed one lacked the cave skins and an evicted chunk's moss floors were decorated differently.
         */
        byte[] compose(int chunkX, int chunkZ, byte[] authored, BiomeVolume volume, java.util.BitSet[] caveMask,
            boolean laps) {
            byte[] materials = base.physicalMaterials(chunkX, chunkZ);
            if (laps) SlowSteps.lap("terrain");
            if (trenches != null) trenches.apply(chunkX, chunkZ, materials, authored);
            if (caves != null) {
                if (authored != null) caveMask[0] = caves.carve(chunkX, chunkZ, materials, authored);
                else caves.carvePhysical(chunkX, chunkZ, materials);
            }
            if (laps) SlowSteps.lap("caves");
            // Cave rock skins (moss, mycelium, ice, dripstone, ...) belong to the terrain.
            if (themes != null) themes.skin(chunkX, chunkZ, materials, volume, i -> false, authored, 384, 64);
            if (islands != null) islands.apply(chunkX, chunkZ, materials);
            if (laps) SlowSteps.lap("skins and islands");
            if (villages != null) villages.shapeTerrain(world, chunkX, chunkZ, materials);
            if (laps) SlowSteps.lap("villages");
            return materials;
        }

        @Override
        public int sampleCarvedGroundSurfaceHeight(int x, int z) {
            return foundation.sampleCarvedGroundSurfaceHeight(x, z);
        }

        @Override
        public FloatingIslandArchetype floatingArchetypeAt(int x, int y, int z) {
            return islands == null ? null : islands.archetypeAt(x, y, z);
        }

        @Override
        public String floatingHabitatAt(int x, int y, int z) {
            return islands == null ? null : islands.habitatAt(x, y, z);
        }

        @Override
        public boolean ownsIntrinsicEnvironment(int x, int y, int z) {
            return caves != null && caves.isCavity(x, y, z) || islands != null && islands.owns(x, y, z)
                || trenches != null && trenches.owns(x, y, z)
                || villages != null && villages.ownsTerrain(world, x, y, z);
        }

        /** Carved caves are terrain; sky islands are a foreign layer Origen never sees (as in the 256 blend). */
        @Override
        public boolean authoredVisible(int x, int y, int z) {
            return islands == null || !islands.owns(x, y, z);
        }

        /**
         * Only the chasm excludes hosted ecology: it carries Alex's Caves' own. The island runtime skins and
         * decorates the islands itself.
         */
        @Override
        public boolean blocksHostedEcology(int x, int y, int z) {
            return trenches != null && trenches.owns(x, y, z);
        }
    }

    @Override
    public boolean chunkExists(int chunkX, int chunkZ) {
        return true;
    }

    @Override
    public WorldgenContext worldgenContext() {
        return context;
    }

    @Override
    public industries.angelic.biomic.storage.BiomeVolume rawBiomeVolume(int chunkX, int chunkZ) {
        return biomes.createVolume(chunkX, chunkZ);
    }

    @Override
    public ShiftedSkyIslands skyIslands() {
        return skyIslands;
    }

    private volatile industries.angelic.biomic.world.terrain.ShiftedGroundHeights groundHeights;

    @Override
    public int groundHeight(int x, int z) {
        var heights = groundHeights;
        if (heights == null)
            groundHeights = heights = new industries.angelic.biomic.world.terrain.ShiftedGroundHeights(context);
        return heights.ground(x, z);
    }

    /** Starts the sky plans around a chunk the workers are computing (BiomicScatteredFeatures#warm). */
    private void warmScatteredPlans(int chunkX, int chunkZ) {
        if (scattered != null && terrainPrecompute != null) scattered.warm(chunkX, chunkZ, terrainPrecompute.workers());
    }

    /**
     * Terrain a hosted runtime may borrow instead of composing a neighbour again: what the server retained, or a
     * payload the workers finished. Both are FoundationView#compose's output. Never waits or computes.
     */
    private byte[] peekHostedRawTerrain(int chunkX, int chunkZ) {
        byte[] retained = features.peekRawTerrain(chunkX, chunkZ);
        if (retained != null || terrainPrecompute == null) return retained;
        return terrainPrecompute.peekRawMaterials(chunkX, chunkZ);
    }

    @Override
    public Chunk provideChunk(int chunkX, int chunkZ) {
        long startedAt = System.nanoTime();
        SlowSteps.Laps laps = slowTerrain.open();
        var payload = terrainPrecompute.take(chunkX, chunkZ);
        laps.lap("take");
        long precomputedAt = System.nanoTime();
        byte[] materials = payload.getMaterials();
        if (caves != null) caves.remember(chunkX, chunkZ, payload.getCaveProjectionMask());
        features.rememberRawTerrain(chunkX, chunkZ, materials);
        // Not the payload's authored companion: composition dressed it (caves, trenches, skins), and features read a
        // neighbour's authored terrain as Origen's own copy, overlaid with the physical terrain (authoredView). Which
        // of the two a feature saw would depend on whether that neighbour had been provided yet.
        Block[] blocks = ChunkProviderBiomic.resolveBlocks(materials);
        byte[] metadata = ChunkProviderBiomic.resolveMetadata(materials);
        BiomeVolume volume = paintSkins(chunkX, chunkZ, materials, blocks, metadata, payload.getBiomeVolume());
        laps.lap("skins");
        features.rememberProvided(chunkX, chunkZ, materials, blocks, metadata);
        if (mapFeaturesEnabled) {
            mineshafts.prepare(this, world, chunkX, chunkZ, blocks);
            laps.lap("mineshaft starts");
            villages.prepare(this, world, chunkX, chunkZ, blocks);
            laps.lap("village starts");
            strongholds.prepare(this, world, chunkX, chunkZ, blocks);
            laps.lap("stronghold starts");
            if (scattered != null) scattered.prepare(this, world, chunkX, chunkZ, blocks);
            laps.lap("scattered starts");
        }
        long materialsResolvedAt = System.nanoTime();
        Chunk chunk = TallTerrainMaterializer.materialize(world, chunkX, chunkZ, blocks, metadata);
        long chunkConstructedAt = System.nanoTime();
        BiomeGenBase[] surfaceBiomes = world.getWorldChunkManager()
            .loadBlockGeneratorData(null, chunkX << 4, chunkZ << 4, 16, 16);
        int[] surfaceBiomeIds = new int[EndlessIdsBiomeStorage.ENTRY_COUNT];
        for (int index = 0; index < surfaceBiomeIds.length; index++)
            surfaceBiomeIds[index] = surfaceBiomes[index].biomeID;
        EndlessIdsBiomeStorage.setBiomeIds(chunk, surfaceBiomeIds);
        BiomeVolumeManager.setVolume(chunk, volume);
        long biomesStoredAt = System.nanoTime();
        chunk.generateSkylightMap();
        laps.lap("chunk");
        slowTerrain.close(laps, chunkX, chunkZ);
        long completedAt = System.nanoTime();
        performance.recordTerrain(
            precomputedAt - startedAt,
            materialsResolvedAt - precomputedAt,
            0,
            chunkConstructedAt - materialsResolvedAt,
            biomesStoredAt - chunkConstructedAt,
            completedAt - biomesStoredAt,
            completedAt - startedAt);
        return chunk;
    }

    /**
     * The surface skins every provided chunk carries (cave habitats, hosted soils, island skins) and the biome volume
     * they project. The one path for providing a chunk and for rebuilding it as provided (order-independent
     * population's baseline). Never mutates the materials.
     */
    private BiomeVolume paintSkins(int chunkX, int chunkZ, byte[] materials, Block[] blocks, byte[] metadata,
        BiomeVolume volume) {
        volume = caveHabitats.surface(chunkX, chunkZ, materials, blocks, metadata, volume);
        mainland.surface(chunkX, chunkZ, materials, blocks, metadata);
        volume = mainland.projectMainland(volume, chunkX, chunkZ);
        if (skyHabitats != null) {
            skyHabitats.surface(chunkX, chunkZ, materials, blocks, metadata);
            volume = HostedIslandBiomes.project(volume, materials, chunkX, chunkZ, mainlandTerrain);
        }
        if (trenches != null) volume = trenches.project(volume, chunkX, chunkZ);
        return volume;
    }

    @Override
    public industries.angelic.biomic.world.terrain.OrigenFeatureRuntime populationRuntime() {
        return features;
    }

    @Override
    public Chunk loadChunk(int chunkX, int chunkZ) {
        return provideChunk(chunkX, chunkZ);
    }

    @Override
    public void populate(IChunkProvider chunkProvider, int chunkX, int chunkZ) {
        long startedAt = System.nanoTime();
        Random random = new Random(context.seedForChunk(WorldgenPhase.POPULATION, chunkX, chunkZ));
        boolean previousFallInstantly = BlockFalling.fallInstantly;
        BlockFalling.fallInstantly = true;
        long featuresAt = startedAt;
        SlowSteps.Laps laps = slowPopulation.open();
        boolean journaled = features.journaledPopulation();
        try {
            MinecraftForge.EVENT_BUS
                .post(new PopulateChunkEvent.Pre(chunkProvider, world, random, chunkX, chunkZ, false));
            boolean villageGenerated = false;
            if (!journaled && mapFeaturesEnabled) {
                mineshafts.populate(world, context, chunkX, chunkZ);
                villageGenerated = villages.populate(world, context, chunkX, chunkZ);
                strongholds.populate(
                    world,
                    new Random(context.seedForChunk(WorldgenPhase.STRUCTURES, chunkX, chunkZ)),
                    chunkX,
                    chunkZ);
                laps.lap("structures");
            }
            // Order-independent population: every Biomic pass reads the chunk as provided and its own writes, and
            // writes into a journal applied at the end (OrigenFeatureRuntime#applyJournal).
            if (journaled) features.beginJournal(chunkX, chunkZ);
            if (trenches != null) trenches.runtime()
                .populate(
                    world,
                    chunkX,
                    chunkZ,
                    features,
                    context.getSettings()
                        .getWorldSeed(),
                    trenches::ownsQuart,
                    trenches::populationFloor);
            // Neighbours' authored terrain never comes from the workers' finished payloads: those carry the cave
            // expansion and trenches, Origen's own copy does not, and which one a feature saw depended on timing.
            // Raw payloads equal what the runtime would compute, so borrowing them only saves work.
            features.populate(world, chunkX, chunkZ, null, terrainPrecompute::peekRawMaterials, null);
            laps.lap("origen features");
            caveHabitats.populate(world, chunkX, chunkZ, features);
            if (caveDecor != null) caveDecor.populate(world, chunkX, chunkZ, features);
            laps.lap("caves");
            featuresAt = System.nanoTime();
            mainland.populate(world, chunkX, chunkZ, features);
            laps.lap("mainland");
            if (skyHabitats != null) skyHabitats.populate(world, chunkX, chunkZ, features);
            flora.populate(world, chunkX, chunkZ, features);
            laps.lap("sky and flora");
            if (villageGenerated) villages.sealBlacksmithLava(world, chunkX, chunkZ, false);
            if (journaled) {
                features.applyJournal(world);
                laps.lap("apply");
                // Structures last, in their own box, reading the chunk as provided: mineshafts carve through ores.
                if (mapFeaturesEnabled) industries.angelic.biomic.world.structure.StructurePopulationBox.populate(
                    world,
                    context,
                    chunkX,
                    chunkZ,
                    features.providedTerrain,
                    mineshafts,
                    villages,
                    strongholds,
                    scattered);
                laps.lap("structures");
            }
            MinecraftForge.EVENT_BUS
                .post(new PopulateChunkEvent.Post(chunkProvider, world, random, chunkX, chunkZ, false));
            laps.lap("post");
        } finally {
            if (journaled) {
                features.closeJournal();
                features.applyDeferredOverflow(world);
            }
            slowPopulation.close(laps, chunkX, chunkZ);
            BlockFalling.fallInstantly = previousFallInstantly;
            long completedAt = System.nanoTime();
            performance.recordPopulation(
                0,
                0,
                0,
                featuresAt - startedAt,
                completedAt - featuresAt,
                0,
                completedAt - startedAt);
        }
    }

    public List<String> getPerformanceReport() {
        List<String> report = new ArrayList<>(performance.report());
        report.addAll(slowTerrain.report());
        report.addAll(slowPopulation.report());
        report.add(terrainPrecompute.performanceReport());
        report.add(features.performanceReport());
        report.add(mainland.report());
        report.add(flora.report());
        if (caveDecor != null) report.add(caveDecor.report());
        if (caves != null) report.add(caves.performanceReport());
        if (trenches != null) report.add(
            trenches.runtime()
                .populationReport());
        return report;
    }

    public void resetPerformanceStats() {
        performance.reset();
        slowTerrain.reset();
        slowPopulation.reset();
        terrainPrecompute.resetPerformanceStats();
        features.resetPerformanceStats();
        mainland.resetPerformanceStats();
    }

    public void shutdownTerrainPrecompute() {
        terrainPrecompute.shutdown();
    }

    @Override
    public boolean saveChunks(boolean saveAll, IProgressUpdate progress) {
        return true;
    }

    @Override
    public boolean unloadQueuedChunks() {
        return false;
    }

    @Override
    public boolean canSave() {
        return true;
    }

    @Override
    public String makeString() {
        return "BiomicTallLevelSource-v" + context.getGeneratorVersion();
    }

    @Override
    public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, int blockX, int blockY,
        int blockZ) {
        return BiomicBiomeApi.getBiome(world, blockX, blockY, blockZ)
            .getSpawnableList(creatureType);
    }

    @Override
    public ChunkPosition func_147416_a(World queryWorld, String structureName, int blockX, int blockY, int blockZ) {
        return mapFeaturesEnabled && BiomicStrongholdGenerator.handles(structureName)
            ? strongholds.findClosest(queryWorld, blockX, blockY, blockZ)
            : null;
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }

    @Override
    public void recreateStructures(int chunkX, int chunkZ) {
        if (!mapFeaturesEnabled) return;
        mineshafts.recreate(this, world, chunkX, chunkZ);
        villages.recreate(this, world, chunkX, chunkZ);
        strongholds.recreate(this, world, chunkX, chunkZ);
        if (scattered != null) scattered.prepare(this, world, chunkX, chunkZ, null);
    }

    @Override
    public void saveExtraData() {}

    @Override
    public boolean prefetchTerrain(int chunkX, int chunkZ) {
        return terrainPrecompute.prefetchAhead(chunkX, chunkZ);
    }

    @Override
    public boolean terrainReady(int chunkX, int chunkZ) {
        return terrainPrecompute.ready(chunkX, chunkZ);
    }
}
