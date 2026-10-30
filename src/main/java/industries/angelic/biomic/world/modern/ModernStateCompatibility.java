package industries.angelic.biomic.world.modern;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import industries.angelic.biomic.content.BiomicBlocks;
import industries.angelic.biomic.content.RuImportedDecorationBlocks;
import industries.angelic.biomic.content.RuImportedTreeBlocks;
import industries.angelic.biomic.world.feature.RuDecorationStateResolver;
import industries.angelic.biomic.world.feature.RuDecorationStateResolver.Resolved;
import industries.angelic.biomic.world.terrain.OrigenBlockStateResolver;

/** Bidirectional block-state boundary between modern feature code and physical 1.7 blocks. */
public final class ModernStateCompatibility {

    private static final String[] FLOWERS = { "aster", "blue_lupine", "pink_lupine", "purple_lupine", "red_lupine",
        "yellow_lupine", "daisy", "fireweed", "hyssop", "mallow", "waratah" };
    private static final String[] SHRUBS_A = { "cypress_shrub", "dead_pine_shrub", "flowering_shrub", "joshua_shrub",
        "lavender_wisteria_shrub", "sky_wisteria_shrub", "salmon_wisteria_shrub", "pine_shrub" };
    private static final String[] SHRUBS_B = { "redwood_shrub", "willow_shrub", "elephant_ear", "tassel",
        "eucalyptus_shrub" };
    private static final OrigenBlockStateResolver VANILLA = new OrigenBlockStateResolver();
    private static final Map<Block, String> IMPORTED = importedBlocks();
    private static final ConcurrentMap<String, Resolved> RESOLVED = new ConcurrentHashMap<String, Resolved>();

    private ModernStateCompatibility() {}

    public static Resolved resolve(String modernState) {
        Resolved cached = RESOLVED.get(modernState);
        if (cached != null) return cached;
        industries.angelic.biomic.content.alexscaves.AbyssalStateCodec.State abyssal = industries.angelic.biomic.content.alexscaves.AbyssalStateCodec
            .resolve(modernState);
        Resolved resolved = abyssal == null ? RuDecorationStateResolver.resolve(modernState)
            : RuDecorationStateResolver.imported(abyssal.block(), abyssal.metadata());
        Resolved raced = RESOLVED.putIfAbsent(modernState, resolved);
        return raced == null ? resolved : raced;
    }

