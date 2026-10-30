package industries.angelic.biomic.content.alexscaves;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import ganymedes01.etfuturum.ModBlocks;

/**
 * Exact allowlist for the Abyssal Chasm's generated states; unknown properties and other content fail closed. The ruin
 * and bone states are in the unused package's {@code AbyssalRuinStateCodec}.
 */
public final class AbyssalStateCodec {

    public record State(Block block, int metadata) {

        public State {
            if (block == null || metadata < 0 || metadata > 15)
                throw new IllegalArgumentException("Invalid Abyssal state");
        }
    }

    private AbyssalStateCodec() {}

    public static State resolve(String encoded) {
        Parsed parsed = Parsed.parse(encoded);
        if (parsed == null) return null;
        int metadata = metadata(parsed);
        if (metadata < 0) {
            var furnishing = RustyVanillaFurnishings.map(encoded);
            if (furnishing == null) return null;
            Block block = furnishing(furnishing.target());
            return block == null ? null : new State(block, furnishing.metadata());
        }
        Block block = switch (parsed.id) {
            case "minecraft:air", "minecraft:cave_air" -> Blocks.air;
            case "minecraft:water" -> Blocks.water;
            case "minecraft:lava" -> Blocks.lava;
            case "minecraft:stone" -> Blocks.stone;
            case "minecraft:bone_block" -> bone();
            case "minecraft:deepslate" -> deepslate();
            case "minecraft:tuff" -> tuff();
            case "minecraft:magma_block" -> magma();
            case "alexscaves:abyssmarine" -> AbyssalContentBlocks.ABYSSMARINE;
            case "alexscaves:muck" -> AbyssalContentBlocks.MUCK;
            case "alexscaves:geothermal_vent" -> AbyssalContentBlocks.VENT;
            case "alexscaves:geothermal_vent_medium" -> (metadata & 8) == 0 ? AbyssalContentBlocks.DRY_VENT_MEDIUM
                : AbyssalContentBlocks.VENT_MEDIUM;
            case "alexscaves:geothermal_vent_thin" -> (metadata & 8) == 0 ? AbyssalContentBlocks.DRY_VENT_THIN
                : AbyssalContentBlocks.VENT_THIN;
            case "alexscaves:tube_worm" -> AbyssalContentBlocks.TUBE_WORM;
            case "alexscaves:ping_pong_sponge" -> AbyssalContentBlocks.SPONGE;
            case "alexscaves:dusk_anemone" -> AbyssalContentBlocks.DUSK;
            case "alexscaves:twilight_anemone" -> AbyssalContentBlocks.TWILIGHT;
            case "alexscaves:midnight_anemone" -> AbyssalContentBlocks.MIDNIGHT;
            case "alexscaves:mussel" -> AbyssalContentBlocks.mussel(musselCount(parsed), (metadata & 8) == 0);
            default -> null;
        };
        return block == null ? null : new State(block, metadata);
    }

    public static String encode(Block block, int metadata) {
        if (metadata < 0 || metadata > 15) return null;
        if (block == AbyssalContentBlocks.ABYSSMARINE) return metadata == 0 ? "alexscaves:abyssmarine" : null;
        if (block == AbyssalContentBlocks.MUCK) return metadata == 0 ? "alexscaves:muck" : null;
        if (block == AbyssalContentBlocks.VENT) return vent("geothermal_vent", metadata, false);
        if (block == AbyssalContentBlocks.VENT_MEDIUM)
            return vent("geothermal_vent_medium", metadata, (metadata & 8) != 0);
        if (block == AbyssalContentBlocks.VENT_THIN) return vent("geothermal_vent_thin", metadata, (metadata & 8) != 0);
        if (block == AbyssalContentBlocks.DRY_VENT_MEDIUM)
            return metadata < 8 ? vent("geothermal_vent_medium", metadata, false) : null;
        if (block == AbyssalContentBlocks.DRY_VENT_THIN)
            return metadata < 8 ? vent("geothermal_vent_thin", metadata, false) : null;
        if (block == AbyssalContentBlocks.TUBE_WORM) return tube(metadata);
        if (block == AbyssalContentBlocks.SPONGE)
            return metadata <= 1 ? "alexscaves:ping_pong_sponge[top=" + (metadata == 1) + ",waterlogged=true]" : null;
        if (block == AbyssalContentBlocks.DUSK)
            return metadata == 0 ? "alexscaves:dusk_anemone[waterlogged=true]" : null;
        if (block == AbyssalContentBlocks.TWILIGHT)
            return metadata == 0 ? "alexscaves:twilight_anemone[waterlogged=true]" : null;
        if (block == AbyssalContentBlocks.MIDNIGHT)
            return metadata == 0 ? "alexscaves:midnight_anemone[waterlogged=true]" : null;
        for (int i = 0; i < AbyssalContentBlocks.MUSSELS.length; i++) {
            if (block == AbyssalContentBlocks.MUSSELS[i]) return mussel(metadata, i + 1);
            if (block == AbyssalContentBlocks.DRY_MUSSELS[i])
                return (metadata & 8) != 0 ? mussel(metadata, i + 1) : null;
        }
        return null;
    }

