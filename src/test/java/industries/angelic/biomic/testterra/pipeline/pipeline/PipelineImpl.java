// Generated test-only Terra 3745c3e oracle; MIT, see adjacent LICENSE.
package industries.angelic.biomic.testterra.pipeline.pipeline;

import java.util.List;

import industries.angelic.biomic.testterra.pipeline.api.BiomeChunk;
import industries.angelic.biomic.testterra.pipeline.api.Expander;
import industries.angelic.biomic.testterra.pipeline.api.Pipeline;
import industries.angelic.biomic.testterra.pipeline.api.SeededVector;
import industries.angelic.biomic.testterra.pipeline.api.Source;
import industries.angelic.biomic.testterra.pipeline.api.Stage;

public class PipelineImpl implements Pipeline {

    private final Source source;
    private final List<Stage> stages;
    private final int chunkSize;
    private final int expanderCount;
    private final int arraySize;
    private final int chunkOriginArrayIndex;
    private final int resolution;

    public PipelineImpl(Source source, List<Stage> stages, int resolution, int idealChunkArraySize) {
        this.source = source;
        this.stages = stages;
        this.resolution = resolution;
        this.expanderCount = (int) stages.stream()
            .filter(s -> s instanceof Expander)
            .count();

        // Optimize for the ideal array size
        int arraySize;
        int chunkOriginArrayIndex;
        int chunkSize;
        int initialSize = 1;
        while (true) {
            arraySize = BiomeChunkImpl.initialSizeToArraySize(expanderCount, initialSize);
            chunkOriginArrayIndex = BiomeChunkImpl.calculateChunkOriginArrayIndex(expanderCount, stages);
            chunkSize = BiomeChunkImpl.calculateChunkSize(arraySize, chunkOriginArrayIndex, expanderCount);
            if (chunkSize > 1 && arraySize >= idealChunkArraySize) break;
            initialSize++;
        }

        this.arraySize = arraySize;
        this.chunkOriginArrayIndex = chunkOriginArrayIndex;
        this.chunkSize = chunkSize;

    }

    @Override
    public BiomeChunk generateChunk(SeededVector worldCoordinates) {
        return new BiomeChunkImpl(worldCoordinates, this);
    }

    @Override
    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    public Source getSource() {
        return source;
    }

    @Override
    public List<Stage> getStages() {
        return stages;
    }

    protected int getExpanderCount() {
        return expanderCount;
    }

    protected int getArraySize() {
        return arraySize;
    }

    protected int getChunkOriginArrayIndex() {
        return chunkOriginArrayIndex;
    }

    protected int getResolution() {
        return resolution;
    }
}
