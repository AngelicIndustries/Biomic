package industries.angelic.biomic.compat.biome;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenCactus;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Native arid patches/cacti with whole-proposal support validation. Caller owns physical commit. */
public final class BopAridFeatures {

    public static final String GRASS = "grassSplatterPerChunk", XERIC = "xericSplatterPerChunk",
        RED_SAND = "redSandSplatterPerChunk";

    private record Pos(int x, int y, int z) {}

    private record Cell(Block block, int meta) {}

    private BopAridFeatures() {}

    /** Replace only the lowest existing sand-skin cell, never build a pillar into air. */
    public static String stableSkin(String biome, String state, int y, int solidBottom) {
        boolean solidSandBase = BopNativeBiomes.XERIC_SHRUBLAND.equals(biome) || BopNativeBiomes.MANGROVE.equals(biome)
            || BopNativeBiomes.OASIS.equals(biome);
        return solidSandBase && "minecraft:sand".equals(state) && y == solidBottom ? "minecraft:sandstone" : state;
    }

    public static boolean splatter(String name) {
        return Set.of(GRASS, XERIC, RED_SAND)
            .contains(name);
    }

    public static String identity(String name, WorldGenerator generator) {
        String expected = switch (name) {
            case GRASS -> "WorldGenGrassSplatter";
            case XERIC -> "WorldGenXericSplatter";
            case RED_SAND -> "WorldGenSplatter";
            default -> throw new IllegalArgumentException("Unknown arid patch");
        };
        if (!generator.getClass()
            .getName()
            .equals("biomesoplenty.common.world.features." + expected))
            throw new IllegalStateException("Native arid patch class changed");
        var fields = new TreeMap<String, Object>();
        try {
            for (var f : generator.getClass()
                .getDeclaredFields()) if (!Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    fields.put(f.getName(), f.get(generator));
                }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot inspect native arid patch", e);
        }
        if (RED_SAND.equals(name)) {
            if (!fields.keySet()
                .equals(Set.of("splatterBlock", "splatterBlockMeta", "blocksToSplatter"))
                || fields.get("splatterBlock") != Blocks.sand
                || !Integer.valueOf(1)
                    .equals(fields.get("splatterBlockMeta"))
                || !List.of(
                    NativeBlockCatalogue.resolve(BopNativeBiomes.HARD_SAND)
                        .block())
                    .equals(fields.get("blocksToSplatter")))
                throw new IllegalStateException("Native red-sand patch parameters changed");
        } else if (!fields.isEmpty()) throw new IllegalStateException("Unexpected native arid patch state");
        return generator.getClass()
            .getName() + (RED_SAND.equals(name) ? "{sand:1,substrate=hardSand}" : "{source128-trials}");
    }

    public static String encode(String name, WorldGenerator generator, Block block, int meta, Block below,
        int belowMeta) {
        identity(name, generator);
        if (RED_SAND.equals(name)) {
            if (block != Blocks.sand || meta != 1) throw new IllegalStateException("Unexpected red-sand patch write");
            return "minecraft:red_sand";
        }
        return BopNativeBiomes.encodeWrite(block, meta, below, belowMeta);
    }

    public static boolean placeSplatter(World world, NativeFeatureCapture.Access physical, Random random, String name,
        WorldGenerator generator, int x, int y, int z) {
        identity(name, generator);
        return place(world, physical, random, generator, x, y, z, name);
    }

    public static boolean placeCactus(World world, NativeFeatureCapture.Access physical, Random random, int x, int y,
        int z) {
        return place(world, physical, random, new WorldGenCactus(), x, y, z, null);
    }

