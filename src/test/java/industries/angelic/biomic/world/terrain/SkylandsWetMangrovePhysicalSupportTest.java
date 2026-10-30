package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;

import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import industries.angelic.biomic.content.block.BlockSkylandsWetMangroveRoots;

/** Full native roots are sturdy wet attachments, not a blanket Material.water support exception. */
public class SkylandsWetMangrovePhysicalSupportTest {

    @Test
    public void allSixPhysicalFacesRemainSupportedWithoutLoadingOrChangingFluidGeometry() {
        var block = new BlockSkylandsWetMangroveRoots();
        assertSame(Material.water, block.getMaterial());
        assertFalse(block.isOpaqueCube());
        assertFalse(block.renderAsNormalBlock());
        assertEquals(63, PhysicalLichenSupport.supported(63, -17, 200, 31, 512, (x, y, z) -> {
            assertEquals(1, Math.abs(x + 17) + Math.abs(y - 200) + Math.abs(z - 31));
            return new PhysicalLichenSupport.Cell(block, 0);
        }));
        var box = block.getCollisionBoundingBoxFromPool(null, -17, 200, 31);
        assertEquals(1D, box.maxX - box.minX, 0D);
        assertEquals(1D, box.maxY - box.minY, 0D);
        assertEquals(1D, box.maxZ - box.minZ, 0D);
    }

    @Test
    public void unknownMetadataAndDirectionsCannotSupplyAttachmentAuthority() {
        var block = new BlockSkylandsWetMangroveRoots();
        for (int meta = -1; meta <= 15; meta++) {
            final int state = meta;
            var view = (IBlockAccess) Proxy.newProxyInstance(
                IBlockAccess.class.getClassLoader(),
                new Class<?>[] { IBlockAccess.class },
                (proxy, method, args) -> {
                    if (method.getName()
                        .equals("getBlockMetadata")) return state;
                    throw new AssertionError(
                        "Only the observed same-cell metadata may be queried: " + method.getName());
                });
            for (var side : ForgeDirection.VALID_DIRECTIONS)
                assertEquals(meta == 0, block.isSideSolid(view, -17, 200, 31, side));
            assertFalse(block.isSideSolid(view, -17, 200, 31, ForgeDirection.UNKNOWN));
            assertFalse(block.isSideSolid(view, -17, 200, 31, null));
            if (meta != 0) assertEquals(
                0,
                PhysicalLichenSupport
                    .supported(63, 0, 511, 0, 512, (x, y, z) -> new PhysicalLichenSupport.Cell(block, state)));
        }
        assertFalse(block.isSideSolid(null, 0, 0, 0, ForgeDirection.UP));
    }
}
