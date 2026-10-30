package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.world.WorldgenContext;

/**
 * Scaled execution of Origen's pinned Terra 6.4.2 density graph.
 *
 * <p>
 * No Biomic terrain-family approximation remains in this path. Horizontal coordinates, biome
 * blending, 3D biome extrusion, graph evaluation, and Terra's 4x4x4 interpolation are preserved.
 * Only Y is transformed piecewise so Origen's complete -64..319 authored envelope fits 0..255
 * while keeping sea level at 63.
 * </p>
 */
final class OrigenTerrainGraph {

    private final OrigenDensityField densityField;
    private final FloatingIslandDensitySampler floatingIslands;

    /** Ground-only construction breaks the placement cycle while floating clusters inspect their footprint. */
    OrigenTerrainGraph(WorldgenContext context) {
        this(context, null);
    }

    /** Reuses the exact floating field already sampled into the chunk's 3D biome volume. */
    OrigenTerrainGraph(WorldgenContext context, FloatingIslandDensitySampler floatingIslands) {
        if (context == null) {
            throw new IllegalArgumentException("Worldgen context cannot be null");
        }
        densityField = OrigenDensityField.shared(context);
        this.floatingIslands = floatingIslands;
    }

    TerrainShapeSample sampleColumn(MacroTerrainSample fields, int blockX, int blockZ) {
        OrigenProvinceSample province = fields.getOrigenProvince();
        int surfaceHeight = densityField.sampleScaledSurfaceHeight(blockX, blockZ);
        return new TerrainShapeSample(surfaceHeight, province.getPrimary());
    }

    double sampleDensity(int blockX, int blockY, int blockZ) {
        double ground = sampleGroundDensity(blockX, blockY, blockZ);
        return floatingIslands == null ? ground : Math.max(ground, sampleFloatingDensity(blockX, blockY, blockZ));
    }

    /** The floating island in a column, {@code bottom << 16 | top}, or -1 (before ground clearance). */
    int floatingSpan(int blockX, int blockZ) {
        return floatingIslands == null ? -1 : floatingIslands.span(blockX, blockZ);
    }

    double sampleFloatingDensity(int blockX, int blockY, int blockZ) {
        return floatingIslands == null ? Double.NEGATIVE_INFINITY
            : floatingIslands.sampleDensity(blockX, blockY, blockZ);
    }

    double sampleGroundDensity(int blockX, int blockY, int blockZ) {
        return densityField.sampleScaledDensity(blockX, blockY, blockZ);
    }

    OrigenDensityField.ScaledChunkDensity createGroundDensityChunk(int chunkX, int chunkZ) {
        return densityField.createScaledChunkDensity(chunkX, chunkZ);
    }

}
