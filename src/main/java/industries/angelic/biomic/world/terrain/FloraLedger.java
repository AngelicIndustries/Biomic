package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.compat.biome.NativeBlockCatalogue;
import industries.angelic.biomic.compat.flora.NativeFeatureCapture;
import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.modern.ModernStateCompatibility;

/**
 * Adds the palette plants of each habitat to the mainland ground, after every other surface pass of the chunk.
 *
 * <p>
 * Each column's habitat is the hosted one on its highest ground surface, or the Origen profile's where Origen owns
 * the ground. A habitat's budget class sets how many plants a chunk holds counting the natives already there, a cap
 * on what is added, and the size and fill of its patches. Every plant species has its own field of patch centres,
 * hashed from the seed on a grid, so a patch continues across chunk borders; a chunk only writes inside itself. At
 * most three added species per chunk; rare ones live on a 64-block grid (at most one patch per 4x4 chunks).
 * Signature plants are exclusive by construction: they appear only in their own habitat's palette.
 * </p>
 */
public final class FloraLedger {

    private static final int MAX_SPECIES_PER_CHUNK = 3;
    private static final int RARE_CELL = 64;
    private static final int SPACED_CELL = 12;

    private final long seed;
    private final HostedIslandRuntime mainland;
    private final OrigenDensityField origen;
    private final int seaLevel;
    private final int height;
    private final FloraPalettes palettes = FloraPalettes.instance();
    private final FoodLayer food;
    private final FloraForms forms;
    private final SkyBeds sky;
    private final MarineLayer marine;
    private HostedIslandRuntime islands;
    private final Map<String, Boolean> unencodable = new java.util.concurrent.ConcurrentHashMap<>();
    private long placed, refused, allowances, natives, chunks, candidates;
    private final Map<String, int[]> outcomes = new java.util.TreeMap<>();
    /** Where each habitat was first met, with how many columns: the review's teleport list. */
    private final Map<String, String> seen = new java.util.TreeMap<>();
    private final Map<String, int[]> seenColumns = new java.util.HashMap<>();

    public FloraLedger(WorldgenContext context, HostedIslandRuntime mainland, int physicalSeaLevel,
        int physicalHeight) {
        this.seed = context.getSettings()
            .getWorldSeed();
        this.mainland = mainland;
        this.origen = OrigenDensityField.shared(context);
        this.seaLevel = physicalSeaLevel;
        this.height = physicalHeight;
        this.food = new FoodLayer(seed, palettes);
        this.forms = new FloraForms(seed);
        this.sky = new SkyBeds(seed, palettes.sky(), forms);
        this.marine = new MarineLayer(seed, physicalSeaLevel);
    }

    /** The floating islands' runtime (512 only): their habitats get their palettes, and the sky-only plants. */
    private HostedIslandRuntime.DecorationGuard guard;

    /** Plants, food and beach litter root nowhere a village's pieces or a scattered structure keep decoration off. */
    public void setGuard(HostedIslandRuntime.DecorationGuard guard) {
        this.guard = guard;
        food.setGuard(guard);
    }

    private boolean guarded(int x, int y, int z) {
        return guard != null && guard.blocks("biomic:flora", false, x, y, z);
    }

    public void setIslands(HostedIslandRuntime islands) {
        this.islands = islands;
    }

    record Column(int x, int z, int floorY) {}

    private record Candidate(Column column, FloraPalettes.Plant plant, double priority) {}

