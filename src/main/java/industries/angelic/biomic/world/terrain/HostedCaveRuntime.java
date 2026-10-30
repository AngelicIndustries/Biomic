package industries.angelic.biomic.world.terrain;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;

import industries.angelic.biomic.biome.VerticalBiomeSampler;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.feature.RuDecorationStateResolver.Resolved;
import industries.angelic.biomic.world.modern.ModernBlockSemantics;
import industries.angelic.biomic.world.modern.ModernCavePlantSupport;
import industries.angelic.biomic.world.modern.ModernDecorationEngine;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationGraph;
import industries.angelic.biomic.world.modern.ModernFeatureEngine;
import industries.angelic.biomic.world.modern.ModernGroundCoverSupport;
import industries.angelic.biomic.world.modern.ModernStateCompatibility;
import industries.angelic.biomic.world.modern.ModernSurfaceSystem;
import industries.angelic.biomic.world.modern.ModernVerticalTransform;
import industries.angelic.biomic.world.modern.noise.ModernLegacyRandom;
import industries.angelic.biomic.world.modern.noise.ModernWorldgenRandom;

/** Exact native RU cave feature graphs on the existing Origen cavities, with transactional biome admission. */
public final class HostedCaveRuntime {

    /** Pre-ecology physical materials and the matching (unprojected) biome volume of one chunk. */
    public interface Source {

        byte[] materials(int chunkX, int chunkZ, BiomeVolume volume);

        BiomeVolume volume(int chunkX, int chunkZ);
    }

