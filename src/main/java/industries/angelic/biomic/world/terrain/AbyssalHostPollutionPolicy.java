package industries.angelic.biomic.world.terrain;

import java.util.Set;

/** Origen features displaced by the source-authored Abyssal Chasm environment. */
public final class AbyssalHostPollutionPolicy {

    private static final Set<String> DISPLACED = Set.of(
        "LAVA_FLOOR",
        "UNDERGROUND_LAVA_COLUMNS",
        "CAVE_GLOW_LICHEN",
        "TEXTURED_STONE_SLANT",
        "FOSSIL_OCEAN",
        "DENDRIFORM_REEF",
        "BALL_REEF",
        "CORAL_PLANTS",
        "CORAL_FANS",
        "KELP",
        "SEAGRASS");

    private AbyssalHostPollutionPolicy() {}

    /**
     * Keep Origen's ore and deposit stages: the pinned biome includes vanilla ores and underwater magma.
     * The listed global/cave/vegetation features instead describe Origen's Abyssal Alleys, not Alex's Caves.
     */
    public static boolean displaced(String stage, String feature) {
        return DISPLACED.contains(feature) || switch (stage) {
            case "landforms", "trees", "underwater-flora", "flora" -> true;
            default -> false;
        };
    }
}
