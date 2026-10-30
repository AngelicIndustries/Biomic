package industries.angelic.biomic.browser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;

import industries.angelic.biomic.api.BiomeKeys;
import industries.angelic.biomic.api.BiomicBiomeApi;
import industries.angelic.biomic.browser.BrowserCatalogue.Place;
import industries.angelic.biomic.network.BiomicNetwork;
import industries.angelic.biomic.world.BiomeLocating;
import industries.angelic.biomic.world.BiomeSearch;
import industries.angelic.biomic.world.terrain.FloatingIslandArchetype;
import industries.angelic.biomic.world.terrain.FloatingIslandDensitySampler;
import industries.angelic.biomic.world.terrain.ShiftedSkyIslands;

/**
 * The biome browser's Go and Next. The search runs on its own thread with Biomic's samplers and generates nothing: the
 * surface and the Nether by column on rings around the player, the sky by island cluster, caves by the raw 3D biome
 * volume. On the server's tick the nearest finds are then checked in generated chunks for a floor with room to stand
 * where the biome really is, and the player lands there, across dimensions without a portal. Next goes on from where
 * Go started, past every spot already visited.
 */
final class BiomeLocator {

    /** What to look for: the browser's entry, its biome and the place chosen. */
    record Target(String key, int id, String name, String habitat, Place place, BiomeGenBase biome) {}

    private record Candidate(int x, int y, int z) {}

    /** Where a player's Go started for a biome, and the spots visited since. */
    private record Trail(String key, Place place, int originX, int originZ, List<int[]> visited) {}

    private static final int SURFACE_RADIUS = 32768, NETHER_RADIUS = 12000, SKY_RADIUS = 20000, CAVE_RADIUS = 6144;
    /** Next skips every find this close to a spot already visited. */
    private static final int SPACING = 1024;
    private static final int CANDIDATES = 6;

    private static final ExecutorService SEARCH = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Biomic biome search");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private static final Map<UUID, Trail> TRAILS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> TICKETS = new ConcurrentHashMap<>();
    private static final AtomicLong NEXT_TICKET = new AtomicLong();
    /** Work the search hands back to the server thread. */
    private static final ConcurrentLinkedQueue<Runnable> SERVER = new ConcurrentLinkedQueue<>();

    private BiomeLocator() {}

    static void tick() {
        // What is queued now; a landing that queues its next try runs it next tick.
        for (int n = SERVER.size(); n > 0; n--) {
            Runnable work = SERVER.poll();
            if (work == null) break;
            work.run();
        }
    }

    static void go(EntityPlayerMP player, Target target, boolean next) {
        int dimension = target.place() == Place.NETHER ? -1 : 0;
        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(dimension);
        UUID id = player.getUniqueID();
        Trail old = TRAILS.get(id);
        Trail trail = next && old != null
            && old.key()
                .equals(target.key())
            && old.place() == target.place()
                ? old
                : new Trail(
                    target.key(),
                    target.place(),
                    origin(player, player.posX, dimension),
                    origin(player, player.posZ, dimension),
                    new ArrayList<>());
        Search search = world == null ? null : search(new BiomeSearch.Samplers(world), target);
        if (search == null) {
            status(
                player,
                dimension == -1 ? "Biomic does not generate this world's Nether."
                    : BiomeSearch.locating(world) == null ? "Biomic does not generate this world's overworld."
                        : "This world has no sky islands.");
            return;
        }
        long ticket = NEXT_TICKET.incrementAndGet();
        TICKETS.put(id, ticket);
        List<int[]> visited = new ArrayList<>(trail.visited());
        status(
            player,
            (next ? "Looking further for " : "Looking for ") + target.name() + "…",
            BrowserMessages.Progress.LOOKING);
        SEARCH.execute(() -> {
            long[] lastProgress = { System.nanoTime() };
            List<Candidate> found;
            try {
                found = search.run(
                    trail.originX(),
                    trail.originZ(),
                    visited,
                    () -> !Long.valueOf(ticket)
                        .equals(TICKETS.get(id)),
                    radius -> {
                        if (System.nanoTime() - lastProgress[0] < 1_500_000_000L) return;
                        lastProgress[0] = System.nanoTime();
                        SERVER.add(
                            () -> status(
                                player,
                                "Looking for " + target.name() + ", " + radius + " blocks out…",
                                BrowserMessages.Progress.LOOKING));
                    });
            } catch (RuntimeException failure) {
                SERVER.add(() -> status(player, "The search failed: " + failure));
                return;
            }
            SERVER.add(() -> land(player, target, world, found, 0, trail, ticket));
        });
    }

