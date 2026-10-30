package industries.angelic.biomic.world.structure;

import net.minecraft.world.World;

import industries.angelic.biomic.world.TallWorlds;

/**
 * Village Names builds its ocean monuments for sea level 63 (y 39..61, water up to 63). In a 512 Biomic world the
 * sea is 128 blocks higher, so the monument, its water and its elder guardians are raised by as much, as vanilla's
 * structures are there.
 */
public final class VillageNamesMonuments {

    private static final ThreadLocal<Integer> BUILDING = new ThreadLocal<>();

    private VillageNamesMonuments() {}

    /** How far a monument in this world is raised. */
    public static int lift(World world) {
        return TallWorlds.liveTall(world) ? 128 : 0;
    }

    /** A monument start is being laid out in this world (its building piece has no world to ask). */
    public static void begin(World world) {
        BUILDING.set(lift(world));
    }

    public static void end() {
        BUILDING.remove();
    }

    /** The lift of the monument being laid out on this thread. */
    public static int buildingLift() {
        Integer lift = BUILDING.get();
        return lift == null ? 0 : lift;
    }
}
