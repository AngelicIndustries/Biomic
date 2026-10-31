package industries.angelic.biomic.compat.efr;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Et Futurum Requiem's tile entities and bee, which Biomic configures as it generates them: recognised by class name
 * (as instanceof would, superclasses included), so nothing of EFR loads unless EFR is installed.
 */
public final class EfrTiles {

    /** Whether Et Futurum Requiem is installed: FML's mod list, or its classes where FML is not running (tests). */
    public static final boolean PRESENT = detect();
    private static final String TILES = "ganymedes01.etfuturum.tileentities.";

    private EfrTiles() {}

    public static boolean isGlowLichen(TileEntity tile) {
        return PRESENT && is(tile, TILES + "TileEntityGlowLichen");
    }

    public static boolean isBeeHive(TileEntity tile) {
        return PRESENT && is(tile, TILES + "TileEntityBeeHive");
    }

    public static boolean isCaveVines(TileEntity tile) {
        return PRESENT && is(tile, TILES + "TileEntityCaveVines");
    }

    /** A new EFR bee, or null without EFR. */
    public static Entity bee(World world) {
        return PRESENT ? Present.bee(world) : null;
    }

    private static boolean detect() {
        try {
            return cpw.mods.fml.common.Loader.isModLoaded("etfuturum");
        } catch (RuntimeException noLoader) {
            try {
                Class.forName("ganymedes01.etfuturum.ModBlocks", false, EfrTiles.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException absent) {
                return false;
            }
        }
    }

    private static boolean is(Object object, String className) {
        for (Class<?> type = object == null ? null : object.getClass(); type != null; type = type.getSuperclass())
            if (type.getName()
                .equals(className)) return true;
        return false;
    }

    /** Loaded only with EFR present. */
    private static final class Present {

        static Entity bee(World world) {
            return new ganymedes01.etfuturum.entities.EntityBee(world);
        }
    }
}
