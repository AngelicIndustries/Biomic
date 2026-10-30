package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import industries.angelic.biomic.world.WorldgenContext;

/**
 * The palette plants that do not stand on the ground (ecology proposal, stage 1 forms), placed with the flower layer:
 * <ul>
 * <li>epiphytes (orchids, bromeliads, bracket fungi, climbing cacti) on tree trunks, facing out;</li>
 * <li>PMP vines in strands from leaves, trunks and cliff faces;</li>
 * <li>immersed plants (reeds, sedges, flags) where one block of still water covers the ground;</li>
 * <li>floating plants (water lilies, water lettuce) on still water up to four deep.</li>
 * </ul>
 * A species appears in a chunk by its tier's chance; water plants grow in hashed patches like the ground flora.
 * Runs inside the ledger's native capture: reads and writes go through the chunk's physical population.
 */
final class FloraForms {

    private static final int WATER_CELL = 24;

    private final long seed;
    private final Map<String, int[]> placed = new TreeMap<>();

    FloraForms(long seed) {
        this.seed = seed;
    }

    record Water(int x, int z, int floorY, int depth) {}

    void populate(World world, OrigenFeatureRuntime.PhysicalPopulation physical, int chunkX, int chunkZ,
        FloraPalettes.Forms forms, List<FloraLedger.Column> columns, List<Water> water) {
        if (!forms.epiphytes()
            .isEmpty() && columns != null) epiphytes(world, physical, chunkX, chunkZ, forms, columns);
        if (!forms.vines()
            .isEmpty() && columns != null) vines(world, physical, chunkX, chunkZ, forms, columns);
        if (water != null) {
            for (FloraPalettes.Plant plant : forms.immersed()) water(world, plant, water, true);
            for (FloraPalettes.Plant plant : forms.floating()) water(world, plant, water, false);
        }
    }

    private static double chance(FloraPalettes.Tier tier) {
        return switch (tier) {
            case SIGNATURE -> 0.6D;
            case COMMON -> 0.5D;
            case UNCOMMON -> 0.3D;
            case RARE -> 0.12D;
        };
    }

    private void epiphytes(World world, OrigenFeatureRuntime.PhysicalPopulation physical, int chunkX, int chunkZ,
        FloraPalettes.Forms forms, List<FloraLedger.Column> columns) {
        List<Trunks.Trunk> trunks = null;
        int next = 0;
        for (FloraPalettes.Plant species : forms.epiphytes()) {
            long h = hash(
                chunkX,
                chunkZ,
                species.id()
                    .hashCode());
            if (unit(h) >= chance(species.tier())) continue;
            FloraPlants.Plant plant = FloraPlants.resolve(species.id());
            if (plant == null) continue;
            if (trunks == null) trunks = Trunks.find(physical, columns, c -> hash(c.x(), c.z(), 0x657069L));
            // One or two trunks each, one to three plants on each.
            for (int t = 0; t < 2 && next < trunks.size(); t++, next++) {
                Trunks.Trunk trunk = trunks.get(next);
                long th = WorldgenContext.mix64(h ^ trunk.x() * 31L ^ trunk.z());
                int count = 1 + (int) Math.floorMod(th, 3L), span = Math.max(1, trunk.top() - trunk.ground() - 2);
                for (int i = 0; i < count; i++) {
                    int[] side = Trunks.SIDES[(int) Math.floorMod(th >>> (4 + 6 * i), 4L)];
                    int x = trunk.x() + side[0], z = trunk.z() + side[1];
                    int y = trunk.ground() + 2 + (int) Math.floorMod(th >>> (7 + 6 * i), (long) span);
                    if (x >> 4 != chunkX || z >> 4 != chunkZ || physical.block(trunk.x(), y, trunk.z()) != trunk.log())
                        continue;
                    set(world, plant, x, y, z, side[2], "epiphyte");
                }
            }
        }
    }

    /** Vine face bit for a support at the horizontal step: south (+z), west, north, east, as vanilla's vine. */
    private static int face(int dx, int dz) {
        return dz == 1 ? 1 : dx == -1 ? 2 : dz == -1 ? 4 : 8;
    }

    private void vines(World world, OrigenFeatureRuntime.PhysicalPopulation physical, int chunkX, int chunkZ,
        FloraPalettes.Forms forms, List<FloraLedger.Column> columns) {
        List<long[]> spots = null;
        for (FloraPalettes.Plant species : forms.vines()) {
            long h = hash(
                chunkX,
                chunkZ,
                species.id()
                    .hashCode());
            if (unit(h) >= chance(species.tier())) continue;
            FloraPlants.Plant plant = FloraPlants.resolve(species.id());
            if (plant == null) continue;
            if (spots == null) spots = vineSpots(physical, columns);
            int strands = 0;
            for (long[] spot : spots) {
                if (strands >= 3) break;
                if (unit(hash(spot[1], spot[3], h)) >= 0.5D) continue;
                int x = (int) spot[1], top = (int) spot[2], z = (int) spot[3], dx = (int) spot[4], dz = (int) spot[5];
                int length = 3 + (int) Math.floorMod(hash(x, z, h ^ top), 5L), grown = 0;
                for (int i = 0; i < length; i++) {
                    int y = top - i;
                    if (physical.block(x, y, z) != Blocks.air) break;
                    // On a wall the strand follows the wall; below leaves it hangs free for three blocks at most.
                    if (i >= 3 && !solid(physical.block(x + dx, y, z + dz))) break;
                    if (set(world, plant, x, y, z, face(dx, dz), null)) grown++;
                    // A block of headroom stays above the ground.
                    if (physical.block(x, y - 2, z) != Blocks.air) break;
                }
                if (grown > 0) {
                    strands++;
                    count("vine " + species.id());
                }
            }
        }
    }

