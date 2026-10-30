package industries.angelic.biomic.world.modern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Exact coral-shape family from the pinned Minecraft 1.21.1 target. */
final class ModernVanillaCoralFeatures {

    private static final String[] CORAL_BLOCKS = { "minecraft:tube_coral_block", "minecraft:brain_coral_block",
        "minecraft:bubble_coral_block", "minecraft:fire_coral_block", "minecraft:horn_coral_block" };
    private static final String[] CORALS = { "minecraft:tube_coral", "minecraft:brain_coral", "minecraft:bubble_coral",
        "minecraft:fire_coral", "minecraft:horn_coral", "minecraft:tube_coral_fan", "minecraft:brain_coral_fan",
        "minecraft:bubble_coral_fan", "minecraft:fire_coral_fan", "minecraft:horn_coral_fan" };
    private static final String[] WALL_CORALS = { "minecraft:tube_coral_wall_fan", "minecraft:brain_coral_wall_fan",
        "minecraft:bubble_coral_wall_fan", "minecraft:fire_coral_wall_fan", "minecraft:horn_coral_wall_fan" };
    private static final int[][] HORIZONTAL = { { 0, 0, -1 }, { 1, 0, 0 }, { 0, 0, 1 }, { -1, 0, 0 } };
    private static final String[] HORIZONTAL_NAMES = { "north", "east", "south", "west" };

    boolean place(String type, World world, ModernRandom random, Pos origin) {
        String coral = CORAL_BLOCKS[random.nextInt(CORAL_BLOCKS.length)];
        if ("minecraft:coral_tree".equals(type)) {
            return tree(world, random, origin, coral);
        }
        if ("minecraft:coral_claw".equals(type)) {
            return claw(world, random, origin, coral);
        }
        if ("minecraft:coral_mushroom".equals(type)) {
            return mushroom(world, random, origin, coral);
        }
        throw new IllegalArgumentException("Unsupported pinned coral feature " + type);
    }

    private boolean tree(World world, ModernRandom random, Pos origin, String coral) {
        Pos cursor = origin;
        int trunkHeight = random.nextInt(3) + 1;
        for (int y = 0; y < trunkHeight; y++) {
            if (!placeCoralBlock(world, random, cursor, coral)) return true;
            cursor = cursor.offset(0, 1, 0);
        }
        Pos crown = cursor;
        int branchCount = random.nextInt(3) + 2;
        List<Integer> directions = horizontalDirections();
        shuffle(directions, random);
        for (int branchIndex = 0; branchIndex < branchCount; branchIndex++) {
            int direction = directions.get(branchIndex);
            int[] delta = HORIZONTAL[direction];
            cursor = crown.offset(delta[0], 0, delta[2]);
            int length = random.nextInt(5) + 2;
            int verticalRun = 0;
            for (int step = 0; step < length && placeCoralBlock(world, random, cursor, coral); step++) {
                cursor = cursor.offset(0, 1, 0);
                if (step == 0 || (++verticalRun >= 2 && random.nextFloat() < 0.25F)) {
                    cursor = cursor.offset(delta[0], 0, delta[2]);
                    verticalRun = 0;
                }
            }
        }
        return true;
    }

    private boolean claw(World world, ModernRandom random, Pos origin, String coral) {
        if (!placeCoralBlock(world, random, origin, coral)) return false;
        int main = random.nextInt(HORIZONTAL.length);
        int branchCount = random.nextInt(2) + 2;
        List<Integer> candidates = new ArrayList<Integer>(3);
        candidates.add(main);
        candidates.add(clockwise(main));
        candidates.add(counterClockwise(main));
        shuffle(candidates, random);
        for (int branch = 0; branch < branchCount; branch++) {
            int side = candidates.get(branch);
            int[] sideDelta = HORIZONTAL[side];
            Pos cursor = origin.offset(sideDelta[0], 0, sideDelta[2]);
            int initialLength = random.nextInt(2) + 1;
            int initialDirection;
            int armLength;
            if (side == main) {
                initialDirection = main;
                armLength = random.nextInt(3) + 2;
            } else {
                cursor = cursor.offset(0, 1, 0);
                initialDirection = random.nextInt(2) == 0 ? side : 4;
                armLength = random.nextInt(3) + 3;
            }
            int[] initialDelta = initialDirection == 4 ? new int[] { 0, 1, 0 } : HORIZONTAL[initialDirection];
            for (int step = 0; step < initialLength && placeCoralBlock(world, random, cursor, coral); step++) {
                cursor = cursor.offset(initialDelta[0], initialDelta[1], initialDelta[2]);
            }
            cursor = cursor.offset(-initialDelta[0], -initialDelta[1], -initialDelta[2])
                .offset(0, 1, 0);
            int[] mainDelta = HORIZONTAL[main];
            boolean failed = false;
            for (int step = 0; step < armLength; step++) {
                cursor = cursor.offset(mainDelta[0], 0, mainDelta[2]);
                if (!placeCoralBlock(world, random, cursor, coral)) {
                    failed = true;
                    break;
                }
                if (random.nextFloat() < 0.25F) cursor = cursor.offset(0, 1, 0);
            }
            if (failed) continue;
        }
        return true;
    }

