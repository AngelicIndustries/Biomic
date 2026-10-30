package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.world.WorldgenContext;

/**
 * The shape of a floating island column: one span, its top and bottom pure functions of x and z.
 * <ul>
 * <li>A flat deck: the outer three blocks round off by up to two (the grass lip), gentle hills further in.</li>
 * <li>A short vertical plate under the rim (soil, then the first rock), then the keel: deeper the farther a column
 * lies inside the coast, pulled into drip fingers, with cones hanging from seeded points under the island's broad
 * parts. So the underside is about as deep as the island is wide, its tip off centre, big islands hang several
 * lobes.</li>
 * <li>The flank is finished as ragged three-block ledges or as hanging rock prisms of different lengths.</li>
 * </ul>
 * Modelled on the general look of hand-built fantasy sky islands, not on any of their data. Strictly chunk-local:
 * every input is a pure function of position, the cluster, or seeded points whose depth inside the coast the caller
 * computes from the same pure plan.
 */
final class IslandKeel {

    /**
     * How an island family hangs: how ragged its coast, hill height, keel slope, spire grid, spire height per block
     * inside the coast.
     */
    record Shape(double edge, double hills, double slope, double spireCell, double spireHeight) {}

    static final Shape ARCHIPELAGO = new Shape(2.0D, 9.0D, 1.1D, 40.0D, 1.3D);
    static final Shape DONUT = new Shape(2.0D, 7.0D, 0.7D, 52.0D, 1.7D);
    /** Islets: a few blocks across, flat, hanging a little deeper than they are wide. */
    static final Shape ISLET = new Shape(0.8D, 0.0D, 1.4D, 0.0D, 0.0D);

    /** No island in the column. */
    static final long NONE = Long.MIN_VALUE;

    /**
     * Blocks inside the coast at a spire seed, from the island's plan. The seed's grid cell identifies it, so callers
     * can remember the answer: one plan sample per seed rather than per column.
     */
    interface Plan {

        double inside(int cellX, int cellZ, double x, double z);
    }

    private static final long EDGE_SALT = 0x6A09E667F3BCC908L, HILL_SALT = 0x3C6EF372FE94F82BL,
        DRIP_SALT = 0xA54FF53A5F1D36F1L, SPIRE_SALT = 0x243F6A8885A308D3L, ROUGH_SALT = 0x452821E638D01377L,
        PRISM_SALT = 0xBE5466CF34E90C6CL, CAP_SALT = 0x9B05688C2B3E6C1FL, STRATA_SALT = 0x510E527FADE682D1L;

    private IslandKeel() {}

    /**
     * One island column, {@code bottom << 32 | top} (see {@link #bottom}, {@link #top}), or {@link #NONE}.
     *
     * @param inside   blocks inside the coast before the ragged edge (more than 0 is island)
     * @param deck     the rim height
     * @param spire    the depth of the spires hanging here ({@link #spire})
     * @param columnar hanging prisms rather than ledges
     */
    static long column(long seed, int x, int z, double inside, double deck, Shape shape, double maxDepth, double spire,
        boolean columnar) {
        double s = inside + shape.edge() * ValueNoise2D.fractal(seed ^ EDGE_SALT, x, z, 9.0D, 2, 0.5D);
        if (s <= 0.0D) return NONE;
        double lip = 2.0D * (1.0D - smoothstep(0.0D, 3.0D, s));
        double hills = shape.hills() * smoothstep(4.0D, 18.0D, s)
            * Math.max(0.0D, ValueNoise2D.fractal(seed ^ HILL_SALT, x, z, 40.0D, 3, 0.5D) + 0.25D);
        double top = deck - lip + hills;
        double cap = 3.0D + 1.5D * ValueNoise2D.fractal(seed ^ CAP_SALT, x, z, 23.0D, 1, 0.5D);
        // Deeper toward the middle; small islands hang at least as deep as they are wide.
        double body = shape.slope() * s * Math.pow(Math.max(12.0D, Math.min(s, 36.0D)) / 12.0D, 0.25D);
        body *= 1.0D + 0.4D * drip(seed, x, z) * smoothstep(3.0D, 10.0D, s);
        double depth = Math.min(maxDepth, cap + Math.max(body, spire));
        double bottom = deck - depth;
        if (columnar) {
            long prism = nearestPoint(seed ^ PRISM_SALT, x, z, 4.5D);
            bottom -= (0.06D + 0.30D * unit(prism >>> 40)) * Math.max(0.0D, depth - cap) + 2.0D * unit(prism >>> 8);
        } else {
            double jitter = ValueNoise2D.fractal(seed ^ STRATA_SALT, x, z, 7.0D, 1, 0.5D);
            bottom = Math.floor((bottom + jitter * 1.5D) / 3.0D) * 3.0D
                + Math.round(1.2D * ValueNoise2D.fractal(seed ^ ROUGH_SALT, x, z, 4.0D, 1, 0.5D));
        }
        int t = (int) Math.round(top), b = (int) Math.round(Math.min(bottom, top - 2.0D));
        return (long) b << 32 | t & 0xFFFF_FFFFL;
    }

