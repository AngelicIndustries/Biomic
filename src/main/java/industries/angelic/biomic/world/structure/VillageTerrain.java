package industries.angelic.biomic.world.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import industries.angelic.biomic.api.ChunkVerticalLayout;
import industries.angelic.biomic.world.terrain.BiomicTerrainSampler;

/**
 * Terrain shaped for the planned villages before they are built, after vanilla's terrain adaptation (beard_thin).
 * Each village gets one height field over its area, a pure function of its plan:
 * <ul>
 * <li>under each house the ground is exactly the house's pad, so the house measures it and sits flush (no stilts,
 * no wall cut into a hill);</li>
 * <li>around houses it blends toward the weighted mean of the nearby pads, at about 2:1, into a version of the
 * natural ground smoothed over the village and fading out around it;</li>
 * <li>the road network (each cross-section of a road, each piece at a road with a one-block apron, each lamp or arch
 * with the road it spans) is a set of parts, each at one height, solved together (RoadNetworkHeights): close to the
 * ground the village wants there, every change of height paying for itself, never more than a block between
 * neighbouring parts, diagonals included. Over water roads are causeways above sea level;</li>
 * <li>ground beside a road stays within a block per block of distance from it, so no walls line the roads;</li>
 * <li>shallow pits inside the village are filled to where they would spill.</li>
 * </ul>
 *
 * <p>
 * Each layer is shaped on its own ground: the land for ground villages, an island's top for sky villages. Works on
 * finished material columns, whose own surface layers move with the ground, so grass stays grass. Shaping and the
 * ownership query Origen's features ask read the same fields, so they always agree on which cells change.
 */
final class VillageTerrain {

    /**
     * How far beyond a house the ground blends back toward the base ground; also how far past its bounds a village
     * shapes.
     */
    static final int REACH = 16;
    /** Radius of the smoothing of the ground inside a village, and the band outside it over which that fades out. */
    private static final int SMOOTH = BiomicVillagePlacement.PAD_SPREAD, FADE = 8;
    /**
     * How far around a chunk {@link #warm} starts village plans: the server's structure checks look for village
     * starts up to 8 chunks away.
     */
    private static final int WARM_MARGIN = 48;
    /** The deepest pit inside a village that is filled; deeper ones are the land's own. */
    private static final int PIT = 3;
    /**
     * What a block of height change between neighbouring parts of the road network costs per block of the edge they
     * share, against a block of misfit per cell (see RoadNetworkHeights): a ridge or notch in a road shorter than
     * about twice this many cross-sections is not worth it.
     */
    private static final double SMOOTHNESS = 2;
    /**
     * How many times more a block of height change costs between a piece and the road it stands at: a house opens
     * level onto its road (Village Names lays its front path on the road at the house's floor) and a plaza meets its
     * roads level, unless the ground differs a lot.
     */
    private static final int DOORSTEP = 8;
    /** Neighbour steps that visit each pair of neighbouring cells once. */
    private static final int[][] FORWARD = { { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };
    /** How far from a road the ground is held within a block per block of the road's height. */
    private static final int ROADSIDE = 6;
    /** Surface layers that move with the ground (grass, dirt, ...). */
    private static final int SKIN = 4;
    /**
     * How far the ground really in a column may lie from the planned ground height: hosted terrain (a native marsh,
     * carvers) reshapes some columns after Origen's density, which the plans read.
     */
    private static final int SLACK = 8;
    private static final int CACHE_SIZE = 1024, FIELD_CACHE_SIZE = 64;
    private static final int UNSHAPED = Integer.MIN_VALUE;

    private final BiomicVillagePlacement placement;
    private final StructureTerrain terrain;
    private final ChunkVerticalLayout layout;
    private final LinkedHashMap<Long, List<Columns>> columns = new LinkedHashMap<>(64, .75F, true);
    private final LinkedHashMap<Long, FutureTask<Field>> fields = new LinkedHashMap<>(16, .75F, true);

    /** One layer's columns of a chunk (x*16+z): the natural ground, the ground it is shaped to, and road cells. */
    private record Columns(BiomicVillagePlacement.Layer layer, int[] ground, int[] target, BitSet road) {}

    /** One village's natural and shaped ground over its area; UNSHAPED where it has no ground or leaves it be. */
    private record Field(int x0, int z0, int width, int depth, int[] ground, int[] target, BitSet road) {

        int index(int x, int z) {
            int i = x - x0, j = z - z0;
            return i < 0 || j < 0 || i >= width || j >= depth ? -1 : i * depth + j;
        }
    }

    VillageTerrain(BiomicVillagePlacement placement, StructureTerrain terrain) {
        this.placement = placement;
        this.terrain = terrain;
        this.layout = terrain.layout();
    }

    /** Shapes a chunk's finished pure materials for the villages near it. Worker-safe. */
    void shape(World world, int chunkX, int chunkZ, byte[] materials) {
        byte[] skin = new byte[SKIN];
        for (Columns plan : columns(world, chunkX, chunkZ)) for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int column = x * 16 + z, planned = plan.ground[column], target = plan.target[column];
            boolean road = plan.road.get(column);
            // Columns the village leaves as they are stay so; roads always lie at their height.
            if (planned == UNSHAPED || target == UNSHAPED || target == planned && !road) continue;
            int ground = surface(materials, x, z, planned, road);
            if (ground == UNSHAPED || target == ground) continue;
            int depth = 0;
            for (int y = ground; y > ground - SKIN; y--) {
                byte material = materials[layout.index(x, y, z)];
                if (!solid(material)) break;
                skin[depth++] = material;
            }
            for (int i = depth; i < SKIN; i++) skin[i] = skin[depth - 1];
            for (int y = target + 1; y <= ground; y++) materials[layout.index(x, y, z)] = BiomicTerrainSampler.AIR;
            for (int y = Math.min(target, ground) - SKIN + 1; y <= target; y++)
                materials[layout.index(x, y, z)] = skin[Math.min(SKIN - 1, target - y)];
        }
    }

