package industries.angelic.biomic.world.modern.noise;

/** Exact 1.21.1 {@code BlendedNoise} used by the modern Overworld base density. */
public final class ModernBlendedNoise {

    private static final double WRAP = 3.3554432E7D;

    private final ModernImprovedNoise[] minimum = new ModernImprovedNoise[16];
    private final ModernImprovedNoise[] maximum = new ModernImprovedNoise[16];
    private final ModernImprovedNoise[] main = new ModernImprovedNoise[8];
    private final double xzMultiplier;
    private final double yMultiplier;
    private final double xzFactor;
    private final double yFactor;
    private final double smearScaleMultiplier;

    public ModernBlendedNoise(long worldSeed, double xzScale, double yScale, double xzFactor, double yFactor,
        double smearScaleMultiplier) {
        ModernRandom random = new ModernXoroshiroRandom(worldSeed).forkPositional()
            .fromHashOf("minecraft:terrain");
        createLegacyOctaves(random, minimum);
        createLegacyOctaves(random, maximum);
        createLegacyOctaves(random, main);
        this.xzMultiplier = 684.412D * xzScale;
        this.yMultiplier = 684.412D * yScale;
        this.xzFactor = xzFactor;
        this.yFactor = yFactor;
        this.smearScaleMultiplier = smearScaleMultiplier;
    }

    public double sample(int blockX, int blockY, int blockZ) {
        double scaledX = blockX * xzMultiplier;
        double scaledY = blockY * yMultiplier;
        double scaledZ = blockZ * xzMultiplier;
        double mainX = scaledX / xzFactor;
        double mainY = scaledY / yFactor;
        double mainZ = scaledZ / xzFactor;
        double smear = yMultiplier * smearScaleMultiplier;
        double mainSmear = smear / yFactor;

        double blendNoise = 0.0D;
        double octaveScale = 1.0D;
        for (int octave = 0; octave < 8; octave++) {
            ModernImprovedNoise noise = octave(main, octave);
            blendNoise += noise.sample(
                wrap(mainX * octaveScale),
                wrap(mainY * octaveScale),
                wrap(mainZ * octaveScale),
                mainSmear * octaveScale,
                mainY * octaveScale) / octaveScale;
            octaveScale /= 2.0D;
        }

        double blend = (blendNoise / 10.0D + 1.0D) / 2.0D;
        boolean skipMinimum = blend >= 1.0D;
        boolean skipMaximum = blend <= 0.0D;
        double minimumValue = 0.0D;
        double maximumValue = 0.0D;
        octaveScale = 1.0D;
        for (int octave = 0; octave < 16; octave++) {
            double x = wrap(scaledX * octaveScale);
            double y = wrap(scaledY * octaveScale);
            double z = wrap(scaledZ * octaveScale);
            double yScale = smear * octaveScale;
            double yMaximum = scaledY * octaveScale;
            if (!skipMinimum) {
                minimumValue += octave(minimum, octave).sample(x, y, z, yScale, yMaximum) / octaveScale;
            }
            if (!skipMaximum) {
                maximumValue += octave(maximum, octave).sample(x, y, z, yScale, yMaximum) / octaveScale;
            }
            octaveScale /= 2.0D;
        }
        return clampedLerp(minimumValue / 512.0D, maximumValue / 512.0D, blend) / 128.0D;
    }

    /** Legacy blended-noise construction creates octave zero first, then walks downward. */
    private static void createLegacyOctaves(ModernRandom random, ModernImprovedNoise[] levels) {
        levels[levels.length - 1] = new ModernImprovedNoise(random);
        for (int index = levels.length - 2; index >= 0; index--) {
            levels[index] = new ModernImprovedNoise(random);
        }
    }

    /** {@code PerlinNoise#getOctaveNoise} exposes its backing array in reverse order. */
    private static ModernImprovedNoise octave(ModernImprovedNoise[] levels, int index) {
        return levels[levels.length - 1 - index];
    }

    private static double wrap(double value) {
        return value - Math.floor(value / WRAP + 0.5D) * WRAP;
    }

    private static double clampedLerp(double start, double end, double delta) {
        if (delta < 0.0D) return start;
        if (delta > 1.0D) return end;
        return start + delta * (end - start);
    }
}
