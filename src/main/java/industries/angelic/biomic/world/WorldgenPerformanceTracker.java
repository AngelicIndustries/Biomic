package industries.angelic.biomic.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/** Low-overhead aggregate timings for live worldgen diagnosis without per-chunk log spam. */
final class WorldgenPerformanceTracker {

    private final AtomicLong terrainChunks = new AtomicLong();
    private final AtomicLong terrainTotal = new AtomicLong();
    private final AtomicLong precompute = new AtomicLong();
    private final AtomicLong materialResolution = new AtomicLong();
    private final AtomicLong structurePreparation = new AtomicLong();
    private final AtomicLong chunkConstruction = new AtomicLong();
    private final AtomicLong biomeStorage = new AtomicLong();
    private final AtomicLong skylightMap = new AtomicLong();
    private final AtomicLong maximumTerrain = new AtomicLong();

    private final AtomicLong populatedChunks = new AtomicLong();
    private final AtomicLong populationTotal = new AtomicLong();
    private final AtomicLong populationEvents = new AtomicLong();
    private final AtomicLong structurePopulation = new AtomicLong();
    private final AtomicLong dungeons = new AtomicLong();
    private final AtomicLong ores = new AtomicLong();
    private final AtomicLong vegetation = new AtomicLong();
    private final AtomicLong caves = new AtomicLong();
    private final AtomicLong maximumPopulation = new AtomicLong();

    void recordTerrain(long precomputeNanos, long materialNanos, long structureNanos, long chunkNanos, long biomeNanos,
        long skylightNanos, long totalNanos) {
        terrainChunks.incrementAndGet();
        precompute.addAndGet(precomputeNanos);
        materialResolution.addAndGet(materialNanos);
        structurePreparation.addAndGet(structureNanos);
        chunkConstruction.addAndGet(chunkNanos);
        biomeStorage.addAndGet(biomeNanos);
        skylightMap.addAndGet(skylightNanos);
        terrainTotal.addAndGet(totalNanos);
        updateMaximum(maximumTerrain, totalNanos);
    }

    void recordPopulation(long eventNanos, long structureNanos, long dungeonNanos, long oreNanos, long vegetationNanos,
        long caveNanos, long totalNanos) {
        populatedChunks.incrementAndGet();
        populationEvents.addAndGet(eventNanos);
        structurePopulation.addAndGet(structureNanos);
        dungeons.addAndGet(dungeonNanos);
        ores.addAndGet(oreNanos);
        vegetation.addAndGet(vegetationNanos);
        caves.addAndGet(caveNanos);
        populationTotal.addAndGet(totalNanos);
        updateMaximum(maximumPopulation, totalNanos);
    }

    List<String> report() {
        List<String> lines = new ArrayList<>(2);
        long terrainCount = terrainChunks.get();
        lines.add(
            "Terrain: " + terrainCount
                + " chunks, avg "
                + averageMillis(terrainTotal.get(), terrainCount)
                + " ms, max "
                + millis(maximumTerrain.get())
                + " ms (pure/wait "
                + averageMillis(precompute.get(), terrainCount)
                + ", resolve "
                + averageMillis(materialResolution.get(), terrainCount)
                + ", structures "
                + averageMillis(structurePreparation.get(), terrainCount)
                + ", chunk "
                + averageMillis(chunkConstruction.get(), terrainCount)
                + ", biomes "
                + averageMillis(biomeStorage.get(), terrainCount)
                + ", skylight "
                + averageMillis(skylightMap.get(), terrainCount)
                + ")");
        long populationCount = populatedChunks.get();
        lines.add(
            "Populate: " + populationCount
                + " chunks, avg "
                + averageMillis(populationTotal.get(), populationCount)
                + " ms, max "
                + millis(maximumPopulation.get())
                + " ms (events "
                + averageMillis(populationEvents.get(), populationCount)
                + ", structures "
                + averageMillis(structurePopulation.get(), populationCount)
                + ", dungeons "
                + averageMillis(dungeons.get(), populationCount)
                + ", ores "
                + averageMillis(ores.get(), populationCount)
                + ", vegetation "
                + averageMillis(vegetation.get(), populationCount)
                + ", caves "
                + averageMillis(caves.get(), populationCount)
                + ")");
        return lines;
    }

    void reset() {
        terrainChunks.set(0L);
        terrainTotal.set(0L);
        precompute.set(0L);
        materialResolution.set(0L);
        structurePreparation.set(0L);
        chunkConstruction.set(0L);
        biomeStorage.set(0L);
        skylightMap.set(0L);
        maximumTerrain.set(0L);
        populatedChunks.set(0L);
        populationTotal.set(0L);
        populationEvents.set(0L);
        structurePopulation.set(0L);
        dungeons.set(0L);
        ores.set(0L);
        vegetation.set(0L);
        caves.set(0L);
        maximumPopulation.set(0L);
    }

    private static void updateMaximum(AtomicLong maximum, long candidate) {
        long previous = maximum.get();
        while (candidate > previous && !maximum.compareAndSet(previous, candidate)) {
            previous = maximum.get();
        }
    }

    private static String averageMillis(long nanos, long count) {
        return count == 0L ? "0.000" : millis(nanos / count);
    }

    private static String millis(long nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0D);
    }
}
