package industries.angelic.biomic.world.terrain;

/**
 * How sky islands choose their habitats, pinned per world (SkyIslandsWorldData).
 *
 * @param ownBiomes an island may differ from the ground below it; when false, an island over land takes the ground's
 *                  habitat (islands over water always choose their own)
 * @param variety   the percentage of a group's islands that choose a habitat of their own rather than the group's
 */
public record IslandBiomeRules(boolean ownBiomes, int variety) {

    /** Every island chooses for itself, over land or water. */
    public static final IslandBiomeRules DEFAULT = new IslandBiomeRules(true, 100);

    public IslandBiomeRules {
        if (variety < 0 || variety > 100)
            throw new IllegalArgumentException("Island biome variety outside 0..100: " + variety);
    }
}
