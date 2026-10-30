package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.ForgeDirection;

import ganymedes01.etfuturum.tileentities.TileEntityCaveVines;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.compat.biome.NativeBlockCatalogue;
import industries.angelic.biomic.compat.flora.NativeFeatureCapture;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.storage.BiomeVolumeManager;
import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.modern.ModernStateCompatibility;

/**
 * Dresses generic caves by their region's theme (CaveThemes, which also laid the rock skins on the workers): moss
 * carpet, wall moss and cave vines over moss; mushrooms on the bare rock; pointed dripstone from dripstone
 * ceilings with stalagmite stubs below, BOP stalactites on plain stone; icicles in frozen caves; dead bushes and
 * bones on dry sand; hanging roots under rooted ceilings; webs in spider nooks. Every mouth gets its twilight zone,
 * glow lichen grows in colonies (fullest by water and mouths, never in frozen, hot or dry caves), and a rare
 * Miner's Delight garden appears at most once per 4x4 chunks.
 *
 * <p>
 * Generic caves are air below a column's ground surface (islands excluded) in cells the 3D biome volume leaves to
 * the surface biome: RU cave biomes and Origen's lush, dripstone, deep-dark and vine-vault caves keep their own
 * ecology. Every choice is a hash of the seed and position; every write uses the block's own placement rules and
 * stays inside the chunk; a placement budget per chunk keeps the server-thread cost bounded.
 * </p>
 */
public final class CaveDecorPass {

    private static final int MOUTH_REACH = 6;
    private static final int COLONY_CELL = 40;

    private final HostedIslandRuntime mainland;
    private final CaveThemes themes;
    private final int height;
    private final boolean shifted512;
    private final int minY;
    private final Map<CaveThemes.Theme, long[]> themeCells = new EnumMap<>(CaveThemes.Theme.class);
    private final Map<CaveThemes.Theme, List<String>> samples = new EnumMap<>(CaveThemes.Theme.class);
    private long placed, chunks;

    public CaveDecorPass(HostedIslandRuntime mainland, CaveThemes themes, int physicalHeight, boolean shifted512) {
        this.mainland = mainland;
        this.themes = themes;
        this.height = physicalHeight;
        this.shifted512 = shifted512;
        this.minY = shifted512 ? 69 : 5;
    }

    /** One cave air cell: floor (solid below), ceiling (solid above), wall side bits, depth below the ground. */
    private record Cell(int x, int y, int z, boolean floor, boolean ceiling, int walls, int cover, boolean mouth,
        CaveThemes.Theme theme) {}

    private final class Chunk3 {

        final World world;
        final int cx, cz;
        final byte[] raw;
        final OrigenFeatureRuntime.PhysicalPopulation physical;
        final Random random;
        final long seed;
        int budget, floorBudget;

        Chunk3(World world, int cx, int cz, byte[] raw, OrigenFeatureRuntime.PhysicalPopulation physical, long seed) {
            this.world = world;
            this.cx = cx;
            this.cz = cz;
            this.raw = raw;
            this.physical = physical;
            this.seed = seed;
            this.random = new Random(seed);
        }

        byte below(Cell cell) {
            return raw[index(cell.x & 15, cell.y - 1, cell.z & 15)];
        }

        byte above(Cell cell) {
            return raw[index(cell.x & 15, cell.y + 1, cell.z & 15)];
        }
    }

