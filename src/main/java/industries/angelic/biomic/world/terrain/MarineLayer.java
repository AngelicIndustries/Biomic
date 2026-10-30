package industries.angelic.biomic.world.terrain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.content.BiomicAquaticBlocks;
import industries.angelic.biomic.content.ports.PortBlocks;
import industries.angelic.biomic.world.WorldgenContext;

/**
 * The sea floor by its look (ecology proposal, oceans): every water column takes one of ten looks from its Origen
 * profile or hosted habitat, its depth and its climate, and is dressed with that look's plants in hashed patches.
 * Origen's and the hosted habitats' animated kelp and seagrass are not generated (creative-only, review decision). The
 * plants are decor slots (DecorSlots, decor.json): Biomic's are its ported seagrass and algae, and nothing for the
 * giant
 * kelp; the pack's overlay fills them with PMP's and BOP's.
 * <ul>
 * <li>flats: bare sand with gravel and clay disks, the odd sea-lettuce tuft;</li>
 * <li>meadows: dense eelgrass in patches on shallow sand and mud, sea grapes where warm;</li>
 * <li>kelp forests: giant kelp (a fifth of it yellow) reaching most of the way up, tangle beneath;</li>
 * <li>rocky reefs: andesite boulders on sand, rockweed in the shallows, coral weed and sea lettuce;</li>
 * <li>coral reefs: PMP corals between Origen's reef bodies, sea pickles;</li>
 * <li>lagoons: sea grapes, mozuku, sea pickles on warm shallow sand;</li>
 * <li>deep plains: gravel and clay, now and then a glowing coral cluster or yellow bamboo coral;</li>
 * <li>polar water: gravel over the sand, rockweed and tangle;</li>
 * <li>estuaries: mud with eelgrass;</li>
 * <li>fresh water: submerged plants by climate.</li>
 * </ul>
 */
final class MarineLayer {

    enum Look {
        FLATS,
        MEADOW,
        KELP,
        ROCKY,
        REEF,
        LAGOON,
        DEEP,
        POLAR,
        ESTUARY,
        FRESH
    }

    /** Origen's animated kelp and seagrass features, and the hosted habitats' vanilla ones. */
    private static final Set<String> REPLACED = Set.of("KELP", "SEAGRASS", "CALDERA_SEAGRASS", "MANGROVE_SEAGRASS");

    record Water(int x, int z, int floorY, int depth, String habitat, OrigenBiomeProfile profile, float temperature) {}

    private final long seed;
    private final int seaLevel;
    private final Map<String, int[]> placed = new TreeMap<>();

    MarineLayer(long seed, int physicalSeaLevel) {
        this.seed = seed;
        this.seaLevel = physicalSeaLevel;
    }

    /** Whether an Origen or hosted feature is replaced by this layer (and so not generated). */
    static boolean replaces(String feature) {
        return REPLACED.contains(feature) || feature.startsWith("minecraft:kelp")
            || feature.startsWith("minecraft:seagrass");
    }

    void populate(World world, List<Water> columns) {
        for (Water column : columns) {
            Look look = look(column);
            if (look != null) dress(world, column, look);
        }
    }

