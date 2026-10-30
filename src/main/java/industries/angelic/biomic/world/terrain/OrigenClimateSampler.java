package industries.angelic.biomic.world.terrain;

import com.dfsek.seismic.algorithms.sampler.DomainWarpedSampler;
import com.dfsek.seismic.algorithms.sampler.TranslateSampler;
import com.dfsek.seismic.algorithms.sampler.noise.DistanceSampler;
import com.dfsek.seismic.algorithms.sampler.noise.cellular.CellularStyleSampler.CellularReturnType;
import com.dfsek.seismic.algorithms.sampler.normalizer.ProbabilityNormalizer;
import com.dfsek.seismic.type.DistanceFunction;
import com.dfsek.seismic.type.sampler.Sampler;

/** Direct Java transcription of Origen's shared continental, elevation, and temperature samplers. */
final class OrigenClimateSampler {

    private static final double CONTINENTAL_SCALE = 0.8D;
    private static final double CONTINENTAL_OFFSET = 0.2D;
    /** Biomic: 1.0 (Origen 0.4, Terra's default pack 1.01), so climate zones hold several biomes, as in vanilla. */
    private static final double TEMPERATURE_SCALE = 1.0D;
    /** Biomic: height cools a cell by one climate band at most (Origen took off up to 1.3, two bands). */
    private static final double MAX_LAPSE = 2.0D / 3.0D;
    private static final double SPOT_SPREAD = 2_000.0D;
    private static final double SPOT_MINIMUM_RADIUS = 50.0D;
    private static final double SPOT_MAXIMUM_RADIUS = 300.0D;
    private static final double SPAWN_ISLAND_INNER_RADIUS = 20.0D;
    private static final double SPAWN_ISLAND_MIDDLE_RADIUS = 125.0D;
    private static final double SPAWN_ISLAND_OUTER_RADIUS = 400.0D;

    private final long worldSeed;
    private final Sampler continentalNoise;
    private final Sampler rawElevationNoise;
    private final Sampler rawTemperatureNoise;
    private final Sampler riverNoise;
    private final Sampler riverErosionStrength;
    private final Sampler spawnIsland;
    private final Sampler spotDistance;
    private final Sampler spotCellValue;

    OrigenClimateSampler(long worldSeed) {
        this.worldSeed = worldSeed;
        continentalNoise = new TranslateSampler(
            new Terra64RidgedFractalSampler(
                0L,
                new Terra64OpenSimplex2Sampler(1.0D / 5_000.0D, 1L),
                0.45D,
                3.0D,
                0.0D,
                4),
            10_000.0D,
            0.0D,
            10_000.0D);
        rawElevationNoise = new TranslateSampler(
            new ProbabilityNormalizer(
                new Terra64RidgedFractalSampler(0L, new Terra64OpenSimplex2Sampler(0.0003D, 0L), 0.4D, 2.2D, 0.0D, 6)),
            10_000.0D,
            0.0D,
            10_000.0D);
        rawTemperatureNoise = new TranslateSampler(
            new Terra64OpenSimplex2SSampler(1.0D / 8_000.0D, 3L),
            10_000.0D,
            0.0D,
            10_000.0D);

        Sampler riverBase = new DomainWarpedSampler(
            new Terra64OpenSimplex2Sampler(0.02D, 0L),
            new Terra64OpenSimplex2Sampler(0.14D, 0L),
            2.0D);
        riverNoise = new TranslateSampler(riverBase, 10_000.0D, 0.0D, 10_000.0D);
        riverErosionStrength = new ProbabilityNormalizer(new Terra64OpenSimplex2Sampler(0.002D, 2_175L));

        Sampler spawnDistance = new ProbabilityNormalizer(
            new DistanceSampler(0L, DistanceFunction.Euclidean, 0.0D, 0.0D, 0.0D, true, SPAWN_ISLAND_OUTER_RADIUS));
        Sampler spawnShape = new TwoDimensionalSampler() {

            @Override
            public double getSample(long seed, double x, double z) {
                return spawnIslandShape(spawnDistance.getSample(seed, x, z));
            }
        };
        Sampler spawnWarp = new Terra64BrownianMotionSampler(
            0L,
            new TranslateSampler(new Terra64OpenSimplex2Sampler(0.005D, 0L), 5_000.0D, 0.0D, -5_000.0D),
            0.5D,
            2.0D,
            0.0D,
            2);
        spawnIsland = new DomainWarpedSampler(spawnShape, spawnWarp, 200.0D);

        double spotJitter = 1.0D - 2.0D * SPOT_MAXIMUM_RADIUS / SPOT_SPREAD;
        Sampler rawSpotDistance = cellular(1.0D / SPOT_SPREAD, 3L, CellularReturnType.Distance, spotJitter);
        Sampler physicalSpotDistance = new TwoDimensionalSampler() {

            @Override
            public double getSample(long seed, double x, double z) {
                return (rawSpotDistance.getSample(seed, x, z) + 1.0D) * SPOT_SPREAD;
            }
        };
        spotDistance = new DomainWarpedSampler(physicalSpotDistance, new Terra64OpenSimplex2Sampler(0.015D, 0L), 7.0D);
        spotCellValue = new ProbabilityNormalizer(
            cellular(1.0D / SPOT_SPREAD, 3L, CellularReturnType.CellValue, spotJitter));
    }

