package industries.angelic.biomic.unused.alexscaves;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import ganymedes01.etfuturum.ModBlocks;
import industries.angelic.biomic.content.alexscaves.AbyssalContentBlocks;
import industries.angelic.biomic.content.alexscaves.RustyVanillaFurnishings;

/**
 * The Abyssal state codec as it was with the ruin and bone blocks: the live {@code AbyssalStateCodec} plus their states
 * and the ruins' vanilla furnishings. A drop-in replacement for it once {@link AbyssalRuinBlocks} are registered again.
 */
public final class AbyssalRuinStateCodec {

    public record State(Block block, int metadata) {

        public State {
            if (block == null || metadata < 0 || metadata > 15)
                throw new IllegalArgumentException("Invalid Abyssal state");
        }
    }

    private AbyssalRuinStateCodec() {}

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
            case "alexscaves:smooth_bone" -> AbyssalRuinBlocks.SMOOTH_BONE;
            case "alexscaves:smooth_bone_slab" -> AbyssalRuinBlocks.SMOOTH_BONE_SLAB;
            case "alexscaves:smooth_bone_stairs" -> AbyssalRuinBlocks.SMOOTH_BONE_STAIRS[stairShape(parsed)];
            case "alexscaves:smooth_bone_wall" -> AbyssalRuinBlocks.SMOOTH_BONE_WALL;
            case "alexscaves:hollow_bone" -> AbyssalRuinBlocks.HOLLOW_BONE;
            case "alexscaves:thin_bone" -> AbyssalRuinBlocks.THIN_BONE;
            case "alexscaves:bone_nodule" -> AbyssalRuinBlocks.BONE_NODULE;
            case "alexscaves:bone_ribs" -> AbyssalRuinBlocks.BONE_RIBS;
            case "alexscaves:baleen_bone" -> AbyssalRuinBlocks.BALEEN_BONE;
            case "alexscaves:bone_worms" -> AbyssalRuinBlocks.BONE_WORMS;
            case "alexscaves:abyssmarine_bricks" -> AbyssalRuinBlocks.ABYSSMARINE_BRICKS;
            case "alexscaves:abyssmarine_tiles" -> AbyssalRuinBlocks.ABYSSMARINE_TILES;
            case "alexscaves:abyssmarine_pillar" -> AbyssalRuinBlocks.ABYSSMARINE_PILLAR;
            case "alexscaves:abyssmarine_stairs" -> AbyssalRuinBlocks.ABYSSMARINE_STAIRS[stairShape(parsed)];
            case "alexscaves:abyssmarine_wall" -> AbyssalRuinBlocks.ABYSSMARINE_WALL;
            default -> null;
        };
        if (block == null && parsed.id.equals("alexscaves:rusty_scrap_metal_plate"))
            block = AbyssalRuinBlocks.RUSTY_SCRAP_METAL_PLATE;
        if (block == null && parsed.id.equals("alexscaves:rusty_scrap_metal"))
            block = AbyssalRuinBlocks.RUSTY_SCRAP_METAL;
        if (block == null && parsed.id.equals("alexscaves:depth_glass")) block = AbyssalRuinBlocks.DEPTH_GLASS;
        if (block == null && parsed.id.equals("alexscaves:rusty_scaffolding"))
            block = AbyssalRuinBlocks.RUSTY_SCAFFOLDING;
        if (block == null && parsed.id.equals("alexscaves:drain")) block = AbyssalRuinBlocks.DRAIN;
        if (block == null && parsed.id.equals("alexscaves:magnetic_activator"))
            block = AbyssalRuinBlocks.MAGNETIC_ACTIVATOR;
        if (block == null && parsed.id.equals("alexscaves:abyssmarine_brick_stairs"))
            block = AbyssalRuinBlocks.ABYSSMARINE_BRICK_STAIRS[stairShape(parsed)];
        if (block == null && parsed.id.equals("alexscaves:abyssmarine_brick_wall"))
            block = AbyssalRuinBlocks.ABYSSMARINE_BRICK_WALL;
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
        if (block == AbyssalRuinBlocks.RUSTY_SCRAP_METAL) return metadata == 0 ? "alexscaves:rusty_scrap_metal" : null;
        if (block == AbyssalRuinBlocks.DEPTH_GLASS) return metadata == 0 ? depthCanonical() : null;
        if (block == AbyssalRuinBlocks.RUSTY_SCAFFOLDING)
            return metadata <= 3
                ? "alexscaves:rusty_scaffolding[bottom=" + ((metadata & 1) != 0)
                    + ",distance=0,liquid_logged="
                    + ((metadata & 2) != 0 ? 1 : 0)
                    + "]"
                : null;
        if (block == AbyssalRuinBlocks.DRAIN)
            return metadata <= 1 ? "alexscaves:drain[open=" + (metadata == 1) + "]" : null;
        if (block == AbyssalRuinBlocks.MAGNETIC_ACTIVATOR)
            return metadata <= 5 ? "alexscaves:magnetic_activator[facing=" + facing(metadata) + "]" : null;
        if (block == AbyssalRuinBlocks.RUSTY_LIGHTNING_ROD) return metadata >= 1 && metadata <= 5
            ? "minecraft:lightning_rod[facing=" + facing(metadata) + ",powered=false,waterlogged=true]"
            : null;
        if (block == AbyssalRuinBlocks.RUSTY_WET_IRON_TRAPDOOR)
            return metadata >= 2 && metadata <= 5 ? "minecraft:iron_trapdoor[facing=" + facing(metadata)
                + ",half=top,open=false,powered=false,waterlogged=true]" : null;
        if (block == AbyssalRuinBlocks.RUSTY_CEILING_BUTTON) return metadata >= 2 && metadata <= 5
            ? "minecraft:stone_button[face=ceiling,facing=" + facing(metadata) + ",powered=false]"
            : null;
        if (block == AbyssalRuinBlocks.RUSTY_WET_CHAIN)
            return metadata == 0 ? "minecraft:chain[axis=y,waterlogged=true]" : null;
        if (block == AbyssalRuinBlocks.RUSTY_WET_WEATHERED_SLAB) return wetSlab("weathered_cut_copper_slab", metadata);
        if (block == AbyssalRuinBlocks.RUSTY_WET_OXIDIZED_SLAB) return wetSlab("oxidized_cut_copper_slab", metadata);
        for (int shape = 0; shape < 2; shape++) {
            if (block == AbyssalRuinBlocks.RUSTY_WET_WEATHERED_STAIRS[shape])
                return wetStair("weathered_cut_copper_stairs", shape, metadata);
            if (block == AbyssalRuinBlocks.RUSTY_WET_WAXED_WEATHERED_STAIRS[shape])
                return wetStair("waxed_weathered_cut_copper_stairs", shape, metadata);
            if (block == AbyssalRuinBlocks.RUSTY_WET_WAXED_OXIDIZED_STAIRS[shape])
                return wetStair("waxed_oxidized_cut_copper_stairs", shape, metadata);
        }
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
            case "alexscaves:smooth_bone" -> p.properties.isEmpty() ? 0 : -1;
            case "alexscaves:smooth_bone_slab" -> slabMetadata(p);
            case "alexscaves:smooth_bone_stairs" -> stairMetadata(p);
            case "alexscaves:smooth_bone_wall" -> wallMetadata(p);
            case "alexscaves:hollow_bone" -> axisMetadata(p);
            case "alexscaves:thin_bone" -> thinMetadata(p);
            case "alexscaves:bone_nodule" -> attachedMetadata(p);
            case "alexscaves:bone_ribs" -> ribsMetadata(p);
            case "alexscaves:baleen_bone" -> baleenMetadata(p);
            case "alexscaves:bone_worms" -> waterloggedAttachedMetadata(p);
            case "alexscaves:abyssmarine_bricks", "alexscaves:abyssmarine_tiles" -> passiveDistance(p) ? 0 : -1;
            case "alexscaves:abyssmarine_pillar" -> pillarMetadata(p);
            case "alexscaves:abyssmarine_stairs", "alexscaves:abyssmarine_brick_stairs" -> stairMetadata(p);
            case "alexscaves:abyssmarine_wall", "alexscaves:abyssmarine_brick_wall" -> wallMetadata(p);
            case "alexscaves:rusty_scrap_metal_plate", "alexscaves:rusty_scrap_metal" -> p.properties.isEmpty() ? 0
                : -1;
            case "alexscaves:depth_glass" -> depthMetadata(p);
            case "alexscaves:rusty_scaffolding" -> scaffoldingMetadata(p);
            case "alexscaves:drain" -> only(p, "open") && bool(p, "open", false) != null
                ? (Boolean.TRUE.equals(bool(p, "open", false)) ? 1 : 0)
                : -1;
            case "alexscaves:magnetic_activator" -> only(p, "facing")
                ? facing(p.properties.getOrDefault("facing", "south"))
                : -1;
            default -> -1;
        };
    }

    private static int depthMetadata(Parsed p) {
        if (!only(p, "down", "east", "north", "south", "up", "west")) return -1;
        for (String key : new String[] { "down", "east", "north", "south", "up", "west" })
            if (bool(p, key, false) == null) return -1;
        return 0;
    }

    private static int scaffoldingMetadata(Parsed p) {
        if (!only(p, "bottom", "distance", "liquid_logged")) return -1;
        Boolean bottom = bool(p, "bottom", false);
        int distance = integer(p, "distance", 12), liquid = integer(p, "liquid_logged", 0);
        return bottom == null || distance < 0 || distance > 12 || liquid < 0 || liquid > 1 ? -1
            : (bottom ? 1 : 0) | (liquid == 1 ? 2 : 0);
    }

    private static int slabMetadata(Parsed p) {
        if (!only(p, "type", "waterlogged") || !Boolean.FALSE.equals(bool(p, "waterlogged", false))) return -1;
        return switch (p.properties.getOrDefault("type", "bottom")) {
            case "bottom" -> 0;
            case "top" -> 1;
            default -> -1;
        };
    }

    private static int stairShape(Parsed p) {
        String shape = p.properties.getOrDefault("shape", "straight");
        return shape.equals("straight") ? 0 : shape.startsWith("inner_") ? 1 : shape.startsWith("outer_") ? 2 : -1;
    }

    private static int stairMetadata(Parsed p) {
        if (!only(p, "active", "distance", "facing", "half", "shape", "waterlogged")
            || !Boolean.FALSE.equals(bool(p, "waterlogged", false))
            || !passiveDistance(p)) return -1;
        int facing = horizontal(p.properties.getOrDefault("facing", "north")), shape = stairShape(p);
        String half = p.properties.getOrDefault("half", "bottom");
        if (facing < 0 || shape < 0 || !half.equals("bottom") && !half.equals("top")) return -1;
        String value = p.properties.getOrDefault("shape", "straight");
        return facing | (half.equals("top") ? 4 : 0) | (value.endsWith("right") ? 8 : 0);
    }

    private static int wallMetadata(Parsed p) {
        if (!only(p, "active", "distance", "east", "north", "south", "up", "waterlogged", "west")
            || !Boolean.FALSE.equals(bool(p, "waterlogged", false))
            || !passiveDistance(p)) return -1;
        int north = wallSide(p, "north"), south = wallSide(p, "south"), west = wallSide(p, "west"),
            east = wallSide(p, "east");
        if ((north | south | west | east) < 0 || bool(p, "up", true) == null) return -1;
        return north | (south << 1) | (west << 2) | (east << 3);
    }

    private static int pillarMetadata(Parsed p) {
        if (!only(p, "active", "axis", "distance")) return -1;
        Boolean active = bool(p, "active", false);
        int distance = integer(p, "distance", 0), axis = axis(p.properties.getOrDefault("axis", "y"));
        // The exact ruin payload is inactive. Active pillars select glow textures which are deliberately not claimed
        // here.
        return Boolean.FALSE.equals(active) && distance >= 0 && distance <= 15 ? axis : -1;
    }

    private static boolean passiveDistance(Parsed p) {
        if (!p.properties.containsKey("active") && !p.properties.containsKey("distance")) return true;
        if (!p.properties.containsKey("active") || !p.properties.containsKey("distance")) return false;
        int distance = integer(p, "distance", 0);
        return Boolean.FALSE.equals(bool(p, "active", false)) && distance >= 0 && distance <= 15;
    }

    private static int wallSide(Parsed p, String name) {
        String value = p.properties.getOrDefault(name, "none");
        return value.equals("none") ? 0 : value.equals("low") || value.equals("tall") ? 1 : -1;
    }

    private static int axisMetadata(Parsed p) {
        if (!only(p, "axis", "waterlogged") || !Boolean.valueOf(false)
            .equals(bool(p, "waterlogged", false))) return -1;
        return axis(p.properties.getOrDefault("axis", "y"));
    }

    private static int thinMetadata(Parsed p) {
        if (!only(p, "axis", "offset", "waterlogged") || !Boolean.FALSE.equals(bool(p, "waterlogged", false)))
            return -1;
        int axis = axis(p.properties.getOrDefault("axis", "y")), offset = integer(p, "offset", 0);
        return axis < 0 || offset < 0 || offset > 2 ? -1 : axis * 3 + offset;
    }

    private static int attachedMetadata(Parsed p) {
        if (!only(p, "facing", "waterlogged") || !Boolean.valueOf(false)
            .equals(bool(p, "waterlogged", false))) return -1;
        return facing(p.properties.getOrDefault("facing", "up"));
    }

    private static int waterloggedAttachedMetadata(Parsed p) {
        if (!only(p, "facing", "waterlogged")) return -1;
        Boolean water = bool(p, "waterlogged", true);
        int face = facing(p.properties.getOrDefault("facing", "up"));
        return water == null || face < 0 ? -1 : face | (water ? 0 : 8);
    }

    private static int ribsMetadata(Parsed p) {
        if (!only(p, "facing", "under", "waterlogged") || !Boolean.FALSE.equals(bool(p, "waterlogged", false)))
            return -1;
        int facing = horizontal(p.properties.getOrDefault("facing", "north"));
        Boolean under = bool(p, "under", false);
        return facing < 0 || under == null ? -1 : facing | (under ? 4 : 0);
    }

    private static int baleenMetadata(Parsed p) {
        if (!only(p, "waterlogged", "x") || !Boolean.FALSE.equals(bool(p, "waterlogged", false))) return -1;
        Boolean x = bool(p, "x", false);
        return x == null ? -1 : x ? 1 : 0;
    }

    private static int axis(String value) {
        return switch (value) {
            case "x" -> 0;
            case "y" -> 1;
            case "z" -> 2;
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

    private static String depthCanonical() {
        StringBuilder out = new StringBuilder("alexscaves:depth_glass[");
        String[] keys = { "down", "east", "north", "south", "up", "west" };
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) out.append(',');
            out.append(keys[i])
                .append('=')
                .append(false);
        }
        return out.append(']')
            .toString();
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
            case LIGHTNING_ROD -> AbyssalRuinBlocks.RUSTY_LIGHTNING_ROD;
            case WET_IRON_TRAPDOOR -> AbyssalRuinBlocks.RUSTY_WET_IRON_TRAPDOOR;
            case CEILING_BUTTON -> AbyssalRuinBlocks.RUSTY_CEILING_BUTTON;
            case WET_CHAIN -> AbyssalRuinBlocks.RUSTY_WET_CHAIN;
            case WET_WEATHERED_SLAB -> AbyssalRuinBlocks.RUSTY_WET_WEATHERED_SLAB;
            case WET_OXIDIZED_SLAB -> AbyssalRuinBlocks.RUSTY_WET_OXIDIZED_SLAB;
            case WET_WEATHERED_STAIR -> AbyssalRuinBlocks.RUSTY_WET_WEATHERED_STAIRS[0];
            case WET_WEATHERED_INNER_STAIR -> AbyssalRuinBlocks.RUSTY_WET_WEATHERED_STAIRS[1];
            case WET_WAXED_WEATHERED_STAIR -> AbyssalRuinBlocks.RUSTY_WET_WAXED_WEATHERED_STAIRS[0];
            case WET_WAXED_WEATHERED_INNER_STAIR -> AbyssalRuinBlocks.RUSTY_WET_WAXED_WEATHERED_STAIRS[1];
            case WET_WAXED_OXIDIZED_STAIR -> AbyssalRuinBlocks.RUSTY_WET_WAXED_OXIDIZED_STAIRS[0];
            case WET_WAXED_OXIDIZED_INNER_STAIR -> AbyssalRuinBlocks.RUSTY_WET_WAXED_OXIDIZED_STAIRS[1];
        };
    }

    private static String wetSlab(String id, int metadata) {
        return metadata >= 0 && metadata <= 1
            ? "minecraft:" + id + "[type=" + (metadata == 0 ? "bottom" : "top") + ",waterlogged=true]"
            : null;
    }

    private static String wetStair(String id, int shape, int metadata) {
        if (metadata < 0 || metadata > 15 || (metadata & 3) > 3 || (shape == 0 && (metadata & 8) != 0)) return null;
        String facing = switch (metadata & 3) {
            case 0 -> "east";
            case 1 -> "west";
            case 2 -> "south";
            default -> "north";
        };
        String form = shape == 0 ? "straight" : (metadata & 8) != 0 ? "inner_right" : "inner_left";
        return "minecraft:" + id
            + "[facing="
            + facing
            + ",half="
            + ((metadata & 4) != 0 ? "top" : "bottom")
            + ",shape="
            + form
            + ",waterlogged=true]";
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
