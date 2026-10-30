package industries.angelic.biomic.world.modern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Exact codec-level implementation of the modern vanilla tree feature. */
final class ModernTreeFeature {

    private final ModernFeatureEngine features;

    ModernTreeFeature(ModernFeatureEngine features) {
        this.features = features;
    }

    boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
        JsonObject trunk = config.getAsJsonObject("trunk_placer");
        JsonObject foliage = config.getAsJsonObject("foliage_placer");
        int treeHeight = trunk.has("height") ? engine.sampleIntValue(trunk.get("height"), random)
            : integer(trunk, "base_height", 0) + random.nextInt(integer(trunk, "height_rand_a", 0) + 1)
                + random.nextInt(integer(trunk, "height_rand_b", 0) + 1);
        int foliageHeight = foliageHeight(engine, foliage, random, treeHeight);
        int trunkBelowFoliage = treeHeight - foliageHeight;
        int foliageRadius = foliageRadius(engine, foliage, random, trunkBelowFoliage);

        Pos trunkOrigin = origin;
        JsonObject root = config.has("root_placer") ? config.getAsJsonObject("root_placer") : null;
        if (root != null && !"regions_unexplored:willow".equals(
            root.get("type")
                .getAsString())) {
            trunkOrigin = origin.offset(0, engine.sampleIntValue(root.get("trunk_offset_y"), random), 0);
        }

        if (Math.min(origin.y, trunkOrigin.y) < world.minBuildHeight() + 1
            || Math.max(origin.y, trunkOrigin.y) + treeHeight + 1 > world.maxBuildHeight()) return false;
        int freeHeight = maxFreeHeight(engine, config, world, trunkOrigin, treeHeight);
        Integer minimumClipped = config.getAsJsonObject("minimum_size")
            .has("min_clipped_height")
                ? config.getAsJsonObject("minimum_size")
                    .get("min_clipped_height")
                    .getAsInt()
                : null;
        if (freeHeight < treeHeight && (minimumClipped == null || freeHeight < minimumClipped)) {
            return false;
        }

