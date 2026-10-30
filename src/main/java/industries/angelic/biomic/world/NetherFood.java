package industries.angelic.biomic.world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.api.PlantLayer;
import industries.angelic.biomic.biome.TartarusBiomes;
import industries.angelic.biomic.compat.food.WildCrops;
import industries.angelic.biomic.world.terrain.FloraPalettes;

/**
 * The plant layer in Tartarus (api.PlantLayer, its Nether kinds): the plants the palettes' "nether" lists name per
 * biome grow wild in patches on nether ground, and their trees stand here and there (about one per six chunks of
 * their biomes). For the pack, Harvest the Nether's crops, glow flowers and ignis trees. Patches are hashed fields on a
 * 40-block grid and cover every cave floor of a column; everything stays inside the chunk being populated. Server
 * thread only.
 */
final class NetherFood {

    private static final int CELL = 40;
    private static final int HEIGHT = 255;

    private final long seed;
    private final Map<String, List<String>> palettes = FloraPalettes.netherFood();
    /** Resolved once per name (an absent answer too). */
    private final Map<String, java.util.Optional<PlantLayer.Patch>> plants = new HashMap<>();
    private final Map<String, java.util.Optional<PlantLayer.Tree>> trees = new HashMap<>();
    private final Map<String, int[]> placed = new java.util.TreeMap<>();
    private final Map<String, String> spots = new java.util.TreeMap<>();

    NetherFood(long seed) {
        this.seed = seed;
    }

    void populate(World world, int chunkX, int chunkZ) {
        if (palettes.isEmpty()) return;
        List<long[]> treeColumns = new java.util.ArrayList<>();
        PlantLayer.Tree chunkTree = null;
        for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) {
            int x = chunkX * 16 + lx, z = chunkZ * 16 + lz;
            TartarusBiomes biome = TartarusBiomes.fromBiomeId(world.getBiomeGenForCoords(x, z).biomeID);
            List<String> names = biome == null ? null : palettes.get(biome.displayName);
            if (names == null) continue;
            boolean treeColumn = false;
            for (String plant : names) {
                PlantLayer.Tree kind = tree(plant);
                if (kind != null) {
                    treeColumn = true;
                    if (chunkTree == null) chunkTree = kind;
                    continue;
                }
                long species = plant.hashCode();
                if (!inPatch(species, x, z)) continue;
                PlantLayer.Patch patch = plant(plant);
                Block block = patch == null ? null : patch.block();
                if (block == null) continue;
                int meta = patch.metadata(0);
                for (int y = 2; y < HEIGHT - 1; y++) {
                    if (world.getBlock(x, y, z) != Blocks.air) continue;
                    Block floor = world.getBlock(x, y - 1, z);
                    if (!WildCrops.netherGround(floor) || !patch.grows(floor)) continue;
                    if (world.getBlock(x, y + 1, z) != Blocks.air) continue;
                    if (world.setBlock(x, y, z, block, meta, 2)) count(plant, x, y, z);
                }
            }
            if (treeColumn && lx >= 2 && lx <= 13 && lz >= 2 && lz <= 13)
                treeColumns.add(new long[] { hash(x, z, 0x69676e6973L), x, z });
        }
        // About one tree per six chunks of its biomes: the first column (in hashed order) with room for it.
        long h = hash(chunkX, chunkZ, 0x69676e6973L);
        if (treeColumns.isEmpty() || unit(h) >= 1 / 6.0D) return;
        treeColumns.sort((a, b) -> Long.compare(a[0], b[0]));
        for (int i = 0; i < treeColumns.size() && i < 24; i++)
            if (tree(world, chunkTree, (int) treeColumns.get(i)[1], (int) treeColumns.get(i)[2], h)) return;
        count("tree refused", chunkX * 16, 0, chunkZ * 16);
    }

    private PlantLayer.Patch plant(String name) {
        return plants.computeIfAbsent(name, key -> java.util.Optional.ofNullable(PlantLayer.netherPlant(key)))
            .orElse(null);
    }

    private PlantLayer.Tree tree(String name) {
        return trees.computeIfAbsent(name, key -> java.util.Optional.ofNullable(PlantLayer.netherTree(key)))
            .orElse(null);
    }

    /**
     * One tree on the lowest open nether floor of the column with room for it. Its sapling roots in netherrack or soul
     * sand only; on nylium or soul soil the block under the trunk becomes netherrack.
     */
    private boolean tree(World world, PlantLayer.Tree kind, int x, int z, long hash) {
        WorldGenerator generator = kind.generator();
        if (generator == null) return true;
        for (int y = 2; y < HEIGHT - 12; y++) {
            Block floor = world.getBlock(x, y - 1, z);
            if (!WildCrops.netherGround(floor) || world.getBlock(x, y, z) != Blocks.air) continue;
            boolean room = true;
            for (int up = 1; up <= 9 && room; up++) room = world.getBlock(x, y + up, z) == Blocks.air;
            if (!room) continue;
            int floorMeta = world.getBlockMetadata(x, y - 1, z);
            boolean swap = floor != Blocks.netherrack && floor != Blocks.soul_sand;
            if (swap) world.setBlock(x, y - 1, z, Blocks.netherrack, 0, 2);
            if (generator.generate(world, new Random(hash), x, y, z)) {
                count("tree", x, y, z);
                return true;
            }
            if (swap) world.setBlock(x, y - 1, z, floor, floorMeta, 2);
        }
        return false;
    }

    private void count(String what, int x, int y, int z) {
        placed.computeIfAbsent(what, key -> new int[1])[0]++;
        spots.putIfAbsent(what, x + " " + y + " " + z);
    }

    String report() {
        StringBuilder out = new StringBuilder("Nether plants:");
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

    private boolean inPatch(long species, int x, int z) {
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        for (int gx = cx - 1; gx <= cx + 1; gx++) for (int gz = cz - 1; gz <= cz + 1; gz++) {
            long h = hash(gx, gz, species);
            if (unit(h) >= 0.55D) continue;
            long jitter = WorldgenContext.mix64(h);
            int centreX = gx * CELL + CELL / 4 + (int) Math.floorMod(jitter, (long) (CELL / 2));
            int centreZ = gz * CELL + CELL / 4 + (int) Math.floorMod(jitter >>> 20, (long) (CELL / 2));
            double radius = 2 + 3 * unit(WorldgenContext.mix64(jitter));
            double dx = x - centreX, dz = z - centreZ;
            if (Math.sqrt(dx * dx + dz * dz) > radius * (0.85D + 0.3D * unit(hash(x, z, species ^ 0x52L)))) continue;
            if (unit(hash(x, z, species ^ 0x436fL)) < 0.7D) return true;
        }
        return false;
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
