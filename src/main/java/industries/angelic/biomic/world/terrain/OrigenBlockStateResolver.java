package industries.angelic.biomic.world.terrain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import ganymedes01.etfuturum.ModBlocks;
import industries.angelic.biomic.content.BiomicAquaticBlocks;
import industries.angelic.biomic.content.BiomicBlocks;
import industries.angelic.biomic.content.BiomicDripleafBlocks;
import industries.angelic.biomic.content.BiomicExpansionBlocks;
import industries.angelic.biomic.content.BiomicFrozenBlocks;
import industries.angelic.biomic.content.BiomicGroundBlocks;
import industries.angelic.biomic.content.BiomicLushPlantBlocks;
import industries.angelic.biomic.content.BiomicMushroomBlocks;
import industries.angelic.biomic.content.BiomicSculkBlocks;

/** Converts the modern block-state strings authored by Origen to the exact available 1.7 block and metadata. */
public final class OrigenBlockStateResolver {

    private static final Map<String, Integer> COLORS = colors();

    /** Retain modern state properties while using a block resolved by the RU compatibility boundary. */
    public static ResolvedBlock imported(String state, Block block, int metadata) {
        ParsedState parsed = ParsedState.parse(state);
        return new ResolvedBlock(block, metadata, parsed.id, parsed.properties, false);
    }

    /** A placed block named as it stands (registry name and metadata), for a write restored from the world. */
    public static String placed(Block block, int metadata) {
        return PLACED + Block.blockRegistry.getNameForObject(block) + "@" + metadata;
    }

    private static final String PLACED = "biomic_placed:";

    public static boolean isPlaced(String state) {
        return state != null && state.startsWith(PLACED);
    }

