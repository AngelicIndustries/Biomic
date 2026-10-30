package industries.angelic.biomic.world;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.simplex.OpenSimplex2Sampler;
import industries.angelic.biomic.world.terrain.TartarusFeatures;
import industries.angelic.biomic.world.terrain.TerraPackChunkRasterizer;
import industries.angelic.biomic.world.terrain.gardens.GardensNetherDecorator;
import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

/**
 * The Tartarus Nether as pure functions of the seed, shared by the terrain workers. A chunk's blocks are its raster
 * plus the writes that reach it from the decorations of its 3x3 neighbourhood. Each chunk's decoration runs once,
 * against pure rasters and its own writes, so the result is seamless and independent of generation order.
 * Arrays use (x*16+z)*256+y.
 */
final class NetherTerrainSource {

    static final int HEIGHT = 256;
    /** Tartarus' features reach at most 9 blocks past their chunk; anything past one chunk is dropped and counted. */
    private static final int REACH = 16;
    /** Tartarus' lava sea fills open space up to this height. */
    private static final int SEA = 32;
    /** Hot biomes, whose lava-sea shores glow with magma. */
    private static final Set<String> HOT = Set.of(
        "CORRUPTED_WASTES",
        "FLAMING_HELLSCAPES",
        "TORRID_CRAGS",
        "BASALTIC_DELTAS",
        "SERRATED_MOUNTAINS",
        "LIFELESS_SPIRES");

    private final TerraPackChunkRasterizer rasterizer;
    private final TartarusFeatures features;
    private final NetherStates states;
    private final long seed;
    /** Pack id to Gardens biome key, for the Gardens biomes this world includes. */
    private final Map<String, String> gardens;
    /** Pack id to modern key of Regions Unexplored's Nether biomes, for those this world includes. */
    private final Map<String, String> ru;
    private final Cache<byte[]> rasters;
    private final Cache<Decoration> decorations;
    private final Cache<TerraPackChunkRasterizer.SourceChunk> snapshots;
    private final AtomicLong droppedWrites = new AtomicLong(), rasterRuns = new AtomicLong(),
        decorationRuns = new AtomicLong(), magmaCooled = new AtomicLong(), magmaShores = new AtomicLong();
    private final OpenSimplex2Sampler shorePatches = new OpenSimplex2Sampler(), keelDrips = new OpenSimplex2Sampler(),
        keelStrata = new OpenSimplex2Sampler();

    NetherTerrainSource(TerraPackChunkRasterizer rasterizer, TartarusFeatures features, NetherStates states, long seed,
        Map<String, String> gardens, Map<String, String> ru) {
        this.rasterizer = rasterizer;
        this.features = features;
        this.states = states;
        this.seed = seed;
        this.gardens = Map.copyOf(gardens);
        this.ru = Map.copyOf(ru);
        rasters = new Cache<>(256, this::raster);
        decorations = new Cache<>(128, this::decorate);
        snapshots = new Cache<>(16, rasterizer::snapshot);
        shorePatches.setFrequency(1 / 14D);
        shorePatches.setSalt(0x6d61676d61L);
        keelDrips.setFrequency(1 / 7D);
        keelDrips.setSalt(0x6b65656c64L);
        keelStrata.setFrequency(1 / 9D);
        keelStrata.setSalt(0x6b65656c73L);
    }

    /** The finished chunk: terrain and every feature write that lands in it. */
    byte[] chunk(int chunkX, int chunkZ) {
        byte[] raw = rasters.get(chunkX, chunkZ), out = raw.clone();
        for (int areaZ = chunkZ - 1; areaZ <= chunkZ + 1; areaZ++)
            for (int areaX = chunkX - 1; areaX <= chunkX + 1; areaX++) decorations.get(areaX, areaZ)
                .applyTo(out, chunkX - areaX, chunkZ - areaZ);
        keels(raw, out, chunkX, chunkZ);
        heat(out, chunkX, chunkZ);
        return out;
    }

    /** A lump at least this thick hangs a keel, over at least this much open space, and keeps this much of it. */
    private static final int KEEL_THICKNESS = 5, KEEL_GAP = 12, KEEL_CLEARANCE = 6;
    /** A keel hangs at most this many times the lump's thickness, and reaches most of that at this thickness. */
    private static final double KEEL_DEPTH = 1.3D, KEEL_SPAN = 40.0D;