    private static boolean plant(String name, Cell cell) {
        String nativeState = NativeBlockCatalogue.encode(cell.block, cell.meta);
        if (GRASS.equals(name)) return Set
            .of(
                "biomesoplenty:foliage[legacy_meta=2]",
                "biomesoplenty:foliage[legacy_meta=10]",
                "biomesoplenty:foliage[legacy_meta=11]")
            .contains(nativeState == null ? "" : nativeState);
        return cell.block == Blocks.red_flower && (cell.meta == 0 || cell.meta == 3)
            || cell.block == Blocks.yellow_flower && cell.meta == 0
            || Set
                .of(
                    "biomesoplenty:flowers[legacy_meta=7]",
                    "biomesoplenty:foliage[legacy_meta=9]",
                    "biomesoplenty:foliage[legacy_meta=4]")
                .contains(nativeState == null ? "" : nativeState);
    }

    private static boolean substrate(Block block, boolean red) {
        if (block == NativeBlockCatalogue.resolve(BopNativeBiomes.HARD_SAND)
            .block()) return true;
        return !red && (block == Blocks.sand || block == Blocks.stone
            || block == NativeBlockCatalogue.resolve("biomesoplenty:mud[legacy_meta=0]")
                .block()
            || block == NativeBlockCatalogue.resolve("biomesoplenty:harddirt[legacy_meta=0]")
                .block());
    }

    private static boolean place(World world, NativeFeatureCapture.Access physical, Random random,
        WorldGenerator generator, int x, int y, int z, String name) {
        if (!NativeFeatureCapture.containsFeatureY(physical, y)) return false;
        Map<Pos, Cell> proposal = new LinkedHashMap<>();
        var view = new NativeFeatureCapture.Access() {

            void check(int px, int py, int pz) {
                if (Math.abs((long) px - x) > 8 || Math.abs((long) pz - z) > 8
                    || py < physical.minimumY() - 8L
                    || py > Math.max((long) physical.maximumYExclusive() - 1, y + 6L))
                    throw NativeFeatureCapture.outside("Native arid feature escaped audited envelope");
            }

            public Block block(int px, int py, int pz) {
                check(px, py, pz);
                Cell c = proposal.get(new Pos(px, py, pz));
                return c == null ? physical.block(px, py, pz) : c.block;
            }

            public int metadata(int px, int py, int pz) {
                check(px, py, pz);
                Cell c = proposal.get(new Pos(px, py, pz));
                return c == null ? physical.metadata(px, py, pz) : c.meta;
            }

            public int heightValue(int px, int pz) {
                throw new IllegalStateException("Unexpected native arid height query");
            }

            public boolean set(int px, int py, int pz, Block block, int meta) {
                check(px, py, pz);
                if (proposal.size() >= 512) throw new IllegalStateException("Excessive arid feature writes");
                proposal.put(new Pos(px, py, pz), new Cell(block, meta));
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
        boolean cactus = name == null, red = RED_SAND.equals(name);
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            Cell c = entry.getValue();
            if (!NativeFeatureCapture.containsFeatureY(physical, p.y)) return false;
            if (cactus) {
                if (c.block != Blocks.cactus || c.meta != 0 || physical.block(p.x, p.y, p.z) != Blocks.air)
                    return false;
                if (!NativeFeatureCapture.run(
                    world,
                    new BopSurfaceCapture(view, random),
                    () -> Blocks.cactus.canBlockStay(world, p.x, p.y, p.z))) return false;
            } else if (c.block == (red ? Blocks.sand : Blocks.grass) && c.meta == (red ? 1 : 0)) {
                if (!substrate(physical.block(p.x, p.y, p.z), red) || physical.block(p.x, p.y + 1, p.z) != Blocks.air)
                    return false;
                Material below = physical.block(p.x, p.y - 1, p.z)
                    .getMaterial();
                if (!below.isSolid() || below == Material.wood || below == Material.leaves) return false;
            } else {
                if (red || !plant(name, c) || physical.block(p.x, p.y, p.z) != Blocks.air) return false;
                Cell ground = proposal.get(new Pos(p.x, p.y - 1, p.z));
                if (ground == null || ground.block != Blocks.grass || ground.meta != 0) return false;
            }
        }
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            Cell c = entry.getValue();
            if (!physical.set(p.x, p.y, p.z, c.block, c.meta)) return false;
        }
        return true;
    }
}
