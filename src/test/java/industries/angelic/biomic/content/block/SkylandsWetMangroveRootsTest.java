package industries.angelic.biomic.content.block;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.block.material.Material;

import org.junit.Test;

import industries.angelic.biomic.client.render.AquaticRenderSemantics;

/** Native block/transition contracts, not a live fluid-flow, renderer or save/reload proof. */
public class SkylandsWetMangroveRootsTest {

    @Test
    public void wetRootRetainsSourceMaterialAndFullCollisionWithoutTiles() {
        var root = new BlockSkylandsWetMangroveRoots();
        assertSame(Material.water, root.getMaterial());
        assertEquals(3, root.getLightOpacity());
        var box = root.getCollisionBoundingBoxFromPool(null, 2, 3, 4);
        assertEquals(2D, box.minX, 0D);
        assertEquals(3D, box.minY, 0D);
        assertEquals(4D, box.minZ, 0D);
        assertEquals(3D, box.maxX, 0D);
        assertEquals(4D, box.maxY, 0D);
        assertEquals(5D, box.maxZ, 0D);
        assertFalse(root.isReplaceable(null, 2, 3, 4));
        assertFalse(root.getBlocksMovement(null, 2, 3, 4));
        assertFalse(root.hasTileEntity(0));
        assertFalse(root.isOpaqueCube());
    }

    @Test
    public void rootGeometryAndSourceWaterUseSeparateNativeMaterials() {
        var root = new BlockSkylandsWetMangroveRoots();
        assertTrue(root.canRenderInPass(0));
        assertTrue(root.canRenderInPass(1));
        assertFalse(root.canRenderInPass(2));
        assertSame(Material.wood, AquaticRenderSemantics.material(root, root.getMaterial(), 0));
        assertSame(Material.water, AquaticRenderSemantics.material(root, root.getMaterial(), 1));
    }

    @Test
    public void harvestRestoresWaterOnceAndPreservesWriteFailure() {
        AtomicInteger writes = new AtomicInteger();
        assertTrue(BlockSkylandsWetMangroveRoots.restoreAfterHarvest(() -> {
            writes.incrementAndGet();
            return true;
        }));
        assertEquals(1, writes.get());
        assertFalse(BlockSkylandsWetMangroveRoots.restoreAfterHarvest(() -> false));
    }

    @Test
    public void bucketTransitionsAreOnlyExactSourceWetAndDryRoots() {
        assertEquals(
            SkylandsWetMangroveBucket.Action.DRAIN,
            SkylandsWetMangroveBucket.action(true, false, true, false, 0));
        assertEquals(
            SkylandsWetMangroveBucket.Action.FILL,
            SkylandsWetMangroveBucket.action(false, true, false, true, 0));
        assertEquals(
            SkylandsWetMangroveBucket.Action.NONE,
            SkylandsWetMangroveBucket.action(false, false, false, true, 0));
        assertEquals(
            SkylandsWetMangroveBucket.Action.NONE,
            SkylandsWetMangroveBucket.action(true, false, true, false, 1));
        assertEquals(
            SkylandsWetMangroveBucket.Action.NONE,
            SkylandsWetMangroveBucket.action(false, true, true, false, 0));
        assertEquals(
            SkylandsWetMangroveBucket.Action.NONE,
            SkylandsWetMangroveBucket.action(true, false, false, true, 0));
    }

    @Test
    public void filledBucketDestinationGuardAndHellFillAreClosed() {
        assertFalse(SkylandsWetMangroveBucket.allowFill(true));
        assertTrue(SkylandsWetMangroveBucket.allowFill(false));
    }

    @Test
    public void silkTouchNeverObtainsPersistedFluidItem() {
        assertFalse(new BlockSkylandsWetMangroveRoots().canSilkHarvest());
    }
}
