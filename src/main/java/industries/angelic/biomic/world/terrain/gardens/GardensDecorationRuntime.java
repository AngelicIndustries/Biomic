package industries.angelic.biomic.world.terrain.gardens;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.material.Material;

import industries.angelic.biomic.content.gardens.BlockSoulSpore;
import industries.angelic.biomic.content.gardens.GardensStateCodec;
import industries.angelic.biomic.content.gardens.GardensStateCodec.State;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/**
 * Anchor-level Gardens feature planner. It never writes a World: callers validate and commit one immutable proposal,
 * so a habitat/bounds/protection failure rejects the complete source feature rather than leaving a partial fungus.
 */
public final class GardensDecorationRuntime {

    private static final int MAX_WRITES = 32768, MAX_RADIUS = 48;
    private static final State AIR = state("minecraft:air"), SOUL_SAND = state("minecraft:soul_sand"),
        SOUL_SOIL = state("minecraft:soul_soil"), NETHERRACK = state("minecraft:netherrack"),
        NETHER_WART = state("minecraft:nether_wart_block"), SHROOMLIGHT = state("minecraft:shroomlight"),
        CRIMSON_FUNGUS = state("minecraft:crimson_fungus"), WARPED_FUNGUS = state("minecraft:warped_fungus"),
        CRIMSON_ROOTS = state("minecraft:crimson_roots"), CRIMSON_NYLIUM = state("minecraft:crimson_nylium"),
        CRIMSON_STEM = state("minecraft:crimson_stem[axis=y]"), WEEPING_BODY = state("minecraft:weeping_vines_plant"),
        WEEPING_HEAD = state("minecraft:weeping_vines[age=25]"), SPROUTS = state("biomic:gardens_soulblight_sprouts"),
        FUNGUS = state("biomic:gardens_soulblight_fungus"), STEM = state("biomic:gardens_soulblight_stem"),
        WART = state("biomic:gardens_blightwart_block"), BLISTER = state("biomic:gardens_blistercrown"),
        TALL_LOWER = state("biomic:gardens_tall_blistercrown[legacy_meta=0]"),
        TALL_UPPER = state("biomic:gardens_tall_blistercrown[legacy_meta=8]"),
        CANE = state("biomic:gardens_whistlecane[legacy_meta=0]");

    public record Position(int x, int y, int z) {}

    public record Proposal(Map<Position, State> writes) {

        public Proposal {
            writes = Collections.unmodifiableMap(new LinkedHashMap<>(writes));
        }
    }

    public record Result(Proposal proposal) {

        static final Result REJECTED = new Result(null);

        public boolean accepted() {
            return proposal != null;
        }
    }

    public interface Access {

        State state(int x, int y, int z);

        boolean habitat(String profile, int x, int y, int z);

        /** Null accepts this write; a non-null stable reason rejects the complete feature. */
        String writeRejection(int x, int y, int z, State before, State after);
    }

    private GardensDecorationRuntime() {}

    public static Result plan(String profile, String feature, ModernRandom random, int x, int y, int z, Access access) {
        if (GardensProfiles.profile(profile) == null) return Result.REJECTED;
        if (random == null || access == null) return Result.REJECTED;
        Attempt attempt = new Attempt(profile, new Position(x, y, z), access);
        try {
            boolean generated = switch (feature) {
                case "soulblight_fungi" -> hugeFungus(attempt, random, x, y, z);
                case "soulblight_forest_vegetation" -> vegetation(attempt, random, x, y, z, true);
                case "short_standing_soul_spore_patch" -> sporePatch(
                    attempt,
                    random,
                    x,
                    y,
                    z,
                    false,
                    3,
                    .05F,
                    SOUL_SOIL);
                case "long_standing_soul_spore_patch" -> sporePatch(
                    attempt,
                    random,
                    x,
                    y,
                    z,
                    false,
                    8,
                    .05F,
                    SOUL_SAND);
                case "short_hanging_soul_spore_patch" -> sporePatch(attempt, random, x, y, z, true, 3, 0, SOUL_SOIL);
                case "long_hanging_soul_spore_patch" -> sporePatch(attempt, random, x, y, z, true, 8, .05F, SOUL_SAND);
                case "nether_wart_block_pile" -> pile(attempt, random, x, y, z);
                case "whistlecane_column" -> whistlecane(attempt, random, x, y, z);
                case "tall_blistercrown_patch" -> tallBlister(attempt, random, x, y, z);
                case "whistling_woods_vegetation" -> vegetation(attempt, random, x, y, z, false);
                case "dense_weeping_vines" -> weepingVines(attempt, random, x, y, z);
                case "noisy_crimson_fungi" -> crimsonFungus(attempt, random, x, y, z);
                default -> throw REJECT;
            };
            return generated && !attempt.writes.isEmpty() ? new Result(new Proposal(attempt.writes)) : Result.REJECTED;
        } catch (Rejected rejected) {
            return Result.REJECTED;
        }
    }

