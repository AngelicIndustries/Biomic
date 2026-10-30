package industries.angelic.biomic.content.alexscaves;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import industries.angelic.biomic.world.modern.ModernDecorationGraph;

/** Bounded single-write port of pinned Alex's Caves MusselBlock.randomTick, revision 4718f428. */
public final class AbyssalMusselGrowth {

    private AbyssalMusselGrowth() {}

    public enum Facing {

        DOWN(0, -1, 0),
        UP(0, 1, 0),
        NORTH(0, 0, -1),
        SOUTH(0, 0, 1),
        WEST(-1, 0, 0),
        EAST(1, 0, 0);

        final int x, y, z;

        Facing(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Facing opposite() {
            return values()[ordinal() ^ 1];
        }
    }

    public record Position(int x, int y, int z) {

        public Position offset(int dx, int dy, int dz) {
            return new Position(Math.addExact(x, dx), Math.addExact(y, dy), Math.addExact(z, dz));
        }

        public Position relative(Facing direction) {
            return offset(direction.x, direction.y, direction.z);
        }
    }

    public record Colony(int count, Facing facing, boolean wet) {

        public Colony {
            if (count < 1 || count > 5 || facing == null) throw new IllegalArgumentException("Invalid mussel colony");
        }

        public int metadata() {
            return facing.ordinal() | (wet ? 0 : 8);
        }
    }

    public record Cell(boolean sourceWater, boolean growsMussels, Colony colony) {}

    public interface Cells {

        default boolean inBuildHeight(Position position) {
            return true;
        }

        boolean loaded(Position position);

        Cell read(Position position);

        boolean sturdy(Position position, Facing face);
    }

    public record Growth(Position position, Cell before, Colony after) {}

    private static final class Tags {

        static final ModernDecorationGraph GRAPH = new ModernDecorationGraph();
    }

    /** Exact source tag expansion, not Material.wood or a generic sturdy-support permission. */
    public static boolean growsOn(String authoredId) {
        return "alexscaves:geothermal_vent".equals(authoredId) || Tags.GRAPH.blockTag("minecraft:logs")
            .contains(authoredId)
            || Tags.GRAPH.blockTag("minecraft:planks")
                .contains(authoredId);
    }

    public static Optional<Growth> propose(Position origin, Colony state, Cells cells, Random random) {
        Position connected = origin.relative(
            state.facing()
                .opposite());
        if (!cells.loaded(connected)) return Optional.empty();
        Cell support = cells.read(connected);
        // Source reads its attachment before this RNG draw, even when the attachment is not in the grow tag.
        if (random.nextInt(20) != 0 || !support.growsMussels()) return Optional.empty();
        if (state.count() < 5) {
            if (!cells.loaded(origin)) return Optional.empty();
            Cell before = cells.read(origin);
            if (!state.equals(before.colony())) return Optional.empty();
            return Optional.of(new Growth(origin, before, new Colony(state.count() + 1, state.facing(), state.wet())));
        }
        Position target = connected.offset(random.nextInt(6) - 3, random.nextInt(6) - 3, random.nextInt(6) - 3);
        if (!cells.inBuildHeight(target) || !cells.loaded(target)) return Optional.empty();
        Cell before = cells.read(target);
        Colony existing = before.colony();
        // Modern Blocks.WATER also represents flowing levels. Native carriers cannot retain those levels:
        // deliberately require source-level water, or an existing exact waterlogged mussel.
        if (!before.sourceWater() && (existing == null || !existing.wet())) return Optional.empty();
        List<Facing> possible = new ArrayList<>();
        for (Facing direction : Facing.values()) {
            Position neighbor = target.relative(direction);
            if (!cells.inBuildHeight(neighbor)) continue;
            if (!cells.loaded(neighbor)) return Optional.empty();
            if (cells.sturdy(neighbor, direction.opposite())) possible.add(direction.opposite());
        }
        if (possible.isEmpty()) return Optional.empty();
        Facing chosen = possible.size() == 1 ? possible.get(0) : possible.get(random.nextInt(possible.size() - 1));
        long dx = (long) target.x - origin.x, dy = (long) target.y - origin.y, dz = (long) target.z - origin.z;
        int sourceLimit = Math.max(
            1,
            Math.min(
                5,
                6 - (int) Math
                    .min(Integer.MAX_VALUE, Math.ceil((double) dx * dx + (double) dy * dy + (double) dz * dz))));
        int current = existing == null ? 0 : existing.count(), limit = Math.max(current, sourceLimit);
        return Optional.of(
            new Growth(
                target,
                before,
                new Colony(
                    existing == null ? 1 : Math.min(current + 1, limit),
                    existing == null ? chosen : existing.facing(),
                    true)));
    }

    public static int spongeMetadata(int before, boolean spongeAbove) {
        return before < 0 || before > 1 ? before : spongeAbove ? 0 : 1;
    }

    public static boolean spongeSupport(boolean spongeBelow, boolean sturdyTop, boolean magma) {
        return spongeBelow || sturdyTop && !magma;
    }
}