    public ResolvedBlock resolve(String state) {
        if (state != null && state.startsWith(PLACED)) {
            int at = state.lastIndexOf('@');
            Block block = (Block) Block.blockRegistry.getObject(state.substring(PLACED.length(), at));
            if (block == null) throw new IllegalArgumentException("Unknown placed block: " + state);
            return new ResolvedBlock(block, Integer.parseInt(state.substring(at + 1)), state, Map.of(), false);
        }
        // Only the explicit fresh-marine alias opts into exact wall geometry; historical modern mappings stay literal.
        if (state != null && state.startsWith("biomic:") && TerraPackCoralWallFanCapability.authored(state)) {
            var exact = TerraPackCoralWallFanCapability.resolveInstalled(state);
            return imported(state, exact.block(), exact.metadata());
        }
        industries.angelic.biomic.content.gardens.GardensContent.Resolved gardens = industries.angelic.biomic.content.gardens.GardensContent
            .resolve(state);
        if (gardens != null) return imported(state, gardens.block(), gardens.metadata());
        industries.angelic.biomic.content.BiomicLegacyStates.Resolved plain = industries.angelic.biomic.content.BiomicLegacyStates
            .resolve(state);
        if (plain != null) return imported(state, plain.block(), plain.metadata());
        industries.angelic.biomic.content.alexscaves.ForlornStateCodec.State forlorn = industries.angelic.biomic.content.alexscaves.ForlornStateCodec
            .resolve(state);
        if (forlorn != null) return imported(state, forlorn.block(), forlorn.metadata());
        industries.angelic.biomic.content.alexscaves.PrimordialStateCodec.State primordial = industries.angelic.biomic.content.alexscaves.PrimordialStateCodec
            .resolve(state);
        if (primordial != null) return imported(state, primordial.block(), primordial.metadata());
        industries.angelic.biomic.content.alexscaves.AbyssalStateCodec.State abyssal = industries.angelic.biomic.content.alexscaves.AbyssalStateCodec
            .resolve(state);
        if (abyssal != null) return imported(state, abyssal.block(), abyssal.metadata());
        if (industries.angelic.biomic.compat.flora.NativeFloraStates.handles(state))
            return industries.angelic.biomic.compat.flora.NativeFloraStates.resolve(state);
        ParsedState parsed = ParsedState.parse(state);
        String id = canonicalExternalId(parsed.id);
        ResolvedBlock resolved = switch (id) {
            case "minecraft:air", "minecraft:cave_air", "minecraft:void_air" -> block(Blocks.air, 0, id, parsed);
            case "minecraft:bedrock" -> block(Blocks.bedrock, 0, id, parsed);
            case "minecraft:cobweb" -> block(Blocks.web, 0, id, parsed);
            case "minecraft:stone" -> block(Blocks.stone, 0, id, parsed);
            case "minecraft:netherrack" -> block(Blocks.netherrack, 0, id, parsed);
            case "minecraft:soul_sand" -> block(Blocks.soul_sand, 0, id, parsed);
            case "minecraft:glowstone" -> block(Blocks.glowstone, 0, id, parsed);
            case "minecraft:ochre_froglight" -> block(
                BiomicExpansionBlocks.OCHRE_FROGLIGHT,
                axisBits(parsed),
                id,
                parsed);
            // EFR intentionally represents soul fire with vanilla fire plus a soul-soil/sand support block.
            // The application boundary must check the support; block+metadata alone cannot encode this type.
            case "minecraft:soul_fire" -> {
                if (!(Blocks.fire instanceof ganymedes01.etfuturum.ducks.ISoulFireInfo))
                    throw new IllegalStateException("Soul fire requires EFR's soul-fire mixins");
                yield block(Blocks.fire, 0, id, parsed);
            }
            case "minecraft:redstone_block" -> block(Blocks.redstone_block, 0, id, parsed);
            case "minecraft:nether_quartz_ore" -> block(Blocks.quartz_ore, 0, id, parsed);
            case "minecraft:soul_soil" -> requiredEfr(ModBlocks.SOUL_SOIL, 0, id, parsed);
            case "minecraft:nether_gold_ore" -> requiredEfr(ModBlocks.NETHER_GOLD_ORE, 0, id, parsed);
            case "minecraft:ancient_debris" -> requiredEfr(ModBlocks.ANCIENT_DEBRIS, 0, id, parsed);
            case "minecraft:crying_obsidian" -> requiredEfr(ModBlocks.CRYING_OBSIDIAN, 0, id, parsed);
            case "minecraft:red_nether_bricks" -> requiredEfr(ModBlocks.RED_NETHERBRICK, 0, id, parsed);
            case "minecraft:crimson_nylium", "minecraft:warped_nylium" -> requiredEfr(
                ModBlocks.NYLIUM,
                id.equals("minecraft:warped_nylium") ? 1 : 0,
                id,
                parsed);
            case "minecraft:crimson_fungus", "minecraft:warped_fungus" -> requiredEfr(
                ModBlocks.NETHER_FUNGUS,
                id.equals("minecraft:warped_fungus") ? 1 : 0,
                id,
                parsed);
            case "minecraft:crimson_roots", "minecraft:warped_roots" -> requiredEfr(
                ModBlocks.NETHER_ROOTS,
                id.equals("minecraft:warped_roots") ? 1 : 0,
                id,
                parsed);
            case "minecraft:nether_sprouts" -> requiredEfr(ModBlocks.NETHER_SPROUTS, 0, id, parsed);
            case "minecraft:nether_wart_block", "minecraft:warped_wart_block" -> requiredEfr(
                ModBlocks.NETHER_WART,
                id.equals("minecraft:warped_wart_block") ? 1 : 0,
                id,
                parsed);
            case "minecraft:crimson_stem", "minecraft:crimson_hyphae" -> requiredEfr(
                ModBlocks.CRIMSON_STEM,
                (id.endsWith("hyphae") ? 1 : 0) | axisBits(parsed),
                id,
                parsed);
            case "minecraft:warped_stem", "minecraft:warped_hyphae" -> requiredEfr(
                ModBlocks.WARPED_STEM,
                (id.endsWith("hyphae") ? 1 : 0) | axisBits(parsed),
                id,
                parsed);
            // Explicit body/head metadata leaves EFR's existing contextual metadata 0/1 untouched.
            case "minecraft:weeping_vines", "minecraft:weeping_vines_plant" -> requiredEfr(
                ModBlocks.WEEPING_VINES,
                industries.angelic.biomic.compat.efr.ExplicitNetherVines.encode(id.endsWith("_plant"), age(parsed)),
                id,
                parsed);
            case "minecraft:twisting_vines", "minecraft:twisting_vines_plant" -> requiredEfr(
                ModBlocks.TWISTING_VINES,
                industries.angelic.biomic.compat.efr.ExplicitNetherVines.encode(id.endsWith("_plant"), age(parsed)),
                id,
                parsed);
            case "minecraft:frosted_ice" -> requiredEfr(ModBlocks.FROSTED_ICE, Math.min(3, age(parsed)), id, parsed);
            case "minecraft:cobblestone" -> block(Blocks.cobblestone, 0, id, parsed);
            case "minecraft:cobblestone_slab" -> block(
                "double".equals(parsed.properties.get("type")) ? Blocks.double_stone_slab : Blocks.stone_slab,
                slabMetadata(3, parsed),
                id,
                parsed);
            case "minecraft:dark_oak_slab" -> block(
                "double".equals(parsed.properties.get("type")) ? Blocks.double_wooden_slab : Blocks.wooden_slab,
                slabMetadata(5, parsed),
                id,
                parsed);
            case "minecraft:stone_slab" -> requiredEfr(
                "double".equals(parsed.properties.get("type")) ? ModBlocks.DOUBLE_STONE_SLAB : ModBlocks.STONE_SLAB,
                slabMetadata(0, parsed),
                id,
                parsed);
            case "minecraft:mossy_cobblestone" -> block(Blocks.mossy_cobblestone, 0, id, parsed);
            case "minecraft:mossy_cobblestone_slab" -> efr(
                ModBlocks.STONE_SLAB,
                slabMetadata(1, parsed),
                id,
                parsed,
                Blocks.stone_slab,
                3);
            case "minecraft:stone_bricks" -> block(Blocks.stonebrick, 0, id, parsed);
            case "minecraft:infested_stone" -> block(Blocks.monster_egg, 0, id, parsed);
            case "minecraft:infested_deepslate" -> block(Blocks.monster_egg, 0, id, parsed, true);
            case "minecraft:andesite" -> efr(ModBlocks.STONE, 5, id, parsed, Blocks.stone);
            case "minecraft:diorite" -> efr(ModBlocks.STONE, 3, id, parsed, Blocks.stone);
            case "minecraft:granite" -> efr(ModBlocks.STONE, 1, id, parsed, Blocks.stone);
            case "minecraft:dirt" -> {
                // Native BOP's grassless legacy dirt is not EFR's separate modern coarse-dirt block.
                String legacy = parsed.properties.get("legacy_meta");
                if (legacy != null && !legacy.equals("1"))
                    throw new IllegalArgumentException("Unsupported explicit legacy dirt state: " + state);
                yield block(Blocks.dirt, legacy == null ? 0 : 1, id, parsed);
            }
            case "minecraft:rooted_dirt" -> block(BiomicGroundBlocks.ROOTED_DIRT, 0, id, parsed);
            case "minecraft:grass_block" -> block(Blocks.grass, 0, id, parsed);
            case "minecraft:grass", "minecraft:short_grass" -> block(Blocks.tallgrass, 1, id, parsed);
            case "minecraft:fern" -> block(Blocks.tallgrass, 2, id, parsed);
            case "minecraft:podzol" -> block(Blocks.dirt, 2, id, parsed);
            case "minecraft:coarse_dirt" -> efr(ModBlocks.COARSE_DIRT, 0, id, parsed, Blocks.dirt);
            case "minecraft:mycelium" -> block(Blocks.mycelium, 0, id, parsed);
            case "minecraft:sponge" -> block(Blocks.sponge, spongeMetadata(id), id, parsed);
            case "minecraft:wet_sponge" -> requiredEfr(ModBlocks.SPONGE, spongeMetadata(id), id, parsed);
            case "minecraft:prismarine", "minecraft:prismarine_bricks", "minecraft:dark_prismarine", "minecraft:sea_lantern" -> abyssalArchitecture(
                id,
                parsed);
            case "minecraft:dark_prismarine_stairs" -> abyssalArchitecture(id, parsed);
            case "minecraft:weathered_copper" -> requiredEfr(ModBlocks.COPPER_BLOCK, 2, id, parsed);
            case "minecraft:oxidized_copper" -> requiredEfr(ModBlocks.COPPER_BLOCK, 3, id, parsed);
            case "minecraft:weathered_cut_copper" -> requiredEfr(ModBlocks.COPPER_BLOCK, 6, id, parsed);
            case "minecraft:oxidized_cut_copper" -> requiredEfr(ModBlocks.COPPER_BLOCK, 7, id, parsed);
            case "minecraft:weathered_cut_copper_slab" -> requiredEfr(
                ModBlocks.CUT_COPPER_SLAB,
                2 | ("top".equals(parsed.properties.get("type")) ? 8 : 0),
                id,
                parsed);
            case "minecraft:oxidized_cut_copper_slab" -> requiredEfr(
                ModBlocks.CUT_COPPER_SLAB,
                3 | ("top".equals(parsed.properties.get("type")) ? 8 : 0),
                id,
                parsed);
            case "minecraft:weathered_cut_copper_stairs" -> requiredEfr(
                ModBlocks.WEATHERED_CUT_COPPER_STAIRS,
                stairMetadata(parsed),
                id,
                parsed);
            case "minecraft:waxed_weathered_cut_copper_stairs" -> requiredEfr(
                ModBlocks.WAXED_WEATHERED_CUT_COPPER_STAIRS,
                stairMetadata(parsed),
                id,
                parsed);
            case "minecraft:waxed_oxidized_cut_copper_stairs" -> requiredEfr(
                ModBlocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS,
                stairMetadata(parsed),
                id,
                parsed);
            case "minecraft:sand" -> block(Blocks.sand, 0, id, parsed);
            case "minecraft:red_sand" -> block(Blocks.sand, 1, id, parsed);
            case "minecraft:sandstone" -> block(Blocks.sandstone, 0, id, parsed);
            case "minecraft:red_sandstone" -> efr(ModBlocks.RED_SANDSTONE, 0, id, parsed, Blocks.sandstone);
            case "minecraft:smooth_sandstone" -> efr(ModBlocks.SMOOTH_SANDSTONE, 0, id, parsed, Blocks.sandstone);
            case "minecraft:smooth_red_sandstone" -> efr(
                ModBlocks.SMOOTH_RED_SANDSTONE,
                0,
                id,
                parsed,
                Blocks.sandstone);
            case "minecraft:smooth_stone" -> efr(ModBlocks.SMOOTH_STONE, 0, id, parsed, Blocks.double_stone_slab);
            case "minecraft:gravel" -> block(Blocks.gravel, 0, id, parsed);
            case "minecraft:clay" -> block(Blocks.clay, 0, id, parsed);
            case "minecraft:terracotta" -> block(Blocks.hardened_clay, 0, id, parsed);
            case "minecraft:water" -> block(Blocks.water, fluidMetadata(parsed), id, parsed);
            case "minecraft:lava" -> block(Blocks.lava, fluidMetadata(parsed), id, parsed);
            case "minecraft:fire" -> block(Blocks.fire, 0, id, parsed);
            case "minecraft:wool" -> block(Blocks.wool, campLegacyMetadata(parsed, 0, 7, 8, 12, 15), id, parsed);
            case "minecraft:bed" -> block(Blocks.bed, campLegacyMetadata(parsed, 0, 1, 2, 3, 8, 9, 10, 11), id, parsed);
            case "minecraft:chest" -> block(
                Blocks.chest,
                parsed.properties.containsKey("legacy_meta") ? campLegacyMetadata(parsed, 2, 3, 4, 5)
                    : horizontalFacing(parsed),
                id,
                parsed);
            case "minecraft:fence_gate" -> block(Blocks.fence_gate, campLegacyMetadata(parsed, 0, 1, 2, 3), id, parsed);
            case "minecraft:hay_block" -> block(Blocks.hay_block, campLegacyMetadata(parsed, 0, 4, 8), id, parsed);
            case "minecraft:wooden_slab" -> block(Blocks.wooden_slab, campLegacyMetadata(parsed, 0), id, parsed);
            case "minecraft:spawner" -> block(Blocks.mob_spawner, 0, id, parsed);
            case "minecraft:glass" -> block(Blocks.glass, 0, id, parsed);
            case "minecraft:orange_stained_glass" -> block(Blocks.stained_glass, 1, id, parsed);
            case "minecraft:blue_stained_glass" -> block(Blocks.stained_glass, 11, id, parsed);
            case "minecraft:light_blue_stained_glass" -> block(Blocks.stained_glass, 3, id, parsed);
            case "minecraft:purple_stained_glass" -> block(Blocks.stained_glass, 10, id, parsed);
            case "minecraft:candle" -> block(Blocks.torch, 0, id, parsed, true);
            case "minecraft:torch" -> block(Blocks.torch, 0, id, parsed);
            case "minecraft:oak_button", "minecraft:crimson_button", "minecraft:warped_button" -> block(
                Blocks.wooden_button,
                horizontalFacing(parsed),
                id,
                parsed,
                !"minecraft:oak_button".equals(id));
            case "minecraft:ice" -> block(Blocks.ice, 0, id, parsed);
            case "minecraft:packed_ice" -> block(Blocks.packed_ice, 0, id, parsed);
            case "minecraft:blue_ice" -> efr(ModBlocks.BLUE_ICE, 0, id, parsed, Blocks.packed_ice);
            case "minecraft:snow" -> block(Blocks.snow_layer, snowMetadata(parsed), id, parsed);
            case "minecraft:snow_block" -> block(Blocks.snow, 0, id, parsed);
            case "minecraft:powder_snow" -> block(BiomicFrozenBlocks.POWDER_SNOW, 0, id, parsed);
            case "minecraft:obsidian" -> block(Blocks.obsidian, 0, id, parsed);
            case "minecraft:bone_block" -> efr(ModBlocks.BONE, axisBits(parsed), id, parsed, Blocks.quartz_block);
            case "minecraft:coal_ore" -> block(Blocks.coal_ore, 0, id, parsed);
            case "minecraft:iron_ore" -> block(Blocks.iron_ore, 0, id, parsed);
            case "minecraft:gold_ore" -> block(Blocks.gold_ore, 0, id, parsed);
            case "minecraft:redstone_ore" -> block(Blocks.redstone_ore, 0, id, parsed);
            case "minecraft:lapis_ore" -> block(Blocks.lapis_ore, 0, id, parsed);
            case "minecraft:diamond_ore" -> block(Blocks.diamond_ore, 0, id, parsed);
            case "minecraft:emerald_ore" -> block(Blocks.emerald_ore, 0, id, parsed);
            case "minecraft:cactus" -> block(Blocks.cactus, 0, id, parsed);
            case "minecraft:sugar_cane" -> block(Blocks.reeds, 0, id, parsed);
            case "minecraft:lily_pad" -> block(Blocks.waterlily, 0, id, parsed);
            case "minecraft:melon" -> block(Blocks.melon_block, 0, id, parsed);
            case "minecraft:pumpkin" -> block(Blocks.pumpkin, horizontalFacing(parsed), id, parsed);
            case "minecraft:carved_pumpkin" -> block(Blocks.pumpkin, horizontalFacing(parsed), id, parsed);
            case "minecraft:pumpkin_stem" -> block(Blocks.pumpkin_stem, age(parsed), id, parsed);
            case "minecraft:wheat" -> block(Blocks.wheat, age(parsed), id, parsed);
            case "minecraft:carrots" -> block(Blocks.carrots, age(parsed), id, parsed);
            case "minecraft:potatoes" -> block(Blocks.potatoes, age(parsed), id, parsed);
            case "minecraft:beetroots" -> efr(ModBlocks.BEETROOTS, Math.min(3, age(parsed)), id, parsed, Blocks.wheat);
            case "minecraft:farmland" -> block(Blocks.farmland, integer(parsed, "moisture", 0), id, parsed);
            case "minecraft:dead_bush", "minecraft:deadbush" -> block(Blocks.deadbush, 0, id, parsed);
            case "minecraft:brown_mushroom" -> block(Blocks.brown_mushroom, 0, id, parsed);
            case "minecraft:red_mushroom" -> block(Blocks.red_mushroom, 0, id, parsed);
            case "minecraft:brown_mushroom_block" -> block(
                Blocks.brown_mushroom_block,
                mushroomMetadata(parsed),
                id,
                parsed);
            case "minecraft:red_mushroom_block" -> block(
                Blocks.red_mushroom_block,
                mushroomMetadata(parsed),
                id,
                parsed);
            case "minecraft:mushroom_stem" -> block(Blocks.brown_mushroom_block, 10, id, parsed);
            case "minecraft:shroomlight" -> efr(ModBlocks.SHROOMLIGHT, 0, id, parsed, Blocks.glowstone);
            case "biomic:mushroom_flesh" -> block(BiomicMushroomBlocks.MUSHROOM_FLESH, 0, id, parsed);
            case "minecraft:dandelion" -> block(Blocks.yellow_flower, 0, id, parsed);
            case "minecraft:poppy" -> block(Blocks.red_flower, 0, id, parsed);
            case "minecraft:blue_orchid" -> block(Blocks.red_flower, 1, id, parsed);
            case "minecraft:allium" -> block(Blocks.red_flower, 2, id, parsed);
            case "minecraft:azure_bluet" -> block(Blocks.red_flower, 3, id, parsed);
            case "minecraft:red_tulip" -> block(Blocks.red_flower, 4, id, parsed);
            case "minecraft:orange_tulip" -> block(Blocks.red_flower, 5, id, parsed);
            case "minecraft:white_tulip" -> block(Blocks.red_flower, 6, id, parsed);
            case "minecraft:pink_tulip" -> block(Blocks.red_flower, 7, id, parsed);
            case "minecraft:oxeye_daisy" -> block(Blocks.red_flower, 8, id, parsed);
            case "minecraft:cornflower" -> efr(ModBlocks.CORNFLOWER, 0, id, parsed, Blocks.red_flower, 8);
            case "minecraft:lily_of_the_valley" -> efr(
                ModBlocks.LILY_OF_THE_VALLEY,
                0,
                id,
                parsed,
                Blocks.red_flower,
                8);
            case "minecraft:sunflower" -> doublePlant(0, id, parsed);
            case "minecraft:lilac" -> doublePlant(1, id, parsed);
            case "minecraft:tall_grass" -> doublePlant(2, id, parsed);
            case "minecraft:large_fern" -> doublePlant(3, id, parsed);
            case "minecraft:rose_bush" -> doublePlant(4, id, parsed);
            case "minecraft:peony" -> doublePlant(5, id, parsed);
            case "minecraft:vine" -> block(Blocks.vine, vineMetadata(parsed), id, parsed);
            case "minecraft:hanging_roots" -> block(BiomicBlocks.HANGING_ROOTS, 0, id, parsed);
            case "minecraft:cocoa" -> block(Blocks.cocoa, cocoaMetadata(parsed), id, parsed);
            case "minecraft:oak_sapling" -> block(Blocks.sapling, 0, id, parsed);
            case "minecraft:spruce_sapling" -> block(Blocks.sapling, 1, id, parsed);
            case "minecraft:birch_sapling" -> block(Blocks.sapling, 2, id, parsed);
            case "minecraft:jungle_sapling" -> block(Blocks.sapling, 3, id, parsed);
            case "minecraft:acacia_sapling" -> block(Blocks.sapling, 4, id, parsed);
            case "minecraft:dark_oak_sapling" -> block(Blocks.sapling, 5, id, parsed);
            case "minecraft:cherry_sapling" -> efr(ModBlocks.SAPLING, 1, id, parsed, Blocks.sapling, 2);
            case "minecraft:oak_log", "minecraft:oak_wood" -> oldLog(0, id.endsWith("_wood"), id, parsed);
            case "minecraft:spruce_log", "minecraft:spruce_wood" -> oldLog(1, id.endsWith("_wood"), id, parsed);
            case "minecraft:birch_log", "minecraft:birch_wood" -> oldLog(2, id.endsWith("_wood"), id, parsed);
            case "minecraft:jungle_log", "minecraft:jungle_wood" -> oldLog(3, id.endsWith("_wood"), id, parsed);
            case "minecraft:acacia_log", "minecraft:acacia_wood" -> newLog(0, id.endsWith("_wood"), id, parsed);
            case "minecraft:dark_oak_log", "minecraft:dark_oak_wood" -> newLog(1, id.endsWith("_wood"), id, parsed);
            case "minecraft:oak_leaves" -> oldLeaves(Blocks.leaves, 0, id, parsed);
            case "minecraft:spruce_leaves" -> oldLeaves(Blocks.leaves, 1, id, parsed);
            case "minecraft:birch_leaves" -> oldLeaves(Blocks.leaves, 2, id, parsed);
            case "minecraft:jungle_leaves" -> oldLeaves(Blocks.leaves, 3, id, parsed);
            case "minecraft:acacia_leaves" -> oldLeaves(Blocks.leaves2, 0, id, parsed);
            case "minecraft:dark_oak_leaves" -> oldLeaves(Blocks.leaves2, 1, id, parsed);
            case "minecraft:oak_planks" -> block(Blocks.planks, 0, id, parsed);
            case "minecraft:spruce_planks" -> block(Blocks.planks, 1, id, parsed);
            case "minecraft:birch_planks" -> block(Blocks.planks, 2, id, parsed);
            case "minecraft:jungle_planks" -> block(Blocks.planks, 3, id, parsed);
            case "minecraft:acacia_planks" -> block(Blocks.planks, 4, id, parsed);
            case "minecraft:dark_oak_planks" -> block(Blocks.planks, 5, id, parsed);
            case "minecraft:oak_fence" -> block(Blocks.fence, 0, id, parsed);
            case "minecraft:spruce_fence" -> efr(ModBlocks.FENCE_SPRUCE, 0, id, parsed, Blocks.fence);
            case "minecraft:dark_oak_fence" -> efr(ModBlocks.FENCE_DARK_OAK, 0, id, parsed, Blocks.fence);
            case "minecraft:stone_stairs" -> stairs(ModBlocks.STONE_STAIRS, Blocks.stone_stairs, id, parsed);
            case "minecraft:andesite_stairs" -> stairs(ModBlocks.ANDESITE_STAIRS, Blocks.stone_stairs, id, parsed);
            case "minecraft:diorite_stairs" -> stairs(ModBlocks.DIORITE_STAIRS, Blocks.stone_stairs, id, parsed);
            case "minecraft:granite_stairs" -> stairs(ModBlocks.GRANITE_STAIRS, Blocks.stone_stairs, id, parsed);
            case "minecraft:sandstone_stairs" -> block(Blocks.sandstone_stairs, stairMetadata(parsed), id, parsed);
            case "minecraft:red_sandstone_stairs" -> stairs(
                ModBlocks.RED_SANDSTONE_STAIRS,
                Blocks.sandstone_stairs,
                id,
                parsed);
            default -> resolveModern(id, parsed);
        };
        if (resolved == null) {
            throw new IllegalArgumentException("No 1.7/EFR mapping for Origen block state " + state);
        }
        return resolved;
    }

