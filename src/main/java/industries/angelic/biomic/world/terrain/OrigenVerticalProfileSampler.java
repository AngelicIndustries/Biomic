package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.List;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.world.WorldgenContext;

/** Position-aware view of the exact Origen extrusion pipeline in Biomic's scaled vertical space. */
public final class OrigenVerticalProfileSampler {

    private static final int[][] AUTHORED_BLOCK_YS_BY_BIOMIC_Y = createAuthoredBlockYTable();

    private final OrigenDensityField densityField;
    private final AuthoredVerticalProjection projection;

    public OrigenVerticalProfileSampler(WorldgenContext context) {
        if (context == null) throw new IllegalArgumentException("Worldgen context cannot be null");
        densityField = OrigenDensityField.shared(context);
        projection = null;
    }

    /** Explicit projection path; storage height alone never selects it. */
    public OrigenVerticalProfileSampler(WorldgenContext context, AuthoredVerticalProjection projection) {
        if (context == null) {
            throw new IllegalArgumentException("Worldgen context cannot be null");
        }
        if (projection == null) throw new IllegalArgumentException("Origen vertical projection cannot be null");
        if (projection != AuthoredVerticalProjections.origenCompressed256()
            && projection != AuthoredVerticalProjections.shifted512()) {
            throw new IllegalArgumentException("Unsupported Origen vertical projection: " + projection.revision());
        }
        densityField = OrigenDensityField.shared(context);
        this.projection = projection;
    }

    /** Samples the two-dimensional pipeline once for a whole vertical quart column. */
    public OrigenBiomeProfile sampleSurface(int blockX, int blockZ) {
        return densityField.sampleSurfaceProfile(blockX, blockZ);
    }

    /** The climate band of the biome cell at a column: 0 cold, 1 medium, 2 warm. */
    public int climateBand(int blockX, int blockZ) {
        return densityField.climateBand(blockX, blockZ);
    }

    /** Applies Origen's exact cave extrusion to a previously sampled surface profile. */
    public OrigenBiomeProfile extrude(OrigenBiomeProfile surfaceProfile, int blockX, int biomicY, int blockZ) {
        return densityField.extrudeProfile(surfaceProfile, blockX, authoredBlockY(biomicY), blockZ);
    }

    private int authoredBlockY(int physicalY) {
        if (projection == null) return toUpstreamBlockY(physicalY);
        return projection.toSourceBlockY(physicalY)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Physical Y " + physicalY + " is outside Origen projection " + projection.revision()));
    }

    /** Maps an authored Origen block Y into the generator's exact 0..255 vertical transform. */
    public static int toBiomicBlockY(int upstreamY) {
        return Math.max(
            0,
            Math.min(
                OrigenDensityField.BIOMIC_MAX_Y_EXCLUSIVE - 1,
                (int) Math.round(OrigenDensityField.toBiomicY(upstreamY))));
    }

    /** All authored integer layers which collapse onto one physical block layer. */
    static int[] authoredBlockYsAt(int biomicY) {
        return biomicY < 0 || biomicY >= AUTHORED_BLOCK_YS_BY_BIOMIC_Y.length ? new int[0]
            : AUTHORED_BLOCK_YS_BY_BIOMIC_Y[biomicY];
    }

    private static int[][] createAuthoredBlockYTable() {
        List<List<Integer>> grouped = new ArrayList<>(OrigenDensityField.BIOMIC_MAX_Y_EXCLUSIVE);
        for (int biomicY = 0; biomicY < OrigenDensityField.BIOMIC_MAX_Y_EXCLUSIVE; biomicY++) {
            grouped.add(new ArrayList<>());
        }
        for (int authoredY = OrigenDensityField.UPSTREAM_MIN_Y; authoredY
            < OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE; authoredY++) {
            grouped.get(toBiomicBlockY(authoredY))
                .add(Integer.valueOf(authoredY));
        }
        int[][] result = new int[grouped.size()][];
        for (int biomicY = 0; biomicY < result.length; biomicY++) {
            List<Integer> authoredYs = grouped.get(biomicY);
            result[biomicY] = new int[authoredYs.size()];
            for (int index = 0; index < authoredYs.size(); index++) {
                result[biomicY][index] = authoredYs.get(index)
                    .intValue();
            }
        }
        return result;
    }

    static int toUpstreamBlockY(int biomicY) {
        return (int) Math.round(OrigenDensityField.toUpstreamY(biomicY));
    }
}
