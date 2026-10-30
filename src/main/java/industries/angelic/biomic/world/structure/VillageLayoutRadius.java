package industries.angelic.biomic.world.structure;

import java.util.function.Supplier;

/**
 * How far a village's roads may grow from its start, for the start being built on this thread. Vanilla and
 * Village Names hard-code 112; small villages build with less.
 */
public final class VillageLayoutRadius {

    private static final ThreadLocal<Integer> RADIUS = new ThreadLocal<>();

    private VillageLayoutRadius() {}

    /** The radius in force, or vanilla's when no Biomic village is being built. */
    public static int get(int vanilla) {
        Integer radius = RADIUS.get();
        return radius == null ? vanilla : radius;
    }

    static <T> T with(int radius, Supplier<T> build) {
        Integer previous = RADIUS.get();
        RADIUS.set(radius);
        try {
            return build.get();
        } finally {
            if (previous == null) RADIUS.remove();
            else RADIUS.set(previous);
        }
    }
}
