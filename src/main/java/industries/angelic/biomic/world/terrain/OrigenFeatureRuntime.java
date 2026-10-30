package industries.angelic.biomic.world.terrain;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.util.Constants;

import com.dfsek.terra.api.util.MathUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import ganymedes01.etfuturum.entities.EntityBee;
import ganymedes01.etfuturum.tileentities.TileEntityGlowLichen;
import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.content.BiomicAquaticBlocks;
import industries.angelic.biomic.internal.terra.noise.NoiseSampler;
import industries.angelic.biomic.world.ChunkProviderBiomic;
import industries.angelic.biomic.world.WorldgenContext;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/** Executes Origen's ordered Terra 6.4.2 feature stages against a Forge 1.7 world. */
public final class OrigenFeatureRuntime implements OrigenStructureEnvironment {

    static final int HOSTED_ENVIRONMENT_TRANSACTION_CAP = 65_536;

    static <T> boolean commitAdmittedHostedEnvironmentWrites(Map<Long, T> writes, boolean rejected,
        java.util.function.BiFunction<Long, T, Boolean> writer) {
        if (rejected || writes.size() > HOSTED_ENVIRONMENT_TRANSACTION_CAP) return false;
        for (var entry : writes.entrySet()) writer.apply(entry.getKey(), entry.getValue());
        return true;
    }

    /**
     * First value (including absence) belongs to this proposal; repeated writes never replace its rollback baseline.
     */
    static final class HostedAuthoredJournal<T> {

        private final LinkedHashMap<Long, T> before = new LinkedHashMap<>();

        boolean record(long position, java.util.function.Supplier<T> current) {
            if (before.containsKey(position)) return true;
            if (before.size() >= HOSTED_ENVIRONMENT_TRANSACTION_CAP) return false;
            before.put(position, current.get());
            return true;
        }

        void restore(java.util.function.BiConsumer<Long, T> writer) {
            before.forEach(writer);
            before.clear();
        }
    }

    static boolean requiresHostedEnvironmentTransaction(boolean intrinsic, boolean displacedEnvironment,
        boolean replacedVegetation) {
        return intrinsic || displacedEnvironment || replacedVegetation;
    }

    static <T> void publishHostedProposalEntities(List<T> entities, boolean accepted,
        java.util.function.Consumer<T> publisher) {
        if (!accepted) return;
        if (entities.size() > HOSTED_ENVIRONMENT_TRANSACTION_CAP)
            throw new IllegalStateException("Hosted proposal entity cap exceeded");
        entities.forEach(publisher);
    }

    private static final boolean PROFILE_FEATURES = Boolean.getBoolean("biomic.profileFeatures");
    private static final int STAGE_RESOLUTION = 4;
    private static final int MAX_RECURSIONS = 1000;
    private static final int RAW_TERRAIN_CACHE_SIZE = 128;
    /** Cave/marsh-projected authored chunks are pure per chunk; each one is read by ~9 neighbouring populations. */
    private static final int AUTHORED_VIEW_CACHE_SIZE = 128;
    private static final int AUTHORED_FEATURE_CACHE_SIZE = 256;
    private static final int AUTHORED_FEATURE_COLUMN_COUNT = 16 * 16;
    private static final int OCCLUDABLE_TREE_CACHE_SIZE = 512;
    private static final int DEPENDENT_STRUCTURE_CACHE_SIZE = OCCLUDABLE_TREE_CACHE_SIZE;
    private static final int[] GROUND_COVER_Y_OFFSETS = { 0, -1, 1, -2, 2 };
    private static final String LARGE_MUSHROOMS = "LARGE_MUSHROOMS";
    /** Origen's fills of every air cell in a band, with the band's lowest authored Y (LAVA_FLOOR rests on bedrock). */
    private static final Map<String, Integer> FLUID_FILLS = Map.of("LAVA_CAVE_FLOOR", -28);
    /** Origen's generic vanilla flower patches; a profile with a flora patch palette grows its own flowers in them. */
    private static final Set<String> RESKINNED_FLOWER_FEATURES = Set.of(
        "POPPY_PATCH",
        "ALLIUM_PATCH",
        "CORNFLOWER_PATCH",
        "DANDELION_PATCH",
        "LILAC_PATCH",
        "PEONY_PATCH",
        "ROSE_BUSH_PATCH",
        "OXEYE_DAISY_PATCH",
        "LILY_OF_THE_VALLEY_PATCH",
        "BLUE_ORCHID_PATCH",
        "MIXED_FLOWER_COVER",
        "WHITE_FLOWER_COVER");
    /** Each vanilla flower kind maps onto one palette entry, so a patch of one kind stays a patch of one plant. */
    private static final List<String> VANILLA_FLOWER_KINDS = List.of(
        "BLOCK:minecraft:poppy",
        "BLOCK:minecraft:dandelion",
        "BLOCK:minecraft:allium",
        "BLOCK:minecraft:cornflower",
        "BLOCK:minecraft:blue_orchid",
        "BLOCK:minecraft:oxeye_daisy",
        "BLOCK:minecraft:azure_bluet",
        "BLOCK:minecraft:lily_of_the_valley",
        "BLOCK:minecraft:red_tulip",
        "BLOCK:minecraft:orange_tulip",
        "BLOCK:minecraft:white_tulip",
        "BLOCK:minecraft:pink_tulip",
        "lilac",
        "peony",
        "rose_bush");
    private final Map<String, Structure> reskinnedFlowers = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<OrigenLeafDecor.Spec, Structure> leafDecor = new java.util.concurrent.ConcurrentHashMap<>();
    /** While a leaf-decor feature runs as Origen authored it, the block its leaf writes become (OrigenLeafDecor). */
    private String activeLeafSubstitute;
    /** The PMP bamboo species for Origen's bamboo where no feature names another. */
    /** While Origen's bamboo patches run, the PMP bamboo species its stalks are drawn in. */
    private String activeBamboo;
    private static final String TREE_VINES = "TREE_VINES";
    private static final String CROSSING_VINES = "CROSSING_VINES";
    private static final String LARGE_LEAF_VINES = "LARGE_LEAF_VINES";
    private static final String MUSHROOM_FLESH = "biomic:mushroom_flesh";
    private static final String MUSHROOM_FOLIAGE = "LARGE_LEAF_VINES_UNCOLORED";
    private static final String MUSHROOM_FOLIAGE_REPLAY = "MUSHROOM_FOLIAGE_REPLAY";
    private static final String POTATO_PATCH = "potato_patch";
    private static final int VILLAGE_SURFACE_DEPTH = 4;

    private final OrigenDensityField densityField;
    private final OrigenSurfaceRuntime surfaceRuntime;
    private final OrigenFeatureTerrainAccess terrainAccess;
    private final OrigenFeatureHeightTransport heightTransport;
    private SurfaceProtection surfaceProtection;
    private final RegionalHabitatSampler mainlandHabitats;
    private final boolean fullNativeMarsh;
    private final boolean physicalLichenSupport;
    private final boolean repairProjectedLandPlants;
    private final boolean nativeTreeAttachments;
    private final boolean nativeBogBushLeaves;

    @FunctionalInterface
    public interface LocatedPlantSupport {

        Boolean test(String state, String below, int x, int y, int z);
    }

    private LocatedPlantSupport hostedPlantSupport;
    private HostedSurfaceOwner hostedSurfaceOwner;
    private HostedSurfaceOwner hostedCaveOwner;
    private HostedSurfaceOwner hostedOreSubstrateOwner;
    private HostedSurfaceOwner hostedEnvironmentOwner;
    private HostedSurfaceOwner intrinsicEnvironmentOwner;

    @FunctionalInterface
    public interface HostedSurfaceOwner {

        boolean owns(int x, int y, int z);
    }

    public void setHostedSurfaceOwner(HostedSurfaceOwner owner) {
        hostedSurfaceOwner = owner;
    }

    private boolean caveLichenColonies;

    /** On when the cave decor pass owns glow lichen in generic caves. */
    public void setCaveLichenColonies(boolean colonies) {
        caveLichenColonies = colonies;
    }

    public void setHostedCaveOwner(HostedSurfaceOwner owner) {
        hostedCaveOwner = owner;
    }

    /** Structure footprints (village roads/buildings) that surface decoration must keep clear of. */
    public void setSurfaceProtection(SurfaceProtection protection) {
        surfaceProtection = java.util.Objects.requireNonNull(protection, "Surface protection");
    }

    public void setHostedOreSubstrateOwner(HostedSurfaceOwner owner) {
        hostedOreSubstrateOwner = owner;
    }

    /** Complete intrinsic environments reject displaced Origen features at both origin and destination. */
    public void setHostedEnvironmentOwner(HostedSurfaceOwner owner) {
        hostedEnvironmentOwner = owner;
    }

    private void setIntrinsicEnvironmentOwner(HostedSurfaceOwner owner) {
        intrinsicEnvironmentOwner = owner;
    }

    private final OrigenTerrainRuntime.Compiler compiler;
    private final OrigenBlockStateResolver blockResolver = new OrigenBlockStateResolver();
    private final Map<String, OrigenBlockStateResolver.ResolvedBlock> blockStateCache = new HashMap<>();
    private final Map<OrigenBlockStateResolver.ResolvedBlock, WorldBlock> resolvedWorldBlockCache = new IdentityHashMap<>();
    private final WorldBlock[] terrainMaterialCache = new WorldBlock[256];
    private final Long2ObjectLinkedOpenHashMap<byte[]> rawTerrainCache = new Long2ObjectLinkedOpenHashMap<>(
        RAW_TERRAIN_CACHE_SIZE);
    private final Long2ObjectLinkedOpenHashMap<byte[]> authoredViewCache = new Long2ObjectLinkedOpenHashMap<>(
        AUTHORED_VIEW_CACHE_SIZE + 1);

    private byte[] cachedAuthoredView(long chunkKey) {
        synchronized (authoredViewCache) {
            return authoredViewCache.getAndMoveToLast(chunkKey);
        }
    }

    private void rememberAuthoredView(long chunkKey, byte[] view) {
        synchronized (authoredViewCache) {
            authoredViewCache.putAndMoveToLast(chunkKey, view);
            if (authoredViewCache.size() > AUTHORED_VIEW_CACHE_SIZE) authoredViewCache.removeFirst();
        }
    }

