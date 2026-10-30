package industries.angelic.biomic.content.block;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import ganymedes01.etfuturum.tileentities.TileEntityGlowLichen;

/** Source-derived native lifecycle contracts, not executed native game physics. */
public class SkylandsWetGlowLichenTest {

    @Test
    public void wetAccessorRetainsOriginalEfrTileAndNoncollidingFaceModel() {
        var block = new BlockSkylandsWetGlowLichen();
        assertSame(Material.water, block.getMaterial());
        assertEquals(3, block.getLightOpacity());
        assertTrue(block.hasTileEntity(0));
        assertEquals(
            TileEntityGlowLichen.class,
            block.createNewTileEntity(null, 0)
                .getClass());
        assertNull(block.getCollisionBoundingBoxFromPool(null, 0, 300, 0));
        assertFalse(block.isReplaceable(null, 0, 300, 0));
        assertTrue(block.canRenderInPass(0));
        assertTrue(block.canRenderInPass(1));
        assertFalse(block.canRenderInPass(2));
        assertFalse(block.canSilkHarvest());
    }

    @Test
    public void supportRemovalNeverInventsFacesAndLastFaceReturnsZeroForWaterBranch() {
        assertEquals(37, BlockSkylandsWetGlowLichen.remainingFaces(37, side -> true));
        assertEquals(36, BlockSkylandsWetGlowLichen.remainingFaces(37, side -> side != 0));
        assertEquals(0, BlockSkylandsWetGlowLichen.remainingFaces(1, side -> false));
        AtomicInteger reads = new AtomicInteger();
        assertEquals(63, BlockSkylandsWetGlowLichen.remainingFaces(63, side -> {
            reads.incrementAndGet();
            return true;
        }));
        assertEquals(6, reads.get());
        assertThrows(IllegalArgumentException.class, () -> BlockSkylandsWetGlowLichen.remainingFaces(64, side -> true));
    }

    @Test
    public void bucketsRequireOriginalFacePayloadAndDoNotFillHell() {
        assertEquals(
            SkylandsWetGlowLichenBucket.Action.DRAIN,
            SkylandsWetGlowLichenBucket.action(true, false, true, false, 63));
        assertEquals(
            SkylandsWetGlowLichenBucket.Action.FILL,
            SkylandsWetGlowLichenBucket.action(false, true, false, true, 37));
        assertEquals(
            SkylandsWetGlowLichenBucket.Action.NONE,
            SkylandsWetGlowLichenBucket.action(true, false, true, false, 0));
        assertEquals(
            SkylandsWetGlowLichenBucket.Action.NONE,
            SkylandsWetGlowLichenBucket.action(false, true, true, false, 63));
        assertFalse(SkylandsWetGlowLichenBucket.allowFill(true));
        assertTrue(SkylandsWetGlowLichenBucket.allowFill(false));
    }

    @Test
    public void bucketAttachFailureRollsBackOriginalBlockAndFullEfrPayload() {
        Block old = new Block(Material.vine) {}, wet = new Block(Material.water) {};
        var payload = new NBTTagCompound();
        payload.setInteger("State", 37);
        payload.setString("id", "etfuturum.glow_lichen");
        payload.setInteger("x", -17);
        payload.setInteger("y", 300);
        payload.setInteger("z", 31);
        var failure = new IllegalStateException("injected attach failure");
        class Cells implements SkylandsWetGlowLichenBucket.Transition {

            Block current = old;
            NBTTagCompound installed;
            int attaches;
            int notices;
            int metadata;

            public boolean replace(Block block, int meta) {
                current = block;
                metadata = meta;
                return true;
            }

            public void tile(NBTTagCompound data) {
                if (attaches++ == 0) throw failure;
                installed = data;
            }

            public void notifyChange(Block block) {
                notices++;
            }
        }
        var cells = new Cells();
        assertSame(
            failure,
            assertThrows(
                IllegalStateException.class,
                () -> SkylandsWetGlowLichenBucket.transition(old, 2, wet, payload, cells)));
        assertSame(old, cells.current);
        assertEquals(payload, cells.installed);
        assertEquals(1, cells.notices);
        assertEquals(37, payload.getInteger("State"));
        assertEquals(2, cells.metadata);
    }

    @Test
    public void bucketWriteRefusalDoesNotAttachOrReportSuccess() {
        var payload = new NBTTagCompound();
        payload.setInteger("State", 63);
        var cells = new SkylandsWetGlowLichenBucket.Transition() {

            public boolean replace(Block block, int metadata) {
                return false;
            }

            public void tile(NBTTagCompound data) {
                fail("No attach after refused write");
            }

            public void notifyChange(Block block) {
                fail("No notification after refused write");
            }
        };
        assertFalse(SkylandsWetGlowLichenBucket.transition(null, 0, null, payload, cells));
    }
}
