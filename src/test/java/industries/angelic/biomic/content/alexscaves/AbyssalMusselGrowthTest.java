package industries.angelic.biomic.content.alexscaves;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import industries.angelic.biomic.content.alexscaves.AbyssalMusselGrowth.Cell;
import industries.angelic.biomic.content.alexscaves.AbyssalMusselGrowth.Cells;
import industries.angelic.biomic.content.alexscaves.AbyssalMusselGrowth.Colony;
import industries.angelic.biomic.content.alexscaves.AbyssalMusselGrowth.Facing;
import industries.angelic.biomic.content.alexscaves.AbyssalMusselGrowth.Growth;
import industries.angelic.biomic.content.alexscaves.AbyssalMusselGrowth.Position;

public class AbyssalMusselGrowthTest {

    private static final Cell GENERIC = new Cell(false, false, null), GROW = new Cell(false, true, null),
        WATER = new Cell(true, false, null);

    private static final class Sequence extends Random {

        final List<Integer> bounds = new ArrayList<>();
        final int[] values;
        int index;

        Sequence(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            bounds.add(bound);
            assertTrue(index < values.length);
            int result = values[index++];
            assertTrue(result >= 0 && result < bound);
            return result;
        }
    }

    private static final class Fixture implements Cells {

        final Map<Position, Cell> cells = new HashMap<>();
        final Set<Position> supports = new HashSet<>(), missing = new HashSet<>();
        final List<Position> reads = new ArrayList<>();
        int height = 512;

        public boolean inBuildHeight(Position p) {
            return p.y() >= 0 && p.y() < height;
        }

        public boolean loaded(Position p) {
            return inBuildHeight(p) && !missing.contains(p);
        }

        public Cell read(Position p) {
            assertTrue(loaded(p));
            reads.add(p);
            return cells.getOrDefault(p, GENERIC);
        }

        public boolean sturdy(Position p, Facing f) {
            assertTrue(loaded(p));
            return supports.contains(p);
        }

        void colony(Position p, Colony c) {
            cells.put(p, new Cell(false, false, c));
        }
    }

    @Test
    public void exactGrowthTagDoesNotIncludeGenericWoodStoneThinVentsOrFurniture() {
        for (String id : new String[] { "alexscaves:geothermal_vent", "minecraft:oak_log", "minecraft:oak_planks",
            "minecraft:mangrove_log", "minecraft:crimson_stem" }) assertTrue(id, AbyssalMusselGrowth.growsOn(id));
        for (String id : new String[] { "minecraft:stone", "minecraft:chest", "minecraft:oak_fence",
            "alexscaves:geothermal_vent_medium", "alexscaves:geothermal_vent_thin" })
            assertFalse(id, AbyssalMusselGrowth.growsOn(id));
    }

    @Test
    public void allSixFacesAndDryWetColoniesGrowWithoutChangingFacingOrWater() {
        for (Facing face : Facing.values()) for (boolean wet : new boolean[] { false, true }) {
            Fixture f = new Fixture();
            Position p = new Position(-17, 31, -16);
            Colony colony = new Colony(4, face, wet);
            f.colony(p, colony);
            f.cells.put(p.relative(face.opposite()), GROW);
            var before = new HashMap<>(f.cells);
            Sequence random = new Sequence(0);
            Growth growth = AbyssalMusselGrowth.propose(p, colony, f, random)
                .orElseThrow();
            assertEquals(p, growth.position());
            assertEquals(new Colony(5, face, wet), growth.after());
            assertEquals(List.of(20), random.bounds);
            assertEquals(before, f.cells);
        }
    }

    @Test
    public void attachmentIsReadBeforeChanceAndGenericSupportNeverGrows() {
        Fixture f = new Fixture();
        Position p = new Position(0, 20, 0);
        Colony colony = new Colony(1, Facing.UP, true);
        f.colony(p, colony);
        Sequence random = new Sequence(0);
        assertTrue(
            AbyssalMusselGrowth.propose(p, colony, f, random)
                .isEmpty());
        assertEquals(p.relative(Facing.DOWN), f.reads.get(0));
        assertEquals(List.of(20), random.bounds);
        f.cells.put(p.relative(Facing.DOWN), GROW);
        random = new Sequence(19);
        assertTrue(
            AbyssalMusselGrowth.propose(p, colony, f, random)
                .isEmpty());
        assertEquals(List.of(20), random.bounds);
    }

    private static Fixture spreading(Position p, Colony origin, Cell target) {
        Fixture f = new Fixture();
        f.colony(p, origin);
        f.cells.put(p.relative(Facing.DOWN), GROW);
        Position t = p.offset(0, 0, 1);
        f.cells.put(t, target);
        f.supports.add(t.relative(Facing.DOWN));
        f.supports.add(t.relative(Facing.UP));
        f.supports.add(t.relative(Facing.EAST));
        return f;
    }

