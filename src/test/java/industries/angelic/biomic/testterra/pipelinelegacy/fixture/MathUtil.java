package industries.angelic.biomic.testterra.pipelinelegacy.fixture;

/** Exact single normalizeIndex operation required by the source stage oracle. */
public final class MathUtil {

    public static int normalizeIndex(double val, int size) {
        return Math.max(Math.min((int) Math.floor(((val + 1D) / 2D) * size), size - 1), 0);
    }
}
