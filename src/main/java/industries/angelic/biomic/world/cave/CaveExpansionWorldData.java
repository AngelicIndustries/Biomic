package industries.angelic.biomic.world.cave;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import industries.angelic.biomic.config.ExpansionConfig;
import industries.angelic.biomic.world.WorldTypeBiomicBlend;

/** Loaded only by normal world initialization; old worlds never opt themselves into a new carver. */
public final class CaveExpansionWorldData extends WorldSavedData {

    public static final String NAME = "biomic_cave_expansion";
    private CaveExpansionSettings settings;
    private String failure;

    public CaveExpansionWorldData(String name) {
        super(name);
    }

    public static void initialize(World world, boolean newWorld) {
        if (world.isRemote || !WorldTypeBiomicBlend.ID.equals(
            world.getWorldInfo()
                .getTerrainType()
                .getWorldTypeName()))
            return;
        CaveExpansionWorldData data = (CaveExpansionWorldData) world.mapStorage
            .loadData(CaveExpansionWorldData.class, NAME);
        if (data == null) {
            data = new CaveExpansionWorldData(NAME);
            data.settings = industries.angelic.biomic.world.TallWorlds.liveTall(world)
                ? CaveExpansionSettings
                    .selectShifted512(newWorld, ExpansionConfig.ultimateCaves, ExpansionConfig.yungCaves)
                : CaveExpansionSettings
                    .select(newWorld, true, ExpansionConfig.ultimateCaves, ExpansionConfig.yungCaves);
            if (data.settings.roomy() && ExpansionConfig.efrCaveEcology)
                data.settings = new CaveExpansionSettings(data.settings.provider(), true);
            if (newWorld)
                data.settings = new CaveExpansionSettings(data.settings.provider(), data.settings.nativeEcology(), 1);
            world.mapStorage.setData(NAME, data);
            data.markDirty();
        }
        data.requireSettings();
    }

    public static CaveExpansionSettings peek(World world) {
        if (world == null || world.isRemote || world.mapStorage == null) return CaveExpansionSettings.OFF;
        java.util.Map<?, ?> loaded = cpw.mods.fml.relauncher.ReflectionHelper
            .getPrivateValue(MapStorage.class, world.mapStorage, "loadedDataMap", "field_75749_b");
        Object data = loaded.get(NAME);
        return data instanceof CaveExpansionWorldData ? ((CaveExpansionWorldData) data).requireSettings()
            : CaveExpansionSettings.OFF;
    }

    private CaveExpansionSettings requireSettings() {
        if (settings == null) throw new IllegalStateException("Invalid saved cave expansion: " + failure);
        return settings;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        try {
            settings = CaveExpansionSettings.read(nbt);
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