    public void populate(World world, int chunkX, int chunkZ, OrigenFeatureRuntime features) {
        Map<FloraPalettes.Habitat, List<Column>> byHabitat = new LinkedHashMap<>();
        Map<FloraPalettes.Habitat, List<FloraForms.Water>> water = new LinkedHashMap<>();
        List<MarineLayer.Water> sea = new ArrayList<>();
        mainland.forEachGroundSurface(chunkX, chunkZ, (x, z, floorY, fluidDepth, habitat) -> {
            if (floorY + fluidDepth + 3 >= height || guarded(x, floorY + 1, z)) return;
            if (fluidDepth > 0) sea.add(
                new MarineLayer.Water(
                    x,
                    z,
                    floorY,
                    fluidDepth,
                    habitat,
                    habitat == null ? origen.sampleSurfaceProfile(x, z) : null,
                    world.getBiomeGenForCoords(x, z).temperature));
            FloraPalettes.Habitat palette = habitat != null ? palettes.habitat(habitat)
                : palettes.origen(origen.sampleSurfaceProfile(x, z));
            if (palette == null) return;
            if (fluidDepth == 0) {
                seen.putIfAbsent(palette.key(), x + " " + (floorY + 1) + " " + z);
                seenColumns.computeIfAbsent(palette.key(), key -> new int[1])[0]++;
            }
            if (fluidDepth == 0) byHabitat.computeIfAbsent(palette, key -> new ArrayList<>())
                .add(new Column(x, z, floorY));
            else if (fluidDepth <= 4 && palettes.forms(palette.key())
                .any())
                water.computeIfAbsent(palette, key -> new ArrayList<>())
                    .add(new FloraForms.Water(x, z, floorY, fluidDepth));
        });
        Map<FloraPalettes.Habitat, List<Column>> islandHabitats = new LinkedHashMap<>();
        List<Column> islandColumns = new ArrayList<>();
        if (islands != null) islands.forEachIslandSurface(chunkX, chunkZ, (x, z, floorY, fluidDepth, habitat) -> {
            if (fluidDepth != 0 || floorY + 3 >= height || guarded(x, floorY + 1, z)) return;
            Column column = new Column(x, z, floorY);
            islandColumns.add(column);
            FloraPalettes.Habitat palette = palettes.habitat(habitat);
            if (palette != null) islandHabitats.computeIfAbsent(palette, key -> new ArrayList<>())
                .add(column);
        });
        if (byHabitat.isEmpty() && sea.isEmpty() && islandColumns.isEmpty()) return;
        features.withPhysicalPopulation(world, chunkX, chunkZ, physical -> {
            NativeFeatureCapture.Access access = access(physical, chunkX, chunkZ);
            NativeFeatureCapture.run(world, access, () -> {
                Random random = new Random(hash(chunkX, chunkZ, 0x666c6f72614c4cL));
                for (Map.Entry<FloraPalettes.Habitat, List<Column>> entry : byHabitat.entrySet()) if (entry.getKey()
                    .adds()) populate(world, physical, random, chunkX, chunkZ, entry.getKey(), entry.getValue());
                java.util.Set<FloraPalettes.Habitat> shaped = new java.util.LinkedHashSet<>(byHabitat.keySet());
                shaped.addAll(water.keySet());
                for (FloraPalettes.Habitat habitat : shaped) {
                    FloraPalettes.Forms habitatForms = palettes.forms(habitat.key());
                    if (habitatForms.any()) forms.populate(
                        world,
                        physical,
                        chunkX,
                        chunkZ,
                        habitatForms,
                        byHabitat.get(habitat),
                        water.get(habitat));
                }
                for (Map.Entry<FloraPalettes.Habitat, List<Column>> entry : islandHabitats.entrySet())
                    if (entry.getKey()
                        .adds()) populate(world, physical, random, chunkX, chunkZ, entry.getKey(), entry.getValue());
                if (!islandColumns.isEmpty()) sky.populate(world, physical, chunkX, chunkZ, islandColumns, random);
                if (!sea.isEmpty()) marine.populate(world, sea);
                for (Map.Entry<FloraPalettes.Habitat, List<Column>> land : byHabitat.entrySet()) marine.strand(
                    world,
                    land.getKey()
                        .key(),
                    land.getValue());
                return null;
            });
            food.populate(world, physical, access, chunkX, chunkZ, byHabitat);
            // Floating islands grow their habitat's food too.
            if (!islandHabitats.isEmpty()) food.populate(world, physical, access, chunkX, chunkZ, islandHabitats);
        });
    }

