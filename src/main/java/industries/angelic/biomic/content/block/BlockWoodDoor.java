package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;

/** One wood's door: RU's art as the "_upper"/"_lower" halves vanilla's door reads. Its item places and drops it. */
public final class BlockWoodDoor extends BlockDoor {

    private Item item;

    public BlockWoodDoor(String name, String texture) {
        super(Material.wood);
        setBlockName(Biomic.MODID + "." + name);
        setBlockTextureName(Biomic.MODID + ":" + texture);
        setHardness(3.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
        disableStats();
    }

    public void setItem(Item item) {
        this.item = item;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return (metadata & 8) != 0 ? null : item;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getItem(World world, int x, int y, int z) {
        return item;
    }
}