    record LegacyPaletteKey(String holder, int metadata) {}

    static LegacyPaletteKey abyssalArchitectureKey(String id) {
        return switch (id) {
            case "minecraft:prismarine" -> new LegacyPaletteKey("PRISMARINE_BLOCK", 0);
            case "minecraft:prismarine_bricks" -> new LegacyPaletteKey("PRISMARINE_BLOCK", 1);
            case "minecraft:dark_prismarine" -> new LegacyPaletteKey("PRISMARINE_BLOCK", 2);
            case "minecraft:sea_lantern" -> new LegacyPaletteKey("SEA_LANTERN", 0);
            case "minecraft:dark_prismarine_stairs" -> new LegacyPaletteKey("PRISMARINE_STAIRS_DARK", -1);
            default -> throw new IllegalArgumentException("Not an Abyssal architecture state: " + id);
        };
    }

    private static ResolvedBlock abyssalArchitecture(String id, ParsedState parsed) {
        LegacyPaletteKey key = abyssalArchitectureKey(id);
        ModBlocks holder = switch (key.holder) {
            case "SEA_LANTERN" -> ModBlocks.SEA_LANTERN;
            case "PRISMARINE_STAIRS_DARK" -> ModBlocks.PRISMARINE_STAIRS_DARK;
            default -> ModBlocks.PRISMARINE_BLOCK;
        };
        return requiredEfr(holder, key.metadata < 0 ? stairMetadata(parsed) : key.metadata, id, parsed);
    }

    public String canonicalId(Block block, int metadata) {
        return canonicalBlockTypeId(block, metadata);
    }