    /**
     * Shells on the strand line (sand or gravel at the water's edge) by climate: mussels on shingle and cold shores,
     * clams and cockles on temperate sand, pab and solonox shells, conches and nautiluses on warm sand, now and then a
     * turtle shell.
     */
    void strand(World world, String habitat, List<FloraLedger.Column> land) {
        boolean seabirds = habitat.equals("origen:TEMPERATE_SEA_ARCHES") || habitat.equals("origen:MARINE_MONOLITHS");
        for (FloraLedger.Column c : land) {
            int x = c.x(), z = c.z(), floor = c.floorY();
            Block ground = world.getBlock(x, floor, z);
            // Seabird cliffs: guano on the bare rock tops above the sea.
            if (seabirds && ground.getMaterial() == Material.rock
                && floor > seaLevel + 4
                && sprinkle(x, z, 0x67756e6fL, 0.03D)
                && world.getBlock(x, floor + 1, z) == Blocks.air) {
                world.setBlock(x, floor + 1, z, PortBlocks.SHELL, PortBlocks.GUANO, 2);
                count("biomic:port_shell:" + PortBlocks.GUANO);
                continue;
            }
            if (floor < seaLevel - 1 || floor > seaLevel + 1 || !sprinkle(x, z, 0x737472L, 0.075D)) continue;
            if (ground != Blocks.sand && ground != Blocks.gravel || world.getBlock(x, floor + 1, z) != Blocks.air
                || !nearWater(world, x, floor, z)) continue;
            float temperature = world.getBiomeGenForCoords(x, z).temperature;
            double u = unit(hash(x, z, 0x6b696e64L));
            int shell;
            if (u < 0.1D) shell = PortBlocks.DRIFTWOOD;
            else if (temperature < 0.9F && u < 0.3D) shell = PortBlocks.SEAWEED;
            else if (ground == Blocks.gravel || temperature <= 0.3F) shell = PortBlocks.MUSSEL;
            else if (temperature < 0.9F) shell = u < 0.65D ? PortBlocks.CLAM : PortBlocks.MOLLUSK;
            else shell = u < 0.4D ? PortBlocks.PAB
                : u < 0.8D ? PortBlocks.SOLONOX
                    : u < 0.89D ? PortBlocks.CONCH : u < 0.98D ? PortBlocks.NAUTILUS : PortBlocks.TURTLE;
            world.setBlock(x, floor + 1, z, PortBlocks.SHELL, shell, 2);
            if (world.getBlock(x, floor + 1, z) == PortBlocks.SHELL) count("biomic:port_shell:" + shell);
        }
    }

    /**
     * Life on the rock faces of a water column: mussel beds (Alex's Caves' mussels) in patches, barnacles near the
     * surface, from the floor up to six blocks.
     */
    private void walls(World world, Water c, boolean barnacles) {
        int top = Math.min(c.floorY + 6, c.floorY + c.depth);
        boolean bed = patch(c.x, c.z, 0x6d757373L, 16, 0.45D, 2, 4, 0.7D);
        for (int y = c.floorY + 1; y <= top; y++) {
            if (world.getBlock(c.x, y, c.z) != Blocks.water || world.getBlockMetadata(c.x, y, c.z) != 0) continue;
            for (int side = 0; side < 4; side++) {
                int[] step = industries.angelic.biomic.content.block.BlockAquaticPlant.WALL_STEPS[side];
                if (world.getBlock(c.x + step[0], y, c.z + step[1])
                    .getMaterial() != Material.rock) continue;
                long h = hash(c.x * 31L + y, c.z, side);
                if (bed && unit(h) < 0.5D) {
                    // The mussel faces away from its rock (ForgeDirection: north 2, south 3, west 4, east 5).
                    int facing = new int[] { 3, 2, 5, 4 }[side];
                    Block mussel = industries.angelic.biomic.content.alexscaves.AbyssalContentBlocks
                        .mussel(1 + (int) Math.floorMod(h >>> 8, 5L), true);
                    world.setBlock(c.x, y, c.z, mussel, facing, 2);
                    count("mussel bed");
                    break;
                }
                if (barnacles && c.floorY + c.depth - y <= 4 && unit(h >>> 16) < 0.25D) {
                    world.setBlock(c.x, y, c.z, PortBlocks.BARNACLES, side + 4 * (int) Math.floorMod(h >>> 24, 4L), 2);
                    count("biomic:port_barnacles");
                    break;
                }
            }
        }
    }

    private static boolean nearWater(World world, int x, int y, int z) {
        for (int d = 1; d <= 2; d++) for (int[] side : Trunks.SIDES)
            for (int dy = -1; dy <= 0; dy++) if (world.getBlock(x + side[0] * d, y + dy, z + side[1] * d)
                .getMaterial() == Material.water) return true;
        return false;
    }

