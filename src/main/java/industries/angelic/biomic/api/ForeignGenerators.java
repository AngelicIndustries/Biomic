package industries.angelic.biomic.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Other mods' world generators (IWorldGenerator, run by Forge after the provider's population) that stand down in the
 * chunks Biomic generates, named by class: a mod that places those blocks itself, through Biomic, registers them here
 * at preInit or init. A name whose mod is not installed never matches, so naming one costs nothing without it.
 */
public final class ForeignGenerators {

    /** Where a generator stands down. */
    public enum Scope {
        /** Biomic's overworld, 256 or 512 high. */
        OVERWORLD,
        /** Every Biomic dimension, the Tartarus Nether included. */
        EVERY_DIMENSION
    }

    private static final Map<String, Scope> STANDING_DOWN = new ConcurrentHashMap<>();

    private ForeignGenerators() {}

    /** The generator class (its binary name) stands down in {@code scope}; registered twice, the wider scope holds. */
    public static void standDown(String generatorClass, Scope scope) {
        if (generatorClass == null || generatorClass.isEmpty() || scope == null)
            throw new IllegalArgumentException("A generator class and a scope are required");
        STANDING_DOWN.merge(generatorClass, scope, (a, b) -> a == Scope.EVERY_DIMENSION ? a : b);
    }

    /** Where the generator class stands down, or null when it runs everywhere. */
    public static Scope scope(String generatorClass) {
        return STANDING_DOWN.isEmpty() ? null : STANDING_DOWN.get(generatorClass);
    }
}