    /**
     * Go for no one from 0,0, as a live Go runs (the search on its thread, the landing on the server's, during the
     * tick): where a player would land, {x, y, z, 1 when the spot is clear or 0 for the fallback}, or null when nothing
     * is found (BrowserServer's probe).
     */
    static int[] probe(Target target) {
        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(target.place() == Place.NETHER ? -1 : 0);
        Search search = world == null ? null : search(new BiomeSearch.Samplers(world), target);
        if (search == null) return null;
        long deadline = System.nanoTime() + 120_000_000_000L;
        List<Candidate> found;
        try {
            found = SEARCH
                .submit(() -> search.run(0, 0, new ArrayList<>(), () -> System.nanoTime() > deadline, radius -> {}))
                .get();
        } catch (InterruptedException | java.util.concurrent.ExecutionException failure) {
            throw new IllegalStateException(failure);
        }
        if (found.isEmpty()) return null;
        for (Candidate candidate : found) {
            int[] spot = spot(world, target, candidate);
            if (spot != null) return new int[] { spot[0], spot[1], spot[2], 1 };
        }
        int[] spot = fallback(world, target, found.get(0));
        return new int[] { spot[0], spot[1], spot[2], 0 };
    }

    /** The distance survey each player started last; a newer one stops the older. */
    private static final Map<UUID, Long> SURVEYS = new ConcurrentHashMap<>();
    private static final int SURVEY_BATCH = 8;

    /**
     * Every target's nearest find from where the player stands, sent to the player as they come: the search alone,
     * nothing generated, one target a task on the search thread, so a Go waits for one target at most.
     */
    static void survey(EntityPlayerMP player, List<Target> targets) {
        UUID id = player.getUniqueID();
        long ticket = NEXT_TICKET.incrementAndGet();
        SURVEYS.put(id, ticket);
        MinecraftServer server = MinecraftServer.getServer();
        WorldServer overworld = server.worldServerForDimension(0), nether = server.worldServerForDimension(-1);
        BiomeSearch.Samplers overworldSamplers = new BiomeSearch.Samplers(overworld);
        BiomeSearch.Samplers netherSamplers = new BiomeSearch.Samplers(nether);
        List<Search> searches = new ArrayList<>();
        for (Target target : targets) searches.add(
            target.place() == Place.NETHER ? nether == null ? null : search(netherSamplers, target)
                : overworld == null ? null : search(overworldSamplers, target));
        int[] origin = { origin(player, player.posX, 0), origin(player, player.posZ, 0),
            origin(player, player.posX, -1), origin(player, player.posZ, -1) };
        surveyStep(player, ticket, targets, searches, origin, 0, new ArrayList<>(), new long[] { System.nanoTime() });
    }

    private static void surveyStep(EntityPlayerMP player, long ticket, List<Target> targets, List<Search> searches,
        int[] origin, int index, List<BrowserMessages.Distance> batch, long[] lastSent) {
        UUID id = player.getUniqueID();
        SEARCH.execute(() -> {
            java.util.function.BooleanSupplier stale = () -> !Long.valueOf(ticket)
                .equals(SURVEYS.get(id)) || player.playerNetServerHandler == null;
            if (stale.getAsBoolean()) return;
            Target target = targets.get(index);
            Search search = searches.get(index);
            int distance = -1;
            if (search != null) {
                boolean inNether = target.place() == Place.NETHER;
                int originX = origin[inNether ? 2 : 0], originZ = origin[inNether ? 3 : 1];
                long deadline = System.nanoTime() + 60_000_000_000L;
                List<Candidate> found;
                try {
                    found = search.run(
                        originX,
                        originZ,
                        List.of(),
                        () -> stale.getAsBoolean() || System.nanoTime() > deadline,
                        radius -> {});
                } catch (RuntimeException failure) {
                    found = List.of();
                }
                if (!found.isEmpty()) distance = (int) Math.hypot(
                    found.get(0)
                        .x() - originX,
                    found.get(0)
                        .z() - originZ);
            }
            batch.add(new BrowserMessages.Distance(target.key(), target.place(), distance));
            boolean last = index + 1 == targets.size();
            if (last || batch.size() >= SURVEY_BATCH || System.nanoTime() - lastSent[0] > 1_000_000_000L) {
                List<BrowserMessages.Distance> sent = new ArrayList<>(batch);
                batch.clear();
                lastSent[0] = System.nanoTime();
                int done = index + 1, total = targets.size();
                SERVER.add(() -> {
                    if (player.playerNetServerHandler != null)
                        BiomicNetwork.send(new BrowserMessages.Distances(sent, done, total), player);
                });
            }
            if (!last) surveyStep(player, ticket, targets, searches, origin, index + 1, batch, lastSent);
        });
    }