    public void populate(World world, int chunkX, int chunkZ, OrigenFeatureRuntime origen) {
        byte[] raw = origen.rawTerrain(chunkX, chunkZ, null);
        int[] ground = new int[256];
        java.util.Arrays.fill(ground, -1);
        mainland.forEachGroundSurface(
            chunkX,
            chunkZ,
            (x, z, floorY, fluidDepth, habitat) -> ground[(z & 15) * 16 + (x & 15)] = floorY);
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        BiomeVolume volume = BiomeVolumeManager.getVolume(chunk);

        List<Cell> cells = new ArrayList<>();
        List<int[]> water = new ArrayList<>();
        int caveColumns = 0;
        for (int lz = 0; lz < 16; lz++) for (int lx = 0; lx < 16; lx++) {
            int top = ground[lz * 16 + lx];
            if (top < 0) continue;
            boolean columnHasCave = false;
            for (int y = minY; y < top - 1 && y < height - 1; y++) {
                byte material = raw[index(lx, y, lz)];
                if (material == BiomicTerrainSampler.WATER) {
                    if (raw[index(lx, y + 1, lz)] == BiomicTerrainSampler.AIR && generic(volume, lx, y, lz))
                        water.add(new int[] { lx, y, lz });
                    continue;
                }
                if (material != BiomicTerrainSampler.AIR || !generic(volume, lx, y, lz)) continue;
                boolean floor = solid(raw[index(lx, y - 1, lz)]);
                boolean ceiling = solid(raw[index(lx, y + 1, lz)]);
                int walls = 0;
                if (lz < 15 && solid(raw[index(lx, y, lz + 1)])) walls |= 1;
                if (lx > 0 && solid(raw[index(lx - 1, y, lz)])) walls |= 2;
                if (lz > 0 && solid(raw[index(lx, y, lz - 1)])) walls |= 4;
                if (lx < 15 && solid(raw[index(lx + 1, y, lz)])) walls |= 8;
                if (!floor && !ceiling && walls == 0) continue;
                int x = (chunkX << 4) + lx, z = (chunkZ << 4) + lz;
                cells.add(
                    new Cell(x, y, z, floor, ceiling, walls, top - y, mouth(ground, lx, lz, y), themes.at(x, y, z)));
                columnHasCave = true;
            }
            if (columnHasCave) caveColumns++;
        }
        if (cells.isEmpty()) return;

        long seed = WorldgenContext.mix64(
            world.getSeed() ^ WorldgenContext.mix64(
                chunkX * 0x9E3779B97F4A7C15L ^ WorldgenContext.mix64(chunkZ * 0xC2B2AE3D27D4EB4FL ^ 0x43617665L)));
        BiomeGenBase surface = world.getBiomeGenForCoords((chunkX << 4) + 8, (chunkZ << 4) + 8);
        int caveFloors = 0;
        for (Cell cell : cells) if (cell.floor) caveFloors++;
        int budget = Math.min(shifted512 ? 240 : 150, 8 + caveColumns);
        int floorBudget = caveFloors * 2 / 5;

        origen.withPhysicalPopulation(world, chunkX, chunkZ, physical -> {
            Chunk3 c = new Chunk3(world, chunkX, chunkZ, raw, physical, seed);
            c.budget = budget;
            c.floorBudget = floorBudget;
            NativeFeatureCapture.run(world, access(physical, chunkX, chunkZ), () -> {
                decorate(c, cells, water, surface);
                return null;
            });
            placed += budget - c.budget;
        });
        chunks++;
        Map<CaveThemes.Theme, Integer> here = new EnumMap<>(CaveThemes.Theme.class);
        for (Cell cell : cells) {
            themeCells.computeIfAbsent(cell.theme, key -> new long[1])[0]++;
            here.merge(cell.theme, 1, Integer::sum);
        }
        // Review spots: a floor cell in a chunk holding a sizeable cave of the theme.
        for (Cell cell : cells) {
            List<String> where = samples.computeIfAbsent(cell.theme, key -> new ArrayList<>());
            if (cell.floor && !cell.mouth
                && here.get(cell.theme) >= 300
                && where.size() < 4
                && where.stream()
                    .noneMatch(p -> p.startsWith(chunkX + "/" + chunkZ + " ")))
                where.add(chunkX + "/" + chunkZ + " " + cell.x + " " + cell.y + " " + cell.z);
        }
    }

    private void decorate(Chunk3 c, List<Cell> cells, List<int[]> water, BiomeGenBase surface) {
        List<Cell> order = new ArrayList<>(cells);
        java.util.Collections.shuffle(order, new Random(c.seed));
        mouth(c, order, surface.temperature, surface.rainfall);
        List<Cell> lichenCells = new ArrayList<>();
        for (Cell cell : order) if (cell.theme != CaveThemes.Theme.FROZEN && cell.theme != CaveThemes.Theme.HOT
            && cell.theme != CaveThemes.Theme.DRY) lichenCells.add(cell);
        // Lichen colonies take up to a quarter of the budget before the themed dressing.
        int reserved = c.budget - c.budget / 4;
        c.budget -= reserved;
        lichenColonies(c, lichenCells, water);
        c.budget += reserved;
        for (Cell cell : order) {
            if (c.budget <= 0) break;
            dress(c, cell);
        }
        minersDelight(c, lichenCells, surface.temperature);
    }

