package industries.angelic.biomic.content.item;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;
import industries.angelic.biomic.content.BiomicWoods;
import industries.angelic.biomic.content.entity.EntityWoodBoat;

/** One wood's boat item: puts its boat in the water the way vanilla's boat item does. */
public final class ItemWoodBoat extends Item {

    private final BiomicWoods.Wood wood;

    public ItemWoodBoat(String name, String texture, BiomicWoods.Wood wood) {
        this.wood = wood;
        maxStackSize = 1;
        setUnlocalizedName(Biomic.MODID + "." + name);
        setTextureName(Biomic.MODID + ":" + texture);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch);
        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw);
        Vec3 eye = Vec3.createVectorHelper(player.posX, player.posY + 1.62D - player.yOffset, player.posZ);
        float cos = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI),
            sin = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float horizontal = -MathHelper.cos(-pitch * 0.017453292F), vertical = MathHelper.sin(-pitch * 0.017453292F);
        double reach = 5.0D;
        Vec3 end = eye.addVector(sin * horizontal * reach, vertical * reach, cos * horizontal * reach);
        MovingObjectPosition hit = world.rayTraceBlocks(eye, end, true);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return stack;
        Vec3 look = player.getLook(1.0F);
        List<?> near = world.getEntitiesWithinAABBExcludingEntity(
            player,
            player.boundingBox.addCoord(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach)
                .expand(1.0D, 1.0D, 1.0D));
        for (Object object : near) {
            Entity entity = (Entity) object;
            float border = entity.getCollisionBorderSize();
            AxisAlignedBB box = entity.boundingBox.expand(border, border, border);
            if (entity.canBeCollidedWith() && box.isVecInside(eye)) return stack;
        }
        int x = hit.blockX, y = hit.blockY, z = hit.blockZ;
        if (world.getBlock(x, y, z) == Blocks.snow_layer) y--;
        EntityWoodBoat boat = new EntityWoodBoat(world, x + 0.5F, y + 1.0F, z + 0.5F, wood);
        boat.rotationYaw = ((MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3) - 1) * 90;
        if (!world.getCollidingBoundingBoxes(boat, boat.boundingBox.expand(-0.1D, -0.1D, -0.1D))
            .isEmpty()) return stack;
        if (!world.isRemote) world.spawnEntityInWorld(boat);
        if (!player.capabilities.isCreativeMode) stack.stackSize--;
        return stack;
    }
}
