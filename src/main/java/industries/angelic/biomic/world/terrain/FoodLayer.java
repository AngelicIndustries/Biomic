package industries.angelic.biomic.world.terrain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.api.PlantLayer;
import industries.angelic.biomic.compat.flora.NativeFeatureCapture;
import industries.angelic.biomic.compat.flora.NativeFloraHeightFrame;
import industries.angelic.biomic.compat.food.WildCrops;
import industries.angelic.biomic.world.WorldgenContext;

/**
 * The plant layer (api.PlantLayer) where the ecology palettes' "food" sections put its plants, after the flower layer.
 * What a name grows as comes from its resolvers (for the pack: HarvestCraft's food, Biomic's food blocks, EFR's bee
 * nests); where comes from here:
 * <ul>
 * <li>crop drifts: each plant has its own hashed field of patch centres on a 40-block grid, so a patch continues
 * across chunk borders; a patch has a lumpy outline, thins towards its rim, has strays around it and keeps grass
 * between its plants, which are at mixed stages; fairy-ring plants grow in rings; only sand plants on sand;</li>
 * <li>bushes the same way, smaller and sparser;</li>
 * <li>groves of one tree on a 32-block grid (14 in orchards), grown by the tree's generator, only where the whole tree
 * fits in the chunk, its fruit ripened;</li>
 * <li>vines on tree trunks, and nests on a trunk side under the canopy (about one per eight chunks);</li>
 * <li>shoots in drifts like the crops, smaller.</li>
 * </ul>
 */
final class FoodLayer {

    private static final int CROP_CELL = 40;
    private static final int BUSH_CELL = 48;
    private static final int SHOOT_CELL = 24;
    private static final int GROVE_CELL = 32;
    private static final int ORCHARD_CELL = 14;
    private static final String ORCHARD = "regions_unexplored:orchard";
    private static final double NEST_CHANCE = 0.12D;
    private static final double VINE_CHANCE = 0.35D;
    private static final int[][] SIDES = Trunks.SIDES;
    /** Vanilla vine face bit for the support at -step: support south, north, east, west. */
    private static final int[] VINE_FACE = { 1, 4, 8, 2 };

    private final long seed;
    private final FloraPalettes palettes;
    private final Map<String, int[]> outcomes = new TreeMap<>();
    /** A few places of each kind, for the review's teleport list. */
    private final Map<String, List<String>> spots = new TreeMap<>();

    private HostedIslandRuntime.DecorationGuard guard;

    /** Fruit trees keep their crowns off a village's houses and roads and off scattered structures. */
    void setGuard(HostedIslandRuntime.DecorationGuard guard) {
        this.guard = guard;
    }

    FoodLayer(long seed, FloraPalettes palettes) {
        this.seed = seed;
        this.palettes = palettes;
    }

    void populate(World world, OrigenFeatureRuntime.PhysicalPopulation physical, NativeFeatureCapture.Access access,
        int chunkX, int chunkZ, Map<FloraPalettes.Habitat, List<FloraLedger.Column>> byHabitat) {
        for (Map.Entry<FloraPalettes.Habitat, List<FloraLedger.Column>> entry : byHabitat.entrySet()) {
            String habitat = entry.getKey()
                .key();
            FloraPalettes.Food food = palettes.food(habitat);
            List<FloraLedger.Column> columns = entry.getValue();
            if (!food.crops()
                .isEmpty()) crops(physical, access, food, columns);
            if (!food.bushes()
                .isEmpty()) bushes(physical, access, food, columns);
            if (!food.trees()
                .isEmpty()) trees(world, physical, access, habitat, food, columns, chunkX, chunkZ);
            if (!food.vines()
                .isEmpty()) vines(physical, access, habitat, food, columns, chunkX, chunkZ);
            if (food.nests()) nest(physical, habitat, columns, chunkX, chunkZ);
            if (!food.shoots()
                .isEmpty()) shoots(physical, access, food, columns);
        }
    }

