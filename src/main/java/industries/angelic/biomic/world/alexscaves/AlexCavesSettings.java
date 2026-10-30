package industries.angelic.biomic.world.alexscaves;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/** Save-pinned, opt-in Alex's Caves content manifest. */
public record AlexCavesSettings(String forlornProvider, String primordialProvider, String abyssalProvider) {

    public static final String NONE = "none";
    public static final String FORLORN_V1 = "alexscaves:forlorn-canyon-legacy-v1";
    public static final String PRIMORDIAL_V1 = "alexscaves:primordial-bowl-legacy-v1";
    public static final String ABYSSAL_V1 = "alexscaves:ocean-trench-legacy-v1";
    public static final String PRIMORDIAL_V2 = "alexscaves:primordial-bowl-legacy-v2";
    public static final String PRIMORDIAL_V3 = "alexscaves:primordial-bowl-legacy-v3";
    public static final String ABYSSAL_V2 = "alexscaves:ocean-trench-legacy-v2";
    public static final String ABYSSAL_NATURAL_V3 = "alexscaves:ocean-trench-natural-v3";
    /** Chunk-local natural trenches over compressed 256 and uncompressed (shifted512) Origen. */
    public static final String ABYSSAL_CHUNK = "alexscaves:ocean-trench-chunk-v1";
    public static final String ABYSSAL_SHIFTED512 = "alexscaves:ocean-trench-chunk-shifted512-v1";
    public static final AlexCavesSettings OFF = new AlexCavesSettings(NONE, NONE, NONE);

    public AlexCavesSettings(String forlornProvider, String primordialProvider) {
        this(forlornProvider, primordialProvider, NONE);
    }

    public AlexCavesSettings {
        if (!NONE.equals(forlornProvider) && !FORLORN_V1.equals(forlornProvider))
            throw new IllegalArgumentException("Unsupported saved Alex's Caves provider: " + forlornProvider);
        if (!NONE.equals(primordialProvider) && !PRIMORDIAL_V1.equals(primordialProvider)
            && !PRIMORDIAL_V2.equals(primordialProvider)
            && !PRIMORDIAL_V3.equals(primordialProvider))
            throw new IllegalArgumentException("Unsupported saved Primordial provider: " + primordialProvider);
        if (!NONE.equals(abyssalProvider) && !ABYSSAL_V1.equals(abyssalProvider)
            && !ABYSSAL_V2.equals(abyssalProvider)
            && !ABYSSAL_NATURAL_V3.equals(abyssalProvider)
            && !ABYSSAL_CHUNK.equals(abyssalProvider)
            && !ABYSSAL_SHIFTED512.equals(abyssalProvider))
            throw new IllegalArgumentException("Unsupported saved Abyssal provider: " + abyssalProvider);
        if ((ABYSSAL_NATURAL_V3.equals(abyssalProvider) || ABYSSAL_CHUNK.equals(abyssalProvider)
            || ABYSSAL_SHIFTED512.equals(abyssalProvider))
            && (!NONE.equals(forlornProvider) || !NONE.equals(primordialProvider)))
            throw new IllegalArgumentException(
                "Natural-only Abyssal manifest cannot select retired Alex's Caves hosts");
    }

    public boolean forlornEnabled() {
        return FORLORN_V1.equals(forlornProvider);
    }

    public boolean primordialEnabled() {
        return PRIMORDIAL_V1.equals(primordialProvider) || primordialReviewFixesEnabled();
    }

    /** Any 256 trench; earlier whole-body revisions now generate the same chunk-local trench. */
    public boolean abyssalEnabled() {
        return !NONE.equals(abyssalProvider) && !ABYSSAL_SHIFTED512.equals(abyssalProvider);
    }

    public boolean primordialReviewFixesEnabled() {
        return PRIMORDIAL_V2.equals(primordialProvider) || PRIMORDIAL_V3.equals(primordialProvider);
    }

    /** The 512 trench; the compressed-256 runtime keys off {@link #abyssalEnabled()}. */
    public boolean abyssalShifted512() {
        return ABYSSAL_SHIFTED512.equals(abyssalProvider);
    }

    public static AlexCavesSettings select(boolean newWorld, boolean blend, boolean abyssal, boolean tall) {
        // Forlorn and Primordial are retired from fresh selection, even with stale opt-ins. All saved
        // providers and registered IDs remain readable; only fresh Abyssal uses the natural-only revision.
        if (!newWorld || !blend || !abyssal) return OFF;
        return new AlexCavesSettings(NONE, NONE, tall ? ABYSSAL_SHIFTED512 : ABYSSAL_CHUNK);
    }

    public void write(NBTTagCompound nbt) {
        nbt.setInteger("schema", 3);
        nbt.setString("forlornProvider", forlornProvider);
        nbt.setString("primordialProvider", primordialProvider);
        nbt.setString("abyssalProvider", abyssalProvider);
    }

    public static AlexCavesSettings read(NBTTagCompound nbt) {
        if (!nbt.hasKey("schema", Constants.NBT.TAG_INT) || !nbt.hasKey("forlornProvider", Constants.NBT.TAG_STRING))
            throw new IllegalArgumentException("Invalid saved Alex's Caves manifest");
        int schema = nbt.getInteger("schema");
        if (schema == 1) return new AlexCavesSettings(nbt.getString("forlornProvider"), NONE, NONE);
        if (schema == 2 && nbt.hasKey("primordialProvider", Constants.NBT.TAG_STRING))
            return new AlexCavesSettings(nbt.getString("forlornProvider"), nbt.getString("primordialProvider"), NONE);
        if (schema != 3 || !nbt.hasKey("primordialProvider", Constants.NBT.TAG_STRING)
            || !nbt.hasKey("abyssalProvider", Constants.NBT.TAG_STRING))
            throw new IllegalArgumentException("Invalid saved Alex's Caves manifest");
        return new AlexCavesSettings(
            nbt.getString("forlornProvider"),
            nbt.getString("primordialProvider"),
            nbt.getString("abyssalProvider"));
    }
}