    /**
     * The ground really in a column near its planned height: the highest solid cell within {@link #SLACK} of it with
     * open air above (or, under a road, water it can fill as a causeway); UNSHAPED where there is none.
     */
    private int surface(byte[] materials, int x, int z, int planned, boolean road) {
        int top = Math.min(planned + SLACK, terrain.maxY() - 2),
            bottom = Math.max(planned - SLACK, terrain.minY() + SKIN);
        for (int y = top; y >= bottom; y--) {
            if (!solid(materials[layout.index(x, y, z)])) continue;
            byte above = materials[layout.index(x, y + 1, z)];
            return above == BiomicTerrainSampler.AIR || road && above == BiomicTerrainSampler.WATER ? y : UNSHAPED;
        }
        return UNSHAPED;
    }

    /**
     * Starts the plans of the villages around a chunk, and the ground of those that reach it, on the workers, one task
     * per village, so they run side by side while the caller builds the chunk's own terrain. Shaping then finds them
     * done or in progress (plans and fields are shared tasks); a failure surfaces on the caller's own request.
     */
    void warm(World world, int chunkX, int chunkZ, Executor workers) {
        int minX = chunkX << 4, minZ = chunkZ << 4;
        for (int[] cell : placement.candidateCells(chunkX, chunkZ, WARM_MARGIN)) {
            Runnable task = () -> {
                try {
                    if (!placement.mayReach(cell, chunkX, chunkZ, WARM_MARGIN)) return;
                    var plan = placement.plan(cell[1], cell[2], BiomicVillagePlacement.Layer.values()[cell[0]], world);
                    if (plan == BiomicVillagePlacement.NONE) return;
                    StructureBoundingBox b = plan.bounds();
                    if (b.maxX + REACH >= minX && b.minX - REACH <= minX + 15
                        && b.maxZ + REACH >= minZ
                        && b.minZ - REACH <= minZ + 15) field(plan);
                } catch (RuntimeException ignored) {
                    // The caller's own request plans again and reports it.
                }
            };
            try {
                workers.execute(task);
            } catch (RejectedExecutionException stopping) {
                return;
            }
        }
    }

    /** Whether shaping may change this cell: Origen's features treat such cells as owned by the village. */
    boolean owns(World world, int x, int y, int z) {
        int column = (x & 15) * 16 + (z & 15);
        for (Columns plan : columns(world, x >> 4, z >> 4)) {
            int ground = plan.ground[column], target = plan.target[column];
            if (ground != UNSHAPED && target != UNSHAPED
                && (target != ground || plan.road.get(column))
                && y > Math.min(target, ground) - SKIN - SLACK
                && y <= Math.max(target, ground) + SLACK) return true;
        }
        return false;
    }

