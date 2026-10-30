package industries.angelic.biomic.content.alexscaves;

/** Natural vent rules from Alex's Caves 4718f428; acid/radon behavior is deliberately outside this port. */
public final class AbyssalVentBehavior {

    private AbyssalVentBehavior() {}

    public enum Fluid {
        NONE,
        WATER,
        LAVA
    }

    public static int refreshedMetadata(int before, Integer belowVent, Fluid belowFluid, boolean aboveVent,
        boolean aboveBlocksMotion) {
        if ((before & 3) == 3) return before; // Do not reinterpret legacy acid states as a supported natural emitter.
        int smoke = belowVent != null ? belowVent & 3
            : belowFluid == Fluid.WATER ? 1 : belowFluid == Fluid.LAVA ? 2 : 0;
        return (before & 8) | smoke | (!aboveVent && !aboveBlocksMotion ? 4 : 0);
    }

    public static boolean emits(int metadata) {
        return (metadata & 4) != 0 && ((metadata & 3) == 1 || (metadata & 3) == 2);
    }

    public static int lifetime(double random) {
        return 40 + (int) (random * 20);
    }

    public static float alpha(int age, int lifetime) {
        return age > lifetime / 2 ? 1F - (age - (float) (lifetime / 2)) / lifetime : 1F;
    }

    public static float growth(int age, float partial, int lifetime) {
        return Math.max(0, Math.min(1, (age + partial) / lifetime * 4));
    }

    public static boolean vent(BlockAbyssalAquatic.Kind kind) {
        return kind == BlockAbyssalAquatic.Kind.VENT_FULL || kind == BlockAbyssalAquatic.Kind.VENT_MEDIUM
            || kind == BlockAbyssalAquatic.Kind.VENT_THIN;
    }
}
