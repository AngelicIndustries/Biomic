// Generated test-only Terra 3745c3e oracle; MIT, see adjacent LICENSE.
package industries.angelic.biomic.testterra.pipeline.api;

import java.util.List;

public interface Pipeline {

    BiomeChunk generateChunk(SeededVector worldCoordinates);

    int getChunkSize();

    Source getSource();

    List<Stage> getStages();
}
