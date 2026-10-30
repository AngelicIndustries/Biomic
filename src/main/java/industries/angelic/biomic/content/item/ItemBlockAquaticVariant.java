package industries.angelic.biomic.content.item;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import industries.angelic.biomic.content.block.AquaticVariantBlock;
import industries.angelic.biomic.content.block.BlockAquaticPlant;

/** Metadata-preserving item form shared by Biomic's compact aquatic block families. */
public final class ItemBlockAquaticVariant extends ItemBlock {

    private final AquaticVariantBlock variants;

    public ItemBlockAquaticVariant(Block block) {
        super(block);
        variants = (AquaticVariantBlock) block;
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + variants.getVariantName(stack.getItemDamage());
    }

    record WallPlacement(int species, boolean dead) {}

    static WallPlacement wallPlacement(int metadata, int side) {
        return side >= 2 && side <= 5 && metadata >= 0 && metadata < 10 ? new WallPlacement(metadata % 5, metadata >= 5)
            : null;
    }

    /** Modern fans share one item between floor and wall models. Only horizontal use takes this new path. */
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (!(variants instanceof BlockAquaticPlant plant) || plant.getKind() != BlockAquaticPlant.Kind.CORAL_FAN
            || side < 2
            || side > 5) return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
        var placement = wallPlacement(stack.getItemDamage(), side);
        if (placement == null || stack.stackSize <= 0 || !world.blockExists(x, y, z)) return false;
        Block clicked = world.getBlock(x, y, z);
        if (!clicked.isReplaceable(world, x, y, z)) {
            var direction = ForgeDirection.getOrientation(side);
            x += direction.offsetX;
            y += direction.offsetY;
            z += direction.offsetZ;
        }
        if (!player.canPlayerEdit(x, y, z, side, stack) || !world.canMineBlock(player, x, y, z)) return false;
        if (!ItemBlockCoralWallFan.place(placement.species(), placement.dead(), stack, player, world, x, y, z, side))
            return false;
        var placed = world.getBlock(x, y, z);
        world.playSoundEffect(
            x + .5,
            y + .5,
            z + .5,
            placed.stepSound.func_150496_b(),
            (placed.stepSound.getVolume() + 1) / 2,
            placed.stepSound.getPitch() * .8F);
        stack.stackSize--;
        return true;
    }
}