    private final long seed;
    private final Source source;
    private final industries.angelic.biomic.api.ChunkVerticalLayout layout;
    /** Physical Y = engine Y + offset; the engine sees [minY, maxY). Compressed 256 is 0/0/256. */
    private final int offset, minY, maxY, minTerrainY, maxTerrainY;
    private final HostedIslandSurfacePalette palette;
    private final ModernDecorationGraph graph = new ModernDecorationGraph();
    private final ModernFeatureEngine features = new ModernFeatureEngine();
    private final ModernDecorationEngine engine;
    private final boolean completeBioshrooms = false;
    private final Map<Long, CaveChunk> chunks = new LinkedHashMap<Long, CaveChunk>(96, .75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, CaveChunk> entry) {
            return size() > 96;
        }
    };

    public HostedCaveRuntime(WorldgenContext context, BiomicTerrainSampler terrain) {
        this(
            context,
            legacySource(terrain, new VerticalBiomeSampler(context)),
            new HostedIslandSurfacePalette(
                context.getSettings()
                    .getWorldSeed(),
                terrain::sampleCarvedGroundSurfaceHeight),
            industries.angelic.biomic.api.ChunkVerticalLayout.LEGACY_256,
            0,
            0,
            256,
            0,
            256);
    }

    /** Shifted512: RU's own modern coordinates (-64..320) sit one-to-one on physical Y-128, Origen at 64..447. */
    public HostedCaveRuntime(WorldgenContext context, HostedTerrainAccess hosted, VerticalBiomeSampler shiftedBiomes) {
        this(context, new Source() {

            @Override
            public byte[] materials(int cx, int cz, BiomeVolume volume) {
                return hosted.createMaterialArray(cx, cz);
            }

            @Override
            public BiomeVolume volume(int cx, int cz) {
                return shiftedBiomes.createVolume(cx, cz);
            }
        },
            new HostedIslandSurfacePalette(
                context.getSettings()
                    .getWorldSeed(),
                hosted::sampleCarvedGroundSurfaceHeight,
                AuthoredVerticalProjections.shifted512()),
            industries.angelic.biomic.api.ChunkVerticalLayout.EXTENDED_512,
            128,
            -64,
            320,
            64,
            448);
    }

    private static Source legacySource(BiomicTerrainSampler terrain, VerticalBiomeSampler vertical) {
        return new Source() {

            @Override
            public byte[] materials(int cx, int cz, BiomeVolume volume) {
                return terrain.createMaterialArray(cx, cz, volume);
            }

            @Override
            public BiomeVolume volume(int cx, int cz) {
                return vertical.createVolume(cx, cz);
            }
        };
    }

    private HostedCaveRuntime(WorldgenContext context, Source source, HostedIslandSurfacePalette palette,
        industries.angelic.biomic.api.ChunkVerticalLayout layout, int offset, int minY, int maxY, int minTerrainY,
        int maxTerrainY) {
        seed = context.getSettings()
            .getWorldSeed();
        this.source = source;
        this.palette = palette;
        this.layout = layout;
        this.offset = offset;
        this.minY = minY;
        this.maxY = maxY;
        this.minTerrainY = minTerrainY;
        this.maxTerrainY = maxTerrainY;
        engine = new ModernDecorationEngine(seed, (engine, type, config, world, random, pos) -> {
            CaveWorld view = (CaveWorld) world;
            String habitat = view.habitat(pos.x, pos.y, pos.z);
            if (habitat == null) return false;
            if (!view.frames.isEmpty() && !habitat.equals(view.frames.peek().biome)) return false;
            Frame frame = new Frame(habitat);
            view.frames.push(frame);
            boolean completed = false, success = false;
            try {
                success = features.place(engine, type, config, world, random, pos);
                completed = true;
            } catch (OutsideCave ignored) {
                // Discard complete structures at cave/biome boundaries, never leave half a mushroom or crystal.
            } finally {
                view.frames.pop();
            }
            if (HostedVegetationPolicy.keepsConfiguredWrites(type, completed, success)) {
                if (view.frames.isEmpty()) {
                    if (view.commit(frame.writes)) view.accepted.putAll(frame.writes);
                    else success = false;
                } else view.frames.peek().writes.putAll(frame.writes);
            }
            return success;
        });
    }

    public BiomeVolume surface(int cx, int cz, byte[] materials, Block[] blocks, byte[] metadata,
        BiomeVolume original) {
        CaveChunk chunk = new CaveChunk(cx, cz, materials, original);
        chunks.put(key(cx, cz), chunk);
        chunk.surfaces.forEach((index, state) -> {
            Resolved resolved = ModernStateCompatibility.resolve(state);
            blocks[index] = resolved.getBlock();
            metadata[index] = (byte) resolved.getMetadata();
        });
        return chunk.project(original);
    }

    /** Ecology ownership used to suppress the displaced Origen decoration, not its density/carving. */
    public boolean owns(int x, int y, int z) {
        return chunk(x >> 4, z >> 4).biome(x, y, z) != null;
    }

    public void populate(net.minecraft.world.World world, int cx, int cz, OrigenFeatureRuntime origen) {
        CaveChunk population = chunk(cx, cz);
        Set<String> habitats = population.biomes();
        if (habitats.isEmpty()) return;
        origen.withPhysicalPopulation(world, cx, cz, physical -> {
            CaveWorld view = new CaveWorld(physical, cx, cz);
            ModernWorldgenRandom random = new ModernWorldgenRandom();
            long decorationSeed = random.setDecorationSeed(seed, cx << 4, cz << 4);
            // Shared ore/deposit generation remains owned by Origen/EFR, not repeated once per cave habitat.
            for (int step = 0; step < ModernDecorationEngine.DECORATION_STEPS; step++) {
                if (step == 6) continue;
                List<String> order = graph.featureOrder(step);
                for (String feature : graph.orderedFeatures(habitats, step)) {
                    if (!feature.startsWith("regions_unexplored:")) continue;
                    random.setFeatureSeed(decorationSeed, order.indexOf(feature), step);
                    runPlacedFeature(
                        () -> engine.place(
                            new com.google.gson.JsonPrimitive(feature),
                            feature,
                            view,
                            random,
                            new Pos(cx << 4, minY, cz << 4)));
                }
            }
        });
    }

    /**
     * Placement modifiers run before the configured executor's transaction frame. Native envelope
     * refusal stops this placed-feature stream, retaining any earlier complete configured commits.
     */
    static boolean runPlacedFeature(Runnable placement) {
        try {
            placement.run();
            return true;
        } catch (OutsideCave outside) {
            throw outside;
        }
    }

    private CaveChunk chunk(int cx, int cz) {
        long key = key(cx, cz);
        CaveChunk found = chunks.get(key);
        if (found != null) return found;
        BiomeVolume volume = source.volume(cx, cz);
        found = new CaveChunk(cx, cz, source.materials(cx, cz, volume), volume);
        chunks.put(key, found);
        return found;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private final class CaveChunk {

        final byte[] materials;
        final HostedCaveHabitats habitats;
        final Map<Integer, String> surfaces;

        CaveChunk(int cx, int cz, byte[] materials, BiomeVolume original) {
            surfaces = new HashMap<>();
            this.materials = materials;
            habitats = new HostedCaveHabitats(original, materials);
            if (biomes().isEmpty()) return;
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                for (int y = minTerrainY + 4; y < maxTerrainY - 2; y++) {
                    int index = layout.index(x, y, z);
                    // The source cave palette owns natural substrate, not fluid, bedrock, ores or structures.
                    // In particular old moss/terracotta floors must not block a new RU cave's native soil.
                    if (!naturalCaveSubstrate(materials[index])) continue;
                    String biome = biome(x, y, z);
                    if (biome == null) continue;
                    byte above = materials[layout.index(x, y + 1, z)];
                    boolean floor = above == BiomicTerrainSampler.AIR || above == BiomicTerrainSampler.WATER;
                    int water = Integer.MIN_VALUE;
                    if (above == BiomicTerrainSampler.WATER) {
                        water = y + 1;
                        while (water < maxTerrainY
                            && materials[layout.index(x, water, z)] == BiomicTerrainSampler.WATER) water++;
                    }
                    String state = floor ? palette.caveState(biome, true, (cx << 4) + x, y, (cz << 4) + z, water)
                        : null;
                    if (state == null) state = (offset == 0 ? ModernVerticalTransform.toModernY(y) : y - offset) < 0
                        ? "minecraft:deepslate[axis=y]"
                        : "minecraft:stone";
                    surfaces.put(index, state);
                }
            }
        }

        String biome(int x, int y, int z) {
            return habitats.biome(x, y, z);
        }

        Set<String> biomes() {
            return habitats.biomes();
        }

        BiomeVolume project(BiomeVolume original) {
            return habitats.project(original);
        }

        /** Physical Y. */
        String state(int x, int y, int z) {
            if (y < 0 || y >= layout.height()) return "minecraft:air";
            int index = layout.index(x & 15, y, z & 15);
            String state = surfaces.get(index);
            return state == null ? TerrainMaterialStates.state(materials[index]) : state;
        }
    }

    static boolean naturalCaveSubstrate(byte material) {
        if (HostedSurfaceSkin.soil(material)) return true;
        return switch (material) {
            case BiomicTerrainSampler.STONE, BiomicTerrainSampler.DEEPSLATE, BiomicTerrainSampler.DRIPSTONE_BLOCK, BiomicTerrainSampler.CALCITE, BiomicTerrainSampler.TUFF, BiomicTerrainSampler.TERRACOTTA, BiomicTerrainSampler.RED_TERRACOTTA, BiomicTerrainSampler.ORANGE_TERRACOTTA, BiomicTerrainSampler.BROWN_TERRACOTTA, BiomicTerrainSampler.LIGHT_GRAY_TERRACOTTA, BiomicTerrainSampler.BLUE_TERRACOTTA, BiomicTerrainSampler.CYAN_TERRACOTTA, BiomicTerrainSampler.PINK_TERRACOTTA, BiomicTerrainSampler.WHITE_TERRACOTTA, BiomicTerrainSampler.YELLOW_TERRACOTTA, BiomicTerrainSampler.LIGHT_BLUE_TERRACOTTA, BiomicTerrainSampler.LIME_TERRACOTTA, BiomicTerrainSampler.RED_CONCRETE, BiomicTerrainSampler.BROWN_CONCRETE, BiomicTerrainSampler.YELLOW_CONCRETE, BiomicTerrainSampler.WHITE_CONCRETE_POWDER, BiomicTerrainSampler.CLAY, BiomicTerrainSampler.GRAVEL, BiomicTerrainSampler.SANDSTONE, BiomicTerrainSampler.RED_SANDSTONE, BiomicTerrainSampler.SMOOTH_SANDSTONE, BiomicTerrainSampler.BASALT, BiomicTerrainSampler.SMOOTH_BASALT, BiomicTerrainSampler.BLACKSTONE, BiomicTerrainSampler.MOSSY_COBBLESTONE, BiomicTerrainSampler.COBBLESTONE, BiomicTerrainSampler.ANDESITE, BiomicTerrainSampler.DIORITE, BiomicTerrainSampler.GRANITE -> true;
            default -> false;
        };
    }

    private static final class Frame {

        final String biome;
        final Map<Pos, String> writes = new LinkedHashMap<>();

        Frame(String biome) {
            this.biome = biome;
        }
    }

    static final class OutsideCave extends RuntimeException {

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    private final class CaveWorld implements ModernDecorationEngine.World {

        final OrigenFeatureRuntime.PhysicalPopulation physical;
        final int minX, minZ, maxX, maxZ;
        final Deque<Frame> frames = new ArrayDeque<>();
        final Map<Pos, String> accepted = new HashMap<>();

        CaveWorld(OrigenFeatureRuntime.PhysicalPopulation physical, int cx, int cz) {
            this.physical = physical;
            minX = (cx << 4) - 32;
            minZ = (cz << 4) - 32;
            maxX = (cx << 4) + 63;
            maxZ = (cz << 4) + 63;
        }

        void check(int x, int z) {
            if (x < minX || z < minZ || x > maxX || z > maxZ) throw new OutsideCave();
        }

        private CaveChunk ecologyChunk(int x, int z) {
            return chunk(x >> 4, z >> 4);
        }

        String habitat(int x, int y, int z) {
            check(x, z);
            return ecologyChunk(x, z).biome(x, physicalY(y), z);
        }

        int physicalY(int y) {
            return y + offset;
        }

        boolean commit(Map<Pos, String> writes) {
            writes.forEach((p, s) -> physical.setBlock(p.x, physicalY(p.y), p.z, s));
            return true;
        }

        @Override
        public long worldSeed() {
            return seed;
        }

        @Override
        public int minBuildHeight() {
            return minY;
        }

        @Override
        public int maxBuildHeight() {
            return maxY;
        }

        @Override
        public int seaLevel() {
            return 63;
        }

        @Override
        public String biome(int x, int y, int z) {
            String value = habitat(x, y, z);
            return value == null ? "minecraft:plains" : value;
        }

        @Override
        public String noiseBiome(int x, int y, int z) {
            return biome(x, y, z);
        }

        @Override
        public String blockState(int x, int y, int z) {
            check(x, z);
            if (y < minY || y >= maxY) return "minecraft:air";
            Pos pos = new Pos(x, y, z);
            for (Frame frame : frames) if (frame.writes.containsKey(pos)) return frame.writes.get(pos);
            if (accepted.containsKey(pos)) return accepted.get(pos);
            int py = physicalY(y);
            return physical.isMaterialized(x, py, z) ? physical.blockState(x, py, z)
                : ecologyChunk(x, z).state(x, py, z);
        }

        @Override
        public boolean setBlock(int x, int y, int z, String state) {
            String habitat = habitat(x, y, z);
            if (frames.isEmpty() || habitat == null
                || !habitat.equals(frames.peek().biome)
                || ModernDecorationEngine.blockName(blockState(x, y, z))
                    .equals("minecraft:bedrock"))
                throw new OutsideCave();
            String before = blockState(x, y, z);
            if (HostedCaveEcology.protectedStructureState(before)) throw new OutsideCave();
            frames.peek().writes.put(new Pos(x, y, z), state);
            return !before.equals(state);
        }

        @Override
        public int height(String type, int x, int z) {
            for (int y = maxY - 1; y >= minY; y--) {
                String state = type.endsWith("_WG") ? ecologyChunk(x, z).state(x, physicalY(y), z)
                    : blockState(x, y, z);
                if (ModernSurfaceSystem.isAirState(state)) continue;
                Block block = type.endsWith("_WG") ? ModernStateCompatibility.resolve(state)
                    .getBlock() : block(x, y, z);
                boolean motion = ModernDecorationEngine.blockName(state)
                    .endsWith("_leaves")
                    || block.getMaterial()
                        .blocksMovement();
                if (type.startsWith("OCEAN_FLOOR") && !motion) continue;
                if (type.startsWith("MOTION_BLOCKING") && !motion && fluidState(x, y, z).equals("minecraft:empty"))
                    continue;
                if (type.equals("MOTION_BLOCKING_NO_LEAVES") && ModernDecorationEngine.blockName(state)
                    .endsWith("_leaves")) continue;
                return y + 1;
            }
            return minY;
        }

        @Override
        public String fluidState(int x, int y, int z) {
            return ModernBlockSemantics.fluidState(blockState(x, y, z));
        }

        private Block block(int x, int y, int z) {
            check(x, z);
            if (y < minY || y >= maxY) return net.minecraft.init.Blocks.air;
            Pos pos = new Pos(x, y, z);
            for (Frame frame : frames)
                if (frame.writes.containsKey(pos)) return ModernStateCompatibility.resolve(frame.writes.get(pos))
                    .getBlock();
            if (accepted.containsKey(pos)) return ModernStateCompatibility.resolve(accepted.get(pos))
                .getBlock();
            // Existing structures/mod blocks are real registered blocks. They need not be writable
            // feature states just for a cave scanner to inspect their material/support.
            int py = physicalY(y);
            return physical.isMaterialized(x, py, z) ? physical.block(x, py, z)
                : ModernStateCompatibility.resolve(ecologyChunk(x, z).state(x, py, z))
                    .getBlock();
        }

        @Override
        public boolean isSolid(int x, int y, int z) {
            return block(x, y, z).getMaterial()
                .isSolid();
        }

        @Override
        public boolean hasSturdyFace(int x, int y, int z, String face) {
            Block block = block(x, y, z);
            return block instanceof net.minecraft.block.BlockLeaves || block.getMaterial()
                .isSolid();
        }

        @Override
        public boolean isReplaceable(int x, int y, int z) {
            String state = ModernDecorationEngine.blockName(blockState(x, y, z));
            return ModernSurfaceSystem.isAirState(state) || state.equals("minecraft:water")
                || state.equals("minecraft:lava")
                || block(x, y, z).getMaterial()
                    .isReplaceable();
        }

        @Override
        public void requireBioshroomClearance(int x, int y, int z) {
            if (!completeBioshrooms) return;
            check(x, z);
            if (y < minY || y >= maxY) throw new OutsideCave();
            Pos pos = new Pos(x, y, z);
            // Cross/stem calls revisit their own freshly staged geometry; that is not a host obstruction.
            for (Frame frame : frames) if (frame.writes.containsKey(pos)) return;
            if (!isReplaceable(x, y, z)) throw new OutsideCave();
        }

        @Override
        public boolean canSurvive(String state, int x, int y, int z) {
            if (y < minY + 1 || y >= maxY) return false;
            String below = blockState(x, y - 1, z);
            boolean opaque = block(x, y - 1, z).isOpaqueCube();
            if (ModernCavePlantSupport.handles(state)) return ModernCavePlantSupport.test(state, below, opaque);
            Boolean ground = ModernGroundCoverSupport.test(state, below, engine::isBlockInTag);
            if (ground != null) return ground;
            String block = ModernDecorationEngine.blockName(state);
            if (block.startsWith("regions_unexplored:") && block.endsWith("_bioshroom")) return opaque;
            if (block.equals("minecraft:moss_carpet")) return !ModernSurfaceSystem.isAirState(below);
            if (block.equals("minecraft:red_mushroom") || block.equals("minecraft:brown_mushroom")) return opaque;
            throw new IllegalStateException("Unaudited native cave plant survival: " + state);
        }

        @Override
        public boolean randomChance(int x, int y, int z, float chance) {
            return new ModernLegacyRandom(seed).forkPositional()
                .at(x, y, z)
                .nextFloat() < chance;
        }

        @Override
        public boolean shouldFreeze(int x, int y, int z, boolean edge) {
            return false;
        }

        @Override
        public boolean shouldSnow(int x, int y, int z) {
            return false;
        }

        @Override
        public boolean intersectsStructure(int x, int y, int z, int radius, String structure) {
            return false;
        }

        @Override
        public List<Pos> carvingMask(String step, int cx, int cz) {
            return Collections.emptyList();
        }
    }
}
