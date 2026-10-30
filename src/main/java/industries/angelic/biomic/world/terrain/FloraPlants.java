package industries.angelic.biomic.world.terrain;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import industries.angelic.biomic.Biomic;

/**
 * The plants of the flora palettes, resolved against the installed mods once. Each places itself with its own mod's
 * rules: a PMP plant through PMP's spawn routine (growth stage, two-block plants, bamboo clusters), except that a
 * palette colour of a multi-colour flower is set exactly; any other block when its own canPlaceBlockAt and
 * canBlockStay accept the spot. A plant whose mod or block is missing resolves to null and is skipped.
 */
final class FloraPlants {

    private static final Map<String, Plant> PLANTS = new ConcurrentHashMap<>();
    private static final Plant MISSING = new Plant("", null, 0, false, false);
    private static Method pmpSpawn;
    private static Class<?> pmpLocation, pmpParams;
    private static Field pmpWorldgen;
    private static boolean pmpUnavailable;

    private FloraPlants() {}

    static final class Plant {

        final String id;
        final Block block;
        /** Fixed metadata, or -1 when the plant's own routine chooses (PMP growth stage or colour). */
        final int metadata;
        final boolean pmp;
        final boolean tall;
        /** A flowerbed (pink petals, leaf litter, coneflowers, clover): metadata is petal count and facing. */
        final boolean flowerbed;
        int stages = 1, colours = 1;

        Plant(String id, Block block, int metadata, boolean pmp, boolean tall) {
            this.id = id;
            this.block = block;
            this.metadata = metadata;
            this.pmp = pmp;
            this.tall = tall || block instanceof net.minecraft.block.BlockDoublePlant;
            this.flowerbed = flowerbed(id);
        }

        /** Blocks placed (0 when the spot is refused). Runs inside a native capture over the chunk. */
        int place(World world, Random random, int x, int y, int z) {
            if (world.getBlock(x, y, z) != Blocks.air || !block.canPlaceBlockAt(world, x, y, z)) return 0;
            if (pmp && metadata < 0) return spawnPmp(world, random, x, y, z);
            if (flowerbed) {
                world.setBlock(x, y, z, block, random.nextInt(16), 2);
                if (world.getBlock(x, y, z) == block && block.canBlockStay(world, x, y, z)) return 1;
                world.setBlock(x, y, z, Blocks.air, 0, 2);
                return 0;
            }
            // A vanilla-style double plant (RU shrubs too): the upper half carries bit 8.
            boolean twoBlocks = !pmp && tall;
            if (twoBlocks && world.getBlock(x, y + 1, z) != Blocks.air) return 0;
            world.setBlock(x, y, z, block, Math.max(0, metadata), 2);
            if (twoBlocks) world.setBlock(x, y + 1, z, block, Math.max(0, metadata) | 8, 2);
            if (world.getBlock(x, y, z) != block || !block.canBlockStay(world, x, y, z)) {
                world.setBlock(x, y, z, Blocks.air, 0, 2);
                if (twoBlocks) world.setBlock(x, y + 1, z, Blocks.air, 0, 2);
                return 0;
            }
            return twoBlocks ? 2 : 1;
        }

        /**
         * A grown state of this plant: {lower metadata, upper metadata or -1}. A PMP multi-colour flower without a
         * palette colour takes the colour the hash picks; other PMP plants their last growth stage, as PMP's own
         * worldgen at full growth sets them.
         */
        int[] grown(long hash) {
            if (flowerbed) return new int[] { (int) Math.floorMod(hash, 16L), -1 };
            if (!pmp) {
                int meta = Math.max(0, metadata);
                if (!tall) return new int[] { meta, -1 };
                return new int[] { meta,
                    block instanceof net.minecraft.block.BlockDoublePlant
                        && !(block instanceof industries.angelic.biomic.content.block.BlockRuDoublePlant) ? 8
                            : meta | 8 };
            }
            if (metadata >= 0) return new int[] { metadata, -1 };
            if (colours > 1) return new int[] { (int) Math.floorMod(hash, (long) colours), -1 };
            int stage = Math.max(0, stages - 2);
            if (!tall) return new int[] { Math.min(15, stage), -1 };
            return switch (Math.min(stage, 4)) {
                case 0 -> new int[] { 0, -1 };
                case 1 -> new int[] { 1, -1 };
                case 2 -> new int[] { 2, 3 };
                case 3 -> new int[] { 4, 5 };
                default -> new int[] { 6, 7 };
            };
        }

        /** Whether the plant's own rule accepts this block under it (BlockBush#canPlaceBlockOn). */
        boolean acceptsSoil(Block soil) {
            if (!(block instanceof net.minecraft.block.BlockBush)) return true;
            try {
                return (Boolean) canPlaceBlockOn().invoke(block, soil);
            } catch (ReflectiveOperationException error) {
                return true;
            }
        }