    private void populate(World world, OrigenFeatureRuntime.PhysicalPopulation physical, Random random, int chunkX,
        int chunkZ, FloraPalettes.Habitat habitat, List<Column> columns) {
        FloraPalettes.Budget budget = habitat.budget();
        double share = columns.size() / 256.0D;
        long habitatSalt = habitat.key()
            .hashCode();
        int target = (int) Math
            .round(lerp(budget.plantsMin(), budget.plantsMax(), unit(hash(chunkX, chunkZ, habitatSalt))) * share);
        int natives = 0;
        for (Column column : columns) natives += nativePlants(physical, column);
        int allowance = Math.min((int) Math.ceil(budget.added() * share), target - natives);
        this.natives += natives;
        chunks++;
        if (allowance <= 0) return;
        allowances += allowance;

        // Species compete only where they can stand: a patch over stone or sand offers nothing to a soil plant.
        Map<FloraPalettes.Plant, List<Candidate>> bySpecies = new HashMap<>();
        for (Column column : columns) {
            int y = column.floorY + 1;
            if (world.getBlock(column.x, y, column.z) != Blocks.air || !sited(physical, budget.rule(), column))
                continue;
            for (FloraPalettes.Plant plant : habitat.plants()) {
                double inside = patch(budget, plant, column.x, column.z);
                if (inside < 0) continue;
                FloraPlants.Plant installed = FloraPlants.resolve(plant.id());
                if (installed == null || !installed.block.canPlaceBlockAt(world, column.x, y, column.z)) continue;
                bySpecies.computeIfAbsent(plant, key -> new ArrayList<>())
                    .add(
                        new Candidate(
                            column,
                            plant,
                            inside + unit(
                                hash(
                                    column.x,
                                    column.z,
                                    plant.id()
                                        .hashCode()))
                                * 0.25));
            }
        }
        List<Map.Entry<FloraPalettes.Plant, List<Candidate>>> species = new ArrayList<>(bySpecies.entrySet());
        species.sort(
            (a, b) -> a.getValue()
                .size()
                != b.getValue()
                    .size()
                        ? b.getValue()
                            .size()
                            - a.getValue()
                                .size()
                        : Long.compare(
                            hash(
                                chunkX,
                                chunkZ,
                                a.getKey()
                                    .id()
                                    .hashCode()),
                            hash(
                                chunkX,
                                chunkZ,
                                b.getKey()
                                    .id()
                                    .hashCode())));
        List<Candidate> chosen = new ArrayList<>();
        for (int i = 0; i < species.size() && i < MAX_SPECIES_PER_CHUNK; i++) chosen.addAll(
            species.get(i)
                .getValue());
        chosen.sort((a, b) -> Double.compare(a.priority, b.priority));
        candidates += chosen.size();
        int added = 0;
        for (Candidate candidate : chosen) {
            if (added >= allowance) break;
            FloraPlants.Plant plant = FloraPlants.resolve(candidate.plant.id());
            if (plant == null) continue;
            Column column = candidate.column;
            int blocks;
            try {
                blocks = plant.place(world, random, column.x, column.floorY + 1, column.z);
            } catch (RuntimeException failure) {
                FloraPlants.disable(plant, failure);
                continue;
            }
            int[] outcome = outcomes.computeIfAbsent(plant.id + " " + floorName(physical, column), key -> new int[2]);
            if (blocks == 0) {
                refused++;
                outcome[1]++;
                continue;
            }
            outcome[0]++;
            added += blocks;
            placed++;
        }
    }

    /**
     * Where the column stands in its species' nearest patch: 0 at the centre to about 1 at the rim, or -1 outside
     * every patch (or in an unfilled cell of one).
     */
    private double patch(FloraPalettes.Budget budget, FloraPalettes.Plant plant, int x, int z) {
        boolean spaced = budget.rule() == FloraPalettes.Rule.SPACED;
        int cell = plant.tier() == FloraPalettes.Tier.RARE ? RARE_CELL : spaced ? SPACED_CELL : budget.patchMax() + 6;
        double presence = switch (plant.tier()) {
            case SIGNATURE -> 0.6D;
            case COMMON -> 0.5D;
            case UNCOMMON -> 0.3D;
            case RARE -> 0.5D;
        };
        long species = plant.id()
            .hashCode() * 0x9E3779B97F4A7C15L;
        int cx = Math.floorDiv(x, cell), cz = Math.floorDiv(z, cell);
        double best = -1;
        for (int gx = cx - 1; gx <= cx + 1; gx++) for (int gz = cz - 1; gz <= cz + 1; gz++) {
            long h = hash(gx, gz, species);
            if (unit(h) >= presence) continue;
            long jitter = WorldgenContext.mix64(h);
            int centreX = gx * cell + cell / 4 + (int) Math.floorMod(jitter, (long) Math.max(1, cell / 2));
            int centreZ = gz * cell + cell / 4 + (int) Math.floorMod(jitter >>> 20, (long) Math.max(1, cell / 2));
            double diameter = spaced ? 1
                : lerp(budget.patchMin(), budget.patchMax(), unit(WorldgenContext.mix64(jitter)));
            double radius = Math.max(0.5D, diameter / 2.0D);
            double dx = x - centreX, dz = z - centreZ;
            double ragged = 0.85D + 0.3D * unit(hash(x, z, species ^ 0x5261676765644cL));
            double reach = Math.sqrt(dx * dx + dz * dz) / (radius * ragged);
            if (reach > 1.0D) continue;
            double fill = lerp(budget.fillMin(), budget.fillMax(), unit(WorldgenContext.mix64(jitter ^ 0x46696c6cL)));
            if (unit(hash(x, z, species ^ 0x436f6c756d6eL)) >= fill) continue;
            if (best < 0 || reach < best) best = reach;
        }
        return best;
    }

    private boolean sited(OrigenFeatureRuntime.PhysicalPopulation physical, FloraPalettes.Rule rule, Column column) {
        return switch (rule) {
            case NONE, SPACED -> true;
            case ABOVE_HIGH_WATER -> column.floorY >= seaLevel + 2;
            case NEAR_WATER -> nearby(physical, column, 4, 0, 1, Material.water);
            case NEAR_STONE -> nearby(physical, column, 2, 0, 2, Material.rock);
        };
    }

