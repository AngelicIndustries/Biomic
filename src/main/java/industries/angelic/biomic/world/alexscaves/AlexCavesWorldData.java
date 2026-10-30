package industries.angelic.biomic.world.alexscaves;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import industries.angelic.biomic.config.ExpansionConfig;

/** Separate save manifest: absence in an old save always means disabled, and saved provider revisions never upgrade. */
public final class AlexCavesWorldData extends WorldSavedData {

    public static final String NAME = "biomic_alex_caves";
    private AlexCavesSettings settings;
    private String failure;

    public AlexCavesWorldData(String name) {
        super(name);
    }

    public static void initialize(World world, boolean newWorld) {
        String type = world.getWorldInfo()
            .getTerrainType()
            .getWorldTypeName();
        if (world.isRemote || !supportsProfile(type, world.provider.dimensionId)) return;
        AlexCavesWorldData data = (AlexCavesWorldData) world.mapStorage.loadData(AlexCavesWorldData.class, NAME);
        if (data == null) {
            data = new AlexCavesWorldData(NAME);
            data.settings = AlexCavesSettings.select(
                newWorld && !world.getWorldInfo()
                    .isInitialized(),
                supportsProfile(type, world.provider.dimensionId),
                ExpansionConfig.abyssalChasm,
                industries.angelic.biomic.world.TallWorlds.liveTall(world));
            world.mapStorage.setData(NAME, data);
            data.markDirty();
        }
        data.requireSettings();
    }

    /** Dedicated Tartarus uses the Blend Overworld profile, never its independent Nether profile. */
    static boolean supportsProfile(String type, int dimension) {
        return dimension == 0 && "biomic_blend".equals(type);
    }

    public static AlexCavesSettings peek(World world) {
        if (world == null || world.isRemote || world.mapStorage == null) return AlexCavesSettings.OFF;
        java.util.Map<?, ?> loaded = cpw.mods.fml.relauncher.ReflectionHelper
            .getPrivateValue(MapStorage.class, world.mapStorage, "loadedDataMap", "field_75749_b");
        Object data = loaded.get(NAME);
        return data instanceof AlexCavesWorldData ? ((AlexCavesWorldData) data).requireSettings()
            : AlexCavesSettings.OFF;
    }

    private AlexCavesSettings requireSettings() {
        if (settings == null) throw new IllegalStateException("Invalid saved Alex's Caves manifest: " + failure);
        return settings;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        try {
            settings = AlexCavesSettings.read(nbt);
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
