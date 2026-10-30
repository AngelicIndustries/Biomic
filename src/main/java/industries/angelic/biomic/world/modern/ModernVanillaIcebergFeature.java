package industries.angelic.biomic.world.modern;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Exact block-volume port of Minecraft 1.21.1's {@code IcebergFeature}. */
final class ModernVanillaIcebergFeature {

    boolean place(String icebergState, World world, ModernRandom random, Pos suppliedOrigin) {
        Pos origin = new Pos(suppliedOrigin.x, world.seaLevel(), suppliedOrigin.z);
        boolean snowy = random.nextDouble() > 0.7D;
        double angle = random.nextDouble() * 2.0D * Math.PI;
        int ellipseRadius = 11 - random.nextInt(5);
        int ellipseWidth = 3 + random.nextInt(3);
        boolean elliptical = random.nextDouble() > 0.7D;
        int height = elliptical ? random.nextInt(6) + 6 : random.nextInt(15) + 3;
        if (!elliptical && random.nextDouble() > 0.9D) {
            height += random.nextInt(19) + 7;
        }
        int underwaterHeight = Math.min(height + random.nextInt(11), 18);
        int topRadius = Math.min(height + random.nextInt(7) - random.nextInt(5), 11);
        int horizontalRadius = elliptical ? ellipseRadius : 11;

        for (int x = -horizontalRadius; x < horizontalRadius; x++) {
            for (int z = -horizontalRadius; z < horizontalRadius; z++) {
                for (int y = 0; y < height; y++) {
                    int radius = elliptical ? heightDependentRadiusEllipse(y, height, topRadius)
                        : heightDependentRadiusRound(random, y, height, topRadius);
                    if (!elliptical && x >= radius) continue;
                    generateIcebergBlock(
                        world,
                        random,
                        origin,
                        height,
                        x,
                        y,
                        z,
                        radius,
                        horizontalRadius,
                        elliptical,
                        ellipseWidth,
                        angle,
                        snowy,
                        icebergState);
                }
            }
        }
        smooth(world, origin, topRadius, height, elliptical, ellipseRadius);

        for (int x = -horizontalRadius; x < horizontalRadius; x++) {
            for (int z = -horizontalRadius; z < horizontalRadius; z++) {
                for (int y = -1; y > -underwaterHeight; y--) {
                    int boundedRadius = elliptical
                        ? ceil(
                            (float) horizontalRadius
                                * (1.0F - (float) Math.pow(y, 2.0D) / ((float) underwaterHeight * 8.0F)))
                        : horizontalRadius;
                    int radius = heightDependentRadiusSteep(random, -y, underwaterHeight, topRadius);
                    if (x >= radius) continue;
                    generateIcebergBlock(
                        world,
                        random,
                        origin,
                        underwaterHeight,
                        x,
                        y,
                        z,
                        radius,
                        boundedRadius,
                        elliptical,
                        ellipseWidth,
                        angle,
                        snowy,
                        icebergState);
                }
            }
        }

        boolean cutOut = elliptical ? random.nextDouble() > 0.1D : random.nextDouble() > 0.7D;
        if (cutOut) {
            generateCutOut(random, world, topRadius, height, origin, elliptical, ellipseRadius, angle, ellipseWidth);
        }
        return true;
    }

