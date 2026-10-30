package industries.angelic.biomic.content.block;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.client.BiomicRenderIds;

/** RU's oak-leaf/flower multipart model, retaining the registered leaf's species and decay bits. */
public final class BlockRuFloweringLeaves extends BlockRuLeaves {

    public static final String FLOWER_TEXTURE = "biomic:ru/flowering_leaves_flower";
    private IIcon flowers;

    public BlockRuFloweringLeaves() {
        super("flowering", "minecraft:leaves_oak", TintMode.BIOME_FOLIAGE);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        flowers = register.registerIcon(FLOWER_TEXTURE);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        // Always oak (0), not this block's persistent/decay bits interpreted as another vanilla species.
        // Delegating at render time also follows vanilla's fancy/fast icon switching and resource packs.
        return oakIcon(Blocks.leaves, side);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        return getIcon(side, 0);
    }

    @SideOnly(Side.CLIENT)
    public IIcon flowerIcon() {
        return flowers;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderType() {
        return BiomicRenderIds.RU_FLOWERING_LEAVES;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBlockColor() {
        return Blocks.leaves.getBlockColor();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int metadata) {
        return Blocks.leaves.getRenderColor(oakMetadata());
    }

    public static int oakMetadata() {
        return 0;
    }

    @SideOnly(Side.CLIENT)
    static IIcon oakIcon(net.minecraft.block.BlockLeaves nativeLeaves, int side) {
        return nativeLeaves.getIcon(side, oakMetadata());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return foliageColor(world, x, y, z);
    }

    /** Vanilla oak's nine-column smoothing, but each lookup uses the actual hosted Y identity. */
    public static int foliageColor(IBlockAccess world, int x, int y, int z) {
        return BlockRuLeaves.foliageColor(world, x, y, z);
    }
}
