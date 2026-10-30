package industries.angelic.biomic.world.feature;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.init.Blocks;

import ganymedes01.etfuturum.ModBlocks;
import industries.angelic.biomic.content.BiomicBlocks;
import industries.angelic.biomic.content.RuImportedTreeBlocks;
import industries.angelic.biomic.content.block.BlockRuBranch;

/** Maps captured 1.21.1 RU states into explicit Forge 1.7.10 block/metadata states. */
final class RuFeatureBlockResolver {

    private static final Set<String> IMPORTED_TREE_STATES = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "regions_unexplored:alpha_leaves",
                "regions_unexplored:alpha_log",
                "regions_unexplored:apple_oak_leaves",
                "regions_unexplored:ashen_leaves",
                "regions_unexplored:ashen_log",
                "regions_unexplored:bamboo_leaves",
                "regions_unexplored:bamboo_log",
                "regions_unexplored:baobab_branch",
                "regions_unexplored:baobab_leaves",
                "regions_unexplored:baobab_log",
                "regions_unexplored:birch_branch",
                "regions_unexplored:blackwood_branch",
                "regions_unexplored:blackwood_leaves",
                "regions_unexplored:blackwood_log",
                "regions_unexplored:blue_bioshroom_block",
                "regions_unexplored:blue_bioshroom_stem",
                "regions_unexplored:blue_magnolia_leaves",
                "regions_unexplored:dead_branch",
                "regions_unexplored:dead_leaves",
                "regions_unexplored:dead_log",
                "regions_unexplored:golden_larch_leaves",
                "regions_unexplored:kapok_branch",
                "regions_unexplored:kapok_leaves",
                "regions_unexplored:kapok_log",
                "regions_unexplored:kapok_vines",
                "regions_unexplored:kapok_vines_plant",
                "regions_unexplored:larch_branch",
                "regions_unexplored:larch_leaves",
                "regions_unexplored:larch_log",
                "regions_unexplored:magnolia_branch",
                "regions_unexplored:magnolia_leaves",
                "regions_unexplored:magnolia_log",
                "regions_unexplored:maple_branch",
                "regions_unexplored:maple_leaf_litter",
                "regions_unexplored:maple_leaves",
                "regions_unexplored:maple_log",
                "regions_unexplored:oak_branch",
                "regions_unexplored:orange_maple_leaf_litter",
                "regions_unexplored:orange_maple_leaves",
                "regions_unexplored:pink_bioshroom_block",
                "regions_unexplored:pink_bioshroom_stem",
                "regions_unexplored:pink_magnolia_leaves",
                "regions_unexplored:red_maple_leaf_litter",
                "regions_unexplored:red_maple_leaves",
                "regions_unexplored:saguaro_cactus",
                "regions_unexplored:saguaro_cactus_flower",
                "regions_unexplored:silver_birch_branch",
                "regions_unexplored:silver_birch_leaf_litter",
                "regions_unexplored:silver_birch_leaves",
                "regions_unexplored:silver_birch_log",
                "regions_unexplored:small_oak_leaves",
                "regions_unexplored:small_oak_log",
                "regions_unexplored:socotra_branch",
                "regions_unexplored:socotra_leaves",
                "regions_unexplored:socotra_log",
                "regions_unexplored:white_magnolia_leaves")));

    enum PineLogVariant {
        BARK,
        STRIPPED,
        TRANSITION
    }

    static final class Resolved {

        private final Block block;
        private final int metadata;

        private Resolved(Block block, int metadata) {
            this.block = block;
            this.metadata = metadata;
        }

        Block getBlock() {
            return block;
        }

        int getMetadata() {
            return metadata;
        }

    }

    private RuFeatureBlockResolver() {}

    static Resolved resolve(String encoded, int transform) {
        ParsedState state = ParsedState.parse(encoded);
        String name = state.name;
        if ("minecraft:dirt".equals(name)) return ground(Blocks.dirt, 0);
        if ("minecraft:podzol".equals(name)) return ground(Blocks.dirt, 2);
        if ("minecraft:moss_block".equals(name)) return ground(ModBlocks.MOSS_BLOCK.get(), 0);
        if ("minecraft:moss_carpet".equals(name)) return plant(ModBlocks.MOSS_CARPET.get(), 0);
        if ("minecraft:hanging_roots".equals(name)) return plant(BiomicBlocks.HANGING_ROOTS, 0);
        if ("minecraft:oak_log".equals(name)) return log(Blocks.log, axisMetadata(state, transform));
        if ("minecraft:spruce_log".equals(name)) return log(Blocks.log, 1 | axisMetadata(state, transform));
        if ("minecraft:birch_log".equals(name)) return log(Blocks.log, 2 | axisMetadata(state, transform));
        if ("minecraft:jungle_log".equals(name)) return log(Blocks.log, 3 | axisMetadata(state, transform));
        if ("minecraft:acacia_log".equals(name)) return log(Blocks.log2, axisMetadata(state, transform));
        if ("minecraft:dark_oak_log".equals(name)) return log(Blocks.log2, 1 | axisMetadata(state, transform));
        if ("minecraft:oak_leaves".equals(name)) return leaves(Blocks.leaves, 4);
        if ("minecraft:spruce_leaves".equals(name)) return leaves(Blocks.leaves, 5);
        if ("minecraft:birch_leaves".equals(name)) return leaves(Blocks.leaves, 6);
        if ("minecraft:jungle_leaves".equals(name)) return leaves(Blocks.leaves, 7);
        if ("minecraft:acacia_leaves".equals(name)) return leaves(Blocks.leaves2, 4);
        if ("minecraft:dark_oak_leaves".equals(name)) return leaves(Blocks.leaves2, 5);
        if ("minecraft:bee_nest".equals(name)) {
            Block beeNest = ModBlocks.BEE_NEST.get();
            return log(beeNest == null ? Blocks.log : beeNest, horizontalFacingMetadata(state, transform));
        }
        if ("minecraft:brown_mushroom".equals(name)) return plant(Blocks.brown_mushroom, 0);
        if ("minecraft:shroomlight".equals(name)) {
            Block shroomlight = ModBlocks.SHROOMLIGHT.get();
            return log(shroomlight == null ? Blocks.glowstone : shroomlight, 0);
        }
        if ("minecraft:snow".equals(name)) return plant(Blocks.snow_layer, snowMetadata(state));
        if ("regions_unexplored:flowering_leaves".equals(name)) {
            return leaves(BiomicBlocks.FLOWERING_LEAVES, 4);
        }
        if ("minecraft:vine".equals(name)) return plant(Blocks.vine, vineMetadata(state, transform));

        if ("regions_unexplored:wisteria_log".equals(name)) {
            return log(BiomicBlocks.WISTERIA_LOG, axisMetadata(state, transform));
        }
        if ("regions_unexplored:wisteria_branch".equals(name)) {
            return log(BiomicBlocks.WISTERIA_BRANCH, facingMetadata(state, transform));
        }
        if (name.endsWith("_wisteria_leaves")) {
            return leaves(BiomicBlocks.WISTERIA_LEAVES, 4 | wisteriaColor(name));
        }
        if (name.endsWith("_wisteria_vines")) {
            return plant(BiomicBlocks.WISTERIA_VINE, wisteriaColor(name) * 2 + booleanProperty(state, "tip"));
        }

        if ("regions_unexplored:pine_log".equals(name)) {
            Block pine = switch (pineLogVariant(state)) {
                case STRIPPED -> BiomicBlocks.STRIPPED_PINE_LOG;
                case TRANSITION -> BiomicBlocks.PINE_TRANSITION_LOG;
                case BARK -> BiomicBlocks.PINE_LOG;
            };
            return log(pine, axisMetadata(state, transform));
        }
        if ("regions_unexplored:pine_branch".equals(name)) {
            return log(BiomicBlocks.PINE_BRANCH, facingMetadata(state, transform));
        }
        if ("regions_unexplored:pine_leaves".equals(name)) return leaves(BiomicBlocks.PINE_LEAVES, 4);
        if ("regions_unexplored:dead_pine_leaves".equals(name)) {
            return leaves(BiomicBlocks.DEAD_PINE_LEAVES, 4);
        }

        if ("regions_unexplored:palm_log".equals(name)) {
            return log(BiomicBlocks.PALM_LOG, axisMetadata(state, transform));
        }
        if ("regions_unexplored:palm_leaves".equals(name)) return leaves(BiomicBlocks.PALM_LEAVES, 4);
        if ("regions_unexplored:palm_beard".equals(name)) {
            return plant(BiomicBlocks.PALM_BEARD, facingMetadata(state, transform));
        }

        if ("regions_unexplored:redwood_log".equals(name)) {
            return log(BiomicBlocks.REDWOOD_LOG, axisMetadata(state, transform));
        }
        if ("regions_unexplored:redwood_branch".equals(name)) {
            return log(BiomicBlocks.REDWOOD_BRANCH, facingMetadata(state, transform));
        }
        if ("regions_unexplored:redwood_leaves".equals(name)) return leaves(BiomicBlocks.REDWOOD_LEAVES, 4);

        if ("regions_unexplored:cypress_log".equals(name)) {
            return log(BiomicBlocks.CYPRESS_LOG, axisMetadata(state, transform));
        }
        if ("regions_unexplored:cypress_branch".equals(name)) {
            return log(BiomicBlocks.CYPRESS_BRANCH, facingMetadata(state, transform));
        }
        if ("regions_unexplored:cypress_leaves".equals(name)) return leaves(BiomicBlocks.CYPRESS_LEAVES, 4);

        if ("regions_unexplored:willow_log".equals(name)) {
            return log(BiomicBlocks.WILLOW_LOG, axisMetadata(state, transform));
        }
        if ("regions_unexplored:willow_leaves".equals(name)) return leaves(BiomicBlocks.WILLOW_LEAVES, 4);
        if ("regions_unexplored:spanish_moss_plant".equals(name)) return plant(BiomicBlocks.SPANISH_MOSS, 0);
        if ("regions_unexplored:spanish_moss".equals(name)) return plant(BiomicBlocks.SPANISH_MOSS, 1);

        if ("regions_unexplored:joshua_log".equals(name)) {
            return log(BiomicBlocks.JOSHUA_LOG, axisMetadata(state, transform));
        }
        if ("regions_unexplored:joshua_leaves".equals(name)) {
            if ("upper".equals(state.properties.get("half"))) return plant(BiomicBlocks.JOSHUA_LEAVES, 1);
            return plant(BiomicBlocks.JOSHUA_LEAVES, booleanProperty(state, "natural") == 1 ? 0 : 2);
        }
        if ("regions_unexplored:joshua_beard".equals(name)) {
            return plant(BiomicBlocks.JOSHUA_BEARD, facingMetadata(state, transform));
        }
        if ("regions_unexplored:green_bioshroom".equals(name)) return plant(BiomicBlocks.GREEN_BIOSHROOM, 0);

        if ("regions_unexplored:eucalyptus_log".equals(name)) {
            return log(BiomicBlocks.EUCALYPTUS_LOG, axisMetadata(state, transform));
        }
        if ("regions_unexplored:eucalyptus_branch".equals(name)) {
            return log(BiomicBlocks.EUCALYPTUS_BRANCH, facingMetadata(state, transform));
        }
        if ("regions_unexplored:eucalyptus_leaves".equals(name)) {
            return leaves(BiomicBlocks.EUCALYPTUS_LEAVES, 4);
        }
        if ("regions_unexplored:stripped_pine_log".equals(name)) {
            return log(BiomicBlocks.STRIPPED_PINE_LOG, axisMetadata(state, transform));
        }
        if ("regions_unexplored:small_oak_leaves".equals(name)) return leaves(Blocks.leaves, 4);

        Block imported = RuImportedTreeBlocks.get(name);
        if (imported != null) {
            if (imported instanceof industries.angelic.biomic.content.block.BlockSaguaro) {
                return log(
                    imported,
                    industries.angelic.biomic.content.block.BlockSaguaro
                        .metadata(state.properties.getOrDefault("cactus_shape", "up_down"), transform));
            }
            if (imported instanceof BlockLeaves) {
                int variant = 0;
                if ("regions_unexplored:apple_oak_leaves".equals(name)) {
                    int age = integerProperty(state, "age", 0);
                    variant = age < 2 ? 0 : age < 4 ? 1 : 2;
                }
                return leaves(imported, 4 | variant);
            }
            if (imported instanceof BlockLog) {
                int variant = 0;
                if ("regions_unexplored:bamboo_log".equals(name)) variant = booleanProperty(state, "leaves");
                if ("regions_unexplored:silver_birch_log".equals(name)) {
                    variant = booleanProperty(state, "is_base");
                }
                return log(imported, variant | axisMetadata(state, transform));
            }
            if (imported instanceof BlockRuBranch) return log(imported, facingMetadata(state, transform));
            if (name.endsWith("_leaf_litter")) return plant(
                imported,
                industries.angelic.biomic.content.RuDecorationPaletteMetadata
                    .leafLitterMetadata(integerProperty(state, "flower_amount", 1), facingMetadata(state, transform)));
            if (name.endsWith("_vines") || name.endsWith("_vines_plant") || name.endsWith("_flower")) {
                return plant(imported, 0);
            }
            return log(imported, 0);
        }

        throw new IllegalArgumentException("No Forge 1.7.10 mapping for pinned RU state " + encoded);
    }

    static boolean supports(String encoded) {
        String name = ParsedState.parse(encoded).name;
        return "minecraft:dirt".equals(name) || "minecraft:podzol".equals(name)
            || "minecraft:moss_block".equals(name)
            || "minecraft:moss_carpet".equals(name)
            || "minecraft:hanging_roots".equals(name)
            || "minecraft:oak_log".equals(name)
            || "minecraft:spruce_log".equals(name)
            || "minecraft:birch_log".equals(name)
            || "minecraft:jungle_log".equals(name)
            || "minecraft:acacia_log".equals(name)
            || "minecraft:dark_oak_log".equals(name)
            || "minecraft:oak_leaves".equals(name)
            || "minecraft:spruce_leaves".equals(name)
            || "minecraft:birch_leaves".equals(name)
            || "minecraft:jungle_leaves".equals(name)
            || "minecraft:acacia_leaves".equals(name)
            || "minecraft:dark_oak_leaves".equals(name)
            || "minecraft:bee_nest".equals(name)
            || "minecraft:brown_mushroom".equals(name)
            || "minecraft:shroomlight".equals(name)
            || "minecraft:snow".equals(name)
            || "minecraft:vine".equals(name)
            || "regions_unexplored:flowering_leaves".equals(name)
            || "regions_unexplored:wisteria_log".equals(name)
            || "regions_unexplored:wisteria_branch".equals(name)
            || name.endsWith("_wisteria_leaves")
            || name.endsWith("_wisteria_vines")
            || "regions_unexplored:pine_log".equals(name)
            || "regions_unexplored:pine_branch".equals(name)
            || "regions_unexplored:pine_leaves".equals(name)
            || "regions_unexplored:dead_pine_leaves".equals(name)
            || "regions_unexplored:palm_log".equals(name)
            || "regions_unexplored:palm_leaves".equals(name)
            || "regions_unexplored:palm_beard".equals(name)
            || "regions_unexplored:redwood_log".equals(name)
            || "regions_unexplored:redwood_branch".equals(name)
            || "regions_unexplored:redwood_leaves".equals(name)
            || "regions_unexplored:cypress_log".equals(name)
            || "regions_unexplored:cypress_branch".equals(name)
            || "regions_unexplored:cypress_leaves".equals(name)
            || "regions_unexplored:willow_log".equals(name)
            || "regions_unexplored:willow_leaves".equals(name)
            || "regions_unexplored:spanish_moss_plant".equals(name)
            || "regions_unexplored:spanish_moss".equals(name)
            || "regions_unexplored:joshua_log".equals(name)
            || "regions_unexplored:joshua_leaves".equals(name)
            || "regions_unexplored:joshua_beard".equals(name)
            || "regions_unexplored:green_bioshroom".equals(name)
            || "regions_unexplored:eucalyptus_log".equals(name)
            || "regions_unexplored:eucalyptus_branch".equals(name)
            || "regions_unexplored:eucalyptus_leaves".equals(name)
            || "regions_unexplored:stripped_pine_log".equals(name)
            || IMPORTED_TREE_STATES.contains(name);
    }

    private static PineLogVariant pineLogVariant(ParsedState state) {
        if (booleanProperty(state, "is_stripped") != 0) return PineLogVariant.STRIPPED;
        if (booleanProperty(state, "transition_block") != 0) return PineLogVariant.TRANSITION;
        return PineLogVariant.BARK;
    }

    private static Resolved ground(Block block, int metadata) {
        return new Resolved(block, metadata);
    }

    private static Resolved log(Block block, int metadata) {
        return new Resolved(block, metadata);
    }

    private static Resolved leaves(Block block, int metadata) {
        return new Resolved(block, metadata);
    }

    private static Resolved plant(Block block, int metadata) {
        return new Resolved(block, metadata);
    }

    private static int wisteriaColor(String name) {
        if (name.contains("blue_")) return 0;
        if (name.contains("lavender_")) return 1;
        if (name.contains("salmon_")) return 2;
        if (name.contains("sky_")) return 0;
        throw new IllegalArgumentException("Unknown wisteria color state " + name);
    }

    private static int booleanProperty(ParsedState state, String property) {
        return "true".equals(state.properties.get(property)) ? 1 : 0;
    }

    private static int integerProperty(ParsedState state, String property, int fallback) {
        String value = state.properties.get(property);
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int snowMetadata(ParsedState state) {
        return Math.max(0, Math.min(7, integerProperty(state, "layers", 1) - 1));
    }

    private static int horizontalFacingMetadata(ParsedState state, int transform) {
        String facing = transformDirection(state.properties.get("facing"), transform);
        if ("west".equals(facing)) return 1;
        if ("north".equals(facing)) return 2;
        if ("east".equals(facing)) return 3;
        return 0;
    }

    private static int axisMetadata(ParsedState state, int transform) {
        String axis = state.properties.get("axis");
        if (axis == null || "y".equals(axis)) return 0;
        boolean swap = swapsHorizontalAxes(transform);
        if (swap) axis = "x".equals(axis) ? "z" : "x";
        return "x".equals(axis) ? 4 : 8;
    }

    private static int facingMetadata(ParsedState state, int transform) {
        String facing = transformDirection(state.properties.get("facing"), transform);
        if ("east".equals(facing)) return 1;
        if ("south".equals(facing)) return 2;
        if ("west".equals(facing)) return 3;
        return 0;
    }

    private static int vineMetadata(ParsedState state, int transform) {
        int metadata = 0;
        for (String direction : new String[] { "north", "south", "east", "west" }) {
            if (!"true".equals(state.properties.get(direction))) continue;
            String transformed = transformDirection(direction, transform);
            if ("south".equals(transformed)) metadata |= 1;
            else if ("west".equals(transformed)) metadata |= 2;
            else if ("north".equals(transformed)) metadata |= 4;
            else if ("east".equals(transformed)) metadata |= 8;
        }
        return metadata == 0 ? 1 : metadata;
    }

    static int transformedX(int x, int z, int transform) {
        switch (transform & 7) {
            case 1:
                return -z;
            case 2:
                return -x;
            case 3:
                return z;
            case 4:
                return -x;
            case 5:
                return -z;
            case 6:
                return x;
            case 7:
                return z;
            default:
                return x;
        }
    }

    static int transformedZ(int x, int z, int transform) {
        switch (transform & 7) {
            case 1:
                return x;
            case 2:
                return -z;
            case 3:
                return -x;
            case 4:
                return z;
            case 5:
                return -x;
            case 6:
                return -z;
            case 7:
                return x;
            default:
                return z;
        }
    }

    private static boolean swapsHorizontalAxes(int transform) {
        int normalized = transform & 7;
        return normalized == 1 || normalized == 3 || normalized == 5 || normalized == 7;
    }

    static String transformDirection(String direction, int transform) {
        if (direction == null) return null;
        int x = "east".equals(direction) ? 1 : "west".equals(direction) ? -1 : 0;
        int z = "south".equals(direction) ? 1 : "north".equals(direction) ? -1 : 0;
        int transformedX = transformedX(x, z, transform);
        int transformedZ = transformedZ(x, z, transform);
        if (transformedX > 0) return "east";
        if (transformedX < 0) return "west";
        return transformedZ > 0 ? "south" : "north";
    }

    private static final class ParsedState {

        private final String name;
        private final Map<String, String> properties;

        private ParsedState(String name, Map<String, String> properties) {
            this.name = name;
            this.properties = properties;
        }

        private static ParsedState parse(String encoded) {
            int open = encoded.indexOf('[');
            if (open < 0) return new ParsedState(encoded, Collections.<String, String>emptyMap());
            if (!encoded.endsWith("]")) throw new IllegalArgumentException("Malformed RU block state " + encoded);
            Map<String, String> properties = new LinkedHashMap<>();
            String body = encoded.substring(open + 1, encoded.length() - 1);
            if (!body.isEmpty()) {
                for (String assignment : body.split(",")) {
                    int equals = assignment.indexOf('=');
                    if (equals <= 0) throw new IllegalArgumentException("Malformed RU block state " + encoded);
                    properties.put(assignment.substring(0, equals), assignment.substring(equals + 1));
                }
            }
            return new ParsedState(encoded.substring(0, open), properties);
        }
    }
}