    /** One cell's themed dressing, decided by its own hash. */
    private void dress(Chunk3 c, Cell cell) {
        long h = hash(c, cell);
        double roll = unit(h);
        byte below = cell.floor ? c.below(cell) : BiomicTerrainSampler.AIR;
        byte above = cell.ceiling ? c.above(cell) : BiomicTerrainSampler.AIR;
        boolean hanging = cell.ceiling && !cell.floor;
        switch (cell.theme) {
            case MOSSY -> {
                if (below == BiomicTerrainSampler.MOSS && roll < 0.25D) floorPlace(c, cell, "etfuturum:moss_carpet:0");
                else if (hanging && roll < 0.05D) caveVines(c, cell, 2 + (int) Math.floorMod(h >>> 20, 6L), h);
                else if (!cell.floor && cell.walls != 0 && roll < 0.14D)
                    vine(c, cell.x, cell.y, cell.z, DecorSlots.cave("mossy_wall"), cell.walls);
            }
            case FUNGAL -> {
                // No soil laid for them (mycelium is the mushroom caves' alone): vanilla's mushrooms grow on any solid
                // floor in the dark, a glowshroom on stone (the pack's BOP one; DecorSlots).
                if (!cell.floor) return;
                if (roll < 0.12D)
                    caveMushroom(c, cell, unit(h >>> 8) < 0.7D ? Blocks.brown_mushroom : Blocks.red_mushroom);
                else if (roll < 0.15D && below == BiomicTerrainSampler.STONE)
                    floorPlant(c, cell, DecorSlots.cave("glowshroom"));
            }
            case DRIPSTONE -> {
                if (hanging && above == BiomicTerrainSampler.DRIPSTONE_BLOCK && roll < 0.3D) pointed(
                    c,
                    "etfuturum:pointed_dripstone",
                    cell.x,
                    cell.y,
                    cell.z,
                    1 + (int) Math.floorMod(h >>> 20, 4L));
                else if (cell.floor && below == BiomicTerrainSampler.DRIPSTONE_BLOCK
                    && roll < 0.2D
                    && c.floorBudget > 0) {
                        if (stub(
                            c,
                            "etfuturum:pointed_dripstone",
                            cell.x,
                            cell.y,
                            cell.z,
                            1 + (int) Math.floorMod(h >>> 24, 2L))) c.floorBudget--;
                    } else if (hanging && above == BiomicTerrainSampler.STONE && roll < 0.06D)
                        set(c, cell.x, cell.y, cell.z, DecorSlots.cave("stalactite"));
                else if (cell.floor && below == BiomicTerrainSampler.STONE && roll < 0.04D)
                    floorPlace(c, cell, DecorSlots.cave("stalagmite"));
            }
            case FROZEN -> {
                if (hanging && roll < 0.12D) {
                    if (pointed(c, "biomic:icicle", cell.x, cell.y, cell.z, 1 + (int) Math.floorMod(h >>> 20, 3L))
                        && unit(h >>> 30) < 0.5D) frozenStub(c, cell);
                }
            }
            case DRY -> {
                if (below == BiomicTerrainSampler.SAND && roll < 0.04D) floorPlant(c, cell, "minecraft:deadbush:0");
                else if (cell.floor && roll < 0.045D && unit(h >>> 12) < 0.1D)
                    floorPlace(c, cell, DecorSlots.cave("dry_bones"));
                else if (crawl(c, cell) && roll < 0.03D) set(c, cell.x, cell.y, cell.z, "minecraft:web:0");
            }
            case ROOTED -> {
                if (hanging && (above == BiomicTerrainSampler.ROOTED_DIRT || cell.cover <= 8) && roll < 0.35D)
                    set(c, cell.x, cell.y, cell.z, "biomic:hanging_roots:0");
            }
            case SPIDER -> {
                if (crawl(c, cell) && Integer.bitCount(cell.walls) >= 1 && roll < 0.12D) set(
                    c,
                    cell.x,
                    cell.y,
                    cell.z,
                    unit(h >>> 8) < 0.9D ? "minecraft:web:0" : DecorSlots.cave("spider_bones"));
            }
            case BARE -> {
                if (crawl(c, cell) && Integer.bitCount(cell.walls) >= 2 && roll < 0.004D)
                    set(c, cell.x, cell.y, cell.z, "minecraft:web:0");
            }
            case HOT -> {}
        }
    }