    private static boolean vegetation(Attempt a, ModernRandom r, int x, int y, int z, boolean soulblight) {
        if (soulblight && !soulGround(a.get(x, y - 1, z))) return false;
        int placed = 0;
        for (int i = 0; i < 64; i++) {
            int px = x + r.nextInt(8) - r.nextInt(8), py = y + r.nextInt(4) - r.nextInt(4),
                pz = z + r.nextInt(8) - r.nextInt(8);
            State chosen;
            int value = r.nextInt(100);
            if (soulblight) chosen = switch (GardensPlantRules.soulblightVegetationSlot(value)) {
                case 0 -> SPROUTS;
                case 1 -> FUNGUS;
                case 2 -> CRIMSON_FUNGUS;
                default -> WARPED_FUNGUS;
            };
            else chosen = value < 70 ? CRIMSON_ROOTS
                : value < 88 ? BLISTER : value < 98 ? CRIMSON_FUNGUS : WARPED_FUNGUS;
            if (a.air(px, py, pz) && !sameBlock(a.get(px, py - 1, pz), chosen)
                && vegetationSurvives(soulblight, chosen, a.get(px, py - 1, pz))) {
                a.set(px, py, pz, chosen);
                placed++;
            }
        }
        return placed > 0;
    }

    private static boolean sporePatch(Attempt a, ModernRandom r, int x, int y, int z, boolean down, int maximum,
        float glow, State support) {
        int placed = 0, direction = down ? -1 : 1;
        for (int trial = 0; trial < 64; trial++) {
            int px = x + r.nextInt(8) - r.nextInt(8), py = y + r.nextInt(4) - r.nextInt(4),
                pz = z + r.nextInt(8) - r.nextInt(8);
            if (!a.air(px, py, pz) || !sameBlock(a.get(px, py - direction, pz), support)) continue;
            int length = 1 + r.nextInt(maximum);
            for (int step = 1; step <= length && a.air(px, py, pz); step++, py += direction) {
                boolean terminal = step == length || !a.air(px, py + direction, pz);
                State write = terminal && r.nextFloat() < glow && step > 1
                    ? state("biomic:gardens_glowing_soul_spore[legacy_meta=" + (down ? 1 : 0) + "]")
                    : state(
                        "biomic:gardens_soul_spore[legacy_meta=" + BlockSoulSpore.metadata(down, terminal, false)
                            + "]");
                a.set(px, py, pz, write);
                placed++;
                if (terminal) break;
            }
        }
        return placed > 0;
    }

    private static boolean whistlecane(Attempt a, ModernRandom r, int x, int y, int z) {
        if (!a.air(x, y, z) || !survives(CANE, a.get(x, y - 1, z))) return false;
        int length = 2 + r.nextInt(5), placed = 0;
        for (int i = 0; i < length && a.air(x, y + i, z); i++) {
            a.set(x, y + i, z, CANE);
            placed++;
        }
        return placed > 0;
    }

    private static boolean tallBlister(Attempt a, ModernRandom r, int x, int y, int z) {
        int placed = 0;
        for (int i = 0; i < 32; i++) {
            int px = x + r.nextInt(8) - r.nextInt(8), py = y + r.nextInt(4) - r.nextInt(4),
                pz = z + r.nextInt(8) - r.nextInt(8);
            if (a.air(px, py, pz) && a.air(px, py + 1, pz) && nylium(a.get(px, py - 1, pz))) {
                a.set(px, py, pz, TALL_LOWER);
                a.set(px, py + 1, pz, TALL_UPPER);
                placed++;
            }
        }
        return placed > 0;
    }

