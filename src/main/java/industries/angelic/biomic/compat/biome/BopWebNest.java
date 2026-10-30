package industries.angelic.biomic.compat.biome;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Native Silkglades web geometry, transactionally fitted; creature spawning has a separate owner. */
public final class BopWebNest {

    private record Pos(int x, int y, int z) {}

    private BopWebNest() {}

    public static String identity(WorldGenerator generator) {
        if (generator == null || !generator.getClass()
            .getName()
            .equals("biomesoplenty.common.world.features.WorldGenCobwebNest")
            || Arrays.stream(
                generator.getClass()
                    .getDeclaredFields())
                .anyMatch(f -> !java.lang.reflect.Modifier.isStatic(f.getModifiers())))
            throw new IllegalArgumentException("Unaudited native web nest");
        return generator.getClass()
            .getName() + "{native-radii2-1,grass-root,spider-draw-preserved-no-spawn}";
    }

    /** Also used by the direct-source oracle. Never admits arbitrary entity side effects. */
    public static final class GeometryCapture extends BopSurfaceCapture
        implements NativeFeatureCapture.EntitySpawnAccess {

        private int suppressedSpiders;

        public GeometryCapture(NativeFeatureCapture.Access access, Random random) {
            super(access, random);
        }

        @Override
        public boolean spawnEntity(Entity entity) {
            if (entity == null || entity.getClass() != EntitySpider.class || ++suppressedSpiders > 1)
                throw new IllegalStateException("Unexpected native web-nest entity side effect");
            return false;
        }
    }

    /** Physical writes are buffered by the caller; false/exception must discard the entire proposal. */
    public static boolean place(World world, NativeFeatureCapture.Access physical, Random random,
        WorldGenerator generator, int x, int y, int z) {
        identity(generator);
        if (!NativeFeatureCapture.containsFeatureY(physical, y)) return false;
        Set<Pos> proposal = new LinkedHashSet<>();
        var view = new NativeFeatureCapture.Access() {

            void check(int px, int py, int pz) {
                if (Math.abs((long) px - x) > 2 || Math.abs((long) pz - z) > 2
                    || py < physical.minimumY()
                    || py > Math.min((long) physical.maximumYExclusive() + 1, y + 2L))
                    throw NativeFeatureCapture.outside("Native web nest escaped bounded descent/envelope");
            }

            public Block block(int px, int py, int pz) {
                check(px, py, pz);
                return proposal.contains(new Pos(px, py, pz)) ? Blocks.web : physical.block(px, py, pz);
            }

            public int metadata(int px, int py, int pz) {
                check(px, py, pz);
                return proposal.contains(new Pos(px, py, pz)) ? 0 : physical.metadata(px, py, pz);
            }

            public boolean set(int px, int py, int pz, Block block, int meta) {
                check(px, py, pz);
                if (block != Blocks.web || meta != 0 || proposal.size() >= 35)
                    throw new IllegalStateException("Native web nest wrote unexpected state");
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
        boolean generated = NativeFeatureCapture
            .run(world, new GeometryCapture(view, random), () -> generator.generate(world, random, x, y, z));
        if (!generated || proposal.isEmpty()) return false;
        for (Pos p : proposal) {
            if (!NativeFeatureCapture.containsFeatureY(physical, p.y)) return false;
            Block existing = physical.block(p.x, p.y, p.z);
            if (existing != Blocks.air && existing != Blocks.web) return false;
            if (proposal.contains(new Pos(p.x, p.y - 1, p.z))) continue;
            Block ground = physical.block(p.x, p.y - 1, p.z);
            Material material = ground.getMaterial();
            if (!material.isSolid() || material == Material.wood || material == Material.leaves) return false;
        }
        for (Pos p : proposal) if (!physical.set(p.x, p.y, p.z, Blocks.web, 0)) return false;
        return true;
    }
}
