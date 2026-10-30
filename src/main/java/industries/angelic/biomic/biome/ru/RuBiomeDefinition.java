package industries.angelic.biomic.biome.ru;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.config.WorldgenConfigSnapshot;

/** Immutable registration, terrain, surface and decoration data for one completed RU biome port. */
public final class RuBiomeDefinition {

    public enum IdSlot {

        CLOVER_PLAINS,
        WISTERIA_GROVE,
        PINE_SLOPES,
        GRASSY_BEACH,
        CHALK_CLIFFS,
        TOWERING_CLIFFS,
        REDWOODS,
        BAYOU,
        OLD_GROWTH_BAYOU,
        JOSHUA_DESERT,
        EUCALYPTUS_FOREST,
        EXPANSION;

        int resolve(WorldgenConfigSnapshot snapshot) {
            switch (this) {
                case CLOVER_PLAINS:
                    return snapshot.getCloverPlainsId();
                case WISTERIA_GROVE:
                    return snapshot.getWisteriaGroveId();
                case PINE_SLOPES:
                    return snapshot.getPineSlopesId();
                case GRASSY_BEACH:
                    return snapshot.getGrassyBeachId();
                case CHALK_CLIFFS:
                    return snapshot.getChalkCliffsId();
                case TOWERING_CLIFFS:
                    return snapshot.getToweringCliffsId();
                case REDWOODS:
                    return snapshot.getRedwoodsId();
                case BAYOU:
                    return snapshot.getBayouId();
                case OLD_GROWTH_BAYOU:
                    return snapshot.getOldGrowthBayouId();
                case JOSHUA_DESERT:
                    return snapshot.getJoshuaDesertId();
                case EUCALYPTUS_FOREST:
                    return snapshot.getEucalyptusForestId();
                case EXPANSION:
                    throw new IllegalStateException("Expansion RU biome IDs require their stable manifest offset");
                default:
                    throw new IllegalStateException("Unmapped RU biome ID slot " + this);
            }
        }
    }

    public enum TreeStyle {

        NONE,
        WISTERIA,
        PINE,
        PALM,
        CHALK_BUSH,
        TOWERING_PINE,
        REDWOOD,
        CYPRESS,
        GIANT_CYPRESS,
        JOSHUA,
        EUCALYPTUS
    }

    public enum SurfaceStyle {

        DEFAULT,
        ALPHA_GROVE,
        COARSE_DIRT,
        SAND,
        CHALK,
        TOWERING_CLIFFS,
        PODZOL,
        BAYOU,
        OLD_GROWTH_BAYOU,
        JOSHUA_DESERT,
        GRAVEL,
        SNOW,
        MUD,
        PEAT,
        SILT,
        ASHEN,
        BADLANDS,
        TROPICS
    }

    public enum Dimension {

        SURFACE,
        CAVE,
        NETHER
    }

    private final String biomeKey;
    private final String displayName;
    private final IdSlot idSlot;
    private final int expansionOffset;
    private final Dimension dimension;
    private final OrigenBiomeProfile terrainProfile;
    private final int mapColor;
    private final float temperature;
    private final float rainfall;
    private final float rootHeight;
    private final float variation;
    private final int grassColor;
    private final int foliageColor;
    private final int waterColor;
    private final int skyColor;
    private final Set<String> tags;
    private final boolean naturalSpawn;

    private RuBiomeDefinition(Builder builder) {
        biomeKey = builder.biomeKey;
        displayName = builder.displayName;
        idSlot = builder.idSlot;
        expansionOffset = builder.expansionOffset;
        dimension = builder.dimension;
        terrainProfile = builder.terrainProfile;
        mapColor = builder.mapColor;
        temperature = builder.temperature;
        rainfall = builder.rainfall;
        rootHeight = builder.rootHeight;
        variation = builder.variation;
        grassColor = builder.grassColor;
        foliageColor = builder.foliageColor;
        waterColor = builder.waterColor;
        skyColor = builder.skyColor;
        tags = Collections.unmodifiableSet(new LinkedHashSet<>(builder.tags));
        naturalSpawn = builder.naturalSpawn;
    }

    public static Builder builder(String biomeKey, String displayName, IdSlot idSlot) {
        return new Builder(biomeKey, displayName, idSlot, -1);
    }

