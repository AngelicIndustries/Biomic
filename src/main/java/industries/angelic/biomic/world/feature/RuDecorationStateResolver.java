package industries.angelic.biomic.world.feature;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import industries.angelic.biomic.content.BiomicBlocks;
import industries.angelic.biomic.content.RuImportedDecorationBlocks;
import industries.angelic.biomic.content.block.BlockRuDoublePlant;
import industries.angelic.biomic.world.terrain.OrigenBlockStateResolver;
import industries.angelic.biomic.world.terrain.OrigenBlockStateResolver.ResolvedBlock;

/** One shared modern-state compatibility layer for generated RU decoration plans. */
public final class RuDecorationStateResolver {

    public static final class Resolved {

        private final Block block;
        private final int metadata;

        Resolved(Block block, int metadata) {
            this.block = block;
            this.metadata = metadata & 15;
        }

        public Block getBlock() {
            return block;
        }

        public int getMetadata() {
            return metadata;
        }

    }

    private static final OrigenBlockStateResolver VANILLA = new OrigenBlockStateResolver();
    private static final Map<String, Integer> FLOWERS = flowerMetadata();
    private static final Map<String, Integer> SHRUBS_A = shrubMetadata(
        "cypress_shrub",
        "dead_pine_shrub",
        "flowering_shrub",
        "joshua_shrub",
        "lavender_wisteria_shrub",
        "sky_wisteria_shrub",
        "salmon_wisteria_shrub",
        "pine_shrub");
    private static final Map<String, Integer> SHRUBS_B = shrubMetadata(
        "redwood_shrub",
        "willow_shrub",
        "elephant_ear",
        "tassel",
        "eucalyptus_shrub");

    private RuDecorationStateResolver() {}

    /** Strict external codec bridge for hosted blocks that are not part of the RU manifest. */
    public static Resolved imported(Block block, int metadata) {
        return new Resolved(block, metadata);
    }

