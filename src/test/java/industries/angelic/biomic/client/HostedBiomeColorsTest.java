package industries.angelic.biomic.client;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.biomic.api.BiomeColorAccess;

public class HostedBiomeColorsTest {

    @Test
    public void angelicaCustomColorCallbackDoesNotReuseGroundOrPriorWorldAtTheSameXZ() throws Exception {
        var method = industries.angelic.biomic.mixins.late.angelica.MixinMCPatcherBiomeColors.class.getDeclaredMethod(
            "biomic$resolve",
            IBlockAccess.class,
            int.class,
            int.class,
            int.class,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable.class);
        method.setAccessible(true);
        for (int height : new int[] { 137, 451, 137 }) {
            BiomeGenBase expected = height == 451 ? BiomeGenBase.jungle : BiomeGenBase.swampland;
            var access = new ColorAccess(BiomeGenBase.taiga, expected);
            var callback = new org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<BiomeGenBase>(
                "getBiomeGenAt",
                true);
            method.invoke(null, access, 17, height, -23, callback);
            Assert.assertTrue(callback.isCancelled());
            Assert.assertSame(expected, callback.getReturnValue());
            Assert.assertEquals(height, access.snapshotY);
            Assert.assertEquals(0, access.groundLookups);
        }
    }

    @Test
    public void fullHeightHostedPresetsUseTheSavedYIdentity() {
        Assert.assertTrue(HostedBiomeColors.enabledType("biomic_blend"));
        for (String type : new String[] { null, "default", "biomic_origen", "biomic_og_tall", "biomic_tartarus" })
            Assert.assertFalse(String.valueOf(type), HostedBiomeColors.enabledType(type));
    }

    @Test
    public void yAwareSnapshotWinsOverTwoDimensionalGroundBiome() {
        ColorAccess access = new ColorAccess(BiomeGenBase.taiga, BiomeGenBase.jungle);
        Assert.assertSame(BiomeGenBase.jungle, HostedBiomeColors.biome(access, 17, 451, -23));
        Assert.assertEquals(17, access.snapshotX);
        Assert.assertEquals(451, access.snapshotY);
        Assert.assertEquals(-23, access.snapshotZ);
        Assert.assertEquals(0, access.groundLookups);
    }

    @Test
    public void unknownSnapshotDelegatesToUnchangedTwoDimensionalFallback() {
        ColorAccess access = new ColorAccess(BiomeGenBase.taiga, null);
        Assert.assertSame(BiomeGenBase.taiga, HostedBiomeColors.biome(access, 17, 451, -23));
        Assert.assertEquals(1, access.groundLookups);
        Assert.assertEquals(17, access.groundX);
        Assert.assertEquals(-23, access.groundZ);
    }

    private static final class ColorAccess implements IBlockAccess, BiomeColorAccess {

        private final BiomeGenBase ground;
        private final BiomeGenBase snapshot;
        private int snapshotX, snapshotY, snapshotZ, groundLookups, groundX, groundZ;

        private ColorAccess(BiomeGenBase ground, BiomeGenBase snapshot) {
            this.ground = ground;
            this.snapshot = snapshot;
        }

        @Override
        public BiomeGenBase biomic$colorBiome(int x, int y, int z) {
            snapshotX = x;
            snapshotY = y;
            snapshotZ = z;
            return snapshot;
        }

        @Override
        public BiomeGenBase getBiomeGenForCoords(int x, int z) {
            groundLookups++;
            groundX = x;
            groundZ = z;
            return ground;
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
