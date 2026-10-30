package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

public class PhysicalLichenSupportTest {

    private static final Block AIR = new Block(Material.air) {};
    private static final Block ROCK = new Block(Material.rock) {};

    @Test
    public void intersectsEveryRequestedFaceWithItsActualPhysicalSupport() {
        for (int requested = 0; requested < 64; requested++) for (int support = 0; support < 64; support++) {
            final int mask = support;
            int actual = PhysicalLichenSupport.supported(requested, 17, 80, -16, 256, (x, y, z) -> {
                int direction = -1;
                for (var d : ForgeDirection.VALID_DIRECTIONS)
                    if (x == 17 + d.offsetX && y == 80 + d.offsetY && z == -16 + d.offsetZ) direction = d.ordinal();
                return new PhysicalLichenSupport.Cell(direction >= 0 && (mask & (1 << direction)) != 0 ? ROCK : AIR, 0);
            });
            assertEquals(requested & support, actual);
        }
    }

    @Test
    public void usesActualMetadataCoordinatesAndOppositeSupportFace() {
        Block support = new Block(Material.rock) {

            @Override
            public boolean isSideSolid(IBlockAccess view, int x, int y, int z, ForgeDirection face) {
                return x == -33 && y == 299
                    && z == 32
                    && face == ForgeDirection.UP
                    && view.getBlockMetadata(x, y, z) == 7;
            }
        };
        for (int meta : new int[] { 0, 7, 8 }) assertEquals(
            meta == 7 ? 1 : 0,
            PhysicalLichenSupport
                .supported(63, -33, 300, 32, 512, (x, y, z) -> new PhysicalLichenSupport.Cell(support, meta)));
        assertEquals(
            62,
            PhysicalLichenSupport.supported(63, 0, 0, 0, 256, (x, y, z) -> new PhysicalLichenSupport.Cell(ROCK, 0)));
        assertEquals(
            61,
            PhysicalLichenSupport.supported(63, 0, 255, 0, 256, (x, y, z) -> new PhysicalLichenSupport.Cell(ROCK, 0)));
    }

    @Test
    public void refusesUnboundedOrUnavailableSupportDependencies() {
        for (int mode = 0; mode < 4; mode++) {
            final int request = mode;
            Block unknown = new Block(Material.rock) {

                @Override
                public boolean isSideSolid(IBlockAccess view, int x, int y, int z, ForgeDirection face) {
                    if (request == 0) view.getTileEntity(x, y, z);
                    if (request == 1) view.getBlock(x + 3, y, z);
                    if (request == 2) view.getLightBrightnessForSkyBlocks(x, y, z, 0);
                    if (request == 3) for (int n = 0; n < 200; n++) view.getBlockMetadata(x, y, z);
                    return true;
                }
            };
            assertEquals(
                0,
                PhysicalLichenSupport
                    .supported(1, 0, 80, 0, 256, (x, y, z) -> new PhysicalLichenSupport.Cell(unknown, 0)));
        }
    }

    @Test
    public void rejectsInvalidMasksAndRetainsUnrelatedStateProperties() {
        for (int faces : new int[] { -1, 64 }) assertThrows(
            IllegalArgumentException.class,
            () -> PhysicalLichenSupport.supported(faces, 0, 80, 0, 256, null));
        assertThrows(IllegalArgumentException.class, () -> PhysicalLichenSupport.state(0, Map.of()));
        String state = PhysicalLichenSupport.state(5, Map.of("waterlogged", "true", "north", "false"));
        assertTrue(state.contains("waterlogged=true"));
        assertTrue(state.contains("north=true"));
        assertTrue(state.contains("down=true"));
        assertTrue(state.contains("up=false"));
        assertEquals(5, PhysicalLichenSupport.faces(Map.of("down", "true", "north", "true")));
    }
}
