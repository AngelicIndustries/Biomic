package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntPredicate;

import industries.angelic.biomic.api.ChunkVerticalLayout;

/** Physical terrain surfaces, independent of biome labels and the world's highest-block heightmap. */
public final class DecorationSurfaceColumn {

    private DecorationSurfaceColumn() {}

    /**
     * Enumerates every solid run's upper face, highest first, retaining its actual roof and fluid depth.
     * The caller supplies density provenance, not a Y cutoff or a biome-name guess. Does not decide
     * which surfaces are appropriate for trees, aquatic plants or caves; those are habitat decisions.
     */
    public static List<Surface> scan(byte[] column, IntPredicate floatingAtY) {
        if (column.length != BiomicTerrainSampler.WORLD_HEIGHT) {
            throw new IllegalArgumentException("Expected a complete physical 256-block column");
        }
        return scanCompleteColumn(column, floatingAtY);
    }

    /** Explicit-layout path for uncompressed physical columns; the legacy overload remains exactly 256-only. */
    public static List<Surface> scan(byte[] column, ChunkVerticalLayout layout, IntPredicate floatingAtY) {
        if (layout == null || column == null || column.length != layout.height()) {
            throw new IllegalArgumentException("Column does not match its explicit physical layout");
        }
        return scanCompleteColumn(column, floatingAtY);
    }

    private static List<Surface> scanCompleteColumn(byte[] column, IntPredicate floatingAtY) {
        List<Surface> surfaces = new ArrayList<>();
        int ceiling = column.length;
        int top = column.length - 1;
        while (top >= 0) {
            if (!solid(column[top])) {
                top--;
                continue;
            }
            int bottom = top;
            while (bottom > 0 && solid(column[bottom - 1])) bottom--;
            int fluidTop = top;
            while (fluidTop + 1 < ceiling && liquid(column[fluidTop + 1])) fluidTop++;
            surfaces.add(new Surface(bottom, top, fluidTop, ceiling, floatingAtY.test(top)));
            ceiling = bottom;
            top = bottom - 1;
        }
        return Collections.unmodifiableList(surfaces);
    }

    private static boolean solid(byte material) {
        return material != BiomicTerrainSampler.AIR && !liquid(material);
    }

    private static boolean liquid(byte material) {
        return material == BiomicTerrainSampler.WATER || material == BiomicTerrainSampler.LAVA;
    }

    public static final class Surface {

        public final int bottomY;
        public final int floorY;
        public final int fluidTopY;
        /** First solid block of the roof above, or the exclusive world ceiling. */
        public final int ceilingY;
        public final boolean floating;

        Surface(int bottomY, int floorY, int fluidTopY, int ceilingY, boolean floating) {
            this.bottomY = bottomY;
            this.floorY = floorY;
            this.fluidTopY = fluidTopY;
            this.ceilingY = ceilingY;
            this.floating = floating;
        }

        public int dryHeadroom() {
            return ceilingY - fluidTopY - 1;
        }

        public int fluidDepth() {
            return fluidTopY - floorY;
        }

        public boolean ownsDecorationAt(int y) {
            return !floating && fluidDepth() == 0 && y >= floorY - 3 && y < ceilingY;
        }

        public boolean canHostMainland(boolean highestGroundSurface, boolean lateralSkyAccess) {
            // Squeezed outdoor meadows can be well below sea level; altitude is not a cave test.
            return !floating && fluidDepth() == 0
                && floorY >= 1
                && (highestGroundSurface || dryHeadroom() >= 12 && lateralSkyAccess);
        }

        public boolean canHostWater(boolean highestGroundSurface, boolean lateralSkyAccess, byte[] column, int minDepth,
            int maxDepth) {
            if (floating || floorY < 1 || fluidDepth() < minDepth || fluidDepth() > maxDepth) return false;
            if (!(highestGroundSurface || dryHeadroom() >= 12 && lateralSkyAccess)) return false;
            // Never interpret lava pools as wetlands. The column scanner retains both fluids.
            for (int y = floorY + 1; y <= fluidTopY; y++) if (column[y] != BiomicTerrainSampler.WATER) return false;
            return true;
        }
    }
}
