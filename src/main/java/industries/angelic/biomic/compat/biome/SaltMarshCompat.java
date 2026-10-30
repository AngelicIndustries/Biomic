package industries.angelic.biomic.compat.biome;

import java.lang.reflect.Constructor;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Native Salty marsh plants, not a copied plant/soil implementation or whole biome decorator. */
public final class SaltMarshCompat {

    public static final String BIOME = "saltymod:salt_marsh";
    public static final String VERSION = "2.0.0-master.150+9a0954aa6f";
    private static boolean discovered;
    private static Constructor<?> plants;
    private static Constructor<?> deadTree, fallenLog, bigDeadTree;
    private static Block saltGrass, saltDirt, saltwort, saltLog;
    private static Block mud, reedsBottom, reedsTop, saltFlower;
    private static Constructor<?> mudGenerator, saltwortGenerator, flowerGenerator, reedsGenerator;
    private static Constructor<?> pondGenerator;
    private static Constructor<?> campGenerator;
    private static int pondAttempts;
    private static boolean campEnabled;
    private static int campFrequency;
    private static String grassState, dirtState;
    private static String status = "not inspected";

    private SaltMarshCompat() {}

    public static boolean available() {
        discover();
        return plants != null && NativeFeatureCapture.installed();
    }

    public static String status() {
        discover();
        return status;
    }

    private static synchronized void discover() {
        if (discovered) return;
        discovered = true;
        var biome = NativeBiomeCatalogue.get(BIOME);
        if (biome == null || !VERSION.equals(biome.version)) {
            status = "Pinned Salty marsh absent or disabled";
            return;
        }
        try {
            Class<?> blocks = Class.forName("darkbum.saltymod.init.ModBlocks");
            saltGrass = (Block) blocks.getField("salt_grass")
                .get(null);
            saltDirt = (Block) blocks.getField("salt_dirt_lite")
                .get(null);
            saltwort = (Block) blocks.getField("saltworts")
                .get(null);
            saltLog = (Block) blocks.getField("salt_crusted_oak_log")
                .get(null);
            mud = (Block) blocks.getField("mineral_mud")
                .get(null);
            reedsBottom = (Block) blocks.getField("marsh_reeds_b")
                .get(null);
            reedsTop = (Block) blocks.getField("marsh_reeds_t")
                .get(null);
            saltFlower = (Block) blocks.getField("salt_flower_d")
                .get(null);
            for (Block block : new Block[] { mud, reedsBottom, reedsTop })
                NativeBlockCatalogue.encodeTerrainWrite(block, 0);
            grassState = NativeBlockCatalogue.encodeTerrainWrite(saltGrass, 0);
            dirtState = NativeBlockCatalogue.encodeTerrainWrite(saltDirt, 0);
            NativeBlockCatalogue.encodeTerrainWrite(saltwort, 0);
            NativeBlockCatalogue.encodeTerrainWrite(saltLog, 0);
            plants = Class.forName("darkbum.saltymod.world.worldgen.WorldGenSaltMarshPlants")
                .getConstructor(int.class);
            deadTree = Class.forName("darkbum.saltymod.world.worldgen.WorldGenSaltTree")
                .getConstructor(boolean.class, int.class);
            fallenLog = Class.forName("darkbum.saltymod.world.worldgen.WorldGenSaltLogs")
                .getConstructor(boolean.class, int.class);
            bigDeadTree = Class.forName("darkbum.saltymod.world.worldgen.WorldGenSaltBigTree")
                .getConstructor(boolean.class, boolean.class);
            mudGenerator = Class.forName("darkbum.saltymod.world.worldgen.WorldGenMineralMud")
                .getConstructor(int.class);
            saltwortGenerator = Class.forName("darkbum.saltymod.world.worldgen.WorldGenSaltwort")
                .getConstructor(int.class);
            flowerGenerator = Class.forName("darkbum.saltymod.world.worldgen.WorldGenAlliumMod")
                .getConstructor(int.class);
            reedsGenerator = Class.forName("darkbum.saltymod.world.worldgen.WorldGenMarshReeds")
                .getConstructor();
            pondGenerator = Class.forName("darkbum.saltymod.world.worldgen.WorldGenLakesMod")
                .getConstructor(Block.class, Block.class, int.class, int.class, int.class);
            campGenerator = Class.forName("darkbum.saltymod.world.worldgen.WorldGenBrickmakerCamp")
                .getConstructor();
            Class<?> worldConfig = Class.forName("darkbum.saltymod.common.config.ModConfigurationWorldGeneration");
            pondAttempts = worldConfig.getField("saltMarshWetness")
                .getInt(null);
            campEnabled = worldConfig.getField("enableBrickmakerCamp")
                .getBoolean(null);
            campFrequency = worldConfig.getField("brickmakerCampFrequency")
                .getInt(null);
            if (campFrequency <= 0)
                throw new IllegalStateException("Invalid Salty brickmaker camp frequency " + campFrequency);
            status = "native Salty 2.0.8 artifact / " + VERSION;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            plants = null;
            status = error.toString();
        }
    }

