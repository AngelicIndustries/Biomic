package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSign;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;

/**
 * One wood's sign, standing or on a wall: vanilla's sign with its own tile entity class ({@link TileEntityWoodSign}),
 * so the client draws it with this wood's sign art (RU's entity texture, laid out as vanilla's).
 */
public final class BlockWoodSign extends BlockSign {

    private final Block planks;
    private final boolean standing;
    private final ResourceLocation texture;
    private Item item;

    public BlockWoodSign(String name, Block planks, boolean standing, String woodId) {
        super(TileEntityWoodSign.class, standing);
        this.planks = planks;
        this.standing = standing;
        texture = new ResourceLocation(Biomic.MODID, "textures/entity/signs/" + woodId + ".png");
        setBlockName(Biomic.MODID + "." + name);
        setHardness(1.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
        disableStats();
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public boolean standing() {
        return standing;
    }

    public ResourceLocation texture() {
        return texture;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return item;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getItem(World world, int x, int y, int z) {
        return item;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return planks.getIcon(side, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}
}
