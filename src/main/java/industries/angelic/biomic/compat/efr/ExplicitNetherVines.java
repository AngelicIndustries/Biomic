package industries.angelic.biomic.compat.efr;

/** Metadata 0/1 remain EFR's original contextual vines, including in existing saves. */
public final class ExplicitNetherVines {

    public static final int BODY = 2, STOPPED_HEAD = 3, GROWING_HEAD = 4;

    private ExplicitNetherVines() {}

    public static int encode(boolean body, int age) {
        return body ? BODY : age >= 25 ? STOPPED_HEAD : GROWING_HEAD;
    }
}
