package industries.angelic.biomic.world.terrain;

import com.dfsek.seismic.algorithms.sampler.noise.cellular.CellularStyleSampler;
import com.dfsek.seismic.type.DistanceFunction;
import com.dfsek.seismic.type.sampler.Sampler;

/**
 * Terra 6.4.2-compatible cellular sampler.
 *
 * <p>
 * Seismic's optimized successor uses fused arithmetic while finding the nearest cell. That is normally harmless,
 * but a cell-boundary rounding change selects a different biome over an entire pipeline cell. Origen was authored
 * against Terra's unfused FastNoiseLite loop, so biome routing needs that exact arithmetic.
 * </p>
 */
final class Terra64CellularSampler extends CellularStyleSampler {

    Terra64CellularSampler(double frequency, long salt, Sampler lookup, DistanceFunction distanceFunction,
        CellularReturnType returnType, double jitter) {
        super(frequency, salt, lookup, distanceFunction, returnType, jitter, true);
    }

    @Override
    @SuppressWarnings("NumericOverflow")
    public double getNoiseRaw(long saltedSeed, double x, double y) {
        int seed = (int) saltedSeed;
        int roundedX = (int) Math.round(x);
        int roundedY = (int) Math.round(y);
        double distance0 = Double.MAX_VALUE;
        double distance1 = Double.MAX_VALUE;
        double distance2 = Double.MAX_VALUE;
        int closestHash = 0;
        double centerX = x;
        double centerY = y;
        int primedX = (roundedX - 1) * PRIME_X;
        int primedYBase = (roundedY - 1) * PRIME_Y;

        for (int cellX = roundedX - 1; cellX <= roundedX + 1; cellX++) {
            int primedY = primedYBase;
            for (int cellY = roundedY - 1; cellY <= roundedY + 1; cellY++) {
                int hash = hash(seed, primedX, primedY);
                int vectorIndex = hash & 255 << 1;
                double vectorX = cellX - x + RAND_VECS_2D[vectorIndex] * twoDCellularJitter;
                double vectorY = cellY - y + RAND_VECS_2D[vectorIndex | 1] * twoDCellularJitter;
                double distance = distance(vectorX, vectorY);

                distance1 = Math.max(Math.min(distance1, distance), distance0);
                if (distance < distance0) {
                    distance0 = distance;
                    closestHash = hash;
                    centerX = (cellX + RAND_VECS_2D[vectorIndex] * twoDCellularJitter) * invFrequency;
                    centerY = (cellY + RAND_VECS_2D[vectorIndex | 1] * twoDCellularJitter) * invFrequency;
                } else if (distance < distance1) {
                    distance2 = distance1;
                    distance1 = distance;
                } else if (distance < distance2) {
                    distance2 = distance;
                }
                primedY += PRIME_Y;
            }
            primedX += PRIME_X;
        }
        return finish(
            saltedSeed,
            x,
            y,
            0.0D,
            centerX,
            centerY,
            0.0D,
            distance0,
            distance1,
            distance2,
            closestHash,
            false);
    }

