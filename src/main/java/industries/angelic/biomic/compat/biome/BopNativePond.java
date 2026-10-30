package industries.angelic.biomic.compat.biome;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.feature.WorldGenLakes;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Installed native lake geometry, isolated and fitted as one closed surface feature. */
public final class BopNativePond {

    /** Hosted-only provenance proof for source-native filler repair; ordinary captures never qualify. */
    public interface HostedAccess extends NativeFeatureCapture.Access {

        boolean immutableCarrier(int x, int y, int z, Block block, int metadata);

        default String immutableCarrierRejection(int x, int y, int z, Block block, int metadata) {
            return immutableCarrier(x, y, z, block, metadata) ? null : "unproven-host-carrier";
        }
    }

    public enum Profile {
        GRASSLAND_WATER,
        OMINOUS_POISON,
        MYSTIC_POISON,
        LUSH_DESERT_WATER,
        MARSH_WATER,
        MOOR_WATER,
        BOG_WATER,
        BOG_POISON,
        LUSH_SWAMP_POISON,
        BAYOU_WATER,
        SLUDGEPIT_POISON,
        LAND_OF_LAKES_MARSH_WATER,
        WASTELAND_WATER,
        WASTELAND_POISON,
        MANGROVE_WATER,
        OASIS_WATER,
        VOLCANO_LAVA
    }

    private static boolean water(Profile profile) {
        return profile == Profile.LAND_OF_LAKES_MARSH_WATER || profile == Profile.GRASSLAND_WATER
            || profile == Profile.LUSH_DESERT_WATER
            || profile == Profile.MARSH_WATER
            || profile == Profile.MOOR_WATER
            || profile == Profile.BOG_WATER
            || profile == Profile.BAYOU_WATER
            || profile == Profile.WASTELAND_WATER
            || profile == Profile.MANGROVE_WATER
            || profile == Profile.OASIS_WATER;
    }

    private static boolean lava(Profile profile) {
        return profile == Profile.VOLCANO_LAVA;
    }

    public static final String POISON = "biomesoplenty:poison[legacy_meta=0]";

    record Pos(int x, int y, int z) {}

    private record Cell(Block block, int meta) {}

    private static final int[][] SEALED = { { -1, 0, 0 }, { 1, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 0, -1, 0 } };
    private static final int[][] NEIGHBORS = { { -1, 0, 0 }, { 1, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 0, -1, 0 },
        { 0, 1, 0 } };

    private BopNativePond() {}

    public static WorldGenerator unwrap(Object wrapper) throws ReflectiveOperationException {
        return unwrap(wrapper, Profile.GRASSLAND_WATER);
    }

    public static WorldGenerator unwrap(Object wrapper, Profile profile) throws ReflectiveOperationException {
        if (!wrapper.getClass()
            .getName()
            .equals("biomesoplenty.common.world.forcedgenerators.LakesForcedGenerator"))
            throw new IllegalStateException("Unaudited native lake wrapper");
        var field = wrapper.getClass()
            .getSuperclass()
            .getDeclaredField("worldGenerator");
        field.setAccessible(true);
        var generator = (WorldGenerator) field.get(wrapper);
        identity(generator, profile);
        return generator;
    }

    public static String identity(WorldGenerator generator) {
        return identity(generator, Profile.GRASSLAND_WATER);
    }

    public static Block fluid(Profile profile) {
        Objects.requireNonNull(profile, "Missing native pond profile");
        if (water(profile)) return Blocks.water;
        if (lava(profile)) return Blocks.lava;
        Block block = NativeBlockCatalogue.resolve(POISON)
            .block();
        if (!block.getClass()
            .getName()
            .equals("biomesoplenty.common.fluids.blocks.BlockPoisonFluid") || block.getMaterial() != Material.water)
            throw new IllegalStateException("Native poison fluid binding changed");
        return block;
    }