    /** Back to a spot (an issue marked there): its chunks loaded, the player on it; a running search is dropped. */
    static void goTo(EntityPlayerMP player, int dimension, int x, int y, int z) {
        TICKETS.put(player.getUniqueID(), NEXT_TICKET.incrementAndGet());
        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(dimension);
        if (world == null) {
            status(player, "This server has no dimension " + dimension + ".");
            return;
        }
        load(world, x, z);
        teleport(player, world, new int[] { x, y, z });
        BiomicNetwork.send(
            new BrowserMessages.Status("Back at " + x + " " + y + " " + z + ".", BrowserMessages.Progress.LANDED),
            player);
    }

    /** A coordinate of the player in the target dimension (the Nether is eight times smaller). */
    private static int origin(EntityPlayerMP player, double coordinate, int dimension) {
        double scale = player.dimension == dimension ? 1 : dimension == -1 ? 1 / 8D : player.dimension == -1 ? 8 : 1;
        return (int) Math.floor(coordinate * scale);
    }

    // -------------------------------------------------------------------------------------------------------- search

    @FunctionalInterface
    private interface Search {

        List<Candidate> run(int originX, int originZ, List<int[]> visited, java.util.function.BooleanSupplier cancelled,
            java.util.function.IntConsumer progress);
    }

    private static Search search(BiomeSearch.Samplers samplers, Target target) {
        switch (target.place()) {
            case NETHER: {
                BiomeSearch.ColumnTest test = samplers.netherTest(target.id());
                return test == null ? null
                    : (x, z, visited, cancelled,
                        progress) -> columns(test, NETHER_RADIUS, x, z, visited, cancelled, progress);
            }
            case SURFACE: {
                BiomeSearch.ColumnTest test = samplers.surfaceTest(target.biome());
                return test == null ? null
                    : (x, z, visited, cancelled,
                        progress) -> columns(test, SURFACE_RADIUS, x, z, visited, cancelled, progress);
            }
            case CAVES: {
                BiomeLocating locating = samplers.locating();
                Set<String> sources = BiomeSearch.caveSources(target.key());
                boolean displaced = BiomeSearch.displacedCave(target.key());
                return locating == null ? null
                    : (x, z, visited, cancelled,
                        progress) -> caves(locating, sources, displaced, x, z, visited, cancelled, progress);
            }
            default: {
                BiomeLocating locating = samplers.locating();
                ShiftedSkyIslands islands = locating == null ? null : locating.skyIslands();
                return islands == null ? null
                    : (x, z, visited, cancelled, progress) -> sky(islands, target, x, z, visited, cancelled, progress);
            }
        }
    }

    /**
     * Columns on square rings around the origin, 16 blocks apart (32 beyond 4096, 64 beyond 16384), until a few finds
     * near the first. The rings are centred on a column 16 modulo 32, so out to 16384 they pass every centre of the
     * habitats' 32-block cells (and beyond, every other one: a province has at least 24).
     */
    private static List<Candidate> columns(BiomeSearch.ColumnTest test, int radius, int originX, int originZ,
        List<int[]> visited, java.util.function.BooleanSupplier cancelled, java.util.function.IntConsumer progress) {
        List<Candidate> found = new ArrayList<>();
        int centerX = Math.floorDiv(originX, 32) * 32 + 16, centerZ = Math.floorDiv(originZ, 32) * 32 + 16;
        int limit = Integer.MAX_VALUE;
        for (int r = 0; r <= radius && r <= limit;) {
            if (cancelled.getAsBoolean()) return found;
            progress.accept(r);
            int step = r < 4096 ? 16 : r < 16384 ? 32 : 64;
            for (int[] point : ring(centerX, centerZ, r, step)) {
                int x = point[0], z = point[1];
                if (near(visited, x, z, SPACING) || nearFound(found, x, z, 48) || !test.test(x, z)) continue;
                found.add(new Candidate(x, -1, z));
                if (limit == Integer.MAX_VALUE) limit = r + 128;
                if (found.size() >= CANDIDATES) return sorted(found, originX, originZ);
            }
            r += step;
        }
        return sorted(found, originX, originZ);
    }

