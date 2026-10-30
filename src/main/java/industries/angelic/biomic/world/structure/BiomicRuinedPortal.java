package industries.angelic.biomic.world.structure;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;

import industries.angelic.biomic.world.WorldgenContext;

/**
 * Biomic's ruined portal, an original layout: a broken obsidian frame four wide and five tall, sunk a block into the
 * ground, some of its blocks crying obsidian and some fallen, on a scorched splash of netherrack and magma with a
 * chest.
 * Which blocks broke is hashed from the cell, so every population box that draws a part of it agrees. In a snowy
 * habitat snow lies on it.
 */
public final class BiomicRuinedPortal extends StructureComponent {

    static final int SIZE = 9, HEIGHT = 7;

    private static final WeightedRandomChestContent[] LOOT = {
        new WeightedRandomChestContent(Item.getItemFromBlock(Blocks.obsidian), 0, 1, 2, 8),
        new WeightedRandomChestContent(Items.flint_and_steel, 0, 1, 1, 6),
        new WeightedRandomChestContent(Items.flint, 0, 1, 4, 6),
        new WeightedRandomChestContent(Items.gold_nugget, 0, 4, 18, 10),
        new WeightedRandomChestContent(Items.gold_ingot, 0, 1, 4, 5),
        new WeightedRandomChestContent(Items.golden_carrot, 0, 2, 6, 4),
        new WeightedRandomChestContent(Items.golden_sword, 0, 1, 1, 3),
        new WeightedRandomChestContent(Items.clock, 0, 1, 1, 2),
        new WeightedRandomChestContent(Items.golden_apple, 0, 1, 1, 1), };

    private long salt;
    /** Whether snow lies on it (its habitat's, chosen with the plan). */
    boolean snowy;

    public BiomicRuinedPortal() {}

    BiomicRuinedPortal(int x, int y, int z, int orientation, long salt) {
        super(0);
        coordBaseMode = orientation;
        boundingBox = new StructureBoundingBox(x, y, z, x + SIZE - 1, y + HEIGHT - 1, z + SIZE - 1);
        this.salt = salt;
    }

    @Override
    protected void func_143012_a(NBTTagCompound tag) {
        tag.setLong("Salt", salt);
        tag.setBoolean("Snowy", snowy);
    }

    @Override
    protected void func_143011_b(NBTTagCompound tag) {
        salt = tag.getLong("Salt");
        snowy = tag.getBoolean("Snowy");
    }

    /** A number in [0, 1) for one cell of this portal, the same in every box. */
    private double roll(int x, int y, int z, long kind) {
        long bits = WorldgenContext.mix64(
            salt ^ kind
                ^ (long) x * 0x9E3779B97F4A7C15L
                ^ (long) y * 0xC2B2AE3D27D4EB4FL
                ^ (long) z * 0x165667B19E3779F9L);
        return (bits >>> 11) * 0x1.0p-53;
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox box) {
        Block crying = Block.getBlockFromName("etfuturum:crying_obsidian");
        Block magma = Block.getBlockFromName("etfuturum:magma");
        // The splash: the ground's top turns to netherrack, thinning out from the centre. It follows the ground
        // column by column (laid at the portal's own height it stood as a platform where the ground fell away).
        for (int x = 0; x < SIZE; x++) for (int z = 0; z < SIZE; z++) {
            double d = Math.sqrt((x - 4) * (x - 4) + (z - 4) * (z - 4));
            double r = roll(x, 0, z, 1);
            if (d > 4.5 || r > 1.05 - d / 4.5) continue;
            int ground = ground(world, x, z, box);
            if (ground == NO_GROUND) continue;
            placeBlockAtCurrentPosition(
                world,
                magma != null && r < 0.08 ? magma : Blocks.netherrack,
                0,
                x,
                ground,
                z,
                box);
        }
        // The frame, in the plane z = 4, sunk one block: obsidian, some of it crying, some fallen; propped on
        // netherrack down to the ground where the ground is lower.
        for (int x = 2; x <= 5; x++) for (int y = 0; y <= 5; y++) {
            boolean edge = x == 2 || x == 5 || y == 0 || y == 5;
            if (!edge) {
                placeBlockAtCurrentPosition(world, Blocks.air, 0, x, y, 4, box);
                continue;
            }
            double r = roll(x, y, 4, 2);
            if (y > 0 && r < 0.06 + y * 0.03) continue;
            Block frame = crying != null && r > 0.86 ? crying : Blocks.obsidian;
            placeBlockAtCurrentPosition(world, frame, 0, x, y, 4, box);
            if (y == 0) func_151554_b(world, Blocks.netherrack, 0, x, -1, 4, box);
        }
        // Fallen pieces lie beside it on the ground, now and then a gold block among them.
        for (int i = 0; i < 4; i++) {
            int x = 1 + (int) (roll(i, 9, 0, 3) * 7), z = (int) (roll(i, 9, 1, 3) * 3) + (i % 2 == 0 ? 1 : 6);
            int ground = ground(world, x, z, box);
            if (ground == NO_GROUND) continue;
            Block piece = roll(i, 9, 2, 3) < 0.12 ? Blocks.gold_block : Blocks.obsidian;
            placeBlockAtCurrentPosition(world, piece, 0, x, ground + 1, z, box);
        }
        int chestGround = ground(world, 6, 6, box);
        if (chestGround != NO_GROUND)
            generateStructureChestContents(world, box, random, 6, chestGround + 1, 6, LOOT, 3 + random.nextInt(4));
        // In a snowy habitat snow lies on the portal's own blocks (the ground keeps the snow it has).
        if (snowy)
            for (int x = 0; x < SIZE; x++) for (int z = 0; z < SIZE; z++) for (int y = HEIGHT - 2; y >= -4; y--) {
                Block top = getBlockAtCurrentPosition(world, x, y, z, box);
                if (top.getMaterial() == Material.air) continue;
                if ((top == Blocks.obsidian || top == crying || top == Blocks.gold_block)
                    && getBlockAtCurrentPosition(world, x, y + 1, z, box).getMaterial() == Material.air)
                    placeBlockAtCurrentPosition(world, Blocks.snow_layer, 0, x, y + 1, z, box);
                break;
            }
        return true;
    }

    private static final int NO_GROUND = Integer.MIN_VALUE;

    /** The ground's top in a column, within a few blocks of the portal's own height (local y), or NO_GROUND. */
    private int ground(World world, int x, int z, StructureBoundingBox box) {
        for (int y = HEIGHT - 2; y >= -5; y--) {
            Block block = getBlockAtCurrentPosition(world, x, y, z, box);
            Material material = block.getMaterial();
            if (material == Material.air || material.isReplaceable()
                || material == Material.plants
                || material == Material.leaves
                || material.isLiquid()
                || block == Blocks.obsidian) continue;
            return y;
        }
        return NO_GROUND;
    }
}