    /**
     * Returns the modern block type represented by a physical 1.7 block state.
     *
     * <p>
     * Terra's Bukkit adapter implements {@code BlockState.matches} as a block-type comparison: state properties
     * such as farmland moisture, crop age, snow layers and log axis do not participate. Some distinct modern block
     * types share one metadata-backed 1.7 block, however, so comparing only the {@link Block} instance would make
     * sand match red sand (and every stained terracotta colour match every other colour).
     * </p>
     */
    static String canonicalBlockTypeId(Block block, int metadata) {
        if (block instanceof industries.angelic.biomic.content.block.BlockSideCapMushroom) return "minecraft:"
            + (industries.angelic.biomic.content.block.BlockSideCapMushroom.metadata(metadata) < 8 ? "brown" : "red")
            + "_mushroom_block";
        if (block == null || block == Blocks.air || block.getMaterial() == Material.air) return "minecraft:air";
        if (block instanceof industries.angelic.biomic.content.block.BlockPoreBottomMushroom) return "minecraft:"
            + (industries.angelic.biomic.content.block.BlockPoreBottomMushroom.kind(metadata) == 0 ? "brown" : "red")
            + "_mushroom_block";
        if (block == Blocks.stone) return "minecraft:stone";
        if (block == Blocks.web) return "minecraft:cobweb";
        if (block == Blocks.brown_mushroom_block && metadata == 10) return "minecraft:mushroom_stem";
        if (block == Blocks.stonebrick) return "minecraft:stone_bricks";
        if (block == Blocks.monster_egg) return "minecraft:infested_stone";
        if (block == Blocks.grass) return "minecraft:grass_block";
        if (block == Blocks.sponge) return "minecraft:sponge";
        if (block == Blocks.tallgrass) return (metadata & 3) == 2 ? "minecraft:fern" : "minecraft:grass";
        if (block == BiomicGroundBlocks.ROOTED_DIRT) return "minecraft:rooted_dirt";
        if (block == Blocks.dirt) return metadata == 2 ? "minecraft:podzol" : "minecraft:dirt";
        if (block == Blocks.water || block == Blocks.flowing_water) return "minecraft:water";
        if (block == Blocks.lava || block == Blocks.flowing_lava) return "minecraft:lava";
        if (block == Blocks.sand) return metadata == 1 ? "minecraft:red_sand" : "minecraft:sand";
        if (block == Blocks.sandstone) return "minecraft:sandstone";
        if (block == Blocks.gravel) return "minecraft:gravel";
        if (block == Blocks.clay) return "minecraft:clay";
        if (block == Blocks.hardened_clay) return "minecraft:terracotta";
        if (block == Blocks.stained_hardened_clay) return "minecraft:" + colorName(metadata) + "_terracotta";
        if (block == Blocks.glass) return "minecraft:glass";
        if (block == Blocks.stained_glass) return "minecraft:" + colorName(metadata) + "_stained_glass";
        if (block == Blocks.snow) return "minecraft:snow_block";
        if (block == Blocks.snow_layer) return "minecraft:snow";
        if (block == Blocks.ice) return "minecraft:ice";
        if (block == Blocks.packed_ice) return "minecraft:packed_ice";
        if (block == Blocks.farmland) return "minecraft:farmland";
        if (block == Blocks.wheat) return "minecraft:wheat";
        if (block == Blocks.carrots) return "minecraft:carrots";
        if (block == Blocks.potatoes) return "minecraft:potatoes";
        if (block == Blocks.reeds) return "minecraft:sugar_cane";
        if (block == Blocks.waterlily) return "minecraft:lily_pad";
        if (block == Blocks.melon_block) return "minecraft:melon";
        if (block == Blocks.pumpkin_stem) return "minecraft:pumpkin_stem";
        if (block == Blocks.yellow_flower) return "minecraft:dandelion";
        if (block == Blocks.red_flower) return switch (metadata & 15) {
            case 1 -> "minecraft:blue_orchid";
            case 2 -> "minecraft:allium";
            case 3 -> "minecraft:azure_bluet";
            case 4 -> "minecraft:red_tulip";
            case 5 -> "minecraft:orange_tulip";
            case 6 -> "minecraft:white_tulip";
            case 7 -> "minecraft:pink_tulip";
            case 8 -> "minecraft:oxeye_daisy";
            default -> "minecraft:poppy";
        };
        if (block == Blocks.double_plant) return switch (metadata & 7) {
            case 1 -> "minecraft:lilac";
            case 2 -> "minecraft:tall_grass";
            case 3 -> "minecraft:large_fern";
            case 4 -> "minecraft:rose_bush";
            case 5 -> "minecraft:peony";
            default -> "minecraft:sunflower";
        };
        if (block == Blocks.sapling) return switch (metadata & 7) {
            case 1 -> "minecraft:spruce_sapling";
            case 2 -> "minecraft:birch_sapling";
            case 3 -> "minecraft:jungle_sapling";
            case 4 -> "minecraft:acacia_sapling";
            case 5 -> "minecraft:dark_oak_sapling";
            default -> "minecraft:oak_sapling";
        };
        if (block == Blocks.planks) return switch (metadata & 7) {
            case 1 -> "minecraft:spruce_planks";
            case 2 -> "minecraft:birch_planks";
            case 3 -> "minecraft:jungle_planks";
            case 4 -> "minecraft:acacia_planks";
            case 5 -> "minecraft:dark_oak_planks";
            default -> "minecraft:oak_planks";
        };
        if (block == Blocks.fence) return "minecraft:oak_fence";
        if (block == Blocks.wooden_button) return "minecraft:oak_button";
        if (block == Blocks.torch) return "minecraft:torch";
        if (block == Blocks.cobblestone) return "minecraft:cobblestone";
        if (block == Blocks.netherrack) return "minecraft:netherrack";
        if (block == Blocks.quartz_ore) return "minecraft:nether_quartz_ore";
        if ((block == Blocks.stone_slab || block == Blocks.double_stone_slab) && (metadata & 7) == 3)
            return "minecraft:cobblestone_slab";
        if ((block == Blocks.wooden_slab || block == Blocks.double_wooden_slab) && (metadata & 7) == 5)
            return "minecraft:dark_oak_slab";
        if (block == Blocks.mossy_cobblestone) return "minecraft:mossy_cobblestone";
        if (block == Blocks.chest) return "minecraft:chest";
        if (block == Blocks.mob_spawner) return "minecraft:spawner";
        if (block == Blocks.log) return switch (metadata & 3) {
            case 1 -> (metadata & 12) == 12 ? "minecraft:spruce_wood" : "minecraft:spruce_log";
            case 2 -> (metadata & 12) == 12 ? "minecraft:birch_wood" : "minecraft:birch_log";
            case 3 -> (metadata & 12) == 12 ? "minecraft:jungle_wood" : "minecraft:jungle_log";
            default -> (metadata & 12) == 12 ? "minecraft:oak_wood" : "minecraft:oak_log";
        };
        if (block == Blocks.log2)
            return (metadata & 3) == 1 ? ((metadata & 12) == 12 ? "minecraft:dark_oak_wood" : "minecraft:dark_oak_log")
                : ((metadata & 12) == 12 ? "minecraft:acacia_wood" : "minecraft:acacia_log");
        if (block == Blocks.leaves) return switch (metadata & 3) {
            case 1 -> "minecraft:spruce_leaves";
            case 2 -> "minecraft:birch_leaves";
            case 3 -> "minecraft:jungle_leaves";
            default -> "minecraft:oak_leaves";
        };
        if (block == Blocks.leaves2)
            return (metadata & 3) == 1 ? "minecraft:dark_oak_leaves" : "minecraft:acacia_leaves";
        if (block == BiomicDripleafBlocks.BIG_DRIPLEAF) return "minecraft:big_dripleaf";
        if (block == BiomicDripleafBlocks.BIG_DRIPLEAF_STEM) return "minecraft:big_dripleaf_stem";
        if (block == BiomicDripleafBlocks.SMALL_DRIPLEAF) return "minecraft:small_dripleaf";
        if (block == BiomicLushPlantBlocks.SPORE_BLOSSOM) return "minecraft:spore_blossom";
        if (block == BiomicLushPlantBlocks.HANGING_MANGROVE_PROPAGULE) return "minecraft:mangrove_propagule";
        if (block == BiomicAquaticBlocks.KELP) return metadata == 1 ? "minecraft:kelp" : "minecraft:kelp_plant";
        if (block == BiomicAquaticBlocks.SEAGRASS) return metadata == 0 ? "minecraft:seagrass"
            : "minecraft:tall_seagrass[half=" + (metadata == 2 ? "upper" : "lower") + "]";
        if (block == BiomicAquaticBlocks.SEA_PICKLE) return "minecraft:sea_pickle";
        if (block == BiomicAquaticBlocks.CORAL_PLANT) return coralId(metadata, "_coral");
        if (block == BiomicAquaticBlocks.CORAL_FAN) return coralId(metadata, "_coral_fan");
        if (block instanceof industries.angelic.biomic.content.block.BlockCoralWallFan)
            return TerraPackCoralWallFanCapability.modernState(block, metadata);
        if (block == BiomicAquaticBlocks.CORAL_BLOCK) return coralId(metadata, "_coral_block");
        if (block == BiomicSculkBlocks.SCULK_SENSOR) return "minecraft:sculk_sensor";
        if (block == BiomicSculkBlocks.SCULK_SHRIEKER) return "minecraft:sculk_shrieker";
        if (block == BiomicSculkBlocks.SCULK_VEIN || block == BiomicSculkBlocks.SCULK_VEIN_UP
            || block == BiomicSculkBlocks.SCULK_VEIN_DOWN
            || block == BiomicSculkBlocks.SCULK_VEIN_VERTICAL) return "minecraft:sculk_vein";
        if (block == BiomicFrozenBlocks.POWDER_SNOW) return "minecraft:powder_snow";
        if (block == BiomicExpansionBlocks.OCHRE_FROGLIGHT) return "minecraft:ochre_froglight";
        if (block == BiomicMushroomBlocks.MUSHROOM_FLESH) return "biomic:mushroom_flesh";
        // Resolve Biomic-owned blocks before touching ModBlocks. Besides avoiding needless work in production, this
        // keeps local block identity usable by tooling that runs outside an initialized FML launch.
        if (block == ModBlocks.COARSE_DIRT.get()) return "minecraft:coarse_dirt";
        if (block == ModBlocks.SPONGE.get()) return metadata == 1 ? "minecraft:wet_sponge" : "minecraft:sponge";
        if (block == ModBlocks.SOUL_SOIL.get()) return "minecraft:soul_soil";
        if (block == ModBlocks.NETHER_GOLD_ORE.get()) return "minecraft:nether_gold_ore";
        if (block == ModBlocks.ANCIENT_DEBRIS.get()) return "minecraft:ancient_debris";
        if (block == ModBlocks.CRYING_OBSIDIAN.get()) return "minecraft:crying_obsidian";
        if (block == ModBlocks.RED_NETHERBRICK.get() && metadata == 0) return "minecraft:red_nether_bricks";
        if (block == ModBlocks.FROSTED_ICE.get()) return "minecraft:frosted_ice";
        if (block == ModBlocks.NYLIUM.get())
            return (metadata & 1) == 0 ? "minecraft:crimson_nylium" : "minecraft:warped_nylium";
        if (block == ModBlocks.NETHER_FUNGUS.get())
            return (metadata & 1) == 0 ? "minecraft:crimson_fungus" : "minecraft:warped_fungus";
        if (block == ModBlocks.NETHER_ROOTS.get())
            return (metadata & 1) == 0 ? "minecraft:crimson_roots" : "minecraft:warped_roots";
        if (block == ModBlocks.NETHER_WART.get())
            return (metadata & 1) == 0 ? "minecraft:nether_wart_block" : "minecraft:warped_wart_block";
        if (block == ModBlocks.NETHER_SPROUTS.get()) return "minecraft:nether_sprouts";
        if (block == ModBlocks.CRIMSON_STEM.get()) return "minecraft:" + ((metadata & 2) != 0 ? "stripped_" : "")
            + ((metadata & 1) == 0 ? "crimson_stem" : "crimson_hyphae");
        if (block == ModBlocks.WARPED_STEM.get()) return "minecraft:" + ((metadata & 2) != 0 ? "stripped_" : "")
            + ((metadata & 1) == 0 ? "warped_stem" : "warped_hyphae");
        // No body/tip assertion without context: EFR stores both in one block. Use canonicalId(world, ...) instead.
        if (block == ModBlocks.WEEPING_VINES.get())
            return metadata == 2 ? "minecraft:weeping_vines_plant" : "minecraft:weeping_vines";
        if (block == ModBlocks.TWISTING_VINES.get())
            return metadata == 2 ? "minecraft:twisting_vines_plant" : "minecraft:twisting_vines";
        if (block == ModBlocks.STONE.get()) return switch (metadata) {
            case 1, 2 -> "minecraft:granite";
            case 3, 4 -> "minecraft:diorite";
            case 5, 6 -> "minecraft:andesite";
            default -> "minecraft:stone";
        };
        if (block == ModBlocks.MOSS_BLOCK.get()) return "minecraft:moss_block";
        if (block == ModBlocks.MUD.get()) return "minecraft:mud";
        if (block == ModBlocks.PACKED_MUD.get()) return "minecraft:packed_mud";
        if (block == ModBlocks.DRIPSTONE_BLOCK.get()) return "minecraft:dripstone_block";
        if (block == ModBlocks.BONE.get()) return "minecraft:bone_block";
        if (block == ModBlocks.DEEPSLATE.get()) return "minecraft:deepslate";
        if (block == ModBlocks.TUFF.get()) return "minecraft:tuff";
        if (block == ModBlocks.CALCITE.get()) return "minecraft:calcite";
        if (block == ModBlocks.BASALT.get()) return "minecraft:basalt";
        if (block == ModBlocks.SMOOTH_BASALT.get()) return "minecraft:smooth_basalt";
        if (block == ModBlocks.BLUE_ICE.get()) return "minecraft:blue_ice";
        if (block == ModBlocks.BLACKSTONE.get()) return "minecraft:blackstone";
        if (block == ModBlocks.COBBLED_DEEPSLATE.get()) return "minecraft:cobbled_deepslate";
        if (block == ModBlocks.POLISHED_DEEPSLATE.get()) return "minecraft:polished_deepslate";
        if (block == ModBlocks.DEEPSLATE_BRICKS.get()) return switch (metadata & 7) {
            case 1 -> "minecraft:cracked_deepslate_bricks";
            case 2 -> "minecraft:deepslate_tiles";
            case 3 -> "minecraft:cracked_deepslate_tiles";
            case 4 -> "minecraft:chiseled_deepslate";
            default -> "minecraft:deepslate_bricks";
        };
        if (block == ModBlocks.COPPER_ORE.get()) return "minecraft:copper_ore";
        if (block == ModBlocks.DEEPSLATE_COAL_ORE.get()) return "minecraft:deepslate_coal_ore";
        if (block == ModBlocks.DEEPSLATE_IRON_ORE.get()) return "minecraft:deepslate_iron_ore";
        if (block == ModBlocks.DEEPSLATE_GOLD_ORE.get()) return "minecraft:deepslate_gold_ore";
        if (block == ModBlocks.DEEPSLATE_REDSTONE_ORE.get()) return "minecraft:deepslate_redstone_ore";
        if (block == ModBlocks.DEEPSLATE_LAPIS_ORE.get()) return "minecraft:deepslate_lapis_ore";
        if (block == ModBlocks.DEEPSLATE_DIAMOND_ORE.get()) return "minecraft:deepslate_diamond_ore";
        if (block == ModBlocks.DEEPSLATE_EMERALD_ORE.get()) return "minecraft:deepslate_emerald_ore";
        if (block == ModBlocks.DEEPSLATE_COPPER_ORE.get()) return "minecraft:deepslate_copper_ore";
        if (block == ModBlocks.RAW_ORE_BLOCK.get()) {
            return metadata == 0 ? "minecraft:raw_copper_block" : "minecraft:raw_iron_block";
        }
        if (block == ModBlocks.AMETHYST_BLOCK.get()) return "minecraft:amethyst_block";
        if (block == ModBlocks.BUDDING_AMETHYST.get()) return "minecraft:budding_amethyst";
        if (block == ModBlocks.AMETHYST_CLUSTER_1.get()) {
            return amethystId(1, metadata);
        }
        if (block == ModBlocks.AMETHYST_CLUSTER_2.get()) return amethystId(2, metadata);
        if (block == ModBlocks.POINTED_DRIPSTONE.get()) return "minecraft:pointed_dripstone";
        if (block == ModBlocks.MAGMA.get()) return "minecraft:magma_block";
        if (block == ModBlocks.BEETROOTS.get()) return "minecraft:beetroots";
        if (block == ModBlocks.CORNFLOWER.get()) return "minecraft:cornflower";
        if (block == ModBlocks.LILY_OF_THE_VALLEY.get()) return "minecraft:lily_of_the_valley";
        if (block == ModBlocks.SMOOTH_STONE.get()) return "minecraft:smooth_stone";
        if (block == ModBlocks.SMOOTH_SANDSTONE.get()) return "minecraft:smooth_sandstone";
        if (block == ModBlocks.RED_SANDSTONE.get()) return "minecraft:red_sandstone";
        if (block == ModBlocks.SMOOTH_RED_SANDSTONE.get()) return "minecraft:smooth_red_sandstone";
        if (block == ModBlocks.STONE_SLAB.get() && (metadata & 7) == 1) return "minecraft:mossy_cobblestone_slab";
        if ((block == ModBlocks.STONE_SLAB.get() || block == ModBlocks.DOUBLE_STONE_SLAB.get()) && (metadata & 7) == 0)
            return "minecraft:stone_slab";
        if (block == ModBlocks.MOSS_CARPET.get()) return "minecraft:moss_carpet";
        if (block == ModBlocks.MANGROVE_ROOTS.get()) return "minecraft:mangrove_roots";
        if (block == ModBlocks.MUDDY_MANGROVE_ROOTS.get()) return "minecraft:muddy_mangrove_roots";
        if (block == ModBlocks.AZALEA.get())
            return metadata % 2 == 1 ? "minecraft:flowering_azalea" : "minecraft:azalea";
        if (block == ModBlocks.AZALEA_LEAVES.get())
            return metadata % 2 == 1 ? "minecraft:flowering_azalea_leaves" : "minecraft:azalea_leaves";
        if (block == ModBlocks.LEAVES.get())
            return metadata % 4 == 1 ? "minecraft:cherry_leaves" : "minecraft:mangrove_leaves";
        if (block == ModBlocks.CHERRY_LOG.get())
            return (metadata & 3) == 1 ? "minecraft:cherry_wood" : "minecraft:cherry_log";
        if (block == ModBlocks.MANGROVE_LOG.get())
            return (metadata & 3) == 1 ? "minecraft:mangrove_wood" : "minecraft:mangrove_log";
        if (block == ModBlocks.SCULK.get()) return "minecraft:sculk";
        if (block == ModBlocks.SCULK_CATALYST.get()) return "minecraft:sculk_catalyst";
        if (block == ModBlocks.SHROOMLIGHT.get()) return "minecraft:shroomlight";
        if (block == ModBlocks.PINK_PETALS.get()) return "minecraft:pink_petals";
        if (block == ModBlocks.SWEET_BERRY_BUSH.get()) return "minecraft:sweet_berry_bush";
        if (block == ModBlocks.BAMBOO.get()) return "minecraft:bamboo";
        if (block == ModBlocks.CHAIN.get()) return "minecraft:chain";
        if (block == ModBlocks.BEE_NEST.get()) return "minecraft:bee_nest";
        if (block == ModBlocks.CONCRETE.get()) return "minecraft:" + colorName(metadata) + "_concrete";
        if (block == ModBlocks.CONCRETE_POWDER.get()) return "minecraft:" + colorName(metadata) + "_concrete_powder";
        if (block == ModBlocks.GLOW_LICHEN.get()) return "minecraft:glow_lichen";
        if (block == ModBlocks.CAVE_VINE.get()) return "minecraft:cave_vines";
        if (block == ModBlocks.CAVE_VINE_PLANT.get()) return "minecraft:cave_vines_plant";
        if (block == ModBlocks.SAPLING.get())
            return (metadata & 7) == 0 ? "minecraft:mangrove_propagule" : "minecraft:cherry_sapling";
        Object registered = Block.blockRegistry.getNameForObject(block);
        return registered == null ? "minecraft:air" : canonicalExternalId(registered.toString());
    }