    public static Builder expansionBuilder(String biomeKey, String displayName, int expansionOffset) {
        if (expansionOffset < 0) {
            throw new IllegalArgumentException("RU expansion offset cannot be negative");
        }
        return new Builder(biomeKey, displayName, IdSlot.EXPANSION, expansionOffset);
    }

    public String getBiomeKey() {
        return biomeKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int resolveId(WorldgenConfigSnapshot snapshot) {
        return idSlot == IdSlot.EXPANSION ? snapshot.getRuExpansionBaseId() + expansionOffset
            : idSlot.resolve(snapshot);
    }

    public Dimension getDimension() {
        return dimension;
    }

    public OrigenBiomeProfile getTerrainProfile() {
        return terrainProfile;
    }

    public int getMapColor() {
        return mapColor;
    }

    public float getTemperature() {
        return temperature;
    }

    public float getRainfall() {
        return rainfall;
    }

    public float getRootHeight() {
        return rootHeight;
    }

    public float getVariation() {
        return variation;
    }

    public int getGrassColor() {
        return grassColor;
    }

    public int getFoliageColor() {
        return foliageColor;
    }

    public int getWaterColor() {
        return waterColor;
    }

    public int getSkyColor() {
        return skyColor;
    }

    public Set<String> getTags() {
        return tags;
    }

    public boolean isNaturalSpawn() {
        return naturalSpawn;
    }

    public static final class Builder {

        private final String biomeKey;
        private final String displayName;
        private final IdSlot idSlot;
        private final int expansionOffset;
        private Dimension dimension = Dimension.SURFACE;
        private OrigenBiomeProfile terrainProfile;
        private int mapColor;
        private float temperature = 0.7F;
        private float rainfall = 0.7F;
        private float rootHeight = 0.1F;
        private float variation = 0.2F;
        private int grassColor = -1;
        private int foliageColor = -1;
        private int waterColor = 0x3F76E4;
        private int skyColor = -1;
        private final Set<String> tags = new LinkedHashSet<>();
        private boolean naturalSpawn;

        private Builder(String biomeKey, String displayName, IdSlot idSlot, int expansionOffset) {
            if (biomeKey == null || displayName == null || idSlot == null) {
                throw new IllegalArgumentException("RU biome key, display name and ID slot cannot be null");
            }
            this.biomeKey = biomeKey;
            this.displayName = displayName;
            this.idSlot = idSlot;
            this.expansionOffset = expansionOffset;
        }

        public Builder terrain(OrigenBiomeProfile profile, float rootHeight, float variation) {
            terrainProfile = profile;
            this.rootHeight = rootHeight;
            this.variation = variation;
            return this;
        }

        public Builder climate(float temperature, float rainfall) {
            this.temperature = temperature;
            this.rainfall = rainfall;
            return this;
        }

        public Builder colors(int map, int grass, int foliage, int water, int sky) {
            mapColor = map;
            grassColor = grass;
            foliageColor = foliage;
            waterColor = water;
            skyColor = sky;
            return this;
        }

        public Builder tags(String... values) {
            if (values != null) {
                Collections.addAll(tags, values);
            }
            return this;
        }

        public Builder decoration(TreeStyle trees, int treeCount, int flowerCount, int grassCount) {
            if (trees == null || treeCount < 0 || flowerCount < 0 || grassCount < 0) {
                throw new IllegalArgumentException("RU decoration values must be non-negative and have a tree style");
            }
            return this;
        }

        public Builder surface(SurfaceStyle style) {
            if (style == null) {
                throw new IllegalArgumentException("RU surface style cannot be null");
            }
            return this;
        }

        public Builder dimension(Dimension value) {
            if (value == null) {
                throw new IllegalArgumentException("RU biome dimension cannot be null");
            }
            dimension = value;
            return this;
        }

        public Builder naturalSpawn() {
            naturalSpawn = true;
            return this;
        }

        public Builder villageLowland() {
            return this;
        }

        public RuBiomeDefinition build() {
            if (tags.isEmpty()) {
                throw new IllegalStateException("RU biome " + biomeKey + " has no semantic tags");
            }
            return new RuBiomeDefinition(this);
        }
    }
}