        TreeBlocks blocks = new TreeBlocks();
        if (root != null && !placeRoots(engine, root, world, random, origin, trunkOrigin, blocks)) return false;
        List<Attachment> attachments = placeTrunk(
            engine,
            config,
            trunk,
            world,
            random,
            freeHeight,
            trunkOrigin,
            blocks);
        for (Attachment attachment : attachments) {
            createFoliage(engine, config, foliage, world, random, attachment, foliageHeight, foliageRadius, blocks);
        }
        if (blocks.logs.isEmpty() && blocks.leaves.isEmpty()) return false;
        // Decorators are applied after the complete trunk/canopy. Their exact implementations
        // are deliberately dispatched separately so no partial decorator can silently pass.
        placeDecorators(engine, config, world, random, blocks);
        ModernTreeShapeUpdates.finish(engine, world, blocks.logs, blocks.leaves, blocks.roots, blocks.decorators);
        return true;
    }

    private boolean placeRoots(ModernDecorationEngine engine, JsonObject root, World world, ModernRandom random,
        Pos origin, Pos trunkOrigin, TreeBlocks blocks) {
        String type = root.get("type")
            .getAsString();
        if ("regions_unexplored:magnolia".equals(type)) {
            int[] direction = horizontal(random);
            List<RootPosition> roots = new ArrayList<RootPosition>();
            Pos position = trunkOrigin.offset(direction[0], 0, direction[1]);
            roots.add(new RootPosition(position, direction[0] != 0 ? "x" : "z"));
            for (int length = 0; length < 6; length++) {
                if (!validTreePos(engine, world.blockState(position.x, position.y, position.z))) break;
                position = position.offset(0, -1, 0);
                roots.add(new RootPosition(position, null));
            }
            int[] clockwise = clockwise(direction);
            position = trunkOrigin.offset(clockwise[0] - direction[0], 0, clockwise[1] - direction[1]);
            roots.add(new RootPosition(position, null));
            for (int length = 0; length < 6; length++) {
                if (!validTreePos(engine, world.blockState(position.x, position.y, position.z))) break;
                position = position.offset(0, -1, 0);
                roots.add(new RootPosition(position, null));
            }
            for (RootPosition candidate : roots) {
                placeRootState(engine, root, world, random, candidate.position, candidate.axis, false, blocks);
            }
            return true;
        }
        if ("regions_unexplored:willow".equals(type)) {
            if (random.forkPositional()
                .at(trunkOrigin.x, trunkOrigin.y, trunkOrigin.z)
                .nextFloat()
                > root.get("chance")
                    .getAsFloat())
                return true;
            List<Pos> roots = new ArrayList<Pos>();
            for (int[] direction : horizontalDirections()) {
                Pos position = trunkOrigin
                    .offset(direction[0], engine.sampleIntValue(root.get("height"), random) - 1, direction[1]);
                roots.add(position);
                while (canPlaceWillowRoot(engine, world, position)) {
                    position = position.offset(0, -1, 0);
                    roots.add(position);
                }
                roots.add(position.offset(0, -1, 0));
            }
            for (Pos position : roots) {
                if (!canPlaceWillowRoot(engine, world, position)) continue;
                placeRootState(engine, root, world, random, position, null, true, blocks);
            }
            return true;
        }
        if ("minecraft:mangrove_root_placer".equals(type)) {
            return placeMangroveRoots(engine, root, world, random, origin, trunkOrigin, blocks);
        }
        throw new IllegalArgumentException("Unsupported pinned tree root placer " + type);
    }

    private boolean placeMangroveRoots(ModernDecorationEngine engine, JsonObject root, World world, ModernRandom random,
        Pos origin, Pos trunkOrigin, TreeBlocks blocks) {
        Pos cursor = origin;
        while (cursor.y < trunkOrigin.y) {
            if (!canPlaceMangroveRoot(engine, root, world, cursor)) return false;
            cursor = cursor.offset(0, 1, 0);
        }
        List<Pos> roots = new ArrayList<Pos>();
        roots.add(trunkOrigin.offset(0, -1, 0));
        for (int[] direction : horizontalDirections()) {
            Pos start = trunkOrigin.offset(direction[0], 0, direction[1]);
            List<Pos> branch = new ArrayList<Pos>();
            if (!simulateMangroveRoots(engine, root, world, random, start, direction, trunkOrigin, branch, 0))
                return false;
            roots.addAll(branch);
            // Vanilla intentionally schedules this position a second time after
            // recursively including it in the branch list.
            roots.add(start);
        }
        for (Pos position : roots) {
            placeMangroveRoot(engine, root, world, random, position, blocks);
        }
        return true;
    }

    private boolean simulateMangroveRoots(ModernDecorationEngine engine, JsonObject root, World world,
        ModernRandom random, Pos position, int[] direction, Pos trunkOrigin, List<Pos> roots, int depth) {
        JsonObject placement = root.getAsJsonObject("mangrove_root_placement");
        int maximumLength = placement.get("max_root_length")
            .getAsInt();
        if (depth == maximumLength || roots.size() > maximumLength) return false;
        for (Pos candidate : potentialMangroveRootPositions(placement, random, position, direction, trunkOrigin)) {
            if (!canPlaceMangroveRoot(engine, root, world, candidate)) continue;
            roots.add(candidate);
            if (!simulateMangroveRoots(
                engine,
                root,
                world,
                random,
                candidate,
                direction,
                trunkOrigin,
                roots,
                depth + 1)) return false;
        }
        return true;
    }

    private List<Pos> potentialMangroveRootPositions(JsonObject placement, ModernRandom random, Pos position,
        int[] direction, Pos trunkOrigin) {
        Pos below = position.offset(0, -1, 0);
        Pos outwardBelow = position.offset(direction[0], -1, direction[1]);
        int distance = Math.abs(position.x - trunkOrigin.x) + Math.abs(position.y - trunkOrigin.y)
            + Math.abs(position.z - trunkOrigin.z);
        int maximumWidth = placement.get("max_root_width")
            .getAsInt();
        float skew = placement.get("random_skew_chance")
            .getAsFloat();
        List<Pos> result = new ArrayList<Pos>(2);
        if (distance > maximumWidth - 3 && distance <= maximumWidth) {
            result.add(below);
            if (random.nextFloat() < skew) result.add(outwardBelow);
        } else if (distance > maximumWidth || random.nextFloat() < skew) {
            result.add(below);
        } else {
            result.add(random.nextBoolean() ? position.offset(direction[0], 0, direction[1]) : below);
        }
        return result;
    }

    private boolean canPlaceMangroveRoot(ModernDecorationEngine engine, JsonObject root, World world, Pos position) {
        String state = world.blockState(position.x, position.y, position.z);
        return validTreePos(engine, state) || engine.isBlockInTag(
            root.getAsJsonObject("mangrove_root_placement")
                .get("can_grow_through")
                .getAsString(),
            state);
    }

    private void placeMangroveRoot(ModernDecorationEngine engine, JsonObject root, World world, ModernRandom random,
        Pos position, TreeBlocks blocks) {
        JsonObject placement = root.getAsJsonObject("mangrove_root_placement");
        String current = world.blockState(position.x, position.y, position.z);
        JsonObject provider;
        if (containsString(placement.get("muddy_roots_in"), ModernDecorationEngine.blockName(current))) {
            provider = placement.getAsJsonObject("muddy_roots_provider");
        } else {
            if (!canPlaceMangroveRoot(engine, root, world, position)) return;
            provider = root.getAsJsonObject("root_provider");
        }
        String state = potentiallyWaterlogged(
            features.stateProvider(engine, provider, random, position),
            world,
            position);
        world.setBlock(position.x, position.y, position.z, state);
        blocks.roots.add(position);
        if (!root.has("above_root_placement") || provider != root.get("root_provider")) return;
        JsonObject above = root.getAsJsonObject("above_root_placement");
        Pos abovePosition = position.offset(0, 1, 0);
        if (random.nextFloat() < above.get("above_root_placement_chance")
            .getAsFloat() && isAir(world.blockState(abovePosition.x, abovePosition.y, abovePosition.z))) {
            String aboveState = potentiallyWaterlogged(
                features.stateProvider(engine, above.getAsJsonObject("above_root_provider"), random, abovePosition),
                world,
                abovePosition);
            world.setBlock(abovePosition.x, abovePosition.y, abovePosition.z, aboveState);
            blocks.roots.add(abovePosition);
        }
    }

    private static String potentiallyWaterlogged(String state, World world, Pos position) {
        return state.contains("waterlogged=")
            ? ModernFeatureEngine.property(
                state,
                "waterlogged",
                Boolean.toString(
                    "minecraft:water".equals(
                        ModernDecorationEngine.blockName(world.fluidState(position.x, position.y, position.z)))))
            : state;
    }

    private static boolean containsString(JsonElement values, String expected) {
        for (JsonElement value : values.getAsJsonArray()) {
            if (expected.equals(value.getAsString())) return true;
        }
        return false;
    }

    private boolean canPlaceWillowRoot(ModernDecorationEngine engine, World world, Pos position) {
        String state = world.blockState(position.x, position.y, position.z);
        return validTreePos(engine, state) || "minecraft:grass_block".equals(ModernDecorationEngine.blockName(state))
            || engine.isBlockInTag("#regions_unexplored:replaceable_blocks", state);
    }

    private void placeRootState(ModernDecorationEngine engine, JsonObject root, World world, ModernRandom random,
        Pos position, String axis, boolean additionallyReplaceable, TreeBlocks blocks) {
        String existing = world.blockState(position.x, position.y, position.z);
        if (!validTreePos(engine, existing) && !additionallyReplaceable) return;
        String state = features.stateProvider(engine, root.getAsJsonObject("root_provider"), random, position);
        if (axis != null && state.contains("axis=")) state = ModernFeatureEngine.property(state, "axis", axis);
        if (state.contains("waterlogged=")) {
            state = ModernFeatureEngine.property(
                state,
                "waterlogged",
                Boolean.toString("minecraft:water".equals(world.fluidState(position.x, position.y, position.z))));
        }
        world.setBlock(position.x, position.y, position.z, state);
        blocks.roots.add(position);
    }

    private int maxFreeHeight(ModernDecorationEngine engine, JsonObject config, World world, Pos origin,
        int treeHeight) {
        JsonObject size = config.getAsJsonObject("minimum_size");
        JsonObject trunk = config.getAsJsonObject("trunk_placer");
        String canGrowThrough = trunk.has("can_grow_through") ? trunk.get("can_grow_through")
            .getAsString() : null;
        boolean ignoreVines = !config.has("ignore_vines") || config.get("ignore_vines")
            .getAsBoolean();
        for (int y = 0; y <= treeHeight + 1; y++) {
            int radius = sizeAtHeight(size, treeHeight, y);
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    String state = world.blockState(origin.x + x, origin.y + y, origin.z + z);
                    boolean free = validTreePos(engine, state) || engine.isBlockInTag("#minecraft:logs", state)
                    // UpwardsBranchingTrunkPlacer overrides TrunkPlacer.validTreePos with
                    // this configured tag. Mangroves consequently grow through roots laid by
                    // an earlier tree; the generic TreeFeature predicate alone rejects them.
                        || canGrowThrough != null && engine.isBlockInTag(canGrowThrough, state);
                    world.recordPlacementCandidate(
                        origin.offset(x, y, z),
                        -6,
                        "tree_free=" + state + ";accepted=" + free);
                    if (!free || (!ignoreVines && "minecraft:vine".equals(ModernDecorationEngine.blockName(state)))) {
                        return y - 2;
                    }
                }
            }
        }
        return treeHeight;
    }

    private static int sizeAtHeight(JsonObject size, int treeHeight, int y) {
        String type = size.get("type")
            .getAsString();
        if ("minecraft:two_layers_feature_size".equals(type)) {
            return y < integer(size, "limit", 1) ? integer(size, "lower_size", 0) : integer(size, "upper_size", 1);
        }
        if ("minecraft:three_layers_feature_size".equals(type)) {
            if (y < integer(size, "limit", 1)) return integer(size, "lower_size", 0);
            if (y >= treeHeight - integer(size, "upper_limit", 1)) {
                return integer(size, "upper_size", 1);
            }
            return integer(size, "middle_size", 1);
        }
        throw new IllegalArgumentException("Unsupported pinned tree feature size " + type);
    }

    private List<Attachment> placeTrunk(ModernDecorationEngine engine, JsonObject config, JsonObject trunk, World world,
        ModernRandom random, int height, Pos origin, TreeBlocks blocks) {
        String type = trunk.get("type")
            .getAsString();
        if ("minecraft:straight_trunk_placer".equals(type)) {
            setDirt(engine, config, world, random, origin.offset(0, -1, 0), blocks);
            for (int y = 0; y < height; y++) {
                placeLog(engine, config, world, random, origin.offset(0, y, 0), blocks, null);
            }
            List<Attachment> result = new ArrayList<Attachment>(1);
            result.add(new Attachment(origin.offset(0, height, 0), 0, false));
            return result;
        }
        if ("minecraft:forking_trunk_placer".equals(type)) {
            return placeForkingTrunk(engine, config, world, random, height, origin, blocks);
        }
        if ("minecraft:giant_trunk_placer".equals(type) || "minecraft:mega_jungle_trunk_placer".equals(type)) {
            List<Attachment> result = placeGiantTrunk(engine, config, world, random, height, origin, blocks);
            if ("minecraft:mega_jungle_trunk_placer".equals(type)) {
                for (int branchY = height - 2 - random.nextInt(4); branchY
                    > height / 2; branchY -= 2 + random.nextInt(4)) {
                    float angle = random.nextFloat() * ((float) Math.PI * 2.0F);
                    int branchX = 0;
                    int branchZ = 0;
                    for (int step = 0; step < 5; step++) {
                        branchX = (int) (1.5F + (float) Math.cos(angle) * step);
                        branchZ = (int) (1.5F + (float) Math.sin(angle) * step);
                        placeLog(
                            engine,
                            config,
                            world,
                            random,
                            origin.offset(branchX, branchY - 3 + step / 2, branchZ),
                            blocks,
                            null);
                    }
                    result.add(new Attachment(origin.offset(branchX, branchY, branchZ), -2, false));
                }
            }
            return result;
        }
        if ("minecraft:dark_oak_trunk_placer".equals(type)) {
            return placeDarkOakTrunk(engine, config, world, random, height, origin, blocks);
        }
        if ("minecraft:fancy_trunk_placer".equals(type)) {
            return placeFancyTrunk(engine, config, world, random, height, origin, blocks);
        }
        if ("minecraft:bending_trunk_placer".equals(type)) {
            return placeBendingTrunk(engine, config, trunk, world, random, height, origin, blocks);
        }
        if ("minecraft:cherry_trunk_placer".equals(type)) {
            return placeCherryTrunk(engine, config, trunk, world, random, height, origin, blocks);
        }
        if ("minecraft:upwards_branching_trunk_placer".equals(type)) {
            return placeUpwardsBranchingTrunk(engine, config, trunk, world, random, height, origin, blocks);
        }
        if ("regions_unexplored:aspen".equals(type)) {
            return placeAspenTrunk(engine, config, trunk, world, random, height, origin, blocks);
        }
        if ("regions_unexplored:magnolia".equals(type)) {
            return placeMagnoliaTrunk(engine, config, trunk, world, random, height, origin, blocks);
        }
        if ("regions_unexplored:redwood".equals(type)) {
            return placeRedwoodTrunk(engine, config, trunk, world, random, height, origin, blocks);
        }
        throw new IllegalArgumentException("Unsupported pinned tree trunk placer " + type);
    }

    private List<Attachment> placeBendingTrunk(ModernDecorationEngine engine, JsonObject config, JsonObject trunk,
        World world, ModernRandom random, int height, Pos origin, TreeBlocks blocks) {
        int[] direction = horizontal(random);
        int last = height - 1;
        Pos position = origin;
        setDirt(engine, config, world, random, origin.offset(0, -1, 0), blocks);
        List<Attachment> result = new ArrayList<Attachment>();
        int minimumLeaves = integer(trunk, "min_height_for_leaves", 1);
        for (int step = 0; step <= last; step++) {
            if (step + 1 >= last + random.nextInt(2)) {
                position = position.offset(direction[0], 0, direction[1]);
            }
            placeLog(engine, config, world, random, position, blocks, null);
            if (step >= minimumLeaves) result.add(new Attachment(position, 0, false));
            position = position.offset(0, 1, 0);
        }
        int bendLength = engine.sampleIntValue(trunk.get("bend_length"), random);
        for (int step = 0; step <= bendLength; step++) {
            placeLog(engine, config, world, random, position, blocks, null);
            result.add(new Attachment(position, 0, false));
            position = position.offset(direction[0], 0, direction[1]);
        }
        return result;
    }

    private List<Attachment> placeCherryTrunk(ModernDecorationEngine engine, JsonObject config, JsonObject trunk,
        World world, ModernRandom random, int height, Pos origin, TreeBlocks blocks) {
        setDirt(engine, config, world, random, origin.offset(0, -1, 0), blocks);
        JsonObject startRange = trunk.getAsJsonObject("branch_start_offset_from_top");
        int minimum = startRange.get("min_inclusive")
            .getAsInt();
        int maximum = startRange.get("max_inclusive")
            .getAsInt();
        int firstStart = Math.max(0, height - 1 + between(random, minimum, maximum));
        int secondStart = Math.max(0, height - 1 + between(random, minimum, maximum - 1));
        if (secondStart >= firstStart) secondStart++;
        int branchCount = engine.sampleIntValue(trunk.get("branch_count"), random);
        boolean three = branchCount == 3;
        boolean multiple = branchCount >= 2;
        int trunkLength = three ? height : multiple ? Math.max(firstStart, secondStart) + 1 : firstStart + 1;
        for (int step = 0; step < trunkLength; step++) {
            placeLog(engine, config, world, random, origin.offset(0, step, 0), blocks, null);
        }
        List<Attachment> result = new ArrayList<Attachment>();
        if (three) result.add(new Attachment(origin.offset(0, trunkLength, 0), 0, false));
        int[] direction = horizontal(random);
        result.add(
            placeCherryBranch(
                engine,
                config,
                trunk,
                world,
                random,
                height,
                origin,
                direction,
                firstStart,
                firstStart < trunkLength - 1,
                blocks));
        if (multiple) {
            result.add(
                placeCherryBranch(
                    engine,
                    config,
                    trunk,
                    world,
                    random,
                    height,
                    origin,
                    new int[] { -direction[0], -direction[1] },
                    secondStart,
                    secondStart < trunkLength - 1,
                    blocks));
        }
        return result;
    }

    private Attachment placeCherryBranch(ModernDecorationEngine engine, JsonObject config, JsonObject trunk,
        World world, ModernRandom random, int height, Pos origin, int[] direction, int branchStart,
        boolean connectedToTrunkAbove, TreeBlocks blocks) {
        Pos cursor = origin.offset(0, branchStart, 0);
        int targetY = height - 1 + engine.sampleIntValue(trunk.get("branch_end_offset_from_top"), random);
        boolean bends = connectedToTrunkAbove || targetY < branchStart;
        int horizontalLength = engine.sampleIntValue(trunk.get("branch_horizontal_length"), random) + (bends ? 1 : 0);
        Pos target = origin.offset(direction[0] * horizontalLength, targetY, direction[1] * horizontalLength);
        for (int step = 0; step < (bends ? 2 : 1); step++) {
            cursor = cursor.offset(direction[0], 0, direction[1]);
            placeLog(engine, config, world, random, cursor, blocks, direction[0] != 0 ? "x" : "z");
        }
        int vertical = target.y > cursor.y ? 1 : -1;
        while (!cursor.equals(target)) {
            int distance = Math.abs(target.x - cursor.x) + Math.abs(target.y - cursor.y)
                + Math.abs(target.z - cursor.z);
            float verticalChance = (float) Math.abs(target.y - cursor.y) / (float) distance;
            boolean moveVertically = random.nextFloat() < verticalChance;
            cursor = moveVertically ? cursor.offset(0, vertical, 0) : cursor.offset(direction[0], 0, direction[1]);
            placeLog(
                engine,
                config,
                world,
                random,
                cursor,
                blocks,
                moveVertically ? null : direction[0] != 0 ? "x" : "z");
        }
        return new Attachment(target.offset(0, 1, 0), 0, false);
    }

    private List<Attachment> placeUpwardsBranchingTrunk(ModernDecorationEngine engine, JsonObject config,
        JsonObject trunk, World world, ModernRandom random, int height, Pos origin, TreeBlocks blocks) {
        List<Attachment> result = new ArrayList<Attachment>();
        Pos cursor = origin;
        for (int step = 0; step < height; step++) {
            int y = origin.y + step;
            cursor = new Pos(origin.x, y, origin.z);
            if (placeMangroveLog(engine, config, trunk, world, random, cursor, blocks) && step < height - 1
                && random.nextFloat() < trunk.get("place_branch_per_log_probability")
                    .getAsFloat()) {
                int[] direction = horizontal(random);
                int firstLength = engine.sampleIntValue(trunk.get("extra_branch_length"), random);
                int start = Math
                    .max(0, firstLength - engine.sampleIntValue(trunk.get("extra_branch_length"), random) - 1);
                int steps = engine.sampleIntValue(trunk.get("extra_branch_steps"), random);
                placeUpwardsBranch(
                    engine,
                    config,
                    trunk,
                    world,
                    random,
                    height,
                    result,
                    cursor,
                    y,
                    direction,
                    start,
                    steps,
                    blocks);
            }
            if (step == height - 1) {
                result.add(new Attachment(new Pos(origin.x, y + 1, origin.z), 0, false));
            }
        }
        return result;
    }

    private void placeUpwardsBranch(ModernDecorationEngine engine, JsonObject config, JsonObject trunk, World world,
        ModernRandom random, int height, List<Attachment> result, Pos mutableStart, int baseY, int[] direction,
        int start, int steps, TreeBlocks blocks) {
        int lastTopY = baseY + start;
        int x = mutableStart.x;
        int z = mutableStart.z;
        int offset = start;
        while (offset < height && steps > 0) {
            if (offset >= 1) {
                int y = baseY + offset;
                x += direction[0];
                z += direction[1];
                lastTopY = y;
                Pos position = new Pos(x, y, z);
                if (placeMangroveLog(engine, config, trunk, world, random, position, blocks)) {
                    lastTopY = y + 1;
                }
                result.add(new Attachment(position, 0, false));
            }
            offset++;
            steps--;
        }
        if (lastTopY - baseY > 1) {
            Pos top = new Pos(x, lastTopY, z);
            result.add(new Attachment(top, 0, false));
            result.add(new Attachment(top.offset(0, -2, 0), 0, false));
        }
    }

    private boolean placeMangroveLog(ModernDecorationEngine engine, JsonObject config, JsonObject trunk, World world,
        ModernRandom random, Pos position, TreeBlocks blocks) {
        String existing = world.blockState(position.x, position.y, position.z);
        if (!validTreePos(engine, existing) && !engine.isBlockInTag(
            trunk.get("can_grow_through")
                .getAsString(),
            existing)) return false;
        String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
        world.setBlock(position.x, position.y, position.z, state);
        blocks.logs.add(position);
        return true;
    }

    private List<Attachment> placeForkingTrunk(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, int height, Pos origin, TreeBlocks blocks) {
        setDirt(engine, config, world, random, origin.offset(0, -1, 0), blocks);
        List<Attachment> result = new ArrayList<Attachment>();
        int[] firstDirection = horizontal(random);
        int bendStart = height - random.nextInt(4) - 1;
        int bendSteps = 3 - random.nextInt(3);
        int x = origin.x;
        int z = origin.z;
        Integer lastTop = null;
        for (int y = 0; y < height; y++) {
            if (y >= bendStart && bendSteps > 0) {
                x += firstDirection[0];
                z += firstDirection[1];
                bendSteps--;
            }
            if (placeLog(engine, config, world, random, new Pos(x, origin.y + y, z), blocks, null)) {
                lastTop = origin.y + y + 1;
            }
        }
        if (lastTop != null) result.add(new Attachment(new Pos(x, lastTop, z), 1, false));

        x = origin.x;
        z = origin.z;
        int[] secondDirection = horizontal(random);
        if (secondDirection[0] != firstDirection[0] || secondDirection[1] != firstDirection[1]) {
            int branchStart = bendStart - random.nextInt(2) - 1;
            int branchSteps = 1 + random.nextInt(3);
            lastTop = null;
            for (int y = branchStart; y < height && branchSteps > 0; y++, branchSteps--) {
                if (y < 1) continue;
                x += secondDirection[0];
                z += secondDirection[1];
                if (placeLog(engine, config, world, random, new Pos(x, origin.y + y, z), blocks, null))
                    lastTop = origin.y + y + 1;
            }
            if (lastTop != null) result.add(new Attachment(new Pos(x, lastTop, z), 0, false));
        }
        return result;
    }

    private List<Attachment> placeGiantTrunk(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, int height, Pos origin, TreeBlocks blocks) {
        Pos below = origin.offset(0, -1, 0);
        setDirt(engine, config, world, random, below, blocks);
        setDirt(engine, config, world, random, below.offset(1, 0, 0), blocks);
        setDirt(engine, config, world, random, below.offset(0, 0, 1), blocks);
        setDirt(engine, config, world, random, below.offset(1, 0, 1), blocks);
        for (int y = 0; y < height; y++) {
            placeLogIfFree(engine, config, world, random, origin.offset(0, y, 0), blocks);
            if (y >= height - 1) continue;
            placeLogIfFree(engine, config, world, random, origin.offset(1, y, 0), blocks);
            placeLogIfFree(engine, config, world, random, origin.offset(1, y, 1), blocks);
            placeLogIfFree(engine, config, world, random, origin.offset(0, y, 1), blocks);
        }
        List<Attachment> result = new ArrayList<Attachment>(1);
        result.add(new Attachment(origin.offset(0, height, 0), 0, true));
        return result;
    }

    private List<Attachment> placeDarkOakTrunk(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, int height, Pos origin, TreeBlocks blocks) {
        Pos below = origin.offset(0, -1, 0);
        setDirt(engine, config, world, random, below, blocks);
        setDirt(engine, config, world, random, below.offset(1, 0, 0), blocks);
        setDirt(engine, config, world, random, below.offset(0, 0, 1), blocks);
        setDirt(engine, config, world, random, below.offset(1, 0, 1), blocks);
        int[] direction = horizontal(random);
        int bendStart = height - random.nextInt(4);
        int bendSteps = 2 - random.nextInt(3);
        int trunkX = origin.x;
        int trunkZ = origin.z;
        int canopyY = origin.y + height - 1;
        for (int y = 0; y < height; y++) {
            if (y >= bendStart && bendSteps > 0) {
                trunkX += direction[0];
                trunkZ += direction[1];
                bendSteps--;
            }
            Pos position = new Pos(trunkX, origin.y + y, trunkZ);
            String current = world.blockState(position.x, position.y, position.z);
            String block = ModernDecorationEngine.blockName(current);
            if (!"minecraft:air".equals(block) && !engine.isBlockInTag("#minecraft:leaves", current)) continue;
            placeLog(engine, config, world, random, position, blocks, null);
            placeLog(engine, config, world, random, position.offset(1, 0, 0), blocks, null);
            placeLog(engine, config, world, random, position.offset(0, 0, 1), blocks, null);
            placeLog(engine, config, world, random, position.offset(1, 0, 1), blocks, null);
        }
        List<Attachment> result = new ArrayList<Attachment>();
        result.add(new Attachment(new Pos(trunkX, canopyY, trunkZ), 0, true));
        for (int x = -1; x <= 2; x++) {
            for (int z = -1; z <= 2; z++) {
                if ((x >= 0 && x <= 1 && z >= 0 && z <= 1) || random.nextInt(3) > 0) continue;
                int branchHeight = random.nextInt(3) + 2;
                for (int down = 0; down < branchHeight; down++) {
                    placeLog(
                        engine,
                        config,
                        world,
                        random,
                        new Pos(origin.x + x, canopyY - down - 1, origin.z + z),
                        blocks,
                        null);
                }
                result.add(new Attachment(new Pos(trunkX + x, canopyY, trunkZ + z), 0, false));
            }
        }
        return result;
    }

    private List<Attachment> placeFancyTrunk(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, int height, Pos origin, TreeBlocks blocks) {
        int totalHeight = height + 2;
        int trunkHeight = (int) Math.floor(totalHeight * 0.618D);
        setDirt(engine, config, world, random, origin.offset(0, -1, 0), blocks);
        int clustersPerLayer = Math.min(1, (int) Math.floor(1.382D + Math.pow((double) totalHeight / 13.0D, 2.0D)));
        int trunkTopY = origin.y + trunkHeight;
        int layer = totalHeight - 5;
        List<FancyAttachment> clusters = new ArrayList<FancyAttachment>();
        clusters.add(new FancyAttachment(origin.offset(0, layer, 0), trunkTopY));
        for (; layer >= 0; layer--) {
            float shape = fancyTreeShape(totalHeight, layer);
            if (shape < 0.0F) continue;
            for (int cluster = 0; cluster < clustersPerLayer; cluster++) {
                double length = shape * (random.nextFloat() + 0.328D);
                double angle = random.nextFloat() * 2.0F * Math.PI;
                double offsetX = length * Math.sin(angle) + 0.5D;
                double offsetZ = length * Math.cos(angle) + 0.5D;
                Pos foliageBase = origin.offset((int) Math.floor(offsetX), layer - 1, (int) Math.floor(offsetZ));
                if (!makeLimb(engine, config, world, random, foliageBase, foliageBase.offset(0, 5, 0), false, blocks))
                    continue;
                int dx = origin.x - foliageBase.x;
                int dz = origin.z - foliageBase.z;
                double projected = foliageBase.y - Math.sqrt(dx * dx + dz * dz) * 0.381D;
                int branchY = projected > trunkTopY ? trunkTopY : (int) projected;
                Pos branchBase = new Pos(origin.x, branchY, origin.z);
                if (!makeLimb(engine, config, world, random, branchBase, foliageBase, false, blocks)) continue;
                clusters.add(new FancyAttachment(foliageBase, branchY));
            }
        }
        makeLimb(engine, config, world, random, origin, origin.offset(0, trunkHeight, 0), true, blocks);
        List<Attachment> result = new ArrayList<Attachment>();
        for (FancyAttachment cluster : clusters) {
            Pos branchBase = new Pos(origin.x, cluster.branchY, origin.z);
            if (!branchBase.equals(cluster.position) && trimFancyBranch(totalHeight, cluster.branchY - origin.y)) {
                makeLimb(engine, config, world, random, branchBase, cluster.position, true, blocks);
            }
            if (trimFancyBranch(totalHeight, cluster.branchY - origin.y)) {
                result.add(new Attachment(cluster.position, 0, false));
            }
        }
        return result;
    }

    private boolean makeLimb(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos start, Pos end, boolean place, TreeBlocks blocks) {
        if (!place && start.equals(end)) return true;
        int dx = end.x - start.x;
        int dy = end.y - start.y;
        int dz = end.z - start.z;
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        float stepX = (float) dx / steps;
        float stepY = (float) dy / steps;
        float stepZ = (float) dz / steps;
        for (int step = 0; step <= steps; step++) {
            Pos position = start.offset(
                (int) Math.floor(0.5F + step * stepX),
                (int) Math.floor(0.5F + step * stepY),
                (int) Math.floor(0.5F + step * stepZ));
            if (place) {
                // FancyTrunkPlacer resolves the rotated-pillar axis for every log against the
                // limb's start, rather than assigning one axis to the complete limb. A limb can
                // therefore begin on X and turn onto Z as its rasterized line advances.
                int absoluteX = Math.abs(position.x - start.x);
                int absoluteZ = Math.abs(position.z - start.z);
                String axis = "y";
                int maximum = Math.max(absoluteX, absoluteZ);
                if (maximum > 0) axis = absoluteX == maximum ? "x" : "z";
                placeLog(engine, config, world, random, position, blocks, axis);
            } else {
                String state = world.blockState(position.x, position.y, position.z);
                if (!validTreePos(engine, state) && !engine.isBlockInTag("#minecraft:logs", state)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean trimFancyBranch(int height, int branchBase) {
        return branchBase >= height * 0.2D;
    }

    private static float fancyTreeShape(int height, int layer) {
        if (layer < height * 0.3F) return -1.0F;
        float half = height / 2.0F;
        float displaced = half - layer;
        float radius = (float) Math.sqrt(half * half - displaced * displaced);
        if (displaced == 0.0F) radius = half;
        else if (Math.abs(displaced) >= half) return 0.0F;
        return radius * 0.5F;
    }

    private List<Attachment> placeAspenTrunk(ModernDecorationEngine engine, JsonObject config, JsonObject trunk,
        World world, ModernRandom random, int height, Pos origin, TreeBlocks blocks) {
        List<Attachment> result = new ArrayList<Attachment>();
        Pos position = origin;
        for (int y = 0; y < height; y++) {
            if (y == height - 2 && random.nextFloat() < trunk.get("bend_chance")
                .getAsFloat()) {
                int[] direction = horizontal(random);
                position = position.offset(direction[0], 0, direction[1]);
                if (random.nextBoolean()) {
                    Pos branchTop = position.offset(-direction[0] * 2, 0, -direction[1] * 2);
                    placeLog(
                        engine,
                        config,
                        world,
                        random,
                        branchTop.offset(0, -1, 0),
                        blocks,
                        direction[0] != 0 ? "x" : "z");
                    result.add(new Attachment(branchTop, 0, false));
                }
            }
            placeLog(engine, config, world, random, position, blocks, null);
            position = position.offset(0, 1, 0);
        }
        result.add(new Attachment(position, 0, false));
        return result;
    }

    private List<Attachment> placeMagnoliaTrunk(ModernDecorationEngine engine, JsonObject config, JsonObject trunk,
        World world, ModernRandom random, int height, Pos origin, TreeBlocks blocks) {
        List<Attachment> result = new ArrayList<Attachment>();
        for (int y = 0; y < height; y++) {
            placeLog(engine, config, world, random, origin.offset(0, y, 0), blocks, null);
        }
        int[] direction = horizontal(random);
        Pos position = origin.offset(0, height - 1, 0);
        for (int step = 0; step < engine.sampleIntValue(trunk.get("primary_branch_length"), random); step++) {
            position = step % 3 == 0 ? position.offset(direction[0], 0, direction[1]) : position.offset(0, 1, 0);
            placeLog(engine, config, world, random, position, blocks, null);
        }
        result.add(new Attachment(position, 0, false));

        position = origin.offset(0, height - 1, 0);
        for (int step = 0; step < engine.sampleIntValue(trunk.get("secondary_branch_length"), random); step++) {
            if (step % 2 == 0) position = position.offset(0, 1, 0);
            position = position.offset(-direction[0], 0, -direction[1]);
            placeLog(engine, config, world, random, position, blocks, direction[0] != 0 ? "x" : "z");
        }
        result.add(new Attachment(position, 0, false));
        return result;
    }

    private List<Attachment> placeRedwoodTrunk(ModernDecorationEngine engine, JsonObject config, JsonObject trunk,
        World world, ModernRandom random, int height, Pos origin, TreeBlocks blocks) {
        List<Attachment> result = new ArrayList<Attachment>();
        placeRuLogColumn(engine, config, world, random, origin, height, blocks);
        int[][] directions = horizontalDirections();
        for (int[] direction : directions) {
            int[] clockwise = clockwise(direction);
            placeRuLogColumn(
                engine,
                config,
                world,
                random,
                origin.offset(direction[0], 0, direction[1]),
                baseTrunkHeight(engine, trunk, random, 0),
                blocks);
            placeRuLogColumn(
                engine,
                config,
                world,
                random,
                origin.offset(direction[0] + clockwise[0], 0, direction[1] + clockwise[1]),
                baseTrunkHeight(engine, trunk, random, 1),
                blocks);
            placeRuLogColumn(
                engine,
                config,
                world,
                random,
                origin.offset(direction[0] * 2, 0, direction[1] * 2),
                baseTrunkHeight(engine, trunk, random, 2),
                blocks);
            placeRuLogColumn(
                engine,
                config,
                world,
                random,
                origin.offset(direction[0] * 2 + clockwise[0], 0, direction[1] * 2 + clockwise[1]),
                baseTrunkHeight(engine, trunk, random, 3),
                blocks);
            placeRuLogColumn(
                engine,
                config,
                world,
                random,
                origin.offset(direction[0] + clockwise[0] * 2, 0, direction[1] + clockwise[1] * 2),
                baseTrunkHeight(engine, trunk, random, 3),
                blocks);
        }
        Pos branchOrigin = origin.offset(0, height, 0);
        result.add(new Attachment(branchOrigin, 0, false));
        int length = 1;
        for (JsonElement count : trunk.getAsJsonArray("branch_counts")) {
            for (int branch = 0; branch < engine.sampleIntValue(count, random); branch++) {
                branchOrigin = branchOrigin.offset(
                    0,
                    -trunk.get("branch_offset")
                        .getAsInt(),
                    0);
                placeRedwoodBranches(engine, config, world, random, length, branchOrigin, blocks, result);
                if (length > 2) {
                    for (int[] direction : directions) {
                        int[] clockwise = clockwise(direction);
                        result.add(
                            new Attachment(
                                branchOrigin.offset(direction[0] + clockwise[0], 0, direction[1] + clockwise[1]),
                                0,
                                false));
                    }
                }
            }
            length++;
        }
        return result;
    }

    private void placeRedwoodBranches(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, int length, Pos origin, TreeBlocks blocks, List<Attachment> attachments) {
        boolean xAxis = random.nextInt(2) == 0;
        for (int[] direction : horizontalDirections()) {
            Pos position = origin;
            boolean directionX = direction[0] != 0;
            if (xAxis != directionX) position = position.offset(0, -1, 0);
            for (int step = 0; step < length; step++) {
                position = position.offset(direction[0], 0, direction[1]);
                placeRuLog(engine, config, world, random, position, blocks, directionX ? "x" : "z");
            }
            attachments.add(new Attachment(position, 0, false));
        }
    }

    private int baseTrunkHeight(ModernDecorationEngine engine, JsonObject trunk, ModernRandom random, int index) {
        return trunk.getAsJsonArray("base_trunk_heights")
            .size() <= index ? 0
                : engine.sampleIntValue(
                    trunk.getAsJsonArray("base_trunk_heights")
                        .get(index),
                    random);
    }

    private void placeRuLogColumn(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin, int height, TreeBlocks blocks) {
        if (height == 0) return;
        setRuDirt(engine, world, origin.offset(0, -1, 0), blocks);
        Pos position = origin;
        for (int y = 0; y < height; y++) {
            if (!placeRuLog(engine, config, world, random, position, blocks, null)) return;
            position = position.offset(0, 1, 0);
        }
    }

    private boolean placeRuLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, TreeBlocks blocks, String axis) {
        String existing = world.blockState(position.x, position.y, position.z);
        if (!validTreePos(engine, existing) && !engine.isBlockInTag("#minecraft:logs", existing)
            && !engine.isBlockInTag("#regions_unexplored:branches", existing)) return false;
        String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
        if (axis != null && state.contains("axis=")) state = ModernFeatureEngine.property(state, "axis", axis);
        world.setBlock(position.x, position.y, position.z, state);
        blocks.logs.add(position);
        return true;
    }

    private void setRuDirt(ModernDecorationEngine engine, World world, Pos position, TreeBlocks blocks) {
        String existing = world.blockState(position.x, position.y, position.z);
        String state = engine.isBlockInTag("#regions_unexplored:replaceable_by_peat_dirt", existing)
            ? "regions_unexplored:peat_dirt"
            : engine.isBlockInTag("#regions_unexplored:replaceable_by_silt_dirt", existing)
                ? "regions_unexplored:silt_dirt"
                : "minecraft:dirt";
        world.setBlock(position.x, position.y, position.z, state);
        // RU's RedwoodTrunkPlacer passes TreeFeature's trunk setter to placeDirtBelow;
        // the dirt therefore intentionally participates in Context.logs() and its shuffle.
        blocks.logs.add(position);
    }

    private int foliageHeight(ModernDecorationEngine engine, JsonObject foliage, ModernRandom random, int treeHeight) {
        String type = foliage.get("type")
            .getAsString();
        if ("minecraft:blob_foliage_placer".equals(type) || "minecraft:fancy_foliage_placer".equals(type)
            || "minecraft:bush_foliage_placer".equals(type)) {
            return foliage.get("height")
                .getAsInt();
        }
        if ("minecraft:pine_foliage_placer".equals(type)) {
            return engine.sampleIntValue(foliage.get("height"), random);
        }
        if ("minecraft:spruce_foliage_placer".equals(type)) {
            return Math.max(4, treeHeight - engine.sampleIntValue(foliage.get("trunk_height"), random));
        }
        if ("minecraft:random_spread_foliage_placer".equals(type)) {
            return engine.sampleIntValue(foliage.get("foliage_height"), random);
        }
        if ("minecraft:cherry_foliage_placer".equals(type)) {
            return engine.sampleIntValue(foliage.get("height"), random);
        }
        if ("minecraft:acacia_foliage_placer".equals(type)) return 0;
        if ("minecraft:dark_oak_foliage_placer".equals(type)) return 4;
        if ("minecraft:mega_pine_foliage_placer".equals(type)) {
            return engine.sampleIntValue(foliage.get("crown_height"), random);
        }
        if ("minecraft:jungle_foliage_placer".equals(type)) return foliage.get("height")
            .getAsInt();
        if ("regions_unexplored:redwood".equals(type) || "regions_unexplored:skinny_pine".equals(type)) return 1;
        if (type.startsWith("regions_unexplored:")) {
            if ("regions_unexplored:sakura".equals(type)) {
                return engine.sampleIntValue(foliage.get("height"), random);
            }
            return 0;
        }
        throw new IllegalArgumentException("Unsupported pinned tree foliage placer " + type);
    }

    private int foliageRadius(ModernDecorationEngine engine, JsonObject foliage, ModernRandom random,
        int trunkBelowFoliage) {
        int radius = foliage.has("radius") ? engine.sampleIntValue(foliage.get("radius"), random)
            : ("regions_unexplored:redwood".equals(
                foliage.get("type")
                    .getAsString())
                || "regions_unexplored:skinny_pine".equals(
                    foliage.get("type")
                        .getAsString()) ? 1 : 0);
        if ("minecraft:pine_foliage_placer".equals(
            foliage.get("type")
                .getAsString())) {
            radius += random.nextInt(Math.max(trunkBelowFoliage + 1, 1));
        }
        return radius;
    }

    private void createFoliage(ModernDecorationEngine engine, JsonObject config, JsonObject foliage, World world,
        ModernRandom random, Attachment attachment, int height, int radius, TreeBlocks blocks) {
        int offset = foliage.has("offset") ? engine.sampleIntValue(foliage.get("offset"), random) : 0;
        String type = foliage.get("type")
            .getAsString();
        if ("minecraft:blob_foliage_placer".equals(type)) {
            for (int y = offset; y >= offset - height; y--) {
                placeLeavesRow(
                    engine,
                    config,
                    world,
                    random,
                    attachment,
                    Math.max(radius + attachment.radiusOffset - 1 - y / 2, 0),
                    y,
                    type,
                    blocks);
            }
            return;
        }
        if ("minecraft:fancy_foliage_placer".equals(type)) {
            for (int y = offset; y >= offset - height; y--) {
                int rowRadius = radius + (y == offset || y == offset - height ? 0 : 1);
                placeLeavesRow(engine, config, world, random, attachment, rowRadius, y, type, blocks);
            }
            return;
        }
        if ("minecraft:bush_foliage_placer".equals(type)) {
            for (int y = offset; y >= offset - height; y--) {
                int rowRadius = radius + attachment.radiusOffset - 1 - y;
                placeLeavesRow(engine, config, world, random, attachment, rowRadius, y, type, blocks);
            }
            return;
        }
        if ("minecraft:pine_foliage_placer".equals(type)) {
            int rowRadius = 0;
            for (int y = offset; y >= offset - height; y--) {
                placeLeavesRow(engine, config, world, random, attachment, rowRadius, y, type, blocks);
                if (rowRadius >= 1 && y == offset - height + 1) rowRadius--;
                else if (rowRadius < radius + attachment.radiusOffset) rowRadius++;
            }
            return;
        }
        if ("minecraft:spruce_foliage_placer".equals(type)) {
            int rowRadius = random.nextInt(2);
            int maximum = 1;
            int reset = 0;
            for (int y = offset; y >= -height; y--) {
                placeLeavesRow(engine, config, world, random, attachment, rowRadius, y, type, blocks);
                if (rowRadius >= maximum) {
                    rowRadius = reset;
                    reset = 1;
                    maximum = Math.min(maximum + 1, radius + attachment.radiusOffset);
                } else rowRadius++;
            }
            return;
        }
        if ("minecraft:random_spread_foliage_placer".equals(type)) {
            int attempts = foliage.get("leaf_placement_attempts")
                .getAsInt();
            for (int attempt = 0; attempt < attempts; attempt++) {
                Pos position = attachment.position.offset(
                    random.nextInt(radius) - random.nextInt(radius),
                    random.nextInt(height) - random.nextInt(height),
                    random.nextInt(radius) - random.nextInt(radius));
                tryPlaceLeaf(engine, config, world, random, position, blocks);
            }
            return;
        }
        if ("minecraft:acacia_foliage_placer".equals(type)) {
            Attachment shifted = new Attachment(
                attachment.position.offset(0, offset, 0),
                attachment.radiusOffset,
                attachment.doubleTrunk);
            placeLeavesRow(
                engine,
                config,
                world,
                random,
                shifted,
                radius + attachment.radiusOffset,
                -1 - height,
                type,
                blocks);
            placeLeavesRow(engine, config, world, random, shifted, radius - 1, -height, type, blocks);
            placeLeavesRow(
                engine,
                config,
                world,
                random,
                shifted,
                radius + attachment.radiusOffset - 1,
                0,
                type,
                blocks);
            return;
        }
        if ("minecraft:dark_oak_foliage_placer".equals(type)) {
            Attachment shifted = new Attachment(
                attachment.position.offset(0, offset, 0),
                attachment.radiusOffset,
                attachment.doubleTrunk);
            if (attachment.doubleTrunk) {
                placeLeavesRow(engine, config, world, random, shifted, radius + 2, -1, type, blocks);
                placeLeavesRow(engine, config, world, random, shifted, radius + 3, 0, type, blocks);
                placeLeavesRow(engine, config, world, random, shifted, radius + 2, 1, type, blocks);
                if (random.nextBoolean()) {
                    placeLeavesRow(engine, config, world, random, shifted, radius, 2, type, blocks);
                }
            } else {
                placeLeavesRow(engine, config, world, random, shifted, radius + 2, -1, type, blocks);
                placeLeavesRow(engine, config, world, random, shifted, radius + 1, 0, type, blocks);
            }
            return;
        }
        if ("minecraft:mega_pine_foliage_placer".equals(type)) {
            int priorRadius = 0;
            for (int absoluteY = attachment.position.y - height + offset; absoluteY
                <= attachment.position.y + offset; absoluteY++) {
                int below = attachment.position.y - absoluteY;
                int rowRadius = radius + attachment.radiusOffset
                    + (int) Math.floor((float) below / (float) height * 3.5F);
                int actualRadius = below > 0 && rowRadius == priorRadius && (absoluteY & 1) == 0 ? rowRadius + 1
                    : rowRadius;
                Attachment row = new Attachment(
                    new Pos(attachment.position.x, absoluteY, attachment.position.z),
                    attachment.radiusOffset,
                    attachment.doubleTrunk);
                placeLeavesRow(engine, config, world, random, row, actualRadius, 0, type, blocks);
                priorRadius = rowRadius;
            }
            return;
        }
        if ("minecraft:jungle_foliage_placer".equals(type)) {
            int rows = attachment.doubleTrunk ? height : 1 + random.nextInt(2);
            for (int y = offset; y >= offset - rows; y--) {
                int rowRadius = radius + attachment.radiusOffset + 1 - y;
                placeLeavesRow(engine, config, world, random, attachment, rowRadius, y, type, blocks);
            }
            return;
        }
        if ("minecraft:cherry_foliage_placer".equals(type)) {
            Attachment shifted = new Attachment(
                attachment.position.offset(0, offset, 0),
                attachment.radiusOffset,
                attachment.doubleTrunk);
            int rowRadius = radius + attachment.radiusOffset - 1;
            placeCherryLeavesRow(engine, config, foliage, world, random, shifted, rowRadius - 2, height - 3, blocks);
            placeCherryLeavesRow(engine, config, foliage, world, random, shifted, rowRadius - 1, height - 4, blocks);
            for (int y = height - 5; y >= 0; y--) {
                placeCherryLeavesRow(engine, config, foliage, world, random, shifted, rowRadius, y, blocks);
            }
            placeLeavesRowWithHangingLeaves(engine, config, foliage, world, random, shifted, rowRadius, -1, blocks);
            placeLeavesRowWithHangingLeaves(engine, config, foliage, world, random, shifted, rowRadius - 1, -2, blocks);
            return;
        }
        if (type.startsWith("regions_unexplored:")) {
            createRuFoliage(engine, config, foliage, world, random, attachment, offset, type, blocks);
            return;
        }
        throw new IllegalArgumentException("Unsupported pinned tree foliage placer " + type);
    }

    private void placeLeavesRowWithHangingLeaves(ModernDecorationEngine engine, JsonObject config, JsonObject foliage,
        World world, ModernRandom random, Attachment attachment, int radius, int y, TreeBlocks blocks) {
        placeCherryLeavesRow(engine, config, foliage, world, random, attachment, radius, y, blocks);
        int doubleOffset = attachment.doubleTrunk ? 1 : 0;
        Pos centerBelow = attachment.position.offset(0, -1, 0);
        for (int[] direction : horizontalDirections()) {
            int[] right = clockwise(direction);
            boolean positive = right[0] > 0 || right[1] > 0;
            int sideDistance = positive ? radius + doubleOffset : radius;
            Pos cursor = attachment.position.offset(0, y - 1, 0)
                .offset(right[0] * sideDistance, 0, right[1] * sideDistance)
                .offset(direction[0] * -radius, 0, direction[1] * -radius);
            for (int along = -radius; along < radius + doubleOffset; along++) {
                Pos above = cursor.offset(0, 1, 0);
                if (blocks.leaves.contains(above) && tryPlaceCherryExtension(
                    engine,
                    config,
                    world,
                    random,
                    centerBelow,
                    cursor,
                    foliage.get("hanging_leaves_chance")
                        .getAsFloat(),
                    blocks)) {
                    Pos below = cursor.offset(0, -1, 0);
                    tryPlaceCherryExtension(
                        engine,
                        config,
                        world,
                        random,
                        centerBelow,
                        below,
                        foliage.get("hanging_leaves_extension_chance")
                            .getAsFloat(),
                        blocks);
                }
                cursor = cursor.offset(direction[0], 0, direction[1]);
            }
        }
    }

    private boolean tryPlaceCherryExtension(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, Pos centerBelow, Pos position, float chance, TreeBlocks blocks) {
        int distance = Math.abs(position.x - centerBelow.x) + Math.abs(position.y - centerBelow.y)
            + Math.abs(position.z - centerBelow.z);
        return distance < 7 && random.nextFloat() <= chance
            && tryPlaceLeaf(engine, config, world, random, position, blocks);
    }

    private void placeCherryLeavesRow(ModernDecorationEngine engine, JsonObject config, JsonObject foliage, World world,
        ModernRandom random, Attachment attachment, int radius, int y, TreeBlocks blocks) {
        int doubleOffset = attachment.doubleTrunk ? 1 : 0;
        for (int rawX = -radius; rawX <= radius + doubleOffset; rawX++) {
            for (int rawZ = -radius; rawZ <= radius + doubleOffset; rawZ++) {
                int x = attachment.doubleTrunk ? Math.min(Math.abs(rawX), Math.abs(rawX - 1)) : Math.abs(rawX);
                int z = attachment.doubleTrunk ? Math.min(Math.abs(rawZ), Math.abs(rawZ - 1)) : Math.abs(rawZ);
                if (skipCherryFoliage(random, foliage, x, y, z, radius)) continue;
                tryPlaceLeaf(engine, config, world, random, attachment.position.offset(rawX, y, rawZ), blocks);
            }
        }
    }

    private static boolean skipCherryFoliage(ModernRandom random, JsonObject foliage, int x, int y, int z, int radius) {
        if (y == -1 && (x == radius || z == radius)
            && random.nextFloat() < foliage.get("wide_bottom_layer_hole_chance")
                .getAsFloat()) {
            return true;
        }
        boolean corner = x == radius && z == radius;
        if (radius > 2) {
            return corner || x + z > radius * 2 - 2 && random.nextFloat() < foliage.get("corner_hole_chance")
                .getAsFloat();
        }
        return corner && random.nextFloat() < foliage.get("corner_hole_chance")
            .getAsFloat();
    }

    private void createRuFoliage(ModernDecorationEngine engine, JsonObject config, JsonObject foliage, World world,
        ModernRandom random, Attachment attachment, int offset, String type, TreeBlocks blocks) {
        Pos origin = attachment.position;
        if ("regions_unexplored:aspen".equals(type)) {
            ruSquare(engine, config, world, random, origin, offset, 0, 2, 1.0F, blocks);
            for (int[] direction : horizontalDirections()) {
                ruSingle(
                    engine,
                    config,
                    world,
                    random,
                    origin.offset(direction[0], 2, direction[1]),
                    offset,
                    0.5F,
                    null,
                    blocks);
            }
            ruDiamond(engine, config, world, random, origin, offset, 1, 1, 1, blocks);
            ruSquare(engine, config, world, random, origin, offset, 1, 0, 1.0F, blocks);
            ruDiamond(engine, config, world, random, origin, offset, 2, 2, -1, blocks);
            return;
        }
        if ("regions_unexplored:magnolia".equals(type)) {
            ruSquare(engine, config, world, random, origin, offset, 3, 1, 0.0F, blocks);
            ruSquare(engine, config, world, random, origin, offset, 1, 2, 1.0F, blocks);
            ruSquare(engine, config, world, random, origin, offset, 1, 0, 1.0F, blocks);
            boolean clockwise = random.nextBoolean();
            for (int[] direction : horizontalDirections()) {
                int[] side = clockwise ? clockwise(direction) : counterClockwise(direction);
                Pos arm = origin.offset(direction[0] * 2, 1, direction[1] * 2);
                ruSingle(engine, config, world, random, arm.offset(0, -1, 0), offset, 1.0F, null, blocks);
                ruSingle(engine, config, world, random, arm.offset(0, 1, 0), offset, 1.0F, null, blocks);
                ruSingle(engine, config, world, random, arm.offset(side[0], -1, side[1]), offset, 1.0F, null, blocks);
                ruSingle(engine, config, world, random, arm.offset(side[0], 1, side[1]), offset, 1.0F, null, blocks);
            }
            return;
        }
        if ("regions_unexplored:redwood".equals(type)) {
            ruDiamond(engine, config, world, random, origin, offset, 1, 1, -1, blocks);
            ruDiamond(engine, config, world, random, origin, offset, 2, 2, 0, blocks);
            ruSquare(engine, config, world, random, origin, offset, 1, 1, 0.33F, blocks);
            ruDiamond(engine, config, world, random, origin, offset, 1, 1, 2, blocks);
            ruSquare(engine, config, world, random, origin, offset, 0, 3, 1.0F, blocks);
            ruSquare(engine, config, world, random, origin, offset, 0, 4, 1.0F, blocks);
            return;
        }
        if ("regions_unexplored:bioshroom".equals(type)) {
            JsonObject secondary = foliage.getAsJsonObject("secondary_provider");
            if (random.nextBoolean()) {
                ruSquare(engine, config, world, random, origin, offset, 1, 0, 1.0F, blocks);
            } else {
                ruSingle(engine, config, world, random, origin, offset, 1.0F, null, blocks);
                int[][] ring = { { 0, -1 }, { 1, -1 }, { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 } };
                for (int index = 0; index < ring.length; index++) {
                    ruSingle(
                        engine,
                        config,
                        world,
                        random,
                        origin.offset(ring[index][0], 0, ring[index][1]),
                        offset,
                        1.0F,
                        index % 2 == 0 ? secondary : null,
                        blocks);
                }
                ruDiamond(engine, config, world, random, origin, offset, 1, 1, 1, blocks);
            }
            return;
        }
        if ("regions_unexplored:fancy_pine".equals(type)) {
            ruSquare(engine, config, world, random, origin, offset, 0, 2, 1.0F, blocks);
            ruSquare(engine, config, world, random, origin, offset, 0, 1, 1.0F, blocks);
            ruDiamond(engine, config, world, random, origin, offset, 1, 1, 0, blocks);
            if (random.nextBoolean()) {
                ruDiamond(engine, config, world, random, origin, offset, 1, 1, -1, blocks);
                ruSquare(engine, config, world, random, origin, offset, 1, -2, 1.0F, blocks);
                ruDiamond(engine, config, world, random, origin, offset, 2, 2, -3, blocks);
                ruDiamond(engine, config, world, random, origin, offset, 1, 1, -4, blocks);
                ruDiamond(engine, config, world, random, origin, offset, 2, 2, -5, blocks);
                ruSquare(engine, config, world, random, origin, offset, 2, -6, 0.0F, blocks);
                if (random.nextFloat() < 0.3F) {
                    ruDiamond(engine, config, world, random, origin, offset, 3, 4, -7, blocks);
                } else ruSquare(engine, config, world, random, origin, offset, 1, -7, 1.0F, blocks);
            } else {
                ruSquare(engine, config, world, random, origin, offset, 0, -1, 1.0F, blocks);
                ruDiamond(engine, config, world, random, origin, offset, 1, 1, -2, blocks);
                ruSquare(engine, config, world, random, origin, offset, 1, -3, 1.0F, blocks);
                ruDiamond(engine, config, world, random, origin, offset, 2, 2, -4, blocks);
                ruDiamond(engine, config, world, random, origin, offset, 1, 1, -5, blocks);
                ruSquare(engine, config, world, random, origin, offset, 2, -6, 0.0F, blocks);
                if (random.nextFloat() < 0.3F) {
                    ruDiamond(engine, config, world, random, origin, offset, 3, 4, -7, blocks);
                } else ruSquare(engine, config, world, random, origin, offset, 2, -7, 1.0F, blocks);
            }
            ruDiamond(engine, config, world, random, origin, offset, 1, 1, -8, blocks);
            return;
        }
        if ("regions_unexplored:maple".equals(type)) {
            ruSquare(engine, config, world, random, origin, offset, 0, 1, 1.0F, blocks);
            ruSquare(engine, config, world, random, origin, offset, 1, 0, 0.4F, blocks);
            ruSquare(engine, config, world, random, origin, offset, 1, -1, 0.4F, blocks);
            ruSquare(engine, config, world, random, origin, offset, 1, -2, 1.0F, blocks);
            for (int[] direction : horizontalDirections()) {
                ruSingle(
                    engine,
                    config,
                    world,
                    random,
                    origin.offset(direction[0] * 2, -2, direction[1] * 2),
                    offset,
                    0.5F,
                    null,
                    blocks);
            }
            ruSquare(engine, config, world, random, origin, offset, 2, -3, 0.5F, blocks);
            ruSquare(engine, config, world, random, origin, offset, 2, -4, 0.5F, blocks);
            ruDiamond(engine, config, world, random, origin, offset, 1, 1, -5, blocks);
            return;
        }
        if ("regions_unexplored:skinny_pine".equals(type)) {
            for (int y = 2 + offset; y >= -(5 + random.nextInt(1)); y--) {
                int radius = y >= offset ? 0 : 1;
                placeLeavesRow(engine, config, world, random, attachment, radius, y, type, blocks);
            }
            return;
        }
        if ("regions_unexplored:willow".equals(type)) {
            ruDiamond(engine, config, world, random, origin, offset, 1, 1, 2, blocks);
            ruDiamond(engine, config, world, random, origin, offset, 2, 2, 1, blocks);
            for (int[] direction : horizontalDirections()) {
                int[] right = clockwise(direction);
                int[] left = counterClockwise(direction);
                Pos arm = origin.offset(direction[0] * 2, 1, direction[1] * 2);
                ruSingle(engine, config, world, random, arm.offset(right[0], 0, right[1]), offset, 0.3F, null, blocks);
                ruSingle(engine, config, world, random, arm.offset(left[0], 0, left[1]), offset, 0.3F, null, blocks);
            }
            ruSquare(engine, config, world, random, origin, offset, 2, 0, 0.0F, blocks);
            float chance = foliage.get("hanging_leaves_chance")
                .getAsFloat();
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                boolean xEdge = Math.abs(x) == 2;
                boolean zEdge = Math.abs(z) == 2;
                if (xEdge == zEdge || chance < random.nextFloat()) continue;
                int length = random.nextInt(2) + 1;
                for (int y = 0; y < length; y++) {
                    ruSingle(engine, config, world, random, origin.offset(x, -1 - y, z), offset, 1.0F, null, blocks);
                }
            }
            return;
        }
        throw new IllegalArgumentException("Unsupported pinned RU foliage placer " + type);
    }

    private void ruSquare(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin, int offset, int radius, int y, float cornerChance, TreeBlocks blocks) {
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (Math.abs(x) == radius && Math.abs(z) == radius && cornerChance < random.nextFloat()) continue;
            ruSingle(engine, config, world, random, origin.offset(x, y, z), offset, 1.0F, null, blocks);
        }
    }

    private void ruDiamond(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin, int offset, int radius, int maximumDistance, int y, TreeBlocks blocks) {
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (Math.abs(x) + Math.abs(z) > maximumDistance) continue;
            ruSingle(engine, config, world, random, origin.offset(x, y, z), offset, 1.0F, null, blocks);
        }
    }

    private void ruSingle(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos unshifted, int offset, float chance, JsonObject provider, TreeBlocks blocks) {
        if (chance < 1.0F && chance >= random.nextFloat()) return;
        Pos position = unshifted.offset(0, offset, 0);
        String existing = world.blockState(position.x, position.y, position.z);
        if (existing.contains("persistent=true") || !validTreePos(engine, existing)) return;
        String state = features.stateProvider(
            engine,
            provider == null ? config.getAsJsonObject("foliage_provider") : provider,
            random,
            position);
        if (state.contains("waterlogged=")) {
            state = ModernFeatureEngine.property(
                state,
                "waterlogged",
                Boolean.toString("minecraft:water".equals(world.fluidState(position.x, position.y, position.z))));
        }
        world.setBlock(position.x, position.y, position.z, state);
        blocks.leaves.add(position);
    }

    private void placeLeavesRow(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Attachment attachment, int radius, int y, String type, TreeBlocks blocks) {
        int doubleOffset = attachment.doubleTrunk ? 1 : 0;
        for (int x = -radius; x <= radius + doubleOffset; x++) {
            for (int z = -radius; z <= radius + doubleOffset; z++) {
                if (skipFoliage(random, x, y, z, radius, attachment.doubleTrunk, type)) continue;
                tryPlaceLeaf(engine, config, world, random, attachment.position.offset(x, y, z), blocks);
            }
        }
    }

    private static boolean skipFoliage(ModernRandom random, int rawX, int y, int rawZ, int radius, boolean doubleTrunk,
        String type) {
        if ("minecraft:dark_oak_foliage_placer".equals(type) && y == 0
            && doubleTrunk
            && (rawX == -radius || rawX >= radius)
            && (rawZ == -radius || rawZ >= radius)) {
            return true;
        }
        int x = doubleTrunk ? Math.min(Math.abs(rawX), Math.abs(rawX - 1)) : Math.abs(rawX);
        int z = doubleTrunk ? Math.min(Math.abs(rawZ), Math.abs(rawZ - 1)) : Math.abs(rawZ);
        if ("minecraft:blob_foliage_placer".equals(type)) {
            return x == radius && z == radius && (random.nextInt(2) == 0 || y == 0);
        }
        if ("minecraft:fancy_foliage_placer".equals(type)) {
            float fx = x + 0.5F;
            float fz = z + 0.5F;
            return fx * fx + fz * fz > radius * radius;
        }
        if ("minecraft:bush_foliage_placer".equals(type)) {
            return x == radius && z == radius && random.nextInt(2) == 0;
        }
        if ("minecraft:pine_foliage_placer".equals(type) || "minecraft:spruce_foliage_placer".equals(type)) {
            return x == radius && z == radius && radius > 0;
        }
        if ("minecraft:acacia_foliage_placer".equals(type)) {
            if (y == 0) return (x > 1 || z > 1) && x != 0 && z != 0;
            return x == radius && z == radius && radius > 0;
        }
        if ("minecraft:dark_oak_foliage_placer".equals(type)) {
            if (y == -1 && !doubleTrunk) return x == radius && z == radius;
            if (y == 1) return x + z > radius * 2 - 2;
            return false;
        }
        if ("minecraft:mega_pine_foliage_placer".equals(type) || "minecraft:jungle_foliage_placer".equals(type)) {
            return x + z >= 7 || x * x + z * z > radius * radius;
        }
        if ("minecraft:cherry_foliage_placer".equals(type)) {
            // The two chances live in the foliage object, so this branch is handled
            // by the instance overload below.
            throw new AssertionError("cherry skip requires foliage configuration");
        }
        if ("regions_unexplored:skinny_pine".equals(type)) {
            return x == radius && z == radius && radius != 0 && random.nextFloat() < 0.8F;
        }
        return false;
    }

    private boolean tryPlaceLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, TreeBlocks blocks) {
        world.recordPlacementCandidate(position, -3, "foliage_try");
        String existing = world.blockState(position.x, position.y, position.z);
        if (!validTreePos(engine, existing)) return false;
        String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
        if (state.contains("waterlogged=")) {
            state = ModernFeatureEngine.property(
                state,
                "waterlogged",
                Boolean.toString("minecraft:water".equals(world.fluidState(position.x, position.y, position.z))));
        }
        world.setBlock(position.x, position.y, position.z, state);
        blocks.leaves.add(position);
        return true;
    }

    private boolean placeLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, TreeBlocks blocks, String axis) {
        if (!validTreePos(engine, world.blockState(position.x, position.y, position.z))) return false;
        String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
        if (axis != null && state.contains("axis=")) state = ModernFeatureEngine.property(state, "axis", axis);
        world.setBlock(position.x, position.y, position.z, state);
        blocks.logs.add(position);
        return true;
    }

    private void placeLogIfFree(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, TreeBlocks blocks) {
        String state = world.blockState(position.x, position.y, position.z);
        if (validTreePos(engine, state) || engine.isBlockInTag("#minecraft:logs", state)) {
            placeLog(engine, config, world, random, position, blocks, null);
        }
    }

    private void setDirt(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, TreeBlocks blocks) {
        String current = world.blockState(position.x, position.y, position.z);
        String currentBlock = ModernDecorationEngine.blockName(current);
        String ruDirt = null;
        if ("regions_unexplored:peat_grass_block".equals(currentBlock)
            || "regions_unexplored:peat_dirt".equals(currentBlock)) {
            ruDirt = "regions_unexplored:peat_dirt";
        } else if ("regions_unexplored:silt_grass_block".equals(currentBlock)
            || "regions_unexplored:silt_dirt".equals(currentBlock)) {
                ruDirt = "regions_unexplored:silt_dirt";
            } else if ("regions_unexplored:alpha_grass_block".equals(currentBlock)) {
                ruDirt = "minecraft:dirt";
            }
        // RU injects this write at the head of TrunkPlacer.setDirtAt. The original method then
        // observes the replacement as ordinary dirt and does not overwrite it when force_dirt is
        // false. The same trunk consumer also includes this support block in decorator context.
        if (ruDirt != null) {
            world.setBlock(position.x, position.y, position.z, ruDirt);
            blocks.logs.add(position);
            if (!config.get("force_dirt")
                .getAsBoolean()) return;
            current = ruDirt;
        }
        boolean ordinaryDirt = engine.isBlockInTag("#minecraft:dirt", current)
            && !"minecraft:grass_block".equals(ModernDecorationEngine.blockName(current))
            && !"minecraft:mycelium".equals(ModernDecorationEngine.blockName(current));
        if (!config.get("force_dirt")
            .getAsBoolean() && ordinaryDirt) return;
        String state = features.stateProvider(engine, config.getAsJsonObject("dirt_provider"), random, position);
        world.setBlock(position.x, position.y, position.z, state);
        // TrunkPlacer.setDirtAt is handed TreeFeature's trunk setter, not its root setter.
        blocks.logs.add(position);
    }

    private void placeDecorators(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        TreeBlocks blocks) {
        if (!config.has("decorators") || config.getAsJsonArray("decorators")
            .size() == 0) return;
        List<Pos> logs = sortedByY(blocks.logs);
        List<Pos> leaves = sortedByY(blocks.leaves);
        for (JsonElement element : config.getAsJsonArray("decorators")) {
            JsonObject decorator = element.getAsJsonObject();
            String type = decorator.get("type")
                .getAsString();
            if ("regions_unexplored:random_branch".equals(type)) {
                placeRandomBranches(engine, decorator, world, random, logs, blocks);
            } else if ("regions_unexplored:group_branch".equals(type)) {
                placeGroupBranches(engine, decorator, world, random, logs, blocks);
            } else if ("minecraft:trunk_vine".equals(type)) {
                placeTrunkVines(world, random, logs, blocks);
            } else if ("minecraft:leave_vine".equals(type)) {
                placeLeafVines(
                    world,
                    random,
                    leaves,
                    decorator.get("probability")
                        .getAsFloat(),
                    blocks);
            } else if ("regions_unexplored:hanging_vines".equals(type)) {
                placeHangingVines(world, random, leaves, decorator, blocks);
            } else if ("regions_unexplored:place_on_ground".equals(type)) {
                placeOnGround(engine, world, random, blocks, logs, decorator);
            } else if ("minecraft:alter_ground".equals(type)) {
                alterGround(engine, world, random, blocks, logs, decorator);
            } else if ("minecraft:cocoa".equals(type)) {
                placeCocoa(
                    world,
                    random,
                    logs,
                    decorator.get("probability")
                        .getAsFloat(),
                    blocks);
            } else if ("minecraft:attached_to_leaves".equals(type)) {
                placeAttachedToLeaves(engine, world, random, leaves, decorator, blocks);
            } else if ("minecraft:beehive".equals(type)) {
                placeBeehive(
                    world,
                    random,
                    logs,
                    leaves,
                    decorator.get("probability")
                        .getAsFloat(),
                    blocks);
            } else {
                throw new IllegalArgumentException("Unsupported pinned tree decorator " + type);
            }
        }
    }

    /** Runs the same pinned decorator closure for RU's hand-authored tree features. */
    void placeCustomDecorators(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Set<Pos> logs, Set<Pos> leaves) {
        TreeBlocks blocks = new TreeBlocks();
        blocks.logs.addAll(logs);
        blocks.leaves.addAll(leaves);
        placeDecorators(engine, config, world, random, blocks);
    }

    private void placeOnGround(ModernDecorationEngine engine, World world, ModernRandom random, TreeBlocks blocks,
        List<Pos> logs, JsonObject decorator) {
        List<Pos> bases = lowestTrunkOrRoot(blocks, logs);
        if (bases.isEmpty()) return;
        Pos first = bases.get(0);
        int minY = first.y;
        int minX = first.x;
        int maxX = first.x;
        int minZ = first.z;
        int maxZ = first.z;
        for (Pos position : bases) {
            if (position.y != minY) continue;
            minX = Math.min(minX, position.x);
            maxX = Math.max(maxX, position.x);
            minZ = Math.min(minZ, position.z);
            maxZ = Math.max(maxZ, position.z);
        }
        int radius = integer(decorator, "radius", 2);
        int vertical = integer(decorator, "height", 1);
        minX -= radius;
        maxX += radius;
        minZ -= radius;
        maxZ += radius;
        int minimumY = minY - vertical;
        int maximumY = minY + vertical;
        for (int attempt = 0; attempt < integer(decorator, "tries", 128); attempt++) {
            int x = minX + random.nextInt(maxX - minX + 1);
            int y = minimumY + random.nextInt(maximumY - minimumY + 1);
            int z = minZ + random.nextInt(maxZ - minZ + 1);
            Pos ground = new Pos(x, y, z);
            String above = world.blockState(x, y + 1, z);
            if ((!isAir(above) && !"minecraft:vine".equals(ModernDecorationEngine.blockName(above)))
                || !engine.isBlockInTag("#minecraft:dirt", world.blockState(x, y, z))
                || world.height("MOTION_BLOCKING_NO_LEAVES", x, z) > y + 1) continue;
            Pos position = ground.offset(0, 1, 0);
            String state = features
                .stateProvider(engine, decorator.getAsJsonObject("block_state_provider"), random, position);
            setDecoratorBlock(world, position, state, blocks);
        }
    }

    private void alterGround(ModernDecorationEngine engine, World world, ModernRandom random, TreeBlocks blocks,
        List<Pos> logs, JsonObject decorator) {
        List<Pos> bases = lowestTrunkOrRoot(blocks, logs);
        if (bases.isEmpty()) return;
        int minimumY = bases.get(0).y;
        for (Pos base : bases) {
            if (base.y != minimumY) continue;
            alterCircle(engine, world, random, decorator, base.offset(-1, 0, -1), blocks);
            alterCircle(engine, world, random, decorator, base.offset(2, 0, -1), blocks);
            alterCircle(engine, world, random, decorator, base.offset(-1, 0, 2), blocks);
            alterCircle(engine, world, random, decorator, base.offset(2, 0, 2), blocks);
            for (int attempt = 0; attempt < 5; attempt++) {
                int selected = random.nextInt(64);
                int x = selected % 8;
                int z = selected / 8;
                if (x != 0 && x != 7 && z != 0 && z != 7) continue;
                alterCircle(engine, world, random, decorator, base.offset(-3 + x, 0, -3 + z), blocks);
            }
        }
    }

    private void alterCircle(ModernDecorationEngine engine, World world, ModernRandom random, JsonObject decorator,
        Pos center, TreeBlocks blocks) {
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            if (Math.abs(x) == 2 && Math.abs(z) == 2) continue;
            Pos column = center.offset(x, 0, z);
            for (int y = 2; y >= -3; y--) {
                Pos candidate = column.offset(0, y, 0);
                String current = world.blockState(candidate.x, candidate.y, candidate.z);
                if ("minecraft:grass_block".equals(ModernDecorationEngine.blockName(current))
                    || engine.isBlockInTag("#minecraft:dirt", current)) {
                    String state = features
                        .stateProvider(engine, decorator.getAsJsonObject("provider"), random, column);
                    setDecoratorBlock(world, candidate, state, blocks);
                    break;
                }
                if (!isAir(current) && y < 0) break;
            }
        }
    }

    private void placeCocoa(World world, ModernRandom random, List<Pos> logs, float probability, TreeBlocks blocks) {
        if (random.nextFloat() >= probability || logs.isEmpty()) return;
        int bottomY = logs.get(0).y;
        for (Pos log : logs) {
            if (log.y - bottomY > 2) continue;
            for (int[] direction : horizontalDirections()) {
                int[] opposite = { -direction[0], -direction[1] };
                Pos position = log.offset(opposite[0], 0, opposite[1]);
                if (random.nextFloat() > 0.25F || !isAir(world.blockState(position.x, position.y, position.z)))
                    continue;
                String state = "minecraft:cocoa[age=" + random.nextInt(3)
                    + ",facing="
                    + horizontalName(direction)
                    + "]";
                setDecoratorBlock(world, position, state, blocks);
            }
        }
    }

    private void placeAttachedToLeaves(ModernDecorationEngine engine, World world, ModernRandom random,
        List<Pos> orderedLeaves, JsonObject decorator, TreeBlocks blocks) {
        List<Pos> leaves = new ArrayList<Pos>(orderedLeaves);
        for (int size = leaves.size(); size > 1; size--) {
            int swap = random.nextInt(size);
            Pos tail = leaves.get(size - 1);
            leaves.set(size - 1, leaves.get(swap));
            leaves.set(swap, tail);
        }
        Set<Pos> excluded = new HashSet<Pos>();
        List<String> directions = new ArrayList<String>();
        for (JsonElement value : decorator.getAsJsonArray("directions")) directions.add(value.getAsString());
        int required = decorator.get("required_empty_blocks")
            .getAsInt();
        int radiusXZ = decorator.get("exclusion_radius_xz")
            .getAsInt();
        int radiusY = decorator.get("exclusion_radius_y")
            .getAsInt();
        for (Pos leaf : leaves) {
            String direction = directions.get(random.nextInt(directions.size()));
            int[] delta = direction3(direction);
            Pos placement = leaf.offset(delta[0], delta[1], delta[2]);
            if (excluded.contains(placement) || random.nextFloat() >= decorator.get("probability")
                .getAsFloat()) {
                continue;
            }
            boolean empty = true;
            for (int distance = 1; distance <= required; distance++) {
                Pos check = leaf.offset(delta[0] * distance, delta[1] * distance, delta[2] * distance);
                if (!isAir(world.blockState(check.x, check.y, check.z))) {
                    empty = false;
                    break;
                }
            }
            if (!empty) continue;
            for (int x = -radiusXZ; x <= radiusXZ; x++) {
                for (int y = -radiusY; y <= radiusY; y++) {
                    for (int z = -radiusXZ; z <= radiusXZ; z++) excluded.add(placement.offset(x, y, z));
                }
            }
            String state = features
                .stateProvider(engine, decorator.getAsJsonObject("block_provider"), random, placement);
            setDecoratorBlock(world, placement, state, blocks);
        }
    }

    private void placeBeehive(World world, ModernRandom random, List<Pos> logs, List<Pos> leaves, float probability,
        TreeBlocks blocks) {
        if (random.nextFloat() >= probability || logs.isEmpty()) return;
        int targetY;
        if (!leaves.isEmpty()) targetY = Math.max(leaves.get(0).y - 1, logs.get(0).y + 1);
        else targetY = Math.min(logs.get(0).y + 1 + random.nextInt(3), logs.get(logs.size() - 1).y);
        // The target calls java.util.Collections.shuffle without the worldgen RandomSource.
        // Its exact position is therefore non-deterministic. Choose the first valid position in
        // the same candidate order while preserving the target's worldgen RNG consumption.
        int[][] spawnDirections = { { 1, 0 }, { 0, 1 }, { -1, 0 } }; // east, south, west; north is excluded.
        for (Pos log : logs) {
            if (log.y != targetY) continue;
            for (int[] direction : spawnDirections) {
                Pos hive = log.offset(direction[0], 0, direction[1]);
                Pos entrance = hive.offset(0, 0, 1);
                if (!isAir(world.blockState(hive.x, hive.y, hive.z))
                    || !isAir(world.blockState(entrance.x, entrance.y, entrance.z))) continue;
                setDecoratorBlock(world, hive, "minecraft:bee_nest[facing=south,honey_level=0]", blocks);
                return;
            }
        }
    }

    private static List<Pos> lowestTrunkOrRoot(TreeBlocks blocks, List<Pos> logs) {
        List<Pos> roots = sortedByY(blocks.roots);
        if (roots.isEmpty()) return new ArrayList<Pos>(logs);
        if (!logs.isEmpty() && roots.get(0).y == logs.get(0).y) {
            List<Pos> both = new ArrayList<Pos>(logs);
            both.addAll(roots);
            return both;
        }
        return roots;
    }

    private void placeTrunkVines(World world, ModernRandom random, List<Pos> logs, TreeBlocks blocks) {
        int[][] offsets = { { -1, 0, 0 }, { 1, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 } };
        String[] faces = { "east", "west", "south", "north" };
        for (Pos log : logs) {
            for (int index = 0; index < offsets.length; index++) {
                Pos position = log.offset(offsets[index][0], 0, offsets[index][2]);
                if (random.nextInt(3) > 0 && isAir(world.blockState(position.x, position.y, position.z))) {
                    setDecoratorBlock(world, position, vineState(faces[index]), blocks);
                    blocks.vines.add(position);
                }
            }
        }
    }

    private void placeLeafVines(World world, ModernRandom random, List<Pos> leaves, float probability,
        TreeBlocks blocks) {
        int[][] offsets = { { -1, 0, 0 }, { 1, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 } };
        String[] faces = { "east", "west", "south", "north" };
        for (Pos leaf : leaves) {
            for (int index = 0; index < offsets.length; index++) {
                Pos position = leaf.offset(offsets[index][0], 0, offsets[index][2]);
                if (random.nextFloat() >= probability || !isAir(world.blockState(position.x, position.y, position.z)))
                    continue;
                setDecoratorBlock(world, position, vineState(faces[index]), blocks);
                blocks.vines.add(position);
                position = position.offset(0, -1, 0);
                for (int remaining = 4; remaining > 0
                    && isAir(world.blockState(position.x, position.y, position.z)); remaining--) {
                    setDecoratorBlock(world, position, vineState(faces[index]), blocks);
                    blocks.vines.add(position);
                    position = position.offset(0, -1, 0);
                }
            }
        }
    }

    private void placeHangingVines(World world, ModernRandom random, List<Pos> leaves, JsonObject decorator,
        TreeBlocks blocks) {
        String block = decorator.get("block")
            .getAsString();
        float probability = decorator.get("probability")
            .getAsFloat();
        for (Pos leaf : leaves) {
            if (probability <= random.nextFloat()) continue;
            Pos position = leaf.offset(0, -1, 0);
            if (!isAir(world.blockState(position.x, position.y, position.z))) continue;
            while (isAir(world.blockState(position.x, position.y - 1, position.z)) && random.nextFloat() > 0.4F) {
                setDecoratorBlock(world, position, ModernFeatureEngine.property(block, "tip", "false"), blocks);
                position = position.offset(0, -1, 0);
            }
            setDecoratorBlock(world, position, ModernFeatureEngine.property(block, "tip", "true"), blocks);
        }
    }

    private static void setDecoratorBlock(World world, Pos position, String state, TreeBlocks blocks) {
        world.setBlock(position.x, position.y, position.z, state);
        blocks.decorators.add(position);
    }

    private static String vineState(String face) {
        String state = "minecraft:vine[east=false,north=false,south=false,up=false,west=false]";
        return ModernFeatureEngine.property(state, face, "true");
    }

    private void placeRandomBranches(ModernDecorationEngine engine, JsonObject decorator, World world,
        ModernRandom random, List<Pos> orderedLogs, TreeBlocks blocks) {
        int topY = Integer.MIN_VALUE;
        for (Pos position : orderedLogs) topY = Math.max(topY, position.y);
        List<Pos> logs = new ArrayList<Pos>(orderedLogs);
        for (int size = logs.size(); size > 1; size--) {
            int swap = random.nextInt(size);
            Pos tail = logs.get(size - 1);
            logs.set(size - 1, logs.get(swap));
            logs.set(swap, tail);
        }
        for (Pos log : logs) {
            if (log.y + 2 >= topY) continue;
            int[] direction = horizontal(random);
            Pos branch = log.offset(direction[0], 0, direction[1]);
            if (random.nextFloat() > decorator.get("probability")
                .getAsFloat()
                || !requiredBranchSpace(
                    engine,
                    world,
                    branch,
                    decorator.get("required_empty_blocks")
                        .getAsInt()))
                continue;
            String facing = horizontalName(direction);
            setDecoratorBlock(
                world,
                branch,
                ModernFeatureEngine.property(
                    decorator.get("branch_block")
                        .getAsString(),
                    "facing",
                    facing),
                blocks);
            if (!decorator.has("leaves_provider")) continue;
            int[][] around = { { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 }, { 1, 0, 0 } };
            for (int[] delta : around) {
                Pos leaf = branch.offset(delta[0], delta[1], delta[2]);
                if (!isAir(world.blockState(leaf.x, leaf.y, leaf.z))) continue;
                String state = features
                    .stateProvider(engine, decorator.getAsJsonObject("leaves_provider"), random, leaf);
                if (state.contains("distance=")) state = ModernFeatureEngine.property(state, "distance", "1");
                setDecoratorBlock(world, leaf, state, blocks);
            }
        }
    }

    private void placeGroupBranches(ModernDecorationEngine engine, JsonObject decorator, World world,
        ModernRandom random, List<Pos> logs, TreeBlocks blocks) {
        int topY = Integer.MIN_VALUE;
        for (Pos position : logs) topY = Math.max(topY, position.y);
        for (Pos log : logs) {
            if (log.y + decorator.get("top_offset")
                .getAsInt() != topY || random.nextFloat() > decorator.get("probability")
                    .getAsFloat())
                continue;
            for (int[] direction : horizontalDirections()) {
                Pos branch = log.offset(direction[0], 0, direction[1]);
                setDecoratorBlock(
                    world,
                    branch,
                    ModernFeatureEngine.property(
                        decorator.get("branch_block")
                            .getAsString(),
                        "facing",
                        horizontalName(direction)),
                    blocks);
                if (!decorator.has("leaves_provider")) continue;
                int[][] around = { { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 }, { 1, 0, 0 } };
                for (int[] delta : around) {
                    Pos leaf = branch.offset(delta[0], delta[1], delta[2]);
                    if (!isAir(world.blockState(leaf.x, leaf.y, leaf.z))) continue;
                    String state = features
                        .stateProvider(engine, decorator.getAsJsonObject("leaves_provider"), random, leaf);
                    if (state.contains("distance=")) state = ModernFeatureEngine.property(state, "distance", "1");
                    setDecoratorBlock(world, leaf, state, blocks);
                }
            }
        }
    }

    private boolean requiredBranchSpace(ModernDecorationEngine engine, World world, Pos branch, int required) {
        for (int down = 0; down <= required; down++) {
            String state = world.blockState(branch.x, branch.y - down, branch.z);
            if (!isAir(state) && !engine.isBlockInTag("#minecraft:leaves", state)) return false;
        }
        return true;
    }

    private static List<Pos> sortedByY(Set<Pos> positions) {
        List<Pos> result = new ArrayList<Pos>(positions);
        java.util.Collections.sort(result, new java.util.Comparator<Pos>() {

            @Override
            public int compare(Pos left, Pos right) {
                return Integer.compare(left.y, right.y);
            }
        });
        return result;
    }

    private static boolean validTreePos(ModernDecorationEngine engine, String state) {
        String block = ModernDecorationEngine.blockName(state);
        return "minecraft:air".equals(block) || "minecraft:cave_air".equals(block)
            || "minecraft:void_air".equals(block)
            || engine.isBlockInTag("#minecraft:replaceable_by_trees", state);
    }

    private static boolean isAir(String state) {
        String block = ModernDecorationEngine.blockName(state);
        return "minecraft:air".equals(block) || "minecraft:cave_air".equals(block)
            || "minecraft:void_air".equals(block);
    }

    private static int integer(JsonObject object, String key, int fallback) {
        return object.has(key) ? object.get(key)
            .getAsInt() : fallback;
    }

    private static int between(ModernRandom random, int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    /** Direction.Plane.HORIZONTAL order in the pinned target: north, east, south, west. */
    private static int[] horizontal(ModernRandom random) {
        switch (random.nextInt(4)) {
            case 0:
                return new int[] { 0, -1 };
            case 1:
                return new int[] { 1, 0 };
            case 2:
                return new int[] { 0, 1 };
            default:
                return new int[] { -1, 0 };
        }
    }

    private static String horizontalName(int[] direction) {
        if (direction[1] < 0) return "north";
        if (direction[0] > 0) return "east";
        if (direction[1] > 0) return "south";
        return "west";
    }

    private static int[] direction3(String direction) {
        if ("up".equals(direction)) return new int[] { 0, 1, 0 };
        if ("down".equals(direction)) return new int[] { 0, -1, 0 };
        if ("north".equals(direction)) return new int[] { 0, 0, -1 };
        if ("south".equals(direction)) return new int[] { 0, 0, 1 };
        if ("west".equals(direction)) return new int[] { -1, 0, 0 };
        if ("east".equals(direction)) return new int[] { 1, 0, 0 };
        throw new IllegalArgumentException("Unknown direction " + direction);
    }

    private static int[][] horizontalDirections() {
        return new int[][] { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
    }

    private static int[] clockwise(int[] direction) {
        return new int[] { -direction[1], direction[0] };
    }

    private static int[] counterClockwise(int[] direction) {
        return new int[] { direction[1], -direction[0] };
    }

    private static final class Attachment {

        private final Pos position;
        private final int radiusOffset;
        private final boolean doubleTrunk;

        private Attachment(Pos position, int radiusOffset, boolean doubleTrunk) {
            this.position = position;
            this.radiusOffset = radiusOffset;
            this.doubleTrunk = doubleTrunk;
        }
    }

    private static final class FancyAttachment {

        private final Pos position;
        private final int branchY;

        private FancyAttachment(Pos position, int branchY) {
            this.position = position;
            this.branchY = branchY;
        }
    }

    private static final class RootPosition {

        private final Pos position;
        private final String axis;

        private RootPosition(Pos position, String axis) {
            this.position = position;
            this.axis = axis;
        }
    }

    private static final class TreeBlocks {

        private final Set<Pos> roots = new HashSet<Pos>();
        private final Set<Pos> logs = new HashSet<Pos>();
        private final Set<Pos> leaves = new HashSet<Pos>();
        private final Set<Pos> vines = new HashSet<Pos>();
        private final Set<Pos> decorators = new HashSet<Pos>();
    }
}