    public static String modernState(Block block, int metadata) {
        String gardens = industries.angelic.biomic.content.gardens.GardensContent.encode(block, metadata);
        if (gardens != null) return gardens;
        String plain = industries.angelic.biomic.content.BiomicLegacyStates.encode(block, metadata);
        if (plain != null) return plain;
        String forlorn = industries.angelic.biomic.content.alexscaves.ForlornStateCodec.encode(block, metadata);
        if (forlorn != null) return forlorn;
        String primordial = industries.angelic.biomic.content.alexscaves.PrimordialStateCodec.encode(block, metadata);
        if (primordial != null) return primordial;
        String abyssal = industries.angelic.biomic.content.alexscaves.AbyssalStateCodec.encode(block, metadata);
        if (abyssal != null) return abyssal;
        String nativeFlora = industries.angelic.biomic.compat.flora.NativeFloraStates
            .encodeIfSupported(block, metadata);
        if (nativeFlora != null) return nativeFlora;
        if (block instanceof industries.angelic.biomic.content.block.BlockRuCaveModel cave)
            return cave.modernState(metadata);
        String imported = IMPORTED.get(block);
        if (imported != null) {
            if (block instanceof net.minecraft.block.BlockLog) {
                String axis = (metadata & 12) == 4 ? "x" : (metadata & 12) == 8 ? "z" : "y";
                String variant = imported.equals("regions_unexplored:brimwood_log_magma")
                    ? ",transition_block=" + ((metadata & 1) != 0)
                    : imported.equals("regions_unexplored:silver_birch_log") ? ",is_base=" + ((metadata & 1) != 0)
                        : imported.equals("regions_unexplored:bamboo_log") ? ",leaves=" + ((metadata & 1) != 0) : "";
                return imported + "[axis=" + axis + variant + "]";
            }
            if (industries.angelic.biomic.content.RuCaveModelData.handles(imported))
                return industries.angelic.biomic.content.RuCaveModelData.state(imported, metadata);
            if ("regions_unexplored:ashen_grass".equals(imported) || "regions_unexplored:ashen_dirt".equals(imported))
                return imported + "[smouldering=" + ((metadata & 1) != 0) + "]";
            if (block instanceof industries.angelic.biomic.content.block.BlockSaguaro) {
                return imported + "[cactus_shape="
                    + industries.angelic.biomic.content.block.BlockSaguaro.shape(metadata)
                    + ']';
            }
            if (industries.angelic.biomic.content.RuDecorationPaletteMetadata.isFlowerbed(imported)) {
                return industries.angelic.biomic.content.RuDecorationPaletteMetadata
                    .leafLitterState(imported, metadata);
            }
            if (block instanceof industries.angelic.biomic.content.block.BlockRuDoublePlant) {
                return industries.angelic.biomic.content.RuDecorationPaletteMetadata
                    .doublePlantState(imported, metadata);
            }
            return imported;
        }
        if (block == BiomicBlocks.CLOVER) return industries.angelic.biomic.content.RuDecorationPaletteMetadata
            .leafLitterState("regions_unexplored:clover", metadata);
        if (block == BiomicBlocks.CHALK) return "regions_unexplored:chalk";
        if (block == BiomicBlocks.CHALK_GRASS) return "regions_unexplored:chalk_grass_block";
        if (block == BiomicBlocks.ICICLE) return icicleState(metadata);
        if (block == BiomicBlocks.WISTERIA_LOG) return "regions_unexplored:wisteria_log";
        if (block == BiomicBlocks.WISTERIA_BRANCH) return "regions_unexplored:wisteria_branch";
        if (block == BiomicBlocks.WISTERIA_LEAVES) {
            return "regions_unexplored:" + wisteriaColor(metadata) + "_wisteria_leaves";
        }
        if (block == BiomicBlocks.WISTERIA_VINE) {
            return "regions_unexplored:" + wisteriaColor(metadata >> 1)
                + "_wisteria_vines[tip="
                + ((metadata & 1) != 0)
                + ']';
        }
        if (block == BiomicBlocks.PINE_LOG || block == BiomicBlocks.PINE_TRANSITION_LOG) {
            return "regions_unexplored:pine_log";
        }
        if (block == BiomicBlocks.STRIPPED_PINE_LOG) return "regions_unexplored:stripped_pine_log";
        if (block == BiomicBlocks.PINE_BRANCH) return "regions_unexplored:pine_branch";
        if (block == BiomicBlocks.PINE_LEAVES) return "regions_unexplored:pine_leaves";
        if (block == BiomicBlocks.DEAD_PINE_LEAVES) return "regions_unexplored:dead_pine_leaves";
        if (block == BiomicBlocks.PALM_LOG) return "regions_unexplored:palm_log";
        if (block == BiomicBlocks.PALM_LEAVES) return "regions_unexplored:palm_leaves";
        if (block == BiomicBlocks.PALM_BEARD) return "regions_unexplored:palm_beard";
        if (block == BiomicBlocks.FLOWERING_LEAVES) return "regions_unexplored:flowering_leaves";
        if (block == BiomicBlocks.REDWOOD_LOG) return "regions_unexplored:redwood_log";
        if (block == BiomicBlocks.REDWOOD_BRANCH) return "regions_unexplored:redwood_branch";
        if (block == BiomicBlocks.REDWOOD_LEAVES) return "regions_unexplored:redwood_leaves";
        if (block == BiomicBlocks.CYPRESS_LOG) return "regions_unexplored:cypress_log";
        if (block == BiomicBlocks.CYPRESS_BRANCH) return "regions_unexplored:cypress_branch";
        if (block == BiomicBlocks.CYPRESS_LEAVES) return "regions_unexplored:cypress_leaves";
        if (block == BiomicBlocks.WILLOW_LOG) return "regions_unexplored:willow_log";
        if (block == BiomicBlocks.WILLOW_LEAVES) return "regions_unexplored:willow_leaves";
        if (block == BiomicBlocks.JOSHUA_LOG) return "regions_unexplored:joshua_log";
        if (block == BiomicBlocks.JOSHUA_LEAVES) return "regions_unexplored:joshua_leaves";
        if (block == BiomicBlocks.JOSHUA_BEARD) return "regions_unexplored:joshua_beard";
        if (block == BiomicBlocks.EUCALYPTUS_LOG) return "regions_unexplored:eucalyptus_log";
        if (block == BiomicBlocks.EUCALYPTUS_BRANCH) return "regions_unexplored:eucalyptus_branch";
        if (block == BiomicBlocks.EUCALYPTUS_LEAVES) return "regions_unexplored:eucalyptus_leaves";
        if (block == BiomicBlocks.SPANISH_MOSS) return "regions_unexplored:spanish_moss";
        if (block == BiomicBlocks.GREEN_BIOSHROOM) return "regions_unexplored:green_bioshroom";
        if (block == BiomicBlocks.HANGING_ROOTS) return "minecraft:hanging_roots";
        if (block == BiomicBlocks.FLOWERING_LILY_PAD) return "regions_unexplored:flowering_lily_pad";
        if (block == BiomicBlocks.GIANT_LILY_PAD) return "regions_unexplored:giant_lily_pad[facing="
            + industries.angelic.biomic.content.RuDecorationPaletteMetadata.horizontalFacing(metadata)
            + ']';
        if (block == BiomicBlocks.SALMONBERRY_BUSH) return "regions_unexplored:salmonberry_bush";
        if (block == BiomicBlocks.RU_FLOWERS) {
            return "regions_unexplored:" + FLOWERS[Math.min(metadata & 15, FLOWERS.length - 1)];
        }
        if (block == BiomicBlocks.RU_TINTED_PLANTS) {
            return switch (metadata & 15) {
                case 1 -> "regions_unexplored:sandy_grass[is_red=false]";
                case 2 -> "regions_unexplored:small_desert_shrub";
                case 3 -> "regions_unexplored:sandy_grass[is_red=true]";
                default -> "regions_unexplored:grass_sprouts";
            };
        }
        if (block == BiomicBlocks.RU_SHRUBS_A) {
            return shrubState(SHRUBS_A, metadata);
        }
        if (block == BiomicBlocks.RU_SHRUBS_B) {
            return shrubState(SHRUBS_B, metadata);
        }
        return VANILLA.canonicalId(block, metadata);
    }

