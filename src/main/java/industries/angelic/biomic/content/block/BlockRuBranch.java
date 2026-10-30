package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;
import industries.angelic.biomic.content.BiomicBlocks;

/** Directional cutout representation of RU's long, thin branch model. */
public class BlockRuBranch extends Block {

    private final String texture;
    private final String logTexture;
    private IIcon branchIcon;
    private IIcon logIcon;

    public BlockRuBranch(String species) {
        this(species, species + "_branch");
    }

    public BlockRuBranch(String name, String texture) {
        this(name, texture, null);
    }

    public BlockRuBranch(String name, String texture, String logTexture) {
        super(Material.wood);
        this.texture = texture;
        this.logTexture = logTexture;
        setBlockName(Biomic.MODID + "." + name + "_branch");
        setHardness(2.0F);
        setStepSound(soundTypeWood);
        setLightOpacity(0);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
        setBlockBounds(0.25F, 0.25F, 0.25F, 0.75F, 0.75F, 0.75F);
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
        return BiomicRenderIds.RU_BRANCH;
    }

    @Override
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        String name = texture.indexOf(':') >= 0 ? texture : Biomic.MODID + ":" + texture;
        branchIcon = logTexture == null ? register.registerIcon(name)
            : industries.angelic.biomic.client.render.RuBranchSprite.register(register, name);
        logIcon = logTexture == null ? null
            : register.registerIcon(logTexture.indexOf(':') >= 0 ? logTexture : Biomic.MODID + ":" + logTexture);
        blockIcon = logIcon == null ? branchIcon : logIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getBranchIcon() {
        return branchIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getLogIcon() {
        return logIcon;
    }

    public boolean isSilverBirch() {
        return "ru/silver_birch_branch".equals(texture);
    }
}
