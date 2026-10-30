package industries.angelic.biomic.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/** Save-level anti-truncation lock, independent of terrain signatures and optional ChunkAPI warnings. */
public final class RequiredWorldHeight extends WorldSavedData {

    public static final String NAME = "biomic_required_storage_height";
    private int required;
    private String failure;

    public RequiredWorldHeight(String name) {
        super(name);
    }

    /** Validate/persist the physical-storage requirement before a Biomic provider can request chunks. */
    public static void initialize(World world) {
        if (world == null) throw new IllegalArgumentException("World cannot be null");
        if (world.isRemote) return;
        int live = LiveChunkLayout.of(world)
            .height();
        RequiredWorldHeight data = (RequiredWorldHeight) world.mapStorage.loadData(RequiredWorldHeight.class, NAME);
        if (data == null) {
            // MapStorage logs a corrupt gzip/read failure and returns null. Never treat it as
            // a fresh256 save and overwrite a possibly512 requirement.
            java.io.File saved = world.getSaveHandler()
                .getMapFileFromName(NAME);
            if (saved != null && saved.exists())
                throw new IllegalStateException("Cannot read Biomic storage-height lock: " + saved);
            data = new RequiredWorldHeight(NAME);
            data.required = live;
            world.mapStorage.setData(NAME, data);
            data.markDirty();
            if (live == 512) persistBeforeChunkAccess(world, data);
        } else {
            int selected = requiredHeight(data.requireValid(), live);
            if (selected != data.required) {
                data.required = selected;
                data.markDirty();
                persistBeforeChunkAccess(world, data);
            }
        }
    }

    /**
     * Return the already-initialized authoritative lock without creating, promoting, or persisting it.
     * Callers must invoke {@link #initialize(World)} at their server startup boundary first.
     */
    public static int validatedRequiredHeight(World world) {
        if (world == null) throw new IllegalArgumentException("World cannot be null");
        if (world.isRemote) throw new IllegalStateException("Storage-height authority is server-side only");
        if (world.mapStorage == null) throw new IllegalStateException("Missing initialized storage-height authority");
        int live = LiveChunkLayout.of(world)
            .height();
        RequiredWorldHeight data = (RequiredWorldHeight) world.mapStorage.loadData(RequiredWorldHeight.class, NAME);
        if (data == null) throw new IllegalStateException("Storage-height lock has not been initialized");
        int saved = data.requireValid();
        if (requiredHeight(saved, live) != saved)
            throw new IllegalStateException("Storage-height lock has not been initialized for the live layout");
        return saved;
    }

    private static void persistBeforeChunkAccess(World world, RequiredWorldHeight data) {
        persist(
            world.getSaveHandler()
                .getMapFileFromName(NAME),
            data);
    }

    static void persist(java.io.File saved, RequiredWorldHeight data) {
        // Do NOT use MapStorage.saveAllData here: Hodgepodge queues those writes and
        // clears the dirty flag before they reach disk. This startup-only safety lock
        // must precede chunk access. Keep its ordinary MapStorage NBT representation.
        if (saved == null) throw new IllegalStateException("Biomic 512 world requires a persistent height lock");
        java.nio.file.Path temporary = null;
        try {
            NBTTagCompound value = new NBTTagCompound();
            data.writeToNBT(value);
            if (data.required != 512)
                throw new IllegalStateException("Only a 512 requirement needs synchronous persistence");
            NBTTagCompound root = new NBTTagCompound();
            root.setTag("data", value);
            byte[] bytes = net.minecraft.nbt.CompressedStreamTools.compress(root);
            java.nio.file.Path target = saved.toPath()
                .toAbsolutePath();
            java.nio.file.Files.createDirectories(target.getParent());
            temporary = java.nio.file.Files.createTempFile(target.getParent(), NAME + "-", ".pending");
            try (java.io.FileOutputStream output = new java.io.FileOutputStream(temporary.toFile())) {
                output.write(bytes);
                output.getFD()
                    .sync();
            }
            java.nio.file.Files.move(
                temporary,
                target,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            temporary = null;
            try (java.io.InputStream input = new java.io.FileInputStream(saved)) {
                NBTTagCompound written = net.minecraft.nbt.CompressedStreamTools.readCompressed(input)
                    .getCompoundTag("data");
                if (!value.equals(written))
                    throw new IllegalStateException("Biomic 512 storage-height lock did not reach disk");
            }
            data.setDirty(false);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(
                "Cannot persist Biomic 512 storage-height lock before chunk access",
                failure);
        } finally {
            if (temporary != null) try {
                java.nio.file.Files.deleteIfExists(temporary);
            } catch (java.io.IOException cleanup) {
                throw new IllegalStateException("Cannot remove incomplete Biomic height lock " + temporary, cleanup);
            }
        }
    }

    static int requiredHeight(int saved, int live) {
        if ((saved != 256 && saved != 512) || (live != 256 && live != 512))
            throw new IllegalStateException("Invalid Biomic storage height");
        if (saved > live) throw new IllegalStateException(
            "This Biomic save requires512-block storage. Restore Hohenangst before opening it; refusing to discard upper sections.");
        return Math.max(saved, live);
    }

    private int requireValid() {
        if (failure != null || required != 256 && required != 512)
            throw new IllegalStateException("Invalid Biomic storage-height lock: " + failure);
        return required;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        failure = null;
        if (!nbt.hasKey("schema", 3) || nbt.getInteger("schema") != 1 || !nbt.hasKey("requiredHeight", 3))
            failure = "unknown/missing schema or height";
        required = nbt.getInteger("requiredHeight");
        if (required != 256 && required != 512) failure = "unsupported required height " + required;
        // Retain the failure instead of throwing: MapStorage must not mistake a failed read for a missing file.
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("schema", 1);
        nbt.setInteger("requiredHeight", requireValid());
    }
}
