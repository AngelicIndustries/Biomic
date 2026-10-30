package industries.angelic.biomic.client;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Debug frame-time log, enabled with -Dbiomic.frameLog=path. Writes one line per wall-clock second
 * ("epochSecond frames worstMs") and one line per frame slower than 50 ms ("spike epochSecond ms").
 */
public final class FrameTimeLog {

    private final PrintWriter out;
    private long last;
    private long second;
    private int frames;
    private long worst;

    private FrameTimeLog(PrintWriter out) {
        this.out = out;
    }

    public static void registerIfEnabled() {
        String path = System.getProperty("biomic.frameLog");
        if (path == null || path.isEmpty()) return;
        try {
            FMLCommonHandler.instance()
                .bus()
                .register(new FrameTimeLog(new PrintWriter(new FileWriter(path), true)));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot open frame log " + path, e);
        }
    }

    @SubscribeEvent
    public void frame(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        long now = System.nanoTime();
        if (last != 0) {
            long frame = now - last;
            long sec = System.currentTimeMillis() / 1000L;
            if (sec != second) {
                if (second != 0) out.printf("%d %d %.1f%n", second, frames, worst / 1e6);
                second = sec;
                frames = 0;
                worst = 0;
            }
            frames++;
            worst = Math.max(worst, frame);
            if (frame > 50_000_000L) out.printf("spike %d %.1f%n", sec, frame / 1e6);
        }
        last = now;
    }
}
