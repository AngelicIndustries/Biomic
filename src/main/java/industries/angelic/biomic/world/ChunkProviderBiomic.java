package industries.angelic.biomic.world;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;

import ganymedes01.etfuturum.ModBlocks;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.api.BiomicBiomeApi;
import industries.angelic.biomic.biome.VerticalBiomeSampler;
import industries.angelic.biomic.compat.endlessids.EndlessIdsBiomeStorage;
import industries.angelic.biomic.config.DebugConfig;
import industries.angelic.biomic.config.PerformanceConfig;
import industries.angelic.biomic.content.BiomicBlocks;
import industries.angelic.biomic.content.RuImportedDecorationBlocks;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.ExpressionFunction;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.storage.BiomeVolumeManager;
import industries.angelic.biomic.world.structure.BiomicMineshaftGenerator;
import industries.angelic.biomic.world.structure.BiomicStrongholdGenerator;
import industries.angelic.biomic.world.structure.BiomicVillageGenerator;
import industries.angelic.biomic.world.terrain.BiomicTerrainSampler;
import industries.angelic.biomic.world.terrain.HostedCaveRuntime;
import industries.angelic.biomic.world.terrain.HostedIslandBiomes;
import industries.angelic.biomic.world.terrain.HostedIslandRuntime;
import industries.angelic.biomic.world.terrain.MacroTerrainSampler;
import industries.angelic.biomic.world.terrain.OrigenFeatureRuntime;