    /**
     * The top solid Y of a layer's terrain at a column as shaping left it, or -1 where the layer has none (off an
     * island's edge).
     */
    int surface(World world, int x, int z, BiomicVillagePlacement.Layer layer) {
        for (Columns plan : columns(world, x >> 4, z >> 4)) {
            if (plan.layer != layer) continue;
            int target = plan.target[(x & 15) * 16 + (z & 15)];
            if (target != UNSHAPED) return target;
        }
        if (layer == BiomicVillagePlacement.Layer.GROUND) return terrain.groundHeight(x, z);
        int span = terrain.skySpan(x, z);
        return span < 0 ? -1 : StructureTerrain.spanTop(span);
    }

    private List<Columns> columns(World world, int chunkX, int chunkZ) {
        long key = (long) chunkX << 32 ^ chunkZ & 0xFFFF_FFFFL;
        synchronized (columns) {
            List<Columns> known = columns.get(key);
            if (known != null) return known;
        }
        List<Columns> computed = compute(world, chunkX, chunkZ);
        synchronized (columns) {
            columns.put(key, computed);
            if (columns.size() > CACHE_SIZE) {
                var eldest = columns.keySet()
                    .iterator();
                eldest.next();
                eldest.remove();
            }
        }
        return computed;
    }

    /** The chunk's view of the fields of the villages around it, one per layer. */
    private List<Columns> compute(World world, int chunkX, int chunkZ) {
        List<Columns> result = new ArrayList<>(2);
        int minX = chunkX << 4, minZ = chunkZ << 4;
        for (var plan : placement.near(chunkX, chunkZ, REACH, world)) {
            Field field = field(plan);
            Columns into = null;
            for (Columns existing : result) if (existing.layer == plan.layer()) into = existing;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int at = field.index(minX + x, minZ + z), column = x * 16 + z;
                if (at < 0 || field.target[at] == UNSHAPED) continue;
                if (into == null) {
                    int[] ground = new int[256], target = new int[256];
                    Arrays.fill(ground, UNSHAPED);
                    Arrays.fill(target, UNSHAPED);
                    result.add(into = new Columns(plan.layer(), ground, target, new BitSet(256)));
                }
                if (into.target[column] != UNSHAPED) continue; // Villages are far apart; the first one shapes.
                into.ground[column] = field.ground[at];
                into.target[column] = field.target[at];
                if (field.road.get(at)) into.road.set(column);
            }
        }
        return List.copyOf(result);
    }

    private Field field(BiomicVillagePlacement.Plan plan) {
        long key = ((long) plan.chunkX() << 32 ^ plan.chunkZ() & 0xFFFF_FFFFL) * 2 + plan.layer()
            .ordinal();
        FutureTask<Field> task;
        boolean owner = false;
        synchronized (fields) {
            task = fields.get(key);
            if (task == null) {
                fields.put(key, task = new FutureTask<>(() -> computeField(plan)));
                owner = true;
                if (fields.size() > FIELD_CACHE_SIZE) {
                    var eldest = fields.keySet()
                        .iterator();
                    eldest.next();
                    eldest.remove();
                }
            }
        }
        if (owner) task.run();
        try {
            return task.get();
        } catch (ExecutionException failure) {
            synchronized (fields) {
                fields.remove(key, task);
            }
            throw new IllegalStateException(
                "Village terrain failed for the village at chunk " + plan.chunkX() + "," + plan.chunkZ(),
                failure.getCause());
        } catch (InterruptedException interrupted) {
            Thread.currentThread()
                .interrupt();
            throw new IllegalStateException("Interrupted while shaping a village", interrupted);
        }
    }

    private Field computeField(BiomicVillagePlacement.Plan plan) {
        boolean sky = plan.layer() == BiomicVillagePlacement.Layer.SKY;
        StructureBoundingBox bounds = plan.bounds();
        int x0 = bounds.minX - REACH, z0 = bounds.minZ - REACH;
        int width = bounds.maxX + REACH - x0 + 1, depth = bounds.maxZ + REACH - z0 + 1, cells = width * depth;
        int sea = terrain.seaLevel();
        // The layer's ground over the area and the smoothing margin, with running sums of dry ground for box means.
        int rawWidth = width + 2 * SMOOTH, rawDepth = depth + 2 * SMOOTH, stride = rawDepth + 1;
        int[] raw = new int[rawWidth * rawDepth];
        long[] sums = new long[(rawWidth + 1) * stride];
        int[] counts = new int[(rawWidth + 1) * stride];
        for (int i = 0; i < rawWidth; i++) for (int j = 0; j < rawDepth; j++) {
            int x = x0 - SMOOTH + i, z = z0 - SMOOTH + j, height;
            if (sky) {
                int span = terrain.skySpan(x, z);
                height = span < 0 ? UNSHAPED : StructureTerrain.spanTop(span);
            } else height = terrain.groundHeight(x, z);
            raw[i * rawDepth + j] = height;
            boolean dry = height != UNSHAPED && (sky || height > sea);
            int at = (i + 1) * stride + j + 1;
            sums[at] = (dry ? height : 0) + sums[at - 1] + sums[at - stride] - sums[at - stride - 1];
            counts[at] = (dry ? 1 : 0) + counts[at - 1] + counts[at - stride] - counts[at - stride - 1];
        }
        int[] ground = new int[cells], target = new int[cells], lowest = new int[cells];
        double[] want = new double[cells], base = new double[cells];
        BitSet road = new BitSet(cells), house = new BitSet(cells);
        Arrays.fill(target, UNSHAPED);
        // The parts of the road network, each at one height: each piece at a road, each road's cross-sections.
        int[] parent = new int[cells];
        for (int at = 0; at < cells; at++) parent[at] = at;
        BitSet grouped = new BitSet(cells), lanes = new BitSet(cells);
        for (var lane : plan.roads()) {
            mark(lane, x0, z0, width, depth, road, null, 0);
            mark(lane, x0, z0, width, depth, lanes, null, 0);
            // A road lies level across its width: no cell of it is a hole beside its neighbours.
            boolean alongX = lane.getXSize() >= lane.getZSize(), alongZ = lane.getZSize() >= lane.getXSize();
            if (alongX) for (int x = lane.minX; x <= lane.maxX; x++)
                join(x, x, lane.minZ, lane.maxZ, x0, z0, width, depth, parent, grouped);
            if (alongZ) for (int z = lane.minZ; z <= lane.maxZ; z++)
                join(lane.minX, lane.maxX, z, z, x0, z0, width, depth, parent, grouped);
        }
        // Every piece at a road (the plaza, houses, street decor) levels its own footprint to the ground it measures
        // there: it is part of the road network, one height across, within a block of the road cells beside it.
        int[] piece = new int[cells];
        Arrays.fill(piece, -1);
        List<BiomicVillagePlacement.House> houses = plan.houses();
        boolean[] onRoad = new boolean[houses.size()];
        for (int k = 0; k < houses.size(); k++) {
            StructureBoundingBox box = houses.get(k)
                .box();
            for (var lane : plan.roads()) onRoad[k] |= BiomicVillagePlacement.touches(box, lane);
            if (!onRoad[k]) continue;
            mark(box, x0, z0, width, depth, road, piece, k);
            join(box.minX, box.maxX, box.minZ, box.maxZ, x0, z0, width, depth, parent, grouped);
        }
        // A one-block apron around each of them is part of the piece: pieces place decor just outside their footprint
        // at their floor (a plaza's lamp posts), and doors open onto it. Between two pieces it goes to the first.
        for (int k = 0; k < houses.size(); k++) {
            if (!onRoad[k]) continue;
            StructureBoundingBox box = houses.get(k)
                .box();
            int first = (Math.max(box.minX, x0) - x0) * depth + Math.max(box.minZ, z0) - z0;
            for (int x = Math.max(box.minX - 1, x0); x <= Math.min(box.maxX + 1, x0 + width - 1); x++)
                for (int z = Math.max(box.minZ - 1, z0); z <= Math.min(box.maxZ + 1, z0 + depth - 1); z++) {
                    int at = (x - x0) * depth + z - z0;
                    if (road.get(at) || padAt(houses, x, z) != UNSHAPED) continue;
                    road.set(at);
                    piece[at] = k;
                    union(parent, at, first);
                    grouped.set(at);
                }
        }
        // Thin pieces at a road (lamp posts, arches) measure the ground under themselves and may build across the
        // road at that height: they, and the road cells they span, are one piece of road.
        List<StructureBoundingBox> accents = plan.accents();
        for (int k = 0; k < accents.size(); k++) {
            StructureBoundingBox accent = accents.get(k);
            int index = houses.size() + k;
            mark(accent, x0, z0, width, depth, road, piece, index);
            join(accent.minX, accent.maxX, accent.minZ, accent.maxZ, x0, z0, width, depth, parent, grouped);
            for (var lane : plan.roads()) {
                if (!BiomicVillagePlacement.touches(accent, lane)) continue;
                boolean spansX = accent.getXSize() <= accent.getZSize(); // Thin along X: it reaches across in X.
                StructureBoundingBox across = spansX
                    ? new StructureBoundingBox(
                        lane.minX,
                        Math.max(accent.minZ, lane.minZ),
                        lane.maxX,
                        Math.min(accent.maxZ, lane.maxZ))
                    : new StructureBoundingBox(
                        Math.max(accent.minX, lane.minX),
                        lane.minZ,
                        Math.min(accent.maxX, lane.maxX),
                        lane.maxZ);
                if (across.minX > across.maxX || across.minZ > across.maxZ) continue;
                mark(across, x0, z0, width, depth, road, piece, index);
                join(across.minX, across.maxX, across.minZ, across.maxZ, x0, z0, width, depth, parent, grouped);
            }
        }
        for (int i = 0; i < width; i++) for (int j = 0; j < depth; j++) {
            int at = i * depth + j, x = x0 + i, z = z0 + j, natural = raw[(i + SMOOTH) * rawDepth + j + SMOOTH];
            ground[at] = natural;
            if (natural == UNSHAPED) {
                road.clear(at); // Off an island's edge a road is not laid.
                continue;
            }
            lowest[at] = sky ? StructureTerrain.spanBottom(terrain.skySpan(x, z)) + SKIN // Never thin an island away.
                : Math.max(terrain.minY() + SKIN, road.get(at) ? sea + 1 : Integer.MIN_VALUE); // Causeways stay dry.
            int i1 = i + 2 * SMOOTH + 1, j1 = j + 2 * SMOOTH + 1;
            int count = counts[i1 * stride + j1] - counts[i * stride + j1]
                - counts[i1 * stride + j]
                + counts[i * stride + j];
            long sum = sums[i1 * stride + j1] - sums[i * stride + j1] - sums[i1 * stride + j] + sums[i * stride + j];
            base[at] = natural;
            if (count > 0) base[at] += village(bounds, x, z) * ((double) sum / count - natural);
            int pad = padAt(houses, x, z);
            if (pad != UNSHAPED) house.set(at);
            want[at] = pad != UNSHAPED ? pad
                : piece[at] >= 0 && piece[at] < houses.size() ? houses.get(piece[at])
                    .ground() : blend(houses, x, z, base[at]);
            want[at] = Math.max(lowest[at], Math.min(terrain.maxY() - 2, want[at]));
        }
        // The road network's heights, solved over its parts at once.
        int[] part = new int[cells], index = new int[cells];
        Arrays.fill(index, -1);
        int parts = 0;
        for (int at = road.nextSetBit(0); at >= 0; at = road.nextSetBit(at + 1)) {
            int root = grouped.get(at) ? find(parent, at) : at;
            if (index[root] < 0) index[root] = parts++;
            part[at] = index[root];
        }
        int[] start = new int[parts + 1], floors = new int[parts];
        boolean[] onLane = new boolean[parts];
        for (int at = road.nextSetBit(0); at >= 0; at = road.nextSetBit(at + 1)) {
            start[part[at] + 1]++;
            onLane[part[at]] |= lanes.get(at);
        }
        for (int p = 0; p < parts; p++) start[p + 1] += start[p];
        double[] wants = new double[start[parts]];
        int[] next = Arrays.copyOf(start, parts);
        Arrays.fill(floors, Integer.MIN_VALUE);
        LinkedHashMap<Long, int[]> pairs = new LinkedHashMap<>();
        for (int at = road.nextSetBit(0); at >= 0; at = road.nextSetBit(at + 1)) {
            int p = part[at], i = at / depth, j = at % depth;
            wants[next[p]++] = want[at];
            floors[p] = Math.max(floors[p], lowest[at]);
            for (int[] step : FORWARD) {
                int ni = i + step[0], nj = j + step[1], neighbour = ni * depth + nj;
                if (ni >= width || nj < 0 || nj >= depth || !road.get(neighbour) || part[neighbour] == p) continue;
                int q = part[neighbour];
                int[] edge = pairs.computeIfAbsent((long) Math.min(p, q) << 32 | Math.max(p, q), key -> new int[1]);
                if (step[0] == 0 || step[1] == 0) edge[0] += onLane[p] == onLane[q] ? 1 : DOORSTEP;
            }
        }
        int[] a = new int[pairs.size()], b = new int[pairs.size()], edges = new int[pairs.size()];
        int pair = 0;
        for (var entry : pairs.entrySet()) {
            a[pair] = (int) (entry.getKey() >>> 32);
            b[pair] = (int) (long) entry.getKey();
            edges[pair++] = entry.getValue()[0];
        }
        int[] heights = RoadNetworkHeights.solve(start, wants, floors, a, b, edges, SMOOTHNESS);
        for (int at = road.nextSetBit(0); at >= 0; at = road.nextSetBit(at + 1)) target[at] = heights[part[at]];
        // Pieces on the road network stand where it put them; the ground around blends toward those heights.
        List<BiomicVillagePlacement.House> placed = new ArrayList<>(houses.size());
        for (int k = 0; k < houses.size(); k++) {
            int height = houses.get(k)
                .ground();
            if (onRoad[k]) for (int at = 0; at < cells; at++) if (piece[at] == k && road.get(at)) {
                height = target[at];
                break;
            }
            placed.add(
                new BiomicVillagePlacement.House(
                    houses.get(k)
                        .box(),
                    height));
        }
        // Beside roads: within a block per block of distance of the road's height.
        double[] roadHeights = new double[cells];
        for (int at = road.nextSetBit(0); at >= 0; at = road.nextSetBit(at + 1)) roadHeights[at] = target[at];
        double[] ceiling = envelope(road, roadHeights, width, depth, true);
        double[] floor = envelope(road, roadHeights, width, depth, false);
        int[] distance = roadDistance(road, width, depth);
        for (int i = 0; i < width; i++) for (int j = 0; j < depth; j++) {
            int at = i * depth + j, natural = ground[at];
            if (natural == UNSHAPED || road.get(at)) continue;
            if (!sky && natural <= sea) continue; // Water's edges stay.
            int x = x0 + i, z = z0 + j, pad = padAt(placed, x, z);
            double wanted = pad != UNSHAPED ? pad : Math.max(lowest[at], blend(placed, x, z, base[at]));
            if (!house.get(at) && distance[at] <= ROADSIDE) wanted = Math.max(floor[at], Math.min(ceiling[at], wanted));
            target[at] = (int) Math.round(Math.max(lowest[at], wanted));
        }
        fillPits(target, bounds, x0, z0, width, depth);
        return new Field(x0, z0, width, depth, ground, target, road);
    }

    /**
     * Fills the pits inside a village's bounds: ground lower than all the ground around it (diagonals included) rises
     * to the level it would spill at, so no hole is left beside a plaza, in a road or between houses. A pit deeper
     * than {@link #PIT} anywhere is the land's own and stays. Each pit fills to one level, so a road cell in one
     * rises to the height of the road beside it and road steps stay within a block. Priority flood from the ground
     * around the bounds; ground with no surface (water, off an island) drains everything beside it.
     */
    private static void fillPits(int[] target, StructureBoundingBox bounds, int x0, int z0, int width, int depth) {
        int cells = width * depth;
        BitSet fillable = new BitSet(cells);
        for (int x = Math.max(bounds.minX, x0); x <= Math.min(bounds.maxX, x0 + width - 1); x++)
            for (int z = Math.max(bounds.minZ, z0); z <= Math.min(bounds.maxZ, z0 + depth - 1); z++) {
                int at = (x - x0) * depth + z - z0;
                if (target[at] != UNSHAPED) fillable.set(at);
            }
        int[] level = new int[cells];
        BitSet reached = new BitSet(cells);
        java.util.PriorityQueue<Long> queue = new java.util.PriorityQueue<>();
        for (int at = 0; at < cells; at++) {
            if (fillable.get(at) || !borders(fillable, at, width, depth)) continue;
            level[at] = target[at] == UNSHAPED ? -1 : target[at];
            queue.add((long) (level[at] + 1) << 32 | at);
        }
        while (!queue.isEmpty()) {
            long next = queue.poll();
            int at = (int) next, spill = (int) (next >>> 32) - 1, i = at / depth, j = at % depth;
            for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) {
                int ni = i + di, nj = j + dj;
                if ((di | dj) == 0 || ni < 0 || nj < 0 || ni >= width || nj >= depth) continue;
                int neighbour = ni * depth + nj;
                if (!fillable.get(neighbour) || reached.get(neighbour)) continue;
                reached.set(neighbour);
                level[neighbour] = Math.max(target[neighbour], spill);
                queue.add((long) (level[neighbour] + 1) << 32 | neighbour);
            }
        }
        // Each pit, as the cells joined by filling, fills whole or not at all.
        BitSet seen = new BitSet(cells);
        int[] pit = new int[cells];
        for (int start = 0; start < cells; start++) {
            if (seen.get(start) || !reached.get(start) || level[start] <= target[start]) continue;
            int size = 0, deepest = 0;
            pit[size++] = start;
            seen.set(start);
            for (int head = 0; head < size; head++) {
                int at = pit[head], i = at / depth, j = at % depth;
                deepest = Math.max(deepest, level[at] - target[at]);
                for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) {
                    int ni = i + di, nj = j + dj;
                    if ((di | dj) == 0 || ni < 0 || nj < 0 || ni >= width || nj >= depth) continue;
                    int neighbour = ni * depth + nj;
                    if (seen.get(neighbour) || !reached.get(neighbour) || level[neighbour] <= target[neighbour])
                        continue;
                    seen.set(neighbour);
                    pit[size++] = neighbour;
                }
            }
            if (deepest <= PIT) for (int k = 0; k < size; k++) target[pit[k]] = level[pit[k]];
        }
    }

    private static boolean borders(BitSet cells, int at, int width, int depth) {
        int i = at / depth, j = at % depth;
        for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) {
            int ni = i + di, nj = j + dj;
            if ((di | dj) != 0 && ni >= 0 && nj >= 0 && ni < width && nj < depth && cells.get(ni * depth + nj))
                return true;
        }
        return false;
    }

    /**
     * The tightest function that changes by at most one per step between neighbouring cells (diagonals included) and
     * stays at or above ({@code below = false}) or at or below ({@code below = true}) the seeds' values: min (or max)
     * over seeds of value plus (minus) the step count. Dijkstra.
     */
    private static double[] envelope(BitSet seeds, double[] values, int width, int depth, boolean below) {
        int cells = width * depth;
        double[] result = new double[cells];
        Arrays.fill(result, below ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
        java.util.PriorityQueue<double[]> queue = new java.util.PriorityQueue<>(
            (a, b) -> below ? Double.compare(a[0], b[0]) : Double.compare(b[0], a[0]));
        for (int at = seeds.nextSetBit(0); at >= 0; at = seeds.nextSetBit(at + 1)) {
            result[at] = values[at];
            queue.add(new double[] { values[at], at });
        }
        while (!queue.isEmpty()) {
            double[] next = queue.poll();
            int at = (int) next[1];
            if (next[0] != result[at]) continue;
            int i = at / depth, j = at % depth;
            double step = below ? next[0] + 1 : next[0] - 1;
            for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) {
                int ni = i + di, nj = j + dj;
                if ((di | dj) == 0 || ni < 0 || nj < 0 || ni >= width || nj >= depth) continue;
                int neighbour = ni * depth + nj;
                if (below ? step >= result[neighbour] : step <= result[neighbour]) continue;
                result[neighbour] = step;
                queue.add(new double[] { step, neighbour });
            }
        }
        return result;
    }

    /** Marks a box's columns within the area; with {@code piece}, also which piece they belong to. */
    private static void mark(StructureBoundingBox box, int x0, int z0, int width, int depth, BitSet cells, int[] piece,
        int index) {
        for (int x = Math.max(box.minX, x0); x <= Math.min(box.maxX, x0 + width - 1); x++)
            for (int z = Math.max(box.minZ, z0); z <= Math.min(box.maxZ, z0 + depth - 1); z++) {
                int at = (x - x0) * depth + z - z0;
                cells.set(at);
                if (piece != null) piece[at] = index;
            }
    }

    /** Joins a box's columns within the area into one group. */
    private static void join(int minX, int maxX, int minZ, int maxZ, int x0, int z0, int width, int depth, int[] parent,
        BitSet grouped) {
        int first = -1;
        for (int x = Math.max(minX, x0); x <= Math.min(maxX, x0 + width - 1); x++)
            for (int z = Math.max(minZ, z0); z <= Math.min(maxZ, z0 + depth - 1); z++) {
                int at = (x - x0) * depth + z - z0;
                grouped.set(at);
                if (first < 0) first = at;
                else union(parent, at, first);
            }
    }

    private static void union(int[] parent, int a, int b) {
        a = find(parent, a);
        b = find(parent, b);
        if (a != b) parent[Math.max(a, b)] = Math.min(a, b);
    }

    private static int find(int[] parent, int at) {
        while (parent[at] != at) at = parent[at] = parent[parent[at]];
        return at;
    }

    /** Steps (diagonals included) from each cell to the nearest road cell. */
    private static int[] roadDistance(BitSet road, int width, int depth) {
        int[] distance = new int[width * depth];
        Arrays.fill(distance, Integer.MAX_VALUE);
        int[] queue = new int[width * depth];
        int head = 0, tail = 0;
        for (int at = road.nextSetBit(0); at >= 0; at = road.nextSetBit(at + 1)) {
            distance[at] = 0;
            queue[tail++] = at;
        }
        while (head < tail) {
            int at = queue[head++], i = at / depth, j = at % depth;
            for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) {
                int ni = i + di, nj = j + dj;
                if ((di | dj) == 0 || ni < 0 || nj < 0 || ni >= width || nj >= depth) continue;
                int neighbour = ni * depth + nj;
                if (distance[neighbour] != Integer.MAX_VALUE) continue;
                distance[neighbour] = distance[at] + 1;
                queue[tail++] = neighbour;
            }
        }
        return distance;
    }

    /** 1 inside a village's bounds, easing to 0 over {@link #FADE} blocks outside them. */
    private static double village(StructureBoundingBox b, int x, int z) {
        int dx = Math.max(0, Math.max(b.minX - x, x - b.maxX)), dz = Math.max(0, Math.max(b.minZ - z, z - b.maxZ));
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance >= FADE) return 0;
        double t = 1 - distance / FADE;
        return t * t * (3 - 2 * t);
    }

    /** The pad of the house standing on this column, or UNSHAPED. */
    private static int padAt(List<BiomicVillagePlacement.House> houses, int x, int z) {
        for (var house : houses) {
            StructureBoundingBox box = house.box();
            if (x >= box.minX && x <= box.maxX && z >= box.minZ && z <= box.maxZ) return house.ground();
        }
        return UNSHAPED;
    }

    /**
     * Around houses, a blend toward the weighted mean of the nearby pads (so ground between two houses ramps from one
     * pad to the other instead of breaking into a cliff where one house takes over) over a distance of about twice
     * the height difference; elsewhere the base ground.
     */
    private static double blend(List<BiomicVillagePlacement.House> houses, int x, int z, double base) {
        double strongest = 0, weights = 0, pads = 0;
        for (var house : houses) {
            StructureBoundingBox box = house.box();
            int dx = Math.max(0, Math.max(box.minX - 1 - x, x - box.maxX - 1));
            int dz = Math.max(0, Math.max(box.minZ - 1 - z, z - box.maxZ - 1));
            double distance = Math.sqrt(dx * dx + dz * dz);
            double reach = Math.max(4, Math.min(REACH, 2 * Math.abs(house.ground() - base) + 3));
            if (distance >= reach) continue;
            double t = 1 - distance / reach, weight = t * t * (3 - 2 * t);
            strongest = Math.max(strongest, weight);
            weights += weight;
            pads += weight * house.ground();
        }
        return weights == 0 ? base : base + strongest * (pads / weights - base);
    }

    private static boolean solid(byte material) {
        return material != BiomicTerrainSampler.AIR && material != BiomicTerrainSampler.WATER
            && material != BiomicTerrainSampler.LAVA;
    }
}
