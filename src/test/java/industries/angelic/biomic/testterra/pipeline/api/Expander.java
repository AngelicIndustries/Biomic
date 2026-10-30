// Generated test-only Terra 3745c3e oracle; MIT, see adjacent LICENSE.
package industries.angelic.biomic.testterra.pipeline.api;

import industries.angelic.biomic.testterra.pipeline.api.biome.PipelineBiome;
import industries.angelic.biomic.testterra.pipeline.pipeline.BiomeChunkImpl.ViewPoint;

/**
 * Resizes the internal grid of a BiomeChunk when applied, serves the purpose of
 * filling in null biomes as a result of this resizing.
 */
public interface Expander extends Stage {

    PipelineBiome fillBiome(ViewPoint viewPoint);

    @Override
    default int maxRelativeReadDistance() {
        return 0;
    }

    @Override
    default PipelineBiome apply(ViewPoint viewPoint) {
        PipelineBiome currentBiome = viewPoint.getBiome();
        if (currentBiome == null) {
            return fillBiome(viewPoint);
        } else {
            return currentBiome;
        }
    }
}
