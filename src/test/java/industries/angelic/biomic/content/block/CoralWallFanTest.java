package industries.angelic.biomic.content.block;

import static org.junit.Assert.*;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import industries.angelic.biomic.client.render.CoralWallFanModel;

public class CoralWallFanTest {

    @Test
    public void facingDeadBitsAndWaterMaterialRemainIndependent() {
        for (int species = 0; species < 5; species++) {
            var dry = new BlockCoralWallFan(species, false);
            var wet = new BlockCoralWallFan(species, true);
            assertEquals(Material.plants, dry.getMaterial());
            assertEquals(Material.water, wet.getMaterial());
            assertFalse(dry.containsSourceWater());
            assertTrue(wet.containsSourceWater());
            for (int meta = 0; meta < 8; meta++) {
                assertTrue(BlockCoralWallFan.validMetadata(meta));
                assertEquals(meta & 3, BlockCoralWallFan.facingMetadata(BlockCoralWallFan.facing(meta)));
                assertEquals(meta | 4, BlockCoralWallFan.deadMetadata(meta));
                assertEquals(species + ((meta & 4) != 0 ? 5 : 0), wet.damageDropped(meta));
                assertEquals(dry.damageDropped(meta), wet.damageDropped(meta));
            }
        }
        assertFalse(BlockCoralWallFan.validMetadata(-1));
        assertFalse(BlockCoralWallFan.validMetadata(8));
        assertThrows(IllegalArgumentException.class, () -> BlockCoralWallFan.facingMetadata(ForgeDirection.UP));
    }

    @Test
    public void deathDelayIsExactlyOriginalSixtyPlusOneFortyBoundedDraw() {
        var actual = new Random(42);
        var expected = new Random(42);
        for (int i = 0; i < 100; i++) assertEquals(60 + expected.nextInt(40), BlockCoralWallFan.deathDelay(actual));
        assertEquals(expected.nextLong(), actual.nextLong());
    }

    @Test
    public void bucketActionsPreserveEveryFacingAndDeadBitWithoutCreatingWaterFromDryDrain() {
        for (int metadata = 0; metadata < 8; metadata++) {
            assertEquals(CoralWallFanBucket.Action.DRAIN, CoralWallFanBucket.action(true, false, true, metadata));
            assertEquals(CoralWallFanBucket.Action.FILL, CoralWallFanBucket.action(false, true, false, metadata));
            assertEquals(CoralWallFanBucket.Action.NONE, CoralWallFanBucket.action(true, false, false, metadata));
            assertEquals(CoralWallFanBucket.Action.NONE, CoralWallFanBucket.action(false, true, true, metadata));
            // Cell transitions choose the other singleton and carry exactly the original metadata.
            assertEquals(metadata & 3, BlockCoralWallFan.facingMetadata(BlockCoralWallFan.facing(metadata)));
        }
        assertEquals(CoralWallFanBucket.Action.NONE, CoralWallFanBucket.action(true, false, true, 8));
    }

    @Test
    public void wallMeshUsesOfficialRescaledPlanesAndFacingRotation() {
        double slope = Math.tan(Math.toRadians(22.5));
        assertArrayEquals(new double[] { 0, (8 + 14 * slope) / 16, 0 }, CoralWallFanModel.vertex(0, 0, 0), 1e-12);
        assertArrayEquals(new double[] { 0, (8 - 14 * slope) / 16, 0 }, CoralWallFanModel.vertex(0, 1, 0), 1e-12);
        for (int plane = 0; plane < 2; plane++) for (int vertex = 0; vertex < 4; vertex++) {
            var north = CoralWallFanModel.vertex(0, plane, vertex);
            var east = CoralWallFanModel.vertex(1, plane, vertex);
            assertArrayEquals(new double[] { 1 - north[2], north[1], north[0] }, east, 1e-12);
            var south = CoralWallFanModel.vertex(2, plane, vertex);
            assertArrayEquals(new double[] { 1 - north[0], north[1], 1 - north[2] }, south, 1e-12);
        }
        assertThrows(IllegalArgumentException.class, () -> CoralWallFanModel.vertex(4, 0, 0));
    }
}
