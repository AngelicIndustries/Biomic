package industries.angelic.biomic.world;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/**
 * Other mods dress their own biomes in the Tartarus Nether, as their own Nether providers would, at population:
 * <ul>
 * <li>BOP: {@code biome.decorate}. Its Nether features mostly already pick heights across 0..255; trees, grass,
 * flowers, lakes and hives stay below 128 as in BOP's own Nether.</li>
 * <li>Netherlicious: the path of its own 256-tall Nether ({@code populate}, {@code populateBig}, {@code decorate}
 * with BigNether on) for biomes that have one. Biomes without it decorate 0..127 only, so they decorate a second
 * time with the world seen 128 blocks higher.</li>
 * </ul>
 * Every pass runs in a {@link Band}: chunks that are not loaded read as bedrock and are never written, so dressing
 * never generates or populates a neighbouring chunk (Netherlicious' vents otherwise reach well past the 2x2).
 */
final class NetherNeighbourDressing {

    private static final int BAND = 128;
    private static final long BAND_SALT = 0x5B3A6D1F0C9E2471L;

    private NetherNeighbourDressing() {}

    static void dress(World world, NetherNeighbour neighbour, BiomeGenBase biome, int chunkX, int chunkZ) {
        int x = chunkX * 16, z = chunkZ * 16;
        Random random = new Random(
            world.getSeed() ^ chunkX * 341873128712L
                ^ chunkZ * 132897987541L
                ^ neighbour.ordinal() * 0x9E3779B97F4A7C15L);
        if (NativeFeatureCapture.activeOnThread(world)) throw new IllegalStateException("Nested Nether dressing");
        // The bands' own random, for features that would read World.rand or Math.random: the chunk's, not the world's
        // running one, and apart from the decorator's stream so that stream keeps its draws.
        long bands = WorldgenContext.mix64(
            world.getSeed() ^ chunkX * 0x9E3779B97F4A7C15L
                ^ chunkZ * 0xC2B2AE3D27D4EB4FL
                ^ neighbour.ordinal()
                ^ BAND_SALT);
        switch (neighbour.dressing) {
            case BOP -> band(world, 0, bands, () -> biome.decorate(world, random, x, z));
            case NETHERLICIOUS -> Netherlicious.dress(world, biome, random, bands, x, z);
            case GARDENS -> {} // Generated with the terrain.
            case RU -> {} // Generated with the terrain.
        }
    }

    private static final class Netherlicious {

        private static final Method POPULATE, POPULATE_BIG;
        private static final Field BIG_NETHER, DECORATOR;

        static {
            try {
                Class<?> biome = Class.forName("DelirusCrux.Netherlicious.Biomes.Utility.NetherliciousBiome");
                POPULATE = biome.getMethod("populate", World.class, Random.class, int.class, int.class);
                POPULATE_BIG = biome.getMethod("populateBig", World.class, Random.class, int.class, int.class);
                DECORATOR = biome.getDeclaredField("decorator");
                DECORATOR.setAccessible(true);
                BIG_NETHER = Class.forName("DelirusCrux.Netherlicious.Utility.Configuration.WorldgenConfiguration")
                    .getField("BigNether");
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unsupported Netherlicious version", error);
            }
        }

        static void dress(World world, BiomeGenBase biome, Random random, long bands, int x, int z) {
            if (tall(biome)) {
                boolean big = flag(null);
                flag(true);
                try {
                    band(world, 0, bands, () -> {
                        invoke(POPULATE, biome, world, random, x, z);
                        invoke(POPULATE_BIG, biome, world, random, x, z);
                        biome.decorate(world, random, x, z);
                    });
                } finally {
                    flag(big);
                }
                return;
            }
            for (int shift : new int[] { 0, BAND }) band(world, shift, bands ^ shift, () -> {
                invoke(POPULATE, biome, world, random, x, z);
                biome.decorate(world, random, x, z);
            });
        }

        private static void invoke(Method method, BiomeGenBase biome, World world, Random random, int x, int z) {
            try {
                method.invoke(biome, world, random, x, z);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Netherlicious decoration failed", error);
            }
        }

