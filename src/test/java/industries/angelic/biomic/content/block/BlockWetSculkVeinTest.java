package industries.angelic.biomic.content.block;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.block.material.Material;

import org.junit.Test;

import industries.angelic.biomic.client.render.AquaticRenderSemantics;
import industries.angelic.biomic.content.BiomicSculkBlocks;
import industries.angelic.biomic.content.BiomicWetSculkBlocks;

public class BlockWetSculkVeinTest {

    @Test
    public void waterAndCutoutPassesUseSameNativeGeometryWithoutChangingDryStates() {
        for (boolean up : new boolean[] { false, true }) for (boolean down : new boolean[] { false, true }) {
            var block = BiomicWetSculkBlocks.wetVein(up, down);
            var dry = BiomicSculkBlocks.vein(up, down);
            assertSame(Material.water, block.getMaterial());
            assertSame(Material.vine, dry.getMaterial());
            assertEquals(3, block.getLightOpacity());
            assertFalse(block.isOpaqueCube());
            assertFalse(block.renderAsNormalBlock());
            assertNull(block.getCollisionBoundingBoxFromPool(null, 0, 400, 0));
            assertTrue(block.getBlocksMovement(null, 0, 400, 0));
            assertTrue(block.canRenderInPass(0));
            assertTrue(block.canRenderInPass(1));
            assertFalse(block.canRenderInPass(2));
            assertEquals(dry.getRenderType(), block.getRenderType());
            assertSame(Material.plants, AquaticRenderSemantics.material(block, block.getMaterial(), 0));
            assertSame(Material.water, AquaticRenderSemantics.material(block, block.getMaterial(), 1));
            assertNull(block.getItemDropped(15, new java.util.Random(1), 3));
            assertFalse(block.canSilkHarvest());
            for (int metadata = 0; metadata < 16; metadata++) assertFalse(block.hasTileEntity(metadata));
            assertSame(dry, BiomicWetSculkBlocks.dryCounterpart(block));
            assertFalse(BiomicWetSculkBlocks.wet(dry));
            assertFalse(BiomicWetSculkBlocks.dry(block));
        }
    }

    @Test
    public void nativeSupportPrunesOnlyObservedMissingFacesAndDefersUnloadedNeighbors() {
        for (int faces = 1; faces < 64; faces++) {
            final int original = faces;
            assertEquals(faces, BlockWetSculkVein.survivingFaces(faces, s -> true, s -> true));
            assertEquals(0, BlockWetSculkVein.survivingFaces(faces, s -> true, s -> false));
            for (int side = 0; side < 6; side++) if ((faces & (1 << side)) != 0) {
                final int missing = side;
                assertEquals(
                    faces & ~(1 << side),
                    BlockWetSculkVein.survivingFaces(faces, s -> true, s -> s != missing));
                AtomicInteger supportReads = new AtomicInteger();
                assertEquals(original, BlockWetSculkVein.survivingFaces(faces, s -> s != missing, s -> {
                    supportReads.incrementAndGet();
                    return false;
                }));
                assertEquals(0, supportReads.get());
            }
        }
    }

    @Test
    public void bucketConversionsPreserveAllFacesAndCannotConsumeAnUnprovedSource() {
        for (int faces = 1; faces < 64; faces++) {
            int metadata = BlockSculkVein.horizontalMetadata(faces);
            assertEquals(
                WetSculkVeinBucket.Action.DRAIN,
                WetSculkVeinBucket.action(true, false, true, false, metadata, faces));
            assertEquals(
                WetSculkVeinBucket.Action.FILL,
                WetSculkVeinBucket.action(false, true, false, true, metadata, faces));
            assertEquals(
                WetSculkVeinBucket.Action.NONE,
                WetSculkVeinBucket.action(true, false, false, true, metadata, faces));
            assertEquals(
                WetSculkVeinBucket.Action.NONE,
                WetSculkVeinBucket.action(false, true, true, false, metadata, faces));
        }
        for (int metadata : new int[] { -1, 16 }) assertEquals(
            WetSculkVeinBucket.Action.NONE,
            WetSculkVeinBucket.action(true, false, true, false, metadata, 63));
        assertEquals(WetSculkVeinBucket.Action.NONE, WetSculkVeinBucket.action(true, false, true, false, 0, 0));
        assertFalse(WetSculkVeinBucket.allowFill(true));
        assertTrue(WetSculkVeinBucket.allowFill(false));
    }
}
