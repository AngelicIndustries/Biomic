package industries.angelic.biomic.world.modern;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Exact standalone port of the pinned 1.21.1 world-generation sculk spreader. */
final class ModernVanillaSculkPatchFeature {

    private static final Direction[] DIRECTIONS = Direction.values();
    private static final List<Pos> NON_CORNER_NEIGHBOURS = neighbours();
    private static final String VEIN = "minecraft:sculk_vein";
    private static final String SCULK = "minecraft:sculk";

    boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
        if (!canSpreadFrom(world, origin)) return false;
        int amountPerCharge = config.get("amount_per_charge")
            .getAsInt();
        int chargeCount = config.get("charge_count")
            .getAsInt();
        int spreadAttempts = config.get("spread_attempts")
            .getAsInt();
        int spreadRounds = config.get("spread_rounds")
            .getAsInt();
        int rounds = spreadRounds + config.get("growth_rounds")
            .getAsInt();
        List<Cursor> cursors = new ArrayList<Cursor>();
        for (int round = 0; round < rounds; round++) {
            for (int charge = 0; charge < chargeCount && cursors.size() < 32; charge++) {
                addCursors(cursors, origin, amountPerCharge);
            }
            boolean spread = round < spreadRounds;
            for (int attempt = 0; attempt < spreadAttempts; attempt++) {
                updateCursors(engine, world, origin, random, spread, cursors);
            }
            cursors.clear();
        }

