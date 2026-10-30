package industries.angelic.biomic.testterra.raster.fixture;

/** The sole Terra helper required by the unchanged interpolator algorithms. */
public final class MathUtil {

    public static double lerp(double t, double v0, double v1) {
        return v0 + t * (v1 - v0);
    }
}
