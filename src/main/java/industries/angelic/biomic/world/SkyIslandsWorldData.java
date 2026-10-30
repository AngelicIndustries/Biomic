package industries.angelic.biomic.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import industries.angelic.biomic.config.ExpansionConfig;
import industries.angelic.biomic.world.terrain.IslandBiomeRules;

/**
 * Save-pinned sky layer of tall (Hohen Angst 512) worlds; worlds created before it stay island-free. Also pins how sky
 * islands of every world (the blend's at 256 too) choose their habitats.
 */
public final class SkyIslandsWorldData extends WorldSavedData {

    public static final String NAME = "biomic_sky_islands";
    public static final String NONE = "none";
    public static final String SHIFTED512 = "biomic:sky-islands-shifted512-v1";
    private String revision;
    private IslandBiomeRules biomes = IslandBiomeRules.DEFAULT;

    public SkyIslandsWorldData(String name) {
        super(name);
    }

    public static void initialize(World world, boolean newWorld) {
        if (world.isRemote) return;
        SkyIslandsWorldData data = (SkyIslandsWorldData) world.mapStorage.loadData(SkyIslandsWorldData.class, NAME);
        if (data == null) {
            data = new SkyIslandsWorldData(NAME);
            data.revision = newWorld && ExpansionConfig.skyIslands && TallWorlds.liveTall(world) ? SHIFTED512 : NONE;
            data.biomes = new IslandBiomeRules(
                ExpansionConfig.skyIslandOwnBiomes,
                ExpansionConfig.skyIslandBiomeVariety);
            world.mapStorage.setData(NAME, data);
            data.markDirty();
        }
        data.require();
    }

    /** Whether this loaded world's pinned sky layer has islands; never creates the record. */
    public static boolean enabled(World world) {
        if (world == null || world.isRemote || world.mapStorage == null) return false;
        java.util.Map<?, ?> loaded = cpw.mods.fml.relauncher.ReflectionHelper
            .getPrivateValue(MapStorage.class, world.mapStorage, "loadedDataMap", "field_75749_b");
        return loaded.get(NAME) instanceof SkyIslandsWorldData data && SHIFTED512.equals(data.require());
    }

    /** This world's pinned island habitat rules; the defaults where it has none (worlds made before them). */
    public static IslandBiomeRules biomes(World world) {
        if (world == null || world.isRemote || world.mapStorage == null) return IslandBiomeRules.DEFAULT;
        SkyIslandsWorldData data = (SkyIslandsWorldData) world.mapStorage.loadData(SkyIslandsWorldData.class, NAME);
        return data == null ? IslandBiomeRules.DEFAULT : data.biomes;
    }

    private String require() {
        if (!NONE.equals(revision) && !SHIFTED512.equals(revision))
            throw new IllegalStateException("Unsupported saved sky-island revision: " + revision);
        return revision;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        revision = nbt.getString("revision");
        biomes = nbt.hasKey("ownBiomes") ? new IslandBiomeRules(nbt.getBoolean("ownBiomes"), nbt.getInteger("variety"))
            : IslandBiomeRules.DEFAULT;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setString("revision", require());
        nbt.setBoolean("ownBiomes", biomes.ownBiomes());
        nbt.setInteger("variety", biomes.variety());
    }
}
