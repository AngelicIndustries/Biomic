package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

/** Snow and frozen grass share a cell visually; no replacement block or world retrogen. */
public final class FrozenGrassRenderer implements ISimpleBlockRenderingHandler {

    private static final class OptionalMods {

        static final boolean ANGELICA = cpw.mods.fml.common.Loader.isModLoaded("angelica");
    }

    private final int id;

    public FrozenGrassRenderer(int id) {
        this.id = id;
    }

    public int getRenderId() {
        return id;
    }

    public boolean shouldRender3DInInventory(int modelId) {
        return false;
    }

    public void renderInventoryBlock(Block b, int metadata, int modelId, RenderBlocks renderer) {}

    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        int layers = layers(world, x, y, z);
        if (layers > 0) {
            IBlockAccess previous = renderer.blockAccess;
            double a = renderer.renderMinX, b = renderer.renderMinY, c = renderer.renderMinZ;
            double d = renderer.renderMaxX, e = renderer.renderMaxY, f = renderer.renderMaxZ;
            try {
                renderer.blockAccess = new SnowView(world, x, y, z, layers);
                renderer.setRenderBounds(0, 0, 0, 1, layers / 8.0, 1);
                if (OptionalMods.ANGELICA) AngelicaAquaticMaterial.beginSnow(layers - 1);
                renderer.renderStandardBlock(Blocks.snow_layer, x, y, z);
            } finally {
                if (OptionalMods.ANGELICA) AngelicaAquaticMaterial.endWater();
                renderer.blockAccess = previous;
                renderer.setRenderBounds(a, b, c, d, e, f);
            }
        }
        return renderer.renderCrossedSquares(block, x, y, z);
    }

    static int layers(IBlockAccess world, int x, int y, int z) {
        if (y < 1 || !world.getBlock(x, y - 1, z)
            .isSideSolid(world, x, y - 1, z, net.minecraftforge.common.util.ForgeDirection.UP)) return 0;
        int[] neighbours = new int[4];
        int index = 0;
        for (int[] o : new int[][] { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } }) {
            if (world.getBlock(x + o[0], y, z + o[1]) == Blocks.snow_layer)
                neighbours[index] = 1 + (world.getBlockMetadata(x + o[0], y, z + o[1]) & 7);
            index++;
        }
        return layers(neighbours);
    }

    static int layers(int... neighbours) {
        int layers = 9;
        for (int amount : neighbours) if (amount > 0 && amount <= 8) layers = Math.min(layers, amount);
        return layers == 9 ? 0 : layers;
    }

    private record SnowView(IBlockAccess delegate, int x, int y, int z, int layers) implements IBlockAccess {

        private boolean here(int a, int b, int c) {
            return a == x && b == y && c == z;
        }

        public Block getBlock(int a, int b, int c) {
            return here(a, b, c) ? Blocks.snow_layer : delegate.getBlock(a, b, c);
        }

        public int getBlockMetadata(int a, int b, int c) {
            return here(a, b, c) ? layers - 1 : delegate.getBlockMetadata(a, b, c);
        }

        public net.minecraft.tileentity.TileEntity getTileEntity(int a, int b, int c) {
            return delegate.getTileEntity(a, b, c);
        }

        public int getLightBrightnessForSkyBlocks(int a, int b, int c, int l) {
            return delegate.getLightBrightnessForSkyBlocks(a, b, c, l);
        }

        public int isBlockProvidingPowerTo(int a, int b, int c, int d) {
            return delegate.isBlockProvidingPowerTo(a, b, c, d);
        }

        public boolean isAirBlock(int a, int b, int c) {
            return !here(a, b, c) && delegate.isAirBlock(a, b, c);
        }

        public net.minecraft.world.biome.BiomeGenBase getBiomeGenForCoords(int a, int c) {
            return delegate.getBiomeGenForCoords(a, c);
        }

        public int getHeight() {
            return delegate.getHeight();
        }

        public boolean extendedLevelsInChunkCache() {
            return delegate.extendedLevelsInChunkCache();
        }

        public boolean isSideSolid(int a, int b, int c, net.minecraftforge.common.util.ForgeDirection side,
            boolean fallback) {
            return here(a, b, c) ? false : delegate.isSideSolid(a, b, c, side, fallback);
        }
    }
}