    private boolean crawl(Chunk3 c, Cell cell) {
        return cell.floor
            && (cell.ceiling || solid(c.raw[index(cell.x & 15, Math.min(height - 1, cell.y + 2), cell.z & 15)]));
    }

    /** An icicle stub on the floor under a hanging one. */
    private void frozenStub(Chunk3 c, Cell cell) {
        for (int y = cell.y - 2; y > cell.y - 10 && y > minY; y--) {
            if (c.physical.block(cell.x, y, cell.z) != Blocks.air) return;
            if (c.physical.block(cell.x, y - 1, cell.z)
                .getMaterial()
                .isSolid()) {
                stub(c, "biomic:icicle", cell.x, y, cell.z, 1);
                return;
            }
        }
    }

    /** A column pointing up from the floor at (x,y,z): EFR pointed-dripstone metadata, 5 + thickness. */
    private boolean stub(Chunk3 c, String name, int x, int y, int z, int length) {
        Block block = Block.getBlockFromName(name);
        if (block == null || block == Blocks.air || x >> 4 != c.cx || z >> 4 != c.cz) return false;
        int n = 0;
        while (n < length && y + n < height - 1 && c.physical.block(x, y + n, z) == Blocks.air) n++;
        if (n == 0 || c.budget < n) return false;
        for (int i = 0; i < n; i++) {
            int fromTip = n - 1 - i;
            int thickness = fromTip == 0 ? 0 : fromTip == 1 ? 1 : fromTip == n - 1 ? 3 : 2;
            c.world.setBlock(x, y + i, z, block, 5 + thickness, 2);
        }
        c.budget -= n;
        return true;
    }

    /**
     * The twilight zone: plain lichens on the walls, ferns and vines at every mouth, snow and reindeer lichen when
     * cold, dead bushes and webs when arid.
     */
    private void mouth(Chunk3 c, List<Cell> order, float temperature, float rain) {
        boolean cold = temperature < 0.2F, arid = rain < 0.2F;
        int quota = 4;
        for (Cell cell : order) {
            if (quota <= 0 || c.budget <= 0) return;
            if (!cell.mouth) continue;
            long h = hash(c, cell);
            // Plain lichens (TFC's) on the light-side walls: cobblestone lichen where it is damp or cold, elegant
            // sunburst lichen where it is dry; reindeer lichen clumps on cold floors.
            int rock = rockFaces(c, cell);
            if (rock != 0 && !cell.floor
                && unit(h >>> 24) < 0.3D
                && vine(
                    c,
                    cell.x,
                    cell.y,
                    cell.z,
                    arid ? "biomic:port_sunburst_lichen:0" : "biomic:port_cobblestone_lichen:0",
                    rock)) {
                quota--;
                continue;
            }
            if (cold && cell.floor && unit(h >>> 32) < 0.3D && floorPlace(c, cell, "biomic:port_reindeer_lichen:0")) {
                quota--;
                continue;
            }
            if (cold) {
                if (cell.floor && floorPlace(c, cell, "minecraft:snow_layer:" + (int) Math.floorMod(h, 2L))) quota--;
                else if (cell.ceiling && !cell.floor
                    && unit(h) < 0.4D
                    && icicle(c, cell.x, cell.y, cell.z, 1 + (int) (h >>> 40) % 2)) quota--;
            } else if (arid) {
                if (cell.floor && unit(h) < 0.5D && floorPlace(c, cell, "minecraft:deadbush:0")) quota--;
                else if (cell.ceiling && cell.walls != 0
                    && unit(h) < 0.2D
                    && set(c, cell.x, cell.y, cell.z, "minecraft:web:0")) quota--;
            } else if (cell.walls != 0 && !cell.floor && unit(h) < 0.5D) {
                String vine = temperature >= 0.9F && rain >= 0.7F ? "minecraft:vine:0" : DecorSlots.cave("mouth_vine");
                if (curtain(c, cell, vine, 3 + (int) Math.floorMod(h >>> 20, 4L)) > 0) quota--;
            } else if (cell.floor) {
                String plant = unit(h >>> 8) < 0.5D ? "minecraft:tallgrass:2"
                    : unit(h >>> 16) < 0.5D ? DecorSlots.cave("mouth_fern") : DecorSlots.cave("mouth_bush");
                if (floorPlant(c, cell, plant)) quota--;
            }
        }
    }

