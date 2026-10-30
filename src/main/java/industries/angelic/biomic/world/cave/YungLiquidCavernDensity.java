/*
 * Liquid-cavern core from YUNG's Better Caves 2.0.4, LGPL-3.0,
 * commit e71f869808fc139773b75125919a65925a983a77. Noise relocation retains Jordan Peck's MIT notice.
 */
package industries.angelic.biomic.world.cave;

import java.util.Objects;

import industries.angelic.biomic.internal.yungbettercaves.YungFastNoiseRigidMulti;

/** Worldless LIQUID branch of CavernCarver.carveColumn. Does not author liquid or replace host blocks. */
public final class YungLiquidCavernDensity {

    private final YungFlooredCavernDensity.Settings settings;
    private final YungFastNoiseRigidMulti[] generators;

    public YungLiquidCavernDensity(long seed, YungFlooredCavernDensity.Settings settings) {
        this.settings = Objects.requireNonNull(settings);
        generators = new YungFastNoiseRigidMulti[settings.generatorCount()];
        for (int i = 0; i < generators.length; i++) generators[i] = new YungFastNoiseRigidMulti(
            (int) seed + 1111 * (i + 1),
            settings.fractalOctaves(),
            settings.fractalGain(),
            settings.fractalFrequency());
    }

    public Column column(int globalX, int globalZ) {
        return new Column(globalX * settings.xzCompression(), globalZ * settings.xzCompression());
    }

    public final class Column {

        private final float x, z;

        private Column(float x, float z) {
            this.x = x;
            this.z = z;
        }

        public float adjustedThreshold(int sourceY, float smoothAmp) {
            check(sourceY, smoothAmp);
            int top = settings.topY() - 2;
            if (sourceY < settings.bottomY() || sourceY > top) return 0;
            int topTransition = Math.max(top - 6, 1), bottomTransition = Math.min(settings.bottomY() + 3, 255);
            float threshold = settings.noiseThreshold();
            if (sourceY >= topTransition) threshold *= (float) (sourceY - top) / (topTransition - top);
            if (sourceY < bottomTransition)
                threshold *= (float) (sourceY - settings.bottomY()) / (bottomTransition - settings.bottomY());
            if (smoothAmp < 1) threshold *= smoothAmp;
            return threshold;
        }

        public float openDensity(int sourceY, float smoothAmp) {
            check(sourceY, smoothAmp);
            if (sourceY < settings.bottomY() || sourceY > settings.topY() - 2) return -1;
            float noise = 1, scaledY = sourceY * settings.yCompression();
            for (var generator : generators) noise = (float) (noise * (double) generator.sample(x, scaledY, z));
            return adjustedThreshold(sourceY, smoothAmp) - noise;
        }
    }

    private static void check(int y, float smoothAmp) {
        if (y < 0 || y > 255 || !Float.isFinite(smoothAmp) || smoothAmp < 0 || smoothAmp > 1)
            throw new IllegalArgumentException("Invalid source Y or region smoothing amplitude");
    }
}
