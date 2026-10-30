package industries.angelic.biomic.compat.biome;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Native replacement-only surface patches. Never fills air, cuts plants, or builds a footing. */
public final class BopSurfaceSplotch {

    private record Pos(int x, int y, int z) {}

    private record Parameters(Block block, int meta, int size, List<?> substrates) {}

    private BopSurfaceSplotch() {}

    private static Parameters parameters(String feature, WorldGenerator generator) {
        if (!generator.getClass()
            .getName()
            .equals("biomesoplenty.common.world.features.WorldGenSplotches"))
            throw new IllegalArgumentException("Unaudited native splotch generator");
        var values = new HashMap<String, Object>();
        try {
            for (var field : generator.getClass()
                .getDeclaredFields()) if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    values.put(field.getName(), field.get(generator));
                }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot inspect native splotch", error);
        }
        if (!values.keySet()
            .equals(Set.of("splotchBlock", "splotchBlockMeta", "numberOfBlocks", "blockList")))
            throw new IllegalStateException("Unexpected native splotch layout");
        var p = new Parameters(
            (Block) values.get("splotchBlock"),
            (Integer) values.get("splotchBlockMeta"),
            (Integer) values.get("numberOfBlocks"),
            (List<?>) values.get("blockList"));
        boolean valid = switch (feature) {
            case "generateQuicksand" -> p.size == 24 && p.meta == 1
                && "biomesoplenty:mud[legacy_meta=1]".equals(NativeBlockCatalogue.encode(p.block, p.meta))
                && p.substrates.equals(List.of(Blocks.grass, Blocks.dirt, Blocks.sand));
            case "generateStoneInGrass" -> p.size == 32 && p.meta == 0
                && p.block == Blocks.stone
                && p.substrates.equals(List.of(Blocks.grass));
            case "generateMycelium" -> p.size == 32 && p.meta == 0
                && p.block == Blocks.mycelium
                && p.substrates.equals(List.of(Blocks.grass));
            case "generateSand" -> p.size == 32 && p.meta == 0
                && p.block == Blocks.sand
                && p.substrates.equals(
                    List.of(
                        NativeBlockCatalogue.resolve("biomesoplenty:rocks[legacy_meta=0]")
                            .block()));
            case "generateQuagmire" -> p.size == 48 && p.meta == 0
                && p.block == Blocks.grass
                && p.substrates.equals(
                    List.of(
                        NativeBlockCatalogue.resolve(BopNativeBiomes.MUD)
                            .block()));
            case "generateSponge" -> p.size == 24 && sponge(p.block, p.meta)
                && registries(p.substrates).equals(List.of("minecraft:dirt", "minecraft:sand", "minecraft:gravel"));
            case "generateAsh" -> p.size == 32 && p.meta == 0
                && "biomesoplenty:ash[legacy_meta=0]".equals(NativeBlockCatalogue.encode(p.block, p.meta))
                && registries(p.substrates).equals(List.of("BiomesOPlenty:ashStone", "minecraft:netherrack"));
            default -> false;
        };
        if (!valid) throw new IllegalStateException(
            "Unaudited native splotch parameters: " + feature
                + " block="
                + registry(p.block)
                + ':'
                + p.meta
                + " size="
                + p.size
                + " substrates="
                + registries(p.substrates));
        return p;
    }

    private static String registry(Block block) {
        return Block.blockRegistry.getNameForObject(block);
    }

    private static boolean sponge(Block block, int meta) {
        if (block == Blocks.sponge && meta == 0 && "minecraft:sponge".equals(registry(block))) return true;
        // EFR's load-complete BOP hook replaces only this generator's block and
        // metadata with its wet sponge. Keep the unpatched BOP form optional.
        return meta == 1 && "etfuturum:sponge".equals(registry(block))
            && block.getClass()
                .getName()
                .equals("ganymedes01.etfuturum.blocks.BlockSponge")
            && block.getMaterial() == Material.sponge
            && !block.hasTileEntity(meta);
    }

    private static List<String> registries(List<?> blocks) {
        var result = new ArrayList<String>();
        var unique = new HashSet<String>();
        for (Object block : blocks) if (!(block instanceof Block value) || !unique.add(registry(value)))
            throw new IllegalStateException("Invalid or duplicate native splotch substrate");
        else result.add(registry(value));
        return result;
    }

    public static String identity(String feature, WorldGenerator generator) {
        var p = parameters(feature, generator);
        return generator.getClass()
            .getName() + "{block="
            + Block.blockRegistry.getNameForObject(p.block)
            + ",meta="
            + p.meta
            + ",size="
            + p.size
            + ",substrates="
            + p.substrates.stream()
                .map(b -> Block.blockRegistry.getNameForObject(b))
                .toList()
            + "}";
    }

    public static String encode(String feature, WorldGenerator generator, Block block, int meta) {
        var p = parameters(feature, generator);
        if (block != p.block || meta != p.meta) throw new IllegalStateException("Unexpected native splotch write");
        if (feature.equals("generateSponge"))
            return block == Blocks.sponge ? "minecraft:sponge" : "minecraft:wet_sponge";
        return block == Blocks.grass ? "minecraft:grass_block[snowy=false]"
            : block == Blocks.sand ? "minecraft:sand"
                : block == Blocks.stone ? "minecraft:stone"
                    : block == Blocks.mycelium ? "minecraft:mycelium" : NativeBlockCatalogue.encode(block, meta);
    }

    /** Caller buffers set writes and discards them on false/exception, including a habitat-edge rejection. */
    public static boolean place(World world, NativeFeatureCapture.Access physical, Random random, String feature,
        WorldGenerator generator, int x, int y, int z) {
        var p = parameters(feature, generator);
        Set<Pos> proposal = new LinkedHashSet<>();
        var view = new NativeFeatureCapture.Access() {

            void check(int px, int py, int pz) {
                int radius = feature.equals("generateQuagmire") ? 10 : 8;
                if (Math.abs((long) px - (x + 8L)) > radius || Math.abs((long) pz - (z + 8L)) > radius
                    || Math.abs((long) py - y) > 6)
                    throw NativeFeatureCapture.outside("Native splotch escaped source envelope");
            }

            public Block block(int px, int py, int pz) {
                check(px, py, pz);
                return proposal.contains(new Pos(px, py, pz)) ? p.block : physical.block(px, py, pz);
            }

            public int metadata(int px, int py, int pz) {
                check(px, py, pz);
                return proposal.contains(new Pos(px, py, pz)) ? p.meta : physical.metadata(px, py, pz);
            }

            public int heightValue(int px, int pz) {
                throw new IllegalStateException("Unexpected splotch height query");
            }

            public boolean set(int px, int py, int pz, Block block, int meta) {
                check(px, py, pz);
                if (block != p.block || meta != p.meta || proposal.size() >= 4096)
                    throw new IllegalStateException("Invalid native splotch write");
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
        for (Pos pos : proposal) {
            if (!NativeFeatureCapture.containsFeatureY(physical, pos.y)
                || !p.substrates.contains(physical.block(pos.x, pos.y, pos.z))) return false;
            if (!proposal.contains(new Pos(pos.x, pos.y + 1, pos.z))) {
                Material above = physical.block(pos.x, pos.y + 1, pos.z)
                    .getMaterial();
                // A neighboring population pass may already have planted this soil. Do not
                // turn its support into quicksand/stone and leave a popping or floating plant.
                if (above == Material.plants || above == Material.vine
                    || above == Material.leaves
                    || above == Material.wood) return false;
            }
            if (proposal.contains(new Pos(pos.x, pos.y - 1, pos.z))) continue;
            Block below = physical.block(pos.x, pos.y - 1, pos.z);
            Material material = below.getMaterial();
            if (!material.isSolid() || material == Material.leaves || material == Material.wood) return false;
        }
        for (Pos pos : proposal) if (!physical.set(pos.x, pos.y, pos.z, p.block, p.meta)) return false;
        return true;
    }
}
