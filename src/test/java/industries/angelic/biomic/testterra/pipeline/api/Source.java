// Generated test-only Terra 3745c3e oracle; MIT, see adjacent LICENSE.
package industries.angelic.biomic.testterra.pipeline.api;

import industries.angelic.biomic.testterra.pipeline.api.biome.PipelineBiome;

public interface Source {

    PipelineBiome get(long seed, int x, int z);

    Iterable<PipelineBiome> getBiomes();
}