    /**
     * Raw biome volumes of chunks on rings around the origin, every other chunk. An Origen cave Regions Unexplored's
     * caves take over is itself only where nothing solid is above it, so its cells count only at or above the ground
     * (before carving), highest first; the landing then tries more of them.
     */
    private static List<Candidate> caves(BiomeLocating locating, Set<String> sources, boolean displaced, int originX,
        int originZ, List<int[]> visited, java.util.function.BooleanSupplier cancelled,
        java.util.function.IntConsumer progress) {
        int wanted = displaced ? 2 * CANDIDATES : CANDIDATES;
        List<Candidate> found = new ArrayList<>();
        int originCX = originX >> 4, originCZ = originZ >> 4, limit = Integer.MAX_VALUE;
        for (int r = 0; r <= CAVE_RADIUS >> 4 && r <= limit; r += 2) {
            if (cancelled.getAsBoolean()) return found;
            progress.accept(r << 4);
            for (int[] chunk : ring(originCX, originCZ, r, 2)) {
                var volume = locating.rawBiomeVolume(chunk[0], chunk[1]);
                if (volume == null) continue;
                int quarts = volume.getQuartHeight();
                cells: for (int i = 1; i < quarts; i++) for (int qz = 0; qz < 4; qz++) for (int qx = 0; qx < 4; qx++) {
                    int qy = displaced ? quarts - i : i;
                    if (!sources.contains(volume.getBiomeKey(qx, qy, qz))) continue;
                    int x = (chunk[0] << 4) + qx * 4 + 2, y = qy * 4 + 2, z = (chunk[1] << 4) + qz * 4 + 2;
                    if (near(visited, x, z, SPACING) || nearFound(found, x, z, displaced ? 128 : 48)) continue;
                    // Three solid blocks above make a roof (HostedCaveHabitats).
                    if (displaced && locating.groundHeight(x, z) > y + 2) continue;
                    found.add(new Candidate(x, y, z));
                    if (limit == Integer.MAX_VALUE) limit = r + (displaced ? 64 : 8);
                    break cells;
                }
                if (found.size() >= wanted) return sorted(found, originX, originZ);
            }
        }
        return sorted(found, originX, originZ);
    }

    /**
     * Island clusters on rings of cluster regions around the origin, their columns 8 blocks apart: an island of the
     * biome's habitat, or for Sky Donut and Aeropelago, which name the islands' shapes, any island of that shape.
     */
    private static List<Candidate> sky(ShiftedSkyIslands islands, Target target, int originX, int originZ,
        List<int[]> visited, java.util.function.BooleanSupplier cancelled, java.util.function.IntConsumer progress) {
        FloatingIslandArchetype shape = shape(target);
        String habitat = target.habitat();
        List<Candidate> found = new ArrayList<>();
        int region = FloatingIslandDensitySampler.CLUSTER_REGION, limit = Integer.MAX_VALUE;
        int originRX = Math.floorDiv(originX, region), originRZ = Math.floorDiv(originZ, region);
        for (int r = 0; r <= SKY_RADIUS / region && r <= limit; r++) {
            if (cancelled.getAsBoolean()) return found;
            progress.accept(r * region);
            for (int[] cell : ring(originRX, originRZ, r, 1)) {
                var cluster = islands.cluster(cell[0], cell[1]);
                if (cluster == null || shape != null && cluster.archetype() != shape) continue;
                int radius = cluster.radius();
                columns: for (int dz = -radius; dz <= radius; dz += 8) for (int dx = -radius; dx <= radius; dx += 8) {
                    int x = cluster.centerX() + dx, z = cluster.centerZ() + dz;
                    String island = islands.columnHabitat(x, z);
                    if (island == null || shape == null && !habitat.equals(island)) continue;
                    if (near(visited, x, z, SPACING) || nearFound(found, x, z, 48)) continue;
                    found.add(new Candidate(x, -1, z));
                    if (limit == Integer.MAX_VALUE) limit = r + 1;
                    break columns;
                }
                if (found.size() >= CANDIDATES) return sorted(found, originX, originZ);
            }
        }
        return sorted(found, originX, originZ);
    }

