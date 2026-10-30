package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.biome.placement.SurfaceLandform;
import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.WorldgenPhase;

/** Shared slow fields that keep landmasses, climates, relief and surface-biome regions coherent. */
public final class MacroTerrainSampler {

    private static final long CONTINENT_SALT = 0xA4093822299F31D0L;
    private static final long RELIEF_SALT = 0x082EFA98EC4E6C89L;
    private static final long EROSION_SALT = 0xC6EF372FE94F82BEL;
    private static final long RIDGE_SALT = 0x9B05688C2B3E6C1FL;
    private static final long RIDGE_DETAIL_SALT = 0x1F83D9ABFB41BD6BL;
    private static final long ARCHIPELAGO_SALT = 0x452821E638D01377L;
    private static final long DRAINAGE_SALT = 0xD1310BA698DFB5ACL;
    private static final long DRAINAGE_DETAIL_SALT = 0x2FFD72DBD01ADFB7L;
    private static final long ISLAND_SHAPE_SALT = 0xB8E1AFED6A267E96L;
    private static final long ISLAND_SHAPE_B_SALT = 0x5BE0CD19137E2179L;
    private static final long ISLAND_EDGE_SALT = 0x24A19947B3916CF7L;
    private static final long WARP_X_SALT = 0x3F84D5B5B5470917L;
    private static final long WARP_Z_SALT = 0x9216D5D98979FB1BL;

    private static final double ISLAND_CHAIN_THRESHOLD = 0.30D;
    private static final double OCEAN_THRESHOLD = -0.02D;
    private static final double DEEP_OCEAN_THRESHOLD = -0.42D;
    private static final double SHALLOW_OCEAN_THRESHOLD = -0.18D;
    private static final double PEAK_THRESHOLD = 0.66D;
    private static final double SLOPE_THRESHOLD = 0.18D;
    private static final double COASTAL_LOWLAND_THRESHOLD = 0.72D;
    private static final double FLAT_RELIEF_THRESHOLD = 0.58D;
    private static final int SAMPLE_CACHE_SIZE = 65_536;

    private final long seed;
    private final OrigenProvinceSampler origenProvinceSampler;
    private final LongSampleCache sampleCache = new LongSampleCache(SAMPLE_CACHE_SIZE);

