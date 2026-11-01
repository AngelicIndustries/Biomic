package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** One wood's slab, single or double, faced with its planks. */
public final class BlockWoodSlab extends BlockSlab {

    private final Block planks;
    private final String name;
    private BlockWoodSlab single;

    /** Regions Unexplored's slabs; the alpha one keeps old Minecraft's stone-like wooden slab (rock, a pickaxe). */
    public BlockWoodSlab(boolean isDouble, Block planks, String name, boolean stoneLike) {
        super(isDouble, stoneLike ? Material.rock : Material.wood);
        this.planks = planks;
        this.name = name;
        single = this;
        setBlockName(Biomic.MODID + "." + name);
        setHardness(stoneLike ? 1.5F : 2.0F);
        setResistance(5.0F);
        setStepSound(stoneLike ? soundTypeStone : soundTypeWood);
        setHarvestLevel(stoneLike ? "pickaxe" : "axe", 0);
        if (!isDouble) setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    /** The single slab a double one breaks into and picks as. */
    public BlockWoodSlab single(BlockWoodSlab single) {
        this.single = single;
        return this;
    }

    @Override
    public String func_150002_b(int metadata) {
        return "tile." + Biomic.MODID + "." + single.name;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return planks.getIcon(side, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return Item.getItemFromBlock(single);
    }

    @Override
    public int damageDropped(int metadata) {
        return 0;
    }

    @Override
    protected ItemStack createStackedBlock(int metadata) {
        return new ItemStack(single, 2, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(single);
    }
}
