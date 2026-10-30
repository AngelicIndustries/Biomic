package industries.angelic.biomic.world.modern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import industries.angelic.biomic.content.RuDecorationPaletteMetadata;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernLegacyRandom;
import industries.angelic.biomic.world.modern.noise.ModernNormalNoise;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Codec-driven implementations of modern configured features shared by vanilla and RU. */
public final class ModernFeatureEngine implements ModernDecorationEngine.FeatureExecutor {

    private final Map<String, ModernNormalNoise> providerNoises = new HashMap<String, ModernNormalNoise>();
    private final ModernTreeFeature treeFeature = new ModernTreeFeature(this);
    private final ModernRuTreeFeatures ruTreeFeatures = new ModernRuTreeFeatures(this);
    private final ModernRuAspenFeature ruAspenFeature = new ModernRuAspenFeature(this, treeFeature);
    private final ModernRuSakuraFeature ruSakuraFeature = new ModernRuSakuraFeature(this);
    private final ModernRuStrippedPineFeature ruStrippedPineFeature = new ModernRuStrippedPineFeature(this);
    private final ModernRuLarchFeature ruLarchFeature = new ModernRuLarchFeature(this);
    private final ModernRuBaobabFeature ruBaobabFeature = new ModernRuBaobabFeature(this);
    private final ModernRuBioshroomFeatures ruBioshroomFeatures = new ModernRuBioshroomFeatures(this);
    private final ModernRuEucalyptusFeature ruEucalyptusFeature = new ModernRuEucalyptusFeature(this);
    private final ModernRuCypressFeature ruCypressFeature = new ModernRuCypressFeature(this);
    private final ModernRuWetlandFeatures ruWetlandFeatures = new ModernRuWetlandFeatures();
    private final ModernRuGiantCypressFeature ruGiantCypressFeature = new ModernRuGiantCypressFeature(this);
    private final ModernRuSeaRockFeature ruSeaRockFeature = new ModernRuSeaRockFeature();
    private final ModernRuBlackwoodFeature ruBlackwoodFeature = new ModernRuBlackwoodFeature(this);
    private final ModernRuLushPineFeature ruLushPineFeature = new ModernRuLushPineFeature(this);
    private final ModernRuKapokFeature ruKapokFeature = new ModernRuKapokFeature(this);
    private final ModernRuRockPillarFeature ruRockPillarFeature = new ModernRuRockPillarFeature();
    private final ModernLithostitchedLargeDripstoneFeature lithostitchedLargeDripstone = new ModernLithostitchedLargeDripstoneFeature(
        this);
    private final ModernRuCarvedPoolFeature ruCarvedPoolFeature = new ModernRuCarvedPoolFeature(this);
    private final ModernVanillaCaveFeatures vanillaCaveFeatures = new ModernVanillaCaveFeatures();
    private final ModernVanillaSurfaceFeatures vanillaSurfaceFeatures = new ModernVanillaSurfaceFeatures();
    private final ModernVanillaGeodeFeature vanillaGeodeFeature = new ModernVanillaGeodeFeature(this);
    private final ModernVanillaCoralFeatures vanillaCoralFeatures = new ModernVanillaCoralFeatures();
    private final ModernVanillaIcebergFeature vanillaIcebergFeature = new ModernVanillaIcebergFeature();
    private final ModernVanillaRootSystemFeature vanillaRootSystemFeature = new ModernVanillaRootSystemFeature(this);
    private final ModernVanillaMonsterRoomFeature vanillaMonsterRoomFeature = new ModernVanillaMonsterRoomFeature();
    private final ModernVanillaFossilFeature vanillaFossilFeature = new ModernVanillaFossilFeature();
    private final ModernVanillaSculkPatchFeature vanillaSculkPatchFeature = new ModernVanillaSculkPatchFeature();

    @Override
    public boolean place(ModernDecorationEngine engine, String type, JsonObject config, World world,
        ModernRandom random, Pos position) {
        // Feature.place(configuration, level, ...) checks ensureCanWrite before entering ANY
        // configured feature, including selectors/patches. A Y=256 candidate must not consume
        // RNG or generate an inward-spreading patch; skipping only its final writes is too late.
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return false;
        if ("minecraft:simple_block".equals(type)) {
            return simpleBlock(engine, config, world, random, position);
        }
        if ("minecraft:random_patch".equals(type) || "minecraft:flower".equals(type)
            || "minecraft:no_bonemeal_flower".equals(type)) {
            return randomPatch(engine, config, world, random, position);
        }
        if ("minecraft:random_selector".equals(type)) {
            return randomSelector(engine, config, world, random, position);
        }
        if ("minecraft:simple_random_selector".equals(type)) {
            JsonArray features = config.getAsJsonArray("features");
            return engine.place(features.get(random.nextInt(features.size())), null, world, random, position);
        }
        if ("minecraft:random_boolean_selector".equals(type)) {
            return engine.place(
                config.get(random.nextBoolean() ? "feature_true" : "feature_false"),
                null,
                world,
                random,
                position);
        }
        if ("lithostitched:weighted_selector".equals(type)) {
            WeightedSelection selected = weightedDataWithRoll(config.getAsJsonArray("features"), random, true);
            if (selected != null) {
                world.recordPlacementCandidate(
                    position,
                    -4,
                    "weighted_roll=" + selected.roll
                        + "/"
                        + selected.total
                        + ";selected="
                        + placedFeatureIdentifier(selected.data));
            }
            return selected != null && engine.place(selected.data, null, world, random, position);
        }
        if ("lithostitched:placed".equals(type)) {
            return engine.place(config, null, world, random, position);
        }
        if ("lithostitched:composite".equals(type)) {
            return composite(engine, config, world, random, position);
        }
        if ("minecraft:block_column".equals(type)) {
            return blockColumn(engine, config, world, random, position);
        }
        if ("minecraft:block_pile".equals(type)) {
            return blockPile(engine, config, world, random, position);
        }
        if ("minecraft:disk".equals(type)) {
            return disk(engine, config, world, random, position);
        }
        if ("minecraft:ore".equals(type)) {
            return ore(engine, config, world, random, position);
        }
        if ("minecraft:spring_feature".equals(type)) {
            return spring(engine, config, world, position);
        }
        if ("minecraft:seagrass".equals(type)) {
            return seagrass(config, world, random, position);
        }
        if ("minecraft:bamboo".equals(type)) {
            return bamboo(engine, config, world, random, position);
        }
        if ("minecraft:forest_rock".equals(type)) {
            return blockBlob(engine, config, world, random, position);
        }
        if ("minecraft:blue_ice".equals(type)) {
            return blueIce(world, random, position);
        }
        if ("minecraft:kelp".equals(type)) {
            return kelp(world, random, position);
        }
        if ("minecraft:sea_pickle".equals(type)) {
            return seaPickle(engine, config, world, random, position);
        }
        if ("minecraft:vines".equals(type)) {
            return vines(world, position);
        }
        if ("regions_unexplored:rock".equals(type)) {
            return ruRock(engine, config, world, random, position);
        }
        if ("regions_unexplored:fallen_tree".equals(type)) {
            return ruFallenTree(engine, config, world, random, position);
        }
        if ("minecraft:huge_brown_mushroom".equals(type)) {
            return hugeMushroom(engine, config, world, random, position, false);
        }
        if ("minecraft:huge_red_mushroom".equals(type)) {
            return hugeMushroom(engine, config, world, random, position, true);
        }
        if ("minecraft:vegetation_patch".equals(type)) {
            return vegetationPatch(engine, config, world, random, position, false);
        }
        if ("minecraft:waterlogged_vegetation_patch".equals(type)) {
            return vegetationPatch(engine, config, world, random, position, true);
        }
        if ("minecraft:multiface_growth".equals(type)) {
            return multifaceGrowth(config, world, random, position);
        }
        if ("minecraft:nether_forest_vegetation".equals(type)) {
            return netherForestVegetation(engine, config, world, random, position);
        }
        if ("regions_unexplored:giant_lily".equals(type)) {
            return ruGiantLily(world, position);
        }
        if ("regions_unexplored:hyacinth_plants".equals(type)) {
            return ruHyacinthPlants(world, random, position);
        }
        if ("regions_unexplored:tall_hyacinth_stock".equals(type)) {
            return ruHyacinthStock(engine, config, world, random, position);
        }
        if ("regions_unexplored:small_yellow_bioshroom".equals(type)) {
            return ruSmallYellowBioshroom(engine, world, random, position);
        }
        if ("regions_unexplored:tree_shrub".equals(type)) {
            return ruTreeShrub(engine, config, world, random, position);
        }
        if ("regions_unexplored:glistering_ivy".equals(type)) {
            return ruRoofGrowth(world, random, position, false);
        }
        if ("regions_unexplored:hanging_earlight".equals(type)) {
            return ruRoofGrowth(world, random, position, true);
        }
        if ("regions_unexplored:brim_willow".equals(type) || "regions_unexplored:tall_brim_willow".equals(type)) {
            return industries.angelic.biomic.world.modern.ruport.BrimWillowCompat
                .place(type.contains(":tall_"), engine, world, random, position);
        }
        if ("regions_unexplored:nether_rock".equals(type)) {
            return ruNetherRock(engine, world, random, position);
        }
        if ("regions_unexplored:obsidian_spire".equals(type)) {
            return ruObsidianSpire();
        }
        if ("regions_unexplored:basalt_blob".equals(type)) {
            return ruBasaltBlob(engine, config, world, random, position);
        }
        if ("regions_unexplored:icicle_up".equals(type)) {
            return ruFloorIcicle(world, position);
        }
        if ("minecraft:tree".equals(type)) {
            return treeFeature.place(engine, config, world, random, position);
        }
        if ("regions_unexplored:aspen_tree".equals(type)) {
            return ruAspenFeature.place(engine, config, world, random, position);
        }
        if ("regions_unexplored:sakura_tree".equals(type)) {
            return ruSakuraFeature.place(engine, config, world, random, position);
        }
        if ("regions_unexplored:stripped_pine_tree".equals(type)) {
            return ruStrippedPineFeature.place(engine, config, world, random, position);
        }
        if ("regions_unexplored:larch_tree".equals(type)) {
            return ruLarchFeature.place(engine, config, world, random, position);
        }
        if ("regions_unexplored:mega_baobab_tree".equals(type)) {
            return ruBaobabFeature.place(engine, config, world, random, position, false);
        }
        if ("regions_unexplored:ultra_baobab_tree".equals(type)) {
            return ruBaobabFeature.place(engine, config, world, random, position, true);
        }
        if ("regions_unexplored:giant_yellow_bioshroom".equals(type)
            || "regions_unexplored:giant_pink_bioshroom".equals(type)
            || "regions_unexplored:giant_green_bioshroom".equals(type)
            || "regions_unexplored:giant_blue_bioshroom".equals(type)) {
            return ruBioshroomFeatures.place(engine, type, config, world, random, position);
        }
        if ("regions_unexplored:small_eucalyptus_tree".equals(type)) {
            return ruEucalyptusFeature.placeSmall(engine, config, world, random, position);
        }
        if ("regions_unexplored:eucalyptus_tree".equals(type)) {
            return ruEucalyptusFeature.placeLarge(engine, config, world, random, position);
        }
        if ("regions_unexplored:cypress_tree".equals(type)) {
            return ruCypressFeature.place(engine, config, world, random, position);
        }
        if ("regions_unexplored:giant_cypress_tree".equals(type)) {
            return ruGiantCypressFeature.place(engine, config, world, random, position);
        }
        if ("regions_unexplored:water_edge".equals(type) || "regions_unexplored:marsh".equals(type)
            || "regions_unexplored:hanging_prismarite".equals(type)) {
            return ruWetlandFeatures.place(type, world, random, position);
        }
        if ("regions_unexplored:ocean_rock".equals(type)) {
            return ruSeaRockFeature.place(config, world, random, position);
        }
        if ("regions_unexplored:blackwood_tree".equals(type)) {
            return ruBlackwoodFeature.place(engine, config, world, random, position);
        }
        if ("regions_unexplored:lush_pine_tree".equals(type)) {
            return ruLushPineFeature.place(engine, config, world, random, position);
        }
        if ("regions_unexplored:kapok_tree".equals(type)) {
            return ruKapokFeature.place(engine, config, world, random, position);
        }
        if ("regions_unexplored:rock_pillar".equals(type)) {
            return ruRockPillarFeature.place(world, random, position);
        }
        if ("lithostitched:large_dripstone".equals(type)) {
            return lithostitchedLargeDripstone.place(engine, config, world, random, position);
        }
        if ("regions_unexplored:carved_limited_pool".equals(type)) {
            return ruCarvedPoolFeature.place(engine, config, world, random, position);
        }
        if ("minecraft:pointed_dripstone".equals(type)) {
            return vanillaCaveFeatures.placePointedDripstone(engine, config, world, random, position);
        }
        if ("minecraft:underwater_magma".equals(type)) {
            return vanillaCaveFeatures.placeUnderwaterMagma(config, world, random, position);
        }
        if ("minecraft:freeze_top_layer".equals(type)) {
            return vanillaCaveFeatures.freezeTopLayer(world, position);
        }
        if ("minecraft:dripstone_cluster".equals(type)) {
            return vanillaCaveFeatures.placeDripstoneCluster(engine, config, world, random, position);
        }
        if ("minecraft:large_dripstone".equals(type)) {
            return lithostitchedLargeDripstone.placeVanilla(engine, config, world, random, position);
        }
        if ("minecraft:desert_well".equals(type)) {
            return vanillaSurfaceFeatures.placeDesertWell(world, random, position);
        }
        if ("minecraft:ice_spike".equals(type)) {
            return vanillaSurfaceFeatures.placeIceSpike(world, random, position);
        }
        if ("minecraft:lake".equals(type)) {
            return vanillaSurfaceFeatures.placeLake(this, engine, config, world, random, position);
        }
        if ("minecraft:geode".equals(type)) {
            return vanillaGeodeFeature.place(engine, config, world, random, position);
        }
        if ("minecraft:coral_tree".equals(type) || "minecraft:coral_claw".equals(type)
            || "minecraft:coral_mushroom".equals(type)) {
            return vanillaCoralFeatures.place(type, world, random, position);
        }
        if ("minecraft:iceberg".equals(type)) {
            return vanillaIcebergFeature
                .place(ModernDecorationEngine.state(config.getAsJsonObject("state")), world, random, position);
        }
        if ("minecraft:root_system".equals(type)) {
            return vanillaRootSystemFeature.place(engine, config, world, random, position);
        }
        if ("minecraft:monster_room".equals(type)) {
            return vanillaMonsterRoomFeature.place(engine, world, random, position);
        }
        if ("minecraft:fossil".equals(type)) {
            return vanillaFossilFeature.place(engine, config, world, random, position);
        }
        if ("minecraft:sculk_patch".equals(type)) {
            return vanillaSculkPatchFeature.place(engine, config, world, random, position);
        }
        if ("regions_unexplored:ashen_tree".equals(type) || "regions_unexplored:small_joshua_tree".equals(type)
            || "regions_unexplored:medium_joshua_tree".equals(type)
            || "regions_unexplored:large_joshua_tree".equals(type)
            || "regions_unexplored:saguaro_cactus".equals(type)
            || "regions_unexplored:palm_tree".equals(type)
            || "regions_unexplored:small_socotra_tree".equals(type)
            || "regions_unexplored:large_socotra_tree".equals(type)
            || "regions_unexplored:bamboo_tree".equals(type)
            || "regions_unexplored:cobalt_tree".equals(type)
            || "regions_unexplored:small_oak_tree".equals(type)
            || "regions_unexplored:dead_tree".equals(type)
            || "regions_unexplored:spire".equals(type)) {
            return ruTreeFeatures.place(engine, type, config, world, random, position);
        }
        throw new IllegalArgumentException("Unsupported pinned configured feature " + type);
    }

