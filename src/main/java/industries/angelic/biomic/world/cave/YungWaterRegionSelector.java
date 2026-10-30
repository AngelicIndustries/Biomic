/*
 * WaterRegionController/NoiseUtils from YUNG Better Caves 2.0.4,
 * LGPL-3.0, commit e71f869808fc139773b75125919a65925a983a77.
 */
package industries.angelic.biomic.world.cave;

import java.util.Objects;
import java.util.Random;

import industries.angelic.biomic.internal.yungbettercaves.YungFastNoiseSimplex2D;

/** Original source-coordinate water/lava/null selector. No world, fluid placement or caches. */
public final class YungWaterRegionSelector {

    public enum Kind {
        WATER,
        LAVA,
        NONE
    }

    public record Settings(float waterChancePercent, float frequency) {

        public Settings {
            if (!Float.isFinite(waterChancePercent) || waterChancePercent < 0
                || waterChancePercent > 100
                || !Float.isFinite(frequency)
                || frequency <= 0
                || frequency > 1) throw new IllegalArgumentException("Invalid water-region settings");
        }

        public static Settings sourceDefaults() {
            return new Settings(40, .004f);
        }
    }

    public static final class Selection {

        private final Kind[] cells;

        private Selection(Kind[] cells) {
            this.cells = cells.clone();
        }

        public Kind at(int localX, int localZ) {
            if (localX < 0 || localX >= 16 || localZ < 0 || localZ >= 16)
                throw new IllegalArgumentException("Outside source liquid column");
            return cells[localX * 16 + localZ];
        }
    }

    private final long seed;
    private final float threshold;

    @FunctionalInterface
    interface Noise {

        float at(float x, float z);
    }

    private final Noise noise;

    public YungWaterRegionSelector(long seed, Settings settings) {
        this(seed, settings, new YungFastNoiseSimplex2D((int) seed + 444, settings.frequency())::sample);
    }

    /** Pure source-noise fixture seam; production always uses the pinned Simplex slice. */
    YungWaterRegionSelector(long seed, Settings settings, Noise noise) {
        this.seed = seed;
        threshold = threshold(settings.waterChancePercent());
        this.noise = Objects.requireNonNull(noise);
    }

    public Selection chunk(int cx, int cz) {
        int ox = Math.multiplyExact(cx, 16), oz = Math.multiplyExact(cz, 16);
        Math.addExact(ox, 15);
        Math.addExact(oz, 15);
        Random random = new Random(seed ^ cx ^ cz);
        Kind[] result = new Kind[256];
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            Kind kind = Kind.LAVA;
            if (threshold > -1f) {
                float value = noise.at(ox + x, oz + z);
                float offset = random.nextFloat() * .01f + .04f;
                kind = classify(value, threshold, offset);
            }
            result[x * 16 + z] = kind;
        }
        return new Selection(result);
    }

    static Kind classify(float value, float threshold, float offset) {
        if (value < threshold - offset) return Kind.WATER;
        if (value < threshold + offset) return Kind.NONE;
        return Kind.LAVA;
    }

    /** Preserve source's float stepping/CDF, including its zero-chance disabled sentinel. */
    static float threshold(float chance) {
        float percent = chance / 100f, current = 0, currNoise = -1;
        while (current < percent && currNoise < 1f) {
            current = cdf(currNoise);
            currNoise += .01f;
        }
        return currNoise - .01f;
    }

    private static float cdf(float x) {
        return (-.435999f * x * x * x) + (.000303f * x * x) + (.916298f * x) + .499721f;
    }
}
