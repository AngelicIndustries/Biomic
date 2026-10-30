package industries.angelic.biomic.world;

import java.util.LinkedHashMap;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import net.minecraft.world.gen.structure.MapGenNetherBridge;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

import industries.angelic.biomic.config.StructureConfig;

/**
 * Vanilla Nether fortresses in the Tartarus Nether's open caverns. One region per {@code spacing}² chunks, of which
 * {@code chance} get a fortress, its start in the middle half of the region as vanilla places it. The layout is
 * vanilla's; its height is where the largest share of its pieces' volume is open cavern air in the pure Tartarus
 * terrain, so bridges span caverns instead of tunnelling through rock, above the lava sea and below the roof. Plans
 * are pure, cached per region, and made by the terrain workers ahead of the server thread.
 */
final class TartarusFortresses extends MapGenNetherBridge {

    /** A planned fortress: its start chunk and the lowest Y of its bounds. */
    private record Plan(int chunkX, int chunkZ, int minY) {}

    private static final Plan NONE = new Plan(0, 0, 0);
    /** Tartarus' lava sea is at 32: bridges keep this far above it. */
    private static final int LOWEST = 40;
    /** Headroom kept under the roof of the world. */
    private static final int ROOF = 24;
    private static final int PLAN_CACHE_SIZE = 256;
    private static final long CHANCE_SALT = 0x7A57A2F0B1DEL;

    private final NetherTerrainSource terrain;
    private final long seed;
    private final StructureConfig.Fortresses settings = StructureConfig.get().fortresses;
    private final LinkedHashMap<Long, FutureTask<Plan>> plans = new LinkedHashMap<>(64, .75F, true);

    TartarusFortresses(NetherTerrainSource terrain, long seed) {
        this.terrain = terrain;
        this.seed = seed;
    }

    /** Plans the regions whose fortress the server may ask about around this chunk. Worker-safe. */
    void prepare(int chunkX, int chunkZ) {
        if (!settings.enabled()) return;
        int spacing = settings.spacingChunks();
        for (int rx = Math.floorDiv(chunkX - range, spacing); rx <= Math.floorDiv(chunkX + range, spacing); rx++)
            for (int rz = Math.floorDiv(chunkZ - range, spacing); rz <= Math.floorDiv(chunkZ + range, spacing); rz++)
                plan(rx, rz);
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
        if (!settings.enabled()) return false;
        Plan plan = plan(
            Math.floorDiv(chunkX, settings.spacingChunks()),
            Math.floorDiv(chunkZ, settings.spacingChunks()));
        return plan != NONE && plan.chunkX == chunkX && plan.chunkZ == chunkZ;
    }

    /**
     * Vanilla's start, from the random state vanilla's generator holds for this chunk, lifted to its planned height.
     * A fresh vanilla random rather than {@code rand}, which Hodgepodge swaps for a generator with other numbers: the
     * layout must be the one whose height was planned.
     */
    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        Plan plan = plan(
            Math.floorDiv(chunkX, settings.spacingChunks()),
            Math.floorDiv(chunkZ, settings.spacingChunks()));
        StructureStart start = new MapGenNetherBridge.Start(worldObj, structureRandom(chunkX, chunkZ), chunkX, chunkZ);
        lift(start, plan.minY - start.getBoundingBox().minY);
        return start;
    }

    private Plan plan(int regionX, int regionZ) {
        long key = (long) regionX << 32 ^ regionZ & 0xFFFF_FFFFL;
        FutureTask<Plan> task;
        boolean owner = false;
        synchronized (plans) {
            task = plans.get(key);
            if (task == null) {
                plans.put(key, task = new FutureTask<>(() -> compute(regionX, regionZ)));
                owner = true;
                if (plans.size() > PLAN_CACHE_SIZE) {
                    var eldest = plans.keySet()
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
            synchronized (plans) {
                plans.remove(key, task);
            }
            throw new IllegalStateException(
                "Fortress planning failed for region " + regionX + "," + regionZ,
                failure.getCause());
        } catch (InterruptedException interrupted) {
            Thread.currentThread()
                .interrupt();
            throw new IllegalStateException("Interrupted while planning a fortress", interrupted);
        }
    }

    private Plan compute(int regionX, int regionZ) {
        int spacing = settings.spacingChunks();
        Random region = new Random(
            WorldgenContext.mix64(seed ^ regionX * 0x9E3779B97F4A7C15L ^ regionZ * 0xC2B2AE3D27D4EB4FL ^ CHANCE_SALT));
        if (region.nextDouble() >= settings.chance()) return NONE;
        // As vanilla: the start chunk in the middle half of the region (4..11 of 16).
        int chunkX = regionX * spacing + spacing / 4 + region.nextInt(Math.max(1, spacing / 2));
        int chunkZ = regionZ * spacing + spacing / 4 + region.nextInt(Math.max(1, spacing / 2));
        StructureStart start = new MapGenNetherBridge.Start(null, structureRandom(chunkX, chunkZ), chunkX, chunkZ);
        return new Plan(chunkX, chunkZ, openestHeight(start));
    }

    /**
     * The lowest Y for the fortress's bounds where the most of its pieces' volume is open air: each piece is judged
     * on its own centre column of the pure terrain, weighted by its footprint. Ties go to the height nearest
     * vanilla's (48..70).
     */
    private int openestHeight(StructureStart start) {
        StructureBoundingBox bounds = start.getBoundingBox();
        int height = bounds.getYSize(), lowest = LOWEST, highest = NetherTerrainSource.HEIGHT - ROOF - height;
        if (highest < lowest) return lowest;
        double[] score = new double[highest - lowest + 1];
        for (Object component : start.getComponents()) {
            StructureBoundingBox box = ((StructureComponent) component).getBoundingBox();
            int x = (box.minX + box.maxX) >> 1, z = (box.minZ + box.maxZ) >> 1;
            byte[] chunk = terrain.pureChunk(x >> 4, z >> 4);
            int column = ((x & 15) * 16 + (z & 15)) * NetherTerrainSource.HEIGHT;
            double weight = (double) box.getXSize() * box.getZSize() / box.getYSize();
            int below = box.minY - bounds.minY, above = box.maxY - bounds.minY;
            for (int base = lowest; base <= highest; base++) {
                int open = 0;
                for (int y = base + below; y <= base + above; y++) if (chunk[column + y] == NetherStates.AIR) open++;
                score[base - lowest] += weight * open;
            }
        }
        int best = lowest;
        for (int base = lowest; base <= highest; base++) {
            double difference = score[base - lowest] - score[best - lowest];
            if (difference > 1e-9 || difference > -1e-9 && Math.abs(base - 56) < Math.abs(best - 56)) best = base;
        }
        return best;
    }

    /** The random state vanilla's structure generator holds when it builds the start in this chunk. */
    private Random structureRandom(int chunkX, int chunkZ) {
        Random random = new Random(seed);
        long xMultiplier = random.nextLong();
        long zMultiplier = random.nextLong();
        random.setSeed(chunkX * xMultiplier ^ chunkZ * zMultiplier ^ seed);
        random.nextInt();
        return random;
    }

    private static void lift(StructureStart start, int dy) {
        if (dy == 0) return;
        start.getBoundingBox()
            .offset(0, dy, 0);
        for (Object component : start.getComponents()) ((StructureComponent) component).getBoundingBox()
            .offset(0, dy, 0);
    }
}
