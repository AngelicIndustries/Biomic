package industries.angelic.biomic.compat.biome;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import industries.angelic.biomic.config.ExpansionConfig;

/** Normal world initialization only, never read-only worldbuilder inspection. */
public final class NativeHabitatWorldData extends WorldSavedData {

    public static final String NAME = "biomic_native_habitats";
    private NativeHabitatSettings settings;
    private String failure;

    public NativeHabitatWorldData(String name) {
        super(name);
    }

    public static void initialize(World world, boolean newWorld) {
        if (world.isRemote || !"biomic_blend".equals(
            world.getWorldInfo()
                .getTerrainType()
                .getWorldTypeName()))
            return;
        // Hohen Angst worlds host native habitats on uncompressed (shifted512) Origen.
        boolean shifted = industries.angelic.biomic.world.TallWorlds.liveTall(world);
        NativeHabitatWorldData data = (NativeHabitatWorldData) world.mapStorage
            .loadData(NativeHabitatWorldData.class, NAME);
        if (data == null) {
            data = new NativeHabitatWorldData(NAME);
            data.settings = initialSettings(
                shifted,
                newWorld,
                ExpansionConfig.saltyMarsh && SaltMarshCompat.available(),
                ExpansionConfig.bopHabitats,
                ExpansionConfig.bopFullCatalogue,
                BopNativeBiomes.enabledKeys());
            world.mapStorage.setData(NAME, data);
            data.markDirty();
        }
        if (data.requireSettings()
            .shifted512() && !shifted) throw new IllegalStateException("Shifted native habitats require a tall world");
        if (shifted && !data.requireSettings()
            .shifted512()
            && (data.requireSettings()
                .saltMarsh()
                || data.requireSettings()
                    .bop()))
            throw new IllegalStateException("A tall world cannot reinterpret a 256 native habitat manifest");
        if (data.requireSettings()
            .saltMarsh() && !SaltMarshCompat.available())
            throw new IllegalStateException(
                "Saved habitat requires the pinned native Salty provider: " + SaltMarshCompat.status());
        if (data.requireSettings()
            .bop()
            && !data.settings.bopManifest()
                .equals(BopNativeBiomes.manifest(data.settings.bopHabitats())))
            throw new IllegalStateException(
                "Saved BOP habitat manifest no longer matches installed native biome settings: "
                    + BopNativeBiomes.status());
    }

    static NativeHabitatSettings initialSettings(boolean shifted, boolean newWorld, boolean saltyAvailable,
        boolean bopEnabled, boolean fullCatalogue, java.util.Collection<String> availableBop) {
        if (!newWorld) return NativeHabitatSettings.OFF;
        NativeHabitatSettings selected = saltyAvailable ? NativeHabitatSettings.SALT : NativeHabitatSettings.OFF;
        if (shifted) selected = selected.shifted512Revision();
        if (bopEnabled) {
            var habitats = BopBiomeSelection.surface(availableBop, fullCatalogue);
            if (!habitats.isEmpty()) selected = selected.withBop(BopNativeBiomes.manifest(habitats), habitats);
        }
        return selected;
    }

    public static NativeHabitatSettings peek(World world) {
        if (world == null || world.isRemote || world.mapStorage == null) return NativeHabitatSettings.OFF;
        java.util.Map<?, ?> loaded = cpw.mods.fml.relauncher.ReflectionHelper
            .getPrivateValue(MapStorage.class, world.mapStorage, "loadedDataMap", "field_75749_b");
        Object value = loaded.get(NAME);
        return value instanceof NativeHabitatWorldData ? ((NativeHabitatWorldData) value).requireSettings()
            : NativeHabitatSettings.OFF;
    }

    private NativeHabitatSettings requireSettings() {
        if (settings == null) throw new IllegalStateException("Invalid native habitat settings: " + failure);
        return settings;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        try {
            settings = NativeHabitatSettings.read(nbt);
            failure = null;
        } catch (RuntimeException error) {
            settings = null;
            failure = error.getMessage();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        requireSettings().write(nbt);
    }
}
