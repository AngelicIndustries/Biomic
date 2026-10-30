// Generated from Regions Unexplored (MIT), revision f5dfe4eea916cb7ccb4990faebc952459dad5f8b
// Source SHA256: cfe0d690696c67aad2d05dd0555ea1b4cec1e260f483fc63559dd0ef51ca24d6
// Regenerate with tools/ru_reference/import_brim_willows.py. Only API and incidental RNG adapted.
package industries.angelic.biomic.world.modern.ruport;

import static industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.incidental;

import java.util.Random;

import industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.BlockPos;
import industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.BlockState;
import industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.BlockTags;
import industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.Blocks;
import industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.Context;
import industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.Direction;
import industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.LeavesBlock;
import industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.LevelAccessor;
import industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.LevelSimulatedReader;
import industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.MagmaLogBlock;
import industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.RUBlockTags;
import industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.RUBlocks;
import industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.RotatedPillarBlock;
import industries.angelic.biomic.world.modern.ruport.BrimWillowCompat.WorldGenLevel;

public class BrimWillowFeature {

    public boolean place(Context context) {
        BlockPos pos = context.origin();
        WorldGenLevel level = context.level();
        int height = context.random()
            .nextInt(4) + 8;

        if (level.getBlockState(pos.north())
            .getBlock() == RUBlocks.BRIMWOOD_WOOD_SET.getLogMagma()) {
            return false;
        }
        if (level.getBlockState(
            pos.north()
                .north())
            .getBlock() == RUBlocks.BRIMWOOD_WOOD_SET.getLogMagma()) {
            return false;
        }
        if (level.getBlockState(pos.west())
            .getBlock() == RUBlocks.BRIMWOOD_WOOD_SET.getLogMagma()) {
            return false;
        }
        if (level.getBlockState(
            pos.west()
                .west())
            .getBlock() == RUBlocks.BRIMWOOD_WOOD_SET.getLogMagma()) {
            return false;
        }
        if (level.getBlockState(pos.east())
            .getBlock() == RUBlocks.BRIMWOOD_WOOD_SET.getLogMagma()) {
            return false;
        }
        if (level.getBlockState(
            pos.east()
                .east())
            .getBlock() == RUBlocks.BRIMWOOD_WOOD_SET.getLogMagma()) {
            return false;
        }
        if (level.getBlockState(pos.south())
            .getBlock() == RUBlocks.BRIMWOOD_WOOD_SET.getLogMagma()) {
            return false;
        }
        if (level.getBlockState(
            pos.south()
                .south())
            .getBlock() == RUBlocks.BRIMWOOD_WOOD_SET.getLogMagma()) {
            return false;
        }
        if (level.getBlockState(
            pos.south()
                .west())
            .getBlock() == RUBlocks.BRIMWOOD_WOOD_SET.getLogMagma()) {
            return false;
        }
        if (level.getBlockState(
            pos.south()
                .east())
            .getBlock() == RUBlocks.BRIMWOOD_WOOD_SET.getLogMagma()) {
            return false;
        }
        if (level.getBlockState(
            pos.north()
                .west())
            .getBlock() == RUBlocks.BRIMWOOD_WOOD_SET.getLogMagma()) {
            return false;
        }
        if (level.getBlockState(
            pos.north()
                .east())
            .getBlock() == RUBlocks.BRIMWOOD_WOOD_SET.getLogMagma()) {
            return false;
        }

        if (level.getBlockState(pos.below())
            .getBlock() == Blocks.SCULK
            || level.getBlockState(pos.below())
                .getBlock() == RUBlocks.BRIMSPROUT_NYLIUM
            || level.getBlockState(pos.below())
                .is(BlockTags.NYLIUM)
            || level.getBlockState(pos.below())
                .is(BlockTags.DIRT)) {} else {
            return false;
        }

        if (level.isEmptyBlock(pos)) {
            BlockPos.MutableBlockPos placePos = pos.mutable();

            for (int i = 0; i <= height; i++) {
                if (i == height - 3) {
                    placeBranches(level, placePos);
                }
                if (i == height - 5) {
                    placeSpine(level, placePos);
                }
                if (isReplaceable(level, placePos)) {
                    if (level.isOutsideBuildHeight(placePos)) {
                        break;
                    }
                    if (i == 2) {
                        level.setBlock(
                            placePos,
                            RUBlocks.BRIMWOOD_WOOD_SET.getLogMagma()
                                .defaultBlockState()
                                .setValue(MagmaLogBlock.TRANSITION_BLOCK, true),
                            2);
                    } else if (i < 2) {
                        level.setBlock(
                            placePos,
                            RUBlocks.BRIMWOOD_WOOD_SET.getLogMagma()
                                .defaultBlockState()
                                .setValue(MagmaLogBlock.TRANSITION_BLOCK, false),
                            2);
                    } else if (i > 2) {
                        level.setBlock(
                            placePos,
                            RUBlocks.BRIMWOOD_WOOD_SET.getLog()
                                .defaultBlockState()
                                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y),
                            2);
                    }
                }
                if (i == height) {
                    placeLeavesUpsideDown(level, placePos);
                }
                placePos.move(Direction.UP);
            }

            return true;
        } else {
            return false;
        }
    }

    private void placeBranches(LevelAccessor level, BlockPos pos) {
        // NORTH
        if (isReplaceable(level, pos.north())) {
            if (isReplaceable(level, pos.north())) {
                placeBrimwoodLog(level, pos.north(), Direction.Axis.Z);
            }
            if (isReplaceable(
                level,
                pos.north()
                    .above()
                    .north())) {
                placeBrimwoodLog(
                    level,
                    pos.north()
                        .above()
                        .north(),
                    Direction.Axis.Z);
            }
            if (isReplaceable(
                level,
                pos.north()
                    .above()
                    .north()
                    .north())) {
                placeBrimwoodLog(
                    level,
                    pos.north()
                        .above()
                        .north()
                        .north(),
                    Direction.Axis.Z);
            }
            if (isReplaceable(
                level,
                pos.north()
                    .above()
                    .north()
                    .north()
                    .north())) {
                placeBrimwoodLog(
                    level,
                    pos.north()
                        .above()
                        .north()
                        .north()
                        .north(),
                    Direction.Axis.Z);
                placeLeavesUpsideDown(
                    level,
                    pos.north()
                        .above()
                        .north()
                        .north()
                        .north());
            }
        }
        // SOUTH
        if (isReplaceable(level, pos.south())) {
            if (isReplaceable(level, pos.south())) {
                placeBrimwoodLog(level, pos.south(), Direction.Axis.Z);
            }
            if (isReplaceable(
                level,
                pos.south()
                    .above()
                    .south())) {
                placeBrimwoodLog(
                    level,
                    pos.south()
                        .above()
                        .south(),
                    Direction.Axis.Z);
            }
            if (isReplaceable(
                level,
                pos.south()
                    .above()
                    .south()
                    .south())) {
                placeBrimwoodLog(
                    level,
                    pos.south()
                        .above()
                        .south()
                        .south(),
                    Direction.Axis.Z);
            }
            if (isReplaceable(
                level,
                pos.south()
                    .above()
                    .south()
                    .south()
                    .south())) {
                placeBrimwoodLog(
                    level,
                    pos.south()
                        .above()
                        .south()
                        .south()
                        .south(),
                    Direction.Axis.Z);
                placeLeavesUpsideDown(
                    level,
                    pos.south()
                        .above()
                        .south()
                        .south()
                        .south());
            }
        }
        // EAST
        if (isReplaceable(level, pos.east())) {
            if (isReplaceable(level, pos.east())) {
                placeBrimwoodLog(level, pos.east(), Direction.Axis.X);
            }
            if (isReplaceable(
                level,
                pos.east()
                    .above()
                    .east())) {
                placeBrimwoodLog(
                    level,
                    pos.east()
                        .above()
                        .east(),
                    Direction.Axis.X);
            }
            if (isReplaceable(
                level,
                pos.east()
                    .above()
                    .east()
                    .east())) {
                placeBrimwoodLog(
                    level,
                    pos.east()
                        .above()
                        .east()
                        .east(),
                    Direction.Axis.X);
            }
            if (isReplaceable(
                level,
                pos.east()
                    .above()
                    .east()
                    .east()
                    .east())) {
                placeBrimwoodLog(
                    level,
                    pos.east()
                        .above()
                        .east()
                        .east()
                        .east(),
                    Direction.Axis.X);
                placeLeavesUpsideDown(
                    level,
                    pos.east()
                        .above()
                        .east()
                        .east()
                        .east());
            }
        }
        // WEST
        if (isReplaceable(level, pos.west())) {
            if (isReplaceable(level, pos.west())) {
                placeBrimwoodLog(level, pos.west(), Direction.Axis.X);
            }
            if (isReplaceable(
                level,
                pos.west()
                    .above()
                    .west())) {
                placeBrimwoodLog(
                    level,
                    pos.west()
                        .above()
                        .west(),
                    Direction.Axis.X);
            }
            if (isReplaceable(
                level,
                pos.west()
                    .above()
                    .west()
                    .west())) {
                placeBrimwoodLog(
                    level,
                    pos.west()
                        .above()
                        .west()
                        .west(),
                    Direction.Axis.X);
            }
            if (isReplaceable(
                level,
                pos.west()
                    .above()
                    .west()
                    .west()
                    .west())) {
                placeBrimwoodLog(
                    level,
                    pos.west()
                        .above()
                        .west()
                        .west()
                        .west(),
                    Direction.Axis.X);
                placeLeavesUpsideDown(
                    level,
                    pos.west()
                        .above()
                        .west()
                        .west()
                        .west());
            }
        }
    }

    private void placeSpine(LevelAccessor level, BlockPos pos) {
        // NORTH
        if (isReplaceable(level, pos.north())) {
            placeBrimwoodLog(level, pos.north(), Direction.Axis.Z);
        }
        // SOUTH
        if (isReplaceable(level, pos.south())) {
            placeBrimwoodLog(level, pos.south(), Direction.Axis.Z);
        }
        // EAST
        if (isReplaceable(level, pos.east())) {
            placeBrimwoodLog(level, pos.east(), Direction.Axis.X);
        }
        // WEST
        if (isReplaceable(level, pos.west())) {
            placeBrimwoodLog(level, pos.west(), Direction.Axis.X);
        }
    }

    private void placeBrimwoodLog(LevelAccessor level, BlockPos pos, Direction.Axis axis) {
        if (level.isOutsideBuildHeight(pos)) {
            return;
        }
        if (isReplaceable(level, pos)) {
            level.setBlock(
                pos,
                RUBlocks.BRIMWOOD_WOOD_SET.getLog()
                    .defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, axis),
                2);
        }
    }

    private void placeBrimwoodLeaves(LevelAccessor level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return;
        }
        if (isReplaceable(level, pos)) {
            level.setBlock(
                pos,
                RUBlocks.BRIMWOOD_NATURAL_SET.getLeaves()
                    .defaultBlockState()
                    .setValue(LeavesBlock.PERSISTENT, true),
                2);
        }
    }

    private void placeLeavesUpsideDown(LevelAccessor level, BlockPos pos) {
        if (isReplaceable(level, pos)) {
            placeBrimwoodLeaves(level, pos);
        }
        // TOP
        if (isReplaceable(level, pos.above())) {
            placeBrimwoodLeaves(level, pos.above());
        }
        if (isReplaceable(
            level,
            pos.above()
                .north())) {
            placeBrimwoodLeaves(
                level,
                pos.above()
                    .north());
        }
        if (isReplaceable(
            level,
            pos.above()
                .north()
                .east())) {
            placeBrimwoodLeaves(
                level,
                pos.above()
                    .north()
                    .east());
        }
        if (isReplaceable(
            level,
            pos.above()
                .north()
                .west())) {
            placeBrimwoodLeaves(
                level,
                pos.above()
                    .north()
                    .west());
        }
        if (isReplaceable(
            level,
            pos.above()
                .south())) {
            placeBrimwoodLeaves(
                level,
                pos.above()
                    .south());
        }
        if (isReplaceable(
            level,
            pos.above()
                .south()
                .east())) {
            placeBrimwoodLeaves(
                level,
                pos.above()
                    .south()
                    .east());
        }
        if (isReplaceable(
            level,
            pos.above()
                .south()
                .west())) {
            placeBrimwoodLeaves(
                level,
                pos.above()
                    .south()
                    .west());
        }
        if (isReplaceable(
            level,
            pos.above()
                .east())) {
            placeBrimwoodLeaves(
                level,
                pos.above()
                    .east());
        }
        if (isReplaceable(
            level,
            pos.above()
                .west())) {
            placeBrimwoodLeaves(
                level,
                pos.above()
                    .west());
        }
        // NORTH
        if (isReplaceable(level, pos.north())) {
            placeBrimwoodLeaves(level, pos.north());
        }
        if (isReplaceable(
            level,
            pos.north()
                .north()
                .above())) {
            placeBrimwoodLeaves(
                level,
                pos.north()
                    .north()
                    .above());
            addVine(
                level,
                pos.north()
                    .north()
                    .above());
        }
        // NORTH-WEST
        if (isReplaceable(
            level,
            pos.north()
                .west())) {
            placeBrimwoodLeaves(
                level,
                pos.north()
                    .west());
        }
        if (isReplaceable(
            level,
            pos.north()
                .north()
                .west()
                .above())) {
            placeBrimwoodLeaves(
                level,
                pos.north()
                    .north()
                    .west()
                    .above());
            addVine(
                level,
                pos.north()
                    .north()
                    .west()
                    .above());
        }
        if (isReplaceable(
            level,
            pos.north()
                .west()
                .west()
                .above())) {
            placeBrimwoodLeaves(
                level,
                pos.north()
                    .west()
                    .west()
                    .above());
            addVine(
                level,
                pos.north()
                    .west()
                    .west()
                    .above());
        }
        // WEST
        if (isReplaceable(level, pos.west())) {
            placeBrimwoodLeaves(level, pos.west());
        }
        if (isReplaceable(
            level,
            pos.west()
                .west()
                .above())) {
            placeBrimwoodLeaves(
                level,
                pos.west()
                    .west()
                    .above());
            addVine(
                level,
                pos.west()
                    .west()
                    .above());
        }
        // SOUTH-WEST
        if (isReplaceable(
            level,
            pos.south()
                .west())) {
            placeBrimwoodLeaves(
                level,
                pos.south()
                    .west());
        }
        if (isReplaceable(
            level,
            pos.south()
                .south()
                .west()
                .above())) {
            placeBrimwoodLeaves(
                level,
                pos.south()
                    .south()
                    .west()
                    .above());
            addVine(
                level,
                pos.south()
                    .south()
                    .west()
                    .above());
        }
        if (isReplaceable(
            level,
            pos.south()
                .west()
                .west()
                .above())) {
            placeBrimwoodLeaves(
                level,
                pos.south()
                    .west()
                    .west()
                    .above());
            addVine(
                level,
                pos.south()
                    .west()
                    .west()
                    .above());
        }
        // SOUTH
        if (isReplaceable(level, pos.south())) {
            placeBrimwoodLeaves(level, pos.south());
        }
        if (isReplaceable(
            level,
            pos.south()
                .south()
                .above())) {
            placeBrimwoodLeaves(
                level,
                pos.south()
                    .south()
                    .above());
            addVine(
                level,
                pos.south()
                    .south()
                    .above());
        }
        // SOUTH-EAST
        if (isReplaceable(
            level,
            pos.south()
                .east())) {
            placeBrimwoodLeaves(
                level,
                pos.south()
                    .east());
        }
        if (isReplaceable(
            level,
            pos.south()
                .south()
                .east()
                .above())) {
            placeBrimwoodLeaves(
                level,
                pos.south()
                    .south()
                    .east()
                    .above());
            addVine(
                level,
                pos.south()
                    .south()
                    .east()
                    .above());
        }
        if (isReplaceable(
            level,
            pos.south()
                .east()
                .east()
                .above())) {
            placeBrimwoodLeaves(
                level,
                pos.south()
                    .east()
                    .east()
                    .above());
            addVine(
                level,
                pos.south()
                    .east()
                    .east()
                    .above());
        }
        // EAST
        if (isReplaceable(level, pos.east())) {
            placeBrimwoodLeaves(level, pos.east());
        }
        if (isReplaceable(
            level,
            pos.east()
                .east()
                .above())) {
            placeBrimwoodLeaves(
                level,
                pos.east()
                    .east()
                    .above());
            addVine(
                level,
                pos.east()
                    .east()
                    .above());
        }
        // NORTH-EAST
        if (isReplaceable(
            level,
            pos.north()
                .east())) {
            placeBrimwoodLeaves(
                level,
                pos.north()
                    .east());
        }
        if (isReplaceable(
            level,
            pos.north()
                .north()
                .east()
                .above())) {
            placeBrimwoodLeaves(
                level,
                pos.north()
                    .north()
                    .east()
                    .above());
            addVine(
                level,
                pos.north()
                    .north()
                    .east()
                    .above());
        }
        if (isReplaceable(
            level,
            pos.north()
                .east()
                .east()
                .above())) {
            placeBrimwoodLeaves(
                level,
                pos.north()
                    .east()
                    .east()
                    .above());
            addVine(
                level,
                pos.north()
                    .east()
                    .east()
                    .above());
        }
    }

    private void addVine(LevelAccessor level, BlockPos pos) {
        Random random = incidental(level, pos);
        int size = random.nextInt(3);
        BlockPos place = pos.above();
        for (int i = 0; i <= size; i++) {
            if (isAir(level, place)) {
                if (i == size) {
                    if (level.isOutsideBuildHeight(place)) {
                        break;
                    }
                    level.setBlock(
                        place,
                        RUBlocks.BRIMWOOD_NATURAL_SET.getLeaves()
                            .defaultBlockState()
                            .setValue(LeavesBlock.PERSISTENT, true),
                        2);
                } else {
                    if (level.isOutsideBuildHeight(place)) {
                        break;
                    }
                    level.setBlock(
                        place,
                        RUBlocks.BRIMWOOD_NATURAL_SET.getLeaves()
                            .defaultBlockState()
                            .setValue(LeavesBlock.PERSISTENT, true),
                        2);
                }
                place = place.above();
            } else {
                if (i != 0) {
                    if (level.isOutsideBuildHeight(place)) {
                        break;
                    }
                    level.setBlock(
                        place.below(),
                        RUBlocks.BRIMWOOD_NATURAL_SET.getLeaves()
                            .defaultBlockState()
                            .setValue(LeavesBlock.PERSISTENT, true),
                        2);
                }
                break;
            }
        }
    }

    public static boolean isAirBlock(BlockState state) {
        return state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR);
    }

    public static boolean isAir(LevelSimulatedReader reader, BlockPos pos) {
        return reader.isStateAtPosition(pos, BrimWillowFeature::isAirBlock);
    }

    public static boolean isReplaceableBlock(BlockState state) {
        return state.is(RUBlockTags.REPLACEABLE_BLOCKS);
    }

    public static boolean isReplaceable(LevelSimulatedReader reader, BlockPos pos) {
        return reader.isStateAtPosition(pos, BrimWillowFeature::isReplaceableBlock);
    }
}
