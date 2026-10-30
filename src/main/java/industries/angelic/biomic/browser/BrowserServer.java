package industries.angelic.biomic.browser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import industries.angelic.biomic.config.DebugConfig;
import industries.angelic.biomic.network.BiomicNetwork;
import industries.angelic.biomic.world.NetherWorldData;

/**
 * The server side of the biome browser, a developer's tool: requests queue from the network thread and are answered on
 * the server's tick, for ops (or the single player with cheats) only.
 */
public final class BrowserServer {

    public static final BrowserServer INSTANCE = new BrowserServer();

    /** A request to answer on the tick, for an allowed player. */
    private record Pending(EntityPlayerMP player, java.util.function.Consumer<EntityPlayerMP> work) {}

    private static final ConcurrentLinkedQueue<Pending> PENDING = new ConcurrentLinkedQueue<>();

    private BrowserServer() {}

    static void enqueue(EntityPlayerMP player, BrowserMessages.Action action) {
        switch (action) {
            case CATALOGUE -> enqueue(player, p -> BiomicNetwork.send(new BrowserMessages.Catalogue(catalogue()), p));
        }
    }

    static void enqueueGo(EntityPlayerMP player, String key, BrowserCatalogue.Place place, boolean next) {
        if (key != null && place != null) enqueue(player, p -> go(p, key, place, next));
    }

    static void enqueueSurvey(EntityPlayerMP player) {
        enqueue(player, p -> {
            List<BiomeLocator.Target> targets = new ArrayList<>();
            for (BrowserCatalogue.Entry entry : catalogue())
                for (BrowserCatalogue.Place place : entry.places()) targets.add(target(entry, place));
            BiomeLocator.survey(p, targets);
        });
    }

    static void enqueueGoTo(EntityPlayerMP player, int dimension, int x, int y, int z) {
        enqueue(player, p -> BiomeLocator.goTo(p, dimension, x, y, z));
    }

    private static void enqueue(EntityPlayerMP player, java.util.function.Consumer<EntityPlayerMP> work) {
        if (player != null && PENDING.size() < 64) PENDING.add(new Pending(player, work));
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (Pending pending; (pending = PENDING.poll()) != null;) answer(pending);
        BiomeLocator.tick();
        if (probeFile != null && ++probeTicks == 40) {
            probe(probeFile);
            probeFile = null;
            MinecraftServer.getServer()
                .initiateShutdown();
        }
    }

    private static void answer(Pending pending) {
        EntityPlayerMP player = pending.player();
        if (player.playerNetServerHandler == null) return;
        if (!allowed(player)) {
            BiomicNetwork.send(new BrowserMessages.Catalogue(null), player);
            return;
        }
        pending.work()
            .accept(player);
    }

    private static void go(EntityPlayerMP player, String key, BrowserCatalogue.Place place, boolean next) {
        for (BrowserCatalogue.Entry entry : catalogue()) {
            if (!entry.key()
                .equals(key)) continue;
            if (!entry.places()
                .contains(place)) break;
            BiomeLocator.go(player, target(entry, place), next);
            return;
        }
        BiomicNetwork.send(
            new BrowserMessages.Status(
                "This world has no " + key
                    + " in "
                    + place.name()
                        .toLowerCase(java.util.Locale.ROOT)
                    + ".",
                BrowserMessages.Progress.DONE),
            player);
    }

    private static BiomeLocator.Target target(BrowserCatalogue.Entry entry, BrowserCatalogue.Place place) {
        return new BiomeLocator.Target(
            entry.key(),
            entry.id(),
            entry.name(),
            entry.habitat(),
            place,
            BrowserCatalogue.biome(entry.key()));
    }

    private static List<BrowserCatalogue.Entry> catalogue;
    private static java.lang.ref.WeakReference<MinecraftServer> catalogueServer = new java.lang.ref.WeakReference<>(
        null);

    /** The catalogue of the running server's world, built once. */
    private static List<BrowserCatalogue.Entry> catalogue() {
        MinecraftServer server = MinecraftServer.getServer();
        if (catalogue == null || catalogueServer.get() != server) {
            catalogue = BrowserCatalogue.build(
                netherBiomes(),
                industries.angelic.biomic.compat.biome.NativeHabitatWorldData.peek(server.worldServerForDimension(0)));
            catalogueServer = new java.lang.ref.WeakReference<>(server);
        }
        return catalogue;
    }

    private static String probeFile;
    private static int probeTicks;

    /**
     * A developer's check of Go ({@code -Dbiomic.browserProbe=<file>}): two seconds into the running server, during
     * its tick as a player's Go runs, every biome of the catalogue in each of its places is looked for from 0,0 and
     * landed on, into a table in the file; then the server stops.
     */
    public static void probeIfRequested() {
        probeFile = System.getProperty("biomic.browserProbe");
    }

