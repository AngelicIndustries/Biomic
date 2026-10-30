package industries.angelic.biomic.content.alexscaves;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure, strict modern-state to legacy-state adapter for vanilla furnishings (anvils, doors, trapdoors, ladders,
 * pistons,
 * buttons, lamps, redstone) that generation data names, written for the Abyssal ruins. The ruins' own wet and rusty
 * targets map to blocks in the unused package and resolve to nothing in {@link AbyssalStateCodec}.
 */
public final class RustyVanillaFurnishings {

    public enum Target {
        ANVIL,
        IRON_DOOR,
        IRON_TRAPDOOR,
        WET_IRON_TRAPDOOR,
        LADDER,
        LIGHTNING_ROD,
        PISTON,
        STATIC_PISTON_HEAD,
        RED_STAINED_GLASS,
        REDSTONE_LAMP,
        REDSTONE_WIRE,
        POWERED_REPEATER,
        SMITHING_TABLE,
        SPRUCE_TRAPDOOR,
        STICKY_PISTON,
        STONE_BUTTON,
        CEILING_BUTTON,
        WET_CHAIN,
        WET_WEATHERED_SLAB,
        WET_OXIDIZED_SLAB,
        WET_WEATHERED_STAIR,
        WET_WEATHERED_INNER_STAIR,
        WET_WAXED_WEATHERED_STAIR,
        WET_WAXED_WEATHERED_INNER_STAIR,
        WET_WAXED_OXIDIZED_STAIR,
        WET_WAXED_OXIDIZED_INNER_STAIR
    }

    public record Mapping(Target target, int metadata) {}

    private RustyVanillaFurnishings() {}

    public static Mapping map(String encoded) {
        Parsed p = Parsed.parse(encoded);
        if (p == null) return null;
        int meta;
        switch (p.id) {
            case "minecraft:damaged_anvil":
                if (!p.only("facing") || (meta = anvilFacing(p.value("facing"))) < 0) return null;
                return mapping(Target.ANVIL, meta | 8);
            case "minecraft:iron_door":
                return door(p);
            case "minecraft:iron_trapdoor":
                return trapdoor(p, true);
            case "minecraft:ladder":
                if (!p.only("facing", "waterlogged") || !p.is("waterlogged", "false")
                    || (meta = attachedFacing(p.value("facing"))) < 0) return null;
                return mapping(Target.LADDER, meta);
            case "minecraft:lightning_rod":
                if (!p.only("facing", "powered", "waterlogged") || !p.is("powered", "false")
                    || !p.is("waterlogged", "true")
                    || (meta = facing(p.value("facing"))) < 1) return null;
                return mapping(Target.LIGHTNING_ROD, meta);
            case "minecraft:piston":
                return piston(p, false);
            case "minecraft:piston_head":
                if (!p.only("facing", "short", "type") || !p.is("short", "false")
                    || !p.is("type", "sticky")
                    || (meta = facing(p.value("facing"))) < 0) return null;
                return mapping(Target.STATIC_PISTON_HEAD, meta | 8);
            case "minecraft:red_stained_glass":
                return p.properties.isEmpty() ? mapping(Target.RED_STAINED_GLASS, 14) : null;
            case "minecraft:redstone_lamp":
                return p.only("lit") && p.is("lit", "false") ? mapping(Target.REDSTONE_LAMP, 0) : null;
            case "minecraft:redstone_wire":
                return wire(p);
            case "minecraft:repeater":
                if (!p.only("delay", "facing", "locked", "powered") || !p.is("delay", "1")
                    || !p.is("locked", "false")
                    || !p.is("powered", "true")
                    || (meta = horizontal(p.value("facing"))) < 0) return null;
                return mapping(Target.POWERED_REPEATER, meta);
            case "minecraft:smithing_table":
                return p.properties.isEmpty() ? mapping(Target.SMITHING_TABLE, 0) : null;
            case "minecraft:spruce_trapdoor":
                return trapdoor(p, false);
            case "minecraft:sticky_piston":
                return piston(p, true);
            case "minecraft:stone_button":
                return button(p);
            case "minecraft:chain":
                return p.only("axis", "waterlogged") && p.is("axis", "y") && p.is("waterlogged", "true")
                    ? mapping(Target.WET_CHAIN, 0)
                    : null;
            case "minecraft:weathered_cut_copper_slab":
                return wetSlab(p, Target.WET_WEATHERED_SLAB);
            case "minecraft:oxidized_cut_copper_slab":
                return wetSlab(p, Target.WET_OXIDIZED_SLAB);
            case "minecraft:weathered_cut_copper_stairs":
                return wetStair(p, Target.WET_WEATHERED_STAIR, Target.WET_WEATHERED_INNER_STAIR);
            case "minecraft:waxed_weathered_cut_copper_stairs":
                return wetStair(p, Target.WET_WAXED_WEATHERED_STAIR, Target.WET_WAXED_WEATHERED_INNER_STAIR);
            case "minecraft:waxed_oxidized_cut_copper_stairs":
                return wetStair(p, Target.WET_WAXED_OXIDIZED_STAIR, Target.WET_WAXED_OXIDIZED_INNER_STAIR);
            default:
                return null;
        }
    }

