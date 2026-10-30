package industries.angelic.biomic.internal.terra.noise.samplers.noise;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.biomic.internal.terra.noise.NoiseSampler;

public class CellularSamplerVerticalTest {

    @Test
    public void pureCellLookupMemoizationIsBitExactAcrossColumnsAndSeeds() {
        for (CellularSampler.DistanceFunction distance : CellularSampler.DistanceFunction.values()) {
            CellularSampler sampler = configured(distance, CellularSampler.ReturnType.NoiseLookup);
            sampler.setMemoizeCellLookup(true);
            assertColumn(sampler, 42L, 128.25D, -64, 512, 1, 1.0D, -96.75D);
            assertColumn(sampler, -789L, -128.25D, -64, 516, 4, 0.375D, 96.75D);
            assertMaskedColumn(sampler, 42L, 128.25D, -64, 512, 1, 1.0D, -96.75D, true);
            assertMaskedColumn(sampler, -789L, -128.25D, -64, 512, 4, 0.375D, 96.75D, false);
        }
    }

    @Test
    public void memoizedLookupOnlyEvaluatesSelectedSitesAndDoesNotLeakAcrossCalls() {
        CellularSampler sampler = configured(
            CellularSampler.DistanceFunction.EuclideanSq,
            CellularSampler.ReturnType.NoiseLookup);
        sampler.setMemoizeCellLookup(true);
        int[] calls = { 0 };
        sampler.setNoiseLookup(new NoiseSampler() {

            public double noise(long seed, double x, double y) {
                throw new AssertionError("Expected3D lookup");
            }

            public double noise(long seed, double x, double y, double z) {
                calls[0]++;
                return seed + x + y - z;
            }
        });
        double[] y = { 0, 0, 0, 0 }, actual = { 91, 92, 93, 94 };
        boolean[] mask = { false, true, false, true };
        sampler.fillNoiseVerticalMasked(42L, 3, -9, y, 0, mask, 0, 4, actual, 0);
        assertEquals(1, calls[0]);
        assertEquals(actual[1], actual[3], 0.0D);
        assertEquals(91, actual[0], 0.0D);
        assertEquals(93, actual[2], 0.0D);
        double first = actual[1];
        sampler.fillNoiseVerticalMasked(43L, 3, -9, y, 0, mask, 0, 4, actual, 0);
        assertEquals(2, calls[0]);
        Assert.assertNotEquals(first, actual[1], 0.0D);
        sampler.fillNoiseVerticalMasked(43L, 3, -9, y, 0, new boolean[4], 0, 4, actual, 0);
        assertEquals(2, calls[0]);
        sampler.setReturnType(CellularSampler.ReturnType.LocalNoiseLookup);
        sampler.fillNoiseVerticalMasked(43L, 3, -9, y, 0, mask, 0, 4, actual, 0);
        assertEquals(4, calls[0]); // Local lookup coordinates depend on the row: never cached.
    }

    @Test
    public void lookupReplacementAndExplicitReconfigurationAreObservedByLaterCalls() {
        CellularSampler sampler = configured(
            CellularSampler.DistanceFunction.EuclideanSq,
            CellularSampler.ReturnType.NoiseLookup);
        sampler.setMemoizeCellLookup(true);
        double[] value = { 1 };
        NoiseSampler lookup = new NoiseSampler() {

            public double noise(long seed, double x, double y) {
                return value[0];
            }

            public double noise(long seed, double x, double y, double z) {
                return value[0];
            }
        };
        sampler.setNoiseLookup(lookup);
        double[] actual = new double[1];
        sampler.fillNoiseVerticalStrided(42, 3, 0, 1, 1, 1, -9, actual, 0);
        assertEquals(1, actual[0], 0);
        value[0] = 2;
        sampler.setNoiseLookup(lookup);
        sampler.fillNoiseVerticalStrided(42, 3, 0, 1, 1, 1, -9, actual, 0);
        assertEquals(2, actual[0], 0);
        value[0] = 3;
        sampler.setMemoizeCellLookup(false);
        sampler.setMemoizeCellLookup(true);
        sampler.fillNoiseVerticalStrided(42, 3, 0, 1, 1, 1, -9, actual, 0);
        assertEquals(3, actual[0], 0);
    }