        private int spawnPmp(World world, Random random, int x, int y, int z) {
            try {
                Object location = pmpLocation.getConstructor()
                    .newInstance();
                pmpLocation.getField("x")
                    .setInt(location, x);
                pmpLocation.getField("y")
                    .setInt(location, y);
                pmpLocation.getField("z")
                    .setInt(location, z);
                Object params = pmpParams.getConstructor(String.class)
                    .newInstance(id.substring(id.indexOf(':') + 1));
                boolean previous = pmpWorldgen.getBoolean(null);
                pmpWorldgen.setBoolean(null, true);
                try {
                    if (!(Boolean) pmpSpawn.invoke(null, world, random, location, params, block)) return 0;
                } finally {
                    pmpWorldgen.setBoolean(null, previous);
                }
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("PMP spawn failed for " + id, error);
            }
            if (world.getBlock(x, y, z) != block) return 0;
            return world.getBlock(x, y + 1, z) == block ? 2 : 1;
        }
    }

    /**
     * Flowerbed blocks: four petal counts by four facings in metadata (EFR pink petals, RU leaf litter and the like).
     */
    private static boolean flowerbed(String id) {
        return id.startsWith("etfuturum:pink_petals") || id.contains("_leaf_litter")
            || id.startsWith("biomic:clover")
            || id.startsWith("biomic:ru_decor_orange_coneflower")
            || id.startsWith("biomic:ru_decor_purple_coneflower");
    }

    /** The installed plant for a palette id ("mod:name:meta", or "plantmegapack:species[:colour]"), or null. */
    static Plant resolve(String id) {
        if (id.isEmpty()) return null; // an empty decor slot (DecorSlots): nothing grows there
        Plant plant = PLANTS.computeIfAbsent(id, FloraPlants::lookup);
        return plant == MISSING ? null : plant;
    }

    private static Plant lookup(String id) {
        String[] parts = id.split(":");
        if (parts.length < 2) return missing(id, "malformed id");
        Block block = Block.getBlockFromName(parts[0] + ":" + parts[1]);
        if (block == null || block == Blocks.air) return missing(id, "block not installed");
        if (parts[0].equals("plantmegapack")) {
            if (!pmpReady()) return missing(id, "PMP spawn API unavailable");
            Pmp data = pmpData(block);
            if (data == null) return missing(id, "not a PMP plant");
            int colour = parts.length > 2 ? Integer.parseInt(parts[2]) : -1;
            Plant plant = new Plant(parts[0] + ":" + parts[1], block, colour, true, data.tall);
            plant.stages = data.stages;
            plant.colours = data.colours;
            return plant;
        }
        int metadata = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        if (metadata < 0 || metadata > 15) return missing(id, "bad metadata");
        if (block.hasTileEntity(metadata)) return missing(id, "tile entity plants need their own adapter");
        return new Plant(id, block, metadata, false, false);
    }

    /** A plant whose own placement threw is dropped for the session, with the reason logged once. */
    static void disable(Plant plant, RuntimeException failure) {
        if (PLANTS.replace(plant.id, plant, MISSING))
            Biomic.LOG.warn("Flora palette plant {} disabled: its placement failed", plant.id, failure);
    }

    private static Plant missing(String id, String why) {
        Biomic.LOG.warn("Flora palette plant {} skipped: {}", id, why);
        return MISSING;
    }

    private record Pmp(boolean tall, int stages, int colours) {}

    private static Pmp pmpData(Block block) {
        try {
            Object data = block.getClass()
                .getField("plantData")
                .get(block);
            if (data == null) return null;
            Object attributes = data.getClass()
                .getField("attributes")
                .get(data);
            Object growth = attributes.getClass()
                .getField("growthType")
                .get(attributes);
            Object category = attributes.getClass()
                .getField("category")
                .get(attributes);
            int stages = attributes.getClass()
                .getField("growthStages")
                .getInt(attributes);
            int colours = "FLOWER_MULTI".equals(String.valueOf(category)) ? (Integer) block.getClass()
                .getMethod("getSubBlocksCount")
                .invoke(block) : 1;
            return new Pmp("DOUBLE".equals(String.valueOf(growth)), stages, colours);
        } catch (ReflectiveOperationException error) {
            return null;
        }
    }

    private static Method canPlaceBlockOn;

    private static synchronized Method canPlaceBlockOn() throws NoSuchMethodException {
        if (canPlaceBlockOn == null) {
            Method method;
            try {
                method = net.minecraft.block.BlockBush.class.getDeclaredMethod("func_149854_a", Block.class);
            } catch (NoSuchMethodException deobfuscated) {
                method = net.minecraft.block.BlockBush.class.getDeclaredMethod("canPlaceBlockOn", Block.class);
            }
            method.setAccessible(true);
            canPlaceBlockOn = method;
        }
        return canPlaceBlockOn;
    }

    private static synchronized boolean pmpReady() {
        if (pmpSpawn != null) return true;
        if (pmpUnavailable) return false;
        try {
            Class<?> plantClass = Class.forName("plantmegapack.block.PMPBlockPlant");
            pmpLocation = Class.forName("plantmegapack.worldgen.PMPSpawnLocation");
            pmpParams = Class.forName("plantmegapack.data.PMPDataPlantSpawnParams");
            pmpWorldgen = Class.forName("de.pilz.plantmegapackfix.PMPFix")
                .getField("isDoingWorldGen");
            pmpSpawn = Class.forName("plantmegapack.worldgen.PMPGenBase")
                .getMethod("spawnPlant", World.class, Random.class, pmpLocation, pmpParams, plantClass);
            return true;
        } catch (ReflectiveOperationException error) {
            pmpUnavailable = true;
            Biomic.LOG.warn("PMP plants skipped: spawn API unavailable ({})", error.toString());
            return false;
        }
    }
}