    Look look(Water c) {
        int surface = c.floorY + c.depth;
        boolean sea = surface >= seaLevel - 1 && surface <= seaLevel + 1 && c.depth >= 3;
        boolean warm = c.temperature >= 0.9F, cold = c.temperature <= 0.2F;
        String h = c.habitat;
        if (h != null) {
            if (h.contains("river") || !sea) return Look.FRESH;
            if (h.equals("regions_unexplored:rocky_reef")) return c.depth > 12 ? Look.KELP : Look.ROCKY;
            if (h.equals("regions_unexplored:hyacinth_deeps")) return c.depth > 18 ? Look.DEEP : Look.MEADOW;
            if (h.equals("biomesoplenty:kelp_forest")) return Look.KELP;
            if (h.equals("biomesoplenty:coral_reef")) return Look.REEF;
            if (h.contains("mangrove") || h.contains("marsh") || h.contains("bayou") || h.contains("swamp"))
                return Look.ESTUARY;
            return ocean(c, warm, cold);
        }
        if (c.profile == null) return sea ? ocean(c, warm, cold) : Look.FRESH;
        String p = c.profile.name();
        if (p.contains("RIVER")) return Look.FRESH;
        return switch (p) {
            case "OCEAN" -> ocean(c, warm, cold);
            case "DEEP_OCEAN" -> c.depth > 30 ? Look.DEEP : Look.FLATS;
            case "ABYSSAL_ALLEYS", "TROPICAL_DEEP_OCEAN" -> Look.DEEP;
            case "CORAL_OCEAN" -> c.depth <= 6 ? Look.LAGOON : Look.REEF;
            case "TROPICAL_OCEAN" -> c.depth <= 6 ? Look.LAGOON
                : mix(c, Look.REEF, 0.45D, Look.MEADOW, 0.3D, Look.LAGOON);
            case "FROZEN_OCEAN", "ICEBERG_OCEAN", "ARCTIC_ARCHES" -> Look.POLAR;
            case "TEMPERATE_SEA_ARCHES", "STONEGATE_SEAS", "MARINE_MONOLITHS" -> c.depth <= 15 ? Look.ROCKY : Look.KELP;
            case "BEACH" -> c.depth >= 2 ? Look.MEADOW : Look.FLATS;
            case "PALM_BEACH" -> Look.LAGOON;
            case "SHALE_BEACH" -> Look.ROCKY;
            case "MANGROVE_SWAMP" -> Look.ESTUARY;
            default -> sea ? ocean(c, warm, cold) : Look.FRESH;
        };
    }

    /** The open sea: polar when cold, else flats with meadows on the shelf and kelp forests where temperate. */
    private Look ocean(Water c, boolean warm, boolean cold) {
        if (cold && c.temperature <= 0.0F) return Look.POLAR;
        if (c.depth > 30) return Look.DEEP;
        if (warm) return c.depth <= 6 ? Look.LAGOON : mix(c, Look.REEF, 0.35D, Look.MEADOW, 0.35D, Look.FLATS);
        Look regional = mix(c, Look.FLATS, 0.7D, Look.MEADOW, 0.15D, Look.KELP);
        if (regional == Look.MEADOW && c.depth > 12) return Look.FLATS;
        if (regional == Look.KELP && c.depth < 8) return Look.FLATS;
        return regional;
    }

    /** One of three looks by a regional field (64-block cells): the first with share a, the second with share b. */
    private Look mix(Water c, Look first, double a, Look second, double b, Look third) {
        // Regions of about a hundred blocks, warped so their borders are not grid lines.
        int wx = c.x + (int) ((noise(c.x, c.z, 0x77783aL, 48) - 0.5D) * 64);
        int wz = c.z + (int) ((noise(c.z, c.x, 0x777a3aL, 48) - 0.5D) * 64);
        double u = unit(hash(Math.floorDiv(wx, 96), Math.floorDiv(wz, 96), 0x6d6978L));
        return u < a ? first : u < a + b ? second : third;
    }

