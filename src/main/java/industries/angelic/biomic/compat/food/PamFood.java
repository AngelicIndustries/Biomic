package industries.angelic.biomic.compat.food;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.gen.feature.WorldGenerator;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameRegistry;
import industries.angelic.biomic.Biomic;

/**
 * HarvestCraft for the Regions Unexplored orchard's apple trees (HarvestCraftOrchards): its blocks and the fruit-tree
 * generators of its saplings, looked up by registry name. Optional; its classes are only touched when installed.
 */
public final class PamFood {

    public static final String MOD = "harvestcraft";
    /** The oldest HarvestCraft build the orchard is written against; later GTNH builds keep its blocks and fields. */
    public static final String SUPPORTED = "1.3.13-GTNH";

    private static final Map<String, Optional<WorldGenerator>> TREES = new ConcurrentHashMap<>();
    private static volatile Boolean installed;

    private PamFood() {}

    /** HarvestCraft at the version the orchard is written against. */
    public static boolean installed() {
        Boolean known = installed;
        if (known == null) {
            ModContainer mod = Loader.instance()
                .getIndexedModList()
                .get(MOD);
            known = mod != null && new cpw.mods.fml.common.versioning.DefaultArtifactVersion(mod.getVersion())
                .compareTo(new cpw.mods.fml.common.versioning.DefaultArtifactVersion(SUPPORTED)) >= 0;
            if (mod != null && !known) Biomic.LOG.warn(
                "HarvestCraft {} is older than {}; the orchard keeps Regions Unexplored's oaks",
                mod.getVersion(),
                SUPPORTED);
            installed = known;
        }
        return known;
    }

    public static Block block(String mod, String name) {
        if (!Loader.isModLoaded(mod)) return null;
        Block block = GameRegistry.findBlock(mod, name);
        return block == null || block == Blocks.air ? null : block;
    }

    /** The generator HarvestCraft's own sapling of this fruit grows (apple: pamappleSapling), or null. */
    public static WorldGenerator fruitTree(String fruit) {
        if (!installed()) return null;
        return TREES.computeIfAbsent(fruit, key -> {
            Block sapling = block(MOD, "pam" + key + "Sapling");
            if (sapling == null) return Optional.empty();
            try {
                Field field = sapling.getClass()
                    .getDeclaredField("treeGen");
                field.setAccessible(true);
                return Optional.ofNullable((WorldGenerator) field.get(sapling));
            } catch (ReflectiveOperationException | RuntimeException error) {
                Biomic.LOG.warn("No HarvestCraft tree generator for {}: {}", key, error.toString());
                return Optional.empty();
            }
        })
            .orElse(null);
    }
}