    public static String identity(WorldGenerator generator, Profile profile) {
        if (generator.getClass() != WorldGenLakes.class) throw new IllegalStateException("Unaudited lake generator");
        Block fluid = fluid(profile);
        int fields = 0;
        try {
            for (var field : WorldGenLakes.class.getDeclaredFields()) if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                fields++;
                if (field.getType() != Block.class || field.get(generator) != fluid)
                    throw new IllegalStateException("Lake fluid does not match explicit native profile");
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot inspect native lake", e);
        }
        if (fields != 1) throw new IllegalStateException("Unexpected native lake layout");
        return water(profile) ? "net.minecraft.world.gen.feature.WorldGenLakes{minecraft:water}"
            : lava(profile) ? "net.minecraft.world.gen.feature.WorldGenLakes{minecraft:lava}"
                : "net.minecraft.world.gen.feature.WorldGenLakes{BiomesOPlenty:poison}";
    }

    public static String encode(Block block, int meta) {
        return encode(block, meta, Profile.GRASSLAND_WATER);
    }

    public static String encode(Block block, int meta, Profile profile) {
        if (meta != 0) throw new IllegalStateException("Unexpected pond metadata");
        if (block == fluid(profile))
            return water(profile) ? "minecraft:water[level=0]" : lava(profile) ? "minecraft:lava[level=0]" : POISON;
        if (block == Blocks.air) return "minecraft:air";
        if (block == Blocks.grass) return "minecraft:grass_block[snowy=false]";
        if (profile == Profile.MANGROVE_WATER && block == Blocks.sand) return "minecraft:sand";
        if ((profile == Profile.WASTELAND_WATER || profile == Profile.WASTELAND_POISON)
            && BopNativeBiomes.DRIED_DIRT.equals(NativeBlockCatalogue.encode(block, meta)))
            return BopNativeBiomes.DRIED_DIRT;
        if (profile == Profile.VOLCANO_LAVA
            && BopNativeBiomes.ASH_STONE.equals(NativeBlockCatalogue.encode(block, meta)))
            return BopNativeBiomes.ASH_STONE;
        if (lava(profile) && block == Blocks.stone) return "minecraft:stone";
        throw new IllegalStateException("Unaudited pond write");
    }

    private static boolean earth(Block block, int meta, Profile profile) {
        if (meta == 0 && (block == Blocks.grass || block == Blocks.dirt
            || block == Blocks.stone
            || block == Blocks.clay
            || block == Blocks.sand
            || block == Blocks.gravel
            || block == Blocks.hardened_clay)) return true;
        String name = Block.blockRegistry.getNameForObject(block);
        if ((profile == Profile.LAND_OF_LAKES_MARSH_WATER || profile == Profile.SLUDGEPIT_POISON
            || profile == Profile.BAYOU_WATER
            || profile == Profile.MARSH_WATER
            || profile == Profile.MOOR_WATER
            || profile == Profile.BOG_WATER
            || profile == Profile.BOG_POISON) && meta == 0 && "BiomesOPlenty:mud".equals(name)) return true;
        if ((profile == Profile.SLUDGEPIT_POISON || profile == Profile.OMINOUS_POISON || profile == Profile.MOOR_WATER)
            && meta == 0
            && ("BiomesOPlenty:newBopGrass".equals(name) || "BiomesOPlenty:newBopDirt".equals(name))) return true;
        if ((profile == Profile.WASTELAND_WATER || profile == Profile.WASTELAND_POISON) && meta == 0
            && "BiomesOPlenty:driedDirt".equals(name)) return true;
        if (profile == Profile.VOLCANO_LAVA && meta == 0 && "BiomesOPlenty:ashStone".equals(name)) return true;
        return "etfuturum:stone".equals(name) && meta >= 0 && meta <= 6;
    }

    private static boolean vegetation(Block block) {
        Material material = block.getMaterial();
        return material == Material.plants || material == Material.vine
            || material == Material.wood
            || material == Material.leaves
            || block == Blocks.red_mushroom_block
            || block == Blocks.brown_mushroom_block;
    }

