/*
 * Terra 3745c3e chunk-generation/interpolation semantics adapted for state-string rasters.
 * Copyright (c) 2020-2023 Polyhedral Development. MIT; see THIRD_PARTY_NOTICES.md.
 */
package industries.angelic.biomic.world.terrain;

import java.util.HashMap;
import java.util.Map;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * Source-height voxel rasters for isolated pack ports. No world access, registration or vertical squeeze.
 * Arrays use (x*16+z)*height+(y-minY). A whole raster fails if a required source profile is quarantined.
 */
public final class TerraPackChunkRasterizer {

    public interface Biomes {

        String at(int x, int y, int z);

        String surface(int x, int z);

        /** A view for one chunk on one thread; it may remember the last column it resolved. */
        default Biomes cursor() {
            return this;
        }
    }

    private final TerraPackTerrainRuntime terrain;
    private final TerraPackPaletteRuntime palettes;
    private final Biomes biomes;
    private final long seed;
    private final int min, max, height;
    /**
     * Skin seams: near a biome border, rock takes the neighbour's palette where the neighbour's share of the surface
     * within SKIN_REACH blocks beats a smooth 3D threshold, so two rocks meet in lobes rather than a vertical wall.
     */
    private static final int SKIN_REACH = 4;
    private final industries.angelic.biomic.internal.terra.noise.samplers.noise.simplex.OpenSimplex2Sampler skinNoise;

    /** Tartarus at its authored 0..255 from a (possibly neighbour-joined) manifest. */
    public static TerraPackChunkRasterizer tartarus(com.google.gson.JsonObject manifest, long seed) {
        int min = TartarusTerrainRuntime.SOURCE_MIN_Y, max = TartarusTerrainRuntime.SOURCE_MAX_Y;
        var pipeline = TerraV2BiomePipeline.tartarus(manifest, seed);
        return new TerraPackChunkRasterizer(
            TartarusTerrainRuntime.load(manifest),
            TerraPackPaletteRuntime.tartarus(manifest),
            new Biomes() {

                public String at(int x, int y, int z) {
                    return pipeline.biome(x, y, z);
                }

                public String surface(int x, int z) {
                    return pipeline.surface(x, z);
                }

                public Biomes cursor() {
                    // Rasters walk whole columns: resolve each column's source biome once, not once per Y.
                    var column = pipeline.nativeColumnSampler();
                    return new Biomes() {

                        public String at(int x, int y, int z) {
                            return column.at(x, y, z);
                        }

                        public String surface(int x, int z) {
                            return column.surface(x, z);
                        }
                    };
                }
            },
            seed,
            min,
            max);
    }

    TerraPackChunkRasterizer(TerraPackTerrainRuntime terrain, TerraPackPaletteRuntime palettes, Biomes biomes,
        long seed, int min, int max) {
        if (min % 4 != 0 || max % 4 != 0
            || min >= max
            || max - min > 512
            || min < -1024
            || max > 1024
            || (max - min) % terrain.settings()
                .carverVertical() != 0)
            throw new IllegalArgumentException("Unsupported Terra source height envelope");
        this.terrain = terrain;
        this.palettes = palettes;
        this.biomes = biomes;
        this.seed = seed;
        this.min = min;
        this.max = max;
        height = max - min;
        skinNoise = new industries.angelic.biomic.internal.terra.noise.samplers.noise.simplex.OpenSimplex2Sampler();
        skinNoise.setFrequency(1 / 24D);
        skinNoise.setSalt(0x534b494eL);
    }

    public String[] raster(int cx, int cz) {
        return new Chunk(cx, cz).raster();
    }

    /** The pack's surface (2D) biome id at a world column. */
    public String surfaceBiome(int x, int z) {
        return biomes.surface(x, z);
    }

    /** Immutable source chunk with its owning interpolator retained for script density checks. */
    public SourceChunk snapshot(int cx, int cz) {
        var chunk = new Chunk(cx, cz);
        chunk.raster();
        // Profile columns and surface lookups only build the lattice/raster. Density queries use the
        // completed lattice/elevation arrays, so retaining these maps multiplies shared-cache weight.
        chunk.clearScratch();
        return new SourceChunk(chunk);
    }

