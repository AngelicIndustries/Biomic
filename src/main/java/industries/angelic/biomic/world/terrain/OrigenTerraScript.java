package industries.angelic.biomic.world.terrain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiFunction;

import net.minecraft.world.World;

import com.dfsek.terra.addons.terrascript.parser.Parser;
import com.dfsek.terra.addons.terrascript.parser.lang.Executable;
import com.dfsek.terra.addons.terrascript.parser.lang.ImplementationArguments;
import com.dfsek.terra.addons.terrascript.parser.lang.Returnable;
import com.dfsek.terra.addons.terrascript.parser.lang.Scope;
import com.dfsek.terra.addons.terrascript.parser.lang.functions.Function;
import com.dfsek.terra.addons.terrascript.parser.lang.functions.FunctionBuilder;
import com.dfsek.terra.addons.terrascript.tokenizer.Position;
import com.dfsek.terra.api.util.MathUtil;

/** Exact Terra 6.4.2 TerraScript grammar with a small Forge 1.7 execution adapter. */
final class OrigenTerraScript {

    private static final boolean PROFILE = Boolean.getBoolean("biomic.profileTerraScripts");
    private static final Map<String, ProfileStats> PROFILE_STATS = new ConcurrentHashMap<>();
    private static final ThreadLocal<ArgumentPool> ARGUMENT_POOL = ThreadLocal.withInitial(ArgumentPool::new);

    private final String id;
    private final Executable executable;
    private final NativeScript nativeScript;

    OrigenTerraScript(String id, String source) {
        this(id, source, true);
    }

    OrigenTerraScript(String id, String source, boolean optimize) {
        this.id = id;
        Parser parser = new Parser(source);
        register(parser);
        try {
            executable = parser.parse();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not parse Origen TerraScript " + id, exception);
        }
        nativeScript = optimize ? nativeScript(id, source) : null;
    }

    boolean generate(OrigenStructureEnvironment environment, World world, int originX, int originY, int originZ,
        int authoredOriginY, int offsetX, int offsetY, int offsetZ, int rotationDegrees, Random random,
        int recursions) {
        ArgumentPool pool = ARGUMENT_POOL.get();
        Arguments arguments = pool.acquire(
            environment,
            world,
            world.getSeed(),
            originX,
            originY,
            originZ,
            authoredOriginY,
            offsetX,
            offsetY,
            offsetZ,
            rotationDegrees,
            random,
            recursions);
        try {
            return execute(arguments);
        } finally {
            pool.release(arguments);
        }
    }

    /** Executes into an environment that owns its own block access; no Minecraft world is passed through. */
    boolean generateDetached(OrigenStructureEnvironment environment, long worldSeed, int originX, int originY,
        int originZ, int authoredOriginY, int offsetX, int offsetY, int offsetZ, int rotationDegrees, Random random,
        int recursions) {
        ArgumentPool pool = ARGUMENT_POOL.get();
        Arguments arguments = pool.acquire(
            environment,
            null,
            worldSeed,
            originX,
            originY,
            originZ,
            authoredOriginY,
            offsetX,
            offsetY,
            offsetZ,
            rotationDegrees,
            random,
            recursions);
        try {
            return execute(arguments);
        } finally {
            pool.release(arguments);
        }
    }

