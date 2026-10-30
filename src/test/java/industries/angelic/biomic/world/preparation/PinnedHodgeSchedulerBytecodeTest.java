package industries.angelic.biomic.world.preparation;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.Assume;
import org.junit.Test;

/** Optional in the generic suite; required in the frozen handoff with the exact pinned jar property. */
public class PinnedHodgeSchedulerBytecodeTest {

    private static final String PIN = "e3bb8bc64d9ad47e802a2090ea3741a83044214e3f778297aff6478b53a406e0";

    @Test
    public void everyPinnedNativeSchedulerEntryMatchesTheInterceptionPlan() throws Exception {
        String named = System.getProperty("biomic.pinnedHodgeJar");
        Assume
            .assumeTrue("Run handoff with -Dbiomic.pinnedHodgeJar=<exact dev jar>", named != null && !named.isBlank());
        Path jar = Path.of(named);
        assertTrue(Files.isRegularFile(jar));
        assertEquals(
            PIN,
            HexFormat.of()
                .formatHex(
                    MessageDigest.getInstance("SHA-256")
                        .digest(Files.readAllBytes(jar))));
        Process javap = new ProcessBuilder(
            "javap",
            "-classpath",
            jar.toString(),
            "-c",
            "-p",
            "-l",
            "com.mitchej123.hodgepodge.mixins.hooks.ChunkGenScheduler").redirectErrorStream(true)
                .start();
        String text = new String(
            javap.getInputStream()
                .readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(text, 0, javap.waitFor());
        String fill = method(text, "private int fillMissingNeighbors(", "private static boolean isWithinChunkRange(");
        String aged = method(text, "private void forceProcessAged(", "private void pruneAndSort(");
        String terrain = method(text, "private int generateTerrain(", "private int processFills(");
        String fills = method(text, "private int processFills(", "private int processPopulationQueue(");
        String tick = method(text, "public void processTick(", "private int generateTerrain(");
        String prune = method(text, "private void pruneAndSort(", "private void rebuildGenWorkList(");
        String watchedRaw = method(text, "public void trackIfUnpopulated(", "private void trackTerrain(");
        String rawBody = method(text, "private void trackTerrain(", "public boolean shouldProtectFromUnload(");
        assertTrue(
            text.contains("public static com.mitchej123.hodgepodge.mixins.hooks.ChunkGenScheduler forDimension(int);"));
        assertTrue(watchedRaw.contains("Method trackTerrain:"));
        assertTrue(rawBody.contains("Chunk.isTerrainPopulated"));
        assertTrue(rawBody.contains("Long2ByteMap.containsKey"));
        assertTrue(rawBody.contains("LongList.add"));
        assertTrue(fill.contains("110: invokevirtual") && fill.contains("ChunkProviderServer.provideChunk"));
        assertTrue(aged.contains("303: invokevirtual") && aged.contains("ChunkProviderServer.provideChunk"));
        assertTrue(terrain.contains("124: invokeinterface") && terrain.contains("LongList.removeLong"));
        assertTrue(terrain.contains("278: invokeinterface") && terrain.contains("ChunkLoadCallback.loadChunk"));
        assertTrue(prune.contains("532: invokeinterface") && prune.contains("ChunkLoadCallback.loadChunk"));
        assertTrue(prune.contains("327: invokevirtual") && prune.contains("ChunkProviderServer.chunkExists"));
        assertTrue(prune.contains("343: invokevirtual") && prune.contains("AnvilChunkLoader.chunkExists"));
        // Pinned queue is live, not a snapshot. A native neighbor load can append a root
        // before this size check's next iteration, so scheduler-local priority is required.
        assertTrue(fills.contains("44: invokeinterface") && fills.contains("LongList.size"));
        assertTrue(fills.contains("58: invokeinterface") && fills.contains("LongList.getLong"));
        assertTrue(fills.contains("242: invokespecial") && fills.contains("fillMissingNeighbors"));
        assertTrue(fills.contains("335: invokeinterface") && fills.contains("LongList.removeElements"));
        assertTrue(tick.indexOf("Method processFills:") < tick.indexOf("Method generateTerrain:"));
        assertTrue(tick.indexOf("Method processFills:") < tick.indexOf("Method forceProcessAged:"));
        assertEquals(1, count(fill, "ChunkProviderServer.provideChunk"));
        assertEquals(1, count(aged, "ChunkProviderServer.provideChunk"));
        assertEquals(1, count(terrain, "ChunkLoadCallback.loadChunk"));
        assertEquals(1, count(prune, "ChunkLoadCallback.loadChunk"));
    }

    private static String method(String disassembly, String begin, String end) {
        int from = disassembly.indexOf(begin);
        assertTrue(begin, from >= 0);
        int until = disassembly.indexOf(end, from + begin.length());
        assertTrue(end, until > from);
        return disassembly.substring(from, until);
    }

    private static int count(String text, String needle) {
        return (text.length() - text.replace(needle, "")
            .length()) / needle.length();
    }
}