    private boolean mushroom(World world, ModernRandom random, Pos origin, String coral) {
        int sizeY = random.nextInt(3) + 3;
        int sizeX = random.nextInt(3) + 3;
        int sizeZ = random.nextInt(3) + 3;
        int down = random.nextInt(3) + 1;
        for (int x = 0; x <= sizeX; x++) {
            for (int y = 0; y <= sizeY; y++) {
                for (int z = 0; z <= sizeZ; z++) {
                    boolean xEdge = x == 0 || x == sizeX;
                    boolean yEdge = y == 0 || y == sizeY;
                    boolean zEdge = z == 0 || z == sizeZ;
                    if ((!xEdge || !yEdge) && (!zEdge || !yEdge)
                        && (!xEdge || !zEdge)
                        && (xEdge || yEdge || zEdge)
                        && random.nextFloat() >= 0.1F) {
                        placeCoralBlock(world, random, origin.offset(x, y - down, z), coral);
                    }
                }
            }
        }
        return true;
    }

    private boolean placeCoralBlock(World world, ModernRandom random, Pos position, String coralBlock) {
        String current = block(world, position);
        if ((!"minecraft:water".equals(current) && !isCoral(current))
            || !"minecraft:water".equals(block(world, position.offset(0, 1, 0)))) return false;
        set(world, position, coralBlock);
        if (random.nextFloat() < 0.25F) {
            set(world, position.offset(0, 1, 0), waterloggedCoral(CORALS[random.nextInt(CORALS.length)]));
        } else if (random.nextFloat() < 0.05F) {
            set(
                world,
                position.offset(0, 1, 0),
                "minecraft:sea_pickle[pickles=" + (random.nextInt(4) + 1) + ",waterlogged=true]");
        }
        for (int direction = 0; direction < HORIZONTAL.length; direction++) {
            if (random.nextFloat() >= 0.2F) continue;
            int[] delta = HORIZONTAL[direction];
            Pos neighbor = position.offset(delta[0], 0, delta[2]);
            if (!"minecraft:water".equals(block(world, neighbor))) continue;
            String wall = WALL_CORALS[random.nextInt(WALL_CORALS.length)] + "[facing="
                + HORIZONTAL_NAMES[direction]
                + ",waterlogged=true]";
            set(world, neighbor, wall);
        }
        return true;
    }

    private static String waterloggedCoral(String block) {
        return block + "[waterlogged=true]";
    }

    private static boolean isCoral(String block) {
        for (String coral : CORALS) if (coral.equals(block)) return true;
        return false;
    }

    private static int clockwise(int direction) {
        return direction + 1 & 3;
    }

    private static int counterClockwise(int direction) {
        return direction + 3 & 3;
    }

    private static List<Integer> horizontalDirections() {
        List<Integer> result = new ArrayList<Integer>(4);
        Collections.addAll(result, 0, 1, 2, 3);
        return result;
    }

    private static <T> void shuffle(List<T> values, ModernRandom random) {
        for (int remaining = values.size(); remaining > 1; remaining--) {
            Collections.swap(values, remaining - 1, random.nextInt(remaining));
        }
    }

    private static String block(World world, Pos position) {
        return ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
    }

    private static void set(World world, Pos position, String state) {
        world.setBlock(position.x, position.y, position.z, state);
    }
}
