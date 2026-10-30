package industries.angelic.biomic.content.alexscaves;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import ganymedes01.etfuturum.ModBlocks;

/** Exact, closed transport vocabulary for the Primordial environment; no registry fallback. */
public final class PrimordialStateCodec {

    public record State(Block block, int metadata) {

        public State {
            if (block == null || metadata < 0 || metadata > 15)
                throw new IllegalArgumentException("Invalid Primordial state");
        }
    }

    private PrimordialStateCodec() {}

    public static State resolve(String value) {
        Parsed p = Parsed.parse(value);
        if (p == null) return null;
        int meta = metadata(p);
        if (meta < 0) return null;
        Block block = switch (p.id) {
            case "minecraft:air", "minecraft:cave_air" -> Blocks.air;
            case "minecraft:water" -> Blocks.water;
            case "minecraft:lava" -> Blocks.lava;
            case "minecraft:smooth_basalt" -> smoothBasalt();
            case "minecraft:grass_block" -> Blocks.grass;
            case "minecraft:dirt" -> Blocks.dirt;
            case "minecraft:sandstone" -> Blocks.sandstone;
            case "minecraft:packed_mud" -> packedMud();
            case "minecraft:moss_block" -> moss();
            case "minecraft:jungle_log" -> Blocks.log;
            case "minecraft:grass" -> Blocks.tallgrass;
            case "minecraft:fern" -> Blocks.tallgrass;
            case "minecraft:large_fern" -> Blocks.double_plant;
            case "alexscaves:limestone" -> PrimordialContentBlocks.LIMESTONE;
            case "alexscaves:flood_basalt" -> PrimordialContentBlocks.FLOOD_BASALT;
            case "alexscaves:primal_magma" -> PrimordialContentBlocks.PRIMAL_MAGMA;
            case "alexscaves:volcanic_core" -> PrimordialContentBlocks.VOLCANIC_CORE;
            case "alexscaves:amber" -> PrimordialContentBlocks.AMBER;
            case "alexscaves:ambersol" -> PrimordialContentBlocks.AMBERSOL;
            case "alexscaves:ambersol_light" -> PrimordialContentBlocks.AMBERSOL_LIGHT;
            case "alexscaves:pewen_log" -> PrimordialContentBlocks.PEWEN_LOG;
            case "alexscaves:pewen_wood" -> PrimordialContentBlocks.PEWEN_WOOD;
            case "alexscaves:pewen_branch" -> PrimordialContentBlocks.PEWEN_BRANCH;
            case "alexscaves:pewen_pines" -> PrimordialContentBlocks.PEWEN_PINES;
            case "alexscaves:cycad" -> PrimordialContentBlocks.CYCAD;
            case "alexscaves:curly_fern" -> PrimordialContentBlocks.CURLY_FERN;
            case "alexscaves:flytrap" -> PrimordialContentBlocks.FLYTRAP;
            case "alexscaves:archaic_vine" -> PrimordialContentBlocks.ARCHAIC_VINE;
            case "alexscaves:archaic_vine_plant" -> PrimordialContentBlocks.ARCHAIC_VINE_PLANT;
            case "alexscaves:ancient_leaves" -> PrimordialContentBlocks.ANCIENT_LEAVES;
            case "alexscaves:tree_star" -> PrimordialContentBlocks.TREE_STAR;
            default -> null;
        };
        return block == null ? null : new State(block, meta);
    }