    /** Pure request mapping; exact EFR block identity is checked only in an initialized launch. */
    static int spongeMetadata(String id) {
        return switch (id) {
            case "minecraft:sponge" -> 0;
            case "minecraft:wet_sponge" -> 1;
            default -> throw new IllegalArgumentException("Not a sponge state: " + id);
        };
    }

    private static String colorName(int metadata) {
        return switch (metadata & 15) {
            case 1 -> "orange";
            case 2 -> "magenta";
            case 3 -> "light_blue";
            case 4 -> "yellow";
            case 5 -> "lime";
            case 6 -> "pink";
            case 7 -> "gray";
            case 8 -> "light_gray";
            case 9 -> "cyan";
            case 10 -> "purple";
            case 11 -> "blue";
            case 12 -> "brown";
            case 13 -> "green";
            case 14 -> "red";
            case 15 -> "black";
            default -> "white";
        };
    }

    static String canonicalExternalId(String id) {
        return switch (id) {
            case "etfuturum:bone_block" -> "minecraft:bone_block";
            case "etfuturum:coarse_dirt" -> "minecraft:coarse_dirt";
            default -> id;
        };
    }

    private static ResolvedBlock resolveModern(String id, ParsedState parsed) {
        if (id.endsWith("_terracotta")) {
            return block(Blocks.stained_hardened_clay, color(id, "_terracotta"), id, parsed);
        }
        if (id.endsWith("_concrete")) {
            return efr(ModBlocks.CONCRETE, color(id, "_concrete"), id, parsed, Blocks.stained_hardened_clay);
        }
        if (id.endsWith("_concrete_powder")) {
            return efr(ModBlocks.CONCRETE_POWDER, color(id, "_concrete_powder"), id, parsed, Blocks.sand);
        }
        return switch (id) {
            case "minecraft:basalt", "minecraft:polished_basalt" -> efr(
                ModBlocks.BASALT,
                id.contains("polished") ? 1 : 0,
                id,
                parsed,
                Blocks.stone);
            case "minecraft:smooth_basalt" -> efr(ModBlocks.SMOOTH_BASALT, 0, id, parsed, Blocks.stone);
            case "minecraft:calcite" -> efr(ModBlocks.CALCITE, 0, id, parsed, Blocks.stone);
            case "minecraft:tuff" -> efr(ModBlocks.TUFF, 0, id, parsed, Blocks.stone);
            case "minecraft:blackstone" -> efr(ModBlocks.BLACKSTONE, 0, id, parsed, Blocks.stone);
            case "minecraft:deepslate" -> efr(ModBlocks.DEEPSLATE, 0, id, parsed, Blocks.stone);
            case "minecraft:cobbled_deepslate" -> efr(ModBlocks.COBBLED_DEEPSLATE, 0, id, parsed, Blocks.cobblestone);
            case "minecraft:polished_deepslate" -> efr(ModBlocks.POLISHED_DEEPSLATE, 0, id, parsed, Blocks.stonebrick);
            case "minecraft:deepslate_bricks" -> efr(ModBlocks.DEEPSLATE_BRICKS, 0, id, parsed, Blocks.stonebrick);
            case "minecraft:cracked_deepslate_bricks" -> efr(
                ModBlocks.DEEPSLATE_BRICKS,
                1,
                id,
                parsed,
                Blocks.stonebrick);
            case "minecraft:deepslate_tiles" -> efr(ModBlocks.DEEPSLATE_BRICKS, 2, id, parsed, Blocks.stonebrick);
            case "minecraft:cracked_deepslate_tiles" -> efr(
                ModBlocks.DEEPSLATE_BRICKS,
                3,
                id,
                parsed,
                Blocks.stonebrick);
            case "minecraft:chiseled_deepslate" -> efr(ModBlocks.DEEPSLATE_BRICKS, 4, id, parsed, Blocks.stonebrick);
            case "minecraft:deepslate_tile_stairs" -> stairs(
                ModBlocks.DEEPSLATE_TILE_STAIRS,
                Blocks.stone_stairs,
                id,
                parsed);
            case "minecraft:cobbled_deepslate_wall" -> efr(
                ModBlocks.DEEPSLATE_WALL,
                0,
                id,
                parsed,
                Blocks.cobblestone_wall);
            case "minecraft:polished_deepslate_wall" -> efr(
                ModBlocks.DEEPSLATE_WALL,
                1,
                id,
                parsed,
                Blocks.cobblestone_wall);
            case "minecraft:deepslate_tile_wall" -> efr(
                ModBlocks.DEEPSLATE_WALL,
                3,
                id,
                parsed,
                Blocks.cobblestone_wall);
            case "minecraft:deepslate_coal_ore" -> efr(ModBlocks.DEEPSLATE_COAL_ORE, 0, id, parsed, Blocks.coal_ore);
            case "minecraft:deepslate_iron_ore" -> efr(ModBlocks.DEEPSLATE_IRON_ORE, 0, id, parsed, Blocks.iron_ore);
            case "minecraft:deepslate_gold_ore" -> efr(ModBlocks.DEEPSLATE_GOLD_ORE, 0, id, parsed, Blocks.gold_ore);
            case "minecraft:deepslate_redstone_ore" -> efr(
                ModBlocks.DEEPSLATE_REDSTONE_ORE,
                0,
                id,
                parsed,
                Blocks.redstone_ore);
            case "minecraft:deepslate_lapis_ore" -> efr(ModBlocks.DEEPSLATE_LAPIS_ORE, 0, id, parsed, Blocks.lapis_ore);
            case "minecraft:deepslate_diamond_ore" -> efr(
                ModBlocks.DEEPSLATE_DIAMOND_ORE,
                0,
                id,
                parsed,
                Blocks.diamond_ore);
            case "minecraft:deepslate_emerald_ore" -> efr(
                ModBlocks.DEEPSLATE_EMERALD_ORE,
                0,
                id,
                parsed,
                Blocks.emerald_ore);
            case "minecraft:deepslate_copper_ore" -> efr(
                ModBlocks.DEEPSLATE_COPPER_ORE,
                0,
                id,
                parsed,
                Blocks.iron_ore);
            case "minecraft:copper_ore" -> efr(ModBlocks.COPPER_ORE, 0, id, parsed, Blocks.iron_ore);
            case "minecraft:raw_copper_block" -> efr(ModBlocks.RAW_ORE_BLOCK, 0, id, parsed, Blocks.iron_block);
            case "minecraft:raw_iron_block" -> efr(ModBlocks.RAW_ORE_BLOCK, 1, id, parsed, Blocks.iron_block);
            case "minecraft:dripstone_block" -> efr(ModBlocks.DRIPSTONE_BLOCK, 0, id, parsed, Blocks.stone);
            case "minecraft:pointed_dripstone" -> efr(
                ModBlocks.POINTED_DRIPSTONE,
                dripstoneMetadata(parsed),
                id,
                parsed,
                Blocks.stone);
            case "minecraft:magma_block" -> efr(ModBlocks.MAGMA, 0, id, parsed, Blocks.netherrack);
            case "minecraft:mud" -> efr(ModBlocks.MUD, 0, id, parsed, Blocks.dirt);
            case "minecraft:packed_mud" -> efr(ModBlocks.PACKED_MUD, 0, id, parsed, Blocks.hardened_clay);
            case "minecraft:moss_block" -> efr(ModBlocks.MOSS_BLOCK, 0, id, parsed, Blocks.mossy_cobblestone);
            case "minecraft:moss_carpet" -> efr(ModBlocks.MOSS_CARPET, 0, id, parsed, Blocks.carpet);
            case "minecraft:azalea" -> efr(ModBlocks.AZALEA, 0, id, parsed, Blocks.sapling);
            case "minecraft:flowering_azalea" -> efr(ModBlocks.AZALEA, 1, id, parsed, Blocks.sapling);
            case "minecraft:azalea_leaves" -> efr(
                ModBlocks.AZALEA_LEAVES,
                leafMetadata(0, parsed),
                id,
                parsed,
                Blocks.leaves);
            case "minecraft:flowering_azalea_leaves" -> efr(
                ModBlocks.AZALEA_LEAVES,
                leafMetadata(1, parsed),
                id,
                parsed,
                Blocks.leaves);
            case "minecraft:mangrove_roots" -> efr(ModBlocks.MANGROVE_ROOTS, 0, id, parsed, Blocks.log);
            case "minecraft:muddy_mangrove_roots" -> efr(ModBlocks.MUDDY_MANGROVE_ROOTS, 0, id, parsed, Blocks.dirt);
            case "minecraft:mangrove_log", "minecraft:mangrove_wood" -> modernLog(
                ModBlocks.MANGROVE_LOG,
                id.endsWith("_wood") ? 1 : 0,
                id,
                parsed);
            case "minecraft:cherry_log", "minecraft:cherry_wood" -> modernLog(
                ModBlocks.CHERRY_LOG,
                id.endsWith("_wood") ? 1 : 0,
                id,
                parsed);
            case "minecraft:stripped_cherry_wood" -> modernLog(ModBlocks.CHERRY_LOG, 3, id, parsed);
            case "minecraft:stripped_oak_wood" -> efr(
                ModBlocks.WOOD_STRIPPED,
                axisBits(parsed),
                id,
                parsed,
                Blocks.log);
            case "minecraft:mangrove_leaves" -> efr(
                ModBlocks.LEAVES,
                leafMetadata(0, parsed),
                id,
                parsed,
                Blocks.leaves);
            case "minecraft:cherry_leaves" -> efr(ModBlocks.LEAVES, leafMetadata(1, parsed), id, parsed, Blocks.leaves);
            case "minecraft:bamboo" -> efr(ModBlocks.BAMBOO, bambooMetadata(parsed), id, parsed, Blocks.reeds);
            case "minecraft:cave_vines" -> efr(ModBlocks.CAVE_VINE, berries(parsed), id, parsed, Blocks.vine);
            case "minecraft:cave_vines_plant" -> efr(
                ModBlocks.CAVE_VINE_PLANT,
                berries(parsed),
                id,
                parsed,
                Blocks.vine);
            case "minecraft:glow_lichen" -> efr(ModBlocks.GLOW_LICHEN, 0, id, parsed, Blocks.vine);
            case "minecraft:pink_petals" -> efr(
                ModBlocks.PINK_PETALS,
                petalMetadata(parsed),
                id,
                parsed,
                Blocks.red_flower);
            case "minecraft:sweet_berry_bush" -> efr(
                ModBlocks.SWEET_BERRY_BUSH,
                age(parsed),
                id,
                parsed,
                Blocks.deadbush);
            case "minecraft:amethyst_block" -> efr(ModBlocks.AMETHYST_BLOCK, 0, id, parsed, Blocks.quartz_block);
            case "minecraft:budding_amethyst" -> efr(ModBlocks.BUDDING_AMETHYST, 0, id, parsed, Blocks.quartz_block);
            case "minecraft:small_amethyst_bud", "minecraft:medium_amethyst_bud", "minecraft:large_amethyst_bud", "minecraft:amethyst_cluster" -> {
                var mapping = amethystMapping(parsed);
                yield efr(
                    mapping.family() == 1 ? ModBlocks.AMETHYST_CLUSTER_1 : ModBlocks.AMETHYST_CLUSTER_2,
                    mapping.metadata(),
                    id,
                    parsed,
                    Blocks.glass);
            }
            case "minecraft:chain" -> efr(ModBlocks.CHAIN, 0, id, parsed, Blocks.iron_bars);
            case "minecraft:bee_nest" -> efr(ModBlocks.BEE_NEST, horizontalFacing(parsed), id, parsed, Blocks.log);
            case "minecraft:sculk" -> efr(ModBlocks.SCULK, 0, id, parsed, Blocks.obsidian);
            case "minecraft:sculk_catalyst" -> efr(ModBlocks.SCULK_CATALYST, 0, id, parsed, Blocks.obsidian);
            case "minecraft:sculk_sensor" -> block(BiomicSculkBlocks.SCULK_SENSOR, 0, id, parsed);
            case "minecraft:sculk_shrieker" -> block(BiomicSculkBlocks.SCULK_SHRIEKER, 0, id, parsed);
            case "minecraft:sculk_vein" -> block(
                BiomicSculkBlocks.vein(bool(parsed, "up"), bool(parsed, "down")),
                sculkVeinMetadata(parsed),
                id,
                parsed);
            case "minecraft:kelp" -> block(BiomicAquaticBlocks.KELP, 1, id, parsed);
            case "minecraft:kelp_plant" -> block(BiomicAquaticBlocks.KELP, 0, id, parsed);
            case "minecraft:seagrass" -> block(BiomicAquaticBlocks.SEAGRASS, 0, id, parsed);
            case "minecraft:tall_seagrass" -> block(
                BiomicAquaticBlocks.SEAGRASS,
                "upper".equals(parsed.properties.get("half")) ? 2 : 1,
                id,
                parsed);
            case "minecraft:sea_pickle" -> block(
                BiomicAquaticBlocks.SEA_PICKLE,
                Math.max(0, Math.min(3, integer(parsed, "pickles", 1) - 1)),
                id,
                parsed);
            case "minecraft:brain_coral", "minecraft:bubble_coral", "minecraft:fire_coral", "minecraft:horn_coral", "minecraft:tube_coral", "minecraft:dead_brain_coral", "minecraft:dead_bubble_coral", "minecraft:dead_fire_coral", "minecraft:dead_horn_coral", "minecraft:dead_tube_coral" -> block(
                BiomicAquaticBlocks.CORAL_PLANT,
                coralMetadata(id, "_coral"),
                id,
                parsed);
            case "minecraft:brain_coral_fan", "minecraft:bubble_coral_fan", "minecraft:fire_coral_fan", "minecraft:horn_coral_fan", "minecraft:tube_coral_fan", "minecraft:dead_brain_coral_fan", "minecraft:dead_bubble_coral_fan", "minecraft:dead_fire_coral_fan", "minecraft:dead_horn_coral_fan", "minecraft:dead_tube_coral_fan" -> block(
                BiomicAquaticBlocks.CORAL_FAN,
                coralMetadata(id, "_coral_fan"),
                id,
                parsed);
            case "minecraft:brain_coral_wall_fan", "minecraft:bubble_coral_wall_fan", "minecraft:fire_coral_wall_fan", "minecraft:horn_coral_wall_fan", "minecraft:tube_coral_wall_fan", "minecraft:dead_brain_coral_wall_fan", "minecraft:dead_bubble_coral_wall_fan", "minecraft:dead_fire_coral_wall_fan", "minecraft:dead_horn_coral_wall_fan", "minecraft:dead_tube_coral_wall_fan" -> block(
                BiomicAquaticBlocks.CORAL_FAN,
                coralMetadata(id, "_coral_wall_fan"),
                id,
                parsed,
                true);
            case "minecraft:brain_coral_block", "minecraft:bubble_coral_block", "minecraft:fire_coral_block", "minecraft:horn_coral_block", "minecraft:tube_coral_block", "minecraft:dead_brain_coral_block", "minecraft:dead_bubble_coral_block", "minecraft:dead_fire_coral_block", "minecraft:dead_horn_coral_block", "minecraft:dead_tube_coral_block" -> block(
                BiomicAquaticBlocks.CORAL_BLOCK,
                coralMetadata(id, "_coral_block"),
                id,
                parsed);
            case "minecraft:big_dripleaf" -> block(
                BiomicDripleafBlocks.BIG_DRIPLEAF,
                horizontalFacing(parsed),
                id,
                parsed);
            case "minecraft:big_dripleaf_stem" -> block(
                BiomicDripleafBlocks.BIG_DRIPLEAF_STEM,
                horizontalFacing(parsed),
                id,
                parsed);
            case "minecraft:small_dripleaf" -> block(
                BiomicDripleafBlocks.SMALL_DRIPLEAF,
                horizontalFacing(parsed) | ("upper".equals(parsed.properties.get("half")) ? 4 : 0),
                id,
                parsed);
            case "minecraft:spore_blossom" -> block(BiomicLushPlantBlocks.SPORE_BLOSSOM, 0, id, parsed);
            case "minecraft:mangrove_propagule" -> Boolean
                .parseBoolean(parsed.properties.getOrDefault("hanging", "false"))
                    ? block(
                        BiomicLushPlantBlocks.HANGING_MANGROVE_PROPAGULE,
                        Math.min(4, integer(parsed, "age", 0)),
                        id,
                        parsed)
                    : efr(ModBlocks.SAPLING, 0, id, parsed, Blocks.sapling);
            default -> null;
        };
    }

