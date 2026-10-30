package industries.angelic.biomic.compat.biome;

import java.lang.reflect.Modifier;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Native Garden flower geometry with a whole air envelope; never writes through a tree. */
public final class BopGiantFlower {

    private BopGiantFlower() {}

    /** Whole-tree caller buffers writes; a later shrub may not replace native flower petals. */
    public static NativeFeatureCapture.Access preservingPetals(NativeFeatureCapture.Access access) {
        Block petals = NativeBlockCatalogue.resolve("biomesoplenty:petals[legacy_meta=0]")
            .block();
        return new NativeFeatureCapture.Access() {

            public Block block(int x, int y, int z) {
                return access.block(x, y, z);
            }

            public int metadata(int x, int y, int z) {
                return access.metadata(x, y, z);
            }

            public int heightValue(int x, int z) {
                return access.heightValue(x, z);
            }

            public int minimumY() {
                return access.minimumY();
            }

            public int maximumYExclusive() {
                return access.maximumYExclusive();
            }

            public boolean set(int x, int y, int z, Block block, int meta) {
                return access.block(x, y, z) != petals && access.set(x, y, z, block, meta);
            }
        };
    }

    public static int variant(WorldGenAbstractTree generator) {
        if (!generator.getClass()
            .getName()
            .equals("biomesoplenty.common.world.features.trees.WorldGenGiantFlower"))
            throw new IllegalStateException("Unaudited native giant flower generator");
        int fields = 0, result = -1;
        try {
            for (var field : generator.getClass()
                .getDeclaredFields()) if (!Modifier.isStatic(field.getModifiers())) {
                    fields++;
                    field.setAccessible(true);
                    if (field.getType() != int.class || !field.getName()
                        .equals("metadata")) throw new IllegalStateException("Native giant flower layout changed");
                    result = field.getInt(generator);
                }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot inspect native giant flower", e);
        }
        if (fields != 1 || result < 0 || result > 1)
            throw new IllegalStateException("Unsupported native giant flower variant");
        return result;
    }

    /** Caller owns the habitat/ceiling transaction and discards it entirely on false/exception. */
    public static boolean place(World world, NativeFeatureCapture.Access access, Random random,
        WorldGenAbstractTree tree, int x, int y, int z) {
        int variant = variant(tree), height = variant == 0 ? 9 : 7;
        if (y < access.minimumY() + 3 || y + height > access.maximumYExclusive()) return false;
        if (access.block(x, y - 1, z) != NativeBlockCatalogue.resolve(BopNativeBiomes.LONG_GRASS)
            .block() || access.metadata(x, y - 1, z) != 0) return false;
        // Native generate() does not check above-ground clearance. Protect the hollow
        // petals as well as occupied source cells from enclosing other terrain/trees.
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
            for (int dy = 0; dy < height; dy++) if (access.block(x + dx, y + dy, z + dz) != Blocks.air) return false;
        return BopWholeTree.place(world, access, random, tree, x, y, z);
    }
}