    private void dress(World world, Water c, Look look) {
        int x = c.x, z = c.z, floor = c.floorY, y = floor + 1;
        Block ground = world.getBlock(x, floor, z);
        boolean soft = ground == Blocks.sand || ground == Blocks.gravel
            || ground == Blocks.dirt
            || ground == Blocks.clay
            || ground.getMaterial() == Material.ground
            || ground.getMaterial() == Material.sand;
        boolean rock = ground.getMaterial() == Material.rock;
        switch (look) {
            case FLATS -> {
                if (ground == Blocks.sand && disk(x, z, 0x677276L, 40, 0.3D, 3, 7))
                    set(world, x, floor, z, Blocks.gravel, 0);
                else if (ground == Blocks.sand && disk(x, z, 0x636c6179L, 48, 0.25D, 2, 5))
                    set(world, x, floor, z, Blocks.clay, 0);
                else if (soft && sprinkle(x, z, 0x6c6574L, 0.004D))
                    plant(world, DecorSlots.marine("sea_lettuce"), x, y, z, 0);
                else if (ground == Blocks.sand && c.depth <= 12 && sprinkle(x, z, 0x7368656cL, 0.003D)) port(
                    world,
                    PortBlocks.SEA_SHELL,
                    sprinkle(x, z, 0x636c616dL, 0.5D) ? PortBlocks.CLAM : PortBlocks.MOLLUSK,
                    x,
                    y,
                    z);
            }
            case MEADOW -> {
                if (!soft || c.depth < 2 || c.depth > 12) return;
                boolean warmMeadow = c.temperature >= 0.9F;
                if (warmMeadow && patch(x, z, 0x67726170L, 24, 0.4D, 2, 4, 0.6D))
                    plant(world, DecorSlots.marine("sea_grapes"), x, y, z, 0);
                else if (patch(x, z, 0x65656cL, 32, 0.65D, 4, 9, 0.72D)) {
                    // A matted floor under part of the meadow (Tropicraft's matted eel grass).
                    if (ground == Blocks.sand && sprinkle(x, z, 0x6d617474L, 0.4D))
                        set(world, x, floor, z, PortBlocks.MATTED_SEAGRASS, 0);
                    // Warm meadows are turtle grass with a third manatee grass (ported), temperate ones eelgrass.
                    if (!warmMeadow) plant(world, DecorSlots.marine("eelgrass"), x, y, z, 0);
                    else port(
                        world,
                        PortBlocks.SEAGRASS,
                        (sprinkle(x, z, 0x6d616eL, 0.3D) ? PortBlocks.MANATEE_GRASS : PortBlocks.TURTLE_GRASS)
                            + (int) Math.floorMod(hash(x, z, 0x76617269L), 4L),
                        x,
                        y,
                        z);
                }
            }
            case KELP -> {
                if (!soft && !rock) return;
                if (patch(x, z, 0x6b656c70L, 40, 0.75D, 8, 16, 0.22D)) {
                    long h = hash(x, z, 0x6b6c70L);
                    String kelp = unit(h) < 0.2D ? DecorSlots.marine("kelp_yellow") : DecorSlots.marine("kelp_green");
                    int height = (int) Math.round(c.depth * (0.6D + 0.3D * unit(WorldgenContext.mix64(h))));
                    stalk(world, kelp, x, y, z, Math.max(1, Math.min(height, c.depth - 1)));
                } else if (patch(x, z, 0x6b656c70L, 40, 0.75D, 8, 16, 1.0D) && sprinkle(x, z, 0x74616eL, 0.15D))
                    plant(world, DecorSlots.marine("tangle"), x, y, z, 0);
            }
            case ROCKY -> {
                walls(world, c, true);
                if (ground == Blocks.sand && boulder(world, c)) return;
                if (rock && c.depth <= 1 && c.temperature < 0.9F && sprinkle(x, z, 0x70616c6dL, 0.06D))
                    port(world, PortBlocks.ALGAE, PortBlocks.SEA_PALM, x, y, z);
                else if (c.depth <= 2 && sprinkle(x, z, 0x67757477L, 0.2D))
                    port(world, PortBlocks.ALGAE, PortBlocks.GUTWEED, x, y, z);
                else if (rock && c.depth <= 4 && sprinkle(x, z, 0x726f636bL, 0.5D))
                    plant(world, DecorSlots.marine("rockweed"), x, y, z, 0);
                else if (rock && c.temperature < 0.9F && sprinkle(x, z, 0x72656461L, 0.08D))
                    port(world, PortBlocks.ALGAE, PortBlocks.RED_ALGAE, x, y, z);
                else if (rock && sprinkle(x, z, 0x75726368L, 0.004D))
                    port(world, PortBlocks.SEA_SHELL, PortBlocks.URCHIN, x, y, z);
                else if (rock && patch(x, z, 0x77656564L, 12, 0.5D, 1, 2, 0.6D))
                    plant(world, DecorSlots.marine("coral_weed"), x, y, z, 0);
                else if (sprinkle(x, z, 0x6c6574L, 0.03D)) plant(world, DecorSlots.marine("sea_lettuce"), x, y, z, 0);
            }
            case REEF -> {
                if (c.depth < 2) return;
                if (ground == Blocks.sand && disk(x, z, 0x63736e64L, 32, 0.35D, 2, 5)) {
                    set(world, x, floor, z, PortBlocks.CORAL_SAND, 0);
                    ground = PortBlocks.CORAL_SAND;
                }
                reef(world, c, floor, soft || rock, c.depth <= 18 ? 0.55D : 0.2D, 0.6D);
            }
            case LAGOON -> {
                if (!soft && !rock) return;
                if (c.depth >= 2 && reefPatch(x, z, 0x6c61676fL, 24, 0.3D) && reef(world, c, floor, true, 0.35D, 0.3D))
                    return;
                if (ground == Blocks.sand && disk(x, z, 0x63736e64L, 32, 0.3D, 2, 5))
                    set(world, x, floor, z, PortBlocks.CORAL_SAND, 0);
                if (soft && patch(x, z, 0x67726170L, 20, 0.55D, 2, 4, 0.6D))
                    plant(world, DecorSlots.marine("sea_grapes"), x, y, z, 0);
                else if (soft && c.depth <= 4 && patch(x, z, 0x6c616773L, 16, 0.5D, 2, 5, 0.65D)) {
                    boolean fern = sprinkle(x, z, 0x7369636bL, 0.5D);
                    if (ground == Blocks.sand && sprinkle(x, z, 0x6d617474L, 0.4D))
                        set(world, x, floor, z, PortBlocks.MATTED_SEAGRASS, fern ? 1 : 2);
                    port(
                        world,
                        PortBlocks.SEAGRASS,
                        fern ? PortBlocks.FERN_SEAGRASS : PortBlocks.SICKLE_SEAGRASS,
                        x,
                        y,
                        z);
                } else if (soft && sprinkle(x, z, 0x7368656cL, 0.006D)) {
                    int[] shells = { PortBlocks.CONCH, PortBlocks.NAUTILUS, PortBlocks.URCHIN };
                    port(
                        world,
                        PortBlocks.SEA_SHELL,
                        shells[(int) Math.floorMod(hash(x, z, 0x6b696e64L), 3L)],
                        x,
                        y,
                        z);
                } else if (c.depth >= 2 && sprinkle(x, z, 0x73617267L, 0.004D))
                    port(world, PortBlocks.ALGAE, PortBlocks.SARGASSUM, x, y, z);
                else if (rock && sprinkle(x, z, 0x6d6f7aL, 0.25D))
                    plant(world, DecorSlots.marine("mozuku"), x, y, z, 0);
                else if (sprinkle(x, z, 0x7069636bL, 0.012D)) pickles(world, x, y, z);
            }
            case DEEP -> {
                if (ground == Blocks.sand && disk(x, z, 0x677276L, 40, 0.35D, 3, 7))
                    set(world, x, floor, z, Blocks.gravel, 0);
                else if (ground == Blocks.sand && disk(x, z, 0x636c6179L, 48, 0.3D, 2, 5))
                    set(world, x, floor, z, Blocks.clay, 0);
                else if (c.depth > 30 && patch(x, z, 0x676c6f77L, 48, 0.2D, 1, 2, 0.6D))
                    plant(world, DecorSlots.marine("glow_coral"), x, y, z, -1);
                else if (c.depth > 30 && patch(x, z, 0x62616d62L, 40, 0.25D, 1, 2, 0.5D))
                    plant(world, DecorSlots.marine("bamboo_coral"), x, y, z, 0);
            }
            case POLAR -> {
                if (ground == Blocks.sand && disk(x, z, 0x677276L, 24, 0.6D, 3, 8))
                    set(world, x, floor, z, Blocks.gravel, 0);
                else if ((rock || ground == Blocks.gravel) && sprinkle(x, z, 0x706f6cL, 0.2D)) plant(
                    world,
                    c.depth <= 4 ? DecorSlots.marine("rockweed") : DecorSlots.marine("tangle"),
                    x,
                    y,
                    z,
                    0);
            }
            case ESTUARY -> {
                Block mud = Block.getBlockFromName("etfuturum:mud");
                if (soft && mud != null && mud != Blocks.air && disk(x, z, 0x6d7564L, 24, 0.55D, 3, 6)) set(
                    world,
                    x,
                    floor,
                    z,
                    c.temperature >= 0.8F && disk(x, z, 0x7069616eL, 12, 0.4D, 1, 3) ? PortBlocks.PIANGUAS_MUD : mud,
                    0);
                else if (soft && sprinkle(x, z, 0x636c616dL, 0.005D))
                    port(world, PortBlocks.SEA_SHELL, PortBlocks.CLAM, x, y, z);
                else if (c.depth <= 6 && patch(x, z, 0x6f797374L, 20, 0.4D, 1, 3, 0.5D))
                    port(world, PortBlocks.SEA_SHELL, PortBlocks.OYSTER, x, y, z);
                else if (soft && c.depth >= 1 && patch(x, z, 0x65656cL, 20, 0.5D, 2, 5, 0.65D))
                    plant(world, DecorSlots.marine("eelgrass"), x, y, z, 0);
                walls(world, c, false);
            }
            case FRESH -> {
                if (c.depth < 2 || c.depth > 8 || !soft) return;
                List<String> plants = DecorSlots.marineList(
                    c.temperature >= 0.9F ? "fresh_warm" : c.temperature <= 0.3F ? "fresh_cold" : "fresh_temperate");
                for (String id : plants) if (patch(x, z, id.hashCode(), 24, 0.35D, 2, 4, 0.55D)) {
                    plant(world, id, x, y, z, 0);
                    break;
                }
            }
        }
    }

