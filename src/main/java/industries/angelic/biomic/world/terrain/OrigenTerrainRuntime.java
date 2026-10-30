package industries.angelic.biomic.world.terrain;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dfsek.paralithic.Expression;
import com.dfsek.paralithic.eval.parser.Parser;
import com.dfsek.paralithic.eval.parser.Scope;
import com.dfsek.paralithic.eval.tokenizer.ParseException;
import com.dfsek.paralithic.functions.Function;
import com.dfsek.paralithic.functions.dynamic.Context;
import com.dfsek.paralithic.functions.dynamic.DynamicFunction;
import com.dfsek.paralithic.functions.natives.NativeMathFunction;
import com.dfsek.paralithic.node.Statefulness;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.internal.terra.noise.NoiseSampler;
import industries.angelic.biomic.internal.terra.noise.normalizer.ExpressionNormalizer;
import industries.angelic.biomic.internal.terra.noise.normalizer.LinearNormalizer;
import industries.angelic.biomic.internal.terra.noise.normalizer.ProbabilityNormalizer;
import industries.angelic.biomic.internal.terra.noise.paralithic.noise.NoiseFunction2;
import industries.angelic.biomic.internal.terra.noise.paralithic.noise.NoiseFunction3;
import industries.angelic.biomic.internal.terra.noise.samplers.DomainWarpedSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.KernelSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.LinearHeightmapSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.TranslateSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.arithmetic.AdditionSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.arithmetic.MultiplicationSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.CellularSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.ConstantSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.DistanceSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.ExpressionFunction;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.NoiseFunction;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.fractal.BrownianMotionSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.fractal.FractalNoiseFunction;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.fractal.RidgedFractalSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.random.GaussianNoiseSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.random.PositiveWhiteNoiseSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.random.WhiteNoiseSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.simplex.OpenSimplex2SSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.simplex.OpenSimplex2Sampler;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.value.ValueSampler;

/**
 * Executable form of the exact Origen density graphs authored for Terra 6.4.2.
 *
 * <p>
 * The JSON resource is generated from the pinned Origen revision. This class deliberately uses
 * a private copy of Terra 6.4.2's sampler implementations and the same Paralithic version instead
 * of translating the equations into Biomic-specific terrain families.
 * </p>
 */
final class OrigenTerrainRuntime {

    static final String RESOURCE = "/assets/biomic/origen/terrain-density.json";
    static final int EXPECTED_FORMAT = 1;

    private static final OrigenBiomeProfile[] PROFILE_VALUES = OrigenBiomeProfile.values();
    private static final OrigenTerrainRuntime INSTANCE = load();

    private final EnumMap<OrigenBiomeProfile, ProfileGraph> profiles;
    /**
     * Per arch profile: its graph with arch families dropped, by the dropped-family bit mask
     * (ArchLobeAdmission.Shape#bit).
     */
    private final EnumMap<OrigenBiomeProfile, NoiseSampler[]> archDrops;
    /** Per arch graph: the samplers its arch geometry reads (ArchLobeAdmission). */
    private final EnumMap<OrigenBiomeProfile, NoiseSampler[]> archSamplers;
    private final EnumMap<OrigenBiomeProfile, NoiseSampler> pillarlessBases;
    private final List<Extrusion> extrusions;
    private final ThreadLocal<double[]> extrusionColumnScratch = ThreadLocal
        .withInitial(() -> new double[OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE - OrigenDensityField.UPSTREAM_MIN_Y]);

    private OrigenTerrainRuntime(JsonObject manifest) {
        int format = required(manifest, "format").getAsInt();
        if (format != EXPECTED_FORMAT) {
            throw new IllegalStateException("Unsupported Origen terrain manifest format " + format);
        }
        JsonObject graphManifest = requiredObject(manifest, "graphs");
        JsonObject profileManifest = requiredObject(manifest, "profiles");
        String pillarGraphId = requiredObject(profileManifest, OrigenBiomeProfile.SNOWY_SPIRES.name()).get("base")
            .getAsString();
        Compiler compiler = new Compiler(
            requiredObject(manifest, "functions"),
            requiredObject(manifest, "samplers"),
            true);
        Map<String, NoiseSampler> graphs = new HashMap<String, NoiseSampler>();
        for (Map.Entry<String, JsonElement> entry : graphManifest.entrySet()) {
            JsonObject graph = entry.getValue()
                .getAsJsonObject();
            if (pillarGraphId.equals(entry.getKey())) {
                graph = polishConditionalPillars(graph, pillarGraphId);
            }
            if (countNamedSampler(graph, "arches") > 0) {
                graph = polishArches(graph, entry.getKey());
            }
            graphs.put(entry.getKey(), compiler.compileSampler(graph));
        }

        profiles = new EnumMap<OrigenBiomeProfile, ProfileGraph>(OrigenBiomeProfile.class);
        for (OrigenBiomeProfile profile : OrigenBiomeProfile.values()) {
            JsonElement rawProfile = profileManifest.get(profile.name());
            if (rawProfile == null || !rawProfile.isJsonObject()) {
                throw new IllegalStateException("Origen terrain manifest is missing " + profile.name());
            }
            JsonObject entry = rawProfile.getAsJsonObject();
            String baseId = required(entry, "base").getAsString();
            NoiseSampler base = graphs.get(baseId);
            if (base == null) {
                throw new IllegalStateException("Origen terrain manifest references missing graph " + baseId);
            }
            NoiseSampler elevation = null;
            JsonElement elevationId = entry.get("elevation");
            if (elevationId != null && !elevationId.isJsonNull()) {
                elevation = graphs.get(elevationId.getAsString());
                if (elevation == null) {
                    throw new IllegalStateException(
                        "Origen terrain manifest references missing graph " + elevationId.getAsString());
                }
            }
            String carverId = required(entry, "carver").getAsString();
            NoiseSampler carver = graphs.get(carverId);
            if (carver == null) {
                throw new IllegalStateException("Origen terrain manifest references missing graph " + carverId);
            }
            profiles.put(
                profile,
                new ProfileGraph(
                    base,
                    elevation,
                    carver,
                    entry.get("distance")
                        .getAsInt(),
                    entry.get("step")
                        .getAsInt(),
                    entry.get("weight")
                        .getAsDouble(),
                    entry.get("weight_2d")
                        .getAsDouble(),
                    strings(entry.getAsJsonArray("tags"))));
        }

        archDrops = new EnumMap<OrigenBiomeProfile, NoiseSampler[]>(OrigenBiomeProfile.class);
        compileArchlessBase(compiler, graphManifest, profileManifest, OrigenBiomeProfile.LUSH_LOOPS);
        compileArchlessBase(compiler, graphManifest, profileManifest, OrigenBiomeProfile.LOOPS_RIVER);
        compileArchFamilyDrops(compiler, graphManifest, profileManifest, OrigenBiomeProfile.STONEGATE_SEAS);
        compileArchFamilyDrops(compiler, graphManifest, profileManifest, OrigenBiomeProfile.ARCTIC_ARCHES);
        archSamplers = new EnumMap<OrigenBiomeProfile, NoiseSampler[]>(OrigenBiomeProfile.class);
        for (OrigenBiomeProfile profile : new OrigenBiomeProfile[] { OrigenBiomeProfile.LUSH_LOOPS,
            OrigenBiomeProfile.STONEGATE_SEAS, OrigenBiomeProfile.ARCTIC_ARCHES }) {
            JsonObject graph = graphManifest.get(
                requiredObject(profileManifest, profile.name()).get("base")
                    .getAsString())
                .getAsJsonObject();
            JsonObject graphSamplers = requiredObject(graph, "samplers");
            JsonObject archInner = requiredObject(requiredObject(graphSamplers, "arches"), "samplers");
            archSamplers.put(
                profile,
                new NoiseSampler[] { compiler.compileSampler(requiredObject(graphSamplers, "surfaceBase")),
                    compiler.compileSampler(requiredObject(archInner, "archMask")),
                    compiler.compileSampler(requiredObject(archInner, "pillarRand")) });
        }
        pillarlessBases = new EnumMap<OrigenBiomeProfile, NoiseSampler>(OrigenBiomeProfile.class);
        compilePillarlessBase(compiler, graphManifest, profileManifest, OrigenBiomeProfile.SNOWY_SPIRES);

        extrusions = new ArrayList<Extrusion>();
        JsonArray extrusionManifest = required(manifest, "extrusions").getAsJsonArray();
        for (JsonElement rawExtrusion : extrusionManifest) {
            JsonObject entry = rawExtrusion.getAsJsonObject();
            JsonObject range = requiredObject(entry, "range");
            List<OrigenBiomeProfile> choices = new ArrayList<OrigenBiomeProfile>();
            for (JsonElement rawChoice : required(entry, "to").getAsJsonArray()) {
                for (Map.Entry<String, JsonElement> choice : rawChoice.getAsJsonObject()
                    .entrySet()) {
                    OrigenBiomeProfile target = "SELF".equals(choice.getKey()) ? null
                        : OrigenBiomeProfile.valueOf(choice.getKey());
                    int weight = (int) compiler.numeric(choice.getValue());
                    for (int index = 0; index < weight; index++) {
                        choices.add(target);
                    }
                }
            }
            extrusions.add(
                new Extrusion(
                    required(entry, "from").getAsString(),
                    (int) compiler.numeric(required(range, "min")),
                    (int) compiler.numeric(required(range, "max")),
                    compiler.compileExtrusionSampler(requiredObject(entry, "sampler")),
                    choices,
                    profiles));
        }
    }

    static OrigenTerrainRuntime instance() {
        return INSTANCE;
    }

    ProfileGraph profile(OrigenBiomeProfile profile) {
        ProfileGraph graph = profiles.get(profile);
        if (graph == null) {
            throw new IllegalArgumentException("No Origen terrain graph for " + profile);
        }
        return graph;
    }

    double sampleBase(OrigenBiomeProfile profile, long seed, double x, double y, double z) {
        return profile(profile).base.noise(seed, x, y, z);
    }

    /** The profile's terrain without the arch families in {@code dropped} (ArchLobeAdmission.Shape#bit). */
    double sampleBaseWithoutArches(OrigenBiomeProfile profile, int dropped, long seed, double x, double y, double z) {
        NoiseSampler[] drops = archDrops.get(profile);
        if (drops == null || dropped <= 0 || dropped >= drops.length || drops[dropped] == null) {
            throw new IllegalArgumentException(
                "No Origen terrain graph for " + profile + " without arch families " + dropped);
        }
        return drops[dropped].noise(seed, x, y, z);
    }

    double sampleBaseWithoutSnowySpiresPillars(OrigenBiomeProfile profile, long seed, double x, double y, double z) {
        NoiseSampler sampler = pillarlessBases.get(profile);
        if (sampler == null) {
            throw new IllegalArgumentException("No pillarless Origen terrain graph for " + profile);
        }
        return sampler.noise(seed, x, y, z);
    }

    double sampleArchSurfaceBase(OrigenBiomeProfile graph, long seed, double x, double z) {
        return archSamplers.get(graph)[0].noise(seed, x, z);
    }

    /** The strip noise, read as the arch sampler reads it: archMask(z, 0). */
    double sampleArchMask(OrigenBiomeProfile graph, long seed, double transformedZ) {
        return archSamplers.get(graph)[1].noise(seed, transformedZ, 0.0D);
    }

    /** The strip's phase and height noise, read like the strip itself after polishArches: pillarRand(z, 0). */
    double sampleArchPillarRandom(OrigenBiomeProfile graph, long seed, double transformedZ) {
        return archSamplers.get(graph)[2].noise(seed, transformedZ, 0.0D);
    }

    double sampleElevation(OrigenBiomeProfile profile, long seed, double x, double z) {
        NoiseSampler elevation = profile(profile).elevation;
        return elevation == null ? 0.0D : elevation.noise(seed, x, z);
    }

    double sampleCarver(OrigenBiomeProfile profile, long seed, double x, double y, double z) {
        return profile(profile).carver.noise(seed, x, y, z);
    }

    OrigenBiomeProfile extrude(OrigenBiomeProfile base, long seed, int x, int y, int z) {
        OrigenBiomeProfile current = base;
        for (Extrusion extrusion : extrusions) {
            if (y < extrusion.minimumY || y >= extrusion.maximumY || !extrusion.accepts(current)) {
                continue;
            }
            double sample = extrusion.sampler.noise(seed, x, y, z);
            int index = (int) Math.floor((sample + 1.0D) * 0.5D * extrusion.choices.length);
            index = Math.max(0, Math.min(extrusion.choices.length - 1, index));
            OrigenBiomeProfile selected = extrusion.choices[index];
            if (selected != null) {
                current = selected;
            }
        }
        return current;
    }

