package industries.angelic.biomic.world;

import java.util.Locale;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Server-thread counters: distinguishes individual stalls from vanilla's accumulated catch-up warning. Each sample is
 * one pass of the server loop, from a tick's start to the next one's, so it covers the tick, every mod's tick handlers
 * (Distant Horizons runs its generation tasks at the tick's end) and the sleep up to 50 ms.
 */
public final class ServerTickTiming {

    private static final Counter COUNTER = new Counter();

    private ServerTickTiming() {}

    /** Times the server loop when a run asked for Biomic's performance report (-Dbiomic.performanceReportOnStop). */
    public static void enableIfRequested() {
        if (!Boolean.getBoolean("biomic.performanceReportOnStop")) return;
        FMLCommonHandler.instance()
            .bus()
            .register(new Listener());
    }

    public static void reset() {
        COUNTER.reset();
    }

    public static String report() {
        return COUNTER.report();
    }

    public static final class Listener {

        private long tickStart;

        @SubscribeEvent
        public void tick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
            long now = System.nanoTime();
            if (tickStart != 0) COUNTER.record(now - tickStart);
            tickStart = now;
        }
    }

    static final class Counter {

        /** A pass over this is a tick the server could not fit in its 50 ms: the next one starts late. */
        private static final long LATE_NANOS = 60_000_000L;

        long ticks, total, maximum, over100ms, over500ms, over1s, over2s;
        /** The current and longest stretch of consecutive late passes: how long players saw the server lag. */
        long lateRun, lateRunTicks, longestLateRun, longestLateRunTicks;

        synchronized void reset() {
            ticks = total = maximum = over100ms = over500ms = over1s = over2s = 0;
            lateRun = lateRunTicks = longestLateRun = longestLateRunTicks = 0;
        }

        synchronized void record(long nanos) {
            ticks++;
            total += nanos;
            maximum = Math.max(maximum, nanos);
            if (nanos > 100_000_000L) over100ms++;
            if (nanos > 500_000_000L) over500ms++;
            if (nanos > 1_000_000_000L) over1s++;
            if (nanos > 2_000_000_000L) over2s++;
            if (nanos > LATE_NANOS) {
                lateRun += nanos;
                lateRunTicks++;
                if (lateRun > longestLateRun) {
                    longestLateRun = lateRun;
                    longestLateRunTicks = lateRunTicks;
                }
            } else {
                lateRun = lateRunTicks = 0;
            }
        }

        synchronized String report() {
            return String.format(
                Locale.ROOT,
                "Server loop passes: %d, avg %.1f ms, max %.0f ms; >100ms %d, >500ms %d, >1s %d, >2s %d; longest lag %.0f ms over %d late passes",
                ticks,
                ticks == 0 ? 0.0D : total / (double) ticks / 1_000_000.0D,
                maximum / 1_000_000.0D,
                over100ms,
                over500ms,
                over1s,
                over2s,
                longestLateRun / 1_000_000.0D,
                longestLateRunTicks);
        }
    }
}