    public final class SourceChunk {

        private final Chunk owner;

        private SourceChunk(Chunk owner) {
            this.owner = owner;
        }

        public double density(double localX, double y, double localZ) {
            if (!Double.isFinite(localX) || !Double.isFinite(y)
                || !Double.isFinite(localZ)
                || localX < 0
                || localX >= 16
                || localZ < 0
                || localZ >= 16) throw new IllegalArgumentException("Not a finite local density coordinate");
            if (y < min || y >= max) return -1;
            return owner.sample(localX, y, localZ);
        }
    }

    /** Per-point only: no coordinates, runtime snapshots or shared scratch escape the constructor. */
    static final class NativeSample {

        final double value;
        double weight; // Zero is unbound; validated Blend weights are strictly positive and finite.

        NativeSample(double value) {
            this.value = value;
        }
    }

    static final class NativeMemo {

        private final Map<String, NativeSample> samples;
        private String lastId;
        private NativeSample last;

        NativeMemo() {
            this(new HashMap<>());
        }

        NativeMemo(Map<String, NativeSample> samples) {
            this.samples = samples;
        }

        NativeSample get(String id) {
            // Identity-only hit cannot invoke equals/hashCode or change equal-but-distinct handling.
            if (last != null && id == lastId) return last;
            NativeSample result = samples.get(id);
            if (result != null) {
                lastId = id;
                last = result;
            }
            return result;
        }

        void put(String id, NativeSample sample) {
            samples.put(id, sample); // Bind only after successful authoritative publication.
            lastId = id;
            last = sample;
        }
    }

    private final class Chunk {

        final int ox, oz;
        final double[][][] lattice = new double[5][5][height / 4 + 1];
        final double[][] elevation = new double[18][18];
        Long2ObjectMap<String[]> columns = new Long2ObjectOpenHashMap<>();
        Long2ObjectMap<String> surfaceProfiles = new Long2ObjectOpenHashMap<>();
        final Biomes cursor = biomes.cursor();

        Chunk(int cx, int cz) {
            ox = cx * 16;
            oz = cz * 16;
            // Both exact classes are final, with immutable profile/blend metadata and no overrides.
            boolean nativeMemo = terrain.getClass() == TartarusTerrainRuntime.class;
            for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) for (int iy = 0; iy < height / 4; iy++) {
                int wx = ox + x * 4, wz = oz + z * 4, y = min + iy * 4;
                var blend = terrain.blend(profile(wx, y, wz));
                double sum = 0, div = 0;
                if (nativeMemo) {
                    NativeMemo samples = new NativeMemo();
                    for (int dx = -blend.distance(); dx <= blend.distance(); dx++)
                        for (int dz = -blend.distance(); dz <= blend.distance(); dz++) {
                            String id = profile(wx + dx * blend.step(), y, wz + dz * blend.step());
                            NativeSample sampled = samples.get(id);
                            if (sampled == null) {
                                double value = finite(terrain.base(id, seed, wx, y, wz));
                                sampled = new NativeSample(value);
                                samples.put(id, sampled);
                            }
                            // Publication follows finite(base), and precedes the original first blend read.
                            if (sampled.weight == 0) sampled.weight = terrain.blend(id)
                                .weight();
                            sum += sampled.value * sampled.weight;
                            div += sampled.weight;
                        }
                } else {
                    Map<String, Double> samples = new HashMap<>();
                    for (int dx = -blend.distance(); dx <= blend.distance(); dx++)
                        for (int dz = -blend.distance(); dz <= blend.distance(); dz++) {
                            String id = profile(wx + dx * blend.step(), y, wz + dz * blend.step());
                            Double sampled = samples.get(id);
                            if (sampled == null) {
                                sampled = finite(terrain.base(id, seed, wx, y, wz));
                                samples.put(id, sampled);
                            }
                            double sample = sampled;
                            double weight = terrain.blend(id)
                                .weight();
                            sum += sample * weight;
                            div += weight;
                        }
                }
                lattice[x][z][iy] = finite(sum / div);
                if (iy == height / 4 - 1) lattice[x][z][iy + 1] = lattice[x][z][iy];

            }
            int smooth = terrain.settings()
                .elevationBlend();
            for (int x = -1; x <= 16; x++) for (int z = -1; z <= 16; z++) {
                int wx = ox + x, wz = oz + z;
                String center = surface(wx, wz);
                boolean same = true;
                for (int dx = -smooth; dx <= smooth; dx++) for (int dz = -smooth; dz <= smooth; dz++)
                    if (!center.equals(surface(wx + dx, wz + dz))) same = false;
                if (same) elevation[x + 1][z + 1] = finite(terrain.elevation(center, seed, wx, wz));
                else {
                    double sum = 0, div = 0;
                    if (nativeMemo) {
                        NativeMemo samples = new NativeMemo();
                        for (int dx = -smooth; dx <= smooth; dx++) for (int dz = -smooth; dz <= smooth; dz++) {
                            String id = surface(wx + dx, wz + dz);
                            NativeSample sampled = samples.get(id);
                            if (sampled == null) {
                                double value = finite(terrain.elevation(id, seed, wx, wz));
                                sampled = new NativeSample(value);
                                samples.put(id, sampled);
                            }
                            if (sampled.weight == 0) sampled.weight = terrain.blend(id)
                                .elevationWeight();
                            sum += sampled.value * sampled.weight;
                            div += sampled.weight;
                        }
                    } else {
                        Map<String, Double> samples = new HashMap<>();
                        for (int dx = -smooth; dx <= smooth; dx++) for (int dz = -smooth; dz <= smooth; dz++) {
                            String id = surface(wx + dx, wz + dz);
                            Double sampled = samples.get(id);
                            if (sampled == null) {
                                sampled = finite(terrain.elevation(id, seed, wx, wz));
                                samples.put(id, sampled);
                            }
                            double value = sampled;
                            double weight = terrain.blend(id)
                                .elevationWeight();
                            sum += value * weight;
                            div += weight;
                        }
                    }
                    elevation[x + 1][z + 1] = finite(sum / div);
                }

            }
        }