    /**
     * Glow lichen in colonies, not white noise: on a 3D grid of about one colony per two or three cave chunks, a
     * colony grows only where its centre is a cave cell by still water or at a mouth's inner edge; it thins to its
     * rim (radius 4 to 9).
     */
    private void lichenColonies(Chunk3 c, List<Cell> order, List<int[]> water) {
        for (Cell cell : order) {
            if (c.budget <= 0) return;
            if (cell.floor && cell.ceiling) continue;
            double inside = colony(c, cell, water);
            if (inside < 0 || unit(hash(c, cell) >>> 3) >= 0.85D * (1 - inside)) continue;
            int faces = lichenFaces(cell);
            if (faces != 0) lichen(c, cell.x, cell.y, cell.z, faces);
        }
    }

    private double colony(Chunk3 c, Cell cell, List<int[]> water) {
        int gx = Math.floorDiv(cell.x, COLONY_CELL), gy = Math.floorDiv(cell.y, 24),
            gz = Math.floorDiv(cell.z, COLONY_CELL);
        double best = -1;
        for (int ox = -1; ox <= 1; ox++) for (int oy = -1; oy <= 1; oy++) for (int oz = -1; oz <= 1; oz++) {
            long h = WorldgenContext.mix64(
                c.world.getSeed() ^ (gx + ox) * 0x9E3779B97F4A7C15L
                    ^ WorldgenContext
                        .mix64((gy + oy) * 0x165667B19E3779F9L ^ (gz + oz) * 0xC2B2AE3D27D4EB4FL ^ 0x4C696368656EL));
            if (unit(h) >= 0.3D) continue;
            int x = (gx + ox) * COLONY_CELL + (int) Math.floorMod(h >>> 8, (long) COLONY_CELL);
            int y = (gy + oy) * 24 + (int) Math.floorMod(h >>> 20, 24L);
            int z = (gz + oz) * COLONY_CELL + (int) Math.floorMod(h >>> 32, (long) COLONY_CELL);
            double radius = 4 + Math.floorMod(h >>> 44, 6L);
            double dx = cell.x - x, dy = (cell.y - y) * 1.5D, dz = cell.z - z;
            double reach = Math.sqrt(dx * dx + dy * dy + dz * dz) / radius;
            if (reach > 1) continue;
            // Colonies are fullest where it is damp or lit: still water or a mouth nearby.
            if (!cell.mouth && !wetNear(c, cell, water)) reach = reach * 0.7D + 0.3D;
            if (best < 0 || reach < best) best = reach;
        }
        return best;
    }