    /** The points of the square ring {@code r} steps of {@code step} around a centre. */
    private static List<int[]> ring(int centerX, int centerZ, int r, int step) {
        List<int[]> points = new ArrayList<>();
        if (r == 0) {
            points.add(new int[] { centerX, centerZ });
            return points;
        }
        for (int d = -r; d <= r; d += step) {
            points.add(new int[] { centerX + d, centerZ - r });
            points.add(new int[] { centerX + d, centerZ + r });
        }
        for (int d = -r + step; d <= r - step; d += step) {
            points.add(new int[] { centerX - r, centerZ + d });
            points.add(new int[] { centerX + r, centerZ + d });
        }
        return points;
    }

    private static boolean near(List<int[]> spots, int x, int z, int distance) {
        for (int[] spot : spots) if (Math.abs(spot[0] - x) < distance && Math.abs(spot[1] - z) < distance) return true;
        return false;
    }

    private static boolean nearFound(List<Candidate> found, int x, int z, int distance) {
        for (Candidate candidate : found)
            if (Math.abs(candidate.x() - x) < distance && Math.abs(candidate.z() - z) < distance) return true;
        return false;
    }

    private static List<Candidate> sorted(List<Candidate> found, int originX, int originZ) {
        found.sort(
            Comparator.comparingLong(
                c -> (long) (c.x() - originX) * (c.x() - originX) + (long) (c.z() - originZ) * (c.z() - originZ)));
        return found;
    }

    // ------------------------------------------------------------------------------------------------------- landing

    /**
     * Tries the finds nearest first, one a tick (each generates its chunks), and lands on the first with a clear spot;
     * when none has one, the player lands at the nearest find all the same.
     */
    private static void land(EntityPlayerMP player, Target target, WorldServer world, List<Candidate> found, int index,
        Trail trail, long ticket) {
        UUID id = player.getUniqueID();
        if (!Long.valueOf(ticket)
            .equals(TICKETS.get(id)) || player.playerNetServerHandler == null) return;
        if (found.isEmpty()) {
            status(
                player,
                "No " + target.name()
                    + " found"
                    + (trail.visited()
                        .isEmpty() ? " within reach" : " beyond the spots visited")
                    + ".");
            return;
        }
        if (index < found.size()) {
            Candidate candidate = found.get(index);
            int[] spot = spot(world, target, candidate);
            trail.visited()
                .add(new int[] { candidate.x(), candidate.z() });
            if (spot == null) {
                SERVER.add(() -> land(player, target, world, found, index + 1, trail, ticket));
                return;
            }
            arrive(player, target, world, spot, trail, true);
            return;
        }
        arrive(player, target, world, fallback(world, target, found.get(0)), trail, false);
    }

    private static void arrive(EntityPlayerMP player, Target target, WorldServer world, int[] spot, Trail trail,
        boolean clear) {
        TRAILS.put(player.getUniqueID(), trail);
        teleport(player, world, spot);
        int dx = spot[0] - trail.originX(), dz = spot[2] - trail.originZ();
        BiomicNetwork.send(
            new BrowserMessages.Status(
                (clear ? "Landed in " : "Landed by ") + target.name()
                    + " at "
                    + spot[0]
                    + " "
                    + spot[1]
                    + " "
                    + spot[2]
                    + ", "
                    + (int) Math.sqrt((double) dx * dx + dz * dz)
                    + " blocks from where the search began"
                    + (clear ? "." : "; no clear floor there, so mind your step."),
                BrowserMessages.Progress.LANDED),
            player);
    }

