package industries.angelic.biomic.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.ChunkCoordinates;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Client lifetime/cadence port of the pinned whistle smoke handler. */
public final class GardensWhistlecaneEffects {

    public static final GardensWhistlecaneEffects INSTANCE = new GardensWhistlecaneEffects();
    static final int WHISTLING_TIME = 50, SMOKE_TIME = 30;
    private final Map<ChunkCoordinates, Integer> effects = new HashMap<>();
    private WorldClient world;

    private GardensWhistlecaneEffects() {}

    public static void add(int x, int y, int z) {
        WorldClient current = Minecraft.getMinecraft().theWorld;
        if (current != INSTANCE.world) {
            INSTANCE.world = current;
            INSTANCE.effects.clear();
        }
        INSTANCE.effects.put(new ChunkCoordinates(x, y, z), WHISTLING_TIME);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        WorldClient current = Minecraft.getMinecraft().theWorld;
        if (current != world) {
            world = current;
            effects.clear();
            return;
        }
        if (current == null) return;
        Iterator<Map.Entry<ChunkCoordinates, Integer>> iterator = effects.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ChunkCoordinates p = entry.getKey();
            int remaining = entry.getValue();
            if (remaining < -SMOKE_TIME || current.getBlock(p.posX, p.posY, p.posZ)
                != industries.angelic.biomic.content.gardens.GardensContent.WHISTLECANE) {
                iterator.remove();
                continue;
            }
            int high = primaryParticles(remaining), after = remaining - 1;
            for (int i = 0; i < high; i++) spawn(current, p, .2, .05);
            if (hasTrailingParticle(remaining)) spawn(current, p, .1, 0);
            entry.setValue(after);
        }
    }

    static int primaryParticles(int remainingBeforeTick) {
        return remainingBeforeTick >= 0 ? 2 : 0;
    }

    static boolean hasTrailingParticle(int remainingBeforeTick) {
        return (remainingBeforeTick - 1) % 5 == 0;
    }

    private static void spawn(WorldClient world, ChunkCoordinates p, double verticalSpeed, double horizontalSpeed) {
        double width = 5 / 16D, x = p.posX + .5 + world.rand.nextDouble() * width - width / 2;
        double z = p.posZ + .5 + world.rand.nextDouble() * width - width / 2;
        double xs = world.rand.nextDouble() * horizontalSpeed - horizontalSpeed / 2;
        double zs = world.rand.nextDouble() * horizontalSpeed - horizontalSpeed / 2;
        industries.angelic.biomic.client.particle.GardensWhistleSmokeParticle
            .spawn(world, x, p.posY + 1, z, xs, verticalSpeed, zs);
    }
}