    double continents(double blockX, double blockZ) {
        double continental = -continentalNoise
            .getSample(worldSeed, blockX / CONTINENTAL_SCALE, blockZ / CONTINENTAL_SCALE) + CONTINENTAL_OFFSET;
        double spawn = spawnIsland.getSample(worldSeed, blockX, blockZ);
        double radius = lerp(
            spotCellValue.getSample(worldSeed, blockX, blockZ),
            0.0D,
            SPOT_MINIMUM_RADIUS,
            1.0D,
            SPOT_MAXIMUM_RADIUS);
        double distance = spotDistance.getSample(worldSeed, blockX, blockZ);
        double spot = 0.3D * (1.0D - square(distance / radius));
        return Math.max(Math.max(continental, spawn), spot);
    }

    double rawElevation(double blockX, double blockZ) {
        return cube(rawElevationNoise.getSample(worldSeed, blockX, blockZ));
    }

    double elevation(double blockX, double blockZ) {
        double continental = continents(blockX, blockZ);
        double spawn = spawnIsland.getSample(worldSeed, blockX, blockZ);
        double spawnScale = hermiteRange(spawn, 1.0D, 0.3D, -1.0D, 1.0D);
        double elevated = rawElevation(blockX, blockZ) * spawnScale;
        double emerged = hermiteRange(continental, -0.3D, 0.0D, 0.2D, elevated);
        return emerged * (1.0D - riverTerrainErosion(blockX, blockZ));
    }

    double temperature(double blockX, double blockZ) {
        double rawTemperature = rawTemperature(blockX, blockZ);
        double lapse = Math.min(MAX_LAPSE, linearRange(elevation(blockX, blockZ), 0.2D, 0.0D, 1.0D, 1.3D));
        return rawTemperature - lapse;
    }

    double rawTemperature(double blockX, double blockZ) {
        return rawTemperatureNoise.getSample(worldSeed, blockX / TEMPERATURE_SCALE, blockZ / TEMPERATURE_SCALE);
    }

    Sampler continentsSampler() {
        return new TwoDimensionalSampler() {

            @Override
            public double getSample(long seed, double x, double z) {
                return continents(x, z);
            }
        };
    }

    Sampler temperatureSampler() {
        return new TwoDimensionalSampler() {

            @Override
            public double getSample(long seed, double x, double z) {
                return temperature(x, z);
            }
        };
    }

    private double riverTerrainErosion(double blockX, double blockZ) {
        double frequency = 1.0D / 20.0D;
        double river = riverNoise.getSample(worldSeed, blockX * frequency, blockZ * frequency);
        double riverMask = hermiteRange(square(Math.abs(river)), 0.0D, 0.95D, 0.5D, 0.0D);
        double erosion = riverErosionStrength.getSample(worldSeed, blockX, blockZ);
        return riverMask * hermiteRange(erosion, 0.2D, 0.0D, 0.8D, 1.0D);
    }

    private static double spawnIslandShape(double distance) {
        double inner = SPAWN_ISLAND_INNER_RADIUS / SPAWN_ISLAND_OUTER_RADIUS;
        double middle = SPAWN_ISLAND_MIDDLE_RADIUS / SPAWN_ISLAND_OUTER_RADIUS;
        if (distance < middle) {
            return hermiteRange(distance, inner, 1.0D, middle, 0.0D);
        }
        double progress = clamp01((distance - middle) / (1.0D - middle));
        return -square(progress);
    }

    private static Sampler cellular(double frequency, long salt, CellularReturnType returnType, double jitter) {
        return new Terra64CellularSampler(
            frequency,
            salt,
            Sampler.zero(),
            DistanceFunction.Euclidean,
            returnType,
            jitter);
    }

    private static double linearRange(double input, double inputA, double outputA, double inputB, double outputB) {
        if (inputA < inputB) {
            if (input <= inputA) return outputA;
            if (input >= inputB) return outputB;
        } else {
            if (input >= inputA) return outputA;
            if (input <= inputB) return outputB;
        }
        return outputA * (input - inputB) / (inputA - inputB) + outputB * (input - inputA) / (inputB - inputA);
    }

    private static double hermiteRange(double input, double inputA, double outputA, double inputB, double outputB) {
        if (inputA < inputB) {
            if (input <= inputA) return outputA;
            if (input >= inputB) return outputB;
        } else {
            if (input >= inputA) return outputA;
            if (input <= inputB) return outputB;
        }
        double progress = (input - inputB) / (inputA - inputB);
        double hermite = progress * progress * (3.0D - 2.0D * progress);
        return hermite * (outputA - outputB) + outputB;
    }

    private static double lerp(double input, double inputA, double outputA, double inputB, double outputB) {
        return linearRange(input, inputA, outputA, inputB, outputB);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static double square(double value) {
        return value * value;
    }

    private static double cube(double value) {
        return value * value * value;
    }

    private abstract static class TwoDimensionalSampler implements Sampler {

        @Override
        public double getSample(long seed, double x, double y, double z) {
            return getSample(seed, x, z);
        }
    }
}