    @Override
    @SuppressWarnings("NumericOverflow")
    public double getNoiseRaw(long saltedSeed, double x, double y, double z) {
        int seed = (int) saltedSeed;
        int roundedX = (int) Math.round(x);
        int roundedY = (int) Math.round(y);
        int roundedZ = (int) Math.round(z);
        double distance0 = Double.MAX_VALUE;
        double distance1 = Double.MAX_VALUE;
        double distance2 = Double.MAX_VALUE;
        int closestHash = 0;
        double centerX = x;
        double centerY = y;
        double centerZ = z;
        int primedX = (roundedX - 1) * PRIME_X;
        int primedYBase = (roundedY - 1) * PRIME_Y;
        int primedZBase = (roundedZ - 1) * PRIME_Z;

        for (int cellX = roundedX - 1; cellX <= roundedX + 1; cellX++) {
            int primedY = primedYBase;
            for (int cellY = roundedY - 1; cellY <= roundedY + 1; cellY++) {
                int primedZ = primedZBase;
                for (int cellZ = roundedZ - 1; cellZ <= roundedZ + 1; cellZ++) {
                    int hash = hash(seed, primedX, primedY, primedZ);
                    int vectorIndex = hash & 255 << 2;
                    double vectorX = cellX - x + RAND_VECS_3D[vectorIndex] * threeDCellularJitter;
                    double vectorY = cellY - y + RAND_VECS_3D[vectorIndex | 1] * threeDCellularJitter;
                    double vectorZ = cellZ - z + RAND_VECS_3D[vectorIndex | 2] * threeDCellularJitter;
                    double distance = distance(vectorX, vectorY, vectorZ);

                    // Terra 6.4.2 updates this bound here only for Hybrid distance in its 3D loop.
                    if (distanceFunction == DistanceFunction.Hybrid) {
                        distance1 = Math.max(Math.min(distance1, distance), distance0);
                    }
                    if (distance < distance0) {
                        distance0 = distance;
                        closestHash = hash;
                        centerX = (cellX + RAND_VECS_3D[vectorIndex] * threeDCellularJitter) * invFrequency;
                        centerY = (cellY + RAND_VECS_3D[vectorIndex | 1] * threeDCellularJitter) * invFrequency;
                        centerZ = (cellZ + RAND_VECS_3D[vectorIndex | 2] * threeDCellularJitter) * invFrequency;
                    } else if (distance < distance1) {
                        distance2 = distance1;
                        distance1 = distance;
                    } else if (distance < distance2) {
                        distance2 = distance;
                    }
                    primedZ += PRIME_Z;
                }
                primedY += PRIME_Y;
            }
            primedX += PRIME_X;
        }
        return finish(
            saltedSeed,
            x,
            y,
            z,
            centerX,
            centerY,
            centerZ,
            distance0,
            distance1,
            distance2,
            closestHash,
            true);
    }

    private double finish(long saltedSeed, double x, double y, double z, double centerX, double centerY, double centerZ,
        double distance0, double distance1, double distance2, int closestHash, boolean threeDimensional) {
        if (distanceFunction == DistanceFunction.Euclidean && returnType != CellularReturnType.CellValue) {
            distance0 = Math.sqrt(distance0);
            distance1 = Math.sqrt(distance1);
        }
        return switch (returnType) {
            case CellValue -> closestHash * (1.0D / 2_147_483_648.0D);
            case Distance -> distance0 - 1.0D;
            case Distance2 -> distance1 - 1.0D;
            case Distance2Add -> (distance1 + distance0) * 0.5D - 1.0D;
            case Distance2Sub -> distance1 - distance0 - 1.0D;
            case Distance2Mul -> distance1 * distance0 * 0.5D - 1.0D;
            case Distance2Div -> distance0 / distance1 - 1.0D;
            case NoiseLookup -> threeDimensional ? noiseLookup.getSample(saltedSeed, centerX, centerY, centerZ)
                : noiseLookup.getSample(saltedSeed, centerX, centerY);
            case LocalNoiseLookup -> threeDimensional
                ? noiseLookup.getSample(
                    saltedSeed,
                    x * invFrequency - centerX,
                    y * invFrequency - centerY,
                    z * invFrequency - centerZ)
                : noiseLookup.getSample(saltedSeed, x * invFrequency - centerX, y * invFrequency - centerY);
            case Distance3 -> distance2 - 1.0D;
            case Distance3Add -> (distance2 + distance0) * 0.5D - 1.0D;
            case Distance3Sub -> distance2 - distance0 - 1.0D;
            case Distance3Mul -> distance2 * distance0 - 1.0D;
            case Distance3Div -> distance0 / distance2 - 1.0D;
            case Angle -> Math.atan2(y * invFrequency - centerY, x * invFrequency - centerX);
        };
    }

    private double distance(double x, double y) {
        return switch (distanceFunction) {
            case Manhattan -> Math.abs(x) + Math.abs(y);
            case Hybrid -> Math.abs(x) + Math.abs(y) + x * x + y * y;
            default -> x * x + y * y;
        };
    }

    private double distance(double x, double y, double z) {
        return switch (distanceFunction) {
            case Manhattan -> Math.abs(x) + Math.abs(y) + Math.abs(z);
            case Hybrid -> Math.abs(x) + Math.abs(y) + Math.abs(z) + x * x + y * y + z * z;
            default -> x * x + y * y + z * z;
        };
    }

    private static int hash(int seed, int primedX, int primedY) {
        return (seed ^ primedX ^ primedY) * 0x27D4_EB2D;
    }

    private static int hash(int seed, int primedX, int primedY, int primedZ) {
        return (seed ^ primedX ^ primedY ^ primedZ) * 0x27D4_EB2D;
    }
}
