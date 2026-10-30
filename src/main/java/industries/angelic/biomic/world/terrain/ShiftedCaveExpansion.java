package industries.angelic.biomic.world.terrain;

import java.util.BitSet;
import java.util.function.BiFunction;

import industries.angelic.biomic.api.ChunkVerticalLayout;
import industries.angelic.biomic.world.cave.CaveExpansionSettings;

/**
 * Ultimate Caves on uncompressed (shifted512) Origen. Shifted512 maps authored Y one-to-one onto physical
 * Y + 128, so the 256-row planner runs unchanged over the window physical 64..319, which is exactly authored
 * array rows 0..255. Masks are applied to the physical payload and to its authored companion at the same row.
 */
public final class ShiftedCaveExpansion {

    private static final ChunkVerticalLayout LAYOUT = ChunkVerticalLayout.EXTENDED_512;
    private static final int WINDOW_MIN_Y = 64;
    private static final int AUTHORED_HEIGHT = 384;
    private final HostedCaveExpansion planner;
    private final BiFunction<Integer, Integer, BitSet> groundMask;
    private final ThreadLocal<LastMask> last = ThreadLocal.withInitial(LastMask::new);

    /** groundMask: OrigenProjectedTerrainSampler#groundMask, which tells caves from open air. */
    public ShiftedCaveExpansion(long seed, CaveExpansionSettings settings,
        BiFunction<Integer, Integer, byte[]> physicalTerrain, BiFunction<Integer, Integer, BitSet> groundMask) {
        if (!settings.shifted512()) throw new IllegalArgumentException("Shifted caves require the shifted512 provider");
        this.groundMask = groundMask;
        planner = new HostedCaveExpansion(
            seed,
            settings,
            (cx, cz) -> window(physicalTerrain.apply(cx, cz), groundMask.apply(cx, cz)));
        // Window rows are authored rows: authored Y + 64.
        CavePockets pockets = new CavePockets(seed);
        planner.setPockets((x, z) -> {
            int range = pockets.range(x, z);
            return range < 0 ? -1 : range + (64 << 16) + 64;
        });
    }

    /** The terrain workers, for a cold mask's halo (HostedCaveExpansion#prefetchHalo). */
    public void setHaloWorkers(java.util.concurrent.Executor workers) {
        planner.setHaloWorkers(workers);
    }

    /** Carves the chunk's physical payload and authored companion in place; returns the (shared) mask. */
    public BitSet carve(int chunkX, int chunkZ, byte[] physical, byte[] authored) {
        if (physical.length != LAYOUT.blockCount() || authored.length != 256 * AUTHORED_HEIGHT)
            throw new IllegalArgumentException("Shifted caves need a 512 physical and 384 authored chunk");
        BitSet mask = planner.mask(chunkX, chunkZ, window(physical, groundMask.apply(chunkX, chunkZ)));
        planner.forEachOpened(chunkX, chunkZ, mask, (i, material) -> {
            int column = i >> 8, row = i & 255;
            physical[column * LAYOUT.height() + WINDOW_MIN_Y + row] = material;
            authored[column * AUTHORED_HEIGHT + row] = material;
        });
        return mask;
    }

    /** Re-pins a worker-computed mask so population lookups never replan a freshly generated chunk. */
    public void remember(int chunkX, int chunkZ, BitSet mask) {
        planner.rememberCompletedMask(chunkX, chunkZ, mask);
    }

    /** Carves a fresh copy of the chunk's pre-expansion physical terrain in place (planning it when not cached). */
    public byte[] carvePhysical(int chunkX, int chunkZ, byte[] physical) {
        planner.forEachOpened(
            chunkX,
            chunkZ,
            planner.mask(chunkX, chunkZ),
            (i, material) -> physical[(i >> 8) * LAYOUT.height() + WINDOW_MIN_Y + (i & 255)] = material);
        return physical;
    }

    /** Whether the expansion carved this physical cell; computes (deterministically) when not cached. */
    public boolean isCavity(int x, int y, int z) {
        int row = y - WINDOW_MIN_Y;
        if (row < 0 || row > 255 || !planner.mayCarveRow(row)) return false;
        int cx = x >> 4, cz = z >> 4;
        LastMask last = this.last.get();
        if (last.mask == null || last.cx != cx || last.cz != cz) {
            last.cx = cx;
            last.cz = cz;
            last.mask = planner.mask(cx, cz);
        }
        return last.mask.get(((x & 15) * 16 + (z & 15)) * 256 + row);
    }

    private static final class LastMask {

        int cx, cz;
        BitSet mask;
    }

    public String performanceReport() {
        return planner.performanceReport();
    }

    /**
     * The 256-row planner input. A cell is ground when it holds something or Origen's density is positive there (a
     * cave its own carver opened); other air is exterior, as the planner requires: the sky, the sea and the hollow
     * under an arch or overhang. Counting every cell below a column's top as ground made the planner read the space
     * under Temperate Sea Arches as a cave and widen it into the pillars, leaving shells and floating crumbs.
     * Window rows are physical 64..319, which is authored rows 0..255 of the ground mask.
     */
    private static HostedCaveExpansion.Base window(byte[] physical, BitSet ground) {
        byte[] materials = new byte[BiomicTerrainSampler.BLOCK_COUNT];
        byte[] origins = new byte[BiomicTerrainSampler.BLOCK_COUNT];
        int height = LAYOUT.height();
        for (int column = 0; column < 256; column++) {
            int base = column * height + WINDOW_MIN_Y, row0 = column * 256, mask0 = column * AUTHORED_HEIGHT;
            System.arraycopy(physical, base, materials, row0, 256);
            for (int row = 0; row < 256; row++)
                if (physical[base + row] != BiomicTerrainSampler.AIR || ground.get(mask0 + row))
                    origins[row0 + row] = TerrainDensitySampler.GROUND_ORIGIN;
        }
        return new HostedCaveExpansion.Base(materials, origins);
    }
}
