package industries.angelic.biomic.world.terrain;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.world.WorldgenContext;

/**
 * Treats each visible lobe of one family of Origen arches as one feature for biome-boundary admission.
 *
 * <p>
 * Origen's arches are infinite analytic density functions, so Terra cuts a lobe where biome blending reaches a
 * different
 * terrain graph: an arch ends in mid-air at its biome's border. Biomic keeps the authored arches in biome interiors but
 * rejects a complete lobe (one wave period of one arch strip) when any part of its visible four-block lattice
 * footprint,
 * including Terra's twelve-block blend stencil, leaves the arch's biome family. A rejected lobe's family is dropped
 * from
 * the terrain graph in those columns (OrigenTerrainRuntime#sampleBaseWithoutArches).
 * </p>
 *
 * <p>
 * Lush Loops (and its river) has one arch family; Stonegate Seas and Arctic Arches have two crossing ones, admitted
 * separately. The geometry follows the polished arch sampler (OrigenTerrainRuntime#polishArches): wave phase and height
 * from the strip's own cell, thickness measured across the wave.
 * </p>
 */
final class ArchLobeAdmission {

    /** One arch family: the authored constants of its sampler call. Bit: its place in the dropped-family mask. */
    record Shape(Set<OrigenBiomeProfile> family, OrigenBiomeProfile graph, int bit, double angle, double base,
        double height, double archHeight, double radius, double phaseScale, double verticalTranslation,
        double maxWidthNoise) {}

    // Exact constants from biomes/rearth/variants at Origen c01cd797.
    static final Shape LUSH_LOOPS = new Shape(
        EnumSet.of(OrigenBiomeProfile.LUSH_LOOPS, OrigenBiomeProfile.LOOPS_RIVER),
        OrigenBiomeProfile.LUSH_LOOPS,
        1,
        1.2D,
        75.0D,
        55.0D,
        3.5D,
        35.0D,
        50.0D,
        21.0D,
        0.8D);
    static final Shape STONEGATE_SEAS_A = new Shape(
        EnumSet.of(OrigenBiomeProfile.STONEGATE_SEAS),
        OrigenBiomeProfile.STONEGATE_SEAS,
        1,
        1.1D,
        55.0D,
        29.0D,
        3.0D,
        14.0D,
        25.0D,
        19.0D,
        0.8D);
    static final Shape STONEGATE_SEAS_B = new Shape(
        EnumSet.of(OrigenBiomeProfile.STONEGATE_SEAS),
        OrigenBiomeProfile.STONEGATE_SEAS,
        2,
        -0.55D,
        55.0D,
        29.0D,
        3.0D,
        14.0D,
        25.0D,
        15.0D,
        0.8D);
    static final Shape ARCTIC_ARCHES_A = new Shape(
        EnumSet.of(OrigenBiomeProfile.ARCTIC_ARCHES),
        OrigenBiomeProfile.ARCTIC_ARCHES,
        1,
        1.1D,
        55.0D,
        29.0D,
        4.0D,
        14.0D,
        25.0D,
        19.0D,
        0.0D);
    static final Shape ARCTIC_ARCHES_B = new Shape(
        EnumSet.of(OrigenBiomeProfile.ARCTIC_ARCHES),
        OrigenBiomeProfile.ARCTIC_ARCHES,
        2,
        -0.55D,
        55.0D,
        29.0D,
        4.0D,
        14.0D,
        25.0D,
        15.0D,
        0.0D);

    private static final int LATTICE_STEP = 4;
    private static final int BLEND_RADIUS = 12;
    private static final int MAX_COMPONENT_COLUMNS = 32_768;
    private static final int CACHE_SIZE = 131_072;
    private static final double WAVE_SCALE = 1.3D;
    /** Lowest arch value that is solid anywhere (one family alone needs more; near a crossing the other adds). */
    private static final double ARCH_SOLID_THRESHOLD = 0.3D;
    private static final double FOOTPRINT_SAFETY = 4.0D;

    private static final byte UNKNOWN = 0;
    private static final byte PASSTHROUGH = 1;
    private static final byte ADMITTED = 2;
    private static final byte REJECTED = 3;
    private static final byte POTENTIAL_FALSE = 1;
    private static final byte POTENTIAL_TRUE = 2;

    private final Shape shape;
    private final double cos, sin;
    private final long worldSeed;
    private final OrigenBiomePipeline pipeline;
    private final OrigenTerrainRuntime runtime;
    private final LongByteCache admissionCache = new LongByteCache(CACHE_SIZE);
    private final LongByteCache potentialCache = new LongByteCache(CACHE_SIZE);

    ArchLobeAdmission(Shape shape, long worldSeed, OrigenBiomePipeline pipeline, OrigenTerrainRuntime runtime) {
        if (shape == null || pipeline == null || runtime == null) {
            throw new IllegalArgumentException("Arch admission requires its shape, the Origen pipeline and runtime");
        }
        this.shape = shape;
        this.cos = Math.cos(shape.angle());
        this.sin = Math.sin(shape.angle());
        this.worldSeed = worldSeed;
        this.pipeline = pipeline;
        this.runtime = runtime;
    }

    Shape shape() {
        return shape;
    }

    boolean isAdmitted(int blockX, int blockZ) {
        int latticeX = Math.floorDiv(blockX, LATTICE_STEP) * LATTICE_STEP;
        int latticeZ = Math.floorDiv(blockZ, LATTICE_STEP) * LATTICE_STEP;
        long key = horizontalKey(latticeX, latticeZ);
        byte cached = admissionCache.get(key);
        if (cached != UNKNOWN) {
            return cached != REJECTED;
        }
        if (!isPotentialArchColumn(latticeX, latticeZ)) {
            admissionCache.put(key, PASSTHROUGH);
            return true;
        }
        return resolveComponent(latticeX, latticeZ);
    }

