package industries.angelic.biomic.world.modern.noise;

public interface ModernRandom {

    long nextLong();

    int nextInt(int bound);

    double nextDouble();

    float nextFloat();

    boolean nextBoolean();

    double nextGaussian();

    ModernPositionalRandom forkPositional();
}
