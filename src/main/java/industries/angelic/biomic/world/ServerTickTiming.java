package industries.angelic.biomic.world;

import java.util.Locale;

/** Server-thread counters: distinguishes individual stalls from vanilla's accumulated catch-up warning. */
public final class ServerTickTiming {

    private static final Counter COUNTER = new Counter();

    private ServerTickTiming() {}

    public static void reset() {
        COUNTER.reset();
    }

    public static String report() {
        return COUNTER.report();
    }

    static final class Counter {

        long ticks, total, maximum, over50ms, over1s, over5s;

        void reset() {
            ticks = total = maximum = over50ms = over1s = over5s = 0;
        }

        String report() {
            return String.format(
                Locale.ROOT,
                "Whole-server ticks since reset: %d, avg %.3f ms, max %.3f ms; >50ms %d, >1s %d, >5s %d",
                ticks,
                ticks == 0 ? 0.0D : total / (double) ticks / 1_000_000.0D,
                maximum / 1_000_000.0D,
                over50ms,
                over1s,
                over5s);
        }
    }
}
