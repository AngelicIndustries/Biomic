package industries.angelic.biomic.world.terrain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.biomic.api.ChunkVerticalLayout;
import industries.angelic.biomic.world.modern.ModernAquaticPlantSupport;
import industries.angelic.biomic.world.modern.ModernBlockSemantics;
import industries.angelic.biomic.world.modern.ModernDecorationEngine;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernFeatureEngine;
import industries.angelic.biomic.world.modern.noise.ModernWorldgenRandom;

public class HostedMarineEcologyPolicyTest {

    private static final String HYACINTH = "regions_unexplored:hyacinth_deeps";
    private static final String REEF = "regions_unexplored:rocky_reef";

    @Test
    public void originalAquaticProgramsReachDeepFloorAndStillRequireWaterAndSubstrate() {
        ModernDecorationEngine engine = new ModernDecorationEngine(42, new ModernFeatureEngine());
        for (String configured : List
            .of("minecraft:seagrass_short", "minecraft:kelp", "regions_unexplored:special/tall_hyacinth_stock")) {
            DeepWaterWorld world = new DeepWaterWorld("minecraft:stone", true);
            ModernWorldgenRandom random = new ModernWorldgenRandom();
            random.setSeed(73);
            Assert.assertTrue(configured, engine.placeConfigured(configured, world, random, new Pos(0, 79, 0)));
            Assert.assertFalse(configured, world.writes.isEmpty());
            Assert.assertTrue(
                world.writes.keySet()
                    .stream()
                    .allMatch(p -> p.y >= 79 && p.y < 190));
            Assert.assertTrue(
                world.writes.keySet()
                    .stream()
                    .anyMatch(p -> p.y == 79));
        }
        // RU's hyacinth plants place their blooms only (one placement in seven): never the animated seagrass.
        DeepWaterWorld hyacinth = new DeepWaterWorld("minecraft:stone", true);
        for (int seed = 0; seed < 64; seed++) {
            ModernWorldgenRandom random = new ModernWorldgenRandom();
            random.setSeed(seed);
            Assert.assertTrue(
                engine.placeConfigured(
                    "regions_unexplored:special/hyacinth_plants",
                    hyacinth,
                    random,
                    new Pos(seed * 32, 79, 0)));
        }
        Assert.assertFalse(hyacinth.writes.isEmpty());
        Assert.assertTrue(
            hyacinth.writes.keySet()
                .stream()
                .allMatch(p -> p.y == 79));
        Assert.assertTrue(
            hyacinth.writes.values()
                .stream()
                .allMatch(state -> state.startsWith("regions_unexplored:hyacinth_bloom")));
        DeepWaterWorld magma = new DeepWaterWorld("minecraft:magma_block", true);
        ModernWorldgenRandom random = new ModernWorldgenRandom();
        random.setSeed(73);
        Assert.assertFalse(engine.placeConfigured("minecraft:kelp", magma, random, new Pos(0, 79, 0)));
        Assert.assertTrue(magma.writes.isEmpty());
        DeepWaterWorld dry = new DeepWaterWorld("minecraft:stone", false);
        random.setSeed(73);
        Assert.assertFalse(engine.placeConfigured("minecraft:seagrass_short", dry, random, new Pos(0, 79, 0)));
        Assert.assertTrue(dry.writes.isEmpty());
    }

    private static byte[] ocean(int floor) {
        byte[] column = new byte[512];
        Arrays.fill(column, 0, floor + 1, BiomicTerrainSampler.STONE);
        Arrays.fill(column, floor + 1, 191, BiomicTerrainSampler.WATER);
        return column;
    }

    private static DecorationSurfaceColumn.Surface surface(byte[] column, boolean floating) {
        return DecorationSurfaceColumn.scan(column, ChunkVerticalLayout.EXTENDED_512, y -> floating)
            .get(0);
    }

    /** Explicit physical fixture, not a natural-world delivery claim; executes unchanged source programs. */
    private static final class DeepWaterWorld implements ModernDecorationEngine.World {

        final Map<Pos, String> writes = new HashMap<>();
        final String bed;
        final boolean water;

        DeepWaterWorld(String bed, boolean water) {
            this.bed = bed;
            this.water = water;
        }

        public long worldSeed() {
            return 42;
        }

        public int minBuildHeight() {
            return 64;
        }

        public int maxBuildHeight() {
            return 512;
        }

        public int seaLevel() {
            return 191;
        }

        public int height(String type, int x, int z) {
            return type.startsWith("OCEAN_FLOOR") ? 79 : 191;
        }

        public String blockState(int x, int y, int z) {
            return writes.getOrDefault(
                new Pos(x, y, z),
                y <= 78 ? bed : water && y <= 190 ? "minecraft:water[level=0]" : "minecraft:air");
        }

        public String fluidState(int x, int y, int z) {
            return ModernBlockSemantics.fluidState(blockState(x, y, z));
        }

        public String biome(int x, int y, int z) {
            return HYACINTH;
        }

        public String noiseBiome(int x, int y, int z) {
            return HYACINTH;
        }

        public boolean shouldFreeze(int x, int y, int z, boolean edge) {
            return false;
        }

        public boolean shouldSnow(int x, int y, int z) {
            return false;
        }

        public boolean isSolid(int x, int y, int z) {
            return y <= 78;
        }

        public boolean hasSturdyFace(int x, int y, int z, String direction) {
            return isSolid(x, y, z);
        }

        public boolean canSurvive(String state, int x, int y, int z) {
            return Boolean.TRUE.equals(
                ModernAquaticPlantSupport
                    .test(state, blockState(x, y - 1, z), blockState(x, y, z), hasSturdyFace(x, y - 1, z, "up")));
        }

        public boolean isReplaceable(int x, int y, int z) {
            return !isSolid(x, y, z);
        }

        public boolean randomChance(int x, int y, int z, float chance) {
            return false;
        }

        public boolean intersectsStructure(int x, int y, int z, int range, String structure) {
            return false;
        }

        public List<Pos> carvingMask(String step, int cx, int cz) {
            return List.of();
        }

        public boolean setBlock(int x, int y, int z, String state) {
            writes.put(new Pos(x, y, z), state);
            return true;
        }
    }
}