    public static String saltSoil(int depth) {
        if (!available()) throw new IllegalStateException(status());
        return depth == 0 ? grassState : dirtState;
    }

    /** Exact native substrate identity for source-ordered camp replacement; not a general replaceable-soil tag. */
    public static String mineralMudState() {
        if (!available()) throw new IllegalStateException(status());
        return NativeBlockCatalogue.encodeTerrainWrite(mud, 0);
    }

    public static String encodePlant(Block block, int meta) {
        if (!available()) throw new IllegalStateException(status());
        if (block == saltwort && meta >= 0 && meta <= 3) return NativeBlockCatalogue.encodeTerrainWrite(block, meta);
        if (block == Blocks.tallgrass && (meta == 0 || meta == 1))
            return meta == 0 ? "minecraft:tallgrass[legacy_meta=0]" : "minecraft:short_grass";
        if (meta == 0 && (block == mud || block == reedsBottom || block == reedsTop))
            return NativeBlockCatalogue.encodeTerrainWrite(block, meta);
        if (block == saltFlower && meta == 4) return NativeBlockCatalogue.encodeTerrainWrite(block, meta);
        if (block == Blocks.red_flower && meta == 2) return "minecraft:allium";
        if (block == Blocks.waterlily && meta == 0) return "minecraft:lily_pad";
        if (block == Blocks.reeds && meta == 0) return "minecraft:sugar_cane";
        if (block == Blocks.water && meta == 0) return "minecraft:water[level=0]";
        throw new IllegalArgumentException("Unexpected Salty marsh plant write: " + block + ":" + meta);
    }

    /** The pinned decorator's native patch sizes and per-chunk attempt counts. Not ocean flora. */
    public enum EcologyFeature {

        SHALLOW_PONDS(2),
        MINERAL_MUD(150),
        MIXED_PLANTS(5),
        SALTWORT(5),
        FLOWERS(1),
        LILY_PADS(4),
        MARSH_REEDS(4),
        SUGAR_CANE(3);

        public final int attempts;

        EcologyFeature(int attempts) {
            this.attempts = attempts;
        }

        public int count() {
            return this == SHALLOW_PONDS ? Math.max(0, Math.min(32, pondAttempts)) : attempts;
        }
    }

    static int shallowPondSeaBoundary(SaltMarshHeight heights) {
        return java.util.Objects.requireNonNull(heights, "Salty marsh height transport")
            .physicalSeaBoundary();
    }

