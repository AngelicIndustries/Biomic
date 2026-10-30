package industries.angelic.biomic.compat.biome;

import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Native Lush Desert waterside grass replacement, never a lake or a terrain filler. */
public final class BopOasisBank {

    private record Pos(int x, int y, int z) {}

    private BopOasisBank() {}

    public static String identity(WorldGenerator generator) {
        if (!generator.getClass()
            .getName()
            .equals("biomesoplenty.common.world.features.WorldGenWaterside"))
            throw new IllegalStateException("Unaudited oasis bank class");
        var values = new TreeMap<String, Object>();
        try {
            for (var f : generator.getClass()
                .getDeclaredFields()) if (!Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    values.put(f.getName(), f.get(generator));
                }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot inspect native oasis bank", e);
        }
        if (!values.keySet()
            .equals(Set.of("watersideBlock", "radius", "sideBlocks"))
            || values.get("watersideBlock") != Blocks.grass
            || !Integer.valueOf(7)
                .equals(values.get("radius"))
            || !List.of(Blocks.sand, Blocks.hardened_clay)
                .equals(values.get("sideBlocks")))
            throw new IllegalStateException("Native oasis bank parameters changed");
        return generator.getClass()
            .getName() + "{grass,radius7,sand+terracotta}";
    }

    /** Native solid-only height predicate restricted to this surface's eight-block pond horizon. */
    public static int anchorY(NativeFeatureCapture.Access view, int x, int y, int z) {
        for (int at = y; at >= Math.max(view.minimumY(), y - 8); at--) {
            Material material = view.block(x, at, z)
                .getMaterial();
            if (material.blocksMovement() && material != Material.leaves) return at + 1;
        }
        return -1;
    }

    public static boolean place(World world, NativeFeatureCapture.Access physical, Random random,
        WorldGenerator generator, int x, int y, int z) {
        identity(generator);
        if (!NativeFeatureCapture.containsFeatureY(physical, y)) return false;
        var proposal = new LinkedHashSet<Pos>();
        var view = new NativeFeatureCapture.Access() {

            void check(int px, int py, int pz) {
                if (Math.abs((long) px - x) > 6 || Math.abs((long) pz - z) > 6 || Math.abs((long) py - y) > 3)
                    throw NativeFeatureCapture.outside("Oasis bank escaped native envelope");
            }

            public Block block(int px, int py, int pz) {
                check(px, py, pz);
                return proposal.contains(new Pos(px, py, pz)) ? Blocks.grass : physical.block(px, py, pz);
            }

            public int metadata(int px, int py, int pz) {
                check(px, py, pz);
                return proposal.contains(new Pos(px, py, pz)) ? 0 : physical.metadata(px, py, pz);
            }

            public boolean set(int px, int py, int pz, Block b, int m) {
                check(px, py, pz);
                if (b != Blocks.grass || m != 0 || proposal.size() >= 625)
                    throw new IllegalStateException("Invalid native oasis write");
                proposal.add(new Pos(px, py, pz));
                return true;
            }

            public int minimumY() {
                return physical.minimumY();
            }

            public int maximumYExclusive() {
                return physical.maximumYExclusive();
            }
        };
        if (!NativeFeatureCapture
            .run(world, new BopSurfaceCapture(view, random), () -> generator.generate(world, random, x, y, z))
            || proposal.isEmpty()) return false;
        for (Pos p : proposal) {
            if (!NativeFeatureCapture.containsFeatureY(physical, p.y)) return false;
            Block old = physical.block(p.x, p.y, p.z);
            if (old != Blocks.sand && old != Blocks.hardened_clay) return false;
            if (!proposal.contains(new Pos(p.x, p.y + 1, p.z))) {
                Material above = physical.block(p.x, p.y + 1, p.z)
                    .getMaterial();
                if (above == Material.plants || above == Material.vine
                    || above == Material.wood
                    || above == Material.leaves
                    || above == Material.cactus) return false;
            }
            if (!proposal.contains(new Pos(p.x, p.y - 1, p.z))) {
                Material below = physical.block(p.x, p.y - 1, p.z)
                    .getMaterial();
                if (!below.isSolid() || below == Material.wood || below == Material.leaves) return false;
            }
        }
        for (Pos p : proposal) if (!physical.set(p.x, p.y, p.z, Blocks.grass, 0)) return false;
        return true;
    }
}