    public MacroTerrainSampler(WorldgenContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Worldgen context cannot be null");
        }
        this.seed = context.seedForPhase(WorldgenPhase.TERRAIN);
        this.origenProvinceSampler = new OrigenProvinceSampler(context);
    }

    public MacroTerrainSample sample(int blockX, int blockZ) {
        long key = coordinateKey(blockX, blockZ);
        MacroTerrainSample cached = sampleCache.get(key);
        if (cached != null) {
            return cached;
        }
        MacroTerrainSample sampled = sampleUncached(blockX, blockZ);
        return sampleCache.putIfAbsent(key, sampled);
    }

    private MacroTerrainSample sampleUncached(int blockX, int blockZ) {
        double warpX = ValueNoise2D.fractal(seed ^ WARP_X_SALT, blockX, blockZ, 896.0D, 2, 0.5D) * 144.0D;
        double warpZ = ValueNoise2D.fractal(seed ^ WARP_Z_SALT, blockX, blockZ, 896.0D, 2, 0.5D) * 144.0D;
        double warpedX = blockX + warpX;
        double warpedZ = blockZ + warpZ;

        double continentalness = ValueNoise2D.fractal(seed ^ CONTINENT_SALT, warpedX, warpedZ, 1_152.0D, 4, 0.5D);
        double relief = toUnit(ValueNoise2D.fractal(seed ^ RELIEF_SALT, warpedX, warpedZ, 448.0D, 3, 0.55D));
        double erosion = toUnit(ValueNoise2D.fractal(seed ^ EROSION_SALT, warpedX, warpedZ, 672.0D, 3, 0.52D));
        double ridgeRaw = ValueNoise2D.fractal(seed ^ RIDGE_SALT, warpedX, warpedZ, 608.0D, 3, 0.54D);
        double ridgeDetail = ValueNoise2D.fractal(seed ^ RIDGE_DETAIL_SALT, warpedX, warpedZ, 176.0D, 2, 0.5D);
        double ridgeStrength = clamp01(1.0D - Math.abs(ridgeRaw + ridgeDetail * 0.18D));
        double archipelagoRegion = toUnit(
            ValueNoise2D.fractal(seed ^ ARCHIPELAGO_SALT, warpedX, warpedZ, 768.0D, 3, 0.5D));
        double drainageField = ValueNoise2D.fractal(seed ^ DRAINAGE_SALT, warpedX, warpedZ, 704.0D, 4, 0.52D);
        drainageField += ValueNoise2D.fractal(seed ^ DRAINAGE_DETAIL_SALT, warpedX, warpedZ, 224.0D, 2, 0.5D) * 0.16D;
        // A wider distance ramp keeps river membership continuous at block resolution after the
        // Terra elevation graph adds steeper full-resolution slopes around the channel.
        double drainagePotential = 1.0D - smoothstep(0.012D, 0.110D, Math.abs(drainageField));
        double riverValley = smoothstep(0.10D, 0.58D, drainagePotential);
        double riverBank = smoothstep(0.36D, 0.82D, drainagePotential);
        double riverChannel = smoothstep(0.64D, 0.98D, drainagePotential);
        double islandShapeA = ValueNoise2D.fractal(seed ^ ISLAND_SHAPE_SALT, warpedX, warpedZ, 176.0D, 3, 0.54D);
        double islandShapeB = ValueNoise2D.fractal(seed ^ ISLAND_SHAPE_B_SALT, warpedX, warpedZ, 104.0D, 3, 0.52D);
        double islandEdge = ValueNoise2D.fractal(seed ^ ISLAND_EDGE_SALT, warpedX, warpedZ, 48.0D, 2, 0.5D);
        double islandSignal = Math.max(islandShapeA, islandShapeB - 0.10D) + islandEdge * 0.14D;
        double islandShelf = smoothstep(-0.42D, -0.24D, islandSignal);
        double islandShore = smoothstep(-0.18D, -0.07D, islandSignal);
        double islandInterior = smoothstep(-0.045D, 0.075D, islandSignal);

        double coastline = 1.0D - smoothstep(0.025D, 0.11D, Math.abs(continentalness));
        double regionalIslands = smoothstep(0.48D, 0.76D, archipelagoRegion);
        double openOceanSuitability = 1.0D - smoothstep(-0.18D, 0.0D, continentalness);
        double archipelagoStrength = openOceanSuitability * regionalIslands;
        double unweathered = 1.0D - smoothstep(0.38D, 0.78D, erosion);
        double rangeBackbone = smoothstep(0.62D, 0.94D, ridgeStrength);
        double terrainCapacity = smoothstep(0.34D, 0.74D, relief);
        double highlandStrength = rangeBackbone * (0.38D + 0.62D * unweathered)
            * terrainCapacity
            * smoothstep(-0.01D, 0.22D, continentalness);
        double islandCapacity = smoothstep(0.14D, 0.52D, islandSignal);
        double islandHighlandCore = rangeBackbone * (0.42D + 0.58D * unweathered) * terrainCapacity * islandCapacity;
        SurfaceTopology topology = classifyTopology(continentalness, archipelagoStrength);
        SurfaceLandform landform = classifyLandform(
            topology,
            continentalness,
            coastline,
            relief,
            highlandStrength,
            riverValley,
            riverBank,
            riverChannel,
            islandShelf,
            islandShore,
            islandInterior,
            islandHighlandCore);
        OrigenProvinceSample origenProvince = origenProvinceSampler.sample(blockX, blockZ);
        return new MacroTerrainSample(topology, landform, origenProvince);
    }

    private static long coordinateKey(int blockX, int blockZ) {
        return (long) blockX << 32 ^ blockZ & 0xFFFF_FFFFL;
    }

    /** Fixed-memory cache; a collision causes a safe deterministic resample. */
    private static final class LongSampleCache {

        private final long[] keys;
        private final MacroTerrainSample[] values;
        private final int mask;

        private LongSampleCache(int size) {
            if (Integer.bitCount(size) != 1) {
                throw new IllegalArgumentException("Macro terrain cache size must be a power of two");
            }
            keys = new long[size];
            values = new MacroTerrainSample[size];
            mask = size - 1;
        }

        private synchronized MacroTerrainSample get(long key) {
            int index = (int) WorldgenContext.mix64(key) & mask;
            return values[index] != null && keys[index] == key ? values[index] : null;
        }

        private synchronized MacroTerrainSample putIfAbsent(long key, MacroTerrainSample value) {
            int index = (int) WorldgenContext.mix64(key) & mask;
            if (values[index] != null && keys[index] == key) {
                return values[index];
            }
            keys[index] = key;
            values[index] = value;
            return value;
        }
    }

    private static SurfaceTopology classifyTopology(double continentalness, double archipelagoStrength) {
        if (archipelagoStrength >= ISLAND_CHAIN_THRESHOLD) {
            return SurfaceTopology.ISLAND_CHAIN;
        }
        return continentalness < OCEAN_THRESHOLD ? SurfaceTopology.OCEAN : SurfaceTopology.CONTINENT;
    }

    private static SurfaceLandform classifyLandform(SurfaceTopology topology, double continentalness, double coastline,
        double relief, double highlandStrength, double riverValley, double riverBank, double riverChannel,
        double islandShelf, double islandShore, double islandInterior, double islandHighlandCore) {
        if (topology == SurfaceTopology.OCEAN) {
            if (continentalness < DEEP_OCEAN_THRESHOLD) {
                return SurfaceLandform.DEEP_OCEAN;
            }
            return continentalness < SHALLOW_OCEAN_THRESHOLD ? SurfaceLandform.OCEAN : SurfaceLandform.SHALLOW_OCEAN;
        }
        if (topology == SurfaceTopology.ISLAND_CHAIN) {
            if (islandShelf < 0.5D) {
                return SurfaceLandform.OCEAN;
            }
            if (islandShore < 0.5D) {
                return SurfaceLandform.SHALLOW_OCEAN;
            }
            if (islandInterior < 0.5D) {
                return SurfaceLandform.BEACH;
            }
            if (islandHighlandCore >= 0.82D) {
                return SurfaceLandform.PEAK;
            }
            if (islandHighlandCore >= 0.28D) {
                return SurfaceLandform.SLOPE;
            }
            return relief < FLAT_RELIEF_THRESHOLD ? SurfaceLandform.FLAT : SurfaceLandform.ROLLING;
        }
        if (highlandStrength >= PEAK_THRESHOLD) {
            return SurfaceLandform.PEAK;
        }
        if (highlandStrength >= SLOPE_THRESHOLD) {
            return SurfaceLandform.SLOPE;
        }
        if (riverChannel >= 0.5D) {
            return SurfaceLandform.RIVER_CHANNEL;
        }
        if (riverBank >= 0.5D) {
            return SurfaceLandform.RIVER_BANK;
        }
        if (riverValley >= 0.5D) {
            return SurfaceLandform.VALLEY;
        }
        if (coastline >= COASTAL_LOWLAND_THRESHOLD) {
            return SurfaceLandform.COASTAL_LOWLAND;
        }
        return relief < FLAT_RELIEF_THRESHOLD ? SurfaceLandform.FLAT : SurfaceLandform.ROLLING;
    }

    private static double toUnit(double value) {
        return clamp01(value * 0.5D + 0.5D);
    }

    private static double smoothstep(double lower, double upper, double value) {
        double progress = clamp01((value - lower) / (upper - lower));
        return progress * progress * (3.0D - 2.0D * progress);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
