package industries.angelic.biomic.world.alexscaves;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import industries.angelic.biomic.world.modern.noise.ModernWorldgenRandom;

/** Pure whole-feature transactions for the pinned Abyssal Chasm intrinsic ecology. */
public final class AbyssalFeaturePlanner {

    public enum HostCell {
        WATER,
        MUCK,
        TUFF,
        REPLACEABLE,
        SOLID_SUPPORT,
        UNMOVEABLE,
        PROTECTED,
        OUTSIDE
    }

    public enum FailureReason {
        FOOTPRINT_UNAVAILABLE,
        INVALID_SUPPORT,
        PROTECTED_INTERSECTION,
        EMPTY
    }

    public enum Flora {

        DUSK("dusk_anemone"),
        TWILIGHT("twilight_anemone"),
        MIDNIGHT("midnight_anemone");

        final String id;

        Flora(String id) {
            this.id = id;
        }
    }

    public enum Direction {

        DOWN(0, -1, 0, "down"),
        UP(0, 1, 0, "up"),
        NORTH(0, 0, -1, "north"),
        SOUTH(0, 0, 1, "south"),
        WEST(-1, 0, 0, "west"),
        EAST(1, 0, 0, "east");

        final int x, y, z;
        final String id;

        Direction(int x, int y, int z, String id) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.id = id;
        }

