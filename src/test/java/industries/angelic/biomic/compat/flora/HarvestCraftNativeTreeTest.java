package industries.angelic.biomic.compat.flora;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.feature.WorldGenerator;

import org.junit.BeforeClass;
import org.junit.Test;

/** Executes the UNMODIFIED pinned Pam generator against synthetic bounded blocks. No Forge runtime proof. */
public class HarvestCraftNativeTreeTest {

    private static Block fruit;
    private static WorldGenerator generator;
    private static sun.misc.Unsafe unsafe;
    private static final Map<Field, Object> originalBlocks = new LinkedHashMap<>();

    @BeforeClass
    public static void setup() throws Exception {
        Class<?> tree;
        try {
            tree = Class.forName("com.pam.harvestcraft.WorldGenPamFruitTree");
        } catch (ClassNotFoundException absent) {
            org.junit.Assume.assumeTrue("Enable -PbiomicHarvestCraftAudit for the optional native-jar audit", false);
            return;
        }
        Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (sun.misc.Unsafe) f.get(null);
        // Forge registration needs LaunchWrapper. Supply real vanilla block implementations but
        // isolate their static constants; this is deliberately a synthetic fixture, not mod startup.
        String[][] constants = { { "air", "BlockAir" }, { "dirt", "BlockDirt" }, { "grass", "BlockGrass" },
            { "log", "BlockOldLog" }, { "leaves", "BlockOldLeaf" }, { "sapling", "BlockSapling" },
            { "vine", "BlockVine" } };
        for (String[] pair : constants) {
            Field constant = Blocks.class.getField(pair[0]);
            originalBlocks.put(constant, constant.get(null));
            var ctor = Class.forName("net.minecraft.block." + pair[1])
                .getDeclaredConstructor();
            ctor.setAccessible(true);
            unsafe.putObject(unsafe.staticFieldBase(constant), unsafe.staticFieldOffset(constant), ctor.newInstance());
        }
        // Fruit support methods have no dependency on initialization; avoid initializing the whole mod/creative tabs.
        fruit = (Block) unsafe.allocateInstance(Class.forName("com.pam.harvestcraft.BlockPamFruit"));
        generator = (WorldGenerator) tree.getConstructor(boolean.class, int.class, int.class, int.class, Block.class)
            .newInstance(false, 5, 0, 0, fruit);
    }