    private boolean execute(Arguments arguments) {
        long startedAt = PROFILE ? System.nanoTime() : 0L;
        try {
            // The upstream script otherwise emits a freestanding chain when its 45-block search expires.
            // Preflight before *any* write, including the anchor exactly at the search limit. Keep its
            // original placement order and RNG unchanged for admitted structures.
            if ("chain_to_ceiling".equals(id) && !chainHasCeiling(arguments)) return false;
            return nativeScript == null ? executable.execute(arguments) : nativeScript.generate(arguments);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                "Origen TerraScript " + id
                    + " failed at "
                    + (arguments.originX + arguments.offsetX)
                    + ","
                    + (arguments.originY + arguments.offsetY)
                    + ","
                    + (arguments.originZ + arguments.offsetZ),
                exception);
        } finally {
            if (PROFILE) {
                ProfileStats stats = PROFILE_STATS.computeIfAbsent(id, ignored -> new ProfileStats());
                stats.calls.increment();
                stats.nanos.add(System.nanoTime() - startedAt);
            }
        }
    }

    private static boolean chainHasCeiling(Arguments arguments) {
        int x = arguments.absoluteX(0, 0);
        int z = arguments.absoluteZ(0, 0);
        for (int offset = 0; offset <= 45; offset++) {
            int y = arguments.absoluteY(offset);
            int authoredY = arguments.absoluteAuthoredY(offset);
            if (!"minecraft:air".equals(arguments.environment.getBlockId(arguments.world, x, y, z, authoredY))) {
                return offset > 0
                    && arguments.environment.supportsCeilingAttachment(arguments.world, x, y, z, authoredY);
            }
        }
        return false;
    }

    private static NativeScript nativeScript(String id, String source) {
        if ("boulder".equals(id)) {
            requirePinnedSource(id, source, "e790c487cb0cdbb48ab9c56cd7eceebe5c9f596ee8980826cd8f9e745c0618eb");
            return arguments -> generateWarpedBoulder(arguments, false);
        }
        if ("small_boulder".equals(id)) {
            requirePinnedSource(id, source, "b97c7b05da357a286fd9b55a6c462834e5d89854679b3a63bbf688e8d0974bb4");
            return arguments -> generateWarpedBoulder(arguments, true);
        }
        if ("snow_layers".equals(id)) {
            requirePinnedSource(id, source, "e289c4886f30ce19ef027df8cbb074a9dff178aaf68ec7ace848b16a584e5fc7");
            return OrigenTerraScript::generateSnowLayers;
        }
        if ("stone_wall_boulder".equals(id)) {
            requirePinnedSource(id, source, "a955c7879471440b4a947bfdf30fe3cacd4f613aa3ffb91c1f0fa0d000704188");
            return arguments -> generateWallBoulder(arguments, "minecraft:stone");
        }
        if ("andesite_wall_boulder".equals(id)) {
            requirePinnedSource(id, source, "ae60000c0b4620a3bb77341673a2879321d5c7656399196a10f54ee9be1bcab9");
            return arguments -> generateWallBoulder(arguments, "minecraft:andesite");
        }
        if ("is_leaf".equals(id)) {
            requirePinnedSource(id, source, "b26ad5fcd34c6eb06b7695027a058178c21571056a2077ffd2d420eb3be9ea79");
            return OrigenTerraScript::isLeaf;
        }
        if ("place_vines".equals(id)) {
            requirePinnedSource(id, source, "d8c1b25ae0e2bdc5678c396d9b059671887439f0ea2721494c8dbd5932f75bad");
            return OrigenTerraScript::generateVines;
        }
        return null;
    }

    private static boolean isLeaf(Arguments arguments) {
        String block = arguments.environment.getBlockId(
            arguments.world,
            arguments.absoluteX(0.0D, 0.0D),
            arguments.absoluteY(0.0D),
            arguments.absoluteZ(0.0D, 0.0D),
            arguments.absoluteAuthoredY(0.0D));
        return "minecraft:oak_leaves".equals(block) || "minecraft:birch_leaves".equals(block)
            || "minecraft:spruce_leaves".equals(block)
            || "minecraft:jungle_leaves".equals(block)
            || "minecraft:acacia_leaves".equals(block)
            || "minecraft:dark_oak_leaves".equals(block)
            || "minecraft:mangrove_leaves".equals(block);
    }

    /** Native equivalent of Origen's ubiquitous hanging-vine script, including nested-structure RNG calls. */
    private static boolean generateVines(Arguments arguments) {
        arguments.waterlog = false;
        boolean north = generateNestedStructure(arguments, 0.0D, 0.0D, -1.0D, "is_leaf");
        boolean south = generateNestedStructure(arguments, 0.0D, 0.0D, 1.0D, "is_leaf");
        boolean east = generateNestedStructure(arguments, 1.0D, 0.0D, 0.0D, "is_leaf");
        boolean west = generateNestedStructure(arguments, -1.0D, 0.0D, 0.0D, "is_leaf");
        if (!(north || south || east || west)) return false;

        String vine = "minecraft:vine[north=" + north + ",south=" + south + ",east=" + east + ",west=" + west + "]";
        int length = arguments.random.nextInt(6) + 10;
        for (int i = 0; i < length; i++) {
            int x = arguments.absoluteX(0.0D, 0.0D);
            int y = arguments.absoluteY(-i);
            int z = arguments.absoluteZ(0.0D, 0.0D);
            int authoredY = arguments.absoluteAuthoredY(-i);
            if (!"minecraft:air".equals(arguments.environment.getBlockId(arguments.world, x, y, z, authoredY))) {
                return true;
            }
            arguments.environment
                .placeBlock(arguments.world, x, y, z, authoredY, vine, false, false, arguments.waterlog);
        }
        return true;
    }

    private static boolean generateNestedStructure(Arguments arguments, double relativeX, double relativeY,
        double relativeZ, String id) {
        // Terra's structure function chooses among the single authored NONE rotation and still consumes this roll.
        arguments.random.nextInt(1);
        return arguments.environment.generateStructure(
            id,
            arguments.world,
            arguments.originX,
            arguments.originY,
            arguments.originZ,
            arguments.authoredOriginY,
            arguments.offsetX + arguments.rotatedXRound(relativeX, relativeZ),
            arguments.offsetY + (int) relativeY,
            arguments.offsetZ + arguments.rotatedZRound(relativeX, relativeZ),
            arguments.rotationDegrees,
            arguments.random,
            arguments.recursions + 1);
    }

    /** Primitive equivalent of Origen's hot per-surface snow-depth binary search. */
    private static boolean generateSnowLayers(Arguments arguments) {
        String blockBelow = arguments.environment.getBlockId(
            arguments.world,
            arguments.absoluteX(0.0D, 0.0D),
            arguments.absoluteY(-1.0D),
            arguments.absoluteZ(0.0D, 0.0D),
            arguments.absoluteAuthoredY(-1.0D));
        if (!"minecraft:grass_block[snowy=true]".equals(blockBelow) && !"minecraft:snow_block".equals(blockBelow)) {
            return false;
        }

        int layers = 0;
        if (!isLand(arguments, 0.0D, -4.0D / 8.0D, 0.0D)) {
            if (!isLand(arguments, 0.0D, -6.0D / 8.0D, 0.0D)) {
                if (isLand(arguments, 0.0D, -7.0D / 8.0D, 0.0D)) layers = 1;
            } else if (!isLand(arguments, 0.0D, -5.0D / 8.0D, 0.0D)) {
                layers = 2;
            } else {
                layers = 3;
            }
        } else if (!isLand(arguments, 0.0D, -2.0D / 8.0D, 0.0D)) {
            layers = !isLand(arguments, 0.0D, -3.0D / 8.0D, 0.0D) ? 4 : 5;
        } else {
            layers = !isLand(arguments, 0.0D, -1.0D / 8.0D, 0.0D) ? 6 : 7;
        }
        if (layers > 0) {
            arguments.environment.placeBlock(
                arguments.world,
                arguments.absoluteX(0.0D, 0.0D),
                arguments.absoluteY(0.0D),
                arguments.absoluteZ(0.0D, 0.0D),
                arguments.absoluteAuthoredY(0.0D),
                "minecraft:snow[layers=" + layers + "]",
                true,
                false,
                arguments.waterlog);
        }
        return true;
    }

    /** Primitive compilation of the two wall-texture boulders; each warp is invariant across the inner X/Z loops. */
    private static boolean generateWallBoulder(Arguments arguments, String block) {
        double precision = 100.0D;
        double radius = 1.5D + arguments.random.nextInt((int) precision) / precision;
        double squish = 0.5D + arguments.random.nextInt((int) precision) / precision / 2.0D;
        double radiusSquared = Math.pow(radius, 2.0D);
        double minimumY = -radius / squish;
        double maximumY = radius / squish;
        for (double y = minimumY; y < maximumY; y += 1.0D) {
            double warpX = arguments.environment
                .sampleNamed("simplex", arguments.worldSeed, arguments.originX * 2.0D, 0.0D, y * 2.0D, false);
            double warpZ = arguments.environment
                .sampleNamed("simplex", arguments.worldSeed, arguments.originZ * 2.0D, 0.0D, y * 2.0D, false);
            for (double x = -radius; x < radius; x += 1.0D) {
                double xSquared = Math.pow(x, 2.0D);
                double ySquared = Math.pow(y * squish, 2.0D);
                for (double z = -radius; z < radius; z += 1.0D) {
                    if (xSquared + ySquared + Math.pow(z, 2.0D) < radiusSquared) {
                        arguments.environment.placeBlock(
                            arguments.world,
                            arguments.absoluteX(x + warpX, z + warpZ),
                            arguments.absoluteY(y),
                            arguments.absoluteZ(x + warpX, z + warpZ),
                            arguments.absoluteAuthoredY(y),
                            block,
                            false,
                            false,
                            arguments.waterlog);
                    }
                }
            }
        }
        return true;
    }

    /** Primitive compilation of Origen's two hottest TerraScripts, preserving every authored operation in order. */
    private static boolean generateWarpedBoulder(Arguments arguments, boolean small) {
        double randPrecision = 100.0D;
        double radius = (small ? 1.0D : 2.0D)
            + arguments.random.nextInt((int) randPrecision) / randPrecision * (small ? 2.0D : 6.0D);
        double warp = small ? 2.0D : 5.0D;
        double warpFrequency = small ? 2.5D : 2.0D;
        double randomSquish = arguments.random.nextInt((int) randPrecision) / randPrecision;
        double squish = 1.0D + (small ? randomSquish / 2.0D : randomSquish);
        double radiusSquared = Math.pow(radius, 2.0D);

        double checkY = -radius / squish;
        if (!isLand(arguments, radius, checkY, 0.0D) || !isLand(arguments, -radius, checkY, 0.0D)
            || !isLand(arguments, 0.0D, checkY, radius)
            || !isLand(arguments, 0.0D, checkY, -radius)) {
            return false;
        }

        double minimumZ = -radius - warp;
        double maximumZ = radius + warp;
        int zSamples = loopIterations(minimumZ, maximumZ);
        double[] warpByZ = new double[zSamples];
        double firstY = (-radius - warp) / squish;
        for (double x = -radius - warp; x < radius + warp; x += 1.0D) {
            int sampleIndex = 0;
            for (double z = minimumZ; z < maximumZ; z += 1.0D) {
                /*
                 * Terra 6.4.2 dispatches this four-argument call through the selected sampler's 2D overload. All
                 * three authored X/Y/Z warp calls therefore return the same value, and that value is independent of
                 * the loop's Y coordinate. Sampling it once per X/Z pair removes tens of thousands of pure calls.
                 */
                warpByZ[sampleIndex++] = warp * sampleBoulderWarp(arguments, x, firstY, z, warpFrequency, 1_000.0D);
            }
            for (double y = firstY; y < (radius + warp) / squish; y += 1.0D) {
                sampleIndex = 0;
                for (double z = minimumZ; z < maximumZ; z += 1.0D) {
                    double warpValue = warpByZ[sampleIndex++];
                    double distanceSquared = Math.pow(x + warpValue, 2.0D) + Math.pow((y + warpValue) * squish, 2.0D)
                        + Math.pow(z + warpValue, 2.0D);
                    if (distanceSquared < radiusSquared) {
                        arguments.environment.placeBlock(
                            arguments.world,
                            arguments.absoluteX(x, z),
                            arguments.absoluteY(y - 1.0D),
                            arguments.absoluteZ(x, z),
                            arguments.absoluteAuthoredY(y - 1.0D),
                            "minecraft:stone",
                            !small,
                            false,
                            arguments.waterlog);
                    }
                }
            }
        }
        return true;
    }

    private static int loopIterations(double minimum, double maximum) {
        int iterations = 0;
        for (double value = minimum; value < maximum; value += 1.0D) iterations++;
        return iterations;
    }

    private static boolean isLand(Arguments arguments, double relativeX, double relativeY, double relativeZ) {
        return "LAND".equals(
            arguments.environment.checkDensity(
                arguments.densityX(relativeX),
                arguments.densityY(relativeY),
                arguments.densityZ(relativeZ)));
    }

    private static double sampleBoulderWarp(Arguments arguments, double x, double y, double z, double frequency,
        double ySalt) {
        return arguments.environment.sampleNamed(
            "simplex3",
            arguments.worldSeed,
            frequency * (x + arguments.originX),
            frequency * (y + arguments.authoredOriginY + ySalt),
            frequency * (z + arguments.originZ),
            true);
    }

    private static void requirePinnedSource(String id, String source, String expectedSha256) {
        String actual = sha256(source);
        if (!expectedSha256.equals(actual)) {
            throw new IllegalStateException(
                "Native TerraScript " + id
                    + " no longer matches its pinned source: expected "
                    + expectedSha256
                    + ", got "
                    + actual);
        }
    }

    private static String sha256(String source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(Character.forDigit(value >>> 4 & 0xF, 16));
                hex.append(Character.forDigit(value & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("Every supported JVM provides SHA-256", exception);
        }
    }

    static void resetProfile() {
        if (PROFILE) PROFILE_STATS.clear();
    }

    static String profileReport(int limit) {
        if (!PROFILE || PROFILE_STATS.isEmpty()) return null;
        List<Map.Entry<String, ProfileStats>> entries = new java.util.ArrayList<>(PROFILE_STATS.entrySet());
        entries.sort((left, right) -> Long.compare(right.getValue().nanos.sum(), left.getValue().nanos.sum()));
        StringBuilder report = new StringBuilder("Origen TerraScript profile:");
        long totalNanos = 0L;
        for (ProfileStats stats : PROFILE_STATS.values()) totalNanos += stats.nanos.sum();
        int count = Math.min(Math.max(0, limit), entries.size());
        for (int i = 0; i < count; i++) {
            Map.Entry<String, ProfileStats> entry = entries.get(i);
            long calls = entry.getValue().calls.sum();
            long nanos = entry.getValue().nanos.sum();
            report.append('\n')
                .append("  ")
                .append(entry.getKey())
                .append(": ")
                .append(calls)
                .append(" calls, ")
                .append(String.format(java.util.Locale.ROOT, "%.3f ms", nanos / 1_000_000.0D))
                .append(" (")
                .append(String.format(java.util.Locale.ROOT, "%.1f%%", nanos * 100.0D / totalNanos))
                .append(')');
        }
        return report.toString();
    }

    private static void register(Parser parser) {
        parser.registerFunction("block", blockBuilder());
        parser.registerFunction("debugBlock", blockBuilder());
        parser.registerFunction("structure", structureBuilder());
        parser.registerFunction(
            "randomInt",
            fixed(
                Returnable.ReturnType.NUMBER,
                Returnable.ReturnType.NUMBER,
                (arguments, values) -> arguments.random.nextInt(number(values, 0).intValue())));
        parser.registerFunction(
            "recursions",
            fixed(Returnable.ReturnType.NUMBER, (arguments, values) -> arguments.recursions));
        parser.registerFunction("setMark", setMarkBuilder());
        parser.registerFunction("getMark", getMarkBuilder());
        parser.registerFunction("entity", entityBuilder());
        parser.registerFunction("getBlock", getBlockBuilder());
        parser.registerFunction("check", checkBuilder());
        parser.registerFunction("sampler", samplerBuilder());
        parser.registerFunction(
            "setWaterlog",
            fixed(Returnable.ReturnType.VOID, Returnable.ReturnType.BOOLEAN, (arguments, values) -> {
                arguments.waterlog = bool(values, 0);
                return null;
            }));
        parser
            .registerFunction("originX", fixed(Returnable.ReturnType.NUMBER, (arguments, values) -> arguments.originX));
        parser.registerFunction(
            "originY",
            fixed(Returnable.ReturnType.NUMBER, (arguments, values) -> arguments.authoredOriginY));
        parser
            .registerFunction("originZ", fixed(Returnable.ReturnType.NUMBER, (arguments, values) -> arguments.originZ));
        parser.registerFunction(
            "rotation",
            fixed(Returnable.ReturnType.STRING, (arguments, values) -> rotationName(arguments.rotationDegrees)));
        parser.registerFunction(
            "rotationDegrees",
            fixed(Returnable.ReturnType.NUMBER, (arguments, values) -> arguments.rotationDegrees));
        parser.registerFunction(
            "print",
            fixed(Returnable.ReturnType.VOID, Returnable.ReturnType.STRING, (arguments, values) -> null));

        unaryNumber(parser, "abs", value -> Math.abs(value.doubleValue()));
        unaryNumber(parser, "pow2", value -> Math.pow(value.doubleValue(), 2.0D));
        binaryNumber(parser, "pow", (left, right) -> Math.pow(left.doubleValue(), right.doubleValue()));
        unaryNumber(parser, "sqrt", value -> Math.sqrt(value.doubleValue()));
        unaryNumber(parser, "rsqrt", value -> MathUtil.invSqrt(value.doubleValue()));
        unaryNumber(parser, "floor", value -> Math.floor(value.doubleValue()));
        unaryNumber(parser, "ceil", value -> Math.ceil(value.doubleValue()));
        unaryNumber(parser, "log", value -> Math.log(value.doubleValue()));
        unaryNumber(parser, "round", value -> Math.round(value.doubleValue()));
        unaryNumber(parser, "sin", value -> MathUtil.sin(value.doubleValue()));
        unaryNumber(parser, "cos", value -> MathUtil.cos(value.doubleValue()));
        unaryNumber(parser, "tan", value -> MathUtil.tan(value.doubleValue()));
        unaryNumber(parser, "asin", value -> Math.asin(value.doubleValue()));
        unaryNumber(parser, "acos", value -> Math.acos(value.doubleValue()));
        unaryNumber(parser, "atan", value -> Math.atan(value.doubleValue()));
        binaryNumber(parser, "max", (left, right) -> Math.max(left.doubleValue(), right.doubleValue()));
        binaryNumber(parser, "min", (left, right) -> Math.min(left.doubleValue(), right.doubleValue()));
    }

    private static FunctionBuilder<Function<Object>> blockBuilder() {
        return variable(Returnable.ReturnType.VOID, position -> switch (position) {
            case 0, 1, 2 -> Returnable.ReturnType.NUMBER;
            case 3 -> Returnable.ReturnType.STRING;
            case 4, 5 -> Returnable.ReturnType.BOOLEAN;
            default -> null;
        }, (arguments, values) -> {
            if (values.size() < 4 || values.size() > 6) {
                throw new IllegalArgumentException("block expects four to six arguments");
            }
            double relativeX = number(values, 0).doubleValue();
            double relativeZ = number(values, 2).doubleValue();
            int x = arguments.absoluteX(relativeX, relativeZ);
            int y = arguments.absoluteY(number(values, 1).doubleValue());
            int authoredY = arguments.absoluteAuthoredY(number(values, 1).doubleValue());
            int z = arguments.absoluteZ(relativeX, relativeZ);
            boolean overwrite = values.size() < 5 || bool(values, 4);
            boolean physics = values.size() >= 6 && bool(values, 5);
            arguments.environment.placeBlock(
                arguments.world,
                x,
                y,
                z,
                authoredY,
                string(values, 3),
                overwrite,
                physics,
                arguments.waterlog);
            return null;
        });
    }

    private static FunctionBuilder<Function<Object>> structureBuilder() {
        return variable(
            Returnable.ReturnType.BOOLEAN,
            position -> position <= 2 ? Returnable.ReturnType.NUMBER : Returnable.ReturnType.STRING,
            (arguments, values) -> {
                if (values.size() < 5) {
                    throw new IllegalArgumentException("structure expects coordinates, ID, and at least one rotation");
                }
                double relativeX = number(values, 0).doubleValue();
                double relativeZ = number(values, 2).doubleValue();
                int selectedRotation = rotationDegrees(string(values, 4 + arguments.random.nextInt(values.size() - 4)));
                return arguments.environment.generateStructure(
                    string(values, 3),
                    arguments.world,
                    arguments.originX,
                    arguments.originY,
                    arguments.originZ,
                    arguments.authoredOriginY,
                    arguments.offsetX + arguments.rotatedXRound(relativeX, relativeZ),
                    arguments.offsetY + number(values, 1).intValue(),
                    arguments.offsetZ + arguments.rotatedZRound(relativeX, relativeZ),
                    normalizeRotation(arguments.rotationDegrees + selectedRotation),
                    arguments.random,
                    arguments.recursions + 1);
            });
    }

    private static FunctionBuilder<Function<Object>> getBlockBuilder() {
        return fixed(
            Returnable.ReturnType.STRING,
            new Returnable.ReturnType[] { Returnable.ReturnType.NUMBER, Returnable.ReturnType.NUMBER,
                Returnable.ReturnType.NUMBER },
            (arguments, values) -> {
                double relativeX = number(values, 0).doubleValue();
                double relativeZ = number(values, 2).doubleValue();
                return arguments.environment.getBlockId(
                    arguments.world,
                    arguments.absoluteX(relativeX, relativeZ),
                    arguments.absoluteY(number(values, 1).doubleValue()),
                    arguments.absoluteZ(relativeX, relativeZ),
                    arguments.absoluteAuthoredY(number(values, 1).doubleValue()));
            });
    }

    private static FunctionBuilder<Function<Object>> checkBuilder() {
        return fixed(
            Returnable.ReturnType.STRING,
            new Returnable.ReturnType[] { Returnable.ReturnType.NUMBER, Returnable.ReturnType.NUMBER,
                Returnable.ReturnType.NUMBER },
            (arguments, values) -> {
                double relativeX = number(values, 0).doubleValue();
                double relativeZ = number(values, 2).doubleValue();
                return arguments.environment.checkDensity(
                    arguments.densityX(relativeX),
                    arguments.densityY(number(values, 1).doubleValue()),
                    arguments.densityZ(relativeZ));
            });
    }

    private static FunctionBuilder<Function<Object>> samplerBuilder() {
        return variable(
            Returnable.ReturnType.NUMBER,
            position -> position == 0 ? Returnable.ReturnType.STRING : Returnable.ReturnType.NUMBER,
            (arguments, values) -> {
                if (values.size() == 3) {
                    return arguments.environment.sampleNamed(
                        string(values, 0),
                        arguments.worldSeed,
                        number(values, 1).doubleValue(),
                        0.0D,
                        number(values, 2).doubleValue(),
                        false);
                }
                if (values.size() == 4) {
                    return arguments.environment.sampleNamed(
                        string(values, 0),
                        arguments.worldSeed,
                        number(values, 1).doubleValue(),
                        number(values, 2).doubleValue(),
                        number(values, 3).doubleValue(),
                        true);
                }
                throw new IllegalArgumentException("sampler expects an ID and either two or three coordinates");
            });
    }

    private static FunctionBuilder<Function<Object>> setMarkBuilder() {
        return fixed(
            Returnable.ReturnType.VOID,
            new Returnable.ReturnType[] { Returnable.ReturnType.NUMBER, Returnable.ReturnType.NUMBER,
                Returnable.ReturnType.NUMBER, Returnable.ReturnType.STRING },
            (arguments, values) -> {
                double relativeX = number(values, 0).doubleValue();
                double relativeZ = number(values, 2).doubleValue();
                arguments.marks.put(
                    key(
                        arguments.absoluteXFloor(relativeX, relativeZ),
                        arguments.absoluteY(number(values, 1).doubleValue()),
                        arguments.absoluteZFloor(relativeX, relativeZ)),
                    string(values, 3));
                return null;
            });
    }

    private static FunctionBuilder<Function<Object>> getMarkBuilder() {
        return fixed(
            Returnable.ReturnType.STRING,
            new Returnable.ReturnType[] { Returnable.ReturnType.NUMBER, Returnable.ReturnType.NUMBER,
                Returnable.ReturnType.NUMBER },
            (arguments, values) -> {
                double relativeX = number(values, 0).doubleValue();
                double relativeZ = number(values, 2).doubleValue();
                String mark = arguments.marks.get(
                    key(
                        arguments.absoluteXFloor(relativeX, relativeZ),
                        arguments.absoluteY(number(values, 1).doubleValue()),
                        arguments.absoluteZFloor(relativeX, relativeZ)));
                return mark == null ? "" : mark;
            });
    }

    private static FunctionBuilder<Function<Object>> entityBuilder() {
        return fixed(
            Returnable.ReturnType.VOID,
            new Returnable.ReturnType[] { Returnable.ReturnType.NUMBER, Returnable.ReturnType.NUMBER,
                Returnable.ReturnType.NUMBER, Returnable.ReturnType.STRING },
            (arguments, values) -> {
                double relativeX = number(values, 0).doubleValue();
                double relativeZ = number(values, 2).doubleValue();
                arguments.environment.spawnEntity(
                    arguments.world,
                    arguments.absoluteX(relativeX, relativeZ),
                    arguments.absoluteY(number(values, 1).doubleValue()),
                    arguments.absoluteZ(relativeX, relativeZ),
                    string(values, 3));
                return null;
            });
    }

    private static void unaryNumber(Parser parser, String id, java.util.function.Function<Number, Number> function) {
        parser.registerFunction(
            id,
            fixed(
                Returnable.ReturnType.NUMBER,
                Returnable.ReturnType.NUMBER,
                (arguments, values) -> function.apply(number(values, 0))));
    }

    private static void binaryNumber(Parser parser, String id, BiFunction<Number, Number, Number> function) {
        parser.registerFunction(
            id,
            fixed(
                Returnable.ReturnType.NUMBER,
                new Returnable.ReturnType[] { Returnable.ReturnType.NUMBER, Returnable.ReturnType.NUMBER },
                (arguments, values) -> function.apply(number(values, 0), number(values, 1))));
    }

    private static FunctionBuilder<Function<Object>> fixed(Returnable.ReturnType result, ScriptOperation operation,
        Returnable.ReturnType... arguments) {
        return fixed(result, arguments, operation);
    }

    private static FunctionBuilder<Function<Object>> fixed(Returnable.ReturnType result, Returnable.ReturnType argument,
        ScriptOperation operation) {
        return fixed(result, new Returnable.ReturnType[] { argument }, operation);
    }

    private static FunctionBuilder<Function<Object>> fixed(Returnable.ReturnType result,
        Returnable.ReturnType[] arguments, ScriptOperation operation) {
        return new ScriptFunctionBuilder(result, arguments, null, operation);
    }

    private static FunctionBuilder<Function<Object>> variable(Returnable.ReturnType result, ArgumentType arguments,
        ScriptOperation operation) {
        return new ScriptFunctionBuilder(result, null, arguments, operation);
    }

    private static Number number(List<Object> values, int index) {
        return (Number) values.get(index);
    }

    private static boolean bool(List<Object> values, int index) {
        return (Boolean) values.get(index);
    }

    private static String string(List<Object> values, int index) {
        return (String) values.get(index);
    }

    private static String key(int x, int y, int z) {
        return x + ":" + y + ":" + z;
    }

    private static int rotationDegrees(String rotation) {
        return switch (rotation) {
            case "NONE" -> 0;
            case "CW_90" -> 90;
            case "CW_180" -> 180;
            case "CCW_90" -> 270;
            default -> throw new IllegalArgumentException("Unsupported Origen structure rotation " + rotation);
        };
    }

    private static String rotationName(int degrees) {
        return switch (normalizeRotation(degrees)) {
            case 0 -> "NONE";
            case 90 -> "CW_90";
            case 180 -> "CW_180";
            case 270 -> "CCW_90";
            default -> throw new AssertionError();
        };
    }

    private static int normalizeRotation(int degrees) {
        return Math.floorMod(degrees, 360);
    }

    private interface ScriptOperation {

        Object apply(Arguments arguments, List<Object> values);
    }

    private interface NativeScript {

        boolean generate(Arguments arguments);
    }

    private static final class ProfileStats {

        private final LongAdder calls = new LongAdder();
        private final LongAdder nanos = new LongAdder();
    }

    private interface ArgumentType {

        Returnable.ReturnType get(int position);
    }

    private static final class ScriptFunctionBuilder implements FunctionBuilder<Function<Object>> {

        private final Returnable.ReturnType result;
        private final Returnable.ReturnType[] arguments;
        private final ArgumentType variableArguments;
        private final ScriptOperation operation;

        private ScriptFunctionBuilder(Returnable.ReturnType result, Returnable.ReturnType[] arguments,
            ArgumentType variableArguments, ScriptOperation operation) {
            this.result = result;
            this.arguments = arguments;
            this.variableArguments = variableArguments;
            this.operation = operation;
        }

        @Override
        public Function<Object> build(List<Returnable<?>> argumentList, Position position) {
            return new ScriptFunction(result, argumentList, operation, position);
        }

        @Override
        public int argNumber() {
            return arguments == null ? -1 : arguments.length;
        }

        @Override
        public Returnable.ReturnType getArgument(int position) {
            if (arguments != null) {
                return position >= 0 && position < arguments.length ? arguments[position] : null;
            }
            return variableArguments.get(position);
        }
    }

    private static final class ScriptFunction implements Function<Object> {

        private final Returnable.ReturnType result;
        private final List<Returnable<?>> arguments;
        private final ScriptOperation operation;
        private final Position position;

        private ScriptFunction(Returnable.ReturnType result, List<Returnable<?>> arguments, ScriptOperation operation,
            Position position) {
            this.result = result;
            this.arguments = arguments;
            this.operation = operation;
            this.position = position;
        }

        @Override
        public Returnable.ReturnType returnType() {
            return result;
        }

        @Override
        public Object apply(ImplementationArguments implementationArguments, Scope scope) {
            Arguments scriptArguments = (Arguments) implementationArguments;
            java.util.ArrayList<Object> values = new java.util.ArrayList<Object>(arguments.size());
            for (Returnable<?> argument : arguments) {
                values.add(argument.apply(implementationArguments, scope));
            }
            return operation.apply(scriptArguments, values);
        }

        @Override
        public Position getPosition() {
            return position;
        }
    }

    private static final class ArgumentPool {

        private final ArrayList<Arguments> entries = new ArrayList<>();
        private int depth;

        private Arguments acquire(OrigenStructureEnvironment environment, World world, long worldSeed, int originX,
            int originY, int originZ, int authoredOriginY, int offsetX, int offsetY, int offsetZ, int rotationDegrees,
            Random random, int recursions) {
            if (depth == entries.size()) entries.add(new Arguments());
            Arguments arguments = entries.get(depth++);
            arguments.reset(
                environment,
                world,
                worldSeed,
                originX,
                originY,
                originZ,
                authoredOriginY,
                offsetX,
                offsetY,
                offsetZ,
                rotationDegrees,
                random,
                recursions);
            return arguments;
        }

        private void release(Arguments arguments) {
            if (depth <= 0 || entries.get(depth - 1) != arguments) {
                throw new IllegalStateException("TerraScript argument pool released out of order");
            }
            depth--;
            arguments.release();
        }
    }

    private static final class Arguments implements ImplementationArguments {

        private OrigenStructureEnvironment environment;
        private World world;
        private long worldSeed;
        private int originX;
        private int originY;
        private int originZ;
        private int authoredOriginY;
        private int offsetX;
        private int offsetY;
        private int offsetZ;
        private int rotationDegrees;
        private Random random;
        private int recursions;
        private final Map<String, String> marks = new HashMap<String, String>();
        private boolean waterlog;

        private void reset(OrigenStructureEnvironment environment, World world, long worldSeed, int originX,
            int originY, int originZ, int authoredOriginY, int offsetX, int offsetY, int offsetZ, int rotationDegrees,
            Random random, int recursions) {
            this.environment = environment;
            this.world = world;
            this.worldSeed = worldSeed;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.authoredOriginY = authoredOriginY;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.rotationDegrees = normalizeRotation(rotationDegrees);
            this.random = random;
            this.recursions = recursions;
            marks.clear();
            waterlog = false;
        }

        private void release() {
            environment = null;
            world = null;
            random = null;
            marks.clear();
        }

        private int absoluteX(double relativeX, double relativeZ) {
            return originX + offsetX + rotatedXRound(relativeX, relativeZ);
        }

        private int absoluteXFloor(double relativeX, double relativeZ) {
            return originX + offsetX + (int) Math.floor(rotatedX(relativeX, relativeZ));
        }

        private int absoluteY(double relative) {
            return environment.physicalY(absoluteAuthoredY(relative));
        }

        private int absoluteAuthoredY(double relative) {
            return authoredOriginY + offsetY + (int) Math.floor(relative);
        }

        private int absoluteZ(double relativeX, double relativeZ) {
            return originZ + offsetZ + rotatedZRound(relativeX, relativeZ);
        }

        private int absoluteZFloor(double relativeX, double relativeZ) {
            return originZ + offsetZ + (int) Math.floor(rotatedZ(relativeX, relativeZ));
        }

        private double densityX(double relativeX) {
            return terraCheckHorizontal(originX, offsetX, relativeX);
        }

        private double densityY(double relative) {
            return authoredOriginY + offsetY + relative;
        }

        private double densityZ(double relativeZ) {
            return terraCheckHorizontal(originZ, offsetZ, relativeZ);
        }

        private int rotatedXRound(double relativeX, double relativeZ) {
            return (int) Math.round(rotatedX(relativeX, relativeZ));
        }

        private int rotatedZRound(double relativeX, double relativeZ) {
            return (int) Math.round(rotatedZ(relativeX, relativeZ));
        }

        private double rotatedX(double relativeX, double relativeZ) {
            return switch (rotationDegrees) {
                case 90 -> relativeZ;
                case 180 -> -relativeX;
                case 270 -> -relativeZ;
                default -> relativeX;
            };
        }

        private double rotatedZ(double relativeX, double relativeZ) {
            return switch (rotationDegrees) {
                case 90 -> -relativeX;
                case 180 -> -relativeZ;
                case 270 -> relativeX;
                default -> relativeZ;
            };
        }
    }

    /**
     * Terra 6.4.2's noise-check add-on rounds each horizontal argument before adding the structure origin. Its call
     * to {@code RotationUtil.rotateVector} discards the returned vector, so the pinned runtime also ignores structure
     * rotation here. Several procedural boulders use fractional radii in these checks and depend on both quirks.
     */
    static double terraCheckHorizontal(int origin, int offset, double relative) {
        return origin + offset + Math.round(relative);
    }
}
