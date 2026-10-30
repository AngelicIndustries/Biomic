package industries.angelic.biomic.biome;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Stable-key behavior assigned to one entry in Biomic's vertical-biome catalogue. */
public final class VerticalBiomeDefinition {

    public enum Trait {
        CAVE,
        VEGETATED,
        LARGE_CAVE,
        FLOATING_ISLAND
    }

    public enum CaveShape {
        NONE,
        VINE_VAULT,
        INFERNO_ISLANDS,
        TERRACOTTA_TOMBS
    }

    public enum CaveSurface {
        NONE,
        VEGETATED,
        INFERNO,
        TERRACOTTA,
        RU_ANCIENT_DELTA,
        RU_BIOSHROOM,
        RU_PRISMACHASM
    }

    /** Surface treatment for terrain whose identity is stored above the ordinary two-dimensional biome. */
    public enum FloatingSurface {
        NONE,
        TEMPERATE_GRASS
    }

    private final String key;
    private final Set<Trait> traits;
    private final CaveShape caveShape;
    private final FloatingSurface floatingSurface;

    VerticalBiomeDefinition(String key, CaveShape caveShape, CaveSurface caveSurface, Trait... traits) {
        this(key, caveShape, caveSurface, FloatingSurface.NONE, traits);
    }

    VerticalBiomeDefinition(String key, CaveShape caveShape, CaveSurface caveSurface, FloatingSurface floatingSurface,
        Trait... traits) {
        if (key == null || key.isEmpty()
            || caveShape == null
            || caveSurface == null
            || floatingSurface == null
            || traits == null) {
            throw new IllegalArgumentException("Vertical biome definition fields cannot be null or empty");
        }
        this.key = key;
        this.caveShape = caveShape;
        this.floatingSurface = floatingSurface;
        EnumSet<Trait> copiedTraits = EnumSet.noneOf(Trait.class);
        Collections.addAll(copiedTraits, traits);
        this.traits = Collections.unmodifiableSet(copiedTraits);
    }

    public String getKey() {
        return key;
    }

    public boolean hasTrait(Trait trait) {
        return traits.contains(trait);
    }

    public CaveShape getCaveShape() {
        return caveShape;
    }

    public FloatingSurface getFloatingSurface() {
        return floatingSurface;
    }
}