    @org.junit.AfterClass
    public static void restoreConstants() {
        if (unsafe != null) originalBlocks.forEach(
            (field, value) -> unsafe.putObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), value));
    }

    private static FixtureWorld world(int floor, boolean clipped) throws Exception {
        Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        FixtureWorld world = (FixtureWorld) ((sun.misc.Unsafe) f.get(null)).allocateInstance(FixtureWorld.class);
        world.floor = floor;
        world.clipped = clipped;
        world.committed = new LinkedHashMap<>();
        return world;
    }

    private static boolean generate(FixtureWorld world, long seed) {
        world.buffer = new LinkedHashMap<>();
        try {
            boolean result = NativeFeatureCapture.scoped(
                world,
                world.access(),
                () -> generator.generate(world, new Random(seed), 0, world.floor + 1, 0));
            if (result) world.committed.putAll(world.buffer);
            return result;
        } catch (Boundary rejection) {
            return false;
        } finally {
            world.buffer = null;
        }
    }

    @Test
    public void nativeFruitHasLeafSupportOnMainlandAndElevatedSoil() throws Exception {
        for (int floor : new int[] { 64, 180, 235 }) {
            int fruitCount = 0;
            for (long seed = 0; seed < 8; seed++) {
                FixtureWorld w = world(floor, false);
                assertTrue(generate(w, seed));
                assertEquals(Blocks.log, w.getBlock(0, floor + 1, 0));
                for (Map.Entry<P, State> entry : w.committed.entrySet()) {
                    P p = entry.getKey();
                    State state = entry.getValue();
                    if (state.block == fruit) {
                        fruitCount++;
                        assertEquals(0, state.meta);
                        assertTrue(fruit.canPlaceBlockAt(w, p.x, p.y, p.z));
                        assertTrue(fruit.canBlockStay(w, p.x, p.y, p.z));
                        assertEquals(Blocks.leaves, w.getBlock(p.x, p.y + 1, p.z));
                    }
                }
                assertTrue(w.committed.size() < 256);
            }
            assertTrue("native hanging apples at floor " + floor, fruitCount > 0);
        }
    }

    @Test
    public void boundaryCrossingRollsBackEverythingIncludingSoil() throws Exception {
        FixtureWorld w = world(64, true);
        assertFalse(generate(w, 42));
        assertTrue(w.committed.isEmpty());
        assertTrue("rejection occurred after actual buffered writes", w.writes > 0);
        assertNull(NativeFeatureCapture.current(w));
    }

    @Test
    public void heightCeilingRejectsWithoutPartialCanopy() throws Exception {
        FixtureWorld w = world(252, false);
        assertFalse(generate(w, 42));
        assertTrue(w.committed.isEmpty());
    }

    @Test
    public void nativeOutputIsDeterministic() throws Exception {
        FixtureWorld a = world(64, false), b = world(64, false);
        assertTrue(generate(a, 731));
        assertTrue(generate(b, 731));
        assertEquals(a.committed, b.committed);
    }

    @Test
    public void captureDoesNotInterceptAnotherWorld() throws Exception {
        FixtureWorld a = world(64, false), b = world(64, false);
        NativeFeatureCapture.scoped(a, a.access(), () -> {
            assertNotNull(NativeFeatureCapture.current(a));
            assertNull(NativeFeatureCapture.current(b));
            return null;
        });
    }

    private record P(int x, int y, int z) {}

    private record State(Block block, int meta) {}

    private static final class Boundary extends RuntimeException {
    }

    private static class FixtureWorld extends WorldServer {

        int floor, writes;
        boolean clipped;
        Map<P, State> buffer, committed;

        private FixtureWorld() {
            super(null, null, "unused", 0, null, new Profiler());
        }

        @Override
        public Block getBlock(int x, int y, int z) {
            var frame = NativeFeatureCapture.current(this);
            return frame != null ? frame.block(x, y, z) : state(x, y, z).block;
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            var frame = NativeFeatureCapture.current(this);
            return frame != null ? frame.metadata(x, y, z) : state(x, y, z).meta;
        }

        @Override
        public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
            var frame = NativeFeatureCapture.current(this);
            if (frame == null) throw new AssertionError("Uncaptured write");
            return frame.set(x, y, z, block, meta);
        }

        @Override
        public net.minecraft.world.chunk.Chunk getChunkFromChunkCoords(int x, int z) {
            throw new AssertionError("Native tree tried to load a chunk");
        }

        private State state(int x, int y, int z) {
            if (Math.abs(x) > 4 || Math.abs(z) > 4 || y < 0 || y >= 256)
                throw new AssertionError("Unbounded native read");
            P p = new P(x, y, z);
            if (buffer != null && buffer.containsKey(p)) return buffer.get(p);
            if (committed.containsKey(p)) return committed.get(p);
            return new State(y == floor ? Blocks.grass : Blocks.air, 0);
        }

        NativeFeatureCapture.Access access() {
            return new NativeFeatureCapture.Access() {

                @Override
                public Block block(int x, int y, int z) {
                    return state(x, y, z).block;
                }

                @Override
                public int metadata(int x, int y, int z) {
                    return state(x, y, z).meta;
                }

                @Override
                public boolean set(int x, int y, int z, Block block, int meta) {
                    if (y < 1 || y >= 256 || Math.abs(x) > 4 || Math.abs(z) > 4 || clipped && x != 0)
                        throw new Boundary();
                    writes++;
                    buffer.put(new P(x, y, z), new State(block, meta));
                    return true;
                }
            };
        }
    }
}
