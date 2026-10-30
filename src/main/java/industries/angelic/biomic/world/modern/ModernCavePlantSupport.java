package industries.angelic.biomic.world.modern;

/** RU cave plants override soil tags with the actual solid-render support test. */
public final class ModernCavePlantSupport {

    private ModernCavePlantSupport() {}

    public static boolean handles(String state) {
        return switch (ModernDecorationEngine.blockName(state)) {
            case "regions_unexplored:prismoss_sprout", "regions_unexplored:cave_hyssop", "regions_unexplored:prismarite_cluster", "regions_unexplored:large_prismarite_cluster", "regions_unexplored:corpse_flower" -> true;
            default -> false;
        };
    }

    public static boolean test(String state, String below, boolean opaque) {
        if (state.contains("half=upper")) return ModernDecorationEngine.blockName(state)
            .equals(ModernDecorationEngine.blockName(below)) && below.contains("half=lower");
        return opaque;
    }
}