    private static Mapping wetSlab(Parsed p, Target target) {
        if (!p.only("type", "waterlogged") || !p.is("waterlogged", "true")) return null;
        return p.is("type", "bottom") ? mapping(target, 0) : p.is("type", "top") ? mapping(target, 1) : null;
    }

    private static Mapping wetStair(Parsed p, Target straight, Target inner) {
        if (!p.only("facing", "half", "shape", "waterlogged") || !p.is("waterlogged", "true")) return null;
        int meta = stairFacing(p.value("facing"));
        if (meta < 0 || !Set.of("bottom", "top")
            .contains(p.value("half"))) return null;
        if (p.is("half", "top")) meta |= 4;
        String shape = p.value("shape");
        if (shape.equals("straight")) return mapping(straight, meta);
        if (shape.equals("inner_right")) return mapping(inner, meta | 8);
        if (shape.equals("inner_left")) return mapping(inner, meta);
        return null;
    }

    private static Mapping door(Parsed p) {
        if (!p.only("facing", "half", "hinge", "open", "powered")) return null;
        String half = p.value("half"), facing = p.value("facing"), hinge = p.value("hinge");
        if (!horizontalCardinal(facing) || !Set.of("left", "right")
            .contains(hinge)) return null;
        boolean opened = hinge.equals("left");
        if (!p.is("open", opened ? "true" : "false") || !p.is("powered", opened ? "true" : "false")) return null;
        if (half.equals("upper")) {
            return mapping(Target.IRON_DOOR, hinge.equals("right") ? 9 : 8);
        }
        if (!half.equals("lower")) return null;
        int meta = doorFacing(facing);
        if (p.is("open", "true")) meta |= 4;
        return mapping(Target.IRON_DOOR, meta);
    }

    private static Mapping trapdoor(Parsed p, boolean iron) {
        if (!trapdoorProperties(p)) return null;
        if (iron && p.is("waterlogged", "true")) {
            int facing = facing(p.value("facing"));
            return p.is("half", "top") && p.is("open", "false") && p.is("powered", "false") && facing >= 2
                ? mapping(Target.WET_IRON_TRAPDOOR, facing)
                : null;
        }
        if (p.is("waterlogged", "true")) return null;
        if (iron && (!horizontalCardinal(p.value("facing")) || !p.is("half", "top")
            || !p.is("open", "false")
            || !p.is("powered", "false"))) return null;
        if (!iron
            && (!p.is("powered", "false") || !(p.is("half", "bottom") || p.is("half", "top") && p.is("open", "false"))))
            return null;
        int meta = trapdoorFacing(p.value("facing"));
        if (meta < 0) return null;
        if (p.is("open", "true")) meta |= 4;
        if (p.is("half", "top")) meta |= 8;
        return mapping(iron ? Target.IRON_TRAPDOOR : Target.SPRUCE_TRAPDOOR, meta);
    }

    private static boolean trapdoorProperties(Parsed p) {
        return p.only("facing", "half", "open", "powered", "waterlogged") && horizontalCardinal(p.value("facing"))
            && Set.of("bottom", "top")
                .contains(p.value("half"))
            && p.bool("open")
            && p.bool("powered")
            && p.bool("waterlogged");
    }

    private static Mapping piston(Parsed p, boolean sticky) {
        int face;
        if (!p.only("extended", "facing") || (face = facing(p.value("facing"))) < 2
            || !p.is("extended", sticky ? "true" : "false")) return null;
        return mapping(sticky ? Target.STICKY_PISTON : Target.PISTON, face | (sticky ? 8 : 0));
    }

    private static Mapping wire(Parsed p) {
        if (!p.only("east", "north", "power", "south", "west")) return null;
        if (!Set.of("none", "side", "up")
            .containsAll(List.of(p.value("east"), p.value("north"), p.value("south"), p.value("west")))) return null;
        int power = p.integer("power");
        if (power < 0 || power > 15) return null;
        String[] shape = { p.value("east"), p.value("south"), p.value("west"), p.value("north") };
        boolean valid = switch (power) {
            case 12 -> adjacentPair(shape, "side");
            case 14, 15 -> oppositePair(shape, "side");
            case 13 -> risingCorner(shape);
            default -> false;
        };
        if (!valid) return null;
        return mapping(Target.REDSTONE_WIRE, power);
    }

