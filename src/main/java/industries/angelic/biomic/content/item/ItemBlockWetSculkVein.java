package industries.angelic.biomic.content.item;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import industries.angelic.biomic.content.BiomicWetSculkBlocks;
import industries.angelic.biomic.content.block.BlockWetSculkVein;
import industries.angelic.biomic.world.LiveChunkLayout;
import industries.angelic.biomic.world.terrain.PhysicalLichenSupport;

/** Command-obtained companion items consume an existing native water source; never create fluid. */
public final class ItemBlockWetSculkVein extends ItemBlock {

    private final BlockWetSculkVein vein;

    public ItemBlockWetSculkVein(Block block) {
        super(block);
        if (!(block instanceof BlockWetSculkVein wet))
            throw new IllegalArgumentException("Wet sculk item requires wet block");
        vein = wet;
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    static boolean sourcePlacement(boolean hell, boolean sourceWater, int sourceLevel, int metadata, int faceMask) {
        return !hell && sourceWater
            && sourceLevel == 0
            && metadata >= 0
            && metadata <= 15
            && faceMask > 0
            && faceMask < 64;
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ, int metadata) {
        if (!BiomicWetSculkBlocks.wet(vein) || !world.blockExists(x, y, z) || metadata < 0 || metadata > 15)
            return false;
        Block before = world.getBlock(x, y, z);
        int faces = vein.faceMask(metadata);
        if (!sourcePlacement(
            world.provider.isHellWorld,
            before == Blocks.water || before == Blocks.flowing_water,
            world.getBlockMetadata(x, y, z),
            metadata,
            faces)) return false;
        int supported = PhysicalLichenSupport.supported(
            faces,
            x,
            y,
            z,
            LiveChunkLayout.of(world)
                .height(),
            (sx, sy, sz) -> {
                if (!world.blockExists(sx, sy, sz))
                    throw new UnsupportedOperationException("Unavailable sculk support");
                return new PhysicalLichenSupport.Cell(world.getBlock(sx, sy, sz), world.getBlockMetadata(sx, sy, sz));
            });
        return supported == faces
            && super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata);
    }
}