    private void crops(OrigenFeatureRuntime.PhysicalPopulation physical, NativeFeatureCapture.Access access,
        FloraPalettes.Food food, List<FloraLedger.Column> columns) {
        for (String crop : food.crops())
            drifts(physical, access, columns, "crop", crop, PlantLayer.crop(crop), CROP_CELL, 0.55D, 1.5D, 3.5D);
    }

    private void shoots(OrigenFeatureRuntime.PhysicalPopulation physical, NativeFeatureCapture.Access access,
        FloraPalettes.Food food, List<FloraLedger.Column> columns) {
        for (String id : food.shoots())
            drifts(physical, access, columns, "shoot", id, PlantLayer.shoot(id), SHOOT_CELL, 0.6D, 1.0D, 2.5D);
    }

    private void bushes(OrigenFeatureRuntime.PhysicalPopulation physical, NativeFeatureCapture.Access access,
        FloraPalettes.Food food, List<FloraLedger.Column> columns) {
        for (String crop : food.bushes())
            drifts(physical, access, columns, "bush", crop, PlantLayer.bush(crop), BUSH_CELL, 0.5D, 1.0D, 2.5D);
    }

    /** One plant's drifting patches over the habitat's columns; its stage rolled per column. */
    private void drifts(OrigenFeatureRuntime.PhysicalPopulation physical, NativeFeatureCapture.Access access,
        List<FloraLedger.Column> columns, String kind, String name, PlantLayer.Patch patch, int cell, double presence,
        double minRadius, double maxRadius) {
        Block block = patch == null ? null : patch.block();
        if (block == null) return;
        long species = (kind + ":" + name).hashCode();
        boolean ring = patch.ring();
        for (FloraLedger.Column column : columns) {
            int x = column.x(), z = column.z();
            if (unit(hash(x, z, species ^ 0x436f6c756d6eL))
                >= drift(species, x, z, cell, presence, minRadius, maxRadius, ring)) continue;
            int metadata = patch.metadata(unit(hash(x, z, species ^ 0x7374616765L)));
            if (plant(physical, access, column, block, metadata, patch.sand()))
                count(kind + " " + name, true, x, column.floorY() + 1, z);
        }
    }

    /**
     * One plant on natural ground, in the open or in place of short grass (half the time: a wild patch keeps grass
     * between its plants).
     */
    private boolean plant(OrigenFeatureRuntime.PhysicalPopulation physical, NativeFeatureCapture.Access access,
        FloraLedger.Column column, Block block, int meta, boolean sand) {
        int x = column.x(), y = column.floorY() + 1, z = column.z();
        Block ground = physical.block(x, column.floorY(), z);
        if (!WildCrops.naturalGround(ground) || ground == Blocks.sand && !sand) return false;
        Block above = physical.block(x, y, z);
        if (above != Blocks.air && (above != Blocks.tallgrass || unit(hash(x, z, 0x6772617373L)) < 0.5D)) return false;
        return access.set(x, y, z, block, meta);
    }

    /**
     * How likely a column holds a plant of this species: patches with a lumpy outline (radius warped by value noise),
     * thinning from the centre to the rim, with a few strays around them; or, for fairy-ring species, a ring.
     * Centres on a {@code cell} grid with the given presence.
     */
    private double drift(long species, int x, int z, int cell, double presence, double minRadius, double maxRadius,
        boolean ring) {
        int cx = Math.floorDiv(x, cell), cz = Math.floorDiv(z, cell);
        double best = 0;
        for (int gx = cx - 1; gx <= cx + 1; gx++) for (int gz = cz - 1; gz <= cz + 1; gz++) {
            long h = hash(gx, gz, species);
            if (unit(h) >= presence) continue;
            long jitter = WorldgenContext.mix64(h);
            int centreX = gx * cell + cell / 4 + (int) Math.floorMod(jitter, (long) (cell / 2));
            int centreZ = gz * cell + cell / 4 + (int) Math.floorMod(jitter >>> 20, (long) (cell / 2));
            double radius = minRadius + (maxRadius - minRadius) * unit(WorldgenContext.mix64(jitter));
            double d = Math.sqrt((double) (x - centreX) * (x - centreX) + (double) (z - centreZ) * (z - centreZ));
            double p;
            if (ring) {
                p = Math.abs(d - radius - 1.0D) < 0.7D ? 0.8D : 0;
            } else {
                double e = d / (radius * (0.7D + 0.6D * noise(x, z, species, 3.0D)));
                p = e < 1 ? 0.12D + 0.68D * (1 - e * e) : e < 2.2D ? 0.04D : 0;
            }
            best = Math.max(best, p);
        }
        return best;
    }

