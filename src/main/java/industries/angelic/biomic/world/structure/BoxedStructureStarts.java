package industries.angelic.biomic.world.structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureMineshaftPieces;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.StructureStrongholdPieces;

import industries.angelic.biomic.mixins.early.minecraft.AccessorMineshaftCorridor;
import industries.angelic.biomic.mixins.early.minecraft.AccessorStructureComponent;
import industries.angelic.biomic.world.WorldgenContext;

/**
 * Draws the structure starts that reach one chunk's population box the same whatever order chunks populate in.
 * Vanilla draws them in its map's hash order (which depends on how many starts it holds) with the population's one
 * random, and drops a mineshaft or stronghold piece for good when the box being drawn finds liquid at its edges, so
 * the first box to populate decided for every other. Here starts go in a fixed order, each piece with its own random
 * for this box, and such a piece is drawn only if no liquid touches its whole hull in the provided terrain: one test,
 * the same for every box.
 */
final class BoxedStructureStarts {

    private final Map<StructureComponent, Boolean> fits = new WeakHashMap<>();

    /** {@code seed}: the structure kind's own (world seed and a salt). */
    boolean draw(Map<Long, StructureStart> starts, World world, long seed, int chunkX, int chunkZ,
        StructurePopulationBox.Terrain provided) {
        int minX = (chunkX << 4) + 8, minZ = (chunkZ << 4) + 8;
        StructureBoundingBox box = new StructureBoundingBox(minX, minZ, minX + 15, minZ + 15);
        List<StructureStart> reaching = new ArrayList<>();
        for (StructureStart start : starts.values()) if (start.isSizeableStructure() && start.getBoundingBox()
            .intersectsWith(box)) reaching.add(start);
        reaching.sort(
            Comparator.comparingInt(StructureStart::func_143019_e)
                .thenComparingInt(StructureStart::func_143018_f));
        for (StructureStart start : reaching) {
            long startSeed = WorldgenContext.mix64(
                seed ^ start.func_143019_e() * 0x9E3779B97F4A7C15L
                    ^ start.func_143018_f() * 0xC2B2AE3D27D4EB4FL
                    ^ chunkX * 0x165667B19E3779F9L
                    ^ chunkZ * 0x27D4EB2F165667C5L);
            // As StructureStart#generateStructure would (MixinStructureStartVillage): a village's pieces find its
            // ground.
            VillageContext.enterStart(start);
            try {
                int index = -1;
                for (StructureComponent piece : start.getComponents()) if (++index >= 0 && piece.getBoundingBox()
                    .intersectsWith(box)) {
                        if (!fits(piece, provided)) continue;
                        boolean spiders = piece instanceof StructureMineshaftPieces.Corridor
                            && ((AccessorMineshaftCorridor) piece).biomic$hasSpiders();
                        // Vanilla rolls the spawner's spot per box and keeps the first that lands inside its box.
                        if (spiders) ((AccessorMineshaftCorridor) piece).biomic$setSpawnerPlaced(true);
                        // Each piece its own random: a shared one handed a piece whatever an earlier piece had drawn,
                        // and pieces that draw by what they read (crops, decorations) made that depend on order. Keyed
                        // by its place in the start's list, never its class name, which obfuscation and package
                        // renames change (the release jar would furnish the same seed's houses differently).
                        StructureBoundingBox hull = piece.getBoundingBox();
                        Random random = new Random(
                            WorldgenContext.mix64(
                                startSeed ^ hull.minX * 0x9E3779B97F4A7C15L
                                    ^ hull.minZ * 0xC2B2AE3D27D4EB4FL
                                    ^ hull.maxX * 0x165667B19E3779F9L
                                    ^ hull.maxZ * 0x27D4EB2F165667C5L
                                    ^ index * 0x94D049BB133111EBL));
                        piece.addComponentParts(world, random, box);
                        if (spiders) placeSpawner(world, seed, piece, box);
                    }
            } finally {
                VillageContext.exitStart();
            }
        }
        return !reaching.isEmpty();
    }

    /** A spider corridor's one cave-spider spawner, at a spot chosen from the corridor alone (as vanilla's rule). */
    private static void placeSpawner(World world, long seed, StructureComponent corridor, StructureBoundingBox box) {
        StructureBoundingBox hull = corridor.getBoundingBox();
        Random random = new Random(
            WorldgenContext.mix64(
                seed ^ 0x5A1DE45L
                    ^ hull.minX * 0x9E3779B97F4A7C15L
                    ^ hull.minY * 0x165667B19E3779F9L
                    ^ hull.minZ * 0xC2B2AE3D27D4EB4FL));
        int section = random.nextInt(Math.max(1, ((AccessorMineshaftCorridor) corridor).biomic$sectionCount()));
        int along = 2 + section * 5 - 1 + random.nextInt(3);
        AccessorStructureComponent offsets = (AccessorStructureComponent) corridor;
        int x = offsets.biomic$x(1, along), y = offsets.biomic$y(0), z = offsets.biomic$z(1, along);
        if (!box.isVecInside(x, y, z)) return;
        world.setBlock(x, y, z, Blocks.mob_spawner, 0, 2);
        if (world.getTileEntity(x, y, z) instanceof TileEntityMobSpawner spawner) spawner.func_145881_a()
            .setEntityName("CaveSpider");
    }

    private boolean fits(StructureComponent piece, StructurePopulationBox.Terrain provided) {
        if (!checksLiquid(piece)) return true;
        Boolean known = fits.get(piece);
        if (known == null) fits.put(piece, known = !liquidOnHull(piece.getBoundingBox(), provided));
        return known;
    }

    /** The pieces vanilla drops on liquid: every mineshaft piece, every stronghold piece but the portal room. */
    private static boolean checksLiquid(StructureComponent piece) {
        return piece instanceof StructureMineshaftPieces.Corridor || piece instanceof StructureMineshaftPieces.Cross
            || piece instanceof StructureMineshaftPieces.Room
            || piece instanceof StructureMineshaftPieces.Stairs
            || piece.getClass()
                .getEnclosingClass() == StructureStrongholdPieces.class
                && !(piece instanceof StructureStrongholdPieces.PortalRoom);
    }

    /** Vanilla's isLiquidInStructureBoundingBox over the whole hull, one block out, in the provided terrain. */
    private static boolean liquidOnHull(StructureBoundingBox hull, StructurePopulationBox.Terrain provided) {
        int minX = hull.minX - 1, minY = Math.max(0, hull.minY - 1), minZ = hull.minZ - 1;
        int maxX = hull.maxX + 1, maxY = Math.min(511, hull.maxY + 1), maxZ = hull.maxZ + 1;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
            if (liquid(provided, x, minY, z) || liquid(provided, x, maxY, z)) return true;
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++)
            if (liquid(provided, x, y, minZ) || liquid(provided, x, y, maxZ)) return true;
        for (int z = minZ; z <= maxZ; z++) for (int y = minY; y <= maxY; y++)
            if (liquid(provided, minX, y, z) || liquid(provided, maxX, y, z)) return true;
        return false;
    }

    private static boolean liquid(StructurePopulationBox.Terrain provided, int x, int y, int z) {
        return provided.block(x, y, z)
            .getMaterial()
            .isLiquid();
    }
}
