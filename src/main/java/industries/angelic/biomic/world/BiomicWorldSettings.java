package industries.angelic.biomic.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.util.Constants;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.config.WorldgenConfigSnapshot;
import industries.angelic.biomic.world.terrain.AuthoredHeightPolicy;

/** Immutable save identity for one Biomic generator line. */
public final class BiomicWorldSettings {

    public static final String DATA_NAME = Biomic.MODID + "_world_settings";
    public static final int SCHEMA_VERSION = 1;
    public static final int GENERATOR_VERSION = 77;

    private static final String SCHEMA_KEY = "schema";
    private static final String GENERATOR_KEY = "generator";
    private static final String WORLD_SEED_KEY = "worldSeed";
    private static final String SEA_LEVEL_KEY = "seaLevel";
    private static final String CONFIG_SIGNATURE_KEY = "configSignature";
    private static final String PLANT_PROJECTION_KEY = "plantProjectionRevision";
    private static final String FLOATING_PLACEMENT_KEY = "floatingPlacementRevision";

    private final int schemaVersion;
    private final int generatorVersion;
    private final long worldSeed;
    private final int seaLevel;
    private final String configSignature;
    private final int plantProjectionRevision;
    private final int floatingPlacementRevision;
    private final String authoredHeightRevision;

    private BiomicWorldSettings(int schemaVersion, int generatorVersion, long worldSeed, int seaLevel,
        String configSignature, int plantProjectionRevision, int floatingPlacementRevision,
        String authoredHeightRevision) {
        if (plantProjectionRevision < 0 || plantProjectionRevision > 1) throw new IllegalArgumentException(
            "Unsupported saved plant projection revision: " + plantProjectionRevision);
        if (floatingPlacementRevision < 0 || floatingPlacementRevision > 1) throw new IllegalArgumentException(
            "Unsupported saved floating placement revision: " + floatingPlacementRevision);
        if (authoredHeightRevision != null) {
            // Validate the saved identifier independently of the live storage layout. Runtime selection
            // below applies the actual 256/512 and peer-capability constraints.
            AuthoredHeightPolicy.select(authoredHeightRevision, 512, true);
        }
        this.schemaVersion = schemaVersion;
        this.generatorVersion = generatorVersion;
        this.worldSeed = worldSeed;
        this.seaLevel = seaLevel;
        this.configSignature = configSignature;
        this.plantProjectionRevision = plantProjectionRevision;
        this.floatingPlacementRevision = floatingPlacementRevision;
        this.authoredHeightRevision = authoredHeightRevision;
    }

    public static BiomicWorldSettings loadOrCreate(World world) {
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
        boolean tall = TallWorlds.liveTall(world);
        WorldgenConfigSnapshot snapshot = WorldgenConfigSnapshot.forWorld(world);
        String heightRevision = tall ? AuthoredHeightPolicy.SHIFTED_512_REVISION : null;
        if (world.isRemote) {
            return current(world.getSeed(), snapshot, heightRevision);
        }

        RequiredWorldHeight.initialize(world);

        MapStorage storage = world.mapStorage;
        SavedData savedData = (SavedData) storage.loadData(SavedData.class, DATA_NAME);
        if (savedData == null) {
            java.io.File settingsFile = world.getSaveHandler()
                .getMapFileFromName(DATA_NAME);
            if (settingsFile != null && settingsFile.exists()) {
                throw new IllegalStateException("Cannot read Biomic world settings: " + settingsFile);
            }
            industries.angelic.biomic.world.cave.CaveExpansionWorldData.initialize(world, true);
            SkyIslandsWorldData.initialize(world, true);
            NetherWorldData.initialize(world, true);
            industries.angelic.biomic.compat.biome.NativeHabitatWorldData.initialize(world, true);
            industries.angelic.biomic.world.alexscaves.AlexCavesWorldData.initialize(world, true);
            BiomicWorldSettings settings = current(world.getSeed(), snapshot, heightRevision);
            settings.validateAuthoredHeight(tall);
            settings.getAuthoredHeightPolicy(
                RequiredWorldHeight.validatedRequiredHeight(world),
                industries.angelic.biomic.storage.BiomeVolumeManager.SUPPORTS_SCHEMA_TWO);
            savedData = new SavedData(DATA_NAME);
            savedData.setSettings(settings);
            storage.setData(DATA_NAME, savedData);
            savedData.markDirty();
            Biomic.LOG.info(
                "Created Biomic world settings: schema={}, generator={}, config={}",
                settings.schemaVersion,
                settings.generatorVersion,
                settings.configSignature);
            return settings;
        }

        BiomicWorldSettings settings = savedData.requireSettings();
        settings.validateAgainst(world.getSeed(), snapshot);
        settings.validateAuthoredHeight(tall);
        settings.getAuthoredHeightPolicy(
            RequiredWorldHeight.validatedRequiredHeight(world),
            industries.angelic.biomic.storage.BiomeVolumeManager.SUPPORTS_SCHEMA_TWO);
        industries.angelic.biomic.world.cave.CaveExpansionWorldData.initialize(world, false);
        SkyIslandsWorldData.initialize(world, false);
        NetherWorldData.initialize(world, false);
        industries.angelic.biomic.compat.biome.NativeHabitatWorldData.initialize(world, false);
        industries.angelic.biomic.world.alexscaves.AlexCavesWorldData.initialize(world, false);
        return settings;
    }

