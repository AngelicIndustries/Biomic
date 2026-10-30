package industries.angelic.biomic.content.alexscaves;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import ganymedes01.etfuturum.ModBlocks;

/** Exact allowlist between source Forlorn states and legacy blocks; never performs registry lookup. */
public final class ForlornStateCodec {

    public record State(Block block, int metadata) {

        public State {
            if (block == null || metadata < 0 || metadata > 15)
                throw new IllegalArgumentException("Invalid Forlorn state");
        }
    }

    private ForlornStateCodec() {}

    public static State resolve(String encoded) {
        Parsed parsed = Parsed.parse(encoded);
        if (parsed == null) return null;
        if (!handles(parsed)) return null;
        return switch (parsed.id) {
            case "minecraft:air", "minecraft:cave_air" -> state(Blocks.air, 0);
            case "minecraft:brown_mushroom" -> state(Blocks.brown_mushroom, 0);
            case "minecraft:packed_mud" -> state(packedMud(), 0);
            case "alexscaves:guanostone" -> own(parsed, ForlornContentBlocks.GUANOSTONE, 0);
            case "alexscaves:coprolith" -> own(parsed, ForlornContentBlocks.COPROLITH, 0);
            case "alexscaves:coprolith_coal_ore" -> own(parsed, ForlornContentBlocks.COPROLITH_COAL_ORE, 0);
            case "alexscaves:guanostone_redstone_ore" -> own(parsed, ForlornContentBlocks.GUANOSTONE_REDSTONE_ORE, 0);
            case "alexscaves:peering_coprolith" -> own(parsed, ForlornContentBlocks.PEERING_COPROLITH, 0);
            case "alexscaves:porous_coprolith" -> own(parsed, ForlornContentBlocks.POROUS_COPROLITH, 0);
            case "alexscaves:thornwood_log" -> pillar(parsed, ForlornContentBlocks.THORNWOOD_LOG);
            case "alexscaves:thornwood_wood" -> pillar(parsed, ForlornContentBlocks.THORNWOOD_WOOD);
            case "alexscaves:thornwood_branch" -> branch(parsed);
            case "alexscaves:underweed" -> own(parsed, ForlornContentBlocks.UNDERWEED, 0);
            case "alexscaves:guano_block" -> own(parsed, ForlornContentBlocks.GUANO_BLOCK, 0);
            case "alexscaves:guano_layer" -> guano(parsed);
            default -> null;
        };
    }

    private static boolean handles(Parsed parsed) {
        return switch (parsed.id) {
            case "minecraft:air", "minecraft:cave_air", "minecraft:packed_mud", "minecraft:brown_mushroom", "alexscaves:guanostone", "alexscaves:coprolith", "alexscaves:coprolith_coal_ore", "alexscaves:guanostone_redstone_ore", "alexscaves:underweed", "alexscaves:peering_coprolith", "alexscaves:porous_coprolith", "alexscaves:guano_block" -> parsed.properties
                .isEmpty();
            case "alexscaves:thornwood_log", "alexscaves:thornwood_wood" -> pillarMetadata(parsed) >= 0;
            case "alexscaves:thornwood_branch" -> branchMetadata(parsed) >= 0;
            case "alexscaves:guano_layer" -> guanoMetadata(parsed) >= 0;
            default -> false;
        };
    }