        /** Reads BigNether when given null, otherwise sets it and returns the new value. */
        private static boolean flag(Boolean value) {
            try {
                if (value != null) BIG_NETHER.setBoolean(null, value);
                return BIG_NETHER.getBoolean(null);
            } catch (IllegalAccessException error) {
                throw new IllegalStateException(error);
            }
        }

        /** Whether the biome's decorator has its own decoration for Netherlicious' 256-tall Nether. */
        private static boolean tall(BiomeGenBase biome) {
            try {
                DECORATOR.get(biome)
                    .getClass()
                    .getDeclaredMethod("populateBig");
                return true;
            } catch (NoSuchMethodException absent) {
                return false;
            } catch (IllegalAccessException error) {
                throw new IllegalStateException(error);
            }
        }
    }

    private static void band(World world, int shift, long seed, Runnable pass) {
        NativeFeatureCapture.run(world, new Band(world, shift, seed), () -> {
            pass.run();
            return null;
        });
    }

    /**
     * The world as a decorator sees it, raised by {@code shift}: a decorator for a 128-tall Nether shifted by 128
     * works on 128..255. Outside 0..255 it reads bedrock and writes nothing, so a band has a floor and a roof.
     * Chunks that are not loaded read as bedrock too and are never written.
     */
    private static final class Band
        implements NativeFeatureCapture.BiomeSurfaceAccess, NativeFeatureCapture.EntitySpawnAccess {

        private final World world;
        private final int shift;
        private final Random random;

        Band(World world, int shift, long seed) {
            this.world = world;
            this.shift = shift;
            this.random = new Random(seed);
        }

        private boolean inside(int x, int y, int z) {
            return y + shift >= 0 && y + shift < NetherTerrainSource.HEIGHT
                && world.getChunkProvider()
                    .chunkExists(x >> 4, z >> 4);
        }

        @Override
        public Block block(int x, int y, int z) {
            return inside(x, y, z) ? world.getBlock(x, y + shift, z) : Blocks.bedrock;
        }

        @Override
        public int metadata(int x, int y, int z) {
            return inside(x, y, z) ? world.getBlockMetadata(x, y + shift, z) : 0;
        }

        @Override
        public boolean set(int x, int y, int z, Block block, int metadata) {
            return inside(x, y, z) && world.setBlock(x, y + shift, z, block, metadata, 2);
        }

        @Override
        public int heightValue(int x, int z) {
            return inside(x, 0, z) ? clamp(world.getHeightValue(x, z) - shift) : BAND;
        }

        @Override
        public int topSolidOrLiquid(int x, int z) {
            return inside(x, 0, z) ? clamp(world.getTopSolidOrLiquidBlock(x, z) - shift) : BAND;
        }

        private static int clamp(int y) {
            return Math.max(0, Math.min(BAND, y));
        }

        @Override
        public TileEntity tileEntity(int x, int y, int z) {
            return inside(x, y, z) ? world.getTileEntity(x, y + shift, z) : null;
        }

        @Override
        public void tileMutation(int x, int y, int z, TileEntity tile) {
            if (!inside(x, y, z)) return;
            if (tile == null) world.removeTileEntity(x, y + shift, z);
            else world.setTileEntity(x, y + shift, z, tile);
        }

        @Override
        public int savedLight(EnumSkyBlock kind, int x, int y, int z) {
            return world.getSavedLightValue(kind, x, y + shift, z);
        }

        @Override
        public Random random() {
            return random;
        }

        @Override
        public int fullLight(int x, int y, int z) {
            return world.getFullBlockLightValue(x, y + shift, z);
        }

        @Override
        public boolean seesSky(int x, int y, int z) {
            return false;
        }

        @Override
        public boolean spawnEntity(Entity entity) {
            entity.setPosition(entity.posX, entity.posY + shift, entity.posZ);
            return world.spawnEntityInWorld(entity);
        }

        @Override
        public BiomeGenBase biome(int x, int z) {
            return world.getBiomeGenForCoords(x, z);
        }

        @Override
        public boolean freezable(int x, int y, int z) {
            return false;
        }
    }
}
