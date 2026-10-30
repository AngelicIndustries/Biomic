package industries.angelic.biomic.world.terrain;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import com.google.gson.JsonPrimitive;

import industries.angelic.biomic.compat.biome.BopNativeBiomes;
import industries.angelic.biomic.compat.biome.BopSurfaceCapture;
import industries.angelic.biomic.compat.flora.HarvestCraftOrchards;
import industries.angelic.biomic.compat.flora.NativeFeatureCapture;
import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.feature.RuDecorationStateResolver.Resolved;
import industries.angelic.biomic.world.modern.ModernAquaticPlantSupport;
import industries.angelic.biomic.world.modern.ModernBlockSemantics;
import industries.angelic.biomic.world.modern.ModernDecorationEngine;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationGraph;
import industries.angelic.biomic.world.modern.ModernFeatureEngine;
import industries.angelic.biomic.world.modern.ModernStateCompatibility;
import industries.angelic.biomic.world.modern.ModernSurfaceSystem;
import industries.angelic.biomic.world.modern.noise.ModernLegacyRandom;
import industries.angelic.biomic.world.modern.noise.ModernWorldgenRandom;

/** Native-height RU surface decoration hosted on existing Origen-compatible floating terrain. */
public final class HostedIslandRuntime {

    private static final int[] DECORATION_STEPS = { 2, 9, 10 };
    private static final int ISLAND_CHUNK_CACHE_SIZE = 192;
    private final long seed;
    private final boolean fullNativeMarsh;
    private final industries.angelic.biomic.compat.biome.BopHeightSchedule nativeBopHeights;
    private final NativeFeatureEnvelope nativeFeatureEnvelope;
    private final industries.angelic.biomic.compat.biome.SaltMarshHeight saltMarshHeight;
    private final HostedTerrainAccess terrain;
    private final IntrinsicHostedTerrainAccess intrinsicTerrain;
    private final OrigenFeatureRuntime.RawTerrainLookup rawTerrainLookup;
    private final RegionalHabitatSampler mainland;
    private final HostedIslandSurfacePalette palette;
    private final ModernDecorationGraph graph = new ModernDecorationGraph();
    private final ModernFeatureEngine features = new ModernFeatureEngine();
    private final ModernDecorationEngine engine;
    private final LongObjectAccessLru<IslandChunk> chunks = new LongObjectAccessLru<>(ISLAND_CHUNK_CACHE_SIZE);
    private long placed, rejected, chunkCacheHits, chunkPayloadBorrows, chunkFallbackComputations;
    private final boolean largeMushrooms = industries.angelic.biomic.config.VegetationConfig.blackwoodLargeMushrooms;
    private final boolean smallMushrooms = industries.angelic.biomic.config.VegetationConfig.blackwoodSmallMushrooms;
    private final boolean shroomlights = industries.angelic.biomic.config.VegetationConfig.blackwoodShroomlights;

    public HostedIslandRuntime(WorldgenContext context, BiomicTerrainSampler terrain, boolean ground,
        OrigenFeatureRuntime.RawTerrainLookup rawTerrainLookup) {
        this(context, (HostedTerrainAccess) terrain, ground, rawTerrainLookup);
    }

    public HostedIslandRuntime(WorldgenContext context, HostedTerrainAccess terrain, boolean ground,
        OrigenFeatureRuntime.RawTerrainLookup rawTerrainLookup) {
        if (context == null || terrain == null || terrain.layout() == null || terrain.projection() == null) {
            throw new IllegalArgumentException("Hosted terrain context, layout and projection are required");
        }
        if (!terrain.projection()
            .supportsStorageHeight(
                terrain.layout()
                    .height())) {
            throw new IllegalArgumentException("Hosted terrain projection disagrees with physical storage");
        }
        boolean legacy = terrain.layout() == industries.angelic.biomic.api.ChunkVerticalLayout.LEGACY_256
            && terrain.projection() == AuthoredVerticalProjections.origenCompressed256();
        boolean shifted = terrain.layout() == industries.angelic.biomic.api.ChunkVerticalLayout.EXTENDED_512
            && terrain.projection() == AuthoredVerticalProjections.shifted512();
        if (!legacy && !shifted) {
            throw new IllegalArgumentException(
                "Hosted terrain requires the canonical legacy or shifted layout/projection pair");
        }
        if (terrain.minimumEcologyY() != (shifted ? terrain.projection()
            .physicalMinY() : 0)) {
            throw new IllegalArgumentException("Hosted terrain ecology floor disagrees with its projection");
        }
        boolean nativeSelected = context.getNativeHabitats()
            .bop()
            || context.getNativeHabitats()
                .saltMarsh();
        if (context.getNativeHabitats()
            .shifted512() && !shifted)
            throw new IllegalStateException("Shifted native habitat manifest requires shifted-512 terrain");
        if (nativeSelected && shifted != context.getNativeHabitats()
            .shifted512())
            throw new IllegalStateException("Saved native habitat height revision disagrees with hosted terrain");
        this.seed = context.getSettings()
            .getWorldSeed();
        this.fullNativeMarsh = context.getNativeHabitats()
            .fullMarsh();
        this.nativeBopHeights = industries.angelic.biomic.compat.biome.BopHeightSchedule
            .forProjection(terrain.projection());
        this.nativeFeatureEnvelope = NativeFeatureEnvelope.forTerrain(terrain);
        this.saltMarshHeight = industries.angelic.biomic.compat.biome.SaltMarshHeight
            .forProjection(terrain.projection());
        this.terrain = terrain;
        this.intrinsicTerrain = terrain instanceof IntrinsicHostedTerrainAccess intrinsic ? intrinsic : null;
        this.rawTerrainLookup = rawTerrainLookup;
        this.mainland = ground ? new RegionalHabitatSampler(context) : null;
        this.palette = terrain.projection() == AuthoredVerticalProjections.shifted512()
            ? new HostedIslandSurfacePalette(seed, terrain::sampleCarvedGroundSurfaceHeight, terrain.projection())
            : new HostedIslandSurfacePalette(seed, terrain::sampleCarvedGroundSurfaceHeight);
        this.engine = new ModernDecorationEngine(seed, (engine, type, config, world, random, pos) -> {
            IslandWorld view = (IslandWorld) world;
            if ("minecraft:freeze_top_layer".equals(type)) return finishWinterColumns(view, pos);
            com.google.gson.JsonObject selected = HostedVegetationPolicy.configured(
                view.biome(pos.x, pos.y, pos.z),
                type,
                config,
                largeMushrooms,
                smallMushrooms,
                shroomlights);
            if (selected == null) return false;
            HostedPositionMap writes = new HostedPositionMap();
            view.transactions.push(writes);
            view.transactionHabitats.push(view.biome(pos.x, pos.y, pos.z));
            boolean success = false;
            boolean completed = false;
            try {
                if (view.appleOrchards
                    && HarvestCraftOrchards.replaces(view.biome(pos.x, pos.y, pos.z), type, selected)) {
                    // One provider consumes this tree slot, not a second population pass. Other placed
                    // features retain their own original feature seeds. Native tree RNG is separately salted.
                    success = HarvestCraftOrchards.place(
                        view.sourceWorld,
                        view.nativeAccess(),
                        new java.util.Random(random.nextLong() ^ 0x70616d6170706c65L),
                        pos.x,
                        pos.y,
                        pos.z);
                } else {
                    com.google.gson.JsonObject fallback = HarvestCraftOrchards
                        .replaces(view.biome(pos.x, pos.y, pos.z), type, selected)
                            ? HarvestCraftOrchards.oakFallback(selected)
                            : selected;
                    success = features.place(engine, type, fallback, world, random, pos);
                }
                if (success && "minecraft:forest_rock".equals(type)
                    && !HostedRockAttachment.attached(writes.keySet(), p -> {
                        return terrain.layout()
                            .containsY(p.y)
                            && HostedRockAttachment.solidTerrain(view.physical.rawMaterial(p.x, p.y, p.z));
                    })) {
                    // The source's later blobs drift from the initial supported origin. On cliffs
                    // they can become disconnected single blocks. Reject before any physical write;
                    // keep source parity mode and unrelated trees/rock pillars unchanged.
                    success = false;
                    throw new OutsideFeature();
                }
                completed = true;
            } catch (OutsideFeature ignored) {
                // Reject the entire configured feature, never keep its in-bounds half.
                success = false;
                rejected++;
            } finally {
                if (view.traced)
                    view.trace(pos, "configured=" + type + " success=" + success + " buffered=" + writes.size());
                view.transactions.pop();
                view.transactionHabitats.pop();
            }
            if (HostedVegetationPolicy.keepsConfiguredWrites(type, completed, success)) {
                if (!view.transactions.isEmpty()) view.transactions.peek()
                    .putAll(writes);
                else {
                    if (view.commit(writes, false)) placed++;
                }
            }
            return success;
        });
    }

    /**
     * Habitats that are snowy anyway: RU's cold ones, or those cold enough to snow at the sea's level (vanilla's snowy
     * plains). Only these freeze and snow: high up every habitat is cold enough, and islands would all be snowed on.
     */
    private boolean snowyHabitat(String habitat, int x, int z) {
        if (HostedVegetationPolicy.isColdExpansion(habitat)) return true;
        return ModernSurfaceSystem.coldEnoughToSnow(habitat, x, SNOWY_TEST_Y, z);
    }

    /** Below the height where cold starts to grow with altitude. */
    private static final int SNOWY_TEST_Y = 63;