    @Test
    public void spreadKeepsOriginalRngOrderAndLastFaceExclusion() {
        Position p = new Position(-16, 300, -17);
        Colony colony = new Colony(5, Facing.UP, true);
        Fixture f = spreading(p, colony, WATER);
        Sequence random = new Sequence(0, 3, 4, 4, 1);
        var growth = AbyssalMusselGrowth.propose(p, colony, f, random)
            .orElseThrow();
        assertEquals(p.offset(0, 0, 1), growth.position());
        assertEquals(new Colony(1, Facing.DOWN, true), growth.after());
        assertEquals(List.of(20, 6, 6, 6, 2), random.bounds);
        // Existing colony keeps its orientation, even though source still draws a candidate facing.
        f = spreading(p, colony, new Cell(false, false, new Colony(2, Facing.WEST, true)));
        random = new Sequence(0, 3, 4, 4, 1);
        assertEquals(
            new Colony(3, Facing.WEST, true),
            AbyssalMusselGrowth.propose(p, colony, f, random)
                .orElseThrow()
                .after());
    }

    @Test
    public void flowingUnknownDryOrUnloadedTargetsCannotBecomeSourceWaterCarriers() {
        Position p = new Position(0, 64, 0);
        Colony colony = new Colony(5, Facing.UP, true);
        for (Cell target : new Cell[] { GENERIC, new Cell(false, false, new Colony(2, Facing.WEST, false)) }) {
            Fixture f = spreading(p, colony, target);
            Sequence random = new Sequence(0, 3, 4, 4);
            assertTrue(
                AbyssalMusselGrowth.propose(p, colony, f, random)
                    .isEmpty());
            assertEquals(List.of(20, 6, 6, 6), random.bounds);
        }
        Fixture f = spreading(p, colony, WATER);
        f.missing.add(p.offset(0, 1, 1));
        assertTrue(
            AbyssalMusselGrowth.propose(p, colony, f, new Sequence(0, 3, 4, 4))
                .isEmpty());
        assertFalse(f.reads.contains(p.offset(0, 1, 1)));
    }

    @Test
    public void distantExistingColonyRetainsSourceSquaredDistanceLimitNotInventedTaxicabGrowth() {
        Position p = new Position(0, 150, 0);
        Colony origin = new Colony(5, Facing.UP, true);
        Fixture f = new Fixture();
        f.colony(p, origin);
        f.cells.put(p.relative(Facing.DOWN), GROW);
        Position target = p.offset(-3, -4, -3);
        f.colony(target, new Colony(4, Facing.NORTH, true));
        f.supports.add(target.relative(Facing.DOWN));
        var growth = AbyssalMusselGrowth.propose(p, origin, f, new Sequence(0, 0, 0, 0))
            .orElseThrow();
        assertEquals(new Colony(4, Facing.NORTH, true), growth.after());
        assertEquals(target, growth.position());
    }

    @Test
    public void blockBehaviorUsesActual512HeightNotWorldgenLower64Band() {
        for (int y : new int[] { 17, 319, 511 }) {
            Position p = new Position(0, y, 0);
            Colony colony = new Colony(4, Facing.UP, true);
            Fixture f = new Fixture();
            f.colony(p, colony);
            f.cells.put(p.relative(Facing.DOWN), GROW);
            assertEquals(
                5,
                AbyssalMusselGrowth.propose(p, colony, f, new Sequence(0))
                    .orElseThrow()
                    .after()
                    .count());
        }
        Position p = new Position(0, 511, 0);
        Colony colony = new Colony(5, Facing.UP, true);
        Fixture f = spreading(p, colony, WATER);
        assertEquals(
            511,
            AbyssalMusselGrowth.propose(p, colony, f, new Sequence(0, 3, 4, 4, 0))
                .orElseThrow()
                .position()
                .y());
        assertTrue(
            f.reads.stream()
                .allMatch(q -> q.y() >= 0 && q.y() < 512));
    }

    @Test
    public void spongeTopTracksActualNeighborAndSourceSupportExcludesMagma() {
        assertEquals(0, AbyssalMusselGrowth.spongeMetadata(1, true));
        assertEquals(1, AbyssalMusselGrowth.spongeMetadata(0, false));
        assertEquals(9, AbyssalMusselGrowth.spongeMetadata(9, false)); // Unsupported state isn't silently normalized.
        assertTrue(AbyssalMusselGrowth.spongeSupport(true, false, false));
        assertTrue(AbyssalMusselGrowth.spongeSupport(false, true, false));
        assertFalse(AbyssalMusselGrowth.spongeSupport(false, true, true));
        assertFalse(AbyssalMusselGrowth.spongeSupport(false, false, false));
    }
}
