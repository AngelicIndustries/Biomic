package industries.angelic.biomic.world.cave;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * One bounded Biomic-authored dry connector, not source YUNG route-graph parity.
 * Complete32x32 owners and inset proposals make admission independent of the requested chunk/order.
 * Floor reservations shape the subsequent chamber mask; no floor is manufactured or filled.
 */
public final class RegionalCaveRouteRoot15Literal {

    public static final String REVISION = "regional-source64-dry-whole-route-stairs-v1";
    public static final String CONNECTED_REVISION = "regional-source64-two-room-whole-route-stairs-v2";
    public static final int OWNER_WIDTH = 32, MAX_PORTS = 16, MAX_PAIRS = 8, MAX_STEPS = 64, MAX_RISE = 8;
    public static final int MIN_FEET = 11, MAX_FEET = 116, WIDTH = 3, CLEAR_HEIGHT = 4;

    public record Foot(int x, int y, int z) {}

    private record Port(int x, int y, int z) {}

    private record Pair(Port a, Port b, int order, int score) {}

    private final int ox, oz;
    private final BitSet air, floors;
    private final List<Foot> route;

    private RegionalCaveRouteRoot15Literal(int ox, int oz, BitSet air, BitSet floors, List<Foot> route) {
        this.ox = ox;
        this.oz = oz;
        this.air = (BitSet) air.clone();
        this.floors = (BitSet) floors.clone();
        this.route = List.copyOf(route);
    }

    public boolean admitted() {
        return !route.isEmpty();
    }

    public List<Foot> route() {
        return route;
    }

    public int ownerOriginX() {
        return ox;
    }

    public int ownerOriginZ() {
        return oz;
    }

    public boolean interior(int x, int y, int z) {
        return contains(air, x, y, z);
    }

    public boolean floor(int x, int y, int z) {
        return contains(floors, x, y, z);
    }

    private boolean contains(BitSet bits, int x, int y, int z) {
        long lx = (long) x - ox, lz = (long) z - oz;
        return lx >= 0 && lx < 32 && lz >= 0 && lz < 32 && y >= 0 && y < 256 && bits.get(index((int) lx, y, (int) lz));
    }

    public CaveComposition.Host planningHost(CaveComposition.Host original) {
        Objects.requireNonNull(original);
        return (x, y, z) -> floor(x, y, z) ? CaveComposition.Material.SOLID
            : interior(x, y, z) ? CaveComposition.Material.CAVITY : original.at(x, y, z);
    }

    /**
     * Connected-room successor: preserve the admitted floor and original chamber seeds only.
     * Route AIR must not seed extra clearance which could excavate an adjacent mouth's floor.
     */
    public CaveComposition.Host retainedFloorHost(CaveComposition.Host original) {
        Objects.requireNonNull(original);
        return (x, y, z) -> floor(x, y, z) ? CaveComposition.Material.SOLID : original.at(x, y, z);
    }

    public BitSet airForChunk(int cx, int cz) {
        return slice(air, cx, cz);
    }

    public BitSet floorsForChunk(int cx, int cz) {
        return slice(floors, cx, cz);
    }

