// Generated test-only Terra 3745c3e oracle; MIT, see adjacent LICENSE.
package industries.angelic.biomic.testterra.pipeline.api;

import industries.angelic.biomic.testterra.pipeline.api.biome.PipelineBiome;
import industries.angelic.biomic.testterra.pipeline.pipeline.BiomeChunkImpl.ViewPoint;

public interface Stage {

    PipelineBiome apply(ViewPoint viewPoint);

    int maxRelativeReadDistance();

    default Iterable<PipelineBiome> getBiomes(Iterable<PipelineBiome> biomes) {
        return biomes;
    }
}