    public static Resolved resolve(String encoded) {
        ParsedState state = ParsedState.parse(encoded);
        String name = state.name;
        if (name.startsWith("minecraft:")
            || industries.angelic.biomic.compat.flora.NativeFloraStates.handles(encoded)) {
            ResolvedBlock vanilla = VANILLA.resolve(encoded);
            return new Resolved(vanilla.block(), vanilla.metadata());
        }

        if ("regions_unexplored:flowering_lily_pad".equals(name)) {
            return new Resolved(BiomicBlocks.FLOWERING_LILY_PAD, 0);
        }
        if ("regions_unexplored:giant_lily_pad".equals(name)) {
            int facing = industries.angelic.biomic.content.RuDecorationPaletteMetadata
                .horizontalMetadata(state.properties.get("facing"));
            return new Resolved(BiomicBlocks.GIANT_LILY_PAD, facing);
        }
        if ("regions_unexplored:clover".equals(name) || "biomic:clover".equals(name)) {
            int facing = switch (state.properties.getOrDefault("facing", "north")) {
                case "east" -> 1;
                case "south" -> 2;
                case "west" -> 3;
                default -> 0;
            };
            return new Resolved(
                BiomicBlocks.CLOVER,
                industries.angelic.biomic.content.RuDecorationPaletteMetadata
                    .leafLitterMetadata(integer(state, "flower_amount", 1), facing));
        }
        // Hosted RU features read settled Origen terrain too. Its native backports (for example
        // all-face mushroom flesh) already have a strict mapping; do not mistake them for RU assets.
        if (name.startsWith("biomic:")) {
            ResolvedBlock nativeBlock = VANILLA.resolve(encoded);
            return new Resolved(nativeBlock.block(), nativeBlock.metadata());
        }
        if ("regions_unexplored:alpha_grass_block".equals(name)) {
            return new Resolved(Blocks.grass, 0);
        }
        if ("regions_unexplored:icicle".equals(name)) {
            int thickness = switch (state.properties.getOrDefault("thickness", "tip")) {
                case "frustum" -> 1;
                case "middle" -> 2;
                case "base" -> 3;
                case "tip_merge" -> 4;
                default -> 0;
            };
            int metadata = thickness + ("up".equals(state.properties.get("vertical_direction")) ? 5 : 0);
            return new Resolved(BiomicBlocks.ICICLE, metadata);
        }
        if ("regions_unexplored:chalk".equals(name)) {
            return new Resolved(BiomicBlocks.CHALK, 0);
        }
        if ("regions_unexplored:chalk_grass_block".equals(name)) {
            return new Resolved(BiomicBlocks.CHALK_GRASS, 0);
        }
        if ("regions_unexplored:salmonberry_bush".equals(name)) {
            return new Resolved(BiomicBlocks.SALMONBERRY_BUSH, 0);
        }
        // Configured tree placement asks whether its matching sapling can survive at the origin;
        // worldgen never leaves this predicate-only state behind.
        if (name.startsWith("regions_unexplored:") && name.endsWith("_sapling")) {
            return new Resolved(Blocks.sapling, 0);
        }
        String path = path(name);
        Integer flower = FLOWERS.get(path);
        if (flower != null) return new Resolved(BiomicBlocks.RU_FLOWERS, flower.intValue());
        if ("grass_sprouts".equals(path)) return new Resolved(BiomicBlocks.RU_TINTED_PLANTS, 0);
        if ("sandy_grass".equals(path))
            return new Resolved(BiomicBlocks.RU_TINTED_PLANTS, "true".equals(state.properties.get("is_red")) ? 3 : 1);
        if ("small_desert_shrub".equals(path)) {
            return new Resolved(BiomicBlocks.RU_TINTED_PLANTS, 2);
        }
        Integer shrubA = SHRUBS_A.get(path);
        if (shrubA != null) return new Resolved(
            BiomicBlocks.RU_SHRUBS_A,
            shrubA.intValue() | ("upper".equals(state.properties.get("half")) ? 8 : 0));
        Integer shrubB = SHRUBS_B.get(path);
        if (shrubB != null) return new Resolved(
            BiomicBlocks.RU_SHRUBS_B,
            shrubB.intValue() | ("upper".equals(state.properties.get("half")) ? 8 : 0));

        if (RuFeatureBlockResolver.supports(encoded)) {
            RuFeatureBlockResolver.Resolved tree = RuFeatureBlockResolver.resolve(encoded, 0);
            return new Resolved(tree.getBlock(), tree.getMetadata());
        }

        Block imported = RuImportedDecorationBlocks.get(name);
        if (imported != null) {
            int metadata = "true".equals(state.properties.get("smouldering")) ? 1
                : integer(state, "age", integer(state, "stage", 0));
            if (industries.angelic.biomic.content.RuDecorationPaletteMetadata.isFlowerbed(name)) {
                int facing = switch (state.properties.getOrDefault("facing", "north")) {
                    case "east" -> 1;
                    case "south" -> 2;
                    case "west" -> 3;
                    default -> 0;
                };
                metadata = industries.angelic.biomic.content.RuDecorationPaletteMetadata
                    .leafLitterMetadata(integer(state, "flower_amount", 1), facing);
            }
            if (industries.angelic.biomic.content.RuCaveModelData.handles(name)) {
                metadata = industries.angelic.biomic.content.RuCaveModelData.metadata(name, state.properties);
                if (industries.angelic.biomic.content.RuCaveModelData.multiface(name)) {
                    imported = RuImportedDecorationBlocks.multiface(
                        name,
                        industries.angelic.biomic.content.RuCaveModelData.attachmentMask(state.properties),
                        "true".equals(state.properties.get("waterlogged")));
                }
                if (industries.angelic.biomic.content.RuCaveModelData.hangingHead(name)
                    && industries.angelic.biomic.content.RuCaveModelData.dropleafAge(state.properties) >= 16)
                    imported = RuImportedDecorationBlocks.matureHangingHead(name);
            } else if (imported instanceof BlockRuDoublePlant) {
                metadata = industries.angelic.biomic.content.RuDecorationPaletteMetadata
                    .doublePlantHalfMetadata(state.properties.get("half"));
            } else if (imported instanceof net.minecraft.block.BlockLog) {
                metadata = ("x".equals(state.properties.get("axis")) ? 4
                    : "z".equals(state.properties.get("axis")) ? 8 : 0)
                    | ("true".equals(state.properties.get("transition_block")) ? 1 : 0);
            }
            return new Resolved(imported, metadata);
        }
        throw new IllegalArgumentException("No 1.7/EFR mapping for pinned RU decoration state " + encoded);
    }

    private static Map<String, Integer> flowerMetadata() {
        String[] names = { "aster", "blue_lupine", "pink_lupine", "purple_lupine", "red_lupine", "yellow_lupine",
            "daisy", "fireweed", "hyssop", "mallow", "waratah" };
        int[] metadata = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int index = 0; index < names.length; index++) result.put(names[index], Integer.valueOf(metadata[index]));
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Integer> shrubMetadata(String... names) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int index = 0; index < names.length; index++) result.put(names[index], Integer.valueOf(index));
        return Collections.unmodifiableMap(result);
    }

    private static String path(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    private static int integer(ParsedState state, String key, int fallback) {
        String value = state.properties.get(key);
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
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
            Map<String, String> properties = new LinkedHashMap<>();
            String body = encoded.substring(open + 1, encoded.endsWith("]") ? encoded.length() - 1 : encoded.length());
            for (String assignment : body.split(",")) {
                int equals = assignment.indexOf('=');
                if (equals > 0) properties.put(assignment.substring(0, equals), assignment.substring(equals + 1));
            }
            return new ParsedState(encoded.substring(0, open), Collections.unmodifiableMap(properties));
        }
    }
}
