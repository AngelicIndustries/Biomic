package industries.angelic.biomic.world;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import industries.angelic.angellib.AngelLibThreadPool;
import industries.angelic.biomic.biome.VerticalBiomeSampler;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.world.terrain.BiomicTerrainSampler;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

/**
 * Bounded look-ahead for the side-effect-free half of chunk generation.
 *
 * <p>
 * Only biome-volume and material-array sampling runs here. Anything that reads or mutates a {@code World}, posts
 * Forge events, or touches structure state stays in {@link ChunkProviderBiomic#provideChunk(int, int)}.
 */
final class TerrainPrecomputeScheduler {

    private static final int[][] NEIGHBOR_OFFSETS = { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 }, { 1, 1 }, { -1, 1 },
        { -1, -1 }, { 1, -1 } };
    private static final int RECENT_DEMAND_CACHE_SIZE = 4_096;
    private static final int RECENT_DEMAND_REGION_COUNT = 32;

    /** Pure per-chunk terrain: must be safe to call from worker threads and the server thread. */
    interface Source {

        TerrainPayload compute(int chunkX, int chunkZ);

        /** Called on the server thread when a payload is consumed. */
        default void adopt(int chunkX, int chunkZ, TerrainPayload payload) {}

        /**
         * Called when the caller is about to compute a chunk itself (nothing was ready): hands work the chunk will
         * need, and that can run on its own, to the otherwise idle workers.
         */
        default void warm(int chunkX, int chunkZ, java.util.concurrent.Executor workers) {}
    }

    /** Work a chunk will need that the workers can start while the caller computes it (see Source#warm). */
    interface Warmer {

        void warm(int chunkX, int chunkZ, java.util.concurrent.Executor workers);
    }

    private final Source source;
    private final int maximumPrefetchTasks;
    private final int maximumActiveTasks;
    private final ExecutorService workers;
    private final Map<Long, PrefetchTask> prefetched = new LinkedHashMap<>();
    /**
     * Completed immutable payloads exposed to population without contending on the scheduler hot lock. Population
     * may need both authored and physical terrain from the same speculative result; publishing only the authored
     * companion forced it to reconstruct the 64 KiB material array (and its cave halo) on the server thread.
     */
    private final ConcurrentHashMap<Long, TerrainPayload> completedTerrain = new ConcurrentHashMap<>();
    /** Exact nearest-first initial-view plans, rotated one request per player for fair multi-player look-ahead. */
    private final ArrayDeque<PrefetchStream> plannedStreams = new ArrayDeque<>();
    /**
     * Running plus executor-queued speculation. Keeping this at four tasks per worker gives the server enough nearby
     * runway while remaining far below the retained-result cache. A demanded queued task is promoted immediately.
     */
    private final AtomicInteger activeTasks = new AtomicInteger();
    /** Prevents neighbor look-ahead from immediately regenerating chunks the server has already consumed. */
    private final LongLinkedOpenHashSet recentDemands = new LongLinkedOpenHashSet(RECENT_DEMAND_CACHE_SIZE);
    /**
     * Small request-history window used to distinguish a new far teleport from interleaved, distant players. A
     * single last-position heuristic makes every request from player B invalidate player A's work and vice versa.
     */
    private final ArrayDeque<Long> recentDemandRegions = new ArrayDeque<>(RECENT_DEMAND_REGION_COUNT);
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong demandMisses = new AtomicLong();
    private final AtomicLong submittedPrefetches = new AtomicLong();
    private final AtomicLong evictedPrefetches = new AtomicLong();
    private final AtomicLong completedHits = new AtomicLong();
    private final AtomicLong runningHits = new AtomicLong();
    private final AtomicLong queuedPromotions = new AtomicLong();
    private volatile boolean shuttingDown;

    /** Reshapes a chunk's finished materials in place on the worker, before anything else reads them. */
    interface Shaper {

        void shape(int chunkX, int chunkZ, byte[] materials);
    }

    TerrainPrecomputeScheduler(VerticalBiomeSampler verticalBiomes, BiomicTerrainSampler terrain, Shaper shaper,
        Warmer warmer, int workerCount, int maximumPrefetchTasks) {
        this(legacySource(verticalBiomes, terrain, shaper, warmer), workerCount, maximumPrefetchTasks);
    }

    TerrainPrecomputeScheduler(Source source, int workerCount, int maximumPrefetchTasks) {
        if (source == null) {
            throw new IllegalArgumentException("Terrain precompute source cannot be null");
        }
        if (workerCount < 0 || maximumPrefetchTasks < 0) {
            throw new IllegalArgumentException("Terrain worker and prefetch limits cannot be negative");
        }
        this.source = source;
        this.maximumPrefetchTasks = maximumPrefetchTasks;
        this.maximumActiveTasks = Math.min(workerCount * 4, maximumPrefetchTasks);
        workers = workerCount == 0 ? null : AngelLibThreadPool.createFixedThreadPool(workerCount, "Biomic terrain");
    }

    /** The worker pool, or null when terrain is made on the caller. */
    java.util.concurrent.Executor workers() {
        return workers;
    }

    TerrainPayload take(int chunkX, int chunkZ) {
        if (workers == null) {
            return compute(chunkX, chunkZ);
        }

        long currentKey = coordinateKey(chunkX, chunkZ);
        PrefetchTask current;
        synchronized (prefetched) {
            // A demanded task must no longer be eligible for speculative eviction while its caller waits on it.
            current = prefetched.remove(currentKey);
            if (current == null) {
                demandMisses.incrementAndGet();
                if (!isNearRecentDemand(chunkX, chunkZ)) {
                    // No recently active request stream owns this area, so this is a genuine teleport rather than
                    // round-robin work from another explorer. Drop the obsolete queue so demand is not trapped
                    // behind dozens of speculative jobs from the old location.
                    discardFarPrefetches(chunkX, chunkZ, staleDistance());
                }
            } else if (current.cancelQueued()) {
                // This guess had not started. Compute it on the caller now instead of waiting behind other guesses.
                queuedPromotions.incrementAndGet();
                demandMisses.incrementAndGet();
                current = null;
            } else {
                cacheHits.incrementAndGet();
                if (current.isComplete()) {
                    completedHits.incrementAndGet();
                } else {
                    runningHits.incrementAndGet();
                }
            }
            rememberDemandRegion(currentKey);
        }

        if (current == null) source.warm(chunkX, chunkZ, workers);
        TerrainPayload payload = current == null ? compute(chunkX, chunkZ) : current.await();
        boolean demandMiss = current == null;
        source.adopt(chunkX, chunkZ, payload);
        completedTerrain.remove(currentKey);
        // Start guesses only after the demanded payload is available. This keeps the shared Origen density caches
        // focused on work that can unblock the server rather than evicting it while the caller is still waiting.
        synchronized (prefetched) {
            recentDemands.addAndMoveToLast(currentKey);
            if (recentDemands.size() > RECENT_DEMAND_CACHE_SIZE) recentDemands.removeFirstLong();
            if (demandMiss) reclaimCompletedDemandFrontier();
            else pumpPlannedPrefetches();
        }
        // A miss proves the old exact-view tail did not cover the active frontier. Let its local neighbors use the
        // reclaimed runway first; on a hit, exact initial-view requests retain their normal priority.
        prefetchNeighbors(chunkX, chunkZ);
        if (demandMiss) {
            synchronized (prefetched) {
                pumpPlannedPrefetches();
            }
        }
        return payload;
    }

    private void pumpPlannedPrefetches() {
        if (shuttingDown) return;
        while (!plannedStreams.isEmpty()) {
            PrefetchStream stream = plannedStreams.removeFirst();
            boolean submitted = false;
            while (!stream.requests.isEmpty()) {
                ChunkRequest request = stream.requests.removeFirst();
                PrefetchResult result = prefetchLocked(request.chunkX, request.chunkZ);
                if (result == PrefetchResult.FULL) {
                    stream.requests.addFirst(request);
                    plannedStreams.addFirst(stream);
                    return;
                }
                if (result == PrefetchResult.SUBMITTED) {
                    submitted = true;
                    break;
                }
            }
            if (!stream.requests.isEmpty()) plannedStreams.addLast(stream);
            // A stream consisting entirely of already-consumed requests is discarded. Otherwise rotate after one
            // submission so a second player's distant view receives equal access to the bounded worker frontier.
            if (!submitted && plannedStreams.isEmpty()) return;
        }
    }

    private PrefetchResult prefetchLocked(int chunkX, int chunkZ) {
        if (maximumPrefetchTasks == 0 || maximumActiveTasks == 0 || shuttingDown) {
            return PrefetchResult.FULL;
        }
        long key = coordinateKey(chunkX, chunkZ);
        if (prefetched.containsKey(key) || recentDemands.contains(key)) {
            return PrefetchResult.SKIPPED;
        }
        if (prefetched.size() >= maximumPrefetchTasks || activeTasks.get() >= maximumActiveTasks) {
            return PrefetchResult.FULL;
        }
        PrefetchTask task = new PrefetchTask(chunkX, chunkZ);
        activeTasks.incrementAndGet();
        try {
            task.future = workers.submit(task::execute);
        } catch (RuntimeException exception) {
            activeTasks.decrementAndGet();
            throw exception;
        }
        prefetched.put(key, task);
        submittedPrefetches.incrementAndGet();
        return PrefetchResult.SUBMITTED;
    }

    private enum PrefetchResult {
        SUBMITTED,
        SKIPPED,
        FULL
    }

    private static final class PrefetchStream {

        private final ArrayDeque<ChunkRequest> requests;

        private PrefetchStream(List<ChunkRequest> requests) {
            this.requests = new ArrayDeque<>(requests);
        }
    }

    void shutdown() {
        synchronized (prefetched) {
            if (shuttingDown) return;
            shuttingDown = true;
            prefetched.clear();
            completedTerrain.clear();
            recentDemands.clear();
            recentDemandRegions.clear();
            plannedStreams.clear();
        }
        if (workers != null) {
            workers.shutdownNow();
        }
    }

    String performanceReport() {
        long hits = cacheHits.get();
        long misses = demandMisses.get();
        int pending;
        int active;
        int planned = 0;
        synchronized (prefetched) {
            pending = prefetched.size();
            active = activeTasks.get();
            for (PrefetchStream stream : plannedStreams) planned += stream.requests.size();
        }
        double hitRate = hits + misses == 0L ? 0.0D : hits * 100.0D / (hits + misses);
        return String.format(
            java.util.Locale.ROOT,
            "Pure-terrain scheduler: %d hits/%d misses (%.1f%%; %d ready, %d running, %d promoted), "
                + "%d prefetches, %d evictions, %d/%d cached, %d/%d active, %d planned",
            hits,
            misses,
            hitRate,
            completedHits.get(),
            runningHits.get(),
            queuedPromotions.get(),
            submittedPrefetches.get(),
            evictedPrefetches.get(),
            pending,
            maximumPrefetchTasks,
            active,
            maximumActiveTasks,
            planned);
    }

    void resetPerformanceStats() {
        cacheHits.set(0L);
        demandMisses.set(0L);
        submittedPrefetches.set(0L);
        evictedPrefetches.set(0L);
        completedHits.set(0L);
        runningHits.set(0L);
        queuedPromotions.set(0L);
    }

    /**
     * Returns authored-height data from completed look-ahead without consuming the terrain payload or waiting for a
     * worker. Origen features inspect unloaded neighboring chunks during population and can reuse this exact result.
     */
    byte[] peekAuthoredMaterials(int chunkX, int chunkZ) {
        TerrainPayload payload = workers == null ? null : completedTerrain.get(coordinateKey(chunkX, chunkZ));
        return payload == null ? null : payload.getAuthoredMaterials();
    }

    /** Returns the exact immutable physical array already completed by look-ahead; never waits on a worker. */
    byte[] peekRawMaterials(int chunkX, int chunkZ) {
        TerrainPayload payload = workers == null ? null : completedTerrain.get(coordinateKey(chunkX, chunkZ));
        return payload == null ? null : payload.getMaterials();
    }

    /** Returns a defensive copy of a COMPLETE payload's cave projection; never waits or computes. */
    BitSet peekCaveProjectionMask(int chunkX, int chunkZ) {
        TerrainPayload payload = workers == null ? null : completedTerrain.get(coordinateKey(chunkX, chunkZ));
        return payload == null ? null : payload.getCaveProjectionMask();
    }

    private TerrainPayload compute(int chunkX, int chunkZ) {
        return source.compute(chunkX, chunkZ);
    }

    private static Source legacySource(VerticalBiomeSampler verticalBiomes, BiomicTerrainSampler terrain, Shaper shaper,
        Warmer warmer) {
        if (verticalBiomes == null || terrain == null) {
            throw new IllegalArgumentException("Terrain precompute samplers cannot be null");
        }
        return new Source() {

            @Override
            public TerrainPayload compute(int chunkX, int chunkZ) {
                BiomeVolume volume = verticalBiomes.createVolume(chunkX, chunkZ);
                byte[] materials = terrain.createMaterialArray(chunkX, chunkZ, volume);
                if (shaper != null) shaper.shape(chunkX, chunkZ, materials);
                // Keep the authored-height companion with the exact speculative result. Its global computation-order
                // LRU may otherwise evict it long before vanilla asks to populate this chunk.
                byte[] authoredMaterials = terrain.cachedAuthoredMaterialArray(chunkX, chunkZ);
                BitSet caveProjectionMask = terrain.completedCaveMaskSnapshot(chunkX, chunkZ);
                return new TerrainPayload(volume, materials, authoredMaterials, caveProjectionMask);
            }

            @Override
            public void warm(int chunkX, int chunkZ, java.util.concurrent.Executor workers) {
                if (warmer != null) warmer.warm(chunkX, chunkZ, workers);
            }

            @Override
            public void adopt(int chunkX, int chunkZ, TerrainPayload payload) {
                // The worker's small cave-mask LRU may have turned over while this payload waited for demand.
                terrain.rememberCompletedCaveMask(chunkX, chunkZ, payload.getCaveProjectionMask());
            }
        };
    }

    private void prefetchNeighbors(int chunkX, int chunkZ) {
        for (int[] offset : NEIGHBOR_OFFSETS) {
            if (!prefetch(chunkX + offset[0], chunkZ + offset[1])) {
                return;
            }
        }
    }

    /** Whether a chunk's terrain is already finished on the workers, so taking it costs the caller nothing. */
    boolean ready(int chunkX, int chunkZ) {
        synchronized (prefetched) {
            PrefetchTask task = prefetched.get(coordinateKey(chunkX, chunkZ));
            return task != null && task.isComplete();
        }
    }

    /** Starts a chunk's terrain on the workers ahead of demand; false once the look-ahead is full. */
    boolean prefetchAhead(int chunkX, int chunkZ) {
        return prefetch(chunkX, chunkZ);
    }

    private boolean prefetch(int chunkX, int chunkZ) {
        synchronized (prefetched) {
            return prefetchLocked(chunkX, chunkZ) != PrefetchResult.FULL;
        }
    }

    /** Drops pure look-ahead work from a previous location before a far teleport queues its demanded chunk. */
    private void discardFarPrefetches(int centerChunkX, int centerChunkZ, int retainedDistance) {
        java.util.Iterator<Map.Entry<Long, PrefetchTask>> iterator = prefetched.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, PrefetchTask> entry = iterator.next();
            int chunkX = (int) (entry.getKey() >> 32);
            int chunkZ = entry.getKey()
                .intValue();
            long distance = Math.max(Math.abs((long) chunkX - centerChunkX), Math.abs((long) chunkZ - centerChunkZ));
            if (distance > retainedDistance && entry.getValue()
                .evict()) {
                completedTerrain.remove(entry.getKey());
                iterator.remove();
                evictedPrefetches.incrementAndGet();
            }
        }
    }

    /**
     * A full cache of completed guesses must not permanently stop the worker frontier. Reclaim only after an actual
     * demand miss, and only from COMPLETE entries: running and queued tasks keep their identity and future. Distance
     * is measured to the nearest recently active demand region so two distant explorers retain useful look-ahead.
     */
    private void reclaimCompletedDemandFrontier() {
        int requestedFreeSlots = Math.min(prefetched.size(), Math.max(0, maximumActiveTasks - activeTasks.get()));
        int targetSize = Math.max(0, maximumPrefetchTasks - requestedFreeSlots);
        while (prefetched.size() > targetSize) {
            Map.Entry<Long, PrefetchTask> stale = null;
            long staleDistance = Long.MIN_VALUE;
            for (Map.Entry<Long, PrefetchTask> entry : prefetched.entrySet()) {
                if (!entry.getValue()
                    .isComplete()) continue;
                long nearest = nearestRecentDemandDistance(entry.getKey());
                if (nearest > staleDistance) {
                    stale = entry;
                    staleDistance = nearest;
                }
            }
            if (stale == null || !stale.getValue()
                .evict()) return;
            prefetched.remove(stale.getKey());
            completedTerrain.remove(stale.getKey());
            evictedPrefetches.incrementAndGet();
        }
    }

    private long nearestRecentDemandDistance(long candidate) {
        int chunkX = (int) (candidate >> 32), chunkZ = (int) candidate;
        long nearest = Long.MAX_VALUE;
        for (long demand : recentDemandRegions) {
            int demandX = (int) (demand >> 32), demandZ = (int) demand;
            nearest = Math.min(nearest, Math.max(Math.abs((long) chunkX - demandX), Math.abs((long) chunkZ - demandZ)));
        }
        return nearest;
    }

    private boolean isNearRecentDemand(int chunkX, int chunkZ) {
        return isNearDemandRegion(recentDemandRegions, chunkX, chunkZ, staleDistance());
    }

    static boolean isNearDemandRegion(Iterable<Long> regions, int chunkX, int chunkZ, int retainedDistance) {
        for (long key : regions) {
            int recentX = (int) (key >> 32);
            int recentZ = (int) key;
            long distance = Math.max(Math.abs((long) recentX - chunkX), Math.abs((long) recentZ - chunkZ));
            if (distance <= retainedDistance) return true;
        }
        return false;
    }

    private void rememberDemandRegion(long key) {
        rememberDemandRegion(recentDemandRegions, key, staleDistance(), RECENT_DEMAND_REGION_COUNT);
    }

    /** Nearby requests advance one stream marker; only a genuinely distant stream consumes another history slot. */
    static void rememberDemandRegion(ArrayDeque<Long> regions, long key, int retainedDistance, int maximumRegions) {
        int chunkX = (int) (key >> 32), chunkZ = (int) key;
        Long nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        for (long candidate : regions) {
            int candidateX = (int) (candidate >> 32), candidateZ = (int) candidate;
            long distance = Math.max(Math.abs((long) candidateX - chunkX), Math.abs((long) candidateZ - chunkZ));
            if (distance <= retainedDistance && distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        if (nearest != null) regions.remove(nearest);
        regions.addLast(key);
        while (regions.size() > maximumRegions) regions.removeFirst();
    }

    private int staleDistance() {
        return Math.max(4, radialPrefetchRadius(maximumPrefetchTasks) * 2 + 2);
    }

    private static int radialPrefetchRadius(int maximumTasks) {
        int radius = 0;
        while ((radius * 2 + 1) * (radius * 2 + 1) < maximumTasks) {
            radius++;
        }
        return radius;
    }

    private static long coordinateKey(int chunkX, int chunkZ) {
        return (long) chunkX << 32 ^ chunkZ & 0xFFFF_FFFFL;
    }

    private final class PrefetchTask {

        private static final int QUEUED = 0;
        private static final int RUNNING = 1;
        private static final int COMPLETE = 2;
        private static final int CANCELLED = 3;

        private final int chunkX;
        private final int chunkZ;
        private final AtomicInteger state = new AtomicInteger(QUEUED);
        private volatile Future<TerrainPayload> future;
        private volatile TerrainPayload payload;

        private PrefetchTask(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        private TerrainPayload execute() {
            if (!state.compareAndSet(QUEUED, RUNNING)) throw new CancellationException("Terrain prefetch cancelled");
            try {
                payload = compute(chunkX, chunkZ);
                return payload;
            } finally {
                activeTasks.decrementAndGet();
                synchronized (prefetched) {
                    if (!shuttingDown && payload != null) {
                        completedTerrain.put(coordinateKey(chunkX, chunkZ), payload);
                    }
                    // Publish before COMPLETE. A demand that observes completion can then await and remove the
                    // side-channel entry without the worker re-inserting it afterward.
                    state.set(COMPLETE);
                    pumpPlannedPrefetches();
                }
            }
        }

        private boolean cancelQueued() {
            if (!state.compareAndSet(QUEUED, CANCELLED)) return false;
            Future<TerrainPayload> submitted = future;
            if (submitted != null) submitted.cancel(false);
            activeTasks.decrementAndGet();
            return true;
        }

        private boolean evict() {
            return state.get() == COMPLETE || cancelQueued();
        }

        private boolean isComplete() {
            return state.get() == COMPLETE;
        }

        private TerrainPayload await() {
            try {
                return future.get();
            } catch (InterruptedException exception) {
                Thread.currentThread()
                    .interrupt();
                throw new IllegalStateException("Interrupted while waiting for Biomic terrain", exception);
            } catch (ExecutionException exception) {
                throw new IllegalStateException("Biomic terrain worker failed", exception.getCause());
            }
        }
    }

    private static final class ChunkRequest {

        private final int chunkX;
        private final int chunkZ;

        private ChunkRequest(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

    }

    static final class TerrainPayload {

        private final BiomeVolume biomeVolume;
        private final byte[] materials;
        private final byte[] authoredMaterials;
        private final BitSet caveProjectionMask;
        private final byte[] blockLight;

        TerrainPayload(BiomeVolume biomeVolume, byte[] materials, byte[] authoredMaterials, BitSet caveProjectionMask) {
            this(biomeVolume, materials, authoredMaterials, caveProjectionMask, null);
        }

        TerrainPayload(BiomeVolume biomeVolume, byte[] materials, byte[] authoredMaterials, BitSet caveProjectionMask,
            byte[] blockLight) {
            this.biomeVolume = biomeVolume;
            this.materials = materials;
            this.authoredMaterials = authoredMaterials;
            this.caveProjectionMask = caveProjectionMask == null ? null : (BitSet) caveProjectionMask.clone();
            this.blockLight = blockLight;
        }

        BiomeVolume getBiomeVolume() {
            return biomeVolume;
        }

        byte[] getMaterials() {
            return materials;
        }

        byte[] getAuthoredMaterials() {
            return authoredMaterials;
        }

        /** Block light computed with the terrain, one value per material cell, or null. */
        byte[] getBlockLight() {
            return blockLight;
        }

        BitSet getCaveProjectionMask() {
            return caveProjectionMask == null ? null : (BitSet) caveProjectionMask.clone();
        }
    }
}