    /** A rare garden of BOP Miner's Delight on a stone floor with a lichen halo, at most one per 4x4 chunks. */
    private void minersDelight(Chunk3 c, List<Cell> order, float temperature) {
        if (temperature < 0.3F || temperature > 1.2F) return;
        long region = WorldgenContext
            .mix64(c.world.getSeed() ^ (c.cx >> 2) * 0x9E3779B97F4A7C15L ^ (c.cz >> 2) * 0xC2B2AE3D27D4EB4FL);
        if (unit(region) >= 0.12D || Math.floorMod(region >>> 20, 16L) != (c.cx & 3) * 4 + (c.cz & 3)) return;
        Cell centre = first(
            order,
            cell -> cell.floor && !cell.mouth && authored(cell.y) >= 0 && authored(cell.y) <= 40);
        if (centre == null) return;
        int flowers = 6 + (int) Math.floorMod(region >>> 40, 9L);
        for (Cell cell : order) {
            if (flowers <= 0 || c.budget <= 0) return;
            if (!near(cell, centre, 4)) continue;
            if (cell.floor && floorPlant(c, cell, DecorSlots.cave("miners_delight"))) flowers--;
            else if (!cell.floor && unit(hash(c, cell)) < 0.3D) {
                int faces = lichenFaces(cell);
                if (faces != 0) lichen(c, cell.x, cell.y, cell.z, faces);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ writers

    private boolean floorPlace(Chunk3 c, Cell cell, String id) {
        if (c.floorBudget <= 0) return false;
        if (!set(c, cell.x, cell.y, cell.z, id)) return false;
        c.floorBudget--;
        return true;
    }

    private boolean floorPlant(Chunk3 c, Cell cell, String id) {
        if (c.floorBudget <= 0 || c.budget <= 0) return false;
        FloraPlants.Plant plant = FloraPlants.resolve(id);
        if (plant == null) return false;
        int blocks;
        try {
            blocks = plant.place(c.world, c.random, cell.x, cell.y, cell.z);
        } catch (RuntimeException failure) {
            FloraPlants.disable(plant, failure);
            return false;
        }
        if (blocks == 0) return false;
        c.budget -= blocks;
        c.floorBudget--;
        return true;
    }

    /**
     * A vanilla mushroom on rock by Forge's cave-plant rule without its light term: while chunks populate, the light
     * depends on which neighbours exist yet (on generation order), and a cave cell away from the mouths is dark anyway.
     */
    private boolean caveMushroom(Chunk3 c, Cell cell, Block mushroom) {
        if (cell.mouth || c.floorBudget <= 0 || c.budget <= 0) return false;
        int x = cell.x, y = cell.y, z = cell.z;
        if (c.physical.block(x, y, z) != Blocks.air || !c.physical.block(x, y - 1, z)
            .isSideSolid(c.world, x, y - 1, z, ForgeDirection.UP)) return false;
        c.world.setBlock(x, y, z, mushroom, 0, 2);
        if (c.world.getBlock(x, y, z) != mushroom) return false;
        c.budget--;
        c.floorBudget--;
        return true;
    }

    /** A block by its own placement rule into an air cell (or, with {@code replaceFluid}, a still fluid top). */
    private boolean set(Chunk3 c, int x, int y, int z, String id) {
        if (c.budget <= 0 || x >> 4 != c.cx || z >> 4 != c.cz) return false;
        FloraPlants.Plant plant = FloraPlants.resolve(id);
        if (plant == null) return false;
        Block current = c.physical.block(x, y, z);
        if (current != Blocks.air) return false;
        if (current == Blocks.air && !plant.block.canPlaceBlockAt(c.world, x, y, z)) return false;
        c.world.setBlock(x, y, z, plant.block, Math.max(0, plant.metadata), 2);
        if (c.world.getBlock(x, y, z) != plant.block) return false;
        if (!plant.block.canBlockStay(c.world, x, y, z)) {
            c.world.setBlock(x, y, z, current, 0, 2);
            return false;
        }
        c.budget--;
        return true;
    }

    /** A vine-type block on the given sides (vanilla vine metadata). */
    private boolean vine(Chunk3 c, int x, int y, int z, String id, int faces) {
        FloraPlants.Plant plant = FloraPlants.resolve(id);
        if (plant == null || !(plant.block instanceof net.minecraft.block.BlockVine) || faces == 0) return false;
        if (c.budget <= 0 || x >> 4 != c.cx || z >> 4 != c.cz || c.physical.block(x, y, z) != Blocks.air) return false;
        c.world.setBlock(x, y, z, plant.block, faces, 2);
        if (!plant.block.canBlockStay(c.world, x, y, z)) {
            c.world.setBlock(x, y, z, Blocks.air, 0, 2);
            return false;
        }
        c.budget--;
        return true;
    }

    /** The cell's wall faces (vine face bits) that are rock, not ice or snow: where lichen grows. */
    private static int rockFaces(Chunk3 c, Cell cell) {
        int faces = 0;
        int[][] sides = { { 0, 1, 1 }, { -1, 0, 2 }, { 0, -1, 4 }, { 1, 0, 8 } };
        for (int[] side : sides)
            if ((cell.walls & side[2]) != 0 && c.physical.block(cell.x + side[0], cell.y, cell.z + side[1])
                .getMaterial() == Material.rock) faces |= side[2];
        return faces;
    }

    private int curtain(Chunk3 c, Cell cell, String id, int length) {
        int placed = 0;
        for (int i = 0; i < length; i++) {
            if (!vine(c, cell.x, cell.y - i, cell.z, id, cell.walls)) break;
            placed++;
        }
        return placed;
    }

    /** A hanging icicle of the given length from the ceiling above (EFR pointed-dripstone geometry, 0..4 down). */
    private boolean icicle(Chunk3 c, int x, int y, int z, int length) {
        return pointed(c, "biomic:icicle", x, y, z, length);
    }

    /**
     * A pointed column: length > 0 hangs down from the ceiling above (x,y,z), length -1 is a one-block floor stub.
     * Metadata: thickness 0 tip, 1 frustum, 2 middle, 3 base; plus 5 when pointing up.
     */
    private boolean pointed(Chunk3 c, String name, int x, int y, int z, int length) {
        Block block = Block.getBlockFromName(name);
        if (block == null || block == Blocks.air || x >> 4 != c.cx || z >> 4 != c.cz) return false;
        if (length < 0) {
            if (c.physical.block(x, y, z) != Blocks.air || c.budget <= 0) return false;
            c.world.setBlock(x, y, z, block, 5, 2);
            c.budget--;
            return true;
        }
        int n = 0;
        while (n < length && y - n > minY && c.physical.block(x, y - n, z) == Blocks.air) n++;
        if (n == 0 || c.budget < n) return false;
        for (int i = 0; i < n; i++) {
            int fromTip = n - 1 - i;
            int thickness = fromTip == 0 ? 0 : fromTip == 1 ? 1 : fromTip == n - 1 ? 3 : 2;
            c.world.setBlock(x, y - i, z, block, thickness, 2);
        }
        c.budget -= n;
        return true;
    }

    private boolean caveVines(Chunk3 c, Cell cell, int length, long hash) {
        int n = 0;
        while (n < length && cell.y - n > minY && c.physical.block(cell.x, cell.y - n, cell.z) == Blocks.air) n++;
        if (n == 0 || c.budget < n) return false;
        for (int i = 0; i < n; i++) {
            boolean berries = unit(hash >>> i) < 0.15D;
            String state = (i == n - 1 ? "minecraft:cave_vines" : "minecraft:cave_vines_plant") + "[berries="
                + berries
                + "]";
            if (!c.physical.canResolve(state)) return i > 0;
            c.physical.setBlock(cell.x, cell.y - i, cell.z, state);
        }
        // EFR's tile picks its maximum length from an unseeded Random: fix it from the seed instead.
        TileEntity tip = c.world.getTileEntity(cell.x, cell.y - n + 1, cell.z);
        if (tip instanceof TileEntityCaveVines) {
            NBTTagCompound nbt = new NBTTagCompound();
            tip.writeToNBT(nbt);
            nbt.setInteger("MaxLength", 2 + (int) Math.floorMod(hash >>> 40, 26L));
            tip.readFromNBT(nbt);
        }
        c.budget -= n;
        return true;
    }

    private boolean lichen(Chunk3 c, int x, int y, int z, int faces) {
        if (c.budget <= 0 || x >> 4 != c.cx || z >> 4 != c.cz || c.physical.block(x, y, z) != Blocks.air) return false;
        String state = lichenState(faces);
        if (!c.physical.canResolve(state)) return false;
        c.physical.setBlock(x, y, z, state);
        c.budget--;
        return true;
    }

    /** Glow lichen faces on solid neighbours: bits down, up, north, south, west, east. */
    private int lichenFaces(Cell cell) {
        int faces = 0;
        if (cell.floor) faces |= 1;
        if (cell.ceiling) faces |= 2;
        if ((cell.walls & 4) != 0) faces |= 4;
        if ((cell.walls & 1) != 0) faces |= 8;
        if ((cell.walls & 2) != 0) faces |= 16;
        if ((cell.walls & 8) != 0) faces |= 32;
        return faces;
    }

    static String lichenState(int faces) {
        String[] names = { "down", "up", "north", "south", "west", "east" };
        StringBuilder state = new StringBuilder("minecraft:glow_lichen[");
        for (int i = 0; i < 6; i++) {
            if (i > 0) state.append(',');
            state.append(names[i])
                .append('=')
                .append((faces & (1 << i)) != 0);
        }
        return state.append(']')
            .toString();
    }

    // ------------------------------------------------------------------------------------------------ helpers

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
                String state = NativeBlockCatalogue.encode(block, metadata);
                try {
                    if (state == null) state = ModernStateCompatibility.modernState(block, metadata);
                } catch (RuntimeException unsupported) {
                    state = null;
                }
                if (state == null || !physical.canResolve(state)) {
                    Biomic.LOG
                        .debug("Cave decor cannot write {}:{}", Block.blockRegistry.getNameForObject(block), metadata);
                    return false;
                }
                physical.setBlock(x, y, z, state);
                return true;
            }

            @Override
            public int minimumY() {
                return 0;
            }

            @Override
            public int maximumYExclusive() {
                return height;
            }
        };
    }

