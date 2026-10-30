package industries.angelic.biomic.world.modern;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Direct port of pinned RU's iceberg-derived SeaRockFeature. */
final class ModernRuSeaRockFeature {

    boolean place(JsonObject config, World world, ModernRandom random, Pos origin) {
        if ("minecraft:packed_ice".equals(block(world, origin.offset(0, -1, 0)))) return false;
        if (random.nextInt(2) == 0) return false;
        boolean alternate = random.nextDouble() > 0.7D;
        String primary = ModernDecorationEngine.state(config.getAsJsonObject("state"));
        String alt = ModernDecorationEngine.state(config.getAsJsonObject("alt_state"));
        double angle = random.nextDouble() * 2.0D * Math.PI;
        int baseRadius = 11 - random.nextInt(5);
        int ellipseWidth = 3 + random.nextInt(3);
        boolean ellipse = random.nextDouble() > 0.7D;
        int upperHeight = ellipse ? random.nextInt(6) + 6 : random.nextInt(15) + 3;
        if (!ellipse && random.nextDouble() > 0.9D) {
            upperHeight += random.nextInt(19) + 7;
        }
        int lowerHeight = Math.min(upperHeight + random.nextInt(11), 18);
        int radius = Math.min(upperHeight + random.nextInt(7) - random.nextInt(5), 11);
        int iterationRadius = ellipse ? baseRadius : 11;

        for (int x = -iterationRadius; x < iterationRadius; x++) {
            for (int z = -iterationRadius; z < iterationRadius; z++) {
                for (int y = 0; y < upperHeight; y++) {
                    int localRadius = ellipse ? heightDependentRadiusEllipse(y, upperHeight, radius)
                        : heightDependentRadiusRound(random, y, upperHeight, radius);
                    if (ellipse || x < localRadius) {
                        generateBlock(
                            world,
                            random,
                            origin,
                            upperHeight,
                            x,
                            y,
                            z,
                            localRadius,
                            iterationRadius,
                            ellipse,
                            ellipseWidth,
                            angle,
                            alternate,
                            primary,
                            alt);
                    }
                }
            }
        }
        smooth(world, origin, radius, upperHeight, ellipse, baseRadius);
        for (int x = -iterationRadius; x < iterationRadius; x++) {
            for (int z = -iterationRadius; z < iterationRadius; z++) {
                for (int y = -1; y > -lowerHeight; y--) {
                    int steepRadius = ellipse
                        ? ceil(iterationRadius * (1.0F - (float) Math.pow(y, 2.0D) / (lowerHeight * 8.0F)))
                        : iterationRadius;
                    int localRadius = heightDependentRadiusSteep(random, -y, lowerHeight, radius);
                    if (x < localRadius) {
                        generateBlock(
                            world,
                            random,
                            origin,
                            lowerHeight,
                            x,
                            y,
                            z,
                            localRadius,
                            steepRadius,
                            ellipse,
                            ellipseWidth,
                            angle,
                            alternate,
                            primary,
                            alt);
                    }
                }
            }
        }
        random.nextDouble(); // pinned source computes an unused final decoration flag
        return true;
    }

    private void generateBlock(World world, ModernRandom random, Pos origin, int height, int x, int y, int z,
        int radius, int secondRadius, boolean ellipse, int ellipseWidth, double angle, boolean alternate,
        String primary, String alt) {
        double distance = ellipse ? signedDistanceEllipse(x, z, secondRadius, ellipseC(y, height, ellipseWidth), angle)
            : signedDistanceCircle(x, z, radius, random);
        if (distance >= 0.0D) return;
        Pos position = origin.offset(x, y, z);
        double edge = ellipse ? -0.5D : -6 - random.nextInt(3);
        if (distance > edge && random.nextDouble() > 0.9D) return;
        setRockBlock(world, random, position, height - y, height, ellipse, alternate, primary, alt);
    }

    private void setRockBlock(World world, ModernRandom random, Pos position, int remainingHeight, int height,
        boolean ellipse, boolean alternate, String primary, String alt) {
        String current = block(world, position);
        if (!"minecraft:air".equals(current) && !"minecraft:snow_block".equals(current)
            && !"minecraft:ice".equals(current)
            && !isWater(world, position)) return;
        boolean allowed = !ellipse || random.nextDouble() > 0.05D;
        int divisor = ellipse ? 3 : 2;
        if (alternate && remainingHeight <= random.nextInt(Math.max(1, height / divisor)) + height * 0.6D && allowed) {
            world.setBlock(position.x, position.y, position.z, alt);
        } else {
            if (random.nextInt(4) == 0) {
                world.setBlock(position.x, position.y, position.z, alt);
            }
            // This intentional overwrite is present in pinned RU.
            world.setBlock(position.x, position.y, position.z, primary);
        }
    }