    /** Canonical inverse used by the persistent cross-chunk feature journal. */
    public static String encode(Block block, int metadata) {
        if (block == Blocks.brown_mushroom) return metadata == 0 ? "minecraft:brown_mushroom" : null;
        if (block == ForlornContentBlocks.GUANOSTONE) return metadata == 0 ? "alexscaves:guanostone" : null;
        if (block == ForlornContentBlocks.COPROLITH) return metadata == 0 ? "alexscaves:coprolith" : null;
        if (block == ForlornContentBlocks.COPROLITH_COAL_ORE)
            return metadata == 0 ? "alexscaves:coprolith_coal_ore" : null;
        if (block == ForlornContentBlocks.GUANOSTONE_REDSTONE_ORE)
            return metadata == 0 ? "alexscaves:guanostone_redstone_ore" : null;
        if (block == ForlornContentBlocks.PEERING_COPROLITH)
            return metadata == 0 ? "alexscaves:peering_coprolith" : null;
        if (block == ForlornContentBlocks.POROUS_COPROLITH) return metadata == 0 ? "alexscaves:porous_coprolith" : null;
        if (block == ForlornContentBlocks.THORNWOOD_LOG) return pillarState("alexscaves:thornwood_log", metadata);
        if (block == ForlornContentBlocks.THORNWOOD_WOOD) return pillarState("alexscaves:thornwood_wood", metadata);
        if (block == ForlornContentBlocks.THORNWOOD_BRANCH)
            return metadata >= 0 && metadata <= 5 ? "alexscaves:thornwood_branch[facing=" + (switch (metadata) {
            case 1 -> "down";
            case 2 -> "north";
            case 3 -> "east";
            case 4 -> "south";
            case 5 -> "west";
            default -> "up";
            }) + "]" : null;
        if (block == ForlornContentBlocks.UNDERWEED) return metadata == 0 ? "alexscaves:underweed" : null;
        if (block == ForlornContentBlocks.GUANO_BLOCK) return metadata == 0 ? "alexscaves:guano_block" : null;
        if (block == ForlornContentBlocks.GUANO_LAYER)
            return metadata >= 0 && metadata < 8 ? "alexscaves:guano_layer[layers=" + (metadata + 1) + "]" : null;
        return null;
    }

    private static String pillarState(String id, int metadata) {
        return switch (metadata) {
            case 0 -> id + "[axis=y]";
            case 4 -> id + "[axis=x]";
            case 8 -> id + "[axis=z]";
            default -> null;
        };
    }

    private static Block packedMud() {
        Block block = ModBlocks.PACKED_MUD.get();
        return block == null ? Blocks.hardened_clay : block;
    }

    private static State own(Parsed parsed, Block block, int metadata) {
        return parsed.properties.isEmpty() ? state(block, metadata) : null;
    }

    private static State pillar(Parsed parsed, Block block) {
        return state(block, pillarMetadata(parsed));
    }

    private static State branch(Parsed parsed) {
        return state(ForlornContentBlocks.THORNWOOD_BRANCH, branchMetadata(parsed));
    }

    private static State guano(Parsed parsed) {
        return state(ForlornContentBlocks.GUANO_LAYER, guanoMetadata(parsed));
    }

    private static int pillarMetadata(Parsed parsed) {
        if (!hasOnlyOptionalProperty(parsed, "axis")) return -1;
        return switch (parsed.properties.getOrDefault("axis", "y")) {
            case "y" -> 0;
            case "x" -> 4;
            case "z" -> 8;
            default -> -1;
        };
    }

    private static int branchMetadata(Parsed parsed) {
        if (!hasOnlyOptionalProperty(parsed, "facing")) return -1;
        return switch (parsed.properties.getOrDefault("facing", "up")) {
            case "up" -> 0;
            case "down" -> 1;
            case "north" -> 2;
            case "east" -> 3;
            case "south" -> 4;
            case "west" -> 5;
            default -> -1;
        };
    }

    private static int guanoMetadata(Parsed parsed) {
        if (!hasOnlyOptionalProperty(parsed, "layers")) return -1;
        try {
            int layers = Integer.parseInt(parsed.properties.getOrDefault("layers", "1"));
            return layers < 1 || layers > 8 ? -1 : layers - 1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean hasOnlyOptionalProperty(Parsed parsed, String name) {
        return parsed.properties.isEmpty() || parsed.properties.size() == 1 && parsed.properties.containsKey(name);
    }

    private static State state(Block block, int metadata) {
        return new State(block, metadata);
    }

    private record Parsed(String id, Map<String, String> properties) {

        static Parsed parse(String value) {
            if (value == null) return null;
            int bracket = value.indexOf('[');
            String id = bracket < 0 ? value : value.substring(0, bracket);
            if (id.isEmpty() || bracket >= 0 && !value.endsWith("]")) return null;
            Map<String, String> properties = new LinkedHashMap<>();
            if (bracket >= 0) for (String property : value.substring(bracket + 1, value.length() - 1)
                .split(",")) {
                    int equals = property.indexOf('=');
                    if (equals < 1 || equals == property.length() - 1) return null;
                    if (properties.put(property.substring(0, equals), property.substring(equals + 1)) != null)
                        return null;
                }
            return new Parsed(id, properties);
        }
    }
}