    /** Smooth value noise in 0..1 on a {@code scale}-block lattice. */
    private double noise(int x, int z, long salt, double scale) {
        double fx = x / scale, fz = z / scale;
        int x0 = (int) Math.floor(fx), z0 = (int) Math.floor(fz);
        double tx = fx - x0, tz = fz - z0;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double a = unit(hash(x0, z0, salt)), b = unit(hash(x0 + 1, z0, salt)), c = unit(hash(x0, z0 + 1, salt)),
            d = unit(hash(x0 + 1, z0 + 1, salt));
        return (a + (b - a) * tx) * (1 - tz) + (c + (d - c) * tx) * tz;
    }

    private void trees(World world, OrigenFeatureRuntime.PhysicalPopulation physical,
        NativeFeatureCapture.Access access, String habitat, FloraPalettes.Food food, List<FloraLedger.Column> columns,
        int chunkX, int chunkZ) {
        boolean orchard = ORCHARD.equals(habitat);
        int cell = orchard ? ORCHARD_CELL : GROVE_CELL;
        double presence = orchard ? 0.85D : 0.7D;
        long salt = ("grove:" + habitat).hashCode();
        Map<Long, FloraLedger.Column> byColumn = new HashMap<>();
        for (FloraLedger.Column column : columns) byColumn.put(key(column.x(), column.z()), column);
        int minX = chunkX * 16, minZ = chunkZ * 16;
        for (int gx = Math.floorDiv(minX - 6, cell); gx <= Math.floorDiv(minX + 21, cell); gx++)
            for (int gz = Math.floorDiv(minZ - 6, cell); gz <= Math.floorDiv(minZ + 21, cell); gz++) {
                long h = hash(gx, gz, salt);
                if (unit(h) >= presence) continue;
                long jitter = WorldgenContext.mix64(h);
                String fruit = food.trees()
                    .get(
                        (int) Math.floorMod(
                            jitter >>> 3,
                            (long) food.trees()
                                .size()));
                int centreX = gx * cell + cell / 4 + (int) Math.floorMod(jitter >>> 13, (long) Math.max(1, cell / 2));
                int centreZ = gz * cell + cell / 4 + (int) Math.floorMod(jitter >>> 29, (long) Math.max(1, cell / 2));
                int trees = orchard ? 3 : 2 + (int) Math.floorMod(jitter >>> 45, 3L);
                for (int t = 0; t < trees; t++) {
                    long th = WorldgenContext.mix64(h ^ (t + 1) * 0x9E3779B97F4A7C15L);
                    int x = centreX + (t == 0 ? 0 : (int) Math.floorMod(th, 11L) - 5);
                    int z = centreZ + (t == 0 ? 0 : (int) Math.floorMod(th >>> 8, 11L) - 5);
                    // The whole tree (leaves reach two blocks out) stays in this chunk.
                    if (x >> 4 != chunkX || z >> 4 != chunkZ
                        || (x & 15) < 2
                        || (x & 15) > 13
                        || (z & 15) < 2
                        || (z & 15) > 13) continue;
                    FloraLedger.Column column = byColumn.get(key(x, z));
                    if (column == null
                        || guard != null && guard.blocks("biomic:fruit_tree", true, x, column.floorY() + 1, z))
                        continue;
                    String refusal = grow(world, physical, access, column, fruit, th);
                    count(
                        "tree " + fruit + (refusal == null ? "" : " (" + refusal + ")"),
                        refusal == null,
                        x,
                        column.floorY() + 1,
                        z);
                }
            }
    }