        Pos below = origin.offset(0, -1, 0);
        if (random.nextFloat() <= config.get("catalyst_chance")
            .getAsFloat() && world.isSolid(below.x, below.y, below.z)) {
            world.setBlock(origin.x, origin.y, origin.z, "minecraft:sculk_catalyst");
        }
        int rare = engine.sampleIntValue(config.get("extra_rare_growths"), random);
        for (int index = 0; index < rare; index++) {
            Pos position = origin.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            if (!isAir(world, position) || !world.hasSturdyFace(position.x, position.y - 1, position.z, "up")) continue;
            world.setBlock(
                position.x,
                position.y,
                position.z,
                "minecraft:sculk_shrieker[can_summon=true,shrieking=false,waterlogged=false]");
        }
        return true;
    }

    private static void addCursors(List<Cursor> cursors, Pos origin, int charge) {
        while (charge > 0 && cursors.size() < 32) {
            int amount = Math.min(charge, 1000);
            cursors.add(new Cursor(origin, amount));
            charge -= amount;
        }
    }

    private static void updateCursors(ModernDecorationEngine engine, World world, Pos origin, ModernRandom random,
        boolean spread, List<Cursor> cursors) {
        if (cursors.isEmpty()) return;
        List<Cursor> retained = new ArrayList<Cursor>();
        for (Cursor cursor : cursors) {
            cursor.update(engine, world, origin, random, spread);
            if (cursor.charge > 0) retained.add(cursor);
        }
        cursors.clear();
        cursors.addAll(retained);
    }

    private static final class Cursor {

        private Pos position;
        private int charge;
        private int updateDelay;
        private int decayDelay = 1;
        private Set<Direction> facings;

        Cursor(Pos position, int charge) {
            this.position = position;
            this.charge = charge;
        }

        void update(ModernDecorationEngine engine, World world, Pos origin, ModernRandom random, boolean spread) {
            if (charge <= 0) return;
            if (updateDelay > 0) {
                updateDelay--;
                return;
            }
            String state = world.blockState(position.x, position.y, position.z);
            Behaviour behaviour = behaviour(state);
            if (spread && attemptSpreadVein(engine, world, position, state, facings)) {
                if (behaviour != Behaviour.SCULK) {
                    state = world.blockState(position.x, position.y, position.z);
                    behaviour = behaviour(state);
                }
            }
            charge = attemptUseCharge(engine, world, origin, random, spread, behaviour, this);
            if (charge <= 0) {
                onDischarged(world, state, position);
                return;
            }
            Pos movement = validMovementPos(engine, world, position, random);
            if (movement != null) {
                onDischarged(world, state, position);
                position = movement;
                int dx = position.x - origin.x;
                int dz = position.z - origin.z;
                if ((double) dx * dx + (double) dz * dz >= 225.0D) {
                    charge = 0;
                    return;
                }
                state = world.blockState(position.x, position.y, position.z);
            }
            if (isBehaviour(state)) facings = availableFaces(state);
            decayDelay = behaviour == Behaviour.DEFAULT ? Math.max(decayDelay - 1, 0) : 1;
            updateDelay = 1;
        }
    }

    private static int attemptUseCharge(ModernDecorationEngine engine, World world, Pos origin, ModernRandom random,
        boolean spread, Behaviour behaviour, Cursor cursor) {
        if (behaviour == Behaviour.DEFAULT) {
            return cursor.decayDelay > 0 ? cursor.charge : 0;
        }
        if (behaviour == Behaviour.VEIN) {
            if (spread && attemptPlaceSculk(engine, world, cursor.position, random)) {
                return cursor.charge - 1;
            }
            return random.nextInt(5) == 0 ? (int) Math.floor(cursor.charge * 0.5F) : cursor.charge;
        }

        int charge = cursor.charge;
        if (charge == 0 || random.nextInt(5) != 0) return charge;
        Pos position = cursor.position;
        boolean close = distanceSquared(position, origin) < 1.0D;
        if (close || !canPlaceGrowth(world, position)) {
            if (random.nextInt(10) != 0) return charge;
            return charge - (close ? 1 : decayPenalty(position, origin, charge));
        }
        if (random.nextInt(50) < charge) {
            Pos above = position.offset(0, 1, 0);
            String growth = random.nextInt(11) == 0
                ? "minecraft:sculk_shrieker[can_summon=true,shrieking=false,waterlogged=false]"
                : "minecraft:sculk_sensor[phase=inactive,power=0,waterlogged=false]";
            if (isWater(world, above)) growth = ModernFeatureEngine.property(growth, "waterlogged", "true");
            world.setBlock(above.x, above.y, above.z, growth);
        }
        return Math.max(0, charge - 50);
    }

    private enum Behaviour {
        DEFAULT,
        VEIN,
        SCULK
    }

    private static Behaviour behaviour(String state) {
        String block = ModernDecorationEngine.blockName(state);
        if (VEIN.equals(block)) return Behaviour.VEIN;
        if (SCULK.equals(block)) return Behaviour.SCULK;
        return Behaviour.DEFAULT;
    }

    private static boolean isBehaviour(String state) {
        return behaviour(state) != Behaviour.DEFAULT;
    }

    private static int decayPenalty(Pos position, Pos origin, int charge) {
        float distance = (float) Math.sqrt(distanceSquared(position, origin)) - 1.0F;
        float factor = Math.min(1.0F, distance * distance / (23.0F * 23.0F));
        return Math.max(1, (int) (charge * factor * 0.5F));
    }

    private static double distanceSquared(Pos left, Pos right) {
        double dx = left.x - right.x;
        double dy = left.y - right.y;
        double dz = left.z - right.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean attemptSpreadVein(ModernDecorationEngine engine, World world, Pos position, String state,
        Set<Direction> facings) {
        Behaviour behaviour = behaviour(state);
        if (behaviour != Behaviour.DEFAULT) {
            return spreadAll(engine, world, position, state, false) > 0;
        }
        if (facings == null) {
            return spreadAll(engine, world, position, state, true) > 0;
        }
        if (!facings.isEmpty()) {
            if (isAir(world, position) || isWater(world, position)) {
                return regrow(world, position, facings);
            }
            return false;
        }
        return spreadAll(engine, world, position, state, false) > 0;
    }

    private static int spreadAll(ModernDecorationEngine engine, World world, Pos source, String sourceState,
        boolean sameSpaceOnly) {
        int placed = 0;
        boolean otherSource = !VEIN.equals(ModernDecorationEngine.blockName(sourceState));
        for (Direction sourceFace : DIRECTIONS) {
            if (!otherSource && !hasFace(sourceState, sourceFace)) continue;
            for (Direction toward : DIRECTIONS) {
                if (toward.axis == sourceFace.axis) continue;
                SpreadPos spread = firstSpreadPosition(engine, world, source, sourceFace, toward, sameSpaceOnly);
                if (spread == null) continue;
                String state = stateForPlacement(world, spread.position, spread.face);
                if (state == null) continue;
                if (world.setBlock(spread.position.x, spread.position.y, spread.position.z, state)) placed++;
            }
        }
        return placed;
    }

    private static SpreadPos firstSpreadPosition(ModernDecorationEngine engine, World world, Pos source,
        Direction sourceFace, Direction toward, boolean sameSpaceOnly) {
        int spreadTypes = sameSpaceOnly ? 1 : 3;
        for (int type = 0; type < spreadTypes; type++) {
            SpreadPos spread;
            if (type == 0) {
                spread = new SpreadPos(source, toward);
            } else if (type == 1) {
                spread = new SpreadPos(toward.move(source), sourceFace);
            } else {
                spread = new SpreadPos(sourceFace.move(toward.move(source)), toward.opposite());
            }
            if (canSpreadInto(engine, world, source, spread)) return spread;
        }
        return null;
    }

    private static boolean canSpreadInto(ModernDecorationEngine engine, World world, Pos source, SpreadPos spread) {
        Pos position = spread.position;
        String targetState = world.blockState(position.x, position.y, position.z);
        Pos support = spread.face.move(position);
        String supportBlock = ModernDecorationEngine.blockName(world.blockState(support.x, support.y, support.z));
        if (SCULK.equals(supportBlock) || "minecraft:sculk_catalyst".equals(supportBlock)
            || "minecraft:moving_piston".equals(supportBlock)) return false;
        if (manhattan(source, position) == 2) {
            Pos behind = spread.face.opposite()
                .move(source);
            if (world.hasSturdyFace(behind.x, behind.y, behind.z, spread.face.name)) return false;
        }
        String fluid = world.fluidState(position.x, position.y, position.z);
        if (!isEmptyFluid(fluid) && !isWaterFluid(fluid)) return false;
        if (engine.isBlockInTag("#minecraft:fire", targetState)) return false;
        String targetBlock = ModernDecorationEngine.blockName(targetState);
        boolean replaceable = world.isReplaceable(position.x, position.y, position.z) || isAirBlock(targetBlock)
            || VEIN.equals(targetBlock)
            || "minecraft:water".equals(targetBlock);
        return replaceable && !hasFace(targetState, spread.face)
            && world.hasSturdyFace(support.x, support.y, support.z, spread.face.opposite().name);
    }

    private static String stateForPlacement(World world, Pos position, Direction face) {
        String current = world.blockState(position.x, position.y, position.z);
        if (hasFace(current, face)) return null;
        String state = VEIN.equals(ModernDecorationEngine.blockName(current)) ? current
            : veinState(isWater(world, position));
        return ModernFeatureEngine.property(state, face.name, "true");
    }

    private static boolean regrow(World world, Pos position, Set<Direction> facings) {
        String state = veinState(isWater(world, position));
        boolean attached = false;
        for (Direction face : facings) {
            Pos support = face.move(position);
            if (!world.hasSturdyFace(support.x, support.y, support.z, face.opposite().name)) continue;
            state = ModernFeatureEngine.property(state, face.name, "true");
            attached = true;
        }
        if (attached) world.setBlock(position.x, position.y, position.z, state);
        return attached;
    }

    private static boolean attemptPlaceSculk(ModernDecorationEngine engine, World world, Pos position,
        ModernRandom random) {
        String state = world.blockState(position.x, position.y, position.z);
        for (Direction direction : shuffledDirections(random)) {
            Pos target = direction.move(position);
            if (!hasFace(state, direction) || !engine
                .isBlockInTag("#minecraft:sculk_replaceable_world_gen", world.blockState(target.x, target.y, target.z)))
                continue;
            world.setBlock(target.x, target.y, target.z, SCULK);
            spreadAll(engine, world, target, SCULK, false);
            Direction ignored = direction.opposite();
            for (Direction neighborDirection : DIRECTIONS) {
                if (neighborDirection == ignored) continue;
                Pos neighbor = neighborDirection.move(target);
                String neighborState = world.blockState(neighbor.x, neighbor.y, neighbor.z);
                if (VEIN.equals(ModernDecorationEngine.blockName(neighborState))) {
                    onDischarged(world, neighborState, neighbor);
                }
            }
            return true;
        }
        return false;
    }

    private static void onDischarged(World world, String state, Pos position) {
        if (!VEIN.equals(ModernDecorationEngine.blockName(state))) return;
        String updated = state;
        for (Direction direction : DIRECTIONS) {
            if (!hasFace(updated, direction)) continue;
            Pos neighbor = direction.move(position);
            if (SCULK.equals(ModernDecorationEngine.blockName(world.blockState(neighbor.x, neighbor.y, neighbor.z)))) {
                updated = ModernFeatureEngine.property(updated, direction.name, "false");
            }
        }
        if (availableFaces(updated).isEmpty()) {
            updated = propertyTrue(updated, "waterlogged") ? "minecraft:water[level=0]" : "minecraft:air";
        }
        world.setBlock(position.x, position.y, position.z, updated);
    }

    private static Pos validMovementPos(ModernDecorationEngine engine, World world, Pos position, ModernRandom random) {
        Pos candidate = null;
        for (Pos offset : shuffledNeighbours(random)) {
            Pos target = position.add(offset);
            String state = world.blockState(target.x, target.y, target.z);
            if (!isBehaviour(state) || !movementUnobstructed(world, position, target)) continue;
            candidate = target;
            if (hasSubstrateAccess(engine, world, state, target)) break;
        }
        return candidate;
    }

    private static boolean hasSubstrateAccess(ModernDecorationEngine engine, World world, String state, Pos position) {
        if (!VEIN.equals(ModernDecorationEngine.blockName(state))) return false;
        for (Direction direction : DIRECTIONS) {
            if (!hasFace(state, direction)) continue;
            Pos support = direction.move(position);
            if (engine.isBlockInTag("#minecraft:sculk_replaceable", world.blockState(support.x, support.y, support.z)))
                return true;
        }
        return false;
    }

    private static boolean movementUnobstructed(World world, Pos from, Pos to) {
        int dx = to.x - from.x;
        int dy = to.y - from.y;
        int dz = to.z - from.z;
        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 1) return true;
        Direction x = dx < 0 ? Direction.WEST : Direction.EAST;
        Direction y = dy < 0 ? Direction.DOWN : Direction.UP;
        Direction z = dz < 0 ? Direction.NORTH : Direction.SOUTH;
        if (dx == 0) return unobstructed(world, from, y) || unobstructed(world, from, z);
        if (dy == 0) return unobstructed(world, from, x) || unobstructed(world, from, z);
        return unobstructed(world, from, x) || unobstructed(world, from, y);
    }

    private static boolean unobstructed(World world, Pos position, Direction direction) {
        Pos neighbor = direction.move(position);
        return !world.hasSturdyFace(neighbor.x, neighbor.y, neighbor.z, direction.opposite().name);
    }

    private static boolean canPlaceGrowth(World world, Pos position) {
        Pos above = position.offset(0, 1, 0);
        String upper = ModernDecorationEngine.blockName(world.blockState(above.x, above.y, above.z));
        if (!isAirBlock(upper) && !"minecraft:water".equals(upper)) return false;
        int growths = 0;
        for (int z = -4; z <= 4; z++) {
            for (int y = 0; y <= 2; y++) {
                for (int x = -4; x <= 4; x++) {
                    String block = ModernDecorationEngine
                        .blockName(world.blockState(position.x + x, position.y + y, position.z + z));
                    if ("minecraft:sculk_sensor".equals(block) || "minecraft:sculk_shrieker".equals(block)) growths++;
                    if (growths > 2) return false;
                }
            }
        }
        return true;
    }

    private static boolean canSpreadFrom(World world, Pos position) {
        String state = world.blockState(position.x, position.y, position.z);
        if (isBehaviour(state)) return true;
        String block = ModernDecorationEngine.blockName(state);
        if (!isAirBlock(block)
            && !("minecraft:water".equals(block) && isWaterFluid(world.fluidState(position.x, position.y, position.z))))
            return false;
        for (Direction direction : DIRECTIONS) {
            Pos neighbor = direction.move(position);
            if (world.isSolid(neighbor.x, neighbor.y, neighbor.z)) return true;
        }
        return false;
    }

    private static List<Direction> shuffledDirections(ModernRandom random) {
        List<Direction> result = new ArrayList<Direction>();
        for (Direction direction : DIRECTIONS) result.add(direction);
        shuffle(result, random);
        return result;
    }

    private static List<Pos> shuffledNeighbours(ModernRandom random) {
        List<Pos> result = new ArrayList<Pos>(NON_CORNER_NEIGHBOURS);
        shuffle(result, random);
        return result;
    }

    private static <T> void shuffle(List<T> values, ModernRandom random) {
        for (int remaining = values.size(); remaining > 1; remaining--) {
            int selected = random.nextInt(remaining);
            T end = values.get(remaining - 1);
            values.set(remaining - 1, values.get(selected));
            values.set(selected, end);
        }
    }

    private static List<Pos> neighbours() {
        List<Pos> result = new ArrayList<Pos>(18);
        for (int z = -1; z <= 1; z++) {
            for (int y = -1; y <= 1; y++) {
                for (int x = -1; x <= 1; x++) {
                    if ((x == 0 || y == 0 || z == 0) && (x != 0 || y != 0 || z != 0)) result.add(new Pos(x, y, z));
                }
            }
        }
        return result;
    }

    private static Set<Direction> availableFaces(String state) {
        Set<Direction> result = EnumSet.noneOf(Direction.class);
        if (!VEIN.equals(ModernDecorationEngine.blockName(state))) return result;
        for (Direction direction : DIRECTIONS) {
            if (hasFace(state, direction)) result.add(direction);
        }
        return result;
    }

    private static boolean hasFace(String state, Direction direction) {
        return VEIN.equals(ModernDecorationEngine.blockName(state)) && propertyTrue(state, direction.name);
    }

    private static boolean propertyTrue(String state, String name) {
        return state.contains(name + "=true");
    }

    private static String veinState(boolean waterlogged) {
        return "minecraft:sculk_vein[down=false,east=false,north=false,south=false,up=false," + "waterlogged="
            + waterlogged
            + ",west=false]";
    }

    private static boolean isAir(World world, Pos position) {
        return isAirBlock(ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z)));
    }

    private static boolean isAirBlock(String block) {
        return "minecraft:air".equals(block) || "minecraft:cave_air".equals(block)
            || "minecraft:void_air".equals(block);
    }

    private static boolean isWater(World world, Pos position) {
        return "minecraft:water"
            .equals(ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z)))
            || isWaterFluid(world.fluidState(position.x, position.y, position.z));
    }

    private static boolean isEmptyFluid(String fluid) {
        return fluid == null || fluid.isEmpty() || "minecraft:empty".equals(fluid) || "empty".equals(fluid);
    }

    private static boolean isWaterFluid(String fluid) {
        return fluid != null && (fluid.startsWith("minecraft:water") || fluid.startsWith("water"));
    }

    private static int manhattan(Pos left, Pos right) {
        return Math.abs(left.x - right.x) + Math.abs(left.y - right.y) + Math.abs(left.z - right.z);
    }

    private static final class SpreadPos {

        final Pos position;
        final Direction face;

        SpreadPos(Pos position, Direction face) {
            this.position = position;
            this.face = face;
        }
    }

    private enum Direction {

        DOWN("down", 'y', 0, -1, 0),
        UP("up", 'y', 0, 1, 0),
        NORTH("north", 'z', 0, 0, -1),
        SOUTH("south", 'z', 0, 0, 1),
        WEST("west", 'x', -1, 0, 0),
        EAST("east", 'x', 1, 0, 0);

        final String name;
        final char axis;
        final int x;
        final int y;
        final int z;

        Direction(String name, char axis, int x, int y, int z) {
            this.name = name;
            this.axis = axis;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Direction opposite() {
            if (this == DOWN) return UP;
            if (this == UP) return DOWN;
            if (this == NORTH) return SOUTH;
            if (this == SOUTH) return NORTH;
            if (this == WEST) return EAST;
            return WEST;
        }

        Pos move(Pos position) {
            return position.offset(x, y, z);
        }
    }
}
