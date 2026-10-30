package industries.angelic.biomic.world.terrain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The habitat of each sky island, by the world's {@link IslandBiomeRules}: its own pick (HostedIslandBiomes), one per
 * island or group, or, with {@code ownBiomes} off, the ground's most common below it (sampled over the island; over
 * water an island keeps its own pick). One answer per island, remembered.
 */
public final class SkyIslandHabitats {

    private static final int CACHE = 4096;

    private final FloatingIslandDensitySampler islands;
    private final IslandBiomeRules rules;
    private final List<String> nativeBop;
    private final Map<Long, String> chosen = new ConcurrentHashMap<>();
    private volatile RegionalHabitatSampler ground;

    SkyIslandHabitats(FloatingIslandDensitySampler islands, IslandBiomeRules rules, List<String> nativeBop) {
        this.islands = islands;
        this.rules = rules;
        this.nativeBop = nativeBop;
    }

    /** The mainland's habitats, which islands follow when the rules say so. */
    void setGround(RegionalHabitatSampler ground) {
        this.ground = ground;
    }

    /** The habitat of the island holding column (x, z), or null. */
    String at(int x, int z) {
        FloatingIslandDensitySampler.HabitatSite site = islands.habitatSite(x, z, rules.variety());
        if (site == null) return null;
        String known = chosen.get(site.key());
        if (known != null) return known;
        String habitat = HostedIslandBiomes.habitat(site.archetype(), site.key(), nativeBop);
        if (habitat == null) return null;
        if (!rules.ownBiomes()) {
            String below = below(site);
            if (below != null) habitat = below;
        }
        if (chosen.size() > CACHE) chosen.clear();
        chosen.put(site.key(), habitat);
        return habitat;
    }

    /**
     * The ground habitat under most of an island (RegionalHabitatSampler#skyHabitat), or null where most of it hangs
     * over water or ground no habitat matches.
     */
    private String below(FloatingIslandDensitySampler.HabitatSite site) {
        RegionalHabitatSampler sampler = ground;
        if (sampler == null) throw new IllegalStateException("Sky islands follow the ground, but no ground was given");
        Map<String, Integer> votes = new LinkedHashMap<>();
        for (int[] point : islands.habitatPoints(site, rules.variety())) {
            String habitat = sampler.skyHabitat(point[0], point[1]);
            votes.merge(habitat == null ? "" : habitat, 1, Integer::sum);
        }
        String best = null;
        for (Map.Entry<String, Integer> vote : votes.entrySet())
            if (best == null || vote.getValue() > votes.get(best)) best = vote.getKey();
        return best == null || best.isEmpty() ? null : best;
    }
}