    /** Grows one fruit tree; null when it grew, else why not. */
    private String grow(World world, OrigenFeatureRuntime.PhysicalPopulation physical,
        NativeFeatureCapture.Access access, FloraLedger.Column column, String fruit, long hash) {
        int x = column.x(), y = column.floorY(), z = column.z();
        Block floor = physical.block(x, y, z);
        int floorMeta = physical.metadata(x, y, z);
        // Saplings root in vanilla grass and dirt only; on any other natural ground (sand under palms, hosted grasses,
        // mud) the block under the trunk becomes dirt, as it does under a grown sapling.
        boolean swap = floor != Blocks.grass && floor != Blocks.dirt;
        PlantLayer.Tree kind = PlantLayer.tree(fruit);
        if (!WildCrops.naturalGround(floor) || floor == Blocks.sand && (kind == null || !kind.sand()))
            return "floor " + Block.blockRegistry.getNameForObject(floor);
        Block base = physical.block(x, y + 1, z);
        if (base != Blocks.air && base != Blocks.tallgrass) return "occupied";
        WorldGenerator generator = kind == null ? null : kind.generator();
        if (generator == null) return "no generator";
        WorldGenerator tree = generator;
        Random random = new Random(hash);
        NativeFeatureCapture.Access ripening = ripening(access, kind, hash);
        if (swap && !access.set(x, y, z, Blocks.dirt, 0)) return "floor";
        if (NativeFloraHeightFrame.run(world, ripening, y + 1, sourceY -> tree.generate(world, random, x, sourceY, z)))
            return null;
        if (swap) access.set(x, y, z, floor, floorMeta);
        return "no room";
    }

    /** The tree's writes with their fruit ripened (PlantLayer.Tree#ripen). */
    private NativeFeatureCapture.Access ripening(NativeFeatureCapture.Access access, PlantLayer.Tree kind, long hash) {
        return new NativeFeatureCapture.Access() {

            @Override
            public Block block(int x, int y, int z) {
                return access.block(x, y, z);
            }

            @Override
            public int metadata(int x, int y, int z) {
                return access.metadata(x, y, z);
            }

            @Override
            public boolean set(int x, int y, int z, Block block, int metadata) {
                return access
                    .set(x, y, z, block, kind.ripen(block, metadata, unit(FoodLayer.this.hash(x, z, hash ^ y))));
            }

            @Override
            public int heightValue(int x, int z) {
                return access.heightValue(x, z);
            }

            @Override
            public int topSolidOrLiquid(int x, int z) {
                return access.topSolidOrLiquid(x, z);
            }

            @Override
            public int minimumY() {
                return access.minimumY();
            }

            @Override
            public int maximumYExclusive() {
                return access.maximumYExclusive();
            }
        };
    }

    /** Fruiting vines hang from a trunk's sides, two to four blocks long, ending above the ground. */
    private void vines(OrigenFeatureRuntime.PhysicalPopulation physical, NativeFeatureCapture.Access access,
        String habitat, FloraPalettes.Food food, List<FloraLedger.Column> columns, int chunkX, int chunkZ) {
        long h = hash(chunkX, chunkZ, ("vines:" + habitat).hashCode());
        if (unit(h) >= VINE_CHANCE) return;
        String crop = food.vines()
            .get(
                (int) Math.floorMod(
                    WorldgenContext.mix64(h),
                    (long) food.vines()
                        .size()));
        Block vine = PlantLayer.vine(crop);
        if (vine == null) return;
        int placed = 0;
        for (Trunks.Trunk trunk : trunks(physical, columns, h)) {
            if (placed >= 2) break;
            boolean any = false;
            for (int s = 0; s < SIDES.length; s++) {
                if (unit(hash(trunk.x(), trunk.z(), h ^ s)) >= 0.6D) continue;
                int x = trunk.x() + SIDES[s][0], z = trunk.z() + SIDES[s][1];
                if (x >> 4 != chunkX || z >> 4 != chunkZ) continue;
                // From the first open cell under the canopy, down the trunk.
                int y = trunk.top();
                while (y > trunk.ground() + 2 && physical.block(x, y, z) != Blocks.air) y--;
                int length = 2 + (int) Math.floorMod(hash(x, z, h), 3L);
                for (int i = 0; i < length && y - i > trunk.ground() + 1; i++) {
                    if (physical.block(trunk.x(), y - i, trunk.z()) != trunk.log()
                        || physical.block(x, y - i, z) != Blocks.air) break;
                    if (access.set(x, y - i, z, vine, VINE_FACE[s])) any = true;
                }
            }
            if (any) {
                placed++;
                count("vine " + crop, true, trunk.x(), trunk.top(), trunk.z());
            }
        }
    }