    private static boolean rejected(Profile profile, int x, int height, int z, String reason) {
        Integer tx = Integer.getInteger("biomic.traceHostedX"), tz = Integer.getInteger("biomic.traceHostedZ");
        if (Boolean.getBoolean("biomic.traceBopAuditPonds") && tx != null
            && tz != null
            && Math.abs((long) x - tx) <= 64
            && Math.abs((long) z - tz) <= 64)
            industries.angelic.biomic.Biomic.LOG
                .info("Native pond rejection {} {},{},{} {}", profile, x, height, z, reason);
        return false;
    }

    public static int sourceY(Random random, Profile profile) {
        return switch (profile) {
            case GRASSLAND_WATER, LUSH_DESERT_WATER, MARSH_WATER, MOOR_WATER, BOG_WATER, BAYOU_WATER, LAND_OF_LAKES_MARSH_WATER, WASTELAND_WATER, MANGROVE_WATER, OASIS_WATER -> random
                .nextInt(random.nextInt(240) + 8);
            case OMINOUS_POISON, MYSTIC_POISON, BOG_POISON, LUSH_SWAMP_POISON, SLUDGEPIT_POISON, WASTELAND_POISON, VOLCANO_LAVA -> random
                .nextInt(random.nextInt(random.nextInt(112) + 8) + 8);
        };
    }

    static int hostedSourceY(int sourceY, Profile profile) {
        // The triple-nested native draw has a large zero bucket. In vanilla terrain that
        // attempt is irrecoverably below the lake generator's four-block descent, while a
        // hosted biome has no underground BOP geology to decorate. Reuse only that already
        // wasted bucket as the nominal source sea-level candidate for the two high-budget
        // poison profiles whose pools are a defining surface feature. All other samples,
        // profiles and the direct source-oracle entry retain their exact source coordinate.
        return sourceY == 0 && (profile == Profile.OMINOUS_POISON || profile == Profile.WASTELAND_POISON) ? 64
            : sourceY;
    }

    static boolean wastelandFillerFallback(int sourceY, Profile profile) {
        return sourceY == 0 && profile == Profile.WASTELAND_POISON;
    }

    public static boolean place(World world, NativeFeatureCapture.Access physical, Random random,
        WorldGenerator generator, BiomeGenBase biome, int x, int surfaceHeight, int z, Profile profile) {
        // BOP's nested-Y draw is retained in source coordinates. Its surface64 is translated
        // to this actual floor, including the generator's descent and bank-light queries.
        int sourceY = sourceY(random, profile);
        return placeAtSourceY(
            world,
            physical,
            random,
            generator,
            biome,
            x,
            surfaceHeight,
            z,
            hostedSourceY(sourceY, profile),
            profile,
            wastelandFillerFallback(sourceY, profile));
    }