    private static int ellipseC(int y, int height, int width) {
        if (y > 0 && height - y <= 3) return width - (4 - (height - y));
        return width;
    }

    private static double signedDistanceCircle(int x, int z, int radius, ModernRandom random) {
        float value = random.nextFloat();
        value = Math.max(0.2F, Math.min(0.8F, value));
        float fuzz = 10.0F * value / radius;
        return fuzz + Math.pow(x, 2.0D) + Math.pow(z, 2.0D) - Math.pow(radius, 2.0D);
    }

    private static double signedDistanceEllipse(int x, int z, int radiusX, int radiusZ, double angle) {
        return Math.pow((x * Math.cos(angle) - z * Math.sin(angle)) / radiusX, 2.0D)
            + Math.pow((x * Math.sin(angle) + z * Math.cos(angle)) / radiusZ, 2.0D)
            - 1.0D;
    }

    private static int heightDependentRadiusRound(ModernRandom random, int y, int height, int radius) {
        float scale = 3.5F - random.nextFloat();
        float value = (1.0F - (float) Math.pow(y, 2.0D) / (height * scale)) * radius;
        if (height > 15 + random.nextInt(5)) {
            int adjustedY = y < 3 + random.nextInt(6) ? y / 2 : y;
            value = (1.0F - adjustedY / (height * scale * 0.4F)) * radius;
        }
        return ceil(value / 2.0F);
    }

    private static int heightDependentRadiusEllipse(int y, int height, int radius) {
        float value = (1.0F - (float) Math.pow(y, 2.0D) / height) * radius;
        return ceil(value / 2.0F);
    }

    private static int heightDependentRadiusSteep(ModernRandom random, int y, int height, int radius) {
        float scale = 1.0F + random.nextFloat() / 2.0F;
        float value = (1.0F - (float) y / (height * scale)) * radius;
        return ceil(value / 2.0F);
    }

    private void smooth(World world, Pos origin, int radius, int height, boolean ellipse, int baseRadius) {
        int range = ellipse ? baseRadius : radius / 2;
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = 0; y <= height; y++) {
                    Pos position = origin.offset(x, y, z);
                    String state = block(world, position);
                    if (!isRock(state) && !"minecraft:snow".equals(state)) continue;
                    if (belowIsAirOrWater(world, position)) {
                        replaceForSeaLevel(world, position);
                        replaceForSeaLevel(world, position.offset(0, 1, 0));
                    } else if (isRock(state)) {
                        int exposed = 0;
                        if (!isRock(block(world, position.offset(-1, 0, 0)))) exposed++;
                        if (!isRock(block(world, position.offset(1, 0, 0)))) exposed++;
                        if (!isRock(block(world, position.offset(0, 0, -1)))) exposed++;
                        if (!isRock(block(world, position.offset(0, 0, 1)))) exposed++;
                        if (exposed >= 3) replaceForSeaLevel(world, position);
                    }
                }
            }
        }
    }

    /** Air above the sea, water in it; the sea's level is the world's (191 at Hohen Angst's 512, not 63). */
    private static void replaceForSeaLevel(World world, Pos position) {
        String replacement = position.y >= world.seaLevel() ? "minecraft:air" : "minecraft:water[level=0]";
        if (!replacement.equals(world.blockState(position.x, position.y, position.z))) {
            world.setBlock(position.x, position.y, position.z, replacement);
        }
    }

    private static boolean belowIsAirOrWater(World world, Pos position) {
        Pos below = position.offset(0, -1, 0);
        return "minecraft:air".equals(block(world, below)) || isWater(world, below);
    }

    private static boolean isRock(String state) {
        return "minecraft:stone".equals(state) || "regions_unexplored:mossy_stone".equals(state)
            || "minecraft:packed_ice".equals(state)
            || "minecraft:moss_block".equals(state);
    }

    private static boolean isWater(World world, Pos position) {
        return "minecraft:water".equals(block(world, position)) || "minecraft:water"
            .equals(ModernDecorationEngine.blockName(world.fluidState(position.x, position.y, position.z)));
    }

    private static String block(World world, Pos position) {
        return ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
    }

    private static int ceil(float value) {
        return (int) Math.ceil(value);
    }
}
