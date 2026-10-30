package industries.angelic.biomic.compat.biome;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenReed;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Native reed/cane columns with whole-proposal air and support checks. */
public final class BopNativeCane {

    public static final String RIVER_CANE = "biomesoplenty:plants[legacy_meta=8]";

    private record Pos(int x, int y, int z) {}

    private record Cell(Block block, int meta) {}

    private BopNativeCane() {}

    public static String encode(Block block, int meta, boolean river) {
        if (river && RIVER_CANE.equals(NativeBlockCatalogue.encode(block, meta))) return RIVER_CANE;
        if (!river && block == Blocks.reeds && meta == 0) return "minecraft:sugar_cane[age=0]";
        throw new IllegalStateException("Unexpected native cane state");
    }

    public static boolean place(World world, NativeFeatureCapture.Access physical, Random random,
        WorldGenerator generator, boolean river, int x, int y, int z) {
        if (river ? !generator.getClass()
            .getName()
            .equals("biomesoplenty.common.world.features.WorldGenRiverCane")
            : generator.getClass() != WorldGenReed.class)
            throw new IllegalArgumentException("Unaudited native cane generator");
        Map<Pos, Cell> proposal = new LinkedHashMap<>();
        var access = new NativeFeatureCapture.Access() {

            void check(int px, int pz) {
                if (Math.abs((long) px - x) > 12 || Math.abs((long) pz - z) > 12)
                    throw NativeFeatureCapture.outside("Native cane escaped envelope");
            }

            public Block block(int px, int py, int pz) {
                check(px, pz);
                Cell c = proposal.get(new Pos(px, py, pz));
                return c == null ? physical.block(px, py, pz) : c.block;
            }

            public int metadata(int px, int py, int pz) {
                check(px, pz);
                Cell c = proposal.get(new Pos(px, py, pz));
                return c == null ? physical.metadata(px, py, pz) : c.meta;
            }

            public int heightValue(int px, int pz) {
                check(px, pz);
                return physical.heightValue(px, pz);
            }

            public int minimumY() {
                return physical.minimumY();
            }

            public int maximumYExclusive() {
                return physical.maximumYExclusive();
            }

            public boolean set(int px, int py, int pz, Block block, int meta) {
                check(px, pz);
                encode(block, meta, river);
                if (py < y || py > y + 3 || proposal.size() >= 80)
                    throw new IllegalStateException("Native cane proposal exceeded size");
                proposal.put(new Pos(px, py, pz), new Cell(block, meta));
                return true;
            }
        };
        boolean valid = NativeFeatureCapture.run(world, new BopSurfaceCapture(access, random), () -> {
            if (!generator.generate(world, random, x, y, z) || proposal.isEmpty()) return false;
            for (var entry : proposal.entrySet()) {
                Pos p = entry.getKey();
                Cell c = entry.getValue();
                if (!NativeFeatureCapture.containsFeatureY(physical, p.y)
                    || physical.block(p.x, p.y, p.z) != Blocks.air) return false;
                if (river) {
                    if (access.block(p.x, p.y - 1, p.z) == c.block && access.metadata(p.x, p.y - 1, p.z) != 8)
                        return false;
                    if (!c.block.canReplace(world, p.x, p.y, p.z, 0, new ItemStack(c.block, 1, c.meta))) return false;
                } else if (!Blocks.reeds.canBlockStay(world, p.x, p.y, p.z)) return false;
            }
            return true;
        });
        if (!valid) return false;
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            Cell c = entry.getValue();
            if (!physical.set(p.x, p.y, p.z, c.block, c.meta)) return false;
        }
        return true;
    }
}