        String surface(int x, int z) {
            long key = key(x, z);
            String result = surfaceProfiles.get(key);
            if (result == null) {
                result = biomes.surface(x, z);
                if (result != null) surfaceProfiles.put(key, result);
            }
            return result;
        }

        private String[] column(int x, int z) {
            long key = key(x, z);
            String[] column = columns.get(key);
            if (column == null) {
                column = new String[height];
                columns.put(key, column);
            }
            return column;
        }

        String profile(int x, int y, int z) {
            return profile(column(x, z), x, y, z);
        }

        private String profile(String[] column, int x, int y, int z) {
            int index = y - min;
            if (column[index] == null) column[index] = cursor.at(x, y, z);
            return column[index];
        }

        double sample(double x, double y, double z) {
            // Terra's slant probes deliberately remain in their owning chunk, including edge extrapolation.
            int gx = Math.max(0, Math.min(3, ((int) x) / 4)), gz = Math.max(0, Math.min(3, ((int) z) / 4));
            int gy = (Math.max(min, Math.min(max - 1, (int) y)) - min) / 4;
            return interpolate(gx, gy, gz, (x % 4) / 4, (y % 4) / 4, (z % 4) / 4)
                + elevation[(int) Math.round(x) + 1][(int) Math.round(z) + 1];
        }

        double interpolate(int x, int y, int z, double fx, double fy, double fz) {
            double a = lerp(
                fz,
                lerp(fy, lattice[x][z][y], lattice[x][z][y + 1]),
                lerp(fy, lattice[x][z + 1][y], lattice[x][z + 1][y + 1]));
            double b = lerp(
                fz,
                lerp(fy, lattice[x + 1][z][y], lattice[x + 1][z][y + 1]),
                lerp(fy, lattice[x + 1][z + 1][y], lattice[x + 1][z + 1][y + 1]));
            return lerp(fx, a, b);
        }

        private double integerDensity(double fx, double fy, double fz, double a00, double a01, double a10, double a11,
            double b00, double b01, double b10, double b11) {
            double a = lerp(fz, lerp(fy, a00, a01), lerp(fy, a10, a11));
            double b = lerp(fz, lerp(fy, b00, b01), lerp(fy, b10, b11));
            return lerp(fx, a, b);
        }

