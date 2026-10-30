package industries.angelic.biomic.compat.flora;

import java.util.List;
import java.util.Set;

/** Loader-free closed optional-hook selection, before any audited constant injector is installed. */
public final class NaturaMixinVersions {

    /** The Natura builds whose classes the mixins are written against. */
    private static final java.util.Set<String> SUPPORTED = java.util.Set.of("2.8.19", "2.8.24");

    private NaturaMixinVersions() {}

    public static void filter(List<String> selected, Set<String> loadedMods, String installedVersion) {
        boolean capital = loadedMods.contains("Natura"), lowercase = loadedMods.contains("natura");
        // Two aliases at once are an ambiguous provider, not proof of a particular class layout.
        if (capital == lowercase || !SUPPORTED.contains(installedVersion))
            selected.removeIf(name -> name.startsWith("natura."));
    }
}
