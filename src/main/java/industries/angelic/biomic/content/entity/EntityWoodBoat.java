package industries.angelic.biomic.content.entity;

import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import industries.angelic.biomic.content.BiomicWoods;
import io.netty.buffer.ByteBuf;

/**
 * One of the woods' boats: vanilla's boat (its sailing, crashing and riding), of a wood. What vanilla's boat drops
 * is turned into this wood's: its boat whole, its planks when it breaks up.
 */
public final class EntityWoodBoat extends EntityBoat implements IEntityAdditionalSpawnData {

    private BiomicWoods.Wood wood = BiomicWoods.Wood.PINE;

    public EntityWoodBoat(World world) {
        super(world);
    }

    public EntityWoodBoat(World world, double x, double y, double z, BiomicWoods.Wood wood) {
        super(world, x, y, z);
        this.wood = wood;
    }

    public BiomicWoods.Wood wood() {
        return wood;
    }

    @Override
    public EntityItem func_145778_a(Item item, int count, float offsetY) {
        if (item == Items.boat) item = wood.boat();
        else if (item == Item.getItemFromBlock(Blocks.planks)) item = Item.getItemFromBlock(wood.planks());
        return super.func_145778_a(item, count, offsetY);
    }

    /** Named as its item ("Pine Boat"), where vanilla's boat is "Boat". */
    @Override
    public String getCommandSenderName() {
        return StatCollector.translateToLocal(
            wood.boat()
                .getUnlocalizedName() + ".name");
    }

    @Override
    public ItemStack getPickedResult(MovingObjectPosition target) {
        return new ItemStack(wood.boat());
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setString("Wood", wood.id);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        BiomicWoods.Wood saved = BiomicWoods.Wood.byId(tag.getString("Wood"));
        if (saved != null && saved.boat() != null) wood = saved;
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeByte(wood.ordinal());
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        wood = BiomicWoods.Wood.values()[buffer.readByte()];
    }
}
