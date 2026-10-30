package industries.angelic.biomic.compat.biome;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenBigMushroom;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Exact vanilla mushroom geometry, fitted as one transaction. No partial caps or tree fusion. */
public final class BopGiantMushroom {

    private record Pos(int x, int y, int z) {}

    private record Cell(Block block, int meta) {}

    private BopGiantMushroom() {}

    public static String identity(WorldGenerator generator) {
        if (generator == null || generator.getClass() != WorldGenBigMushroom.class)
            throw new IllegalStateException("Unaudited native giant mushroom generator");
        int fields = 0;
        try {
            for (var field : WorldGenBigMushroom.class.getDeclaredFields())
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    if (field.getType() != int.class || field.getInt(generator) != -1)
                        throw new IllegalStateException("Native giant mushroom is not the mixed red/brown selector");
                    fields++;
                }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot inspect native giant mushroom", error);
        }
        if (fields != 1) throw new IllegalStateException("Native giant mushroom layout changed");
        return "net.minecraft.world.gen.feature.WorldGenBigMushroom{mushroomType=-1}";
    }

    public static String encode(Block block, int meta) {
        if (block != Blocks.brown_mushroom_block && block != Blocks.red_mushroom_block || meta < 0 || meta > 10)
            throw new IllegalStateException("Unexpected native giant mushroom state");
        return (block == Blocks.brown_mushroom_block ? "minecraft:brown_mushroom_block"
            : "minecraft:red_mushroom_block") + "[legacy_meta=" + meta + "]";
    }

    /** The caller buffers all writes and discards them on false/exception (habitat or ceiling rejection). */
    public static boolean place(World world, NativeFeatureCapture.Access physical, Random random,
        WorldGenerator generator, int x, int y, int z) {
        identity(generator);
        var proposal = new LinkedHashMap<Pos, Cell>();
        var clearance = new LinkedHashSet<Pos>();
        var ideal = new NativeFeatureCapture.Access() {

            void check(int px, int py, int pz) {
                if (Math.abs((long) px - x) > 3 || Math.abs((long) pz - z) > 3 || py < y - 1L || py > y + 7L)
                    throw NativeFeatureCapture.outside("Native giant mushroom escaped bounded envelope");
            }

            public Block block(int px, int py, int pz) {
                check(px, py, pz);
                Pos p = new Pos(px, py, pz);
                Cell cell = proposal.get(p);
                if (py >= y) clearance.add(p);
                return cell != null ? cell.block : py >= y ? Blocks.air : physical.block(px, py, pz);
            }

            public int metadata(int px, int py, int pz) {
                check(px, py, pz);
                Cell cell = proposal.get(new Pos(px, py, pz));
                return cell == null ? 0 : cell.meta;
            }

            public boolean set(int px, int py, int pz, Block block, int meta) {
                check(px, py, pz);
                encode(block, meta);
                if (proposal.size() >= 256) throw new IllegalStateException("Oversized native mushroom proposal");
                proposal.put(new Pos(px, py, pz), new Cell(block, meta));
                return true;
            }

            public int heightValue(int px, int pz) {
                throw new IllegalStateException("Unexpected mushroom height query");
            }

            public int minimumY() {
                return physical.minimumY();
            }

            public int maximumYExclusive() {
                return physical.maximumYExclusive();
            }
        };
        if (!NativeFeatureCapture
            .run(world, new BopSurfaceCapture(ideal, random), () -> generator.generate(world, random, x, y, z))
            || proposal.isEmpty()) return false;
        // Native permits leaves and can carve a cap through a canopy. Preserve its shape but reject
        // that placement: every source clearance cell and every written cap/stem cell must be air.
        clearance.addAll(proposal.keySet());
        for (Pos p : clearance)
            if (!NativeFeatureCapture.containsFeatureY(physical, p.y) || physical.block(p.x, p.y, p.z) != Blocks.air)
                return false;
        // A narrow source stem clearance permits a neighboring trunk inside the cap's footprint.
        // Check the entire mushroom envelope plus one horizontal cell, not just its written shell.
        int top = proposal.keySet()
            .stream()
            .mapToInt(Pos::y)
            .max()
            .orElseThrow();
        for (int px = x - 4; px <= x + 4; px++)
            for (int pz = z - 4; pz <= z + 4; pz++) for (int py = y; py <= top + 1; py++) {
                Material material = physical.block(px, py, pz)
                    .getMaterial();
                if (material == Material.wood || material == Material.leaves) return false;
            }
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            Cell cell = entry.getValue();
            if (!physical.set(p.x, p.y, p.z, cell.block, cell.meta)) return false;
        }
        return true;
    }
}