        Direction opposite() {
            return values()[ordinal() ^ 1];
        }
    }

    public record Position(int x, int y, int z) {}

    public record Placement(Position position, String state) {}

    public record Bounds(int minY, int maxY) {

        public Bounds {
            if (minY > maxY) throw new IllegalArgumentException("Invalid Abyssal feature bounds");
        }
    }

    public interface PredictingHost {

        boolean covers(Bounds bounds);

        HostCell predict(int x, int y, int z);
    }

    public static final class Proposal {

        private final FailureReason failure;
        private final List<Placement> placements;

        private Proposal(FailureReason failure, Map<Position, String> writes) {
            this.failure = failure;
            List<Placement> copy = new ArrayList<>();
            if (writes != null) writes.forEach((p, s) -> copy.add(new Placement(p, s)));
            placements = List.copyOf(copy);
        }

        public boolean accepted() {
            return failure == null;
        }

        public FailureReason failureReason() {
            return failure;
        }

        public List<Placement> placements() {
            return placements;
        }
    }

    private AbyssalFeaturePlanner() {}

    public static Proposal boulder(long seed, Position origin, PredictingHost host,
        AbyssalSourceHeightTransport height) {
        Objects.requireNonNull(origin);
        Objects.requireNonNull(host);
        int minimum = height.physicalY(-64);
        Bounds bounds = new Bounds(minimum, origin.y + 4);
        if (!host.covers(bounds)) return rejected(FailureReason.FOOTPRINT_UNAVAILABLE);
        ModernWorldgenRandom random = random(seed);
        int y = origin.y;
        while (y > minimum + 1) {
            HostCell below = required(host, origin.x, y - 1, origin.z);
            if (below == HostCell.MUCK) break;
            y--;
        }
        if (y <= minimum + 1) return rejected(FailureReason.INVALID_SUPPORT);
        Position center = new Position(origin.x, y + 1, origin.z);
        Map<Position, String> writes = new LinkedHashMap<>();
        for (int blob = 0; blob < 3; blob++) {
            int rx = random.nextInt(3), ry = random.nextInt(3), rz = random.nextInt(3);
            double radius = Math.pow((rx + ry + rz) * .333F + .5F, 2D);
            for (int x = center.x - rx; x <= center.x + rx; x++) for (int py = center.y - ry; py <= center.y + ry; py++)
                for (int z = center.z - rz; z <= center.z + rz; z++) {
                    long dx = (long) x - center.x, dy = (long) py - center.y, dz = (long) z - center.z;
                    if (dx * dx + dy * dy + dz * dz > radius) continue;
                    Position at = new Position(x, py, z);
                    HostCell cell = cell(host, writes, at);
                    if (cell == HostCell.UNMOVEABLE) continue;
                    FailureReason failure = replaceFailure(cell);
                    if (failure != null) return rejected(failure);
                    writes.put(at, "minecraft:deepslate");
                    if (random.nextInt(2) == 0) {
                        Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
                        Position mussel = move(at, direction);
                        HostCell target = cell(host, writes, mussel);
                        if (target == HostCell.WATER) writes.put(mussel, mussel(direction, 1 + random.nextInt(4)));
                    }
                }
            center = new Position(
                center.x - 2 + random.nextInt(4),
                center.y - random.nextInt(2),
                center.z - 2 + random.nextInt(4));
        }
        return writes.isEmpty() ? rejected(FailureReason.EMPTY) : accepted(writes);
    }

    public static Proposal blackVent(long seed, Position origin, PredictingHost host,
        AbyssalSourceHeightTransport height) {
        Objects.requireNonNull(origin);
        Objects.requireNonNull(host);
        int minimum = height.physicalY(-64);
        Bounds bounds = new Bounds(minimum, origin.y + 7);
        if (!host.covers(bounds)) return rejected(FailureReason.FOOTPRINT_UNAVAILABLE);
        int y = origin.y;
        while (y > minimum && required(host, origin.x, y, origin.z) == HostCell.WATER) {
            y--;
        }
        HostCell support = required(host, origin.x, y - 1, origin.z);
        if (support != HostCell.TUFF) return rejected(FailureReason.INVALID_SUPPORT);
        ModernWorldgenRandom random = random(seed);
        int featureHeight = random.nextInt(6) + 2, bottom = y;
        Map<Position, String> writes = new LinkedHashMap<>();
        for (Direction d : new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST })
            writes.put(new Position(origin.x + d.x, bottom, origin.z + d.z), "minecraft:tuff");
        writes.put(new Position(origin.x, bottom - 1, origin.z), "minecraft:tuff");
        writes.put(new Position(origin.x, bottom, origin.z), "minecraft:lava");
        int middleStart = Math.max(1, featureHeight / 3), middleTop = middleStart * 2;
        for (int i = 1; i <= featureHeight; i++) {
            String id = i <= middleStart ? "geothermal_vent"
                : i > middleTop ? "geothermal_vent_thin" : "geothermal_vent_medium";
            boolean waterlogged = i > middleStart;
            writes.put(
                new Position(origin.x, bottom + i, origin.z),
                "alexscaves:" + id
                    + "[smoke_type=2,spawning_particles="
                    + (i == featureHeight)
                    + ",waterlogged="
                    + waterlogged
                    + "]");
        }
        FailureReason failure = validateWrites(host, writes);
        if (failure != null) return rejected(failure);
        return accepted(writes);
    }

    public static Proposal sponge(long seed, Position origin, PredictingHost host,
        AbyssalSourceHeightTransport height) {
        Objects.requireNonNull(origin);
        Objects.requireNonNull(host);
        int minimum = height.physicalY(-64);
        Bounds bounds = new Bounds(minimum, origin.y + 4);
        if (!host.covers(bounds)) return rejected(FailureReason.FOOTPRINT_UNAVAILABLE);
        int y = origin.y;
        while (y > minimum && required(host, origin.x, y, origin.z) == HostCell.WATER) {
            y--;
        }
        if (required(host, origin.x, y - 1, origin.z) != HostCell.MUCK || height.sourceDepth(origin.y, y) < 15)
            return rejected(FailureReason.INVALID_SUPPORT);
        int featureHeight = (int) Math.ceil(random(seed).nextFloat() * 3.5F);
        Map<Position, String> writes = new LinkedHashMap<>();
        for (int i = 0; i <= featureHeight; i++) {
            Position at = new Position(origin.x, y + i, origin.z);
            HostCell cell = required(host, at.x, at.y, at.z);
            if (cell == HostCell.WATER)
                writes.put(at, "alexscaves:ping_pong_sponge[top=" + (i == featureHeight) + ",waterlogged=true]");
        }
        return writes.isEmpty() ? rejected(FailureReason.EMPTY) : accepted(writes);
    }

    public static Proposal flora(Flora flora, Position origin, PredictingHost host,
        AbyssalSourceHeightTransport height) {
        Objects.requireNonNull(flora);
        Objects.requireNonNull(origin);
        Objects.requireNonNull(host);
        int minimum = height.physicalY(-64);
        Bounds bounds = new Bounds(minimum, origin.y);
        if (!host.covers(bounds)) return rejected(FailureReason.FOOTPRINT_UNAVAILABLE);
        int y = origin.y;
        while (y > minimum && required(host, origin.x, y, origin.z) == HostCell.WATER) {
            y--;
        }
        FailureReason floorFailure = replaceFailure(required(host, origin.x, y, origin.z));
        if (floorFailure != null) return rejected(floorFailure);
        if (height.sourceDepth(origin.y, y) < 15) return rejected(FailureReason.INVALID_SUPPORT);
        Position at = new Position(origin.x, y + 1, origin.z);
        if (required(host, at.x, at.y, at.z) != HostCell.WATER) return rejected(FailureReason.EMPTY);
        Map<Position, String> writes = new LinkedHashMap<>();
        writes.put(at, "alexscaves:" + flora.id + "[waterlogged=true]");
        return accepted(writes);
    }

    public static Proposal mussel(long seed, Position origin, PredictingHost host) {
        Objects.requireNonNull(origin);
        Objects.requireNonNull(host);
        int minChunkX = origin.x >> 4, minChunkZ = origin.z >> 4;
        Bounds bounds = new Bounds(origin.y - 2, origin.y + 1);
        if (!host.covers(bounds)) return rejected(FailureReason.FOOTPRINT_UNAVAILABLE);
        ModernWorldgenRandom random = random(seed);
        Map<Position, String> writes = new LinkedHashMap<>();
        boolean placed = tryMussel(host, writes, origin, minChunkX, minChunkZ, random);
        if (placed) for (int i = 0, count = random.nextInt(3); i < count; i++) tryMussel(
            host,
            writes,
            new Position(
                origin.x + random.nextInt(4) - 2,
                origin.y + random.nextInt(4) - 2,
                origin.z + random.nextInt(4) - 2),
            minChunkX,
            minChunkZ,
            random);
        return writes.isEmpty() ? rejected(FailureReason.EMPTY) : accepted(writes);
    }

    public static Proposal tubeWorm(long seed, Position origin, PredictingHost host,
        AbyssalSourceHeightTransport height) {
        Objects.requireNonNull(origin);
        Objects.requireNonNull(host);
        int minimum = height.physicalY(-64), maximum = height.physicalY(319);
        Bounds bounds = new Bounds(minimum, Math.min(maximum, origin.y + 20));
        if (!host.covers(bounds)) return rejected(FailureReason.FOOTPRINT_UNAVAILABLE);
        int bottom = origin.y;
        while (bottom > minimum && required(host, origin.x, bottom, origin.z) == HostCell.WATER) {
            bottom--;
        }
        int ceiling = height.physicalTubeCeilingY();
        if (bottom >= ceiling || required(host, origin.x, bottom - 1, origin.z) != HostCell.TUFF)
            return rejected(FailureReason.INVALID_SUPPORT);
        ModernWorldgenRandom random = random(seed);
        Map<Position, String> writes = new LinkedHashMap<>();
        int worms = 4 + random.nextInt(4);
        Direction[] sourceHorizontal = { Direction.NORTH, Direction.EAST, Direction.SOUTH };
        for (int attempt = 0; attempt < worms; attempt++) {
            Position at = new Position(
                origin.x + random.nextInt(10) - 5,
                bottom + random.nextInt(4),
                origin.z + random.nextInt(10) - 5);
            Direction attach = Direction.DOWN, direction = sourceHorizontal[random.nextInt(3)];
            Position side = move(at, direction);
            HostCell sideCell = cell(host, writes, side);
            Position attached;
            if (solid(sideCell)) {
                attach = direction;
                attached = side;
            } else {
                while (at.y < maximum && solid(cell(host, writes, at))) {
                    at = new Position(at.x, at.y + 1, at.z);
                }
                while (at.y > minimum && waterCell(host, writes, at)) {
                    at = new Position(at.x, at.y - 1, at.z);
                }
                attached = new Position(at.x, at.y - 1, at.z);
            }
            HostCell avoided = cell(host, writes, at);
            if (avoided == HostCell.UNMOVEABLE || tube(writes, at)) continue;
            int maxSegments = 4 + random.nextInt(12);
            growTube(host, writes, attached, attach, random, maxSegments, sourceHorizontal, ceiling);
        }
        FailureReason failure = validateWrites(host, writes);
        if (failure != null) return rejected(failure);
        return writes.isEmpty() ? rejected(FailureReason.EMPTY) : accepted(writes);
    }

    private static void growTube(PredictingHost host, Map<Position, String> writes, Position attached, Direction attach,
        ModernWorldgenRandom random, int maxSegments, Direction[] sourceHorizontal, int ceiling) {
        if (attached.y > ceiling) return;
        int placed = 0;
        Position worm = move(attached, attach.opposite());
        boolean canBranch = false;
        String straight = tubeState("straight", Direction.NORTH);
        while (placed < maxSegments) {
            Position previous = worm;
            String state = straight;
            if (worm.y > ceiling) return;
            if (canBranch) {
                if (random.nextBoolean()) {
                    Direction direction = sourceHorizontal[random.nextInt(3)];
                    worm = move(worm, direction);
                    if (waterCell(host, writes, worm)) {
                        if (!replaceableWater(host, writes, worm)) return;
                        if (replaceableWater(host, writes, previous) || tube(writes, previous))
                            writes.put(previous, tubeState("turn", direction));
                        state = tubeState("elbow", direction.opposite());
                    } else worm = new Position(previous.x, previous.y + 1, previous.z);
                }
                canBranch = false;
            } else {
                worm = new Position(worm.x, worm.y + 1, worm.z);
                canBranch = placed > 1;
            }
            if (!waterCell(host, writes, worm)) break;
            if (replaceableWater(host, writes, worm)) writes.put(worm, state);
            placed++;
        }
        Position attachment = move(attached, attach.opposite());
        if (attach != Direction.DOWN && attach != Direction.UP
            && (replaceableWater(host, writes, attachment) || tube(writes, attachment)))
            writes.put(attachment, tubeState("elbow", attach));
    }

    private static boolean tryMussel(PredictingHost host, Map<Position, String> writes, Position at, int chunkX,
        int chunkZ, ModernWorldgenRandom random) {
        if (cell(host, writes, at) != HostCell.WATER) return false;
        List<Direction> possible = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            Position check = move(at, direction);
            if ((check.x >> 4) == chunkX && (check.z >> 4) == chunkZ) {
                HostCell support = cell(host, writes, check);
                if (solid(support)) possible.add(direction.opposite());
            }
        }
        if (possible.isEmpty()) return false;
        Direction chosen = possible.size() == 1 ? possible.get(0) : possible.get(random.nextInt(possible.size() - 1));
        writes.put(at, mussel(chosen, 1 + random.nextInt(4)));
        return true;
    }

    private static String mussel(Direction direction, int count) {
        return "alexscaves:mussel[facing=" + direction.id + ",mussels=" + count + ",waterlogged=true]";
    }

    private static String tubeState(String type, Direction direction) {
        return "alexscaves:tube_worm[facing=" + direction.id + ",type=" + type + ",waterlogged=true]";
    }

    private static boolean tube(Map<Position, String> writes, Position at) {
        String state = writes.get(at);
        return state != null && state.startsWith("alexscaves:tube_worm[");
    }

    private static boolean waterCell(PredictingHost host, Map<Position, String> writes, Position at) {
        String state = writes.get(at);
        return state == null ? required(host, at.x, at.y, at.z) == HostCell.WATER : state.contains("waterlogged=true");
    }

    private static boolean replaceableWater(PredictingHost host, Map<Position, String> writes, Position at) {
        String state = writes.get(at);
        return state == null ? required(host, at.x, at.y, at.z) == HostCell.WATER : tube(writes, at);
    }

    private static HostCell cell(PredictingHost host, Map<Position, String> writes, Position at) {
        String state = writes.get(at);
        if (state == null) return required(host, at.x, at.y, at.z);
        if (state.equals("minecraft:tuff") || state.equals("minecraft:deepslate")) return HostCell.SOLID_SUPPORT;
        if (state.equals("minecraft:lava")) return HostCell.UNMOVEABLE;
        return state.contains("mussel") ? HostCell.REPLACEABLE : HostCell.WATER;
    }

    private static FailureReason validateWrites(PredictingHost host, Map<Position, String> writes) {
        for (Position at : writes.keySet()) {
            FailureReason failure = replaceFailure(required(host, at.x, at.y, at.z));
            if (failure != null) return failure;
        }
        return null;
    }

    private static FailureReason replaceFailure(HostCell cell) {
        return switch (cell) {
            case OUTSIDE -> FailureReason.FOOTPRINT_UNAVAILABLE;
            case PROTECTED -> FailureReason.PROTECTED_INTERSECTION;
            default -> null;
        };
    }

    private static boolean solid(HostCell cell) {
        return cell == HostCell.MUCK || cell == HostCell.TUFF
            || cell == HostCell.SOLID_SUPPORT
            || cell == HostCell.UNMOVEABLE;
    }

    private static HostCell required(PredictingHost host, int x, int y, int z) {
        return Objects.requireNonNull(host.predict(x, y, z), "Predicting host returned no cell");
    }

    private static Position move(Position p, Direction d) {
        return new Position(p.x + d.x, p.y + d.y, p.z + d.z);
    }

    private static ModernWorldgenRandom random(long seed) {
        ModernWorldgenRandom random = new ModernWorldgenRandom();
        random.setSeed(seed);
        return random;
    }

    private static Proposal accepted(Map<Position, String> writes) {
        return new Proposal(null, writes);
    }

    private static Proposal rejected(FailureReason failure) {
        return new Proposal(failure, null);
    }
}
