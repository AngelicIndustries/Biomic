package industries.angelic.biomic.content.block;

/** Rendering-only marker for optional native plants containing a source-water cell. */
public interface WaterloggedRenderPlant {

    /** Shared model families may also have dry instances; the marker alone is not fluid evidence. */
    default boolean containsSourceWater() {
        return true;
    }
}
