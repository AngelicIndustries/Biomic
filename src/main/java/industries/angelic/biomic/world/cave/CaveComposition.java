package industries.angelic.biomic.world.cave;

import java.util.BitSet;

/**
 * Pure pre-decoration excavation planning. Geometry providers supply a single
 * composed field; they do not independently mutate the host or stack carvers.
 * Source must be coordinate-addressable pre-expansion terrain, not World reads
 * or a recursively carved neighbor chunk. This planner never writes blocks.
 */
public final class CaveComposition {

    // CAVITY is an already-authored subterranean opening inside the terrain host.
    // AIR is exterior air (including under arches), never an inferred cave identity.
    public enum Material {
        ROCK,
        SOLID,
        CAVITY,
        AIR,
        FLUID,
        PROTECTED
    }

    @FunctionalInterface
    public interface Host {

        Material at(int x, int y, int z);
    }

    @FunctionalInterface
    public interface Density {

        double at(int x, int y, int z);
    }

    public record Limits(int minY, int maxY, int roof, int wall, int floor) {

        public Limits {
            if (minY < 1 || maxY > 254
                || minY > maxY
                || roof < 2
                || roof > 32
                || wall < 1
                || wall > 16
                || floor < 1
                || floor > 16) throw new IllegalArgumentException("Unsafe cave envelope limits");
        }
    }

    private CaveComposition() {}

    /** Linear density blending, not an OR of two separately excavated masks. */
    public static double blend(double first, double second, double secondWeight) {
        if (!Double.isFinite(first) || !Double.isFinite(second)
            || !Double.isFinite(secondWeight)
            || secondWeight < 0
            || secondWeight > 1) throw new IllegalArgumentException("Invalid cave blend sample");
        return first * (1 - secondWeight) + second * secondWeight;
    }

