package industries.angelic.biomic.world.modern;

/** Local paired-plant shape updates after a generation write; preserve the water, never spawn drops. */
public final class ModernAquaticShapeUpdates {

    private ModernAquaticShapeUpdates() {}

    public static boolean upper(String state) {
        return state.startsWith("minecraft:tall_seagrass[") && state.contains("half=upper");
    }

    public static boolean lower(String state) {
        return state.startsWith("minecraft:tall_seagrass[") && state.contains("half=lower");
    }

    public static boolean brokenByNeighbor(String plant, String neighbor, boolean neighborAbove) {
        return neighborAbove ? lower(plant) && !upper(neighbor) : upper(plant) && !lower(neighbor);
    }
}