    private static ResolvedBlock requiredEfr(ModBlocks source, int metadata, String id, ParsedState parsed) {
        if (!installedEfr(source)) throw new IllegalStateException(
            "Required EFR content disabled or unregistered: " + source.name() + " for " + id);
        return block(source.get(), metadata, id, parsed);
    }

    private static ResolvedBlock efr(ModBlocks source, int metadata, String id, ParsedState parsed, Block fallback) {
        boolean installed = installedEfr(source);
        Block block = installed ? source.get() : fallback;
        return block(block, metadata, id, parsed, !installed);
    }

    private static ResolvedBlock efr(ModBlocks source, int metadata, String id, ParsedState parsed, Block fallback,
        int fallbackMetadata) {
        boolean installed = installedEfr(source);
        Block block = installed ? source.get() : fallback;
        return block(block, installed ? metadata : fallbackMetadata, id, parsed, !installed);
    }

    private static boolean installedEfr(ModBlocks source) {
        return installedEfr(source.isEnabled(), source.get());
    }

    static boolean installedEfr(boolean enabled, Block block) {
        return installedEfr(enabled, block, candidate -> {
            Object name = Block.blockRegistry.getNameForObject(candidate);
            return name != null && Block.blockRegistry.getObject(name) == candidate;
        });
    }