    /**
     * A living reef in this column: coral-block bommies (one colour each, one to four high, a coral on top), and
     * between them a cover of Biomic's corals and fans, PMP's and Mariculture's corals in zones, sun polyps under
     * overhangs, sea pickles here and there. {@code cover} is the share of the floor grown over, {@code bommies} the
     * presence of bommie centres. True when something grew.
     */
    private boolean reef(World world, Water c, int floor, boolean ground, double cover, double bommies) {
        int x = c.x, z = c.z, y = floor + 1;
        if (!ground) return false;
        int height = bommie(c, bommies);
        if (height > 0) {
            int colour = (int) Math.floorMod(hash(Math.floorDiv(x, 6), Math.floorDiv(z, 6), 0x626f6d6dL), 5L);
            for (int i = 0; i < height; i++) set(world, x, y + i, z, BiomicAquaticBlocks.CORAL_BLOCK, colour);
            count("biomic:coral_block");
            if (sprinkle(x, z, 0x746f70L, 0.6D)) coral(world, x, y + height, z, true);
            return true;
        }
        if (overhang(world, x, y, z) && sprinkle(x, z, 0x73756eL, 0.5D)) {
            plant(world, DecorSlots.marine("sun_polyps"), x, y, z, 0);
            return true;
        }
        if (!sprinkle(x, z, 0x636f766572L, cover)) return false;
        coral(world, x, y, z, false);
        return true;
    }