    private BitSet slice(BitSet bits, int cx, int cz) {
        int x = Math.multiplyExact(cx, 16), z = Math.multiplyExact(cz, 16);
        var result = new BitSet(65536);
        for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
            int wx = ox + i / 8192, wz = oz + (i / 256) % 32, y = i % 256;
            if ((long) wx - x >= 0 && (long) wx - x < 16 && (long) wz - z >= 0 && (long) wz - z < 16)
                result.set(((wx - x) * 16 + wz - z) * 256 + y);
        }
        return result;
    }

    public static RegionalCaveRouteRoot15Literal prepare(long seed, int cx, int cz, CaveComposition.Host original,
        CaveComposition.Density field, CaveComposition.Limits limits) {
        return prepare(seed, cx, cz, original, field, limits, false);
    }

    /**
     * Fresh successor: both ends must reach distinct actual composed-clearance rooms, not merely
     * positive noise samples. All proofs use the original host with only retained floors reserved.
     */
    public static RegionalCaveRouteRoot15Literal prepareConnected(long seed, int cx, int cz,
        CaveComposition.Host original, CaveComposition.Density field, CaveComposition.Limits limits) {
        return prepare(seed, cx, cz, original, field, limits, true);
    }

    /**
     * Additive upper-cave frame, not stretching the historical staircase algorithm. The complete
     * original-room and retained-floor proofs run in a translated source frame; unavailable heights
     * remain protected. Offset64 yields source feet75..180 / native139..244.
     */
    public static RegionalCaveRouteRoot15Literal prepareUpperConnected(long seed, int cx, int cz,
        CaveComposition.Host original, CaveComposition.Density field) {
        Objects.requireNonNull(original);
        Objects.requireNonNull(field);
        final int offset = 64;
        CaveComposition.Host shifted = (x, y, z) -> y < 0 || y + offset >= 256 ? CaveComposition.Material.PROTECTED
            : original.at(x, y + offset, z);
        CaveComposition.Density shiftedField = (x, y, z) -> field.at(x, y + offset, z);
        var plan = prepareConnected(
            seed ^ 0x5550504552524F55L,
            cx,
            cz,
            shifted,
            shiftedField,
            new CaveComposition.Limits(8, 120, 10, 4, 4));
        BitSet air = new BitSet(262144), floors = new BitSet(262144);
        translate(plan.air, air, offset);
        translate(plan.floors, floors, offset);
        return new RegionalCaveRouteRoot15Literal(
            plan.ox,
            plan.oz,
            air,
            floors,
            plan.route.stream()
                .map(p -> new Foot(p.x, p.y + offset, p.z))
                .toList());
    }

    private static void translate(BitSet original, BitSet translated, int offset) {
        for (int i = original.nextSetBit(0); i >= 0; i = original.nextSetBit(i + 1)) {
            int y = i % 256 + offset;
            if (y >= 256) throw new IllegalStateException("Upper route exceeds source frame");
            translated.set(i - i % 256 + y);
        }
    }

    private static RegionalCaveRouteRoot15Literal prepare(long seed, int cx, int cz, CaveComposition.Host original,
        CaveComposition.Density field, CaveComposition.Limits limits, boolean connected) {
        Objects.requireNonNull(original);
        Objects.requireNonNull(field);
        Objects.requireNonNull(limits);
        if (limits.minY() != 8 || limits.maxY() != 120
            || limits.roof() != 10
            || limits.wall() != 4
            || limits.floor() != 4)
            throw new IllegalArgumentException("Route admission requires its exact signed clearance limits");
        int ox = Math.multiplyExact(Math.floorDiv(cx, 2), 32), oz = Math.multiplyExact(Math.floorDiv(cz, 2), 32);
        Math.addExact(ox, 31);
        Math.addExact(oz, 31);
        List<Port> ports = new ArrayList<>();
        // New endpoints start at actual supported room feet, not the highest positive voxel.
        // A high density sample can be several blocks above a cavern floor and certify only a slit.
        BitSet empty = new BitSet();
        BitSet qualified = connected ? ownerRooms(ox, oz, original, field, limits, empty) : null;
        // The connected successor takes one actual supported endpoint per walkable room component.
        // No coordinate hunt: the complete existing owner is its finite candidate domain.
        BitSet selectedComponents = new BitSet(262144);
        int[] candidates = connected ? java.util.stream.IntStream.rangeClosed(6, 25)
            .toArray() : new int[] { 6, 12, 19, 25 };
        // Body±1 plus wall4 fits within the owner; even a neighboring owner's plan needs no extra margin reads.
        outer: for (int x : candidates) for (int z : candidates) {
            for (int y = MAX_FEET; y >= MIN_FEET; y--) {
                if (connected) {
                    if (selectedComponents.get(index(x, y, z))) continue;
                    if (!roomVolume(qualified, x, y, z)
                        || !walkable(qualified, empty, empty, ox, oz, original, x, y, z)) continue;
                } else {
                    double value = field.at(ox + x, y + 1, oz + z);
                    if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite route port density");
                    if (value <= 0) continue;
                }
                var port = new Port(x, y, z);
                if (portFits(original, field, ox, oz, port, limits)) {
                    ports.add(port);
                    if (connected)
                        selectedComponents.or(footComponent(qualified, empty, empty, ox, oz, original, index(x, y, z)));
                    if (ports.size() == MAX_PORTS) break outer;
                    break;
                }
            }
        }
        List<Pair> pairs = new ArrayList<>();
        int order = 0;
        for (int a = 0; a < ports.size(); a++) for (int b = a + 1; b < ports.size(); b++) {
            Port first = ports.get(a), last = ports.get(b);
            int distance = Math.abs(first.x - last.x) + Math.abs(first.z - last.z);
            int rise = Math.abs(first.y - last.y);
            if (distance >= 12 && distance <= MAX_STEPS && rise <= MAX_RISE && distance >= 3 * rise + 4)
                pairs.add(new Pair(first, last, order++, distance + 4 * rise));
        }
        pairs.sort(
            Comparator.comparingInt(Pair::score)
                .reversed()
                .thenComparingInt(Pair::order));
        boolean xFirst = ((seed ^ ((long) Math.floorDiv(cx, 2) << 32) ^ Math.floorDiv(cz, 2)) & 1) == 0;
        for (int i = 0; i < Math.min(MAX_PAIRS, pairs.size()); i++)
            for (boolean direction : new boolean[] { xFirst, !xFirst }) {
                Pair pair = pairs.get(i);
                var proposal = connector(original, field, ox, oz, pair.a, pair.b, direction, limits);
                if (proposal != null && (!connected || proposal.connectsUsefulRooms(original, field, limits)))
                    return proposal;
            }
        return new RegionalCaveRouteRoot15Literal(ox, oz, new BitSet(), new BitSet(), List.of());
    }

    private boolean connectsUsefulRooms(CaveComposition.Host original, CaveComposition.Density field,
        CaveComposition.Limits limits) {
        BitSet rooms = ownerRooms(ox, oz, original, field, limits, floors);
        rooms.andNot(air);
        BitSet first = roomAtEnd(rooms, route.get(0), original);
        if (first.isEmpty()) return false;
        BitSet second = roomAtEnd(rooms, route.get(route.size() - 1), original);
        if (second.isEmpty()) return false;
        var overlap = (BitSet) first.clone();
        overlap.and(second);
        return overlap.isEmpty();
    }

    private static BitSet ownerRooms(int ox, int oz, CaveComposition.Host original, CaveComposition.Density field,
        CaveComposition.Limits limits, BitSet reservedFloors) {
        // Keep every read inside the complete owner. Missing exterior is protected, never guessed rock.
        // Do not turn route AIR into CAVITY here: that would manufacture positive room evidence.
        CaveComposition.Host host = (x, y, z) -> x < ox || x >= ox + 32 || z < oz || z >= oz + 32
            ? CaveComposition.Material.PROTECTED
            : y >= 0 && y < 256 && reservedFloors.get(index(x - ox, y, z - oz)) ? CaveComposition.Material.SOLID
                : original.at(x, y, z);
        BitSet rooms = new BitSet(262144);
        for (int cx = ox / 16; cx < ox / 16 + 2; cx++) for (int cz = oz / 16; cz < oz / 16 + 2; cz++) {
            var mask = CaveComposition.planWithClearance(cx, cz, host, field, limits);
            for (int i = mask.nextSetBit(0); i >= 0; i = mask.nextSetBit(i + 1)) {
                int x = cx * 16 + i / 4096 - ox, z = cz * 16 + (i / 256) % 16 - oz, y = i % 256;
                // Retain low pre-liquid excavation bits for floor vetoes: deleting them would
                // mislabel a future WATER/AIR floor as solid. Only feet>=11 can enter the graph.
                rooms.set(index(x, y, z));
            }
        }
        return rooms;
    }

    private BitSet roomAtEnd(BitSet rooms, Foot end, CaveComposition.Host original) {
        return walkableRoomAtEnd(rooms, air, floors, ox, oz, end, original);
    }

    /**
     * Feet-position graph, not voxel connectivity: every edge has two-block collision clearance,
     * a retained solid floor and at most one jumpable block of rise. No swimming or flying proof.
     */
    static BitSet walkableRoomAtEnd(BitSet rooms, BitSet routeAir, BitSet floors, int ox, int oz, Foot end,
        CaveComposition.Host original) {
        BitSet checked = new BitSet(262144);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -1; dy <= 1; dy++) {
            int x = end.x + dx - ox, z = end.z + dz - oz, y = end.y + dy;
            if (x < 0 || x >= 32 || z < 0 || z >= 32 || y < 0 || y >= 256) continue;
            int seed = index(x, y, z);
            if (!walkable(rooms, routeAir, floors, ox, oz, original, x, y, z) || checked.get(seed)) continue;
            // A supported mouth may step one block from the actual endpoint, not fly to a high voxel.
            boolean touches = false;
            for (int[] d : HORIZONTAL)
                if (clear(routeAir, x + d[0], end.y, z + d[1]) && clear(routeAir, x + d[0], end.y + 1, z + d[1])
                    && clear(floors, x + d[0], end.y - 1, z + d[1])
                    && (dy <= 0 || clear(routeAir, x + d[0], end.y + 2, z + d[1]))
                    && (dy >= 0 || clear(rooms, x, y + 2, z))) {
                        touches = true;
                        break;
                    }
            if (!touches) continue;
            BitSet component = footComponent(rooms, routeAir, floors, ox, oz, original, seed);
            checked.or(component);
            boolean wholeRoom = false;
            for (int i = component.nextSetBit(0); i >= 0; i = component.nextSetBit(i + 1))
                if (roomVolume(rooms, i / 8192, i % 256, (i / 256) % 32)) {
                    wholeRoom = true;
                    break;
                }
            if (wholeRoom) return component;
        }
        return new BitSet();
    }

    private static BitSet footComponent(BitSet rooms, BitSet routeAir, BitSet floors, int ox, int oz,
        CaveComposition.Host original, int seed) {
        BitSet component = new BitSet(262144);
        var queue = new java.util.ArrayDeque<Integer>();
        queue.add(seed);
        while (!queue.isEmpty()) {
            int i = queue.removeFirst();
            if (component.get(i)) continue;
            component.set(i);
            int px = i / 8192, pz = (i / 256) % 32, py = i % 256;
            for (int[] d : HORIZONTAL) for (int rise = -1; rise <= 1; rise++) {
                int nx = px + d[0], ny = py + rise, nz = pz + d[1];
                if (!walkable(rooms, routeAir, floors, ox, oz, original, nx, ny, nz)) continue;
                // Crossing a step needs clearance at the higher head on BOTH columns.
                if (rise > 0 && !clear(rooms, px, py + 2, pz) || rise < 0 && !clear(rooms, nx, ny + 2, nz)) continue;
                int next = index(nx, ny, nz);
                if (!component.get(next)) queue.add(next);
            }
        }
        return component;
    }

    private static boolean clear(BitSet cells, int x, int y, int z) {
        return x >= 0 && x < 32 && z >= 0 && z < 32 && y >= 0 && y < 256 && cells.get(index(x, y, z));
    }

    private static boolean walkable(BitSet rooms, BitSet routeAir, BitSet floors, int ox, int oz,
        CaveComposition.Host original, int x, int y, int z) {
        return y >= MIN_FEET && clear(rooms, x, y, z)
            && clear(rooms, x, y + 1, z)
            && !clear(rooms, x, y - 1, z)
            && !clear(routeAir, x, y - 1, z)
            && (clear(floors, x, y - 1, z) || solid(original.at(ox + x, y - 1, oz + z)));
    }

    private static boolean roomVolume(BitSet rooms, int x, int y, int z) {
        if (x < 1 || x >= 31 || z < 1 || z >= 31 || y > 252) return false;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            for (int dy = 0; dy < CLEAR_HEIGHT; dy++) if (!rooms.get(index(x + dx, y + dy, z + dz))) return false;
        return true;
    }

    private static final int[][] HORIZONTAL = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

    private static boolean portFits(CaveComposition.Host host, CaveComposition.Density field, int ox, int oz, Port p,
        CaveComposition.Limits limits) {
        for (int x = p.x - 1; x <= p.x + 1; x++) for (int z = p.z - 1; z <= p.z + 1; z++) {
            if (!solid(host.at(ox + x, p.y - 1, oz + z))) return false;
            if (!retainedBed(host, field, ox + x, p.y - limits.floor() - 1, oz + z, limits)) return false;
            for (int y = p.y; y < p.y + CLEAR_HEIGHT; y++) if (!guarded(host, ox + x, y, oz + z, limits)) return false;
        }
        return true;
    }

    private static RegionalCaveRouteRoot15Literal connector(CaveComposition.Host host, CaveComposition.Density field,
        int ox, int oz, Port a, Port b, boolean xFirst, CaveComposition.Limits limits) {
        List<Foot> path = new ArrayList<>();
        int x = a.x, z = a.z;
        path.add(new Foot(x, 0, z));
        for (int axis = 0; axis < 2; axis++) {
            boolean changeX = axis == 0 ? xFirst : !xFirst;
            while (changeX ? x != b.x : z != b.z) {
                if (changeX) x += Integer.signum(b.x - x);
                else z += Integer.signum(b.z - z);
                path.add(new Foot(x, 0, z));
            }
        }
        if (path.size() - 1 > MAX_STEPS) return null;
        int[] heights = new int[1024];
        Arrays.fill(heights, -1);
        int rise = Math.abs(b.y - a.y), sign = Integer.signum(b.y - a.y);
        // Two flat approach steps at both ports; one natural block rise per three horizontal steps (jumpable,
        // not a claim that vanilla's0.6 automatic step height climbs full blocks without jumping).
        for (int i = 0; i < path.size(); i++) {
            Foot p = path.get(i);
            int feet = a.y + sign * Math.min(rise, Math.max(0, (i - 2) / 3));
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                int column = (p.x + dx) * 32 + p.z + dz;
                heights[column] = Math.max(heights[column], feet - 1);
            }
        }
        List<Foot> actual = new ArrayList<>();
        int previous = -1;
        for (Foot p : path) {
            int feet = heights[p.x * 32 + p.z] + 1;
            if (feet < MIN_FEET || feet > MAX_FEET || previous >= 0 && Math.abs(feet - previous) > 1) return null;
            actual.add(new Foot(ox + p.x, feet, oz + p.z));
            previous = feet;
        }
        if (actual.get(0).y != a.y || actual.get(actual.size() - 1).y != b.y) return null;
        BitSet air = new BitSet(262144), floor = new BitSet(262144);
        for (int i = 0; i < heights.length; i++) if (heights[i] >= 0) {
            int lx = i / 32, lz = i % 32, y = heights[i];
            // Preserve the whole existing floor envelope, and attach it directly to original rock which
            // no chamber seed can excavate. No fabricated pillar or suspended one-block floor ribbon.
            for (int d = 0; d < limits.floor(); d++) {
                if (!solid(host.at(ox + lx, y - d, oz + lz))) return null;
                floor.set(index(lx, y - d, lz));
            }
            if (!retainedBed(host, field, ox + lx, y - limits.floor(), oz + lz, limits)) return null;
            for (int dy = 1; dy <= CLEAR_HEIGHT; dy++) {
                if (!guarded(host, ox + lx, y + dy, oz + lz, limits)) return null;
                air.set(index(lx, y + dy, lz));
            }
        }
        return new RegionalCaveRouteRoot15Literal(ox, oz, air, floor, actual);
    }

    private static boolean guarded(CaveComposition.Host host, int x, int y, int z, CaveComposition.Limits limits) {
        if (y < MIN_FEET || y > limits.maxY() || host.at(x, y, z) != CaveComposition.Material.ROCK) return false;
        for (int d = 1; d <= limits.roof(); d++) if (!solid(host.at(x, y + d, z))) return false;
        for (int d = 1; d <= limits.floor(); d++) if (!solid(host.at(x, y - d, z))) return false;
        for (int d = 1; d <= limits.wall(); d++) if (!solid(host.at(x - d, y, z)) || !solid(host.at(x + d, y, z))
            || !solid(host.at(x, y, z - d))
            || !solid(host.at(x, y, z + d))) return false;
        return true;
    }

    private static boolean solid(CaveComposition.Material m) {
        return m == CaveComposition.Material.ROCK || m == CaveComposition.Material.SOLID;
    }

    private static boolean retainedBed(CaveComposition.Host host, CaveComposition.Density field, int x, int y, int z,
        CaveComposition.Limits limits) {
        if (y < 0 || !solid(host.at(x, y, z))) return false;
        if (y < limits.minY()) return true;
        // A3x3x4 seed can write this cell only from these nearby seed centers. Negative everywhere is
        // conservative proof of retained original bedrock/stone, not an optimistic fit at one endpoint.
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int sy = y - 2; sy <= y + 1; sy++) {
            if (sy < limits.minY() + 1 || sy > limits.maxY() - 2) continue;
            double value = field.at(x + dx, sy, z + dz);
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite route support density");
            if (value > 0) return false;
        }
        return true;
    }

    private static int index(int x, int y, int z) {
        return (x * 32 + z) * 256 + y;
    }
}