    private boolean blockPile(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        if (origin.y < world.minBuildHeight() + 5) return false;
        int radiusX = 2 + random.nextInt(2);
        int radiusZ = 2 + random.nextInt(2);
        JsonObject provider = config.getAsJsonObject("state_provider");
        for (int z = origin.z - radiusZ; z <= origin.z + radiusZ; z++) {
            for (int y = origin.y; y <= origin.y + 1; y++) {
                for (int x = origin.x - radiusX; x <= origin.x + radiusX; x++) {
                    int deltaX = origin.x - x;
                    int deltaZ = origin.z - z;
                    boolean selected = deltaX * deltaX + deltaZ * deltaZ
                        <= random.nextFloat() * 10.0F - random.nextFloat() * 6.0F;
                    if (!selected && random.nextFloat() >= 0.031F) continue;
                    if (!isAir(world.blockState(x, y, z))) continue;
                    String below = ModernDecorationEngine.blockName(world.blockState(x, y - 1, z));
                    if ("minecraft:dirt_path".equals(below)) {
                        if (!random.nextBoolean()) continue;
                    } else if (!world.hasSturdyFace(x, y - 1, z, "up")) {
                        continue;
                    }
                    world.setBlock(x, y, z, stateProvider(engine, provider, random, new Pos(x, y, z), world));
                }
            }
        }
        return true;
    }

    /** Exact shape semantics; pinned RU incorrectly draws size from the mutable level RNG. */
    private boolean ruFloorIcicle(World world, Pos origin) {
        int size = world.levelRandomInt(origin, industries.angelic.biomic.world.replay.RuIncidentalRandom.ICICLE, 4)
            + 2;
        // The source repeats the origin replacement check without moving its check cursor.
        if (!world.isReplaceable(origin.x, origin.y, origin.z)) return false;
        for (int y = 0; y < size; y++) {
            String thickness = y == size - 1 ? "tip"
                : y == size - 2 ? "frustum" : y == 0 && size >= 3 ? "base" : "middle";
            world.setBlock(
                origin.x,
                origin.y + y,
                origin.z,
                "regions_unexplored:icicle[thickness=" + thickness + ",vertical_direction=up,waterlogged=false]");
        }
        return true;
    }

    /** Exact 1.21.1 MultifaceGrowthFeature, including its pinned one-offset search quirk. */
    private boolean multifaceGrowth(JsonObject config, World world, ModernRandom random, Pos origin) {
        String current = world.blockState(origin.x, origin.y, origin.z);
        if (!isAirOrWater(current)) return false;

        List<String> directions = multifaceDirections(config);
        shuffle(directions, random);
        if (placeMultifaceGrowth(config, world, random, origin, current, directions)) return true;

        for (String searchDirection : directions) {
            List<String> faceDirections = multifaceDirections(config);
            faceDirections.remove(opposite(searchDirection));
            shuffle(faceDirections, random);
            for (int distance = 0; distance < config.get("search_range")
                .getAsInt(); distance++) {
                // This intentionally uses origin every time. That is what the pinned 1.21.1
                // bytecode does; search_range repeats the same candidate rather than advancing.
                int[] step = direction(searchDirection);
                Pos candidate = origin.offset(step[0], step[1], step[2]);
                current = world.blockState(candidate.x, candidate.y, candidate.z);
                if (!isAirOrWater(current) && !config.get("block")
                    .getAsString()
                    .equals(ModernDecorationEngine.blockName(current))) break;
                if (placeMultifaceGrowth(config, world, random, candidate, current, faceDirections)) return true;
            }
        }
        return false;
    }

    private static List<String> multifaceDirections(JsonObject config) {
        List<String> directions = new ArrayList<String>(6);
        if (config.get("can_place_on_ceiling")
            .getAsBoolean()) directions.add("up");
        if (config.get("can_place_on_floor")
            .getAsBoolean()) directions.add("down");
        if (config.get("can_place_on_wall")
            .getAsBoolean()) {
            directions.add("north");
            directions.add("east");
            directions.add("south");
            directions.add("west");
        }
        return directions;
    }

    private boolean placeMultifaceGrowth(JsonObject config, World world, ModernRandom random, Pos position,
        String current, List<String> directions) {
        for (String face : directions) {
            int[] delta = direction(face);
            String support = world.blockState(position.x + delta[0], position.y + delta[1], position.z + delta[2]);
            if (!containsConfiguredBlock(config.get("can_be_placed_on"), ModernDecorationEngine.blockName(support)))
                continue;
            // MultifaceBlock.getStateForPlacement returns null for an already-present face.
            // The feature returns false immediately, without consuming spread randomness.
            if (config.get("block")
                .getAsString()
                .equals(ModernDecorationEngine.blockName(current)) && stateBoolean(current, face)) return false;
            String state = multifaceState(
                config.get("block")
                    .getAsString(),
                current,
                face);
            world.setBlock(position.x, position.y, position.z, state);
            if (random.nextFloat() < config.get("chance_of_spreading")
                .getAsFloat()) {
                spreadMultiface(
                    world,
                    random,
                    position,
                    state,
                    face,
                    config.get("block")
                        .getAsString());
            }
            return true;
        }
        return false;
    }

    /** Exact DefaultSpreaderConfig order: same-position, same-plane, then wrap-around. */
    private void spreadMultiface(World world, ModernRandom random, Pos source, String sourceState, String sourceFace,
        String block) {
        List<String> towardDirections = new ArrayList<String>();
        Collections.addAll(towardDirections, "down", "up", "north", "south", "west", "east");
        shuffle(towardDirections, random);
        for (String toward : towardDirections) {
            if (sameAxis(sourceFace, toward) || stateBoolean(sourceState, toward)) continue;

            Pos[] positions = new Pos[] { source, relative(source, toward),
                relative(relative(source, toward), sourceFace) };
            String[] faces = new String[] { toward, sourceFace, opposite(toward) };
            for (int type = 0; type < positions.length; type++) {
                Pos target = positions[type];
                String face = faces[type];
                String targetState = world.blockState(target.x, target.y, target.z);
                String targetBlock = ModernDecorationEngine.blockName(targetState);
                if (block.equals(targetBlock) && stateBoolean(targetState, face)) continue;
                if (!isAir(targetState) && !"minecraft:water".equals(targetBlock) && !block.equals(targetBlock))
                    continue;
                int[] supportOffset = direction(face);
                Pos support = target.offset(supportOffset[0], supportOffset[1], supportOffset[2]);
                if (!world.hasSturdyFace(support.x, support.y, support.z, opposite(face))) continue;
                world.setBlock(target.x, target.y, target.z, multifaceState(block, targetState, face));
                return;
            }
        }
    }

    private static String multifaceState(String block, String current, String face) {
        String state;
        if (block.equals(ModernDecorationEngine.blockName(current))) {
            state = current;
        } else {
            state = block + "[down=false,east=false,north=false,south=false,up=false,waterlogged="
                + ("minecraft:water".equals(ModernDecorationEngine.blockName(current)) ? "true" : "false")
                + ",west=false]";
        }
        return property(state, face, "true");
    }

    private static boolean stateBoolean(String state, String property) {
        int open = state.indexOf('[');
        if (open < 0) return false;
        String body = state.substring(open + 1, state.length() - 1);
        for (String entry : body.split(",")) {
            if ((property + "=true").equals(entry)) return true;
        }
        return false;
    }

    private static Pos relative(Pos position, String direction) {
        int[] delta = direction(direction);
        return position.offset(delta[0], delta[1], delta[2]);
    }

    private static boolean sameAxis(String first, String second) {
        return ("up".equals(first) || "down".equals(first)) ? ("up".equals(second) || "down".equals(second))
            : ("north".equals(first) || "south".equals(first)) ? ("north".equals(second) || "south".equals(second))
                : ("west".equals(second) || "east".equals(second));
    }

    private static boolean isAirOrWater(String state) {
        String block = ModernDecorationEngine.blockName(state);
        return isAir(state) || "minecraft:water".equals(block);
    }

    private static boolean containsConfiguredBlock(JsonElement blocks, String wanted) {
        if (blocks.isJsonPrimitive()) return wanted.equals(blocks.getAsString());
        for (JsonElement block : blocks.getAsJsonArray()) {
            if (wanted.equals(block.getAsString())) return true;
        }
        return false;
    }

    /** Exact port of vanilla 1.21.1's {@code NetherForestVegetationFeature}. */
    private boolean netherForestVegetation(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, Pos origin) {
        if (!engine.isBlockInTag("#minecraft:nylium", world.blockState(origin.x, origin.y - 1, origin.z))) return false;
        if (origin.y < world.minBuildHeight() + 1 || origin.y + 1 >= world.maxBuildHeight()) return false;

        int spreadWidth = config.get("spread_width")
            .getAsInt();
        int spreadHeight = config.get("spread_height")
            .getAsInt();
        int successes = 0;
        for (int attempt = 0; attempt < spreadWidth * spreadWidth; attempt++) {
            Pos position = origin.offset(
                random.nextInt(spreadWidth) - random.nextInt(spreadWidth),
                random.nextInt(spreadHeight) - random.nextInt(spreadHeight),
                random.nextInt(spreadWidth) - random.nextInt(spreadWidth));
            String state = stateProvider(engine, config.getAsJsonObject("state_provider"), random, position, world);
            if (isAir(world.blockState(position.x, position.y, position.z)) && position.y > world.minBuildHeight()
                && world.canSurvive(state, position.x, position.y, position.z)) {
                world.setBlock(position.x, position.y, position.z, state);
                successes++;
            }
        }
        return successes > 0;
    }

