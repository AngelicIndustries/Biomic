package industries.angelic.biomic.world.modern.noise;

public interface ModernPositionalRandom {

    ModernRandom fromHashOf(String name);

    ModernRandom at(int x, int y, int z);
}
