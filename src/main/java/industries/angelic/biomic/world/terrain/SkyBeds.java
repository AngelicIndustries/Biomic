package industries.angelic.biomic.world.terrain;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import industries.angelic.biomic.world.WorldgenContext;

/**
 * Plants that grow on floating islands only (proposal v3, "island-only plants"): unearthly colours of azalea, rose,
 * nemesia, bellflower, gladiolus, lupine and daisy, and the Tillandsia air plant. Each island (a 96-block cell of the
 * sky, by height band) picks one or two of them; they grow in single-species beds near the island's rim, the air plant
 * on its islet trees. Never on the ground, so a cyan rose means you are in the sky.
 */
final class SkyBeds {

    private static final int ISLAND_CELL = 96;
    private static final int ISLAND_BAND = 48;
    private static final int BED_CELL = 14;
    private static final int RIM = 3;

    private final long seed;
    private final List<String> plants;
    private final FloraForms forms;
    private final Map<String, int[]> placed = new TreeMap<>();

    SkyBeds(long seed, List<String> plants, FloraForms forms) {
        this.seed = seed;
        this.plants = plants;
        this.forms = forms;
    }

    void populate(World world, OrigenFeatureRuntime.PhysicalPopulation physical, int chunkX, int chunkZ,
        List<FloraLedger.Column> columns, Random random) {
        if (plants.isEmpty()) return;
        java.util.Set<String> airPlants = new java.util.TreeSet<>();
        for (FloraLedger.Column column : columns) {
            for (String id : picks(column)) {
                FloraPlants.Plant plant = FloraPlants.resolve(id);
                if (plant == null) continue;
                if (id.contains(":epiphyte")) {
                    airPlants.add(id);
                    continue;
                }
                if (!inBed(id.hashCode(), column.x(), column.z()) || !rim(physical, column)) continue;
                int y = column.floorY() + 1;
                if (world.getBlock(column.x(), y, column.z()) != Blocks.air) continue;
                if (plant.place(world, random, column.x(), y, column.z()) > 0)
                    placed.computeIfAbsent(id, key -> new int[1])[0]++;
            }
        }
        for (String id : airPlants) forms.populate(
            world,
            physical,
            chunkX,
            chunkZ,
            new FloraPalettes.Forms(
                List.of(new FloraPalettes.Plant(id, FloraPalettes.Tier.SIGNATURE)),
                List.of(),
                List.of(),
                List.of()),
            columns,
            null);
    }

    /** The one or two sky plants of the island this column belongs to. */
    private List<String> picks(FloraLedger.Column column) {
        long island = hash(
            Math.floorDiv(column.x(), ISLAND_CELL),
            Math.floorDiv(column.z(), ISLAND_CELL),
            Math.floorDiv(column.floorY(), ISLAND_BAND));
        String first = plants.get((int) Math.floorMod(island, (long) plants.size()));
        long second = WorldgenContext.mix64(island);
        if (unit(second) >= 0.5D) return List.of(first);
        String other = plants.get((int) Math.floorMod(second >>> 8, (long) plants.size()));
        return other.equals(first) ? List.of(first) : List.of(first, other);
    }

    /** Within three blocks of the island's edge: open sky below the floor's level beside the column. */
    private static boolean rim(OrigenFeatureRuntime.PhysicalPopulation physical, FloraLedger.Column column) {
        for (int d = 2; d <= RIM; d++) for (int[] side : Trunks.SIDES) {
            int x = column.x() + side[0] * d, z = column.z() + side[1] * d;
            if (physical.block(x, column.floorY(), z) == Blocks.air
                && physical.block(x, column.floorY() - 1, z) == Blocks.air) return true;
        }
        return false;
    }

    /** Beds of four to twelve plants: hashed centres on a 14-block grid, two blocks across. */
    private boolean inBed(long species, int x, int z) {
        int cx = Math.floorDiv(x, BED_CELL), cz = Math.floorDiv(z, BED_CELL);
        for (int gx = cx - 1; gx <= cx + 1; gx++) for (int gz = cz - 1; gz <= cz + 1; gz++) {
            long h = hash(gx, gz, species);
            if (unit(h) >= 0.7D) continue;
            long jitter = WorldgenContext.mix64(h);
            int centreX = gx * BED_CELL + (int) Math.floorMod(jitter, (long) BED_CELL);
            int centreZ = gz * BED_CELL + (int) Math.floorMod(jitter >>> 20, (long) BED_CELL);
            double radius = 1.5D + 1.5D * unit(WorldgenContext.mix64(jitter));
            double dx = x - centreX, dz = z - centreZ;
            if (Math.sqrt(dx * dx + dz * dz) <= radius && unit(hash(x, z, species ^ 0x426564L)) < 0.75D) return true;
        }
        return false;
    }

    String report() {
        StringBuilder out = new StringBuilder("Sky beds:");
        placed.forEach(
            (id, n) -> out.append(' ')
                .append(id)
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
