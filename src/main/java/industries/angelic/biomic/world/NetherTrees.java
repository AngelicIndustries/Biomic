package industries.angelic.biomic.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import cpw.mods.fml.common.Loader;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.biome.TartarusBiomes;
import industries.angelic.biomic.compat.food.WildCrops;

/**
 * Natura's nether trees in Tartarus. Natura only plants them in biomes named Hell, Boneyard, Phantasmagoric Inferno or
 * Corrupted Sands, so on its own it grows none here; Biomic grows each where it fits: bloodwood hanging from the
 * ceilings of the red forests, ghostwood in Soul Valley and the Lifeless Spires, darkwood in the warped and
 * crystalline forests, fusewood in the fiery wastes, glowshroom clusters in the pale undergrowth and the veldts.
 * About one tree of a kind per six chunks of its biomes, wholly inside the chunk. Server thread only.
 */
final class NetherTrees {

    private enum Kind {

        BLOODWOOD("BloodTreeLargeGen", true, 5, 1 / 5.0D, TartarusBiomes.BLOODY_REMNANTS,
            TartarusBiomes.CRIMSON_FOREST),
        GHOSTWOOD("WhiteTreeGen", false, 5, 1 / 6.0D, TartarusBiomes.SOUL_VALLEY, TartarusBiomes.LIFELESS_SPIRES),
        DARKWOOD("DarkwoodGen", false, 3, 1 / 5.0D, TartarusBiomes.WARPED_FOREST, TartarusBiomes.CRYSTALLINE_VELDTS),
        FUSEWOOD("FusewoodGen", false, 3, 1 / 6.0D, TartarusBiomes.FLAMING_HELLSCAPES, TartarusBiomes.TORRID_CRAGS),
        GLOWSHROOM("GlowshroomGenBlueGreen", false, 4, 1 / 4.0D, TartarusBiomes.BLOODLESS_UNDERGROWTH,
            TartarusBiomes.CRYSTALLINE_VELDTS);

        final String generator;
        final boolean ceiling;
        final int margin;
        final double chance;
        final Set<TartarusBiomes> biomes;

        Kind(String generator, boolean ceiling, int margin, double chance, TartarusBiomes... biomes) {
            this.generator = generator;
            this.ceiling = ceiling;
            this.margin = margin;
            this.chance = chance;
            this.biomes = Set.of(biomes);
        }
    }

    private static final int HEIGHT = 255;

    private final long seed;
    private final Map<Kind, WorldGenerator> generators = new java.util.EnumMap<>(Kind.class);
    private WorldGenerator purpleGlowshroom;
    private boolean resolved;
    private final Map<String, int[]> placed = new TreeMap<>();
    private final Map<String, String> spots = new TreeMap<>();

    NetherTrees(long seed) {
        this.seed = seed;
    }