    /**
     * Loads a find's chunk and its neighbours, so it is populated: trees and plants stand where they will stay. Loaded
     * outright, not through provideChunk, which during the tick may hand back an empty chunk instead of generating one
     * (Hodgepodge's chunk generation throttle).
     */
    private static void load(WorldServer world, int x, int z) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) world.theChunkProviderServer.loadChunk((x >> 4) + dx, (z >> 4) + dz);
    }

    /**
     * Where to land at a find with no clear floor: on the highest ground of its column (below any island; on the
     * island, for the sky), on the Nether's lowest floor above the lava sea, or in a cave at its cell, in a gap if
     * one is near.
     */
    private static int[] fallback(WorldServer world, Target target, Candidate candidate) {
        int x = candidate.x(), z = candidate.z(), height = world.getHeight();
        load(world, x, z);
        BiomeLocating locating = BiomeSearch.locating(world);
        ShiftedSkyIslands islands = locating == null || world.provider.dimensionId != 0 ? null : locating.skyIslands();
        int span = islands == null ? -1 : islands.span(x, z);
        int y = switch (target.place()) {
            case CAVES -> {
                int hint = Math.max(2, candidate.y());
                for (int d = 0; d <= 12; d++) for (int at : new int[] { hint - d, hint + d })
                    if (at > 1 && passable(world.getBlock(x, at, z)) && passable(world.getBlock(x, at + 1, z)))
                        yield at;
                yield hint;
            }
            case NETHER -> {
                for (int at = 34; at < height - 4; at++)
                    if (passable(world.getBlock(x, at, z)) && world.getBlock(x, at - 1, z)
                        .getMaterial()
                        .isSolid()) yield at;
                yield 64;
            }
            case SKY -> span >= 0 ? (span & 0xFFFF) + 1 : top(world, x, z, height - 2);
            case SURFACE -> top(world, x, z, span >= 0 ? (span >>> 16) - 2 : height - 2);
        };
        return new int[] { x, y, z };
    }

    /** Just above the highest block of a column at or below {@code from} that is not air, a plant or a tree. */
    private static int top(WorldServer world, int x, int z, int from) {
        for (int y = from; y > 1; y--) if (!decoration(world.getBlock(x, y, z))) return y + 1;
        return world.getHeight() / 2;
    }

    /** A spot to stand in the candidate's chunk, nearest the candidate, where the biome really is: {x, y, z}. */
    private static int[] spot(WorldServer world, Target target, Candidate candidate) {
        load(world, candidate.x(), candidate.z());
        int baseX = candidate.x() >> 4 << 4, baseZ = candidate.z() >> 4 << 4;
        int height = world.getHeight();
        ShiftedSkyIslands islands = null;
        BiomeLocating locating = BiomeSearch.locating(world);
        if (locating != null && world.provider.dimensionId == 0) islands = locating.skyIslands();
        List<int[]> columns = new ArrayList<>();
        for (int lx = 0; lx < 16; lx++)
            for (int lz = 0; lz < 16; lz++) columns.add(new int[] { baseX + lx, baseZ + lz });
        columns.sort(Comparator.comparingInt(c -> Math.abs(c[0] - candidate.x()) + Math.abs(c[1] - candidate.z())));
        for (int[] column : columns) {
            int x = column[0], z = column[1];
            int y = switch (target.place()) {
                case SURFACE -> surface(world, x, z, height, islands, target.id());
                case NETHER -> world.getBiomeGenForCoords(x, z).biomeID == target.id() ? nether(world, x, z) : -1;
                case CAVES -> cave(world, x, z, candidate.y(), target.key());
                case SKY -> islands == null ? -1 : island(world, x, z, height, islands, target);
            };
            if (y >= 0) return new int[] { x, y, z };
        }
        return null;
    }

    /**
     * Feet on the ground of a column that holds the biome: its surface biome, or the 3D identity of its floor or (under
     * water) its seabed.
     */
    private static int surface(WorldServer world, int x, int z, int height, ShiftedSkyIslands islands, int id) {
        int y = ground(world, x, z, height, islands);
        if (y < 0) return -1;
        if (world.getBiomeGenForCoords(x, z).biomeID == id || BiomicBiomeApi.getBiome(world, x, y - 1, z).biomeID == id)
            return y;
        int floor = y - 1;
        while (floor > 1 && world.getBlock(x, floor, z)
            .getMaterial()
            .isLiquid()) floor--;
        return floor < y - 1 && BiomicBiomeApi.getBiome(world, x, floor + 1, z).biomeID == id ? y : -1;
    }

    /** Feet on the ground below any island: the highest block that is not a plant, a tree or air; on water, its top. */
    private static int ground(WorldServer world, int x, int z, int height, ShiftedSkyIslands islands) {
        int top = height - 2;
        int span = islands == null ? -1 : islands.span(x, z);
        if (span >= 0) top = (span >>> 16) - 2;
        for (int y = top; y > 1; y--) {
            Block block = world.getBlock(x, y, z);
            if (decoration(block)) continue;
            if (block.getMaterial() == Material.lava) return -1;
            if (block.getMaterial()
                .isLiquid()) return y + 1;
            return passable(world.getBlock(x, y + 1, z)) && passable(world.getBlock(x, y + 2, z)) ? y + 1 : -1;
        }
        return -1;
    }

    /** Feet on a Nether floor with room to stand, the lowest above the lava sea. */
    private static int nether(WorldServer world, int x, int z) {
        for (int y = 34; y < 250; y++) if (standing(world, x, y, z) && world.getBlock(x, y - 1, z)
            .getMaterial() != Material.lava) return y;
        for (int y = 5; y < 34; y++) if (standing(world, x, y, z)) return y;
        return -1;
    }

    /** Feet in the cave biome, near the height its volume cell named, with room to stand. */
    private static int cave(WorldServer world, int x, int z, int hint, String key) {
        for (int d = 0; d <= 12; d++) for (int y : new int[] { hint - d, hint + d }) {
            if (y < 2 || !standing(world, x, y, z)) continue;
            if (key.equals(BiomicBiomeApi.getBiomeKey(world, x, y, z))) return y;
        }
        return -1;
    }

    /** Feet on the island's own top, when the island there is the habitat's (or the shape's). */
    private static int island(WorldServer world, int x, int z, int height, ShiftedSkyIslands islands, Target target) {
        int span = islands.span(x, z);
        if (span < 0) return -1;
        int top = span & 0xFFFF;
        FloatingIslandArchetype shape = shape(target);
        if (shape != null ? islands.archetypeAt(x, top, z) != shape
            : !target.habitat()
                .equals(islands.habitatAt(x, top, z)))
            return -1;
        for (int y = top + 1; y < Math.min(height - 2, top + 48); y++) if (standing(world, x, y, z)) return y;
        return -1;
    }

    /** The island shape a sky biome names: Sky Donut's and Aeropelago's islands host other biomes. */
    private static FloatingIslandArchetype shape(Target target) {
        return switch (target.key()) {
            case BiomeKeys.SKY_DONUT -> FloatingIslandArchetype.RING_BASIN;
            case BiomeKeys.AEROPELAGO -> FloatingIslandArchetype.AEROPELAGO;
            default -> null;
        };
    }

    private static boolean standing(WorldServer world, int x, int y, int z) {
        return passable(world.getBlock(x, y, z)) && passable(world.getBlock(x, y + 1, z))
            && world.getBlock(x, y - 1, z)
                .getMaterial()
                .isSolid();
    }

    /** Feet and head room: air, water, or something a player walks through. */
    private static boolean passable(Block block) {
        Material material = block.getMaterial();
        return material == Material.air || material == Material.water
            || !material.blocksMovement() && material != Material.lava
                && material != Material.fire
                && material != Material.portal;
    }

    /** What stands on the ground rather than being it: air, plants and trees. */
    private static boolean decoration(Block block) {
        Material material = block.getMaterial();
        return material == Material.air || material == Material.leaves
            || material == Material.wood
            || material == Material.plants
            || material == Material.vine
            || material == Material.cactus
            || material == Material.gourd
            || material == Material.web
            || !material.blocksMovement() && !material.isLiquid();
    }

    private static void teleport(EntityPlayerMP player, WorldServer world, int[] spot) {
        double x = spot[0] + 0.5, y = spot[1], z = spot[2] + 0.5;
        if (player.dimension != world.provider.dimensionId) MinecraftServer.getServer()
            .getConfigurationManager()
            .transferPlayerToDimension(player, world.provider.dimensionId, new Landing(world, x, y, z));
        player.playerNetServerHandler.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
        player.fallDistance = 0;
    }

    /** A dimension change without a portal: the player arrives at the spot. */
    private static final class Landing extends Teleporter {

        private final double x, y, z;

        Landing(WorldServer world, double x, double y, double z) {
            super(world);
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public void placeInPortal(Entity entity, double px, double py, double pz, float yaw) {
            entity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
            entity.motionX = entity.motionY = entity.motionZ = 0;
        }

        @Override
        public boolean placeInExistingPortal(Entity entity, double px, double py, double pz, float yaw) {
            placeInPortal(entity, px, py, pz, yaw);
            return true;
        }

        @Override
        public boolean makePortal(Entity entity) {
            return true;
        }

        @Override
        public void removeStalePortalLocations(long time) {}
    }

    private static void status(EntityPlayerMP player, String text) {
        status(player, text, BrowserMessages.Progress.DONE);
    }

    private static void status(EntityPlayerMP player, String text, BrowserMessages.Progress progress) {
        if (player.playerNetServerHandler != null)
            BiomicNetwork.send(new BrowserMessages.Status(text, progress), player);
    }
}
