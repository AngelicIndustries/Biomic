package industries.angelic.biomic.content.alexscaves;

/** Dormant client seam; root integration assigns and registers the dedicated renderer later. */
public final class AbyssalClientRendering {

    private static int renderId = -1, generatedItemRenderId = -1;

    private AbyssalClientRendering() {}

    public static int renderId() {
        return renderId;
    }

    public static int generatedItemRenderId() {
        return generatedItemRenderId;
    }

    public static void bindRenderIds(int modelId, int flatItemId) {
        if (modelId < 0 || flatItemId < 0) throw new IllegalArgumentException("Invalid Abyssal render id");
        renderId = modelId;
        generatedItemRenderId = flatItemId;
    }
}
