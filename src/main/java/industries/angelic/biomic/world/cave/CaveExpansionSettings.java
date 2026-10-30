package industries.angelic.biomic.world.cave;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/** Save-pinned Ultimate Caves geometry and ecology choices. Geometry changes require a new provider id. */
public record CaveExpansionSettings(String provider, boolean nativeEcology, int lichenSupportRevision) {

    public CaveExpansionSettings(String provider) {
        this(provider, false, 0);
    }

    public CaveExpansionSettings(String provider, boolean nativeEcology) {
        this(provider, true, 0);
    }

    public static final String WORLEY_V1 = "ultimatecaves:protected-worley-v1";
    public static final String WORLEY_V2 = "ultimatecaves:protected-worley-v2";
    /** V2 clearance caves over uncompressed (shifted512) Origen. */
    public static final String WORLEY_SHIFTED512 = "ultimatecaves:protected-worley-shifted512-v1";
    /** One field: Ultimate Worley, Ultimate chambers and YUNG (liquid) caverns weighted by sparse regions. */
    public static final String REGIONAL = "biomic:ultimate-yung-regional-v1";
    public static final String REGIONAL_SHIFTED512 = "biomic:ultimate-yung-regional-shifted512-v1";
    public static final CaveExpansionSettings OFF = new CaveExpansionSettings("none");
    public static final CaveExpansionSettings ROOMY_WORLEY = new CaveExpansionSettings(WORLEY_V2);
    public static final CaveExpansionSettings SHIFTED_WORLEY = new CaveExpansionSettings(WORLEY_SHIFTED512);
    public static final CaveExpansionSettings REGIONAL_CAVES = new CaveExpansionSettings(REGIONAL);
    public static final CaveExpansionSettings SHIFTED_REGIONAL_CAVES = new CaveExpansionSettings(REGIONAL_SHIFTED512);

    public CaveExpansionSettings {
        if (!"none".equals(provider) && !WORLEY_V1.equals(provider)
            && !WORLEY_V2.equals(provider)
            && !WORLEY_SHIFTED512.equals(provider)
            && !REGIONAL.equals(provider)
            && !REGIONAL_SHIFTED512.equals(provider))
            throw new IllegalArgumentException("Unsupported saved cave provider: " + provider);
        if (nativeEcology && (WORLEY_V1.equals(provider) || "none".equals(provider)))
            throw new IllegalArgumentException("Native cave ecology requires a clearance cave provider");
        if (lichenSupportRevision < 0 || lichenSupportRevision > 1)
            throw new IllegalArgumentException("Unsupported cave lichen support revision: " + lichenSupportRevision);
    }

    public boolean enabled() {
        return !provider.equals("none");
    }

    public boolean roomy() {
        return enabled() && !WORLEY_V1.equals(provider);
    }

    public boolean shifted512() {
        return WORLEY_SHIFTED512.equals(provider) || REGIONAL_SHIFTED512.equals(provider);
    }

    public boolean regional() {
        return REGIONAL.equals(provider) || REGIONAL_SHIFTED512.equals(provider);
    }

    public static CaveExpansionSettings select(boolean newWorld, boolean blend, boolean enabled, boolean yung) {
        return newWorld && enabled ? (yung ? REGIONAL_CAVES : ROOMY_WORLEY) : OFF;
    }

    public static CaveExpansionSettings selectShifted512(boolean newWorld, boolean enabled, boolean yung) {
        return newWorld && enabled ? (yung ? SHIFTED_REGIONAL_CAVES : SHIFTED_WORLEY) : OFF;
    }

    public void write(NBTTagCompound nbt) {
        nbt.setInteger("schema", lichenSupportRevision == 0 ? 2 : 3);
        nbt.setString("provider", provider);
        nbt.setBoolean("nativeEcology", nativeEcology);
        if (lichenSupportRevision != 0) nbt.setInteger("lichenSupportRevision", lichenSupportRevision);
    }

    public static CaveExpansionSettings read(NBTTagCompound nbt) {
        int schema = nbt.getInteger("schema");
        if (!nbt.hasKey("schema", Constants.NBT.TAG_INT) || schema < 1
            || schema > 3
            || schema >= 2 && !nbt.hasKey("nativeEcology", Constants.NBT.TAG_BYTE)
            || schema >= 3 && (!nbt.hasKey("lichenSupportRevision", Constants.NBT.TAG_INT)
                || nbt.getInteger("lichenSupportRevision") != 1)
            || !nbt.hasKey("provider", Constants.NBT.TAG_STRING))
            throw new IllegalArgumentException("Invalid cave expansion manifest");
        return new CaveExpansionSettings(
            nbt.getString("provider"),
            schema >= 2 && nbt.getBoolean("nativeEcology"),
            schema >= 3 ? nbt.getInteger("lichenSupportRevision") : 0);
    }
}
