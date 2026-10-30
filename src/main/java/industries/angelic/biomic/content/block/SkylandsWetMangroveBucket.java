package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.FillBucketEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import industries.angelic.biomic.content.BiomicBlocks;

/** Handles only the exact wet/dry root target, before vanilla's destructive source pickup. */
public final class SkylandsWetMangroveBucket {

    public enum Action {
        NONE,
        DRAIN,
        FILL
    }

    static Action action(Item item, boolean wet, boolean dry, int metadata) {
        return action(
            item != null && item == Items.bucket,
            item != null && item == Items.water_bucket,
            wet,
            dry,
            metadata);
    }

    static Action action(boolean emptyBucket, boolean waterBucket, boolean wet, boolean dry, int metadata) {
        if (metadata != 0) return Action.NONE;
        return wet && emptyBucket ? Action.DRAIN : dry && waterBucket ? Action.FILL : Action.NONE;
    }

    static boolean allowFill(boolean hellWorld) {
        return !hellWorld;
    }

    @SubscribeEvent
    public void fill(FillBucketEvent event) {
        var hit = event.target;
        var world = event.world;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        int x = hit.blockX, y = hit.blockY, z = hit.blockZ;
        if (!world.blockExists(x, y, z)) return;
        Item item = event.current.getItem();
        Block block = world.getBlock(x, y, z);
        boolean wet = block == BiomicBlocks.SKYLANDS_WET_MANGROVE_ROOTS;
        boolean dry = block == ganymedes01.etfuturum.ModBlocks.MANGROVE_ROOTS.get();
        Action action = action(item, wet, dry, world.getBlockMetadata(x, y, z));
        boolean filled = item == Items.water_bucket || item == Items.lava_bucket;
        // Vanilla filled buckets place at the side-offset target, not the ray hit itself.
        if (action == Action.NONE && filled && hit.sideHit >= 0 && hit.sideHit < 6) {
            var side = ForgeDirection.getOrientation(hit.sideHit);
            int dx = x + side.offsetX, dy = y + side.offsetY, dz = z + side.offsetZ;
            if (world.blockExists(dx, dy, dz)
                && world.getBlock(dx, dy, dz) == BiomicBlocks.SKYLANDS_WET_MANGROVE_ROOTS) {
                event.setCanceled(true);
                return;
            }
        }
        if (!wet && !dry) return;
        // A wet source cell must never fall through to vanilla pickup, including invalid metadata/permissions.
        if (wet && event.current.getItem() == Items.bucket) event.setCanceled(true);
        if (action == Action.NONE) return;
        event.setCanceled(true);
        if (action == Action.FILL && !allowFill(world.provider.isHellWorld)) return;
        if (!world.canMineBlock(event.entityPlayer, x, y, z)
            || !event.entityPlayer.canPlayerEdit(x, y, z, hit.sideHit, event.current)) return;
        if (world.isRemote) return;
        BlockSkylandsWetMangroveRoots.dryRoots(); // Both transitions require the exact installed dry counterpart.
        Block replacement = action == Action.DRAIN ? BlockSkylandsWetMangroveRoots.dryRoots()
            : BiomicBlocks.SKYLANDS_WET_MANGROVE_ROOTS;
        if (!world.setBlock(x, y, z, replacement, 0, 3)) return;
        event.result = new ItemStack(action == Action.DRAIN ? Items.water_bucket : Items.bucket);
        event.setCanceled(false);
        event.setResult(Event.Result.ALLOW);
    }
}
