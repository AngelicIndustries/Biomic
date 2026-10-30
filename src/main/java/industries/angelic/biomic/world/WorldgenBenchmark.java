package industries.angelic.biomic.world;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunkProvider;

import cpw.mods.fml.common.FMLCommonHandler;
import industries.angelic.biomic.Biomic;

/** Reproducible cold-generation benchmark matching vanilla's 25-by-25 spawn preparation window. */
public final class WorldgenBenchmark {

    public static final String ENABLE_PROPERTY = "biomic.worldgenBenchmark";
    public static final String RESULT_PROPERTY = "biomic.worldgenBenchmarkResult";

    private static final long WORLD_SEED = 42L;
    private static final int RADIUS_CHUNKS = integerProperty("biomic.worldgenBenchmarkRadius", 12, 1, 32);
    private static final int CENTER_CHUNK_X = Integer.getInteger("biomic.worldgenBenchmarkCenterChunkX", 256);
    private static final int CENTER_CHUNK_Z = Integer.getInteger("biomic.worldgenBenchmarkCenterChunkZ", -256);
    /** Optional list of extra window centers in chunk coordinates, "x,z;x,z;...", generated after the main one. */
    private static final String EXTRA_CENTERS = System.getProperty("biomic.worldgenBenchmarkCenters", "");
    /** 0 for the overworld, -1 for the Nether. */
    private static final int DIMENSION = Integer.getInteger("biomic.worldgenBenchmarkDimension", 0);
    /**
     * Load order inside a window: "grid" (x columns, z ascending; the default) or "shuffled" (a fixed shuffle, so
     * chunks populate with their neighbourhoods in other states, as a moving player loads them).
     */
    private static final String ORDER = System.getProperty("biomic.worldgenBenchmarkOrder", "grid");

    private WorldgenBenchmark() {}

    public static void runIfRequested() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        MinecraftServer server = MinecraftServer.getServer();
        File result = new File(requiredProperty(RESULT_PROPERTY));
        try {
            WorldServer world = server.worldServerForDimension(DIMENSION);
            if (world.getSeed() != WORLD_SEED) {
                throw new IllegalStateException("Worldgen benchmark requires seed " + WORLD_SEED);
            }
            IChunkProvider delegate = world.theChunkProviderServer.currentChunkProvider;
            if (!(delegate instanceof BenchmarkedChunkProvider provider)) {
                throw new IllegalStateException("Worldgen benchmark requires a Biomic world in dimension " + DIMENSION);
            }
            provider.resetPerformanceStats();

            java.lang.management.ThreadMXBean threads = java.lang.management.ManagementFactory.getThreadMXBean();
            long serverCpuStart = threads.getCurrentThreadCpuTime();
            long workerCpuStart = workerCpuNanos(threads);
            long chunksBefore = world.theChunkProviderServer.getLoadedChunkCount();
            long startedAt = System.nanoTime();
            generateWindow(world, CENTER_CHUNK_X, CENTER_CHUNK_Z);
            int windows = 1;
            for (String center : EXTRA_CENTERS.split(";")) {
                if (center.isEmpty()) continue;
                String[] parts = center.split(",");
                generateWindow(world, Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
                windows++;
            }
            long completedAt = System.nanoTime();
            long serverCpu = threads.getCurrentThreadCpuTime() - serverCpuStart;
            long workerCpu = workerCpuNanos(threads) - workerCpuStart;
            long chunks = Math.max(1, world.theChunkProviderServer.getLoadedChunkCount() - chunksBefore);

            List<String> details = provider.getPerformanceReport();
            StringBuilder report = new StringBuilder("PASS\n");
            report.append("seed=")
                .append(WORLD_SEED)
                .append(" center_chunk=")
                .append(CENTER_CHUNK_X)
                .append(',')
                .append(CENTER_CHUNK_Z)
                .append(" windows=")
                .append(windows)
                .append(" window=")
                .append(RADIUS_CHUNKS * 2 + 1)
                .append('x')
                .append(RADIUS_CHUNKS * 2 + 1)
                .append(" elapsed_ms=")
                .append(String.format(java.util.Locale.ROOT, "%.3f", (completedAt - startedAt) / 1_000_000.0D))
                .append('\n')
                .append(
                    String.format(
                        java.util.Locale.ROOT,
                        "CPU: %d new chunks, server thread %.3f ms/chunk, Biomic workers %.3f ms/chunk, total %.3f ms/chunk%n",
                        chunks,
                        serverCpu / 1e6 / chunks,
                        workerCpu / 1e6 / chunks,
                        (serverCpu + workerCpu) / 1e6 / chunks));
            for (String line : details) report.append(line)
                .append('\n');
            writeResult(result, report.toString());
            Biomic.LOG.info("Fixed-seed worldgen benchmark completed in {} ms", (completedAt - startedAt) / 1_000_000L);
            for (String line : details) Biomic.LOG.info("Benchmark: {}", line);
        } catch (Throwable failure) {
            try {
                writeResult(result, "FAIL\n" + failure + '\n');
            } catch (IOException writeFailure) {
                failure.addSuppressed(writeFailure);
            }
            Biomic.LOG.error("Fixed-seed worldgen benchmark failed", failure);
            FMLCommonHandler.instance()
                .exitJava(1, false);
            throw new AssertionError("FML refused to terminate a failed worldgen benchmark", failure);
        } finally {
            server.initiateShutdown();
        }
    }

    /** CPU time of Biomic's own worker threads ("Biomic ..." pool names). */
    private static long workerCpuNanos(java.lang.management.ThreadMXBean threads) {
        long total = 0;
        for (java.lang.management.ThreadInfo info : threads.getThreadInfo(threads.getAllThreadIds())) {
            if (info == null || !info.getThreadName()
                .startsWith("Biomic")) continue;
            long cpu = threads.getThreadCpuTime(info.getThreadId());
            if (cpu > 0) total += cpu;
        }
        return total;
    }

    private static void generateWindow(WorldServer world, int centerX, int centerZ) {
        java.util.List<int[]> chunks = new java.util.ArrayList<>();
        for (int chunkX = centerX - RADIUS_CHUNKS; chunkX <= centerX + RADIUS_CHUNKS; chunkX++) {
            for (int chunkZ = centerZ - RADIUS_CHUNKS; chunkZ <= centerZ + RADIUS_CHUNKS; chunkZ++) {
                chunks.add(new int[] { chunkX, chunkZ });
            }
        }
        if ("shuffled".equals(ORDER))
            java.util.Collections.shuffle(chunks, new java.util.Random(centerX * 31L + centerZ));
        else if (!"grid".equals(ORDER)) throw new IllegalArgumentException("Unknown benchmark load order " + ORDER);
        for (int[] chunk : chunks) world.theChunkProviderServer.loadChunk(chunk[0], chunk[1]);
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isEmpty()) throw new IllegalStateException("Missing system property " + name);
        return value;
    }

    private static int integerProperty(String name, int defaultValue, int minimum, int maximum) {
        int value = Integer.getInteger(name, defaultValue);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be in [" + minimum + ", " + maximum + "], got " + value);
        }
        return value;
    }

    private static void writeResult(File target, String contents) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create benchmark directory " + parent);
        }
        Files.write(target.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    }
}
