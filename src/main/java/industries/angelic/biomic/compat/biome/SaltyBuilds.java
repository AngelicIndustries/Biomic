package industries.angelic.biomic.compat.biome;

/**
 * SaltyMod Expanded builds by the version string they report at run time (their mcmod.info says 2.0.4 for all). No
 * Minecraft types here: core.Mixins reads these while it chooses mixins, before game classes may load.
 */
public final class SaltyBuilds {

    public static final String V2_0_8 = "2.0.0-master.150+9a0954aa6f";
    public static final String V2_1_2 = "2.0.0-master.162+2a8955c302-dirty";
    public static final String V2_1_3 = "2.0.0-master.164+575583cf7f";

    private SaltyBuilds() {}
}