    public static boolean placeEcology(EcologyFeature feature, World world, NativeFeatureCapture.Access access,
        Random random, int x, int y, int z, SaltMarshHeight heights) {
        if (!available()) return false;
        java.util.Objects.requireNonNull(heights, "Salty marsh height transport");
        try {
            WorldGenerator generator = (WorldGenerator) switch (feature) {
                // Biomic's water surface is one physical block above the native vanilla source level.
                case SHALLOW_PONDS -> pondGenerator.newInstance(
                    Blocks.water,
                    mud,
                    4,
                    shallowPondSeaBoundary(heights),
                    shallowPondSeaBoundary(heights));
                case MINERAL_MUD -> mudGenerator.newInstance(5);
                case MIXED_PLANTS -> plants.newInstance(64);
                case SALTWORT -> saltwortGenerator.newInstance(32);
                case FLOWERS -> flowerGenerator.newInstance(16);
                case LILY_PADS -> new net.minecraft.world.gen.feature.WorldGenWaterlily();
                case MARSH_REEDS -> reedsGenerator.newInstance();
                case SUGAR_CANE -> new net.minecraft.world.gen.feature.WorldGenReed();
            };
            NativeFeatureCapture.Access paired = new NativeFeatureCapture.Access() {

                public Block block(int px, int py, int pz) {
                    return access.block(px, py, pz);
                }

                public int metadata(int px, int py, int pz) {
                    return access.metadata(px, py, pz);
                }

                public int heightValue(int px, int pz) {
                    return access.heightValue(px, pz);
                }

                public boolean set(int px, int py, int pz, Block block, int meta) {
                    boolean placed = access.set(px, py, pz, block, meta);
                    // Captured writes deliberately suppress live block callbacks. Replay only the
                    // audited reed callback's second half inside the SAME bounded transaction.
                    if (placed && block == reedsBottom) {
                        if (access.block(px, py + 1, pz) != Blocks.air)
                            throw new IllegalStateException("Occupied native reed top");
                        return access.set(px, py + 1, pz, reedsTop, 0);
                    }
                    return placed;
                }
            };
            return NativeFeatureCapture.run(world, paired, () -> generator.generate(world, random, x, y, z));
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot invoke native Salty ecology", error);
        }
    }

    /** Narrow native substrate bridge for settlement; do not add salt soil to every modern dirt tag. */
    public static boolean supportsHostedGrass(String state, String below) {
        if (!"minecraft:short_grass".equals(state) && !"minecraft:grass".equals(state)
            && !"minecraft:tallgrass[legacy_meta=0]".equals(state)
            && !"minecraft:allium".equals(state)) return false;
        if (below == null || !below.startsWith("saltymod:salt_grass[")) return false;
        try {
            return "saltymod:salt_grass".equals(
                NativeBlockCatalogue.parse(below)
                    .key());
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    public enum WoodFeature {

        DEAD_TREE(5),
        FALLEN_LOG(5),
        BIG_DEAD_TREE(30);

        public final int rarity;

        WoodFeature(int rarity) {
            this.rarity = rarity;
        }
    }

    public static String encodeWood(Block block, int meta) {
        if (!available()) throw new IllegalStateException(status());
        if (block == saltLog && (meta == 0 || meta == 4 || meta == 8 || meta == 12))
            return NativeBlockCatalogue.encodeTerrainWrite(block, meta);
        if (block == Blocks.log && (meta == 0 || meta == 4 || meta == 8))
            return "minecraft:oak_log[axis=" + (meta == 0 ? "y" : meta == 4 ? "x" : "z") + "]";
        if (block == Blocks.dirt && meta == 0) return "minecraft:dirt";
        throw new IllegalArgumentException("Unexpected Salty woody feature write: " + block + ":" + meta);
    }

    public static boolean placeWood(WoodFeature feature, World world, NativeFeatureCapture.Access access, Random random,
        int x, int y, int z) {
        if (!available()) return false;
        try {
            // Native big trees retain World and height state. Never share a generator instance between placements.
            WorldGenerator generator = (WorldGenerator) switch (feature) {
                case DEAD_TREE -> deadTree.newInstance(false, 3);
                case FALLEN_LOG -> fallenLog.newInstance(false, 3);
                case BIG_DEAD_TREE -> bigDeadTree.newInstance(false, false);
            };
            return NativeFeatureCapture.run(world, access, () -> generator.generate(world, random, x, y, z));
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot invoke native Salty wood", error);
        }
    }

    /** Native config gate and one-in-N frequency. The host supplies the per-chunk deterministic random draw. */
    public static boolean shouldAttemptCamp(Random random) {
        discover();
        return campGenerator != null && campEnabled && random.nextInt(campFrequency) == 0;
    }

    public static SaltyBrickmakerCamp.Plan planCamp(World world, SaltyBrickmakerCamp.Surface surface, Random random,
        int x, int y, int z) {
        discover();
        if (campGenerator == null) return null;
        return SaltyBrickmakerCamp.generate(campGenerator, world, surface, random, x, y, z);
    }
}