    @Test
    public void columnsStayExactAcrossFullLongSeedsAndSamplerConfigurationChanges() {
        CellularSampler sampler = configured(
            CellularSampler.DistanceFunction.Hybrid,
            CellularSampler.ReturnType.NoiseLookup);
        sampler.setMemoizeCellLookup(true);
        sampler.setNoiseLookup(new NoiseSampler() {

            public double noise(long seed, double x, double y) {
                return seed + x - y;
            }

            public double noise(long seed, double x, double y, double z) {
                return (seed >>> 32) * 0.125 + x - y + z;
            }
        });
        // Repeatedly revisit a broad set of sites, including seeds with equal low32 bits.
        for (int repeat = 0; repeat < 2; repeat++) for (int i = 0; i < 300; i++)
            assertColumn(sampler, 42L + ((long) (i % 7) << 32), i * 153.125, -64, 512, 64, 1.0, -i * 177.75);
        sampler.setFrequency(0.007);
        sampler.setJitterModifier(0.19);
        sampler.setSalt(-999);
        assertColumn(sampler, 42L, -256.5, -64, 512, 4, 1.0, 511.75);
    }

    @Test
    public void lookupReconfigurationIsObservedAcrossThreadsWithoutSharingScratch() throws Exception {
        CellularSampler sampler = configured(
            CellularSampler.DistanceFunction.Hybrid,
            CellularSampler.ReturnType.NoiseLookup);
        sampler.setMemoizeCellLookup(true);
        java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            for (int revision = 1; revision <= 2; revision++) {
                final int result = revision;
                sampler.setNoiseLookup(new NoiseSampler() {

                    public double noise(long seed, double x, double y) {
                        return result;
                    }

                    public double noise(long seed, double x, double y, double z) {
                        return result;
                    }
                });
                java.util.concurrent.Callable<Double> sample = () -> {
                    double[] out = new double[1];
                    sampler.fillNoiseVerticalStrided(42, 3, 0, 1, 1, 1, -9, out, 0);
                    return out[0];
                };
                java.util.concurrent.Future<Double> background = worker.submit(sample);
                assertEquals(result, sample.call(), 0);
                assertEquals(result, background.get(10, java.util.concurrent.TimeUnit.SECONDS), 0);
            }
        } finally {
            worker.shutdownNow();
        }
    }

    @Test
    public void verticalBatchIsBitIdenticalToScalarSampling() {
        for (CellularSampler.DistanceFunction distance : CellularSampler.DistanceFunction.values()) {
            for (CellularSampler.ReturnType result : CellularSampler.ReturnType.values()) {
                CellularSampler sampler = configured(distance, result);
                assertColumn(sampler, 0x5EEDC0DEL, 4_096.375D, -64, 64, 1, 1.0D, -3_777.625D);
                assertColumn(sampler, -0x41B10C42L, -12_345.125D, -30, 40, 1, 2.0D, 9_876.875D);
                assertColumn(sampler, 42L, -3_072.5D, -64, 324, 4, 1.0D, 2_852.25D);
            }
        }
    }

    @Test
    public void filteredCellularFallsBackToTheExactScalarSequence() {
        CellularSampler sampler = configured(
            CellularSampler.DistanceFunction.EuclideanSq,
            CellularSampler.ReturnType.NoiseLookup);
        sampler.setCellFilter(new NoiseSampler() {

            @Override
            public double noise(long seed, double x, double y) {
                return x + y > 0.0D ? 1.0D : -1.0D;
            }

            @Override
            public double noise(long seed, double x, double y, double z) {
                return x + y - z > 0.0D ? 1.0D : -1.0D;
            }
        });
        assertColumn(sampler, 42L, 128.25D, -64, 80, 1, 1.0D, -96.75D);
        assertColumn(sampler, 42L, 128.25D, -64, 84, 4, 1.0D, -96.75D);
        assertMaskedColumn(sampler, 42L, 128.25D, -64, 84, 4, 1.0D, -96.75D, true);
    }

    @Test
    public void maskedVerticalBatchIsBitIdenticalAndLeavesRejectedRowsUntouched() {
        for (CellularSampler.DistanceFunction distance : CellularSampler.DistanceFunction.values()) {
            for (CellularSampler.ReturnType result : CellularSampler.ReturnType.values()) {
                CellularSampler sampler = configured(distance, result);
                assertMaskedColumn(sampler, 42L, 128.25D, -64, 84, 1, 0.375D, -96.75D, false);
                assertMaskedColumn(sampler, -91L, -3072.5D, -64, 324, 4, 1.0D, 2852.25D, true);
            }
        }
    }

    @Test
    public void maskedNoiseLookupDoesNotEvaluateRejectedRowsAndSurvivesNestedColumnLookup() {
        CellularSampler sampler = configured(
            CellularSampler.DistanceFunction.EuclideanSq,
            CellularSampler.ReturnType.NoiseLookup);
        int[] calls = { 0 };
        sampler.setNoiseLookup(new NoiseSampler() {

            public double noise(long seed, double x, double y) {
                calls[0]++;
                return x - y;
            }

            public double noise(long seed, double x, double y, double z) {
                calls[0]++;
                return x + y - z;
            }
        });
        assertMaskedColumn(sampler, 42L, 128.25D, -30, 40, 1, 1.0D, -96.75D, false);
        // 46 selected rows in the batch plus the same 46 scalar reference evaluations.
        assertEquals(92, calls[0]);

        CellularSampler nested = configured(
            CellularSampler.DistanceFunction.EuclideanSq,
            CellularSampler.ReturnType.NoiseLookup);
        nested.setMemoizeCellLookup(true);
        boolean[] inside = { false };
        nested.setNoiseLookup(new NoiseSampler() {

            public double noise(long seed, double x, double y) {
                return x - y;
            }

            public double noise(long seed, double x, double y, double z) {
                if (!inside[0]) {
                    inside[0] = true;
                    try {
                        double[] innerY = { -7.25D, 3.5D }, inner = { 0, 0 };
                        boolean[] selected = { true, true };
                        nested.fillNoiseVerticalMasked(seed, 19.5D, -23.75D, innerY, 0, selected, 0, 2, inner, 0);
                    } finally {
                        inside[0] = false;
                    }
                }
                return x + y - z;
            }
        });
        CellularSampler baseline = configured(
            CellularSampler.DistanceFunction.EuclideanSq,
            CellularSampler.ReturnType.NoiseLookup);
        baseline.setNoiseLookup(new NoiseSampler() {

            public double noise(long seed, double x, double y) {
                return x - y;
            }

            public double noise(long seed, double x, double y, double z) {
                return x + y - z;
            }
        });
        double[] y = { -30.0D, -11.5D, 0.0D, 17.25D, 39.0D }, expected = new double[5], actual = new double[5];
        boolean[] selected = { true, true, true, true, true };
        baseline.fillNoiseVerticalMasked(77L, 256.125D, -511.875D, y, 0, selected, 0, 5, expected, 0);
        nested.fillNoiseVerticalMasked(77L, 256.125D, -511.875D, y, 0, selected, 0, 5, actual, 0);
        for (int i = 0; i < 5; i++)
            assertEquals(Double.doubleToRawLongBits(expected[i]), Double.doubleToRawLongBits(actual[i]));
    }

    private static CellularSampler configured(CellularSampler.DistanceFunction distance,
        CellularSampler.ReturnType result) {
        CellularSampler sampler = new CellularSampler();
        sampler.setFrequency(0.005D);
        sampler.setSalt(24L);
        sampler.setJitterModifier(0.73D);
        sampler.setDistanceFunction(distance);
        sampler.setReturnType(result);
        return sampler;
    }

    private static void assertColumn(CellularSampler sampler, long seed, double x, int minimumY, int maximumYExclusive,
        int yStep, double yScale, double z) {
        int sampleCount = (maximumYExclusive - minimumY + yStep - 1) / yStep;
        double[] actual = new double[sampleCount];
        sampler.fillNoiseVerticalStrided(seed, x, minimumY, maximumYExclusive, yStep, yScale, z, actual, 0);
        int sample = 0;
        for (int y = minimumY; y < maximumYExclusive; y += yStep) {
            double expected = sampler.noise(seed, x, y * yScale, z);
            Assert.assertEquals(
                "distance=" + sampler + " y=" + y,
                Long.toHexString(Double.doubleToRawLongBits(expected)),
                Long.toHexString(Double.doubleToRawLongBits(actual[sample++])));
        }
    }

    private static void assertMaskedColumn(CellularSampler sampler, long seed, double x, int minimumY,
        int maximumYExclusive, int yStep, double yScale, double z, boolean sparse) {
        int count = (maximumYExclusive - minimumY + yStep - 1) / yStep;
        double[] y = new double[count + 4], actual = new double[count + 6];
        boolean[] selected = new boolean[count + 3];
        java.util.Arrays.fill(actual, 0x1.5a5a5a5a5a5a5p37D);
        int row = 0;
        for (int value = minimumY; value < maximumYExclusive; value += yStep) {
            y[2 + row] = (value * yScale) * 2.0D;
            selected[1 + row] = sparse ? row % 11 == 0 : row % 3 != 0;
            row++;
        }
        sampler.fillNoiseVerticalMasked(seed, x, z, y, 2, selected, 1, count, actual, 3);
        for (row = 0; row < count; row++) {
            double expected = selected[1 + row] ? sampler.noise(seed, x, y[2 + row], z) : 0x1.5a5a5a5a5a5a5p37D;
            Assert.assertEquals(
                Long.toHexString(Double.doubleToRawLongBits(expected)),
                Long.toHexString(Double.doubleToRawLongBits(actual[3 + row])));
        }
        assertEquals(0x1.5a5a5a5a5a5a5p37D, actual[0], 0.0D);
        assertEquals(0x1.5a5a5a5a5a5a5p37D, actual[actual.length - 1], 0.0D);
    }
}