    static int bottom(long column) {
        return (int) (column >> 32);
    }

    static int top(long column) {
        return (int) column;
    }

    /**
     * The depth of the cones hanging at a column: one seed per {@code spireCell} square, jittered, hangs a cone if it
     * lies well inside the coast, as wide and deep as it lies inside. Seeds near the coast hang nothing, so small
     * islands have no spire and big ones two to four, off centre.
     */
    static double spire(long seed, int x, int z, Shape shape, Plan plan) {
        double cell = shape.spireCell();
        if (cell <= 0.0D) return 0.0D;
        int cellX = (int) Math.floor(x / cell), cellZ = (int) Math.floor(z / cell);
        double deepest = 0.0D;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            long hash = point(seed ^ SPIRE_SALT, cellX + dx, cellZ + dz);
            double px = (cellX + dx + 0.15D + 0.7D * unit(hash >>> 16)) * cell;
            double pz = (cellZ + dz + 0.15D + 0.7D * unit(hash >>> 32)) * cell;
            double reach = Math.sqrt(square(x - px) + square(z - pz));
            if (reach >= 36.0D) continue; // No cone is wider.
            double inside = plan.inside(cellX + dx, cellZ + dz, px, pz);
            if (inside < 8.0D) continue;
            double radius = Math.min(36.0D, 0.85D * inside + 4.0D);
            if (reach >= radius) continue;
            double height = Math.min(60.0D, (shape.spireHeight() + 0.5D * unit(hash >>> 48)) * inside);
            deepest = Math.max(deepest, height * Math.pow(1.0D - reach / radius, 1.3D));
        }
        return deepest;
    }

    /** Drip fingers in [0, 1]: 1 at a jittered point of a 13-block grid, fading to 0 seven blocks away. */
    private static double drip(long seed, int x, int z) {
        final double cell = 13.0D;
        int cellX = (int) Math.floor(x / cell), cellZ = (int) Math.floor(z / cell);
        double nearest = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            long hash = point(seed ^ DRIP_SALT, cellX + dx, cellZ + dz);
            nearest = Math.min(
                nearest,
                square(x - (cellX + dx + unit(hash >>> 16)) * cell)
                    + square(z - (cellZ + dz + unit(hash >>> 32)) * cell));
        }
        double distance = Math.sqrt(nearest) / 7.0D;
        return distance >= 1.0D ? 0.0D : square(1.0D - distance);
    }

    /** The hash of the nearest jittered point of a grid: the prism a column hangs from. */
    private static long nearestPoint(long seed, int x, int z, double cell) {
        int cellX = (int) Math.floor(x / cell), cellZ = (int) Math.floor(z / cell);
        double nearest = Double.MAX_VALUE;
        long id = 0L;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            long hash = point(seed, cellX + dx, cellZ + dz);
            double distance = square(x - (cellX + dx + unit(hash >>> 16)) * cell)
                + square(z - (cellZ + dz + unit(hash >>> 32)) * cell);
            if (distance < nearest) {
                nearest = distance;
                id = hash;
            }
        }
        return id;
    }

    private static long point(long seed, int cellX, int cellZ) {
        return WorldgenContext.mix64(seed ^ cellX * 0x9E3779B97F4A7C15L ^ cellZ * 0xC2B2AE3D27D4EB4FL);
    }

    static double smoothstep(double from, double to, double value) {
        double t = Math.max(0.0D, Math.min(1.0D, (value - from) / (to - from)));
        return t * t * (3.0D - 2.0D * t);
    }

    /** 16 bits of a hash in [0, 1). */
    private static double unit(long bits) {
        return (bits & 0xFFFF) / 65536.0D;
    }

    private static double square(double value) {
        return value * value;
    }
}
