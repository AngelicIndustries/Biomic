package industries.angelic.biomic.api.worldbuilder;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

import org.junit.Test;

/**
 * Executes ORIGINAL native makePortal against a recording resident-only world; no real native lighting/latency proof.
 */
public class DefaultPortalResidentCreationBoundsTest {

    private static <T> T empty(Class<T> type) throws Exception {
        Class<?> u = Class.forName("sun.misc.Unsafe");
        Field f = u.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return type.cast(
            u.getMethod("allocateInstance", Class.class)
                .invoke(f.get(null), type));
    }

    private static final Block ROCK = new Block(Material.rock) {}, AIR = new Block(Material.air) {};

    private static final class Draw extends Random {

        final int draw;
        int calls;

        Draw(int draw) {
            this.draw = draw;
        }

        @Override
        public int nextInt(int bound) {
            assertEquals(4, bound);
            calls++;
            return draw;
        }
    }

    private static final class RecordingWorld extends WorldServer {

        int x, z, reads, writes, notifies, maxRead, maxWrite;
        boolean valid;
        StringBuilder events;

        private RecordingWorld() {
            super(null, null, "unused", 0, null, new Profiler());
        }

        @Override
        public long getSeed() {
            return 0L;
        }

        @Override
        public int getActualHeight() {
            return 128;
        }

        private void record(int bx, int bz, boolean write) {
            int delta = Math.max(Math.abs(bx - x), Math.abs(bz - z));
            assertTrue("outside completed existing-search footprint", delta <= 128);
            if (write) maxWrite = Math.max(maxWrite, delta);
            else {
                reads++;
                maxRead = Math.max(maxRead, delta);
            }
        }

        @Override
        public boolean isAirBlock(int bx, int y, int bz) {
            record(bx, bz, false);
            return valid && y >= 2;
        }

        @Override
        public Block getBlock(int bx, int y, int bz) {
            record(bx, bz, false);
            return y < 2 ? ROCK : AIR;
        }

        @Override
        public boolean setBlock(int bx, int y, int bz, Block block) {
            record(bx, bz, true);
            writes++;
            events.append('W');
            return true;
        }

        @Override
        public boolean setBlock(int bx, int y, int bz, Block block, int metadata, int flags) {
            record(bx, bz, true);
            assertEquals(0, metadata);
            assertEquals(2, flags);
            writes++;
            events.append('W');
            return true;
        }

        @Override
        public void notifyBlocksOfNeighborChange(int bx, int y, int bz, Block block) {
            record(bx, bz, true);
            notifies++;
            events.append('N');
        }

        @Override
        public net.minecraft.world.chunk.Chunk getChunkFromChunkCoords(int x, int z) {
            throw new AssertionError("native creation must not introduce an explicit load in the fixture");
        }
    }

    private static void run(boolean valid, int draw, int x, int z) throws Exception {
        RecordingWorld w = empty(RecordingWorld.class);
        w.valid = valid;
        w.x = x;
        w.z = z;
        w.events = new StringBuilder();
        EntityPlayerMP entity = empty(EntityPlayerMP.class);
        entity.posX = x + .25;
        entity.posY = 80;
        entity.posZ = z + .75;
        Teleporter portal = new Teleporter(w);
        Draw random = new Draw(draw);
        Field f = Teleporter.class.getDeclaredField("random");
        f.setAccessible(true);
        f.set(portal, random);
        assertTrue(portal.makePortal(entity));
        assertEquals(1, random.calls);
        assertTrue(w.reads > 0);
        assertTrue(w.maxRead <= 18);
        assertTrue(w.maxWrite <= 18);
        assertEquals(valid ? 80 : 104, w.writes);
        assertEquals(80, w.notifies);
        StringBuilder expected = new StringBuilder();
        if (!valid) for (int i = 0; i < 24; i++) expected.append('W');
        for (int pass = 0; pass < 4; pass++) {
            for (int i = 0; i < 20; i++) expected.append('W');
            for (int i = 0; i < 20; i++) expected.append('N');
        }
        assertEquals(expected.toString(), w.events.toString());
        // Direct notification targets are inside±18; vanilla neighbor dispatch adds at most one block.
        assertTrue(w.maxWrite + 1 <= 19);
    }

    @Test
    public void originalFourRngOrientationsAndSuccessfulGeometryStayInsideResidentSearch() throws Exception {
        for (int draw = 0; draw < 4; draw++) run(true, draw, -17, 31);
    }

    @Test
    public void originalNoGeometryFallbackAndRepeatedWriteNotifyOrderRemainNative() throws Exception {
        for (int draw = 0; draw < 4; draw++) run(false, draw, 16, -32);
    }
}