    /** Only vanilla upper double plants store their species in a neighbouring block. */
    public static boolean requiresBelowContext(Block block, int metadata) {
        return block == Blocks.double_plant && (metadata & 8) != 0;
    }

    /**
     * Reconstructs a modern state when the 1.7 representation stores part of its identity below.
     * A context-free conversion mislabels upper tall-grass and large-fern blocks as sunflowers.
     */
    public static String modernState(Block block, int metadata, Block below, int belowMetadata) {
        if (requiresBelowContext(block, metadata) && below == Blocks.double_plant) {
            return VANILLA.canonicalId(below, belowMetadata) + "[half=upper]";
        }
        return modernState(block, metadata);
    }

    private static String shrubState(String[] names, int metadata) {
        int variant = Math.min(metadata & 7, names.length - 1);
        return "regions_unexplored:" + names[variant] + "[half=" + ((metadata & 8) == 0 ? "lower" : "upper") + ']';
    }

    private static String wisteriaColor(int metadata) {
        return switch (Math.min(metadata & 3, 2)) {
            case 1 -> "lavender";
            case 2 -> "salmon";
            default -> "sky";
        };
    }

    private static String icicleState(int metadata) {
        int state = metadata & 15;
        String thickness = switch (state % 5) {
            case 1 -> "frustum";
            case 2 -> "middle";
            case 3 -> "base";
            case 4 -> "tip_merge";
            default -> "tip";
        };
        return "regions_unexplored:icicle[thickness=" + thickness
            + ",vertical_direction="
            + (state >= 5 ? "up" : "down")
            + ",waterlogged=false]";
    }

    private static Map<Block, String> importedBlocks() {
        Map<Block, String> result = new IdentityHashMap<>();
        for (Map.Entry<String, Block> entry : RuImportedDecorationBlocks.blocks()
            .entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<String, Block> entry : RuImportedTreeBlocks.blocks()
            .entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }
}