    /** Index layout matches Biomic's 16×256×16 material arrays. */
    public static BitSet plan(int chunkX, int chunkZ, Host host, Density field, Limits limits) {
        if (host == null || field == null || limits == null)
            throw new IllegalArgumentException("Missing cave planner input");
        BitSet result = new BitSet(65536);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = limits.minY; y <= limits.maxY; y++) {
            int wx = chunkX * 16 + x, wz = chunkZ * 16 + z;
            if (host.at(wx, y, wz) != Material.ROCK) continue;
            double density = field.at(wx, y, wz);
            if (!Double.isFinite(density)) throw new IllegalArgumentException("Non-finite cave density");
            if (density <= 0 || !supported(host, wx, y, wz, limits)) continue;
            result.set((x * 16 + z) * 256 + y);
        }
        return result;
    }

    private static boolean supported(Host host, int x, int y, int z, Limits limits) {
        // Continuous scans, not just remote endpoints: catch a thin water pocket
        // or an open arch between the subject and its nominal surface envelope.
        for (int dy = 1; dy <= limits.roof; dy++) if (!hostRock(host.at(x, y + dy, z))) return false;
        for (int dy = 1; dy <= limits.floor; dy++) if (!hostRock(host.at(x, y - dy, z))) return false;
        for (int d = 1; d <= limits.wall; d++) {
            if (!hostRock(host.at(x - d, y, z)) || !hostRock(host.at(x + d, y, z))
                || !hostRock(host.at(x, y, z - d))
                || !hostRock(host.at(x, y, z + d))) return false;
        }
        return true;
    }

    /**
     * V2: expand each positive field seed to a complete 3x3x4 clearance volume.
     * Preflight the ENTIRE volume, including roof/wall/fluid guards, before admitting
     * its seed. Clipping a wide room against a per-voxel veto recreates cramped slits.
     * Seed and envelope halos cross chunk edges; only destination writes are clipped.
     */
    public static BitSet planWithClearance(int chunkX, int chunkZ, Host host, Density field, Limits limits) {
        if (host == null || field == null || limits == null)
            throw new IllegalArgumentException("Missing cave planner input");
        // Bit-parallel over 256-cell Y columns: each host cell is read once, the roof/floor/wall support scans
        // become shifted ANDs, and the 3x3x4 clearance test and write are separable box erosion/dilation.
        // Output is identical to the per-voxel version (verified on randomized hosts and the 256 reference window),
        // which made ~2M host reads and ~5M bit probes per chunk.
        final int width = 20, pad = limits.wall(), span = width + 2 * pad, y0 = limits.minY() - limits.floor(),
            y1 = limits.maxY() + limits.roof();
        final int ox = chunkX * 16 - 2, oz = chunkZ * 16 - 2;
        long[] rock = new long[span * span * 4], cavity = new long[span * span * 4],
            hostRock = new long[span * span * 4];
        for (int i = 0; i < span; i++) for (int k = 0; k < span; k++) {
            int c = (i * span + k) * 4;
            for (int y = y0; y <= y1; y++) {
                Material m = host.at(ox - pad + i, y, oz - pad + k);
                long bit = 1L << (y & 63);
                int w = c + (y >> 6);
                if (m == Material.ROCK) rock[w] |= bit;
                else if (m == Material.CAVITY) cavity[w] |= bit;
                if (hostRock(m)) hostRock[w] |= bit;
            }
        }
        long[] band = columnRange(limits.minY(), limits.maxY()),
            seedBand = columnRange(limits.minY() + 1, limits.maxY() - 2);
        long[] allowed = new long[width * width * 4], t = new long[4], acc = new long[4];
        for (int x = 0; x < width; x++) for (int z = 0; z < width; z++) {
            int c = ((x + pad) * span + z + pad) * 4, a = (x * width + z) * 4;
            for (int w = 0; w < 4; w++) acc[w] = (rock[c + w] | cavity[c + w]) & band[w];
            for (int d = 1; d <= limits.roof(); d++) {
                shiftDown(hostRock, c, d, t);
                and(acc, t);
            }
            for (int d = 1; d <= limits.floor(); d++) {
                shiftUp(hostRock, c, d, t);
                and(acc, t);
            }
            for (int d = 1; d <= limits.wall(); d++) {
                andAt(acc, hostRock, ((x + pad - d) * span + z + pad) * 4);
                andAt(acc, hostRock, ((x + pad + d) * span + z + pad) * 4);
                andAt(acc, hostRock, ((x + pad) * span + z + pad - d) * 4);
                andAt(acc, hostRock, ((x + pad) * span + z + pad + d) * 4);
            }
            System.arraycopy(acc, 0, allowed, a, 4);
        }
        // Vertical erosion: a seed at y needs y-1..y+2 allowed; then the 3x3 horizontal erosion.
        long[] vertical = new long[width * width * 4];
        for (int col = 0; col < width * width; col++) {
            int a = col * 4;
            System.arraycopy(allowed, a, acc, 0, 4);
            shiftUp(allowed, a, 1, t);
            and(acc, t);
            shiftDown(allowed, a, 1, t);
            and(acc, t);
            shiftDown(allowed, a, 2, t);
            and(acc, t);
            for (int w = 0; w < 4; w++) vertical[a + w] = acc[w] & seedBand[w];
        }
        long[] seeds = new long[width * width * 4];
        for (int x = 1; x < width - 1; x++) for (int z = 1; z < width - 1; z++) {
            int a = (x * width + z) * 4;
            System.arraycopy(vertical, a, acc, 0, 4);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                if (dx != 0 || dz != 0) andAt(acc, vertical, ((x + dx) * width + z + dz) * 4);
            int c = ((x + pad) * span + z + pad) * 4;
            for (int w = 0; w < 4; w++) for (long bits = acc[w]; bits != 0; bits &= bits - 1) {
                int y = (w << 6) + Long.numberOfTrailingZeros(bits);
                double value = field.at(ox + x, y, oz + z);
                if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite cave density");
                // Reconcile already-authored underground openings too. Widening only the
                // new field leaves the old one-block throats between providers untouched.
                // Exterior air is never CAVITY and cannot seed this pass.
                if (value > 0 || (cavity[c + (y >> 6)] & (1L << (y & 63))) != 0) seeds[a + (y >> 6)] |= 1L << (y & 63);
            }
        }
        // Dilation back: a seed at y clears y-1..y+2 over its 3x3, but only host ROCK inside this chunk.
        long[] cover = new long[width * width * 4];
        for (int col = 0; col < width * width; col++) {
            int a = col * 4;
            System.arraycopy(seeds, a, acc, 0, 4);
            shiftDown(seeds, a, 1, t);
            or(acc, t);
            shiftUp(seeds, a, 1, t);
            or(acc, t);
            shiftUp(seeds, a, 2, t);
            or(acc, t);
            System.arraycopy(acc, 0, cover, a, 4);
        }
        BitSet result = new BitSet(65536);
        for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) {
            int x = lx + 2, z = lz + 2;
            java.util.Arrays.fill(acc, 0L);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                int a = ((x + dx) * width + z + dz) * 4;
                for (int w = 0; w < 4; w++) acc[w] |= cover[a + w];
            }
            int c = ((x + pad) * span + z + pad) * 4, out = (lx * 16 + lz) * 256;
            for (int w = 0; w < 4; w++) for (long bits = acc[w] & rock[c + w]; bits != 0; bits &= bits - 1)
                result.set(out + (w << 6) + Long.numberOfTrailingZeros(bits));
        }
        return result;
    }

    private static long[] columnRange(int from, int to) {
        long[] bits = new long[4];
        for (int y = Math.max(0, from); y <= Math.min(255, to); y++) bits[y >> 6] |= 1L << (y & 63);
        return bits;
    }

    /** out[y]=column[y+d] (bits move toward lower Y). */
    private static void shiftDown(long[] columns, int at, int d, long[] out) {
        for (int w = 0; w < 4; w++) {
            int src = w + (d >> 6), bit = d & 63;
            long lo = src < 4 ? columns[at + src] >>> bit : 0L,
                hi = bit != 0 && src + 1 < 4 ? columns[at + src + 1] << (64 - bit) : 0L;
            out[w] = lo | hi;
        }
    }

    /** out[y]=column[y-d] (bits move toward higher Y). */
    private static void shiftUp(long[] columns, int at, int d, long[] out) {
        for (int w = 3; w >= 0; w--) {
            int src = w - (d >> 6), bit = d & 63;
            long hi = src >= 0 ? columns[at + src] << bit : 0L,
                lo = bit != 0 && src - 1 >= 0 ? columns[at + src - 1] >>> (64 - bit) : 0L;
            out[w] = hi | lo;
        }
    }

    private static void and(long[] acc, long[] t) {
        for (int w = 0; w < 4; w++) acc[w] &= t[w];
    }

    private static void or(long[] acc, long[] t) {
        for (int w = 0; w < 4; w++) acc[w] |= t[w];
    }

    private static void andAt(long[] acc, long[] columns, int at) {
        for (int w = 0; w < 4; w++) acc[w] &= columns[at + w];
    }

    private static boolean hostRock(Material material) {
        return material == Material.ROCK || material == Material.SOLID || material == Material.CAVITY;
    }
}
