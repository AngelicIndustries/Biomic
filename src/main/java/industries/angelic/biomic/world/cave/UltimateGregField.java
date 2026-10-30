package industries.angelic.biomic.world.cave;

import java.util.Random;

import industries.angelic.biomic.internal.ultimatecaves.NoiseCaveGenerator;

/**
 * Relocated UCM/Greg cave-density kernel. No replacement terrain generator or
 * global ore/bedrock hooks. Input host density has the source's legacy units;
 * output preserves its solid-positive convention. It is reachable only through the explicit regional provider.
 */
public final class UltimateGregField {

    private final NoiseCaveGenerator source;

    public UltimateGregField(long worldSeed) {
        source = new NoiseCaveGenerator(sourceRandom(worldSeed));
    }

    static Random sourceRandom(long worldSeed) {
        Random random = new Random(worldSeed);
        // MapGenGregCaves creates 16+16+8+16 vanilla octave samplers first.
        // Advance their exact ImprovedNoise constructor draws without allocating
        // unused vanilla terrain fields or importing its terrain/biome blending.
        for (int octave = 0; octave < 56; octave++) {
            random.nextDouble();
            random.nextDouble();
            random.nextDouble();
            for (int i = 0; i < 256; i++) random.nextInt(256 - i);
        }
        return random;
    }

    public double density(double hostDensity, int x, int y, int z) {
        if (!Double.isFinite(hostDensity)) throw new IllegalArgumentException("Non-finite Greg host density");
        return source.sample(hostDensity, y, z, x);
    }
}