    private static boolean placeAtSourceY(World world, NativeFeatureCapture.Access physical, Random random,
        WorldGenerator generator, BiomeGenBase biome, int x, int surfaceHeight, int z, int sourceY, Profile profile,
        boolean surfaceFallback) {
        Objects.requireNonNull(profile, "Missing native pond profile");
        if (sourceY < 0 || sourceY > (water(profile) ? 247 : 125))
            throw new IllegalArgumentException("Invalid native pond source Y: " + sourceY + " for " + profile);
        // Layer sites describe terrain before decoration. Earlier buffered features can remove
        // its last physical surface, so WORLD_SURFACE legitimately returns 0. That is a failed
        // placement, not an unsupported native generator. Reject before capture or any writes.
        int physicalMinimum = physical == null ? 0 : physical.minimumY();
        int physicalMaximum = physical == null ? 256 : physical.maximumYExclusive();
        if (surfaceHeight <= physicalMinimum || surfaceHeight > physicalMaximum)
            return rejected(profile, x, surfaceHeight, z, "missing-or-out-of-range-host-surface");
        identity(generator, profile);
        Block fluid = fluid(profile);
        boolean climate = switch (profile) {
            case LAND_OF_LAKES_MARSH_WATER -> biome.getClass()
                .getName()
                .equals("biomesoplenty.common.biome.overworld.sub.BiomeGenLandOfLakesMarsh")
                && biome.topBlock == Blocks.grass
                && biome.fillerBlock == Blocks.dirt
                && biome.temperature == .5F
                && biome.rainfall == .9F;
            case SLUDGEPIT_POISON -> biome.getClass()
                .getName()
                .equals("biomesoplenty.common.biome.overworld.BiomeGenSludgepit") && biome.temperature == .8F
                && biome.rainfall == .9F;
            case BAYOU_WATER -> biome.getClass()
                .getName()
                .equals("biomesoplenty.common.biome.overworld.BiomeGenBayou") && biome.topBlock == Blocks.grass
                && biome.fillerBlock == Blocks.dirt
                && biome.temperature == .8F
                && biome.rainfall == .9F;
            case LUSH_SWAMP_POISON -> biome.getClass()
                .getName()
                .equals("biomesoplenty.common.biome.overworld.BiomeGenLushSwamp") && biome.topBlock == Blocks.grass
                && biome.fillerBlock == Blocks.dirt
                && biome.temperature == .7F
                && biome.rainfall == 1F;
            case BOG_WATER, BOG_POISON -> biome.getClass()
                .getName()
                .equals("biomesoplenty.common.biome.overworld.BiomeGenBog") && biome.topBlock == Blocks.grass
                && biome.fillerBlock == Blocks.dirt
                && biome.temperature == .5F
                && biome.rainfall == .9F;
            case GRASSLAND_WATER -> biome.topBlock == Blocks.grass && biome.temperature == .6F;
            case LUSH_DESERT_WATER -> biome.getClass()
                .getName()
                .equals("biomesoplenty.common.biome.overworld.BiomeGenLushDesert") && biome.temperature == 1.1F
                && biome.rainfall == .5F;
            case MARSH_WATER -> biome.getClass()
                .getName()
                .equals("biomesoplenty.common.biome.overworld.BiomeGenMarsh") && biome.topBlock == Blocks.grass
                && biome.fillerBlock == Blocks.dirt
                && biome.temperature == .5F
                && biome.rainfall == .9F;
            case MOOR_WATER -> biome.getClass()
                .getName()
                .equals("biomesoplenty.common.biome.overworld.BiomeGenMoor") && biome.temperature == .5F
                && biome.rainfall == 1F;
            case OMINOUS_POISON -> biome.getClass()
                .getName()
                .equals("biomesoplenty.common.biome.overworld.BiomeGenOminousWoods") && biome.temperature == .5F;
            case MYSTIC_POISON -> biome.getClass()
                .getName()
                .equals("biomesoplenty.common.biome.overworld.BiomeGenMysticGrove") && biome.topBlock == Blocks.grass
                && biome.fillerBlock == Blocks.dirt
                && biome.temperature == .7F
                && biome.rainfall == 1F;
            case WASTELAND_WATER, WASTELAND_POISON -> biome.getClass()
                .getName()
                .equals("biomesoplenty.common.biome.overworld.BiomeGenWasteland") && biome.temperature == 1F
                && biome.rainfall == .05F;
            case MANGROVE_WATER -> biome.getClass()
                .getName()
                .equals("biomesoplenty.common.biome.overworld.sub.BiomeGenMangrove") && biome.topBlock == Blocks.sand
                && biome.fillerBlock == Blocks.sand
                && biome.temperature == .8F
                && biome.rainfall == .9F;
            case OASIS_WATER -> biome.getClass()
                .getName()
                .equals("biomesoplenty.common.biome.overworld.sub.BiomeGenOasis") && biome.topBlock == Blocks.grass
                && biome.fillerBlock == Blocks.dirt
                && biome.temperature == 2F
                && biome.rainfall == .3F;
            case VOLCANO_LAVA -> biome.getClass()
                .getName()
                .equals("biomesoplenty.common.biome.overworld.sub.BiomeGenVolcano") && biome.temperature == 2F
                && biome.rainfall == .05F;
        };
        if (!climate) throw new IllegalArgumentException(
            "Unsupported native pond climate: profile=" + profile
                + " biome="
                + biome.getClass()
                    .getName()
                + " temperature="
                + biome.temperature
                + " rainfall="
                + biome.rainfall
                + " position="
                + x
                + ","
                + surfaceHeight
                + ","
                + z
                + " sourceY="
                + sourceY);
        int offset = surfaceHeight - 64;
        Map<Pos, Cell> proposal = new LinkedHashMap<>();
        var source = new NativeFeatureCapture.Access() {

            private void check(int px, int pz) {
                if (px < x - 8L || px > x + 7L || pz < z - 8L || pz > z + 7L)
                    throw NativeFeatureCapture.outside("Native pond escaped16x16 envelope");
            }

            public Block block(int px, int py, int pz) {
                check(px, pz);
                int y = py + offset;
                if (!NativeFeatureCapture.containsPhysicalY(physical, y)) return Blocks.air;
                Cell c = proposal.get(new Pos(px, y, pz));
                return c == null ? physical.block(px, y, pz) : c.block;
            }

            public int metadata(int px, int py, int pz) {
                check(px, pz);
                int y = py + offset;
                if (!NativeFeatureCapture.containsPhysicalY(physical, y)) return 0;
                Cell c = proposal.get(new Pos(px, y, pz));
                return c == null ? physical.metadata(px, y, pz) : c.meta;
            }

            public int heightValue(int px, int pz) {
                check(px, pz);
                return physical.heightValue(px, pz) - offset;
            }

            public boolean set(int px, int py, int pz, Block block, int meta) {
                check(px, pz);
                encode(block, meta, profile);
                if (proposal.size() >= 2048) throw new IllegalStateException("Native pond write budget exceeded");
                proposal.put(new Pos(px, py + offset, pz), new Cell(block, meta));
                return true;
            }

            public int minimumY() {
                return Math.subtractExact(physical.minimumY(), offset);
            }

            public int maximumYExclusive() {
                return Math.subtractExact(physical.maximumYExclusive(), offset);
            }
        };
        // Skylight scans the complete real physical column, not a translated/clipped source ceiling.
        var actual = new NativeFeatureCapture.Access() {

            public Block block(int px, int py, int pz) {
                return source.block(px, py - offset, pz);
            }

            public int metadata(int px, int py, int pz) {
                return source.metadata(px, py - offset, pz);
            }

            public int heightValue(int px, int pz) {
                return source.heightValue(px, pz) + offset;
            }

            public boolean set(int px, int py, int pz, Block block, int meta) {
                return source.set(px, py - offset, pz, block, meta);
            }

            public int minimumY() {
                return physical.minimumY();
            }

            public int maximumYExclusive() {
                return physical.maximumYExclusive();
            }
        };
        var light = new BopSurfaceCapture(actual, random);
        var view = new NativeFeatureCapture.BiomeSurfaceAccess() {

            public Block block(int px, int py, int pz) {
                return light.block(px, py + offset, pz);
            }

            public int metadata(int px, int py, int pz) {
                return light.metadata(px, py + offset, pz);
            }

            public int heightValue(int px, int pz) {
                return light.heightValue(px, pz) - offset;
            }

            public boolean set(int px, int py, int pz, Block block, int meta) {
                return light.set(px, py + offset, pz, block, meta);
            }

            public int savedLight(EnumSkyBlock kind, int px, int py, int pz) {
                return light.savedLight(kind, px, py + offset, pz);
            }

            public int fullLight(int px, int py, int pz) {
                return light.fullLight(px, py + offset, pz);
            }

            public boolean seesSky(int px, int py, int pz) {
                return light.seesSky(px, py + offset, pz);
            }

            public TileEntity tileEntity(int px, int py, int pz) {
                return light.tileEntity(px, py + offset, pz);
            }

            public Random random() {
                return random;
            }

            public BiomeGenBase biome(int px, int pz) {
                source.metadata(px, 64, pz);
                return biome;
            }

            public boolean freezable(int px, int py, int pz) {
                source.metadata(px, py, pz);
                // WorldGenLakes queries every water-material lake, including poison. The
                // native World predicate only freezes vanilla water, never BlockPoisonFluid.
                if (!water(profile)) return false;
                // The admitted Grassland climate remains above the native freezing threshold
                // throughout0..255. No fabricated cold-biome ice rule is shared with other profiles.
                if (biome.getFloatTemperature(px, py + offset, pz) < 0.15F)
                    throw new IllegalStateException("Unexpected cold pond climate");
                return false;
            }
        };
        if (!NativeFeatureCapture.run(world, view, () -> generator.generate(world, random, x, sourceY, z))
            || proposal.isEmpty()) return rejected(profile, x, surfaceHeight, z, "native-shape sourceY=" + sourceY);
        Set<Pos> fillerSeal = surfaceFallback && profile == Profile.WASTELAND_POISON
            ? wastelandFillerSeal(proposal, physical, biome, fluid)
            : Set.of();
        if (fillerSeal == null)
            return rejected(profile, x, surfaceHeight, z, "non-native-filler-support sourceY=" + sourceY);
        {
            boolean water = false;
            for (var entry : proposal.entrySet()) {
                Pos p = entry.getKey();
                Cell c = entry.getValue();
                if (!NativeFeatureCapture.containsFeatureY(physical, p.y) || p.y < surfaceHeight - 8
                    || p.y > surfaceHeight + 4)
                    return rejected(profile, x, surfaceHeight, z, "host-horizon sourceY=" + sourceY);
                Block existing = physical.block(p.x, p.y, p.z);
                int meta = physical.metadata(p.x, p.y, p.z);
                if (existing != Blocks.air && existing != fluid
                    && !earth(existing, meta, profile)
                    && !fillerSeal.contains(p))
                    return rejected(
                        profile,
                        x,
                        surfaceHeight,
                        z,
                        "foreign-block " + Block.blockRegistry.getNameForObject(existing) + ":" + meta);
                if (c.block == Blocks.grass && existing != Blocks.dirt) return false;
                for (int[] d : NEIGHBORS) {
                    Pos n = new Pos(p.x + d[0], p.y + d[1], p.z + d[2]);
                    if (!proposal.containsKey(n) && vegetation(physical.block(n.x, n.y, n.z)))
                        return rejected(profile, x, surfaceHeight, z, "existing-vegetation");
                }
                if (c.block == Blocks.air || c.block == fluid) {
                    Pos above = new Pos(p.x, p.y + 1, p.z);
                    if (!proposal.containsKey(above)
                        && physical.block(above.x, above.y, above.z) instanceof BlockFalling) return false;
                }
                if (c.block != fluid) continue;
                water = true;
                for (int[] d : SEALED) {
                    Pos n = new Pos(p.x + d[0], p.y + d[1], p.z + d[2]);
                    Cell value = proposal.get(n);
                    Block block = value == null ? physical.block(n.x, n.y, n.z) : value.block;
                    boolean stableSand = profile == Profile.MANGROVE_WATER && block == Blocks.sand
                        && stableSandColumn(proposal, physical, n.x, n.y, n.z, fluid);
                    if (block != fluid && (!block.getMaterial()
                        .blocksMovement() || block instanceof BlockFalling && !stableSand))
                        return rejected(profile, x, surfaceHeight, z, "unsealed-fluid");
                }
            }
            if (!water) return false;
        }
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            Cell c = entry.getValue();
            if (!physical.set(p.x, p.y, p.z, c.block, c.meta)) return false;
        }
        return true;
    }

    /** Restore only source-native filler around unsupported falling carrier bordering poison. */
    private static Set<Pos> wastelandFillerSeal(Map<Pos, Cell> proposal, NativeFeatureCapture.Access physical,
        BiomeGenBase biome, Block fluid) {
        if (!(physical instanceof HostedAccess hosted)) return null;
        Block filler = biome.fillerBlock;
        if (!BopNativeBiomes.DRIED_DIRT.equals(NativeBlockCatalogue.encode(filler, 0)))
            throw new IllegalStateException("Native Wasteland filler changed");
        Set<Pos> fluidCells = new LinkedHashSet<>();
        for (var entry : proposal.entrySet()) if (entry.getValue().block == fluid) fluidCells.add(entry.getKey());
        Set<Pos> seal = fillerSealPositions(fluidCells, proposal.keySet(), n -> {
            Block existing = physical.block(n.x, n.y, n.z);
            return !(existing == fluid || existing.getMaterial()
                .blocksMovement() && !(existing instanceof BlockFalling));
        }, n -> {
            Block existing = physical.block(n.x, n.y, n.z);
            int meta = physical.metadata(n.x, n.y, n.z);
            boolean falling = fallingCarrier(existing, meta);
            String provenance = falling ? hosted.immutableCarrierRejection(n.x, n.y, n.z, existing, meta)
                : "unsupported-carrier";
            boolean immutable = falling && provenance == null;
            if (!immutable) traceFillerSealRejection(n, existing, meta, falling, provenance);
            return eligibleFillerCarrier(existing, meta, immutable);
        });
        if (seal == null) return null;
        for (Pos p : seal) proposal.put(p, new Cell(filler, 0));
        return seal;
    }

    /** Exact side/bottom closure of the native lower-fluid mask; the upper-air envelope is not sealed. */
    static Set<Pos> fillerSealPositions(Set<Pos> fluidCells, Set<Pos> proposal,
        java.util.function.Predicate<Pos> needsSeal, java.util.function.Predicate<Pos> eligible) {
        Set<Pos> seal = new LinkedHashSet<>();
        for (Pos p : fluidCells) for (int[] d : SEALED) {
            Pos n = new Pos(p.x + d[0], p.y + d[1], p.z + d[2]);
            if (proposal.contains(n) || seal.contains(n) || !needsSeal.test(n)) continue;
            if (!eligible.test(n)) return null;
            seal.add(n);
        }
        return seal;
    }

    static boolean eligibleFillerCarrier(Block block, int metadata, boolean immutable) {
        return block != null
            && eligibleFillerCarrier(block == Blocks.sand, block == Blocks.gravel, metadata, immutable);
    }

    static boolean eligibleFillerCarrier(boolean sand, boolean gravel, int metadata, boolean immutable) {
        return immutable && (gravel && metadata == 0 || sand && (metadata == 0 || metadata == 1));
    }

    static boolean fallingCarrier(Block block, int metadata) {
        return eligibleFillerCarrier(block, metadata, true);
    }

    private static void traceFillerSealRejection(Pos p, Block block, int metadata, boolean falling, String reason) {
        Integer tx = Integer.getInteger("biomic.traceHostedX"), tz = Integer.getInteger("biomic.traceHostedZ");
        if (Boolean.getBoolean("biomic.traceBopAuditPonds") && tx != null
            && tz != null
            && Math.abs((long) p.x - tx) <= 64
            && Math.abs((long) p.z - tz) <= 64)
            industries.angelic.biomic.Biomic.LOG.info(
                "Native Wasteland filler seal rejected at {},{},{} state={}:{} falling={} provenance={}",
                p.x,
                p.y,
                p.z,
                Block.blockRegistry.getNameForObject(block),
                metadata,
                falling,
                reason);
    }

    private static boolean stableSandColumn(Map<Pos, Cell> proposal, NativeFeatureCapture.Access physical, int x, int y,
        int z, Block fluid) {
        for (int depth = 1; depth <= 16; depth++) {
            Pos p = new Pos(x, y - depth, z);
            Cell cell = proposal.get(p);
            Block below = cell == null ? physical.block(p.x, p.y, p.z) : cell.block;
            if (below == Blocks.sand) continue;
            return below != fluid && below.getMaterial()
                .blocksMovement() && !(below instanceof BlockFalling);
        }
        return false;
    }
}