    /** Snow is independent per column, unlike whole trees: a biome border must not reject a 16x16 snow sheet. */
    private boolean finishWinterColumns(IslandWorld view, Pos origin) {
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = origin.x + dx, z = origin.z + dz;
            int y = view.height("MOTION_BLOCKING", x, z);
            if (y <= terrain.minimumEcologyY() || !terrain.layout()
                .containsY(y)) continue;
            String habitat = view.biome(x, y, z);
            if (!snowyHabitat(habitat, x, z)) continue;
            HostedPositionMap writes = new HostedPositionMap();
            view.transactions.push(writes);
            view.transactionHabitats.push(habitat);
            boolean success = false;
            try {
                if (view.shouldFreeze(x, y - 1, z, false)) view.setBlock(x, y - 1, z, "minecraft:ice");
                if (view.shouldSnow(x, y, z)) {
                    view.setBlock(x, y, z, "minecraft:snow[layers=1]");
                    String below = view.blockState(x, y - 1, z);
                    if (below.contains("snowy=false"))
                        view.setBlock(x, y - 1, z, below.replace("snowy=false", "snowy=true"));
                }
                success = true;
            } catch (OutsideFeature ignored) {
                rejected++;
            } finally {
                view.transactions.pop();
                view.transactionHabitats.pop();
            }
            if (success) {
                view.commit(writes, false);
            }
        }
        return true;
    }

    /** Remap admitted soil skins only; retain terrain geometry and geological cross-sections. */
    public void surface(int chunkX, int chunkZ, byte[] materials, Block[] blocks, byte[] metadata) {
        // A chunk's sites are a function of its terrain: one prepared from the same terrain serves again.
        IslandChunk chunk;
        synchronized (chunks) {
            chunk = chunks.get(key(chunkX, chunkZ));
        }
        if (chunk == null || chunk.materials != materials && !java.util.Arrays.equals(chunk.materials, materials)) {
            chunk = new IslandChunk(chunkX, chunkZ, materials);
            synchronized (chunks) {
                chunks.put(key(chunkX, chunkZ), chunk);
            }
        }
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            for (Site site : chunk.sites[z * 16 + x]) {
                String skin = skinHabitat(site, (chunkX << 4) + x, (chunkZ << 4) + z);
                if (skin == null) continue;
                for (int depth = 0; depth < site.soilLayers; depth++) {
                    Resolved state = ModernStateCompatibility
                        .resolve(soilState(site, skin, depth, (chunkX << 4) + x, (chunkZ << 4) + z));
                    int index = terrain.layout()
                        .index(x, site.surface.floorY - depth, z);
                    blocks[index] = state.getBlock();
                    metadata[index] = (byte) state.getMetadata();
                }
            }
        }
    }

    /** The mainland's habitat regions (null on an island runtime): what lies below the sky islands. */
    /** A ground column's habitat: its hosted habitat, else Origen's profile ({@code origen:NAME}). Mainland only. */
    public String surfaceHabitat(int x, int z) {
        String hosted = mainland.sample(x, z);
        return hosted != null ? hosted
            : "origen:" + mainland.foundationAt(x, z)
                .name();
    }

    public RegionalHabitatSampler groundHabitats() {
        return mainland;
    }

    /** The sky islands' shape: their spans, for what hangs under them, and their islets, for their trees. */
    public interface IslandShape {

        /** The island in a column, {@code bottom << 16 | top}, or -1. */
        int span(int x, int z);

        /** The islets centred in a chunk, {x, z, trees} (see FloatingIslandDensitySampler#islets). */
        List<int[]> islets(int chunkX, int chunkZ);
    }

    private IslandShape islands;
    private final Map<String, String> isletTrees = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Islands get their underside (IslandUnderside: vines, roots, lichen, dripstone, waterfalls), and islets one or two
     * of their habitat's trees: too small for its flora's chance to land one.
     */
    public void setIslands(IslandShape islands) {
        this.islands = islands;
    }

    /** Where a feature may not root (a village at its height); {@code bulky} for whole trees, boulders and the like. */
    public interface DecorationGuard {

        boolean blocks(String feature, boolean bulky, int x, int y, int z);

        /** Whether anything guarded lies within {@code reach} blocks (true when unknown: then every block is asked). */
        default boolean near(int x, int y, int z, int reach) {
            return true;
        }
    }

    /** Snow cover lies on villages and structures as on everything else. */
    public static boolean snowCover(String feature) {
        return "minecraft:freeze_top_layer".equals(feature) || feature != null && feature.endsWith("/winter");
    }

    private DecorationGuard villageGuard;

    /** Decoration roots nowhere the guard blocks (villages built on these habitats). */
    public void setVillageGuard(DecorationGuard guard) {
        villageGuard = guard;
    }

    private static boolean bulkyFeature(String feature) {
        return feature.contains("tree") || feature.contains("bamboo")
            || feature.contains("huge")
            || feature.contains("cactus")
            || feature.contains("boulder")
            || feature.contains("rock")
            || feature.contains("bush");
    }

    /** One column's highest ground surface: its floor, the fluid depth over it, and its hosted habitat. */
    public interface GroundSurface {

        void accept(int x, int z, int floorY, int fluidDepth, String habitat);
    }

    /**
     * Mainland only: every column's highest ground surface in the chunk, with the hosted habitat on it (null where
     * Origen owns the ground). Pure terrain: the same before and after population.
     */
    public void forEachGroundSurface(int chunkX, int chunkZ, GroundSurface consumer) {
        if (mainland == null) throw new IllegalStateException("Ground surfaces belong to the mainland runtime");
        IslandChunk chunk = chunk(chunkX, chunkZ);
        for (int i = 0; i < 256; i++) {
            if (chunk.groundFloor[i] < 0) continue;
            consumer.accept(
                (chunkX << 4) + (i & 15),
                (chunkZ << 4) + (i >> 4),
                chunk.groundFloor[i],
                chunk.groundFluid[i],
                chunk.groundHabitat[i]);
        }
    }

    /**
     * Sky only: every floating island surface in the chunk with room above it (floor, fluid depth, habitat), all
     * layers. Pure terrain: the same before and after population.
     */
    public void forEachIslandSurface(int chunkX, int chunkZ, GroundSurface consumer) {
        if (islands == null) throw new IllegalStateException("Island surfaces belong to the sky runtime");
        IslandChunk chunk = chunk(chunkX, chunkZ);
        for (int i = 0; i < 256; i++) for (Site site : chunk.sites[i]) {
            if (!site.surface.floating || site.surface.floorY + 3 >= site.surface.ceilingY) continue;
            consumer.accept(
                (chunkX << 4) + (i & 15),
                (chunkZ << 4) + (i >> 4),
                site.surface.floorY,
                site.surface.fluidDepth(),
                site.biome);
        }
    }

    public void populate(net.minecraft.world.World world, int chunkX, int chunkZ, OrigenFeatureRuntime origen) {
        boolean appleOrchards = HarvestCraftOrchards.available();
        Set<String> habitats = new LinkedHashSet<>();
        IslandChunk chunk = chunk(chunkX, chunkZ);
        for (List<Site> sites : chunk.sites) for (Site site : sites) habitats.add(site.biome);
        if (habitats.isEmpty()) return;
        Set<String> modernHabitats = new LinkedHashSet<>(habitats);
        modernHabitats.remove(industries.angelic.biomic.compat.biome.SaltMarshCompat.BIOME);
        modernHabitats.removeIf(BopNativeBiomes::handles);
        origen.withPhysicalPopulation(world, chunkX, chunkZ, physical -> {
            if (islands != null) decorateUndersides(physical, chunkX, chunkZ);
            int layers = 0;
            for (List<Site> sites : chunk.sites) layers = Math.max(layers, sites.size());
            for (int layer = 0; layer < layers; layer++) {
                IslandWorld view = new IslandWorld(physical, chunkX, chunkZ, layer);
                view.sourceWorld = world;
                view.appleOrchards = appleOrchards;
                ModernWorldgenRandom random = new ModernWorldgenRandom();
                long decorationSeed = random.setDecorationSeed(seed, chunkX << 4, chunkZ << 4);
                if (layer != 0) decorationSeed = WorldgenContext.mix64(decorationSeed ^ layer);
                for (int step : DECORATION_STEPS) {
                    for (String feature : graph.orderedFeatures(modernHabitats, step)) {
                        if (!HostedVegetationPolicy.hostsDecoration(step, feature)) continue;
                        if (MarineLayer.replaces(feature) || RemovedFeatures.removed(feature)) continue;
                        // Keep the source stage and global feature index: admitting rocks must not reseed trees.
                        random.setFeatureSeed(
                            decorationSeed,
                            graph.featureOrder(step)
                                .indexOf(feature),
                            step);
                        view.feature = feature;
                        Pos origin = new Pos(chunkX << 4, 0, chunkZ << 4);
                        if ("minecraft:freeze_top_layer".equals(feature)) {
                            // Its source placed feature tests biome membership at Y=0. Hosted biomes live
                            // above their own floors instead: the configured adapter checks EVERY column's
                            // actual exposed biome, including when the chunk origin is in another habitat.
                            engine.placeConfigured(feature, view, random, origin);
                        } else {
                            engine.place(new JsonPrimitive(feature), feature, view, random, origin);
                        }
                    }
                }
                view.feature = null;
                if (habitats.contains(industries.angelic.biomic.compat.biome.SaltMarshCompat.BIOME))
                    placeSaltMarshPlants(view, chunkX, chunkZ);
                for (String habitat : new java.util.TreeSet<>(habitats))
                    if (BopNativeBiomes.handles(habitat)) placeBopPlants(view, chunkX, chunkZ, habitat);
                if (habitats.stream()
                    .anyMatch(BopNativeBiomes::winter)) finishBopWinterColumns(view, chunkX, chunkZ);
                if (mainland != null && layer == 0) placeRefugeRoofTree(view, chunkX, chunkZ);
                if (islands != null) placeIsletTrees(view, chunkX, chunkZ);
            }
            if (mainland != null) placeTemperateCliffVines(physical, chunkX, chunkZ);
        });
    }

    /** Native vanilla/BOP finishing order (+8 offset), explicitly on this physical habitat layer. */
    private void finishBopWinterColumns(IslandWorld view, int cx, int cz) {
        var heat = new industries.angelic.biomic.compat.biome.BopWinterSurface.HeatBound(
            (x, y, z) -> view.block(x, y, z)
                .getLightValue(view.sourceWorld, x, y, z));
        var access = view.nativeAccess();
        for (int dx = 8; dx < 24; dx++) for (int dz = 8; dz < 24; dz++) {
            int x = cx * 16 + dx, z = cz * 16 + dz, y = view.height("MOTION_BLOCKING", x, z);
            if (y <= terrain.minimumEcologyY() || !terrain.layout()
                .containsY(y)) continue;
            String habitat = view.biome(x, y, z);
            if (!BopNativeBiomes.winter(habitat)) continue;
            var nativeBiome = BopNativeBiomes.binding(habitat).biome;
            // Recreate a per-column read budget, while sharing bounded emitter observations.
            var capture = new BopSurfaceCapture(access, new java.util.Random(0));
            HostedPositionMap writes = new HostedPositionMap();
            view.transactions.push(writes);
            view.transactionHabitats.push(habitat);
            view.feature = "biomic:native_bop/" + habitat + "/winter";
            boolean success = false;
            try {
                success = NativeFeatureCapture.run(view.sourceWorld, capture, () -> {
                    Block below = view.block(x, y - 1, z);
                    if (industries.angelic.biomic.compat.biome.BopWinterSurface.freeze(
                        nativeBiome.getFloatTemperature(x, y - 1, z),
                        heat.at(x, y - 1, z),
                        below,
                        access.metadata(x, y - 1, z))) {
                        view.setBlock(x, y - 1, z, "minecraft:ice");
                        below = net.minecraft.init.Blocks.ice;
                    }
                    if (industries.angelic.biomic.compat.biome.BopWinterSurface.snow(
                        nativeBiome.getFloatTemperature(x, y, z),
                        heat.at(x, y, z),
                        view.block(x, y, z),
                        below,
                        access.metadata(x, y - 1, z),
                        below.isLeaves(view.sourceWorld, x, y - 1, z))
                        && net.minecraft.init.Blocks.snow_layer.canPlaceBlockAt(view.sourceWorld, x, y, z))
                        view.setBlock(x, y, z, "minecraft:snow[layers=1]");
                    return true;
                });
            } catch (OutsideFeature ignored) {
                rejected++;
            } finally {
                view.transactions.pop();
                view.transactionHabitats.pop();
            }
            if (success) {
                view.commit(writes, false);
            }
        }
    }

    /** Native BOP surface generators, buffered as complete bounded features. */
    public static java.util.Random nativeBopRandom(long seed, int cx, int cz, String habitat, String slot, int layer) {
        return new java.util.Random(
            WorldgenContext.mix64(
                seed ^ key(cx, cz)
                    ^ 0x424f504445434f52L
                    ^ ((long) habitat.hashCode() << 32)
                    ^ slot.hashCode()
                    ^ layer));
    }

    static java.util.Random saltyCampRandom(long seed, int cx, int cz, int layer) {
        return new java.util.Random(WorldgenContext.mix64(seed ^ key(cx, cz) ^ 0x73616c7463616d70L ^ layer));
    }

    static boolean saltyCampVertical(int y, int physicalHeight, int authoredHeight) {
        return y >= 1 && y < physicalHeight && y < authoredHeight;
    }

    static boolean saltyCampReplaceable(boolean materialized, String current, String raw, Material material,
        boolean reviewedNativeTransition) {
        return !materialized || Objects.equals(current, raw)
            || material == Material.plants
            || material == Material.vine
            || reviewedNativeTransition;
    }

    static boolean saltyCampMudTransition(String current, String raw, String acceptedSamePass, String nativeMineralMud,
        String nativeSaltDirt, boolean freshNaturalSoil) {
        boolean exact = saltyCampMudSource(current, raw, nativeMineralMud, nativeSaltDirt);
        return exact && (Objects.equals(current, acceptedSamePass) || freshNaturalSoil);
    }

    static boolean saltyCampMudSource(String current, String raw, String nativeMineralMud, String nativeSaltDirt) {
        // WorldGenMineralMud replaces the salt-soil skin. The earlier native WorldGenLakesMod also writes its
        // configured mineral-mud sub-water block directly at waterY-1 after accepting a solid basin, so its hosted
        // predecessor can be exact geological stone. These are the only reviewed predecessors, not a solid tag.
        return Objects.equals(current, nativeMineralMud)
            && (Objects.equals(raw, nativeSaltDirt) || Objects.equals(raw, "minecraft:stone"));
    }

    static boolean saltyCampOwnedPoolTransition(Block current, int currentMetadata, String raw, Block acceptedSamePass,
        int acceptedMetadata, String nativeSaltGrass, boolean freshNaturalPool) {
        boolean stationaryWater = current != null && current == net.minecraft.init.Blocks.water && currentMetadata == 0;
        boolean acceptedStationaryWater = acceptedSamePass != null
            && acceptedSamePass == net.minecraft.init.Blocks.water
            && acceptedMetadata == 0;
        return saltyCampOwnedPoolPolicy(
            stationaryWater,
            raw,
            acceptedStationaryWater,
            nativeSaltGrass,
            freshNaturalPool);
    }

    static boolean saltyCampOwnedPoolPolicy(boolean stationaryWater, String raw, boolean acceptedStationaryWater,
        String nativeSaltGrass, boolean freshNaturalPool) {
        // Native validation scans x+[0,18],z+[0,15], but rotation-0 fill writes x+[0,19],z+[0,-16]. A shallow
        // pond in that actual footprint is therefore intentionally overwritten without entering the ground scan.
        return stationaryWater && Objects.equals(raw, nativeSaltGrass) && (acceptedStationaryWater || freshNaturalPool);
    }

    private void placeBopPlants(IslandWorld view, int cx, int cz, String habitat) {
        var binding = BopNativeBiomes.binding(habitat);
        for (var slot : binding.slots) {
            var random = nativeBopRandom(seed, cx, cz, habitat, slot.name(), view.layer);
            int attempts = slot.attempts();
            if ((slot.kind() == BopNativeBiomes.Kind.TREE || slot.kind() == BopNativeBiomes.Kind.ORCHARD_TREE)
                && random.nextInt(10) == 0) attempts++;
            for (int attempt = 0; attempt < attempts; attempt++) {
                int x = cx * 16 + random.nextInt(16) + 8, z = cz * 16 + random.nextInt(16) + 8;
                var sites = view.sites(x, z);
                if (view.layer >= sites.size()) continue;
                Site site = sites.get(view.layer);
                if (!habitat.equals(site.biome)) continue;
                int height = (BopNativeBiomes.shore(slot) || slot.kind() == BopNativeBiomes.Kind.MOSS
                    || slot.kind() == BopNativeBiomes.Kind.SPLOTCH ? site.surface.floorY : site.surface.fluidTopY) + 1;
                if (habitat.equals(BopNativeBiomes.GRASSLAND) || habitat.equals(BopNativeBiomes.OMINOUS_WOODS)
                    || habitat.equals(BopNativeBiomes.MYSTIC_GROVE)) height = view.height("WORLD_SURFACE", x, z);
                if (habitat.equals(BopNativeBiomes.LUSH_DESERT)) {
                    height = slot.kind() == BopNativeBiomes.Kind.OASIS_BANK
                        ? industries.angelic.biomic.compat.biome.BopOasisBank
                            .anchorY(view.nativeBopAccess(habitat, slot), x, site.surface.floorY + 1, z)
                        : view.height("WORLD_SURFACE", x, z);
                    if (height < 1) continue;
                }
                if (BopNativeBiomes.nativeMudBanks(habitat)) {
                    height = BopNativeBiomes.shore(slot)
                        ? industries.angelic.biomic.compat.biome.BopOasisBank
                            .anchorY(view.nativeBopAccess(habitat, slot), x, site.surface.floorY + 1, z)
                        : view.height("WORLD_SURFACE", x, z);
                    if (height < 1) continue;
                }
                if ((BopNativeBiomes.FEN.equals(habitat) || BopNativeBiomes.BAYOU.equals(habitat)
                    || BopNativeBiomes.WETLAND.equals(habitat)) && slot.kind() == BopNativeBiomes.Kind.MOSS)
                    height = site.surface.floorY + 1;
                if (BopNativeBiomes.finalBatch(habitat)) height = view.height("WORLD_SURFACE", x, z);
                // The sparse hosted view returns zero when this random offset has no physical surface. In a shifted
                // world zero is lower headroom, not an authored BOP Y. Skip the complete native attempt: clamping or
                // falling back to the nearby Site floor would move source geometry into a different column.
                if (!admitsNativeBopSurface(nativeBopHeights, height)) continue;
                int y = nativeBopHeights.select(slot.height(), random, height);
                view.feature = "biomic:native_bop/" + habitat + "/" + slot.name();
                if (villageGuard != null && villageGuard.blocks(view.feature, BopNativeBiomes.woody(slot), x, y, z)) {
                    rejected++;
                    continue;
                }
                HostedPositionMap writes = new HostedPositionMap();
                view.transactions.push(writes);
                view.transactionHabitats.push(habitat);
                boolean success = false;
                try {
                    industries.angelic.biomic.compat.biome.BopNativePond.HostedAccess bopAccess = view
                        .nativeBopAccess(habitat, slot);
                    int generatorY = y;
                    if (slot.kind() == BopNativeBiomes.Kind.TREE) {
                        generatorY = nativeBopHeights.nativeGeneratorY(y);
                        int offset = Math.subtractExact(y, generatorY);
                        if (offset != 0)
                            bopAccess = new industries.angelic.biomic.compat.biome.BopHostedYOffset(bopAccess, offset);
                    }
                    if (slot.kind() == BopNativeBiomes.Kind.ORCHARD_TREE) {
                        var selected = industries.angelic.biomic.compat.biome.BopOrchardTrees.select(random);
                        if (selected.fruit() && view.appleOrchards) {
                            success = HarvestCraftOrchards
                                .place(view.sourceWorld, view.nativeAccess(), random, x, y, z);
                        } else if (selected.fruit()) {
                            // Without an enabled fruit provider retain a real oak, not fake fruit leaves.
                            success = industries.angelic.biomic.compat.biome.BopWholeTree.place(
                                view.sourceWorld,
                                bopAccess,
                                random,
                                new net.minecraft.world.gen.feature.WorldGenTrees(false),
                                x,
                                generatorY,
                                z);
                        } else success = industries.angelic.biomic.compat.biome.BopOrchardTrees
                            .placeNonFruit(view.sourceWorld, bopAccess, random, selected, x, generatorY, z);
                    } else success = BopNativeBiomes.place(
                        habitat,
                        slot,
                        view.sourceWorld,
                        BopNativeBiomes.SACRED_SPRINGS.equals(habitat) && slot.kind() == BopNativeBiomes.Kind.TREE
                            ? BopSurfaceCapture.sacredOak(bopAccess, random)
                            : BopSurfaceCapture.hostedAware(bopAccess, random),
                        random,
                        slot.kind() == BopNativeBiomes.Kind.SPLOTCH ? x - 8 : x,
                        generatorY,
                        slot.kind() == BopNativeBiomes.Kind.SPLOTCH ? z - 8 : z);
                    // Native generators may wrap leaves around a pre-existing mushroom instead of
                    // failing. Inspect the complete proposal against the pre-transaction physical world.
                    if (success && BopNativeBiomes.woody(slot) && !writes.isEmpty()) {
                        var envelope = HostedTreeLedger.bounds(writes.keySet());
                        success = HostedTreeLedger.clear(envelope, p -> {
                            view.checkColumn(p.x, p.z);
                            return HostedTreeLedger.mushroom(view.physical.blockState(p.x, p.y, p.z));
                        });
                        if (!success) rejected++;
                    }
                } catch (OutsideFeature rejectedFeature) {
                    rejected++;
                } finally {
                    view.transactions.pop();
                    view.transactionHabitats.pop();
                }
                if (view.traced && slot.kind() == BopNativeBiomes.Kind.SPLOTCH) view.trace(
                    new Pos(x, y, z),
                    "native-splotch-attempt success=" + success + " proposed=" + writes.size());
                if (view.traced && slot.kind() == BopNativeBiomes.Kind.SPLATTER) view.trace(
                    new Pos(x, y, z),
                    "native-splatter-attempt success=" + success + " proposed=" + writes.size());
                if (view.traced && slot.kind() == BopNativeBiomes.Kind.POND) view
                    .trace(new Pos(x, y, z), "native-pond-attempt success=" + success + " proposed=" + writes.size());
                if (view.traced && BopNativeBiomes.lowlandWetland(habitat)) view.trace(
                    new Pos(x, y, z),
                    "native-wetland-attempt success=" + success
                        + " proposed="
                        + writes.size()
                        + " floor="
                        + site.surface.floorY
                        + " height="
                        + height);
                if (success) {
                    success = view.commit(writes, BopNativeBiomes.woody(slot) && !writes.isEmpty());
                }
                if (success) {
                    if (view.traced && BopNativeBiomes.lowlandWetland(habitat)) writes.forEach(
                        (p, state) -> view.trace(
                            p,
                            "committed-native-wetland=" + state
                                + " physical="
                                + view.physical.blockState(p.x, p.y, p.z)));
                    if (view.traced && slot.kind() == BopNativeBiomes.Kind.ROCKPILE)
                        writes.forEach((p, state) -> view.trace(p, "committed-native-rock=" + state));
                    if (view.traced && BopNativeBiomes.TUNDRA.equals(habitat) && BopNativeBiomes.shore(slot))
                        writes.forEach((p, state) -> view.trace(p, "committed-native-shore=" + state));
                    if (view.traced && slot.kind() == BopNativeBiomes.Kind.SPLOTCH)
                        writes.forEach((p, state) -> view.trace(p, "committed-native-splotch=" + state));
                    if (view.traced && slot.kind() == BopNativeBiomes.Kind.SPLATTER)
                        writes.forEach((p, state) -> view.trace(p, "committed-native-splatter=" + state));
                    if (view.traced && slot.kind() == BopNativeBiomes.Kind.POND)
                        writes.forEach((p, state) -> view.trace(p, "committed-native-pond=" + state));
                    if (view.traced && slot.kind() == BopNativeBiomes.Kind.GIANT_MUSHROOM)
                        writes.forEach((p, state) -> view.trace(p, "committed-native-giant-mushroom=" + state));
                    if (!writes.isEmpty()) placed++;
                }
            }
        }
    }

    static boolean admitsNativeBopSurface(industries.angelic.biomic.compat.biome.BopHeightSchedule schedule,
        int physicalSurfaceY) {
        return schedule.acceptsPhysicalSurfaceY(physicalSurfaceY);
    }

    private void placeSaltMarshPlants(IslandWorld view, int cx, int cz) {
        String habitat = industries.angelic.biomic.compat.biome.SaltMarshCompat.BIOME;
        if (!fullNativeMarsh) placeSaltMarshWood(view, cx, cz, habitat);
        for (var feature : fullNativeMarsh
            ? industries.angelic.biomic.compat.biome.SaltMarshCompat.EcologyFeature.values()
            : new industries.angelic.biomic.compat.biome.SaltMarshCompat.EcologyFeature[] {
                industries.angelic.biomic.compat.biome.SaltMarshCompat.EcologyFeature.MIXED_PLANTS }) {
            if (fullNativeMarsh
                && feature == industries.angelic.biomic.compat.biome.SaltMarshCompat.EcologyFeature.LILY_PADS) {
                placeSaltMarshCamp(view, cx, cz, habitat);
                placeSaltMarshWood(view, cx, cz, habitat);
            }
            java.util.Random random = new java.util.Random(
                WorldgenContext.mix64(
                    seed ^ key(cx, cz)
                        ^ 0x73616c746d617273L
                        ^ view.layer
                        ^ (fullNativeMarsh ? ((long) feature.ordinal() << 32) : 0)));
            for (int attempt = 0; attempt < feature.count(); attempt++) {
                int x = cx * 16 + random.nextInt(16), z = cz * 16 + random.nextInt(16);
                List<Site> sites = view.sites(x, z);
                if (view.layer >= sites.size()) continue;
                Site site = sites.get(view.layer);
                if (!habitat.equals(site.biome)) continue;
                if (!fullNativeMarsh && site.surface.fluidDepth() > 0) continue;
                view.feature = "biomic:native_salt_marsh_" + feature.name()
                    .toLowerCase(java.util.Locale.ROOT);
                HostedPositionMap writes = new HostedPositionMap();
                view.transactions.push(writes);
                view.transactionHabitats.push(habitat);
                boolean success = false;
                try {
                    success = industries.angelic.biomic.compat.biome.SaltMarshCompat.placeEcology(
                        feature,
                        view.sourceWorld,
                        view.nativeAccess(industries.angelic.biomic.compat.biome.SaltMarshCompat::encodePlant),
                        // Despite its name, 1.7's getTopSolidOrLiquidBlock ignores water.
                        random,
                        x,
                        site.surface.floorY + 1,
                        z,
                        saltMarshHeight);
                } catch (OutsideFeature ignored) {
                    rejected++;
                } finally {
                    view.transactions.pop();
                    view.transactionHabitats.pop();
                }
                if (success) {
                    view.commit(writes, false);
                }
            }
        }
    }

    /** One native decorator attempt, planned and validated in full before any physical write. */
    private void placeSaltMarshCamp(IslandWorld view, int cx, int cz, String habitat) {
        java.util.Random random = saltyCampRandom(seed, cx, cz, view.layer);
        // Source draws the offset before its configured one-in-N frequency gate.
        int x = cx * 16 + random.nextInt(16) + 8, z = cz * 16 + random.nextInt(16) + 8;
        if (!industries.angelic.biomic.compat.biome.SaltMarshCompat.shouldAttemptCamp(random)) return;
        List<Site> originSites = view.sites(x, z);
        if (view.layer >= originSites.size()) {
            traceSaltyCamp(view, cx, cz, x, z, "origin-layer-absent");
            return;
        }
        Site origin = originSites.get(view.layer);
        if (!habitat.equals(origin.biome) || origin.surface.fluidDepth() > 0) {
            traceSaltyCamp(view, cx, cz, x, z, "origin-not-dry-marsh");
            return;
        }
        int physicalHeight = terrain.layout()
            .height();
        int authoredHeight = terrain.projection()
            .physicalMaxYExclusive();
        view.feature = "biomic:native_salt_marsh_brickmaker_camp";
        var surface = new industries.angelic.biomic.compat.biome.SaltyBrickmakerCamp.Surface() {

            private Site site(int px, int pz) {
                List<Site> target = view.sites(px, pz);
                if (view.layer >= target.size()) throw new OutsideFeature();
                Site site = target.get(view.layer);
                if (!habitat.equals(site.biome) || site.surface.fluidDepth() > 0) throw new OutsideFeature();
                return site;
            }

            public int minimumY() {
                return terrain.minimumEcologyY();
            }

            public int maximumYExclusive() {
                return physicalHeight;
            }

            public Block block(int px, int py, int pz) {
                site(px, pz);
                return view.block(px, py, pz);
            }

            public int metadata(int px, int py, int pz) {
                site(px, pz);
                return view.physical.metadata(px, py, pz);
            }

            public int heightValue(int px, int pz) {
                return site(px, pz).surface.floorY + 1;
            }

            public boolean allowsWrite(int px, int py, int pz, Block before, int beforeMetadata, Block after,
                int afterMetadata) {
                Site site = site(px, pz);
                if (py <= terrain.minimumEcologyY() || !saltyCampVertical(py, physicalHeight, authoredHeight)) {
                    traceSaltyCamp(view, cx, cz, x, z, "policy-build-height at=" + px + "," + py + "," + pz);
                    return false;
                }
                int minimum = Math.max(site.surface.bottomY, site.surface.floorY - 2);
                if (py < minimum) {
                    traceSaltyCamp(
                        view,
                        cx,
                        cz,
                        x,
                        z,
                        "policy-support at=" + px
                            + ","
                            + py
                            + ","
                            + pz
                            + " minimum="
                            + minimum
                            + " bottom="
                            + site.surface.bottomY
                            + " floor="
                            + site.surface.floorY);
                    return false;
                }
                if (py >= site.surface.ceilingY) {
                    traceSaltyCamp(
                        view,
                        cx,
                        cz,
                        x,
                        z,
                        "policy-ceiling at=" + px + "," + py + "," + pz + " ceiling=" + site.surface.ceilingY);
                    return false;
                }
                String current = view.blockState(px, py, pz), raw = view.rawState(px, py, pz);
                boolean materialized = view.physical.isMaterialized(px, py, pz);
                Block currentBlock = view.block(px, py, pz);
                int currentMetadata = view.physical.metadata(px, py, pz);
                Material material = currentBlock.getMaterial();
                String acceptedSamePass = view.accepted.get(px, py, pz);
                var acceptedResolved = acceptedSamePass == null ? null
                    : industries.angelic.biomic.world.modern.ModernStateCompatibility.resolve(acceptedSamePass);
                Block acceptedBlock = acceptedResolved == null ? null : acceptedResolved.getBlock();
                int acceptedMetadata = acceptedResolved == null ? -1 : acceptedResolved.getMetadata();
                String nativeMud = industries.angelic.biomic.compat.biome.SaltMarshCompat.mineralMudState();
                String nativeSaltDirt = industries.angelic.biomic.compat.biome.SaltMarshCompat.saltSoil(1);
                String nativeSaltGrass = industries.angelic.biomic.compat.biome.SaltMarshCompat.saltSoil(0);
                boolean exactMudTransition = saltyCampMudSource(current, raw, nativeMud, nativeSaltDirt);
                String soilRejection = null;
                boolean ownPass = exactMudTransition && Objects.equals(current, acceptedSamePass);
                if (exactMudTransition && !ownPass) {
                    var mud = industries.angelic.biomic.world.modern.ModernStateCompatibility.resolve(nativeMud);
                    soilRejection = view.physical
                        .freshNaturalSoilRejection(px, py, pz, mud.getBlock(), mud.getMetadata());
                }
                boolean reviewedMud = saltyCampMudTransition(
                    current,
                    raw,
                    acceptedSamePass,
                    nativeMud,
                    nativeSaltDirt,
                    exactMudTransition && !ownPass && soilRejection == null);
                boolean exactPool = currentBlock == net.minecraft.init.Blocks.water && currentMetadata == 0
                    && Objects.equals(raw, nativeSaltGrass);
                boolean ownPool = acceptedBlock == net.minecraft.init.Blocks.water && acceptedMetadata == 0;
                String poolRejection = null;
                if (exactPool && !ownPool) poolRejection = view.physical
                    .freshNaturalSoilRejection(px, py, pz, net.minecraft.init.Blocks.water, 0);
                boolean reviewedPool = saltyCampOwnedPoolTransition(
                    currentBlock,
                    currentMetadata,
                    raw,
                    acceptedBlock,
                    acceptedMetadata,
                    nativeSaltGrass,
                    exactPool && !ownPool && poolRejection == null);
                boolean replaceable = saltyCampReplaceable(
                    materialized,
                    current,
                    raw,
                    material,
                    reviewedMud || reviewedPool);
                if (!replaceable) traceSaltyCamp(
                    view,
                    cx,
                    cz,
                    x,
                    z,
                    "policy-collision at=" + px
                        + ","
                        + py
                        + ","
                        + pz
                        + " materialized="
                        + materialized
                        + " current="
                        + current
                        + " raw="
                        + raw
                        + " accepted="
                        + acceptedSamePass
                        + " soilProof="
                        + soilRejection
                        + " poolProof="
                        + poolRejection
                        + " reviewedPool="
                        + reviewedPool
                        + " material="
                        + material);
                return replaceable;
            }
        };
        industries.angelic.biomic.compat.biome.SaltyBrickmakerCamp.Plan plan;
        try {
            plan = industries.angelic.biomic.compat.biome.SaltMarshCompat
                .planCamp(view.sourceWorld, surface, random, x, origin.surface.floorY + 1, z);
        } catch (OutsideFeature ignored) {
            traceSaltyCamp(view, cx, cz, x, z, "capture-surface-boundary");
            rejected++;
            return;
        }
        if (plan == null) {
            traceSaltyCamp(
                view,
                cx,
                cz,
                x,
                z,
                "capture-" + industries.angelic.biomic.compat.biome.SaltyBrickmakerCamp.takeLastRejection());
            return;
        }
        // Revalidate the complete immutable plan against the unchanged physical view. The
        // physical commit separately validates every chest payload before its first write.
        try {
            for (var entry : plan.placements()
                .entrySet()) {
                var p = entry.getKey();
                var placement = entry.getValue();
                Block after = industries.angelic.biomic.world.modern.ModernStateCompatibility.resolve(placement.state())
                    .getBlock();
                if (!surface.allowsWrite(
                    p.x(),
                    p.y(),
                    p.z(),
                    view.block(p.x(), p.y(), p.z()),
                    view.physical.metadata(p.x(), p.y(), p.z()),
                    after,
                    industries.angelic.biomic.world.modern.ModernStateCompatibility.resolve(placement.state())
                        .getMetadata()))
                    throw new OutsideFeature();
            }
            view.physical.commitSaltyCamp(plan);
        } catch (OutsideFeature ignored) {
            traceSaltyCamp(view, cx, cz, x, z, "whole-plan-revalidation");
            rejected++;
            return;
        }
        plan.placements()
            .forEach((p, placement) -> view.accepted.put(new Pos(p.x(), p.y(), p.z()), placement.state()));
        traceSaltyCamp(
            view,
            cx,
            cz,
            x,
            z,
            "committed writes=" + plan.placements()
                .size());
        placed++;
    }

    private static void traceSaltyCamp(IslandWorld view, int cx, int cz, int x, int z, String reason) {
        if (Boolean.getBoolean("biomic.traceSaltyCamp")) industries.angelic.biomic.Biomic.LOG
            .info("Salty camp root={},{} layer={} origin={},{} result={}", cx, cz, view.layer, x, z, reason);
    }

    private void placeSaltMarshWood(IslandWorld view, int cx, int cz, String habitat) {
        for (var feature : industries.angelic.biomic.compat.biome.SaltMarshCompat.WoodFeature.values()) {
            var random = new java.util.Random(
                WorldgenContext
                    .mix64(seed ^ key(cx, cz) ^ 0x73616c74776f6f64L ^ ((long) feature.ordinal() << 32) ^ view.layer));
            int x = cx * 16 + random.nextInt(16), z = cz * 16 + random.nextInt(16);
            if (random.nextInt(feature.rarity) != 0) continue;
            List<Site> sites = view.sites(x, z);
            if (view.layer >= sites.size()) continue;
            Site site = sites.get(view.layer);
            if (!habitat.equals(site.biome) || site.surface.fluidDepth() > 0) continue;
            view.feature = "biomic:native_salt_marsh_" + feature.name()
                .toLowerCase(java.util.Locale.ROOT);
            HostedPositionMap writes = new HostedPositionMap();
            view.transactions.push(writes);
            view.transactionHabitats.push(habitat);
            boolean success = false;
            try {
                success = industries.angelic.biomic.compat.biome.SaltMarshCompat.placeWood(
                    feature,
                    view.sourceWorld,
                    view.nativeAccess(industries.angelic.biomic.compat.biome.SaltMarshCompat::encodeWood),
                    random,
                    x,
                    site.surface.floorY + 1,
                    z);
            } catch (OutsideFeature ignored) {
                rejected++;
            } finally {
                view.transactions.pop();
                view.transactionHabitats.pop();
            }
            if (success) {
                view.commit(writes, false);
            }
        }
    }

    /**
     * One or two trees on each islet centred in the chunk: the first plain tree of its habitat's vegetation (no giant,
     * tall or fallen forms), at its middle or a block off, and for a second two blocks off diagonally.
     */
    private void placeIsletTrees(IslandWorld view, int chunkX, int chunkZ) {
        for (int[] islet : islands.islets(chunkX, chunkZ)) {
            ModernWorldgenRandom random = new ModernWorldgenRandom();
            random.setSeed(WorldgenContext.mix64(seed ^ key(islet[0], islet[1]) ^ 0x69736C6574L));
            int[][] spots = { { 0, 0 }, { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
                { random.nextBoolean() ? 2 : -2, random.nextBoolean() ? 2 : -2 } };
            int planted = 0;
            for (int spot = 0; spot < spots.length && planted < islet[2]; spot++) {
                if (planted == 1 && spot < 5) spot = 5; // The second tree stands apart from the first.
                int x = islet[0] + spots[spot][0], z = islet[1] + spots[spot][1];
                if (x >> 4 != chunkX || z >> 4 != chunkZ) continue;
                List<Site> sites = view.sites(x, z);
                if (sites.size() <= view.layer) continue;
                Site site = sites.get(view.layer);
                String feature = isletTree(site.biome);
                if (feature == null || site.surface.fluidDepth() > 0
                    || site.surface.dryHeadroom() < 16
                    || !view.canSurvive("minecraft:oak_sapling", x, site.surface.floorY + 1, z)) continue;
                view.feature = feature;
                if (engine.placeConfigured(feature, view, random, new Pos(x, site.surface.floorY + 1, z))) planted++;
                view.feature = null;
            }
        }
    }

    /** What hangs under the islands of a chunk, and its waterfalls (IslandUnderside), before the flora roots. */
    private void decorateUndersides(OrigenFeatureRuntime.PhysicalPopulation physical, int chunkX, int chunkZ) {
        Map<Long, IslandKind> kinds = new java.util.HashMap<>();
        IslandUnderside.decorate(seed, chunkX, chunkZ, new IslandUnderside.Cells() {

            @Override
            public int span(int x, int z) {
                return islands.span(x, z);
            }

            @Override
            public IslandKind kind(int x, int z, int top) {
                return kinds.computeIfAbsent(key(x, z), ignored -> {
                    String habitat = terrain.floatingHabitatAt(x, top, z);
                    return habitat == null ? null : IslandKind.of(habitat);
                });
            }

            @Override
            public boolean air(int x, int y, int z) {
                return terrain.layout()
                    .containsY(y) && ModernSurfaceSystem.isAirState(physical.blockState(x, y, z));
            }

            @Override
            public boolean soil(int x, int y, int z) {
                net.minecraft.block.material.Material material = physical.block(x, y, z)
                    .getMaterial();
                return material == net.minecraft.block.material.Material.grass
                    || material == net.minecraft.block.material.Material.ground;
            }

            @Override
            public byte raw(int x, int y, int z) {
                return physical.rawMaterial(x, y, z);
            }

            @Override
            public boolean guarded(int x, int y, int z, boolean wide) {
                return villageGuard != null && villageGuard.blocks("biomic:island_underside", wide, x, y, z);
            }

            @Override
            public void set(int x, int y, int z, String state) {
                physical.setBlock(x, y, z, state);
            }
        });
    }

    /**
     * A habitat's first plain tree among its vegetation features (configured id), or null. Habitats outside the modern
     * graph (BOP's own) borrow a vanilla one of their kind.
     */
    private String isletTree(String habitat) {
        String found = isletTrees.computeIfAbsent(habitat, key -> {
            String source = graph.biomeIds()
                .contains(key) ? key : switch (IslandKind.of(key)) {
                case SNOWY -> "minecraft:snowy_taiga";
                case DRY -> "minecraft:savanna";
                default -> "minecraft:forest";
            };
            for (String placed : graph.orderedFeatures(java.util.Collections.singleton(source), 9)) {
                if (!placed.contains("tree")) continue;
                boolean small = true;
                for (String large : new String[] { "mega", "giant", "huge", "tall", "big", "redwood", "fallen" })
                    small &= !placed.contains(large);
                if (!small) continue;
                var feature = graph.placedFeature(placed)
                    .get("feature");
                if (feature == null || !feature.isJsonPrimitive()) continue;
                String configured = feature.getAsString();
                String type = graph.configuredFeature(configured)
                    .get("type")
                    .getAsString();
                if (type.endsWith(":tree") || type.endsWith("random_selector") || type.endsWith("weighted_selector"))
                    return configured;
            }
            return "";
        });
        return found.isEmpty() ? null : found;
    }

    /** Sparse RU's single random X/Z attempt often hits a refuge's rock side, never its small soil cap. */
    private void placeRefugeRoofTree(IslandWorld view, int chunkX, int chunkZ) {
        long salt = WorldgenContext.mix64(seed ^ key(chunkX, chunkZ) ^ 0x726566756765L);
        ModernWorldgenRandom random = new ModernWorldgenRandom();
        random.setSeed(salt);
        view.feature = "biomic:refuge_roof_redwoods_primary";
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = (chunkX << 4) + random.nextInt(16), z = (chunkZ << 4) + random.nextInt(16);
            if (mainland.foundationAt(x, z) != industries.angelic.biomic.biome.origen.OrigenBiomeProfile.ROCKY_REFUGE
                || !("regions_unexplored:redwoods".equals(mainland.sample(x, z))
                    || "regions_unexplored:sparse_redwoods".equals(mainland.sample(x, z))))
                continue;
            List<Site> sites = view.sites(x, z);
            if (sites.isEmpty()) continue;
            Site site = sites.get(0);
            int floor = site.surface.floorY;
            if (floor < physicalFromLegacyEcologyY(100) || floor > physicalFromLegacyEcologyY(195)
                || site.surface.dryHeadroom() < 48) continue;
            // Fit the complete trunk footing onto real, nearly level soil; do not make dirt pedestals.
            boolean footing = true;
            for (int dz = -1; dz <= 1 && footing; dz++) for (int dx = -1; dx <= 1; dx++) {
                List<Site> neighbor = view.sites(x + dx, z + dz);
                if (neighbor.isEmpty() || Math.abs(neighbor.get(0).surface.floorY - floor) > 1
                    || !view.canSurvive("minecraft:oak_sapling", x + dx, neighbor.get(0).surface.floorY + 1, z + dz)) {
                    footing = false;
                    break;
                }
            }
            if (!footing) continue;
            // Existing native trees keep their space; this pass only fills an unoccupied roof patch.
            boolean occupied = false;
            for (int dz = -12; dz <= 12 && !occupied; dz++) for (int dx = -12; dx <= 12; dx++) {
                if (view.blockState(x + dx, floor + 8, z + dz)
                    .startsWith("regions_unexplored:redwood_log")) {
                    occupied = true;
                    break;
                }
            }
            if (!occupied && engine
                .placeConfigured("regions_unexplored:tree/redwood_medium", view, random, new Pos(x, floor + 1, z)))
                return;
        }
    }

    /** Thin, supported vanilla vines instead of tropical leaf ropes on temperate cliff faces. */
    private void placeTemperateCliffVines(OrigenFeatureRuntime.PhysicalPopulation physical, int chunkX, int chunkZ) {
        int[][] faces = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        String[] states = { "minecraft:vine[west=true]", "minecraft:vine[east=true]", "minecraft:vine[north=true]",
            "minecraft:vine[south=true]" };
        for (int z = chunkZ << 4; z < (chunkZ << 4) + 16; z++) for (int x = chunkX << 4; x < (chunkX << 4) + 16; x++) {
            String habitat = mainland.sample(x, z);
            if (!HostedLandmarkVegetation.thinVines(habitat)
                || !RegionalHabitatSampler.isLandmark(mainland.foundationAt(x, z))) continue;
            long hash = WorldgenContext.mix64(seed ^ key(x, z) ^ 0x641B27A9L);
            if (Math.floorMod(hash, 7) != 0) continue;
            for (int legacyY = 242 - (int) Math.floorMod(hash >>> 9, 16); legacyY > 63; legacyY -= 16) {
                int y = physicalFromLegacyEcologyY(legacyY);
                if (physical.rawMaterial(x, y, z) != BiomicTerrainSampler.AIR
                    || !ModernSurfaceSystem.isAirState(physical.blockState(x, y, z))) continue;
                for (int f = 0; f < faces.length; f++) {
                    int nx = x + faces[f][0], nz = z + faces[f][1];
                    byte support = physical.rawMaterial(nx, y, nz);
                    if (support == BiomicTerrainSampler.AIR || support == BiomicTerrainSampler.WATER
                        || support == BiomicTerrainSampler.LAVA) continue;
                    int length = 4 + (int) Math.floorMod(hash >>> (f + 16), 9);
                    for (int down = 0; down < length && y - down > physicalFromLegacyEcologyY(62); down++) {
                        if (!ModernSurfaceSystem.isAirState(physical.blockState(x, y - down, z))) break;
                        physical.setBlock(x, y - down, z, states[f]);
                    }
                    break;
                }
            }
        }
    }

    /** Maps thresholds from the established hosted ecology without re-compressing shifted terrain. */
    private int physicalFromLegacyEcologyY(int legacyY) {
        if (terrain.projection() == AuthoredVerticalProjections.origenCompressed256()) return legacyY;
        int sourceY = industries.angelic.biomic.world.modern.ModernVerticalTransform.toModernY(legacyY);
        return terrain.projection()
            .toPhysicalBlockY(sourceY)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Hosted ecology threshold is outside the shifted authored envelope"));
    }

    public String report() {
        String features = (mainland == null ? "Hosted island" : "Regional mainland") + " features: "
            + placed
            + " accepted, "
            + rejected
            + " boundary rejects; chunk materials "
            + chunkCacheHits
            + " cached, "
            + chunkPayloadBorrows
            + " borrowed, "
            + chunkFallbackComputations
            + " fallback";
        return features;
    }

    public void resetPerformanceStats() {
        chunkCacheHits = 0L;
        chunkPayloadBorrows = 0L;
        chunkFallbackComputations = 0L;
    }

    public Boolean groundCoverSupport(String state, String below) {
        if (industries.angelic.biomic.compat.biome.SaltMarshCompat.supportsHostedGrass(state, below)) return true;
        Boolean nativeBop = industries.angelic.biomic.compat.biome.BopGroundCoverSupport.test(state, below);
        if (nativeBop != null) return nativeBop;
        return industries.angelic.biomic.world.modern.ModernGroundCoverSupport.test(state, below, engine::isBlockInTag);
    }

    public Boolean groundCoverSupportAt(String state, String below, int x, int y, int z) {
        String habitat = mainland == null ? null : mainland.sample(x, z);
        Boolean nativeSupport = industries.angelic.biomic.compat.biome.BopGroundCoverSupport
            .located(habitat, state, below);
        return nativeSupport != null ? nativeSupport : groundCoverSupport(state, below);
    }

    /** Immutable native soil, not live decoration, reserves a thin layer from projected source ores. */
    public boolean ownsNativeShoreSoilAt(int x, int y, int z) {
        if (mainland != null && !BopNativeBiomes.reservesOreSubstrate(mainland.sample(x, z))) return false;
        if (mainland == null && terrain.floatingArchetypeAt(x, y, z) == null) return false;
        for (Site site : chunk(x >> 4, z >> 4).sites[(z & 15) * 16 + (x & 15)]) if (y >= site.surface.bottomY
            && BopNativeBiomes.ownsOreSubstrate(site.biome, site.surface.floorY - y, site.soilLayers)) return true;
        return false;
    }

    /** Same admitted surfaces as RU population, including outdoor wetland beds but never enclosed caves. */
    public boolean ownsSurfaceDecorationAt(int x, int y, int z) {
        if (mainland == null) return false;
        for (Site site : chunk(x >> 4, z >> 4).sites[(z & 15) * 16 + (x & 15)]) {
            if (site.surface.ownsDecorationAt(y)
                || HostedVegetationPolicy.hasWetSurface(site.biome) && y >= site.surface.floorY - 3
                    && y < site.surface.ceilingY)
                return true;
        }
        return false;
    }

    /** Persist mainland habitat only around its actual exposed surface; retain all deeper cave identities. */
    public industries.angelic.biomic.storage.BiomeVolume projectMainland(
        industries.angelic.biomic.storage.BiomeVolume original, int chunkX, int chunkZ) {
        if (mainland == null) return original;
        List<String> palette = new java.util.ArrayList<>(original.getPalette());
        short[] cells = original.copyPaletteIndices();
        IslandChunk chunk = chunk(chunkX, chunkZ);
        for (int qz = 0; qz < 4; qz++) for (int qx = 0; qx < 4; qx++) {
            for (Site site : chunk.sites[(qz * 4 + 2) * 16 + qx * 4 + 2]) {
                String key = site.storedBiome;
                int index = palette.indexOf(key);
                if (index < 0) {
                    index = palette.size();
                    palette.add(key);
                }
                for (int qy = 0; qy < original.getQuartHeight(); qy++) {
                    int y = qy * 4 + 2;
                    if (y >= Math.max(site.surface.bottomY, site.surface.floorY - 4) && y < site.surface.ceilingY) {
                        cells[(qy * 4 + qz) * 4 + qx] = (short) index;
                    }
                }
            }
        }
        return industries.angelic.biomic.storage.BiomeVolume
            .create(original.getGeneratorVersion(), original.getBlockHeight(), palette, cells);
    }

    /**
     * Prepares a chunk's sites on a worker from the terrain it was just composed with, before the server paints its
     * skins (surface) or populates it: on the server thread that took a sixth of its time where hosted habitats lie.
     * Sites are a function of the terrain and of the seed's samplers, so the server finds the ones it would have made.
     */
    public void prepare(int chunkX, int chunkZ, byte[] materials) {
        long key = key(chunkX, chunkZ);
        synchronized (chunks) {
            if (chunks.get(key) != null) return;
        }
        IslandChunk chunk = new IslandChunk(chunkX, chunkZ, materials);
        synchronized (chunks) {
            if (chunks.get(key) == null) chunks.put(key, chunk);
        }
    }

    private IslandChunk chunk(int x, int z) {
        long key = key(x, z);
        IslandChunk value;
        synchronized (chunks) {
            value = chunks.get(key);
        }
        if (value == null) {
            byte[] materials = validatedBorrowedRawTerrain(
                rawTerrainLookup == null ? null : rawTerrainLookup.find(x, z),
                terrain.layout());
            if (materials == null) {
                materials = terrain.createMaterialArray(x, z);
                chunkFallbackComputations++;
            } else chunkPayloadBorrows++;
            value = new IslandChunk(x, z, materials);
            synchronized (chunks) {
                chunks.put(key, value);
            }
        } else chunkCacheHits++;
        return value;
    }

    static byte[] validatedBorrowedRawTerrain(byte[] materials) {
        if (materials != null && materials.length != BiomicTerrainSampler.BLOCK_COUNT)
            throw new IllegalArgumentException(
                "Borrowed hosted terrain must contain exactly one authored-height chunk");
        return materials;
    }

    static byte[] validatedBorrowedRawTerrain(byte[] materials,
        industries.angelic.biomic.api.ChunkVerticalLayout layout) {
        if (layout == industries.angelic.biomic.api.ChunkVerticalLayout.LEGACY_256) {
            return validatedBorrowedRawTerrain(materials);
        }
        if (layout == null || materials != null && materials.length != layout.blockCount()) {
            throw new IllegalArgumentException("Borrowed hosted terrain disagrees with its physical layout");
        }
        return materials;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    private final class IslandChunk {

        final byte[] materials;
        final List<Site>[] sites;
        /**
         * Mainland only: each column's highest ground surface (floor, fluid depth; floor -1 when none) and its
         * hosted habitat, null where Origen owns the ground.
         */
        final int[] groundFloor = new int[256], groundFluid = new int[256];
        final String[] groundHabitat = new String[256];

        @SuppressWarnings("unchecked")
        IslandChunk(int chunkX, int chunkZ, byte[] materials) {
            this.materials = materials;
            sites = (List<Site>[]) new List<?>[256];
            java.util.Arrays.fill(groundFloor, -1);
            int[] tops = mainland == null ? null : columnTops(materials);
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                int wx = (chunkX << 4) + x, wz = (chunkZ << 4) + z;
                byte[] column = new byte[terrain.layout()
                    .height()];
                for (int y = 0; y < column.length; y++) {
                    column[y] = materials[terrain.layout()
                        .index(x, y, z)];
                }
                List<Site> found = new java.util.ArrayList<>();
                String baseGroundHabitat = mainland == null ? null : mainland.sample(wx, wz);
                boolean landmark = mainland != null && RegionalHabitatSampler.isLandmark(mainland.foundationAt(wx, wz));
                boolean sawGroundSurface = false;
                for (DecorationSurfaceColumn.Surface surface : DecorationSurfaceColumn
                    .scan(column, terrain.layout(), y -> terrain.floatingArchetypeAt(wx, y, wz) != null)) {
                    if (surface.floorY < terrain.minimumEcologyY()) continue;
                    if (mainland != null) {
                        boolean highestGround = !sawGroundSurface && !surface.floating;
                        if (!surface.floating) sawGroundSurface = true;
                        // A shaft or pit bottom, walled in on most sides, is cave, not the habitat's ground: it must
                        // see the sky (the rays run only where this chunk's own columns wall it in).
                        if (highestGround && enclosed(tops, x, z, surface.floorY)
                            && !seesSky(wx, surface.fluidTopY, wz)) continue;
                        if (highestGround) {
                            groundFloor[z * 16 + x] = surface.floorY;
                            groundFluid[z * 16 + x] = surface.fluidDepth();
                        }
                        String groundHabitat = baseGroundHabitat;
                        boolean wetland = HostedVegetationPolicy.hasWetSurface(groundHabitat);
                        boolean outside = !highestGround && surface.dryHeadroom() >= 12
                            && hasOutdoorAccess(wx, surface.fluidTopY, wz)
                            && seesSky(wx, surface.fluidTopY, wz);
                        boolean admitted = wetland
                            ? surface.canHostWater(
                                highestGround,
                                outside,
                                column,
                                HostedVegetationPolicy.minimumWaterDepth(groundHabitat),
                                HostedVegetationPolicy.waterDepthLimit(groundHabitat))
                            : surface.canHostMainland(highestGround, outside);
                        admitted |= false;
                        if (highestGround) this.groundHabitat[z * 16 + x] = groundHabitat;
                        if (groundHabitat != null && admitted) {
                            int layers = HostedSurfaceSkin.layers(column, surface, landmark, wetland);
                            // Keep the authored narrow sand shore.
                            if (BopNativeBiomes.winter(groundHabitat))
                                layers = HostedSurfaceSkin.nativeWinterLayers(column, surface, layers);
                            if (BopNativeBiomes.barrenSurface(groundHabitat))
                                layers = HostedSurfaceSkin.geologicalLayers(column, surface, 3);
                            if (HostedIslandSurfacePalette.hasGeologicalSkin(groundHabitat)
                                || HostedVegetationPolicy.isCoastalBed(groundHabitat))
                                layers = HostedSurfaceSkin.coastalLayers(
                                    column,
                                    surface,
                                    palette.layerCount(groundHabitat, wx, surface.floorY, wz),
                                    groundHabitat);
                            if (!intrinsicSiteConflict(wx, wz, surface, layers))
                                found.add(new Site(surface, groundHabitat, layers));
                        }
                        continue;
                    }
                    if (!surface.floating) continue;
                    String biome = terrain.floatingHabitatAt(wx, surface.floorY, wz);
                    int layers = HostedSurfaceSkin.layers(column, surface);
                    if (biome != null && !intrinsicSiteConflict(wx, wz, surface, layers))
                        found.add(new Site(surface, biome, layers));
                }
                sites[z * 16 + x] = found;
            }
        }
    }

    private static final class Site {

        final DecorationSurfaceColumn.Surface surface;
        final String biome;
        final String storedBiome;
        final int soilLayers;

        Site(DecorationSurfaceColumn.Surface surface, String biome, int soilLayers) {
            this.surface = surface;
            this.biome = biome;
            this.soilLayers = soilLayers;
            this.storedBiome = industries.angelic.biomic.world.modern.ModernBiomeCompatibility.storedKey(biome);
        }
    }

    private boolean intrinsicSiteConflict(int x, int z, DecorationSurfaceColumn.Surface surface, int soilLayers) {
        return intrinsicSiteConflict(
            intrinsicTerrain,
            x,
            z,
            surface.bottomY,
            surface.floorY,
            surface.ceilingY,
            soilLayers);
    }

    static boolean intrinsicSiteConflict(IntrinsicHostedTerrainAccess intrinsic, int x, int z, int bottomY, int floorY,
        int ceilingY, int soilLayers) {
        if (intrinsic == null) return false;
        int minimum = Math.max(bottomY, floorY - Math.max(0, soilLayers - 1));
        for (int y = minimum; y <= Math.min(ceilingY - 1, floorY + 1); y++)
            if (intrinsic.blocksHostedEcology(x, y, z)) return true;
        return false;
    }

    private static int waterHeight(Site site) {
        return site.surface.fluidDepth() == 0 ? Integer.MIN_VALUE : site.surface.fluidTopY + 1;
    }

    private String soilState(Site site, String habitat, int depth, int x, int z) {
        int y = site.surface.floorY - depth;
        return industries.angelic.biomic.compat.biome.BopAridFeatures
            .stableSkin(habitat, palette.state(habitat, depth, x, y, z, waterHeight(site)), y, site.surface.bottomY);
    }

    /**
     * The habitat whose soil skins a ground site ({@link SkinJitter}): a neighbour's up to four blocks away, or null
     * where that neighbour is Origen land, whose own surface then stays. Plain land skins cross freely; on dry ground a
     * beach's (RU's gravel and grassy beaches) crosses too, both ways, so a shore meets the grass in an organic line.
     * Underwater sites, wet, geological, winter and barren skins, islands and estuaries keep their own. Identity,
     * features and stored biomes do not move.
     */
    private String skinHabitat(Site site, int x, int z) {
        boolean dry = site.surface.fluidDepth() == 0;
        if (mainland == null || site.surface.floating || !skinCrosses(site.biome, dry)) return site.biome;
        int jx = SkinJitter.x(seed, x, z), jz = SkinJitter.z(seed, x, z);
        if (jx == x && jz == z) return site.biome;
        String other = mainland.sample(jx, jz);
        if (other == null) return !HostedVegetationPolicy.isCoastalBed(site.biome)
            && SkinJitter.origenLand(mainland.foundationAt(jx, jz)) ? null : site.biome;
        return skinCrosses(other, dry) ? other : site.biome;
    }

    private static boolean skinCrosses(String habitat, boolean dry) {
        return habitat != null && !HostedVegetationPolicy.hasWetSurface(habitat)
            && !HostedIslandSurfacePalette.hasGeologicalSkin(habitat)
            && !BopNativeBiomes.winter(habitat)
            && !BopNativeBiomes.barrenSurface(habitat)
            && !"minecraft:warm_ocean".equals(habitat)
            && !"regions_unexplored:muddy_river".equals(habitat)
            && (dry || !HostedVegetationPolicy.isCoastalBed(habitat));
    }

    private Resolved resolveHostedState(String state) {
        return ModernStateCompatibility.resolve(state);
    }

    private boolean hasOutdoorAccess(int x, int floor, int z) {
        // An under-overhang meadow has a lateral opening to the surrounding sky surface.
        // Closed cave floors must not become extra copies of the surface biome/vegetation.
        return terrain.sampleCarvedGroundSurfaceHeight(x - 16, z) <= floor + 3
            || terrain.sampleCarvedGroundSurfaceHeight(x + 16, z) <= floor + 3
            || terrain.sampleCarvedGroundSurfaceHeight(x, z - 16) <= floor + 3
            || terrain.sampleCarvedGroundSurfaceHeight(x, z + 16) <= floor + 3;
    }

    /** Each column's highest solid cell in the chunk's raw materials (z * 16 + x). */
    private int[] columnTops(byte[] materials) {
        var layout = terrain.layout();
        int[] tops = new int[256];
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int y = layout.height() - 1;
            while (y > 0) {
                byte material = materials[layout.index(x, y, z)];
                if (material != BiomicTerrainSampler.AIR && material != BiomicTerrainSampler.WATER
                    && material != BiomicTerrainSampler.LAVA) break;
                y--;
            }
            tops[z * 16 + x] = y;
        }
        return tops;
    }

    /**
     * Whether this chunk's own columns wall the floor in: in at least four of eight directions a column two to six
     * blocks away rises more than eight above it (a direction leaving the chunk counts half).
     */
    private static boolean enclosed(int[] tops, int x, int z, int y) {
        double walled = 0;
        for (int[] ray : SKY_RAYS) {
            boolean inside = false, wall = false;
            for (int d = 2; d <= 6 && !wall; d++) {
                int nx = x + ray[0] * d, nz = z + ray[1] * d;
                if (nx < 0 || nx > 15 || nz < 0 || nz > 15) break;
                inside = true;
                wall = tops[nz * 16 + nx] > y + 8;
            }
            walled += wall ? 1 : inside ? 0 : 0.5D;
        }
        return walled >= 4;
    }

    private static final int[][] SKY_RAYS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { 1, -1 }, { -1, 1 },
        { -1, -1 } };

    /**
     * Whether a floor below the column's highest ground (under an overhang or arch) sees the sky: of nine rays
     * (straight up, and eight rising at 45 degrees over the column heights around), at least three get clear within
     * 24 blocks. The ground under an arch or overhang sees the sky; a cave floor reached through a shaft does not, and
     * stays cave (the surface habitat's soil, trees and plants stop at its rim).
     */
    private boolean seesSky(int x, int y, int z) {
        int open = terrain.sampleCarvedGroundSurfaceHeight(x, z) <= y ? 1 : 0;
        // Gentle open ground, the common case: nothing around rises above the floor nearby.
        if (open == 1 && terrain.sampleCarvedGroundSurfaceHeight(x + 4, z) <= y + 4
            && terrain.sampleCarvedGroundSurfaceHeight(x - 4, z) <= y + 4
            && terrain.sampleCarvedGroundSurfaceHeight(x, z + 4) <= y + 4
            && terrain.sampleCarvedGroundSurfaceHeight(x, z - 4) <= y + 4) return true;
        for (int[] ray : SKY_RAYS) {
            boolean clear = true;
            for (int step = 1; step <= 24 && clear; step++)
                clear = terrain.sampleCarvedGroundSurfaceHeight(x + ray[0] * step, z + ray[1] * step) <= y + step;
            if (clear && ++open >= 3) return true;
        }
        return false;
    }

    private static final class OutsideFeature extends RuntimeException {

        OutsideFeature() {
            super(null, null, false, false);
        }
    }

    private final class IslandWorld implements ModernDecorationEngine.World {

        private net.minecraft.world.World sourceWorld;
        private Map<Long, List<Site>> preparedReviewedSites = Map.of();
        private boolean appleOrchards;

        NativeFeatureCapture.Access nativeAccess() {
            return nativeAccess(HarvestCraftOrchards::encodeWrite);
        }

        industries.angelic.biomic.compat.biome.BopNativePond.HostedAccess nativeBopAccess(String habitat,
            BopNativeBiomes.Slot slot) {
            var delegate = nativeAccess();
            return new industries.angelic.biomic.compat.biome.BopNativePond.HostedAccess() {

                public Block block(int x, int y, int z) {
                    return delegate.block(x, y, z);
                }

                public int metadata(int x, int y, int z) {
                    return delegate.metadata(x, y, z);
                }

                public int heightValue(int x, int z) {
                    return delegate.heightValue(x, z);
                }

                public int minimumY() {
                    return delegate.minimumY();
                }

                public int maximumYExclusive() {
                    return delegate.maximumYExclusive();
                }

                public boolean immutableCarrier(int x, int y, int z, Block block, int metadata) {
                    return immutableCarrierRejection(x, y, z, block, metadata) == null;
                }

                public String immutableCarrierRejection(int x, int y, int z, Block block, int metadata) {
                    if (accepted.containsKey(x, y, z)) return "accepted-hosted-feature";
                    for (HostedPositionMap writes : transactions)
                        if (writes.containsKey(x, y, z)) return "buffered-hosted-feature";
                    var target = sites(x, z);
                    if (layer >= target.size()) return "absent-host-layer";
                    var site = target.get(layer);
                    if (!habitat.equals(site.biome) || y < Math.max(site.surface.bottomY, site.surface.floorY - 7)
                        || y >= site.surface.ceilingY) return "foreign-habitat-or-vertical-owner";
                    var raw = ModernStateCompatibility.resolve(rawState(x, y, z));
                    if (raw.getBlock() != block || raw.getMetadata() != metadata) return "hosted-raw-state-mismatch";
                    return physical.naturalCarrierRejection(x, y, z, block, metadata);
                }

                public boolean set(int x, int y, int z, Block block, int meta) {
                    // A lower floor must not receive plants meant for an overhead arch, or vice versa.
                    var target = sites(x, z);
                    if (layer >= target.size()) throw new OutsideFeature();
                    var site = target.get(layer);
                    int minimum = slot.kind() == BopNativeBiomes.Kind.POND
                        || slot.kind() == BopNativeBiomes.Kind.OASIS_BANK
                        || BopNativeBiomes.finalBatch(habitat)
                            && (BopNativeBiomes.shore(slot) || slot.kind() == BopNativeBiomes.Kind.SPLOTCH
                                || slot.kind() == BopNativeBiomes.Kind.WASTELAND_FORMATION
                                || slot.kind() == BopNativeBiomes.Kind.VOLCANO_SPOUT)
                        || BopNativeBiomes.nativeMudBanks(habitat) && BopNativeBiomes.shore(slot)
                            ? Math.max(site.surface.bottomY, site.surface.floorY - 7)
                            : slot.kind() == BopNativeBiomes.Kind.MOSS ? site.surface.bottomY
                                : Math.max(site.surface.bottomY, site.surface.floorY - site.soilLayers + 1);
                    if (y < minimum || y >= site.surface.ceilingY) throw new OutsideFeature();
                    return IslandWorld.this.setBlock(
                        x,
                        y,
                        z,
                        BopNativeBiomes
                            .encodeWrite(habitat, slot, block, meta, block(x, y - 1, z), metadata(x, y - 1, z)));
                }
            };
        }

        NativeFeatureCapture.Access nativeAccess(java.util.function.BiFunction<Block, Integer, String> encoder) {
            return new NativeFeatureCapture.Access() {

                @Override
                public Block block(int x, int y, int z) {
                    return IslandWorld.this.block(x, y, z);
                }

                @Override
                public int metadata(int x, int y, int z) {
                    checkColumn(x, z);
                    if (!terrain.layout()
                        .containsY(y)) return 0;
                    HostedPositionMap writes = HostedPositionMap.containing(transactions, x, y, z);
                    if (writes != null) {
                        return resolveHostedState(writes.get(x, y, z)).getMetadata();
                    }
                    if (accepted.containsKey(x, y, z)) {
                        return resolveHostedState(accepted.get(x, y, z)).getMetadata();
                    }
                    return physical.isMaterialized(x, y, z) ? physical.metadata(x, y, z)
                        : ModernStateCompatibility.resolve(rawState(x, y, z))
                            .getMetadata();
                }

                @Override
                public boolean set(int x, int y, int z, Block block, int metadata) {
                    return IslandWorld.this.setBlock(x, y, z, encoder.apply(block, metadata));
                }

                @Override
                public int heightValue(int x, int z) {
                    return IslandWorld.this.height("WORLD_SURFACE", x, z);
                }

                @Override
                public int minimumY() {
                    return nativeFeatureEnvelope.minimumY;
                }

                @Override
                public int maximumYExclusive() {
                    return nativeFeatureEnvelope.maximumYExclusive;
                }
            };
        }

        final OrigenFeatureRuntime.PhysicalPopulation physical;
        final int minX, minZ, maxX, maxZ;
        final int layer;
        final HostedPositionMap accepted = new HostedPositionMap();
        final Deque<HostedPositionMap> transactions = new ArrayDeque<>();
        final Deque<String> transactionHabitats = new ArrayDeque<>();
        String feature;
        final boolean traced = Integer.getInteger("biomic.traceHostedX") != null
            && Integer.getInteger("biomic.traceHostedZ") != null;

        void trace(Pos pos, String phase) {
            if (traced && feature != null
                && feature.contains(System.getProperty("biomic.traceHostedFeature", "redwoods_primary"))
                && Math.abs((long) pos.x - Integer.getInteger("biomic.traceHostedX")) <= 64
                && Math.abs((long) pos.z - Integer.getInteger("biomic.traceHostedZ")) <= 64)
                industries.angelic.biomic.Biomic.LOG
                    .info("Hosted trace {} layer={} {},{},{} {}", feature, layer, pos.x, pos.y, pos.z, phase);
        }

        /** Publishes the complete buffered proposal. No path commits a prefix. */
        boolean commit(HostedPositionMap writes, boolean nativeTree) {
            if (writes.isEmpty()) return true;
            // A tree rooted clear of a village or structure may still reach into it: then it goes whole, never cut. A
            // patch of plants is many plants: only those on a guarded cell stay away.
            if (guardedAnywhere(writes.keySet())) {
                if (bulkyFeature(feature)) {
                    rejected++;
                    return false;
                }
                writes.keySet()
                    .removeIf(p -> villageGuard.blocks(feature, false, p.x, p.y, p.z));
                if (writes.isEmpty()) return true;
            }
            Map<Pos, String> publication = writes;
            if (nativeTree) physical.commitNativeTree(writes);
            else publication.forEach((position, state) -> physical.setBlock(position.x, position.y, position.z, state));
            accepted.putAll(writes);
            return true;
        }

        private boolean guardedAnywhere(java.util.Set<Pos> positions) {
            if (villageGuard == null || snowCover(feature)) return false;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE,
                maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (Pos p : positions) {
                minX = Math.min(minX, p.x);
                maxX = Math.max(maxX, p.x);
                minY = Math.min(minY, p.y);
                maxY = Math.max(maxY, p.y);
                minZ = Math.min(minZ, p.z);
                maxZ = Math.max(maxZ, p.z);
            }
            int reach = Math.max(maxX - minX, maxZ - minZ) / 2 + 4;
            if (!villageGuard.near((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2, reach)) return false;
            for (Pos p : positions) if (villageGuard.blocks(feature, false, p.x, p.y, p.z)) return true;
            return false;
        }

        @Override
        public void recordPlacementCandidate(Pos pos, int depth, String phase) {
            if (traced) trace(
                pos,
                "depth=" + depth + " " + phase + (pos.y > 0 ? " below=" + blockState(pos.x, pos.y - 1, pos.z) : ""));
        }

        IslandWorld(OrigenFeatureRuntime.PhysicalPopulation physical, int x, int z, int layer) {
            this.physical = physical;
            this.layer = layer;
            minX = HostedAdapterEnvelope.minimum(x);
            minZ = HostedAdapterEnvelope.minimum(z);
            maxX = HostedAdapterEnvelope.maximum(x);
            maxZ = HostedAdapterEnvelope.maximum(z);
        }

        void checkColumn(int x, int z) {
            if (x < minX || x > maxX || z < minZ || z > maxZ) throw new OutsideFeature();
        }

        List<Site> sites(int x, int z) {
            checkColumn(x, z);
            var prepared = preparedReviewedSites.get(key(x, z));
            if (prepared != null) return prepared;
            return chunk(x >> 4, z >> 4).sites[(z & 15) * 16 + (x & 15)];
        }

        String rawState(int x, int y, int z) {
            if (!terrain.layout()
                .containsY(y)) return "minecraft:air";
            for (Site site : sites(x, z)) {
                int depth = site.surface.floorY - y;
                if (depth >= 0 && depth < site.soilLayers) {
                    String skin = skinHabitat(site, x, z);
                    if (skin != null) return soilState(site, skin, depth, x, z);
                }
            }
            return TerrainMaterialStates.state(
                chunk(x >> 4, z >> 4).materials[terrain.layout()
                    .index(x & 15, y, z & 15)]);
        }

        @Override
        public long worldSeed() {
            return seed;
        }

        @Override
        public int minBuildHeight() {
            return terrain.minimumEcologyY();
        }

        @Override
        public int maxBuildHeight() {
            return terrain.layout()
                .height();
        }

        @Override
        public int seaLevel() {
            return terrain.projection() == AuthoredVerticalProjections.shifted512() ? terrain.projection()
                .physicalSeaBoundary() : 63;
        }

        @Override
        public String blockState(int x, int y, int z) {
            checkColumn(x, z);
            if (!terrain.layout()
                .containsY(y)) return "minecraft:air";
            HostedPositionMap writes = HostedPositionMap.containing(transactions, x, y, z);
            if (writes != null) {
                return writes.get(x, y, z);
            }
            String state = accepted.get(x, y, z);
            if (state != null) {
                return state;
            }
            // Only synthesize the soil of a not-yet-created block. Equal block names do NOT prove that a
            // loaded block is raw terrain: Origen may have intentionally exposed stone during population.
            return physical.isMaterialized(x, y, z) ? physical.blockState(x, y, z) : rawState(x, y, z);
        }

        @Override
        public int height(String heightmap, int x, int z) {
            List<Site> sites = sites(x, z);
            if (sites.size() <= layer) return 0;
            Site site = sites.get(layer);
            boolean worldgen = heightmap.endsWith("_WG");
            int top = site.surface.ceilingY - 1;
            if (!worldgen) {
                // Nothing loaded or pending lies above the column's top filled section or the highest write.
                long bound = Math.max((long) physical.topCandidateY(x, z), pendingTopY());
                if (bound < top) top = (int) bound;
            }
            for (int y = top; y >= site.surface.bottomY; y--) {
                // Tall columns are mostly materialized air above the surface; skip it without building state strings.
                if (!worldgen && materializedAir(x, y, z)) continue;
                String state = worldgen ? rawState(x, y, z) : blockState(x, y, z);
                if (ModernSurfaceSystem.isAirState(state)) continue;
                Block actual = heightmap.endsWith("_WG") ? resolveHostedState(state).getBlock() : block(x, y, z);
                boolean leaves = actual instanceof net.minecraft.block.BlockLeaves
                    || ModernDecorationEngine.blockName(state)
                        .endsWith("_leaves");
                boolean motion = leaves || actual.getMaterial()
                    .blocksMovement();
                if (heightmap.startsWith("OCEAN_FLOOR") && !motion) continue;
                if (heightmap.startsWith("MOTION_BLOCKING") && !motion
                    && "minecraft:empty".equals(ModernBlockSemantics.fluidState(state))) continue;
                if ("MOTION_BLOCKING_NO_LEAVES".equals(heightmap) && leaves) continue;
                return y + 1;
            }
            return 0;
        }

        private int pendingTopY() {
            int top = accepted.maxY();
            for (HostedPositionMap writes : transactions) top = Math.max(top, writes.maxY());
            return top;
        }

        /** Exactly the cells blockState would report as plain air from the loaded world (no pending writes). */
        private boolean materializedAir(int x, int y, int z) {
            return terrain.layout()
                .containsY(y) && HostedPositionMap.containing(transactions, x, y, z) == null
                && accepted.get(x, y, z) == null
                && physical.isMaterialized(x, y, z)
                && physical.block(x, y, z) == net.minecraft.init.Blocks.air;
        }

        @Override
        public boolean allowsFeatureAt(String feature, Pos pos) {
            if (villageGuard != null && villageGuard.blocks(feature, bulkyFeature(feature), pos.x, pos.y, pos.z))
                return false;
            for (Site site : sites(pos.x, pos.z)) {
                if (pos.y > site.surface.floorY && pos.y < site.surface.ceilingY
                    && HostedVegetationPolicy.allowsFeature()) return true;
            }
            return false;
        }

        @Override
        public String biome(int x, int y, int z) {
            for (Site site : sites(x, z)) if (y >= site.surface.bottomY && y < site.surface.ceilingY) return site.biome;
            return "minecraft:ocean";
        }

        @Override
        public String noiseBiome(int x, int y, int z) {
            return biome(x, y, z);
        }

        @Override
        public String fluidState(int x, int y, int z) {
            return ModernBlockSemantics.fluidState(blockState(x, y, z));
        }

        private boolean cold(int x, int y, int z) {
            if (y <= terrain.minimumEcologyY() || y >= terrain.layout()
                .height() - 1) return false;
            if (!snowyHabitat(biome(x, y, z), x, z)) return false;
            int sourceY = terrain.projection() == AuthoredVerticalProjections.shifted512() ? terrain.projection()
                .toSourceBlockY(y)
                .orElseThrow(() -> new OutsideFeature())
                : industries.angelic.biomic.world.modern.ModernVerticalTransform.toModernY(y);
            return ModernSurfaceSystem.coldEnoughToSnow(biome(x, y, z), x, sourceY, z);
        }

        @Override
        public boolean shouldFreeze(int x, int y, int z, boolean edge) {
            if (!cold(x, y, z) || !ModernDecorationEngine.blockName(blockState(x, y, z))
                .equals("minecraft:water")) return false;
            if (!edge) return true;
            for (int[] offset : new int[][] { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } })
                if (!ModernDecorationEngine.blockName(blockState(x + offset[0], y, z + offset[1]))
                    .equals("minecraft:water")) return true;
            return false;
        }

        @Override
        public boolean shouldSnow(int x, int y, int z) {
            if (!cold(x, y, z) || !engine.hasPrecipitation(biome(x, y, z))) return false;
            String state = blockState(x, y, z);
            if (!ModernSurfaceSystem.isAirState(state) && !ModernDecorationEngine.blockName(state)
                .equals("minecraft:snow")) return false;
            String below = blockState(x, y - 1, z);
            if (engine.isBlockInTag("minecraft:snow_layer_cannot_survive_on", below)) return false;
            return engine.isBlockInTag("minecraft:snow_layer_can_survive_on", below)
                || below.startsWith("minecraft:snow[") && below.contains("layers=8")
                || hasSturdyFace(x, y - 1, z, "up");
        }

        private Block block(int x, int y, int z) {
            checkColumn(x, z);
            if (!terrain.layout()
                .containsY(y)) return net.minecraft.init.Blocks.air;
            HostedPositionMap writes = HostedPositionMap.containing(transactions, x, y, z);
            if (writes != null) {
                return resolveHostedState(writes.get(x, y, z)).getBlock();
            }
            if (accepted.containsKey(x, y, z)) {
                return resolveHostedState(accepted.get(x, y, z)).getBlock();
            }
            // Read registered structure/mod blocks directly. Inspecting an existing block is not
            // a request to synthesize it from the deliberately restricted feature-state palette.
            return physical.isMaterialized(x, y, z) ? physical.block(x, y, z)
                : ModernStateCompatibility.resolve(rawState(x, y, z))
                    .getBlock();
        }

        @Override
        public boolean isSolid(int x, int y, int z) {
            return block(x, y, z).getMaterial()
                .isSolid();
        }

        @Override
        public boolean hasSturdyFace(int x, int y, int z, String direction) {
            Block actual = block(x, y, z);
            return industries.angelic.biomic.world.modern.ModernBlockSemantics.legacyFullFace(actual);
        }

        @Override
        public boolean isReplaceable(int x, int y, int z) {
            String state = blockState(x, y, z);
            return ModernSurfaceSystem.isAirState(state) || ModernDecorationEngine.blockName(state)
                .equals("minecraft:water")
                || ModernDecorationEngine.blockName(state)
                    .equals("minecraft:lava")
                || block(x, y, z).getMaterial()
                    .isReplaceable();
        }

        @Override
        public boolean randomChance(int x, int y, int z, float chance) {
            return new ModernLegacyRandom(seed).forkPositional()
                .at(x, y, z)
                .nextFloat() < chance;
        }

        @Override
        public boolean intersectsStructure(int x, int y, int z, int radius, String structure) {
            return false;
        }

        @Override
        public List<Pos> carvingMask(String step, int x, int z) {
            return Collections.emptyList();
        }

        @Override
        public boolean canSurvive(String state, int x, int y, int z) {
            if (y <= terrain.minimumEcologyY() || !terrain.layout()
                .containsY(y)) return false;
            String block = ModernDecorationEngine.blockName(state);
            if (industries.angelic.biomic.world.modern.ModernCavePlantSupport.handles(state)) {
                String support = blockState(x, y - 1, z);
                return industries.angelic.biomic.world.modern.ModernCavePlantSupport
                    .test(state, support, block(x, y - 1, z).isOpaqueCube());
            }
            String below = blockState(x, y - 1, z);
            if (ModernAquaticPlantSupport.handles(state)) return ModernAquaticPlantSupport
                .test(state, below, blockState(x, y, z), hasSturdyFace(x, y - 1, z, "up"));
            if ("regions_unexplored:cattail".equals(block)) return ModernAquaticPlantSupport.cattail(
                state,
                below,
                blockState(x, y + 1, z),
                engine.isBlockInTag("regions_unexplored:cattail_can_survive_on", below));
            if ("regions_unexplored:flowering_lily_pad".equals(block))
                return ModernAquaticPlantSupport.floweringLily(below, blockState(x, y, z));
            if ("regions_unexplored:duckweed".equals(block) || "minecraft:lily_pad".equals(block))
                return ModernBlockSemantics.canDuckweedSurvive(below, fluidState(x, y - 1, z), fluidState(x, y, z));
            if ("minecraft:dead_bush".equals(block) || "minecraft:deadbush".equals(block)) {
                return HostedVegetationPolicy.legacyDeadBushGround(below);
            }
            Boolean support = groundCoverSupportAt(state, below, x, y, z);
            if (support != null) return support;
            if (block.endsWith("_leaf_litter")) return hasSturdyFace(x, y - 1, z, "up");
            if (block.startsWith("regions_unexplored:") && block.endsWith("_bioshroom")) {
                return block(x, y - 1, z).isOpaqueCube();
            }
            if ("minecraft:moss_carpet".equals(block)) return !ModernSurfaceSystem.isAirState(below);
            if ("minecraft:red_mushroom".equals(block) || "minecraft:brown_mushroom".equals(block)) {
                return engine.isBlockInTag("minecraft:mushroom_grow_block", below);
            }
            if ("minecraft:sugar_cane".equals(block)) {
                if ("minecraft:sugar_cane".equals(ModernDecorationEngine.blockName(below))) return true;
                if (!engine.isBlockInTag("minecraft:dirt", below) && !below.startsWith("minecraft:sand")) return false;
                return "minecraft:water".equals(fluidState(x - 1, y - 1, z))
                    || "minecraft:water".equals(fluidState(x + 1, y - 1, z))
                    || "minecraft:water".equals(fluidState(x, y - 1, z - 1))
                    || "minecraft:water".equals(fluidState(x, y - 1, z + 1));
            }
            if ("minecraft:cactus".equals(block)) {
                if (!below.startsWith("minecraft:sand") && !below.startsWith("minecraft:red_sand")
                    && !below.startsWith("minecraft:cactus")) return false;
                return !isSolid(x - 1, y, z) && !isSolid(x + 1, y, z) && !isSolid(x, y, z - 1) && !isSolid(x, y, z + 1);
            }
            return engine.isBlockInTag("minecraft:dirt", below)
                || "minecraft:farmland".equals(ModernDecorationEngine.blockName(below));
        }

        @Override
        public boolean setBlock(int x, int y, int z, String state) {
            checkColumn(x, z);
            int minimumY = mainland == null ? Math.max(FloatingIslandDensitySampler.MIN_Y, terrain.minimumEcologyY())
                : terrain.minimumEcologyY() + 1;
            if (y < minimumY || !terrain.layout()
                .containsY(y)) throw new OutsideFeature();
            if (intrinsicTerrain != null && intrinsicTerrain.blocksHostedEcology(x, y, z)) throw new OutsideFeature();
            // Enforce the complete feature envelope in air too; crossing a neighboring biome must
            // reject the transaction, not graft one biome's canopy onto another biome's trees.
            String targetHabitat = mainland == null ? biome(x, y, z) : mainland.sample(x, z);
            // Deep-sea flora must remain inside the admitted deep-water sites, not just the larger province.
            // This also checks air/water writes, so a feature cannot spill from the basin onto a shallow shelf.
            if (mainland != null && (HostedVegetationPolicy.minimumWaterDepth(transactionHabitats.peek()) > 0))
                targetHabitat = biome(x, y, z);
            if (!HostedLandmarkVegetation.sameHabitat(transactionHabitats.peek(), targetHabitat)) {
                if (traced) trace(new Pos(x, y, z), "biome-envelope-reject target=" + biome(x, y, z));
                throw new OutsideFeature();
            }
            byte raw = physical.rawMaterial(x, y, z);
            // A complete rooted island is terrain, including submerged shoulders.
            // Reject the whole coral/other proposal if it would replace its body
            // or original footing with a non-full-solid plant, AIR or fluid.
            // This is not the foreign-layout gate: own trees above the dry skin
            // and ordinary solid skins/ores remain eligible.
            if (mainland != null) {
                if (terrain.floatingArchetypeAt(x, y, z) != null) throw new OutsideFeature();
                if (raw != BiomicTerrainSampler.AIR && raw != BiomicTerrainSampler.WATER) {
                    if (raw == BiomicTerrainSampler.BEDROCK) throw new OutsideFeature();
                    List<Site> target = sites(x, z);
                    boolean valid = false;
                    for (Site site : target) if (y
                        >= Math.max(
                            site.surface.bottomY,
                            site.surface.floorY - HostedVegetationPolicy.foundationDepth(feature))
                        && y < site.surface.ceilingY) valid = true;
                    if (!valid) throw new OutsideFeature();
                }
            } else if (raw != BiomicTerrainSampler.AIR && raw != BiomicTerrainSampler.WATER
                && terrain.floatingArchetypeAt(x, y, z) == null) throw new OutsideFeature();
            if (transactions.isEmpty()) throw new IllegalStateException("Unbuffered hosted feature write");
            transactions.peek()
                .put(new Pos(x, y, z), state);
            return true;
        }
    }

    /** Physical capture bounds; source geometry is translated once by BopHeightSchedule, never clamped to 255. */
    static final class NativeFeatureEnvelope {

        final int minimumY, maximumYExclusive;

        private NativeFeatureEnvelope(int minimumY, int maximumYExclusive) {
            if (minimumY < 0 || maximumYExclusive <= minimumY || maximumYExclusive - minimumY > 512)
                throw new IllegalArgumentException("Invalid hosted native-feature envelope");
            this.minimumY = minimumY;
            this.maximumYExclusive = maximumYExclusive;
        }

        static NativeFeatureEnvelope forTerrain(HostedTerrainAccess terrain) {
            Objects.requireNonNull(terrain, "Hosted native-feature terrain");
            return new NativeFeatureEnvelope(
                terrain.minimumEcologyY(),
                terrain.layout()
                    .height());
        }
    }
}