    boolean isPotentialArchColumn(int blockX, int blockZ) {
        int latticeX = Math.floorDiv(blockX, LATTICE_STEP) * LATTICE_STEP;
        int latticeZ = Math.floorDiv(blockZ, LATTICE_STEP) * LATTICE_STEP;
        long key = horizontalKey(latticeX, latticeZ);
        byte cached = potentialCache.get(key);
        if (cached != UNKNOWN) {
            return cached == POTENTIAL_TRUE;
        }
        boolean potential = samplePotentialArchColumn(latticeX, latticeZ);
        potentialCache.put(key, potential ? POTENTIAL_TRUE : POTENTIAL_FALSE);
        return potential;
    }

    private synchronized boolean resolveComponent(int startX, int startZ) {
        long startKey = horizontalKey(startX, startZ);
        byte cached = admissionCache.get(startKey);
        if (cached != UNKNOWN) {
            return cached != REJECTED;
        }

        long candidate = candidateId(startX, startZ);
        ArrayDeque<Long> pending = new ArrayDeque<Long>();
        Set<Long> component = new HashSet<Long>();
        Set<Long> inspectedProfiles = new HashSet<Long>();
        pending.add(startKey);
        component.add(startKey);
        boolean rejected = false;

        while (!pending.isEmpty()) {
            long key = pending.removeFirst();
            int blockX = keyX(key);
            int blockZ = keyZ(key);
            if (!rejected && blendStencilLeavesFamily(blockX, blockZ, inspectedProfiles)) {
                rejected = true;
            }

            if (component.size() >= MAX_COMPONENT_COLUMNS) {
                rejected = true;
                break;
            }
            for (int offsetX = -LATTICE_STEP; offsetX <= LATTICE_STEP; offsetX += LATTICE_STEP) {
                for (int offsetZ = -LATTICE_STEP; offsetZ <= LATTICE_STEP; offsetZ += LATTICE_STEP) {
                    if (offsetX == 0 && offsetZ == 0) {
                        continue;
                    }
                    int neighborX = blockX + offsetX;
                    int neighborZ = blockZ + offsetZ;
                    long neighborKey = horizontalKey(neighborX, neighborZ);
                    if (!component.contains(neighborKey) && candidateId(neighborX, neighborZ) == candidate
                        && isPotentialArchColumn(neighborX, neighborZ)) {
                        component.add(neighborKey);
                        pending.addLast(neighborKey);
                    }
                }
            }
        }

        byte result = rejected ? REJECTED : ADMITTED;
        for (Long key : component) {
            admissionCache.put(key.longValue(), result);
        }
        return !rejected;
    }

    private boolean blendStencilLeavesFamily(int blockX, int blockZ, Set<Long> inspected) {
        for (int offsetX = -BLEND_RADIUS; offsetX <= BLEND_RADIUS; offsetX += LATTICE_STEP) {
            for (int offsetZ = -BLEND_RADIUS; offsetZ <= BLEND_RADIUS; offsetZ += LATTICE_STEP) {
                int sampleX = blockX + offsetX;
                int sampleZ = blockZ + offsetZ;
                long key = horizontalKey(sampleX, sampleZ);
                if (inspected.add(key) && !shape.family()
                    .contains(pipeline.sampleProfile(sampleX, sampleZ))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean samplePotentialArchColumn(int blockX, int blockZ) {
        double transformedX = blockX * cos - blockZ * sin;
        double transformedZ = blockZ * cos - blockX * sin;
        double pillarRandom = runtime.sampleArchPillarRandom(shape.graph(), worldSeed, transformedZ);
        double phase = (transformedX * WAVE_SCALE + pillarRandom * shape.phaseScale()) / shape.radius();
        double amplitude = shape.height() * (pillarRandom * 0.3D + 1.0D);
        double archCenter = shape.base() + shape.verticalTranslation() - amplitude * Math.sin(phase);
        double mask = runtime.sampleArchMask(shape.graph(), worldSeed, transformedZ);
        double crossPenalty = (mask + 0.98D) * 190.0D * (pillarRandom * 0.4D + 1.0D);
        // Thickness is measured across the wave (see OrigenTerrainRuntime#polishArches): vertically it grows with the
        // slope.
        double slope = amplitude * WAVE_SCALE / shape.radius() * Math.cos(phase);
        double halfThickness = shape.archHeight() * (1.0D + shape.maxWidthNoise() - crossPenalty - ARCH_SOLID_THRESHOLD)
            * Math.sqrt(1.0D + slope * slope);
        if (halfThickness <= 0.0D) {
            return false;
        }
        double ground = shape.base() - 3.0D
            + runtime.sampleArchSurfaceBase(shape.graph(), worldSeed, blockX, blockZ) * 20.0D;
        return archCenter + halfThickness + FOOTPRINT_SAFETY >= ground;
    }

    private long candidateId(int blockX, int blockZ) {
        double transformedX = blockX * cos - blockZ * sin;
        double transformedZ = blockZ * cos - blockX * sin;
        double pillarRandom = runtime.sampleArchPillarRandom(shape.graph(), worldSeed, transformedZ);
        double phase = (transformedX * WAVE_SCALE + pillarRandom * shape.phaseScale()) / shape.radius();
        return (long) Math.floor((phase - Math.PI * 0.5D) / (Math.PI * 2.0D));
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
                throw new IllegalArgumentException("Arch admission cache size must be a power of two");
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
}
