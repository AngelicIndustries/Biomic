package industries.angelic.biomic.content.alexscaves;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/** Existing visible item identities select an exact dry/wet physical carrier, never create water from air. */
public final class ItemBlockAbyssalAquatic extends ItemBlock {

    private final BlockAbyssalAquatic family;

    public ItemBlockAbyssalAquatic(Block block) {
        super(block);
        family = (BlockAbyssalAquatic) block;
        if (family.kind() != BlockAbyssalAquatic.Kind.MUSSEL && family.kind() != BlockAbyssalAquatic.Kind.VENT_MEDIUM
            && family.kind() != BlockAbyssalAquatic.Kind.VENT_THIN)
            throw new IllegalArgumentException("Unsupported aquatic item family");
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    record Placement(BlockAbyssalAquatic block, int metadata) {}

    static boolean replaceableTarget(boolean sourceWater, boolean liquid, boolean replaceable, boolean air) {
        return sourceWater || !liquid && (replaceable || air);
    }

    static Placement placement(BlockAbyssalAquatic family, int side, boolean sourceWater) {
        int count = AbyssalContentBlocks.musselCount(family);
        if (count > 0) {
            if (side < 0 || side > 5) return null;
            return new Placement(AbyssalContentBlocks.mussel(count, sourceWater), side | (sourceWater ? 0 : 8));
        }
        boolean medium = family.kind() == BlockAbyssalAquatic.Kind.VENT_MEDIUM;
        return new Placement(
            sourceWater ? (medium ? AbyssalContentBlocks.VENT_MEDIUM : AbyssalContentBlocks.VENT_THIN)
                : (medium ? AbyssalContentBlocks.DRY_VENT_MEDIUM : AbyssalContentBlocks.DRY_VENT_THIN),
            sourceWater ? 12 : 4);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (stack.stackSize <= 0 || side < 0 || side > 5 || !world.blockExists(x, y, z)) return false;
        Block clicked = world.getBlock(x, y, z);
        if (!clicked.isReplaceable(world, x, y, z) && clicked != Blocks.water && clicked != Blocks.flowing_water) {
            ForgeDirection direction = ForgeDirection.getOrientation(side);
            x += direction.offsetX;
            y += direction.offsetY;
            z += direction.offsetZ;
        }
        if (!player.canPlayerEdit(x, y, z, side, stack) || !world.canMineBlock(player, x, y, z)
            || !placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, stack.getItemDamage()))
            return false;
        world.playSoundEffect(
            x + .5,
            y + .5,
            z + .5,
            family.stepSound.func_150496_b(),
            (family.stepSound.getVolume() + 1) / 2,
            family.stepSound.getPitch() * .8F);
        stack.stackSize--;
        return true;
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ, int metadata) {
        if (y < 0 || y >= world.getActualHeight() || !world.blockExists(x, y, z)) return false;
        Block before = world.getBlock(x, y, z);
        int beforeMetadata = world.getBlockMetadata(x, y, z);
        boolean source = before == Blocks.water && beforeMetadata == 0;
        if (!replaceableTarget(
            source,
            before.getMaterial()
                .isLiquid(),
            before.isReplaceable(world, x, y, z),
            before == Blocks.air)) return false;
        Placement placement = placement(family, side, source);
        if (placement == null) return false;
        if (AbyssalContentBlocks.musselCount(family) > 0) {
            ForgeDirection face = ForgeDirection.getOrientation(side);
            int sx = x - face.offsetX, sy = y - face.offsetY, sz = z - face.offsetZ;
            if (sy < 0 || sy >= world.getActualHeight()
                || !world.blockExists(sx, sy, sz)
                || !world.getBlock(sx, sy, sz)
                    .isSideSolid(world, sx, sy, sz, face))
                return false;
        }
        if (!world.canPlaceEntityOnSide(placement.block(), x, y, z, false, side, player, stack)) return false;
        if (world.isRemote) return true;
        return world.setBlock(x, y, z, placement.block(), placement.metadata(), 3);
    }
}