    private boolean generic(BiomeVolume volume, int lx, int y, int lz) {
        if (volume == null || !volume.containsBlockY(y)) return true;
        return "biomic:surface".equals(volume.getBiomeKey(lx >> 2, y >> 2, lz >> 2));
    }

    /** A cell is in a mouth's twilight zone when a nearby column of this chunk is open to the sky at its height. */
    private static boolean mouth(int[] ground, int lx, int lz, int y) {
        for (int dz = -MOUTH_REACH; dz <= MOUTH_REACH; dz += 2)
            for (int dx = -MOUTH_REACH; dx <= MOUTH_REACH; dx += 2) {
                int x = lx + dx, z = lz + dz;
                if (x < 0 || x > 15 || z < 0 || z > 15) continue;
                int top = ground[z * 16 + x];
                if (top >= 0 && top <= y) return true;
            }
        return false;
    }

    private boolean wetNear(Chunk3 c, Cell cell, List<int[]> water) {
        for (int[] pool : water) {
            int dx = (c.cx << 4) + pool[0] - cell.x, dy = pool[1] - cell.y, dz = (c.cz << 4) + pool[2] - cell.z;
            if (dx * dx + dy * dy + dz * dz <= 64) return true;
        }
        return false;
    }

    private int authored(int y) {
        return themes.authored(y);
    }

