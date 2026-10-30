package industries.angelic.biomic.world.terrain;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.dfsek.paralithic.functions.dynamic.DynamicFunction;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OrigenNativeFunctionsTest {

    @Test
    public void nativeHelpersAreBitIdenticalToPinnedParalithicExpressions() throws Exception {
        JsonObject manifest = manifest();
        OrigenTerrainRuntime.Compiler compiler = new OrigenTerrainRuntime.Compiler(new JsonObject(), new JsonObject());
        Method compile = OrigenTerrainRuntime.Compiler.class.getDeclaredMethod("compileFunction", JsonObject.class);
        compile.setAccessible(true);
        Random random = new Random(0xB10C42L);
        for (Map.Entry<String, JsonElement> entry : manifest.getAsJsonObject("functions")
            .entrySet()) {
            JsonObject definition = entry.getValue()
                .getAsJsonObject();
            int argumentCount = definition.getAsJsonArray("arguments")
                .size();
            DynamicFunction interpreted = (DynamicFunction) compile.invoke(compiler, definition);
            Method optimized = OrigenNativeFunctions.find(entry.getKey(), argumentCount);
            Assert.assertNotNull(entry.getKey(), optimized);
            for (int sample = 0; sample < 2_000; sample++) {
                double[] values = new double[argumentCount];
                Object[] boxed = new Object[argumentCount];
                for (int index = 0; index < argumentCount; index++) {
                    values[index] = random.nextDouble() * 16.0D - 8.0D;
                    boxed[index] = values[index];
                }
                double expected = interpreted.eval(values);
                double actual = ((Number) optimized.invoke(null, boxed)).doubleValue();
                Assert.assertEquals(
                    entry.getKey() + " sample " + sample,
                    Long.toHexString(Double.doubleToRawLongBits(expected)),
                    Long.toHexString(Double.doubleToRawLongBits(actual)));
            }
        }
    }

    @Test
    public void directSamplerShortcutPreservesAChildsTwoDimensionalOverload() throws Exception {
        JsonObject bedrock = manifest().getAsJsonObject("palettes")
            .getAsJsonObject("BEDROCK")
            .getAsJsonArray("layers")
            .get(0)
            .getAsJsonObject()
            .getAsJsonObject("sampler");
        OrigenTerrainRuntime.Compiler optimized = new OrigenTerrainRuntime.Compiler(
            new JsonObject(),
            new JsonObject(),
            true);
        OrigenTerrainRuntime.Compiler interpreted = new OrigenTerrainRuntime.Compiler(
            new JsonObject(),
            new JsonObject(),
            false);
        industries.angelic.biomic.internal.terra.noise.NoiseSampler expected = interpreted.compileSampler(bedrock);
        industries.angelic.biomic.internal.terra.noise.NoiseSampler actual = optimized.compileSampler(bedrock);
        Random random = new Random(0x2D1A3D42L);
        for (int sample = 0; sample < 1_000; sample++) {
            long seed = random.nextLong();
            double x = random.nextDouble() * 40_000.0D - 20_000.0D;
            double y = random.nextDouble() * 4.0D - 64.0D;
            double z = random.nextDouble() * 40_000.0D - 20_000.0D;
            assertRawEquals(
                "3D expression containing a 2D child sample " + sample,
                expected.noise(seed, x, y, z),
                actual.noise(seed, x, y, z));
        }
    }

    @Test
    public void directClimateExpressionsAreBitIdenticalToParalithic() throws Exception {
        JsonObject manifest = manifest();
        OrigenTerrainRuntime.Compiler optimized = new OrigenTerrainRuntime.Compiler(
            manifest.getAsJsonObject("functions"),
            manifest.getAsJsonObject("samplers"),
            true);
        OrigenTerrainRuntime.Compiler interpreted = new OrigenTerrainRuntime.Compiler(
            manifest.getAsJsonObject("functions"),
            manifest.getAsJsonObject("samplers"),
            false);
        List<JsonObject> expressions = new ArrayList<>();
        collectDirectClimateExpressions(manifest, expressions);
        Assert.assertEquals(74, expressions.size());

        Random random = new Random(0xC11A7E42L);
        for (JsonObject node : expressions) {
            String expression = node.get("expression")
                .getAsString();
            industries.angelic.biomic.internal.terra.noise.NoiseSampler expected = interpreted.compileSampler(node);
            industries.angelic.biomic.internal.terra.noise.NoiseSampler actual = optimized.compileSampler(node);
            for (int sample = 0; sample < 100; sample++) {
                long seed = random.nextLong();
                double x = random.nextDouble() * 40_000.0D - 20_000.0D;
                double y = random.nextDouble() * 384.0D - 64.0D;
                double z = random.nextDouble() * 40_000.0D - 20_000.0D;
                assertRawEquals(
                    expression + " 2D sample " + sample + " seed=" + seed + " x=" + x + " z=" + z,
                    expected.noise(seed, x, z),
                    actual.noise(seed, x, z));
                assertRawEquals(
                    expression + " 3D sample " + sample + " seed=" + seed + " x=" + x + " y=" + y + " z=" + z,
                    expected.noise(seed, x, y, z),
                    actual.noise(seed, x, y, z));
            }
        }
    }

    private static JsonObject manifest() throws Exception {
        try (InputStream input = OrigenNativeFunctionsTest.class
            .getResourceAsStream("/assets/biomic/origen/terrain-density.json")) {
            Assert.assertNotNull(input);
            return new JsonParser().parse(new InputStreamReader(input, StandardCharsets.UTF_8))
                .getAsJsonObject();
        }
    }

    private static void collectDirectClimateExpressions(JsonElement element, List<JsonObject> result) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) collectDirectClimateExpressions(child, result);
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        JsonElement expression = object.get("expression");
        if (expression != null) {
            String compact = expression.getAsString()
                .replaceAll("//[^\\r\\n]*", "")
                .replace(" ", "")
                .replace("\t", "")
                .replace("\r", "")
                .replace("\n", "");
            if ("(noise(x,z)+1)*spread".equals(compact)
                || "lerp(spotSizePercent(x,z),0,minRadius,1,maxRadius)".equals(compact)
                || ("max(max(-sampler(x/continentalScale/globalScale,z/continentalScale/globalScale)"
                    + "+continentalOffset,spawnIsland(x,z)),spotContinentalFactor*(1-(spotDistance(x,z)/"
                    + "spotRadius(x,z))^2))").equals(compact)
                || "if(y>max,-1,if(y<min,1,-y+min+(offset(x,y,z)+1)/2*(max+1-min)))".equals(compact)
                || "noise(x/elevationScale/globalScale,z/elevationScale/globalScale)^3".equals(compact)
                || ("herp(|riverNoise(x*riverFrequency,z*riverFrequency)|^2,erosionStart,erosionMax,erosionEnd,"
                    + "erosionMin)*herp(erosionStrength(x,z),0.2,0,0.8,1)").equals(compact)
                || ("herp(continents(x,z),continentZero,0,continentFull,rawElevation(x,z)*herp(spawnIsland(x,z),"
                    + "1,spawnIslandElevationScale,-1,1))*(1-riverTerrainErosion(x,z))").equals(compact)) {
                result.add(object);
            } else if (("-y+base+cellLean(x,z)*height*remapCellRange(cells(x,z))+cells(x,z)*"
                + "cellHeightVariation+surfaceBase(x,z)*mountainHeight").equals(compact)
                || "x*tiltStrength+z*tiltStrength*0.7".equals(compact)
                || "|webWalls(x,y,z)|<wallThickness".equals(compact)
                || "webPunchouts(x,y,z)>-webThickness".equals(compact)
                || ("-y+base+flatHeight*surfaceOffset(x,z)+max(mountains(x,z),0)*mountainHeight+"
                    + "if(mountains(x,z)>0.35,clamp((-pillars(x,z)-0.99)*pillarScale,0,pillarMaxHeight+"
                    + "mountains(x*2,z*2)*3),0)").equals(compact)
                || "-y+base".equals(compact)
                || "-y+base+lerp(overhangsMask(x,y,z),0,0,1,overhangs(x,y*3,z)*20)".equals(compact)) {
                    result.add(object);
                }
        }
        for (Map.Entry<String, JsonElement> child : object.entrySet()) {
            collectDirectClimateExpressions(child.getValue(), result);
        }
    }

    private static void assertRawEquals(String message, double expected, double actual) {
        Assert.assertEquals(
            message,
            Long.toHexString(Double.doubleToRawLongBits(expected)),
            Long.toHexString(Double.doubleToRawLongBits(actual)));
    }
}