    private static void probe(String file) {
        String match = System.getProperty("biomic.browserProbeMatch");
        WorldServer overworld = MinecraftServer.getServer()
            .worldServerForDimension(0);
        // Whether chunk generation is throttled now (a missing chunk comes back empty), as it is for a live Go.
        int far = 1 << 20;
        boolean throttled = !overworld.theChunkProviderServer.chunkExists(far, far)
            && overworld.theChunkProviderServer.provideChunk(far, far) instanceof net.minecraft.world.chunk.EmptyChunk;
        StringBuilder table = new StringBuilder(
            "provideChunk during the tick: " + (throttled ? "empty (throttled)" : "loads")
                + "\n\n| key | place | spot | landing | distance | ms |\n|---|---|---|---|---|---|\n");
        int found = 0, clear = 0, total = 0;
        for (BrowserCatalogue.Entry entry : catalogue()) for (BrowserCatalogue.Place place : entry.places()) {
            if (match != null && !entry.key()
                .matches(match)) continue;
            long start = System.nanoTime();
            int[] spot = BiomeLocator.probe(target(entry, place));
            long ms = (System.nanoTime() - start) / 1_000_000;
            String landing = spot == null ? "" : spot[3] == 1 ? "clear" : "fallback";
            industries.angelic.biomic.Biomic.LOG.info(
                "Browser probe: {} {} {} {} in {} ms",
                entry.key(),
                place,
                spot == null ? "not found" : spot[0] + " " + spot[1] + " " + spot[2],
                landing,
                ms);
            total++;
            if (spot != null) found++;
            if (spot != null && spot[3] == 1) clear++;
            table.append("| ")
                .append(entry.key())
                .append(" | ")
                .append(place)
                .append(" | ")
                .append(spot == null ? "not found" : spot[0] + " " + spot[1] + " " + spot[2])
                .append(" | ")
                .append(landing)
                .append(" | ")
                .append(spot == null ? "" : String.valueOf((int) Math.hypot(spot[0], spot[2])))
                .append(" | ")
                .append(ms)
                .append(" |\n");
        }
        table.append("\n")
            .append(found)
            .append(" of ")
            .append(total)
            .append(" found, ")
            .append(clear)
            .append(" on a clear spot\n");
        // The biomes the browser lists as never generated: a find means the list is out of date.
        table.append("\nListed as never generated:\n");
        for (BrowserCatalogue.Entry entry : catalogue()) {
            BrowserCatalogue.Place place = BrowserCatalogue.NEVER_GENERATED.get(entry.key());
            if (place == null || match != null && !entry.key()
                .matches(match)) continue;
            int[] spot = BiomeLocator.probe(target(entry, place));
            table.append("- ")
                .append(entry.key())
                .append(' ')
                .append(place)
                .append(": ")
                .append(
                    spot == null ? "not found"
                        : "FOUND at " + spot[0] + " " + spot[1] + " " + spot[2] + ", take it off the list")
                .append('\n');
        }
        try {
            java.nio.file.Files.write(
                java.nio.file.Paths.get(file),
                table.toString()
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }

    /**
     * A developer's dump of the biome catalogue ({@code -Dbiomic.biomeCatalogueDump=<file>}): every key with its kind,
     * tags and biome, as a Markdown table, when the server has started.
     */
    public static void dumpCatalogueIfRequested() {
        String file = System.getProperty("biomic.biomeCatalogueDump");
        if (file == null) return;
        StringBuilder table = new StringBuilder(
            "Biome catalogue (API version " + industries.angelic.biomic.api.BiomicApi.VERSION
                + "), complete: "
                + industries.angelic.biomic.api.BiomeCatalogue.isComplete()
                + "\n\n| key | kind | tags | biome |\n|---|---|---|---|\n");
        for (industries.angelic.biomic.api.BiomeCatalogue.Entry entry : industries.angelic.biomic.api.BiomeCatalogue
            .entries())
            table.append("| `")
                .append(entry.key())
                .append("` | ")
                .append(entry.kind())
                .append(" | ")
                .append(String.join(", ", entry.tags()))
                .append(" | ")
                .append(
                    entry.biome() == null ? "(the column's surface)"
                        : entry.biome().biomeName + " (" + entry.biome().biomeID + ")")
                .append(" |\n");
        try {
            java.nio.file.Files.write(
                java.nio.file.Paths.get(file),
                table.toString()
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }

    /** Ops, or the single player with cheats on; and only while the server allows the browser. */
    static boolean allowed(EntityPlayerMP player) {
        return DebugConfig.biomeBrowser && player.canCommandSenderUseCommand(2, "biomic");
    }

    private static List<BiomeGenBase> netherBiomes() {
        World nether = MinecraftServer.getServer()
            .worldServerForDimension(-1);
        return nether == null ? new ArrayList<>() : NetherWorldData.neighbourBiomes(nether);
    }
}