    /** Inspection only: peek at the existing memory cache, never read disk, create, repair or mark data dirty. */
    public static BiomicWorldSettings peekLoaded(World world) {
        if (world == null || world.isRemote || world.mapStorage == null) return null;
        java.util.Map<?, ?> loaded = cpw.mods.fml.relauncher.ReflectionHelper
            .getPrivateValue(MapStorage.class, world.mapStorage, "loadedDataMap", "field_75749_b");
        Object value = loaded.get(DATA_NAME);
        return value instanceof SavedData ? ((SavedData) value).settings : null;
    }

    static BiomicWorldSettings current(long worldSeed, WorldgenConfigSnapshot snapshot, String authoredHeightRevision) {
        return new BiomicWorldSettings(
            SCHEMA_VERSION,
            GENERATOR_VERSION,
            worldSeed,
            snapshot.getSeaLevel(),
            snapshot.getSignature(),
            1,
            snapshot.isFloatingIslandsEnabled() ? 1 : 0,
            authoredHeightRevision);
    }

    static BiomicWorldSettings readFromNbt(NBTTagCompound nbt) {
        requireTag(nbt, SCHEMA_KEY, Constants.NBT.TAG_INT);
        requireTag(nbt, GENERATOR_KEY, Constants.NBT.TAG_INT);
        requireTag(nbt, WORLD_SEED_KEY, Constants.NBT.TAG_LONG);
        requireTag(nbt, SEA_LEVEL_KEY, Constants.NBT.TAG_INT);
        requireTag(nbt, CONFIG_SIGNATURE_KEY, Constants.NBT.TAG_STRING);
        if (nbt.hasKey(PLANT_PROJECTION_KEY)) requireTag(nbt, PLANT_PROJECTION_KEY, Constants.NBT.TAG_INT);
        if (nbt.hasKey(FLOATING_PLACEMENT_KEY)) requireTag(nbt, FLOATING_PLACEMENT_KEY, Constants.NBT.TAG_INT);
        if (nbt.hasKey(AuthoredHeightPolicy.SAVE_KEY)) {
            requireTag(nbt, AuthoredHeightPolicy.SAVE_KEY, Constants.NBT.TAG_STRING);
        }
        return new BiomicWorldSettings(
            nbt.getInteger(SCHEMA_KEY),
            nbt.getInteger(GENERATOR_KEY),
            nbt.getLong(WORLD_SEED_KEY),
            nbt.getInteger(SEA_LEVEL_KEY),
            nbt.getString(CONFIG_SIGNATURE_KEY),
            nbt.hasKey(PLANT_PROJECTION_KEY) ? nbt.getInteger(PLANT_PROJECTION_KEY) : 0,
            nbt.hasKey(FLOATING_PLACEMENT_KEY) ? nbt.getInteger(FLOATING_PLACEMENT_KEY) : 0,
            nbt.hasKey(AuthoredHeightPolicy.SAVE_KEY) ? nbt.getString(AuthoredHeightPolicy.SAVE_KEY) : null);
    }

