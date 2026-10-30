/*
 * Floored-cavern density semantics ported from YUNG's Better Caves 2.0.4,
 * commit e71f869808fc139773b75125919a65925a983a77, under LGPL-3.0.
 * This is a worldless density source, not an activated carver.
 */
package industries.angelic.biomic.world.cave;

import industries.angelic.biomic.internal.yungbettercaves.YungFastNoiseRigidMulti;

public final class YungFlooredCavernDensity {

    private static final float CLOSED = -1.0f;

    public record Settings(int bottomY, int topY, int liquidAltitude, float yCompression, float xzCompression,
        float noiseThreshold, int fractalOctaves, float fractalGain, float fractalFrequency, int generatorCount) {

        public Settings {
            // CavernCarver clamps topY-8 to 1. An effective top of one would
            // make its top-ramp denominator zero, so this pure API rejects the
            // malformed source configuration instead of emitting NaN.
            if (bottomY < 0 || bottomY > 255 || topY < 4 || topY > 255 || topY - 2 < bottomY)
                throw new IllegalArgumentException("invalid source altitude envelope");
            if (liquidAltitude < 0 || liquidAltitude > 255)
                throw new IllegalArgumentException("liquidAltitude must be in [0,255]");
            finitePositive(yCompression, "yCompression");
            finitePositive(xzCompression, "xzCompression");
            if (!Float.isFinite(noiseThreshold) || noiseThreshold < -1.0f || noiseThreshold > 1.0f)
                throw new IllegalArgumentException("noiseThreshold must be finite and in [-1,1]");
            if (fractalOctaves < 1 || fractalOctaves > 16)
                throw new IllegalArgumentException("fractalOctaves must be in [1,16]");
            if (!Float.isFinite(fractalGain) || fractalGain < 0.0f || fractalGain > 1.0f)
                throw new IllegalArgumentException("fractalGain must be finite and in [0,1]");
            if (!Float.isFinite(fractalFrequency) || fractalFrequency <= 0.0f || fractalFrequency > 1.0f)
                throw new IllegalArgumentException("fractalFrequency must be finite and in (0,1]");
            if (generatorCount < 1 || generatorCount > 16)
                throw new IllegalArgumentException("generatorCount must be in [1,16]");
        }

        private static void finitePositive(float value, String name) {
            if (!Float.isFinite(value) || value <= 0.0f || value > 100.0f)
                throw new IllegalArgumentException(name + " must be finite and in (0,100]");
        }
    }

    private final Settings settings;
    private final YungFastNoiseRigidMulti[] generators;

    public YungFlooredCavernDensity(long worldSeed, Settings settings) {
        if (settings == null) throw new IllegalArgumentException("settings cannot be null");
        this.settings = settings;
        generators = new YungFastNoiseRigidMulti[settings.generatorCount()];
        for (int i = 0; i < generators.length; i++) {
            int sourceSeed = (int) worldSeed + 1111 * (i + 1);
            generators[i] = new YungFastNoiseRigidMulti(
                sourceSeed,
                settings.fractalOctaves(),
                settings.fractalGain(),
                settings.fractalFrequency());
        }
    }

    public Column column(int blockX, int blockZ) {
        return new Column(blockX * settings.xzCompression(), blockZ * settings.xzCompression());
    }

    public final class Column {

        private final float scaledX, scaledZ;

        private Column(float scaledX, float scaledZ) {
            this.scaledX = scaledX;
            this.scaledZ = scaledZ;
        }

        public float adjustedThreshold(int sourceY) {
            if (sourceY < 0 || sourceY > 255) throw new IllegalArgumentException("sourceY must be in [0,255]");
            int effectiveTop = settings.topY() - 2;
            if (sourceY < settings.bottomY() || sourceY > effectiveTop) return 0.0f;
            float threshold = settings.noiseThreshold();
            int topTransition = Math.max(effectiveTop - 6, 1);
            int bottomTransition = Math.min(
                settings.bottomY() < settings.liquidAltitude() ? settings.liquidAltitude() + 8 : settings.bottomY() + 7,
                255);
            if (sourceY >= topTransition)
                threshold *= (float) (sourceY - effectiveTop) / (topTransition - effectiveTop);
            if (sourceY < bottomTransition)
                threshold *= (float) (sourceY - settings.bottomY()) / (bottomTransition - settings.bottomY());
            return threshold;
        }

        public float openDensity(int sourceY) {
            if (sourceY < 0 || sourceY > 255) throw new IllegalArgumentException("sourceY must be in [0,255]");
            int effectiveTop = settings.topY() - 2;
            if (sourceY < settings.bottomY() || sourceY > effectiveTop) return CLOSED;
            float scaledY = sourceY * settings.yCompression();
            float noise = 1;
            for (YungFastNoiseRigidMulti generator : generators)
                noise = (float) (noise * (double) generator.sample(scaledX, scaledY, scaledZ));
            return adjustedThreshold(sourceY) - noise;
        }
    }
}
