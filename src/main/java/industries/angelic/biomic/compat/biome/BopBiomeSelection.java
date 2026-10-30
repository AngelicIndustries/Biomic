package industries.angelic.biomic.compat.biome;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/** New-save editorial selection, not registry support. Never apply this filter to a saved manifest. */
public final class BopBiomeSelection {

    private BopBiomeSelection() {}

    // Rationale and the complete keep/cut matrix: docs/bop-curation-sep17.md.
    private static final Set<String> SURFACE = Set.of(
        BopNativeBiomes.DEAD_SWAMP,
        BopNativeBiomes.GARDEN,
        BopNativeBiomes.KELP_FOREST,
        BopNativeBiomes.LAVENDER,
        BopNativeBiomes.MYSTIC_GROVE,
        BopNativeBiomes.OMINOUS_WOODS,
        BopNativeBiomes.SACRED_SPRINGS,
        BopNativeBiomes.SILKGLADES,
        BopNativeBiomes.SLUDGEPIT,
        BopNativeBiomes.VOLCANO,
        BopNativeBiomes.WASTELAND);

    public static List<String> surface(Collection<String> available, boolean fullCatalogue) {
        List<String> selected = select(available, SURFACE, BopNativeBiomes.supportedKeys(), fullCatalogue);
        // Silkglades is genuinely a Sludgepit child. Never enable a disabled/missing native parent to keep it.
        return selected.stream()
            .filter(key -> {
                var rule = BopSubBiomePolicy.rule(key);
                return rule == null || BopSubBiomePolicy.externalParent(key) || selected.contains(rule.parent());
            })
            .toList();
    }

    private static List<String> select(Collection<String> available, Set<String> curated, Set<String> supported,
        boolean fullCatalogue) {
        if (available == null || !supported.containsAll(available))
            throw new IllegalArgumentException("Unknown available BOP habitat");
        return available.stream()
            .filter(key -> fullCatalogue || curated.contains(key))
            .distinct()
            .sorted()
            .toList();
    }
}
