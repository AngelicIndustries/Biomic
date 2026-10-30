package industries.angelic.biomic.compat.biome;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenBase;

/** Pinned constructor invariants for the final BOP 2.1.0.2308 overworld batch. */
final class BopFinalProfile {

    private record Shape(float base, float variation, int grass, int flowers, int mushrooms, int reeds, int cacti,
        int lilies) {}

    private static final Map<String, Shape> SHAPES = Map.ofEntries(
        Map.entry("BiomeGenSacredSprings", new Shape(0F, .6F, 4, 0, 0, 0, 0, 5)),
        Map.entry("BiomeGenWasteland", new Shape(.1F, .1F, 20, 0, 0, 0, 0, 0)),
        Map.entry("BiomeGenOrchard", new Shape(.1F, .1F, 0, 0, 0, 0, 0, 0)),
        Map.entry("BiomeGenOasis", new Shape(-.2F, 0F, 0, 0, 0, 100, 7, 0)),
        Map.entry("BiomeGenScrubland", new Shape(.125F, .05F, 0, 0, 0, 0, 0, 0)),
        Map.entry("BiomeGenSpruceWoods", new Shape(.1F, .2F, 6, 0, 4, 0, 0, 0)),
        Map.entry("BiomeGenCoralReef", new Shape(-.6F, .1F, 0, 0, 0, 0, 0, 0)),
        Map.entry("BiomeGenKelpForest", new Shape(-1.2F, .1F, 0, 0, 0, 0, 0, 0)),
        Map.entry("BiomeGenMangrove", new Shape(0F, .2F, 0, 0, 0, -999, -999, 0)),
        Map.entry("BiomeGenTropics", new Shape(.2F, .3F, 7, 10, 0, 0, 0, 0)),
        Map.entry("BiomeGenVolcano", new Shape(2.5F, .5F, -999, -999, 0, 0, 0, 0)));
    private static final Map<String, Map<String, Object>> FEATURES = Map.ofEntries(
        Map.entry(
            "BiomeGenSacredSprings",
            values(
                "waterSpringsPerChunk",
                50,
                "lavaSpringsPerChunk",
                20,
                "waterLakesPerChunk",
                0,
                "bopLilyPerChunk",
                5,
                "seaweedPerChunk",
                15,
                "leafPilesPerChunk",
                10,
                "bopFlowersPerChunk",
                2)),
        Map.entry(
            "BiomeGenWasteland",
            values(
                "poisonLakesPerChunk",
                10,
                "waterLakesPerChunk",
                2,
                "wasteland1PerChunk",
                1,
                "wasteland2PerChunk",
                1,
                "wasteland3PerChunk",
                1,
                "wasteland4PerChunk",
                1,
                "wastelandRockPilesPerChunk",
                2,
                "bopGrassPerChunk",
                20)),
        Map.entry(
            "BiomeGenOrchard",
            values(
                "portobellosPerChunk",
                2,
                "berryBushesPerChunk",
                3,
                "wildCarrotsPerChunk",
                1,
                "shrubsPerChunk",
                10,
                "waterReedsPerChunk",
                4,
                "cloverPatchesPerChunk",
                15,
                "flaxPerChunk",
                1,
                "bopFlowersPerChunk",
                20,
                "bopGrassPerChunk",
                15)),
        Map.entry(
            "BiomeGenOasis",
            values(
                "tinyCactiPerChunk",
                2,
                "desertSproutsPerChunk",
                3,
                "bromeliadsPerChunk",
                4,
                "waterLakesPerChunk",
                4,
                "generateQuicksand",
                true,
                "generateMelons",
                true,
                "generatePumpkins",
                false,
                "bopGrassPerChunk",
                11)),
        Map.entry(
            "BiomeGenScrubland",
            values("shrubsPerChunk", 2, "waterReedsPerChunk", 2, "generatePumpkins", false, "bopGrassPerChunk", 30)),
        Map.entry(
            "BiomeGenSpruceWoods",
            values(
                "poisonIvyPerChunk",
                1,
                "sproutsPerChunk",
                1,
                "bushesPerChunk",
                2,
                "riverCanePerChunk",
                5,
                "berryBushesPerChunk",
                1,
                "shrubsPerChunk",
                2,
                "waterReedsPerChunk",
                6,
                "leafPilesPerChunk",
                15,
                "deadLeafPilesPerChunk",
                5,
                "cloverPatchesPerChunk",
                5,
                "algaePerChunk",
                2,
                "toadstoolsPerChunk",
                2,
                "flaxPerChunk",
                1,
                "bopGrassPerChunk",
                2,
                "bopFlowersPerChunk",
                5)),
        Map.entry(
            "BiomeGenCoralReef",
            values("coralPerChunk", 300, "shortKelpPerChunk", 100, "generateSponge", true, "seaweedPerChunk", 20)),
        Map.entry(
            "BiomeGenKelpForest",
            values("kelpPerChunk", 999, "kelpThickPerChunk", 999, "shortKelpPerChunk", 200, "seaweedPerChunk", 20)),
        Map.entry(
            "BiomeGenMangrove",
            values(
                "waterReedsPerChunk",
                2,
                "desertSproutsPerChunk",
                1,
                "waterLakesPerChunk",
                10,
                "bopGrassPerChunk",
                9)),
        Map.entry(
            "BiomeGenTropics",
            values(
                "bopFlowersPerChunk",
                30,
                "shrubsPerChunk",
                4,
                "leafPilesPerChunk",
                10,
                "seaweedPerChunk",
                10,
                "generatePumpkins",
                false,
                "bopGrassPerChunk",
                7)),
        Map.entry("BiomeGenVolcano", values("lavaLakesPerChunk", 20, "lavaSpoutsPerChunk", 1, "generateAsh", true)));

    private BopFinalProfile() {}

    private static Map<String, Object> values(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return Collections.unmodifiableMap(result);
    }

    static void validate(BiomeGenBase biome, BiomeDecorator decorator, Object features)
        throws ReflectiveOperationException {
        String name = biome.getClass()
            .getSimpleName();
        Shape shape = SHAPES.get(name);
        if (shape == null) throw new IllegalArgumentException("Not a final BOP profile: " + name);
        if (biome.rootHeight != shape.base || biome.heightVariation != shape.variation
            || decorator.grassPerChunk != shape.grass
            || decorator.flowersPerChunk != shape.flowers
            || decorator.mushroomsPerChunk != shape.mushrooms
            || decorator.reedsPerChunk != shape.reeds
            || decorator.cactiPerChunk != shape.cacti
            || decorator.waterlilyPerChunk != shape.lilies)
            throw new IllegalStateException("Native final-profile terrain/decorator changed: " + name);
        if (name.equals("BiomeGenMangrove") && decorator.deadBushPerChunk != 1
            || name.equals("BiomeGenTropics") && (decorator.sandPerChunk != 50 || decorator.sandPerChunk2 != 50))
            throw new IllegalStateException("Native final-profile vanilla schedule changed: " + name);
        for (var entry : FEATURES.get(name)
            .entrySet()) {
            Object actual = features.getClass()
                .getField(entry.getKey())
                .get(features);
            if (!entry.getValue()
                .equals(actual))
                throw new IllegalStateException("Native final-profile feature changed: " + name + '.' + entry.getKey());
        }
    }
}
