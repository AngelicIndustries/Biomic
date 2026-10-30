package industries.angelic.biomic.world.terrain.gardens;

/** Canonical-state survival rules kept pure so source semantics are testable before FML bootstrap. */
final class GardensPlantRules {

    private GardensPlantRules() {}

    /** Pinned weighted provider: 48 sprouts, 48 native fungus, 2 crimson fungus, 2 warped fungus. */
    static int soulblightVegetationSlot(int value) {
        if (value < 0 || value >= 100)
            throw new IllegalArgumentException("Soulblight vegetation roll outside [0,100): " + value);
        return value < 48 ? 0 : value < 96 ? 1 : value < 98 ? 2 : 3;
    }

    static boolean sourceFungusSurvivesOn(String plant, String below) {
        boolean fungus = plant.startsWith("minecraft:crimson_fungus") || plant.startsWith("minecraft:warped_fungus");
        return fungus && (below.startsWith("minecraft:soul_soil") || below.startsWith("minecraft:crimson_nylium")
            || below.startsWith("minecraft:warped_nylium"));
    }
}
