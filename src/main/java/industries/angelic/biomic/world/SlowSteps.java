package industries.angelic.biomic.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The slowest few terrain and population steps of a run, with the time of each phase, for diagnosing stalls (the
 * first chunk of a fresh area on a 2-core server). A step on the server thread opens laps; code it calls may add laps
 * of its own while one is open.
 */
public final class SlowSteps {

    private static final int KEEP = 6;
    private static final ThreadLocal<Laps> OPEN = new ThreadLocal<>();

    private final String kind;
    private final List<Entry> slowest = new ArrayList<>();

    private record Entry(int x, int z, long total, String phases) {}

    SlowSteps(String kind) {
        this.kind = kind;
    }

    /** Phase times of one step on this thread. */
    public static final class Laps {

        private final Laps outer;
        private final long started = System.nanoTime();
        private long last = started;
        private final StringBuilder phases = new StringBuilder();
        private final java.util.Map<String, long[]> notes = new java.util.LinkedHashMap<>();

        private Laps(Laps outer) {
            this.outer = outer;
        }

        public void lap(String phase) {
            long now = System.nanoTime();
            if (phases.length() > 0) phases.append(", ");
            phases.append(phase)
                .append(' ')
                .append(String.format(Locale.ROOT, "%.1f", (now - last) / 1e6));
            last = now;
        }
    }

    /** Opens a step on this thread; a step opened inside another (a chunk loaded mid-population) nests. */
    Laps open() {
        Laps laps = new Laps(OPEN.get());
        OPEN.set(laps);
        return laps;
    }

    /** Time spent on one kind of nested work inside the open step on this thread (summed, with a count). */
    public static void note(String what, long nanos) {
        Laps laps = OPEN.get();
        if (laps == null) return;
        long[] total = laps.notes.computeIfAbsent(what, key -> new long[2]);
        total[0]++;
        total[1] += nanos;
    }

    /** A phase inside the open step on this thread, if any (workers have none). */
    public static void lap(String phase) {
        Laps laps = OPEN.get();
        if (laps != null) laps.lap(phase);
    }

    void close(Laps laps, int chunkX, int chunkZ) {
        if (laps.outer == null) OPEN.remove();
        else OPEN.set(laps.outer);
        long total = System.nanoTime() - laps.started;
        for (var note : laps.notes.entrySet()) laps.phases.append("; ")
            .append(note.getKey())
            .append(" x")
            .append(note.getValue()[0])
            .append(' ')
            .append(String.format(Locale.ROOT, "%.1f", note.getValue()[1] / 1e6));
        synchronized (slowest) {
            if (slowest.size() == KEEP && slowest.get(KEEP - 1).total >= total) return;
            slowest.add(new Entry(chunkX, chunkZ, total, laps.phases.toString()));
            slowest.sort((a, b) -> Long.compare(b.total, a.total));
            if (slowest.size() > KEEP) slowest.remove(KEEP);
        }
    }

    List<String> report() {
        List<String> lines = new ArrayList<>();
        synchronized (slowest) {
            for (Entry entry : slowest) lines.add(
                String.format(
                    Locale.ROOT,
                    "Slow %s %d,%d: %.1f ms (%s)",
                    kind,
                    entry.x,
                    entry.z,
                    entry.total / 1e6,
                    entry.phases));
        }
        return lines;
    }

    void reset() {
        synchronized (slowest) {
            slowest.clear();
        }
    }
}