    /**
     * The pinned runtime's four sequential placements immediately fail their mutual
     * survival checks. The configured feature therefore observably places nothing
     * and reports false, despite the misleading source-level placement calls.
     */
    private boolean ruGiantLily(World world, Pos origin) {
        int[][] offsets = { { 0, 0 }, { 0, 1 }, { -1, 1 }, { -1, 0 } };
        String[] facings = { "north", "east", "south", "west" };
        for (int[] offset : offsets) {
            int x = origin.x + offset[0], z = origin.z + offset[1];
            if (!ModernAquaticPlantSupport.sourceWater(world.blockState(x, origin.y - 1, z))
                || !ModernSurfaceSystem.isAirState(world.blockState(x, origin.y, z))) return false;
        }
        for (int i = 0; i < offsets.length; i++) world.setBlock(
            origin.x + offsets[i][0],
            origin.y,
            origin.z + offsets[i][1],
            "regions_unexplored:giant_lily_pad[facing=" + facings[i] + "]");
        // Pinned RU returns false even after placing all four quadrants. Preserve its selector result.
        return false;
    }

    /**
     * Port of RU's HyacinthPlantsFeature (its probability field is unused upstream), blooms only: its seagrass is
     * Minecraft's animated one, creative-only here (review, 2026-10-24: "there is still animated biomic seagrass in
     * the hyacinth biome"); MarineLayer dresses the floor with its own. The random sequence is RU's.
     */
    private boolean ruHyacinthPlants(World world, ModernRandom random, Pos origin) {
        int x = origin.x + random.nextInt(8) - random.nextInt(8);
        int z = origin.z + random.nextInt(8) - random.nextInt(8);
        int y = world.height("OCEAN_FLOOR", x, z);
        if ("minecraft:water".equals(ModernDecorationEngine.blockName(world.blockState(x, y, z)))
            && world.canSurvive("minecraft:seagrass", x, y, z)
            && random.nextInt(7) == 0) {
            world.setBlock(x, y, z, "regions_unexplored:hyacinth_bloom");
        }
        return true;
    }

    /** Exact state-shape progression from RU's HyacinthStockFeature. */
    private boolean ruHyacinthStock(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        if (!world.hasSturdyFace(origin.x, origin.y - 1, origin.z, "up")) return false;
        JsonObject provider = config.getAsJsonObject("stock_provider");
        Pos cursor = origin;
        for (int offset = 0; offset <= height; offset++) {
            if (!"minecraft:water"
                .equals(ModernDecorationEngine.blockName(world.blockState(cursor.x, cursor.y, cursor.z)))) break;
            placeHyacinthStock(engine, provider, world, random, cursor);
            cursor = cursor.offset(0, 1, 0);
        }
        return true;
    }

    private void placeHyacinthStock(ModernDecorationEngine engine, JsonObject provider, World world,
        ModernRandom random, Pos position) {
        String below = world.blockState(position.x, position.y - 1, position.z);
        String twoBelow = world.blockState(position.x, position.y - 2, position.z);
        if (!"regions_unexplored:tall_hyacinth_stock".equals(ModernDecorationEngine.blockName(below))) {
            world
                .setBlock(position.x, position.y, position.z, stateProvider(engine, provider, random, position, world));
        }
        // These are deliberately independent checks, matching the source.
        if ("regions_unexplored:tall_hyacinth_stock".equals(ModernDecorationEngine.blockName(below))
            && stockShape(below, "base_tip")) {
            world.setBlock(
                position.x,
                position.y,
                position.z,
                property(stateProvider(engine, provider, random, position, world), "tall_hyacinth_stock_shape", "tip"));
            world.setBlock(
                position.x,
                position.y - 1,
                position.z,
                property(
                    stateProvider(engine, provider, random, position.offset(0, -1, 0), world),
                    "tall_hyacinth_stock_shape",
                    "base_frustum"));
        }
        if ("regions_unexplored:tall_hyacinth_stock".equals(ModernDecorationEngine.blockName(twoBelow))
            && stockShape(twoBelow, "base_frustum")) {
            world.setBlock(
                position.x,
                position.y,
                position.z,
                property(stateProvider(engine, provider, random, position, world), "tall_hyacinth_stock_shape", "tip"));
            world.setBlock(
                position.x,
                position.y - 1,
                position.z,
                property(
                    stateProvider(engine, provider, random, position.offset(0, -1, 0), world),
                    "tall_hyacinth_stock_shape",
                    "frustum"));
            world.setBlock(
                position.x,
                position.y - 2,
                position.z,
                property(
                    stateProvider(engine, provider, random, position.offset(0, -2, 0), world),
                    "tall_hyacinth_stock_shape",
                    "base"));
        }
        if ("regions_unexplored:tall_hyacinth_stock".equals(ModernDecorationEngine.blockName(twoBelow))
            && stockShape(twoBelow, "frustum")) {
            world.setBlock(
                position.x,
                position.y,
                position.z,
                property(stateProvider(engine, provider, random, position, world), "tall_hyacinth_stock_shape", "tip"));
            world.setBlock(
                position.x,
                position.y - 1,
                position.z,
                property(
                    stateProvider(engine, provider, random, position.offset(0, -1, 0), world),
                    "tall_hyacinth_stock_shape",
                    "frustum"));
            world.setBlock(
                position.x,
                position.y - 2,
                position.z,
                property(
                    stateProvider(engine, provider, random, position.offset(0, -2, 0), world),
                    "tall_hyacinth_stock_shape",
                    "middle"));
        }
    }

    private static boolean stockShape(String state, String shape) {
        return state.contains("tall_hyacinth_stock_shape=" + shape + "]")
            || state.contains("tall_hyacinth_stock_shape=" + shape + ",");
    }

    private boolean ruSmallYellowBioshroom(ModernDecorationEngine engine, World world, ModernRandom random,
        Pos origin) {
        if (!ruReplaceable(engine, world, origin)) return false;
        Pos cursor = origin;
        placeYellowBioshroomStem(engine, world, cursor);
        cursor = cursor.offset(0, 1, 0);
        placeYellowBioshroomStem(engine, world, cursor);
        cursor = cursor.offset(0, 1, 0);
        if (random.nextInt(3) == 0) {
            placeYellowBioshroomStem(engine, world, cursor);
            cursor = cursor.offset(0, 1, 0);
        }
        for (int z = -1; z <= 1; z++) {
            for (int x = -1; x <= 1; x++) {
                Pos cap = cursor.offset(x, 0, z);
                if (ruReplaceable(engine, world, cap)) {
                    world.setBlock(cap.x, cap.y, cap.z, "regions_unexplored:yellow_bioshroom_block");
                }
            }
        }
        return true;
    }

    private void placeYellowBioshroomStem(ModernDecorationEngine engine, World world, Pos position) {
        String current = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        if ("regions_unexplored:glistering_nylium".equals(current)
            || "regions_unexplored:mycotoxic_moss".equals(current)
            || "regions_unexplored:brimsprout_nylium".equals(current)) {
            world.setBlock(position.x, position.y, position.z, "minecraft:netherrack");
        }
        if ("regions_unexplored:cobalt_nylium".equals(current)) {
            world.setBlock(position.x, position.y, position.z, "minecraft:blackstone");
        } else if (ruReplaceable(engine, world, position)) {
            world.setBlock(position.x, position.y, position.z, "regions_unexplored:yellow_bioshroom_stem[axis=y]");
        }
        Pos below = position.offset(0, -1, 0);
        String belowBlock = ModernDecorationEngine.blockName(world.blockState(below.x, below.y, below.z));
        if ("regions_unexplored:glistering_nylium".equals(belowBlock)
            || "regions_unexplored:mycotoxic_moss".equals(belowBlock)
            || "regions_unexplored:brimsprout_nylium".equals(belowBlock)) {
            world.setBlock(below.x, below.y, below.z, "minecraft:netherrack");
        }
        if ("regions_unexplored:cobalt_nylium".equals(belowBlock)) {
            world.setBlock(below.x, below.y, below.z, "minecraft:blackstone");
        } else if (ruReplaceable(engine, world, below)) {
            world.setBlock(below.x, below.y, below.z, "regions_unexplored:yellow_bioshroom_stem[axis=y]");
        }
    }

