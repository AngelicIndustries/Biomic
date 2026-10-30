package industries.angelic.biomic.storage;

import java.nio.ByteBuffer;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.Constants;

import com.falsepattern.chunk.api.DataManager;
import com.falsepattern.chunk.api.DataRegistry;

import industries.angelic.biomic.Biomic;

/** ChunkAPI persistence, synchronization, and cloning for Biomic's vertical biome volume. */
public final class BiomeVolumeManager implements DataManager.ChunkDataManager, DataManager.PacketDataManager {

    public static final BiomeVolumeManager INSTANCE = new BiomeVolumeManager();
    public static final String MANAGER_ID = "biome_volume";
    // ChunkAPI stores this in level.dat, not in packet framing. Both payload schemas remain readable.
    // The increment is an intentional old-binary/downgrade warning even for schema-1-only worlds.
    public static final String DATA_VERSION = "2";
    public static final boolean SUPPORTS_SCHEMA_TWO = true;

    private static final int ABSENT = 0;
    private static final int PRESENT = 1;
    private static boolean registered;

    private BiomeVolumeManager() {}

    public static synchronized void register() {
        if (registered) {
            throw new IllegalStateException("Biome volume manager is already registered");
        }
        DataRegistry.registerDataManager(INSTANCE, 0);
        registered = true;
        Biomic.LOG.info("Registered ChunkAPI data manager {}:{} version {}", Biomic.MODID, MANAGER_ID, DATA_VERSION);
    }

    public static BiomeVolume getVolume(Chunk chunk) {
        return holder(chunk).biomic$getBiomeVolume();
    }

    public static void setVolume(Chunk chunk, BiomeVolume volume) {
        if (volume == null) {
            throw new IllegalArgumentException("Biome volume cannot be null; use clearVolume instead");
        }
        replaceVolume(chunk, volume);
        chunk.setChunkModified();
    }

    @Override
    public String domain() {
        return Biomic.MODID;
    }

    @Override
    public String id() {
        return MANAGER_ID;
    }

    @Override
    public String version() {
        return DATA_VERSION;
    }

    @Override
    public String newInstallDescription() {
        return null;
    }

    @Override
    public String uninstallMessage() {
        return "This world contains Biomic vertical-biome data. Removing Biomic discards cave-biome identity.";
    }

    @Override
    public String versionChangeMessage(String priorVersion) {
        if (DATA_VERSION.equals(priorVersion) || "1".equals(priorVersion)) {
            return null;
        }
        return "Biomic cannot safely reinterpret biome-volume data written by version " + priorVersion + ".";
    }

    @Override
    public void writeChunkToNBT(Chunk chunk, NBTTagCompound nbt) {
        BiomeVolume volume = getVolume(chunk);
        requireCurrentRegistration(chunk, volume);
        BiomeVolumeCodec.clearNbt(nbt);
        if (volume != null) {
            BiomeVolumeCodec.writeNbt(volume, nbt);
        }
    }

    @Override
    public void readChunkFromNBT(Chunk chunk, NBTTagCompound nbt) {
        if (nbt == null || !nbt.hasKey("schema", Constants.NBT.TAG_INT)) {
            if (isTallVolumeWorld(chunk))
                throw new IllegalStateException("Tall chunk is missing required biome-volume identity");
            replaceVolume(chunk, null);
            return;
        }
        BiomeVolume decoded;
        try {
            decoded = BiomeVolumeCodec.readNbt(nbt);
        } catch (RuntimeException exception) {
            if (isTallVolumeWorld(chunk))
                throw new IllegalStateException("Cannot read required tall-world biome identity", exception);
            replaceVolume(chunk, null);
            Biomic.LOG
                .error("Ignored malformed biome volume in chunk {}, {}", chunk.xPosition, chunk.zPosition, exception);
            return;
        }
        validateSavedCoverage(chunk, decoded);
        replaceVolume(chunk, decoded);
    }

    @Override
    public void cloneChunk(Chunk from, Chunk to) {
        BiomeVolume volume = getVolume(from);
        replaceVolume(to, volume == null ? null : volume.copy());
    }

    @Override
    public int maxPacketSize() {
        return 1 + BiomeVolumeCodec.MAX_PACKET_PAYLOAD;
    }

