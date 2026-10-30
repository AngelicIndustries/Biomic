package industries.angelic.biomic.world.structure;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;

/**
 * Biomic's igloo, an original layout: a snow dome seven blocks across with a short entrance tunnel, packed-ice
 * windows (plain ice would melt by the lamp), a carpeted floor, a bed, a furnace, a workbench and a small chest. It
 * stands on snowy ground, its floor carried down to the ground below.
 */
public final class BiomicIgloo extends StructureComponent {

    static final int WIDTH = 7, HEIGHT = 5, DEPTH = 10;

    private static final WeightedRandomChestContent[] LOOT = { new WeightedRandomChestContent(Items.coal, 0, 2, 6, 10),
        new WeightedRandomChestContent(Items.bread, 0, 1, 3, 10),
        new WeightedRandomChestContent(Items.cooked_fished, 0, 1, 3, 8),
        new WeightedRandomChestContent(Items.leather_boots, 0, 1, 1, 3),
        new WeightedRandomChestContent(Items.leather_chestplate, 0, 1, 1, 2),
        new WeightedRandomChestContent(Items.snowball, 0, 4, 12, 6),
        new WeightedRandomChestContent(Items.golden_apple, 0, 1, 1, 1), };

    public BiomicIgloo() {}

    BiomicIgloo(int x, int y, int z, int orientation) {
        super(0);
        coordBaseMode = orientation;
        boundingBox = orientation == 1 || orientation == 3
            ? new StructureBoundingBox(x, y, z, x + DEPTH - 1, y + HEIGHT - 1, z + WIDTH - 1)
            : new StructureBoundingBox(x, y, z, x + WIDTH - 1, y + HEIGHT - 1, z + DEPTH - 1);
    }

    @Override
    protected void func_143012_a(NBTTagCompound tag) {}

    @Override
    protected void func_143011_b(NBTTagCompound tag) {}

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox box) {
        // The dome: centre (3, 3) in the first seven rows; the tunnel runs out through rows 7 to 9.
        for (int x = 0; x < WIDTH; x++) for (int z = 0; z < WIDTH; z++) {
            int d = (x - 3) * (x - 3) + (z - 3) * (z - 3);
            if (d > 10) continue;
            func_151554_b(world, Blocks.snow, 0, x, -1, z, box);
            placeBlockAtCurrentPosition(world, Blocks.snow, 0, x, 0, z, box);
            for (int y = 1; y <= 2; y++)
                placeBlockAtCurrentPosition(world, d > 5 ? Blocks.snow : Blocks.air, 0, x, y, z, box);
            placeBlockAtCurrentPosition(world, d > 6 ? Blocks.air : d > 2 ? Blocks.snow : Blocks.air, 0, x, 3, z, box);
            placeBlockAtCurrentPosition(world, d <= 3 ? Blocks.snow : Blocks.air, 0, x, 4, z, box);
            if (d <= 5) placeBlockAtCurrentPosition(world, Blocks.carpet, 0, x, 1, z, box);
        }
        for (int z = 6; z < DEPTH; z++) for (int x = 2; x <= 4; x++) {
            func_151554_b(world, Blocks.snow, 0, x, -1, z, box);
            placeBlockAtCurrentPosition(world, Blocks.snow, 0, x, 0, z, box);
            boolean wall = x != 3;
            for (int y = 1; y <= 2; y++)
                placeBlockAtCurrentPosition(world, wall ? Blocks.snow : Blocks.air, 0, x, y, z, box);
            placeBlockAtCurrentPosition(world, Blocks.snow, 0, x, 3, z, box);
        }
        placeBlockAtCurrentPosition(world, Blocks.air, 0, 3, 1, DEPTH - 1, box);
        placeBlockAtCurrentPosition(world, Blocks.air, 0, 3, 2, DEPTH - 1, box);
        // Windows east and west, the furnishings along the back wall.
        placeBlockAtCurrentPosition(world, Blocks.packed_ice, 0, 0, 2, 3, box);
        placeBlockAtCurrentPosition(world, Blocks.packed_ice, 0, 6, 2, 3, box);
        placeBlockAtCurrentPosition(world, Blocks.crafting_table, 0, 1, 1, 2, box);
        // Its own onBlockAdded turns a furnace to the open side.
        placeBlockAtCurrentPosition(world, Blocks.furnace, 2, 5, 1, 2, box);
        placeBed(world, box);
        placeBlockAtCurrentPosition(world, Blocks.torch, 0, 3, 2, 1, box);
        generateStructureChestContents(world, box, random, 5, 1, 4, LOOT, 3 + random.nextInt(3));
        return true;
    }

    private void placeBed(World world, StructureBoundingBox box) {
        // Foot at (3, 1, 2), head toward the back wall (local -z); bed metadata is the facing, with 8 for the head.
        int facing = switch (coordBaseMode) {
            case 0 -> 2;
            case 1 -> 3;
            case 2 -> 0;
            default -> 1;
        };
        placeBlockAtCurrentPosition(world, Blocks.bed, facing, 3, 1, 2, box);
        placeBlockAtCurrentPosition(world, Blocks.bed, facing | 8, 3, 1, 1, box);
    }
}