/** Biomic-owned chunk provider with deterministic terrain, biome storage and population boundaries. */
public final class ChunkProviderBiomic
    implements IChunkProvider, BenchmarkedChunkProvider, JournaledPopulation, TerrainPrefetching, BiomeLocating {

    private static final byte[] TERRAIN_METADATA_LOOKUP = createTerrainMetadataLookup();

    private final World world;
    private final WorldgenContext context;
    private final boolean mapFeaturesEnabled;
    private final boolean structurePassEnabled;
    private final Block[] terrainBlockLookup;
    private final BiomicTerrainSampler terrainSampler;
    private final VerticalBiomeSampler verticalBiomeSampler;
    private final TerrainPrecomputeScheduler terrainPrecompute;
    private final BiomicMineshaftGenerator mineshaftGenerator;
    private final BiomicVillageGenerator villageGenerator;
    private final BiomicStrongholdGenerator strongholdGenerator;
    private final OrigenFeatureRuntime origenFeatureRuntime;
    /**
     * Temples, witch huts, igloos, ruined portals, dungeons, wells and island geodes (order-independent population
     * only).
     */
    private final industries.angelic.biomic.world.structure.BiomicScatteredFeatures scattered;
    private final HostedIslandRuntime hostedIslands;
    private final HostedIslandRuntime mainlandHabitats;
    private final industries.angelic.biomic.world.terrain.FloraLedger flora;
    private final HostedCaveRuntime caveHabitats;
    private final industries.angelic.biomic.world.terrain.CaveDecorPass caveDecor;
    private final WorldgenPerformanceTracker performance = new WorldgenPerformanceTracker();

    /** Non-null only while an operator audit is capturing the arrays actually handed to new chunks. */

    public ChunkProviderBiomic(World world, WorldgenContext context) {
        if (world == null || context == null) {
            throw new IllegalArgumentException("World and worldgen context cannot be null");
        }
        this.world = world;
        this.context = context;
        this.mapFeaturesEnabled = world.getWorldInfo()
            .isMapFeaturesEnabled();
        // Structures belong to the complete Biomic world, not to the provisional RU surface switch. Keep the
        // disposable strict-Origen oracle free of additions, but enable vanilla-compatible structures for every
        // composed pack world (including Origen + floating islands with RU still disabled).
        this.structurePassEnabled = !context.getConfig()
            .isStrictOrigenBaseline();
        this.terrainBlockLookup = createTerrainBlockLookup();
        MacroTerrainSampler macroTerrainSampler = world.getWorldChunkManager() instanceof WorldChunkManagerBiomic
            ? ((WorldChunkManagerBiomic) world.getWorldChunkManager()).getMacroTerrainSampler()
            : new MacroTerrainSampler(context);
        this.verticalBiomeSampler = new VerticalBiomeSampler(context);
        this.terrainSampler = new BiomicTerrainSampler(context, macroTerrainSampler, verticalBiomeSampler);
        this.villageGenerator = new BiomicVillageGenerator(context, macroTerrainSampler, terrainSampler);
        // Village ground is shaped on the terrain workers, where village plans are made, before any reader sees it.
        this.terrainPrecompute = new TerrainPrecomputeScheduler(
            verticalBiomeSampler,
            terrainSampler,
            mapFeaturesEnabled && structurePassEnabled
                ? (chunkX, chunkZ, materials) -> villageGenerator.shapeTerrain(world, chunkX, chunkZ, materials)
                : null,
            mapFeaturesEnabled && structurePassEnabled
                ? (chunkX, chunkZ, workers) -> warmStructures(chunkX, chunkZ, workers)
                : null,
            PerformanceConfig.terrainWorkerThreads,
            PerformanceConfig.terrainPrefetchTasks);
        this.hostedIslands = new HostedIslandRuntime(context, terrainSampler, false, this::hostedRawTerrain);
        this.mainlandHabitats = new HostedIslandRuntime(context, terrainSampler, true, this::hostedRawTerrain);
        this.flora = new industries.angelic.biomic.world.terrain.FloraLedger(
            context,
            mainlandHabitats,
            63,
            BiomicTerrainSampler.WORLD_HEIGHT);
        terrainSampler.setSkyIslandRules(SkyIslandsWorldData.biomes(world), mainlandHabitats.groundHabitats());
        hostedIslands.setIslands(new HostedIslandRuntime.IslandShape() {

            @Override
            public int span(int x, int z) {
                return terrainSampler.floatingSpan(x, z);
            }

            @Override
            public java.util.List<int[]> islets(int chunkX, int chunkZ) {
                return terrainSampler.floatingIslets(chunkX, chunkZ);
            }
        });
        this.caveHabitats = new HostedCaveRuntime(context, terrainSampler);
        industries.angelic.biomic.world.terrain.CaveThemes caveThemes = context.getCaveExpansion()
            .nativeEcology()
                ? new industries.angelic.biomic.world.terrain.CaveThemes(
                    context,
                    false,
                    BiomicTerrainSampler.WORLD_HEIGHT)
                : null;
        terrainSampler.setCaveThemes(caveThemes);
        this.caveDecor = caveThemes == null ? null
            : new industries.angelic.biomic.world.terrain.CaveDecorPass(
                mainlandHabitats,
                caveThemes,
                BiomicTerrainSampler.WORLD_HEIGHT,
                false);
        this.mineshaftGenerator = new BiomicMineshaftGenerator(context, terrainSampler);
        this.strongholdGenerator = new BiomicStrongholdGenerator(
            industries.angelic.biomic.world.structure.StructureTerrain.legacy(context, terrainSampler));
        this.origenFeatureRuntime = new OrigenFeatureRuntime(
            context,
            terrainSampler,
            (x, y, z, bulky) -> villageGenerator.guardsDecoration(world, "origen", bulky, x, y, z));
        this.scattered = mapFeaturesEnabled && structurePassEnabled && OrigenFeatureRuntime.ORDER_INDEPENDENT
            ? new industries.angelic.biomic.world.structure.BiomicScatteredFeatures(
                context,
                industries.angelic.biomic.world.structure.StructureTerrain.legacy(context, terrainSampler),
                origenFeatureRuntime.providedTerrain,
                new industries.angelic.biomic.world.structure.BiomicScatteredFeatures.Habitats() {

                    @Override
                    public String ground(int x, int z) {
                        return mainlandHabitats.surfaceHabitat(x, z);
                    }

                    @Override
                    public String island(int x, int y, int z) {
                        return terrainSampler.floatingHabitatAt(x, y, z);
                    }
                })
            : null;
        HostedIslandRuntime.DecorationGuard guard = new HostedIslandRuntime.DecorationGuard() {

            @Override
            public boolean blocks(String feature, boolean bulky, int x, int y, int z) {
                return !HostedIslandRuntime.snowCover(feature)
                    && (villageGenerator.guardsDecoration(world, feature, bulky, x, y, z)
                        || scattered != null && scattered.protects(x, y, z, bulky ? 8 : 2));
            }

            @Override
            public boolean near(int x, int y, int z, int reach) {
                return villageGenerator.nearDecoration(world, x, y, z, reach)
                    || scattered != null && scattered.protects(x, y, z, reach);
            }
        };
        if (scattered != null)
            scattered.setVillages((x, y, z, reach) -> villageGenerator.nearDecoration(world, x, y, z, reach));
        if (scattered != null) origenFeatureRuntime.setSurfaceProtection(new OrigenFeatureRuntime.SurfaceProtection() {

            @Override
            public boolean contains(int x, int y, int z, boolean bulky) {
                return guard.blocks("origen", bulky, x, y, z);
            }

            @Override
            public boolean near(int x, int y, int z, int reach) {
                return guard.near(x, y, z, reach);
            }
        });
        if (mapFeaturesEnabled && structurePassEnabled) {
            hostedIslands.setVillageGuard(guard);
            mainlandHabitats.setVillageGuard(guard);
            flora.setGuard(guard);
        }
        origenFeatureRuntime.setCaveLichenColonies(caveDecor != null);
        if (OrigenFeatureRuntime.ORDER_INDEPENDENT) origenFeatureRuntime.enableJournaledPopulation(
            (cx, cz, m, b, meta) -> paintSkins(cx, cz, m, b, meta, verticalBiomeSampler.createVolume(cx, cz)),
            this::shapedRawTerrain);
        {
            origenFeatureRuntime.setLocatedHostedPlantSupport(mainlandHabitats::groundCoverSupportAt);
            origenFeatureRuntime.setHostedSurfaceOwner(mainlandHabitats::ownsSurfaceDecorationAt);
            origenFeatureRuntime.setHostedCaveOwner(caveHabitats::owns);
        }
        // Origen's own sea-floor and cave dressing gives way to the chasm's ecology.
        if (terrainSampler.abyssalRuntime() != null)
            origenFeatureRuntime.setHostedEnvironmentOwner(terrainSampler::isAbyssalHabitat);
        if (context.getNativeHabitats()
            .bopHabitats()
            .stream()
            .anyMatch(industries.angelic.biomic.compat.biome.BopNativeBiomes::reservesOreSubstrate))
            origenFeatureRuntime.setHostedOreSubstrateOwner(
                (x, y, z) -> mainlandHabitats.ownsNativeShoreSoilAt(x, y, z)
                    || hostedIslands.ownsNativeShoreSoilAt(x, y, z));
    }

    @Override
    public boolean chunkExists(int chunkX, int chunkZ) {
        return true;
    }

    /** Releases this provider's pure-terrain workers when its server world is unloaded. */
    void shutdownTerrainPrecompute() {
        terrainPrecompute.shutdown();
    }

    /**
     * A chunk's raw terrain for the hosted habitats, exactly as provided (shapedRawTerrain). Their own fallback has no
     * village ground shaping, and whether a read fell back to it depended on what had generated since.
     */
    private byte[] hostedRawTerrain(int chunkX, int chunkZ) {
        byte[] retained = origenFeatureRuntime.peekRawTerrain(chunkX, chunkZ);
        return retained != null ? retained : shapedRawTerrain(chunkX, chunkZ);
    }

    public List<String> getPerformanceReport() {
        List<String> report = performance.report();
        report.add(ServerTickTiming.report());
        report.add(terrainPrecompute.performanceReport());
        String caveCacheReport = terrainSampler.caveCachePerformanceReport();
        if (caveCacheReport != null) report.add(caveCacheReport);
        report.add(origenFeatureRuntime.performanceReport());
        report.add(hostedIslands.report());
        report.add(mainlandHabitats.report());
        report.add(flora.report());
        if (caveDecor != null) report.add(caveDecor.report());
        String featureProfile = origenFeatureRuntime.featureProfileReport(20);
        if (featureProfile != null) report.add(featureProfile);
        String terraScriptProfile = origenFeatureRuntime.terraScriptProfileReport(20);
        if (terraScriptProfile != null) report.add(terraScriptProfile);
        String expressionProfile = ExpressionFunction.profileReport(20);
        if (expressionProfile != null) report.add(expressionProfile);
        return report;
    }

    public void resetPerformanceStats() {
        ServerTickTiming.reset();
        performance.reset();
        terrainPrecompute.resetPerformanceStats();
        origenFeatureRuntime.resetPerformanceStats();
        hostedIslands.resetPerformanceStats();
        mainlandHabitats.resetPerformanceStats();
        ExpressionFunction.resetProfile();
    }

    @Override
    public WorldgenContext worldgenContext() {
        return context;
    }

    @Override
    public industries.angelic.biomic.storage.BiomeVolume rawBiomeVolume(int chunkX, int chunkZ) {
        return verticalBiomeSampler.createVolume(chunkX, chunkZ);
    }

    @Override
    public industries.angelic.biomic.world.terrain.ShiftedSkyIslands skyIslands() {
        return null;
    }

    @Override
    public int groundHeight(int x, int z) {
        return -1;
    }

    /** Hands the village and sky structure plans around a chunk the server is about to build to the workers. */
    private void warmStructures(int chunkX, int chunkZ, java.util.concurrent.Executor workers) {
        villageGenerator.warm(world, chunkX, chunkZ, workers);
        if (scattered != null) scattered.warm(chunkX, chunkZ, workers);
    }

    @Override
    public Chunk provideChunk(int chunkX, int chunkZ) {
        long startedAt = System.nanoTime();
        TerrainPrecomputeScheduler.TerrainPayload terrainPayload = terrainPrecompute.take(chunkX, chunkZ);
        long precomputedAt = System.nanoTime();
        BiomeVolume biomeVolume = terrainPayload.getBiomeVolume();
        byte[] materials = terrainPayload.getMaterials();
        origenFeatureRuntime.rememberRawTerrain(chunkX, chunkZ, materials);
        origenFeatureRuntime.rememberAuthoredTerrain(chunkX, chunkZ, terrainPayload.getAuthoredMaterials());
        ResolvedTerrain resolved = resolveTerrain(materials, terrainBlockLookup);
        Block[] blocks = resolved.blocks;
        byte[] metadata = resolved.metadata;
        biomeVolume = paintSkins(chunkX, chunkZ, materials, blocks, metadata, biomeVolume);
        origenFeatureRuntime.rememberProvided(chunkX, chunkZ, materials, blocks, metadata);
        long materialsResolvedAt = System.nanoTime();
        if (mapFeaturesEnabled && structurePassEnabled) {
            mineshaftGenerator.prepare(this, world, chunkX, chunkZ, blocks);
            villageGenerator.prepare(this, world, chunkX, chunkZ, blocks);
            strongholdGenerator.prepare(this, world, chunkX, chunkZ, blocks);
            if (scattered != null) scattered.prepare(this, world, chunkX, chunkZ, blocks);
        }
        long structuresPreparedAt = System.nanoTime();
        Chunk chunk = new Chunk(world, blocks, metadata, chunkX, chunkZ);
        long chunkConstructedAt = System.nanoTime();
        BiomeGenBase[] surfaceBiomes = world.getWorldChunkManager()
            .loadBlockGeneratorData(null, chunkX << 4, chunkZ << 4, 16, 16);
        int[] surfaceBiomeIds = new int[EndlessIdsBiomeStorage.ENTRY_COUNT];
        for (int index = 0; index < surfaceBiomeIds.length; index++) {
            surfaceBiomeIds[index] = surfaceBiomes[index].biomeID;
        }
        EndlessIdsBiomeStorage.setBiomeIds(chunk, surfaceBiomeIds);
        BiomeVolumeManager.setVolume(chunk, biomeVolume);
        long biomesStoredAt = System.nanoTime();
        chunk.generateSkylightMap();
        // Keep Hodgepodge's SendWithoutPopulation latch closed until population completes. Sending this raw terrain
        // and later replacing it with the populated chunk makes the client light and mesh every new chunk twice and
        // permits an older queued bulk packet to overwrite the newer populated snapshot.
        long completedAt = System.nanoTime();
        performance.recordTerrain(
            precomputedAt - startedAt,
            materialsResolvedAt - precomputedAt,
            structuresPreparedAt - materialsResolvedAt,
            chunkConstructedAt - structuresPreparedAt,
            biomesStoredAt - chunkConstructedAt,
            completedAt - biomesStoredAt,
            completedAt - startedAt);

        if (DebugConfig.traceTerrainPipeline) {
            Biomic.LOG.info(
                "Generated Biomic terrain chunk [{}, {}] in {} us",
                chunkX,
                chunkZ,
                (completedAt - startedAt) / 1_000L);
        }
        return chunk;
    }

    @Override
    public Chunk loadChunk(int chunkX, int chunkZ) {
        return provideChunk(chunkX, chunkZ);
    }

    public void nativeTreeSourceLoaded(Chunk chunk) {
        if (context.getNativeHabitats()
            .bop() && chunk.isTerrainPopulated)
            origenFeatureRuntime.nativeTreeSourceComplete(world, chunk.xPosition, chunk.zPosition);
    }

    /**
     * A chunk's raw terrain exactly as provideChunk takes it: the workers' payload, or their computation here, village
     * ground shaping included (Origen's own fallback has none, and which one a population read depended on timing).
     */
    private byte[] shapedRawTerrain(int chunkX, int chunkZ) {
        byte[] materials = terrainPrecompute.peekRawMaterials(chunkX, chunkZ);
        if (materials != null) return materials;
        materials = terrainSampler
            .createMaterialArray(chunkX, chunkZ, verticalBiomeSampler.createVolume(chunkX, chunkZ));
        if (mapFeaturesEnabled && structurePassEnabled) villageGenerator.shapeTerrain(world, chunkX, chunkZ, materials);
        return materials;
    }

    /** The habitats' surface skins over resolved terrain, as the chunk is provided (and as population reads it). */
    private BiomeVolume paintSkins(int chunkX, int chunkZ, byte[] materials, Block[] blocks, byte[] metadata,
        BiomeVolume biomeVolume) {
        biomeVolume = caveHabitats.surface(chunkX, chunkZ, materials, blocks, metadata, biomeVolume);
        mainlandHabitats.surface(chunkX, chunkZ, materials, blocks, metadata);
        biomeVolume = mainlandHabitats.projectMainland(biomeVolume, chunkX, chunkZ);
        hostedIslands.surface(chunkX, chunkZ, materials, blocks, metadata);
        biomeVolume = HostedIslandBiomes.project(biomeVolume, materials, chunkX, chunkZ, terrainSampler);
        return terrainSampler.projectAbyssal(biomeVolume, chunkX, chunkZ);
    }

    @Override
    public OrigenFeatureRuntime populationRuntime() {
        return origenFeatureRuntime;
    }

    @Override
    public void populate(IChunkProvider chunkProvider, int chunkX, int chunkZ) {
        long startedAt = System.nanoTime();
        long eventsAt = startedAt;
        long structuresAt = startedAt;
        long dungeonsAt = startedAt;
        long oresAt = startedAt;
        long vegetationAt = startedAt;
        long cavesAt = startedAt;
        Random random = new Random(context.seedForChunk(WorldgenPhase.POPULATION, chunkX, chunkZ));
        boolean previousFallInstantly = BlockFalling.fallInstantly;
        BlockFalling.fallInstantly = true;
        boolean villageGenerated = false;
        boolean journaled = origenFeatureRuntime.journaledPopulation();
        try {
            MinecraftForge.EVENT_BUS
                .post(new PopulateChunkEvent.Pre(chunkProvider, world, random, chunkX, chunkZ, false));
            eventsAt = System.nanoTime();
            if (!journaled && mapFeaturesEnabled && structurePassEnabled) {
                mineshaftGenerator.populate(world, context, chunkX, chunkZ);
                villageGenerated = villageGenerator.populate(world, context, chunkX, chunkZ);
                Random structureRandom = new Random(context.seedForChunk(WorldgenPhase.STRUCTURES, chunkX, chunkZ));
                strongholdGenerator.populate(world, structureRandom, chunkX, chunkZ);
            }
            structuresAt = System.nanoTime();
            // Origen's imported stages own preprocessing, ores, deposits, trees, flora and postprocessing.
            dungeonsAt = structuresAt;
            oresAt = structuresAt;
            // Order-independent population: OrigenFeatureRuntime#applyJournal (see ChunkProviderBiomicTall#populate).
            if (journaled) origenFeatureRuntime.beginJournal(chunkX, chunkZ);
            if (terrainSampler.abyssalRuntime() != null) terrainSampler.abyssalRuntime()
                .populate(
                    world,
                    chunkX,
                    chunkZ,
                    origenFeatureRuntime,
                    context.getSettings()
                        .getWorldSeed(),
                    terrainSampler::isAbyssalHabitat,
                    terrainSampler::abyssalSourceFloor);
            origenFeatureRuntime.populate(
                world,
                chunkX,
                chunkZ,
                terrainPrecompute::peekAuthoredMaterials,
                this::shapedRawTerrain,
                terrainPrecompute::peekCaveProjectionMask);
            caveHabitats.populate(world, chunkX, chunkZ, origenFeatureRuntime);
            if (caveDecor != null) caveDecor.populate(world, chunkX, chunkZ, origenFeatureRuntime);
            mainlandHabitats.populate(world, chunkX, chunkZ, origenFeatureRuntime);
            hostedIslands.populate(world, chunkX, chunkZ, origenFeatureRuntime);
            flora.populate(world, chunkX, chunkZ, origenFeatureRuntime);
            if (villageGenerated) villageGenerator.sealBlacksmithLava(world, chunkX, chunkZ, false);
            if (journaled) {
                origenFeatureRuntime.applyJournal(world);
                if (mapFeaturesEnabled && structurePassEnabled)
                    villageGenerated = industries.angelic.biomic.world.structure.StructurePopulationBox.populate(
                        world,
                        context,
                        chunkX,
                        chunkZ,
                        origenFeatureRuntime.providedTerrain,
                        mineshaftGenerator,
                        villageGenerator,
                        strongholdGenerator,
                        scattered);
            }
            vegetationAt = System.nanoTime();
            cavesAt = vegetationAt;
            MinecraftForge.EVENT_BUS
                .post(new PopulateChunkEvent.Post(chunkProvider, world, random, chunkX, chunkZ, villageGenerated));
        } finally {
            if (journaled) {
                origenFeatureRuntime.closeJournal();
                origenFeatureRuntime.applyDeferredOverflow(world);
            }
            BlockFalling.fallInstantly = previousFallInstantly;
            long completedAt = System.nanoTime();
            performance.recordPopulation(
                eventsAt - startedAt + completedAt - cavesAt,
                structuresAt - eventsAt,
                dungeonsAt - structuresAt,
                oresAt - dungeonsAt,
                vegetationAt - oresAt,
                cavesAt - vegetationAt,
                completedAt - startedAt);
        }
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
        return "BiomicLevelSource-v" + context.getGeneratorVersion();
    }

    @Override
    public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, int blockX, int blockY,
        int blockZ) {
        // None of the currently owned structures has an alternate natural-spawn list. A future hut or monument
        // adapter must resolve its structure override here before falling back to the position-aware 3D biome.
        return BiomicBiomeApi.getBiome(world, blockX, blockY, blockZ)
            .getSpawnableList(creatureType);
    }

    @Override
    public ChunkPosition func_147416_a(World queryWorld, String structureName, int blockX, int blockY, int blockZ) {
        return mapFeaturesEnabled && structurePassEnabled && BiomicStrongholdGenerator.handles(structureName)
            ? strongholdGenerator.findClosest(queryWorld, blockX, blockY, blockZ)
            : null;
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }

    @Override
    public void recreateStructures(int chunkX, int chunkZ) {
        if (mapFeaturesEnabled && structurePassEnabled) {
            mineshaftGenerator.recreate(this, world, chunkX, chunkZ);
            villageGenerator.recreate(this, world, chunkX, chunkZ);
            strongholdGenerator.recreate(this, world, chunkX, chunkZ);
            if (scattered != null) scattered.prepare(this, world, chunkX, chunkZ, null);
        }
    }

    @Override
    public void saveExtraData() {}

    /** Resolves the compact pure-terrain buffer for both chunk construction and non-generating feature reads. */
    public static Block[] resolveBlocks(byte[] materials) {
        Block[] lookup = createTerrainBlockLookup();
        Block[] blocks = new Block[materials.length];
        for (int index = 0; index < materials.length; index++) {
            int material = materials[index] & 0xFF;
            validateTerrainMaterial(material);
            if (material != BiomicTerrainSampler.AIR) blocks[index] = lookup[material];
        }
        return blocks;
    }

    /** Resolves the metadata half of the compact pure-terrain buffer. */
    public static byte[] resolveMetadata(byte[] materials) {
        byte[] metadata = new byte[materials.length];
        for (int index = 0; index < materials.length; index++) {
            int material = materials[index] & 0xFF;
            validateTerrainMaterial(material);
            byte value = TERRAIN_METADATA_LOOKUP[material];
            if (value != 0) metadata[index] = value;
        }
        return metadata;
    }

    private static ResolvedTerrain resolveTerrain(byte[] materials, Block[] blockLookup) {
        Block[] blocks = new Block[materials.length];
        byte[] metadata = new byte[materials.length];
        for (int index = 0; index < materials.length; index++) {
            int material = materials[index] & 0xFF;
            validateTerrainMaterial(material);
            if (material == BiomicTerrainSampler.AIR) continue;
            blocks[index] = blockLookup[material];
            byte value = TERRAIN_METADATA_LOOKUP[material];
            if (value != 0) metadata[index] = value;
        }
        return new ResolvedTerrain(blocks, metadata);
    }

    private static Block[] createTerrainBlockLookup() {
        Block[] lookup = new Block[BiomicTerrainSampler.MAX_MATERIAL + 1];
        Block moss = ModBlocks.MOSS_BLOCK.get();
        Block magma = ModBlocks.MAGMA.get();
        Block basalt = ModBlocks.BASALT.get();
        Block tuff = ModBlocks.TUFF.get();
        Block bountifulStone = ModBlocks.STONE.get();
        Block blackstone = ModBlocks.BLACKSTONE.get();
        Block blueIce = ModBlocks.BLUE_ICE.get();
        Block calcite = ModBlocks.CALCITE.get();
        Block deepslate = ModBlocks.DEEPSLATE.get();
        Block dripstone = ModBlocks.DRIPSTONE_BLOCK.get();
        Block redSandstone = ModBlocks.RED_SANDSTONE.get();
        Block smoothBasalt = ModBlocks.SMOOTH_BASALT.get();
        Block smoothSandstone = ModBlocks.SMOOTH_SANDSTONE.get();
        Block concrete = ModBlocks.CONCRETE.get();
        Block concretePowder = ModBlocks.CONCRETE_POWDER.get();
        Block mud = ModBlocks.MUD.get();
        Block coarseDirt = ModBlocks.COARSE_DIRT.get();
        Block packedMud = ModBlocks.PACKED_MUD.get();

        lookup[BiomicTerrainSampler.BEDROCK] = Blocks.bedrock;
        lookup[BiomicTerrainSampler.STONE] = Blocks.stone;
        lookup[BiomicTerrainSampler.DIRT] = Blocks.dirt;
        lookup[BiomicTerrainSampler.GRASS] = Blocks.grass;
        lookup[BiomicTerrainSampler.WATER] = Blocks.water;
        lookup[BiomicTerrainSampler.SAND] = Blocks.sand;
        lookup[BiomicTerrainSampler.GRAVEL] = Blocks.gravel;
        lookup[BiomicTerrainSampler.MOSS] = moss == null ? Blocks.mossy_cobblestone : moss;
        lookup[BiomicTerrainSampler.TERRACOTTA] = Blocks.hardened_clay;
        lookup[BiomicTerrainSampler.BASALT] = basalt == null ? Blocks.stone : basalt;
        lookup[BiomicTerrainSampler.MAGMA] = magma == null ? Blocks.stone : magma;
        lookup[BiomicTerrainSampler.SNOW] = Blocks.snow;
        lookup[BiomicTerrainSampler.MYCELIUM] = Blocks.mycelium;
        lookup[BiomicTerrainSampler.ICE] = Blocks.ice;
        lookup[BiomicTerrainSampler.PACKED_ICE] = Blocks.packed_ice;
        lookup[BiomicTerrainSampler.CHALK] = BiomicBlocks.CHALK;
        lookup[BiomicTerrainSampler.TUFF] = tuff == null ? Blocks.stone : tuff;
        lookup[BiomicTerrainSampler.PODZOL] = Blocks.dirt;
        lookup[BiomicTerrainSampler.MUD] = mud == null ? Blocks.dirt : mud;
        lookup[BiomicTerrainSampler.RED_TERRACOTTA] = Blocks.stained_hardened_clay;
        lookup[BiomicTerrainSampler.ORANGE_TERRACOTTA] = Blocks.stained_hardened_clay;
        lookup[BiomicTerrainSampler.BROWN_TERRACOTTA] = Blocks.stained_hardened_clay;
        lookup[BiomicTerrainSampler.COARSE_DIRT] = coarseDirt == null ? Blocks.dirt : coarseDirt;
        lookup[BiomicTerrainSampler.PACKED_MUD] = packedMud == null ? Blocks.hardened_clay : packedMud;
        lookup[BiomicTerrainSampler.RED_CONCRETE] = concrete == null ? Blocks.stained_hardened_clay : concrete;
        lookup[BiomicTerrainSampler.BROWN_CONCRETE] = concrete == null ? Blocks.stained_hardened_clay : concrete;
        lookup[BiomicTerrainSampler.LIGHT_GRAY_TERRACOTTA] = Blocks.stained_hardened_clay;
        lookup[BiomicTerrainSampler.PINK_TERRACOTTA] = Blocks.stained_hardened_clay;
        lookup[BiomicTerrainSampler.GRANITE] = bountifulStone == null ? Blocks.stone : bountifulStone;
        lookup[BiomicTerrainSampler.SANDSTONE] = Blocks.sandstone;
        lookup[BiomicTerrainSampler.ANDESITE] = bountifulStone == null ? Blocks.stone : bountifulStone;
        lookup[BiomicTerrainSampler.BLACKSTONE] = blackstone == null ? Blocks.cobblestone : blackstone;
        lookup[BiomicTerrainSampler.BLUE_ICE] = blueIce == null ? Blocks.packed_ice : blueIce;
        lookup[BiomicTerrainSampler.BLUE_TERRACOTTA] = Blocks.stained_hardened_clay;
        lookup[BiomicTerrainSampler.CALCITE] = calcite == null ? Blocks.quartz_block : calcite;
        lookup[BiomicTerrainSampler.CLAY] = Blocks.clay;
        lookup[BiomicTerrainSampler.CYAN_TERRACOTTA] = Blocks.stained_hardened_clay;
        lookup[BiomicTerrainSampler.DEAD_BRAIN_CORAL] = BiomicBlocks.CORAL_BLOCK;
        lookup[BiomicTerrainSampler.DEAD_BUBBLE_CORAL] = BiomicBlocks.CORAL_BLOCK;
        lookup[BiomicTerrainSampler.DEAD_HORN_CORAL] = BiomicBlocks.CORAL_BLOCK;
        lookup[BiomicTerrainSampler.DEEPSLATE] = deepslate == null ? Blocks.stone : deepslate;
        lookup[BiomicTerrainSampler.DIORITE] = bountifulStone == null ? Blocks.stone : bountifulStone;
        lookup[BiomicTerrainSampler.DRIPSTONE_BLOCK] = dripstone == null ? Blocks.stone : dripstone;
        lookup[BiomicTerrainSampler.FARMLAND_MOIST] = Blocks.farmland;
        lookup[BiomicTerrainSampler.LAVA] = Blocks.lava;
        lookup[BiomicTerrainSampler.LIGHT_BLUE_TERRACOTTA] = Blocks.stained_hardened_clay;
        lookup[BiomicTerrainSampler.LIME_TERRACOTTA] = Blocks.stained_hardened_clay;
        lookup[BiomicTerrainSampler.MOSSY_COBBLESTONE] = Blocks.mossy_cobblestone;
        lookup[BiomicTerrainSampler.OBSIDIAN] = Blocks.obsidian;
        lookup[BiomicTerrainSampler.POWDER_SNOW] = BiomicBlocks.POWDER_SNOW;
        lookup[BiomicTerrainSampler.RED_SAND] = Blocks.sand;
        lookup[BiomicTerrainSampler.RED_SANDSTONE] = redSandstone == null ? Blocks.sandstone : redSandstone;
        lookup[BiomicTerrainSampler.ROOTED_DIRT] = BiomicBlocks.ROOTED_DIRT;
        lookup[BiomicTerrainSampler.SMOOTH_BASALT] = smoothBasalt == null ? (basalt == null ? Blocks.stone : basalt)
            : smoothBasalt;
        lookup[BiomicTerrainSampler.SMOOTH_SANDSTONE] = smoothSandstone == null ? Blocks.sandstone : smoothSandstone;
        lookup[BiomicTerrainSampler.WHITE_CONCRETE_POWDER] = concretePowder == null ? Blocks.sand : concretePowder;
        lookup[BiomicTerrainSampler.WHITE_TERRACOTTA] = Blocks.stained_hardened_clay;
        lookup[BiomicTerrainSampler.YELLOW_CONCRETE] = concrete == null ? Blocks.stained_hardened_clay : concrete;
        lookup[BiomicTerrainSampler.YELLOW_TERRACOTTA] = Blocks.stained_hardened_clay;
        lookup[BiomicTerrainSampler.COBBLESTONE] = Blocks.cobblestone;
        lookup[BiomicTerrainSampler.CHALK_GRASS] = BiomicBlocks.CHALK_GRASS;
        lookup[BiomicTerrainSampler.PEAT_GRASS] = imported("regions_unexplored:peat_grass_block", Blocks.grass);
        lookup[BiomicTerrainSampler.PEAT_DIRT] = imported("regions_unexplored:peat_dirt", Blocks.dirt);
        lookup[BiomicTerrainSampler.SILT_GRASS] = imported("regions_unexplored:silt_grass_block", Blocks.grass);
        lookup[BiomicTerrainSampler.SILT_DIRT] = imported("regions_unexplored:silt_dirt", Blocks.dirt);
        lookup[BiomicTerrainSampler.ASHEN_DIRT] = imported("regions_unexplored:ashen_dirt", Blocks.dirt);
        lookup[BiomicTerrainSampler.ASH] = imported("regions_unexplored:ash", Blocks.gravel);
        lookup[BiomicTerrainSampler.ARGILLITE] = imported("regions_unexplored:argillite", Blocks.stone);
        lookup[BiomicTerrainSampler.ARGILLITE_GRASS] = imported(
            "regions_unexplored:argillite_grass_block",
            Blocks.grass);
        lookup[BiomicTerrainSampler.VIRIDESCENT_NYLIUM] = imported(
            "regions_unexplored:viridescent_nylium",
            Blocks.mycelium);
        lookup[BiomicTerrainSampler.DEEPSLATE_VIRIDESCENT_NYLIUM] = imported(
            "regions_unexplored:deepslate_viridescent_nylium",
            deepslate == null ? Blocks.stone : deepslate);
        lookup[BiomicTerrainSampler.PRISMOSS] = imported(
            "regions_unexplored:prismoss",
            moss == null ? Blocks.mossy_cobblestone : moss);
        lookup[BiomicTerrainSampler.DEEPSLATE_PRISMOSS] = imported(
            "regions_unexplored:deepslate_prismoss",
            deepslate == null ? Blocks.stone : deepslate);
        lookup[BiomicTerrainSampler.BRIMSPROUT_NYLIUM] = imported(
            "regions_unexplored:brimsprout_nylium",
            Blocks.netherrack);
        lookup[BiomicTerrainSampler.COBALT_NYLIUM] = imported("regions_unexplored:cobalt_nylium", Blocks.netherrack);
        lookup[BiomicTerrainSampler.GLISTERING_NYLIUM] = imported(
            "regions_unexplored:glistering_nylium",
            Blocks.netherrack);
        lookup[BiomicTerrainSampler.MYCOTOXIC_MOSS] = imported("regions_unexplored:mycotoxic_moss", Blocks.netherrack);
        lookup[BiomicTerrainSampler.PEAT_MUD] = imported(
            "regions_unexplored:peat_mud",
            mud == null ? Blocks.dirt : mud);
        lookup[BiomicTerrainSampler.SILT_MUD] = imported(
            "regions_unexplored:silt_mud",
            mud == null ? Blocks.dirt : mud);
        lookup[BiomicTerrainSampler.PEAT_PODZOL] = imported("regions_unexplored:peat_podzol", Blocks.dirt);
        lookup[BiomicTerrainSampler.SILT_PODZOL] = imported("regions_unexplored:silt_podzol", Blocks.dirt);
        lookup[BiomicTerrainSampler.FORLORN_GUANOSTONE] = industries.angelic.biomic.content.alexscaves.ForlornContentBlocks.GUANOSTONE;
        lookup[BiomicTerrainSampler.FORLORN_COPROLITH] = industries.angelic.biomic.content.alexscaves.ForlornContentBlocks.COPROLITH;
        lookup[BiomicTerrainSampler.PRIMORDIAL_LIMESTONE] = industries.angelic.biomic.content.alexscaves.PrimordialContentBlocks.LIMESTONE;
        lookup[BiomicTerrainSampler.PRIMORDIAL_FLOOD_BASALT] = industries.angelic.biomic.content.alexscaves.PrimordialContentBlocks.FLOOD_BASALT;
        lookup[BiomicTerrainSampler.PRIMORDIAL_MAGMA] = industries.angelic.biomic.content.alexscaves.PrimordialContentBlocks.PRIMAL_MAGMA;
        lookup[BiomicTerrainSampler.PRIMORDIAL_MAGMA_ACTIVE] = industries.angelic.biomic.content.alexscaves.PrimordialContentBlocks.PRIMAL_MAGMA;
        lookup[BiomicTerrainSampler.PRIMORDIAL_VOLCANIC_CORE] = industries.angelic.biomic.content.alexscaves.PrimordialContentBlocks.VOLCANIC_CORE;
        lookup[BiomicTerrainSampler.PRIMORDIAL_MOSS_CARPET] = ganymedes01.etfuturum.ModBlocks.MOSS_CARPET.get() == null
            ? Blocks.carpet
            : ganymedes01.etfuturum.ModBlocks.MOSS_CARPET.get();
        lookup[BiomicTerrainSampler.PRIMORDIAL_MOSS_BLOCK] = ganymedes01.etfuturum.ModBlocks.MOSS_BLOCK.get() == null
            ? Blocks.mossy_cobblestone
            : ganymedes01.etfuturum.ModBlocks.MOSS_BLOCK.get();
        lookup[BiomicTerrainSampler.ABYSSMARINE] = industries.angelic.biomic.content.alexscaves.AbyssalContentBlocks.ABYSSMARINE;
        lookup[BiomicTerrainSampler.ABYSSAL_MUCK] = industries.angelic.biomic.content.alexscaves.AbyssalContentBlocks.MUCK;
        return lookup;
    }

    private static Block imported(String modernId, Block fallback) {
        Block block = RuImportedDecorationBlocks.get(modernId);
        return block == null ? fallback : block;
    }

    private static byte[] createTerrainMetadataLookup() {
        byte[] lookup = new byte[BiomicTerrainSampler.MAX_MATERIAL + 1];
        lookup[BiomicTerrainSampler.PODZOL] = 2;
        lookup[BiomicTerrainSampler.RED_TERRACOTTA] = 14;
        lookup[BiomicTerrainSampler.ORANGE_TERRACOTTA] = 1;
        lookup[BiomicTerrainSampler.BROWN_TERRACOTTA] = 12;
        lookup[BiomicTerrainSampler.LIGHT_GRAY_TERRACOTTA] = 8;
        lookup[BiomicTerrainSampler.PINK_TERRACOTTA] = 6;
        lookup[BiomicTerrainSampler.RED_CONCRETE] = 14;
        lookup[BiomicTerrainSampler.BROWN_CONCRETE] = 12;
        lookup[BiomicTerrainSampler.GRANITE] = 1;
        lookup[BiomicTerrainSampler.ANDESITE] = 5;
        lookup[BiomicTerrainSampler.DIORITE] = 3;
        lookup[BiomicTerrainSampler.BLUE_TERRACOTTA] = 11;
        lookup[BiomicTerrainSampler.CYAN_TERRACOTTA] = 9;
        lookup[BiomicTerrainSampler.DEAD_BRAIN_CORAL] = 5;
        lookup[BiomicTerrainSampler.DEAD_BUBBLE_CORAL] = 6;
        lookup[BiomicTerrainSampler.DEAD_HORN_CORAL] = 8;
        lookup[BiomicTerrainSampler.YELLOW_CONCRETE] = 4;
        lookup[BiomicTerrainSampler.YELLOW_TERRACOTTA] = 4;
        lookup[BiomicTerrainSampler.FARMLAND_MOIST] = 7;
        lookup[BiomicTerrainSampler.LIGHT_BLUE_TERRACOTTA] = 3;
        lookup[BiomicTerrainSampler.LIME_TERRACOTTA] = 5;
        lookup[BiomicTerrainSampler.RED_SAND] = 1;
        lookup[BiomicTerrainSampler.PRIMORDIAL_MAGMA_ACTIVE] = 3;
        return lookup;
    }

    private static void validateTerrainMaterial(int material) {
        if (material >= TERRAIN_METADATA_LOOKUP.length) {
            throw new IllegalStateException("Unknown Biomic terrain material " + material);
        }
    }

    private static final class ResolvedTerrain {

        private final Block[] blocks;
        private final byte[] metadata;

        private ResolvedTerrain(Block[] blocks, byte[] metadata) {
            this.blocks = blocks;
            this.metadata = metadata;
        }
    }

    @Override
    public boolean prefetchTerrain(int chunkX, int chunkZ) {
        return terrainPrecompute.prefetchAhead(chunkX, chunkZ);
    }

    @Override
    public boolean terrainReady(int chunkX, int chunkZ) {
        return terrainPrecompute.ready(chunkX, chunkZ);
    }
}