    private static Mapping button(Parsed p) {
        if (!p.only("face", "facing", "powered") || !p.is("powered", "false")) return null;
        if (p.is("face", "ceiling")) {
            int facing = facing(p.value("facing"));
            return facing >= 2 ? mapping(Target.CEILING_BUTTON, facing) : null;
        }
        if (!p.is("face", "wall")) return null;
        int meta = switch (p.value("facing")) {
            case "east" -> 1;
            case "west" -> 2;
            case "south" -> 3;
            case "north" -> 4;
            default -> -1;
        };
        return meta < 0 ? null : mapping(Target.STONE_BUTTON, meta);
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

    private static int horizontal(String value) {
        return switch (value) {
            case "south" -> 0;
            case "west" -> 1;
            case "north" -> 2;
            case "east" -> 3;
            default -> -1;
        };
    }

    private static int attachedFacing(String value) {
        return switch (value) {
            case "north" -> 2;
            case "south" -> 3;
            case "west" -> 4;
            case "east" -> 5;
            default -> -1;
        };
    }

    private static int trapdoorFacing(String value) {
        return switch (value) {
            case "north" -> 0;
            case "south" -> 1;
            case "west" -> 2;
            case "east" -> 3;
            default -> -1;
        };
    }

    private static int doorFacing(String value) {
        return switch (value) {
            case "east" -> 0;
            case "south" -> 1;
            case "west" -> 2;
            case "north" -> 3;
            default -> -1;
        };
    }

    private static int stairFacing(String value) {
        return switch (value) {
            case "east" -> 0;
            case "west" -> 1;
            case "south" -> 2;
            case "north" -> 3;
            default -> -1;
        };
    }

    private static int anvilFacing(String value) {
        return switch (value) {
            case "north" -> 0;
            case "east" -> 1;
            case "south" -> 2;
            case "west" -> 3;
            default -> -1;
        };
    }

    private static boolean horizontalCardinal(String value) {
        return Set.of("north", "east", "south", "west")
            .contains(value);
    }

    private static boolean adjacentPair(String[] shape, String connected) {
        for (int i = 0; i < 4; i++) if (shape[i].equals(connected) && shape[(i + 1) & 3].equals(connected)
            && shape[(i + 2) & 3].equals("none")
            && shape[(i + 3) & 3].equals("none")) return true;
        return false;
    }

    private static boolean oppositePair(String[] shape, String connected) {
        return shape[0].equals(connected) && shape[2].equals(connected)
            && shape[1].equals("none")
            && shape[3].equals("none")
            || shape[1].equals(connected) && shape[3].equals(connected)
                && shape[0].equals("none")
                && shape[2].equals("none");
    }

    private static boolean risingCorner(String[] shape) {
        for (int i = 0; i < 4; i++) if (shape[i].equals("up") && shape[(i + 1) & 3].equals("side")
            && shape[(i + 2) & 3].equals("none")
            && shape[(i + 3) & 3].equals("none")) return true;
        return false;
    }

    private static Mapping mapping(Target target, int meta) {
        return new Mapping(target, meta);
    }

    private record Parsed(String id, Map<String, String> properties) {

        static Parsed parse(String encoded) {
            if (encoded == null) return null;
            int open = encoded.indexOf('[');
            String id = open < 0 ? encoded : encoded.substring(0, open);
            if (id.isEmpty() || open >= 0 && !encoded.endsWith("]")) return null;
            Map<String, String> properties = new LinkedHashMap<>();
            if (open >= 0 && !encoded.substring(open + 1, encoded.length() - 1)
                .isEmpty()) for (String entry : encoded.substring(open + 1, encoded.length() - 1)
                    .split(",", -1)) {
                        int equals = entry.indexOf('=');
                        if (equals < 1 || equals == entry.length() - 1
                            || properties.put(entry.substring(0, equals), entry.substring(equals + 1)) != null)
                            return null;
                    }
            return new Parsed(id, Collections.unmodifiableMap(properties));
        }

        boolean only(String... keys) {
            return properties.keySet()
                .equals(Set.of(keys));
        }

        boolean is(String key, String value) {
            return value.equals(properties.get(key));
        }

        boolean bool(String key) {
            return is(key, "true") || is(key, "false");
        }

        String value(String key) {
            return properties.get(key);
        }

        int integer(String key) {
            try {
                return Integer.parseInt(value(key));
            } catch (NumberFormatException | NullPointerException ignored) {
                return -1;
            }
        }
    }
}
