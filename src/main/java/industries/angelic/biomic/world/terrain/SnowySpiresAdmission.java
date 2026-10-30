package industries.angelic.biomic.world.terrain;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.CellularSampler;
import industries.angelic.biomic.world.WorldgenContext;

/** Rejects an entire cellular Snowy Spires pillar when the biome blend would clip its footprint. */
final class SnowySpiresAdmission {

    private static final int LATTICE_STEP = 4;
    private static final int BLEND_RADIUS = 12;
    private static final int MIN_PILLAR_Y = 64;
    private static final int MAX_PILLAR_Y = 168;
    private static final int MAX_COMPONENT_COLUMNS = 8_192;
    private static final int CACHE_SIZE = 131_072;

    private static final byte UNKNOWN = 0;
    private static final byte PASSTHROUGH = 1;
    private static final byte ADMITTED = 2;
    private static final byte REJECTED = 3;
    private static final byte POTENTIAL_FALSE = 1;
    private static final byte POTENTIAL_TRUE = 2;

    private final long worldSeed;
    private final OrigenBiomePipeline pipeline;
    private final OrigenTerrainRuntime runtime;
    private final CellularSampler pillarCells;
    private final LongByteCache admissionCache = new LongByteCache(CACHE_SIZE);
    private final LongByteCache potentialCache = new LongByteCache(CACHE_SIZE);
    private final LongIntCache solidMaskCache = new LongIntCache(CACHE_SIZE);

    SnowySpiresAdmission(long worldSeed, OrigenBiomePipeline pipeline, OrigenTerrainRuntime runtime) {
        if (pipeline == null || runtime == null) {
            throw new IllegalArgumentException("Production pillar admission requires the Origen pipeline and runtime");
        }
        this.worldSeed = worldSeed;
        this.pipeline = pipeline;
        this.runtime = runtime;
        pillarCells = new CellularSampler();
        pillarCells.setFrequency(0.022D);
        pillarCells.setJitterModifier(1.0D);
        pillarCells.setDistanceFunction(CellularSampler.DistanceFunction.Euclidean);
        pillarCells.setReturnType(CellularSampler.ReturnType.CellValue);
    }

    boolean isAdmitted(int blockX, int blockZ) {
        int latticeX = Math.floorDiv(blockX, LATTICE_STEP) * LATTICE_STEP;
        int latticeZ = Math.floorDiv(blockZ, LATTICE_STEP) * LATTICE_STEP;
        long key = horizontalKey(latticeX, latticeZ);
        byte cached = admissionCache.get(key);
        if (cached != UNKNOWN) return cached != REJECTED;
        if (!isPotentialPillarColumn(latticeX, latticeZ)) {
            admissionCache.put(key, PASSTHROUGH);
            return true;
        }
        return resolveComponent(latticeX, latticeZ);
    }

    boolean isPotentialPillarColumn(int blockX, int blockZ) {
        int latticeX = Math.floorDiv(blockX, LATTICE_STEP) * LATTICE_STEP;
        int latticeZ = Math.floorDiv(blockZ, LATTICE_STEP) * LATTICE_STEP;
        long key = horizontalKey(latticeX, latticeZ);
        byte cached = potentialCache.get(key);
        if (cached != UNKNOWN) return cached == POTENTIAL_TRUE;
        boolean potential = samplePotentialPillarColumn(latticeX, latticeZ);
        potentialCache.put(key, potential ? POTENTIAL_TRUE : POTENTIAL_FALSE);
        return potential;
    }

    private synchronized boolean resolveComponent(int startX, int startZ) {
        long startKey = horizontalKey(startX, startZ);
        byte cached = admissionCache.get(startKey);
        if (cached != UNKNOWN) return cached != REJECTED;

        ArrayDeque<Long> pending = new ArrayDeque<Long>();
        Set<Long> component = new HashSet<Long>();
        pending.add(startKey);
        component.add(startKey);
        boolean rejected = false;

        while (!pending.isEmpty()) {
            long key = pending.removeFirst();
            int blockX = keyX(key);
            int blockZ = keyZ(key);
            if (!rejected && pillarFootprintLeavesBiome(blockX, blockZ)) rejected = true;
            if (component.size() >= MAX_COMPONENT_COLUMNS) {
                rejected = true;
                break;
            }
            for (int offsetX = -LATTICE_STEP; offsetX <= LATTICE_STEP; offsetX += LATTICE_STEP) {
                for (int offsetZ = -LATTICE_STEP; offsetZ <= LATTICE_STEP; offsetZ += LATTICE_STEP) {
                    if (offsetX == 0 && offsetZ == 0) continue;
                    int neighborX = blockX + offsetX;
                    int neighborZ = blockZ + offsetZ;
                    long neighborKey = horizontalKey(neighborX, neighborZ);
                    if (component.add(neighborKey)) {
                        boolean sameCell = sampleCell(neighborX, neighborZ) == sampleCell(blockX, blockZ);
                        boolean connectedCell = pillarColumnsTouch(blockX, blockZ, neighborX, neighborZ);
                        if (isPotentialPillarColumn(neighborX, neighborZ) && (sameCell || connectedCell)) {
                            pending.addLast(neighborKey);
                        } else component.remove(neighborKey);
                    }
                }
            }
        }

        byte result = rejected ? REJECTED : ADMITTED;
        for (Long key : component) admissionCache.put(key.longValue(), result);
        return !rejected;
    }