    /** The top open cell of each face of leaves, wood or rock, three to twenty blocks up, in hashed order. */
    private List<long[]> vineSpots(OrigenFeatureRuntime.PhysicalPopulation physical, List<FloraLedger.Column> columns) {
        List<long[]> spots = new ArrayList<>();
        for (FloraLedger.Column column : columns) {
            int x = column.x(), z = column.z();
            for (int y = column.floorY() + 3; y <= column.floorY() + 20; y++) {
                if (physical.block(x, y, z) != Blocks.air) continue;
                for (int[] side : Trunks.SIDES) {
                    if (!solid(physical.block(x + side[0], y, z + side[1]))) continue;
                    if (physical.block(x, y + 1, z) == Blocks.air
                        && solid(physical.block(x + side[0], y + 1, z + side[1]))) continue;
                    spots.add(new long[] { hash(x, z, y), x, y, z, side[0], side[1] });
                    break;
                }
            }
        }
        spots.sort((a, b) -> Long.compare(a[0], b[0]));
        return spots;
    }

    private static boolean solid(Block block) {
        Material material = block.getMaterial();
        return material == Material.leaves || material == Material.wood || material == Material.rock;
    }

    private void water(World world, FloraPalettes.Plant species, List<Water> water, boolean immersed) {
        FloraPlants.Plant plant = FloraPlants.resolve(species.id());
        if (plant == null) return;
        long salt = species.id()
            .hashCode();
        // Reeds and sedges grow in belts along the one-block shallows of a shore: wide, dense patches, so a strip of
        // shoreline catches them; floating plants in smaller rafts.
        double presence = Math.min(0.9D, chance(species.tier()) + (immersed ? 0.4D : 0.2D));
        for (Water column : water) {
            if (immersed ? column.depth != 1 : column.depth > 4) continue;
            if (!inPatch(
                salt,
                column.x,
                column.z,
                presence,
                immersed ? 4 : 2,
                immersed ? 9 : 5,
                immersed ? 0.85D : 0.65D)) continue;
            int surface = column.floorY + column.depth, y = surface + 1;
            Block fluid = world.getBlock(column.x, surface, column.z);
            if (fluid.getMaterial() != Material.water || world.getBlockMetadata(column.x, surface, column.z) != 0
                || world.getBlock(column.x, y, column.z) != Blocks.air) continue;
            set(world, plant, column.x, y, column.z, -1, immersed ? "immersed" : "floating");
        }
    }

    /**
     * One plant: metadata {@code meta}, or its grown state (growth stage, colour, both halves of a tall plant) when
     * -1. Kept only where the plant's own canBlockStay agrees.
     */
    private boolean set(World world, FloraPlants.Plant plant, int x, int y, int z, int meta, String kind) {
        if (world.getBlock(x, y, z) != Blocks.air) return false;
        int[] grown = meta >= 0 ? new int[] { meta, -1 } : plant.grown(hash(x, z, y));
        if (grown[1] >= 0 && world.getBlock(x, y + 1, z) != Blocks.air) grown = new int[] { 0, -1 };
        world.setBlock(x, y, z, plant.block, grown[0], 2);
        if (grown[1] >= 0) world.setBlock(x, y + 1, z, plant.block, grown[1], 2);
        if (world.getBlock(x, y, z) != plant.block || !plant.block.canBlockStay(world, x, y, z)) {
            world.setBlock(x, y, z, Blocks.air, 0, 2);
            if (grown[1] >= 0) world.setBlock(x, y + 1, z, Blocks.air, 0, 2);
            return false;
        }
        if (kind != null) count(kind + " " + plant.id);
        return true;
    }

    private boolean inPatch(long species, int x, int z, double presence, int minRadius, int maxRadius, double fill) {
        int cx = Math.floorDiv(x, WATER_CELL), cz = Math.floorDiv(z, WATER_CELL);
        for (int gx = cx - 1; gx <= cx + 1; gx++) for (int gz = cz - 1; gz <= cz + 1; gz++) {
            long h = hash(gx, gz, species);
            if (unit(h) >= presence) continue;
            long jitter = WorldgenContext.mix64(h);
            int centreX = gx * WATER_CELL + WATER_CELL / 4 + (int) Math.floorMod(jitter, (long) (WATER_CELL / 2));
            int centreZ = gz * WATER_CELL + WATER_CELL / 4
                + (int) Math.floorMod(jitter >>> 20, (long) (WATER_CELL / 2));
            double radius = minRadius + (maxRadius - minRadius) * unit(WorldgenContext.mix64(jitter));
            double dx = x - centreX, dz = z - centreZ;
            if (Math.sqrt(dx * dx + dz * dz) > radius * (0.85D + 0.3D * unit(hash(x, z, species ^ 0x52L)))) continue;
            if (unit(hash(x, z, species ^ 0x436fL)) < fill) return true;
        }
        return false;
    }

    private void count(String what) {
        placed.computeIfAbsent(what, key -> new int[1])[0]++;
    }

    String report() {
        StringBuilder out = new StringBuilder("Plant forms:");
        placed.forEach(
            (what, n) -> out.append("\n  ")
                .append(what)
                .append(' ')
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