    static boolean installedEfr(boolean enabled, Block block,
        java.util.function.Predicate<Block> exactRegistryIdentity) {
        return enabled && block != null && exactRegistryIdentity.test(block);
    }

    private static ResolvedBlock stairs(ModBlocks source, Block fallback, String id, ParsedState parsed) {
        return efr(source, stairMetadata(parsed), id, parsed, fallback);
    }

    private static ResolvedBlock oldLog(int species, boolean wood, String id, ParsedState parsed) {
        return block(Blocks.log, species | (wood ? 12 : axisBits(parsed)), id, parsed);
    }

    private static ResolvedBlock newLog(int species, boolean wood, String id, ParsedState parsed) {
        return block(Blocks.log2, species | (wood ? 12 : axisBits(parsed)), id, parsed);
    }

    private static ResolvedBlock modernLog(ModBlocks source, int subtype, String id, ParsedState parsed) {
        return efr(source, subtype | axisBits(parsed), id, parsed, Blocks.log);
    }

    private static ResolvedBlock oldLeaves(Block leaves, int species, String id, ParsedState parsed) {
        return block(leaves, leafMetadata(species, parsed), id, parsed);
    }

    private static int leafMetadata(int species, ParsedState parsed) {
        return species | (Boolean.parseBoolean(parsed.properties.get("persistent")) ? 4 : 0);
    }

    private static int axisBits(ParsedState parsed) {
        return switch (parsed.properties.getOrDefault("axis", "y")) {
            case "x" -> 4;
            case "z" -> 8;
            case "none" -> 12;
            default -> 0;
        };
    }

    private static ResolvedBlock doublePlant(int species, String id, ParsedState parsed) {
        return block(Blocks.double_plant, "upper".equals(parsed.properties.get("half")) ? 8 : species, id, parsed);
    }

    private static int stairMetadata(ParsedState parsed) {
        int facing = switch (parsed.properties.getOrDefault("facing", "east")) {
            case "west" -> 1;
            case "south" -> 2;
            case "north" -> 3;
            default -> 0;
        };
        return facing | ("top".equals(parsed.properties.get("half")) ? 4 : 0);
    }

    private static int horizontalFacing(ParsedState parsed) {
        return switch (parsed.properties.getOrDefault("facing", "south")) {
            case "west" -> 1;
            case "north" -> 2;
            case "east" -> 3;
            default -> 0;
        };
    }

    private static int vineMetadata(ParsedState parsed) {
        int metadata = 0;
        if (Boolean.parseBoolean(parsed.properties.get("south"))) metadata |= 1;
        if (Boolean.parseBoolean(parsed.properties.get("west"))) metadata |= 2;
        if (Boolean.parseBoolean(parsed.properties.get("north"))) metadata |= 4;
        if (Boolean.parseBoolean(parsed.properties.get("east"))) metadata |= 8;
        return metadata;
    }

    private static int cocoaMetadata(ParsedState parsed) {
        int facing = switch (parsed.properties.getOrDefault("facing", "south")) {
            case "east" -> 1;
            case "north" -> 2;
            case "west" -> 3;
            default -> 0;
        };
        return facing | Math.min(2, age(parsed)) << 2;
    }