    private void generateCutOut(ModernRandom random, World world, int topRadius, int height, Pos origin,
        boolean elliptical, int ellipseRadius, double angle, int ellipseWidth) {
        int signX = random.nextBoolean() ? -1 : 1;
        int signZ = random.nextBoolean() ? -1 : 1;
        int offsetX = random.nextInt(Math.max(topRadius / 2 - 2, 1));
        if (random.nextBoolean()) {
            offsetX = topRadius / 2 + 1 - random.nextInt(Math.max(topRadius - topRadius / 2 - 1, 1));
        }
        int offsetZ = random.nextInt(Math.max(topRadius / 2 - 2, 1));
        if (random.nextBoolean()) {
            offsetZ = topRadius / 2 + 1 - random.nextInt(Math.max(topRadius - topRadius / 2 - 1, 1));
        }
        if (elliptical) {
            offsetX = offsetZ = random.nextInt(Math.max(ellipseRadius - 5, 1));
        }
        Pos offset = new Pos(signX * offsetX, 0, signZ * offsetZ);
        double cutAngle = elliptical ? angle + Math.PI / 2.0D : random.nextDouble() * 2.0D * Math.PI;
        for (int y = 0; y < height - 3; y++) {
            int radius = heightDependentRadiusRound(random, y, height, topRadius);
            carve(radius, y, origin, world, false, cutAngle, offset, ellipseRadius, ellipseWidth);
        }
        for (int y = -1; y > -height + random.nextInt(5); y--) {
            int radius = heightDependentRadiusSteep(random, -y, height, topRadius);
            carve(radius, y, origin, world, true, cutAngle, offset, ellipseRadius, ellipseWidth);
        }
    }

    private void carve(int radius, int y, Pos origin, World world, boolean underwater, double angle, Pos offset,
        int ellipseRadius, int ellipseWidth) {
        int radiusA = radius + 1 + ellipseRadius / 3;
        int radiusB = Math.min(radius - 3, 3) + ellipseWidth / 2 - 1;
        for (int x = -radiusA; x < radiusA; x++) {
            for (int z = -radiusA; z < radiusA; z++) {
                if (signedDistanceEllipse(x, z, offset, radiusA, radiusB, angle) >= 0.0D) {
                    continue;
                }
                Pos position = origin.offset(x, y, z);
                String block = block(world, position);
                if (!isIcebergState(block) && !"minecraft:snow_block".equals(block)) continue;
                set(world, position, underwater ? "minecraft:water[level=0]" : "minecraft:air");
                if (!underwater) removeFloatingSnowLayer(world, position);
            }
        }
    }

    private void removeFloatingSnowLayer(World world, Pos position) {
        Pos above = position.offset(0, 1, 0);
        if ("minecraft:snow".equals(block(world, above))) {
            set(world, above, "minecraft:air");
        }
    }

    private void generateIcebergBlock(World world, ModernRandom random, Pos origin, int height, int x, int y, int z,
        int radius, int boundedRadius, boolean elliptical, int ellipseWidth, double angle, boolean snowy,
        String icebergState) {
        double distance = elliptical
            ? signedDistanceEllipse(x, z, new Pos(0, 0, 0), boundedRadius, getEllipseC(y, height, ellipseWidth), angle)
            : signedDistanceCircle(x, z, new Pos(0, 0, 0), radius, random);
        if (distance >= 0.0D) return;
        Pos position = origin.offset(x, y, z);
        double raggedBoundary = elliptical ? -0.5D : -6.0D - random.nextInt(3);
        if (distance > raggedBoundary && random.nextDouble() > 0.9D) return;
        setIcebergBlock(position, world, random, height - y, height, elliptical, snowy, icebergState);
    }

    private void setIcebergBlock(Pos position, World world, ModernRandom random, int heightRemaining, int height,
        boolean elliptical, boolean snowy, String icebergState) {
        String current = block(world, position);
        if (!isAir(current) && !"minecraft:snow_block".equals(current)
            && !"minecraft:ice".equals(current)
            && !"minecraft:water".equals(current)) return;
        boolean keepSnow = !elliptical || random.nextDouble() > 0.05D;
        int divisor = elliptical ? 3 : 2;
        if (snowy && !"minecraft:water".equals(current)
            && (double) heightRemaining <= random.nextInt(Math.max(1, height / divisor)) + (double) height * 0.6D
            && keepSnow) {
            set(world, position, "minecraft:snow_block");
        } else {
            set(world, position, icebergState);
        }
    }