    private int index(int lx, int y, int lz) {
        return (lx * 16 + lz) * height + y;
    }

    private static boolean solid(byte material) {
        return material != BiomicTerrainSampler.AIR && material != BiomicTerrainSampler.WATER
            && material != BiomicTerrainSampler.LAVA;
    }

    private static Cell first(List<Cell> order, java.util.function.Predicate<Cell> test) {
        for (Cell cell : order) if (test.test(cell)) return cell;
        return null;
    }

    private static boolean near(Cell a, Cell b, int radius) {
        return distance(a, b) <= radius;
    }

    private static double distance(Cell a, Cell b) {
        double dx = a.x - b.x, dy = (a.y - b.y) * 1.5D, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static long hash(Chunk3 c, Cell cell) {
        return WorldgenContext.mix64(
            c.seed ^ cell.x * 0x9E3779B97F4A7C15L
                ^ WorldgenContext.mix64(cell.z * 0xC2B2AE3D27D4EB4FL ^ cell.y * 0x165667B19E3779F9L));
    }

    private static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }

    public String report() {
        StringBuilder cells = new StringBuilder();
        themeCells.forEach(
            (theme, count) -> cells.append(' ')
                .append(theme)
                .append('=')
                .append(count[0]));
        return "Cave decor: " + chunks
            + " cave chunks, "
            + placed
            + " placements; cave cells by theme"
            + cells
            + "; spots "
            + samples;
    }
}