    /**
     * Keels under the open Nether's floating rock (review, 2026-10-24: "the big meatballs ... would need a similar
     * treatment as the floating island bottoms"). Tartarus' 3D noise leaves round lumps hanging over the lava sea;
     * under
     * each a keel now hangs, deeper the thicker the lump is above it, so deepest under its middle and little at its rim
     * (as the overworld islands hang deeper the farther inside their coast), pulled into drip fingers and stepped in
     * three-block strata like their keels. The open space below caps it smoothly, so a keel never reaches the ground or
     * the lava. Only open cells fill (what grows below stays whole), in the lump's own rock. Read from each column's
     * own
     * pure terrain: chunk-local and order-independent.
     */
    private void keels(byte[] raw, byte[] out, int chunkX, int chunkZ) {
        net.minecraft.block.Block[] blocks = states.blocks();
        byte lava = states.id("minecraft:lava");
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int column = (x * 16 + z) * HEIGHT, y = HEIGHT - 1;
            // The roof hangs from the top of the world; floating runs lie between it and the floor.
            while (y > 0 && solid(blocks, raw[column + y])) y--;
            while (y > 0) {
                while (y > 0 && !solid(blocks, raw[column + y])) y--;
                int top = y;
                while (y > 0 && solid(blocks, raw[column + y])) y--;
                int bottom = y + 1, below = y;
                while (below > 0 && !solid(blocks, raw[column + below]) && raw[column + below] != lava) below--;
                int gap = bottom - 1 - below, thickness = top - bottom + 1;
                if (below <= 0 || gap < KEEL_GAP || thickness < KEEL_THICKNESS) continue;
                int worldX = (chunkX << 4) + x, worldZ = (chunkZ << 4) + z;
                // Growing with the square of the thickness: a pointed keel, little under the rim, most under the
                // middle.
                double room = Math.min(0.75D * (gap - KEEL_CLEARANCE), KEEL_DEPTH * thickness);
                double depth = room * Math.tanh(thickness * thickness / (KEEL_SPAN * KEEL_SPAN))
                    * (1.0D + 0.35D * keelDrips.noise(seed, worldX, worldZ));
                double keel = Math.floor((bottom - depth + 1.5D * keelStrata.noise(seed, worldX, worldZ)) / 3.0D)
                    * 3.0D;
                int tip = Math.max(below + KEEL_CLEARANCE, (int) keel);
                byte rock = raw[column + bottom];
                for (int fill = tip; fill < bottom; fill++)
                    if (out[column + fill] == NetherStates.AIR) out[column + fill] = rock;
            }
        }
    }

    private static boolean solid(net.minecraft.block.Block[] blocks, byte id) {
        return id != NetherStates.AIR && blocks[id & 255].isOpaqueCube();
    }

    /**
     * Magma belongs to lava, as in vanilla (its magma band hugs the lava sea, its deltas rim their pools): a magma
     * block
     * with no lava within two blocks cools to the rock around it, and in the hot biomes the exposed rock at the lava
     * sea's shore turns to magma in patches. Reads only this chunk and its neighbours' pure terrain: order-independent.
     */
    private void heat(byte[] out, int chunkX, int chunkZ) {
        byte magma = states.id("minecraft:magma_block"), lava = states.id("minecraft:lava");
        byte netherrack = states.id("minecraft:netherrack"), blackstone = states.id("minecraft:blackstone");
        Neighbourhood at = new Neighbourhood(out, chunkX, chunkZ);
        net.minecraft.block.Block[] blocks = states.blocks();
        long cooled = 0, shores = 0;
        for (int i = 0; i < out.length; i++) {
            if (out[i] != magma) continue;
            int y = i & 255, z = i >> 8 & 15, x = i >> 12;
            if (near(at, x, y, z, lava, 2)) continue;
            // The rock most of its faces touch, else netherrack.
            byte rock = netherrack;
            int best = 0;
            for (int f = 0; f < 6; f++) {
                byte id = at.get(x + FACES[f][0], y + FACES[f][1], z + FACES[f][2]);
                if (id == magma || id == lava || !blocks[id & 255].isOpaqueCube()) continue;
                int n = 0;
                for (int g = 0; g < 6; g++) if (at.get(x + FACES[g][0], y + FACES[g][1], z + FACES[g][2]) == id) n++;
                if (n > best) {
                    best = n;
                    rock = id;
                }
            }
            out[i] = rock;
            cooled++;
        }
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            String biome = null;
            for (int y = SEA - 4; y <= SEA + 2; y++) {
                int i = (x * 16 + z) * HEIGHT + y;
                if (out[i] != netherrack && out[i] != blackstone) continue;
                boolean open = false;
                for (int f = 0; f < 6 && !open; f++)
                    open = at.get(x + FACES[f][0], y + FACES[f][1], z + FACES[f][2]) == NetherStates.AIR;
                if (!open) continue;
                int reach = near(at, x, y, z, lava, 1) ? 1 : near(at, x, y, z, lava, 2) ? 2 : 0;
                if (reach == 0) continue;
                double patch = shorePatches.noise(seed, chunkX * 16 + x, chunkZ * 16 + z);
                if (patch < (reach == 1 ? -0.1 : 0.25)) continue;
                if (biome == null) biome = rasterizer.surfaceBiome(chunkX * 16 + x, chunkZ * 16 + z);
                if (!HOT.contains(biome)) break;
                out[i] = magma;
                shores++;
            }
        }
        magmaCooled.addAndGet(cooled);
        magmaShores.addAndGet(shores);
    }

    private static final int[][] FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    /** Whether {@code target} lies within {@code r} blocks (a cube) of the position. */
    private static boolean near(Neighbourhood at, int x, int y, int z, byte target, int r) {
        for (int dy = -r; dy <= r; dy++) for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) if (at.get(x + dx, y + dy, z + dz) == target) return true;
        return false;
    }

    /** A chunk's finished blocks, with its neighbours' pure terrain beyond its edges. */
    private final class Neighbourhood {

        private final byte[] own;
        private final int chunkX, chunkZ;
        private int lastX = Integer.MIN_VALUE, lastZ;
        private byte[] last;

        Neighbourhood(byte[] own, int chunkX, int chunkZ) {
            this.own = own;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        byte get(int x, int y, int z) {
            if (y < 0 || y >= HEIGHT) return NetherStates.AIR;
            if (x >= 0 && x < 16 && z >= 0 && z < 16) return own[(x * 16 + z) * HEIGHT + y];
            int cx = chunkX + (x >> 4), cz = chunkZ + (z >> 4);
            if (last == null || lastX != cx || lastZ != cz) {
                last = rasters.get(cx, cz);
                lastX = cx;
                lastZ = cz;
            }
            return last[((x & 15) * 16 + (z & 15)) * HEIGHT + y];
        }
    }

    /** The chunk's pure terrain, before features: state ids at (x*16+z)*256+y. Shared and cached; do not modify. */
    byte[] pureChunk(int chunkX, int chunkZ) {
        return rasters.get(chunkX, chunkZ);
    }

    /**
     * Block light from the chunk's own emitters (lava, glowstone, shroomlight, fire), spread as vanilla does: each step
     * loses the target's opacity, at least 1. Light crossing from neighbouring chunks arrives later through vanilla's
     * relight checks.
     */
    byte[] blockLight(byte[] materials) {
        byte[] emission = states.emission(), opacity = states.opacity();
        byte[] light = new byte[materials.length];
        var queue = new IntArrayFIFOQueue();
        for (int i = 0; i < materials.length; i++) {
            byte level = emission[materials[i] & 255];
            if (level > 0) {
                light[i] = level;
                queue.enqueue(i);
            }
        }
        while (!queue.isEmpty()) {
            int i = queue.dequeueInt(), level = light[i];
            if (level <= 1) continue;
            int y = i & 255, z = i >> 8 & 15, x = i >> 12;
            if (y > 0) spread(materials, opacity, light, queue, i - 1, level);
            if (y < HEIGHT - 1) spread(materials, opacity, light, queue, i + 1, level);
            if (z > 0) spread(materials, opacity, light, queue, i - HEIGHT, level);
            if (z < 15) spread(materials, opacity, light, queue, i + HEIGHT, level);
            if (x > 0) spread(materials, opacity, light, queue, i - 16 * HEIGHT, level);
            if (x < 15) spread(materials, opacity, light, queue, i + 16 * HEIGHT, level);
        }
        return light;
    }

    private static void spread(byte[] materials, byte[] opacity, byte[] light, IntArrayFIFOQueue queue, int i,
        int level) {
        int next = level - Math.max(1, opacity[materials[i] & 255]);
        if (next > light[i]) {
            light[i] = (byte) next;
            queue.enqueue(i);
        }
    }

    String report() {
        return "Tartarus: " + rasterRuns.get()
            + " rasters, "
            + decorationRuns.get()
            + " decorations, "
            + droppedWrites.get()
            + " feature writes beyond one chunk dropped, "
            + magmaCooled.get()
            + " magma blocks away from lava cooled to rock, "
            + magmaShores.get()
            + " shore blocks turned to magma";
    }

    private byte[] raster(int chunkX, int chunkZ) {
        rasterRuns.incrementAndGet();
        String[] raster = rasterizer.raster(chunkX, chunkZ);
        byte[] ids = new byte[raster.length];
        Map<String, Byte> seen = new IdentityHashMap<>();
        for (int i = 0; i < raster.length; i++) {
            Byte id = seen.get(raster[i]);
            if (id == null) seen.put(raster[i], id = states.id(raster[i]));
            ids[i] = id;
        }
        return ids;
    }

    private Decoration decorate(int areaX, int areaZ) {
        decorationRuns.incrementAndGet();
        var area = new Area(areaX, areaZ);
        features.decorate(area, areaX * 16, areaZ * 16);
        if (!gardens.isEmpty()) GardensNetherDecorator.decorate(area, seed, areaX, areaZ);
        if (!ru.isEmpty()) RuNetherDressing.decorate(area, seed, areaX, areaZ);
        return new Decoration(area.writes);
    }

    /** Writes keyed by position relative to the decorated chunk: x and z in -16..31, then y. */
    private static int key(int dx, int y, int dz) {
        return (dx + 16) << 14 | (dz + 16) << 8 | y;
    }

    /** One chunk's feature writes, final state per position. */
    private static final class Decoration {

        private final int[] keys;
        private final byte[] ids;

        Decoration(Int2ByteOpenHashMap writes) {
            keys = new int[writes.size()];
            ids = new byte[writes.size()];
            int i = 0;
            for (var entry : writes.int2ByteEntrySet()) {
                keys[i] = entry.getIntKey();
                ids[i++] = entry.getByteValue();
            }
        }

        /** Applies the writes that land in the chunk (offsetX, offsetZ) chunks away from the decorated one. */
        void applyTo(byte[] chunk, int offsetX, int offsetZ) {
            int minX = offsetX * 16, minZ = offsetZ * 16;
            for (int i = 0; i < keys.length; i++) {
                int x = (keys[i] >> 14) - 16 - minX, z = (keys[i] >> 8 & 63) - 16 - minZ;
                if (x < 0 || x > 15 || z < 0 || z > 15) continue;
                chunk[(x * 16 + z) * HEIGHT + (keys[i] & 255)] = ids[i];
            }
        }
    }

    /** One decoration's view: pure rasters under its own writes. */
    private final class Area
        implements TartarusFeatures.Region, GardensNetherDecorator.Region, RuNetherDressing.Region {

        private final int originX, originZ;
        private final Int2ByteOpenHashMap writes = new Int2ByteOpenHashMap();
        private final Map<String, Byte> written = new HashMap<>();
        private String[] names = states.states();
        private net.minecraft.block.Block[] blocks = states.blocks();
        private int rasterX = Integer.MIN_VALUE, rasterZ;
        private byte[] raster;

        Area(int areaX, int areaZ) {
            originX = areaX * 16;
            originZ = areaZ * 16;
            writes.defaultReturnValue((byte) -1);
        }

        private byte id(int x, int y, int z) {
            int dx = x - originX, dz = z - originZ;
            if (dx >= -REACH && dx < 16 + REACH && dz >= -REACH && dz < 16 + REACH) {
                byte written = writes.get(key(dx, y, dz));
                // -1 is the map's absent value; NetherStates never hands out id 255.
                if (written != (byte) -1) return written;
            }
            if (raster == null || rasterX != x >> 4 || rasterZ != z >> 4) {
                rasterX = x >> 4;
                rasterZ = z >> 4;
                raster = rasters.get(rasterX, rasterZ);
            }
            return raster[((x & 15) * 16 + (z & 15)) * HEIGHT + y];
        }

        private String name(byte id) {
            int index = id & 255;
            if (index >= names.length) names = states.states();
            return names[index];
        }

        @Override
        public String block(int x, int y, int z) {
            return name(id(x, y, z));
        }

        @Override
        public void set(int x, int y, int z, String state) {
            int dx = x - originX, dz = z - originZ;
            if (dx < -REACH || dx >= 16 + REACH || dz < -REACH || dz >= 16 + REACH) {
                if (droppedWrites.getAndIncrement() == 0)
                    Biomic.LOG.warn("A Tartarus feature wrote more than one chunk beyond its own at {},{},{}", x, y, z);
                return;
            }
            Byte id = written.get(state);
            if (id == null) written.put(state, id = states.id(state));
            writes.put(key(dx, y, dz), id.byteValue());
        }

        @Override
        public boolean solid(int x, int y, int z) {
            int index = id(x, y, z) & 255;
            if (index >= blocks.length) blocks = states.blocks();
            return blocks[index].isOpaqueCube();
        }

        @Override
        public double density(double x, double y, double z) {
            int chunkX = (int) Math.floor(x) >> 4, chunkZ = (int) Math.floor(z) >> 4;
            return snapshots.get(chunkX, chunkZ)
                .density(x - chunkX * 16, y, z - chunkZ * 16);
        }

        @Override
        public String biome(int x, int z) {
            return rasterizer.surfaceBiome(x, z);
        }

        @Override
        public net.minecraft.block.Block blockAt(int x, int y, int z) {
            if (y < 0 || y >= HEIGHT) return net.minecraft.init.Blocks.bedrock;
            int index = id(x, y, z) & 255;
            if (index >= blocks.length) blocks = states.blocks();
            return blocks[index];
        }

        @Override
        public int metadata(int x, int y, int z) {
            return y < 0 || y >= HEIGHT ? 0 : states.metadata()[id(x, y, z) & 255];
        }

        @Override
        public String profile(int x, int z) {
            return gardens.get(biome(x, z));
        }

        @Override
        public String ruBiome(int x, int z) {
            return ru.get(biome(x, z));
        }
    }

    @FunctionalInterface
    private interface ChunkFunction<T> {

        T compute(int chunkX, int chunkZ);
    }

    /** Bounded, compute-once cache: concurrent callers for the same chunk wait for the one computation. */
    private static final class Cache<T> {

        private final int capacity;
        private final ChunkFunction<T> function;
        private final LinkedHashMap<Long, FutureTask<T>> entries = new LinkedHashMap<>(64, .75F, true);

        Cache(int capacity, ChunkFunction<T> function) {
            this.capacity = capacity;
            this.function = function;
        }

        T get(int chunkX, int chunkZ) {
            long key = (long) chunkX << 32 ^ chunkZ & 0xFFFF_FFFFL;
            FutureTask<T> task;
            boolean owner = false;
            synchronized (entries) {
                task = entries.get(key);
                if (task == null) {
                    entries.put(key, task = new FutureTask<>(() -> function.compute(chunkX, chunkZ)));
                    owner = true;
                    if (entries.size() > capacity) {
                        var eldest = entries.keySet()
                            .iterator();
                        eldest.next();
                        eldest.remove();
                    }
                }
            }
            if (owner) task.run();
            try {
                return task.get();
            } catch (ExecutionException failure) {
                synchronized (entries) {
                    entries.remove(key, task);
                }
                throw new IllegalStateException(
                    "Tartarus generation failed at chunk " + chunkX + "," + chunkZ,
                    failure.getCause());
            } catch (InterruptedException interrupted) {
                Thread.currentThread()
                    .interrupt();
                throw new IllegalStateException("Interrupted while generating the Tartarus Nether", interrupted);
            }
        }
    }
}
