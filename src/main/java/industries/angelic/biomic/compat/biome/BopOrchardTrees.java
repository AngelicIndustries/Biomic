package industries.angelic.biomic.compat.biome;

import java.lang.reflect.Field;
import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenTrees;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Preserves BOP's nested Orchard selector while giving one saved external provider ownership of fruit trees. */
public final class BopOrchardTrees {

    public record Selection(WorldGenAbstractTree nativeTree, boolean fruit) {}

    private BopOrchardTrees() {}

    public static Selection select(Random random) {
        WorldGenAbstractTree tree = BopNativeBiomes.binding(BopNativeBiomes.ORCHARD).biome.func_150567_a(random);
        String name = tree.getClass()
            .getName();
        if (tree.getClass() == WorldGenTrees.class) return new Selection(new WorldGenTrees(false), false);
        if (!name.equals("biomesoplenty.common.world.features.trees.WorldGenMixedTree")
            && !name.equals("biomesoplenty.common.world.features.trees.WorldGenOriginalTree"))
            throw new IllegalStateException("Unaudited native Orchard tree: " + name);
        int meta = fruitMeta(tree);
        if (name.endsWith("WorldGenMixedTree") && meta != 0 && meta != 2
            || name.endsWith("WorldGenOriginalTree") && meta != 3)
            throw new IllegalStateException("Native Orchard fruit selector changed: " + name + ':' + meta);
        return new Selection(tree, true);
    }

    private static int fruitMeta(WorldGenAbstractTree tree) {
        try {
            Field field = tree.getClass()
                .getDeclaredField("metaFruit");
            field.setAccessible(true);
            return field.getInt(tree);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot inspect native Orchard fruit tree", error);
        }
    }

    public static boolean placeNonFruit(World world, NativeFeatureCapture.Access access, Random random,
        Selection selection, int x, int y, int z) {
        if (selection == null || selection.fruit)
            throw new IllegalArgumentException("External Orchard provider owns native fruit selection");
        return BopWholeTree.place(world, access, random, selection.nativeTree, x, y, z);
    }
}