    private static boolean pile(Attempt a, ModernRandom r, int x, int y, int z) {
        if (y < 5) return false;
        int radiusX = 2 + r.nextInt(2), radiusZ = 2 + r.nextInt(2), height = 1 + r.nextInt(2), placed = 0;
        for (int px = x - radiusX; px <= x + radiusX; px++)
            for (int pz = z - radiusZ; pz <= z + radiusZ; pz++) for (int py = y; py <= y + height; py++) {
                int dx = px - x, dz = pz - z;
                if (dx * dx + dz * dz > (radiusX + .5) * (radiusZ + .5) || !a.air(px, py, pz)) continue;
                State below = a.get(px, py - 1, pz);
                if (!solid(below) || r.nextFloat() > .75F) continue;
                a.set(px, py, pz, r.nextInt(11) == 0 ? SHROOMLIGHT : NETHER_WART);
                placed++;
            }
        return placed > 0;
    }

    private static boolean hugeFungus(Attempt a, ModernRandom r, int ox, int oy, int oz) {
        if (!sameBlock(a.get(ox, oy - 1, oz), SOUL_SOIL)) return false;
        int height = 4 + r.nextInt(6);
        if (oy + height + 1 > GardensProfiles.SOURCE_MAX_Y) return false;
        a.set(ox, oy, oz, AIR);
        for (int dy = 0; dy < height; dy++) if (replaceable(a.get(ox, oy + dy, oz))) a.set(ox, oy + dy, oz, STEM);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) {
            boolean corner = dx != 0 && dz != 0;
            if ((!corner && r.nextFloat() <= .75F) || (corner && r.nextFloat() < .25F)) {
                if (!a.air(ox + dx, oy - 1, oz + dz) && replaceable(a.get(ox + dx, oy, oz + dz))) {
                    a.set(ox + dx, oy, oz + dz, STEM);
                    if (!corner && r.nextFloat() < .25F && replaceable(a.get(ox + dx, oy + 1, oz + dz)))
                        a.set(ox + dx, oy + 1, oz + dz, STEM);
                }
            }
            if ((!corner && r.nextFloat() <= .75F) || (corner && r.nextFloat() < .25F)) {
                int py = oy + height - 1;
                if (replaceable(a.get(ox + dx, py, oz + dz))) {
                    a.set(ox + dx, py, oz + dz, STEM);
                    if (height > 4 && !corner && r.nextFloat() < .25F && replaceable(a.get(ox + dx, py - 1, oz + dz)))
                        a.set(ox + dx, py - 1, oz + dz, STEM);
                }
            }
        }
        float wobbliness = .1F + r.nextFloat() * .15F, wobbleAngle = r.nextFloat() * (float) Math.PI * 2,
            size = (6 + r.nextFloat() * 3) / 2;
        int maximum = (int) Math.ceil(size * 1.25F);
        boolean glowing = r.nextFloat() < .4F;
        for (int dx = -maximum; dx <= maximum; dx++) for (int dz = -maximum; dz <= maximum; dz++) {
            float angle = dx == 0 && dz == 0 ? 0 : (float) Math.atan2(dx, dz),
                wobble = 1 + wobbliness * (float) Math.cos(angle - wobbleAngle);
            float distance = dx * dx + dz * dz;
            if (wobble * wobble * distance > size * size) continue;
            boolean edge = wobble * wobble * distance > (size - 1.5F) * (size - 1.5F);
            int py = oy + height - (edge ? 1 : 0), px = ox + dx, pz = oz + dz;
            if (!replaceable(a.get(px, py, pz))) continue;
            a.set(px, py, pz, WART);
            if (!edge && r.nextFloat() < .05F && a.air(px, py - 1, pz)) a.set(px, py - 1, pz, SHROOMLIGHT);
            else if (r.nextFloat() < (edge ? .3F : .75F) && a.air(px, py - 1, pz))
                column(a, r, px, py - 1, pz, 2 + r.nextInt(2) + (edge ? 0 : 1), edge && glowing ? 1 : 0);
            if (r.nextFloat() < .2F && a.air(px, py + 1, pz)) a.set(px, py + 1, pz, SPROUTS);
        }
        return true;
    }

    /**
     * The configured feature used by the source is vanilla's weeping-vines feature; density is the slot's 40 anchors.
     */
    private static boolean weepingVines(Attempt a, ModernRandom r, int x, int y, int z) {
        if (!a.air(x, y, z) || !wartRoof(a.get(x, y + 1, z))) return false;
        a.set(x, y, z, NETHER_WART);
        for (int i = 0; i < 200; i++) {
            int px = x + r.nextInt(6) - r.nextInt(6), py = y + r.nextInt(2) - r.nextInt(5),
                pz = z + r.nextInt(6) - r.nextInt(6);
            if (!a.air(px, py, pz)) continue;
            int neighbors = 0;
            for (int[] d : DIRECTIONS) if (wartRoof(a.get(px + d[0], py + d[1], pz + d[2])) && ++neighbors > 1) break;
            if (neighbors == 1) a.set(px, py, pz, NETHER_WART);
        }
        for (int i = 0; i < 100; i++) {
            int px = x + r.nextInt(8) - r.nextInt(8), py = y + r.nextInt(2) - r.nextInt(7),
                pz = z + r.nextInt(8) - r.nextInt(8);
            if (!a.air(px, py, pz) || !wartRoof(a.get(px, py + 1, pz))) continue;
            int length = 1 + r.nextInt(8);
            if (r.nextInt(6) == 0) length *= 2;
            if (r.nextInt(5) == 0) length = 1;
            weepingColumn(a, px, py, pz, length);
        }
        return true;
    }

    /** Vanilla crimson-fungus configured feature; the noise-based anchor count remains a scheduler responsibility. */
    private static boolean crimsonFungus(Attempt a, ModernRandom r, int x, int y, int z) {
        if (!sameBlock(a.get(x, y - 1, z), CRIMSON_NYLIUM)) return false;
        int height = 4 + r.nextInt(10);
        if (r.nextInt(12) == 0) height *= 2;
        if (y + height + 2 > GardensProfiles.SOURCE_MAX_Y) return false;
        boolean thick = r.nextFloat() < .06F;
        a.set(x, y, z, AIR);
        a.set(x, y - 1, z, NETHERRACK);
        int stemRadius = thick ? 1 : 0;
        for (int dx = -stemRadius; dx <= stemRadius; dx++) for (int dz = -stemRadius; dz <= stemRadius; dz++) {
            boolean corner = thick && Math.abs(dx) == stemRadius && Math.abs(dz) == stemRadius;
            for (int dy = 0; dy < height; dy++)
                if (replaceable(a.get(x + dx, y + dy, z + dz)) && (!corner || r.nextFloat() < .1F))
                    a.set(x + dx, y + dy, z + dz, CRIMSON_STEM);
        }
        int capDepth = Math.min(r.nextInt(1 + height / 3) + 5, height), bottom = height - capDepth;
        for (int dy = bottom; dy <= height; dy++) {
            int radius = dy < height - r.nextInt(3) ? 2 : 1;
            if (capDepth > 8 && dy < bottom + 4) radius = 3;
            if (thick) radius++;
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                boolean edgeX = dx == -radius || dx == radius, edgeZ = dz == -radius || dz == radius;
                boolean inside = !edgeX && !edgeZ && dy != height, corner = edgeX && edgeZ, drop = dy < bottom + 3;
                int px = x + dx, py = y + dy, pz = z + dz;
                if (!replaceable(a.get(px, py, pz))) continue;
                if (drop) {
                    if (!inside) crimsonDrop(a, r, px, py, pz);
                } else if (inside) crimsonHat(a, r, px, py, pz, .1F, .2F, .1F);
                else if (corner) crimsonHat(a, r, px, py, pz, .01F, .7F, .083F);
                else crimsonHat(a, r, px, py, pz, .0005F, .98F, .07F);
            }
        }
        return true;
    }

    private static void crimsonHat(Attempt a, ModernRandom r, int x, int y, int z, float light, float wart,
        float vine) {
        if (r.nextFloat() < light) a.set(x, y, z, SHROOMLIGHT);
        else if (r.nextFloat() < wart) {
            a.set(x, y, z, NETHER_WART);
            if (r.nextFloat() < vine) tryWeeping(a, r, x, y, z);
        }
    }

    private static void crimsonDrop(Attempt a, ModernRandom r, int x, int y, int z) {
        if (sameBlock(a.get(x, y - 1, z), NETHER_WART)) a.set(x, y, z, NETHER_WART);
        else if (r.nextFloat() < .15F) {
            a.set(x, y, z, NETHER_WART);
            if (r.nextInt(11) == 0) tryWeeping(a, r, x, y, z);
        }
    }

    private static void tryWeeping(Attempt a, ModernRandom r, int x, int y, int z) {
        if (a.air(x, y - 1, z)) {
            int length = 1 + r.nextInt(5);
            if (r.nextInt(7) == 0) length *= 2;
            weepingColumn(a, x, y - 1, z, length);
        }
    }

    private static void weepingColumn(Attempt a, int x, int y, int z, int length) {
        for (int i = 0; i <= length && a.air(x, y - i, z); i++) {
            boolean terminal = i == length || !a.air(x, y - i - 1, z);
            a.set(x, y - i, z, terminal ? WEEPING_HEAD : WEEPING_BODY);
            if (terminal) return;
        }
    }

    private static boolean wartRoof(State state) {
        return sameBlock(state, NETHERRACK) || sameBlock(state, NETHER_WART);
    }

    private static void column(Attempt a, ModernRandom r, int x, int y, int z, int length, float glow) {
        int direction = -1;
        for (int i = 1; i <= length && a.air(x, y, z); i++, y += direction) {
            boolean terminal = i == length || !a.air(x, y + direction, z);
            State write = terminal && r.nextFloat() < glow && i > 1
                ? state("biomic:gardens_glowing_soul_spore[legacy_meta=" + 1 + "]")
                : state(
                    "biomic:gardens_soul_spore[legacy_meta=" + BlockSoulSpore.metadata(true, terminal, false) + "]");
            a.set(x, y, z, write);
            if (terminal) return;
        }
    }

    private static boolean survives(State plant, State below) {
        if (sameBlock(plant, SPROUTS) || sameBlock(plant, FUNGUS)) return soulGround(below);
        if (sameBlock(plant, CANE)) return sameBlock(below, SOUL_SAND) || sameBlock(below, SOUL_SOIL) || nylium(below);
        return nylium(below);
    }

    private static boolean vegetationSurvives(boolean soulblight, State plant, State below) {
        // Modern FungusBlock explicitly accepts soul soil. The source feature starts on soul sand/soil and then
        // asks each weighted state (including crimson/warped fungus) whether it survives at the scattered target.
        if (soulblight && (sameBlock(plant, CRIMSON_FUNGUS) || sameBlock(plant, WARPED_FUNGUS)))
            return GardensPlantRules.sourceFungusSurvivesOn(plant.canonical(), below.canonical());
        return survives(plant, below);
    }

    private static boolean soulGround(State state) {
        return sameBlock(state, SOUL_SAND) || sameBlock(state, SOUL_SOIL) || sameBlock(state, WART);
    }

    private static boolean nylium(State state) {
        return state.canonical()
            .startsWith("minecraft:crimson_nylium")
            || state.canonical()
                .startsWith("minecraft:warped_nylium");
    }

    private static boolean replaceable(State state) {
        return state.block()
            .getMaterial() == Material.air || isPlant(state);
    }

    private static boolean isPlant(State state) {
        return state.block()
            .getMaterial() == Material.plants
            || state.block()
                .getMaterial() == Material.vine;
    }

    private static boolean solid(State state) {
        return state.block()
            .getMaterial()
            .blocksMovement();
    }

    private static boolean sameBlock(State a, State b) {
        return a.block() == b.block() && a.metadata() == b.metadata();
    }

    private static State state(String value) {
        return GardensStateCodec.resolve(value);
    }

    private static final int[][] DIRECTIONS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    private static final class Attempt {

        final String profile;
        final Position anchor;
        final Access access;
        final Map<Position, State> writes = new LinkedHashMap<>();

        Attempt(String profile, Position anchor, Access access) {
            this.profile = profile;
            this.anchor = anchor;
            this.access = access;
        }

        State get(int x, int y, int z) {
            State state = writes.get(new Position(x, y, z));
            return state == null ? access.state(x, y, z) : state;
        }

        boolean air(int x, int y, int z) {
            return get(x, y, z).block()
                .getMaterial() == Material.air;
        }

        void set(int x, int y, int z, State after) {
            if (y < 1 || y >= GardensProfiles.SOURCE_MAX_Y) throw REJECT;
            if (Math.abs((long) x - anchor.x) > MAX_RADIUS || Math.abs((long) z - anchor.z) > MAX_RADIUS) throw REJECT;
            if (!access.habitat(profile, x, y, z)) throw REJECT;
            Position position = new Position(x, y, z);
            State before = get(x, y, z);
            String reason = access.writeRejection(x, y, z, before, after);
            if (reason != null) throw REJECT;
            if (writes.size() >= MAX_WRITES && !writes.containsKey(position)) throw REJECT;
            writes.put(position, after);
        }
    }

    /** A feature that cannot be placed whole. */
    private static final Rejected REJECT = new Rejected();

    private static final class Rejected extends RuntimeException {

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }
}