    private static int metadata(Parsed p) {
        return switch (p.id) {
            case "minecraft:air", "minecraft:cave_air", "minecraft:water", "minecraft:lava", "minecraft:stone", "minecraft:deepslate", "minecraft:tuff", "minecraft:magma_block", "alexscaves:abyssmarine", "alexscaves:muck" -> p.properties
                .isEmpty() ? 0 : -1;
            case "minecraft:bone_block" -> only(p, "axis") ? boneAxis(p.properties.getOrDefault("axis", "y")) : -1;
            case "alexscaves:geothermal_vent" -> ventMetadata(p, false);
            case "alexscaves:geothermal_vent_medium", "alexscaves:geothermal_vent_thin" -> ventMetadata(p, true);
            case "alexscaves:tube_worm" -> tubeMetadata(p);
            case "alexscaves:mussel" -> musselMetadata(p);
            case "alexscaves:ping_pong_sponge" -> spongeMetadata(p);
            case "alexscaves:dusk_anemone", "alexscaves:twilight_anemone", "alexscaves:midnight_anemone" -> waterOnly(p)
                ? 0
                : -1;
            default -> -1;
        };
    }

    private static int boneAxis(String value) {
        return switch (value) {
            case "x" -> 4;
            case "y" -> 0;
            case "z" -> 8;
            default -> -1;
        };
    }

    private static int ventMetadata(Parsed p, boolean mustWater) {
        if (!only(p, "smoke_type", "spawning_particles", "waterlogged")) return -1;
        int smoke = integer(p, "smoke_type", 0);
        Boolean spawn = bool(p, "spawning_particles", true), water = bool(p, "waterlogged", mustWater);
        if (smoke < 0 || smoke > 3 || spawn == null || water == null || !mustWater && water) return -1;
        return smoke | (spawn ? 4 : 0) | (water ? 8 : 0);
    }

    private static int tubeMetadata(Parsed p) {
        if (!only(p, "facing", "type", "waterlogged") || !Boolean.TRUE.equals(bool(p, "waterlogged", true))) return -1;
        int facing = horizontal(p.properties.getOrDefault("facing", "north")),
            type = switch (p.properties.getOrDefault("type", "straight")) {
            case "straight" -> 0;
            case "turn" -> 1;
            case "elbow" -> 2;
            default -> -1;
            };
        return facing < 0 || type < 0 ? -1 : type * 4 + facing;
    }

    private static int musselMetadata(Parsed p) {
        if (!only(p, "facing", "mussels", "waterlogged")) return -1;
        Boolean water = bool(p, "waterlogged", true);
        int count = musselCount(p), face = facing(p.properties.getOrDefault("facing", "up"));
        return count < 1 || count > 5 || face < 0 || water == null ? -1 : face | (water ? 0 : 8);
    }

    private static int musselCount(Parsed p) {
        return integer(p, "mussels", 1);
    }

    private static int spongeMetadata(Parsed p) {
        if (!only(p, "top", "waterlogged") || !Boolean.TRUE.equals(bool(p, "waterlogged", true))) return -1;
        Boolean top = bool(p, "top", true);
        return top == null ? -1 : top ? 1 : 0;
    }

    private static boolean waterOnly(Parsed p) {
        return only(p, "waterlogged") && Boolean.TRUE.equals(bool(p, "waterlogged", true));
    }

    private static String vent(String id, int meta, boolean water) {
        return "alexscaves:" + id
            + "[smoke_type="
            + (meta & 3)
            + ",spawning_particles="
            + ((meta & 4) != 0)
            + ",waterlogged="
            + water
            + "]";
    }

    private static String tube(int meta) {
        if (meta < 0 || meta > 11) return null;
        String type = switch (meta / 4) {
            case 0 -> "straight";
            case 1 -> "turn";
            default -> "elbow";
        };
        return "alexscaves:tube_worm[facing=" + horizontal(meta & 3) + ",type=" + type + ",waterlogged=true]";
    }

    private static String mussel(int meta, int count) {
        if (meta < 0 || (meta & 7) > 5 || count < 1 || count > 5) return null;
        return "alexscaves:mussel[facing=" + facing(
            meta & 7) + ",mussels=" + count + ",waterlogged=" + ((meta & 8) == 0) + "]";
    }