    /** Exact sparse 384-high feature state used by later Terra locators before projection into physical storage. */
    private final Long2ObjectLinkedOpenHashMap<AuthoredFeatureColumn[]> authoredFeatureCache = new Long2ObjectLinkedOpenHashMap<>(
        AUTHORED_FEATURE_CACHE_SIZE);
    /** Trees retained until every immediately neighboring cross-chunk canopy has resolved. */
    private final Map<FeatureOrigin, AuthoredTreeStructure> occludableTreeStructures = new LinkedHashMap<FeatureOrigin, AuthoredTreeStructure>(
        OCCLUDABLE_TREE_CACHE_SIZE,
        0.75F,
        true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<FeatureOrigin, AuthoredTreeStructure> eldest) {
            return size() > OCCLUDABLE_TREE_CACHE_SIZE;
        }
    };
    /** Decorations whose locator was admitted by an authored tree, grouped by that tree's canonical origin. */
    private final Map<FeatureOrigin, List<AuthoredDependentStructure>> dependentStructures = new LinkedHashMap<FeatureOrigin, List<AuthoredDependentStructure>>(
        DEPENDENT_STRUCTURE_CACHE_SIZE,
        0.75F,
        true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<FeatureOrigin, List<AuthoredDependentStructure>> eldest) {
            return size() > DEPENDENT_STRUCTURE_CACHE_SIZE;
        }
    };
    private final List<String> stages = new ArrayList<>();
    private final Map<String, Feature> features = new LinkedHashMap<>();
    private final Map<String, Structure> structures = new LinkedHashMap<>();
    private final Map<String, FeatureProfile> featureProfiles = new HashMap<>();
    private final EnumMap<OrigenBiomeProfile, Map<String, List<Feature>>> profileFeatures = new EnumMap<>(
        OrigenBiomeProfile.class);
    private final Set<String> compatibilitySubstitutes = new TreeSet<>();
    private final Random distributorRandom = new Random(0L);
    private final Random locatorRandom = new Random(0L);
    private final Random structureRandom = new Random(0L);
    private PopulationView populationView;
    /** True only while a reviewed hosted runtime is issuing its own physical writes. */
    private boolean hostedPhysicalPopulation;
    private FeatureOrigin locatorOrigin;
    private FeatureOrigin activeFeatureOrigin;
    private OrigenBlockStateResolver.ResolvedBlock lastAuthoredResolved;
    private FeatureOrigin lastAuthoredOrigin;
    private FeatureOrigin lastAuthoredDependencyOrigin;
    private String lastAuthoredFeatureId;
    private AuthoredFeatureBlock lastAuthoredFeatureBlock;
    private String activeFeatureId;
    private String activeFeatureStage;
    private OrigenBiomeProfile activeFeatureProfile;
    private int activeStructureAuthoredY;
    private Set<Long> activeOccludableTreeBlocks;
    private Set<Long> activeDependentStructureBlocks;
    private FeatureOrigin activeDependencyOrigin;
    private Set<Long> activeLargeMushroomColumns;
    private Map<Long, RejectedMushroomPlacement> activeRejectedMushroomPlacements;
    private long authoredTerrainCacheHits;
    private long authoredTerrainCacheMisses;
    private long authoredTerrainPayloadRecoveries;
    private long authoredTerrainLoadedMisses;
    private long authoredTerrainUnloadedMisses;
    private final long[] authoredTerrainMissDistance = new long[4];
    private long authoredColumnFallbacks;
    private long rawTerrainCacheHits;
    private long rawTerrainPayloadRecoveries;
    private long rawTerrainCacheMisses;
    private long caveProjectionPayloadRecoveries;

    public OrigenFeatureRuntime(WorldgenContext context, BiomicTerrainSampler terrainSampler,
        SurfaceProtection surfaceProtection) {
        this(context, legacyAccess(context, terrainSampler), surfaceProtection);
    }

    /** Dormant shifted RU-mainland seam; provider/save-policy activation remains external. */
    public static OrigenFeatureRuntime projectedHosted(WorldgenContext context, OrigenProjectedTerrainSampler terrain,
        HostedTerrainAccess hosted) {
        requireProjectedHostedContext(context, hosted);
        OrigenFeatureTerrainAccess access = OrigenFeatureTerrainAccess.projectedHosted(context, terrain, hosted);
        requireProjectedTransport(access);
        return new OrigenFeatureRuntime(context, access, (x, y, z, bulky) -> false, true);
    }

    /** Dormant shifted complete-environment seam; provider/save-policy activation remains external. */
    public static OrigenFeatureRuntime projectedIntrinsicHosted(WorldgenContext context,
        OrigenProjectedTerrainSampler terrain, IntrinsicHostedTerrainAccess hosted) {
        requireProjectedHostedContext(context, hosted);
        OrigenFeatureTerrainAccess access = OrigenFeatureTerrainAccess
            .projectedIntrinsicHosted(context, terrain, hosted);
        requireProjectedTransport(access);
        OrigenFeatureRuntime runtime = new OrigenFeatureRuntime(context, access, (x, y, z, bulky) -> false, true);
        HostedSurfaceOwner owner = hosted::blocksHostedEcology;
        runtime.setHostedEnvironmentOwner(owner);
        runtime.setHostedCaveOwner(owner);
        runtime.setIntrinsicEnvironmentOwner(owner);
        return runtime;
    }

    /** Saved shifted-native adapter; ordinary projected factories retain their original rejection gates. */
    public static OrigenFeatureRuntime projectedNativeIntrinsicHosted(WorldgenContext context,
        OrigenProjectedTerrainSampler terrain, IntrinsicHostedTerrainAccess hosted,
        ShiftedNativeMarshTerrainAccess marsh) {
        requireProjectedNativeHostedContext(context, hosted);
        OrigenFeatureTerrainAccess access = OrigenFeatureTerrainAccess
            .projectedNativeIntrinsicHosted(context, terrain, hosted, marsh);
        requireProjectedTransport(access);
        OrigenFeatureRuntime runtime = new OrigenFeatureRuntime(context, access, (x, y, z, bulky) -> false, true);
        HostedSurfaceOwner owner = hosted::blocksHostedEcology;
        runtime.setHostedEnvironmentOwner(owner);
        runtime.setHostedCaveOwner(owner);
        runtime.setIntrinsicEnvironmentOwner(owner);
        return runtime;
    }

    static void requireProjectedHostedContext(WorldgenContext context, HostedTerrainAccess hosted) {
        requireProjectedHostedContext(context, hosted, false);
    }

    static void requireProjectedNativeHostedContext(WorldgenContext context, HostedTerrainAccess hosted) {
        if (context == null || !context.getNativeHabitats()
            .shifted512())
            throw new IllegalStateException("Projected native factory requires its saved shifted revision");
        requireProjectedHostedContext(context, hosted, true);
    }

    private static void requireProjectedHostedContext(WorldgenContext context, HostedTerrainAccess hosted,
        boolean savedNative) {
        if (context == null || hosted == null
            || hosted.layout() != industries.angelic.biomic.api.ChunkVerticalLayout.EXTENDED_512
            || hosted.projection() != AuthoredVerticalProjections.shifted512()
            || hosted.minimumEcologyY() != 64)
            throw new IllegalArgumentException("Projected hosted mainland requires canonical shifted-512 terrain");
        if (!savedNative && (context.getNativeHabitats()
            .bop()
            || context.getNativeHabitats()
                .fullMarsh())
            || context.getCaveExpansion()
                .enabled()
                && !context.getCaveExpansion()
                    .shifted512()
            || context.getAlexCaves()
                .forlornEnabled()
            || context.getAlexCaves()
                .primordialEnabled()
            || context.getAlexCaves()
                .abyssalEnabled())
            throw new IllegalStateException(
                "Projected hosted mainland excludes unaudited BOP, marsh, cave and Alex adapters");
    }

    static OrigenFeatureHeightTransport requireProjectedTransport(OrigenFeatureTerrainAccess access) {
        if (access == null || access.heightTransport()
            .projection() != AuthoredVerticalProjections.shifted512()
            || access.heightTransport()
                .layout() != industries.angelic.biomic.api.ChunkVerticalLayout.EXTENDED_512)
            throw new IllegalArgumentException("Projected Origen features require the canonical shifted512 layout");
        return access.heightTransport();
    }

    private OrigenFeatureRuntime(WorldgenContext context, OrigenFeatureTerrainAccess terrainAccess,
        SurfaceProtection surfaceProtection) {
        this(context, terrainAccess, surfaceProtection, false);
    }

    private OrigenFeatureRuntime(WorldgenContext context, OrigenFeatureTerrainAccess terrainAccess,
        SurfaceProtection surfaceProtection, boolean projectedHosted) {
        if (context == null || terrainAccess == null || surfaceProtection == null)
            throw new IllegalArgumentException("Worldgen context and terrain access cannot be null");
        densityField = OrigenDensityField.shared(context);
        surfaceRuntime = OrigenSurfaceRuntime.shared(context);
        this.terrainAccess = terrainAccess;
        this.heightTransport = terrainAccess.heightTransport();
        this.surfaceProtection = surfaceProtection;
        this.mainlandHabitats = heightTransport.compressed() || projectedHosted ? new RegionalHabitatSampler(context)
            : null;
        this.fullNativeMarsh = heightTransport.compressed() && context.getNativeHabitats()
            .fullMarsh();
        this.physicalLichenSupport = context.getCaveExpansion()
            .lichenSupportRevision() == 1;
        this.repairProjectedLandPlants = context.getSettings()
            .getPlantProjectionRevision() == 1;
        this.nativeTreeAttachments = heightTransport.compressed() && context.getNativeHabitats()
            .bopHabitats()
            .stream()
            .anyMatch(industries.angelic.biomic.compat.biome.BopNativeBiomes::humidForest);
        this.nativeBogBushLeaves = heightTransport.compressed() && context.getNativeHabitats()
            .bopHabitats()
            .contains(industries.angelic.biomic.compat.biome.BopNativeBiomes.BOG);
        JsonObject manifest = loadManifest();
        compiler = new OrigenTerrainRuntime.Compiler(object(manifest, "functions"), object(manifest, "samplers"));
        loadStructures(object(manifest, "structures"));
        loadOres(object(manifest, "ores"));
        loadStages(array(manifest, "stages"));
        loadFeatures(object(manifest, "features"));
        loadProfiles(object(manifest, "profiles"));
        validateReferences();
        validateAuthoredBlockVocabulary(array(manifest, "authored_block_ids"));
    }

    private static OrigenFeatureTerrainAccess legacyAccess(WorldgenContext context, BiomicTerrainSampler terrain) {
        if (context == null || terrain == null)
            throw new IllegalArgumentException("Worldgen context and terrain sampler cannot be null");
        return OrigenFeatureTerrainAccess.legacy(context, terrain);
    }

    // ---- Order-independent population (docs/order-independent-population.md) ----

    /**
     * Paints a chunk's surface skins onto blocks resolved from its terrain, exactly as the provider does when it
     * provides the chunk (cave habitats, hosted soils, island skins). Never mutates the materials.
     */
    @FunctionalInterface
    public interface ProvidedSkins {

        void apply(int chunkX, int chunkZ, byte[] materials, Block[] blocks, byte[] metadata);
    }

    /** One chunk as provided, before any population: its terrain and the cells its surface skins changed. */
    private static final class ProvidedBaseline {

        final byte[] materials;
        final it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<WorldBlock> skins;
        /**
         * Each column's covering surface (providedSurface) and highest non-air cell (providedTop), when first asked.
         */
        int[] surface, top;

        ProvidedBaseline(byte[] materials, it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<WorldBlock> skins) {
            this.materials = materials;
            this.skins = skins;
        }
    }

    /**
     * One chunk's population as a journal: every Biomic pass reads the provided terrain and this journal, never the
     * world, and writes here. The provider then applies it (applyJournal): the population's own box (its corner plus
     * 8) wins over writes reaching in from neighbours, and among those a fixed order decides, so the world does not
     * depend on the order chunks were generated in.
     */
    private static final class PopulationJournal {

        final int chunkX, chunkZ;
        final it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<PendingBlock> writes = new it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<>();
        final List<PendingEntity> entities = new ArrayList<>();
        /** Each column's highest write: nothing this population wrote lies above it. */
        final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap columnTop = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();

        PopulationJournal(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            columnTop.defaultReturnValue(Integer.MIN_VALUE);
        }

        void put(long key, PendingBlock block) {
            writes.put(key, block);
            long column = horizontalKey(blockKeyX(key), blockKeyZ(key));
            int y = blockKeyY(key);
            if (y > columnTop.get(column)) columnTop.put(column, y);
        }

        boolean owns(int x, int z) {
            int minX = (chunkX << 4) + 8, minZ = (chunkZ << 4) + 8;
            return x >= minX && x < minX + 16 && z >= minZ && z < minZ + 16;
        }
    }

    /** Painting a baseline (raw terrain and skins) is most of a provided chunk's cost: keep a population's reach. */
    private static final int BASELINE_CACHE_SIZE = 256;
    /** One WorldBlock per block state, shared by every baseline's skins. */
    private final java.util.Map<Block, WorldBlock[]> skinStates = new java.util.IdentityHashMap<>();
    private final Long2ObjectLinkedOpenHashMap<ProvidedBaseline> baselines = new Long2ObjectLinkedOpenHashMap<>(
        BASELINE_CACHE_SIZE + 1);
    private ProvidedSkins providedSkins;
    private RawTerrainLookup baselineRawLookup;
    private PopulationJournal journal;
    /** Who wrote each cell a neighbour's population reached into (see applyJournal); persisted with the chunk. */
    final PopulationTags populationTags = new PopulationTags();
    private long journalDroppedWrites;

    /** Order-independent population on (the default); -Dbiomic.liveOrderPopulation=true restores the live path. */
    public static final boolean ORDER_INDEPENDENT = !Boolean.getBoolean("biomic.liveOrderPopulation");

    /** Turns on order-independent population for this runtime's provider. */
    public void enableJournaledPopulation(ProvidedSkins skins, RawTerrainLookup rawLookup) {
        this.providedSkins = skins;
        this.baselineRawLookup = rawLookup;
    }

    public boolean journaledPopulation() {
        return providedSkins != null;
    }

    /** The provider's own skinned blocks for a chunk it just provided, so the baseline need not be painted again. */
    public void rememberProvided(int chunkX, int chunkZ, byte[] materials, Block[] blocks, byte[] metadata) {
        if (providedSkins == null) return;
        ProvidedBaseline baseline = baselineOf(materials, blocks, metadata);
        baselinesRemembered++;
        synchronized (baselines) {
            baselines.putAndMoveToLast(horizontalKey(chunkX, chunkZ), baseline);
            if (baselines.size() > BASELINE_CACHE_SIZE) baselines.removeFirst();
        }
    }

    private long baselinesRemembered, baselinesPainted, baselineSkinCells;

    private ProvidedBaseline baselineOf(byte[] materials, Block[] blocks, byte[] metadata) {
        it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<WorldBlock> skins = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
        for (int index = 0; index < materials.length; index++) {
            WorldBlock terrain = resolveTerrainMaterial(materials[index]);
            Block block = blocks[index] == null ? net.minecraft.init.Blocks.air : blocks[index];
            int meta = metadata[index] & 15;
            if (block != terrain.block() || meta != terrain.metadata()) skins.put(index, skinState(block, meta));
        }
        baselineSkinCells += skins.size();
        return new ProvidedBaseline(materials, skins);
    }

    private WorldBlock skinState(Block block, int metadata) {
        synchronized (skinStates) {
            WorldBlock[] states = skinStates.computeIfAbsent(block, ignored -> new WorldBlock[16]);
            WorldBlock state = states[metadata];
            if (state == null) states[metadata] = state = new WorldBlock(block, metadata);
            return state;
        }
    }

    private record LastBaseline(long key, ProvidedBaseline baseline) {}

    /** Cell reads come in runs within one chunk: skip the shared LRU's monitor for them. */
    private volatile LastBaseline lastBaseline;

    private ProvidedBaseline baseline(int chunkX, int chunkZ) {
        long key = horizontalKey(chunkX, chunkZ);
        LastBaseline last = lastBaseline;
        if (last != null && last.key == key) return last.baseline;
        synchronized (baselines) {
            ProvidedBaseline known = baselines.getAndMoveToLast(key);
            if (known != null) {
                lastBaseline = new LastBaseline(key, known);
                return known;
            }
        }
        byte[] materials = rawTerrain(chunkX, chunkZ, baselineRawLookup);
        Block[] blocks = ChunkProviderBiomic.resolveBlocks(materials);
        byte[] metadata = ChunkProviderBiomic.resolveMetadata(materials);
        providedSkins.apply(chunkX, chunkZ, materials, blocks, metadata);
        ProvidedBaseline baseline = baselineOf(materials, blocks, metadata);
        baselinesPainted++;
        synchronized (baselines) {
            baselines.putAndMoveToLast(key, baseline);
            if (baselines.size() > BASELINE_CACHE_SIZE) baselines.removeFirst();
        }
        lastBaseline = new LastBaseline(key, baseline);
        return baseline;
    }

    /** A cell as its chunk was provided, before any population. */
    private WorldBlock baselineState(int x, int y, int z) {
        if (!heightTransport.containsPhysicalY(y)) return WorldBlock.AIR;
        ProvidedBaseline baseline = baseline(x >> 4, z >> 4);
        int index = heightTransport.physicalIndex(x & 15, y, z & 15);
        WorldBlock skin = baseline.skins.get(index);
        return skin != null ? skin : resolveTerrainMaterial(baseline.materials[index]);
    }

    /** Opens a chunk's population journal; every pass until applyJournal reads and writes through it. */
    public void beginJournal(int chunkX, int chunkZ) {
        if (providedSkins == null) return;
        if (journal != null) throw new IllegalStateException("Nested population journal");
        journal = new PopulationJournal(chunkX, chunkZ);
    }

    /** A cell as provided, before any population (box-confined structures read this under a journal). */
    public Block providedBlock(int x, int y, int z) {
        return baselineState(x, y, z).block();
    }

    public int providedMetadata(int x, int y, int z) {
        return baselineState(x, y, z).metadata();
    }

    /** What box-confined structures and other mods' generators see under order-independent population. */
    public final industries.angelic.biomic.world.structure.StructurePopulationBox.Terrain providedTerrain = new industries.angelic.biomic.world.structure.StructurePopulationBox.Terrain() {

        @Override
        public Block block(int x, int y, int z) {
            return providedBlock(x, y, z);
        }

        @Override
        public int metadata(int x, int y, int z) {
            return providedMetadata(x, y, z);
        }

        @Override
        public void owned(int x, int y, int z, Block block, int metadata) {
            ownedWrite(x, y, z, block, metadata);
        }

        @Override
        public int surface(int x, int z) {
            return providedSurface(x, z);
        }

        @Override
        public int top(int x, int z) {
            return providedTop(x, z);
        }
    };

    /** The highest provided cell of a column that covers what lies below it: opaque ground, not trees or plants. */
    public int providedSurface(int x, int z) {
        ProvidedBaseline baseline = baseline(x >> 4, z >> 4);
        int[] surface = baseline.surface;
        if (surface == null) {
            surface = new int[256];
            java.util.Arrays.fill(surface, Integer.MIN_VALUE);
            baseline.surface = surface;
        }
        int column = (z & 15) << 4 | x & 15;
        if (surface[column] == Integer.MIN_VALUE) surface[column] = columnSurface(baseline, x & 15, z & 15);
        return surface[column];
    }

    /** The highest non-air provided cell of a column, or -1. */
    public int providedTop(int x, int z) {
        ProvidedBaseline baseline = baseline(x >> 4, z >> 4);
        int[] top = baseline.top;
        if (top == null) {
            top = new int[256];
            java.util.Arrays.fill(top, Integer.MIN_VALUE);
            baseline.top = top;
        }
        int column = (z & 15) << 4 | x & 15;
        if (top[column] == Integer.MIN_VALUE) top[column] = columnTop(baseline, x & 15, z & 15);
        return top[column];
    }

    private int columnTop(ProvidedBaseline baseline, int localX, int localZ) {
        for (int y = heightTransport.layout()
            .height() - 1; y >= 0; y--) {
            int index = heightTransport.physicalIndex(localX, y, localZ);
            WorldBlock skin = baseline.skins.get(index);
            if (skin != null ? skin.block() != net.minecraft.init.Blocks.air
                : resolveTerrainMaterial(baseline.materials[index]).block() != net.minecraft.init.Blocks.air) return y;
        }
        return -1;
    }

    /** Per terrain material: 1 if it covers (see providedSurface), 2 if not, 0 not yet known. */
    private final byte[] materialCovers = new byte[256];

    private int columnSurface(ProvidedBaseline baseline, int localX, int localZ) {
        for (int y = heightTransport.layout()
            .height() - 1; y >= 0; y--) {
            int index = heightTransport.physicalIndex(localX, y, localZ);
            WorldBlock skin = baseline.skins.get(index);
            if (skin != null) {
                if (covers(skin.block())) return y;
                continue;
            }
            int material = baseline.materials[index] & 0xFF;
            if (materialCovers[material] == 0)
                materialCovers[material] = (byte) (covers(resolveTerrainMaterial((byte) material).block()) ? 1 : 2);
            if (materialCovers[material] == 1) return y;
        }
        return -1;
    }

    private static boolean covers(Block block) {
        Material material = block.getMaterial();
        return material.isSolid() && block.isOpaqueCube()
            && material != Material.leaves
            && material != Material.wood
            && material != Material.gourd;
    }

    /**
     * Forge's world-generator pass over a chunk (ChunkProviderServer#populate, after the provider's own): under
     * order-independent population other mods' generators are confined to the box and read it as provided.
     */
    public void generateForeign(World world, int chunkX, int chunkZ, Runnable generation) {
        if (!journaledPopulation()) generation.run();
        else industries.angelic.biomic.world.structure.StructurePopulationBox
            .runForeign(world, chunkX, chunkZ, providedTerrain, generation);
    }

    /**
     * Applies the open journal, bottom-up. A cell in the population's own box takes its write unless the cell no
     * longer holds the provided terrain and no neighbour's write is recorded there (a structure, or the world, changed
     * it). A cell outside takes the write only if it still holds the provided terrain, or holds a neighbour's write
     * that sorts after this one (lower chunk X, then Z, wins); cells of unloaded chunks queue it until they load. A
     * write the cell refuses claims nothing, and a cell whose write loses its ground gets back the best one it had
     * displaced (see applyEntries, restoreDisplaced).
     */
    public void applyJournal(World world) {
        PopulationJournal open = journal;
        if (open == null) return;
        if (populationView != null) throw new IllegalStateException("Population journal applied inside a population");
        // The applying view keeps the journal: support checks in placeInChunk see what the passes saw.
        populationView = new PopulationView(world, open.chunkX, open.chunkZ, null, null, null);
        journal = null;
        try {
            long writer = horizontalKey(open.chunkX, open.chunkZ);
            List<ApplyEntry> entries = new ArrayList<>(open.writes.size());
            for (long key : bottomUp(
                open.writes.keySet()
                    .toLongArray())) {
                PendingBlock block = open.writes.get(key);
                int x = blockKeyX(key), y = blockKeyY(key), z = blockKeyZ(key);
                boolean own = open.owns(x, z);
                Chunk chunk = populationView.loadedChunk(x >> 4, z >> 4);
                if (CellTrace.ON) CellTrace.log(
                    x,
                    y,
                    z,
                    "applying chunk {},{}'s journal: {} ({}, target chunk {})",
                    open.chunkX,
                    open.chunkZ,
                    block.state(),
                    own ? "own box" : "reaching in",
                    chunk == null ? "not loaded: queued" : "loaded");
                if (chunk == null) {
                    if (own) throw new IllegalStateException("A population's own box is not loaded");
                    populationView.pendingData.journalOverflow
                        .computeIfAbsent(horizontalKey(x >> 4, z >> 4), ignored -> new ArrayList<>())
                        .add(new PendingPlacement(x, y, z, block, writer));
                    populationView.pendingData.markDirty();
                    continue;
                }
                entries.add(new ApplyEntry(x, y, z, block, own ? PopulationTags.OWNER : writer));
            }
            applyEntries(world, entries);
            for (PendingEntity entity : open.entities) {
                if (!open.owns(entity.x(), entity.z())) continue;
                spawnEntityNow(world, entity.x(), entity.y(), entity.z(), entity.id());
            }
        } finally {
            populationView.finish();
            populationView = null;
        }
    }

    /** A plant that lost its ground lost its writer's claim to the cell too (population tags). */
    private void voidClaim(World world, Chunk chunk, int x, int y, int z) {
        if (providedSkins == null) return;
        if (CellTrace.ON) CellTrace.log(x, y, z, "claim voided (lost its ground), cell {}", cellOf(chunk, x, y, z));
        populationTags.remove(horizontalKey(chunk.xPosition, chunk.zPosition), blockKey(x, y, z));
        restoreDisplaced(world, chunk, x, y, z);
    }

    /** Cells bottom-up (stable within a layer): a population's own ground lands before what stands on it. */
    private static long[] bottomUp(long[] keys) {
        it.unimi.dsi.fastutil.longs.LongArrays.mergeSort(keys, (a, b) -> Integer.compare(blockKeyY(a), blockKeyY(b)));
        return keys;
    }

    /** Drops an open journal after a failed population, so the next one starts clean. */
    public void closeJournal() {
        journal = null;
    }

    private final it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet deferredOverflow = new it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet();

    /** Chunks that loaded during a population get their queued writes once it is over. */
    public void applyDeferredOverflow(World world) {
        if (populationView != null) return;
        while (!deferredOverflow.isEmpty()) {
            long key = deferredOverflow.removeFirstLong();
            int chunkX = (int) (key >> 32), chunkZ = (int) key;
            if (!world.getChunkProvider()
                .chunkExists(chunkX, chunkZ)) continue;
            applyPendingOverflow(world, world.getChunkFromChunkCoords(chunkX, chunkZ));
        }
    }

    /** Writes queued for a chunk that was not loaded, applied by the same rule when it loads. */
    public void applyPendingOverflow(World world, Chunk chunk) {
        if (providedSkins == null) return;
        if (populationView != null) {
            deferredOverflow.add(horizontalKey(chunk.xPosition, chunk.zPosition));
            return;
        }
        PendingFeatureData data = PendingFeatureData.load(world);
        long chunkKey = horizontalKey(chunk.xPosition, chunk.zPosition);
        List<PendingPlacement> placements = data.journalOverflow.remove(chunkKey);
        if (placements == null) return;
        data.markDirty();
        // Bottom-up, and within a cell the losing writers first: each later one replaces the cell only if it wins it
        // and the cell takes it, so the cell ends with the best write that lands, as when the writes came in live.
        placements.sort(
            (a, b) -> a.y() != b.y() ? Integer.compare(a.y(), b.y())
                : PopulationTags.beats(a.writer(), b.writer()) ? 1
                    : PopulationTags.beats(b.writer(), a.writer()) ? -1 : 0);
        populationView = new PopulationView(world, chunk.xPosition, chunk.zPosition, null, null, null);
        try {
            List<ApplyEntry> entries = new ArrayList<>(placements.size());
            for (PendingPlacement placement : placements) {
                if (CellTrace.ON) CellTrace.log(
                    placement.x(),
                    placement.y(),
                    placement.z(),
                    "replaying a queued write as chunk {},{} loads: {} from writer {}",
                    chunk.xPosition,
                    chunk.zPosition,
                    placement.block()
                        .state(),
                    placement.writer());
                entries.add(
                    new ApplyEntry(placement.x(), placement.y(), placement.z(), placement.block(), placement.writer()));
            }
            applyEntries(world, entries);
        } finally {
            populationView.finish();
            populationView = null;
        }
    }

    /**
     * Whether the population rule lets {@code writer} have a cell now (see applyJournal). Kind comes first: a plant
     * (soft) never displaces a log or a block another population put in the cell, and such a block always displaces a
     * plant, whoever owns the cell; each population only sees its own writes, so the owner's grass, planted where it
     * saw air, cut a neighbour's tree trunk (review, 2026-10-24: "why isn't the tree connected to the ground?"). Among
     * writes of one kind the owner wins, then the fixed order between neighbours.
     */
    private boolean admits(Chunk chunk, int x, int y, int z, long writer, boolean soft) {
        long tag = populationTags.get(horizontalKey(chunk.xPosition, chunk.zPosition), blockKey(x, y, z));
        WorldBlock current = cellOf(chunk, x, y, z);
        // Another population's write stands here: a neighbour's (tagged), or the owner's (settled off the baseline).
        boolean written = tag != PopulationTags.NONE ? tag != PopulationTags.OWNER
            : !current.equals(baselineState(x, y, z));
        if (written && soft != soft(current.block())) return !soft;
        if (writer == PopulationTags.OWNER) return tag != PopulationTags.NONE || current.equals(baselineState(x, y, z));
        if (tag == PopulationTags.OWNER) return false;
        if (tag != PopulationTags.NONE) return PopulationTags.beats(writer, tag);
        return current.equals(baselineState(x, y, z));
    }

    /** A plant, vine or snow layer: a write that gives way to any log or block (see admits). */
    private static boolean soft(Block block) {
        Material material = block.getMaterial();
        return block instanceof net.minecraft.block.BlockBush || material == Material.plants
            || material == Material.vine
            || material == Material.snow;
    }

    private boolean soft(PendingBlock block) {
        return soft(resolve(block.state()).block());
    }

    private static WorldBlock cellOf(Chunk chunk, int x, int y, int z) {
        return new WorldBlock(chunk.getBlock(x & 15, y, z & 15), chunk.getBlockMetadata(x & 15, y, z & 15));
    }

    /** Applies one write by the population rule; whether it landed (the cell took it, or already held it). */
    private boolean applyWrite(World world, Chunk chunk, int x, int y, int z, PendingBlock block, long writer) {
        if (!admits(chunk, x, y, z, writer, soft(block))) {
            long chunkKey = horizontalKey(chunk.xPosition, chunk.zPosition), cell = blockKey(x, y, z);
            long tag = populationTags.get(chunkKey, cell);
            if (CellTrace.ON) CellTrace.log(
                x,
                y,
                z,
                "refused {} from writer {}: cell {} tag {} baseline {}",
                block.state(),
                writer,
                cellOf(chunk, x, y, z),
                tag,
                baselineState(x, y, z));
            // Lost to a write that stands here, the owner's or another neighbour's: if that one goes (a two-block plant
            // broken by a later write, a plant whose ground went), this one is a candidate again, as when it had come
            // first and been displaced.
            WorldBlock standing = cellOf(chunk, x, y, z);
            if (writer != PopulationTags.OWNER && tag != writer
                && (tag != PopulationTags.NONE || !standing.equals(baselineState(x, y, z))))
                populationTags.displace(chunkKey, cell, writer, block.state());
            journalDroppedWrites++;
            return false;
        }
        WorldBlock current = cellOf(chunk, x, y, z);
        placeInChunk(world, chunk, x, y, z, block);
        WorldBlock now = cellOf(chunk, x, y, z);
        if (CellTrace.ON) CellTrace.log(
            x,
            y,
            z,
            "admitted {} from writer {}: {} -> {} (below {})",
            block.state(),
            writer,
            current,
            now,
            cellOf(chunk, x, y - 1, z));
        if (current.block() instanceof net.minecraft.block.BlockDoublePlant
            || now.block() instanceof net.minecraft.block.BlockDoublePlant) pairCells.add(blockKey(x, y, z));
        var resolved = resolve(block.state());
        // A write the cell refused (a plant on ground that cannot carry it) claims nothing: it would beat, in one
        // order only, another population's write that did land.
        if (now.equals(current) && (now.block() != resolved.block() || now.metadata() != resolved.metadata())) {
            journalDroppedWrites++;
            return false;
        }
        long chunkKey = horizontalKey(chunk.xPosition, chunk.zPosition), cell = blockKey(x, y, z);
        if (writer != PopulationTags.OWNER) {
            long displaced = populationTags.get(chunkKey, cell);
            if (displaced != PopulationTags.NONE && displaced != PopulationTags.OWNER
                && displaced != writer
                && !now.equals(current))
                populationTags.displace(
                    chunkKey,
                    cell,
                    displaced,
                    OrigenBlockStateResolver.placed(current.block(), current.metadata()));
            populationTags.put(chunkKey, cell, writer);
            return true;
        }
        // The owner's write wins the cell, but a neighbour's write it displaced stays a candidate: the owner's own
        // two-block plant can still be broken by a later write, and then the cell must end as in the other order.
        long displaced = populationTags.get(chunkKey, cell);
        if (displaced != PopulationTags.NONE && displaced != PopulationTags.OWNER && !now.equals(current))
            populationTags.displace(
                chunkKey,
                cell,
                displaced,
                OrigenBlockStateResolver.placed(current.block(), current.metadata()));
        if (now.equals(baselineState(x, y, z))) populationTags.put(chunkKey, cell, PopulationTags.OWNER);
        else populationTags.remove(chunkKey, cell);
        return true;
    }

    /**
     * A cell's write is gone (the ground under it went to another population's write, or its other half did): the
     * best write it had displaced that still stands takes the cell, as when the writes had come in the other order.
     */
    private void restoreDisplaced(World world, Chunk chunk, int x, int y, int z) {
        long chunkKey = horizontalKey(chunk.xPosition, chunk.zPosition), cell = blockKey(x, y, z);
        List<PopulationTags.Claim> displaced = populationTags.displaced(chunkKey, cell);
        for (int i = 0; i < displaced.size(); i++) {
            PopulationTags.Claim claim = displaced.get(i);
            if (!applyWrite(world, chunk, x, y, z, new PendingBlock(claim.state(), false), claim.writer())) continue;
            // The rest stay displaced, now by this one.
            for (int j = i + 1; j < displaced.size(); j++) populationTags.displace(
                chunkKey,
                cell,
                displaced.get(j)
                    .writer(),
                displaced.get(j)
                    .state());
            return;
        }
    }

    private record ApplyEntry(int x, int y, int z, PendingBlock block, long writer) {}

    /** Cells where a write put or took a two-block plant's half: only around them can a pair be broken. */
    private final it.unimi.dsi.fastutil.longs.LongArrayList pairCells = new it.unimi.dsi.fastutil.longs.LongArrayList();

    /**
     * Applies writes in the given order (bottom-up; a cell's losing writers first), a two-block plant's halves
     * together: both cells must admit them, and if the upper half is refused the lower cell gets back what it held.
     * Half a plant let in alone was later settled back to the provided terrain, losing what it had displaced, in one
     * order only.
     */
    private void applyEntries(World world, List<ApplyEntry> entries) {
        pairCells.clear();
        java.util.Set<ApplyEntry> paired = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (ApplyEntry entry : entries) {
            if (paired.contains(entry)) continue;
            Chunk chunk = world.getChunkFromChunkCoords(entry.x() >> 4, entry.z() >> 4);
            ApplyEntry upper = upperHalf(entries, entry);
            if (CellTrace.ON) CellTrace.log(
                entry.x(),
                entry.y(),
                entry.z(),
                "apply {} from writer {}: upper half {}",
                entry.block()
                    .state(),
                entry.writer(),
                upper == null ? "none in this batch"
                    : upper.block()
                        .state() + " admitted "
                        + admits(chunk, upper.x(), upper.y(), upper.z(), upper.writer(), soft(upper.block()))
                        + ", lower admitted "
                        + admits(chunk, entry.x(), entry.y(), entry.z(), entry.writer(), soft(entry.block())));
            if (upper == null) {
                applyWrite(world, chunk, entry.x(), entry.y(), entry.z(), entry.block(), entry.writer());
                continue;
            }
            paired.add(upper);
            if (!admits(chunk, entry.x(), entry.y(), entry.z(), entry.writer(), soft(entry.block()))
                || !admits(chunk, upper.x(), upper.y(), upper.z(), upper.writer(), soft(upper.block()))) {
                journalDroppedWrites += 2;
                continue;
            }
            long chunkKey = horizontalKey(chunk.xPosition, chunk.zPosition),
                lowerCell = blockKey(entry.x(), entry.y(), entry.z());
            WorldBlock before = cellOf(chunk, entry.x(), entry.y(), entry.z());
            long beforeTag = populationTags.get(chunkKey, lowerCell);
            if (!applyWrite(world, chunk, entry.x(), entry.y(), entry.z(), entry.block(), entry.writer())) {
                journalDroppedWrites++;
                continue;
            }
            if (applyWrite(world, chunk, upper.x(), upper.y(), upper.z(), upper.block(), upper.writer())) continue;
            placeInChunk(
                world,
                chunk,
                entry.x(),
                entry.y(),
                entry.z(),
                OrigenBlockStateResolver
                    .imported(Block.blockRegistry.getNameForObject(before.block()), before.block(), before.metadata()));
            if (beforeTag == PopulationTags.NONE) populationTags.remove(chunkKey, lowerCell);
            else populationTags.put(chunkKey, lowerCell, beforeTag);
        }
        long[] cells = pairCells.toLongArray();
        for (long cell : cells) settlePairs(world, blockKeyX(cell), blockKeyY(cell), blockKeyZ(cell));
    }

    /** The same writer's upper half over a two-block plant's lower half, or null. */
    private ApplyEntry upperHalf(List<ApplyEntry> entries, ApplyEntry lower) {
        var resolved = resolve(
            lower.block()
                .state());
        if (!(resolved.block() instanceof net.minecraft.block.BlockDoublePlant) || (resolved.metadata() & 8) != 0)
            return null;
        for (ApplyEntry entry : entries) if (entry.x() == lower.x() && entry.z() == lower.z()
            && entry.y() == lower.y() + 1
            && entry.writer() == lower.writer()
            && resolve(
                entry.block()
                    .state()).block()
                == resolved.block())
            return entry;
        return null;
    }

    /**
     * A two-block plant whose other half another population's write displaced (or never let in) goes back to the
     * provided terrain, half by half, whichever order the two populations came in: kept, it stood as half a plant in
     * one order and fell to a block update in the other.
     */
    private void settlePairs(World world, int x, int y, int z) {
        if (!world.getChunkProvider()
            .chunkExists(x >> 4, z >> 4)) return;
        for (int cellY = y - 1; cellY <= y + 1; cellY++) {
            if (!heightTransport.containsPhysicalY(cellY)) continue;
            Block block = world.getBlock(x, cellY, z);
            if (!(block instanceof net.minecraft.block.BlockDoublePlant)) continue;
            boolean upper = (world.getBlockMetadata(x, cellY, z) & 8) != 0;
            int partnerY = upper ? cellY - 1 : cellY + 1;
            if (heightTransport.containsPhysicalY(partnerY) && world.getBlock(x, partnerY, z) == block
                && (world.getBlockMetadata(x, partnerY, z) & 8) != 0 != upper) continue;
            WorldBlock provided = baselineState(x, cellY, z);
            world.setBlock(x, cellY, z, provided.block(), provided.metadata(), 2);
            Chunk chunk = world.getChunkFromChunkCoords(x >> 4, z >> 4);
            populationTags.remove(horizontalKey(x >> 4, z >> 4), blockKey(x, cellY, z));
            restoreDisplaced(world, chunk, x, cellY, z);
        }
    }

    /**
     * A box-confined generator (village, mineshaft, foreign mod) wrote this cell of its own box: no neighbour may.
     * A cell that now differs from the provided terrain and carries no tag already says so.
     */
    public void ownedWrite(int x, int y, int z, Block block, int metadata) {
        if (providedSkins == null || !heightTransport.containsPhysicalY(y)) return;
        long chunkKey = horizontalKey(x >> 4, z >> 4), cell = blockKey(x, y, z);
        WorldBlock provided = baselineState(x, y, z);
        if (populationTags.get(chunkKey, cell) != PopulationTags.NONE
            || provided.block() == block && provided.metadata() == (metadata & 15))
            populationTags.put(chunkKey, cell, PopulationTags.OWNER);
    }

    /** Persists the chunk's population tags with it (ChunkDataEvent). */
    public void savePopulationTags(int chunkX, int chunkZ, NBTTagCompound level) {
        populationTags.save(horizontalKey(chunkX, chunkZ), level);
    }

    public void loadPopulationTags(int chunkX, int chunkZ, NBTTagCompound level) {
        populationTags.load(horizontalKey(chunkX, chunkZ), level);
    }

    public void unloadPopulationTags(int chunkX, int chunkZ) {
        populationTags.unload(horizontalKey(chunkX, chunkZ));
    }

    /**
     * Retains the compact, pre-feature terrain that was already calculated for a real chunk. Population frequently
     * needs to distinguish an earlier feature write from projected base terrain; keeping the compact layout-sized
     * material array avoids regenerating that chunk and avoids the much larger Block[] representation.
     */
    public void rememberRawTerrain(int chunkX, int chunkZ, byte[] materials) {
        validatePhysicalTerrainPayload(materials);
        synchronized (rawTerrainCache) {
            rawTerrainCache.putAndMoveToLast(horizontalKey(chunkX, chunkZ), materials);
            if (rawTerrainCache.size() > RAW_TERRAIN_CACHE_SIZE) rawTerrainCache.removeFirst();
        }
    }

    /**
     * Borrows an already retained immutable physical-material array for another hosted view. A miss returns null;
     * this never computes terrain, queries a World, or changes cache eviction order. Callers must not modify it.
     */
    public byte[] peekRawTerrain(int chunkX, int chunkZ) {
        synchronized (rawTerrainCache) {
            return rawTerrainCache.get(horizontalKey(chunkX, chunkZ));
        }
    }

    /**
     * The chunk's pre-feature terrain: retained, borrowed from {@code lookup}, or computed again. Population passes use
     * this rather than {@link #peekRawTerrain}, whose answer depends on how many chunks generated since.
     */
    public byte[] rawTerrain(int chunkX, int chunkZ, RawTerrainLookup lookup) {
        long key = horizontalKey(chunkX, chunkZ);
        synchronized (rawTerrainCache) {
            byte[] cached = rawTerrainCache.getAndMoveToLast(key);
            if (cached != null) {
                rawTerrainCacheHits++;
                return cached;
            }
        }
        byte[] recovered = lookup == null ? null : lookup.find(chunkX, chunkZ);
        if (recovered != null) validatePhysicalTerrainPayload(recovered);
        if (recovered != null) {
            synchronized (rawTerrainCache) {
                byte[] raced = rawTerrainCache.getAndMoveToLast(key);
                if (raced != null) {
                    rawTerrainCacheHits++;
                    return raced;
                }
                rawTerrainCache.putAndMoveToLast(key, recovered);
                if (rawTerrainCache.size() > RAW_TERRAIN_CACHE_SIZE) rawTerrainCache.removeFirst();
            }
            rawTerrainPayloadRecoveries++;
            return recovered;
        }
        rawTerrainCacheMisses++;
        long started = System.nanoTime();
        byte[] materials = terrainAccess.createPhysicalMaterials(chunkX, chunkZ);
        industries.angelic.biomic.world.SlowSteps.note("raw chunk", System.nanoTime() - started);
        validatePhysicalTerrainPayload(materials);
        synchronized (rawTerrainCache) {
            byte[] raced = rawTerrainCache.getAndMoveToLast(key);
            if (raced != null) return raced;
            rawTerrainCache.putAndMoveToLast(key, materials);
            if (rawTerrainCache.size() > RAW_TERRAIN_CACHE_SIZE) rawTerrainCache.removeFirst();
        }
        return materials;
    }

    private byte[] validatePhysicalTerrainPayload(byte[] materials) {
        if (materials == null || materials.length != heightTransport.layout()
            .blockCount())
            throw new IllegalArgumentException("Raw Origen terrain does not match the selected physical layout");
        return materials;
    }

    /** Re-pins the authored-height companion when its speculative terrain payload becomes a demanded chunk. */
    public void rememberAuthoredTerrain(int chunkX, int chunkZ, byte[] authoredMaterials) {
        surfaceRuntime
            .rememberAuthoredChunk(chunkX, chunkZ, OrigenFeatureTerrainAccess.requireAuthoredChunk(authoredMaterials));
    }

    public String performanceReport() {
        long authoredRequests = authoredTerrainCacheHits + authoredTerrainPayloadRecoveries
            + authoredTerrainCacheMisses;
        long rawRequests = rawTerrainCacheHits + rawTerrainPayloadRecoveries + rawTerrainCacheMisses;
        double authoredHitRate = authoredRequests == 0L ? 0.0D
            : (authoredTerrainCacheHits + authoredTerrainPayloadRecoveries) * 100.0D / authoredRequests;
        double rawHitRate = rawRequests == 0L ? 0.0D
            : (rawTerrainCacheHits + rawTerrainPayloadRecoveries) * 100.0D / rawRequests;
        return String.format(
            java.util.Locale.ROOT,
            "Origen population caches: authored %d hits/%d payload/%d misses (%.1f%%; %d loaded/%d unloaded; "
                + "distance 0/1/2/3+ = %d/%d/%d/%d), %d column fallbacks; "
                + "raw %d hits/%d payload/%d misses (%.1f%%); %d cave-mask payloads; baselines %d remembered/%d painted"
                + " (%d skin cells avg); %d journal writes refused",
            authoredTerrainCacheHits,
            authoredTerrainPayloadRecoveries,
            authoredTerrainCacheMisses,
            authoredHitRate,
            authoredTerrainLoadedMisses,
            authoredTerrainUnloadedMisses,
            authoredTerrainMissDistance[0],
            authoredTerrainMissDistance[1],
            authoredTerrainMissDistance[2],
            authoredTerrainMissDistance[3],
            authoredColumnFallbacks,
            rawTerrainCacheHits,
            rawTerrainPayloadRecoveries,
            rawTerrainCacheMisses,
            rawHitRate,
            caveProjectionPayloadRecoveries,
            baselinesRemembered,
            baselinesPainted,
            baselinesRemembered + baselinesPainted == 0 ? 0
                : baselineSkinCells / (baselinesRemembered + baselinesPainted),
            journalDroppedWrites);
    }

    public String terraScriptProfileReport(int limit) {
        return OrigenTerraScript.profileReport(limit);
    }

    public String featureProfileReport(int limit) {
        if (!PROFILE_FEATURES || featureProfiles.isEmpty()) return null;
        List<Map.Entry<String, FeatureProfile>> entries = new ArrayList<>(featureProfiles.entrySet());
        entries.sort((left, right) -> Long.compare(right.getValue().totalNanos, left.getValue().totalNanos));
        StringBuilder report = new StringBuilder("Origen feature profile:");
        int count = Math.min(Math.max(0, limit), entries.size());
        for (int i = 0; i < count; i++) {
            Map.Entry<String, FeatureProfile> entry = entries.get(i);
            FeatureProfile profile = entry.getValue();
            report.append('\n')
                .append("  ")
                .append(entry.getKey())
                .append(": ")
                .append(String.format(java.util.Locale.ROOT, "%.3f ms", profile.totalNanos / 1_000_000.0D))
                .append(" (")
                .append(profile.distributorHits)
                .append(" hits, ")
                .append(profile.anchors)
                .append(" anchors; locator ")
                .append(String.format(java.util.Locale.ROOT, "%.3f", profile.locatorNanos / 1_000_000.0D))
                .append(" ms, structures ")
                .append(String.format(java.util.Locale.ROOT, "%.3f", profile.structureNanos / 1_000_000.0D))
                .append(" ms)");
        }
        return report.toString();
    }

    public void resetPerformanceStats() {
        OrigenTerraScript.resetProfile();
        if (PROFILE_FEATURES) featureProfiles.clear();
        authoredTerrainCacheHits = 0L;
        authoredTerrainCacheMisses = 0L;
        authoredTerrainPayloadRecoveries = 0L;
        authoredTerrainLoadedMisses = 0L;
        authoredTerrainUnloadedMisses = 0L;
        java.util.Arrays.fill(authoredTerrainMissDistance, 0L);
        authoredColumnFallbacks = 0L;
        rawTerrainCacheHits = 0L;
        rawTerrainPayloadRecoveries = 0L;
        rawTerrainCacheMisses = 0L;
        caveProjectionPayloadRecoveries = 0L;
    }

    public void populate(World world, int chunkX, int chunkZ, AuthoredTerrainLookup authoredTerrainLookup,
        RawTerrainLookup rawTerrainLookup, CaveProjectionLookup caveProjectionLookup) {
        if (populationView != null) throw new IllegalStateException("Nested Origen population is not supported");
        populationView = new PopulationView(
            world,
            chunkX,
            chunkZ,
            authoredTerrainLookup,
            rawTerrainLookup,
            caveProjectionLookup);
        try {
            populationView.flush(chunkX, chunkZ);
            populateStages(world, chunkX, chunkZ);
            populationView.pendingData.hostedTrees.sourceComplete(chunkX, chunkZ);
            populationView.pendingData.markDirty();
        } finally {
            populationView.finish();
            populationView = null;
        }
    }

    @FunctionalInterface
    public interface AuthoredTerrainLookup {

        byte[] find(int chunkX, int chunkZ);
    }

    /** Non-blocking lookup for an exact, completed material payload in the selected physical layout. */
    @FunctionalInterface
    public interface RawTerrainLookup {

        byte[] find(int chunkX, int chunkZ);
    }

    /** Non-blocking lookup for the exact completed cave projection paired with a terrain payload. */
    @FunctionalInterface
    public interface CaveProjectionLookup {

        BitSet find(int chunkX, int chunkZ);
    }

    /** A populated chunk loaded from disk is also a completed neighboring mushroom source. */
    public void nativeTreeSourceComplete(World world, int chunkX, int chunkZ) {
        PendingFeatureData data = PendingFeatureData.load(world);
        data.hostedTrees.sourceComplete(chunkX, chunkZ);
        data.markDirty();
    }

    /** Reuse the settled, persistent physical write path without running Origen's authored feature graph again. */
    public void withPhysicalPopulation(World world, int chunkX, int chunkZ,
        java.util.function.Consumer<PhysicalPopulation> action) {
        if (populationView != null) throw new IllegalStateException("Nested physical population");
        populationView = new PopulationView(world, chunkX, chunkZ, null, null, null);
        try {
            populationView.flush(chunkX, chunkZ);
            hostedPhysicalPopulation = true;
            action.accept(new PhysicalPopulation() {

                @Override
                public Block block(int x, int y, int z) {
                    return readPhysical(world, x, y, z).block();
                }

                @Override
                public int metadata(int x, int y, int z) {
                    return readPhysical(world, x, y, z).metadata();
                }

                @Override
                public int topCandidateY(int x, int z) {
                    // Journaled: above the provided column's top and this population's own writes there is only air.
                    if (populationView.journal != null)
                        return Math.max(providedTop(x, z), populationView.journal.columnTop.get(horizontalKey(x, z)));
                    Chunk loaded = populationView.loadedChunk(x >> 4, z >> 4);
                    return loaded == null ? Integer.MAX_VALUE : loaded.getTopFilledSegment() + 15;
                }

                @Override
                public boolean isMaterialized(int x, int y, int z) {
                    if (populationView.isLoaded(x >> 4, z >> 4)) return true;
                    LinkedHashMap<Long, PendingPlacement> pending = populationView.pendingData.pendingPlacements
                        .get(horizontalKey(x >> 4, z >> 4));
                    return pending != null && pending.containsKey(blockKey(x, y, z));
                }

                @Override
                public String naturalCarrierRejection(int x, int y, int z, Block block, int metadata) {
                    return populationView.naturalCarrierRejection(x, y, z, block, metadata);
                }

                @Override
                public String freshNaturalSoilRejection(int x, int y, int z, Block block, int metadata) {
                    return populationView.freshNaturalSoilRejection(x, y, z, block, metadata);
                }

                @Override
                public String blockState(int x, int y, int z) {
                    WorldBlock block = readPhysical(world, x, y, z);
                    if (!industries.angelic.biomic.world.modern.ModernStateCompatibility
                        .requiresBelowContext(block.block(), block.metadata()))
                        return industries.angelic.biomic.world.modern.ModernStateCompatibility
                            .modernState(block.block(), block.metadata());
                    WorldBlock below = readPhysical(world, x, y - 1, z);
                    return industries.angelic.biomic.world.modern.ModernStateCompatibility
                        .modernState(block.block(), block.metadata(), below.block(), below.metadata());
                }

                @Override
                public byte rawMaterial(int x, int y, int z) {
                    if (!heightTransport.containsPhysicalY(y)) return BiomicTerrainSampler.AIR;
                    return populationView.createRawTerrain(x >> 4, z >> 4)[heightTransport
                        .physicalIndex(x & 15, y, z & 15)];
                }

                @Override
                public void setBlock(int x, int y, int z, String state) {
                    writePhysical(world, x, y, z, new PendingBlock(state, false));
                }

                @Override
                public boolean canResolve(String state) {
                    try {
                        return resolve(state) != null;
                    } catch (IllegalArgumentException unsupported) {
                        return false;
                    }
                }

                @Override
                public void setChest(int x, int y, int z, String state, NBTTagCompound chestNbt) {
                    validateChestPayload(state, chestNbt, x, y, z);
                    writePhysical(world, x, y, z, new PendingBlock(state, false, null, chestNbt));
                }

                @Override
                public void commitNativeTree(
                    Map<industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos, String> writes) {
                    Map<industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos, String> exact = new LinkedHashMap<>();
                    writes.forEach((p, state) -> {
                        var block = resolve(state);
                        exact.put(p, treeState(block.block(), block.metadata()));
                    });
                    if (populationView.journal != null) {
                        // Journaled: the tree is the population's own write; no cross-chunk ledger.
                        exact
                            .forEach((p, state) -> populationView.write(p.x, p.y, p.z, new PendingBlock(state, false)));
                        return;
                    }
                    populationView.treeJournalWrite = true;
                    try {
                        populationView.pendingData.hostedTrees.commit(
                            exact,
                            populationView.treeAccess(),
                            (cx, cz) -> cx == chunkX && cz == chunkZ || populationView.isPopulated(cx, cz));
                        populationView.pendingData.markDirty();
                    } finally {
                        populationView.treeJournalWrite = false;
                    }
                }
            });
        } finally {
            hostedPhysicalPopulation = false;
            populationView.finish();
            populationView = null;
        }
    }

    public interface PhysicalPopulation {

        /** True for an actual loaded block or a queued write, false for synthesized raw terrain. */
        boolean isMaterialized(int x, int y, int z);

        /** No materialized non-air block of this column lies above this Y (MAX_VALUE: unknown). */
        default int topCandidateY(int x, int z) {
            return Integer.MAX_VALUE;
        }

        default String naturalCarrierRejection(int x, int y, int z, Block block, int metadata) {
            return "unsupported-physical-population";
        }

        /** Chronology proof for a caller-whitelisted hosted-soil substitution; null means admitted. */
        default String freshNaturalSoilRejection(int x, int y, int z, Block block, int metadata) {
            return "unsupported-physical-population";
        }

        default Block block(int x, int y, int z) {
            return industries.angelic.biomic.world.modern.ModernStateCompatibility.resolve(blockState(x, y, z))
                .getBlock();
        }

        String blockState(int x, int y, int z);

        default int metadata(int x, int y, int z) {
            return industries.angelic.biomic.world.modern.ModernStateCompatibility.resolve(blockState(x, y, z))
                .getMetadata();
        }

        byte rawMaterial(int x, int y, int z);

        default boolean canResolve(String state) {
            return false;
        }

        void setBlock(int x, int y, int z, String state);

        /**
         * Exact, already-generated inventory payload. Implementations must never roll the loot table again. Also
         * carries an EFR bee nest's bees.
         */
        default void setChest(int x, int y, int z, String state, NBTTagCompound chestNbt) {
            throw new UnsupportedOperationException("This physical population cannot persist chest payloads");
        }

        /** Prevalidates every tile payload before the first camp block is queued. */
        default void commitSaltyCamp(industries.angelic.biomic.compat.biome.SaltyBrickmakerCamp.Plan plan) {
            for (var entry : plan.placements()
                .entrySet()) {
                var p = entry.getKey();
                var placement = entry.getValue();
                if (placement.hasTileEntity())
                    validateChestPayload(placement.state(), placement.tileEntity(), p.x(), p.y(), p.z());
            }
            for (var entry : plan.placements()
                .entrySet()) {
                var p = entry.getKey();
                var placement = entry.getValue();
                if (placement.hasTileEntity()) setChest(p.x(), p.y(), p.z(), placement.state(), placement.tileEntity());
                else setBlock(p.x(), p.y(), p.z(), placement.state());
            }
        }

        default void commitNativeTree(
            Map<industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos, String> writes) {
            writes.forEach((p, state) -> setBlock(p.x, p.y, p.z, state));
        }
    }

    @FunctionalInterface
    public interface SurfaceProtection {

        /**
         * Whether a surface feature may not root at a column: a village's pieces or a scattered structure at its
         * height; {@code bulky} for trees and landforms, which keep farther off than plants.
         */
        boolean contains(int blockX, int physicalY, int blockZ, boolean bulky);

        /** Whether protection lies within about {@code reach} blocks: a feature rooted there runs as a whole. */
        default boolean near(int blockX, int physicalY, int blockZ, int reach) {
            int step = Math.max(1, reach / 2);
            for (int dx = -reach; dx <= reach; dx += step) for (int dz = -reach; dz <= reach; dz += step)
                if (contains(blockX + dx, physicalY, blockZ + dz, true)) return true;
            return false;
        }
    }

    private void populateStages(World world, int chunkX, int chunkZ) {
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;
        @SuppressWarnings("unchecked")
        List<ProfileRange>[] ranges = (List<ProfileRange>[]) new List<?>[16];
        for (int tileX = 0; tileX < 16; tileX += STAGE_RESOLUTION) {
            for (int tileZ = 0; tileZ < 16; tileZ += STAGE_RESOLUTION) {
                int x = startX + tileX;
                int z = startZ + tileZ;
                ranges[stageTileIndex(tileX, tileZ)] = profileRanges(x, z);
            }
        }
        for (int stageOrdinal = 0; stageOrdinal < stages.size(); stageOrdinal++) {
            String stage = stages.get(stageOrdinal);
            for (int tileX = 0; tileX < 16; tileX += STAGE_RESOLUTION) {
                for (int tileZ = 0; tileZ < 16; tileZ += STAGE_RESOLUTION) {
                    int tileWorldX = startX + tileX;
                    int tileWorldZ = startZ + tileZ;
                    for (ProfileRange range : ranges[stageTileIndex(tileX, tileZ)]) {
                        List<Feature> stageFeatures = profileFeatures.get(range.profile)
                            .getOrDefault(stage, Collections.emptyList());
                        if (stageFeatures.isEmpty()) continue;
                        for (int subX = 0; subX < STAGE_RESOLUTION; subX++) {
                            for (int subZ = 0; subZ < STAGE_RESOLUTION; subZ++) {
                                populateColumn(
                                    world,
                                    tileWorldX + subX,
                                    tileWorldZ + subZ,
                                    range,
                                    stageFeatures,
                                    stageOrdinal);
                            }
                        }
                    }
                }
            }
        }
    }

    private static int stageTileIndex(int tileX, int tileZ) {
        return tileX / STAGE_RESOLUTION * (16 / STAGE_RESOLUTION) + tileZ / STAGE_RESOLUTION;
    }

    private void validateAuthoredBlockVocabulary(JsonArray identifiers) {
        for (JsonElement identifier : identifiers) resolve(identifier.getAsString());
        if (!compatibilitySubstitutes.isEmpty()) {
            throw new IllegalStateException(
                "Origen requires exact blocks that are disabled or absent from the hard-dependent EFR build: "
                    + compatibilitySubstitutes);
        }
    }

    static boolean habitatOwnsDecoration(String stage, String featureId) {
        // These are part of a landmark, not interchangeable groundcover/canopy. The source stage
        // alone is insufficient: Sanctuary mushrooms are in "trees", hanging arch foliage in "flora".
        if (switch (featureId) {
            case "LARGE_MUSHROOMS", "LARGE_LEAF_VINES", "LARGE_LEAF_VINES_UNCOLORED", "LARGE_LEAF_VINES_CANYON", "LARGE_CEILING_VINES", "CROSSING_VINES", "CROSSING_STONE_VINES", "TREE_VINES", "MOUNTAIN_GLOW_BERRIES", "BIG_DRIPLEAF", "SMALL_DRIPLEAF" -> true;
            default -> false;
        }) return false;
        return "trees".equals(stage) || "flora".equals(stage) || "landforms".equals(stage) && switch (featureId) {
            case "BOULDERS", "SMALL_BOULDER_PATCHES", "MOSSY_BOULDERS", "SMALL_MOSSY_BOULDER_PATCHES", "GRANITE_BOULDERS", "SMALL_GRANITE_BOULDER_PATCHES", "SANDSTONE_BOULDERS", "SMALL_SANDSTONE_BOULDER_PATCHES" -> true;
            default -> false;
        };
    }

    private void populateColumn(World world, int x, int z, ProfileRange range, List<Feature> stageFeatures,
        int stageOrdinal) {
        long seed = world.getSeed();
        long coordinateSeed = (seed * 31L + x) * 31L + z;
        for (int featureOrdinal = 0; featureOrdinal < stageFeatures.size(); featureOrdinal++) {
            Feature feature = stageFeatures.get(featureOrdinal);
            // The cave decor pass grows glow lichen in colonies instead of Origen's white noise (CaveDecorPass).
            if (caveLichenColonies && "CAVE_GLOW_LICHEN".equals(feature.id)
                && "global-preprocessors".equals(stages.get(stageOrdinal))) continue;
            if (!feature.distributor.matches(x, z, seed)) continue;
            FeatureProfile timing = PROFILE_FEATURES
                ? featureProfiles.computeIfAbsent(feature.id, ignored -> new FeatureProfile())
                : null;
            long featureStarted = timing == null ? 0L : System.nanoTime();
            if (timing != null) timing.distributorHits++;
            FeatureOrigin origin = usesStableTerrainSurface(feature.id)
                ? FeatureOrigin.at(x, z, stageOrdinal, range.minimumY, featureOrdinal)
                : null;
            FeatureOrigin previousLocatorOrigin = locatorOrigin;
            locatorOrigin = origin;
            try {
                BitSet locations;
                if (timing == null) {
                    locations = feature.locator.locate(world, x, z, range.minimumY, range.maximumY);
                } else {
                    long locatorStarted = System.nanoTime();
                    try {
                        locations = feature.locator.locate(world, x, z, range.minimumY, range.maximumY);
                    } finally {
                        timing.locatorNanos += System.nanoTime() - locatorStarted;
                    }
                }
                if (!locations.isEmpty() && origin == null) {
                    origin = FeatureOrigin.at(x, z, stageOrdinal, range.minimumY, featureOrdinal);
                }
                for (int location = locations.nextSetBit(0); location
                    >= 0; location = locations.nextSetBit(location + 1)) {
                    int authoredY = authoredY(location);
                    int physicalY = heightTransport.physicalY(authoredY);
                    if (!heightTransport.containsPhysicalY(physicalY)) continue;
                    if (hostedEnvironmentOwner != null && hostedEnvironmentOwner.owns(x, physicalY, z)
                        && AbyssalHostPollutionPolicy.displaced(stages.get(stageOrdinal), feature.id)) continue;
                    if (intrinsicEnvironmentOwner != null && intrinsicEnvironmentOwner.owns(x, physicalY, z)) continue;
                    // Reject the rooted proposal before its script runs; never trim an individual flora body.
                    // Veins select individual BLOCK structures, bypassing OreStructure's blob guard.
                    // Their authored-space MATCH_SET must also hold in the physical terrain.
                    if (mainlandHabitats != null
                        && ("COPPER_ORE_VEINS".equals(feature.id) || "IRON_ORE_VEINS".equals(feature.id))
                        && !isProjectedVeinHost(feature.id, physicalOreHostMaterial(x, physicalY, z))) continue;
                    // RU replaces underground ecology only. Keep Origen cavities, ores and fluid-containment
                    // passes, but do not stack its azaleas, sculk, chains or glow berries into a native cave.
                    if (!range.profile.isSurface() && hostedCaveOwner != null
                        && hostedCaveOwner.owns(x, physicalY, z)
                        && HostedCaveEcology.replaces(stages.get(stageOrdinal), feature.id)) continue;
                    String habitat = mainlandHabitats == null ? null : mainlandHabitats.sample(x, z);
                    // New native cold habitats finish snow AFTER their native trees. Source slabs would
                    // otherwise occupy the tree's air-space checks before its generator even starts.
                    if (range.profile.isSurface() && HostedLandmarkVegetation.replacesSourceWinter(habitat, feature.id))
                        continue;
                    if (range.profile.isSurface() && HostedLandmarkVegetation.replaceDrape(habitat, feature.id))
                        continue;
                    if (mainlandHabitats != null && range.profile.isSurface()
                        && "trees".equals(stages.get(stageOrdinal))
                        && habitatOwnsDecoration("trees", feature.id)
                        && !origenTreeHasHabitatClearance(x, z)) continue;
                    // Hosted habitats own surface decoration, including decorative boulders. Keep actual
                    // terrain, cave geology and landmarks; reject whole features, never trim their edges.
                    if (mainlandHabitats != null && range.profile.isSurface()
                        && (habitatOwnsDecoration(stages.get(stageOrdinal), feature.id)
                            || fullNativeMarsh && "underwater-flora".equals(stages.get(stageOrdinal))
                                && industries.angelic.biomic.compat.biome.SaltMarshCompat.BIOME.equals(habitat))
                        && habitat != null
                        && (!"landforms".equals(stages.get(stageOrdinal))
                            || terrainAccess.floatingArchetypeAt(x, physicalY, z) == null)
                        && ("trees".equals(stages.get(stageOrdinal))
                            || (hostedSurfaceOwner != null ? hostedSurfaceOwner.owns(x, physicalY, z)
                                : physicalY >= terrainAccess.carvedGroundSurfaceHeight(x, z) - 3)))
                        continue;
                    // Reject the complete surface-rooted structure before it can leave a clipped tree, cactus or
                    // canopy at a village boundary. Deep ores and cave features remain unaffected.
                    if (!SNOW_COVER.contains(feature.id) && isProtectedSurface(
                        x,
                        physicalY,
                        z,
                        "trees".equals(stages.get(stageOrdinal)) || "landforms".equals(stages.get(stageOrdinal))))
                        continue;
                    // Animated kelp and seagrass are creative-only (ecology review): MarineLayer dresses the sea floor.
                    if (MarineLayer.replaces(feature.id)) continue;
                    if (RemovedFeatures.removed(feature.id)) continue;
                    // Origen's fluid fills flood every air cell of a band, assuming its caves floor there. Expansion
                    // caves run on below the band: a fill cell needs a floor (rock or the fluid) under itself, or the
                    // lake hangs in the air. Where the fill's profile ends beside open air (a cave of another profile,
                    // a hosted habitat), its edge cells would stand as walls of lava: they cool to a basalt crust.
                    Integer fillBottom = FLUID_FILLS.get(feature.id);
                    if (fillBottom != null) {
                        int band = heightTransport.physicalY(fillBottom);
                        if (!floored(world, x, physicalY - 1, z, band)) continue;
                        if (!fillContained(world, feature, stageOrdinal, x, authoredY, physicalY, z, band)) {
                            writePhysical(world, x, physicalY, z, resolve(FILL_CRUST), false);
                            continue;
                        }
                    }
                    // These are exterior wall boulders, not cave geology. A new underground wall can satisfy
                    // their slant locator; reject the whole bounded structure rather than filling/clipping it.
                    if ("TEXTURED_STONE_SLANT".equals(feature.id)
                        && terrainAccess.surfaceBoulderIntersectsExpansion(x, physicalY, z)) continue;
                    if (timing != null) timing.anchors++;
                    WeightedStructure selectedChoice = feature.selectChoice(seed, x, authoredY, z);
                    Structure structure = selectedChoice.structure();
                    if (range.profile.isSurface() && RESKINNED_FLOWER_FEATURES.contains(feature.id)) {
                        Structure flower = reskinnedFlower(range.profile, selectedChoice.id);
                        if (flower != null) structure = flower;
                    }
                    OrigenLeafDecor.Spec decor = OrigenLeafDecor.spec(feature.id, range.profile);
                    String previousLeafSubstitute = activeLeafSubstitute;
                    String previousBamboo = activeBamboo;
                    if (decor instanceof OrigenLeafDecor.Substitute substitute)
                        activeLeafSubstitute = substitute.state();
                    else if (decor instanceof OrigenLeafDecor.Bamboo bamboo) activeBamboo = bamboo.species();
                    else if (decor != null) structure = leafDecor.computeIfAbsent(decor, this::leafDecorStructure);
                    FeatureOrigin previousActiveFeatureOrigin = activeFeatureOrigin;
                    String previousActiveFeatureId = activeFeatureId;
                    String previousActiveFeatureStage = activeFeatureStage;
                    OrigenBiomeProfile previousActiveFeatureProfile = activeFeatureProfile;
                    int previousStructureAuthoredY = activeStructureAuthoredY;
                    Set<Long> previousOccludableTreeBlocks = activeOccludableTreeBlocks;
                    Set<Long> previousDependentStructureBlocks = activeDependentStructureBlocks;
                    FeatureOrigin previousDependencyOrigin = activeDependencyOrigin;
                    Set<Long> previousLargeMushroomColumns = activeLargeMushroomColumns;
                    Map<Long, RejectedMushroomPlacement> previousRejectedMushroomPlacements = activeRejectedMushroomPlacements;
                    Set<Long> occludableTreeBlocks = isOccludableTree(feature.id) ? new HashSet<>() : null;
                    FeatureOrigin dependencyOrigin = treeVineDependencyOrigin(feature.id, x, authoredY, z);
                    Set<Long> dependentStructureBlocks = dependencyOrigin == null ? null : new HashSet<>();
                    Set<Long> largeMushroomColumns = LARGE_MUSHROOMS.equals(feature.id) ? new HashSet<>() : null;
                    Map<Long, RejectedMushroomPlacement> rejectedMushroomPlacements = LARGE_MUSHROOMS.equals(feature.id)
                        ? new LinkedHashMap<>()
                        : null;
                    activeFeatureOrigin = origin;
                    activeFeatureId = feature.id;
                    activeFeatureStage = stages.get(stageOrdinal);
                    activeFeatureProfile = range.profile;
                    activeStructureAuthoredY = authoredY;
                    activeOccludableTreeBlocks = occludableTreeBlocks;
                    activeDependentStructureBlocks = dependentStructureBlocks;
                    activeDependencyOrigin = dependencyOrigin;
                    activeLargeMushroomColumns = largeMushroomColumns;
                    activeRejectedMushroomPlacements = rejectedMushroomPlacements;
                    boolean generated;
                    long structureStarted = timing == null ? 0L : System.nanoTime();
                    try {
                        if (requiresHostedEnvironmentTransaction(
                            intrinsicEnvironmentOwner != null,
                            hostedEnvironmentOwner != null
                                && AbyssalHostPollutionPolicy.displaced(activeFeatureStage, activeFeatureId),
                            range.profile.isSurface() && nearVillageSurface(x, physicalY, z)))
                            populationView.beginHostedEnvironmentTransaction();
                        structureRandom.setSeed(coordinateSeed * 31L + authoredY);
                        generated = structure.generate(world, x, physicalY, z, authoredY, 0, structureRandom);
                        boolean admitted = populationView.finishHostedEnvironmentTransaction();
                        if (!admitted) {
                            if (occludableTreeBlocks != null) occludableTreeBlocks.clear();
                            if (dependentStructureBlocks != null) dependentStructureBlocks.clear();
                        }
                        generated = admitted && generated;
                    } finally {
                        activeLeafSubstitute = previousLeafSubstitute;
                        activeBamboo = previousBamboo;
                        populationView.discardHostedEnvironmentTransaction();
                        if (timing != null) timing.structureNanos += System.nanoTime() - structureStarted;
                        activeLargeMushroomColumns = previousLargeMushroomColumns;
                        activeOccludableTreeBlocks = previousOccludableTreeBlocks;
                        activeDependentStructureBlocks = previousDependentStructureBlocks;
                        activeDependencyOrigin = previousDependencyOrigin;
                        activeRejectedMushroomPlacements = previousRejectedMushroomPlacements;
                        activeFeatureId = previousActiveFeatureId;
                        activeFeatureStage = previousActiveFeatureStage;
                        activeFeatureProfile = previousActiveFeatureProfile;
                        activeStructureAuthoredY = previousStructureAuthoredY;
                        activeFeatureOrigin = previousActiveFeatureOrigin;
                    }
                    // Journaled, neighbours' trees are settled by the journal's apply rule, not rolled back here.
                    boolean crossPopulation = populationView.journal == null;
                    if (crossPopulation && occludableTreeBlocks != null && !occludableTreeBlocks.isEmpty()) {
                        if (isTreeCanopyOccluder(feature.id)) {
                            suppressTreeOccludedNeighbourTrees(world, origin, occludableTreeBlocks);
                        }
                        synchronized (occludableTreeStructures) {
                            occludableTreeStructures.put(
                                origin,
                                new AuthoredTreeStructure(
                                    feature.id,
                                    authoredY,
                                    new ArrayList<>(occludableTreeBlocks)));
                        }
                    }
                    if (crossPopulation && dependencyOrigin != null
                        && dependentStructureBlocks != null
                        && !dependentStructureBlocks.isEmpty()) {
                        synchronized (dependentStructures) {
                            dependentStructures.computeIfAbsent(dependencyOrigin, ignored -> new ArrayList<>())
                                .add(
                                    new AuthoredDependentStructure(
                                        origin,
                                        feature.id,
                                        new ArrayList<>(dependentStructureBlocks)));
                        }
                    }
                    if (generated && largeMushroomColumns != null) {
                        if (crossPopulation)
                            suppressMushroomOccludedNeighbourTrees(world, origin, largeMushroomColumns);
                        replayRejectedMushroomPlacements(world, origin, rejectedMushroomPlacements);
                        replayMissedMushroomFoliage(world, origin, largeMushroomColumns);
                    }
                }
            } finally {
                locatorOrigin = previousLocatorOrigin;
                if (timing != null) timing.totalNanos += System.nanoTime() - featureStarted;
            }
        }
    }

    /**
     * These very large canopy structures routinely cross chunk borders. During ordinary player-driven loading, a
     * canonically later neighbour may be populated first; its canopy must not hide the ground from an earlier TOP
     * locator. The authored block cache retains each writer's canonical Terra order so only that reversed dependency
     * is ignored. Genuine earlier collisions remain identical to Paper, unlike a terrain-only locator which would
     * admit every distributor point and nearly double Scarlet's giant structures.
     */
    private static boolean usesStableTerrainSurface(String featureId) {
        return LARGE_MUSHROOMS.equals(featureId);
    }

    private static boolean isOccludableTree(String featureId) {
        return "DEAD_SWAMP_TREES".equals(featureId) || "DEAD_TREES_SPARSE".equals(featureId)
            || "LUSH_FOREST_TREES".equals(featureId)
            || "BLACK_FOREST_REDWOODS".equals(featureId);
    }

    private boolean origenTreeHasHabitatClearance(int x, int z) {
        // Origen's procedural canopies may reach well beyond their anchor. Keep a complete
        // conservative envelope on the authored side of a habitat boundary, never shear its leaves.
        for (int dz = -48; dz <= 48; dz += 8) for (int dx = -48; dx <= 48; dx += 8) {
            if (mainlandHabitats.sample(x + dx, z + dz) != null) return false;
        }
        return true;
    }

    private static boolean isTreeCanopyOccluder(String featureId) {
        return "LUSH_FOREST_TREES".equals(featureId) || "BLACK_FOREST_REDWOODS".equals(featureId);
    }

    /** TREE_VINES is admitted by an air cell immediately below one authored leaf cell. */
    private FeatureOrigin treeVineDependencyOrigin(String featureId, int x, int authoredY, int z) {
        if (!TREE_VINES.equals(featureId)) return null;
        AuthoredFeatureBlock support = authoredFeatureBlock(x, authoredY + 1, z);
        if (support == null) return null;
        if (support.dependencyOrigin() != null) return support.dependencyOrigin();
        return support.origin() != null && isOccludableTree(support.featureId()) ? support.origin() : null;
    }

    /**
     * A neighboring giant tree can put its canopy above another candidate's root. Terra then admits or rejects that
     * root according to which chunk happened to populate first. If the covered tree arrived first, remove it when the
     * canopy eventually arrives; the opposite order is already rejected naturally by the TOP + ground pattern. Same-
     * chunk trees retain Terra's fixed column traversal semantics.
     */
    private void suppressTreeOccludedNeighbourTrees(World world, FeatureOrigin canopyOrigin, Set<Long> canopyBlocks) {
        if (canopyBlocks.isEmpty()) return;
        Map<Long, Integer> canopyTops = new HashMap<>();
        for (Long encodedPosition : canopyBlocks) {
            long position = encodedPosition.longValue();
            long column = horizontalKey(blockKeyX(position), blockKeyZ(position));
            int authoredY = blockKeyY(position);
            canopyTops.merge(column, Integer.valueOf(authoredY), Math::max);
        }
        List<SuppressedTree> suppressed = new ArrayList<>();
        synchronized (occludableTreeStructures) {
            java.util.Iterator<Map.Entry<FeatureOrigin, AuthoredTreeStructure>> iterator = occludableTreeStructures
                .entrySet()
                .iterator();
            while (iterator.hasNext()) {
                Map.Entry<FeatureOrigin, AuthoredTreeStructure> entry = iterator.next();
                FeatureOrigin treeOrigin = entry.getKey();
                AuthoredTreeStructure tree = entry.getValue();
                Integer canopyTop = canopyTops.get(horizontalKey(treeOrigin.x(), treeOrigin.z()));
                if (treeOrigin.chunkX() == canopyOrigin.chunkX() && treeOrigin.chunkZ() == canopyOrigin.chunkZ()
                    || canopyTop == null
                    || canopyTop.intValue() < tree.anchorAuthoredY()) {
                    continue;
                }
                suppressed.add(new SuppressedTree(treeOrigin, tree));
                iterator.remove();
            }
        }
        for (SuppressedTree entry : suppressed) rollbackAuthoredTree(world, entry.origin, entry.tree);
    }

    /**
     * Terra lets the chunk that arrives first decide whether a tree may stand below a neighboring mushroom cap. If
     * the tree wins the race it remains as an isolated log cluster; if the mushroom wins, its higher TOP surface
     * rejects the whole tree. Resolve that ambiguity in favor of the visually coherent mushroom-first result. Only
     * an earlier tree rooted in a neighboring chunk is rolled back, and only while its authored cells are still owned
     * by that tree. Retaining the tree record proves its feature already ran, even while Forge has not yet raised the
     * chunk's terrain-populated latch.
     */
    private void suppressMushroomOccludedNeighbourTrees(World world, FeatureOrigin mushroomOrigin,
        Set<Long> mushroomColumns) {
        if (mushroomColumns.isEmpty()) return;
        List<SuppressedTree> suppressed = new ArrayList<>();
        synchronized (occludableTreeStructures) {
            java.util.Iterator<Map.Entry<FeatureOrigin, AuthoredTreeStructure>> iterator = occludableTreeStructures
                .entrySet()
                .iterator();
            while (iterator.hasNext()) {
                Map.Entry<FeatureOrigin, AuthoredTreeStructure> entry = iterator.next();
                FeatureOrigin treeOrigin = entry.getKey();
                if (treeOrigin.chunkX() == mushroomOrigin.chunkX() && treeOrigin.chunkZ() == mushroomOrigin.chunkZ()
                    || !mushroomColumns.contains(horizontalKey(treeOrigin.x(), treeOrigin.z()))) {
                    continue;
                }
                suppressed.add(new SuppressedTree(treeOrigin, entry.getValue()));
                iterator.remove();
            }
        }
        for (SuppressedTree entry : suppressed) {
            rollbackAuthoredTree(world, entry.origin, entry.tree);
        }
    }

    private void rollbackAuthoredTree(World world, FeatureOrigin origin, AuthoredTreeStructure tree) {
        rollbackDependentStructures(world, origin);
        rollbackAuthoredBlocks(world, origin, tree.featureId, tree.blocks);
    }

    /**
     * Terra never runs a leaf-hanging feature against a tree that its canonical neighbor order rejects. If Forge's
     * chunk order temporarily admitted the tree, every structure located from that canopy must disappear with it.
     */
    private void rollbackDependentStructures(World world, FeatureOrigin dependencyOrigin) {
        List<AuthoredDependentStructure> dependents;
        synchronized (dependentStructures) {
            dependents = dependentStructures.remove(dependencyOrigin);
        }
        if (dependents == null) return;
        for (AuthoredDependentStructure dependent : dependents) {
            rollbackAuthoredBlocks(world, dependent.origin, dependent.featureId, dependent.blocks);
        }
    }

    private void rollbackAuthoredBlocks(World world, FeatureOrigin origin, String featureId, List<Long> blocks) {
        for (Long encodedPosition : blocks) {
            long position = encodedPosition.longValue();
            int x = blockKeyX(position);
            int authoredY = blockKeyY(position);
            int z = blockKeyZ(position);
            AuthoredFeatureBlock removed;
            synchronized (authoredFeatureCache) {
                long chunkKey = horizontalKey(x >> 4, z >> 4);
                AuthoredFeatureColumn[] chunk = authoredFeatureCache.getAndMoveToLast(chunkKey);
                AuthoredFeatureBlock current = indexedAuthoredFeatureBlock(chunk, x, authoredY, z);
                if (current == null || !origin.equals(current.origin()) || !featureId.equals(current.featureId())) {
                    continue;
                }
                removed = setIndexedAuthoredFeatureBlock(chunk, x, authoredY, z, null);
                if (populationView != null) {
                    populationView.forgetAuthoredFeatureBlock(chunkKey, chunk, x, z);
                }
            }
            int physicalY = heightTransport.physicalY(authoredY);
            WorldBlock physical = readPhysical(world, x, physicalY, z);
            if (physical.block() != removed.block()
                .block() || physical.metadata()
                    != removed.block()
                        .metadata()) {
                continue;
            }
            WorldBlock terrain = projectedAuthoredReplacement(x, physicalY, z);
            writePhysical(
                world,
                x,
                physicalY,
                z,
                new PendingBlock(blockResolver.canonicalId(terrain.block(), terrain.metadata()), false));
        }
    }

    private WorldBlock projectedAuthoredReplacement(int x, int physicalY, int z) {
        AuthoredFeatureBlock selected = null;
        for (int candidateY : heightTransport.authoredYsAtPhysical(physicalY)) {
            AuthoredFeatureBlock candidate = authoredFeatureBlock(x, candidateY, z);
            if (candidate == null) continue;
            if (selected == null || selected.block()
                .block()
                .getMaterial() != Material.wood
                && candidate.block()
                    .block()
                    .getMaterial() == Material.wood
                || selected.block()
                    .block()
                    .getMaterial()
                    == candidate.block()
                        .block()
                        .getMaterial()
                    && selected.origin() != null
                    && candidate.origin() != null
                    && selected.origin()
                        .compareTo(candidate.origin()) < 0) {
                selected = candidate;
            }
        }
        if (selected != null) return selected.block();
        int authoredY = heightTransport.projection()
            .toSourceBlockY(physicalY)
            .orElseThrow(
                () -> new IllegalStateException("Projected feature cell has no authored terrain layer: " + physicalY));
        return resolveTerrainMaterial(populationView.authoredTerrainMaterial(x, authoredY, z));
    }

    /** Restores mushroom writes that temporarily lost to a canonically later tree which was then rolled back. */
    private void replayRejectedMushroomPlacements(World world, FeatureOrigin mushroomOrigin,
        Map<Long, RejectedMushroomPlacement> rejected) {
        if (rejected == null || rejected.isEmpty()) return;
        FeatureOrigin previousOrigin = activeFeatureOrigin;
        String previousFeatureId = activeFeatureId;
        activeFeatureOrigin = mushroomOrigin;
        activeFeatureId = LARGE_MUSHROOMS;
        try {
            for (RejectedMushroomPlacement placement : rejected.values()) {
                AuthoredFeatureBlock existing = authoredFeatureBlock(
                    placement.x(),
                    placement.authoredY(),
                    placement.z());
                if (existing != null && existing.origin() != null
                    && existing.origin()
                        .compareTo(mushroomOrigin) > 0) {
                    continue;
                }
                writeAuthoredFeatureBlock(placement.x(), placement.authoredY(), placement.z(), placement.block());
                int physicalY = heightTransport.physicalY(placement.authoredY());
                if (heightTransport.containsPhysicalY(physicalY)) {
                    writePhysical(
                        world,
                        placement.x(),
                        physicalY,
                        placement.z(),
                        new PendingBlock(canonicalState(placement.block()), placement.physics()));
                }
            }
        } finally {
            activeFeatureId = previousFeatureId;
            activeFeatureOrigin = previousOrigin;
        }
    }

    /**
     * A giant mushroom may extend into a neighboring chunk whose flora stage already ran. Terra then leaves that
     * side bare even though the same seed/load order can decorate it when the mushroom's chunk happens to populate
     * first. Replay only Origen's deterministic persistent-leaf feature on mushroom columns in already-populated
     * neighbors. Unpopulated neighbors and the source chunk still receive the ordinary flora stage exactly once.
     */
    private void replayMissedMushroomFoliage(World world, FeatureOrigin mushroomOrigin, Set<Long> mushroomColumns) {
        Feature foliage = features.get(MUSHROOM_FOLIAGE);
        int stageOrdinal = stages.indexOf("flora");
        if (foliage == null || stageOrdinal < 0 || mushroomColumns.isEmpty()) return;

        List<FeatureReplay> replays = new ArrayList<>();
        for (Long columnKey : mushroomColumns) {
            int x = (int) (columnKey.longValue() >> 32);
            int z = (int) columnKey.longValue();
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            if (chunkX == mushroomOrigin.chunkX() && chunkZ == mushroomOrigin.chunkZ()
                || !populationView.isPopulated(chunkX, chunkZ)) {
                continue;
            }
            for (ProfileRange range : profileRanges(x, z)) {
                List<Feature> stageFeatures = profileFeatures.get(range.profile)
                    .getOrDefault("flora", Collections.emptyList());
                int featureOrdinal = stageFeatures.indexOf(foliage);
                if (featureOrdinal >= 0) {
                    FeatureOrigin originalOrigin = FeatureOrigin.at(x, z, stageOrdinal, range.minimumY, featureOrdinal);
                    replays.add(new FeatureReplay(x, z, range, originalOrigin));
                }
            }
        }
        Collections.sort(replays, (first, second) -> first.originalOrigin.compareTo(second.originalOrigin));
        for (FeatureReplay replay : replays) {
            replayMushroomFoliageColumn(world, foliage, replay, mushroomOrigin);
        }
    }

    private void replayMushroomFoliageColumn(World world, Feature foliage, FeatureReplay replay,
        FeatureOrigin mushroomOrigin) {
        long seed = world.getSeed();
        if (!foliage.distributor.matches(replay.x, replay.z, seed)) return;
        FeatureOrigin previousLocatorOrigin = locatorOrigin;
        locatorOrigin = null;
        try {
            BitSet locations = foliage.locator
                .locate(world, replay.x, replay.z, replay.range.minimumY, replay.range.maximumY);
            for (int location = locations.nextSetBit(0); location >= 0; location = locations.nextSetBit(location + 1)) {
                int authoredY = authoredY(location);
                int physicalY = heightTransport.physicalY(authoredY);
                if (!heightTransport.containsPhysicalY(physicalY)) continue;
                var selected = foliage.selectChoice(seed, replay.x, authoredY, replay.z);
                Structure structure = selected.structure();
                FeatureOrigin previousActiveFeatureOrigin = activeFeatureOrigin;
                String previousActiveFeatureId = activeFeatureId;
                activeFeatureOrigin = mushroomOrigin;
                activeFeatureId = MUSHROOM_FOLIAGE_REPLAY;
                try {
                    long coordinateSeed = (seed * 31L + replay.x) * 31L + replay.z;
                    structureRandom.setSeed(coordinateSeed * 31L + authoredY);
                    structure.generate(world, replay.x, physicalY, replay.z, authoredY, 0, structureRandom);
                } finally {
                    activeFeatureId = previousActiveFeatureId;
                    activeFeatureOrigin = previousActiveFeatureOrigin;
                }
            }
        } finally {
            locatorOrigin = previousLocatorOrigin;
        }
    }

    private List<ProfileRange> profileRanges(int x, int z) {
        List<ProfileRange> ranges = new ArrayList<>();
        int runningMinimum = OrigenDensityField.UPSTREAM_MIN_Y;
        // Every vertical sample shares the same surface province. Sampling through sampleProfile here used to
        // re-enter the complete 2D biome pipeline for all 96 four-block slices in each stage tile. Retain Terra's
        // exact extrusion function while resolving that invariant surface input once per column.
        OrigenBiomeProfile surface = densityField.sampleSurfaceProfile(x, z);
        OrigenBiomeProfile running = densityField.extrudeProfile(surface, x, runningMinimum, z);
        for (int y = runningMinimum + STAGE_RESOLUTION; y
            < OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE; y += STAGE_RESOLUTION) {
            OrigenBiomeProfile current = densityField.extrudeProfile(surface, x, y, z);
            if (current != running) {
                ranges.add(new ProfileRange(runningMinimum, y, running));
                runningMinimum = y;
                running = current;
            }
        }
        ranges.add(new ProfileRange(runningMinimum, OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE, running));
        return ranges;
    }

    private void loadStages(JsonArray definitions) {
        for (JsonElement definition : definitions) stages.add(definition.getAsString());
    }

    private void loadFeatures(JsonObject definitions) {
        for (Map.Entry<String, JsonElement> entry : definitions.entrySet()) {
            JsonObject definition = entry.getValue()
                .getAsJsonObject();
            Distributor distributor = compileDistributor(object(definition, "distributor"));
            JsonObject locatorDefinition = object(definition, "locator");
            Locator locator = compileLocator(locatorDefinition);
            locator = optimizeFeatureLocator(entry.getKey(), locatorDefinition, locator);
            NoiseSampler selector = compiler.compileSampler(object(definition, "structure_sampler"));
            List<WeightedStructure> choices = new ArrayList<>();
            int totalWeight = 0;
            for (JsonElement rawChoice : array(definition, "structures")) {
                JsonObject choice = rawChoice.getAsJsonObject();
                int weight = (int) compiler.numeric(required(choice, "weight"));
                if (weight < 1) throw new IllegalStateException("Feature " + entry.getKey() + " has invalid weight");
                totalWeight += weight;
                String structureId = required(choice, "structure").getAsString();
                choices.add(new WeightedStructure(structureId, totalWeight, structure(structureId)));
            }
            if (choices.isEmpty()) throw new IllegalStateException("Feature " + entry.getKey() + " has no structures");
            features
                .put(entry.getKey(), new Feature(entry.getKey(), distributor, locator, selector, choices, totalWeight));
        }
    }

    private Locator optimizeFeatureLocator(String featureId, JsonObject definition, Locator fallback) {
        if ("LARGE_LEAF_VINES".equals(featureId)) {
            JsonArray children = array(definition, "locators");
            IntRange search = range(
                required(
                    children.get(0)
                        .getAsJsonObject(),
                    "range"));
            NoiseSampler sampler = compiler.compileSampler(
                object(
                    children.get(2)
                        .getAsJsonObject(),
                    "sampler"));
            BlockPattern solid = new BlockPattern(new IntRange(0, 1), BlockMatch.SOLID, Collections.emptyList());
            return (world, x, z, columnMinimum, columnMaximum) -> {
                if (populationView == null || locatorOrigin != null) {
                    return fallback.locate(world, x, z, columnMinimum, columnMaximum);
                }
                return populationView.scanLargeLeafVines(
                    x,
                    z,
                    Math.max(search.minimum, columnMinimum),
                    Math.min(search.maximum, columnMaximum),
                    sampler,
                    solid);
            };
        }
        if ("SMALL_DRIPLEAF".equals(featureId)) {
            IntRange search = range(required(definition, "range"));
            JsonArray alternatives = array(object(definition, "pattern"), "patterns");
            JsonArray water = array(
                alternatives.get(0)
                    .getAsJsonObject(),
                "patterns");
            JsonArray dry = array(
                alternatives.get(1)
                    .getAsJsonObject(),
                "patterns");
            BlockPattern aboveAir = requireBlockPattern(water.get(0));
            BlockPattern waterBlock = requireBlockPattern(water.get(1));
            BlockPattern wetSupport = requireBlockPattern(water.get(2));
            BlockPattern dryAir = requireBlockPattern(dry.get(1));
            BlockPattern drySupport = requireBlockPattern(dry.get(2));
            return (world, x, z, columnMinimum, columnMaximum) -> {
                if (populationView == null || locatorOrigin != null) {
                    return fallback.locate(world, x, z, columnMinimum, columnMaximum);
                }
                return populationView.scanSmallDripleaf(
                    x,
                    z,
                    Math.max(search.minimum, columnMinimum),
                    Math.min(search.maximum, columnMaximum),
                    aboveAir,
                    waterBlock,
                    wetSupport,
                    dryAir,
                    drySupport);
            };
        }
        if ("ARCH_GLOW_LICHEN".equals(featureId)) {
            JsonArray children = array(definition, "locators");
            Locator random = compileLocator(
                children.get(0)
                    .getAsJsonObject());
            IntRange search = range(
                required(
                    children.get(1)
                        .getAsJsonObject(),
                    "range"));
            JsonArray currentPatterns = array(
                object(
                    children.get(1)
                        .getAsJsonObject(),
                    "pattern"),
                "patterns");
            BlockPattern air = requireBlockPattern(currentPatterns.get(0));
            BlockPattern water = requireBlockPattern(currentPatterns.get(1));
            BlockPattern solid = new BlockPattern(new IntRange(0, 1), BlockMatch.SOLID, Collections.emptyList());
            return (world, x, z, columnMinimum, columnMaximum) -> {
                if (populationView == null || locatorOrigin != null) {
                    return fallback.locate(world, x, z, columnMinimum, columnMaximum);
                }
                BitSet candidates = random.locate(world, x, z, columnMinimum, columnMaximum);
                return populationView.retainArchGlowLichen(
                    x,
                    z,
                    Math.max(search.minimum, columnMinimum),
                    Math.min(search.maximum, columnMaximum),
                    candidates,
                    air,
                    water,
                    solid);
            };
        }
        if ("TEXTURED_STONE_SLANT".equals(featureId)) {
            JsonObject samplerDefinition = array(definition, "locators").get(0)
                .getAsJsonObject()
                .getAsJsonObject("sampler");
            NoiseSampler sampler = compiler.compileSampler(samplerDefinition);
            List<OrigenBlockStateResolver.ResolvedBlock> rocks = new ArrayList<>();
            rocks.add(resolve("minecraft:stone"));
            rocks.add(resolve("minecraft:andesite"));
            BlockPattern rock = new BlockPattern(new IntRange(0, 1), BlockMatch.STATE, rocks);
            return (world, x, z, columnMinimum, columnMaximum) -> {
                if (populationView == null || locatorOrigin != null) {
                    return fallback.locate(world, x, z, columnMinimum, columnMaximum);
                }
                return populationView.scanTexturedStoneSlant(
                    x,
                    z,
                    Math.max(71, columnMinimum),
                    Math.min(161, columnMaximum),
                    sampler,
                    rock);
            };
        }
        int twoBlockMinimum;
        int twoBlockMaximum;
        boolean requireAir;
        switch (featureId) {
            case "KELP" -> {
                twoBlockMinimum = -5;
                twoBlockMaximum = 46;
                requireAir = false;
            }
            case "SEAGRASS", "MANGROVE_SEAGRASS" -> {
                twoBlockMinimum = -5;
                twoBlockMaximum = 63;
                requireAir = false;
            }
            case "CALDERA_SEAGRASS" -> {
                twoBlockMinimum = 65;
                twoBlockMaximum = 319;
                requireAir = false;
            }
            case "TREE_SNOW" -> {
                twoBlockMinimum = 63;
                twoBlockMaximum = 319;
                requireAir = true;
            }
            case "TREE_SNOW_CANYON" -> {
                twoBlockMinimum = 25;
                twoBlockMaximum = 63;
                requireAir = true;
            }
            default -> {
                twoBlockMinimum = 0;
                twoBlockMaximum = 0;
                requireAir = false;
            }
        }
        if (twoBlockMinimum < twoBlockMaximum) {
            BlockPattern current = requireAir ? null
                : new BlockPattern(
                    new IntRange(0, 1),
                    BlockMatch.STATE,
                    Collections.singletonList(resolve("minecraft:water")));
            List<OrigenBlockStateResolver.ResolvedBlock> supportStates = new ArrayList<>();
            if (requireAir) {
                supportStates.add(resolve("minecraft:oak_leaves"));
                supportStates.add(resolve("minecraft:spruce_leaves"));
                supportStates.add(resolve("minecraft:birch_leaves"));
            } else {
                supportStates.add(resolve("minecraft:mud"));
                supportStates.add(resolve("minecraft:sand"));
                supportStates.add(resolve("minecraft:gravel"));
            }
            BlockPattern support = new BlockPattern(new IntRange(0, 1), BlockMatch.STATE, supportStates);
            return (world, x, z, columnMinimum, columnMaximum) -> {
                if (populationView == null || locatorOrigin != null) {
                    return fallback.locate(world, x, z, columnMinimum, columnMaximum);
                }
                return populationView.scanTwoBlockPattern(
                    x,
                    z,
                    Math.max(twoBlockMinimum, columnMinimum),
                    Math.min(twoBlockMaximum, columnMaximum),
                    current,
                    support);
            };
        }
        int minimum;
        int maximum;
        boolean above;
        boolean below;
        boolean adjacent;
        switch (featureId) {
            case "CONTAIN_FLOATING_WATER" -> {
                minimum = -5;
                maximum = 64;
                above = true;
                below = false;
                adjacent = true;
            }
            case "CONTAIN_FLOATING_WATER_DEEP" -> {
                minimum = OrigenDensityField.UPSTREAM_MIN_Y;
                maximum = 40;
                above = true;
                below = true;
                adjacent = true;
            }
            case "CONTAIN_FLOATING_WATER_HIGH" -> {
                minimum = 64;
                maximum = 112;
                above = false;
                below = false;
                adjacent = true;
            }
            default -> {
                return fallback;
            }
        }
        BlockPattern water = new BlockPattern(
            new IntRange(0, 1),
            BlockMatch.STATE,
            Collections.singletonList(resolve("minecraft:water")));
        return (world, x, z, columnMinimum, columnMaximum) -> {
            if (populationView == null || locatorOrigin != null) {
                return fallback.locate(world, x, z, columnMinimum, columnMaximum);
            }
            return populationView.scanFloatingWaterContainment(
                x,
                z,
                Math.max(minimum, columnMinimum),
                Math.min(maximum, columnMaximum),
                water,
                above,
                below,
                adjacent);
        };
    }

    private BlockPattern requireBlockPattern(JsonElement definition) {
        Pattern pattern = compilePattern(definition.getAsJsonObject());
        if (!(pattern instanceof BlockPattern block)) {
            throw new IllegalStateException("Expected a direct block pattern in optimized Origen locator");
        }
        return block;
    }

    private void loadProfiles(JsonObject definitions) {
        for (OrigenBiomeProfile profile : OrigenBiomeProfile.values()) {
            JsonObject definition = object(definitions, profile.name());
            JsonObject stageDefinitions = object(definition, "features");
            Map<String, List<Feature>> byStage = new LinkedHashMap<>();
            for (String stage : stages) {
                JsonArray ids = stageDefinitions.has(stage) ? stageDefinitions.getAsJsonArray(stage) : new JsonArray();
                List<Feature> ordered = new ArrayList<>();
                for (JsonElement id : ids) {
                    Feature feature = features.get(id.getAsString());
                    if (feature == null) {
                        throw new IllegalStateException(
                            "Profile " + profile + " references missing feature " + id.getAsString());
                    }
                    ordered.add(feature);
                }
                byStage.put(stage, Collections.unmodifiableList(ordered));
            }
            profileFeatures.put(profile, Collections.unmodifiableMap(byStage));
        }
    }

    private void loadStructures(JsonObject definitions) {
        for (Map.Entry<String, JsonElement> entry : definitions.entrySet()) {
            JsonObject definition = entry.getValue()
                .getAsJsonObject();
            String format = string(definition, "format", "");
            switch (format) {
                case "TERRASCRIPT" -> structures.put(
                    entry.getKey(),
                    new ScriptStructure(
                        new OrigenTerraScript(
                            entry.getKey(),
                            translatePotatoPatchSupport(
                                entry.getKey(),
                                translateMushroomFlesh(
                                    entry.getKey(),
                                    required(definition, "script").getAsString())))));
                case "SPONGE_SCHEMATIC" -> structures
                    .put(entry.getKey(), decodeSchematic(entry.getKey(), required(definition, "base64").getAsString()));
                default -> throw new IllegalStateException("Unknown Origen structure format " + format);
            }
        }
    }

    /** Keeps genuine trunks on the vanilla 1.7 stem model while giving white cap spots their modern cube texture. */
    static String translateMushroomFlesh(String structureId, String source) {
        if (!"large_mixed_mushroom_procedural".equals(structureId) && !"red_mushroom_disk".equals(structureId)) {
            return source;
        }
        String original = "str spotBlock = \"minecraft:mushroom_stem\";";
        if (!source.contains(original)) {
            throw new IllegalStateException("Missing mushroom spot declaration in Origen structure " + structureId);
        }
        String translated = source.replace(original, "str spotBlock = \"" + MUSHROOM_FLESH + "\";");
        if ("large_mixed_mushroom_procedural".equals(structureId)) {
            String stem = "block(x+warpX, h, z+warpZ, \"minecraft:mushroom_stem\");";
            if (!translated.contains(stem)) {
                throw new IllegalStateException("Missing mushroom stem placement in Origen structure " + structureId);
            }
            translated = translated.replace(
                stem,
                "if (h == stemHeight - 1) {\n" + "                    block(x+warpX, h, z+warpZ, \""
                    + MUSHROOM_FLESH
                    + "\");\n"
                    + "                } else {\n"
                    + "                    block(x+warpX, h, z+warpZ, \"minecraft:mushroom_stem\");\n"
                    + "                }");
            // A bent/tapering stem exposes steps below its last layer too. Revisit only the source stem
            // footprint after the cap is complete; do not consume RNG, reshape it, or whiten covered trunks.
            translated += """

                for (num h = -2; h < stemHeight; h = h + 1) {
                    t = h/stemHeight;
                    it = 1-t;
                    currentRadius = stemStartRadius * it + stemEndRadius * t;
                    if (t > stemCollarThreshold) currentRadius = currentRadius + stemCollarThickness * it;
                    currentRadiusSquared = pow2(currentRadius);
                    for (num x = round(-currentRadius); x < round(currentRadius); x = x + 1) {
                        for (num z = round(-currentRadius); z < round(currentRadius); z = z + 1) {
                            if (pow2(x)+pow2(z) < currentRadiusSquared) {
                                num warpX = sampler("simplex", h, originX())*warp*t;
                                num warpZ = sampler("simplex", h, originZ())*warp*t;
                                if (getBlock(x+warpX, h, z+warpZ) == "minecraft:mushroom_stem"
                                    && getBlock(x+warpX, h+1, z+warpZ) == "minecraft:air") {
                                    block(x+warpX, h, z+warpZ, "biomic:mushroom_flesh");
                                }
                            }
                        }
                    }
                }
                """;
        }
        return translated;
    }

    /** Repairs Origen's potato patch so its vanilla crop cannot uproot on the broad generic plantable-soil set. */
    static String translatePotatoPatchSupport(String structureId, String source) {
        if (!POTATO_PATCH.equals(structureId)) return source;
        String potato = "block(x, y, z, \"minecraft:potatoes[age=4]\");";
        if (!source.contains(potato)) {
            throw new IllegalStateException("Missing crop placement in Origen structure " + structureId);
        }
        return source.replace(potato, "block(x, y - 1, z, \"minecraft:farmland[moisture=7]\");\n  " + potato);
    }

    /**
     * Origen deliberately uses biome-tinted jungle leaves for Sakura Streams' crossing and hanging leaf vines. The
     * modern cherry-grove foliage tint makes those leaves pink, but vanilla 1.7 renders the same state as ordinary
     * jungle foliage. Translate only those two Sakura-owned decorative streams to EFR cherry leaves; authored-space
     * state and feature traces remain untouched so later Terra locators still observe the exact upstream material.
     */
    static String translateSakuraTintedFoliage(OrigenBiomeProfile profile, String featureId, String state) {
        if (profile != OrigenBiomeProfile.SAKURA_STREAMS
            || !CROSSING_VINES.equals(featureId) && !LARGE_LEAF_VINES.equals(featureId)
            || !state.startsWith("minecraft:jungle_leaves")) return state;
        return "minecraft:cherry_leaves" + state.substring("minecraft:jungle_leaves".length());
    }

    private void loadOres(JsonObject definitions) {
        for (Map.Entry<String, JsonElement> entry : definitions.entrySet()) {
            JsonObject definition = entry.getValue()
                .getAsJsonObject();
            OrigenBlockStateResolver.ResolvedBlock material = resolve(required(definition, "material").getAsString());
            Set<Block> replaceable = new HashSet<>();
            for (JsonElement replacement : array(definition, "replace")) {
                replaceable.add(resolve(replacement.getAsString()).block());
            }
            Map<Block, OrigenBlockStateResolver.ResolvedBlock> overrides = new HashMap<>();
            JsonObject rawOverrides = object(definition, "material_overrides");
            for (Map.Entry<String, JsonElement> override : rawOverrides.entrySet()) {
                overrides.put(
                    resolve(override.getKey()).block(),
                    resolve(
                        override.getValue()
                            .getAsString()));
            }
            structures.put(
                entry.getKey(),
                new OreStructure(
                    material,
                    compiler.numeric(required(definition, "size")),
                    replaceable,
                    required(definition, "physics").getAsBoolean(),
                    compiler.numeric(required(definition, "exposed")),
                    definition.has("hidden_from_sky") && definition.get("hidden_from_sky")
                        .getAsBoolean(),
                    overrides));
        }
    }

    private Distributor compileDistributor(JsonObject definition) {
        String type = required(definition, "type").getAsString();
        return switch (type) {
            case "YES" -> (x, z, seed) -> true;
            case "NO" -> (x, z, seed) -> false;
            case "PADDED_GRID" -> {
                int width = (int) compiler.numeric(required(definition, "width"));
                int cellWidth = width + (int) compiler.numeric(required(definition, "padding"));
                int salt = (int) compiler.numeric(required(definition, "salt"));
                yield (x, z, seed) -> {
                    int cellX = Math.floorDiv(x, cellWidth);
                    int cellZ = Math.floorDiv(z, cellWidth);
                    distributorRandom.setSeed((MathUtil.murmur64(MathUtil.squash(cellX, cellZ)) ^ seed) + salt);
                    return x == distributorRandom.nextInt(width) + cellX * cellWidth
                        && z == distributorRandom.nextInt(width) + cellZ * cellWidth;
                };
            }
            case "SAMPLER" -> {
                NoiseSampler sampler = compiler.compileSampler(object(definition, "sampler"));
                double threshold = number(definition, "threshold", 0.0D);
                yield (x, z, seed) -> sampler.noise(seed, x, z) < threshold;
            }
            case "AND", "OR", "XOR" -> {
                List<Distributor> children = new ArrayList<>();
                for (JsonElement child : array(definition, "distributors")) {
                    children.add(compileDistributor(child.getAsJsonObject()));
                }
                if (children.isEmpty()) throw new IllegalStateException(type + " distributor has no children");
                yield (x, z, seed) -> {
                    boolean value = children.get(0)
                        .matches(x, z, seed);
                    for (int index = 1; index < children.size(); index++) {
                        boolean next = children.get(index)
                            .matches(x, z, seed);
                        value = switch (type) {
                            case "AND" -> value && next;
                            case "OR" -> value || next;
                            default -> value ^ next;
                        };
                    }
                    return value;
                };
            }
            default -> throw new IllegalStateException("Unsupported Origen distributor " + type);
        };
    }

    private Locator compileLocator(JsonObject definition) {
        String type = required(definition, "type").getAsString();
        return switch (type) {
            case "AND", "OR" -> {
                List<Locator> children = new ArrayList<>();
                for (JsonElement child : array(definition, "locators")) {
                    children.add(compileLocator(child.getAsJsonObject()));
                }
                if (children.isEmpty()) throw new IllegalStateException(type + " locator has no children");
                yield compositeLocator(type, children);
            }
            case "PATTERN" -> {
                Pattern pattern = compilePattern(object(definition, "pattern"));
                IntRange search = range(required(definition, "range"));
                yield restrictableLocator(
                    (world, x, z, columnMin, columnMax) -> scanPattern(
                        world,
                        x,
                        z,
                        Math.max(columnMin, search.minimum),
                        Math.min(columnMax, search.maximum),
                        columnMin,
                        columnMax,
                        pattern),
                    (world, x, z, columnMin, columnMax, candidates) -> scanPatternWithin(
                        world,
                        x,
                        z,
                        Math.max(columnMin, search.minimum),
                        Math.min(columnMax, search.maximum),
                        columnMin,
                        columnMax,
                        pattern,
                        candidates));
            }
            case "SURFACE" -> {
                IntRange search = range(required(definition, "range"));
                yield restrictableLocator(
                    (world, x, z, columnMin, columnMax) -> scanSurface(
                        x,
                        z,
                        Math.max(columnMin, search.minimum),
                        Math.min(columnMax, search.maximum)),
                    (world, x, z, columnMin, columnMax, candidates) -> scanSurfaceWithin(
                        x,
                        z,
                        Math.max(columnMin, search.minimum),
                        Math.min(columnMax, search.maximum),
                        candidates));
            }
            case "TOP" -> {
                IntRange search = range(required(definition, "range"));
                BitSet result = new BitSet();
                // Terra 6.4.2's TopLocator intentionally ignores the clamped biome column.
                yield (world, x, z, columnMin, columnMax) -> {
                    result.clear();
                    boolean currentAir = isAir(x, search.maximum, z);
                    for (int y = search.maximum; y >= search.minimum; y--) {
                        boolean belowAir = isAir(x, y - 1, z);
                        if (currentAir && !belowAir) {
                            setLocation(result, y);
                            return result;
                        }
                        currentAir = belowAir;
                    }
                    return result;
                };
            }
            case "RANDOM" -> randomLocator(definition, false);
            case "GAUSSIAN_RANDOM" -> randomLocator(definition, true);
            case "SAMPLER_3D" -> {
                NoiseSampler sampler = compiler.compileSampler(object(definition, "sampler"));
                yield restrictableLocator(
                    (world, x, z, columnMin,
                        columnMax) -> scanSampler(world, x, z, columnMin, columnMax, sampler, null),
                    (world, x, z, columnMin, columnMax,
                        candidates) -> scanSampler(world, x, z, columnMin, columnMax, sampler, candidates));
            }
            case "ADJACENT_PATTERN" -> {
                Pattern pattern = compilePattern(object(definition, "pattern"));
                IntRange search = range(required(definition, "range"));
                boolean matchAll = bool(definition, "match-all");
                yield restrictableLocator(
                    (world, x, z, columnMin, columnMax) -> scanAdjacentPattern(
                        world,
                        x,
                        z,
                        search.minimum,
                        search.maximum,
                        pattern,
                        matchAll,
                        null),
                    (world, x, z, columnMin, columnMax, candidates) -> scanAdjacentPattern(
                        world,
                        x,
                        z,
                        search.minimum,
                        search.maximum,
                        pattern,
                        matchAll,
                        candidates));
            }
            default -> throw new IllegalStateException("Unsupported Origen locator " + type);
        };
    }

    private static Locator compositeLocator(String type, List<Locator> children) {
        return new Locator() {

            @Override
            public BitSet locate(World world, int x, int z, int columnMin, int columnMax) {
                BitSet result = children.get(0)
                    .locate(world, x, z, columnMin, columnMax);
                for (int index = 1; index < children.size(); index++) {
                    if ("AND".equals(type)) {
                        if (result.isEmpty()) break;
                        result = children.get(index)
                            .locateWithin(world, x, z, columnMin, columnMax, result);
                    } else {
                        result.or(
                            children.get(index)
                                .locate(world, x, z, columnMin, columnMax));
                    }
                }
                return result;
            }

            @Override
            public BitSet locateWithin(World world, int x, int z, int columnMin, int columnMax, BitSet candidates) {
                if ("AND".equals(type)) {
                    BitSet result = (BitSet) candidates.clone();
                    for (int index = 0; index < children.size(); index++) {
                        if (result.isEmpty()) break;
                        result = children.get(index)
                            .locateWithin(world, x, z, columnMin, columnMax, result);
                    }
                    return result;
                }
                BitSet result = new BitSet();
                for (int index = 0; index < children.size(); index++) {
                    BitSet branch = (BitSet) candidates.clone();
                    result.or(
                        children.get(index)
                            .locateWithin(world, x, z, columnMin, columnMax, branch));
                }
                return result;
            }
        };
    }

    private static Locator restrictableLocator(Locator full, RestrictedLocator restricted) {
        return new Locator() {

            @Override
            public BitSet locate(World world, int x, int z, int columnMin, int columnMax) {
                return full.locate(world, x, z, columnMin, columnMax);
            }

            @Override
            public BitSet locateWithin(World world, int x, int z, int columnMin, int columnMax, BitSet candidates) {
                return restricted.locate(world, x, z, columnMin, columnMax, candidates);
            }
        };
    }

    private Locator randomLocator(JsonObject definition, boolean gaussian) {
        IntRange height = range(required(definition, "height"));
        IntRange amount = range(required(definition, "amount"));
        int salt = (int) number(definition, "salt", 0.0D);
        double mean = (height.maximum + height.minimum) / 2.0D;
        double deviation = gaussian ? compiler.numeric(required(definition, "standard-deviation")) : 0.0D;
        BitSet result = new BitSet();
        return (world, x, z, columnMin, columnMax) -> {
            long seed = 31L * (31L * world.getSeed() + x) + z + salt;
            locatorRandom.setSeed(seed);
            int count = amount.get(locatorRandom);
            result.clear();
            for (int index = 0; index < count; index++) {
                int y = gaussian ? (int) (locatorRandom.nextGaussian() * deviation + mean) : height.get(locatorRandom);
                if (y >= columnMin && y < columnMax) setLocation(result, y);
            }
            return result;
        };
    }

    private Pattern compilePattern(JsonObject definition) {
        String type = required(definition, "type").getAsString();
        return switch (type) {
            case "AND", "OR" -> {
                List<Pattern> children = new ArrayList<>();
                for (JsonElement child : array(definition, "patterns")) {
                    children.add(compilePattern(child.getAsJsonObject()));
                }
                if (children.isEmpty()) throw new IllegalStateException(type + " pattern has no children");
                yield new CompositePattern("AND".equals(type), children);
            }
            case "NOT" -> {
                Pattern child = compilePattern(object(definition, "pattern"));
                yield (world, x, y, z, columnMin, columnMax) -> !child.matches(world, x, y, z, columnMin, columnMax);
            }
            case "MATCH_AIR" -> new BlockPattern(
                range(required(definition, "offset")),
                BlockMatch.AIR,
                Collections.emptyList());
            case "MATCH_SOLID" -> new BlockPattern(
                range(required(definition, "offset")),
                BlockMatch.SOLID,
                Collections.emptyList());
            case "MATCH" -> {
                OrigenBlockStateResolver.ResolvedBlock state = resolve(required(definition, "block").getAsString());
                yield new BlockPattern(
                    range(required(definition, "offset")),
                    BlockMatch.STATE,
                    Collections.singletonList(state));
            }
            case "MATCH_SET" -> {
                List<OrigenBlockStateResolver.ResolvedBlock> blocks = new ArrayList<>();
                for (JsonElement rawBlock : array(definition, "blocks")) {
                    blocks.add(resolve(rawBlock.getAsString()));
                }
                yield new BlockPattern(range(required(definition, "offset")), BlockMatch.STATE, blocks);
            }
            default -> throw new IllegalStateException("Unsupported Origen pattern " + type);
        };
    }

    static boolean matchesAnyBlockState(List<OrigenBlockStateResolver.ResolvedBlock> expected, Block candidate,
        int candidateMetadata) {
        for (OrigenBlockStateResolver.ResolvedBlock state : expected) {
            if (state.matches(candidate, candidateMetadata)) return true;
        }
        return false;
    }

    static int clampedPatternMinimum(int authoredY, int offsetMinimum, int columnMinimum) {
        return Math.max(columnMinimum, authoredY + offsetMinimum);
    }

    static int clampedPatternMaximum(int authoredY, int offsetMaximum, int columnMaximum) {
        return Math.min(columnMaximum, authoredY + offsetMaximum);
    }

    private static BitSet scanPattern(World world, int x, int z, int minimum, int maximum, int columnMinimum,
        int columnMaximum, Pattern pattern) {
        BitSet result = new BitSet();
        int clampedMinimum = Math.max(minimum, OrigenDensityField.UPSTREAM_MIN_Y);
        int clampedMaximum = Math.min(maximum, OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE);
        if (clampedMinimum >= clampedMaximum) return result;
        result.set(location(clampedMinimum), location(clampedMaximum));
        pattern.retainMatches(world, x, z, columnMinimum, columnMaximum, result);
        return result;
    }

    private static BitSet scanPatternWithin(World world, int x, int z, int minimum, int maximum, int columnMinimum,
        int columnMaximum, Pattern pattern, BitSet candidates) {
        int clampedMinimum = Math.max(minimum, OrigenDensityField.UPSTREAM_MIN_Y);
        int clampedMaximum = Math.min(maximum, OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE);
        BitSet result = candidates;
        if (clampedMinimum >= clampedMaximum) {
            result.clear();
            return result;
        }
        result.clear(0, location(clampedMinimum));
        result.clear(location(clampedMaximum), Math.max(location(clampedMaximum), result.length()));
        pattern.retainMatches(world, x, z, columnMinimum, columnMaximum, result);
        return result;
    }

    private BitSet scanSurface(int x, int z, int minimum, int maximum) {
        if (populationView != null && locatorOrigin == null) {
            return populationView.scanSurface(x, z, minimum, maximum, null);
        }
        BitSet result = new BitSet();
        if (minimum >= maximum) return result;
        boolean belowAir = isAir(x, minimum - 1, z);
        for (int y = minimum; y < maximum; y++) {
            boolean currentAir = isAir(x, y, z);
            if (currentAir && !belowAir) setLocation(result, y);
            belowAir = currentAir;
        }
        return result;
    }

    private BitSet scanSurfaceWithin(int x, int z, int minimum, int maximum, BitSet candidates) {
        if (populationView != null && locatorOrigin == null) {
            return populationView.scanSurface(x, z, minimum, maximum, candidates);
        }
        int clampedMinimum = Math.max(minimum, OrigenDensityField.UPSTREAM_MIN_Y);
        int clampedMaximum = Math.min(maximum, OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE);
        if (clampedMinimum >= clampedMaximum) {
            candidates.clear();
            return candidates;
        }
        candidates.clear(0, location(clampedMinimum));
        candidates.clear(location(clampedMaximum), Math.max(location(clampedMaximum), candidates.length()));
        for (int candidate = candidates.nextSetBit(0); candidate
            >= 0; candidate = candidates.nextSetBit(candidate + 1)) {
            int y = authoredY(candidate);
            if (!isAir(x, y, z) || isAir(x, y - 1, z)) candidates.clear(candidate);
        }
        return candidates;
    }

    private static BitSet scanSampler(World world, int x, int z, int minimum, int maximum, NoiseSampler sampler,
        BitSet candidates) {
        long seed = world.getSeed();
        if (candidates == null) {
            BitSet result = new BitSet();
            for (int y = minimum; y < maximum; y++) {
                if (sampler.noise(seed, x, y, z) > 0.0D) setLocation(result, y);
            }
            return result;
        }
        int clampedMinimum = Math.max(minimum, OrigenDensityField.UPSTREAM_MIN_Y);
        int clampedMaximum = Math.min(maximum, OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE);
        if (clampedMinimum >= clampedMaximum) {
            candidates.clear();
            return candidates;
        }
        candidates.clear(0, location(clampedMinimum));
        candidates.clear(location(clampedMaximum), Math.max(location(clampedMaximum), candidates.length()));
        for (int candidate = candidates.nextSetBit(0); candidate
            >= 0; candidate = candidates.nextSetBit(candidate + 1)) {
            if (sampler.noise(seed, x, authoredY(candidate), z) <= 0.0D) candidates.clear(candidate);
        }
        return candidates;
    }

    private static BitSet scanAdjacentPattern(World world, int x, int z, int minimum, int maximum, Pattern pattern,
        boolean matchAll, BitSet candidates) {
        BitSet result = scanPatternColumn(
            world,
            x,
            z - 1,
            minimum,
            maximum,
            pattern,
            candidates == null ? null : (BitSet) candidates.clone());
        if (matchAll && result.isEmpty()) return result;
        combineAdjacent(
            result,
            scanPatternColumn(
                world,
                x,
                z + 1,
                minimum,
                maximum,
                pattern,
                candidates == null ? null : (BitSet) candidates.clone()),
            matchAll);
        if (matchAll && result.isEmpty()) return result;
        combineAdjacent(
            result,
            scanPatternColumn(
                world,
                x - 1,
                z,
                minimum,
                maximum,
                pattern,
                candidates == null ? null : (BitSet) candidates.clone()),
            matchAll);
        if (matchAll && result.isEmpty()) return result;
        combineAdjacent(
            result,
            scanPatternColumn(
                world,
                x + 1,
                z,
                minimum,
                maximum,
                pattern,
                candidates == null ? null : (BitSet) candidates.clone()),
            matchAll);
        return result;
    }

    private static BitSet scanPatternColumn(World world, int x, int z, int minimum, int maximum, Pattern pattern,
        BitSet candidates) {
        return candidates == null
            ? scanPattern(
                world,
                x,
                z,
                minimum,
                maximum,
                OrigenDensityField.UPSTREAM_MIN_Y,
                OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE,
                pattern)
            : scanPatternWithin(
                world,
                x,
                z,
                minimum,
                maximum,
                OrigenDensityField.UPSTREAM_MIN_Y,
                OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE,
                pattern,
                candidates);
    }

    private static void combineAdjacent(BitSet result, BitSet next, boolean matchAll) {
        if (matchAll) {
            result.and(next);
        } else {
            result.or(next);
        }
    }

    private static void setLocation(BitSet locations, int authoredY) {
        locations.set(location(authoredY));
    }

    private static int location(int authoredY) {
        return authoredY - OrigenDensityField.UPSTREAM_MIN_Y;
    }

    private static int authoredY(int location) {
        return location + OrigenDensityField.UPSTREAM_MIN_Y;
    }

    private void validateReferences() {
        for (Feature feature : features.values()) {
            for (WeightedStructure choice : feature.choices) {
                if (choice.id.startsWith("BLOCK:")) {
                    resolve(choice.id.substring("BLOCK:".length()));
                } else if (!structures.containsKey(choice.id)) {
                    throw new IllegalStateException(
                        "Feature " + feature.id + " references missing structure " + choice.id);
                }
            }
        }
    }

    private SchematicStructure decodeSchematic(String id, String encoded) {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(
            Base64.getDecoder()
                .decode(encoded))) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(stream);
            int version = root.getInteger("Version");
            int width = root.getShort("Width");
            int length = root.getShort("Length");
            int height = root.getShort("Height");
            int offsetX = 0;
            int offsetY = 0;
            int offsetZ = 0;
            if (version == 2) {
                NBTTagCompound metadata = root.getCompoundTag("Metadata");
                if (metadata.hasKey("WEOffsetX") || metadata.hasKey("WEOffsetY") || metadata.hasKey("WEOffsetZ")) {
                    if (!(metadata.hasKey("WEOffsetX") && metadata.hasKey("WEOffsetY")
                        && metadata.hasKey("WEOffsetZ"))) {
                        throw new IllegalStateException("Malformed WorldEdit offset in " + id);
                    }
                    offsetX = metadata.getInteger("WEOffsetX");
                    offsetY = metadata.getInteger("WEOffsetY");
                    offsetZ = metadata.getInteger("WEOffsetZ");
                }
            } else if (version == 3) {
                int[] offset = root.getIntArray("Offset");
                offsetX = offset[0];
                offsetY = offset[1];
                offsetZ = offset[2];
            } else {
                throw new IllegalStateException("Unsupported Sponge schematic version " + version + " in " + id);
            }
            NBTTagCompound paletteTag = root.getCompoundTag("Palette");
            Map<Integer, OrigenBlockStateResolver.ResolvedBlock> palette = new HashMap<>();
            @SuppressWarnings("unchecked")
            Set<String> keys = paletteTag.func_150296_c();
            for (String state : keys) {
                if (!state.startsWith("minecraft:structure_void")) {
                    palette.put(Integer.valueOf(paletteTag.getInteger(state)), resolve(state));
                }
            }
            return new SchematicStructure(
                width,
                length,
                height,
                offsetX,
                offsetY,
                offsetZ,
                root.getByteArray("BlockData"),
                palette);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Could not decode Origen schematic " + id, exception);
        }
    }

    private OrigenBlockStateResolver.ResolvedBlock resolve(String state) {
        OrigenBlockStateResolver.ResolvedBlock cached = blockStateCache.get(state);
        if (cached != null) return cached;
        OrigenBlockStateResolver.ResolvedBlock resolved;
        if (state.startsWith("regions_unexplored:")) {
            industries.angelic.biomic.world.feature.RuDecorationStateResolver.Resolved ru = industries.angelic.biomic.world.modern.ModernStateCompatibility
                .resolve(state);
            resolved = OrigenBlockStateResolver.imported(state, ru.getBlock(), ru.getMetadata());
        } else {
            resolved = blockResolver.resolve(state);
        }
        blockStateCache.put(state, resolved);
        if (resolved.compatibilitySubstitute()) compatibilitySubstitutes.add(resolved.canonicalId());
        return resolved;
    }

    private WorldBlock blockAt(int x, int authoredY, int z) {
        if (authoredY < OrigenDensityField.UPSTREAM_MIN_Y || authoredY >= OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE) {
            return WorldBlock.AIR;
        }
        if (populationView != null && locatorOrigin == null) return populationView.authoredBlock(x, authoredY, z);
        AuthoredFeatureBlock modified = authoredFeatureBlock(x, authoredY, z);
        return blockAtWithKnownFeature(x, authoredY, z, modified);
    }

    /** Resolves an authored cell after its sparse feature entry has already been fetched by a hot structure loop. */
    private WorldBlock blockAtWithKnownFeature(int x, int authoredY, int z, AuthoredFeatureBlock modified) {
        if (modified != null) {
            if (locatorOrigin == null || modified.origin() == null
                || modified.origin()
                    .compareTo(locatorOrigin) <= 0) {
                return modified.block();
            }
        }
        byte material = populationView == null
            ? authoredTerrainColumn(x, z)[authoredY - OrigenDensityField.UPSTREAM_MIN_Y]
            : populationView.authoredTerrainMaterial(x, authoredY, z);
        return resolveTerrainMaterial(material);
    }

    private AuthoredFeatureBlock authoredFeatureBlock(int x, int authoredY, int z) {
        if (populationView != null) return populationView.authoredFeatureBlock(x, authoredY, z);
        synchronized (authoredFeatureCache) {
            AuthoredFeatureColumn[] chunk = authoredFeatureCache.getAndMoveToLast(horizontalKey(x >> 4, z >> 4));
            return indexedAuthoredFeatureBlock(chunk, x, authoredY, z);
        }
    }

    private boolean writeAuthoredFeatureBlock(int x, int authoredY, int z,
        OrigenBlockStateResolver.ResolvedBlock resolved) {
        if (authoredY < OrigenDensityField.UPSTREAM_MIN_Y || authoredY >= OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE)
            return false;
        WorldBlock replacement = resolvedWorldBlock(resolved);
        boolean sameFeatureId = activeFeatureId == lastAuthoredFeatureId
            || activeFeatureId != null && activeFeatureId.equals(lastAuthoredFeatureId);
        AuthoredFeatureBlock written;
        if (resolved == lastAuthoredResolved && activeFeatureOrigin == lastAuthoredOrigin
            && activeDependencyOrigin == lastAuthoredDependencyOrigin
            && sameFeatureId) {
            written = lastAuthoredFeatureBlock;
        } else {
            written = new AuthoredFeatureBlock(
                replacement,
                activeFeatureOrigin,
                activeFeatureId,
                activeDependencyOrigin);
            lastAuthoredResolved = resolved;
            lastAuthoredOrigin = activeFeatureOrigin;
            lastAuthoredDependencyOrigin = activeDependencyOrigin;
            lastAuthoredFeatureId = activeFeatureId;
            lastAuthoredFeatureBlock = written;
        }
        if (populationView != null) {
            boolean changed = populationView.writeAuthoredFeatureBlock(x, authoredY, z, written);
            return changed;
        }
        synchronized (authoredFeatureCache) {
            long chunkKey = horizontalKey(x >> 4, z >> 4);
            AuthoredFeatureColumn[] chunk = authoredFeatureCache.getAndMoveToLast(chunkKey);
            if (chunk == null) {
                chunk = new AuthoredFeatureColumn[AUTHORED_FEATURE_COLUMN_COUNT];
                authoredFeatureCache.putAndMoveToLast(chunkKey, chunk);
                trimAuthoredFeatureCache();
            }
            AuthoredFeatureBlock previous = setIndexedAuthoredFeatureBlock(chunk, x, authoredY, z, written);
            return previous == null || previous.block()
                .block() != replacement.block()
                || previous.block()
                    .metadata() != replacement.metadata();
        }
    }

    private WorldBlock resolvedWorldBlock(OrigenBlockStateResolver.ResolvedBlock resolved) {
        WorldBlock cached = resolvedWorldBlockCache.get(resolved);
        if (cached != null) return cached;
        WorldBlock created = new WorldBlock(resolved.block(), resolved.metadata());
        resolvedWorldBlockCache.put(resolved, created);
        return created;
    }

    /** Caller holds {@link #authoredFeatureCache}'s monitor. */
    private void trimAuthoredFeatureCache() {
        if (authoredFeatureCache.size() <= AUTHORED_FEATURE_CACHE_SIZE) return;
        authoredFeatureCache.removeFirst();
    }

    private static AuthoredFeatureBlock indexedAuthoredFeatureBlock(AuthoredFeatureColumn[] chunk, int x, int authoredY,
        int z) {
        if (chunk == null || authoredY < OrigenDensityField.UPSTREAM_MIN_Y
            || authoredY >= OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE) return null;
        AuthoredFeatureColumn column = chunk[authoredFeatureColumnIndex(x, z)];
        return column == null ? null : column.get(authoredY - OrigenDensityField.UPSTREAM_MIN_Y);
    }

    private static AuthoredFeatureBlock setIndexedAuthoredFeatureBlock(AuthoredFeatureColumn[] chunk, int x,
        int authoredY, int z, AuthoredFeatureBlock block) {
        if (chunk == null || authoredY < OrigenDensityField.UPSTREAM_MIN_Y
            || authoredY >= OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE) return null;
        int columnIndex = authoredFeatureColumnIndex(x, z);
        AuthoredFeatureColumn column = chunk[columnIndex];
        if (column == null) {
            if (block == null) return null;
            column = new AuthoredFeatureColumn();
            chunk[columnIndex] = column;
        }
        return column.set(authoredY - OrigenDensityField.UPSTREAM_MIN_Y, block);
    }

    private static int authoredFeatureColumnIndex(int x, int z) {
        return (x & 15) << 4 | z & 15;
    }

    private byte[] authoredTerrainColumn(int x, int z) {
        authoredColumnFallbacks++;
        return terrainAccess.authoredColumn(x, z, () -> terrainAccess.createPhysicalMaterials(x >> 4, z >> 4));
    }

    private WorldBlock resolveTerrainMaterial(byte material) {
        int index = material & 0xFF;
        WorldBlock cached = terrainMaterialCache[index];
        if (cached != null) return cached;
        byte[] singleton = { material };
        Block[] blocks = ChunkProviderBiomic.resolveBlocks(singleton);
        byte[] metadata = ChunkProviderBiomic.resolveMetadata(singleton);
        WorldBlock resolved = new WorldBlock(
            blocks[0] == null ? net.minecraft.init.Blocks.air : blocks[0],
            metadata[0] & 0xFF);
        terrainMaterialCache[index] = resolved;
        return resolved;
    }

    private boolean isAir(int x, int authoredY, int z) {
        if (populationView != null && locatorOrigin == null
            && authoredY >= OrigenDensityField.UPSTREAM_MIN_Y
            && authoredY < OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE) {
            return populationView.authoredAir(x, authoredY, z);
        }
        return blockAt(x, authoredY, z).block() == net.minecraft.init.Blocks.air;
    }

    /** Terra's Bukkit platform implements MATCH_SOLID with Material.isOccluding(), not movement solidity. */
    static boolean isTerraOccluding(Block block) {
        // BlockLeaves reports opaque on a dedicated 1.7 server because its client-only graphics flag defaults to
        // fast mode. Terra's Material.isOccluding() is immutable, so exclude the leaf material before consulting the
        // otherwise equivalent full-cube predicate.
        return block != null && block.getMaterial() != Material.leaves && block.isOpaqueCube();
    }

    private static int legacyPhysicalY(int authoredY) {
        return OrigenVerticalProfileSampler.toBiomicBlockY(authoredY);
    }

    private IntRange range(JsonElement definition) {
        if (definition.isJsonObject()) {
            JsonObject object = definition.getAsJsonObject();
            return new IntRange(
                (int) compiler.numeric(required(object, "min")),
                (int) compiler.numeric(required(object, "max")));
        }
        int value = (int) compiler.numeric(definition);
        return new IntRange(value, value + 1);
    }

    @Override
    public boolean placeBlock(World world, int x, int y, int z, int authoredY, String state, boolean overwrite,
        boolean physics, boolean waterlog) {
        if (activeLeafSubstitute != null && state.contains("_leaves")) state = activeLeafSubstitute;
        // Origen's bamboo in the species its feature's leaf decor names, else the default one (OrigenLeafDecor), else
        // as Origen draws it. PMP bamboo stalk: metadata 0 along the stalk, 4 for the leafy top (PMP's spawnBamboo).
        String species = activeBamboo != null ? activeBamboo : OrigenLeafDecor.defaultBamboo();
        if (species != null && state.startsWith("minecraft:bamboo")) {
            Block bamboo = Block.getBlockFromName(species);
            String pmp = bamboo == null ? null : paletteState(bamboo, state.contains("stage=1") ? 4 : 0);
            if (pmp != null) state = pmp;
        }
        return placeResolvedBlock(world, x, z, authoredY, resolve(state), overwrite, physics);
    }

    private boolean placeResolvedBlock(World world, int x, int z, int authoredY,
        OrigenBlockStateResolver.ResolvedBlock resolved, boolean overwrite, boolean physics) {
        if (authoredY < OrigenDensityField.UPSTREAM_MIN_Y || authoredY >= OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE
            || (!overwrite && blockAt(x, authoredY, z).block() != net.minecraft.init.Blocks.air)) return false;
        return placeResolvedBlockWithKnownFeature(
            world,
            x,
            z,
            authoredY,
            resolved,
            physics,
            authoredFeatureBlock(x, authoredY, z));
    }

    /** Completes a placement using the feature cell fetched by the caller in the same single-threaded pass. */
    private boolean placeResolvedBlockWithKnownFeature(World world, int x, int z, int authoredY,
        OrigenBlockStateResolver.ResolvedBlock resolved, boolean physics, AuthoredFeatureBlock existing) {
        int projectedY = heightTransport.physicalY(authoredY);
        // A wide structure rooted outside a village's reserved area that reaches into it goes whole: its
        // transaction (opened near villages) is rejected, so no mushroom or boulder is left cut along the margin.
        if (isProtectedSurface(x, projectedY, z, false)) {
            // A tree or landform goes whole; a patch of plants only loses the plants on the guarded cells.
            if (populationView != null && populationView.hostedEnvironmentTransaction != null
                && ("trees".equals(activeFeatureStage) || "landforms".equals(activeFeatureStage)))
                populationView.hostedEnvironmentTransactionRejected = true;
            return false;
        }
        if (resolved.block() == Blocks.potatoes && !hasValidPotatoSupport(
            blockAt(x, authoredY - 1, z).block(),
            projectedY > 0 ? readPhysical(world, x, projectedY - 1, z).block() : Blocks.air)) {
            return false;
        }
        if (activeLargeMushroomColumns != null) activeLargeMushroomColumns.add(horizontalKey(x, z));
        if (activeFeatureOrigin != null && existing != null
            && existing.origin() != null
            && existing.origin()
                .compareTo(activeFeatureOrigin) > 0) {
            // The later writer already ran because chunks arrived backwards. It owns the canonical final cell, but
            // locator reads above deliberately ignored it so the earlier structure could still generate around it.
            if (activeRejectedMushroomPlacements != null) {
                activeRejectedMushroomPlacements
                    .put(blockKey(x, authoredY, z), new RejectedMushroomPlacement(x, authoredY, z, resolved, physics));
            }
            return false;
        }
        boolean authoredWritten = writeAuthoredFeatureBlock(x, authoredY, z, resolved);
        if (activeOccludableTreeBlocks != null) activeOccludableTreeBlocks.add(blockKey(x, authoredY, z));
        if (activeDependentStructureBlocks != null) activeDependentStructureBlocks.add(blockKey(x, authoredY, z));
        // Terra structures reason and collide in their exact authored 384-high space. Project only the accepted
        // write, so scripts never observe a rounded/collided 1.7 block while deciding the rest of the structure.
        if (heightTransport.compressed() && "minecraft:sculk_vein".equals(resolved.canonicalId())) {
            projectedY = alignSurfaceAttachmentY(world, x, projectedY, z, resolved);
        } else if (heightTransport.compressed() && isSingleBlockGroundCover(resolved.canonicalId())) {
            projectedY = alignGroundCoverY(world, x, projectedY, z);
        }
        // The upper-world squeeze occasionally folds two neighboring authored layers into the same physical layer.
        // Preserve a structural branch from the sibling layer instead of allowing a later foliage write to flatten
        // it. A leaf that genuinely replaces wood at the same authored coordinate still follows Terra's write order.
        if (heightTransport.compressed() && resolved.block()
            .getMaterial() == Material.leaves
            && hasProjectedWoodSibling(x, projectedY, z, authoredY)
            && shouldPreserveProjectedWood(resolved.block(), readPhysical(world, x, projectedY, z).block())) {
            return authoredWritten;
        }
        // The two authored seagrass halves can alias at the sea-level squeeze. Preserve
        // one valid short plant in that water cell, not an upper half sitting directly on sand.
        if (heightTransport.compressed() && repairProjectedLandPlants
            && (resolved.canonicalId()
                .equals("minecraft:tall_grass")
                || resolved.canonicalId()
                    .equals("minecraft:large_fern"))) {
            return writeProjectedLandPlantPair(world, x, authoredY, z, resolved, physics) || authoredWritten;
        }
        String aquatic = heightTransport.compressed() ? projectedAquaticState(canonicalState(resolved), authoredY)
            : null;
        OrigenBlockStateResolver.ResolvedBlock physical = aquatic == null ? resolved : resolve(aquatic);
        boolean physicalWritten = heightTransport.containsPhysicalY(projectedY)
            && writePhysical(world, x, projectedY, z, physical, physics);
        return authoredWritten || physicalWritten;
    }

    static String projectedAquaticState(String state, int authoredY) {
        if (!state.startsWith("minecraft:tall_seagrass[")) return null;
        int lower = state.contains("half=upper") ? authoredY - 1 : authoredY;
        return legacyPhysicalY(lower) == legacyPhysicalY(lower + 1) ? "minecraft:seagrass" : null;
    }

    /** Keep the authored pair intact; only its collapsed physical projection becomes one short plant. */
    static String projectedLandPlantState(String state, int authoredY) {
        String shortPlant = state.startsWith("minecraft:tall_grass[") ? "minecraft:grass"
            : state.startsWith("minecraft:large_fern[") ? "minecraft:fern" : null;
        if (shortPlant == null) return null;
        int lower = state.contains("half=upper") ? authoredY - 1 : authoredY;
        return legacyPhysicalY(lower) == legacyPhysicalY(lower + 1) ? shortPlant : null;
    }

    private boolean writeProjectedLandPlantPair(World world, int x, int authoredY, int z,
        OrigenBlockStateResolver.ResolvedBlock resolved, boolean physics) {
        int lower = resolved.properties()
            .getOrDefault("half", "lower")
            .equals("upper") ? authoredY - 1 : authoredY;
        AuthoredFeatureBlock bottom = authoredFeatureBlock(x, lower, z), top = authoredFeatureBlock(x, lower + 1, z);
        int species = resolved.canonicalId()
            .equals("minecraft:tall_grass") ? 2 : 3;
        // Terra's tall_grass script deliberately writes the upper half first. Keep that
        // authored order, but publish neither physical half until the same feature owns both.
        if (bottom == null || top == null
            || bottom.block()
                .block() != Blocks.double_plant
            || top.block()
                .block() != Blocks.double_plant
            || bottom.block()
                .metadata() != species
            || (top.block()
                .metadata() & 8) == 0
            || !java.util.Objects.equals(bottom.origin(), top.origin())
            || !java.util.Objects.equals(bottom.featureId(), top.featureId())) return false;
        int y = heightTransport.physicalY(lower), upperY = heightTransport.physicalY(lower + 1);
        if (y < 1 || !heightTransport.containsPhysicalY(upperY)
            || isProtectedSurface(x, y, z, false)
            || isProtectedSurface(x, upperY, z, false)) return false;
        WorldBlock oldBottom = readPhysical(world, x, y, z), oldTop = readPhysical(world, x, upperY, z);
        if (oldBottom.block() != Blocks.air
            && !(oldBottom.block() == Blocks.double_plant && oldBottom.metadata() == species)) return false;
        if (upperY != y && oldTop.block() != Blocks.air
            && !(oldTop.block() == Blocks.double_plant && (oldTop.metadata() & 8) != 0)) return false;
        String shortPlant = projectedLandPlantState(resolved.canonicalId() + "[half=lower]", lower);
        if (shortPlant != null) return writePhysical(world, x, y, z, resolve(shortPlant), physics);
        var lowerState = resolve(resolved.canonicalId() + "[half=lower]");
        var upperState = resolve(resolved.canonicalId() + "[half=upper]");
        boolean changed = writePhysical(world, x, y, z, lowerState, physics);
        WorldBlock actual = readPhysical(world, x, y, z);
        if (actual.block() != Blocks.double_plant || actual.metadata() != species) return false;
        changed = writePhysical(world, x, upperY, z, upperState, physics) || changed;
        actual = readPhysical(world, x, upperY, z);
        if (actual.block() == Blocks.double_plant && (actual.metadata() & 8) != 0) return changed;
        // Both cells are in one column and preflight admits only air/the same plant.
        // A later support guard must still never leave a newly published lower orphan.
        if (!oldBottom.equals(readPhysical(world, x, y, z))) writePhysical(
            world,
            x,
            y,
            z,
            resolve(
                industries.angelic.biomic.world.modern.ModernStateCompatibility
                    .modernState(oldBottom.block(), oldBottom.metadata())),
            false);
        return false;
    }

    static boolean isProtectedSurface(int physicalY, int naturalSurfaceY) {
        return physicalY >= naturalSurfaceY - VILLAGE_SURFACE_DEPTH;
    }

    static boolean absentDiskNaturalCarrierTarget(boolean anvilLoader, boolean chunkExists) {
        return anvilLoader && !chunkExists;
    }

    static boolean freshNaturalSoilTarget(boolean loaded, boolean populationComplete, boolean settlementRequired,
        boolean absentOnDisk, boolean freshUnexposedGeneration) {
        return loaded ? !populationComplete || settlementRequired || freshUnexposedGeneration : absentOnDisk;
    }

    /**
     * A fill cell is held when its column, and every open neighbour column, reaches a floor going down through the
     * band (rock or fluid); a column whose band air runs on below the band's bottom would drain into the cave there.
     */
    private static final String FILL_CRUST = "minecraft:basalt[axis=y]";
    private final Map<Long, List<ProfileRange>> fillRanges = new java.util.HashMap<>();

    /**
     * Whether every open horizontal neighbour of a fill cell will be filled as well: its stage tile's profile at this
     * height runs the same fill, and it rests on a floor. Pure terrain and profile, so the answer does not depend on
     * the order columns are filled in.
     */
    private boolean fillContained(World world, Feature feature, int stageOrdinal, int x, int authoredY, int physicalY,
        int z, int bandBottom) {
        int[][] sides = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] d : sides) {
            int nx = x + d[0], nz = z + d[1];
            net.minecraft.block.material.Material side = readPhysical(world, nx, physicalY, nz).block()
                .getMaterial();
            if (side.isSolid() || side.isLiquid()) continue;
            if (!fills(world, feature, stageOrdinal, nx, authoredY, nz)
                || !floored(world, nx, physicalY - 1, nz, bandBottom)) return false;
        }
        return true;
    }

    private boolean fills(World world, Feature feature, int stageOrdinal, int x, int authoredY, int z) {
        if (!feature.distributor.matches(x, z, world.getSeed())) return false;
        int tileX = x - Math.floorMod(x, STAGE_RESOLUTION), tileZ = z - Math.floorMod(z, STAGE_RESOLUTION);
        long key = (long) tileX << 32 ^ tileZ & 0xFFFFFFFFL;
        if (fillRanges.size() > 4096) fillRanges.clear();
        List<ProfileRange> ranges = fillRanges.computeIfAbsent(key, k -> profileRanges(tileX, tileZ));
        for (ProfileRange range : ranges) {
            if (authoredY < range.minimumY || authoredY >= range.maximumY) continue;
            for (Feature other : profileFeatures.get(range.profile)
                .getOrDefault(stages.get(stageOrdinal), Collections.emptyList()))
                if (other.id.equals(feature.id)) return true;
            return false;
        }
        return false;
    }

    private boolean floored(World world, int x, int physicalY, int z, int bandBottom) {
        for (int y = physicalY; heightTransport.containsPhysicalY(y); y--) {
            net.minecraft.block.material.Material below = readPhysical(world, x, y, z).block()
                .getMaterial();
            if (below.isSolid() || below.isLiquid()) return true;
            if (y < bandBottom) return false;
        }
        return true;
    }

    /** Whether a feature rooted here could reach a village's protected surface (24 blocks covers the widest). */
    private boolean nearVillageSurface(int blockX, int physicalY, int blockZ) {
        return surfaceProtection.near(blockX, physicalY, blockZ, 24);
    }

    /** Snow lies on villages and structures as on everything else: guarding it left bare squares in the snow. */
    private static final Set<String> SNOW_COVER = Set
        .of("SNOW_LAYERS", "SINKHOLE_SNOW_LAYERS", "ICEBERG_SNOW_LAYERS", "TREE_SNOW", "TREE_SNOW_CANYON");

    private boolean isProtectedSurface(int blockX, int physicalY, int blockZ, boolean bulky) {
        if (activeFeatureId != null && SNOW_COVER.contains(activeFeatureId)) return false;
        if (!surfaceProtection.contains(blockX, physicalY, blockZ, bulky)) return false;
        return isProtectedSurface(physicalY, terrainAccess.carvedGroundSurfaceHeight(blockX, blockZ));
    }

    /** Both Terra's authored view and the final 1.7 projection must support the actual vanilla crop. */
    private static boolean hasValidPotatoSupport(Block authoredSupport, Block physicalSupport) {
        return authoredSupport == Blocks.farmland && physicalSupport == Blocks.farmland;
    }

    private boolean hasProjectedWoodSibling(int x, int projectedY, int z, int authoredY) {
        for (int candidateY : heightTransport.authoredYsAtPhysical(projectedY)) {
            if (candidateY == authoredY) continue;
            AuthoredFeatureBlock sibling = authoredFeatureBlock(x, candidateY, z);
            if (sibling != null && sibling.block()
                .block()
                .getMaterial() == Material.wood) return true;
        }
        return false;
    }

    static boolean shouldPreserveProjectedWood(Block replacement, Block current) {
        return replacement != null && replacement.getMaterial() == Material.leaves
            && current != null
            && current.getMaterial() == Material.wood;
    }

    /**
     * Continuous terrain resampling and discrete feature projection can disagree by one target voxel where the
     * vertical squeeze is fractional. Keep only independent ground-cover blocks attached; paired and hanging plants
     * retain their authored structure projection.
     */
    private int alignGroundCoverY(World world, int x, int projectedY, int z) {
        for (int offset : GROUND_COVER_Y_OFFSETS) {
            int candidateY = projectedY + offset;
            if (candidateY <= 0 || !heightTransport.containsPhysicalY(candidateY)) continue;
            if (readPhysical(world, x, candidateY, z).block() == net.minecraft.init.Blocks.air
                && isTerraOccluding(readPhysical(world, x, candidateY - 1, z).block())) {
                return candidateY;
            }
        }
        return projectedY;
    }

    /** Keeps a six-face decal in compressed cave air instead of replacing the rock face it was attached to. */
    private int alignSurfaceAttachmentY(World world, int x, int projectedY, int z,
        OrigenBlockStateResolver.ResolvedBlock resolved) {
        for (int offset : GROUND_COVER_Y_OFFSETS) {
            int candidateY = projectedY + offset;
            if (candidateY <= 0 || !heightTransport.containsPhysicalY(candidateY + 1)) continue;
            if (readPhysical(world, x, candidateY, z).block() != net.minecraft.init.Blocks.air) continue;
            Map<String, String> properties = resolved.properties();
            if (attached(properties, "north", readPhysical(world, x, candidateY, z - 1).block())
                || attached(properties, "east", readPhysical(world, x + 1, candidateY, z).block())
                || attached(properties, "south", readPhysical(world, x, candidateY, z + 1).block())
                || attached(properties, "west", readPhysical(world, x - 1, candidateY, z).block())
                || attached(properties, "up", readPhysical(world, x, candidateY + 1, z).block())
                || attached(properties, "down", readPhysical(world, x, candidateY - 1, z).block())) return candidateY;
        }
        return projectedY;
    }

    private static boolean attached(Map<String, String> properties, String face, Block support) {
        return Boolean.parseBoolean(properties.get(face)) && isTerraOccluding(support);
    }

    static boolean isSingleBlockGroundCover(String canonicalId) {
        return switch (canonicalId) {
            case "minecraft:grass", "minecraft:fern", "minecraft:dead_bush", "minecraft:brown_mushroom", "minecraft:red_mushroom", "minecraft:dandelion", "minecraft:poppy", "minecraft:blue_orchid", "minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip", "minecraft:orange_tulip", "minecraft:white_tulip", "minecraft:pink_tulip", "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:lily_of_the_valley", "minecraft:moss_carpet", "minecraft:azalea", "minecraft:flowering_azalea", "minecraft:pink_petals", "minecraft:sweet_berry_bush", "minecraft:beetroots", "minecraft:sculk_sensor", "minecraft:sculk_shrieker" -> true;
            default -> false;
        };
    }

    @Override
    public String getBlockId(World world, int x, int y, int z, int authoredY) {
        if (!heightTransport.containsAuthoredY(authoredY)) return "minecraft:air";
        WorldBlock current = blockAt(x, authoredY, z);
        return blockResolver.canonicalId(current.block(), current.metadata());
    }

    /**
     * TerraScript evaluates its physical argument before the called operation can reject an out-of-envelope authored
     * coordinate. Preserve the legacy projection's historical clamping, but return an invalid storage coordinate for
     * shifted overflow so a wide script is rejected without being folded into either headroom band.
     */
    @Override
    public int physicalY(int authoredY) {
        if (heightTransport.compressed()) return legacyPhysicalY(authoredY);
        return heightTransport.containsAuthoredY(authoredY) ? heightTransport.physicalY(authoredY) : Integer.MIN_VALUE;
    }

    @Override
    public boolean supportsCeilingAttachment(World world, int x, int y, int z, int authoredY) {
        if (!heightTransport.containsAuthoredY(authoredY)) return false;
        Block block = blockAt(x, authoredY, z).block();
        return block.getMaterial()
            .isSolid() && block.getMaterial() != Material.leaves
            && block.renderAsNormalBlock();
    }

    @Override
    public boolean generateStructure(String id, World world, int originX, int originY, int originZ, int authoredOriginY,
        int offsetX, int offsetY, int offsetZ, int rotationDegrees, Random random, int recursions) {
        if (recursions > MAX_RECURSIONS) throw new IllegalStateException("Origen structure recursion exceeded " + id);
        int targetAuthoredY = authoredOriginY + offsetY;
        if (!heightTransport.compressed() && !heightTransport.containsAuthoredY(targetAuthoredY)) return false;
        Structure structure = structure(id);
        if (structure instanceof ScriptStructure script) {
            return script.script.generate(
                this,
                world,
                originX,
                originY,
                originZ,
                authoredOriginY,
                offsetX,
                offsetY,
                offsetZ,
                rotationDegrees,
                random,
                recursions);
        }
        if (!heightTransport.containsAuthoredY(targetAuthoredY)) return false;
        return structure.generate(
            world,
            originX + offsetX,
            heightTransport.physicalY(targetAuthoredY),
            originZ + offsetZ,
            targetAuthoredY,
            rotationDegrees,
            random);
    }

    @Override
    public double sampleNamed(String id, long seed, double x, double y, double z, boolean threeDimensional) {
        return compiler.sampleNamed(id, seed, x, z, threeDimensional);
    }

    @Override
    public String checkDensity(double x, double authoredY, double z) {
        if (authoredY >= OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE || authoredY < OrigenDensityField.UPSTREAM_MIN_Y)
            return "AIR";
        if (terrainAccess.isExpansionCavity(
            (int) Math.floor(x),
            heightTransport.physicalY((int) Math.floor(authoredY)),
            (int) Math.floor(z))) return "AIR";
        return densityField.sampleUpstreamInterpolated(x, authoredY, z) > 0.0D ? "LAND" : "AIR";
    }

    @Override
    public void spawnEntity(World world, int x, int y, int z, String id) {
        if (populationView != null && populationView.world == world
            && populationView.hostedEnvironmentTransaction != null) {
            if (!heightTransport.containsPhysicalY(y)
                || populationView.hostedEnvironmentEntities.size() >= HOSTED_ENVIRONMENT_TRANSACTION_CAP) {
                populationView.hostedEnvironmentTransactionRejected = true;
                return;
            }
            populationView.hostedEnvironmentEntities.add(new PendingEntity(x, y, z, id));
            return;
        }
        if (!heightTransport.containsPhysicalY(y)) return;
        if (populationView != null && populationView.world == world && populationView.journal != null) {
            populationView.journal.entities.add(new PendingEntity(x, y, z, id));
            return;
        }
        if (populationView != null && populationView.world == world && !populationView.isLoaded(x >> 4, z >> 4)) {
            populationView.queueEntity(x, y, z, id);
            return;
        }
        spawnEntityNow(world, x, y, z, id);
    }

    private static void spawnEntityNow(World world, int x, int y, int z, String id) {
        Entity entity;
        if ("minecraft:bee".equals(id)) entity = new EntityBee(world);
        else return;
        entity.setPosition(x + 0.5D, y, z + 0.5D);
        world.spawnEntityInWorld(entity);
    }

    private WorldBlock readPhysical(World world, int x, int y, int z) {
        if (!heightTransport.containsPhysicalY(y)) return WorldBlock.AIR;
        if (populationView != null && populationView.world == world) return populationView.read(x, y, z);
        IChunkProvider provider = world.getChunkProvider();
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!provider.chunkExists(chunkX, chunkZ)) return WorldBlock.AIR;
        Chunk chunk = provider.provideChunk(chunkX, chunkZ);
        return new WorldBlock(chunk.getBlock(x & 15, y, z & 15), chunk.getBlockMetadata(x & 15, y, z & 15));
    }

    /** Same transport on both sides of a tree journal comparison. New humid saves also retain attachment metadata. */
    private String treeState(Block block, int metadata) {
        if (nativeTreeAttachments) {
            if (block == net.minecraft.init.Blocks.vine && metadata > 0)
                return industries.angelic.biomic.compat.biome.BopTreeAttachments.vineState(metadata);
            if (block == net.minecraft.init.Blocks.cocoa)
                return industries.angelic.biomic.compat.biome.BopTreeAttachments.cocoaState(metadata);
        }
        String nativeState = industries.angelic.biomic.compat.biome.NativeBlockCatalogue.encode(block, metadata);
        if (nativeState != null) return nativeState;
        String state = industries.angelic.biomic.world.modern.ModernStateCompatibility.modernState(block, metadata);
        state = retainBogBushLeaves(state, metadata, nativeBogBushLeaves);
        if ((block == net.minecraft.init.Blocks.log || block == net.minecraft.init.Blocks.log2)
            && state.indexOf('[') < 0)
            state += "[axis=" + ((metadata & 12) == 4 ? "x" : (metadata & 12) == 8 ? "z" : "y") + "]";
        return state;
    }

    /** New saved Bog profiles retain the source bush's decay-disabled leaves through the tree journal. */
    static String retainBogBushLeaves(String state, int metadata, boolean enabled) {
        return enabled && metadata == 4 && state.equals("minecraft:oak_leaves")
            ? "minecraft:oak_leaves[persistent=true]"
            : state;
    }

    private boolean writePhysical(World world, int x, int y, int z, PendingBlock placement) {
        if (!heightTransport.containsPhysicalY(y)) return false;
        boolean written;
        if (populationView != null && populationView.world == world) {
            written = populationView.write(x, y, z, placement);
        } else {
            IChunkProvider provider = world.getChunkProvider();
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            written = provider.chunkExists(chunkX, chunkZ)
                && placeInChunk(world, provider.provideChunk(chunkX, chunkZ), x, y, z, placement);
        }
        return written;
    }

    private boolean writePhysical(World world, int x, int y, int z, OrigenBlockStateResolver.ResolvedBlock resolved,
        boolean physics) {
        if (!heightTransport.containsPhysicalY(y)) return false;
        String authoredState = canonicalState(resolved);
        String physicalState = translateSakuraTintedFoliage(activeFeatureProfile, activeFeatureId, authoredState);
        OrigenBlockStateResolver.ResolvedBlock physical = authoredState.equals(physicalState) ? resolved
            : resolve(physicalState);
        boolean written;
        if (populationView != null && populationView.world == world) {
            written = populationView.write(x, y, z, physical, physics);
        } else {
            IChunkProvider provider = world.getChunkProvider();
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            written = provider.chunkExists(chunkX, chunkZ)
                && placeInChunk(world, provider.provideChunk(chunkX, chunkZ), x, y, z, physical);
        }
        return written;
    }

    private boolean placeInChunk(World world, Chunk chunk, int x, int y, int z, PendingBlock placement) {
        return placeInChunk(
            world,
            chunk,
            x,
            y,
            z,
            resolve(placement.state()),
            placement.spawnerEntity(),
            placement.chestNbt());
    }

    private boolean placeInChunk(World world, Chunk chunk, int x, int y, int z,
        OrigenBlockStateResolver.ResolvedBlock resolved) {
        return placeInChunk(world, chunk, x, y, z, resolved, null, null);
    }

    private boolean placeInChunk(World world, Chunk chunk, int x, int y, int z,
        OrigenBlockStateResolver.ResolvedBlock resolved, String spawnerEntity, NBTTagCompound chestNbt) {
        if (!heightTransport.containsPhysicalY(y)) return false;
        // Blocks swapped in Biomic's worlds (api.BlockSubstitutes; vanilla's vine becomes PMP's ivy with its faces).
        // These writes are generation and pass by the chunk's own hook.
        Block substitute = industries.angelic.biomic.api.BlockSubstitutes
            .substitute(resolved.block(), chunk, x & 15, z & 15, true);
        if (substitute != resolved.block()) {
            String id = industries.angelic.biomic.compat.biome.NativeBlockCatalogue
                .encode(substitute, resolved.metadata());
            resolved = OrigenBlockStateResolver.imported(
                id != null ? id : OrigenBlockStateResolver.placed(substitute, resolved.metadata()),
                substitute,
                resolved.metadata());
        }
        boolean inertStructure = populationView != null && populationView.inertStructureWrite;
        if (!inertStructure && physicalLichenSupport
            && resolved.canonicalId()
                .equals("minecraft:glow_lichen")) {
            int requested = PhysicalLichenSupport.faces(resolved.properties());
            int supported = PhysicalLichenSupport.supported(
                requested,
                x,
                y,
                z,
                heightTransport.layout()
                    .height(),
                (sx, sy, sz) -> {
                    WorldBlock support = readPhysical(world, sx, sy, sz);
                    return new PhysicalLichenSupport.Cell(support.block(), support.metadata());
                });
            if (supported == 0) return false; // Preserve the existing air/water/feature; never invent a footing.
            if (supported != requested)
                resolved = resolve(PhysicalLichenSupport.state(supported, resolved.properties()));
        }
        int localX = x & 15;
        int localY = y & 15;
        int localZ = z & 15;
        // Cross-border ocean patches must not introduce seagrass into the native marsh either.
        if (!inertStructure && fullNativeMarsh
            && mainlandHabitats != null
            && (resolved.block() == BiomicAquaticBlocks.SEAGRASS || resolved.block() == BiomicAquaticBlocks.KELP)
            && industries.angelic.biomic.compat.biome.SaltMarshCompat.BIOME.equals(mainlandHabitats.sample(x, z))
            && hostedSurfaceOwner != null
            && hostedSurfaceOwner.owns(x, y, z)) return false;
        if (!inertStructure && mainlandHabitats != null
            && y > 0
            && resolved.block() == BiomicAquaticBlocks.SEAGRASS
            && resolved.metadata() == 2) {
            WorldBlock belowCell = supportCell(chunk, x, y - 1, z);
            String belowState = industries.angelic.biomic.world.modern.ModernStateCompatibility
                .modernState(belowCell.block(), belowCell.metadata());
            if (!industries.angelic.biomic.world.modern.ModernAquaticShapeUpdates.lower(belowState)) return false;
        }
        // Squeezing can project an authored plant onto invalid support. Modern aquatic blocks use
        // Material.water, so include them explicitly rather than treating only BlockBush/plant material as ground
        // cover.
        // Validate the incoming plant as well as plants displaced by later terrain writes.
        if (!inertStructure && hostedPlantSupport != null
            && y > 0
            && isHostedSupportDependentPlant(resolved.block())
            && ownsHostedGroundCover(x, y, z)) {
            WorldBlock belowCell = supportCell(chunk, x, y - 1, z);
            Block below = belowCell.block();
            if (!isSupportedUpperPlant(resolved.block(), resolved.metadata(), below)) {
                String state = supportState(resolved);
                String belowState = industries.angelic.biomic.world.modern.ModernStateCompatibility
                    .modernState(below, belowCell.metadata());
                WorldBlock hereCell = supportCell(chunk, x, y, z);
                Boolean support = physicalAquaticSupport(
                    state,
                    belowState,
                    industries.angelic.biomic.world.modern.ModernStateCompatibility
                        .modernState(hereCell.block(), hereCell.metadata()),
                    below);
                if (support == null) support = hostedPlantSupport.test(state, belowState, x, y, z);
                if (Boolean.FALSE.equals(support)) return false;
            }
        }
        ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
        int sectionIndex = y >> 4;
        if (sectionIndex >= storage.length)
            throw new IllegalStateException("Live chunk storage cannot represent projected Origen Y=" + y);
        ExtendedBlockStorage section = storage[sectionIndex];
        boolean sectionCreated = section == null;
        if (section == null) {
            if (resolved.block() == net.minecraft.init.Blocks.air) return false;
            section = new ExtendedBlockStorage(sectionIndex << 4, !world.provider.hasNoSky);
            storage[sectionIndex] = section;
        }
        Block previous = section.getBlockByExtId(localX, localY, localZ);
        int previousMetadata = section.getExtBlockMetadata(localX, localY, localZ);
        if (previous == resolved.block() && previousMetadata == resolved.metadata()) {
            if (spawnerEntity == null && chestNbt == null) return false;
            TileEntity existing = chunk.chunkTileEntityMap.get(new ChunkPosition(localX, y, localZ));
            if (spawnerEntity != null) configureSpawner(existing, spawnerEntity);
            if (chestNbt != null) configureChest(existing, chestNbt, x, y, z);
            chunk.setChunkModified();
            return true;
        }
        int previousLightOpacity = previous.getLightOpacity(world, x, y, z);
        int previousLightValue = previous.getLightValue(world, x, y, z);
        if (previous.hasTileEntity(previousMetadata)) {
            TileEntity previousTile = chunk.chunkTileEntityMap.remove(new ChunkPosition(localX, y, localZ));
            if (previousTile != null) previousTile.invalidate();
        }
        section.func_150818_a(localX, localY, localZ, resolved.block());
        section.setExtBlockMetadata(localX, localY, localZ, resolved.metadata());
        chunk.setChunkModified();
        if (populationView != null) {
            populationView.recordChange(
                chunk.xPosition,
                chunk.zPosition,
                x,
                y,
                z,
                resolved.block()
                    .hasTileEntity(resolved.metadata()),
                requiresSkylightRegeneration(
                    sectionCreated,
                    previousLightOpacity,
                    resolved.block()
                        .getLightOpacity(world, x, y, z),
                    previousLightValue,
                    resolved.block()
                        .getLightValue(world, x, y, z)));
        }
        TileEntity createdTile = null;
        if (resolved.block()
            .hasTileEntity(resolved.metadata())) {
            createdTile = resolved.block()
                .createTileEntity(world, resolved.metadata());
            if (createdTile != null) {
                createdTile.xCoord = x;
                createdTile.yCoord = y;
                createdTile.zCoord = z;
                // Chunk#func_150806_e delegates to World#setTileEntity, whose comparator notification can load and
                // populate a neighbor recursively. Chunk#addTileEntity attaches the already-created instance without
                // touching another chunk.
                chunk.addTileEntity(createdTile);
            }
        }
        if (createdTile instanceof TileEntityGlowLichen lichen) configureGlowLichen(lichen, resolved);
        if (spawnerEntity != null) configureSpawner(createdTile, spawnerEntity);
        if (chestNbt != null) configureChest(createdTile, chestNbt, x, y, z);
        // Direct generation writes bypass Block.updateShape. A later kelp/short-grass/rock
        // placement must not strand half of an earlier tall seagrass plant. Only inspect the
        // two vertical neighbors in this already loaded chunk; do not load or tick anything.
        if (!inertStructure && mainlandHabitats != null) {
            for (int dy : new int[] { -1, 1 }) {
                int neighborY = y + dy;
                if (neighborY <= 0 || !heightTransport.containsPhysicalY(neighborY)) continue;
                WorldBlock neighborCell = supportCell(chunk, x, neighborY, z);
                if (neighborCell.block() != BiomicAquaticBlocks.SEAGRASS || neighborCell.metadata() == 0) continue;
                String neighbor = industries.angelic.biomic.world.modern.ModernStateCompatibility
                    .modernState(neighborCell.block(), neighborCell.metadata());
                if (industries.angelic.biomic.world.modern.ModernAquaticShapeUpdates
                    .brokenByNeighbor(neighbor, supportState(resolved), dy < 0)) {
                    placeInChunk(world, chunk, x, neighborY, z, new PendingBlock("minecraft:water[level=0]", false));
                    voidClaim(world, chunk, x, neighborY, z);
                }
            }
        }
        // A neighboring Origen boulder or later seabed rewrite can run after this column's hosted vegetation.
        // Aquatic failures restore their represented source water and recursively settle a kelp column in-place.
        // Direct buffered writes
        // intentionally bypass normal block ticks; settle a displaced plant here, before neighborhood delivery,
        // instead of leaving it on stone until a random tick drops an item. No lighting/light-level query and no
        // neighboring chunk load. The explicit Origen-only preset retains its existing behavior.
        if (!inertStructure && hostedPlantSupport != null
            && y + 1 < heightTransport.layout()
                .height()) {
            WorldBlock aboveCell = supportCell(chunk, x, y + 1, z);
            Block above = aboveCell.block();
            int aboveMeta = aboveCell.metadata();
            if (isHostedSupportDependentPlant(above) && !isSupportedUpperPlant(above, aboveMeta, resolved.block())
                && ownsHostedGroundCover(x, y + 1, z)
                && terrainAccess.floatingArchetypeAt(x, y, z) == null) {
                String aboveState = industries.angelic.biomic.world.modern.ModernStateCompatibility
                    .modernState(above, aboveMeta);
                Boolean support = physicalAquaticSupport(
                    aboveState,
                    supportState(resolved),
                    aboveState,
                    resolved.block());
                if (support == null) support = hostedPlantSupport.test(aboveState, supportState(resolved), x, y + 1, z);
                if (Boolean.FALSE.equals(support)) {
                    placeInChunk(
                        world,
                        chunk,
                        x,
                        y + 1,
                        z,
                        new PendingBlock(supportFailureReplacementState(above), false));
                    voidClaim(world, chunk, x, y + 1, z);
                }
            }
        }
        return true;
    }

    /**
     * The cell a write's support checks see: the chunk as it stands. A plant and the ground under it may come from two
     * populations in either order; checked against the world both ways, the plant stands only on ground that carries
     * it (a writer's own view would keep it on ground that lost its cell to another population in one order only).
     */
    private static WorldBlock supportCell(Chunk chunk, int x, int y, int z) {
        return new WorldBlock(chunk.getBlock(x & 15, y, z & 15), chunk.getBlockMetadata(x & 15, y, z & 15));
    }

    private static void configureSpawner(TileEntity tile, String entityId) {
        if (!(tile instanceof net.minecraft.tileentity.TileEntityMobSpawner spawner))
            throw new IllegalStateException("Mob-spawner payload materialized without a mob spawner");
        spawner.func_145881_a()
            .setEntityName(entityId);
        modified(spawner);
    }

    /**
     * Marks a freshly configured tile entity's chunk for saving. Not markDirty: vanilla's updates comparators on
     * the neighbouring blocks and EFR's bee nest looks for fire around it, and either loads (generates and
     * populates) the next chunk when the block sits on a chunk edge, a cascade inside population.
     */
    private static void modified(TileEntity tile) {
        if (tile.getWorldObj() != null) tile.getWorldObj()
            .markTileEntityChunkModified(tile.xCoord, tile.yCoord, tile.zCoord, tile);
    }

    private static void configureChest(TileEntity tile, NBTTagCompound payload, int x, int y, int z) {
        if (tile instanceof ganymedes01.etfuturum.tileentities.TileEntityBeeHive hive) {
            hive.readFromNBT(validatedChestCopy(BEE_NEST, payload, x, y, z));
            hive.setWorldObj(tile.getWorldObj());
            hive.xCoord = x;
            hive.yCoord = y;
            hive.zCoord = z;
            modified(hive);
            return;
        }
        if (!(tile instanceof net.minecraft.tileentity.TileEntityChest chest))
            throw new IllegalStateException("Chest payload materialized without a normal chest");
        NBTTagCompound exact = validatedChestCopy("minecraft:chest", payload, x, y, z);
        chest.readFromNBT(exact);
        chest.setWorldObj(tile.getWorldObj());
        chest.xCoord = x;
        chest.yCoord = y;
        chest.zCoord = z;
        modified(chest);
    }

    boolean ownsHostedGroundCover(int x, int y, int z) {
        return mainlandHabitats != null && mainlandHabitats.sample(x, z) != null
            || hostedCaveOwner != null && hostedCaveOwner.owns(x, y, z);
    }

    static boolean isSoilRootedGroundCover(Block block) {
        return (block.getMaterial() == Material.plants || block instanceof net.minecraft.block.BlockBush)
            && !(block instanceof net.minecraft.block.BlockMushroom);
    }

    static boolean isHostedSupportDependentPlant(Block block) {
        return isSoilRootedGroundCover(block) || block == BiomicAquaticBlocks.SEAGRASS
            || block == BiomicAquaticBlocks.KELP;
    }

    static String supportFailureReplacementState(Block block) {
        return block == BiomicAquaticBlocks.SEAGRASS || block == BiomicAquaticBlocks.KELP ? "minecraft:water[level=0]"
            : "minecraft:air";
    }

    private static Boolean physicalAquaticSupport(String state, String below, String target, Block belowBlock) {
        if (!industries.angelic.biomic.world.modern.ModernAquaticPlantSupport.handles(state)) return null;
        return industries.angelic.biomic.world.modern.ModernAquaticPlantSupport.test(
            state,
            below,
            target,
            industries.angelic.biomic.world.modern.ModernBlockSemantics.legacyFullFace(belowBlock));
    }

    public void setLocatedHostedPlantSupport(LocatedPlantSupport support) {
        hostedPlantSupport = support;
    }

    static boolean isSupportedUpperPlant(Block above, int metadata, Block below) {
        return above instanceof net.minecraft.block.BlockDoublePlant && (metadata & 8) != 0 && above == below;
    }

    static boolean requiresSkylightRegeneration(boolean sectionCreated, int previousOpacity, int replacementOpacity,
        int previousLight, int replacementLight) {
        /*
         * ExtendedBlockStorage initializes its nibble arrays to zero. Even a transparent plant therefore needs a
         * complete skylight pass when it is the first block placed in an otherwise empty 16-high section.
         */
        return sectionCreated || previousOpacity != replacementOpacity || previousLight != replacementLight;
    }

    private static void configureGlowLichen(TileEntityGlowLichen lichen,
        OrigenBlockStateResolver.ResolvedBlock resolved) {
        int faces = 0;
        if (Boolean.parseBoolean(
            resolved.properties()
                .get("down")))
            faces |= 1;
        if (Boolean.parseBoolean(
            resolved.properties()
                .get("up")))
            faces |= 1 << 1;
        if (Boolean.parseBoolean(
            resolved.properties()
                .get("north")))
            faces |= 1 << 2;
        if (Boolean.parseBoolean(
            resolved.properties()
                .get("south")))
            faces |= 1 << 3;
        if (Boolean.parseBoolean(
            resolved.properties()
                .get("west")))
            faces |= 1 << 4;
        if (Boolean.parseBoolean(
            resolved.properties()
                .get("east")))
            faces |= 1 << 5;
        // EFR's public setter marks the tile dirty and asks World for the block immediately. During population that
        // can synchronously load and populate an adjacent chunk, re-entering this runtime. Round-trip the persisted
        // field instead: this is the same state the setter stores, without notifications that are only useful for a
        // player edit in an already-live world.
        NBTTagCompound state = new NBTTagCompound();
        lichen.writeToNBT(state);
        state.setInteger("State", faces);
        lichen.readFromNBT(state);
    }

    /**
     * The profile's own flower for a vanilla flower kind of Origen's generic patches (FloraPalettes patches), or null
     * when the profile keeps Origen's flowers. Same cells and shapes; the plant's own soil rule decides each spot.
     */
    private Structure reskinnedFlower(OrigenBiomeProfile profile, String vanillaKind) {
        List<String> palette = FloraPalettes.instance()
            .patches(profile);
        if (palette == null) return null;
        int kind = VANILLA_FLOWER_KINDS.indexOf(vanillaKind);
        if (kind < 0) kind = Math.floorMod(vanillaKind.hashCode(), VANILLA_FLOWER_KINDS.size());
        String id = palette.get(Math.floorMod(kind + profile.ordinal(), palette.size()));
        return reskinnedFlowers.computeIfAbsent(id, this::paletteFlower);
    }

    private Structure paletteFlower(String id) {
        FloraPlants.Plant plant = FloraPlants.resolve(id);
        if (plant == null) return (world, x, y, z, authoredY, rotationDegrees, random) -> false;
        Map<Integer, OrigenBlockStateResolver.ResolvedBlock[]> states = new java.util.concurrent.ConcurrentHashMap<>();
        return (world, x, y, z, authoredY, rotationDegrees, random) -> {
            if (!plant.acceptsSoil(blockAt(x, authoredY - 1, z).block())) return false;
            // One colour per patch neighbourhood for a palette flower given without one.
            int[] grown = plant.grown(
                WorldgenContext.mix64(
                    world.getSeed() ^ Math.floorDiv(x, 6) * 341873128712L
                        ^ Math.floorDiv(z, 6) * 132897987541L
                        ^ id.hashCode()));
            OrigenBlockStateResolver.ResolvedBlock[] resolved = states.computeIfAbsent(
                grown[0] * 16 + grown[1] + 1,
                key -> new OrigenBlockStateResolver.ResolvedBlock[] { resolve(paletteState(plant.block, grown[0])),
                    grown[1] < 0 ? null : resolve(paletteState(plant.block, grown[1])) });
            if (resolved[1] != null && blockAt(x, authoredY + 1, z).block() != net.minecraft.init.Blocks.air)
                return false;
            if (!placeResolvedBlock(world, x, z, authoredY, resolved[0], true, false)) return false;
            if (resolved[1] != null) placeResolvedBlock(world, x, z, authoredY + 1, resolved[1], true, false);
            return true;
        };
    }

    /** Origen's leaf-block decor as real plants (OrigenLeafDecor): shrub clumps, wall vines, curtains. */
    private Structure leafDecorStructure(OrigenLeafDecor.Spec spec) {
        if (spec instanceof OrigenLeafDecor.Nothing)
            return (world, x, y, z, authoredY, rotationDegrees, random) -> false;
        if (spec instanceof OrigenLeafDecor.MossSkin) {
            return (world, x, y, z, authoredY, rotationDegrees, random) -> {
                if (blockAt(x, authoredY, z).block() != net.minecraft.init.Blocks.air) return false;
                long hash = decorHash(world, x, authoredY, z);
                boolean placed = false;
                int[][] sides = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, 1 }, { -1, 0, 0 }, { 0, 0, -1 }, { 1, 0, 0 } };
                for (int i = 0; i < sides.length; i++) {
                    int[] d = sides[i];
                    if (((hash >>> (i * 6)) & 63) >= 45) continue;
                    placed |= replaceNaturalRock(world, x + d[0], authoredY + d[1], z + d[2], "minecraft:moss_block");
                }
                int faces = wallFaces(x, authoredY, z);
                if (faces != 0 && (hash >>> 40 & 3) == 0)
                    placed |= placeVine(world, x, authoredY, z, DecorSlots.cave("moss_skin_wall"), faces);
                else if (attachable(blockAt(x, authoredY + 1, z).block()) && (hash >>> 44 & 3) == 0)
                    placed |= placeDecorPlant(world, x, authoredY, z, "biomic:hanging_roots:0", hash);
                return placed;
            };
        }
        if (spec instanceof OrigenLeafDecor.CaveCeiling) {
            return (world, x, y, z, authoredY, rotationDegrees, random) -> {
                if (blockAt(x, authoredY, z).block() != net.minecraft.init.Blocks.air
                    || !attachable(blockAt(x, authoredY + 1, z).block())) return false;
                long hash = decorHash(world, x, authoredY, z);
                double roll = (hash >>> 11) * 0x1.0p-53;
                if (roll < 0.2D) return placeDecorPlant(world, x, authoredY, z, "biomic:spore_blossom:0", hash);
                if (roll < 0.55D) return placeDecorPlant(world, x, authoredY, z, "biomic:hanging_roots:0", hash);
                int length = 2 + (int) Math.floorMod(hash >>> 20, 7L);
                int n = 0;
                while (n < length && blockAt(x, authoredY - n, z).block() == net.minecraft.init.Blocks.air) n++;
                for (int i = 0; i < n; i++) {
                    String state = (i == n - 1 ? "minecraft:cave_vines" : "minecraft:cave_vines_plant") + "[berries="
                        + (((hash >>> (24 + i)) & 7) == 0)
                        + "]";
                    placeResolvedBlock(world, x, z, authoredY - i, resolve(state), false, false);
                }
                return n > 0;
            };
        }
        if (spec instanceof OrigenLeafDecor.FrozenWall) {
            return (world, x, y, z, authoredY, rotationDegrees, random) -> {
                if (blockAt(x, authoredY, z).block() != net.minecraft.init.Blocks.air) return false;
                long hash = decorHash(world, x, authoredY, z);
                if (attachable(blockAt(x, authoredY + 1, z).block()) && (hash & 3) == 0) {
                    // A hanging icicle of one or two blocks (EFR pointed-dripstone metadata: 1 frustum, 0 tip).
                    boolean two = (hash >>> 8 & 1) == 0
                        && blockAt(x, authoredY - 1, z).block() == net.minecraft.init.Blocks.air;
                    FloraPlants.Plant icicle = FloraPlants.resolve("biomic:icicle:0");
                    if (icicle == null) return false;
                    placeResolvedBlock(
                        world,
                        x,
                        z,
                        authoredY,
                        resolve(paletteState(icicle.block, two ? 1 : 0)),
                        false,
                        false);
                    if (two) placeResolvedBlock(
                        world,
                        x,
                        z,
                        authoredY - 1,
                        resolve(paletteState(icicle.block, 0)),
                        false,
                        false);
                    return true;
                }
                if (attachable(blockAt(x, authoredY - 1, z).block()) && (hash >>> 4 & 1) == 0)
                    return placeDecorPlant(world, x, authoredY, z, "minecraft:snow_layer:0", hash);
                return false;
            };
        }
        if (spec instanceof OrigenLeafDecor.Clump clump) {
            return (world, x, y, z, authoredY, rotationDegrees, random) -> {
                long hash = decorHash(world, x, authoredY, z);
                List<String> plants = clump.plants();
                String centre = clump.single() ? plants.get((int) Math.floorMod(hash, (long) plants.size()))
                    : plants.get(0);
                boolean placed = placeDecorPlant(world, x, authoredY, z, centre, hash);
                int extra = clump.extra() == 0 ? 0 : (int) Math.floorMod(hash >>> 8, (long) clump.extra() + 1);
                for (int i = 0; i < extra && plants.size() > 1; i++) {
                    long h = WorldgenContext.mix64(hash + i * 0x9E3779B97F4A7C15L);
                    int span = clump.radius() * 2 + 1;
                    int dx = (int) Math.floorMod(h, (long) span) - clump.radius();
                    int dz = (int) Math.floorMod(h >>> 16, (long) span) - clump.radius();
                    String plant = plants.get(1 + (int) Math.floorMod(h >>> 32, (long) plants.size() - 1));
                    placed |= placeDecorPlant(world, x + dx, authoredY, z + dz, plant, h);
                }
                return placed;
            };
        }
        if (spec instanceof OrigenLeafDecor.Wall wall) {
            return (world, x, y, z, authoredY, rotationDegrees, random) -> {
                if (blockAt(x, authoredY, z).block() != net.minecraft.init.Blocks.air) return false;
                int faces = wallFaces(x, authoredY, z);
                if (faces == 0) return false;
                if (wall.nearWater() && !waterWithin(x, authoredY, z, 6)) return false;
                long hash = decorHash(world, x, authoredY, z);
                String id = wall.accents()
                    .isEmpty() || (hash >>> 11) * 0x1.0p-53 >= wall.accentShare() ? wall.vine()
                        : wall.accents()
                            .get(
                                (int) Math.floorMod(
                                    hash >>> 32,
                                    (long) wall.accents()
                                        .size()));
                if (placeVine(world, x, authoredY, z, id, faces)) return true;
                // A non-vine accent (a ledge fern) stands on the ledge instead; otherwise the base vine.
                return placeDecorPlant(world, x, authoredY, z, id, hash)
                    || placeVine(world, x, authoredY, z, wall.vine(), faces);
            };
        }
        OrigenLeafDecor.Curtain curtain = (OrigenLeafDecor.Curtain) spec;
        return (world, x, y, z, authoredY, rotationDegrees, random) -> {
            if (blockAt(x, authoredY, z).block() != net.minecraft.init.Blocks.air) return false;
            long hash = decorHash(world, x, authoredY, z);
            int length = curtain.minLength()
                + (int) Math.floorMod(hash >>> 8, (long) curtain.maxLength() - curtain.minLength() + 1);
            int faces = wallFaces(x, authoredY, z);
            if (faces != 0) {
                String vine = curtain.vines()
                    .get(
                        (int) Math.floorMod(
                            hash >>> 32,
                            (long) curtain.vines()
                                .size()));
                boolean placed = false;
                for (int i = 0; i < length; i++) {
                    if (blockAt(x, authoredY - i, z).block() != net.minecraft.init.Blocks.air) break;
                    // Below the first, a vine hangs from the one above it even where the wall falls back.
                    int below = i == 0 ? faces : faces & (wallFaces(x, authoredY - i, z) | faces);
                    if (!placeVine(world, x, authoredY - i, z, vine, below)) break;
                    placed = true;
                }
                return placed;
            }
            if (!attachable(blockAt(x, authoredY + 1, z).block())) return false;
            if (curtain.strand() != null) {
                boolean placed = false;
                for (int i = 0; i < length / 2 + 1; i++) {
                    if (!placeDecorPlant(world, x, authoredY - i, z, curtain.strand(), hash + i)) break;
                    placed = true;
                }
                return placed;
            }
            if (curtain.roots() != null) return placeDecorPlant(world, x, authoredY, z, curtain.roots(), hash);
            // Under a canopy with no wall: one vine hanging from the leaves above (vanilla vine metadata 0).
            return placeVine(
                world,
                x,
                authoredY,
                z,
                curtain.vines()
                    .get(0),
                0);
        };
    }

    private static long decorHash(World world, int x, int authoredY, int z) {
        return WorldgenContext.mix64(
            world.getSeed() ^ x * 0x9E3779B97F4A7C15L
                ^ WorldgenContext.mix64(z * 0xC2B2AE3D27D4EB4FL ^ authoredY * 0x165667B19E3779F9L));
    }

    /** A palette plant at an air cell whose ground its own rule accepts; doubles need the cell above free too. */
    private boolean placeDecorPlant(World world, int x, int authoredY, int z, String id, long hash) {
        FloraPlants.Plant plant = FloraPlants.resolve(id);
        if (plant == null || blockAt(x, authoredY, z).block() != net.minecraft.init.Blocks.air) return false;
        Block below = blockAt(x, authoredY - 1, z).block();
        if (!plant.acceptsSoil(below) && !(plant.block == below)) return false;
        int[] grown = plant.grown(hash);
        if (grown[1] >= 0 && blockAt(x, authoredY + 1, z).block() != net.minecraft.init.Blocks.air) return false;
        if (!placeResolvedBlock(world, x, z, authoredY, resolve(paletteState(plant.block, grown[0])), false, false))
            return false;
        if (grown[1] >= 0)
            placeResolvedBlock(world, x, z, authoredY + 1, resolve(paletteState(plant.block, grown[1])), false, false);
        return true;
    }

    /** A vine-type plant (BlockVine: metadata are the attached sides, 0 the ceiling) at an air cell. */
    private boolean placeVine(World world, int x, int authoredY, int z, String id, int faces) {
        FloraPlants.Plant plant = FloraPlants.resolve(id);
        if (plant == null || !(plant.block instanceof net.minecraft.block.BlockVine)) return false;
        if (faces == 0 && !attachable(blockAt(x, authoredY + 1, z).block())) return false;
        return placeResolvedBlock(world, x, z, authoredY, resolve(paletteState(plant.block, faces)), false, false);
    }

    /** Natural rock (not ore, not soil) at this cell becomes the given block. */
    private boolean replaceNaturalRock(World world, int x, int authoredY, int z, String state) {
        Block current = blockAt(x, authoredY, z).block();
        if (current != net.minecraft.init.Blocks.stone
            && !(current.getMaterial() == net.minecraft.block.material.Material.rock
                && !(current instanceof net.minecraft.block.BlockOre)
                && current.isOpaqueCube()
                && String.valueOf(Block.blockRegistry.getNameForObject(current))
                    .matches("etfuturum:(deepslate|tuff|stone|calcite)")))
            return false;
        return placeResolvedBlock(world, x, z, authoredY, resolve(state), true, false);
    }

    /** Vine side bits (1 south, 2 west, 4 north, 8 east) of the faces a vine at this cell could hold on to. */
    private int wallFaces(int x, int authoredY, int z) {
        int faces = 0;
        if (attachable(blockAt(x, authoredY, z + 1).block())) faces |= 1;
        if (attachable(blockAt(x - 1, authoredY, z).block())) faces |= 2;
        if (attachable(blockAt(x, authoredY, z - 1).block())) faces |= 4;
        if (attachable(blockAt(x + 1, authoredY, z).block())) faces |= 8;
        return faces;
    }

    private static boolean attachable(Block block) {
        return block.renderAsNormalBlock() && block.getMaterial()
            .blocksMovement();
    }

    private boolean waterWithin(int x, int authoredY, int z, int reach) {
        for (int dx = -reach; dx <= reach; dx += 2) for (int dz = -reach; dz <= reach; dz += 2)
            for (int dy = -2; dy <= 1; dy++) if (blockAt(x + dx, authoredY + dy, z + dz).block()
                .getMaterial() == net.minecraft.block.material.Material.water) return true;
        return false;
    }

    private static String paletteState(Block block, int metadata) {
        String state = industries.angelic.biomic.compat.biome.NativeBlockCatalogue.encode(block, metadata);
        return state != null ? state
            : industries.angelic.biomic.world.modern.ModernStateCompatibility.modernState(block, metadata);
    }

    private Structure structure(String id) {
        if (id.startsWith("BLOCK:")) {
            OrigenBlockStateResolver.ResolvedBlock state = resolve(id.substring("BLOCK:".length()));
            return (world, x, y, z, authoredY, rotationDegrees,
                random) -> placeResolvedBlock(world, x, z, authoredY, state, true, false);
        }
        Structure structure = structures.get(id);
        if (structure == null) throw new IllegalArgumentException("Unknown Origen structure " + id);
        return structure;
    }

    private static String properties(Map<String, String> properties) {
        if (properties.isEmpty()) return "";
        StringBuilder result = new StringBuilder("[");
        for (Map.Entry<String, String> property : properties.entrySet()) {
            if (result.length() > 1) result.append(',');
            result.append(property.getKey())
                .append('=')
                .append(property.getValue());
        }
        return result.append(']')
            .toString();
    }

    private static JsonObject loadManifest() {
        return OrigenManifest.read();
    }

    private static JsonElement required(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) throw new IllegalStateException("Missing Origen property " + key);
        return value;
    }

    private static JsonObject object(JsonObject object, String key) {
        return required(object, key).getAsJsonObject();
    }

    private static JsonArray array(JsonObject object, String key) {
        return required(object, key).getAsJsonArray();
    }

    private double number(JsonObject object, String key, double fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : compiler.numeric(value);
    }

    private static boolean bool(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? false : value.getAsBoolean();
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }

    private static long horizontalKey(int x, int z) {
        return (long) x << 32 ^ z & 0xFFFF_FFFFL;
    }

    @FunctionalInterface
    private interface Distributor {

        boolean matches(int x, int z, long seed);
    }

    @FunctionalInterface
    private interface Locator {

        BitSet locate(World world, int x, int z, int columnMinimum, int columnMaximum);

        default BitSet locateWithin(World world, int x, int z, int columnMinimum, int columnMaximum,
            BitSet candidates) {
            BitSet result = locate(world, x, z, columnMinimum, columnMaximum);
            result.and(candidates);
            return result;
        }
    }

    @FunctionalInterface
    private interface RestrictedLocator {

        BitSet locate(World world, int x, int z, int columnMinimum, int columnMaximum, BitSet candidates);
    }

    @FunctionalInterface
    private interface Pattern {

        boolean matches(World world, int x, int authoredY, int z, int columnMinimum, int columnMaximum);

        default void retainMatches(World world, int x, int z, int columnMinimum, int columnMaximum, BitSet candidates) {
            for (int location = candidates.nextSetBit(0); location
                >= 0; location = candidates.nextSetBit(location + 1)) {
                if (!matches(world, x, authoredY(location), z, columnMinimum, columnMaximum)) {
                    candidates.clear(location);
                }
            }
        }
    }

    private enum BlockMatch {
        AIR,
        SOLID,
        STATE
    }

    private final class CompositePattern implements Pattern {

        private final boolean conjunction;
        private final List<Pattern> children;
        private final BlockPattern[] directBlockChildren;

        private CompositePattern(boolean conjunction, List<Pattern> children) {
            this.conjunction = conjunction;
            this.children = Collections.unmodifiableList(new ArrayList<>(children));
            BlockPattern[] direct = conjunction ? new BlockPattern[children.size()] : null;
            if (direct != null) {
                for (int index = 0; index < children.size(); index++) {
                    if (!(children.get(index) instanceof BlockPattern block)) {
                        direct = null;
                        break;
                    }
                    direct[index] = block;
                }
            }
            if (direct != null) {
                Arrays.sort(direct, (left, right) -> Integer.compare(patternCost(left), patternCost(right)));
            }
            directBlockChildren = direct;
        }

        @Override
        public boolean matches(World world, int x, int authoredY, int z, int columnMinimum, int columnMaximum) {
            for (Pattern child : children) {
                if (child.matches(world, x, authoredY, z, columnMinimum, columnMaximum) != conjunction) {
                    return !conjunction;
                }
            }
            return conjunction;
        }

        @Override
        public void retainMatches(World world, int x, int z, int columnMinimum, int columnMaximum, BitSet candidates) {
            if (!conjunction) {
                Pattern.super.retainMatches(world, x, z, columnMinimum, columnMaximum, candidates);
                return;
            }
            if (directBlockChildren != null && populationView != null && locatorOrigin == null) {
                populationView.retainBlockMatches(x, z, directBlockChildren, columnMinimum, columnMaximum, candidates);
                return;
            }
            for (Pattern child : children) {
                if (candidates.isEmpty()) return;
                child.retainMatches(world, x, z, columnMinimum, columnMaximum, candidates);
            }
        }

        private static int patternCost(BlockPattern pattern) {
            return switch (pattern.match) {
                case STATE -> 0;
                case SOLID -> 1;
                case AIR -> 2;
            };
        }
    }

    private final class BlockPattern implements Pattern {

        private final IntRange offsets;
        private final BlockMatch match;
        private final List<OrigenBlockStateResolver.ResolvedBlock> states;
        private final byte[] terrainMatches = new byte[256];

        private BlockPattern(IntRange offsets, BlockMatch match, List<OrigenBlockStateResolver.ResolvedBlock> states) {
            this.offsets = offsets;
            this.match = match;
            this.states = Collections.unmodifiableList(new ArrayList<>(states));
        }

        @Override
        public boolean matches(World world, int x, int authoredY, int z, int columnMinimum, int columnMaximum) {
            int minimum = clampedPatternMinimum(authoredY, offsets.minimum, columnMinimum);
            int maximum = clampedPatternMaximum(authoredY, offsets.maximum, columnMaximum);
            if (maximum <= minimum) return false;
            for (int testY = minimum; testY < maximum; testY++) {
                if (!matchesBlock(x, testY, z)) return false;
            }
            return true;
        }

        @Override
        public void retainMatches(World world, int x, int z, int columnMinimum, int columnMaximum, BitSet candidates) {
            if (populationView == null || locatorOrigin != null) {
                Pattern.super.retainMatches(world, x, z, columnMinimum, columnMaximum, candidates);
            } else {
                populationView.retainBlockMatches(x, z, this, columnMinimum, columnMaximum, candidates);
            }
        }

        private boolean matchesBlock(int x, int authoredY, int z) {
            if (match == BlockMatch.AIR) return isAir(x, authoredY, z);
            WorldBlock candidate = blockAt(x, authoredY, z);
            return matchesState(candidate.block(), candidate.metadata());
        }

        private boolean matchesState(Block block, int metadata) {
            return switch (match) {
                case AIR -> block == net.minecraft.init.Blocks.air;
                case SOLID -> isTerraOccluding(block);
                case STATE -> matchesAnyBlockState(states, block, metadata);
            };
        }

        private boolean matchesTerrainMaterial(byte material) {
            if (match == BlockMatch.AIR) return material == BiomicTerrainSampler.AIR;
            int index = material & 0xFF;
            byte cached = terrainMatches[index];
            if (cached != 0) return cached > 0;
            WorldBlock block = resolveTerrainMaterial(material);
            boolean result = matchesState(block.block(), block.metadata());
            terrainMatches[index] = (byte) (result ? 1 : -1);
            return result;
        }
    }

    @FunctionalInterface
    private interface Structure {

        boolean generate(World world, int x, int y, int z, int authoredY, int rotationDegrees, Random random);
    }

    private final class ScriptStructure implements Structure {

        private final OrigenTerraScript script;

        private ScriptStructure(OrigenTerraScript script) {
            this.script = script;
        }

        @Override
        public boolean generate(World world, int x, int y, int z, int authoredY, int rotationDegrees, Random random) {
            return script
                .generate(OrigenFeatureRuntime.this, world, x, y, z, authoredY, 0, 0, 0, rotationDegrees, random, 0);
        }
    }

    private final class SchematicStructure implements Structure {

        private final int width;
        private final int length;
        private final int height;
        private final int offsetX;
        private final int offsetY;
        private final int offsetZ;
        private final byte[] blocks;
        private final Map<Integer, OrigenBlockStateResolver.ResolvedBlock> palette;

        private SchematicStructure(int width, int length, int height, int offsetX, int offsetY, int offsetZ,
            byte[] blocks, Map<Integer, OrigenBlockStateResolver.ResolvedBlock> palette) {
            this.width = width;
            this.length = length;
            this.height = height;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.blocks = blocks;
            this.palette = palette;
        }

        @Override
        public boolean generate(World world, int x, int y, int z, int authoredY, int rotationDegrees, Random random) {
            for (int localX = 0; localX < width; localX++) {
                for (int localZ = 0; localZ < length; localZ++) {
                    int relativeX = localX + offsetX;
                    int relativeZ = localZ + offsetZ;
                    int rotatedX = switch (Math.floorMod(rotationDegrees, 360)) {
                        case 90 -> relativeZ;
                        case 180 -> -relativeX;
                        case 270 -> -relativeZ;
                        default -> relativeX;
                    };
                    int rotatedZ = switch (Math.floorMod(rotationDegrees, 360)) {
                        case 90 -> -relativeX;
                        case 180 -> -relativeZ;
                        case 270 -> relativeX;
                        default -> relativeZ;
                    };
                    for (int localY = 0; localY < height; localY++) {
                        int index = localX + localZ * width + localY * width * length;
                        OrigenBlockStateResolver.ResolvedBlock state = palette
                            .get(Integer.valueOf(blocks[index] & 0xFF));
                        if (state != null) {
                            placeResolvedBlock(
                                world,
                                x + rotatedX,
                                z + rotatedZ,
                                authoredY + localY + offsetY,
                                state,
                                true,
                                false);
                        }
                    }
                }
            }
            return true;
        }
    }

    private final class OreStructure implements Structure {

        private final OrigenBlockStateResolver.ResolvedBlock material;
        private final double size;
        private final Block[] replaceable;
        private final boolean physics;
        private final double exposed;
        /** A rock deposit: it may line caves but never shows on a face open to the sky (cliffs, slopes). */
        private final boolean hiddenFromSky;
        private final Block[] overrideBlocks;
        private final OrigenBlockStateResolver.ResolvedBlock[] overrideStates;
        private final int sizeInt;
        private final double[] points;
        private final long[] visitedWords;

        private OreStructure(OrigenBlockStateResolver.ResolvedBlock material, double size, Set<Block> replaceable,
            boolean physics, double exposed, boolean hiddenFromSky,
            Map<Block, OrigenBlockStateResolver.ResolvedBlock> overrides) {
            this.material = material;
            this.hiddenFromSky = hiddenFromSky;
            this.size = size;
            this.replaceable = replaceable.toArray(new Block[replaceable.size()]);
            this.physics = physics;
            this.exposed = exposed;
            overrideBlocks = new Block[overrides.size()];
            overrideStates = new OrigenBlockStateResolver.ResolvedBlock[overrides.size()];
            int overrideIndex = 0;
            for (Map.Entry<Block, OrigenBlockStateResolver.ResolvedBlock> override : overrides.entrySet()) {
                overrideBlocks[overrideIndex] = override.getKey();
                overrideStates[overrideIndex] = override.getValue();
                overrideIndex++;
            }
            sizeInt = (int) size;
            points = new double[sizeInt * 4];
            int outset = (int) Math.ceil((size / 16.0F * 2.0F + 1.0F) / 2.0F);
            int horizontalSize = (int) (2 * (Math.ceil(size / 8.0F) + outset));
            int verticalSize = 2 * (2 + outset);
            visitedWords = new long[(horizontalSize * horizontalSize * verticalSize + 63) >>> 6];
        }

        @Override
        public boolean generate(World world, int locationX, int locationY, int locationZ, int authoredY,
            int rotationDegrees, Random random) {
            float randomRadian = random.nextFloat() * (float) Math.PI;
            double eighthSize = size / 8.0F;
            double startX = locationX + MathUtil.sin(randomRadian) * eighthSize;
            double endX = locationX - MathUtil.sin(randomRadian) * eighthSize;
            double startZ = locationZ + MathUtil.cos(randomRadian) * eighthSize;
            double endZ = locationZ - MathUtil.cos(randomRadian) * eighthSize;
            double startY = locationY + random.nextInt(3) - 2;
            double endY = locationY + random.nextInt(3) - 2;
            for (int index = 0; index < sizeInt; index++) {
                float progress = (float) index / sizeInt;
                points[index * 4] = MathUtil.lerp(progress, startX, endX);
                points[index * 4 + 1] = MathUtil.lerp(progress, startY, endY);
                points[index * 4 + 2] = MathUtil.lerp(progress, startZ, endZ);
                double roll = random.nextDouble() * size / 16.0D;
                points[index * 4 + 3] = ((MathUtil.sin((float) Math.PI * progress) + 1.0F) * roll + 1.0D) / 2.0D;
            }
            for (int first = 0; first < sizeInt - 1; first++) {
                double firstRadius = points[first * 4 + 3];
                if (firstRadius <= 0.0D) continue;
                for (int second = first + 1; second < sizeInt; second++) {
                    double secondRadius = points[second * 4 + 3];
                    if (secondRadius <= 0.0D) continue;
                    double dx = points[first * 4] - points[second * 4];
                    double dy = points[first * 4 + 1] - points[second * 4 + 1];
                    double dz = points[first * 4 + 2] - points[second * 4 + 2];
                    double dr = firstRadius - secondRadius;
                    if (dr * dr > dx * dx + dy * dy + dz * dz) {
                        points[(dr > 0.0D ? second : first) * 4 + 3] = -1.0D;
                    }
                }
            }
            int outset = (int) Math.ceil((size / 16.0F * 2.0F + 1.0F) / 2.0F);
            int minimumX = (int) (locationX - Math.ceil(eighthSize) - outset);
            int minimumY = locationY - 2 - outset;
            int minimumZ = (int) (locationZ - Math.ceil(eighthSize) - outset);
            int horizontalSize = (int) (2 * (Math.ceil(eighthSize) + outset));
            int verticalSize = 2 * (2 + outset);
            int placed = 0;
            Arrays.fill(visitedWords, 0L);
            for (int index = 0; index < sizeInt; index++) {
                double radius = points[index * 4 + 3];
                if (radius <= 0.0D) continue;
                double centerX = points[index * 4];
                double centerY = points[index * 4 + 1];
                double centerZ = points[index * 4 + 2];
                int lowerX = (int) Math.max(Math.floor(centerX - radius), minimumX);
                int upperX = (int) Math.max(Math.floor(centerX + radius), lowerX);
                int lowerY = (int) Math.max(Math.floor(centerY - radius), minimumY);
                int upperY = (int) Math.max(Math.floor(centerY + radius), lowerY);
                int lowerZ = (int) Math.max(Math.floor(centerZ - radius), minimumZ);
                int upperZ = (int) Math.max(Math.floor(centerZ + radius), lowerZ);
                for (int x = lowerX; x <= upperX; x++) {
                    double dx = (x + 0.5D - centerX) / radius;
                    if (dx * dx >= 1.0D) continue;
                    for (int y = lowerY; y <= upperY; y++) {
                        double dy = (y + 0.5D - centerY) / radius;
                        if (dx * dx + dy * dy >= 1.0D) continue;
                        for (int z = lowerZ; z <= upperZ; z++) {
                            double dz = (z + 0.5D - centerZ) / radius;
                            if (dx * dx + dy * dy + dz * dz >= 1.0D) continue;
                            int visitedIndex = x - minimumX
                                + (y - minimumY) * horizontalSize
                                + (z - minimumZ) * horizontalSize * verticalSize;
                            int visitedWord = visitedIndex >>> 6;
                            long visitedMask = 1L << visitedIndex;
                            if ((visitedWords[visitedWord] & visitedMask) != 0L) continue;
                            visitedWords[visitedWord] |= visitedMask;
                            int authoredBlockY = authoredY + y - locationY;
                            if (authoredBlockY < OrigenDensityField.UPSTREAM_MIN_Y
                                || authoredBlockY >= OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE) continue;
                            AuthoredFeatureBlock existing = authoredFeatureBlock(x, authoredBlockY, z);
                            Block current = blockAtWithKnownFeature(x, authoredBlockY, z, existing).block();
                            if (!isReplaceable(current) || !mayExpose(x, authoredBlockY, z, random)) continue;
                            // Authored 384-high rock can project into a physically empty
                            // cave voxel. Ore and rock blobs replace substrate; they must not create
                            // floating deposits there. Use immutable terrain, not another chunk's
                            // possibly-later ore write, so chunk arrival order cannot affect admission.
                            // Strict source-parity mode retains its authored-space behavior.
                            if (mainlandHabitats != null && !isReplaceable(
                                resolveTerrainMaterial(
                                    physicalOreHostMaterial(x, heightTransport.physicalY(authoredBlockY), z)).block()))
                                continue;
                            OrigenBlockStateResolver.ResolvedBlock output = override(current);
                            if (placeResolvedBlockWithKnownFeature(
                                world,
                                x,
                                z,
                                authoredBlockY,
                                output,
                                physics,
                                existing)) {
                                placed++;
                            }
                        }
                    }
                }
            }
            return placed > 0;
        }

        private boolean isReplaceable(Block block) {
            for (Block candidate : replaceable) {
                if (candidate == block) return true;
            }
            return false;
        }

        private OrigenBlockStateResolver.ResolvedBlock override(Block block) {
            for (int index = 0; index < overrideBlocks.length; index++) {
                if (overrideBlocks[index] == block) return overrideStates[index];
            }
            return material;
        }

        private boolean mayExpose(int x, int authoredY, int z, Random random) {
            if (hiddenFromSky && mainlandHabitats != null && facesSky(x, authoredY, z)) return false;
            if (exposed >= 1.0D) return true;
            if (exposed > 0.0D && random.nextFloat() < exposed) return true;
            return blockAt(x, authoredY, z - 1).block() != net.minecraft.init.Blocks.air
                && blockAt(x, authoredY, z + 1).block() != net.minecraft.init.Blocks.air
                && blockAt(x, authoredY - 1, z).block() != net.minecraft.init.Blocks.air
                && blockAt(x, authoredY + 1, z).block() != net.minecraft.init.Blocks.air
                && blockAt(x - 1, authoredY, z).block() != net.minecraft.init.Blocks.air
                && blockAt(x + 1, authoredY, z).block() != net.minecraft.init.Blocks.air;
        }
    }

    private static final int[][] FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    /** Whether a face of the block touches air with nothing but air or water above it in the raw terrain. */
    private boolean facesSky(int x, int authoredY, int z) {
        for (int[] face : FACES) {
            int nx = x + face[0], ny = authoredY + face[1], nz = z + face[2];
            if (!heightTransport.containsAuthoredY(ny)) continue;
            if (blockAt(nx, ny, nz).block() != net.minecraft.init.Blocks.air) continue;
            if (openToSky(nx, heightTransport.physicalY(ny), nz)) return true;
        }
        return false;
    }

    private boolean openToSky(int x, int physicalY, int z) {
        return physicalY > rawTop(x, z);
    }

    /** The highest physical Y of the raw terrain's column that is neither air nor water, cached per chunk. */
    private int rawTop(int x, int z) {
        long key = horizontalKey(x >> 4, z >> 4);
        int[] tops = populationView.rawTops.get(key);
        if (tops == null) {
            byte[] terrain = populationView.rawTerrain
                .computeIfAbsent(key, ignored -> populationView.createRawTerrain(x >> 4, z >> 4));
            int top = 0;
            while (heightTransport.containsPhysicalY(top + 1)) top++;
            tops = new int[256];
            for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) {
                int y = top;
                while (y >= 0 && (terrain[heightTransport.physicalIndex(lx, y, lz)] == BiomicTerrainSampler.AIR
                    || terrain[heightTransport.physicalIndex(lx, y, lz)] == BiomicTerrainSampler.WATER)) y--;
                tops[lx * 16 + lz] = y;
            }
            populationView.rawTops.put(key, tops);
        }
        return tops[(x & 15) * 16 + (z & 15)];
    }

    private byte physicalOreHostMaterial(int x, int y, int z) {
        if (!heightTransport.containsPhysicalY(y)) return BiomicTerrainSampler.AIR;
        // The immutable raw terrain lacks the hosted soil skin. A later neighboring ore
        // source must not treat that native soil/shore patch as stone. Never derive this
        // reservation from arrival-dependent live block state; old profiles retain their policy.
        if (hostedOreSubstrateOwner != null && hostedOreSubstrateOwner.owns(x, y, z)) return BiomicTerrainSampler.AIR;
        // This is only used while running the population graph, which owns this bounded raw cache.
        byte[] terrain = populationView.rawTerrain
            .computeIfAbsent(horizontalKey(x >> 4, z >> 4), ignored -> populationView.createRawTerrain(x >> 4, z >> 4));
        return terrain[heightTransport.physicalIndex(x & 15, y, z & 15)];
    }

    static boolean isProjectedVeinHost(String featureId, byte physicalMaterial) {
        if ("COPPER_ORE_VEINS".equals(featureId)) return physicalMaterial == BiomicTerrainSampler.STONE;
        if ("IRON_ORE_VEINS".equals(featureId)) return physicalMaterial == BiomicTerrainSampler.DEEPSLATE;
        return true;
    }

    private final class Feature {

        private final String id;
        private final Distributor distributor;
        private final Locator locator;
        private final NoiseSampler selector;
        private final List<WeightedStructure> choices;
        private final int totalWeight;

        private Feature(String id, Distributor distributor, Locator locator, NoiseSampler selector,
            List<WeightedStructure> choices, int totalWeight) {
            this.id = id;
            this.distributor = distributor;
            this.locator = locator;
            this.selector = selector;
            this.choices = Collections.unmodifiableList(new ArrayList<>(choices));
            this.totalWeight = totalWeight;
        }

        private WeightedStructure selectChoice(long seed, int x, int authoredY, int z) {
            if (choices.size() == 1) return choices.get(0);
            int selected = MathUtil.normalizeIndex(selector.noise(seed, x, authoredY, z), totalWeight);
            for (int index = 0; index < choices.size(); index++) {
                WeightedStructure choice = choices.get(index);
                if (selected < choice.maximumExclusive) return choice;
            }
            throw new IllegalStateException("No weighted structure selected for " + id);
        }
    }

    /**
     * Terra features run against a bounded generation region. Forge 1.7 has no equivalent and its World accessors
     * generate missing chunks as a side effect, so this view reads loaded chunks directly, synthesizes absent base
     * terrain without loading it, and defers writes until the destination chunk exists.
     */
    private final class PopulationView {

        /** Open while a journaled population runs (null when the view applies a journal or runs live). */
        private final PopulationJournal journal = OrigenFeatureRuntime.this.journal;
        private final World world;
        private final IChunkProvider provider;
        private final PendingFeatureData pendingData;
        private final int originChunkX;
        private final int originChunkZ;
        private final AuthoredTerrainLookup authoredTerrainLookup;
        private final RawTerrainLookup rawTerrainLookup;
        private final CaveProjectionLookup caveProjectionLookup;
        private final Long2ObjectOpenHashMap<byte[]> rawTerrain = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<int[]> rawTops = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<byte[]> authoredChunks = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<byte[]> authoredTerrain = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<AuthoredFeatureColumn[]> authoredFeatureChunks = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<PopulationChunkChanges> modifiedChunks = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<OrigenPopulationNotifications> shiftedModifiedChunks = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<Boolean> absentOnDisk = new Long2ObjectOpenHashMap<>();
        private final BitSet floatingWaterLocations = new BitSet();
        private final BitSet twoBlockLocations = new BitSet();
        private final BitSet texturedStoneLocations = new BitSet();
        private final BitSet largeLeafVineLocations = new BitSet();
        private final BitSet smallDripleafLocations = new BitSet();
        private final WorldBlockSnapshotReuse worldBlockSnapshots = new WorldBlockSnapshotReuse();
        private final BlockColumnRef[] floatingWaterColumns = { new BlockColumnRef(), new BlockColumnRef(),
            new BlockColumnRef(), new BlockColumnRef(), new BlockColumnRef() };
        private boolean hasLastAuthoredChunk;
        private long lastAuthoredChunkKey;
        private byte[] lastAuthoredChunk;
        private boolean hasLastAuthoredColumn;
        private long lastAuthoredColumnKey;
        private byte[] lastAuthoredColumn;
        private boolean hasLastAuthoredFeatureChunk;
        private long lastAuthoredFeatureChunkKey;
        private AuthoredFeatureColumn[] lastAuthoredFeatureIndex;
        private boolean hasLastBlockColumn;
        private long lastBlockColumnKey;
        private byte[] lastBlockTerrain;
        private int lastBlockTerrainOffset;
        private AuthoredFeatureColumn lastBlockFeatures;
        private boolean hasLastLoadedChunk;
        private long lastLoadedChunkKey;
        private Chunk lastLoadedChunk;
        private boolean treeJournalWrite;
        private boolean inertStructureWrite;
        private boolean hasLastModifiedChunk;
        private long lastModifiedChunkKey;
        private PopulationChunkChanges lastModifiedChanges;
        private OrigenPopulationNotifications lastShiftedModifiedChanges;

        private PopulationView(World world, int originChunkX, int originChunkZ,
            AuthoredTerrainLookup authoredTerrainLookup, RawTerrainLookup rawTerrainLookup,
            CaveProjectionLookup caveProjectionLookup) {
            this.world = world;
            this.provider = world.getChunkProvider();
            this.pendingData = PendingFeatureData.load(world);
            this.originChunkX = originChunkX;
            this.originChunkZ = originChunkZ;
            this.authoredTerrainLookup = authoredTerrainLookup;
            this.rawTerrainLookup = rawTerrainLookup;
            this.caveProjectionLookup = caveProjectionLookup;
        }

        /** Journaled: every chunk counts as a live, fresh one; nothing depends on what happens to be loaded. */
        private boolean isLoaded(int chunkX, int chunkZ) {
            if (journal != null) return true;
            return loadedChunk(chunkX, chunkZ) != null;
        }

        /** Journaled: no neighbour counts as populated, so no decision depends on population order. */
        private boolean isPopulated(int chunkX, int chunkZ) {
            if (journal != null) return false;
            Chunk chunk = loadedChunk(chunkX, chunkZ);
            return chunk != null && chunk.isTerrainPopulated;
        }

        /** The chunk being populated by this view is never complete; any other populated chunk is. */
        private boolean populationComplete(Chunk chunk) {
            return chunk.isTerrainPopulated && !(chunk.xPosition == originChunkX && chunk.zPosition == originChunkZ);
        }

        /** Unsettled while any chunk of its 3x3 neighbourhood (including the current root) is still unpopulated. */
        private boolean populationUnsettled(Chunk chunk) {
            if (Math.abs(chunk.xPosition - originChunkX) <= 1 && Math.abs(chunk.zPosition - originChunkZ) <= 1)
                return true;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                Chunk neighbour = loadedChunk(chunk.xPosition + dx, chunk.zPosition + dz);
                if (neighbour == null || !neighbour.isTerrainPopulated) return true;
            }
            return false;
        }

        private String naturalCarrierRejection(int x, int y, int z, Block expected, int expectedMetadata) {
            if (!heightTransport.containsPhysicalY(y)) return "vertical-bounds";
            if (expected == null) return "null-expected";
            long targetKey = horizontalKey(x >> 4, z >> 4);
            Chunk target = journal != null ? null : loadedChunk(x >> 4, z >> 4);
            if (journal == null) {
                if (target == null && !absentOnDisk(targetKey, x >> 4, z >> 4))
                    return "unloaded-existing-or-unknown-disk-chunk";
                if (target != null && populationComplete(target)) return "completed-target-chunk";
                LinkedHashMap<Long, PendingPlacement> queued = pendingData.pendingPlacements.get(targetKey);
                boolean pending = queued != null && queued.containsKey(blockKey(x, y, z));
                if (pending) return "pending-placement";
            }
            if (isProtectedSurface(x, y, z, false)) return "village-protected-surface";
            boolean authoredHistory = false;
            for (int authoredY : heightTransport.authoredYsAtPhysical(y))
                if (authoredFeatureBlock(x, authoredY, z) != null) {
                    authoredHistory = true;
                    break;
                }
            if (authoredHistory) return "authored-feature-history";
            byte[] raw = rawTerrain.computeIfAbsent(targetKey, ignored -> createRawTerrain(x >> 4, z >> 4));
            WorldBlock natural = resolveTerrainMaterial(raw[heightTransport.physicalIndex(x & 15, y, z & 15)]);
            WorldBlock current = journal != null ? read(x, y, z)
                : target == null ? natural
                    : new WorldBlock(target.getBlock(x & 15, y, z & 15), target.getBlockMetadata(x & 15, y, z & 15));
            boolean exact = current.block() == expected && current.metadata() == expectedMetadata
                && natural.block() == expected
                && natural.metadata() == expectedMetadata;
            return exact ? null : "live-or-raw-state-mismatch";
        }

        private String freshNaturalSoilRejection(int x, int y, int z, Block expected, int expectedMetadata) {
            if (!heightTransport.containsPhysicalY(y)) return "vertical-bounds";
            if (expected == null) return "null-expected";
            long targetKey = horizontalKey(x >> 4, z >> 4);
            Chunk target = journal != null ? null : loadedChunk(x >> 4, z >> 4);
            if (journal == null) {
                boolean targetComplete = target != null && populationComplete(target);
                boolean targetSettlementRequired = target != null && populationUnsettled(target);
                boolean targetFreshUnexposed = targetSettlementRequired;
                boolean absent = target == null && absentOnDisk(targetKey, x >> 4, z >> 4);
                if (!freshNaturalSoilTarget(
                    target != null,
                    targetComplete,
                    targetSettlementRequired,
                    absent,
                    targetFreshUnexposed))
                    return target == null ? "unloaded-existing-or-unknown-disk-chunk" : "settled-target-chunk";
                LinkedHashMap<Long, PendingPlacement> queued = pendingData.pendingPlacements.get(targetKey);
                if (queued != null && queued.containsKey(blockKey(x, y, z))) return "pending-placement";
            }
            if (isProtectedSurface(x, y, z, false)) return "village-protected-surface";
            for (int authoredY : heightTransport.authoredYsAtPhysical(y))
                if (authoredFeatureBlock(x, authoredY, z) != null) return "authored-feature-history";
            byte[] raw = rawTerrain.computeIfAbsent(targetKey, ignored -> createRawTerrain(x >> 4, z >> 4));
            byte rawMaterial = raw[heightTransport.physicalIndex(x & 15, y, z & 15)];
            if (!HostedRockAttachment.solidTerrain(rawMaterial)) return "non-solid-base-terrain";
            WorldBlock current = journal != null ? read(x, y, z)
                : target == null ? resolveTerrainMaterial(rawMaterial)
                    : new WorldBlock(target.getBlock(x & 15, y, z & 15), target.getBlockMetadata(x & 15, y, z & 15));
            return current.block() == expected && current.metadata() == expectedMetadata ? null : "live-state-mismatch";
        }

        /** Disk existence is provenance, not terrain access: never load or generate the target chunk. */
        private boolean absentOnDisk(long key, int chunkX, int chunkZ) {
            if (absentOnDisk.containsKey(key)) return absentOnDisk.get(key)
                .booleanValue();
            Object loader = world.getSaveHandler()
                .getChunkLoader(world.provider);
            boolean anvilLoader = loader instanceof AnvilChunkLoader;
            boolean exists = anvilLoader && ((AnvilChunkLoader) loader).chunkExists(world, chunkX, chunkZ);
            boolean absent = absentDiskNaturalCarrierTarget(anvilLoader, exists);
            absentOnDisk.put(key, Boolean.valueOf(absent));
            return absent;
        }

        /** Population is server-thread confined, so a destination cannot change loaded state during this view. */
        private Chunk loadedChunk(int chunkX, int chunkZ) {
            long key = horizontalKey(chunkX, chunkZ);
            if (!hasLastLoadedChunk || lastLoadedChunkKey != key) {
                hasLastLoadedChunk = true;
                lastLoadedChunkKey = key;
                lastLoadedChunk = provider.chunkExists(chunkX, chunkZ) ? provider.provideChunk(chunkX, chunkZ) : null;
            }
            return lastLoadedChunk;
        }

        private WorldBlock read(int x, int y, int z) {
            if (!heightTransport.containsPhysicalY(y)) return WorldBlock.AIR;
            if (hostedEnvironmentTransaction != null) {
                PendingBlock buffered = hostedEnvironmentTransaction.get(blockKey(x, y, z));
                if (buffered != null) {
                    var state = resolve(buffered.state());
                    return worldBlockSnapshots.snapshot(state.block(), state.metadata());
                }
            }
            if (journal != null) {
                PendingBlock own = journal.writes.get(blockKey(x, y, z));
                if (own != null) {
                    var state = resolve(own.state());
                    return worldBlockSnapshots.snapshot(state.block(), state.metadata());
                }
                WorldBlock provided = baselineState(x, y, z);
                return worldBlockSnapshots.snapshot(provided.block(), provided.metadata());
            }
            long chunkKey = horizontalKey(x >> 4, z >> 4);
            LinkedHashMap<Long, PendingPlacement> queued = pendingData.pendingPlacements.isEmpty() ? null
                : pendingData.pendingPlacements.get(chunkKey);
            PendingPlacement pending = queued == null ? null : queued.get(blockKey(x, y, z));
            if (pending != null) {
                OrigenBlockStateResolver.ResolvedBlock state = resolve(
                    pending.block()
                        .state());
                return worldBlockSnapshots.snapshot(state.block(), state.metadata());
            }
            Chunk chunk = loadedChunk(x >> 4, z >> 4);
            if (chunk != null) {
                Block block = chunk.getBlock(x & 15, y, z & 15);
                int metadata = chunk.getBlockMetadata(x & 15, y, z & 15);
                return worldBlockSnapshots.snapshot(block, metadata);
            }
            byte[] raw = rawTerrain.computeIfAbsent(chunkKey, ignored -> createRawTerrain(x >> 4, z >> 4));
            int index = heightTransport.physicalIndex(x & 15, y, z & 15);
            WorldBlock state = resolveTerrainMaterial(raw[index]);
            return worldBlockSnapshots.snapshot(state.block(), state.metadata());
        }

        private boolean write(int x, int y, int z, PendingBlock placement) {
            if (!heightTransport.containsPhysicalY(y)) return false;
            if (hostedEnvironmentTransaction != null) {
                if (rejectHostedEnvironmentWrite(x, y, z)) {
                    hostedEnvironmentTransactionRejected = true;
                    return false;
                }
                long key = blockKey(x, y, z);
                if (!hostedEnvironmentTransaction.containsKey(key)
                    && hostedEnvironmentTransaction.size() >= HOSTED_ENVIRONMENT_TRANSACTION_CAP) {
                    hostedEnvironmentTransactionRejected = true;
                    return false;
                }
                hostedEnvironmentTransaction.put(key, placement);
                return true;
            }
            if (rejectHostedEnvironmentWrite(x, y, z)) return false;
            if (journal != null) {
                if (CellTrace.ON) CellTrace.log(
                    x,
                    y,
                    z,
                    "journal of chunk {},{} writes {} (feature {}) {}",
                    journal.chunkX,
                    journal.chunkZ,
                    placement.state(),
                    activeFeatureId,
                    CellTrace.caller());
                journal.put(blockKey(x, y, z), placement);
                return true;
            }
            beforeTreeWrite(x, y, z, placement.state());
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            Chunk chunk = loadedChunk(chunkX, chunkZ);
            if (chunk != null) {
                return placeInChunk(world, chunk, x, y, z, placement);
            }
            queuePending(new PendingPlacement(x, y, z, placement));
            return true;
        }

        private boolean write(int x, int y, int z, OrigenBlockStateResolver.ResolvedBlock resolved, boolean physics) {
            if (!heightTransport.containsPhysicalY(y)) return false;
            if (hostedEnvironmentTransaction != null)
                return write(x, y, z, new PendingBlock(canonicalState(resolved), physics));
            if (rejectHostedEnvironmentWrite(x, y, z)) return false;
            if (journal != null) {
                journal.put(blockKey(x, y, z), new PendingBlock(canonicalState(resolved), physics));
                return true;
            }
            beforeTreeWrite(x, y, z, canonicalState(resolved));
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            Chunk chunk = loadedChunk(chunkX, chunkZ);
            if (chunk != null) {
                return placeInChunk(world, chunk, x, y, z, resolved);
            }
            PendingBlock pending = new PendingBlock(canonicalState(resolved), physics);
            queuePending(new PendingPlacement(x, y, z, pending));
            return true;
        }

        private void queuePending(PendingPlacement placement) {
            long chunkKey = horizontalKey(placement.x() >> 4, placement.z() >> 4);
            var queued = pendingData.pendingPlacements.computeIfAbsent(chunkKey, ignored -> new LinkedHashMap<>());
            queued.put(blockKey(placement.x(), placement.y(), placement.z()), placement);
            pendingData.markDirty();
        }

        private HostedTreeLedger.Access treeAccess() {
            return new HostedTreeLedger.Access() {

                @Override
                public String read(industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos pos) {
                    WorldBlock state = PopulationView.this.read(pos.x, pos.y, pos.z);
                    return treeState(state.block(), state.metadata());
                }

                @Override
                public void write(industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos pos, String state) {
                    PopulationView.this.write(pos.x, pos.y, pos.z, new PendingBlock(state, false));
                }
            };
        }

        private boolean rejectHostedEnvironmentWrite(int x, int y, int z) {
            if (hostedPhysicalPopulation || activeFeatureId == null || activeFeatureStage == null) return false;
            return intrinsicEnvironmentOwner != null && intrinsicEnvironmentOwner.owns(x, y, z)
                || hostedEnvironmentOwner != null && hostedEnvironmentOwner.owns(x, y, z)
                    && AbyssalHostPollutionPolicy.displaced(activeFeatureStage, activeFeatureId);
        }

        private LinkedHashMap<Long, PendingBlock> hostedEnvironmentTransaction;
        private HostedAuthoredJournal<AuthoredFeatureBlock> hostedEnvironmentAuthoredBefore;
        private List<PendingEntity> hostedEnvironmentEntities;
        private boolean hostedEnvironmentTransactionRejected;

        private void beginHostedEnvironmentTransaction() {
            if (hostedEnvironmentTransaction != null)
                throw new IllegalStateException("Nested hosted environment feature transaction");
            hostedEnvironmentTransaction = new LinkedHashMap<>();
            hostedEnvironmentAuthoredBefore = new HostedAuthoredJournal<>();
            hostedEnvironmentEntities = new ArrayList<>();
            hostedEnvironmentTransactionRejected = false;
        }

        private boolean finishHostedEnvironmentTransaction() {
            if (hostedEnvironmentTransaction == null) return true;
            LinkedHashMap<Long, PendingBlock> writes = hostedEnvironmentTransaction;
            boolean accepted = !hostedEnvironmentTransactionRejected;
            List<PendingEntity> entities = hostedEnvironmentEntities;
            hostedEnvironmentEntities = null;
            hostedEnvironmentTransaction = null;
            hostedEnvironmentTransactionRejected = false;
            if (!accepted) restoreHostedEnvironmentAuthored();
            boolean committed = commitAdmittedHostedEnvironmentWrites(
                writes,
                !accepted,
                (position,
                    placement) -> write(blockKeyX(position), blockKeyY(position), blockKeyZ(position), placement));
            if (committed) hostedEnvironmentAuthoredBefore = null;
            publishHostedProposalEntities(
                entities,
                committed,
                entity -> spawnEntity(world, entity.x(), entity.y(), entity.z(), entity.id()));
            return committed;
        }

        private void restoreHostedEnvironmentAuthored() {
            if (hostedEnvironmentAuthoredBefore == null) return;
            hostedEnvironmentAuthoredBefore.restore((position, previous) -> {
                int x = blockKeyX(position), y = blockKeyY(position), z = blockKeyZ(position);
                var chunk = authoredFeatureChunks.get(horizontalKey(x >> 4, z >> 4));
                setIndexedAuthoredFeatureBlock(chunk, x, y, z, previous);
            });
            hostedEnvironmentAuthoredBefore = null;
        }

        private void discardHostedEnvironmentTransaction() {
            restoreHostedEnvironmentAuthored();
            hostedEnvironmentTransaction = null;
            hostedEnvironmentEntities = null;
            hostedEnvironmentTransactionRejected = false;
        }

        private void beforeTreeWrite(int x, int y, int z, String state) {
            if (treeJournalWrite || pendingData.hostedTrees.treeCount() == 0) return;
            var pos = new industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos(x, y, z);
            treeJournalWrite = true;
            try {
                if (LARGE_MUSHROOMS.equals(activeFeatureId) && HostedTreeLedger.mushroom(state))
                    pendingData.hostedTrees.mushroomAt(pos, treeAccess());
                pendingData.hostedTrees.forget(pos);
                pendingData.markDirty();
            } finally {
                treeJournalWrite = false;
            }
        }

        private void recordChange(int chunkX, int chunkZ, int x, int y, int z, boolean tileEntity,
            boolean lightingChanged) {
            long key = horizontalKey(chunkX, chunkZ);
            if (!hasLastModifiedChunk || lastModifiedChunkKey != key) {
                hasLastModifiedChunk = true;
                lastModifiedChunkKey = key;
                if (heightTransport.compressed()) {
                    lastModifiedChanges = modifiedChunks.get(key);
                    if (lastModifiedChanges == null) {
                        lastModifiedChanges = new PopulationChunkChanges(ForgeModContainer.clumpingThreshold);
                        modifiedChunks.put(key, lastModifiedChanges);
                    }
                } else {
                    lastShiftedModifiedChanges = shiftedModifiedChunks.get(key);
                    if (lastShiftedModifiedChanges == null) {
                        lastShiftedModifiedChanges = new OrigenPopulationNotifications(
                            heightTransport.layout(),
                            ForgeModContainer.clumpingThreshold);
                        shiftedModifiedChunks.put(key, lastShiftedModifiedChanges);
                    }
                }
            }
            if (heightTransport.compressed()) lastModifiedChanges.record(x, y, z, tileEntity, lightingChanged);
            else lastShiftedModifiedChanges.record(x, y, z, tileEntity, lightingChanged);
        }

        private byte authoredTerrainMaterial(int x, int authoredY, int z) {
            long chunkKey = horizontalKey(x >> 4, z >> 4);
            if (!hasLastAuthoredChunk || lastAuthoredChunkKey != chunkKey) {
                hasLastAuthoredChunk = true;
                lastAuthoredChunkKey = chunkKey;
                lastAuthoredChunk = authoredChunks.get(chunkKey);
                if (lastAuthoredChunk == null && !authoredChunks.containsKey(chunkKey)) {
                    lastAuthoredChunk = surfaceRuntime.cachedAuthoredChunk(x >> 4, z >> 4);
                    if (lastAuthoredChunk == null && authoredTerrainLookup != null) {
                        lastAuthoredChunk = authoredTerrainLookup.find(x >> 4, z >> 4);
                        if (lastAuthoredChunk != null) {
                            OrigenFeatureTerrainAccess.requireAuthoredChunk(lastAuthoredChunk);
                            authoredTerrainPayloadRecoveries++;
                            surfaceRuntime.rememberAuthoredChunk(x >> 4, z >> 4, lastAuthoredChunk);
                        }
                    }
                    // Journaled, a chunk's authored terrain is always Origen's own copy, never the column fallback.
                    if (lastAuthoredChunk == null && journal != null)
                        lastAuthoredChunk = surfaceRuntime.createAuthoredChunk(x >> 4, z >> 4);
                    if (lastAuthoredChunk == null) {
                        authoredTerrainCacheMisses++;
                        int requestedChunkX = x >> 4;
                        int requestedChunkZ = z >> 4;
                        if (isLoaded(requestedChunkX, requestedChunkZ)) {
                            authoredTerrainLoadedMisses++;
                        } else {
                            authoredTerrainUnloadedMisses++;
                        }
                        int distance = Math
                            .max(Math.abs(requestedChunkX - originChunkX), Math.abs(requestedChunkZ - originChunkZ));
                        authoredTerrainMissDistance[Math.min(distance, authoredTerrainMissDistance.length - 1)]++;
                    } else {
                        authoredTerrainCacheHits++;
                    }
                    if (lastAuthoredChunk != null) {
                        byte[] view = cachedAuthoredView(chunkKey);
                        if (view == null) {
                            BitSet completedMask = caveProjectionLookup == null ? null
                                : caveProjectionLookup.find(x >> 4, z >> 4);
                            if (completedMask != null) caveProjectionPayloadRecoveries++;
                            view = terrainAccess.authoredViewWithPreparedNativeMarsh(
                                lastAuthoredChunk,
                                (x >> 4) << 4,
                                (z >> 4) << 4,
                                () -> rawTerrain.computeIfAbsent(chunkKey, ignored -> createRawTerrain(x >> 4, z >> 4)),
                                completedMask,
                                null);
                            rememberAuthoredView(chunkKey, view);
                        }
                        lastAuthoredChunk = view;
                    }
                    authoredChunks.put(chunkKey, lastAuthoredChunk);
                }
            }
            if (lastAuthoredChunk != null) {
                return lastAuthoredChunk[OrigenSurfaceRuntime.authoredMaterialIndex(x & 15, authoredY, z & 15)];
            }

            long columnKey = horizontalKey(x, z);
            if (!hasLastAuthoredColumn || lastAuthoredColumnKey != columnKey) {
                hasLastAuthoredColumn = true;
                lastAuthoredColumnKey = columnKey;
                lastAuthoredColumn = authoredTerrain.get(columnKey);
                if (lastAuthoredColumn == null) {
                    lastAuthoredColumn = OrigenFeatureRuntime.this.authoredTerrainColumn(x, z);
                    authoredTerrain.put(columnKey, lastAuthoredColumn);
                }
            }
            return lastAuthoredColumn[authoredY - OrigenDensityField.UPSTREAM_MIN_Y];
        }

        private AuthoredFeatureBlock authoredFeatureBlock(int x, int authoredY, int z) {
            if (authoredY < OrigenDensityField.UPSTREAM_MIN_Y
                || authoredY >= OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE) return null;
            long chunkKey = horizontalKey(x >> 4, z >> 4);
            if (!hasLastAuthoredFeatureChunk || lastAuthoredFeatureChunkKey != chunkKey) {
                lastAuthoredFeatureIndex = authoredFeatureChunks.get(chunkKey);
                // Journaled, the overlay is the population's own: a neighbour's earlier features never show through.
                if (lastAuthoredFeatureIndex == null && !authoredFeatureChunks.containsKey(chunkKey)) {
                    if (journal == null) synchronized (authoredFeatureCache) {
                        lastAuthoredFeatureIndex = authoredFeatureCache.getAndMoveToLast(chunkKey);
                    }
                    authoredFeatureChunks.put(chunkKey, lastAuthoredFeatureIndex);
                }
                hasLastAuthoredFeatureChunk = true;
                lastAuthoredFeatureChunkKey = chunkKey;
            }
            if (lastAuthoredFeatureIndex == null) return null;
            AuthoredFeatureColumn column = lastAuthoredFeatureIndex[columnIndex(x, z)];
            return column == null ? null : column.get(authoredY - OrigenDensityField.UPSTREAM_MIN_Y);
        }

        private WorldBlock authoredBlock(int x, int authoredY, int z) {
            selectBlockColumn(x, z);
            int index = authoredY - OrigenDensityField.UPSTREAM_MIN_Y;
            AuthoredFeatureBlock feature = lastBlockFeatures == null ? null : lastBlockFeatures.get(index);
            return feature == null ? resolveTerrainMaterial(lastBlockTerrain[lastBlockTerrainOffset + index])
                : feature.block();
        }

        private boolean authoredAir(int x, int authoredY, int z) {
            selectBlockColumn(x, z);
            return selectedBlockAir(authoredY - OrigenDensityField.UPSTREAM_MIN_Y);
        }

        private boolean selectedBlockAir(int index) {
            if (index < 0 || index >= OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE - OrigenDensityField.UPSTREAM_MIN_Y)
                return true;
            AuthoredFeatureBlock feature = lastBlockFeatures == null ? null : lastBlockFeatures.get(index);
            return feature == null ? lastBlockTerrain[lastBlockTerrainOffset + index] == BiomicTerrainSampler.AIR
                : feature.block()
                    .block() == net.minecraft.init.Blocks.air;
        }

        private BitSet scanSurface(int x, int z, int minimum, int maximum, BitSet candidates) {
            int clampedMinimum = Math.max(minimum, OrigenDensityField.UPSTREAM_MIN_Y);
            int clampedMaximum = Math.min(maximum, OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE);
            BitSet result = candidates == null ? new BitSet() : candidates;
            if (clampedMinimum >= clampedMaximum) {
                result.clear();
                return result;
            }
            selectBlockColumn(x, z);
            if (candidates == null) {
                boolean belowAir = selectedBlockAir(location(clampedMinimum) - 1);
                for (int y = clampedMinimum; y < clampedMaximum; y++) {
                    int location = location(y);
                    boolean currentAir = selectedBlockAir(location);
                    if (currentAir && !belowAir) result.set(location);
                    belowAir = currentAir;
                }
                return result;
            }
            result.clear(0, location(clampedMinimum));
            result.clear(location(clampedMaximum), Math.max(location(clampedMaximum), result.length()));
            for (int location = result.nextSetBit(0); location >= 0; location = result.nextSetBit(location + 1)) {
                if (!selectedBlockAir(location) || selectedBlockAir(location - 1)) result.clear(location);
            }
            return result;
        }

        private BitSet scanFloatingWaterContainment(int x, int z, int minimum, int maximum, BlockPattern water,
            boolean above, boolean below, boolean adjacent) {
            int clampedMinimum = Math.max(minimum, OrigenDensityField.UPSTREAM_MIN_Y);
            int clampedMaximum = Math.min(maximum, OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE);
            BitSet result = floatingWaterLocations;
            result.clear();
            if (clampedMinimum >= clampedMaximum) return result;
            loadBlockColumn(x, z, floatingWaterColumns[0]);
            if (adjacent) {
                loadBlockColumn(x, z - 1, floatingWaterColumns[1]);
                loadBlockColumn(x, z + 1, floatingWaterColumns[2]);
                loadBlockColumn(x - 1, z, floatingWaterColumns[3]);
                loadBlockColumn(x + 1, z, floatingWaterColumns[4]);
            }
            BlockColumnRef center = floatingWaterColumns[0];
            for (int authoredY = clampedMinimum; authoredY < clampedMaximum; authoredY++) {
                if (!columnAir(center, authoredY)) continue;
                boolean contained = above && columnMatches(floatingWaterColumns[0], authoredY + 1, water)
                    || below && columnMatches(floatingWaterColumns[0], authoredY - 1, water);
                if (!contained && adjacent) {
                    contained = columnMatches(floatingWaterColumns[1], authoredY, water)
                        || columnMatches(floatingWaterColumns[2], authoredY, water)
                        || columnMatches(floatingWaterColumns[3], authoredY, water)
                        || columnMatches(floatingWaterColumns[4], authoredY, water);
                }
                if (contained) setLocation(result, authoredY);
            }
            return result;
        }

        private BitSet scanTwoBlockPattern(int x, int z, int minimum, int maximum, BlockPattern current,
            BlockPattern support) {
            int clampedMinimum = Math.max(minimum, OrigenDensityField.UPSTREAM_MIN_Y);
            int clampedMaximum = Math.min(maximum, OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE);
            BitSet result = twoBlockLocations;
            result.clear();
            if (clampedMinimum >= clampedMaximum) return result;
            BlockColumnRef column = floatingWaterColumns[0];
            loadBlockColumn(x, z, column);
            for (int authoredY = clampedMinimum; authoredY < clampedMaximum; authoredY++) {
                if ((current == null ? columnAir(column, authoredY) : columnMatches(column, authoredY, current))
                    && columnMatches(column, authoredY - 1, support)) {
                    setLocation(result, authoredY);
                }
            }
            return result;
        }

        private BitSet scanTexturedStoneSlant(int x, int z, int minimum, int maximum, NoiseSampler sampler,
            BlockPattern rock) {
            int clampedMinimum = Math.max(minimum, OrigenDensityField.UPSTREAM_MIN_Y);
            int clampedMaximum = Math.min(maximum, OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE);
            BitSet result = texturedStoneLocations;
            result.clear();
            if (clampedMinimum >= clampedMaximum) return result;
            loadBlockColumn(x, z, floatingWaterColumns[0]);
            loadBlockColumn(x, z - 1, floatingWaterColumns[1]);
            loadBlockColumn(x, z + 1, floatingWaterColumns[2]);
            loadBlockColumn(x - 1, z, floatingWaterColumns[3]);
            loadBlockColumn(x + 1, z, floatingWaterColumns[4]);
            BlockColumnRef center = floatingWaterColumns[0];
            long seed = world.getSeed();
            for (int authoredY = clampedMinimum; authoredY < clampedMaximum; authoredY++) {
                if (sampler.noise(seed, x, authoredY, z) <= 0.0D || !columnMatches(center, authoredY, rock)
                    || !anyAdjacentAir(authoredY)) {
                    continue;
                }
                boolean rising = columnMatches(center, authoredY + 1, rock) && anyAdjacentSlant(authoredY, 1, -1, rock);
                boolean falling = columnMatches(center, authoredY - 1, rock)
                    && anyAdjacentSlant(authoredY, -1, 1, rock);
                if (rising || falling) setLocation(result, authoredY);
            }
            return result;
        }

        private BitSet scanLargeLeafVines(int x, int z, int minimum, int maximum, NoiseSampler sampler,
            BlockPattern solid) {
            int clampedMinimum = Math.max(minimum, OrigenDensityField.UPSTREAM_MIN_Y);
            int clampedMaximum = Math.min(maximum, OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE);
            BitSet result = largeLeafVineLocations;
            result.clear();
            if (clampedMinimum >= clampedMaximum) return result;
            loadBlockColumn(x, z, floatingWaterColumns[0]);
            loadBlockColumn(x, z - 1, floatingWaterColumns[1]);
            loadBlockColumn(x, z + 1, floatingWaterColumns[2]);
            loadBlockColumn(x - 1, z, floatingWaterColumns[3]);
            loadBlockColumn(x + 1, z, floatingWaterColumns[4]);
            BlockColumnRef center = floatingWaterColumns[0];
            long seed = world.getSeed();
            for (int authoredY = clampedMinimum; authoredY < clampedMaximum; authoredY++) {
                if (!columnAir(center, authoredY)) continue;
                boolean touchesSolid = columnMatches(center, authoredY + 1, solid)
                    || columnMatches(center, authoredY - 1, solid);
                if (!touchesSolid) {
                    for (int index = 1; index < floatingWaterColumns.length; index++) {
                        if (columnMatches(floatingWaterColumns[index], authoredY, solid)) {
                            touchesSolid = true;
                            break;
                        }
                    }
                }
                if (touchesSolid && sampler.noise(seed, x, authoredY, z) > 0.0D) {
                    setLocation(result, authoredY);
                }
            }
            return result;
        }

        private BitSet scanSmallDripleaf(int x, int z, int minimum, int maximum, BlockPattern aboveAir,
            BlockPattern water, BlockPattern wetSupport, BlockPattern dryAir, BlockPattern drySupport) {
            int clampedMinimum = Math.max(minimum, OrigenDensityField.UPSTREAM_MIN_Y);
            int clampedMaximum = Math.min(maximum, OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE);
            BitSet result = smallDripleafLocations;
            result.clear();
            if (clampedMinimum >= clampedMaximum) return result;
            BlockColumnRef column = floatingWaterColumns[0];
            loadBlockColumn(x, z, column);
            for (int authoredY = clampedMinimum; authoredY < clampedMaximum; authoredY++) {
                if (!columnMatches(column, authoredY + 1, aboveAir)) continue;
                boolean wet = columnMatches(column, authoredY, water)
                    && columnMatches(column, authoredY - 1, wetSupport);
                boolean dry = columnMatches(column, authoredY, dryAir)
                    && columnMatches(column, authoredY - 1, drySupport);
                if (wet || dry) setLocation(result, authoredY);
            }
            return result;
        }

        private BitSet retainArchGlowLichen(int x, int z, int minimum, int maximum, BitSet candidates, BlockPattern air,
            BlockPattern water, BlockPattern solid) {
            int clampedMinimum = Math.max(minimum, OrigenDensityField.UPSTREAM_MIN_Y);
            int clampedMaximum = Math.min(maximum, OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE);
            if (clampedMinimum >= clampedMaximum) {
                candidates.clear();
                return candidates;
            }
            candidates.clear(0, location(clampedMinimum));
            candidates.clear(location(clampedMaximum), Math.max(location(clampedMaximum), candidates.length()));
            loadBlockColumn(x, z, floatingWaterColumns[0]);
            loadBlockColumn(x, z - 1, floatingWaterColumns[1]);
            loadBlockColumn(x, z + 1, floatingWaterColumns[2]);
            loadBlockColumn(x - 1, z, floatingWaterColumns[3]);
            loadBlockColumn(x + 1, z, floatingWaterColumns[4]);
            BlockColumnRef center = floatingWaterColumns[0];
            for (int location = candidates.nextSetBit(0); location
                >= 0; location = candidates.nextSetBit(location + 1)) {
                int authoredY = authoredY(location);
                if (!columnMatches(center, authoredY, air) && !columnMatches(center, authoredY, water)) {
                    candidates.clear(location);
                    continue;
                }
                boolean touchesSolid = columnMatches(center, authoredY + 1, solid)
                    || columnMatches(center, authoredY - 1, solid);
                if (!touchesSolid) {
                    for (int index = 1; index < floatingWaterColumns.length; index++) {
                        if (columnMatches(floatingWaterColumns[index], authoredY, solid)) {
                            touchesSolid = true;
                            break;
                        }
                    }
                }
                if (!touchesSolid) candidates.clear(location);
            }
            return candidates;
        }

        private boolean anyAdjacentAir(int authoredY) {
            for (int index = 1; index < floatingWaterColumns.length; index++) {
                if (columnAir(floatingWaterColumns[index], authoredY)) return true;
            }
            return false;
        }

        private boolean anyAdjacentSlant(int authoredY, int airOffset, int rockOffset, BlockPattern rock) {
            for (int index = 1; index < floatingWaterColumns.length; index++) {
                BlockColumnRef adjacent = floatingWaterColumns[index];
                if (columnAir(adjacent, authoredY + airOffset) && columnMatches(adjacent, authoredY + rockOffset, rock))
                    return true;
            }
            return false;
        }

        private void loadBlockColumn(int x, int z, BlockColumnRef target) {
            selectBlockColumn(x, z);
            target.terrain = lastBlockTerrain;
            target.terrainOffset = lastBlockTerrainOffset;
            target.features = lastBlockFeatures;
        }

        private boolean columnAir(BlockColumnRef column, int authoredY) {
            int index = authoredY - OrigenDensityField.UPSTREAM_MIN_Y;
            if (index < 0 || index >= OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE - OrigenDensityField.UPSTREAM_MIN_Y)
                return true;
            AuthoredFeatureBlock feature = column.features == null ? null : column.features.get(index);
            return feature == null ? column.terrain[column.terrainOffset + index] == BiomicTerrainSampler.AIR
                : feature.block()
                    .block() == net.minecraft.init.Blocks.air;
        }

        private boolean columnMatches(BlockColumnRef column, int authoredY, BlockPattern expected) {
            int index = authoredY - OrigenDensityField.UPSTREAM_MIN_Y;
            if (index < 0 || index >= OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE - OrigenDensityField.UPSTREAM_MIN_Y)
                return false;
            AuthoredFeatureBlock feature = column.features == null ? null : column.features.get(index);
            if (feature == null) {
                return expected.matchesTerrainMaterial(column.terrain[column.terrainOffset + index]);
            }
            WorldBlock block = feature.block();
            return expected.matchesState(block.block(), block.metadata());
        }

        private void retainBlockMatches(int x, int z, BlockPattern pattern, int columnMinimum, int columnMaximum,
            BitSet candidates) {
            selectBlockColumn(x, z);
            for (int location = candidates.nextSetBit(0); location
                >= 0; location = candidates.nextSetBit(location + 1)) {
                if (!selectedBlockMatches(pattern, authoredY(location), columnMinimum, columnMaximum)) {
                    candidates.clear(location);
                }
            }
        }

        private void retainBlockMatches(int x, int z, BlockPattern[] patterns, int columnMinimum, int columnMaximum,
            BitSet candidates) {
            selectBlockColumn(x, z);
            for (int location = candidates.nextSetBit(0); location
                >= 0; location = candidates.nextSetBit(location + 1)) {
                int authoredY = authoredY(location);
                for (BlockPattern pattern : patterns) {
                    if (!selectedBlockMatches(pattern, authoredY, columnMinimum, columnMaximum)) {
                        candidates.clear(location);
                        break;
                    }
                }
            }
        }

        private boolean selectedBlockMatches(BlockPattern pattern, int authoredY, int columnMinimum,
            int columnMaximum) {
            int minimum = clampedPatternMinimum(authoredY, pattern.offsets.minimum, columnMinimum);
            int maximum = clampedPatternMaximum(authoredY, pattern.offsets.maximum, columnMaximum);
            if (maximum <= minimum) return false;
            for (int testY = minimum; testY < maximum; testY++) {
                int index = testY - OrigenDensityField.UPSTREAM_MIN_Y;
                AuthoredFeatureBlock feature = lastBlockFeatures == null ? null : lastBlockFeatures.get(index);
                if (feature == null) {
                    if (!pattern.matchesTerrainMaterial(lastBlockTerrain[lastBlockTerrainOffset + index])) return false;
                } else {
                    WorldBlock block = feature.block();
                    if (!pattern.matchesState(block.block(), block.metadata())) return false;
                }
            }
            return true;
        }

        private void selectBlockColumn(int x, int z) {
            long columnKey = horizontalKey(x, z);
            if (!hasLastBlockColumn || lastBlockColumnKey != columnKey) {
                authoredTerrainMaterial(x, OrigenDensityField.UPSTREAM_MIN_Y, z);
                if (lastAuthoredChunk == null) {
                    lastBlockTerrain = lastAuthoredColumn;
                    lastBlockTerrainOffset = 0;
                } else {
                    lastBlockTerrain = lastAuthoredChunk;
                    lastBlockTerrainOffset = OrigenSurfaceRuntime
                        .authoredMaterialIndex(x & 15, OrigenDensityField.UPSTREAM_MIN_Y, z & 15);
                }
                authoredFeatureBlock(x, OrigenDensityField.UPSTREAM_MIN_Y, z);
                lastBlockFeatures = lastAuthoredFeatureIndex == null ? null
                    : lastAuthoredFeatureIndex[columnIndex(x, z)];
                hasLastBlockColumn = true;
                lastBlockColumnKey = columnKey;
            }
        }

        private void rememberAuthoredFeatureBlock(long chunkKey, AuthoredFeatureColumn[] featureChunk, int x, int z) {
            if (hasLastAuthoredFeatureChunk && lastAuthoredFeatureChunkKey == chunkKey) {
                lastAuthoredFeatureIndex = featureChunk;
            }
            if (hasLastBlockColumn && lastBlockColumnKey == horizontalKey(x, z)) {
                lastBlockFeatures = featureChunk[columnIndex(x, z)];
            }
        }

        /**
         * A population pass is deliberately single-threaded (the enclosing runtime rejects re-entry). Resolve and
         * touch the shared LRU once per destination chunk, then mutate that same paged view through the pass-local
         * cache. Large trees and ore veins otherwise took the global monitor and relinked the LRU for every voxel.
         */
        private boolean writeAuthoredFeatureBlock(int x, int authoredY, int z, AuthoredFeatureBlock written) {
            if (hostedEnvironmentAuthoredBefore != null) {
                long key = blockKey(x, authoredY, z);
                if (!hostedEnvironmentAuthoredBefore.record(key, () -> authoredFeatureBlock(x, authoredY, z))) {
                    hostedEnvironmentTransactionRejected = true;
                    return false;
                }
            }
            long chunkKey = horizontalKey(x >> 4, z >> 4);
            AuthoredFeatureColumn[] featureChunk = hasLastAuthoredFeatureChunk
                && lastAuthoredFeatureChunkKey == chunkKey ? lastAuthoredFeatureIndex
                    : authoredFeatureChunks.get(chunkKey);
            if (featureChunk == null) {
                if (journal != null) featureChunk = new AuthoredFeatureColumn[AUTHORED_FEATURE_COLUMN_COUNT];
                else synchronized (authoredFeatureCache) {
                    featureChunk = authoredFeatureCache.getAndMoveToLast(chunkKey);
                    if (featureChunk == null) {
                        featureChunk = new AuthoredFeatureColumn[AUTHORED_FEATURE_COLUMN_COUNT];
                        authoredFeatureCache.putAndMoveToLast(chunkKey, featureChunk);
                        trimAuthoredFeatureCache();
                    }
                }
                authoredFeatureChunks.put(chunkKey, featureChunk);
            }
            AuthoredFeatureBlock previous = setIndexedAuthoredFeatureBlock(featureChunk, x, authoredY, z, written);
            rememberAuthoredFeatureBlock(chunkKey, featureChunk, x, z);
            WorldBlock replacement = written.block();
            return previous == null || previous.block()
                .block() != replacement.block()
                || previous.block()
                    .metadata() != replacement.metadata();
        }

        private void forgetAuthoredFeatureBlock(long chunkKey, AuthoredFeatureColumn[] featureChunk, int x, int z) {
            authoredFeatureChunks.put(chunkKey, featureChunk);
            if (hasLastAuthoredFeatureChunk && lastAuthoredFeatureChunkKey == chunkKey) {
                lastAuthoredFeatureIndex = featureChunk;
            }
            if (hasLastBlockColumn && lastBlockColumnKey == horizontalKey(x, z)) {
                lastBlockFeatures = featureChunk[columnIndex(x, z)];
            }
        }

        private int columnIndex(int x, int z) {
            return authoredFeatureColumnIndex(x, z);
        }

        private void queueEntity(int x, int y, int z, String id) {
            if (!heightTransport.containsPhysicalY(y))
                throw new IllegalArgumentException("Origen entity is outside the selected physical layout: " + y);
            pendingData.pendingEntities.computeIfAbsent(horizontalKey(x >> 4, z >> 4), ignored -> new ArrayList<>())
                .add(new PendingEntity(x, y, z, id));
            pendingData.markDirty();
        }

        private void flush(int chunkX, int chunkZ) {
            // Journaled: queued writes land when their chunk loads (applyPendingOverflow), never mid-population.
            if (journal != null) return;
            Chunk chunk = loadedChunk(chunkX, chunkZ);
            if (chunk == null)
                throw new IllegalStateException("Cannot populate an unloaded chunk " + chunkX + "," + chunkZ);
            long chunkKey = horizontalKey(chunkX, chunkZ);
            LinkedHashMap<Long, PendingPlacement> queued = pendingData.pendingPlacements.get(chunkKey);
            List<PendingEntity> entities = pendingData.pendingEntities.get(chunkKey);
            validatePendingPhysicalCoordinates(queued, entities);
            if (queued != null) pendingData.pendingPlacements.remove(chunkKey);
            if (entities != null) pendingData.pendingEntities.remove(chunkKey);
            if (queued != null) {
                for (PendingPlacement placement : queued.values())
                    placeInChunk(world, chunk, placement.x(), placement.y(), placement.z(), placement.block());
            }
            if (entities != null) {
                for (PendingEntity entity : entities)
                    spawnEntityNow(world, entity.x(), entity.y(), entity.z(), entity.id());
            }
            if (queued != null || entities != null) pendingData.markDirty();
        }

        private void validatePendingPhysicalCoordinates(LinkedHashMap<Long, PendingPlacement> queued,
            List<PendingEntity> entities) {
            if (queued != null) for (PendingPlacement placement : queued.values())
                if (!heightTransport.containsPhysicalY(placement.y())) throw new IllegalStateException(
                    "Pending Origen block is outside the selected layout: " + placement
                        .x() + "," + placement.y() + "," + placement.z());
            if (entities != null) for (PendingEntity entity : entities)
                if (!heightTransport.containsPhysicalY(entity.y())) throw new IllegalStateException(
                    "Pending Origen entity is outside the selected layout: " + entity
                        .x() + "," + entity.y() + "," + entity.z());
        }

        private byte[] createRawTerrain(int chunkX, int chunkZ) {
            return rawTerrain(chunkX, chunkZ, rawTerrainLookup);
        }

        private void finish() {
            ObjectIterator<Long2ObjectMap.Entry<PopulationChunkChanges>> iterator = modifiedChunks.long2ObjectEntrySet()
                .fastIterator();
            while (iterator.hasNext()) {
                Long2ObjectMap.Entry<PopulationChunkChanges> entry = iterator.next();
                long chunkKey = entry.getLongKey();
                int chunkX = (int) (chunkKey >> 32);
                int chunkZ = (int) chunkKey;
                Chunk chunk = loadedChunk(chunkX, chunkZ);
                if (chunk == null) continue;
                if (entry.getValue()
                    .lightingChanged()) {
                    chunk.generateSkylightMap();
                }
                if (world instanceof WorldServer serverWorld) {
                    PlayerManager manager = serverWorld.getPlayerManager();
                    if (manager != null) notifyWatchingClients(manager, chunkX, chunkZ, entry.getValue());
                }
            }
            if (!heightTransport.compressed()) {
                ObjectIterator<Long2ObjectMap.Entry<OrigenPopulationNotifications>> shifted = shiftedModifiedChunks
                    .long2ObjectEntrySet()
                    .fastIterator();
                while (shifted.hasNext()) {
                    Long2ObjectMap.Entry<OrigenPopulationNotifications> entry = shifted.next();
                    long chunkKey = entry.getLongKey();
                    int chunkX = (int) (chunkKey >> 32), chunkZ = (int) chunkKey;
                    Chunk chunk = loadedChunk(chunkX, chunkZ);
                    if (chunk == null) continue;
                    if (entry.getValue()
                        .lightingChanged()) chunk.generateSkylightMap();
                    if (world instanceof WorldServer serverWorld) {
                        PlayerManager manager = serverWorld.getPlayerManager();
                        if (manager != null) entry.getValue()
                            .deliver(serverWorld, manager, chunkX, chunkZ);
                    }
                }
            }
        }
    }

    /**
     * Records only enough positions to make Forge choose the correct update packet. Once a population pass changes
     * {@link ForgeModContainer#clumpingThreshold} distinct blocks, Forge sends complete affected 16-high sections;
     * retaining every later tree leaf or terrain voxel would merely allocate and linearly scan a huge short array.
     */
    static final class PopulationChunkChanges {

        private final int clumpingThreshold;
        private short[] retained;
        private int retainedCount;
        private boolean sectionRefresh;
        private int sectionMask;
        private short[] tileEntities = new short[2];
        private int tileEntityCount;
        private boolean lightingChanged;

        PopulationChunkChanges(int clumpingThreshold) {
            if (clumpingThreshold < 1) throw new IllegalArgumentException("Clumping threshold must be positive");
            this.clumpingThreshold = clumpingThreshold;
            retained = new short[Math.min(64, clumpingThreshold)];
        }

        void record(int x, int y, int z, boolean tileEntity, boolean blockLightingChanged) {
            short packed = packLocalPosition(x, y, z);
            sectionMask |= 1 << (y >> 4);
            lightingChanged |= blockLightingChanged;
            if (tileEntity) tileEntityCount = addUnique(tileEntities, tileEntityCount, packed);
            if (sectionRefresh || contains(retained, retainedCount, packed)) return;
            if (retainedCount == retained.length) retained = grow(retained, clumpingThreshold);
            retained[retainedCount++] = packed;
            sectionRefresh = retainedCount >= clumpingThreshold;
        }

        boolean requiresSectionRefresh() {
            return sectionRefresh;
        }

        int sectionMask() {
            return sectionMask;
        }

        int retainedCount() {
            return retainedCount;
        }

        short retainedAt(int index) {
            return retained[index];
        }

        int tileEntityCount() {
            return tileEntityCount;
        }

        boolean lightingChanged() {
            return lightingChanged;
        }

        short tileEntityAt(int index) {
            return tileEntities[index];
        }

        private int addUnique(short[] values, int count, short value) {
            if (contains(values, count, value)) return count;
            if (count == values.length) {
                short[] grown = grow(values, 65_536);
                tileEntities = grown;
                values = grown;
            }
            values[count] = value;
            return count + 1;
        }

        private static short[] grow(short[] values, int maximum) {
            int next = Math.min(maximum, Math.max(values.length + 1, values.length << 1));
            short[] grown = new short[next];
            System.arraycopy(values, 0, grown, 0, values.length);
            return grown;
        }

        private static boolean contains(short[] values, int count, short target) {
            for (int index = 0; index < count; index++) {
                if (values[index] == target) return true;
            }
            return false;
        }
    }

    private static void notifyWatchingClients(PlayerManager manager, int chunkX, int chunkZ,
        PopulationChunkChanges changes) {
        int representedSections = 0;
        for (int index = 0; index < changes.retainedCount(); index++) {
            short packed = changes.retainedAt(index);
            representedSections |= 1 << (localY(packed) >> 4);
            markClientChange(manager, chunkX, chunkZ, packed);
        }
        if (!changes.requiresSectionRefresh()) return;

        // Later writes may touch a section absent from the threshold-sized retained prefix. One marker is enough to
        // add that section to S21PacketChunkData's bit mask; the packet still remains bounded to threshold + 16.
        int missingSections = changes.sectionMask() & ~representedSections;
        for (int section = 0; section < 16; section++) {
            if ((missingSections & 1 << section) != 0) {
                markClientChange(manager, chunkX, chunkZ, packLocalPosition(0, section << 4, 0));
            }
        }
        // Full section packets do not carry tile-entity description packets in Forge 1.7. Keep their real positions
        // in PlayerInstance's bounded change list so its follow-up pass sends the required NBT.
        for (int index = 0; index < changes.tileEntityCount(); index++) {
            short packed = changes.tileEntityAt(index);
            if (!containsRetained(changes, packed)) markClientChange(manager, chunkX, chunkZ, packed);
        }
    }

    private static boolean containsRetained(PopulationChunkChanges changes, short packed) {
        for (int index = 0; index < changes.retainedCount(); index++) {
            if (changes.retainedAt(index) == packed) return true;
        }
        return false;
    }

    private static void markClientChange(PlayerManager manager, int chunkX, int chunkZ, short packed) {
        manager.markBlockForUpdate((chunkX << 4) + localX(packed), localY(packed), (chunkZ << 4) + localZ(packed));
    }

    static short packLocalPosition(int x, int y, int z) {
        if (y < 0 || y >= 256) throw new IllegalArgumentException(
            "Legacy population record requires Y0..255; use full-coordinate/section delivery for upper edits");
        return (short) ((x & 15) << 12 | (z & 15) << 8 | y & 255);
    }

    static int localX(short packed) {
        return packed >> 12 & 15;
    }

    static int localY(short packed) {
        return packed & 255;
    }

    static int localZ(short packed) {
        return packed >> 8 & 15;
    }

    static long blockKey(int x, int y, int z) {
        return (x & 0x3FF_FFFFL) << 38 | (z & 0x3FF_FFFFL) << 12 | y & 0xFFFL;
    }

    static int blockKeyX(long key) {
        return (int) (key >> 38);
    }

    static int blockKeyY(long key) {
        int y = (int) (key & 0xFFFL);
        return y >= 0x800 ? y - 0x1000 : y;
    }

    static int blockKeyZ(long key) {
        return (int) (key << 26 >> 38);
    }

    private static String canonicalState(OrigenBlockStateResolver.ResolvedBlock resolved) {
        return resolved.canonicalId() + properties(resolved.properties());
    }

    /**
     * The state support checks judge a write by. One restored from the world (a displaced write taking its cell back)
     * is named by its block, and is judged as that block standing there is: by its name it stood on any ground.
     */
    private static String supportState(OrigenBlockStateResolver.ResolvedBlock resolved) {
        return OrigenBlockStateResolver.isPlaced(resolved.canonicalId())
            ? industries.angelic.biomic.world.modern.ModernStateCompatibility
                .modernState(resolved.block(), resolved.metadata())
            : canonicalState(resolved);
    }

    static record PendingBlock(String state, boolean physics, String spawnerEntity, NBTTagCompound chestNbt) {

        PendingBlock(String state, boolean physics) {
            this(state, physics, null, null);
        }

        PendingBlock(String state, boolean physics, String spawnerEntity) {
            this(state, physics, spawnerEntity, null);
        }

        PendingBlock {
            if (state == null || state.isEmpty()) throw new IllegalArgumentException("Empty pending block state");
            if (spawnerEntity != null
                && (!"minecraft:spawner".equals(state) || !"BiomesOPlenty.Wasp".equals(spawnerEntity)))
                throw new IllegalArgumentException(
                    "Unsupported pending mob-spawner payload: " + state + " / " + spawnerEntity);
            if (spawnerEntity != null && chestNbt != null)
                throw new IllegalArgumentException("Multiple pending tile payloads");
            if (chestNbt != null) chestNbt = validatedChestCopy(
                state,
                chestNbt,
                chestNbt.getInteger("x"),
                chestNbt.getInteger("y"),
                chestNbt.getInteger("z"));
        }

        @Override
        public NBTTagCompound chestNbt() {
            return chestNbt == null ? null : (NBTTagCompound) chestNbt.copy();
        }
    }

    static PendingBlock readPendingBlock(NBTTagCompound entry) {
        String payload = entry.hasKey("spawner_entity") ? entry.getString("spawner_entity") : null;
        NBTTagCompound chest = entry.hasKey("chest_nbt") ? entry.getCompoundTag("chest_nbt") : null;
        return new PendingBlock(entry.getString("state"), entry.getBoolean("physics"), payload, chest);
    }

    static void writePendingBlock(NBTTagCompound entry, PendingBlock block) {
        entry.setString("state", block.state());
        entry.setBoolean("physics", block.physics());
        if (block.spawnerEntity() != null) entry.setString("spawner_entity", block.spawnerEntity());
        if (block.chestNbt() != null) entry.setTag("chest_nbt", block.chestNbt());
    }

    static void validateChestPayload(String state, NBTTagCompound payload, int x, int y, int z) {
        validatedChestCopy(state, payload, x, y, z);
    }

    /** EFR's bee nest: the one other tile payload, its bees (a plant layer nest). */
    static final String BEE_NEST = "etfuturum:bee_nest[";

    private static NBTTagCompound validatedChestCopy(String state, NBTTagCompound payload, int x, int y, int z) {
        if (payload == null) throw new IllegalArgumentException("Missing pending chest payload");
        if (state.startsWith(BEE_NEST)) {
            NBTTagCompound copy = (NBTTagCompound) payload.copy();
            if (copy.getInteger("x") != x || copy.getInteger("y") != y
                || copy.getInteger("z") != z
                || !copy.hasKey("Bees", Constants.NBT.TAG_LIST))
                throw new IllegalArgumentException("Bee nest payload does not match " + x + "," + y + "," + z);
            return copy;
        }
        if (!("minecraft:chest".equals(state) || state.startsWith("minecraft:chest[")))
            throw new IllegalArgumentException("Chest payload targets unsupported block: " + state);
        NBTTagCompound copy = (NBTTagCompound) payload.copy();
        if (!"Chest".equals(copy.getString("id")) || copy.getInteger("x") != x
            || copy.getInteger("y") != y
            || copy.getInteger("z") != z)
            throw new IllegalArgumentException(
                "Chest payload identity/coordinates do not match " + x + "," + y + "," + z);
        NBTTagList items = copy.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        boolean[] slots = new boolean[27];
        for (int index = 0; index < items.tagCount(); index++) {
            NBTTagCompound item = items.getCompoundTagAt(index);
            int slot = item.getByte("Slot") & 255;
            if (slot >= slots.length || slots[slot])
                throw new IllegalArgumentException("Invalid/duplicate chest slot " + slot);
            slots[slot] = true;
        }
        return copy;
    }

    /** {@code writer}: the populating chunk's key for a journaled neighbour's write, else NO_WRITER. */
    private record PendingPlacement(int x, int y, int z, PendingBlock block, long writer) {

        static final long NO_WRITER = Long.MIN_VALUE;

        PendingPlacement(int x, int y, int z, PendingBlock block) {
            this(x, y, z, block, NO_WRITER);
        }
    }

    private record PendingEntity(int x, int y, int z, String id) {}

    private record WeightedStructure(String id, int maximumExclusive, Structure structure) {}

    private record ProfileRange(int minimumY, int maximumY, OrigenBiomeProfile profile) {}

    private record FeatureReplay(int x, int z, ProfileRange range, FeatureOrigin originalOrigin) {}

    private record AuthoredFeatureBlock(WorldBlock block, FeatureOrigin origin, String featureId,
        FeatureOrigin dependencyOrigin) {}

    /** Sparse paged view of one authored 384-high block column. Disjoint cave and surface writes stay compact. */
    private static final class AuthoredFeatureColumn {

        private static final int HEIGHT = OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE
            - OrigenDensityField.UPSTREAM_MIN_Y;
        private static final int PAGE_SHIFT = 5;
        private static final int PAGE_SIZE = 1 << PAGE_SHIFT;
        private static final int PAGE_MASK = PAGE_SIZE - 1;
        private static final int PAGE_COUNT = (HEIGHT + PAGE_MASK) >>> PAGE_SHIFT;

        private AuthoredFeatureBlock[][] pages;

        private AuthoredFeatureBlock get(int index) {
            if (pages == null || index < 0 || index >= HEIGHT) return null;
            AuthoredFeatureBlock[] page = pages[index >>> PAGE_SHIFT];
            return page == null ? null : page[index & PAGE_MASK];
        }

        private AuthoredFeatureBlock set(int index, AuthoredFeatureBlock block) {
            if (index < 0 || index >= HEIGHT) return null;
            int pageIndex = index >>> PAGE_SHIFT;
            if (block == null) {
                if (pages == null || pages[pageIndex] == null) return null;
                AuthoredFeatureBlock previous = pages[pageIndex][index & PAGE_MASK];
                pages[pageIndex][index & PAGE_MASK] = null;
                return previous;
            }
            if (pages == null) pages = new AuthoredFeatureBlock[PAGE_COUNT][];
            AuthoredFeatureBlock[] page = pages[pageIndex];
            if (page == null) {
                page = new AuthoredFeatureBlock[Math.min(PAGE_SIZE, HEIGHT - pageIndex * PAGE_SIZE)];
                pages[pageIndex] = page;
            }
            int localIndex = index & PAGE_MASK;
            AuthoredFeatureBlock previous = page[localIndex];
            page[localIndex] = block;
            return previous;
        }
    }

    private static final class BlockColumnRef {

        private byte[] terrain;
        private int terrainOffset;
        private AuthoredFeatureColumn features;
    }

    private record AuthoredTreeStructure(String featureId, int anchorAuthoredY, List<Long> blocks) {}

    private record AuthoredDependentStructure(FeatureOrigin origin, String featureId, List<Long> blocks) {}

    private record RejectedMushroomPlacement(int x, int authoredY, int z, OrigenBlockStateResolver.ResolvedBlock block,
        boolean physics) {}

    private record SuppressedTree(FeatureOrigin origin, AuthoredTreeStructure tree) {}

    /** Total order used by the Paper oracle: chunk Z/X, then stage, tile X/Z, range, sub-column X/Z and feature. */
    private record FeatureOrigin(int chunkZ, int chunkX, int stage, int tileX, int tileZ, int rangeMinimum, int subX,
        int subZ, int feature) implements Comparable<FeatureOrigin> {

        private static FeatureOrigin at(int x, int z, int stage, int rangeMinimum, int feature) {
            int localX = x & 15;
            int localZ = z & 15;
            return new FeatureOrigin(
                z >> 4,
                x >> 4,
                stage,
                localX / STAGE_RESOLUTION,
                localZ / STAGE_RESOLUTION,
                rangeMinimum,
                localX % STAGE_RESOLUTION,
                localZ % STAGE_RESOLUTION,
                feature);
        }

        private int x() {
            return chunkX * 16 + tileX * STAGE_RESOLUTION + subX;
        }

        private int z() {
            return chunkZ * 16 + tileZ * STAGE_RESOLUTION + subZ;
        }

        @Override
        public int compareTo(FeatureOrigin other) {
            int compared = Integer.compare(chunkZ, other.chunkZ);
            if (compared != 0) return compared;
            compared = Integer.compare(chunkX, other.chunkX);
            if (compared != 0) return compared;
            compared = Integer.compare(stage, other.stage);
            if (compared != 0) return compared;
            compared = Integer.compare(tileX, other.tileX);
            if (compared != 0) return compared;
            compared = Integer.compare(tileZ, other.tileZ);
            if (compared != 0) return compared;
            compared = Integer.compare(rangeMinimum, other.rangeMinimum);
            if (compared != 0) return compared;
            compared = Integer.compare(subX, other.subX);
            if (compared != 0) return compared;
            compared = Integer.compare(subZ, other.subZ);
            return compared != 0 ? compared : Integer.compare(feature, other.feature);
        }
    }

    static final class WorldBlockSnapshotReuse {

        private WorldBlock last;

        WorldBlock snapshot(Block block, int metadata) {
            WorldBlock known = last;
            if (known != null && known.block() == block && known.metadata() == metadata) return known;
            last = new WorldBlock(block, metadata);
            return last;
        }
    }

    record WorldBlock(Block block, int metadata) {

        private static final WorldBlock AIR = new WorldBlock(net.minecraft.init.Blocks.air, 0);
    }

    private record IntRange(int minimum, int maximum) {

        private IntRange {
            if (minimum >= maximum)
                throw new IllegalArgumentException("Invalid Origen range " + minimum + ".." + maximum);
        }

        private int get(Random random) {
            return minimum + random.nextInt(maximum - minimum);
        }
    }

    private static final class FeatureProfile {

        private long distributorHits;
        private long anchors;
        private long totalNanos;
        private long locatorNanos;
        private long structureNanos;
    }

    /**
     * Durable edge buffer for Terra structures which extend into an unloaded neighboring chunk.
     *
     * <p>
     * Terra normally writes through a bounded generation region. Forge 1.7 exposes either a loaded Chunk or a World
     * accessor that recursively generates missing chunks, so deferred writes are the safe equivalent. Persisting them
     * prevents a save/restart between the two chunk populations from shearing vines, trees, arches, or plateau pillars
     * at chunk boundaries.
     * </p>
     */
    public static final class PendingFeatureData extends WorldSavedData {

        private static final String DATA_NAME = "biomic_origen_feature_edges_v1";
        private static final String PLACEMENTS_KEY = "placements";
        private static final String ENTITIES_KEY = "entities";

        // Physical reads probe this outer index for every block. Keep dispatch primitive;
        // the inner insertion-ordered map still controls deferred write/flush order.
        private final Long2ObjectOpenHashMap<LinkedHashMap<Long, PendingPlacement>> pendingPlacements = new Long2ObjectOpenHashMap<>();
        private final Map<Long, List<PendingEntity>> pendingEntities = new HashMap<>();
        private final HostedTreeLedger hostedTrees = new HostedTreeLedger();
        /** Journaled populations' writes into chunks that were not loaded: every contender for a cell, not a winner. */
        private final Long2ObjectOpenHashMap<List<PendingPlacement>> journalOverflow = new Long2ObjectOpenHashMap<>();

        public PendingFeatureData(String mapName) {
            super(mapName);
        }

        private static PendingFeatureData load(World world) {
            MapStorage storage = world.mapStorage;
            String name = DATA_NAME;
            PendingFeatureData data = (PendingFeatureData) storage.loadData(PendingFeatureData.class, name);
            if (data == null) {
                data = new PendingFeatureData(name);
                storage.setData(name, data);
            }
            return data;
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            pendingPlacements.clear();
            pendingEntities.clear();
            journalOverflow.clear();
            hostedTrees.load(nbt.getCompoundTag("hosted_trees_v1"));
            NBTTagList overflow = nbt.getTagList("journal_overflow", Constants.NBT.TAG_COMPOUND);
            for (int index = 0; index < overflow.tagCount(); index++) {
                NBTTagCompound entry = overflow.getCompoundTagAt(index);
                int x = entry.getInteger("x"), z = entry.getInteger("z");
                journalOverflow.computeIfAbsent(horizontalKey(x >> 4, z >> 4), ignored -> new ArrayList<>())
                    .add(
                        new PendingPlacement(
                            x,
                            entry.getInteger("y"),
                            z,
                            readPendingBlock(entry),
                            entry.getLong("writer")));
            }
            NBTTagList placements = nbt.getTagList(PLACEMENTS_KEY, Constants.NBT.TAG_COMPOUND);
            for (int index = 0; index < placements.tagCount(); index++) {
                NBTTagCompound entry = placements.getCompoundTagAt(index);
                int x = entry.getInteger("x");
                int y = entry.getInteger("y");
                int z = entry.getInteger("z");
                PendingPlacement placement = new PendingPlacement(x, y, z, readPendingBlock(entry));
                pendingPlacements.computeIfAbsent(horizontalKey(x >> 4, z >> 4), ignored -> new LinkedHashMap<>())
                    .put(blockKey(x, y, z), placement);
            }
            NBTTagList entities = nbt.getTagList(ENTITIES_KEY, Constants.NBT.TAG_COMPOUND);
            for (int index = 0; index < entities.tagCount(); index++) {
                NBTTagCompound entry = entities.getCompoundTagAt(index);
                PendingEntity entity = new PendingEntity(
                    entry.getInteger("x"),
                    entry.getInteger("y"),
                    entry.getInteger("z"),
                    entry.getString("id"));
                pendingEntities
                    .computeIfAbsent(horizontalKey(entity.x() >> 4, entity.z() >> 4), ignored -> new ArrayList<>())
                    .add(entity);
            }
        }

        @Override
        public void writeToNBT(NBTTagCompound nbt) {
            nbt.setTag("hosted_trees_v1", hostedTrees.save());
            NBTTagList placements = new NBTTagList();
            for (LinkedHashMap<Long, PendingPlacement> chunkPlacements : pendingPlacements.values()) {
                for (PendingPlacement placement : chunkPlacements.values()) {
                    NBTTagCompound entry = new NBTTagCompound();
                    entry.setInteger("x", placement.x());
                    entry.setInteger("y", placement.y());
                    entry.setInteger("z", placement.z());
                    writePendingBlock(entry, placement.block());
                    placements.appendTag(entry);
                }
            }
            nbt.setTag(PLACEMENTS_KEY, placements);
            NBTTagList overflow = new NBTTagList();
            for (List<PendingPlacement> chunkOverflow : journalOverflow.values()) {
                for (PendingPlacement placement : chunkOverflow) {
                    NBTTagCompound entry = new NBTTagCompound();
                    entry.setInteger("x", placement.x());
                    entry.setInteger("y", placement.y());
                    entry.setInteger("z", placement.z());
                    entry.setLong("writer", placement.writer());
                    writePendingBlock(entry, placement.block());
                    overflow.appendTag(entry);
                }
            }
            nbt.setTag("journal_overflow", overflow);

            NBTTagList entities = new NBTTagList();
            for (List<PendingEntity> chunkEntities : pendingEntities.values()) {
                for (PendingEntity entity : chunkEntities) {
                    NBTTagCompound entry = new NBTTagCompound();
                    entry.setInteger("x", entity.x());
                    entry.setInteger("y", entity.y());
                    entry.setInteger("z", entity.z());
                    entry.setString("id", entity.id());
                    entities.appendTag(entry);
                }
            }
            nbt.setTag(ENTITIES_KEY, entities);
        }
    }
}
