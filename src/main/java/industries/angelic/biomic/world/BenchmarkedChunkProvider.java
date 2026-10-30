package industries.angelic.biomic.world;

import java.util.List;

/** Biomic overworld providers that report per-stage generation timings to {@link WorldgenBenchmark}. */
interface BenchmarkedChunkProvider {

    List<String> getPerformanceReport();

    void resetPerformanceStats();
}
