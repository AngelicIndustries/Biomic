package industries.angelic.biomic.world.terrain;

import java.util.Set;

/**
 * Origen and hosted features the overworld does not generate (review, 2026-10-23): bone structures ("remove them
 * altogether, except from the nether") and boulders ("I don't like this stone terrain feature, remove it, same for
 * mossy
 * ones", then "remove the granite boulders too"). The Nether's own bones are Tartarus features and stay.
 */
final class RemovedFeatures {

    private static final Set<String> REMOVED = Set.of(
        // Origen: rib cages over the fens, bones on the sea floor.
        "FOSSIL_RIBS",
        "FOSSIL_OCEAN",
        // Origen: every boulder, stone, mossy, granite or sandstone ("remove the granite boulders too").
        "BOULDERS",
        "SMALL_BOULDER_PATCHES",
        "GIANT_BOULDERS",
        "MOSSY_BOULDERS",
        "SMALL_MOSSY_BOULDER_PATCHES",
        "GRANITE_BOULDERS",
        "GRANITE_BOULDERS_CANYON",
        "SMALL_GRANITE_BOULDER_PATCHES",
        "SANDSTONE_BOULDERS",
        "SMALL_SANDSTONE_BOULDER_PATCHES",
        // Hosted habitats: vanilla's buried and exposed fossils, and its mossy forest rocks.
        "minecraft:fossil_upper",
        "minecraft:fossil_lower",
        "minecraft:forest_rock");

    private RemovedFeatures() {}

    static boolean removed(String feature) {
        // RU's cobblestone and mossy rock groups (highland fields, maple forests, tundra, icy heights).
        return REMOVED.contains(feature) || feature.startsWith("regions_unexplored:rock/");
    }
}