    /** One coral of the zone's kinds on the floor or a bommie top; sea pickles now and then. */
    private void coral(World world, int x, int y, int z, boolean onBommie) {
        if (sprinkle(x, z, 0x7069636bL, 0.03D)) {
            pickles(world, x, y, z);
            return;
        }
        // Zones of about eight blocks pick a family; within a zone two kinds mix.
        long zone = hash(
            Math.floorDiv(x + (int) (noise(x, z, 0x77617270L, 6) * 6), 8),
            Math.floorDiv(z + (int) (noise(z, x, 0x77617270L, 6) * 6), 8),
            0x7a6f6e65L);
        int family = (int) Math.floorMod(zone, 3L);
        int kind = (int) Math.floorMod(zone >>> 8, 7L) + (sprinkle(x, z, 0x6d6978L, 0.35D) ? 1 : 0);
        // The second family is the decor's corals (PMP's for the pack); without them, Biomic's own.
        List<String> corals = DecorSlots.marineList("corals");
        if (family == 0 || onBommie || family == 1 && corals.isEmpty()) {
            Block block = sprinkle(x, z, 0x66616eL, 0.35D) ? BiomicAquaticBlocks.CORAL_FAN
                : BiomicAquaticBlocks.CORAL_PLANT;
            if (world.getBlock(x, y, z)
                .getMaterial() != Material.water || !block.canPlaceBlockAt(world, x, y, z)) return;
            world.setBlock(x, y, z, block, kind % 5, 2);
            count(Block.blockRegistry.getNameForObject(block));
        } else if (family == 1) plant(world, corals.get(kind % corals.size()), x, y, z, 0);
        else port(world, PortBlocks.CORAL, kind % 7, x, y, z);
    }