    void fillExtrudedProfiles(OrigenBiomeProfile base, long seed, int x, int z, byte[] destination, int offset) {
        fillExtrudedProfilesStrided(
            base,
            seed,
            x,
            z,
            OrigenDensityField.UPSTREAM_MIN_Y,
            OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE,
            1,
            destination,
            offset);
    }

    void fillExtrudedProfilesStrided(OrigenBiomeProfile base, long seed, int x, int z, int minimumY,
        int maximumYExclusive, int yStep, byte[] destination, int offset) {
        if (yStep <= 0) throw new IllegalArgumentException("Y step must be positive");
        int sampleCount = (maximumYExclusive - minimumY + yStep - 1) / yStep;
        Arrays.fill(destination, offset, offset + sampleCount, (byte) base.ordinal());
        double[] samples = extrusionColumnScratch.get();
        for (Extrusion extrusion : extrusions) {
            int firstY = Math.max(minimumY, extrusion.minimumY);
            int remainder = Math.floorMod(firstY - minimumY, yStep);
            if (remainder != 0) firstY += yStep - remainder;
            int maximumY = Math.min(maximumYExclusive, extrusion.maximumY);
            if (firstY >= maximumY) continue;
            int accepted = 0;
            for (int y = firstY; y < maximumY; y += yStep) {
                int destinationIndex = offset + (y - minimumY) / yStep;
                OrigenBiomeProfile current = PROFILE_VALUES[destination[destinationIndex] & 0xFF];
                if (extrusion.accepts(current)) accepted++;
            }
            if (accepted == 0) continue;

            int extrusionSampleCount = (maximumY - firstY + yStep - 1) / yStep;
            boolean completeRange = accepted == extrusionSampleCount;
            if (completeRange) {
                extrusion.sampler.fillNoiseVerticalStrided(seed, x, firstY, maximumY, yStep, 1.0D, z, samples, 0);
            }
            int sampleIndex = 0;
            for (int y = firstY; y < maximumY; y += yStep) {
                int destinationIndex = offset + (y - minimumY) / yStep;
                OrigenBiomeProfile current = PROFILE_VALUES[destination[destinationIndex] & 0xFF];
                if (!extrusion.accepts(current)) {
                    sampleIndex++;
                    continue;
                }
                double sample = completeRange ? samples[sampleIndex] : extrusion.sampler.noise(seed, x, y, z);
                int choiceIndex = (int) Math.floor((sample + 1.0D) * 0.5D * extrusion.choices.length);
                choiceIndex = Math.max(0, Math.min(extrusion.choices.length - 1, choiceIndex));
                OrigenBiomeProfile selected = extrusion.choices[choiceIndex];
                if (selected != null) destination[destinationIndex] = (byte) selected.ordinal();
                sampleIndex++;
            }
        }
    }

    private static Set<String> strings(JsonArray values) {
        if (values == null) {
            return Collections.emptySet();
        }
        Set<String> strings = new HashSet<String>();
        for (JsonElement value : values) {
            strings.add(value.getAsString());
        }
        return Collections.unmodifiableSet(strings);
    }

    private void compileArchlessBase(Compiler compiler, JsonObject graphs, JsonObject profileManifest,
        OrigenBiomeProfile profile) {
        String graphId = requiredObject(profileManifest, profile.name()).get("base")
            .getAsString();
        JsonObject source = requiredObject(graphs, graphId);
        JsonObject copy = new JsonParser().parse(source.toString())
            .getAsJsonObject();
        int replacements = replaceNamedSampler(copy, "arches", constantSampler(-1.0D, 3));
        if (replacements != 1) {
            throw new IllegalStateException(
                "Expected one Lush Loops arch sampler in " + graphId + ", found " + replacements);
        }
        archDrops.put(profile, new NoiseSampler[] { null, compiler.compileSampler(copy) });
    }

    /**
     * Stonegate Seas and Arctic Arches sum the ground and two arch families, each family's term floored at -3 (absent).
     * Their graph without family A (bit 1), B (bit 2) or both: the dropped terms read as absent.
     */
    private void compileArchFamilyDrops(Compiler compiler, JsonObject graphs, JsonObject profileManifest,
        OrigenBiomeProfile profile) {
        String graphId = requiredObject(profileManifest, profile.name()).get("base")
            .getAsString();
        JsonObject source = polishArches(requiredObject(graphs, graphId), graphId);
        String[] terms = { "max(arches(rotate(x, z, angle), y - 19, rotate(z, x, angle)) * 10, -3)",
            "max(arches(rotate(x, z, -angle / 2), y - 15, rotate(z, x, -angle / 2)) * 10, -3)" };
        NoiseSampler[] drops = new NoiseSampler[4];
        for (int dropped = 1; dropped < 4; dropped++) {
            JsonObject copy = new JsonParser().parse(source.toString())
                .getAsJsonObject();
            String expression = required(copy, "expression").getAsString();
            for (int family = 0; family < 2; family++) {
                if ((dropped & 1 << family) == 0) continue;
                if (!expression.contains(terms[family])) {
                    throw new IllegalStateException("Expected arch family " + family + " in " + graphId);
                }
                expression = expression.replace(terms[family], "-3");
            }
            copy.addProperty("expression", expression);
            drops[dropped] = compiler.compileSampler(copy);
        }
        archDrops.put(profile, drops);
    }

    private void compilePillarlessBase(Compiler compiler, JsonObject graphs, JsonObject profileManifest,
        OrigenBiomeProfile profile) {
        String graphId = requiredObject(profileManifest, profile.name()).get("base")
            .getAsString();
        JsonObject source = requiredObject(graphs, graphId);
        JsonObject copy = new JsonParser().parse(source.toString())
            .getAsJsonObject();
        int replacements = replaceNamedSampler(copy, "pillars", constantSampler(0.0D, 3));
        if (replacements != 1) {
            throw new IllegalStateException(
                "Expected one Snowy Spires pillar sampler in " + graphId + ", found " + replacements);
        }
        pillarlessBases.put(profile, compiler.compileSampler(copy));
    }

    /**
     * Origen gates EQ_PILLARS by multiplying a cellular distance field with a second cellular NoiseLookup. Both
     * samplers use the same sites, but the lookup's zero-valued neighboring site can still own part of an enabled
     * pillar and slice it along a straight Voronoi edge. Preserve that upstream result, then add only the positive
     * overlap from the next enabled site's filtered distance/CellValue pair where the original owner was disabled.
     * Mesa Monuments shares this exact graph and receives the same geometric correction without eroding its ordinary
     * ground between monuments.
     */
    private static JsonObject polishConditionalPillars(JsonObject source, String graphId) {
        JsonObject copy = new JsonParser().parse(source.toString())
            .getAsJsonObject();
        JsonObject graphSamplers = requiredObject(copy, "samplers");
        JsonObject upstreamPillars = requiredObject(graphSamplers, "pillars");
        JsonObject enabledPillars = new JsonParser().parse(upstreamPillars.toString())
            .getAsJsonObject();
        JsonObject enabledSamplers = requiredObject(enabledPillars, "samplers");
        JsonObject existence = requiredObject(enabledSamplers, "basePillarsExistance");
        JsonObject activeSite = requiredObject(existence, "lookup");
        for (String samplerName : new String[] { "basepillars", "basepillarsCenter" }) {
            JsonObject cellular = requiredObject(enabledSamplers, samplerName);
            if (!"CELLULAR".equals(required(cellular, "type").getAsString())) {
                throw new IllegalStateException("Expected cellular " + samplerName + " in " + graphId);
            }
            cellular.add(
                "cell-filter",
                new JsonParser().parse(activeSite.toString())
                    .getAsJsonObject());
        }
        enabledSamplers.add("basePillarsExistance", constantSampler(1.0D, 2));

        // Preserve the upstream signed contribution wherever its nearest site is enabled. When that nearest site is
        // disabled, extend only the positive part of the next enabled site's pillar into the clipped corner. A raw
        // filtered cellular distance has no candidate in some 3x3 neighborhoods and becomes +MAX_VALUE; feeding that
        // through the original signed expression would subtract two density units and erode ordinary ground.
        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("type", "EXPRESSION");
        wrapper.addProperty("dimensions", 3);
        wrapper.addProperty(
            "expression",
            "upstreamPillars(x,y,z) + (1 - upstreamExistence(x,z)) * max(0, enabledPillars(x,y,z))");
        JsonObject wrapperSamplers = new JsonObject();
        wrapperSamplers.add(
            "upstreamPillars",
            new JsonParser().parse(upstreamPillars.toString())
                .getAsJsonObject());
        wrapperSamplers.add(
            "upstreamExistence",
            new JsonParser().parse(existence.toString())
                .getAsJsonObject());
        wrapperSamplers.add("enabledPillars", enabledPillars);
        wrapper.add("samplers", wrapperSamplers);
        graphSamplers.add("pillars", wrapper);
        return copy;
    }

    /**
     * Origen's arches (Stonegate Seas, Arctic Arches, Lush Loops and its river) are ribbons of a sine wave, three or
     * four
     * authored blocks thick, placed along strips of cell noise. Three things in the authored sampler break them apart:
     * <ul>
     * <li>Each strip takes its wave phase and height from {@code pillarRand(0, z)} but its place from
     * {@code archMask(z, 0)}: the same cell noise read along another line. Where a pillarRand cell border crosses a
     * strip, the arch splits lengthwise into two offset halves. Both now read {@code (z, 0)}: one cell, one arch.</li>
     * <li>The thickness is measured vertically, so on the steep legs the ribbon thins to a blade a block wide that the
     * voxel grid breaks into strands and loose blocks. The distance is now divided by the wave's slope term, giving
     * every part of the arch the same thickness across it.</li>
     * <li>Unbounded signed width noise at 0.8 can cut clean through: it now only thickens (the authored rough
     * outside stays).</li>
     * </ul>
     */
    private static JsonObject polishArches(JsonObject source, String graphId) {
        JsonObject copy = new JsonParser().parse(source.toString())
            .getAsJsonObject();
        int[] polished = { 0 };
        forEachNamedSampler(copy, "arches", arches -> {
            String expression = required(arches, "expression").getAsString();
            String keyed = expression.replace("pillarRand(0, z)", "pillarRand(z, 0)");
            // Compiled here: the runtime loads in a static initializer that runs before later static fields.
            java.util.regex.Matcher distance = java.util.regex.Pattern.compile(
                "\\(-abs\\(\\(base \\+ height \\* \\(pillarRand\\(z, 0\\) \\* 0\\.3 \\+ 1\\) \\* wave\\(x \\* 1\\.3 \\+ pillarRand\\(z, 0\\) \\* (\\d+), archRadius\\)\\) - y\\) \\+ archHeight\\) / archHeight")
                .matcher(keyed);
            if (keyed.equals(expression) || !distance.find()) {
                throw new IllegalStateException("Unexpected Origen arch sampler in " + graphId);
            }
            String phase = distance.group(1);
            String slope = "(height * (pillarRand(z, 0) * 0.3 + 1) * 1.3 / archRadius * cos((x * 1.3 + pillarRand(z, 0) * "
                + phase
                + ") / archRadius))";
            String even = distance.replaceFirst(
                java.util.regex.Matcher.quoteReplacement(
                    "(-abs((base + height * (pillarRand(z, 0) * 0.3 + 1) * wave(x * 1.3 + pillarRand(z, 0) * " + phase
                        + ", archRadius)) - y) / sqrt(1 + "
                        + slope
                        + " * "
                        + slope
                        + ") + archHeight) / archHeight"));
            arches.addProperty(
                "expression",
                even.replace("+ widthRandom(x,y,z) * 0.8", "+ max(widthRandom(x,y,z), 0) * 0.8"));
            polished[0]++;
        });
        if (polished[0] == 0) throw new IllegalStateException("No Origen arch sampler in " + graphId);
        return copy;
    }

    private static int countNamedSampler(JsonElement element, String name) {
        int[] count = { 0 };
        forEachNamedSampler(element, name, sampler -> count[0]++);
        return count[0];
    }