    void writeToNbt(NBTTagCompound nbt) {
        nbt.setInteger(SCHEMA_KEY, schemaVersion);
        nbt.setInteger(GENERATOR_KEY, generatorVersion);
        nbt.setLong(WORLD_SEED_KEY, worldSeed);
        nbt.setInteger(SEA_LEVEL_KEY, seaLevel);
        nbt.setString(CONFIG_SIGNATURE_KEY, configSignature);
        // Omission is the old format: do not enroll existing saves or rewrite their identity.
        if (plantProjectionRevision != 0) nbt.setInteger(PLANT_PROJECTION_KEY, plantProjectionRevision);
        if (floatingPlacementRevision != 0) nbt.setInteger(FLOATING_PLACEMENT_KEY, floatingPlacementRevision);
        if (authoredHeightRevision != null) {
            nbt.setString(AuthoredHeightPolicy.SAVE_KEY, authoredHeightRevision);
        }
    }

    void validateAgainst(long activeWorldSeed, WorldgenConfigSnapshot snapshot) {
        requireCompatible(schemaVersion == SCHEMA_VERSION, "unsupported settings schema " + schemaVersion);
        requireCompatible(
            generatorVersion == GENERATOR_VERSION,
            "save generator version " + generatorVersion + " does not match active version " + GENERATOR_VERSION);
        requireCompatible(worldSeed == activeWorldSeed, "saved world seed does not match the active world seed");
        requireCompatible(
            seaLevel == snapshot.getSeaLevel(),
            "saved sea level does not match the active configuration");
        requireCompatible(
            snapshot.getSignature()
                .equals(configSignature),
            "worldgen-critical configuration signature changed");
    }

    public int getGeneratorVersion() {
        return generatorVersion;
    }

    public long getWorldSeed() {
        return worldSeed;
    }

    public int getSeaLevel() {
        return seaLevel;
    }

    public int getPlantProjectionRevision() {
        return plantProjectionRevision;
    }

    /** Zero is the exact old ocean-only island admission. One additionally admits clear overland rings. */
    public int getFloatingPlacementRevision() {
        return floatingPlacementRevision;
    }

    public String getAuthoredHeightRevision() {
        return authoredHeightRevision;
    }

    public AuthoredHeightPolicy getAuthoredHeightPolicy(int storageHeight, boolean schema2CapabilityReady) {
        return AuthoredHeightPolicy.select(authoredHeightRevision, storageHeight, schema2CapabilityReady);
    }

    void validateAuthoredHeight(boolean tall) {
        boolean shifted = AuthoredHeightPolicy.SHIFTED_512_REVISION.equals(authoredHeightRevision);
        requireCompatible(
            !tall || shifted,
            "a tall Biomic preset requires " + AuthoredHeightPolicy.SHIFTED_512_REVISION);
        requireCompatible(tall || !shifted, "shifted authored height belongs only to a tall Biomic preset");
    }

    private static void requireTag(NBTTagCompound nbt, String key, int type) {
        if (!nbt.hasKey(key, type)) {
            throw new IllegalArgumentException("Missing or invalid Biomic world-settings field '" + key + "'");
        }
    }

    private static void requireCompatible(boolean condition, String reason) {
        if (!condition) {
            throw new IllegalStateException("Biomic world settings are incompatible: " + reason);
        }
    }

    /** Public for vanilla MapStorage reflection; callers use {@link BiomicWorldSettings#loadOrCreate(World)}. */
    public static final class SavedData extends WorldSavedData {

        private BiomicWorldSettings settings;
        private String readFailure;

        public SavedData(String mapName) {
            super(mapName);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            try {
                settings = BiomicWorldSettings.readFromNbt(nbt);
                readFailure = null;
            } catch (RuntimeException exception) {
                settings = null;
                readFailure = exception.getMessage();
            }
        }

        @Override
        public void writeToNBT(NBTTagCompound nbt) {
            requireSettings().writeToNbt(nbt);
        }

        private void setSettings(BiomicWorldSettings settings) {
            this.settings = settings;
            this.readFailure = null;
        }

        private BiomicWorldSettings requireSettings() {
            if (settings == null) {
                throw new IllegalStateException(
                    "Biomic world settings could not be read" + (readFailure == null ? "" : ": " + readFailure));
            }
            return settings;
        }
    }
}