    /** Height of the bommie this column belongs to (0 when none): mounds of radius one to two and a half. */
    private int bommie(Water c, double presence) {
        int cell = 9, cx = Math.floorDiv(c.x, cell), cz = Math.floorDiv(c.z, cell);
        for (int gx = cx - 1; gx <= cx + 1; gx++) for (int gz = cz - 1; gz <= cz + 1; gz++) {
            long h = hash(gx, gz, 0x626f6dL);
            if (unit(h) >= presence) continue;
            long j = WorldgenContext.mix64(h);
            int bx = gx * cell + (int) Math.floorMod(j, (long) cell),
                bz = gz * cell + (int) Math.floorMod(j >>> 16, (long) cell);
            double radius = 1 + 1.5D * unit(WorldgenContext.mix64(j));
            double d = Math.sqrt((c.x - bx) * (c.x - bx) + (c.z - bz) * (c.z - bz));
            if (d > radius) continue;
            int top = 1 + (int) Math.round(3 * unit(j >>> 32) * (1 - d / (radius + 0.5D)));
            return Math.max(0, Math.min(top, c.depth - 2));
        }
        return 0;
    }

    private boolean reefPatch(int x, int z, long salt, int cell, double presence) {
        return patch(x, z, salt, cell, presence, 3, 7, 1.0D);
    }

    /** Rock or a bommie within three blocks above: a shaded spot for sun polyps. */
    private static boolean overhang(World world, int x, int y, int z) {
        for (int up = 1; up <= 3; up++) {
            Block above = world.getBlock(x, y + up, z);
            if (above.getMaterial() == Material.rock || above == BiomicAquaticBlocks.CORAL_BLOCK) return true;
            if (above.getMaterial() != Material.water) return false;
        }
        return false;
    }

    /** Smooth value noise in 0..1 on a {@code scale}-block lattice. */
    private double noise(int x, int z, long salt, double scale) {
        double fx = x / scale, fz = z / scale;
        int x0 = (int) Math.floor(fx), z0 = (int) Math.floor(fz);
        double tx = fx - x0, tz = fz - z0;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double a = unit(hash(x0, z0, salt)), b = unit(hash(x0 + 1, z0, salt)), cc = unit(hash(x0, z0 + 1, salt)),
            d = unit(hash(x0 + 1, z0 + 1, salt));
        return (a + (b - a) * tx) * (1 - tz) + (cc + (d - cc) * tx) * tz;
    }

    /** An andesite boulder of radius one to three on the sand; true when this column is part of one. */
    private boolean boulder(World world, Water c) {
        int cell = 20, cx = Math.floorDiv(c.x, cell), cz = Math.floorDiv(c.z, cell);
        for (int gx = cx - 1; gx <= cx + 1; gx++) for (int gz = cz - 1; gz <= cz + 1; gz++) {
            long h = hash(gx, gz, 0x626f756cL);
            if (unit(h) >= 0.4D) continue;
            long j = WorldgenContext.mix64(h);
            int bx = gx * cell + (int) Math.floorMod(j, (long) cell),
                bz = gz * cell + (int) Math.floorMod(j >>> 16, (long) cell);
            double radius = 1 + 2 * unit(WorldgenContext.mix64(j));
            double d = Math.sqrt((c.x - bx) * (c.x - bx) + (c.z - bz) * (c.z - bz));
            if (d > radius) continue;
            int height = Math.min(c.depth - 1, (int) Math.round(radius - d + 0.5D));
            for (int i = 1; i <= height; i++) set(world, c.x, c.floorY + i, c.z, Blocks.stone, 5);
            return height > 0;
        }
        return false;
    }