        double slant(int x, int y, int z) {
            double d = .55;
            if (terrain.settings()
                .slantMethod()
                .equals("Derivative")) {
                double base = sample((double) x, y, z);
                double x1 = (sample(x + d, y, z) - base) / d, x2 = (sample(x - d, y, z) - base) / d;
                double z1 = (sample(x, y, z + d) - base) / d, z2 = (sample(x, y, z - d) - base) / d;
                double y1 = (sample(x, y + d, z) - base) / d, y2 = (sample(x, y - d, z) - base) / d;
                return Math.sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1) + (y2 - y1) * (y2 - y1));
            }
            double nx = d * sample(x - d, y, z) - d * sample(x + d, y, z);
            double ny = d * sample(x, y - d, z) - d * sample(x, y + d, z);
            double nz = d * sample(x, y, z - d) - d * sample(x, y, z + d);
            double length = nx * nx + ny * ny + nz * nz;
            if (length == 0) return 0;
            long bits = 0x5FE6EC85E7DE30DAL - (Double.doubleToLongBits(length) >> 1);
            double inverse = Double.longBitsToDouble(bits);
            inverse = inverse * (1.5 - .5 * length * inverse * inverse);
            return ny * inverse;
        }

        String[] raster() {
            String[] blocks = new String[256 * height];
            java.util.Arrays.fill(blocks, "minecraft:air");
            Carver carver = new Carver();
            // Visitation matters to the upstream lazy carver. Do not parallelize columns or eagerly seed its grid.
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int depth = 0, paletteSurface = -1, skinSurface = -1;
                String paletteProfile = null, skinProfile = null;
                // Near a seam: the most common other surface biome around the column, and the share of the pair's
                // later-named biome among the two. Both sides of a seam decide by that same share, so they agree.
                String here = surface(ox + x, oz + z), other = null;
                double share = 0;
                if (here != null && nearSeam(ox + x, oz + z, here)) {
                    Map<String, Integer> around = new HashMap<>();
                    for (int dx = -SKIN_REACH; dx <= SKIN_REACH; dx++)
                        for (int dz = -SKIN_REACH; dz <= SKIN_REACH; dz++) {
                            String near = surface(ox + x + dx, oz + z + dz);
                            if (near == null) continue;
                            int n = around.merge(near, 1, Integer::sum);
                            if (!near.equals(here) && (other == null || n > around.get(other))) other = near;
                        }
                    if (other != null) {
                        String later = other.compareTo(here) > 0 ? other : here;
                        share = around.get(later) / (double) (around.get(here) + around.get(other));
                    }
                }
                // Bind only the existing lazy column. Per-Y null fills and callback order stay
                // literal; carver neighbor fills remain visible through this same array alias.
                String[] profileColumn = column(ox + x, oz + z);
                int gx = x / 4, gz = z / 4, cachedGy = -1;
                double fx = (x & 3) / 4D, fz = (z & 3) / 4D;
                double a00 = 0, a01 = 0, a10 = 0, a11 = 0, b00 = 0, b01 = 0, b10 = 0, b11 = 0;
                for (int y = max - 1; y >= min; y--) {
                    String id = profile(profileColumn, ox + x, y, oz + z);
                    int index = (x * 16 + z) * height + y - min;
                    int gy = (y - min) / 4;
                    if (gy != cachedGy) {
                        // Private completed lattice: bind only at the original integer sample
                        // position after this Y's profile callback. No arithmetic is reassociated.
                        a00 = lattice[gx][gz][gy];
                        a01 = lattice[gx][gz][gy + 1];
                        a10 = lattice[gx][gz + 1][gy];
                        a11 = lattice[gx][gz + 1][gy + 1];
                        b00 = lattice[gx + 1][gz][gy];
                        b01 = lattice[gx + 1][gz][gy + 1];
                        b10 = lattice[gx + 1][gz + 1][gy];
                        b11 = lattice[gx + 1][gz + 1][gy + 1];
                        cachedGy = gy;
                    }
                    if (integerDensity(fx, (y & 3) / 4D, fz, a00, a01, a10, a11, b00, b01, b10, b11)
                        + elevation[x + 1][z + 1] > 0) {
                        if (carver.sample(x, y, z) <= 0) {
                            // Where the column is its surface biome, its skin may come from the biome across a seam.
                            String skin = id;
                            if (other != null && id.equals(here)) {
                                boolean later = share > .5 + skinNoise.noise(seed, ox + x, y, oz + z);
                                skin = later == (other.compareTo(here) > 0) ? other : here;
                            }
                            if (skinSurface < 0 || !java.util.Objects.equals(skin, skinProfile)) {
                                skinSurface = palettes.surfaceId(skin);
                                skinProfile = skin;
                            }
                            blocks[index] = palettes.state(
                                skinSurface,
                                depth,
                                seed,
                                ox + x,
                                y,
                                oz + z,
                                palettes.needsSlant(skinSurface, depth) ? slant(x, y, z) : Double.NaN);
                            depth++;
                        } else {
                            if (paletteSurface < 0 || !java.util.Objects.equals(id, paletteProfile)) {
                                paletteSurface = palettes.surfaceId(id);
                                paletteProfile = id;
                            }
                            depth = palettes.updatePaletteWhenCarving(paletteSurface) ? 0 : depth + 1;
                        }
                    } else {
                        if (paletteSurface < 0 || !java.util.Objects.equals(id, paletteProfile)) {
                            paletteSurface = palettes.surfaceId(id);
                            paletteProfile = id;
                        }
                        Integer sea = palettes.oceanLevel(paletteSurface);
                        if (sea != null && y <= sea)
                            blocks[index] = palettes.oceanState(paletteSurface, sea - y, seed, ox + x, y, oz + z);
                        depth = 0;
                    }
                }
            }
            return blocks;
        }

        /**
         * Whether another surface biome shows on the square SKIN_REACH blocks around the column (corners and edges).
         */
        private boolean nearSeam(int x, int z, String here) {
            for (int dx = -SKIN_REACH; dx <= SKIN_REACH; dx += SKIN_REACH)
                for (int dz = -SKIN_REACH; dz <= SKIN_REACH; dz += SKIN_REACH)
                    if (!here.equals(surface(x + dx, z + dz))) return true;
            return false;
        }

        void clearScratch() {
            columns = Long2ObjectMaps.emptyMap();
            surfaceProfiles = Long2ObjectMaps.emptyMap();
        }

        private final class Carver {

            final int hr = terrain.settings()
                .carverHorizontal(),
                vr = terrain.settings()
                    .carverVertical(),
                width = 16 / hr + 1;
            final Double[] cache = new Double[width * width * (height / vr + 1)];

            double value(int gx, int gy, int gz, int x, int y, int z) {
                int index = gx + gz * width + gy * width * width;
                if (cache[index] == null) {
                    y = Math.min(max - 1, y);
                    cache[index] = finite(terrain.carving(profile(ox + x, y, oz + z), seed, ox + x, y, oz + z));
                }
                return cache[index];
            }

            double sample(int x, int y, int z) {
                int gx = x / hr, gy = (y - min) / vr, gz = z / hr;
                double a = value(gx, gy, gz, x, y, z);
                if (x % hr == 0 && y % vr == 0 && z % hr == 0) return a;
                double b = value(gx, gy, gz + 1, x, y, z + hr), c = value(gx + 1, gy, gz, x + hr, y, z),
                    e = value(gx + 1, gy, gz + 1, x + hr, y, z + hr);
                double fx = (x % hr) / (double) hr, fz = (z % hr) / (double) hr;
                double bottom = lerp(fx, lerp(fz, a, b), lerp(fz, c, e));
                if (y % vr == 0) return bottom;
                double aa = value(gx, gy + 1, gz, x, y + vr, z), bb = value(gx, gy + 1, gz + 1, x, y + vr, z + hr);
                double cc = value(gx + 1, gy + 1, gz, x + hr, y + vr, z),
                    ee = value(gx + 1, gy + 1, gz + 1, x + hr, y + vr, z + hr);
                return lerp(Math.floorMod(y, vr) / (double) vr, bottom, lerp(fx, lerp(fz, aa, bb), lerp(fz, cc, ee)));
            }
        }
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static double finite(double value) {
        if (!Double.isFinite(value)) throw new IllegalStateException("Non-finite Terra density");
        return value;
    }
}
