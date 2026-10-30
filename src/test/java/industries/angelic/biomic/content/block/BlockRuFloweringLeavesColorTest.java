package industries.angelic.biomic.content.block;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import industries.angelic.biomic.api.BiomeColorAccess;
import sun.misc.Unsafe;

public class BlockRuFloweringLeavesColorTest {

    private static BiomeGenBase colorBiome() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (BiomeGenBase) ((Unsafe) f.get(null)).allocateInstance(ColorBiome.class);
    }

    @Test
    public void oakSmoothingUsesAllNineActualUpperBiomeIdentitiesAtTheLeafHeight() throws Exception {
        var access = new Access(colorBiome(), true);
        assertEquals(0x11C3E9, BlockRuFloweringLeaves.foliageColor(access, 17, 451, -23));
        assertEquals(9, access.snapshots.size());
        assertEquals(0, access.groundLookups);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) assertTrue(access.snapshots.contains((17 + dx) + ",451," + (-23 + dz)));
        assertEquals(0x1189E9, BlockRuFloweringLeaves.foliageColor(access, 17, 137, -23));
    }

    @Test
    public void missingHostedIdentityRetainsOrdinaryNineColumnFallback() throws Exception {
        var access = new Access(colorBiome(), false);
        assertEquals(0x1140E9, BlockRuFloweringLeaves.foliageColor(access, 17, 64, -23));
        assertEquals(9, access.snapshots.size());
        assertEquals(9, access.groundLookups);
    }

    @Test
    public void ordinaryLeavesAndBothKapokVinePartsShareOakSmoothingAtMainlandAndUpperHeights() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        var leaves = (BlockRuLeaves) unsafe.allocateInstance(BlockRuLeaves.class);
        Field leafMode = BlockRuLeaves.class.getDeclaredField("tintMode");
        leafMode.setAccessible(true);
        leafMode.set(leaves, BlockRuLeaves.TintMode.BIOME_FOLIAGE);
        for (int height : new int[] { 137, 451 }) {
            for (int part = 0; part < 2; part++) {
                var vine = (BlockRuPlant) unsafe.allocateInstance(BlockRuPlant.class);
                Field vineMode = BlockRuPlant.class.getDeclaredField("tintMode");
                vineMode.setAccessible(true);
                vineMode.set(vine, BlockRuPlant.TintMode.BIOME_FOLIAGE);
                var leafAccess = new Access(colorBiome(), true);
                var vineAccess = new Access(colorBiome(), true);
                int expected = BlockRuFloweringLeaves.foliageColor(new Access(colorBiome(), true), 17, height, -23);
                assertEquals(expected, leaves.colorMultiplier(leafAccess, 17, height, -23));
                assertEquals(expected, vine.colorMultiplier(vineAccess, 17, height, -23));
                assertEquals(9, leafAccess.snapshots.size());
                assertEquals(9, vineAccess.snapshots.size());
                assertEquals(0, leafAccess.groundLookups);
                assertEquals(0, vineAccess.groundLookups);
            }
        }
    }

    @Test
    public void untintedSourceMossNeverInheritsFoliageOrGroundColor() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var moss = (BlockRuPlant) ((Unsafe) unsafeField.get(null)).allocateInstance(BlockRuPlant.class);
        Field mode = BlockRuPlant.class.getDeclaredField("tintMode");
        mode.setAccessible(true);
        mode.set(moss, BlockRuPlant.TintMode.CONSTANT);
        Field color = BlockRuPlant.class.getDeclaredField("color");
        color.setAccessible(true);
        color.setInt(moss, 0xFFFFFF);
        var access = new Access(colorBiome(), true);
        assertEquals(0xFFFFFF, moss.colorMultiplier(access, 17, 451, -23));
        assertEquals(0, access.snapshots.size());
        assertEquals(0, access.groundLookups);
    }

    @Test
    public void palmUsesActualHostedHeightAndTheSameFoliageAverageAsItsVines() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var palm = (BlockPalmLeaves) ((Unsafe) unsafeField.get(null)).allocateInstance(BlockPalmLeaves.class);
        for (int height : new int[] { 137, 451 }) {
            var access = new Access(colorBiome(), true);
            assertEquals(
                BlockRuLeaves.foliageColor(new Access(colorBiome(), true), 17, height, -23),
                palm.colorMultiplier(access, 17, height, -23));
            assertEquals(9, access.snapshots.size());
            assertEquals(0, access.groundLookups);
        }
    }

    @Test
    public void palmKeepsItsExistingNoBiomeFallback() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var palm = (BlockPalmLeaves) ((Unsafe) unsafeField.get(null)).allocateInstance(BlockPalmLeaves.class);
        assertEquals(0x77B14D, palm.colorMultiplier(new Access(null, false), 17, 451, -23));
    }

    @Test
    public void everyLeafMetadataUsesExactNativeOakFancyOrFastResourcePackIcon() {
        var oak = new net.minecraft.block.BlockOldLeaf();
        var icons = new java.util.HashMap<String, net.minecraft.util.IIcon>();
        oak.registerBlockIcons(name -> {
            var icon = (net.minecraft.util.IIcon) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { net.minecraft.util.IIcon.class },
                (proxy, method, args) -> method.getName()
                    .equals("getIconName") ? name : null);
            icons.put(name, icon);
            return icon;
        });
        for (boolean fancy : new boolean[] { true, false }) {
            oak.setGraphicsLevel(fancy);
            var expected = icons.get(fancy ? "leaves_oak" : "leaves_oak_opaque");
            assertNotNull(expected);
            for (int side = 0; side < 6; side++) for (int metadata = 0; metadata < 16; metadata++)
                assertSame(expected, BlockRuFloweringLeaves.oakIcon(oak, side));
        }
    }

    private static final class ColorBiome extends BiomeGenBase {

        private ColorBiome() {
            super(255);
        }

        @Override
        public int getBiomeFoliageColor(int x, int y, int z) {
            return ((x & 255) << 16) | ((y & 255) << 8) | (z & 255);
        }
    }

    private static final class Access implements IBlockAccess, BiomeColorAccess {

        final BiomeGenBase biome;
        final boolean hosted;
        final Set<String> snapshots = new HashSet<>();
        int groundLookups;

        Access(BiomeGenBase biome, boolean hosted) {
            this.biome = biome;
            this.hosted = hosted;
        }

        @Override
        public BiomeGenBase biomic$colorBiome(int x, int y, int z) {
            snapshots.add(x + "," + y + "," + z);
            return hosted ? biome : null;
        }

        @Override
        public BiomeGenBase getBiomeGenForCoords(int x, int z) {
            groundLookups++;
            return biome;
        }

        @Override
        public Block getBlock(int x, int y, int z) {
            return null;
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            return null;
        }

        @Override
        public int getLightBrightnessForSkyBlocks(int x, int y, int z, int light) {
            return 0;
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            return 0;
        }

        @Override
        public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
            return 0;
        }

        @Override
        public boolean isAirBlock(int x, int y, int z) {
            return true;
        }

        @Override
        public int getHeight() {
            return 512;
        }

        @Override
        public boolean extendedLevelsInChunkCache() {
            return true;
        }

        @Override
        public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean fallback) {
            return fallback;
        }
    }
}