    public static String encode(Block block, int meta) {
        if (meta < 0 || meta > 15) return null;
        if (block == Blocks.air) return meta == 0 ? "minecraft:air" : null;
        if (block == Blocks.water) return meta == 0 ? "minecraft:water[level=0]" : null;
        if (block == Blocks.lava) return meta == 0 ? "minecraft:lava[level=0]" : null;
        if (block == Blocks.grass) return meta == 0 ? "minecraft:grass_block" : null;
        if (block == Blocks.dirt) return meta == 0 ? "minecraft:dirt" : null;
        if (block == Blocks.sandstone) return meta == 0 ? "minecraft:sandstone" : null;
        if (block == Blocks.log) return switch (meta) {
            case 3 -> "minecraft:jungle_log[axis=y]";
            case 7 -> "minecraft:jungle_log[axis=x]";
            case 11 -> "minecraft:jungle_log[axis=z]";
            default -> null;
        };
        if (block == Blocks.tallgrass) return meta == 1 ? "minecraft:grass" : meta == 2 ? "minecraft:fern" : null;
        if (block == Blocks.double_plant) return meta == 3 ? "minecraft:large_fern[half=lower]"
            : meta == 11 ? "minecraft:large_fern[half=upper]" : null;
        if (block == PrimordialContentBlocks.FLOOD_BASALT) return pillar("alexscaves:flood_basalt", meta);
        if (block == PrimordialContentBlocks.PRIMAL_MAGMA)
            return meta == 0 ? "alexscaves:primal_magma[active=false,permanent=false]"
                : meta == 3 ? "alexscaves:primal_magma[active=true,permanent=true]" : null;
        if (block == PrimordialContentBlocks.VOLCANIC_CORE) return plain("alexscaves:volcanic_core", meta);
        if (block == PrimordialContentBlocks.LIMESTONE) return plain("alexscaves:limestone", meta);
        if (block == PrimordialContentBlocks.AMBER) return plain("alexscaves:amber", meta);
        if (block == PrimordialContentBlocks.AMBERSOL) return plain("alexscaves:ambersol", meta);
        if (block == PrimordialContentBlocks.AMBERSOL_LIGHT) return plain("alexscaves:ambersol_light", meta);
        if (block == PrimordialContentBlocks.PEWEN_LOG) return pillar("alexscaves:pewen_log", meta);
        if (block == PrimordialContentBlocks.PEWEN_WOOD) return pillar("alexscaves:pewen_wood", meta);
        if (block == PrimordialContentBlocks.PEWEN_BRANCH)
            return meta <= 15 ? "alexscaves:pewen_branch[rotation=" + (meta & 7) + ",pines=" + ((meta & 8) != 0) + "]"
                : null;
        if (block == PrimordialContentBlocks.PEWEN_PINES) return plain("alexscaves:pewen_pines", meta);
        if (block == PrimordialContentBlocks.CYCAD)
            return meta <= 1 ? "alexscaves:cycad[top=" + (meta == 1) + "]" : null;
        if (block == PrimordialContentBlocks.CURLY_FERN)
            return meta <= 7
                ? "alexscaves:curly_fern[half=" + ((meta & 4) == 0 ? "lower" : "upper")
                    + ",facing="
                    + direction(meta & 3)
                    + "]"
                : null;
        if (block == PrimordialContentBlocks.FLYTRAP)
            return meta <= 1 ? "alexscaves:flytrap[open=" + (meta == 1) + "]" : null;
        if (block == PrimordialContentBlocks.ARCHAIC_VINE) return plain("alexscaves:archaic_vine", meta);
        if (block == PrimordialContentBlocks.ARCHAIC_VINE_PLANT) return plain("alexscaves:archaic_vine_plant", meta);
        if (block == PrimordialContentBlocks.ANCIENT_LEAVES) return plain("alexscaves:ancient_leaves", meta);
        if (block == PrimordialContentBlocks.TREE_STAR)
            return meta < 6 ? "alexscaves:tree_star[facing=" + face(meta) + "]" : null;
        // These optional EFR holders perform Forge configuration work in their class initializer. Reach them only
        // after every intrinsic/vanilla identity has failed, so closed-codec queries remain safe before FML bootstrap.
        if (block == smoothBasalt()) return meta == 0 ? "minecraft:smooth_basalt" : null;
        if (block == packedMud()) return meta == 0 ? "minecraft:packed_mud" : null;
        if (block == moss()) return meta == 0 ? "minecraft:moss_block" : null;
        return null;
    }

    private static int metadata(Parsed p) {
        return switch (p.id) {
            case "minecraft:air", "minecraft:cave_air", "minecraft:grass_block", "minecraft:dirt", "minecraft:sandstone", "minecraft:packed_mud", "minecraft:moss_block", "minecraft:smooth_basalt", "alexscaves:limestone", "alexscaves:volcanic_core", "alexscaves:amber", "alexscaves:ambersol", "alexscaves:ambersol_light", "alexscaves:pewen_pines", "alexscaves:archaic_vine", "alexscaves:archaic_vine_plant", "alexscaves:ancient_leaves" -> p.properties
                .isEmpty() ? 0 : -1;
            case "minecraft:water", "minecraft:lava" -> "0".equals(p.properties.get("level"))
                && p.properties.size() == 1 ? 0 : -1;
            case "alexscaves:flood_basalt" -> pillarMeta(p);
            case "alexscaves:primal_magma" -> magmaMeta(p);
            case "minecraft:jungle_log" -> {
                int axis = pillarMeta(p);
                yield axis < 0 ? -1 : axis + 3;
            }
            case "minecraft:grass" -> p.properties.isEmpty() ? 1 : -1;
            case "minecraft:fern" -> p.properties.isEmpty() ? 2 : -1;
            case "minecraft:large_fern" -> halfMeta(p, 3);
            case "alexscaves:pewen_log", "alexscaves:pewen_wood" -> pillarMeta(p);
            case "alexscaves:pewen_branch" -> branchMeta(p);
            case "alexscaves:cycad" -> boolMeta(p, "top");
            case "alexscaves:flytrap" -> boolMeta(p, "open");
            case "alexscaves:curly_fern" -> fernMeta(p);
            case "alexscaves:tree_star" -> faceMeta(p);
            default -> -1;
        };
    }