    private static int dripstoneMetadata(ParsedState parsed) {
        int thickness = switch (parsed.properties.getOrDefault("thickness", "tip")) {
            case "frustum" -> 1;
            case "middle" -> 2;
            case "base" -> 3;
            case "tip_merge" -> 4;
            default -> 0;
        };
        return thickness + ("up".equals(parsed.properties.get("vertical_direction")) ? 5 : 0);
    }

    private static int bambooMetadata(ParsedState parsed) {
        int metadata = "1".equals(parsed.properties.get("age")) ? 1 : 0;
        metadata |= switch (parsed.properties.getOrDefault("leaves", "none")) {
            case "small" -> 2;
            case "large" -> 4;
            default -> 0;
        };
        if ("1".equals(parsed.properties.get("stage"))) metadata |= 8;
        return metadata;
    }

    /**
     * EFR2.6.59 BlockAmethystCluster: type0 has small/medium, type1 large/cluster;
     * metadata is ForgeDirection ordinal plus6 for the second stage, never a bit mask.
     */
    record AmethystMapping(int family, int metadata) {}

    private static AmethystMapping amethystMapping(ParsedState parsed) {
        int family = switch (parsed.id) {
            case "minecraft:small_amethyst_bud", "minecraft:medium_amethyst_bud" -> 1;
            case "minecraft:large_amethyst_bud", "minecraft:amethyst_cluster" -> 2;
            default -> throw new IllegalArgumentException("Not an amethyst bud/cluster state");
        };
        int side = switch (parsed.properties.getOrDefault("facing", "up")) {
            case "down" -> 0;
            case "up" -> 1;
            case "north" -> 2;
            case "south" -> 3;
            case "west" -> 4;
            case "east" -> 5;
            default -> throw new IllegalArgumentException("Invalid amethyst attachment face");
        };
        boolean second = parsed.id.equals("minecraft:medium_amethyst_bud")
            || parsed.id.equals("minecraft:amethyst_cluster");
        return new AmethystMapping(family, side + (second ? 6 : 0));
    }

    static String amethystId(int family, int metadata) {
        if (metadata < 0 || metadata >= 12) throw new IllegalArgumentException("Invalid EFR amethyst metadata");
        if (family == 1) return metadata < 6 ? "minecraft:small_amethyst_bud" : "minecraft:medium_amethyst_bud";
        if (family == 2) return metadata < 6 ? "minecraft:large_amethyst_bud" : "minecraft:amethyst_cluster";
        throw new IllegalArgumentException("Not an EFR amethyst cluster family");
    }

    private static int mushroomMetadata(ParsedState parsed) {
        if (parsed.properties.containsKey("legacy_meta")) {
            if (parsed.properties.size() != 1)
                throw new IllegalArgumentException("Legacy mushroom metadata cannot mix with modern faces");
            int meta = Integer.parseInt(parsed.properties.get("legacy_meta"));
            if (meta < 0 || meta > 15) throw new IllegalArgumentException("Legacy mushroom metadata outside nibble");
            return meta;
        }
        // Modern huge-mushroom blocks default to a cap on every face. Terra omits that default state from the
        // serialized ID, while 1.7 metadata 14 is the equivalent all-cap model. An explicitly face-less cap is the
        // pore texture on every side (metadata 0), not the white stem texture (metadata 10).
        if (parsed.properties.isEmpty()) return 14;
        boolean hasFaceProperty = false;
        boolean anyFace = false;
        for (String face : new String[] { "up", "down", "north", "east", "south", "west" }) {
            String value = parsed.properties.get(face);
            if (value != null) {
                hasFaceProperty = true;
                anyFace |= Boolean.parseBoolean(value);
            }
        }
        if (hasFaceProperty && !anyFace && parsed.properties.size() >= 6) return 0;
        // Origen's small disks spell only down=false. All omitted directions retain the modern default of true, for
        // which 1.7's all-cap metadata is the closest representable state.
        return 14;
    }

    private static int fluidMetadata(ParsedState parsed) {
        return integer(parsed, "level", 0);
    }

    private static int campLegacyMetadata(ParsedState parsed, int... allowed) {
        if (parsed.properties.size() != 1 || !parsed.properties.containsKey("legacy_meta"))
            throw new IllegalArgumentException("Native camp state requires explicit legacy metadata: " + parsed.id);
        int metadata;
        try {
            metadata = Integer.parseInt(parsed.properties.get("legacy_meta"));
        } catch (NumberFormatException malformed) {
            throw new IllegalArgumentException("Malformed native camp metadata", malformed);
        }
        for (int candidate : allowed) if (metadata == candidate) return metadata;
        throw new IllegalArgumentException("Unsupported native camp metadata for " + parsed.id + ": " + metadata);
    }

    private static int snowMetadata(ParsedState parsed) {
        return Math.max(0, Math.min(7, integer(parsed, "layers", 1) - 1));
    }

    private static int slabMetadata(int material, ParsedState parsed) {
        return material | ("top".equals(parsed.properties.get("type")) ? 8 : 0);
    }

    private static int age(ParsedState parsed) {
        return integer(parsed, "age", 0);
    }

    private static int berries(ParsedState parsed) {
        return Boolean.parseBoolean(parsed.properties.get("berries")) ? 1 : 0;
    }

    private static int sculkVeinMetadata(ParsedState parsed) {
        int metadata = 0;
        if (bool(parsed, "north")) metadata |= 1;
        if (bool(parsed, "east")) metadata |= 2;
        if (bool(parsed, "south")) metadata |= 4;
        if (bool(parsed, "west")) metadata |= 8;
        return metadata;
    }

    private static boolean bool(ParsedState parsed, String property) {
        return Boolean.parseBoolean(parsed.properties.get(property));
    }

    /** EFR pink petals: petal count minus one, plus the facing in steps of four (south, west, north, east). */
    private static int petalMetadata(ParsedState parsed) {
        int amount = Math.max(1, Math.min(4, integer(parsed, "flower_amount", 1)));
        int facing = switch (String.valueOf(parsed.properties.get("facing"))) {
            case "west" -> 4;
            case "north" -> 8;
            case "east" -> 12;
            default -> 0;
        };
        return facing + amount - 1;
    }

    private static int integer(ParsedState parsed, String property, int fallback) {
        String value = parsed.properties.get(property);
        if (value == null || value.isEmpty()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int color(String id, String suffix) {
        String color = id.substring("minecraft:".length(), id.length() - suffix.length());
        Integer metadata = COLORS.get(color);
        if (metadata == null) throw new IllegalArgumentException("Unknown Minecraft color " + color);
        return metadata.intValue();
    }

    private static int coralMetadata(String id, String suffix) {
        String variant = id.substring("minecraft:".length(), id.length() - suffix.length());
        boolean dead = variant.startsWith("dead_");
        if (dead) variant = variant.substring("dead_".length());
        int metadata = switch (variant) {
            case "bubble" -> 1;
            case "fire" -> 2;
            case "horn" -> 3;
            case "tube" -> 4;
            default -> 0;
        };
        return dead ? metadata + 5 : metadata;
    }

    private static String coralId(int metadata, String suffix) {
        int normalized = metadata & 15;
        boolean dead = normalized >= 5 && normalized < 10;
        int color = dead ? normalized - 5 : normalized;
        String variant = switch (color) {
            case 1 -> "bubble";
            case 2 -> "fire";
            case 3 -> "horn";
            case 4 -> "tube";
            default -> "brain";
        };
        return "minecraft:" + (dead ? "dead_" : "") + variant + suffix;
    }

    private static Map<String, Integer> colors() {
        String[] names = { "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
            "cyan", "purple", "blue", "brown", "green", "red", "black" };
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (int index = 0; index < names.length; index++) result.put(names[index], Integer.valueOf(index));
        return Collections.unmodifiableMap(result);
    }

    private static ResolvedBlock block(Block block, int metadata, String id, ParsedState parsed) {
        return block(block, metadata, id, parsed, false);
    }

    private static ResolvedBlock block(Block block, int metadata, String id, ParsedState parsed, boolean substitute) {
        return new ResolvedBlock(block, metadata & 15, id, parsed.properties, substitute);
    }

    public static final class ResolvedBlock {

        private final Block block;
        private final int metadata;
        private final String canonicalId;
        private final Map<String, String> properties;
        private final boolean compatibilitySubstitute;

        private ResolvedBlock(Block block, int metadata, String canonicalId, Map<String, String> properties,
            boolean compatibilitySubstitute) {
            this.block = block;
            this.metadata = metadata;
            this.canonicalId = canonicalId;
            this.properties = properties;
            this.compatibilitySubstitute = compatibilitySubstitute;
        }

        public Block block() {
            return block;
        }

        public int metadata() {
            return metadata;
        }

        public String canonicalId() {
            return canonicalId;
        }

        public Map<String, String> properties() {
            return properties;
        }

        public boolean compatibilitySubstitute() {
            return compatibilitySubstitute;
        }

        boolean matches(Block candidate, int candidateMetadata) {
            return canonicalId.equals(canonicalBlockTypeId(candidate, candidateMetadata));
        }

    }

    private static final class ParsedState {

        private final String id;
        private final Map<String, String> properties;

        private ParsedState(String id, Map<String, String> properties) {
            this.id = id;
            this.properties = properties;
        }

        private static ParsedState parse(String state) {
            if (state == null) throw new IllegalArgumentException("Origen block state cannot be null");
            String normalized = state.trim();
            int bracket = normalized.indexOf('[');
            String id = (bracket < 0 ? normalized : normalized.substring(0, bracket)).toLowerCase(Locale.ROOT);
            if (bracket < 0) return new ParsedState(id, Collections.emptyMap());
            int end = normalized.lastIndexOf(']');
            String values = normalized.substring(bracket + 1, end < bracket ? normalized.length() : end);
            Map<String, String> properties = new LinkedHashMap<String, String>();
            for (String entry : values.split(",")) {
                int separator = entry.indexOf('=');
                if (separator > 0) {
                    properties.put(
                        entry.substring(0, separator)
                            .trim()
                            .toLowerCase(Locale.ROOT),
                        entry.substring(separator + 1)
                            .trim()
                            .toLowerCase(Locale.ROOT));
                }
            }
            return new ParsedState(id, Collections.unmodifiableMap(properties));
        }
    }
}