    /** Exact structural port of RU's TreeShrubFeature, including its unseeded root depth. */
    private boolean ruTreeShrub(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int variation = config.get("size_variation")
            .getAsInt();
        int height = variation <= 0 ? config.get("minimum_size")
            .getAsInt()
            : random.nextInt(variation) + config.get("minimum_size")
                .getAsInt();
        for (int offset = 0; offset <= height; offset++) {
            if (!ruReplaceable(engine, world, origin.offset(0, offset, 0))) return false;
        }
        Pos cursor = origin;
        for (int offset = 0; offset <= height; offset++) {
            placeRuShrubLog(engine, config, world, random, cursor);
            if (offset == 0) placeRuShrubRoot(engine, config, world, random, cursor);
            if (offset == height) {
                int[][] leaves = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 } };
                for (int[] delta : leaves)
                    placeRuShrubLeaf(engine, config, world, random, cursor.offset(delta[0], delta[1], delta[2]));
            }
            cursor = cursor.offset(0, 1, 0);
        }
        return true;
    }

    private void placeRuShrubRoot(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int depth = new java.util.Random().nextInt(2) + 4;
        Pos cursor = origin;
        for (int offset = 0; offset <= depth; offset++) {
            String current = world.blockState(cursor.x, cursor.y, cursor.z);
            String above = world.blockState(cursor.x, cursor.y + 1, cursor.z);
            if (world.isReplaceable(cursor.x, cursor.y, cursor.z) && engine.isBlockInTag("#minecraft:dirt", above)) {
                world.setBlock(cursor.x, cursor.y, cursor.z, "minecraft:hanging_roots");
                break;
            }
            if (engine.isBlockInTag("#minecraft:dirt", current) || ruReplaceable(engine, world, cursor)
                || isAir(current)) {
                placeRuShrubLog(engine, config, world, random, cursor);
            } else break;
            cursor = cursor.offset(0, -1, 0);
        }
    }

    private void placeRuShrubLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        String current = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        if ("regions_unexplored:peat_grass_block".equals(current)) {
            world.setBlock(position.x, position.y, position.z, "regions_unexplored:peat_dirt");
        } else if ("regions_unexplored:silt_grass_block".equals(current)) {
            world.setBlock(position.x, position.y, position.z, "regions_unexplored:silt_dirt");
        } else if ("regions_unexplored:alpha_grass_block".equals(current) || "minecraft:grass_block".equals(current)) {
            world.setBlock(position.x, position.y, position.z, "minecraft:dirt");
        } else if (ruReplaceable(engine, world, position)) {
            world.setBlock(
                position.x,
                position.y,
                position.z,
                stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position, world));
        }
        Pos below = position.offset(0, -1, 0);
        String belowBlock = ModernDecorationEngine.blockName(world.blockState(below.x, below.y, below.z));
        if ("regions_unexplored:peat_grass_block".equals(belowBlock)) {
            world.setBlock(below.x, below.y, below.z, "regions_unexplored:peat_dirt");
        } else if ("regions_unexplored:silt_grass_block".equals(belowBlock)) {
            world.setBlock(below.x, below.y, below.z, "regions_unexplored:silt_dirt");
        } else if ("regions_unexplored:alpha_grass_block".equals(belowBlock)
            || "minecraft:grass_block".equals(belowBlock)) {
                world.setBlock(below.x, below.y, below.z, "minecraft:dirt");
            }
    }

    private void placeRuShrubLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        if (world.isReplaceable(position.x, position.y, position.z)) {
            String state = stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position, world);
            world.setBlock(position.x, position.y, position.z, property(state, "distance", "1"));
        }
    }

    private static boolean ruReplaceable(ModernDecorationEngine engine, World world, Pos position) {
        return engine.isBlockInTag(
            "#regions_unexplored:replaceable_blocks",
            world.blockState(position.x, position.y, position.z));
    }

    /** Shared exact algorithm behind RU's GlisteringIvy and HangingEarlight features. */
    private boolean ruRoofGrowth(World world, ModernRandom random, Pos origin, boolean earlight) {
        if (!isAir(world.blockState(origin.x, origin.y, origin.z))) return false;
        String support = ModernDecorationEngine.blockName(world.blockState(origin.x, origin.y + 1, origin.z));
        String spreadBlock = earlight ? "regions_unexplored:cobalt_obsidian" : "regions_unexplored:glistering_wart";
        if (earlight) {
            if (!"minecraft:blackstone".equals(support) && !spreadBlock.equals(support)) return false;
        } else if (!"minecraft:netherrack".equals(support) && !spreadBlock.equals(support)) {
            return false;
        }

        world.setBlock(origin.x, origin.y, origin.z, spreadBlock);
        int[][] directions = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 }, { 1, 0, 0 } };
        for (int attempt = 0; attempt < 200; attempt++) {
            Pos position = origin.offset(
                random.nextInt(6) - random.nextInt(6),
                random.nextInt(2) - random.nextInt(5),
                random.nextInt(6) - random.nextInt(6));
            if (!isAir(world.blockState(position.x, position.y, position.z))) continue;
            int neighbors = 0;
            for (int[] delta : directions) {
                String block = ModernDecorationEngine
                    .blockName(world.blockState(position.x + delta[0], position.y + delta[1], position.z + delta[2]));
                // HangingEarlightFeature intentionally accepts netherrack rather than blackstone here.
                if ("minecraft:netherrack".equals(block) || spreadBlock.equals(block)) neighbors++;
                if (neighbors > 1) break;
            }
            if (neighbors == 1) world.setBlock(position.x, position.y, position.z, spreadBlock);
        }

        int maximumLength = earlight ? 19 : 25;
        int lowAge = earlight ? 10 : 17;
        String head = earlight ? "regions_unexplored:hanging_earlight" : "regions_unexplored:glistering_ivy";
        String plant = earlight ? "regions_unexplored:hanging_earlight_plant"
            : "regions_unexplored:glistering_ivy_plant";
        for (int attempt = 0; attempt < 100; attempt++) {
            Pos position = origin.offset(
                random.nextInt(8) - random.nextInt(8),
                random.nextInt(2) - random.nextInt(7),
                random.nextInt(8) - random.nextInt(8));
            if (!isAir(world.blockState(position.x, position.y, position.z))) continue;
            String above = ModernDecorationEngine.blockName(world.blockState(position.x, position.y + 1, position.z));
            if (!"minecraft:netherrack".equals(above) && !spreadBlock.equals(above)) continue;
            int length = 1 + random.nextInt(maximumLength);
            if (random.nextInt(6) == 0) length *= 2;
            if (random.nextInt(5) == 0) length = 1;
            placeRuHangingColumn(world, random, position, length, lowAge, maximumLength, head, plant);
        }
        return true;
    }

    private static void placeRuHangingColumn(World world, ModernRandom random, Pos origin, int length, int lowAge,
        int highAge, String head, String plant) {
        Pos cursor = origin;
        for (int offset = 0; offset <= length; offset++) {
            if (isAir(world.blockState(cursor.x, cursor.y, cursor.z))) {
                if (offset == length || !isAir(world.blockState(cursor.x, cursor.y - 1, cursor.z))) {
                    world.setBlock(
                        cursor.x,
                        cursor.y,
                        cursor.z,
                        head + "[age=" + (lowAge + random.nextInt(highAge - lowAge + 1)) + "]");
                    break;
                }
                world.setBlock(cursor.x, cursor.y, cursor.z, plant);
            }
            cursor = cursor.offset(0, -1, 0);
        }
    }

    /** Exact structural port of RU's NetherRockFeature. */
    private boolean ruNetherRock(ModernDecorationEngine engine, World world, ModernRandom random, Pos origin) {
        int[] heights = { random.nextInt(3), random.nextInt(3), random.nextInt(3), random.nextInt(3) };
        for (int y = 0; y <= 5; y++) {
            if (!ruReplaceable(engine, world, origin.offset(0, y, 0))) return false;
        }
        Pos[] columns = { origin, origin.offset(0, 0, -1), origin.offset(1, 0, -1), origin.offset(1, 0, 0) };
        for (int y = 0; y <= 5; y++) {
            for (int column = 0; column < columns.length; column++) {
                Pos position = columns[column].offset(0, y, 0);
                if (y == 0) placeRuNetherRockBottom(engine, world, position);
                if (y <= heights[column]) {
                    placeRuNetherRockBlock(engine, world, position, "minecraft:bone_block[axis=y]");
                    if (y == heights[column] && random.nextInt(3) != 0) {
                        world.setBlock(position.x, position.y, position.z, "regions_unexplored:overgrown_bone_block");
                    }
                }
            }
        }
        return true;
    }

    private void placeRuNetherRockBottom(ModernDecorationEngine engine, World world, Pos origin) {
        Pos cursor = origin;
        boolean convertedNylium = false;
        for (int depth = 0; depth <= 2; depth++) {
            if ("regions_unexplored:glistering_nylium"
                .equals(ModernDecorationEngine.blockName(world.blockState(cursor.x, cursor.y, cursor.z)))) {
                convertedNylium = true;
                world.setBlock(cursor.x, cursor.y, cursor.z, "minecraft:netherrack");
            }
            if (depth == 1 && !isAir(world.blockState(cursor.x, cursor.y - 2, cursor.z))) {
                world.setBlock(cursor.x, cursor.y - 1, cursor.z, "minecraft:netherrack");
            }
            if (ruReplaceable(engine, world, cursor)) {
                world.setBlock(cursor.x, cursor.y, cursor.z, "minecraft:bone_block[axis=y]");
            }
            cursor = cursor.offset(0, -1, 0);
            if (convertedNylium && isAir(world.blockState(cursor.x, cursor.y, cursor.z))) return;
        }
    }

    private void placeRuNetherRockBlock(ModernDecorationEngine engine, World world, Pos position, String state) {
        String current = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        if ("regions_unexplored:peat_grass_block".equals(current)) {
            world.setBlock(position.x, position.y, position.z, "regions_unexplored:peat_dirt");
        } else if ("regions_unexplored:silt_grass_block".equals(current)) {
            world.setBlock(position.x, position.y, position.z, "regions_unexplored:silt_dirt");
        } else if ("regions_unexplored:alpha_grass_block".equals(current) || "minecraft:grass_block".equals(current)) {
            world.setBlock(position.x, position.y, position.z, "minecraft:dirt");
        } else if (ruReplaceable(engine, world, position)) {
            world.setBlock(position.x, position.y, position.z, state);
        }
        Pos below = position.offset(0, -1, 0);
        String belowBlock = ModernDecorationEngine.blockName(world.blockState(below.x, below.y, below.z));
        if ("regions_unexplored:peat_grass_block".equals(belowBlock)) {
            world.setBlock(below.x, below.y, below.z, "regions_unexplored:peat_dirt");
        } else if ("regions_unexplored:silt_grass_block".equals(belowBlock)) {
            world.setBlock(below.x, below.y, below.z, "regions_unexplored:silt_dirt");
        } else if ("regions_unexplored:alpha_grass_block".equals(belowBlock)
            || "minecraft:grass_block".equals(belowBlock)) {
                world.setBlock(below.x, below.y, below.z, "minecraft:dirt");
            }
    }

    /**
     * The pinned RU source compares Block against its registry Supplier (without get()).
     * That condition is always true, so the observable configured feature is a no-op.
     */
    private boolean ruObsidianSpire() {
        return false;
    }

    /** Exact port of RU's BasaltBlobFeature. */
    private boolean ruBasaltBlob(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        if (!canPlaceBasaltAt(engine, world, origin)) return false;
        int height = engine.sampleIntValue(config.get("height"), random);
        boolean commonShape = random.nextFloat() < 0.9F;
        int radius = Math.min(height, commonShape ? 5 : 8);
        int attempts = commonShape ? 50 : 15;
        boolean placed = false;
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = random.nextInt(radius * 2 + 1) - radius;
            random.nextInt(1); // randomBetweenClosed samples the fixed Y axis too.
            int z = random.nextInt(radius * 2 + 1) - radius;
            Pos sampled = origin.offset(x, 0, z);
            int remaining = height - manhattan(sampled, origin);
            if (remaining >= 0) {
                placed |= placeBasaltColumn(
                    engine,
                    world,
                    sampled,
                    remaining,
                    engine.sampleIntValue(config.get("reach"), random));
            }
        }
        return placed;
    }

    private boolean placeBasaltColumn(ModernDecorationEngine engine, World world, Pos center, int height, int reach) {
        boolean placed = false;
        // BlockPos.betweenClosed advances X, then Y, then Z; Y is fixed here.
        for (int z = center.z - reach; z <= center.z + reach; z++) {
            for (int x = center.x - reach; x <= center.x + reach; x++) {
                Pos sampled = new Pos(x, center.y, z);
                int distance = manhattan(sampled, center);
                Pos surface = isBasaltAirOrOcean(engine, world, sampled)
                    ? findBasaltSurface(engine, world, sampled, distance)
                    : findBasaltAir(world, sampled, distance);
                if (surface == null) continue;
                int remaining = height - distance / 2;
                Pos cursor = surface;
                while (remaining >= 0) {
                    if (isBasaltAirOrOcean(engine, world, cursor)) {
                        world.setBlock(cursor.x, cursor.y, cursor.z, "minecraft:basalt[axis=y]");
                        cursor = cursor.offset(0, 1, 0);
                        placed = true;
                    } else {
                        if (!"minecraft:basalt"
                            .equals(ModernDecorationEngine.blockName(world.blockState(cursor.x, cursor.y, cursor.z))))
                            break;
                        cursor = cursor.offset(0, 1, 0);
                    }
                    remaining--;
                }
            }
        }
        return placed;
    }

    private Pos findBasaltSurface(ModernDecorationEngine engine, World world, Pos supplied, int remaining) {
        Pos cursor = supplied;
        while (cursor.y > world.minBuildHeight() + 1 && remaining > 0) {
            remaining--;
            if (canPlaceBasaltAt(engine, world, cursor)) return cursor;
            cursor = cursor.offset(0, -1, 0);
        }
        return null;
    }

    private Pos findBasaltAir(World world, Pos supplied, int remaining) {
        Pos cursor = supplied;
        while (cursor.y < world.maxBuildHeight() && remaining > 0) {
            remaining--;
            String current = ModernDecorationEngine.blockName(world.blockState(cursor.x, cursor.y, cursor.z));
            if (cannotPlaceBasaltOn(current)) return null;
            if (isAir(current)) return cursor;
            cursor = cursor.offset(0, 1, 0);
        }
        return null;
    }

    private boolean canPlaceBasaltAt(ModernDecorationEngine engine, World world, Pos position) {
        if (!isBasaltAirOrOcean(engine, world, position)) return false;
        String below = ModernDecorationEngine.blockName(world.blockState(position.x, position.y - 1, position.z));
        return !isAir(below) && !cannotPlaceBasaltOn(below);
    }

    private boolean isBasaltAirOrOcean(ModernDecorationEngine engine, World world, Pos position) {
        String block = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        return isAir(block) || "minecraft:lava".equals(block)
            || "minecraft:water".equals(block) && position.y <= world.seaLevel()
            || ruReplaceable(engine, world, position);
    }

    private static boolean cannotPlaceBasaltOn(String block) {
        return "minecraft:cobblestone".equals(block) || block.startsWith("minecraft:cobblestone_")
            || "minecraft:mossy_cobblestone".equals(block)
            || block.startsWith("minecraft:mossy_cobblestone_")
            || "minecraft:stone_bricks".equals(block)
            || "minecraft:mossy_stone_bricks".equals(block)
            || "minecraft:cracked_stone_bricks".equals(block)
            || "minecraft:stone_brick_slab".equals(block)
            || "minecraft:mossy_stone_brick_slab".equals(block)
            || "minecraft:red_nether_bricks".equals(block)
            || "minecraft:red_nether_brick_slab".equals(block)
            || "minecraft:red_nether_brick_stairs".equals(block)
            || "minecraft:coal_block".equals(block)
            || "minecraft:netherrack".equals(block)
            || "minecraft:soul_sand".equals(block)
            || "minecraft:soul_soil".equals(block)
            || "minecraft:polished_andesite".equals(block)
            || "minecraft:polished_diorite".equals(block)
            || "minecraft:polished_granite".equals(block)
            || block.startsWith("minecraft:polished_andesite_")
            || block.startsWith("minecraft:polished_diorite_")
            || block.startsWith("minecraft:polished_granite_")
            || "minecraft:iron_bars".equals(block)
            || "minecraft:gravel".equals(block)
            || "minecraft:lava".equals(block)
            || "minecraft:bedrock".equals(block)
            || "minecraft:magma_block".equals(block)
            || "minecraft:nether_bricks".equals(block)
            || "minecraft:nether_brick_fence".equals(block)
            || "minecraft:nether_brick_stairs".equals(block)
            || "minecraft:nether_wart".equals(block)
            || "minecraft:chest".equals(block)
            || "regions_unexplored:ash_vent".equals(block)
            || "regions_unexplored:dead_leaves".equals(block);
    }

    private static int manhattan(Pos left, Pos right) {
        return Math.abs(left.x - right.x) + Math.abs(left.y - right.y) + Math.abs(left.z - right.z);
    }

    private boolean disk(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int radius = engine.sampleIntValue(config.get("radius"), random);
        int halfHeight = config.get("half_height")
            .getAsInt();
        boolean placed = false;
        // BlockPos.betweenClosed advances X first, then Z (Y is fixed here).
        for (int z = origin.z - radius; z <= origin.z + radius; z++) {
            for (int x = origin.x - radius; x <= origin.x + radius; x++) {
                int dx = x - origin.x;
                int dz = z - origin.z;
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int y = origin.y + halfHeight; y >= origin.y - halfHeight; y--) {
                    Pos position = new Pos(x, y, z);
                    if (!engine.testPredicate(config.getAsJsonObject("target"), world, position)) continue;
                    world.setBlock(
                        x,
                        y,
                        z,
                        stateProvider(engine, config.getAsJsonObject("state_provider"), random, position, world));
                    placed = true;
                }
            }
        }
        return placed;
    }

    private boolean ore(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int size = config.get("size")
            .getAsInt();
        float angle = random.nextFloat() * (float) Math.PI;
        float length = (float) size / 8.0F;
        int padding = (int) Math.ceil(((float) size / 16.0F * 2.0F + 1.0F) / 2.0F);
        double startX = origin.x + Math.sin((double) angle) * length;
        double endX = origin.x - Math.sin((double) angle) * length;
        double startZ = origin.z + Math.cos((double) angle) * length;
        double endZ = origin.z - Math.cos((double) angle) * length;
        double startY = origin.y + random.nextInt(3) - 2;
        double endY = origin.y + random.nextInt(3) - 2;
        int minimumX = origin.x - (int) Math.ceil(length) - padding;
        int minimumY = origin.y - 2 - padding;
        int minimumZ = origin.z - (int) Math.ceil(length) - padding;
        int width = 2 * ((int) Math.ceil(length) + padding);
        int height = 2 * (2 + padding);
        boolean mayIntersect = false;
        for (int x = minimumX; x <= minimumX + width && !mayIntersect; x++) {
            for (int z = minimumZ; z <= minimumZ + width; z++) {
                if (minimumY <= world.height("OCEAN_FLOOR_WG", x, z)) {
                    mayIntersect = true;
                    break;
                }
            }
        }
        if (!mayIntersect) return false;

        double[] spheres = new double[size * 4];
        for (int index = 0; index < size; index++) {
            float progress = (float) index / (float) size;
            double centerX = lerp(progress, startX, endX);
            double centerY = lerp(progress, startY, endY);
            double centerZ = lerp(progress, startZ, endZ);
            double randomRadius = random.nextDouble() * size / 16.0D;
            double radius = ((double) (mthSin((float) Math.PI * progress) + 1.0F) * randomRadius + 1.0D) / 2.0D;
            int offset = index * 4;
            spheres[offset] = centerX;
            spheres[offset + 1] = centerY;
            spheres[offset + 2] = centerZ;
            spheres[offset + 3] = radius;
        }
        for (int left = 0; left < size - 1; left++) {
            if (spheres[left * 4 + 3] <= 0.0D) continue;
            for (int right = left + 1; right < size; right++) {
                if (spheres[right * 4 + 3] <= 0.0D) continue;
                double dx = spheres[left * 4] - spheres[right * 4];
                double dy = spheres[left * 4 + 1] - spheres[right * 4 + 1];
                double dz = spheres[left * 4 + 2] - spheres[right * 4 + 2];
                double dr = spheres[left * 4 + 3] - spheres[right * 4 + 3];
                if (dr * dr > dx * dx + dy * dy + dz * dz) {
                    if (dr > 0.0D) spheres[right * 4 + 3] = -1.0D;
                    else spheres[left * 4 + 3] = -1.0D;
                }
            }
        }

        java.util.BitSet visited = new java.util.BitSet(width * height * width);
        int placed = 0;
        for (int index = 0; index < size; index++) {
            double radius = spheres[index * 4 + 3];
            if (radius < 0.0D) continue;
            double centerX = spheres[index * 4];
            double centerY = spheres[index * 4 + 1];
            double centerZ = spheres[index * 4 + 2];
            int fromX = Math.max(floor(centerX - radius), minimumX);
            int fromY = Math.max(floor(centerY - radius), minimumY);
            int fromZ = Math.max(floor(centerZ - radius), minimumZ);
            int toX = Math.max(floor(centerX + radius), fromX);
            int toY = Math.max(floor(centerY + radius), fromY);
            int toZ = Math.max(floor(centerZ + radius), fromZ);
            for (int x = fromX; x <= toX; x++) {
                double normalizedX = (x + 0.5D - centerX) / radius;
                if (normalizedX * normalizedX >= 1.0D) continue;
                for (int y = fromY; y <= toY; y++) {
                    double normalizedY = (y + 0.5D - centerY) / radius;
                    if (normalizedX * normalizedX + normalizedY * normalizedY >= 1.0D) continue;
                    for (int z = fromZ; z <= toZ; z++) {
                        double normalizedZ = (z + 0.5D - centerZ) / radius;
                        if (normalizedX * normalizedX + normalizedY * normalizedY + normalizedZ * normalizedZ >= 1.0D
                            || y < world.minBuildHeight()
                            || y >= world.maxBuildHeight()) continue;
                        int bit = x - minimumX + (y - minimumY) * width + (z - minimumZ) * width * height;
                        if (visited.get(bit)) continue;
                        visited.set(bit);
                        Pos position = new Pos(x, y, z);
                        String current = world.blockState(x, y, z);
                        for (JsonElement targetElement : config.getAsJsonArray("targets")) {
                            JsonObject target = targetElement.getAsJsonObject();
                            JsonObject test = target.getAsJsonObject("target");
                            boolean accepted = false;
                            if ("minecraft:tag_match".equals(
                                test.get("predicate_type")
                                    .getAsString())
                                && engine.isBlockInTag(
                                    "#" + test.get("tag")
                                        .getAsString(),
                                    current)) {
                                float discard = config.get("discard_chance_on_air_exposure")
                                    .getAsFloat();
                                boolean skipAirCheck = discard <= 0.0F
                                    || discard < 1.0F && random.nextFloat() >= discard;
                                accepted = skipAirCheck || !adjacentToAir(world, position);
                            }
                            world.recordPlacementCandidate(
                                position,
                                -9,
                                "ore_target=" + current + ";accepted=" + accepted);
                            if (!accepted) continue;
                            world.setBlock(x, y, z, ModernDecorationEngine.state(target.getAsJsonObject("state")));
                            placed++;
                            break;
                        }
                    }
                }
            }
        }
        return placed > 0;
    }

    private static boolean adjacentToAir(World world, Pos position) {
        int[][] neighbors = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 }, { 1, 0, 0 } };
        for (int[] delta : neighbors) {
            if (isAir(world.blockState(position.x + delta[0], position.y + delta[1], position.z + delta[2])))
                return true;
        }
        return false;
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static double lerp(float progress, double start, double end) {
        return start + progress * (end - start);
    }

    private static float mthSin(float value) {
        int index = (int) (value * 10430.378F) & 65535;
        return (float) Math.sin(index * Math.PI * 2.0D / 65536.0D);
    }

    private boolean spring(ModernDecorationEngine engine, JsonObject config, World world, Pos origin) {
        JsonElement valid = config.get("valid_blocks");
        if (!matchesBlocks(engine, valid, world.blockState(origin.x, origin.y + 1, origin.z))) {
            return false;
        }
        if (config.get("requires_block_below")
            .getAsBoolean() && !matchesBlocks(engine, valid, world.blockState(origin.x, origin.y - 1, origin.z))) {
            return false;
        }
        String current = world.blockState(origin.x, origin.y, origin.z);
        if (!isAir(current) && !matchesBlocks(engine, valid, current)) return false;
        int rocks = 0;
        int holes = 0;
        int[][] neighbors = { { -1, 0, 0 }, { 1, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 0, -1, 0 } };
        for (int[] delta : neighbors) {
            String state = world.blockState(origin.x + delta[0], origin.y + delta[1], origin.z + delta[2]);
            if (matchesBlocks(engine, valid, state)) rocks++;
            if (isAir(state)) holes++;
        }
        if (rocks != config.get("rock_count")
            .getAsInt() || holes
                != config.get("hole_count")
                    .getAsInt())
            return false;
        String fluid = ModernDecorationEngine.state(config.getAsJsonObject("state"));
        world.setBlock(origin.x, origin.y, origin.z, springBlockState(fluid));
        return true;
    }

    /**
     * Converts SpringConfiguration's FluidState to the BlockState written by SpringFeature.
     *
     * <p>
     * The registry name selects the source fluid ({@code minecraft:water} or
     * {@code minecraft:lava}). {@code falling=true} is a property of that source FluidState; it
     * does not turn it into the flowing-fluid registry entry. Modern
     * {@code FluidState#createLegacyBlock} therefore writes liquid level zero and separately
     * schedules a fluid tick. Treating the falling property as block level eight changed the
     * authored pre-state observed by later decoration features.
     * </p>
     */
    static String springBlockState(String fluid) {
        String block = ModernDecorationEngine.blockName(fluid);
        if ("minecraft:water".equals(block) || "minecraft:lava".equals(block)) {
            return block + "[level=0]";
        }
        return fluid;
    }

    private boolean seagrass(JsonObject config, World world, ModernRandom random, Pos origin) {
        int x = origin.x + random.nextInt(8) - random.nextInt(8);
        int z = origin.z + random.nextInt(8) - random.nextInt(8);
        int y = world.height("OCEAN_FLOOR", x, z);
        if (!"minecraft:water".equals(ModernDecorationEngine.blockName(world.blockState(x, y, z)))) return false;
        boolean tall = random.nextDouble() < config.get("probability")
            .getAsDouble();
        String lower = tall ? "minecraft:tall_seagrass[half=lower]" : "minecraft:seagrass";
        if (!world.canSurvive(lower, x, y, z)) return false;
        if (tall) {
            if ("minecraft:water".equals(ModernDecorationEngine.blockName(world.blockState(x, y + 1, z)))) {
                world.setBlock(x, y, z, lower);
                world.setBlock(x, y + 1, z, "minecraft:tall_seagrass[half=upper]");
            }
        } else world.setBlock(x, y, z, lower);
        return true;
    }

    private static boolean matchesBlocks(ModernDecorationEngine engine, JsonElement selection, String state) {
        if (selection.isJsonPrimitive()) {
            String value = selection.getAsString();
            return value.startsWith("#") ? engine.isBlockInTag(value, state)
                : value.equals(ModernDecorationEngine.blockName(state));
        }
        for (JsonElement value : selection.getAsJsonArray()) {
            if (value.getAsString()
                .equals(ModernDecorationEngine.blockName(state))) return true;
        }
        return false;
    }

    private boolean bamboo(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        if (!isAir(world.blockState(origin.x, origin.y, origin.z))) return false;
        String trunk = "minecraft:bamboo[age=1,leaves=none,stage=0]";
        if (world.canSurvive(trunk, origin.x, origin.y, origin.z)) {
            int height = random.nextInt(12) + 5;
            if (random.nextFloat() < config.get("probability")
                .getAsFloat()) {
                int radius = random.nextInt(4) + 1;
                for (int x = origin.x - radius; x <= origin.x + radius; x++) {
                    for (int z = origin.z - radius; z <= origin.z + radius; z++) {
                        int dx = x - origin.x;
                        int dz = z - origin.z;
                        if (dx * dx + dz * dz > radius * radius) continue;
                        int y = world.height("WORLD_SURFACE", x, z) - 1;
                        if (engine.isBlockInTag("#minecraft:dirt", world.blockState(x, y, z))) {
                            world.setBlock(x, y, z, "minecraft:podzol[snowy=false]");
                        }
                    }
                }
            }
            Pos cursor = origin;
            for (int step = 0; step < height && isAir(world.blockState(cursor.x, cursor.y, cursor.z)); step++) {
                world.setBlock(cursor.x, cursor.y, cursor.z, trunk);
                cursor = cursor.offset(0, 1, 0);
            }
            if (cursor.y - origin.y >= 3) {
                world.setBlock(cursor.x, cursor.y, cursor.z, "minecraft:bamboo[age=1,leaves=large,stage=1]");
                cursor = cursor.offset(0, -1, 0);
                world.setBlock(cursor.x, cursor.y, cursor.z, "minecraft:bamboo[age=1,leaves=large,stage=0]");
                cursor = cursor.offset(0, -1, 0);
                world.setBlock(cursor.x, cursor.y, cursor.z, "minecraft:bamboo[age=1,leaves=small,stage=0]");
            }
        }
        // Vanilla reports success for any empty origin, even when bamboo cannot survive.
        return true;
    }

    private boolean blockBlob(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos suppliedOrigin) {
        Pos origin = suppliedOrigin;
        while (origin.y > world.minBuildHeight() + 3) {
            String below = world.blockState(origin.x, origin.y - 1, origin.z);
            if (!isAir(below) && (engine.isBlockInTag("#minecraft:dirt", below)
                || engine.isBlockInTag("#minecraft:base_stone_overworld", below))) break;
            origin = origin.offset(0, -1, 0);
        }
        if (origin.y <= world.minBuildHeight() + 3) return false;
        String state = ModernDecorationEngine.state(config.getAsJsonObject("state"));
        for (int blob = 0; blob < 3; blob++) {
            int radiusX = random.nextInt(2);
            int radiusY = random.nextInt(2);
            int radiusZ = random.nextInt(2);
            float radius = (radiusX + radiusY + radiusZ) * 0.333F + 0.5F;
            for (int z = origin.z - radiusZ; z <= origin.z + radiusZ; z++) {
                for (int y = origin.y - radiusY; y <= origin.y + radiusY; y++) {
                    for (int x = origin.x - radiusX; x <= origin.x + radiusX; x++) {
                        double dx = x - origin.x;
                        double dy = y - origin.y;
                        double dz = z - origin.z;
                        if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                            world.setBlock(x, y, z, state);
                        }
                    }
                }
            }
            origin = origin.offset(-1 + random.nextInt(2), -random.nextInt(2), -1 + random.nextInt(2));
        }
        return true;
    }

    private boolean blueIce(World world, ModernRandom random, Pos origin) {
        if (origin.y >= world.seaLevel()) return false;
        if (!isBlock(world, origin, "minecraft:water") && !isBlock(world, origin.offset(0, -1, 0), "minecraft:water"))
            return false;
        int[][] directions = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 }, { 1, 0, 0 } };
        boolean packedNeighbor = false;
        for (int index = 1; index < directions.length; index++) {
            int[] delta = directions[index];
            if (isBlock(world, origin.offset(delta[0], delta[1], delta[2]), "minecraft:packed_ice")) {
                packedNeighbor = true;
                break;
            }
        }
        if (!packedNeighbor) return false;
        world.setBlock(origin.x, origin.y, origin.z, "minecraft:blue_ice");
        for (int attempt = 0; attempt < 200; attempt++) {
            int y = random.nextInt(5) - random.nextInt(6);
            int radius = 3;
            if (y < 2) radius += y / 2;
            if (radius < 1) continue;
            Pos position = origin.offset(
                random.nextInt(radius) - random.nextInt(radius),
                y,
                random.nextInt(radius) - random.nextInt(radius));
            String block = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
            if (!isAir(block) && !"minecraft:water".equals(block)
                && !"minecraft:packed_ice".equals(block)
                && !"minecraft:ice".equals(block)) continue;
            for (int[] delta : directions) {
                if (isBlock(world, position.offset(delta[0], delta[1], delta[2]), "minecraft:blue_ice")) {
                    world.setBlock(position.x, position.y, position.z, "minecraft:blue_ice");
                    break;
                }
            }
        }
        return true;
    }

    private boolean kelp(World world, ModernRandom random, Pos origin) {
        int y = world.height("OCEAN_FLOOR", origin.x, origin.z);
        Pos cursor = new Pos(origin.x, y, origin.z);
        if (!isBlock(world, cursor, "minecraft:water")) return false;
        int height = 1 + random.nextInt(10);
        int placed = 0;
        for (int step = 0; step <= height; step++) {
            if (isBlock(world, cursor, "minecraft:water") && isBlock(world, cursor.offset(0, 1, 0), "minecraft:water")
                && world.canSurvive("minecraft:kelp_plant", cursor.x, cursor.y, cursor.z)) {
                if (step == height) {
                    world
                        .setBlock(cursor.x, cursor.y, cursor.z, "minecraft:kelp[age=" + (random.nextInt(4) + 20) + "]");
                    placed++;
                } else world.setBlock(cursor.x, cursor.y, cursor.z, "minecraft:kelp_plant");
            } else if (step > 0) {
                Pos below = cursor.offset(0, -1, 0);
                if (world.canSurvive("minecraft:kelp", below.x, below.y, below.z)
                    && !isBlock(world, below.offset(0, -1, 0), "minecraft:kelp")) {
                    world.setBlock(below.x, below.y, below.z, "minecraft:kelp[age=" + (random.nextInt(4) + 20) + "]");
                    placed++;
                }
                break;
            }
            cursor = cursor.offset(0, 1, 0);
        }
        return placed > 0;
    }

    private boolean seaPickle(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int count = engine.sampleIntValue(config.get("count"), random);
        int placed = 0;
        for (int attempt = 0; attempt < count; attempt++) {
            int x = origin.x + random.nextInt(8) - random.nextInt(8);
            int z = origin.z + random.nextInt(8) - random.nextInt(8);
            int y = world.height("OCEAN_FLOOR", x, z);
            String state = "minecraft:sea_pickle[pickles=" + (random.nextInt(4) + 1) + ",waterlogged=true]";
            if (isBlock(world, new Pos(x, y, z), "minecraft:water") && world.canSurvive(state, x, y, z)) {
                world.setBlock(x, y, z, state);
                placed++;
            }
        }
        return placed > 0;
    }

    private boolean vines(World world, Pos origin) {
        if (!isAir(world.blockState(origin.x, origin.y, origin.z))) return false;
        String[] names = { "down", "up", "north", "south", "west", "east" };
        int[][] directions = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 }, { 1, 0, 0 } };
        for (int index = 1; index < directions.length; index++) {
            int[] delta = directions[index];
            Pos neighbor = origin.offset(delta[0], delta[1], delta[2]);
            if (world.hasSturdyFace(neighbor.x, neighbor.y, neighbor.z, opposite(names[index]))) {
                String state = "minecraft:vine[east=false,north=false,south=false,up=false,west=false]";
                world.setBlock(origin.x, origin.y, origin.z, property(state, names[index], "true"));
                return true;
            }
        }
        return false;
    }

    /** Exact port of RU's {@code RURockFeature}. */
    private boolean ruRock(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos suppliedOrigin) {
        Pos origin = suppliedOrigin;
        // RU samples blobCount in the loop condition, so it intentionally consumes a
        // fresh random value before every iteration (including the terminating check).
        for (int blob = 0; blob < engine.sampleIntValue(config.get("blob_count"), random); blob++) {
            int radiusX = Math.min(random.nextInt(3), 1);
            int radiusY = Math.min(random.nextInt(3), 1);
            int radiusZ = Math.min(random.nextInt(3), 1);
            float radius = (radiusX + radiusY + radiusZ) * 0.333F + 0.5F;
            // BlockPos.betweenClosed advances X first, then Y, then Z.
            for (int z = origin.z - radiusZ; z <= origin.z + radiusZ; z++) {
                for (int y = origin.y - radiusY; y <= origin.y + radiusY; y++) {
                    for (int x = origin.x - radiusX; x <= origin.x + radiusX; x++) {
                        double dx = x - origin.x;
                        double dy = y - origin.y;
                        double dz = z - origin.z;
                        if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                            world.setBlock(
                                x,
                                y,
                                z,
                                stateProvider(engine, config.getAsJsonObject("state_provider"), random, origin, world));
                        }
                    }
                }
            }
            origin = origin.offset(
                engine.sampleIntValue(config.get("blob_offset_xz"), random),
                ruRockYOffset(random),
                engine.sampleIntValue(config.get("blob_offset_xz"), random));
        }
        return true;
    }

    /** WeightedListInt[-1*3, 0*3, 1*1], preserving its single nextInt(7) draw. */
    private static int ruRockYOffset(ModernRandom random) {
        int selected = random.nextInt(7);
        if (selected < 3) return -1;
        return selected < 6 ? 0 : 1;
    }

    /** Exact port of RU's {@code RUFallenTreeFeature}. */
    private boolean ruFallenTree(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        Pos stump = placeFallenLogBlock(engine, config.getAsJsonObject("trunk_provider"), world, random, origin, null);
        decorateFallenLogs(
            engine,
            config.getAsJsonArray("stump_decorators"),
            world,
            random,
            Collections.singleton(stump));

        // Direction.Plane.HORIZONTAL is NORTH, EAST, SOUTH, WEST.
        int[][] horizontal = { { 0, 0, -1 }, { 1, 0, 0 }, { 0, 0, 1 }, { -1, 0, 0 } };
        int[] direction = horizontal[random.nextInt(horizontal.length)];
        int logLength = engine.sampleIntValue(config.get("log_length"), random) - 2;
        int distance = 2 + random.nextInt(2);
        Pos start = origin.offset(direction[0] * distance, 1, direction[2] * distance);
        for (int attempt = 0; attempt < 6; attempt++) {
            if (validTreePosition(engine, world, start) && world.hasSturdyFace(start.x, start.y - 1, start.z, "up"))
                break;
            start = start.offset(0, -1, 0);
        }

        Pos cursor = start;
        int unsupported = 0;
        for (int index = 0; index < logLength; index++) {
            if (!validTreePosition(engine, world, cursor)) return true;
            if (!world.hasSturdyFace(cursor.x, cursor.y - 1, cursor.z, "up")) {
                if (++unsupported > 2) return true;
            } else unsupported = 0;
            cursor = cursor.offset(direction[0], 0, direction[2]);
        }

        Set<Pos> logs = new HashSet<Pos>();
        cursor = start;
        String axis = direction[0] == 0 ? "z" : "x";
        for (int index = 0; index < logLength; index++) {
            logs.add(
                placeFallenLogBlock(engine, config.getAsJsonObject("trunk_provider"), world, random, cursor, axis));
            cursor = cursor.offset(direction[0], 0, direction[2]);
        }
        decorateFallenLogs(engine, config.getAsJsonArray("log_decorators"), world, random, logs);
        return true;
    }

    private Pos placeFallenLogBlock(ModernDecorationEngine engine, JsonObject provider, World world,
        ModernRandom random, Pos position, String axis) {
        String state = stateProvider(engine, provider, random, position, world);
        if (axis != null && state.contains("axis=")) state = property(state, "axis", axis);
        world.setBlock(position.x, position.y, position.z, state);
        return position;
    }

    private static boolean validTreePosition(ModernDecorationEngine engine, World world, Pos position) {
        String state = world.blockState(position.x, position.y, position.z);
        return isAir(state) || engine.isBlockInTag("#minecraft:replaceable_by_trees", state);
    }

    private void decorateFallenLogs(ModernDecorationEngine engine, JsonArray decorators, World world,
        ModernRandom random, Set<Pos> suppliedLogs) {
        if (decorators == null || decorators.size() == 0) return;
        List<Pos> logs = new ArrayList<Pos>(suppliedLogs);
        // TreeDecorator.Context performs this stable Y sort before exposing logs().
        Collections.sort(logs, new Comparator<Pos>() {

            @Override
            public int compare(Pos left, Pos right) {
                return Integer.compare(left.y, right.y);
            }
        });
        for (JsonElement decoratorElement : decorators) {
            JsonObject decorator = decoratorElement.getAsJsonObject();
            String type = decorator.get("type")
                .getAsString();
            if (!"regions_unexplored:attached_to_logs".equals(type)) {
                throw new IllegalArgumentException("Unsupported fallen-tree decorator " + type);
            }
            List<Pos> shuffled = new ArrayList<Pos>(logs);
            shuffle(shuffled, random);
            JsonArray directions = decorator.getAsJsonArray("directions");
            boolean all = decorator.has("check_all_directions") && decorator.get("check_all_directions")
                .getAsBoolean();
            float probability = decorator.get("probability")
                .getAsFloat();
            for (Pos log : shuffled) {
                if (all) {
                    for (JsonElement direction : directions) {
                        placeAttachedToLog(engine, decorator, world, random, log, direction.getAsString(), probability);
                    }
                } else {
                    String direction = directions.get(random.nextInt(directions.size()))
                        .getAsString();
                    placeAttachedToLog(engine, decorator, world, random, log, direction, probability);
                }
            }
        }
    }

    private void placeAttachedToLog(ModernDecorationEngine engine, JsonObject decorator, World world,
        ModernRandom random, Pos log, String direction, float probability) {
        int[] delta = direction(direction);
        Pos position = log.offset(delta[0], delta[1], delta[2]);
        if (random.nextFloat() <= probability && isAir(world.blockState(position.x, position.y, position.z))
            && !isAir(world.blockState(position.x, position.y - 1, position.z))) {
            world.setBlock(
                position.x,
                position.y,
                position.z,
                stateProvider(engine, decorator.getAsJsonObject("block_provider"), random, position, world));
        }
    }

    private static <T> void shuffle(List<T> values, ModernRandom random) {
        for (int size = values.size(); size > 1; size--) {
            int selected = random.nextInt(size);
            T previous = values.set(size - 1, values.get(selected));
            values.set(selected, previous);
        }
    }

    /** Exact port of the two vanilla AbstractHugeMushroomFeature implementations. */
    private boolean hugeMushroom(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin, boolean red) {
        int height = random.nextInt(3) + 4;
        if (random.nextInt(12) == 0) height *= 2;
        if (origin.y < world.minBuildHeight() + 1 || origin.y + height + 1 >= world.maxBuildHeight()) return false;
        String ground = world.blockState(origin.x, origin.y - 1, origin.z);
        if (!engine.isBlockInTag("#minecraft:dirt", ground)
            && !engine.isBlockInTag("#minecraft:mushroom_grow_block", ground)) return false;

        int foliageRadius = config.get("foliage_radius")
            .getAsInt();
        for (int offsetY = 0; offsetY <= height; offsetY++) {
            int radius = red ? 0 : (offsetY <= 3 ? 0 : foliageRadius);
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    String current = world.blockState(origin.x + x, origin.y + offsetY, origin.z + z);
                    if (!isAir(current) && !engine.isBlockInTag("#minecraft:leaves", current)) {
                        return false;
                    }
                }
            }
        }
        if (red) makeRedMushroomCap(engine, config, world, random, origin, height);
        else makeBrownMushroomCap(engine, config, world, random, origin, height);
        String stemProvider = "stem_provider";
        for (int offsetY = 0; offsetY < height; offsetY++) {
            Pos position = origin.offset(0, offsetY, 0);
            if (!world.isSolid(position.x, position.y, position.z)) {
                world.setBlock(
                    position.x,
                    position.y,
                    position.z,
                    stateProvider(engine, config.getAsJsonObject(stemProvider), random, origin, world));
            }
        }
        return true;
    }

    private void makeBrownMushroomCap(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, Pos origin, int height) {
        int radius = config.get("foliage_radius")
            .getAsInt();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                boolean minX = x == -radius;
                boolean maxX = x == radius;
                boolean minZ = z == -radius;
                boolean maxZ = z == radius;
                boolean edgeX = minX || maxX;
                boolean edgeZ = minZ || maxZ;
                if (edgeX && edgeZ) continue;
                Pos position = origin.offset(x, height, z);
                if (world.isSolid(position.x, position.y, position.z)) continue;
                String state = stateProvider(engine, config.getAsJsonObject("cap_provider"), random, origin, world);
                state = property(state, "west", Boolean.toString(minX || edgeZ && x == 1 - radius));
                state = property(state, "east", Boolean.toString(maxX || edgeZ && x == radius - 1));
                state = property(state, "north", Boolean.toString(minZ || edgeX && z == 1 - radius));
                state = property(state, "south", Boolean.toString(maxZ || edgeX && z == radius - 1));
                world.setBlock(position.x, position.y, position.z, state);
            }
        }
    }

    private void makeRedMushroomCap(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin, int height) {
        int foliageRadius = config.get("foliage_radius")
            .getAsInt();
        int inner = foliageRadius - 2;
        for (int offsetY = height - 3; offsetY <= height; offsetY++) {
            int radius = offsetY < height ? foliageRadius : foliageRadius - 1;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    boolean edgeX = x == -radius || x == radius;
                    boolean edgeZ = z == -radius || z == radius;
                    if (offsetY < height && edgeX == edgeZ) continue;
                    Pos position = origin.offset(x, offsetY, z);
                    if (world.isSolid(position.x, position.y, position.z)) continue;
                    String state = stateProvider(engine, config.getAsJsonObject("cap_provider"), random, origin, world);
                    state = property(state, "up", Boolean.toString(offsetY >= height - 1));
                    state = property(state, "west", Boolean.toString(x < -inner));
                    state = property(state, "east", Boolean.toString(x > inner));
                    state = property(state, "north", Boolean.toString(z < -inner));
                    state = property(state, "south", Boolean.toString(z > inner));
                    world.setBlock(position.x, position.y, position.z, state);
                }
            }
        }
    }

    /** Exact port of vanilla VegetationPatchFeature and its waterlogged subclass. */
    private boolean vegetationPatch(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin, boolean waterlogged) {
        int radiusX = engine.sampleIntValue(config.get("xz_radius"), random) + 1;
        int radiusZ = engine.sampleIntValue(config.get("xz_radius"), random) + 1;
        int surfaceDirection = "ceiling".equals(
            config.get("surface")
                .getAsString()) ? 1 : -1;
        Set<Pos> ground = new HashSet<Pos>();
        float edgeChance = config.get("extra_edge_column_chance")
            .getAsFloat();
        int verticalRange = config.get("vertical_range")
            .getAsInt();
        String replaceable = config.get("replaceable")
            .getAsString();
        for (int x = -radiusX; x <= radiusX; x++) {
            boolean edgeX = x == -radiusX || x == radiusX;
            for (int z = -radiusZ; z <= radiusZ; z++) {
                boolean edgeZ = z == -radiusZ || z == radiusZ;
                boolean edge = edgeX || edgeZ;
                boolean corner = edgeX && edgeZ;
                if (corner) continue;
                if (edge && (edgeChance == 0.0F || random.nextFloat() > edgeChance)) continue;
                Pos cursor = origin.offset(x, 0, z);
                int moved = 0;
                while (isAir(world.blockState(cursor.x, cursor.y, cursor.z)) && moved < verticalRange) {
                    cursor = cursor.offset(0, surfaceDirection, 0);
                    moved++;
                }
                moved = 0;
                while (!isAir(world.blockState(cursor.x, cursor.y, cursor.z)) && moved < verticalRange) {
                    cursor = cursor.offset(0, -surfaceDirection, 0);
                    moved++;
                }
                Pos groundPosition = cursor.offset(0, surfaceDirection, 0);
                String cursorState = world.blockState(cursor.x, cursor.y, cursor.z);
                boolean empty = isAir(cursorState);
                world.recordPlacementCandidate(
                    cursor,
                    -8,
                    "vegetation_patch_empty=" + cursorState + ";accepted=" + empty);
                if (!empty) continue;
                boolean sturdy = world.hasSturdyFace(
                    groundPosition.x,
                    groundPosition.y,
                    groundPosition.z,
                    surfaceDirection < 0 ? "up" : "down");
                world.recordPlacementCandidate(
                    groundPosition,
                    -8,
                    "vegetation_patch_sturdy=" + world.blockState(groundPosition.x, groundPosition.y, groundPosition.z)
                        + ";accepted="
                        + sturdy);
                if (!sturdy) continue;
                int depth = engine.sampleIntValue(config.get("depth"), random);
                float extraBottom = config.get("extra_bottom_block_chance")
                    .getAsFloat();
                if (extraBottom > 0.0F && random.nextFloat() < extraBottom) depth++;
                Pos saved = groundPosition;
                if (placeVegetationGround(
                    engine,
                    config,
                    replaceable,
                    world,
                    random,
                    groundPosition,
                    depth,
                    surfaceDirection)) ground.add(saved);
            }
        }

        if (waterlogged) {
            Set<Pos> interior = new HashSet<Pos>();
            int[][] exposedDirections = { { 0, 0, -1 }, { 1, 0, 0 }, { 0, 0, 1 }, { -1, 0, 0 }, { 0, -1, 0 } };
            String[] oppositeFaces = { "south", "west", "north", "east", "up" };
            for (Pos position : ground) {
                boolean exposed = false;
                for (int index = 0; index < exposedDirections.length; index++) {
                    int[] delta = exposedDirections[index];
                    if (!world.hasSturdyFace(
                        position.x + delta[0],
                        position.y + delta[1],
                        position.z + delta[2],
                        oppositeFaces[index])) {
                        exposed = true;
                        break;
                    }
                }
                if (!exposed) interior.add(position);
            }
            for (Pos position : interior) {
                world.setBlock(position.x, position.y, position.z, "minecraft:water[level=0]");
            }
            ground = interior;
        }

        float vegetationChance = config.get("vegetation_chance")
            .getAsFloat();
        for (Pos position : ground) {
            if (vegetationChance <= 0.0F || random.nextFloat() >= vegetationChance) continue;
            Pos vegetationOrigin;
            if (waterlogged) {
                // The subclass supplies below(), then the base feature applies the
                // surface opposite, landing back in the water cell for floor patches.
                vegetationOrigin = position.offset(0, -1 - surfaceDirection, 0);
            } else {
                vegetationOrigin = position.offset(0, -surfaceDirection, 0);
            }
            boolean placed = engine.place(config.get("vegetation_feature"), null, world, random, vegetationOrigin);
            if (waterlogged && placed) {
                String state = world.blockState(position.x, position.y, position.z);
                if (state.contains("waterlogged=false")) {
                    world.setBlock(position.x, position.y, position.z, property(state, "waterlogged", "true"));
                }
            }
        }
        return !ground.isEmpty();
    }

    private boolean placeVegetationGround(ModernDecorationEngine engine, JsonObject config, String replaceable,
        World world, ModernRandom random, Pos suppliedPosition, int depth, int surfaceDirection) {
        Pos position = suppliedPosition;
        for (int offset = 0; offset < depth; offset++) {
            String replacement = stateProvider(engine, config.getAsJsonObject("ground_state"), random, position, world);
            String current = world.blockState(position.x, position.y, position.z);
            if (ModernDecorationEngine.blockName(replacement)
                .equals(ModernDecorationEngine.blockName(current))) continue;
            if (!engine.isBlockInTag(replaceable, current)) return offset != 0;
            world.setBlock(position.x, position.y, position.z, replacement);
            position = position.offset(0, surfaceDirection, 0);
        }
        return true;
    }

    private static String opposite(String direction) {
        if ("up".equals(direction)) return "down";
        if ("down".equals(direction)) return "up";
        if ("north".equals(direction)) return "south";
        if ("south".equals(direction)) return "north";
        if ("west".equals(direction)) return "east";
        return "west";
    }

    private static boolean isBlock(World world, Pos position, String block) {
        return block.equals(ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z)));
    }

    private boolean simpleBlock(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        String state = stateProvider(engine, config.getAsJsonObject("to_place"), random, position);
        world.recordPlacementCandidate(position, -1, "state=" + ModernDecorationEngine.blockName(state));
        boolean survives = world.canSurvive(state, position.x, position.y, position.z);
        world.recordPlacementCandidate(
            position,
            -8,
            "simple_block_survive=" + ModernDecorationEngine.blockName(state)
                + ";target="
                + world.blockState(position.x, position.y, position.z)
                + ";below="
                + world.blockState(position.x, position.y - 1, position.z)
                + ";accepted="
                + survives);
        if (!survives) return false;
        if (isDoublePlant(state)) {
            if (!isAir(world.blockState(position.x, position.y + 1, position.z))) return false;
            world.setBlock(position.x, position.y, position.z, property(state, "half", "lower"));
            world.setBlock(position.x, position.y + 1, position.z, property(state, "half", "upper"));
        } else {
            world.setBlock(position.x, position.y, position.z, state);
        }
        return true;
    }

    private boolean randomPatch(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int successes = 0;
        int xzBound = config.get("xz_spread")
            .getAsInt() + 1;
        int yBound = config.get("y_spread")
            .getAsInt() + 1;
        int tries = config.get("tries")
            .getAsInt();
        for (int attempt = 0; attempt < tries; attempt++) {
            Pos position = origin.offset(
                random.nextInt(xzBound) - random.nextInt(xzBound),
                random.nextInt(yBound) - random.nextInt(yBound),
                random.nextInt(xzBound) - random.nextInt(xzBound));
            if (engine.place(config.get("feature"), null, world, random, position)) successes++;
        }
        return successes > 0;
    }

    private boolean randomSelector(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        for (JsonElement element : config.getAsJsonArray("features")) {
            JsonObject weighted = element.getAsJsonObject();
            if (random.nextFloat() < weighted.get("chance")
                .getAsFloat()) {
                return engine.place(weighted.get("feature"), null, world, random, position);
            }
        }
        return engine.place(config.get("default"), null, world, random, position);
    }

    private boolean composite(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        boolean any = false;
        String mode = config.has("placement_type") ? config.get("placement_type")
            .getAsString() : "never_cancel";
        for (JsonElement feature : config.getAsJsonArray("features")) {
            boolean placed = engine.place(feature, null, world, random, position);
            if (placed) any = true;
            if ("cancel_on_failure".equals(mode) && !placed) break;
            if ("cancel_on_success".equals(mode) && placed) break;
        }
        return any;
    }

    private boolean blockColumn(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int[] direction = direction(
            config.get("direction")
                .getAsString());
        JsonArray layers = config.getAsJsonArray("layers");
        int[] heights = new int[layers.size()];
        int total = 0;
        for (int index = 0; index < layers.size(); index++) {
            heights[index] = engine.sampleIntValue(
                layers.get(index)
                    .getAsJsonObject()
                    .get("height"),
                random);
            total += heights[index];
        }
        if (total == 0) return false;
        int placeable = 0;
        Pos cursor = origin.offset(direction[0], direction[1], direction[2]);
        JsonObject allowed = config.getAsJsonObject("allowed_placement");
        while (placeable < total && engine.testPredicate(allowed, world, cursor)) {
            placeable++;
            cursor = cursor.offset(direction[0], direction[1], direction[2]);
        }
        if (config.has("prioritize_tip") && config.get("prioritize_tip")
            .getAsBoolean() && placeable < total) {
            int removed = total - placeable;
            for (int index = 0; index < heights.length && removed > 0; index++) {
                int take = Math.min(heights[index], removed);
                heights[index] -= take;
                removed -= take;
            }
        } else if (placeable < total) {
            int removed = total - placeable;
            for (int index = heights.length - 1; index >= 0 && removed > 0; index--) {
                int take = Math.min(heights[index], removed);
                heights[index] -= take;
                removed -= take;
            }
        }
        cursor = origin;
        for (int index = 0; index < layers.size(); index++) {
            JsonObject provider = layers.get(index)
                .getAsJsonObject()
                .getAsJsonObject("provider");
            for (int offset = 0; offset < heights[index]; offset++) {
                String state = stateProvider(engine, provider, random, cursor);
                world.setBlock(cursor.x, cursor.y, cursor.z, state);
                cursor = cursor.offset(direction[0], direction[1], direction[2]);
            }
        }
        return true;
    }

    String stateProvider(ModernDecorationEngine engine, JsonObject provider, ModernRandom random, Pos position) {
        return stateProvider(engine, provider, random, position, null);
    }

    private String stateProvider(ModernDecorationEngine engine, JsonObject provider, ModernRandom random, Pos position,
        World world) {
        if (!provider.has("type") && provider.has("fallback") && provider.has("rules")) {
            if (world == null) throw new IllegalArgumentException("Rule-based state provider requires a world context");
            for (JsonElement element : provider.getAsJsonArray("rules")) {
                JsonObject rule = element.getAsJsonObject();
                if (engine.testPredicate(rule.getAsJsonObject("if_true"), world, position)) {
                    return stateProvider(engine, rule.getAsJsonObject("then"), random, position, world);
                }
            }
            return stateProvider(engine, provider.getAsJsonObject("fallback"), random, position, world);
        }
        String type = provider.get("type")
            .getAsString();
        if ("minecraft:simple_state_provider".equals(type)) {
            return ModernDecorationEngine.state(provider.getAsJsonObject("state"));
        }
        if ("minecraft:rotated_block_provider".equals(type)) {
            String state = ModernDecorationEngine.state(provider.getAsJsonObject("state"));
            return property(state, "axis", new String[] { "x", "y", "z" }[random.nextInt(3)]);
        }
        if ("minecraft:weighted_state_provider".equals(type)) {
            JsonElement selected = weightedData(provider.getAsJsonArray("entries"), random);
            return ModernDecorationEngine.state(selected.getAsJsonObject());
        }
        if ("lithostitched:random_block".equals(type)) {
            JsonArray blocks = provider.getAsJsonArray("blocks");
            return blocks.get(random.nextInt(blocks.size()))
                .getAsString();
        }
        if ("minecraft:randomized_int_state_provider".equals(type)) {
            String source = stateProvider(engine, provider.getAsJsonObject("source"), random, position, world);
            int value = engine.sampleIntValue(provider.get("values"), random);
            return property(
                source,
                provider.get("property")
                    .getAsString(),
                Integer.toString(value));
        }
        if ("regions_unexplored:randomized_ground_cover".equals(type)) {
            String state = provider.get("block")
                .getAsString();
            // GroundCoverBlock.AMOUNT is the modern FLOWER_AMOUNT property. The
            // Java field name is deliberately not the serialized state name.
            state = property(state, "flower_amount", Integer.toString(random.nextInt(4) + 1));
            String[] directions = { "north", "east", "south", "west" };
            return property(state, "facing", directions[random.nextInt(4)]);
        }
        if ("lithostitched:weighted".equals(type)) {
            JsonElement selected = weightedData(provider.getAsJsonArray("entries"), random);
            return stateProvider(engine, selected.getAsJsonObject(), random, position, world);
        }
        if ("minecraft:noise_provider".equals(type)) {
            JsonArray states = provider.getAsJsonArray("states");
            double value = providerNoise(provider, "noise").sample(
                position.x * provider.get("scale")
                    .getAsDouble(),
                position.y * provider.get("scale")
                    .getAsDouble(),
                position.z * provider.get("scale")
                    .getAsDouble());
            return stateAtNoise(states, value);
        }
        if ("minecraft:noise_threshold_provider".equals(type)) {
            double scale = provider.get("scale")
                .getAsDouble();
            double value = providerNoise(provider, "noise")
                .sample(position.x * scale, position.y * scale, position.z * scale);
            if (value < provider.get("threshold")
                .getAsDouble()) {
                return randomState(provider.getAsJsonArray("low_states"), random);
            }
            if (random.nextFloat() < provider.get("high_chance")
                .getAsFloat()) {
                return randomState(provider.getAsJsonArray("high_states"), random);
            }
            return ModernDecorationEngine.state(provider.getAsJsonObject("default_state"));
        }
        if ("minecraft:dual_noise_provider".equals(type)) {
            return dualNoiseState(provider, position);
        }
        throw new IllegalArgumentException("Unsupported pinned block-state provider " + type);
    }

    private String dualNoiseState(JsonObject provider, Pos position) {
        double slowScale = provider.get("slow_scale")
            .getAsDouble();
        ModernNormalNoise slow = providerNoise(provider, "slow_noise");
        double slowValue = slow.sample(position.x * slowScale, position.y * slowScale, position.z * slowScale);
        JsonElement encodedVariety = provider.get("variety");
        int min;
        int max;
        if (encodedVariety.isJsonArray()) {
            JsonArray variety = encodedVariety.getAsJsonArray();
            min = variety.get(0)
                .getAsInt();
            max = variety.get(1)
                .getAsInt();
        } else {
            min = encodedVariety.getAsInt();
            max = min;
        }
        int count = (int) clampedMap(slowValue, -1.0D, 1.0D, min, max + 1.0D);
        List<String> choices = new ArrayList<String>(count);
        JsonArray states = provider.getAsJsonArray("states");
        for (int index = 0; index < count; index++) {
            double value = slow.sample(
                (position.x + index * 54545) * slowScale,
                position.y * slowScale,
                (position.z + index * 34234) * slowScale);
            choices.add(stateAtNoise(states, value));
        }
        double scale = provider.get("scale")
            .getAsDouble();
        double value = providerNoise(provider, "noise")
            .sample(position.x * scale, position.y * scale, position.z * scale);
        double normalized = Math.max(0.0D, Math.min(0.9999D, (1.0D + value) / 2.0D));
        return choices.get((int) (normalized * choices.size()));
    }

    private ModernNormalNoise providerNoise(JsonObject provider, String field) {
        String key = provider.get("seed")
            .getAsLong() + ":"
            + provider.get(field)
                .toString();
        ModernNormalNoise noise = providerNoises.get(key);
        if (noise != null) return noise;
        JsonObject parameters = provider.getAsJsonObject(field);
        JsonArray amplitudesJson = parameters.getAsJsonArray("amplitudes");
        double[] amplitudes = new double[amplitudesJson.size()];
        for (int index = 0; index < amplitudes.length; index++) {
            amplitudes[index] = amplitudesJson.get(index)
                .getAsDouble();
        }
        noise = new ModernNormalNoise(
            new ModernLegacyRandom(
                provider.get("seed")
                    .getAsLong()),
            parameters.get("firstOctave")
                .getAsInt(),
            amplitudes);
        providerNoises.put(key, noise);
        return noise;
    }

    private static JsonElement weightedData(JsonArray entries, ModernRandom random) {
        WeightedSelection selected = weightedDataWithRoll(entries, random, false);
        return selected == null ? null : selected.data;
    }

    private static WeightedSelection weightedDataWithRoll(JsonArray entries, ModernRandom random,
        boolean allowBareFallback) {
        int total = 0;
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            total += entry.has("data") ? entry.get("weight")
                .getAsInt() : 1;
        }
        int roll = random.nextInt(total);
        int selected = roll;
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            int weight = entry.has("data") ? entry.get("weight")
                .getAsInt() : 1;
            selected -= weight;
            if (selected < 0) {
                return new WeightedSelection(entry.has("data") ? entry.get("data") : entry, roll, total);
            }
        }
        return allowBareFallback ? new WeightedSelection(entries.get(entries.size() - 1), roll, total) : null;
    }

    private static final class WeightedSelection {

        final JsonElement data;
        final int roll;
        final int total;

        WeightedSelection(JsonElement data, int roll, int total) {
            this.data = data;
            this.roll = roll;
            this.total = total;
        }
    }

    private static String placedFeatureIdentifier(JsonElement placedFeature) {
        if (placedFeature.isJsonPrimitive()) return placedFeature.getAsString();
        JsonObject placed = placedFeature.getAsJsonObject();
        return placed.has("feature") && placed.get("feature")
            .isJsonPrimitive() ? placed.get("feature")
                .getAsString() : "direct";
    }

    private static String randomState(JsonArray states, ModernRandom random) {
        return ModernDecorationEngine.state(
            states.get(random.nextInt(states.size()))
                .getAsJsonObject());
    }

    private static String stateAtNoise(JsonArray states, double value) {
        double normalized = Math.max(0.0D, Math.min(0.9999D, (1.0D + value) / 2.0D));
        return ModernDecorationEngine.state(
            states.get((int) (normalized * states.size()))
                .getAsJsonObject());
    }

    /** Source trySetValue does not invent an axis on a facing-only RU branch (or vice versa). */
    static String tryProperty(String state, String key, String value) {
        int start = state.indexOf('[');
        if (start < 0) return state;
        for (String assignment : state.substring(start + 1, state.length() - 1)
            .split(",")) {
            if (assignment.startsWith(key + "=")) return property(state, key, value);
        }
        return state;
    }

    public static String property(String state, String key, String value) {
        JsonObject properties = new JsonObject();
        int open = state.indexOf('[');
        if (open >= 0) {
            String body = state.substring(open + 1, state.length() - 1);
            if (!body.isEmpty()) {
                for (String entry : body.split(",")) {
                    int equals = entry.indexOf('=');
                    properties.addProperty(entry.substring(0, equals), entry.substring(equals + 1));
                }
            }
            state = state.substring(0, open);
        }
        properties.addProperty(key, value);
        List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
            properties.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, JsonElement>>() {

            @Override
            public int compare(Map.Entry<String, JsonElement> left, Map.Entry<String, JsonElement> right) {
                return left.getKey()
                    .compareTo(right.getKey());
            }
        });
        StringBuilder result = new StringBuilder(state).append('[');
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) result.append(',');
            result.append(
                entries.get(index)
                    .getKey())
                .append('=')
                .append(
                    entries.get(index)
                        .getValue()
                        .getAsString());
        }
        return result.append(']')
            .toString();
    }

    static boolean isDoublePlant(String state) {
        String block = ModernDecorationEngine.blockName(state);
        return "minecraft:tall_grass".equals(block) || "minecraft:large_fern".equals(block)
            || "minecraft:sunflower".equals(block)
            || "minecraft:lilac".equals(block)
            || "minecraft:rose_bush".equals(block)
            || "minecraft:peony".equals(block)
            || "minecraft:small_dripleaf".equals(block)
            || RuDecorationPaletteMetadata.isDoublePlant(block)
            || isPackedRuDoublePlant(block)
            // The imported palette currently renders the waterloggable mangrove shrub as
            // a solid compatibility block, but its modern placement contract is still a
            // DoublePlantBlock and must occupy both vertical cells.
            || "regions_unexplored:mangrove_shrub".equals(block);
    }

    /**
     * RU states retained in Biomic's compact two-block shrub palettes predate the generated
     * decoration palette, so they do not occur in its metadata file. They still extend modern
     * DoublePlantBlock and must consume/place an upper cell exactly like imported double plants.
     */
    private static boolean isPackedRuDoublePlant(String block) {
        return switch (block) {
            case "regions_unexplored:cypress_shrub", "regions_unexplored:dead_pine_shrub", "regions_unexplored:flowering_shrub", "regions_unexplored:joshua_shrub", "regions_unexplored:lavender_wisteria_shrub", "regions_unexplored:sky_wisteria_shrub", "regions_unexplored:salmon_wisteria_shrub", "regions_unexplored:pine_shrub", "regions_unexplored:redwood_shrub", "regions_unexplored:willow_shrub", "regions_unexplored:elephant_ear", "regions_unexplored:tassel", "regions_unexplored:eucalyptus_shrub" -> true;
            default -> false;
        };
    }

    private static boolean isAir(String state) {
        String block = ModernDecorationEngine.blockName(state);
        return "minecraft:air".equals(block) || "minecraft:cave_air".equals(block)
            || "minecraft:void_air".equals(block);
    }

    private static int[] direction(String direction) {
        if ("up".equals(direction)) return new int[] { 0, 1, 0 };
        if ("down".equals(direction)) return new int[] { 0, -1, 0 };
        if ("north".equals(direction)) return new int[] { 0, 0, -1 };
        if ("south".equals(direction)) return new int[] { 0, 0, 1 };
        if ("west".equals(direction)) return new int[] { -1, 0, 0 };
        if ("east".equals(direction)) return new int[] { 1, 0, 0 };
        throw new IllegalArgumentException("Unknown direction " + direction);
    }

    private static double clampedMap(double value, double low, double high, double mappedLow, double mappedHigh) {
        double delta = (value - low) / (high - low);
        delta = Math.max(0.0D, Math.min(1.0D, delta));
        return mappedLow + delta * (mappedHigh - mappedLow);
    }
}
