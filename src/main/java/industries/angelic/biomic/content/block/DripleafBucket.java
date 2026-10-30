package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.FillBucketEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import industries.angelic.biomic.content.BiomicDripleafBlocks;

/** Exact cell transitions retain the paired plant, facing/half bits and the other cell's water. */
public final class DripleafBucket {

    enum Action {
        NONE,
        DRAIN,
        FILL
    }

    static Action action(boolean empty, boolean filled, boolean wet, boolean dripleaf, int metadata, boolean small) {
        if (!dripleaf || metadata < 0 || metadata > (small ? 7 : 3)) return Action.NONE;
        return empty && wet ? Action.DRAIN : filled && !wet ? Action.FILL : Action.NONE;
    }

    static Block replacement(Block block, boolean wet) {
        if (block instanceof BlockSmallDripleaf)
            return wet ? BiomicDripleafBlocks.WET_SMALL_DRIPLEAF : BiomicDripleafBlocks.SMALL_DRIPLEAF;
        if (block instanceof BlockBigDripleaf big) return big.isStem()
            ? wet ? BiomicDripleafBlocks.WET_BIG_DRIPLEAF_STEM : BiomicDripleafBlocks.BIG_DRIPLEAF_STEM
            : wet ? BiomicDripleafBlocks.WET_BIG_DRIPLEAF : BiomicDripleafBlocks.BIG_DRIPLEAF;
        throw new IllegalArgumentException("Not a dripleaf cell");
    }

    @SubscribeEvent
    public void fill(FillBucketEvent event) {
        var hit = event.target;
        var world = event.world;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        int x = hit.blockX, y = hit.blockY, z = hit.blockZ;
        if (!world.blockExists(x, y, z)) return;
        Block block = world.getBlock(x, y, z);
        boolean dripleaf = block instanceof BlockDripleafBase;
        boolean wet = dripleaf && ((BlockDripleafBase) block).isWaterlogged();
        var item = event.current.getItem();
        boolean filled = item == Items.water_bucket || item == Items.lava_bucket;
        Action action = action(
            item == Items.bucket,
            item == Items.water_bucket,
            wet,
            dripleaf,
            world.getBlockMetadata(x, y, z),
            block instanceof BlockSmallDripleaf);
        if (action == Action.NONE && filled && hit.sideHit >= 0 && hit.sideHit < 6) {
            var side = ForgeDirection.getOrientation(hit.sideHit);
            int dx = x + side.offsetX, dy = y + side.offsetY, dz = z + side.offsetZ;
            if (world.blockExists(dx, dy, dz) && world.getBlock(dx, dy, dz) instanceof BlockDripleafBase leaf
                && leaf.isWaterlogged()) {
                event.setCanceled(true);
                return;
            }
        }
        if (!dripleaf) return;
        if (wet && item == Items.bucket) event.setCanceled(true);
        if (action == Action.NONE) return;
        event.setCanceled(true);
        if (action == Action.FILL && world.provider.isHellWorld) return;
        if (!world.canMineBlock(event.entityPlayer, x, y, z)
            || !event.entityPlayer.canPlayerEdit(x, y, z, hit.sideHit, event.current)
            || world.isRemote) return;
        int metadata = world.getBlockMetadata(x, y, z);
        Block replacement = replacement(block, action == Action.FILL);
        if (action == Action.FILL && !((BlockDripleafBase) replacement).canBlockStay(world, x, y, z)) return;
        if (!world.setBlock(x, y, z, replacement, metadata, 3)) return;
        event.result = new ItemStack(action == Action.DRAIN ? Items.water_bucket : Items.bucket);
        event.setCanceled(false);
        event.setResult(Event.Result.ALLOW);
    }
}
