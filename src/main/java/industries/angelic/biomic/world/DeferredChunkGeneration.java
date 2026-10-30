package industries.angelic.biomic.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import industries.angelic.biomic.config.PerformanceConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * Chunks a player's view asks for that are neither loaded nor saved are generated over the following ticks, nearest a
 * player first and within a time budget per tick, instead of all at once on the server thread: at a view distance of
 * 32 a join or a trip to the Nether asked for some 4,000 new chunks and stalled the server for 30 seconds, so the
 * client timed out (review, 2026-10-24). Forge's chunk watchers already wait on chunks read from disk through a
 * runnable ({@code ChunkProviderServer#loadChunk(int, int, Runnable)}); a deferred chunk runs that runnable once it is
 * generated, and {@code ChunkIOExecutor#dropQueuedChunkLoad} drops it, as for Forge's own queue. Chunks close to a
 * player load at once, so a player always stands on loaded ground. Only Biomic's generators defer.
 *
 * <p>
 * A cold chunk (and the populations its arrival completes) can take far longer than a tick's budget, so the budget is
 * kept as time debt: a slow chunk is paid back over the following ticks instead of every tick overrunning (a second
 * review found the server 9 seconds behind). The terrain workers start the next chunks of the queue ahead of time, and
 * vanilla's per-tick passes over the chunks near players (random ticks, mood sounds) skip the ones still waiting,
 * which they would otherwise generate on the spot. The budget also shrinks to what the server's recent ticks leave of
 * their 50 ms (Hodgepodge throttles its own generation alongside, and the Nether's random ticks are heavy), so the
 * server is never kept behind; one chunk still goes through every two seconds however busy it is.
 * </p>
 *
 * <p>
 * A player standing in a Nether portal has the terrain around the far end started on the workers: crossing, the
 * portal looks for its twin over about 17 by 17 chunks there and loads every one of them.
 * </p>
 */
public final class DeferredChunkGeneration {

    public static final DeferredChunkGeneration INSTANCE = new DeferredChunkGeneration();

    /** Per world: chunk -> the watchers' runnables waiting for it. */
    private final Map<WorldServer, Long2ObjectOpenHashMap<List<Runnable>>> queues = new WeakHashMap<>();
    /** Generation time owed beyond the budget, in nanoseconds; no chunk starts while any is owed. */
    private long debt;
    /** This handler's average time a tick (nanoseconds, about the last 20), and ticks since it last generated. */
    private double ownAverage;
    private int idleTicks;
    private static final long TICK = 50_000_000L, TICK_MARGIN = 5_000_000L;
    /** However busy the server, one chunk goes through after this many ticks without one. */
    private static final int STARVATION_TICKS = 40;
    private static final long MAX_DEBT = 2_000_000_000L;
    /** How many of a queue's next chunks the terrain workers are asked to start each tick. */
    private static final int LOOK_AHEAD = 32;

    private DeferredChunkGeneration() {}

    /** Whether {@code loadChunk(x, z, runnable)} waits for a later tick (it then returns null, as for a disk read). */
    public boolean defer(ChunkProviderServer provider, int chunkX, int chunkZ, Runnable runnable) {
        if (runnable == null || !deferring(provider)) return false;
        WorldServer world = provider.worldObj;
        if (provider.chunkExists(chunkX, chunkZ)) return false;
        if (provider.currentChunkLoader instanceof AnvilChunkLoader loader && loader.chunkExists(world, chunkX, chunkZ))
            return false;
        if (nearestPlayer(world, chunkX, chunkZ) <= PerformanceConfig.initialSynchronousChunkRadius) return false;
        queues.computeIfAbsent(world, ignored -> new Long2ObjectOpenHashMap<>())
            .computeIfAbsent(ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ), ignored -> new ArrayList<>(2))
            .add(runnable);
        ((TerrainPrefetching) provider.currentChunkProvider).prefetchTerrain(chunkX, chunkZ);
        return true;
    }

    private static boolean deferring(ChunkProviderServer provider) {
        return PerformanceConfig.deferInitialChunkGeneration
            && provider.currentChunkProvider instanceof TerrainPrefetching;
    }

    /**
     * Vanilla's chunks near players for this tick (World#activeChunkSet), without those still waiting here or touching
     * one: its relight checks read a block into each side neighbour, which would generate it on the spot.
     */
    public void dropWaiting(World world, java.util.Set<ChunkCoordIntPair> active) {
        if (!(world instanceof WorldServer server) || server.theChunkProviderServer == null
            || !deferring(server.theChunkProviderServer)) return;
        ChunkProviderServer provider = server.theChunkProviderServer;
        List<ChunkCoordIntPair> loaded = new ArrayList<>(active.size());
        for (ChunkCoordIntPair chunk : active) {
            int x = chunk.chunkXPos, z = chunk.chunkZPos;
            if (provider.chunkExists(x, z) && provider.chunkExists(x - 1, z)
                && provider.chunkExists(x + 1, z)
                && provider.chunkExists(x, z - 1)
                && provider.chunkExists(x, z + 1)) loaded.add(chunk);
        }
        if (loaded.size() == active.size()) return;
        // Only clear and add: other mods' replacements of this set need not support removal.
        active.clear();
        active.addAll(loaded);
    }

    /**
     * A watcher stopped waiting (Forge's ChunkIOExecutor#dropQueuedChunkLoad); whether the runnable was waiting here,
     * in which case Forge's own queue never held it and must not be asked (it logs "This should not happen").
     */
    public boolean drop(World world, int chunkX, int chunkZ, Runnable runnable) {
        Long2ObjectOpenHashMap<List<Runnable>> queue = queues.get(world);
        if (queue == null) return false;
        long key = ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ);
        List<Runnable> waiting = queue.get(key);
        if (waiting == null || !waiting.remove(runnable)) return false;
        if (waiting.isEmpty()) queue.remove(key);
        return true;
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (WorldServer world : MinecraftServer.getServer().worldServers)
            if (world != null && (world.provider.dimensionId == 0 || world.provider.dimensionId == -1))
                prefetchPortalTwins(world);
        if (queues.isEmpty()) {
            debt = 0L;
            ownAverage *= 0.95D;
            return;
        }
        long allowed = Math.min(PerformanceConfig.initialChunkGenBudgetMs * 1_000_000L, room());
        debt = Math.max(0L, debt - allowed);
        long start = System.nanoTime(), deadline = start + allowed - debt;
        boolean starving = ++idleTicks > STARVATION_TICKS;
        if (starving) deadline = Math.max(deadline, start + 1);
        int reach = MinecraftServer.getServer()
            .getConfigurationManager()
            .getViewDistance() + 1;
        for (Map.Entry<WorldServer, Long2ObjectOpenHashMap<List<Runnable>>> entry : new ArrayList<>(
            queues.entrySet())) {
            WorldServer world = entry.getKey();
            Long2ObjectOpenHashMap<List<Runnable>> queue = entry.getValue();
            // Nearest first; chunks no watcher can still need are dropped with their runnables.
            long[] keys = queue.keySet()
                .toLongArray();
            int[] distances = new int[keys.length];
            for (int i = 0; i < keys.length; i++)
                distances[i] = nearestPlayer(world, (int) keys[i], (int) (keys[i] >> 32));
            Integer[] order = new Integer[keys.length];
            for (int i = 0; i < keys.length; i++) order[i] = i;
            java.util.Arrays.sort(order, (a, b) -> Integer.compare(distances[a], distances[b]));
            int ahead = 0;
            if (world.theChunkProviderServer.currentChunkProvider instanceof TerrainPrefetching workers)
                for (int i : order) {
                    if (distances[i] > reach) continue;
                    if (++ahead > LOOK_AHEAD || !workers.prefetchTerrain((int) keys[i], (int) (keys[i] >> 32))) break;
                }
            for (int i : order) {
                if (distances[i] > reach) {
                    queue.remove(keys[i]);
                    continue;
                }
                if (System.nanoTime() >= deadline) break;
                // Only chunks whose terrain the workers have finished: the rest would be computed here, on the spot.
                if (!starving && world.theChunkProviderServer.currentChunkProvider instanceof TerrainPrefetching workers
                    && !workers.terrainReady((int) keys[i], (int) (keys[i] >> 32))) continue;
                List<Runnable> waiting = queue.remove(keys[i]);
                if (waiting == null) continue;
                idleTicks = 0;
                starving = false;
                world.theChunkProviderServer.loadChunk((int) keys[i], (int) (keys[i] >> 32));
                for (Runnable runnable : waiting) runnable.run();
            }
            if (queue.isEmpty()) queues.remove(world);
        }
        // Whatever this tick spent past its budget is owed by the next ones.
        long spent = System.nanoTime() - start;
        debt = Math.min(MAX_DEBT, debt + Math.max(0L, spent - allowed));
        ownAverage = ownAverage * 0.95D + spent * 0.05D;
    }

    /** Vanilla's portal search reaches 128 blocks around the far end. */
    private static final int PORTAL_REACH = 8;

    private void prefetchPortalTwins(WorldServer world) {
        for (Object entity : world.playerEntities) {
            EntityPlayer player = (EntityPlayer) entity;
            int x = MathHelper.floor_double(player.posX), y = MathHelper.floor_double(player.posY);
            int z = MathHelper.floor_double(player.posZ);
            if (!(world.getBlock(x, y, z) instanceof net.minecraft.block.BlockPortal)) continue;
            boolean toNether = world.provider.dimensionId == 0;
            WorldServer far = net.minecraftforge.common.DimensionManager.getWorld(toNether ? -1 : 0);
            if (far == null || far.theChunkProviderServer == null || !deferring(far.theChunkProviderServer)) continue;
            TerrainPrefetching workers = (TerrainPrefetching) far.theChunkProviderServer.currentChunkProvider;
            int cx = (toNether ? Math.floorDiv(x, 8) : x * 8) >> 4, cz = (toNether ? Math.floorDiv(z, 8) : z * 8) >> 4;
            for (int ring = 0; ring <= PORTAL_REACH; ring++)
                for (int dx = -ring; dx <= ring; dx++) for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring
                        || far.theChunkProviderServer.chunkExists(cx + dx, cz + dz)) continue;
                    if (!workers.prefetchTerrain(cx + dx, cz + dz)) return;
                }
        }
    }

    /** What the server's last 20 ticks left of a tick, besides this handler's own share. */
    private long room() {
        MinecraftServer server = MinecraftServer.getServer();
        long[] times = server.tickTimeArray;
        int counter = server.getTickCounter();
        long sum = 0L;
        for (int i = 1; i <= 20; i++) sum += times[Math.floorMod(counter - i, times.length)];
        long others = Math.max(0L, sum / 20 - (long) ownAverage);
        return Math.max(0L, TICK - TICK_MARGIN - others);
    }

    /** The chessboard distance in chunks to the nearest player in the world (MAX_VALUE with none). */
    private static int nearestPlayer(WorldServer world, int chunkX, int chunkZ) {
        int nearest = Integer.MAX_VALUE;
        for (Object entity : world.playerEntities) {
            EntityPlayer player = (EntityPlayer) entity;
            int dx = Math.abs((MathHelper.floor_double(player.posX) >> 4) - chunkX);
            int dz = Math.abs((MathHelper.floor_double(player.posZ) >> 4) - chunkZ);
            nearest = Math.min(nearest, Math.max(dx, dz));
        }
        return nearest;
    }
}