    private void nest(OrigenFeatureRuntime.PhysicalPopulation physical, String habitat,
        List<FloraLedger.Column> columns, int chunkX, int chunkZ) {
        long h = hash(chunkX, chunkZ, ("nest:" + habitat).hashCode());
        PlantLayer.Nest nest = PlantLayer.nest();
        if (unit(h) >= NEST_CHANCE || nest == null || !nest.available()) return;
        for (Trunks.Trunk trunk : trunks(physical, columns, h)) {
            for (int s = 0; s < SIDES.length; s++) {
                int side = (s + (int) Math.floorMod(h >>> 5, 4L)) % 4;
                int x = trunk.x() + SIDES[side][0], z = trunk.z() + SIDES[side][1];
                if (x >> 4 != chunkX || z >> 4 != chunkZ) continue;
                // Under the canopy: leaves or the trunk's own wood right above the nest, open air in front.
                for (int y = trunk.ground() + 2; y < trunk.top(); y++) {
                    Block above = physical.block(x, y + 1, z);
                    if (physical.block(x, y, z) != Blocks.air
                        || physical.block(x + SIDES[side][0], y, z + SIDES[side][1]) != Blocks.air
                        || above.getMaterial() != Material.leaves && above != trunk.log()) continue;
                    int facing = SIDES[side][2];
                    physical.setChest(x, y, z, nest.state(facing), nest.payload(x, y, z, facing, h));
                    count("bee nest", true, x, y, z);
                    return;
                }
            }
        }
        count("bee nest", false, 0, 0, 0);
    }

    private List<Trunks.Trunk> trunks(OrigenFeatureRuntime.PhysicalPopulation physical,
        List<FloraLedger.Column> columns, long salt) {
        return Trunks.find(physical, columns, column -> hash(column.x(), column.z(), salt));
    }

    private void count(String what, boolean placed, int x, int y, int z) {
        synchronized (outcomes) {
            int[] n = outcomes.computeIfAbsent(what, key -> new int[2]);
            n[placed ? 0 : 1]++;
            // Spread the samples: the first, then every 16th.
            List<String> at = spots.computeIfAbsent(what, key -> new java.util.ArrayList<>());
            if (placed && at.size() < 3 && (n[0] - 1) % 16 == 0) at.add(x + " " + y + " " + z);
        }
    }

    String report() {
        StringBuilder out = new StringBuilder("Food layer:");
        synchronized (outcomes) {
            outcomes.forEach(
                (what, n) -> out.append("\n  ")
                    .append(what)
                    .append(" placed ")
                    .append(n[0])
                    .append(n[1] > 0 ? " refused " + n[1] : "")
                    .append(
                        spots.getOrDefault(what, List.of())
                            .isEmpty() ? "" : " at " + String.join(", ", spots.get(what))));
        }
        return out.toString();
    }

    private static long key(int x, int z) {
        return (long) x << 32 ^ z & 0xFFFFFFFFL;
    }

    private long hash(long a, long b, long salt) {
        return FloraLedger.hash(seed, a, b, salt);
    }

    private static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }
}
