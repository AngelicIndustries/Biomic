package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class IslandUndersideTest {

    /** A flat round island (radius 20, deck 150, 30 deep at the middle) over sea or land; records every write. */
    private static final class World implements IslandUnderside.Cells {

        final boolean sea;
        final IslandKind kind;
        final Map<Long, String> writes = new HashMap<>();
        int chunkX, chunkZ;

        World(boolean sea, IslandKind kind) {
            this.sea = sea;
            this.kind = kind;
        }

        @Override
        public int span(int x, int z) {
            double distance = Math.sqrt(x * x + z * z);
            if (distance > 20) return -1;
            int bottom = 150 - Math.max(3, (int) (30 - 1.4 * distance));
            return bottom << 16 | 150;
        }

        @Override
        public IslandKind kind(int x, int z, int top) {
            return span(x, z) < 0 ? null : kind;
        }

        @Override
        public boolean air(int x, int y, int z) {
            int span = span(x, z);
            if (span >= 0 && y >= span >>> 16 && y <= (span & 0xFFFF)) return false;
            return !writes.containsKey(key(x, y, z)) && raw(x, y, z) == BiomicTerrainSampler.AIR;
        }

        @Override
        public boolean soil(int x, int y, int z) {
            int span = span(x, z);
            return span >= 0 && y == (span & 0xFFFF);
        }

        @Override
        public byte raw(int x, int y, int z) {
            if (y <= (sea ? 50 : 70)) return BiomicTerrainSampler.STONE;
            return sea && y <= 63 ? BiomicTerrainSampler.WATER : BiomicTerrainSampler.AIR;
        }

        @Override
        public boolean guarded(int x, int y, int z, boolean wide) {
            return false;
        }

        @Override
        public void set(int x, int y, int z, String state) {
            assertEquals("writes stay in the populated chunk", chunkX, x >> 4);
            assertEquals("writes stay in the populated chunk", chunkZ, z >> 4);
            assertNull("one write per cell", writes.put(key(x, y, z), state));
        }

        void decorate(long seed) {
            for (chunkX = -2; chunkX <= 1; chunkX++)
                for (chunkZ = -2; chunkZ <= 1; chunkZ++) IslandUnderside.decorate(seed, chunkX, chunkZ, this);
        }

        static long key(int x, int y, int z) {
            return ((long) x & 0xFFFFF) << 40 | ((long) y & 0xFFFFF) << 20 | (z & 0xFFFFF);
        }

        long count(String prefix) {
            return writes.values()
                .stream()
                .filter(state -> state.startsWith(prefix))
                .count();
        }
    }

    @Test
    public void waterfallsFallIntoTheSeaAndNeverOntoLand() {
        int falls = 0;
        for (long seed = 0; seed < 60; seed++) {
            World land = new World(false, IslandKind.GREEN);
            land.decorate(seed);
            assertEquals(0, land.count("minecraft:water"));
            World sea = new World(true, IslandKind.GREEN);
            sea.decorate(seed);
            for (Map.Entry<Long, String> write : sea.writes.entrySet()) {
                if (!write.getValue()
                    .equals("minecraft:water[level=9]")) continue;
                int y = (int) (write.getKey() >>> 20 & 0xFFFFF);
                if (y != 64) continue;
                falls++;
            }
            long springs = sea.count("minecraft:water[level=0]");
            // Falls are two or three wide: every spring has another spring beside it.
            for (Map.Entry<Long, String> write : sea.writes.entrySet()) {
                if (!write.getValue()
                    .equals("minecraft:water[level=0]")) continue;
                long k = write.getKey();
                int x = (int) (k >>> 40), y = (int) (k >>> 20 & 0xFFFFF), z = (int) (k & 0xFFFFF);
                boolean paired = false;
                for (int[] d : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } })
                    paired |= "minecraft:water[level=0]".equals(sea.writes.get(World.key(x + d[0], y, z + d[1])));
                assertTrue("a waterfall is wider than one block", paired);
            }
            assertEquals(springs, sea.count("minecraft:water[level=1]"));
            // Each spring falls all the way: 150 - 1 down to 64.
            assertEquals(springs * (149 - 64 + 1), sea.count("minecraft:water[level=9]"));
        }
        assertTrue("some waterfalls over 60 seeds", falls > 0);
    }

    @Test
    public void dryIslandsHangDripstoneButNoVinesRootsOrWater() {
        World dry = new World(true, IslandKind.DRY);
        for (long seed = 0; seed < 20; seed++) dry.decorate(seed);
        assertEquals(0, dry.count("minecraft:vine"));
        assertEquals(0, dry.count("minecraft:hanging_roots"));
        assertEquals(0, dry.count("minecraft:water"));
    }

    @Test
    public void greenIslandsHangVinesAndRootsInModeration() {
        World green = new World(true, IslandKind.GREEN);
        green.decorate(7);
        assertTrue(green.count("minecraft:vine") > 0);
        assertTrue(green.count("minecraft:hanging_roots") > 0);
        assertEquals(0, green.count("minecraft:glow_lichen"));
        assertEquals(0, green.count("minecraft:pointed_dripstone"));
        assertTrue("about 2-8 per strand on 12% of ~125 coast columns", green.count("minecraft:vine") < 400);
    }

    @Test
    public void sameSeedSameUnderside() {
        World first = new World(true, IslandKind.LUSH), second = new World(true, IslandKind.LUSH);
        first.decorate(3);
        second.decorate(3);
        assertEquals(first.writes, second.writes);
    }
}