    private static boolean nearby(OrigenFeatureRuntime.PhysicalPopulation physical, Column column, int reach, int below,
        int above, Material material) {
        for (int dx = -reach; dx <= reach; dx++) for (int dz = -reach; dz <= reach; dz++)
            for (int y = column.floorY - below; y <= column.floorY + above; y++)
                if (physical.block(column.x + dx, y, column.z + dz)
                    .getMaterial() == material) return true;
        return false;
    }

    /** Plants already on the column: grass and ferns do not count, a two-block plant counts twice. */
    private static int nativePlants(OrigenFeatureRuntime.PhysicalPopulation physical, Column column) {
        Block block = physical.block(column.x, column.floorY + 1, column.z);
        if (block == Blocks.air || block == Blocks.tallgrass) return 0;
        Material material = block.getMaterial();
        if (material != Material.plants && material != Material.vine && material != Material.cactus) return 0;
        if (block == Blocks.double_plant) {
            int meta = physical.metadata(column.x, column.floorY + 1, column.z);
            return meta == 2 || meta == 3 ? 0 : 2;
        }
        return physical.block(column.x, column.floorY + 2, column.z) == block ? 2 : 1;
    }

    private NativeFeatureCapture.Access access(OrigenFeatureRuntime.PhysicalPopulation physical, int chunkX,
        int chunkZ) {
        return new NativeFeatureCapture.Access() {

            @Override
            public Block block(int x, int y, int z) {
                return physical.block(x, y, z);
            }

            @Override
            public int metadata(int x, int y, int z) {
                return physical.metadata(x, y, z);
            }

            @Override
            public boolean set(int x, int y, int z, Block block, int metadata) {
                if (x >> 4 != chunkX || z >> 4 != chunkZ || y < 1 || y >= height) return false;
                String state = encode(block, metadata);
                if (state == null) return false;
                physical.setBlock(x, y, z, state);
                return true;
            }

            @Override
            public int heightValue(int x, int z) {
                for (int y = Math.min(height - 1, physical.topCandidateY(x, z)); y > 0; y--)
                    if (physical.block(x, y, z) != Blocks.air) return y + 1;
                return 0;
            }

            @Override
            public int minimumY() {
                return 0;
            }

            @Override
            public int maximumYExclusive() {
                return height;
            }

            private String encode(Block block, int metadata) {
                String state = NativeBlockCatalogue.encode(block, metadata);
                try {
                    if (state == null) state = ModernStateCompatibility.modernState(block, metadata);
                } catch (RuntimeException unsupported) {
                    state = null;
                }
                if (state != null && physical.canResolve(state)) return state;
                // Another mod's block (the plant layer's resolvers name any registered block): as it stands.
                if (Block.blockRegistry.getNameForObject(block) != null) {
                    String placed = OrigenBlockStateResolver.placed(block, metadata);
                    if (physical.canResolve(placed)) return placed;
                }
                String name = Block.blockRegistry.getNameForObject(block) + ":" + metadata;
                if (unencodable.putIfAbsent(name, Boolean.TRUE) == null)
                    Biomic.LOG.warn("Flora ledger cannot write {} (no block state codec)", name);
                return null;
            }
        };
    }

    private static String floorName(OrigenFeatureRuntime.PhysicalPopulation physical, Column column) {
        return "on " + Block.blockRegistry.getNameForObject(physical.block(column.x, column.floorY, column.z));
    }

    public String report() {
        StringBuilder worst = new StringBuilder();
        outcomes.entrySet()
            .stream()
            .sorted((a, b) -> b.getValue()[1] - a.getValue()[1])
            .limit(25)
            .forEach(
                e -> worst.append("\n  ")
                    .append(e.getKey())
                    .append(" placed ")
                    .append(e.getValue()[0])
                    .append(" refused ")
                    .append(e.getValue()[1]));
        return "Flora ledger: " + placed
            + " plants placed, "
            + refused
            + " spots refused; "
            + chunks
            + " habitat-chunks, natives "
            + natives
            + ", allowance "
            + allowances
            + ", candidates "
            + candidates
            + worst
            + "\n"
            + forms.report()
            + "\n"
            + sky.report()
            + "\n"
            + marine.report()
            + "\n"
            + food.report()
            + "\nHabitats seen:"
            + seenReport();
    }

    private String seenReport() {
        StringBuilder out = new StringBuilder();
        seen.forEach(
            (key, at) -> out.append("\n  ")
                .append(key)
                .append(" (")
                .append(seenColumns.get(key)[0])
                .append(" columns) at ")
                .append(at));
        return out.toString();
    }

    private long hash(long a, long b, long salt) {
        return hash(seed, a, b, salt);
    }

    static long hash(long seed, long a, long b, long salt) {
        return WorldgenContext.mix64(
            seed ^ WorldgenContext
                .mix64(a * 0x9E3779B97F4A7C15L ^ WorldgenContext.mix64(b * 0xC2B2AE3D27D4EB4FL ^ salt)));
    }

    private static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }
}
