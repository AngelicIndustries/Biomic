package industries.angelic.biomic.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.gen.feature.WorldGenerator;

/**
 * Plants Biomic places for other mods in its habitats, after the flowers, by kind. The ecology palettes' "food" section
 * names them per habitat (EcologyOverlays); the mod that owns them registers resolvers that say what a name grows as.
 * Biomic keeps the placement: where the patches drift, where the groves stand, which trunks carry vines and nests, all
 * from hashes of the world seed and the position, so the world is the same in any generation order.
 * <ul>
 * <li>crops: patches drifting across chunk borders on a 40-block grid, thinning to their rims, grass between
 * plants;</li>
 * <li>bushes: the same, smaller and sparser (48-block grid);</li>
 * <li>shoots: the same, small and close (24-block grid), mostly young;</li>
 * <li>trees: groves of one tree on a 32-block grid (14 in orchards), each tree whole in its chunk;</li>
 * <li>vines: hanging from tree trunks, two to four blocks;</li>
 * <li>nests: one on a trunk side under the canopy, in about one chunk of eight;</li>
 * <li>Nether plants: patches on a 40-block grid over every cave floor of a column in Tartarus's biomes, as the
 * palettes' "nether" lists name them per biome;</li>
 * <li>Nether trees: about one per six chunks of the biomes whose list names them, on the lowest floor with room.</li>
 * </ul>
 * Resolvers are asked in registration order; the first answer that is not null counts, and a name nobody answers is
 * skipped. Register at init or postInit.
 */
public final class PlantLayer {

    /** A plant of drifting patches. */
    public interface Patch {

        /** The block, or null when it is not installed (the plant is skipped). */
        Block block();

        /** The metadata a plant takes for a roll in [0, 1) hashed from its position (its stage). */
        int metadata(double roll);

        /** Whether it grows on sand as well as grass and dirt. */
        default boolean sand() {
            return false;
        }

        /** Whether its patches are fairy rings. */
        default boolean ring() {
            return false;
        }

        /** Whether it roots on this ground, beyond its kind's own rule (nether ground for Nether plants). */
        default boolean grows(Block ground) {
            return true;
        }
    }

    /** A grove tree. */
    public interface Tree {

        /** Grows the tree at a position like a sapling generator, or null when it is not installed. */
        WorldGenerator generator();

        /** Whether it roots in sand as well as grass and dirt. */
        default boolean sand() {
            return false;
        }

        /**
         * The metadata a block the tree writes takes, for a roll in [0, 1) hashed from its position (fruit ripening).
         */
        default int ripen(Block block, int metadata, double roll) {
            return metadata;
        }
    }

    /** A nest on a trunk side: a block with a tile entity. */
    public interface Nest {

        boolean available();

        /** The nest's block state facing {@code facing} (2 to 5). */
        String state(int facing);

        /** Its tile entity's data; {@code hash} varies per nest. */
        NBTTagCompound payload(int x, int y, int z, int facing, long hash);
    }

    private static final List<Function<String, Patch>> CROPS = new CopyOnWriteArrayList<>();
    private static final List<Function<String, Patch>> BUSHES = new CopyOnWriteArrayList<>();
    private static final List<Function<String, Patch>> SHOOTS = new CopyOnWriteArrayList<>();
    private static final List<Function<String, Tree>> TREES = new CopyOnWriteArrayList<>();
    private static final List<Function<String, Block>> VINES = new CopyOnWriteArrayList<>();
    private static final List<Function<String, Patch>> NETHER_PLANTS = new CopyOnWriteArrayList<>();
    private static final List<Function<String, Tree>> NETHER_TREES = new CopyOnWriteArrayList<>();
    private static volatile Nest nest;

    private PlantLayer() {}

    public static void crops(Function<String, Patch> resolver) {
        CROPS.add(resolver);
    }

    public static void bushes(Function<String, Patch> resolver) {
        BUSHES.add(resolver);
    }

    public static void shoots(Function<String, Patch> resolver) {
        SHOOTS.add(resolver);
    }

    public static void trees(Function<String, Tree> resolver) {
        TREES.add(resolver);
    }

    /** Vine blocks by name; a vine takes vanilla's face metadata for the trunk it hangs from. */
    public static void vines(Function<String, Block> resolver) {
        VINES.add(resolver);
    }

    public static void netherPlants(Function<String, Patch> resolver) {
        NETHER_PLANTS.add(resolver);
    }

    /** Trees of Tartarus's biomes, rooted on nether ground. */
    public static void netherTrees(Function<String, Tree> resolver) {
        NETHER_TREES.add(resolver);
    }

    /** The nest every habitat with nests gets; one per world, the last registered. */
    public static void nest(Nest provider) {
        nest = provider;
    }

    public static Patch crop(String name) {
        return resolve(CROPS, name);
    }

    public static Patch bush(String name) {
        return resolve(BUSHES, name);
    }

    public static Patch shoot(String name) {
        return resolve(SHOOTS, name);
    }

    public static Tree tree(String name) {
        return resolve(TREES, name);
    }

    public static Block vine(String name) {
        return resolve(VINES, name);
    }

    public static Patch netherPlant(String name) {
        return resolve(NETHER_PLANTS, name);
    }

    public static Tree netherTree(String name) {
        return resolve(NETHER_TREES, name);
    }

    public static Nest nest() {
        return nest;
    }

    private static <T> T resolve(List<Function<String, T>> resolvers, String name) {
        for (Function<String, T> resolver : resolvers) {
            T answer = resolver.apply(name);
            if (answer != null) return answer;
        }
        return null;
    }
}
