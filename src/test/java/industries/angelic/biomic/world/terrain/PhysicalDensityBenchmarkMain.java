package industries.angelic.biomic.world.terrain;

import java.lang.reflect.Constructor;
import java.util.Random;

/** Bounded interpolation-only benchmark; graph compilation/noise, world work and GC are outside its scope. */
public final class PhysicalDensityBenchmarkMain {

    private static volatile long sink;

    public static void main(String[] args) throws Exception {
        Random random = new Random(42);
        double[] lattice = new double[5 * 5 * 97];
        double[] elevation = new double[256];
        for (int i = 0; i < lattice.length; i++) lattice[i] = random.nextDouble() * 20 - 10;
        for (int i = 0; i < elevation.length; i++) elevation[i] = random.nextDouble() * 4 - 2;
        Constructor<OrigenDensityField.ScaledChunkDensity> constructor = OrigenDensityField.ScaledChunkDensity.class
            .getDeclaredConstructor(double[].class, double[].class);
        constructor.setAccessible(true);
        OrigenDensityField.ScaledChunkDensity raster = constructor.newInstance(lattice, elevation);
        double[] column = new double[256];
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            raster.fillPhysicalColumn(x, z, column);
            for (int y = 0; y < 256; y++) {
                if (Double.doubleToRawLongBits(raster.sample(x, y, z)) != Double.doubleToRawLongBits(column[y]))
                    throw new AssertionError("Raw-bit divergence at " + x + "," + y + "," + z);
            }
        }
        for (int i = 0; i < 20; i++) {
            run(raster, column, false, 8);
            run(raster, column, true, 8);
        }
        for (int i = 0; i < 5; i++) {
            boolean batchFirst = i % 2 == 1;
            long first = run(raster, column, batchFirst, 80);
            long firstChecksum = sink;
            long second = run(raster, column, !batchFirst, 80);
            if (firstChecksum != sink) throw new AssertionError("Benchmark checksum divergence");
            System.out.printf(
                java.util.Locale.ROOT,
                "trial=%d scalar_ms=%.3f batch_ms=%.3f checksum=%d%n",
                i,
                (batchFirst ? second : first) / 1e6,
                (batchFirst ? first : second) / 1e6,
                sink);
        }
    }

    private static long run(OrigenDensityField.ScaledChunkDensity raster, double[] column, boolean batch, int chunks) {
        long checksum = 0;
        long start = System.nanoTime();
        for (int chunk = 0; chunk < chunks; chunk++) for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            if (batch) raster.fillPhysicalColumn(x, z, column);
            for (int y = 0; y < 256; y++)
                checksum += Double.doubleToRawLongBits(batch ? column[y] : raster.sample(x, y, z));
        }
        sink = checksum;
        return System.nanoTime() - start;
    }
}
