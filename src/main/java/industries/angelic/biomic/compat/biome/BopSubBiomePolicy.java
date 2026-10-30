package industries.angelic.biomic.compat.biome;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import industries.angelic.biomic.world.WorldgenContext;

/** Native parent relationships, sampled once per host province/sky cluster, never per tree or column. */
public final class BopSubBiomePolicy {

    public record Rule(String child, String parent, double sourceZoom, double sourceThreshold) {}

    public static final String OCEAN = "minecraft:ocean", DEEP_OCEAN = "minecraft:deep_ocean",
        DESERT = "minecraft:desert", SAVANNA = "minecraft:savanna", FOREST = "minecraft:forest";
    private static final List<Rule> RULES = List.of(
        new Rule(BopNativeBiomes.ALPS_FOREST, BopNativeBiomes.ALPS, .25, .25),
        new Rule(BopNativeBiomes.MEADOW_FOREST, BopNativeBiomes.MEADOW, .25, .25),
        new Rule(BopNativeBiomes.SEASONAL_CLEARING, BopNativeBiomes.SEASONAL, .5, .5),
        new Rule(BopNativeBiomes.GLACIER, BopNativeBiomes.ARCTIC, .25, .25),
        new Rule(BopNativeBiomes.QUAGMIRE, BopNativeBiomes.SLUDGEPIT, .5, 0),
        new Rule(BopNativeBiomes.SILKGLADES, BopNativeBiomes.SLUDGEPIT, .25, .25),
        new Rule(BopNativeBiomes.CANYON_RAVINE, BopNativeBiomes.CANYON, .25, .25),
        new Rule(BopNativeBiomes.LAND_OF_LAKES_MARSH, BopNativeBiomes.LAND_OF_LAKES, .5, .25),
        new Rule(BopNativeBiomes.CORAL_REEF, OCEAN, .25, .25),
        new Rule(BopNativeBiomes.KELP_FOREST, OCEAN, .25, .25),
        new Rule(BopNativeBiomes.MANGROVE, DEEP_OCEAN, .01, .5),
        new Rule(BopNativeBiomes.TROPICS, DEEP_OCEAN, .01, .5),
        new Rule(BopNativeBiomes.VOLCANO, DEEP_OCEAN, .01, .5),
        new Rule(BopNativeBiomes.OASIS, DESERT, .5, .5),
        new Rule(BopNativeBiomes.SCRUBLAND, SAVANNA, .25, 0),
        new Rule(BopNativeBiomes.SPRUCE_WOODS, FOREST, .25, .25));
    private static final Map<String, Rule> BY_CHILD = RULES.stream()
        .collect(java.util.stream.Collectors.toUnmodifiableMap(Rule::child, r -> r));
    private static final Set<String> EXTERNAL_PARENTS = Set.of(OCEAN, DEEP_OCEAN, DESERT, SAVANNA, FOREST);

    private BopSubBiomePolicy() {}

    public static Rule rule(String child) {
        return child == null ? null : BY_CHILD.get(child);
    }

    public static boolean isChild(String child) {
        return rule(child) != null;
    }

    public static boolean externalParent(String child) {
        Rule rule = rule(child);
        return rule != null && EXTERNAL_PARENTS.contains(rule.parent());
    }

    public static boolean validSelection(Collection<String> enabled) {
        return RULES.stream()
            .allMatch(
                r -> !enabled.contains(r.child()) || EXTERNAL_PARENTS.contains(r.parent())
                    || enabled.contains(r.parent()));
    }

    public static String select(String parent, long provinceHash, Collection<String> enabled) {
        if (parent == null || !enabled.contains(parent)) return parent;
        if (parent.equals(BopNativeBiomes.SLUDGEPIT)) {
            // Equal native sibling weights, disjoint whole-province quarters. An
            // unavailable child leaves its bucket to the parent, not its sibling.
            int bucket = (int) Math.floorMod(WorldgenContext.mix64(provinceHash ^ 0x424f50534c554431L), 4);
            String child = bucket == 0 ? BopNativeBiomes.QUAGMIRE : bucket == 1 ? BopNativeBiomes.SILKGLADES : null;
            return child != null && enabled.contains(child) ? child : parent;
        }
        for (var rule : RULES) if (rule.parent()
            .equals(parent) && enabled.contains(rule.child())) {
                // Intentional hybrid policy: preserve a whole parent landform, with one quarter of its
                // provinces using the native sub-biome ecology. BOP's tiny GenLayer/noise patches would
                // bisect our arches/islands. This is not a replay of BOP's source biome map.
                long hash = WorldgenContext.mix64(
                    provinceHash ^ rule.child()
                        .hashCode() ^ 0x424f505355423031L);
                if (Math.floorMod(hash, 4) == 0) return rule.child();
            }
        return parent;
    }

    /** Whole-source-parent equivalent for native children whose vanilla parent is not a selectable BOP profile. */
    public static String selectExternal(String parent, long provinceHash, Collection<String> enabled) {
        if (parent == null || !EXTERNAL_PARENTS.contains(parent)) return parent;
        List<Rule> children = RULES.stream()
            .filter(
                r -> r.parent()
                    .equals(parent))
            .toList();
        if (children.isEmpty()) return parent;
        int bucket = (int) Math
            .floorMod(WorldgenContext.mix64(provinceHash ^ parent.hashCode() ^ 0x424f504558543031L), 4);
        if (bucket >= children.size()) return parent;
        String child = children.get(bucket)
            .child();
        return enabled.contains(child) ? child : parent;
    }

    public static String identity(Rule rule) {
        return "|native-parent=" + rule.parent()
            + "|source-sub-zoom="
            + rule.sourceZoom()
            + "|source-sub-threshold="
            + rule.sourceThreshold()
            + (rule.parent()
                .equals(BopNativeBiomes.SLUDGEPIT) || EXTERNAL_PARENTS.contains(rule.parent())
                    ? "|whole-parent-disjoint-sibling-quarters-v1"
                    : "|whole-parent-province-quarter-v1");
    }
}
