package industries.angelic.biomic.world;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;

import industries.angelic.biomic.config.ExpansionConfig;

/**
 * Save-pinned Nether of a Biomic world. Worlds created before the Tartarus Nether keep whatever Nether they had;
 * v1 Tartarus worlds keep a Nether without other mods' biomes; v2 pins the neighbour biomes installed at creation.
 */
public final class NetherWorldData extends WorldSavedData {

    public static final String NAME = "biomic_nether";
    public static final String NONE = "none";
    public static final String TARTARUS = "biomic:tartarus-v1";
    public static final String TARTARUS_NEIGHBOURS = "biomic:tartarus-v2";
    private String revision;
    private List<String> neighbours = List.of();

    public NetherWorldData(String name) {
        super(name);
    }

    public static void initialize(World world, boolean newWorld) {
        if (world.isRemote) return;
        NetherWorldData data = (NetherWorldData) world.mapStorage.loadData(NetherWorldData.class, NAME);
        if (data == null) {
            data = new NetherWorldData(NAME);
            data.revision = newWorld && ExpansionConfig.tartarusNether ? TARTARUS_NEIGHBOURS : NONE;
            if (TARTARUS_NEIGHBOURS.equals(data.revision)) data.neighbours = NetherNeighbour.available();
            world.mapStorage.setData(NAME, data);
            data.markDirty();
        }
        data.require();
    }

    /**
     * Whether Biomic generates this server world as the Tartarus Nether. Safe inside the World constructor: the
     * Nether shares the overworld's map storage, which has already loaded the pin. Client worlds never qualify.
     */
    public static boolean claims(World world) {
        NetherWorldData data = loaded(world);
        return data != null && !NONE.equals(data.require());
    }

    /** The biomes of other mods (and Biomic's own ports) a new world's Nether can include with what is installed. */
    public static List<net.minecraft.world.biome.BiomeGenBase> availableNeighbourBiomes() {
        List<net.minecraft.world.biome.BiomeGenBase> biomes = new java.util.ArrayList<>();
        for (NetherNeighbour neighbour : NetherNeighbour.of(NetherNeighbour.available())) biomes.add(neighbour.biome());
        return biomes;
    }

    /** The biomes of other mods (and Biomic's own ports) this world's Nether includes, for the biome browser. */
    public static List<net.minecraft.world.biome.BiomeGenBase> neighbourBiomes(World world) {
        List<net.minecraft.world.biome.BiomeGenBase> biomes = new java.util.ArrayList<>();
        for (NetherNeighbour neighbour : neighbours(world)) biomes.add(neighbour.biome());
        return biomes;
    }

    /** The other mods' biomes this world's Nether includes. */
    static List<NetherNeighbour> neighbours(World world) {
        NetherWorldData data = loaded(world);
        if (data == null) return List.of();
        List<NetherNeighbour> neighbours = NetherNeighbour.of(data.neighbours);
        for (NetherNeighbour neighbour : neighbours) if (neighbour.biome() == null) throw new IllegalStateException(
            "This world's Nether includes " + neighbour.key
                + ", whose mod is no longer installed or has the biome disabled");
        return neighbours;
    }

    private static NetherWorldData loaded(World world) {
        if (!(world instanceof WorldServer) || world.provider == null
            || world.provider.dimensionId != -1
            || world.mapStorage == null) return null;
        java.util.Map<?, ?> loaded = cpw.mods.fml.relauncher.ReflectionHelper
            .getPrivateValue(MapStorage.class, world.mapStorage, "loadedDataMap", "field_75749_b");
        return loaded.get(NAME) instanceof NetherWorldData data ? data : null;
    }

    private String require() {
        if (!NONE.equals(revision) && !TARTARUS.equals(revision) && !TARTARUS_NEIGHBOURS.equals(revision))
            throw new IllegalStateException("Unsupported saved Nether revision: " + revision);
        return revision;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        revision = nbt.getString("revision");
        String names = nbt.getString("neighbours");
        neighbours = names.isEmpty() ? List.of() : List.of(names.split(","));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setString("revision", require());
        nbt.setString("neighbours", String.join(",", neighbours));
    }
}