    private static int horizontal(String value) {
        return switch (value) {
            case "north" -> 0;
            case "east" -> 1;
            case "south" -> 2;
            case "west" -> 3;
            default -> -1;
        };
    }

    private static String horizontal(int value) {
        return switch (value) {
            case 0 -> "north";
            case 1 -> "east";
            case 2 -> "south";
            default -> "west";
        };
    }

    private static int facing(String value) {
        return switch (value) {
            case "down" -> 0;
            case "up" -> 1;
            case "north" -> 2;
            case "south" -> 3;
            case "west" -> 4;
            case "east" -> 5;
            default -> -1;
        };
    }

    private static String facing(int value) {
        return switch (value) {
            case 0 -> "down";
            case 1 -> "up";
            case 2 -> "north";
            case 3 -> "south";
            case 4 -> "west";
            default -> "east";
        };
    }

    private static int integer(Parsed p, String key, int fallback) {
        try {
            return Integer.parseInt(p.properties.getOrDefault(key, Integer.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static Boolean bool(Parsed p, String key, boolean fallback) {
        String value = p.properties.getOrDefault(key, Boolean.toString(fallback));
        if (value.equals("true")) return Boolean.TRUE;
        if (value.equals("false")) return Boolean.FALSE;
        return null;
    }

    private static boolean only(Parsed p, String... names) {
        java.util.Set<String> allowed = new java.util.HashSet<>(java.util.Arrays.asList(names));
        return allowed.containsAll(p.properties.keySet());
    }

    /** Vanilla and Et Futurum furnishings; the ruins' wet and rusty ones are unused (not registered), so null. */
    private static Block furnishing(RustyVanillaFurnishings.Target target) {
        return switch (target) {
            case ANVIL -> Blocks.anvil;
            case IRON_DOOR -> Blocks.iron_door;
            case IRON_TRAPDOOR -> ModBlocks.IRON_TRAPDOOR.get();
            case LADDER -> Blocks.ladder;
            // Legacy names are deceptive: piston_head is the static, tile-free BlockPistonExtension;
            // piston_extension is the transient BlockPistonMoving tile-entity carrier.
            case PISTON -> Blocks.piston;
            case STATIC_PISTON_HEAD -> Blocks.piston_head;
            case RED_STAINED_GLASS -> Blocks.stained_glass;
            case REDSTONE_LAMP -> Blocks.redstone_lamp;
            case REDSTONE_WIRE -> Blocks.redstone_wire;
            case POWERED_REPEATER -> Blocks.powered_repeater;
            case SMITHING_TABLE -> ModBlocks.SMITHING_TABLE.get();
            case SPRUCE_TRAPDOOR -> ModBlocks.TRAPDOOR_SPRUCE.get();
            case STICKY_PISTON -> Blocks.sticky_piston;
            case STONE_BUTTON -> Blocks.stone_button;
            case LIGHTNING_ROD, WET_IRON_TRAPDOOR, CEILING_BUTTON, WET_CHAIN, WET_WEATHERED_SLAB, WET_OXIDIZED_SLAB, WET_WEATHERED_STAIR, WET_WEATHERED_INNER_STAIR, WET_WAXED_WEATHERED_STAIR, WET_WAXED_WEATHERED_INNER_STAIR, WET_WAXED_OXIDIZED_STAIR, WET_WAXED_OXIDIZED_INNER_STAIR -> null;
        };
    }

    private static Block deepslate() {
        Block block = ModBlocks.DEEPSLATE.get();
        return block == null ? Blocks.stone : block;
    }

    private static Block tuff() {
        Block block = ModBlocks.TUFF.get();
        return block == null ? Blocks.stone : block;
    }

    private static Block magma() {
        Block block = ModBlocks.MAGMA.get();
        return block == null ? Blocks.netherrack : block;
    }

    private static Block bone() {
        Block block = ModBlocks.BONE.get();
        return block == null ? Blocks.quartz_block : block;
    }

    private record Parsed(String id, Map<String, String> properties) {

        static Parsed parse(String value) {
            if (value == null) return null;
            int bracket = value.indexOf('[');
            String id = bracket < 0 ? value : value.substring(0, bracket);
            if (id.isEmpty() || bracket >= 0 && !value.endsWith("]")) return null;
            Map<String, String> properties = new LinkedHashMap<>();
            if (bracket >= 0 && !value.substring(bracket + 1, value.length() - 1)
                .isEmpty()) for (String property : value.substring(bracket + 1, value.length() - 1)
                    .split(",")) {
                        int equals = property.indexOf('=');
                        if (equals < 1 || equals == property.length() - 1
                            || properties.put(property.substring(0, equals), property.substring(equals + 1)) != null)
                            return null;
                    }
            return new Parsed(id, properties);
        }
    }
}