    private static int getEllipseC(int y, int height, int ellipseWidth) {
        int result = ellipseWidth;
        if (y > 0 && height - y <= 3) result -= 4 - (height - y);
        return result;
    }

    private static double signedDistanceCircle(int x, int z, Pos offset, int radius, ModernRandom random) {
        float noise = 10.0F * clamp(random.nextFloat(), 0.2F, 0.8F) / (float) radius;
        return (double) noise + Math.pow(x - offset.x, 2.0D) + Math.pow(z - offset.z, 2.0D) - Math.pow(radius, 2.0D);
    }

    private static double signedDistanceEllipse(int x, int z, Pos offset, int radiusA, int radiusB, double angle) {
        return Math.pow(
            ((double) (x - offset.x) * Math.cos(angle) - (double) (z - offset.z) * Math.sin(angle)) / (double) radiusA,
            2.0D)
            + Math.pow(
                ((double) (x - offset.x) * Math.sin(angle) + (double) (z - offset.z) * Math.cos(angle))
                    / (double) radiusB,
                2.0D)
            - 1.0D;
    }

    private static int heightDependentRadiusRound(ModernRandom random, int y, int height, int radius) {
        float curve = 3.5F - random.nextFloat();
        float result = (1.0F - (float) Math.pow(y, 2.0D) / ((float) height * curve)) * (float) radius;
        if (height > 15 + random.nextInt(5)) {
            int adjustedY = y < 3 + random.nextInt(6) ? y / 2 : y;
            result = (1.0F - (float) adjustedY / ((float) height * curve * 0.4F)) * (float) radius;
        }
        return ceil(result / 2.0F);
    }

    private static int heightDependentRadiusEllipse(int y, int height, int radius) {
        float result = (1.0F - (float) Math.pow(y, 2.0D) / (float) height) * (float) radius;
        return ceil(result / 2.0F);
    }

    private static int heightDependentRadiusSteep(ModernRandom random, int y, int height, int radius) {
        float curve = 1.0F + random.nextFloat() / 2.0F;
        float result = (1.0F - (float) y / ((float) height * curve)) * (float) radius;
        return ceil(result / 2.0F);
    }

    private void smooth(World world, Pos origin, int topRadius, int height, boolean elliptical, int ellipseRadius) {
        int radius = elliptical ? ellipseRadius : topRadius / 2;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 0; y <= height; y++) {
                    Pos position = origin.offset(x, y, z);
                    String current = block(world, position);
                    if (!isIcebergState(current) && !"minecraft:snow".equals(current)) continue;
                    if (isAir(block(world, position.offset(0, -1, 0)))) {
                        set(world, position, "minecraft:air");
                        set(world, position.offset(0, 1, 0), "minecraft:air");
                        continue;
                    }
                    if (!isIcebergState(current)) continue;
                    int exposedSides = 0;
                    if (!isIcebergState(block(world, position.offset(-1, 0, 0)))) exposedSides++;
                    if (!isIcebergState(block(world, position.offset(1, 0, 0)))) exposedSides++;
                    if (!isIcebergState(block(world, position.offset(0, 0, -1)))) exposedSides++;
                    if (!isIcebergState(block(world, position.offset(0, 0, 1)))) exposedSides++;
                    if (exposedSides >= 3) set(world, position, "minecraft:air");
                }
            }
        }
    }

    private static boolean isIcebergState(String block) {
        return "minecraft:packed_ice".equals(block) || "minecraft:snow_block".equals(block)
            || "minecraft:blue_ice".equals(block);
    }

    private static boolean isAir(String block) {
        return "minecraft:air".equals(block) || "minecraft:cave_air".equals(block)
            || "minecraft:void_air".equals(block);
    }

    private static String block(World world, Pos position) {
        return ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
    }

    private static void set(World world, Pos position, String state) {
        world.setBlock(position.x, position.y, position.z, state);
    }

    private static int ceil(float value) {
        return (int) Math.ceil(value);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