    @Override
    public void writeToBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        if (!forceUpdate) {
            return;
        }
        BiomeVolume volume = getVolume(chunk);
        requireCurrentRegistration(chunk, volume);
        if (volume == null) {
            buffer.put((byte) ABSENT);
            return;
        }
        buffer.put((byte) PRESENT);
        BiomeVolumeCodec.writePacket(volume, buffer);
    }

    @Override
    public void readFromBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        if (!forceUpdate) {
            return;
        }
        try {
            int state = buffer.get() & 0xFF;
            if (state == ABSENT) {
                replaceVolume(chunk, null);
            } else if (state == PRESENT) {
                replaceVolume(chunk, BiomeVolumeCodec.readPacket(buffer));
            } else {
                throw new IllegalArgumentException("Unknown biome-volume packet state " + state);
            }
        } catch (RuntimeException exception) {
            replaceVolume(chunk, null);
            Biomic.LOG.error(
                "Ignored malformed biome-volume packet for chunk {}, {}",
                chunk.xPosition,
                chunk.zPosition,
                exception);
        }
    }

    private static BiomeVolumeChunk holder(Chunk chunk) {
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (!(chunk instanceof BiomeVolumeChunk)) {
            throw new IllegalStateException("Chunk does not implement Biomic BiomeVolumeChunk");
        }
        return (BiomeVolumeChunk) chunk;
    }

    private static void replaceVolume(Chunk chunk, BiomeVolume volume) {
        holder(chunk).biomic$setBiomeVolume(volume);
    }

    private static void requireCurrentRegistration(Chunk chunk, BiomeVolume volume) {
        if (volume == null || volume.getBlockHeight() == BiomeVolume.BLOCK_HEIGHT) return;
        var world = chunk.worldObj;
        boolean tall = isTallVolumeWorld(chunk);
        var settings = world == null || world.isRemote ? null
            : industries.angelic.biomic.world.BiomicWorldSettings.peekLoaded(world);
        int height = world == null ? 0
            : industries.angelic.biomic.world.LiveChunkLayout.of(chunk)
                .height();
        String revision = settings == null ? null : settings.getAuthoredHeightRevision();
        requireFullHeightWrite(
            tall,
            world != null && !world.isRemote,
            height,
            revision,
            industries.angelic.biomic.network.BiomicBiomeVolumeProtocol.schemaTwoSessionPublished());
    }

    private static void validateSavedCoverage(Chunk chunk, BiomeVolume volume) {
        var world = chunk.worldObj;
        boolean tall = isTallVolumeWorld(chunk);
        if (volume.getBlockHeight() == BiomeVolume.BLOCK_HEIGHT && !tall) return;
        int height = world == null ? 0
            : industries.angelic.biomic.world.LiveChunkLayout.of(chunk)
                .height();
        String revision = null;
        if (world != null && !world.isRemote) {
            var settings = industries.angelic.biomic.world.BiomicWorldSettings.peekLoaded(world);
            revision = settings == null ? null : settings.getAuthoredHeightRevision();
        } else if (tall) {
            revision = industries.angelic.biomic.world.terrain.AuthoredHeightPolicy.SHIFTED_512_REVISION;
        }
        requireSavedCoverage(volume.getBlockHeight(), tall, height, revision);
    }

    private static boolean isTallVolumeWorld(Chunk chunk) {
        var world = chunk.worldObj;
        if (world == null || world.getWorldInfo() == null
            || world.getWorldInfo()
                .getTerrainType() == null
            || world.provider == null) return false;
        return industries.angelic.biomic.world.TallWorlds.liveTall(world);
    }

    static void requireSavedCoverage(int volumeHeight, boolean tallPreset, int storageHeight, String revision) {
        if (volumeHeight == 256 && !tallPreset) return;
        if (volumeHeight != 512 || !tallPreset
            || storageHeight != 512
            || !industries.angelic.biomic.world.terrain.AuthoredHeightPolicy.SHIFTED_512_REVISION.equals(revision)) {
            throw new IllegalStateException(
                "Saved biome-volume coverage conflicts with the world's authored-height policy");
        }
    }

    static void requireFullHeightWrite(boolean tallPreset, boolean serverOwnedDimension, int storageHeight,
        String savedRevision, boolean publishedSchemaTwo) {
        if (!tallPreset || !serverOwnedDimension
            || storageHeight != 512
            || !industries.angelic.biomic.world.terrain.AuthoredHeightPolicy.SHIFTED_512_REVISION.equals(savedRevision)
            || !publishedSchemaTwo) {
            throw new IllegalStateException(
                "Full-height biome writes require the saved tall preset, HA512 storage, and published schema-2 peer barrier");
        }
    }
}
