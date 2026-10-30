package industries.angelic.biomic.unused.tartarus;

import static org.junit.Assert.*;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import org.junit.Test;

public class BlockAuthoredSoulFireTest {

    @Test
    public void explicitIdentityUsesFireGeometryLight10AndNoItem() {
        var block = new BlockAuthoredSoulFire();
        assertTrue(block instanceof BlockFire);
        assertEquals(Material.fire, block.getMaterial());
        assertEquals(3, block.getRenderType());
        assertEquals(10, block.getLightValue());
        assertEquals(10, block.getLightValue(null, 0, 511, 0));
        assertFalse(block.isOpaqueCube());
        assertFalse(block.renderAsNormalBlock());
        assertNull(block.getCollisionBoundingBoxFromPool(null, 0, 190, 0));
        assertTrue(block.isSoulFire(null, 0, 190, 0));
        assertNull(block.getItem(null, 0, 190, 0));
        assertNull(block.getItemDropped(0, new Random(1), 0));
        assertEquals(0, block.quantityDropped(new Random(1)));
    }

    @Test
    public void onlyKnownMissingSupportRemovesAndAllSpreadPortalEntrypointsAreOverridden() throws Exception {
        var block = new BlockAuthoredSoulFire();
        int[] removed = { 0 }, queried = { 0 };
        for (boolean known : new boolean[] { false, true }) for (boolean solid : new boolean[] { false, true }) {
            int previous = removed[0], reads = queried[0];
            block.checkSupport(new BlockAuthoredSoulFire.SupportAccess() {

                public boolean available() {
                    return known;
                }

                public boolean solidTop() {
                    queried[0]++;
                    return solid;
                }

                public void remove() {
                    removed[0]++;
                }
            });
            assertEquals(previous + (known && !solid ? 1 : 0), removed[0]);
            assertEquals(reads + (known ? 1 : 0), queried[0]);
        }
        // No entrypoint may accidentally inherit BlockFire's portal creation, aging, fuel or spread loop.
        assertNotNull(
            BlockAuthoredSoulFire.class
                .getDeclaredMethod("onBlockAdded", World.class, int.class, int.class, int.class));
        assertNotNull(
            BlockAuthoredSoulFire.class
                .getDeclaredMethod("onNeighborBlockChange", World.class, int.class, int.class, int.class, Block.class));
        assertNotNull(
            BlockAuthoredSoulFire.class
                .getDeclaredMethod("updateTick", World.class, int.class, int.class, int.class, Random.class));
    }
}
