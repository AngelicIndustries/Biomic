package industries.angelic.biomic.content.block;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;
import industries.angelic.biomic.content.BiomicBlocks;

/** Flowering and four-quadrant giant RU lily pads. */
public final class BlockRuLilyPad extends Block {

    public enum Kind {
        FLOWERING,
        GIANT
    }

    private final Kind kind;
    private final String topTexture;
    private final String bottomTexture;
    private final String flowerTexture;
    private IIcon topIcon;
    private IIcon bottomIcon;
    private IIcon flowerIcon;
    private IIcon closedFlowerIcon;

    public BlockRuLilyPad(String name, Kind kind, String topTexture, String bottomTexture, String flowerTexture) {
        super(Material.plants);
        this.kind = kind;
        this.topTexture = topTexture;
        this.bottomTexture = bottomTexture;
        this.flowerTexture = flowerTexture;
        setBlockName(Biomic.MODID + "." + name);
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightOpacity(0);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, kind == Kind.GIANT ? 0.25F : 0.15F, 1.0F);
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return BiomicRenderIds.RU_AQUATIC;
    }

    @Override
    public int getRenderBlockPass() {
        // The pinned RU models use cutout, not translucent, rendering. Pass one washed the sprites out under shaders.
        return 0;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.isAirBlock(x, y, z) && canBlockStay(world, x, y, z);
    }

    public boolean canBlockStay(World world, int x, int y, int z) {
        if (y <= 0) return false;
        Block below = world.getBlock(x, y - 1, z);
        // Aquatic metadata describes the plant/half, not a flowing-water level.
        // All our aquatic plant variants retain a source-water cell, including tall seagrass tops.
        return below instanceof BlockAquaticPlant
            || below.getMaterial() == Material.water && world.getBlockMetadata(x, y - 1, z) == 0;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) world.setBlockToAir(x, y, z);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List boxes,
        Entity entity) {
        if (!(entity instanceof EntityBoat)) super.addCollisionBoxesToList(world, x, y, z, mask, boxes, entity);
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return kind == Kind.FLOWERING ? Item.getItemFromBlock(this) : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        topIcon = register.registerIcon(Biomic.MODID + ":" + topTexture);
        bottomIcon = register.registerIcon(Biomic.MODID + ":" + bottomTexture);
        flowerIcon = flowerTexture == null ? topIcon : register.registerIcon(Biomic.MODID + ":" + flowerTexture);
        closedFlowerIcon = flowerTexture == null ? flowerIcon
            : register.registerIcon(Biomic.MODID + ":" + flowerTexture + "_closed");
        blockIcon = topIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return side == 0 ? bottomIcon : topIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getTopIcon() {
        return topIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getBottomIcon() {
        return bottomIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getFlowerIcon(boolean closed) {
        return closed ? closedFlowerIcon : flowerIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBlockColor() {
        return 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int metadata) {
        return 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return 0xFFFFFF;
    }
}