    private static int magmaMeta(Parsed p) {
        if (p.properties.size() != 2) return -1;
        try {
            return (parseBool(p.properties.get("active")) ? 1 : 0) | (parseBool(p.properties.get("permanent")) ? 2 : 0);
        } catch (Exception e) {
            return -1;
        }
    }

    private static int pillarMeta(Parsed p) {
        if (!only(p, "axis")) return -1;
        return switch (p.properties.getOrDefault("axis", "y")) {
            case "y" -> 0;
            case "x" -> 4;
            case "z" -> 8;
            default -> -1;
        };
    }

    private static int branchMeta(Parsed p) {
        if (p.properties.size() != 2 || !p.properties.containsKey("rotation") || !p.properties.containsKey("pines"))
            return -1;
        try {
            int r = Integer.parseInt(p.properties.get("rotation"));
            boolean pins = parseBool(p.properties.get("pines"));
            return r < 0 || r > 7 ? -1 : r + (pins ? 8 : 0);
        } catch (Exception e) {
            return -1;
        }
    }

    private static int boolMeta(Parsed p, String key) {
        if (!only(p, key)) return -1;
        try {
            return parseBool(p.properties.getOrDefault(key, "false")) ? 1 : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    private static int halfMeta(Parsed p, int base) {
        if (!only(p, "half")) return -1;
        return switch (p.properties.getOrDefault("half", "lower")) {
            case "lower" -> base;
            case "upper" -> base + 8;
            default -> -1;
        };
    }

    private static int fernMeta(Parsed p) {
        if (p.properties.size() != 2 || !p.properties.containsKey("half") || !p.properties.containsKey("facing"))
            return -1;
        int d = directionMeta(p.properties.get("facing"));
        if (d < 0) return -1;
        return switch (p.properties.get("half")) {
            case "lower" -> d;
            case "upper" -> d + 4;
            default -> -1;
        };
    }

    private static int faceMeta(Parsed p) {
        if (!only(p, "facing")) return -1;
        return switch (p.properties.getOrDefault("facing", "down")) {
            case "down" -> 0;
            case "up" -> 1;
            case "north" -> 2;
            case "south" -> 3;
            case "west" -> 4;
            case "east" -> 5;
            default -> -1;
        };
    }

    private static boolean parseBool(String value) {
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;
        throw new IllegalArgumentException();
    }

    private static int directionMeta(String d) {
        return switch (d) {
            case "north" -> 0;
            case "east" -> 1;
            case "south" -> 2;
            case "west" -> 3;
            default -> -1;
        };
    }

    private static String direction(int m) {
        return switch (m) {
            case 1 -> "east";
            case 2 -> "south";
            case 3 -> "west";
            default -> "north";
        };
    }

    private static String face(int m) {
        return switch (m) {
            case 1 -> "up";
            case 2 -> "north";
            case 3 -> "south";
            case 4 -> "west";
            case 5 -> "east";
            default -> "down";
        };
    }

    private static boolean only(Parsed p, String key) {
        return p.properties.isEmpty() || p.properties.size() == 1 && p.properties.containsKey(key);
    }

    private static String plain(String id, int meta) {
        return meta == 0 ? id : null;
    }

    private static String pillar(String id, int meta) {
        return switch (meta) {
            case 0 -> id + "[axis=y]";
            case 4 -> id + "[axis=x]";
            case 8 -> id + "[axis=z]";
            default -> null;
        };
    }

    private static Block packedMud() {
        Block b = ModBlocks.PACKED_MUD.get();
        return b == null ? Blocks.hardened_clay : b;
    }

    private static Block moss() {
        Block b = ModBlocks.MOSS_BLOCK.get();
        return b == null ? Blocks.mossy_cobblestone : b;
    }

    private static Block smoothBasalt() {
        Block b = ModBlocks.SMOOTH_BASALT.get();
        return b == null ? Blocks.stone : b;
    }

    private record Parsed(String id, Map<String, String> properties) {

        static Parsed parse(String value) {
            if (value == null) return null;
            int bracket = value.indexOf('[');
            String id = bracket < 0 ? value : value.substring(0, bracket);
            if (id.isEmpty() || bracket >= 0 && !value.endsWith("]")) return null;
            Map<String, String> props = new LinkedHashMap<>();
            if (bracket >= 0) {
                String body = value.substring(bracket + 1, value.length() - 1);
                if (body.isEmpty()) return null;
                for (String part : body.split(",")) {
                    int equals = part.indexOf('=');
                    if (equals < 1 || equals == part.length() - 1
                        || props.put(part.substring(0, equals), part.substring(equals + 1)) != null) return null;
                }
            }
            return new Parsed(id, props);
        }
    }
}