    void populate(World world, int chunkX, int chunkZ) {
        if (!Loader.isModLoaded("Natura") || !resolve()) return;
        TartarusBiomes[] columns = new TartarusBiomes[256];
        for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) columns[lx * 16 + lz] = TartarusBiomes
            .fromBiomeId(world.getBiomeGenForCoords(chunkX * 16 + lx, chunkZ * 16 + lz).biomeID);
        for (Kind kind : Kind.values()) {
            WorldGenerator generator = generators.get(kind);
            if (generator == null) continue;
            long h = hash(chunkX, chunkZ, kind.ordinal() * 0x51L + 0x6e61747572L);
            if (unit(h) >= kind.chance) continue;
            List<long[]> candidates = new ArrayList<>();
            for (int lx = kind.margin; lx < 16 - kind.margin; lx++)
                for (int lz = kind.margin; lz < 16 - kind.margin; lz++)
                    // Neighbour mods' biomes in the Tartarus map are not Tartarus biomes (null).
                    if (columns[lx * 16 + lz] != null && kind.biomes.contains(columns[lx * 16 + lz])) candidates.add(
                        new long[] { hash(chunkX * 16 + lx, chunkZ * 16 + lz, h), chunkX * 16 + lx, chunkZ * 16 + lz });
            candidates.sort((a, b) -> Long.compare(a[0], b[0]));
            for (int i = 0; i < candidates.size() && i < 16; i++) {
                int x = (int) candidates.get(i)[1], z = (int) candidates.get(i)[2];
                WorldGenerator tree = kind == Kind.GLOWSHROOM && unit(h >>> 7) < 0.35D && purpleGlowshroom != null
                    ? purpleGlowshroom
                    : generator;
                if (grow(world, kind, tree, x, z, h)) break;
            }
        }
    }

    /** On the lowest open nether floor of the column with room (netherrack under the trunk), or under a ceiling. */
    private boolean grow(World world, Kind kind, WorldGenerator generator, int x, int z, long hash) {
        for (int y = 2; y < HEIGHT - 12; y++) {
            Block floor = world.getBlock(x, y - 1, z);
            if (!WildCrops.netherGround(floor) || world.getBlock(x, y, z) != Blocks.air) continue;
            boolean room = true;
            for (int up = 1; up <= 8 && room; up++) room = world.getBlock(x, y + up, z) == Blocks.air;
            if (!room) continue;
            if (kind.ceiling) {
                // Bloodwood finds the first nether ceiling above this open floor and hangs from it.
                if (generator.generate(world, new Random(hash), x, y + 4, z)) return count(kind, x, y, z);
                continue;
            }
            int floorMeta = world.getBlockMetadata(x, y - 1, z);
            boolean swap = floor != Blocks.netherrack && floor != Blocks.soul_sand;
            if (swap) world.setBlock(x, y - 1, z, Blocks.netherrack, 0, 2);
            if (generator.generate(world, random(generator, hash), x, y, z)) return count(kind, x, y, z);
            if (swap) world.setBlock(x, y - 1, z, floor, floorMeta, 2);
        }
        return false;
    }

    /**
     * The generator's random. Natura's purple glowshroom takes its height from the first nextInt(4) + 1, and at height
     * 1 or 2 its two cap layers sit on the ground with little or no stem (review, 2026-10-24: "doesn't have a stem"):
     * the seed moves on until the height is 3 or 4.
     */
    private Random random(WorldGenerator generator, long hash) {
        if (generator != purpleGlowshroom) return new Random(hash);
        long seed = hash;
        while (new Random(seed).nextInt(4) < 2) seed++;
        return new Random(seed);
    }

    private boolean count(Kind kind, int x, int y, int z) {
        String what = kind.name()
            .toLowerCase(java.util.Locale.ROOT);
        placed.computeIfAbsent(what, key -> new int[1])[0]++;
        spots.putIfAbsent(what, x + " " + y + " " + z);
        return true;
    }

    String report() {
        StringBuilder out = new StringBuilder("Natura nether trees:");
        placed.forEach(
            (what, n) -> out.append(' ')
                .append(what)
                .append(' ')
                .append(n[0])
                .append(" (")
                .append(spots.get(what))
                .append(')'));
        return out.toString();
    }

    private boolean resolve() {
        if (resolved) return !generators.isEmpty();
        resolved = true;
        try {
            for (Kind kind : Kind.values()) {
                Class<?> type = Class.forName("mods.natura.worldgen." + kind.generator);
                WorldGenerator generator = switch (kind) {
                    case BLOODWOOD -> (WorldGenerator) type.getConstructor(int.class, int.class)
                        .newInstance(3, 2);
                    case GHOSTWOOD -> (WorldGenerator) type.getConstructor(boolean.class, int.class, int.class)
                        .newInstance(false, 2, 1);
                    case DARKWOOD -> (WorldGenerator) type.getConstructor(boolean.class, int.class, int.class)
                        .newInstance(false, 3, 0);
                    case FUSEWOOD -> (WorldGenerator) type.getConstructor(boolean.class, int.class, int.class)
                        .newInstance(false, 3, 1);
                    case GLOWSHROOM -> (WorldGenerator) type.getConstructor(boolean.class)
                        .newInstance(false);
                };
                generators.put(kind, generator);
            }
            purpleGlowshroom = (WorldGenerator) Class.forName("mods.natura.worldgen.GlowshroomGenPurple")
                .getConstructor(boolean.class)
                .newInstance(false);
        } catch (ReflectiveOperationException | LinkageError error) {
            Biomic.LOG.warn("Natura's nether trees unavailable: {}", error.toString());
        }
        return !generators.isEmpty();
    }

    private long hash(long a, long b, long salt) {
        return WorldgenContext.mix64(
            seed ^ WorldgenContext
                .mix64(a * 0x9E3779B97F4A7C15L ^ WorldgenContext.mix64(b * 0xC2B2AE3D27D4EB4FL ^ salt)));
    }

    private static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }
}