    private boolean pillarFootprintLeavesBiome(int blockX, int blockZ) {
        int solidMask = sampleSolidMask(blockX, blockZ);
        for (int bit = 0; bit < Integer.SIZE - 1; bit++) {
            if ((solidMask & 1 << bit) != 0
                && blendStencilLeavesBiome(blockX, MIN_PILLAR_Y + bit * LATTICE_STEP, blockZ)) return true;
        }
        return false;
    }

    private boolean blendStencilLeavesBiome(int blockX, int blockY, int blockZ) {
        for (int offsetX = -BLEND_RADIUS; offsetX <= BLEND_RADIUS; offsetX += LATTICE_STEP) {
            for (int offsetZ = -BLEND_RADIUS; offsetZ <= BLEND_RADIUS; offsetZ += LATTICE_STEP) {
                int sampleX = blockX + offsetX;
                int sampleZ = blockZ + offsetZ;
                if (sampleProfile(sampleX, blockY, sampleZ) != OrigenBiomeProfile.SNOWY_SPIRES) {
                    return true;
                }
            }
        }
        return false;
    }

    private OrigenBiomeProfile sampleProfile(int blockX, int blockY, int blockZ) {
        return runtime.extrude(pipeline.sampleProfile(blockX, blockZ), worldSeed, blockX, blockY, blockZ);
    }

    private long sampleCell(int blockX, int blockZ) {
        return Double.doubleToRawLongBits(pillarCells.noise(worldSeed, blockX, blockZ));
    }

    private boolean samplePotentialPillarColumn(int blockX, int blockZ) {
        return sampleSolidMask(blockX, blockZ) != 0;
    }

    private boolean pillarColumnsTouch(int leftX, int leftZ, int rightX, int rightZ) {
        int left = sampleSolidMask(leftX, leftZ);
        int right = sampleSolidMask(rightX, rightZ);
        return (left & (right | right << 1 | right >>> 1)) != 0;
    }

    private int sampleSolidMask(int blockX, int blockZ) {
        long key = horizontalKey(blockX, blockZ);
        int cached = solidMaskCache.get(key);
        if (cached != LongIntCache.UNKNOWN) return cached;
        int mask = 0;
        int bit = 0;
        for (int blockY = MIN_PILLAR_Y; blockY <= MAX_PILLAR_Y; blockY += LATTICE_STEP) {
            if (isPillarSolidAt(blockX, blockY, blockZ)) mask |= 1 << bit;
            bit++;
        }
        solidMaskCache.put(key, mask);
        return mask;
    }

    private boolean isPillarSolidAt(int blockX, int blockY, int blockZ) {
        double full = runtime.sampleBase(OrigenBiomeProfile.SNOWY_SPIRES, worldSeed, blockX, blockY, blockZ);
        double ground = runtime
            .sampleBaseWithoutSnowySpiresPillars(OrigenBiomeProfile.SNOWY_SPIRES, worldSeed, blockX, blockY, blockZ);
        return full > 0.0D && ground <= 0.0D;
    }

    private static long horizontalKey(int blockX, int blockZ) {
        return (long) blockX << 32 ^ blockZ & 0xFFFF_FFFFL;
    }

    private static int keyX(long key) {
        return (int) (key >> 32);
    }

    private static int keyZ(long key) {
        return (int) key;
    }

    private static final class LongByteCache {

        private final long[] keys;
        private final byte[] values;
        private final boolean[] occupied;
        private final int mask;

        private LongByteCache(int size) {
            if (Integer.bitCount(size) != 1) {
                throw new IllegalArgumentException("Pillar admission cache size must be a power of two");
            }
            keys = new long[size];
            values = new byte[size];
            occupied = new boolean[size];
            mask = size - 1;
        }

        private synchronized byte get(long key) {
            int index = slot(key);
            return occupied[index] && keys[index] == key ? values[index] : UNKNOWN;
        }

        private synchronized void put(long key, byte value) {
            int index = slot(key);
            keys[index] = key;
            values[index] = value;
            occupied[index] = true;
        }

        private int slot(long key) {
            return (int) WorldgenContext.mix64(key) & mask;
        }
    }

    private static final class LongIntCache {

        private static final int UNKNOWN = Integer.MIN_VALUE;

        private final long[] keys;
        private final int[] values;
        private final boolean[] occupied;
        private final int mask;

        private LongIntCache(int size) {
            if (Integer.bitCount(size) != 1) {
                throw new IllegalArgumentException("Pillar mask cache size must be a power of two");
            }
            keys = new long[size];
            values = new int[size];
            occupied = new boolean[size];
            mask = size - 1;
        }

        private synchronized int get(long key) {
            int index = slot(key);
            return occupied[index] && keys[index] == key ? values[index] : UNKNOWN;
        }

        private synchronized void put(long key, int value) {
            int index = slot(key);
            keys[index] = key;
            values[index] = value;
            occupied[index] = true;
        }

        private int slot(long key) {
            return (int) WorldgenContext.mix64(key) & mask;
        }
    }
}