    private static void forEachNamedSampler(JsonElement element, String name,
        java.util.function.Consumer<JsonObject> action) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        JsonObject samplers = object.getAsJsonObject("samplers");
        if (samplers != null) {
            for (Map.Entry<String, JsonElement> sampler : samplers.entrySet()) {
                if (name.equals(sampler.getKey()) && sampler.getValue()
                    .isJsonObject())
                    action.accept(
                        sampler.getValue()
                            .getAsJsonObject());
                else forEachNamedSampler(sampler.getValue(), name, action);
            }
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!"samplers".equals(entry.getKey())) forEachNamedSampler(entry.getValue(), name, action);
        }
    }

    private static int replaceNamedSampler(JsonElement element, String name, JsonObject replacement) {
        if (element == null || !element.isJsonObject()) {
            return 0;
        }
        JsonObject object = element.getAsJsonObject();
        int replacements = 0;
        JsonObject samplers = object.getAsJsonObject("samplers");
        if (samplers != null && samplers.has(name)) {
            samplers.add(name, replacement);
            replacements++;
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!"samplers".equals(entry.getKey())) {
                replacements += replaceNamedSampler(entry.getValue(), name, replacement);
                continue;
            }
            for (Map.Entry<String, JsonElement> sampler : entry.getValue()
                .getAsJsonObject()
                .entrySet()) {
                if (!name.equals(sampler.getKey())) {
                    replacements += replaceNamedSampler(sampler.getValue(), name, replacement);
                }
            }
        }
        return replacements;
    }

    private static JsonObject constantSampler(double value, int dimensions) {
        JsonObject sampler = new JsonObject();
        sampler.addProperty("type", "CONSTANT");
        sampler.addProperty("value", value);
        sampler.addProperty("dimensions", dimensions);
        return sampler;
    }

    private static OrigenTerrainRuntime load() {
        try {
            return new OrigenTerrainRuntime(OrigenManifest.read());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not compile Origen terrain manifest", exception);
        }
    }

    private static JsonElement required(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            throw new IllegalStateException("Missing required Origen terrain property " + key);
        }
        return value;
    }

    private static JsonObject requiredObject(JsonObject object, String key) {
        JsonElement value = required(object, key);
        if (!value.isJsonObject()) {
            throw new IllegalStateException("Origen terrain property " + key + " is not an object");
        }
        return value.getAsJsonObject();
    }

    static final class ProfileGraph {

        private final NoiseSampler base;
        private final NoiseSampler elevation;
        private final NoiseSampler carver;
        private final int blendDistance;
        private final int blendStep;
        private final double blendWeight;
        private final double elevationWeight;
        private final Set<String> tags;

        private ProfileGraph(NoiseSampler base, NoiseSampler elevation, NoiseSampler carver, int blendDistance,
            int blendStep, double blendWeight, double elevationWeight, Set<String> tags) {
            this.base = base;
            this.elevation = elevation;
            this.carver = carver;
            this.blendDistance = blendDistance;
            this.blendStep = blendStep;
            this.blendWeight = blendWeight;
            this.elevationWeight = elevationWeight;
            this.tags = tags;
        }

        int getBlendDistance() {
            return blendDistance;
        }

        int getBlendStep() {
            return blendStep;
        }

        double getBlendWeight() {
            return blendWeight;
        }

        double getElevationWeight() {
            return elevationWeight;
        }
    }

    private static final class Extrusion {

        private final int minimumY;
        private final int maximumY;
        private final NoiseSampler sampler;
        private final OrigenBiomeProfile[] choices;
        private final boolean[] acceptedProfiles;

        private Extrusion(String from, int minimumY, int maximumY, NoiseSampler sampler,
            List<OrigenBiomeProfile> choices, EnumMap<OrigenBiomeProfile, ProfileGraph> profiles) {
            if (choices.isEmpty()) {
                throw new IllegalStateException("Origen extrusion must have at least one choice");
            }
            this.minimumY = minimumY;
            this.maximumY = maximumY;
            this.sampler = sampler;
            this.choices = choices.toArray(new OrigenBiomeProfile[choices.size()]);
            this.acceptedProfiles = new boolean[OrigenBiomeProfile.values().length];
            for (OrigenBiomeProfile profile : OrigenBiomeProfile.values()) {
                acceptedProfiles[profile.ordinal()] = "ALL".equals(from) ? !openSinkholeFloor(profile)
                    : profiles.get(profile).tags.contains(from);
            }
        }

        /**
         * Biomic: the cave habitats Origen extrudes into every column between authored -30 and 40 (Inferno Isles,
         * Terracotta Tombs, Vine Vault) are rock with caverns inside. A sinkhole's floor lies at -40 (±9), so that band
         * is its open sky, and a cave habitat there hung a domed mass of rock over the floor, its blend with the floor
         * an undercut lip and a sheer cliff where it ended (review, 2026-10-24: "the wall and round formation at 6499
         * 129 -19002 looks weird and unnatural"). The floors take no extrusion from ALL; the rim around them, whose
         * ground stands above the band, still does.
         */
        private static boolean openSinkholeFloor(OrigenBiomeProfile profile) {
            return profile == OrigenBiomeProfile.SINKHOLE_JUNGLE || profile == OrigenBiomeProfile.SINKHOLE_FOREST
                || profile == OrigenBiomeProfile.SINKHOLE_FROZEN;
        }

        private boolean accepts(OrigenBiomeProfile profile) {
            return acceptedProfiles[profile.ordinal()];
        }
    }

    /** Compiles the generated runtime-neutral manifest with Terra 6.4.2 defaults. */
    static final class Compiler {

        private final JsonObject globalFunctionDefinitions;
        private final Map<String, Function> globalFunctions;
        private final Map<String, DimensionedSampler> globalSamplers;
        private final Map<JsonObject, NoiseSampler> samplerCache = new IdentityHashMap<JsonObject, NoiseSampler>();
        private final Map<JsonObject, Function> functionCache = new IdentityHashMap<JsonObject, Function>();
        private final Map<String, Double> numericExpressionCache = new HashMap<String, Double>();
        private final boolean optimizeRuntime;

        Compiler(JsonObject functions, JsonObject samplers) {
            this(functions, samplers, true);
        }

        Compiler(JsonObject functions, JsonObject samplers, boolean optimizeRuntime) {
            this.optimizeRuntime = optimizeRuntime;
            globalFunctionDefinitions = functions;
            globalFunctions = optimizeRuntime ? compileGlobalFunctionDefinitions(functions)
                : compileFunctionDefinitions(functions);
            Map<String, ForwardingSampler> forwards = new LinkedHashMap<String, ForwardingSampler>();
            globalSamplers = new LinkedHashMap<String, DimensionedSampler>();
            for (Map.Entry<String, JsonElement> entry : samplers.entrySet()) {
                JsonObject node = entry.getValue()
                    .getAsJsonObject();
                ForwardingSampler forward = new ForwardingSampler(entry.getKey());
                forwards.put(entry.getKey(), forward);
                globalSamplers.put(entry.getKey(), new DimensionedSampler(forward, dimensions(node)));
            }
            for (Map.Entry<String, JsonElement> entry : samplers.entrySet()) {
                forwards.get(entry.getKey())
                    .bind(
                        compileSampler(
                            entry.getValue()
                                .getAsJsonObject()));
            }
        }

        NoiseSampler compileSampler(JsonObject node) {
            NoiseSampler cached = samplerCache.get(node);
            if (cached != null) {
                return cached;
            }
            String type = required(node, "type").getAsString();
            NoiseSampler sampler;
            switch (type) {
                case "OPEN_SIMPLEX_2":
                    sampler = configureNoise(new OpenSimplex2Sampler(), node);
                    break;
                case "PERLIN":
                    sampler = configureNoise(
                        new industries.angelic.biomic.internal.terra.noise.samplers.noise.simplex.PerlinSampler(),
                        node);
                    break;
                case "OPEN_SIMPLEX_2S":
                    sampler = configureNoise(new OpenSimplex2SSampler(), node);
                    break;
                case "WHITE_NOISE":
                    sampler = configureNoise(new WhiteNoiseSampler(), node);
                    break;
                case "POSITIVE_WHITE_NOISE":
                    sampler = configureNoise(new PositiveWhiteNoiseSampler(), node);
                    break;
                case "GAUSSIAN":
                    sampler = configureNoise(new GaussianNoiseSampler(), node);
                    break;
                case "VALUE":
                    sampler = configureNoise(new ValueSampler(), node);
                    break;
                case "CELLULAR":
                    sampler = compileCellular(node);
                    break;
                case "FBM":
                    sampler = compileFractal(new BrownianMotionSampler(child(node, "sampler")), node);
                    break;
                case "RIDGED":
                    sampler = compileFractal(new RidgedFractalSampler(child(node, "sampler")), node);
                    break;
                case "DOMAIN_WARP":
                    sampler = new DomainWarpedSampler(
                        child(node, "sampler"),
                        child(node, "warp"),
                        number(node, "amplitude", 1.0D));
                    break;
                case "TRANSLATE":
                    sampler = new TranslateSampler(
                        child(node, "sampler"),
                        number(node, "x", 0.0D),
                        number(node, "y", 0.0D),
                        number(node, "z", 0.0D));
                    break;
                case "LINEAR":
                    sampler = new LinearNormalizer(
                        child(node, "sampler"),
                        number(node, "min", 0.0D),
                        number(node, "max", 1.0D));
                    break;
                case "CLAMP": {
                    double minimum = requiredNumber(node, "min");
                    double maximum = requiredNumber(node, "max");
                    sampler = new industries.angelic.biomic.internal.terra.noise.normalizer.Normalizer(
                        child(node, "sampler")) {

                        @Override
                        public double normalize(double value) {
                            return Math.max(Math.min(value, maximum), minimum);
                        }
                    };
                    break;
                }
                case "PROBABILITY":
                    sampler = new ProbabilityNormalizer(child(node, "sampler"));
                    break;
                case "EXPRESSION_NORMALIZER":
                    sampler = compileExpressionNormalizer(node);
                    break;
                case "EXPRESSION":
                    sampler = compileExpression(node);
                    break;
                case "DISTANCE":
                    sampler = compileDistance(node);
                    break;
                case "KERNEL":
                    sampler = compileKernel(node);
                    break;
                case "CONSTANT":
                    sampler = new ConstantSampler(number(node, "value", 0.0D));
                    break;
                case "MUL":
                    sampler = new MultiplicationSampler(child(node, "left"), child(node, "right"));
                    break;
                case "ADD":
                    sampler = new AdditionSampler(child(node, "left"), child(node, "right"));
                    break;
                case "LINEAR_HEIGHTMAP":
                    sampler = new LinearHeightmapSampler(
                        optionalChild(node, "sampler", NoiseSampler.zero()),
                        number(node, "scale", 1.0D),
                        requiredNumber(node, "base"));
                    break;
                default:
                    throw new IllegalStateException("Unsupported Origen terrain sampler type " + type);
            }
            samplerCache.put(node, sampler);
            return sampler;
        }

        /** Removes the interpreter frame from Origen's globally-applied room-scale cave selector. */
        private NoiseSampler compileExtrusionSampler(JsonObject node) {
            if (!"EXPRESSION".equals(required(node, "type").getAsString())) return compileSampler(node);
            String expression = compactExpression(node);
            if (!"if(caves(x,y*2,z)<-0.79&&caveElevation(x,y*2,z)>0.1,caveVariation(x,y*2,z),-1)".equals(expression)) {
                return compileSampler(node);
            }
            JsonObject samplers = requiredObject(node, "samplers");
            NoiseSampler caves = compileSampler(requiredObject(samplers, "caves"));
            NoiseSampler caveElevation = compileSampler(requiredObject(samplers, "caveElevation"));
            NoiseSampler caveVariation = compileSampler(requiredObject(samplers, "caveVariation"));
            return new NoiseSampler() {

                private final ThreadLocal<ConditionalColumnScratchStack> conditionalScratch = ThreadLocal
                    .withInitial(ConditionalColumnScratchStack::new);

                @Override
                public double noise(long seed, double x, double z) {
                    return sample(seed, x, 0.0D, z);
                }

                @Override
                public double noise(long seed, double x, double y, double z) {
                    return sample(seed, x, y, z);
                }

                @Override
                public void fillNoiseVerticalStrided(long seed, double x, int minimumY, int maximumYExclusive,
                    int yStep, double yScale, double z, double[] samples, int offset) {
                    caves.fillNoiseVerticalStrided(
                        seed,
                        x,
                        minimumY,
                        maximumYExclusive,
                        yStep,
                        yScale * 2.0D,
                        z,
                        samples,
                        offset);
                    int count = (maximumYExclusive - minimumY + yStep - 1) / yStep;
                    ConditionalColumnScratchStack stack = conditionalScratch.get();
                    ConditionalColumnScratch scratch = stack.acquire();
                    try {
                        if (count > scratch.selected.length)
                            throw new IllegalArgumentException("Extrusion column exceeds authored height");
                        boolean anyElevation = false;
                        int row = 0;
                        for (int y = minimumY; y < maximumYExclusive; y += yStep) {
                            double doubledY = y * yScale * 2.0D;
                            scratch.y[row] = doubledY;
                            scratch.selected[row] = samples[offset + row] < -0.79D;
                            anyElevation |= scratch.selected[row];
                            row++;
                        }
                        if (anyElevation) caveElevation.fillNoiseVerticalMasked(
                            seed,
                            x,
                            z,
                            scratch.y,
                            0,
                            scratch.selected,
                            0,
                            count,
                            scratch.elevation,
                            0);
                        boolean anyVariation = false;
                        for (row = 0; row < count; row++) {
                            scratch.selected[row] = scratch.selected[row] && scratch.elevation[row] > 0.1D;
                            anyVariation |= scratch.selected[row];
                        }
                        if (anyVariation) caveVariation.fillNoiseVerticalMasked(
                            seed,
                            x,
                            z,
                            scratch.y,
                            0,
                            scratch.selected,
                            0,
                            count,
                            samples,
                            offset);
                        for (row = 0; row < count; row++) if (!scratch.selected[row]) samples[offset + row] = -1.0D;
                    } finally {
                        stack.release();
                    }
                }

                private final class ConditionalColumnScratch {

                    private final double[] y = new double[OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE
                        - OrigenDensityField.UPSTREAM_MIN_Y];
                    private final double[] elevation = new double[y.length];
                    private final boolean[] selected = new boolean[y.length];
                }

                private final class ConditionalColumnScratchStack {

                    private static final int MAX_DEPTH = 4;
                    private final ConditionalColumnScratch[] entries = new ConditionalColumnScratch[MAX_DEPTH];
                    private int depth;

                    private ConditionalColumnScratch acquire() {
                        if (depth == MAX_DEPTH)
                            throw new IllegalStateException("Extrusion sampler re-entry exceeded " + MAX_DEPTH);
                        ConditionalColumnScratch result = entries[depth];
                        if (result == null) entries[depth] = result = new ConditionalColumnScratch();
                        depth++;
                        return result;
                    }

                    private void release() {
                        if (depth <= 0) throw new IllegalStateException("Unbalanced extrusion scratch release");
                        depth--;
                    }
                }

                private double sample(long seed, double x, double y, double z) {
                    double doubledY = y * 2.0D;
                    return caves.noise(seed, x, doubledY, z) < -0.79D
                        && caveElevation.noise(seed, x, doubledY, z) > 0.1D ? caveVariation.noise(seed, x, doubledY, z)
                            : -1.0D;
                }
            };
        }

        double sampleNamed(String id, long seed, double x, double z, boolean threeDimensional) {
            DimensionedSampler sampler = globalSamplers.get(id);
            if (sampler == null) {
                throw new IllegalArgumentException("Unknown Origen global sampler " + id);
            }
            if (threeDimensional && sampler.dimensions != 3) {
                throw new IllegalArgumentException("Origen sampler " + id + " is not three-dimensional");
            }
            if (!threeDimensional && sampler.dimensions != 2) {
                throw new IllegalArgumentException("Origen sampler " + id + " is not two-dimensional");
            }
            /*
             * Terra 6.4.2's SamplerFunctionBuilder selects the three-dimensional sampler registry for a
             * four-argument TerraScript call, but passes twoD=true to both ConstantSamplerFunction and
             * SamplerFunction. The resulting function invokes the selected sampler's 2D overload and
             * ignores the middle coordinate. Origen's procedural boulders and mushrooms were authored against that
             * exact behavior, so retain the pinned loader quirk here rather than silently "fixing" their geometry.
             */
            return sampler.sampler.noise(seed, x, z);
        }

        private NoiseSampler compileCellular(JsonObject node) {
            CellularSampler cellular = new CellularSampler();
            cellular
                .setDistanceFunction(CellularSampler.DistanceFunction.valueOf(string(node, "distance", "EuclideanSq")));
            cellular.setReturnType(CellularSampler.ReturnType.valueOf(string(node, "return", "Distance")));
            cellular.setJitterModifier(number(node, "jitter", 1.0D));
            cellular.setNoiseLookup(optionalChild(node, "lookup", new OpenSimplex2Sampler()));
            cellular.setEdgeBlend(number(node, "edge-blend", 0.0D));
            // Imported noise graphs are pure. A Voronoi site's lookup coordinates are constant across
            // its owning rows: do not re-evaluate the entire landform/climate graph for each Y.
            // LocalNoiseLookup and filtered scalar calls remain uncached.
            cellular.setMemoizeCellLookup(optimizeRuntime);
            JsonElement cellFilter = node.get("cell-filter");
            if (cellFilter != null && !cellFilter.isJsonNull()) {
                cellular.setCellFilter(compileSampler(cellFilter.getAsJsonObject()));
            }
            return configureNoise(cellular, node);
        }

        private NoiseSampler compileFractal(FractalNoiseFunction fractal, JsonObject node) {
            fractal.setOctaves(integer(node, "octaves", 3));
            fractal.setGain(number(node, "gain", 0.5D));
            fractal.setLacunarity(number(node, "lacunarity", 2.0D));
            fractal.setWeightedStrength(number(node, "weighted-strength", 0.0D));
            return fractal;
        }

        private NoiseSampler compileExpression(JsonObject node) {
            NoiseSampler common = optimizeRuntime ? compileCommonExpression(node) : null;
            if (common != null) return common;
            NoiseSampler direct = optimizeRuntime ? directSamplerCall(node) : null;
            if (direct != null) return direct;
            try {
                return new ExpressionFunction(
                    expressionFunctions(node),
                    required(node, "expression").getAsString(),
                    variables(node));
            } catch (ParseException exception) {
                throw new IllegalStateException("Could not parse Origen density expression", exception);
            }
        }

        /** Removes interpreter frames from common exact expression shapes duplicated throughout Origen's climate. */
        private NoiseSampler compileCommonExpression(JsonObject node) {
            String expression = canonicalExpression(node);
            JsonObject localSamplers = object(node, "samplers");
            NoiseSampler profiled = compileProfiledSurfaceExpression(node, expression, localSamplers);
            if (profiled != null) return profiled;
            if ("(lerp(sampler(x,z),-1,1,1,-1)^4)*2-1".equals(expression) && localSamplers != null
                && localSamplers.has("sampler")) {
                NoiseSampler sampler = compileSampler(localSamplers.getAsJsonObject("sampler"));
                return sampler2d((seed, x, z) -> {
                    double mapped = OrigenNativeFunctions.lerp(sampler.noise(seed, x, z), -1.0D, 1.0D, 1.0D, -1.0D);
                    double squared = mapped * mapped;
                    return squared * squared * 2.0D - 1.0D;
                });
            }
            if ("abs(webs(x,z))-0.08".equals(expression) && localSamplers != null && localSamplers.has("webs")) {
                NoiseSampler webs = compileSampler(localSamplers.getAsJsonObject("webs"));
                return sampler2d((seed, x, z) -> Math.abs(webs.noise(seed, x, z)) - 0.08D);
            }
            if ("noise(x,y*10+yOffset(x,z)*100,z)*2+1".equals(expression)
                && hasSamplers(localSamplers, "noise", "yOffset")) {
                NoiseSampler noise = compileSampler(localSamplers.getAsJsonObject("noise"));
                NoiseSampler yOffset = compileSampler(localSamplers.getAsJsonObject("yOffset"));
                return sampler3d(
                    (seed, x, y, z) -> noise.noise(seed, x, y * 10.0D + yOffset.noise(seed, x, z) * 100.0D, z) * 2.0D
                        + 1.0D);
            }
            JsonObject rawVariables = object(node, "variables");
            if (rawVariables == null) return null;
            NoiseSampler riverDensity = compileRiverDensityExpression(expression, localSamplers, rawVariables);
            if (riverDensity != null) return riverDensity;
            NoiseSampler elevation = compileElevationExpression(expression, localSamplers, rawVariables);
            if (elevation != null) return elevation;
            NoiseSampler carver = compileCarverExpression(expression, localSamplers, rawVariables);
            if (carver != null) return carver;
            if ("-y+base".equals(expression) && rawVariables.has("base")) {
                double base = numeric(rawVariables.get("base"));
                return sampler3d((seed, x, y, z) -> -y + base);
            }
            if (localSamplers == null) return null;
            if ("(noise(x,z)+1)*spread".equals(expression) && localSamplers.has("noise")
                && rawVariables.has("spread")) {
                NoiseSampler noise = compileSampler(localSamplers.getAsJsonObject("noise"));
                double spread = numeric(rawVariables.get("spread"));
                return sampler2d((seed, x, z) -> (noise.noise(seed, x, z) + 1.0D) * spread);
            }
            if ("lerp(spotSizePercent(x,z),0,minRadius,1,maxRadius)".equals(expression)
                && localSamplers.has("spotSizePercent")
                && rawVariables.has("minRadius")
                && rawVariables.has("maxRadius")) {
                NoiseSampler spotSizePercent = compileSampler(localSamplers.getAsJsonObject("spotSizePercent"));
                double minimumRadius = numeric(rawVariables.get("minRadius"));
                double maximumRadius = numeric(rawVariables.get("maxRadius"));
                return sampler2d(
                    (seed, x, z) -> OrigenNativeFunctions
                        .lerp(spotSizePercent.noise(seed, x, z), 0.0D, minimumRadius, 1.0D, maximumRadius));
            }
            if ("-y+base+cellLean(x,z)*height*remapCellRange(cells(x,z))+cells(x,z)*cellHeightVariation"
                .concat("+surfaceBase(x,z)*mountainHeight")
                .equals(expression) && hasSamplers(localSamplers, "cellLean", "cells", "surfaceBase")) {
                NoiseSampler cellLean = compileSampler(localSamplers.getAsJsonObject("cellLean"));
                NoiseSampler cells = compileSampler(localSamplers.getAsJsonObject("cells"));
                NoiseSampler surfaceBase = compileSampler(localSamplers.getAsJsonObject("surfaceBase"));
                double base = numeric(rawVariables.get("base"));
                double height = numeric(rawVariables.get("height"));
                double cellHeightVariation = numeric(rawVariables.get("cellHeightVariation"));
                double mountainHeight = numeric(rawVariables.get("mountainHeight"));
                return sampler3d((seed, x, y, z) -> {
                    double density = -y + base;
                    double leanContribution = cellLean.noise(seed, x, z) * height;
                    double remapSource = cells.noise(seed, x, z);
                    double remappedCell = Math.abs(remapSource) < 0.4D ? 1.0D : remapSource;
                    density += leanContribution * remappedCell;
                    density += cells.noise(seed, x, z) * cellHeightVariation;
                    density += surfaceBase.noise(seed, x, z) * mountainHeight;
                    return density;
                });
            }
            if ("x*tiltStrength+z*tiltStrength*0.7".equals(expression) && rawVariables.has("tiltStrength")) {
                double tiltStrength = numeric(rawVariables.get("tiltStrength"));
                double secondaryTiltStrength = tiltStrength * 0.7D;
                return sampler2d((seed, x, z) -> x * tiltStrength + z * secondaryTiltStrength);
            }
            if ("|webWalls(x,y,z)|<wallThickness".equals(expression) && localSamplers.has("webWalls")) {
                NoiseSampler webWalls = compileSampler(localSamplers.getAsJsonObject("webWalls"));
                double wallThickness = numeric(rawVariables.get("wallThickness"));
                return sampler3d(
                    (seed, x, y, z) -> Math.abs(webWalls.noise(seed, x, y, z)) < wallThickness ? 1.0D : 0.0D);
            }
            if ("webPunchouts(x,y,z)>-webThickness".equals(expression) && localSamplers.has("webPunchouts")) {
                NoiseSampler webPunchouts = compileSampler(localSamplers.getAsJsonObject("webPunchouts"));
                double webThickness = numeric(rawVariables.get("webThickness"));
                return sampler3d((seed, x, y, z) -> webPunchouts.noise(seed, x, y, z) > -webThickness ? 1.0D : 0.0D);
            }
            if ("-y+base+flatHeight*surfaceOffset(x,z)+max(mountains(x,z),0)*mountainHeight"
                .concat("+if(mountains(x,z)>0.35,clamp((-pillars(x,z)-0.99)*pillarScale,0,")
                .concat("pillarMaxHeight+mountains(x*2,z*2)*3),0)")
                .equals(expression) && hasSamplers(localSamplers, "surfaceOffset", "mountains", "pillars")) {
                NoiseSampler surfaceOffset = compileSampler(localSamplers.getAsJsonObject("surfaceOffset"));
                NoiseSampler mountains = compileSampler(localSamplers.getAsJsonObject("mountains"));
                NoiseSampler pillars = compileSampler(localSamplers.getAsJsonObject("pillars"));
                double base = numeric(rawVariables.get("base"));
                double flatHeight = numeric(rawVariables.get("flatHeight"));
                double mountainHeight = numeric(rawVariables.get("mountainHeight"));
                double pillarScale = numeric(rawVariables.get("pillarScale"));
                double pillarMaximumHeight = numeric(rawVariables.get("pillarMaxHeight"));
                return sampler3d((seed, x, y, z) -> {
                    double mountain = mountains.noise(seed, x, z);
                    double density = -y + base
                        + flatHeight * surfaceOffset.noise(seed, x, z)
                        + Math.max(mountain, 0.0D) * mountainHeight;
                    if (mountain > 0.35D) {
                        density += OrigenNativeFunctions.clamp(
                            (-pillars.noise(seed, x, z) - 0.99D) * pillarScale,
                            0.0D,
                            pillarMaximumHeight + mountains.noise(seed, x * 2.0D, z * 2.0D) * 3.0D);
                    }
                    return density;
                });
            }
            if ("-y+base+lerp(overhangsMask(x,y,z),0,0,1,overhangs(x,y*3,z)*20)".equals(expression)
                && hasSamplers(localSamplers, "overhangsMask", "overhangs")) {
                NoiseSampler overhangsMask = compileSampler(localSamplers.getAsJsonObject("overhangsMask"));
                NoiseSampler overhangs = compileSampler(localSamplers.getAsJsonObject("overhangs"));
                double base = numeric(rawVariables.get("base"));
                return sampler3d(
                    (seed, x, y, z) -> -y + base
                        + OrigenNativeFunctions.lerp(
                            overhangsMask.noise(seed, x, y, z),
                            0.0D,
                            0.0D,
                            1.0D,
                            overhangs.noise(seed, x, y * 3.0D, z) * 20.0D));
            }
            if ("max(max(-sampler(x/continentalScale/globalScale,z/continentalScale/globalScale)"
                .concat("+continentalOffset,spawnIsland(x,z)),spotContinentalFactor*(1-(spotDistance(x,z)/")
                .concat("spotRadius(x,z))^2))")
                .equals(expression)
                && hasSamplers(localSamplers, "sampler", "spawnIsland", "spotDistance", "spotRadius")) {
                NoiseSampler continentalSampler = compileSampler(localSamplers.getAsJsonObject("sampler"));
                NoiseSampler spawnIsland = compileSampler(localSamplers.getAsJsonObject("spawnIsland"));
                NoiseSampler spotDistance = compileSampler(localSamplers.getAsJsonObject("spotDistance"));
                NoiseSampler spotRadius = compileSampler(localSamplers.getAsJsonObject("spotRadius"));
                double continentalScale = numeric(rawVariables.get("continentalScale"));
                double globalScale = numeric(rawVariables.get("globalScale"));
                double continentalOffset = numeric(rawVariables.get("continentalOffset"));
                double spotContinentalFactor = numeric(rawVariables.get("spotContinentalFactor"));
                return sampler2d((seed, x, z) -> {
                    double continental = -continentalSampler
                        .noise(seed, x / continentalScale / globalScale, z / continentalScale / globalScale)
                        + continentalOffset;
                    double spotRatio = spotDistance.noise(seed, x, z) / spotRadius.noise(seed, x, z);
                    return Math.max(
                        Math.max(continental, spawnIsland.noise(seed, x, z)),
                        spotContinentalFactor * (1.0D - spotRatio * spotRatio));
                });
            }
            if ("if(y>max,-1,if(y<min,1,-y+min+(offset(x,y,z)+1)/2*(max+1-min)))".equals(expression)
                && localSamplers.has("offset")) {
                NoiseSampler offset = compileSampler(localSamplers.getAsJsonObject("offset"));
                double maximum = numeric(rawVariables.get("max"));
                double minimum = numeric(rawVariables.get("min"));
                return sampler3d(
                    (seed, x, y, z) -> y > maximum ? -1.0D
                        : y < minimum ? 1.0D
                            : -y + minimum + (offset.noise(seed, x, y, z) + 1.0D) / 2.0D * (maximum + 1.0D - minimum));
            }
            if ("noise(x/elevationScale/globalScale,z/elevationScale/globalScale)^3".equals(expression)
                && localSamplers.has("noise")) {
                NoiseSampler noise = compileSampler(localSamplers.getAsJsonObject("noise"));
                double elevationScale = numeric(rawVariables.get("elevationScale"));
                double globalScale = numeric(rawVariables.get("globalScale"));
                return sampler2d((seed, x, z) -> {
                    double sampled = noise
                        .noise(seed, x / elevationScale / globalScale, z / elevationScale / globalScale);
                    return sampled * (sampled * sampled);
                });
            }
            if ("herp(continents(x,z),continentZero,0,continentFull,-1+rawElevation(x,z))".equals(expression)
                && hasSamplers(localSamplers, "continents", "rawElevation")) {
                NoiseSampler continents = compileSampler(localSamplers.getAsJsonObject("continents"));
                NoiseSampler rawElevation = compileSampler(localSamplers.getAsJsonObject("rawElevation"));
                double continentZero = numeric(rawVariables.get("continentZero"));
                double continentFull = numeric(rawVariables.get("continentFull"));
                return sampler2d(
                    (seed, x, z) -> OrigenNativeFunctions.herp(
                        continents.noise(seed, x, z),
                        continentZero,
                        0.0D,
                        continentFull,
                        -1.0D + rawElevation.noise(seed, x, z)));
            }
            if ("herp(|riverNoise(x*riverFrequency,z*riverFrequency)|^2,erosionStart,erosionMax,erosionEnd,"
                .concat("erosionMin)*herp(erosionStrength(x,z),0.2,0,0.8,1)")
                .equals(expression) && hasSamplers(localSamplers, "riverNoise", "erosionStrength")) {
                NoiseSampler riverNoise = compileSampler(localSamplers.getAsJsonObject("riverNoise"));
                NoiseSampler erosionStrength = compileSampler(localSamplers.getAsJsonObject("erosionStrength"));
                double riverFrequency = numeric(rawVariables.get("riverFrequency"));
                double erosionStart = numeric(rawVariables.get("erosionStart"));
                double erosionMaximum = numeric(rawVariables.get("erosionMax"));
                double erosionEnd = numeric(rawVariables.get("erosionEnd"));
                double erosionMinimum = numeric(rawVariables.get("erosionMin"));
                return sampler2d((seed, x, z) -> {
                    double river = Math.abs(riverNoise.noise(seed, x * riverFrequency, z * riverFrequency));
                    return OrigenNativeFunctions
                        .herp(river * river, erosionStart, erosionMaximum, erosionEnd, erosionMinimum)
                        * OrigenNativeFunctions.herp(erosionStrength.noise(seed, x, z), 0.2D, 0.0D, 0.8D, 1.0D);
                });
            }
            if ("herp(continents(x,z),continentZero,0,continentFull,rawElevation(x,z)*herp(spawnIsland(x,z),1,"
                .concat("spawnIslandElevationScale,-1,1))*(1-riverTerrainErosion(x,z))")
                .equals(expression)
                && hasSamplers(localSamplers, "continents", "rawElevation", "spawnIsland", "riverTerrainErosion")) {
                NoiseSampler continents = compileSampler(localSamplers.getAsJsonObject("continents"));
                NoiseSampler rawElevation = compileSampler(localSamplers.getAsJsonObject("rawElevation"));
                NoiseSampler spawnIsland = compileSampler(localSamplers.getAsJsonObject("spawnIsland"));
                NoiseSampler riverTerrainErosion = compileSampler(localSamplers.getAsJsonObject("riverTerrainErosion"));
                double continentZero = numeric(rawVariables.get("continentZero"));
                double continentFull = numeric(rawVariables.get("continentFull"));
                double spawnIslandElevationScale = numeric(rawVariables.get("spawnIslandElevationScale"));
                return sampler2d(
                    (seed, x,
                        z) -> OrigenNativeFunctions.herp(
                            continents.noise(seed, x, z),
                            continentZero,
                            0.0D,
                            continentFull,
                            rawElevation.noise(seed, x, z) * OrigenNativeFunctions
                                .herp(spawnIsland.noise(seed, x, z), 1.0D, spawnIslandElevationScale, -1.0D, 1.0D))
                            * (1.0D - riverTerrainErosion.noise(seed, x, z)));
            }
            return null;
        }

        private NoiseSampler compileElevationExpression(String expression, JsonObject localSamplers,
            JsonObject variables) {
            if (localSamplers == null) return null;
            if ("scale*(terrace(combine(combine(elevation(x,z),detailStart,0,detailEnd,detail(x,z)*detailFactor),"
                .concat("mountainPeakStart,0,mountainPeakEnd,mountainPeaks(x,z)*mountainPeakFactor),")
                .concat("terraceBaseScale+terraceScale(x,z)*terraceScaleVariation,")
                .concat("terraceOffset(x,z)*terraceOffsetVariation,0,(terraceDepth(x,z)^3)*")
                .concat("terraceDepthVariation,0,terraceDepthStart))")
                .equals(expression)
                && hasSamplers(
                    localSamplers,
                    "detail",
                    "mountainPeaks",
                    "terraceDepth",
                    "terraceOffset",
                    "terraceScale")
                && hasVariables(
                    variables,
                    "detailEnd",
                    "detailFactor",
                    "detailStart",
                    "mountainPeakEnd",
                    "mountainPeakFactor",
                    "mountainPeakStart",
                    "scale",
                    "terraceBaseScale",
                    "terraceDepthStart",
                    "terraceDepthVariation",
                    "terraceOffsetVariation",
                    "terraceScaleVariation")) {
                DimensionedSampler globalElevation = globalSamplers.get("elevation");
                if (globalElevation == null || globalElevation.dimensions != 2) return null;
                NoiseSampler detail = compileSampler(localSamplers.getAsJsonObject("detail"));
                NoiseSampler mountainPeaks = compileSampler(localSamplers.getAsJsonObject("mountainPeaks"));
                NoiseSampler terraceDepth = compileSampler(localSamplers.getAsJsonObject("terraceDepth"));
                NoiseSampler terraceOffset = compileSampler(localSamplers.getAsJsonObject("terraceOffset"));
                NoiseSampler terraceScale = compileSampler(localSamplers.getAsJsonObject("terraceScale"));
                double detailEnd = numeric(variables.get("detailEnd"));
                double detailFactor = numeric(variables.get("detailFactor"));
                double detailStart = numeric(variables.get("detailStart"));
                double mountainPeakEnd = numeric(variables.get("mountainPeakEnd"));
                double mountainPeakFactor = numeric(variables.get("mountainPeakFactor"));
                double mountainPeakStart = numeric(variables.get("mountainPeakStart"));
                double scale = numeric(variables.get("scale"));
                double terraceBaseScale = numeric(variables.get("terraceBaseScale"));
                double terraceDepthStart = numeric(variables.get("terraceDepthStart"));
                double terraceDepthVariation = numeric(variables.get("terraceDepthVariation"));
                double terraceOffsetVariation = numeric(variables.get("terraceOffsetVariation"));
                double terraceScaleVariation = numeric(variables.get("terraceScaleVariation"));
                return sampler2d((seed, x, z) -> {
                    double input = combine(
                        globalElevation.sampler.noise(seed, x, z),
                        detailStart,
                        0.0D,
                        detailEnd,
                        detail.noise(seed, x, z) * detailFactor);
                    input = combine(
                        input,
                        mountainPeakStart,
                        0.0D,
                        mountainPeakEnd,
                        mountainPeaks.noise(seed, x, z) * mountainPeakFactor);
                    double depthSample = terraceDepth.noise(seed, x, z);
                    double depth = depthSample * depthSample * depthSample * terraceDepthVariation;
                    depth = OrigenNativeFunctions.lerp(input, 0.0D, 0.0D, terraceDepthStart, depth);
                    return scale * OrigenNativeFunctions.terraceParalinear(
                        input,
                        terraceBaseScale + terraceScale.noise(seed, x, z) * terraceScaleVariation,
                        terraceOffset.noise(seed, x, z) * terraceOffsetVariation,
                        0.0D,
                        depth);
                });
            }
            if ("scale*combine(combine(elevation(x,z)*0.1,mountainPeakStart,0,mountainPeakEnd,"
                .concat("mountainPeaks(x,z)*mountainPeakFactor),duneStart,")
                .concat("dunes(x*duneFrequency,z*duneFrequency)*duneFactor,duneEnd,0)")
                .equals(expression) && hasSamplers(localSamplers, "dunes", "mountainPeaks")
                && hasVariables(
                    variables,
                    "duneEnd",
                    "duneFactor",
                    "duneFrequency",
                    "duneStart",
                    "mountainPeakEnd",
                    "mountainPeakFactor",
                    "mountainPeakStart",
                    "scale")) {
                DimensionedSampler globalElevation = globalSamplers.get("elevation");
                if (globalElevation == null || globalElevation.dimensions != 2) return null;
                NoiseSampler dunes = compileSampler(localSamplers.getAsJsonObject("dunes"));
                NoiseSampler mountainPeaks = compileSampler(localSamplers.getAsJsonObject("mountainPeaks"));
                double duneEnd = numeric(variables.get("duneEnd"));
                double duneFactor = numeric(variables.get("duneFactor"));
                double duneFrequency = numeric(variables.get("duneFrequency"));
                double duneStart = numeric(variables.get("duneStart"));
                double mountainPeakEnd = numeric(variables.get("mountainPeakEnd"));
                double mountainPeakFactor = numeric(variables.get("mountainPeakFactor"));
                double mountainPeakStart = numeric(variables.get("mountainPeakStart"));
                double scale = numeric(variables.get("scale"));
                return sampler2d((seed, x, z) -> {
                    double input = combine(
                        globalElevation.sampler.noise(seed, x, z) * 0.1D,
                        mountainPeakStart,
                        0.0D,
                        mountainPeakEnd,
                        mountainPeaks.noise(seed, x, z) * mountainPeakFactor);
                    return scale * combine(
                        input,
                        duneStart,
                        dunes.noise(seed, x * duneFrequency, z * duneFrequency) * duneFactor,
                        duneEnd,
                        0.0D);
                });
            }
            if ("-mask(x,z)*(-|sin((x*sin(rotation(x,z)*duneRotationRange)+z*cos(rotation(x,z)*"
                .concat("duneRotationRange))/duneSpacing)|+1)")
                .equals(expression) && hasSamplers(localSamplers, "mask", "rotation")
                && hasVariables(variables, "duneRotationRange", "duneSpacing")) {
                NoiseSampler mask = compileSampler(localSamplers.getAsJsonObject("mask"));
                NoiseSampler rotation = compileSampler(localSamplers.getAsJsonObject("rotation"));
                double duneRotationRange = numeric(variables.get("duneRotationRange"));
                double duneSpacing = numeric(variables.get("duneSpacing"));
                return sampler2d((seed, x, z) -> {
                    double angle = rotation.noise(seed, x, z) * duneRotationRange;
                    double wave = Math.sin((x * Math.sin(angle) + z * Math.cos(angle)) / duneSpacing);
                    return -mask.noise(seed, x, z) * (-Math.abs(wave) + 1.0D);
                });
            }
            return null;
        }

        private static double combine(double value, double at, double a, double bt, double b) {
            return value + OrigenNativeFunctions.lerp(value, at, a, bt, b);
        }

        private NoiseSampler compileRiverDensityExpression(String expression, JsonObject localSamplers,
            JsonObject variables) {
            if (localSamplers == null) return null;
            if ("if(ceilingEq(x,y,z)>0,(ceilingEq(x,y,z)^2)*(1/originalStrength),0)".equals(expression)
                && hasSamplers(localSamplers, "ceilingEq")
                && hasVariables(variables, "originalStrength")) {
                NoiseSampler ceiling = compileSampler(localSamplers.getAsJsonObject("ceilingEq"));
                double originalStrength = numeric(variables.get("originalStrength"));
                return sampler3d((seed, x, y, z) -> {
                    double value = ceiling.noise(seed, x, y, z);
                    return value > 0.0D ? Math.pow(value, 2.0D) * (1.0D / originalStrength) : 0.0D;
                });
            }
            if ("if(y>ceilingHeight+ceilingVariation,parentEq(x,y,z),if(y<floorHeight,bottomEq(x,y,z),"
                .concat("max(parentEq(x,y,z)-topEq(x,y,z),bottomEq(x,y,z))))")
                .equals(expression) && hasSamplers(localSamplers, "parentEq", "bottomEq", "topEq")
                && hasVariables(variables, "ceilingHeight", "ceilingVariation", "floorHeight")) {
                NoiseSampler parent = compileSampler(localSamplers.getAsJsonObject("parentEq"));
                NoiseSampler bottom = compileSampler(localSamplers.getAsJsonObject("bottomEq"));
                NoiseSampler top = compileSampler(localSamplers.getAsJsonObject("topEq"));
                double ceiling = numeric(variables.get("ceilingHeight")) + numeric(variables.get("ceilingVariation"));
                double floor = numeric(variables.get("floorHeight"));
                return sampler3d((seed, x, y, z) -> {
                    if (y > ceiling) return parent.noise(seed, x, y, z);
                    if (y < floor) return bottom.noise(seed, x, y, z);
                    return Math
                        .max(parent.noise(seed, x, y, z) - top.noise(seed, x, y, z), bottom.noise(seed, x, y, z));
                });
            }
            if ("(-y+base+(simplex(x,z)+1)/2*height-|erosion(x,y/3,z)|*erosion*"
                .concat("max(0,min((y-erosionYMin)/(erosionYMid-erosionYMin),")
                .concat("(y-erosionYMax)/(erosionYMid-erosionYMax))))*density")
                .equals(expression) && hasSamplers(localSamplers, "erosion")
                && hasVariables(
                    variables,
                    "base",
                    "density",
                    "erosion",
                    "erosionYMin",
                    "erosionYMid",
                    "erosionYMax",
                    "height")) {
                DimensionedSampler globalSimplex = globalSamplers.get("simplex");
                if (globalSimplex == null || globalSimplex.dimensions != 2) return null;
                NoiseSampler simplex = globalSimplex.sampler;
                NoiseSampler erosionSampler = compileSampler(localSamplers.getAsJsonObject("erosion"));
                double base = numeric(variables.get("base"));
                double density = numeric(variables.get("density"));
                double erosionStrength = numeric(variables.get("erosion"));
                double erosionMinimumY = numeric(variables.get("erosionYMin"));
                double erosionMiddleY = numeric(variables.get("erosionYMid"));
                double erosionMaximumY = numeric(variables.get("erosionYMax"));
                double height = numeric(variables.get("height"));
                return sampler3d((seed, x, y, z) -> {
                    double result = -y + base + (simplex.noise(seed, x, z) + 1.0D) / 2.0D * height;
                    double erosionTaper = Math.max(
                        0.0D,
                        Math.min(
                            (y - erosionMinimumY) / (erosionMiddleY - erosionMinimumY),
                            (y - erosionMaximumY) / (erosionMiddleY - erosionMaximumY)));
                    result -= Math.abs(erosionSampler.noise(seed, x, y / 3.0D, z)) * erosionStrength * erosionTaper;
                    return result * density;
                });
            }

            boolean floorSpikes = "-y+floorHeight-if(y>floorHeight,floorStrength*(-(-y+floorHeight))^2,0)"
                .concat("+(noise(x,z)+1)/2*floorVariation+(spikes(x,z)+floorSpikeBias)/2*floorSpikeVar^2")
                .equals(expression);
            boolean simpleFloor = "-y+floorHeight-if(y>floorHeight,floorStrength*(-(-y+floorHeight))^2,0)"
                .concat("+(noise(x,z)+1)/2*floorVariation")
                .equals(expression);
            if ((simpleFloor || floorSpikes) && hasSamplers(localSamplers, "noise")
                && (!floorSpikes || hasSamplers(localSamplers, "spikes"))
                && hasVariables(variables, "floorHeight", "floorStrength", "floorVariation")
                && (!floorSpikes || hasVariables(variables, "floorSpikeBias", "floorSpikeVar"))) {
                NoiseSampler noise = compileSampler(localSamplers.getAsJsonObject("noise"));
                NoiseSampler spikes = floorSpikes ? compileSampler(localSamplers.getAsJsonObject("spikes")) : null;
                double floorHeight = numeric(variables.get("floorHeight"));
                double floorStrength = numeric(variables.get("floorStrength"));
                double floorVariation = numeric(variables.get("floorVariation"));
                double floorSpikeBias = floorSpikes ? numeric(variables.get("floorSpikeBias")) : 0.0D;
                double floorSpikeVariation = floorSpikes ? numeric(variables.get("floorSpikeVar")) : 0.0D;
                return sampler3d((seed, x, y, z) -> {
                    double floorDensity = -y + floorHeight;
                    double result = floorDensity
                        - (y > floorHeight ? floorStrength * Math.pow(-floorDensity, 2.0D) : 0.0D)
                        + (noise.noise(seed, x, z) + 1.0D) / 2.0D * floorVariation;
                    if (spikes != null) {
                        result += (spikes.noise(seed, x, z) + floorSpikeBias) / 2.0D
                            * Math.pow(floorSpikeVariation, 2.0D);
                    }
                    return result;
                });
            }

            boolean ceilingSpikes = "-y+ceilingHeight+(noise(x,z)+1)/2*ceilingVariation"
                .concat("+(spikes(x,z)+1)/2*-ceilingSpikeVar")
                .equals(expression);
            boolean simpleCeiling = "-y+ceilingHeight+(noise(x,z)+1)/2*ceilingVariation".equals(expression);
            if ((simpleCeiling || ceilingSpikes) && hasSamplers(localSamplers, "noise")
                && (!ceilingSpikes || hasSamplers(localSamplers, "spikes"))
                && hasVariables(variables, "ceilingHeight", "ceilingVariation")
                && (!ceilingSpikes || hasVariables(variables, "ceilingSpikeVar"))) {
                NoiseSampler noise = compileSampler(localSamplers.getAsJsonObject("noise"));
                NoiseSampler spikes = ceilingSpikes ? compileSampler(localSamplers.getAsJsonObject("spikes")) : null;
                double ceilingHeight = numeric(variables.get("ceilingHeight"));
                double ceilingVariation = numeric(variables.get("ceilingVariation"));
                double ceilingSpikeVariation = ceilingSpikes ? numeric(variables.get("ceilingSpikeVar")) : 0.0D;
                return sampler3d((seed, x, y, z) -> {
                    double result = -y + ceilingHeight + (noise.noise(seed, x, z) + 1.0D) / 2.0D * ceilingVariation;
                    if (spikes != null) {
                        result += (spikes.noise(seed, x, z) + 1.0D) / 2.0D * -ceilingSpikeVariation;
                    }
                    return result;
                });
            }
            return null;
        }

        private NoiseSampler compileCarverExpression(String expression, JsonObject localSamplers,
            JsonObject variables) {
            if (localSamplers == null || !expression.startsWith(
                "-carvingThreshold+if(y<carvingMinHeight,0,maskSmooth(min(carvingCap,max(maskSmooth(max(")) {
                return null;
            }
            if (!expression.contains("spaghettiAStrength*((-(|simplex3(x,y+0000,z)|+|simplex3(x,y+1000,z)|)/2)+1)")
                || !expression.contains("cheeseStrength*(simplex3(x*cheeseHorizontalFrequency,")
                || !expression.endsWith("carvingMinHeight,carvingMinHeight+carvingMinTaper,y))")
                || !hasSamplers(localSamplers, "simplex2", "simplex3", "megaCaves", "cavePillars")) {
                return null;
            }
            String[] requiredVariables = { "carvingCap", "carvingMaxHeight", "carvingMaxMode", "carvingMinHeight",
                "carvingMinTaper", "carvingThreshold", "cheeseHorizontalFrequency", "cheeseStrength",
                "cheeseSurfaceBreak", "cheeseSurfaceBreakTaper", "cheeseVerticalFrequency", "megaCaveSize",
                "megaCaveStrength", "oceanBase", "oceanDepth", "pillarRadius", "pillarStrength", "spaghettiAStrength",
                "spaghettiBStrength", "spaghettiSurfaceBreak", "spaghettiSurfaceBreakTaper", "terrainBase",
                "terrainScale" };
            for (String variable : requiredVariables) {
                if (!variables.has(variable)) return null;
            }

            NoiseSampler simplex2 = compileSampler(localSamplers.getAsJsonObject("simplex2"));
            NoiseSampler simplex3 = compileSampler(localSamplers.getAsJsonObject("simplex3"));
            NoiseSampler megaCaves = compileSampler(localSamplers.getAsJsonObject("megaCaves"));
            NoiseSampler cavePillars = compileSampler(localSamplers.getAsJsonObject("cavePillars"));
            int carvingMaximumMode = (int) numeric(variables.get("carvingMaxMode"));
            DimensionedSampler maximumSampler = globalSamplers
                .get(carvingMaximumMode == 2 ? "oceanElevation" : "elevation");
            if ((carvingMaximumMode == 1 || carvingMaximumMode == 2)
                && (maximumSampler == null || maximumSampler.dimensions != 2)) {
                return null;
            }

            double carvingCap = numeric(variables.get("carvingCap"));
            double carvingMaximumHeight = numeric(variables.get("carvingMaxHeight"));
            double carvingMinimumHeight = numeric(variables.get("carvingMinHeight"));
            double carvingMinimumTaper = numeric(variables.get("carvingMinTaper"));
            double carvingThreshold = numeric(variables.get("carvingThreshold"));
            double cheeseHorizontalFrequency = numeric(variables.get("cheeseHorizontalFrequency"));
            double cheeseStrength = numeric(variables.get("cheeseStrength"));
            double cheeseSurfaceBreak = numeric(variables.get("cheeseSurfaceBreak"));
            double cheeseSurfaceBreakTaper = numeric(variables.get("cheeseSurfaceBreakTaper"));
            double cheeseVerticalFrequency = numeric(variables.get("cheeseVerticalFrequency"));
            double megaCaveSize = numeric(variables.get("megaCaveSize"));
            double megaCaveStrength = numeric(variables.get("megaCaveStrength"));
            double oceanBase = numeric(variables.get("oceanBase"));
            double oceanDepth = numeric(variables.get("oceanDepth"));
            double pillarRadius = numeric(variables.get("pillarRadius"));
            double pillarStrength = numeric(variables.get("pillarStrength"));
            double spaghettiAStrength = numeric(variables.get("spaghettiAStrength"));
            double spaghettiBStrength = numeric(variables.get("spaghettiBStrength"));
            double spaghettiSurfaceBreak = numeric(variables.get("spaghettiSurfaceBreak"));
            double spaghettiSurfaceBreakTaper = numeric(variables.get("spaghettiSurfaceBreakTaper"));
            double terrainBase = numeric(variables.get("terrainBase"));
            double terrainScale = numeric(variables.get("terrainScale"));
            return sampler3d((seed, x, y, z) -> {
                double carved = 0.0D;
                if (y >= carvingMinimumHeight) {
                    double maximum = carvingMaximumHeight;
                    if (carvingMaximumMode == 1) {
                        maximum = maximumSampler.sampler.noise(seed, x, z) * terrainScale + terrainBase;
                    } else if (carvingMaximumMode == 2) {
                        maximum = maximumSampler.sampler.noise(seed, x, z) * oceanDepth + oceanBase;
                    }

                    double spaghettiA = spaghettiAStrength * (-(Math.abs(simplex3.noise(seed, x, y + 0.0D, z))
                        + Math.abs(simplex3.noise(seed, x, y + 1_000.0D, z))) / 2.0D + 1.0D);
                    double spaghettiB = spaghettiBStrength * (-(Math.abs(simplex3.noise(seed, x, y + 2_000.0D, z))
                        + Math.abs(simplex3.noise(seed, x, y + 3_000.0D, z))) / 2.0D + 1.0D);
                    double spaghetti = OrigenNativeFunctions.maskSmooth(
                        Math.max(spaghettiA, spaghettiB),
                        maximum + spaghettiSurfaceBreakTaper,
                        maximum,
                        y - spaghettiSurfaceBreak);

                    double cheese = cheeseStrength * (simplex3.noise(
                        seed,
                        x * cheeseHorizontalFrequency,
                        (y + simplex2.noise(seed, x, z) * 5.0D) * cheeseVerticalFrequency,
                        z * cheeseHorizontalFrequency) + 1.0D) / 2.0D;
                    cheese += OrigenNativeFunctions
                        .lerp(megaCaves.noise(seed, x, y, z), 1.0D - megaCaveSize, 0.0D, 1.0D, megaCaveStrength);
                    cheese -= OrigenNativeFunctions
                        .lerp(cavePillars.noise(seed, x, z), -1.0D + pillarRadius, 0.0D, -1.0D, 1.0D) * pillarStrength;
                    cheese = OrigenNativeFunctions
                        .maskSmooth(cheese, maximum + cheeseSurfaceBreakTaper, maximum, y - cheeseSurfaceBreak);
                    carved = OrigenNativeFunctions.maskSmooth(
                        Math.min(carvingCap, Math.max(spaghetti, cheese)),
                        carvingMinimumHeight,
                        carvingMinimumHeight + carvingMinimumTaper,
                        y);
                }
                return -carvingThreshold + carved;
            });
        }

        private static boolean hasSamplers(JsonObject samplers, String... names) {
            for (String name : names) {
                if (!samplers.has(name)) return false;
            }
            return true;
        }

        private static boolean hasVariables(JsonObject variables, String... names) {
            for (String name : names) {
                if (!variables.has(name)) return false;
            }
            return true;
        }

        /** Exact pinned shapes from the September 18 population profile; unknown graphs stay interpreted. */
        private NoiseSampler compileProfiledSurfaceExpression(JsonObject node, String expression, JsonObject samplers) {
            if (samplers == null) return null;
            boolean riverMask = "if(-abs(rivers(x,z))>-0.35||biomeInfluence(x,z)>-0.89,0,1)".equals(expression);
            boolean oceanMask = "if(-abs(rivers(x,z))>-0.5||biomeInfluence(x,z)>-0.86,0,1)".equals(expression);
            if ((riverMask || oceanMask) && hasSamplers(samplers, "rivers", "biomeInfluence")
                && samplerDimensions(samplers, 2, "rivers", "biomeInfluence")) {
                NoiseSampler rivers = compileSampler(samplers.getAsJsonObject("rivers"));
                NoiseSampler influence = compileSampler(samplers.getAsJsonObject("biomeInfluence"));
                double riverThreshold = riverMask ? -0.35D : -0.5D;
                double biomeThreshold = riverMask ? -0.89D : -0.86D;
                return sampler2d(
                    (seed, x, z) -> -Math.abs(rivers.noise(seed, x, z)) > riverThreshold
                        || influence.noise(seed, x, z) > biomeThreshold ? 0.0D : 1.0D);
            }
            if ("noise(y+offset(x,z)*10,0)".equals(expression) && hasSamplers(samplers, "noise", "offset")
                && samplerDimensions(samplers, 2, "noise", "offset")) {
                NoiseSampler noise = compileSampler(samplers.getAsJsonObject("noise"));
                NoiseSampler offset = compileSampler(samplers.getAsJsonObject("offset"));
                return sampler3d((seed, x, y, z) -> noise.noise(seed, y + offset.noise(seed, x, z) * 10.0D, 0.0D));
            }
            if ("if(caves(x,y*2,z)<-0.9,terracota_rand_height(y-15,x/10+z/20),"
                .concat("if(wall_variation(x,y,z)>0,-1,terracota_rand_height(y-1,x/8+z/19)))")
                .equals(expression) && hasSamplers(samplers, "caves", "terracota_rand_height", "wall_variation")
                && samplerDimensions(samplers, 3, "caves", "wall_variation")
                && samplerDimensions(samplers, 2, "terracota_rand_height")) {
                NoiseSampler caves = compileSampler(samplers.getAsJsonObject("caves"));
                NoiseSampler terracotta = compileSampler(samplers.getAsJsonObject("terracota_rand_height"));
                NoiseSampler wall = compileSampler(samplers.getAsJsonObject("wall_variation"));
                return sampler3d((seed, x, y, z) -> {
                    if (caves.noise(seed, x, y * 2.0D, z) < -0.9D)
                        return terracotta.noise(seed, y - 15.0D, x / 10.0D + z / 20.0D);
                    return wall.noise(seed, x, y, z) > 0.0D ? -1.0D
                        : terracotta.noise(seed, y - 1.0D, x / 8.0D + z / 19.0D);
                });
            }
            String pillarSuffix = "+basepillarsCenter(x,z)*0.12+pillarWidthRand(y,x/10)*0.09"
                + "+(min(0,base+pillarHeight*(basepillarsCenter(x,z)*0.4+1)-y)/12)^3)";
            boolean landPillars = ("basePillarsExistance(x,z)*(-basepillars(x,z)-0.6" + pillarSuffix)
                .equals(expression);
            boolean oceanPillars = ("basePillarsExistance(x,z)*(-basepillars(x,z)-0.72" + pillarSuffix)
                .equals(expression);
            JsonObject variables = object(node, "variables");
            boolean warpedPillars = "clamp(-y+base+noiseBase(x,z)*height,-2,3)"
                .concat("+clamp(pillars(x+noiseBase(y*3,x/4+z/5)*5,y,z+noiseBase(y*2.5+500,x/6+z/2)*5)*24,-2,3)")
                .equals(expression);
            boolean plainPillars = "clamp(-y+base+noiseBase(x,z)*height,-2,3)+clamp(pillars(x,y,z)*24,-2,3)"
                .equals(expression);
            if ((warpedPillars || plainPillars) && variables != null
                && hasVariables(variables, "base", "height")
                && hasSamplers(samplers, "noiseBase", "pillars")
                && samplerDimensions(samplers, 2, "noiseBase")
                && samplerDimensions(samplers, 3, "pillars")) {
                NoiseSampler noise = compileSampler(samplers.getAsJsonObject("noiseBase"));
                NoiseSampler pillars = compileSampler(samplers.getAsJsonObject("pillars"));
                double base = numeric(variables.get("base"));
                double height = numeric(variables.get("height"));
                return sampler3d((seed, x, y, z) -> {
                    double ground = OrigenNativeFunctions.clamp(-y + base + noise.noise(seed, x, z) * height, -2D, 3D);
                    double pillarX = warpedPillars ? x + noise.noise(seed, y * 3D, x / 4D + z / 5D) * 5D : x;
                    double pillarZ = warpedPillars ? z + noise.noise(seed, y * 2.5D + 500D, x / 6D + z / 2D) * 5D : z;
                    return ground
                        + OrigenNativeFunctions.clamp(pillars.noise(seed, pillarX, y, pillarZ) * 24D, -2D, 3D);
                });
            }
            if ((landPillars || oceanPillars) && variables != null
                && hasVariables(variables, "base", "pillarHeight")
                && hasSamplers(samplers, "basePillarsExistance", "basepillars", "basepillarsCenter", "pillarWidthRand")
                && samplerDimensions(
                    samplers,
                    2,
                    "basePillarsExistance",
                    "basepillars",
                    "basepillarsCenter",
                    "pillarWidthRand")) {
                NoiseSampler existence = compileSampler(samplers.getAsJsonObject("basePillarsExistance"));
                NoiseSampler pillars = compileSampler(samplers.getAsJsonObject("basepillars"));
                NoiseSampler center = compileSampler(samplers.getAsJsonObject("basepillarsCenter"));
                NoiseSampler width = compileSampler(samplers.getAsJsonObject("pillarWidthRand"));
                double base = numeric(variables.get("base"));
                double height = numeric(variables.get("pillarHeight"));
                double bias = landPillars ? 0.6D : 0.72D;
                return sampler3d((seed, x, y, z) -> {
                    double mask = existence.noise(seed, x, z);
                    double pillar = -pillars.noise(seed, x, z) - bias;
                    double centerValue = center.noise(seed, x, z);
                    pillar += centerValue * 0.12D;
                    pillar += width.noise(seed, y, x / 10.0D) * 0.09D;
                    double cap = Math.min(0.0D, base + height * (centerValue * 0.4D + 1.0D) - y) / 12.0D;
                    return mask * (pillar + cap * cap * cap);
                });
            }
            return null;
        }

        private boolean samplerDimensions(JsonObject samplers, int expected, String... names) {
            for (String name : names) if (dimensions(samplers.getAsJsonObject(name)) != expected) return false;
            return true;
        }

        private static NoiseSampler sampler2d(Sampler2d sampler) {
            return new NoiseSampler() {

                @Override
                public double noise(long seed, double x, double z) {
                    return sampler.sample(seed, x, z);
                }

                @Override
                public double noise(long seed, double x, double y, double z) {
                    return sampler.sample(seed, x, z);
                }
            };
        }

        private static NoiseSampler sampler3d(Sampler3d sampler) {
            return new NoiseSampler() {

                @Override
                public double noise(long seed, double x, double z) {
                    return sampler.sample(seed, x, 0.0D, z);
                }

                @Override
                public double noise(long seed, double x, double y, double z) {
                    return sampler.sample(seed, x, y, z);
                }
            };
        }

        /** Collapses expressions such as {@code noise(x,z)} which otherwise pay for a redundant Paralithic frame. */
        private NoiseSampler directSamplerCall(JsonObject node) {
            String expression = compactExpression(node);
            int dimensions;
            String suffix;
            if (expression.endsWith("(x,z)")) {
                dimensions = 2;
                suffix = "(x,z)";
            } else if (expression.endsWith("(x,y,z)")) {
                dimensions = 3;
                suffix = "(x,y,z)";
            } else {
                return null;
            }
            String name = expression.substring(0, expression.length() - suffix.length());
            if (name.isEmpty()) return null;
            for (int index = 0; index < name.length(); index++) {
                char character = name.charAt(index);
                if (!(character == '_' || Character.isLetterOrDigit(character))) return null;
            }

            JsonObject localSamplers = object(node, "samplers");
            NoiseSampler delegate;
            if (localSamplers != null && localSamplers.has(name)) {
                JsonObject samplerNode = localSamplers.getAsJsonObject(name);
                if (dimensions(samplerNode) != dimensions) return null;
                delegate = compileSampler(samplerNode);
            } else {
                DimensionedSampler global = globalSamplers.get(name);
                if (global == null || global.dimensions != dimensions) return null;
                delegate = global.sampler;
            }
            // Keep ExpressionFunction's outer-dimensional adapter. In particular, a 3D expression containing
            // noise(x,z) must continue to call the child's 2D overload and ignore Y; returning the child itself
            // would make a 3D caller invoke its 3D overload and silently alter the graph.
            return dimensions == 2 ? sampler2d((seed, x, z) -> delegate.noise(seed, x, z))
                : sampler3d((seed, x, y, z) -> delegate.noise(seed, x, y, z));
        }

        private static String compactExpression(JsonObject node) {
            return required(node, "expression").getAsString()
                .replace(" ", "")
                .replace("\t", "")
                .replace("\r", "")
                .replace("\n", "");
        }

        private static String canonicalExpression(JsonObject node) {
            return required(node, "expression").getAsString()
                .replaceAll("//[^\\r\\n]*", "")
                .replace(" ", "")
                .replace("\t", "")
                .replace("\r", "")
                .replace("\n", "");
        }

        private NoiseSampler compileExpressionNormalizer(JsonObject node) {
            try {
                return new ExpressionNormalizer(
                    child(node, "sampler"),
                    expressionFunctions(node),
                    required(node, "expression").getAsString(),
                    variables(node));
            } catch (ParseException exception) {
                throw new IllegalStateException("Could not parse Origen normalizer expression", exception);
            }
        }

        private Map<String, Function> expressionFunctions(JsonObject node) {
            Map<String, Function> functions = new HashMap<String, Function>(globalFunctions);
            JsonObject localFunctions = object(node, "functions");
            if (localFunctions != null) {
                functions.putAll(compileFunctionDefinitions(localFunctions));
            }
            Map<String, DimensionedSampler> samplers = new HashMap<String, DimensionedSampler>(globalSamplers);
            JsonObject localSamplers = object(node, "samplers");
            if (localSamplers != null) {
                for (Map.Entry<String, JsonElement> entry : localSamplers.entrySet()) {
                    JsonObject sampler = entry.getValue()
                        .getAsJsonObject();
                    samplers.put(entry.getKey(), new DimensionedSampler(compileSampler(sampler), dimensions(sampler)));
                }
            }
            for (Map.Entry<String, DimensionedSampler> entry : samplers.entrySet()) {
                DimensionedSampler sampler = entry.getValue();
                functions.put(
                    entry.getKey(),
                    sampler.dimensions == 2 ? new NoiseFunction2(sampler.sampler)
                        : new NoiseFunction3(sampler.sampler));
            }
            return functions;
        }

        private NoiseSampler compileDistance(JsonObject node) {
            JsonObject point = object(node, "point");
            return new DistanceSampler(
                DistanceSampler.DistanceFunction.valueOf(string(node, "distance-function", "Euclidean")),
                point == null ? 0.0D : number(point, "x", 0.0D),
                point == null ? 0.0D : number(point, "y", 0.0D),
                point == null ? 0.0D : number(point, "z", 0.0D),
                bool(node, "normalize"),
                number(node, "radius", 100.0D));
        }

        private NoiseSampler compileKernel(JsonObject node) {
            JsonArray rows = required(node, "kernel").getAsJsonArray();
            double factor = number(node, "factor", 1.0D);
            double[][] kernel = new double[rows.size()][];
            for (int x = 0; x < rows.size(); x++) {
                JsonArray row = rows.get(x)
                    .getAsJsonArray();
                kernel[x] = new double[row.size()];
                for (int z = 0; z < row.size(); z++) {
                    kernel[x][z] = numeric(row.get(z)) * factor;
                }
            }
            KernelSampler sampler = new KernelSampler(kernel, child(node, "sampler"));
            sampler.setFrequency(number(node, "frequency", 1.0D));
            return sampler;
        }

        private NoiseFunction configureNoise(NoiseFunction sampler, JsonObject node) {
            sampler.setFrequency(number(node, "frequency", 0.02D));
            sampler.setSalt((long) number(node, "salt", 0.0D));
            return sampler;
        }

        private NoiseSampler child(JsonObject node, String key) {
            return compileSampler(required(node, key).getAsJsonObject());
        }

        private NoiseSampler optionalChild(JsonObject node, String key, NoiseSampler fallback) {
            JsonElement value = node.get(key);
            return value == null || value.isJsonNull() ? fallback : compileSampler(value.getAsJsonObject());
        }

        private Map<String, Function> compileFunctionDefinitions(JsonObject definitions) {
            if (definitions == null || definitions.entrySet()
                .isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, Function> functions = new LinkedHashMap<String, Function>();
            for (Map.Entry<String, JsonElement> entry : definitions.entrySet()) {
                JsonElement globalDefinition = globalFunctionDefinitions.get(entry.getKey());
                Function globalFunction = globalFunctions == null ? null : globalFunctions.get(entry.getKey());
                if (optimizeRuntime && globalFunction != null
                    && entry.getValue()
                        .equals(globalDefinition)) {
                    functions.put(entry.getKey(), globalFunction);
                    continue;
                }
                functions.put(
                    entry.getKey(),
                    compileFunction(
                        entry.getValue()
                            .getAsJsonObject()));
            }
            return functions;
        }

        /** Terra's named Origen helpers are static math and can be emitted as direct bytecode calls. */
        private Map<String, Function> compileGlobalFunctionDefinitions(JsonObject definitions) {
            if (definitions == null || definitions.entrySet()
                .isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, Function> functions = new LinkedHashMap<String, Function>();
            for (Map.Entry<String, JsonElement> entry : definitions.entrySet()) {
                JsonObject definition = entry.getValue()
                    .getAsJsonObject();
                int arguments = required(definition, "arguments").getAsJsonArray()
                    .size();
                Method nativeMethod = OrigenNativeFunctions.find(entry.getKey(), arguments);
                functions.put(
                    entry.getKey(),
                    nativeMethod == null ? compileFunction(definition) : new StaticNativeFunction(nativeMethod));
            }
            return functions;
        }

        private Function compileFunction(JsonObject definition) {
            Function cached = functionCache.get(definition);
            if (cached != null) {
                return cached;
            }
            try {
                Parser parser = new Parser();
                Scope functionScope = new Scope().withParent(new Scope());
                JsonArray arguments = required(definition, "arguments").getAsJsonArray();
                for (JsonElement argument : arguments) {
                    functionScope.addInvocationVariable(argument.getAsString());
                }
                JsonObject nested = object(definition, "functions");
                if (nested != null) {
                    for (Map.Entry<String, JsonElement> entry : nested.entrySet()) {
                        parser.registerFunction(
                            entry.getKey(),
                            compileFunction(
                                entry.getValue()
                                    .getAsJsonObject()));
                    }
                }
                Function function = new UserDefinedFunction(
                    parser.parse(required(definition, "expression").getAsString(), functionScope),
                    arguments.size());
                functionCache.put(definition, function);
                return function;
            } catch (ParseException exception) {
                throw new IllegalStateException("Could not parse Origen user-defined function", exception);
            }
        }

        private Map<String, Double> variables(JsonObject node) {
            JsonObject variables = object(node, "variables");
            if (variables == null) {
                return Collections.emptyMap();
            }
            Map<String, Double> result = new LinkedHashMap<String, Double>();
            for (Map.Entry<String, JsonElement> entry : variables.entrySet()) {
                result.put(entry.getKey(), numeric(entry.getValue()));
            }
            return result;
        }

        private int dimensions(JsonObject node) {
            return integer(node, "dimensions", 2);
        }

        private int integer(JsonObject node, String key, int fallback) {
            JsonElement value = node.get(key);
            return value == null || value.isJsonNull() ? fallback : (int) numeric(value);
        }

        private double requiredNumber(JsonObject node, String key) {
            return numeric(required(node, key));
        }

        private double number(JsonObject node, String key, double fallback) {
            JsonElement value = node.get(key);
            return value == null || value.isJsonNull() ? fallback : numeric(value);
        }

        double numeric(JsonElement value) {
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive()
                .isNumber()) {
                return value.getAsDouble();
            }
            String expression = value.getAsString();
            Double cached = numericExpressionCache.get(expression);
            if (cached != null) {
                return cached.doubleValue();
            }
            try {
                double result = new Parser().eval(expression);
                numericExpressionCache.put(expression, result);
                return result;
            } catch (ParseException exception) {
                throw new IllegalStateException("Could not parse Origen numeric expression " + expression, exception);
            }
        }

        private static String string(JsonObject node, String key, String fallback) {
            JsonElement value = node.get(key);
            return value == null || value.isJsonNull() ? fallback : value.getAsString();
        }

        private static boolean bool(JsonObject node, String key) {
            JsonElement value = node.get(key);
            return value == null || value.isJsonNull() ? false : value.getAsBoolean();
        }

        private static JsonObject object(JsonObject node, String key) {
            JsonElement value = node.get(key);
            return value == null || value.isJsonNull() ? null : value.getAsJsonObject();
        }

        @FunctionalInterface
        private interface Sampler2d {

            double sample(long seed, double x, double z);
        }

        @FunctionalInterface
        private interface Sampler3d {

            double sample(long seed, double x, double y, double z);
        }
    }

    /** Exact runtime shape of Terra's user-defined, stateless Paralithic function. */
    private static final class UserDefinedFunction implements DynamicFunction {

        private final Expression expression;
        private final int arguments;

        private UserDefinedFunction(Expression expression, int arguments) {
            this.expression = expression;
            this.arguments = arguments;
        }

        @Override
        public double eval(double... values) {
            return expression.evaluate(values);
        }

        @Override
        public double eval(Context context, double... values) {
            return expression.evaluate(context, values);
        }

        @Override
        public int getArgNumber() {
            return arguments;
        }

        @Override
        public Statefulness statefulness() {
            return Statefulness.STATELESS;
        }
    }

    private static final class StaticNativeFunction implements NativeMathFunction {

        private final Method method;

        private StaticNativeFunction(Method method) {
            this.method = method;
        }

        @Override
        public Method getMethod() {
            return method;
        }
    }

    private static final class DimensionedSampler {

        private final NoiseSampler sampler;
        private final int dimensions;

        private DimensionedSampler(NoiseSampler sampler, int dimensions) {
            if (dimensions != 2 && dimensions != 3) {
                throw new IllegalStateException("Origen sampler dimensions must be 2 or 3, got " + dimensions);
            }
            this.sampler = sampler;
            this.dimensions = dimensions;
        }
    }

    /** Allows global expression functions to reference global samplers loaded later in the map. */
    private static final class ForwardingSampler implements NoiseSampler {

        private final String name;
        private NoiseSampler delegate;

        private ForwardingSampler(String name) {
            this.name = name;
        }

        private void bind(NoiseSampler delegate) {
            if (this.delegate != null) {
                throw new IllegalStateException("Origen global sampler was bound twice: " + name);
            }
            this.delegate = delegate;
        }

        @Override
        public double noise(long seed, double x, double y) {
            return requiredDelegate().noise(seed, x, y);
        }

        @Override
        public double noise(long seed, double x, double y, double z) {
            return requiredDelegate().noise(seed, x, y, z);
        }

        private NoiseSampler requiredDelegate() {
            if (delegate == null) {
                throw new IllegalStateException("Origen global sampler was evaluated before binding: " + name);
            }
            return delegate;
        }

    }
}
