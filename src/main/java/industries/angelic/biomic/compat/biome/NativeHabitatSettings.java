package industries.angelic.biomic.compat.biome;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/** New-save native habitat ownership. A discovered biome is not automatically selected. */
public record NativeHabitatSettings(String saltMarshVersion, int marshRevision, String bopManifest,
    java.util.List<String> bopHabitats, String heightRevision) {

    public static final String SHIFTED_512_REVISION = "native-habitats-shifted512-v1";
    public static final NativeHabitatSettings OFF = new NativeHabitatSettings("");
    public static final NativeHabitatSettings SALT = new NativeHabitatSettings(SaltMarshCompat.VERSION, 2);

    /** Existing callers and schema-1 saves retain the original geometry. */
    public NativeHabitatSettings(String version) {
        this(version, version.isEmpty() ? 0 : 1);
    }

    public NativeHabitatSettings(String version, int revision) {
        this(version, revision, "", java.util.List.of(), "");
    }

    public NativeHabitatSettings(String version, int revision, String manifest, java.util.List<String> habitats) {
        this(version, revision, manifest, habitats, "");
    }

    public NativeHabitatSettings {
        if (!"".equals(saltMarshVersion) && !SaltMarshCompat.VERSION.equals(saltMarshVersion))
            throw new IllegalArgumentException("Unsupported saved Salty habitat version: " + saltMarshVersion);
        if (saltMarshVersion.isEmpty() ? marshRevision != 0 : marshRevision < 1 || marshRevision > 2)
            throw new IllegalArgumentException("Unsupported native marsh revision: " + marshRevision);
        if (bopManifest == null || !bopManifest.isEmpty() && !bopManifest.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Invalid BOP manifest fingerprint");
        if (bopHabitats == null || bopHabitats.size() > BopNativeBiomes.supportedKeys()
            .size()
            || !BopNativeBiomes.supportedKeys()
                .containsAll(bopHabitats)
            || new java.util.HashSet<>(bopHabitats).size() != bopHabitats.size()
            || bopManifest.isEmpty() != bopHabitats.isEmpty())
            throw new IllegalArgumentException("Invalid BOP habitat selection");
        bopHabitats = bopHabitats.stream()
            .sorted()
            .toList();
        if (!BopSubBiomePolicy.validSelection(bopHabitats))
            throw new IllegalArgumentException("Native BOP sub-biome requires its saved parent");
        if (heightRevision == null || !heightRevision.isEmpty() && !SHIFTED_512_REVISION.equals(heightRevision))
            throw new IllegalArgumentException("Unsupported native habitat height revision: " + heightRevision);
    }

    public boolean saltMarsh() {
        return !saltMarshVersion.isEmpty();
    }

    public boolean fullMarsh() {
        return marshRevision == 2;
    }

    public boolean bop() {
        return !bopManifest.isEmpty();
    }

    public boolean shifted512() {
        return SHIFTED_512_REVISION.equals(heightRevision);
    }

    public NativeHabitatSettings withBop(String manifest, java.util.Collection<String> habitats) {
        return new NativeHabitatSettings(
            saltMarshVersion,
            marshRevision,
            manifest,
            java.util.List.copyOf(habitats),
            heightRevision);
    }

    public NativeHabitatSettings shifted512Revision() {
        return new NativeHabitatSettings(
            saltMarshVersion,
            marshRevision,
            bopManifest,
            bopHabitats,
            SHIFTED_512_REVISION);
    }

    public void write(NBTTagCompound nbt) {
        nbt.setInteger("schema", heightRevision.isEmpty() ? 3 : 4);
        nbt.setString("saltMarshVersion", saltMarshVersion);
        nbt.setInteger("marshRevision", marshRevision);
        nbt.setString("bopManifest", bopManifest);
        nbt.setString("bopHabitats", String.join(",", bopHabitats));
        if (!heightRevision.isEmpty()) nbt.setString("heightRevision", heightRevision);
    }

    public static NativeHabitatSettings read(NBTTagCompound nbt) {
        if (!nbt.hasKey("schema", Constants.NBT.TAG_INT) || nbt.getInteger("schema") < 1
            || nbt.getInteger("schema") > 4
            || !nbt.hasKey("saltMarshVersion", Constants.NBT.TAG_STRING))
            throw new IllegalArgumentException("Invalid native habitat manifest");
        if (nbt.getInteger("schema") == 1) return new NativeHabitatSettings(nbt.getString("saltMarshVersion"));
        if (!nbt.hasKey("marshRevision", Constants.NBT.TAG_INT))
            throw new IllegalArgumentException("Missing marsh revision");
        if (nbt.getInteger("schema") == 2)
            return new NativeHabitatSettings(nbt.getString("saltMarshVersion"), nbt.getInteger("marshRevision"));
        if (!nbt.hasKey("bopManifest", Constants.NBT.TAG_STRING)
            || !nbt.hasKey("bopHabitats", Constants.NBT.TAG_STRING))
            throw new IllegalArgumentException("Missing BOP manifest");
        String habitats = nbt.getString("bopHabitats");
        if (nbt.getInteger("schema") == 4 && !nbt.hasKey("heightRevision", Constants.NBT.TAG_STRING))
            throw new IllegalArgumentException("Missing native habitat height revision");
        if (nbt.getInteger("schema") == 4 && nbt.getString("heightRevision")
            .isEmpty()) throw new IllegalArgumentException("Empty native habitat height revision");
        if (nbt.getInteger("schema") < 4 && nbt.hasKey("heightRevision"))
            throw new IllegalArgumentException("Legacy native habitat gained a height revision");
        return new NativeHabitatSettings(
            nbt.getString("saltMarshVersion"),
            nbt.getInteger("marshRevision"),
            nbt.getString("bopManifest"),
            habitats.isEmpty() ? java.util.List.of() : java.util.List.of(habitats.split(",", -1)),
            nbt.getInteger("schema") == 4 ? nbt.getString("heightRevision") : "");
    }
}