    private boolean disk(int x, int z, long salt, int cell, double presence, int minRadius, int maxRadius) {
        return patch(x, z, salt, cell, presence, minRadius, maxRadius, 1.0D);
    }

    private boolean patch(int x, int z, long salt, int cell, double presence, int minRadius, int maxRadius,
        double fill) {
        int cx = Math.floorDiv(x, cell), cz = Math.floorDiv(z, cell);
        for (int gx = cx - 1; gx <= cx + 1; gx++) for (int gz = cz - 1; gz <= cz + 1; gz++) {
            long h = hash(gx, gz, salt);
            if (unit(h) >= presence) continue;
            long j = WorldgenContext.mix64(h);
            int px = gx * cell + (int) Math.floorMod(j, (long) cell),
                pz = gz * cell + (int) Math.floorMod(j >>> 20, (long) cell);
            double radius = minRadius + (maxRadius - minRadius) * unit(WorldgenContext.mix64(j));
            double dx = x - px, dz = z - pz;
            if (Math.sqrt(dx * dx + dz * dz) > radius * (0.85D + 0.3D * unit(hash(x, z, salt ^ 0x52L)))) continue;
            if (fill >= 1.0D || unit(hash(x, z, salt ^ 0x46L)) < fill) return true;
        }
        return false;
    }

    private boolean sprinkle(int x, int z, long salt, double chance) {
        return unit(hash(x, z, salt)) < chance;
    }

    private void set(World world, int x, int y, int z, Block block, int meta) {
        world.setBlock(x, y, z, block, meta, 2);
    }

    /** A plant in the water cell; PMP's own canPlaceBlockAt decides. Metadata -1 takes the palette id's. */
    private void plant(World world, String id, int x, int y, int z, int meta) {
        FloraPlants.Plant plant = FloraPlants.resolve(id);
        if (plant == null || world.getBlock(x, y, z)
            .getMaterial() != Material.water
            || world.getBlock(x, y + 1, z)
                .getMaterial() != Material.water)
            return;
        if (!plant.block.canPlaceBlockAt(world, x, y, z)) return;
        // A slot's own metadata (a ported plant's variant) wins over the look's.
        world.setBlock(x, y, z, plant.block, plant.metadata >= 0 ? plant.metadata : Math.max(0, meta), 2);
        if (world.getBlock(x, y, z) == plant.block) count(id);
    }

    /** A ported water plant or shell in the water cell, on a solid floor. */
    private void port(World world, Block block, int meta, int x, int y, int z) {
        if (world.getBlock(x, y, z)
            .getMaterial() != Material.water || world.getBlockMetadata(x, y, z) != 0
            || !World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)) return;
        world.setBlock(x, y, z, block, meta, 2);
        count(Block.blockRegistry.getNameForObject(block) + ":" + meta);
    }

    private void stalk(World world, String id, int x, int y, int z, int height) {
        FloraPlants.Plant plant = FloraPlants.resolve(id);
        if (plant == null || !plant.block.canPlaceBlockAt(world, x, y, z)) return;
        for (int i = 0; i < height; i++) {
            if (world.getBlock(x, y + i, z)
                .getMaterial() != Material.water
                || world.getBlock(x, y + i + 1, z)
                    .getMaterial() != Material.water)
                break;
            world.setBlock(x, y + i, z, plant.block, 0, 2);
        }
        count(id);
    }

    private void pickles(World world, int x, int y, int z) {
        if (world.getBlock(x, y, z)
            .getMaterial() != Material.water) return;
        world.setBlock(x, y, z, BiomicAquaticBlocks.SEA_PICKLE, (int) Math.floorMod(hash(x, z, 0x6e756dL), 4L), 2);
        count("biomic:sea_pickle");
    }

    private void count(String what) {
        placed.computeIfAbsent(what, key -> new int[1])[0]++;
    }

    String report() {
        StringBuilder out = new StringBuilder("Marine layer:");
        placed.forEach(
            (what, n) -> out.append(' ')
                .append(what)
                .append('=')
                .append(n[0]));
        return out.toString();
    }

    private long hash(long a, long b, long salt) {
        return FloraLedger.hash(seed, a, b, salt);
    }

    private static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }
}
