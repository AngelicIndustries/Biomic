package industries.angelic.biomic.world;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;

import industries.angelic.biomic.biome.BiomicBiomes;
import industries.angelic.biomic.biome.VerticalBiomeCatalogue;
import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.biome.ru.RuBiomeCatalogue;
import industries.angelic.biomic.biome.ru.RuBiomeDefinition;
import industries.angelic.biomic.world.modern.ModernBiomeCompatibility;
import industries.angelic.biomic.world.terrain.OrigenVerticalProfileSampler;
import industries.angelic.biomic.world.terrain.RegionalHabitatSampler;

/**
 * The samplers the biome browser's search reads, each its own (the search runs off the server thread): the biome of a
 * column in the overworld or the Nether as the chunk manager draws it, which is the biome a generated chunk stores.
 */
public final class BiomeSearch {

    private BiomeSearch() {}

    /** The overworld provider's search access, or null when Biomic does not generate this world. */
    public static BiomeLocating locating(WorldServer world) {
        return world != null && world.theChunkProviderServer.currentChunkProvider instanceof BiomeLocating locating
            ? locating
            : null;
    }

    /** Whether a column holds the biome searched for. */
    @FunctionalInterface
    public interface ColumnTest {

        boolean test(int x, int z);
    }

    /**
     * A world's samplers for searching, shared by every biome one Go or one survey looks for, and used by one thread
     * at a time (the browser's search thread). The surface's are made when first sampled.
     */
    public static final class Samplers {

        private final WorldServer world;
        private final BiomeLocating locating;
        private WorldChunkManagerBiomic manager;
        private OrigenVerticalProfileSampler profiles;
        private RegionalHabitatSampler habitats;
        private WorldChunkManagerBiomicNether nether;

        public Samplers(WorldServer world) {
            this.world = world;
            locating = BiomeSearch.locating(world);
        }

        /** The overworld provider's search access, or null when Biomic does not generate this world. */
        public BiomeLocating locating() {
            return locating;
        }

        /**
         * Whether a column holds the target on its surface, or null when Biomic does not generate this overworld. The
         * full lookup samples Origen's noise a dozen times, so the column's Origen profile (one sample) comes first
         * and must be one that can lead to the target. A hosted habitat (Regions Unexplored, vanilla, BOP...) is
         * chosen per province of 32-block cells, so it is asked at cell centres only ({@code x} and {@code z} 16
         * modulo 32), and at every column of the narrow river and shore families; its habitats that live on a seabed
         * in the 3D volume count.
         */
        public ColumnTest surfaceTest(BiomeGenBase target) {
            if (locating == null || target == null) return null;
            EnumSet<OrigenBiomeProfile> direct = EnumSet.noneOf(OrigenBiomeProfile.class);
            boolean origen = false;
            for (OrigenBiomeProfile profile : OrigenBiomeProfile.values()) {
                BiomeGenBase biome = BiomicBiomes.getOrigenBiome(profile);
                origen |= biome == target;
                // Origen's own, and the profiles whose lookup falls through to the macro terrain's fallbacks.
                if (biome == target || biome == null || !profile.isSurface()) direct.add(profile);
            }
            boolean hosted = !origen;
            return (x, z) -> {
                if (manager == null) {
                    WorldgenContext context = locating.worldgenContext();
                    manager = new WorldChunkManagerBiomic(context);
                    profiles = new OrigenVerticalProfileSampler(context);
                    habitats = new RegionalHabitatSampler(context);
                }
                OrigenBiomeProfile profile = profiles.sampleSurface(x, z);
                if (direct.contains(profile) && manager.getBiomeGenAt(x, z) == target) return true;
                if (!hosted) return false;
                int family = RegionalHabitatSampler.family(profile);
                if (family == 0) return false;
                int grid = RegionalHabitatSampler.grid(family);
                if (grid > 16 && (Math.floorMod(x, grid) != grid / 2 || Math.floorMod(z, grid) != grid / 2))
                    return false;
                String habitat = habitats.sample(x, z);
                return habitat != null && ModernBiomeCompatibility.biome(habitat) == target;
            };
        }

        /** Whether a column of a Tartarus Nether holds the biome, or null when Biomic does not generate this Nether. */
        public ColumnTest netherTest(int biomeId) {
            if (world == null || !(world.getWorldChunkManager() instanceof WorldChunkManagerBiomicNether)) return null;
            if (nether == null)
                nether = new WorldChunkManagerBiomicNether(world.getSeed(), NetherWorldData.neighbours(world));
            WorldChunkManagerBiomicNether manager = nether;
            return (x, z) -> manager.getBiomeGenAt(x, z).biomeID == biomeId;
        }
    }

    /**
     * Whether a cave biome is an Origen cave Regions Unexplored's caves take over (HostedCaveHabitats): under any roof
     * its cells become theirs, so it stays itself only where nothing solid is above it.
     */
    public static boolean displacedCave(String biomeKey) {
        for (RuBiomeDefinition cave : RuBiomeCatalogue.caves())
            if (VerticalBiomeCatalogue.getOrigenCave(cave.getTerrainProfile())
                .getKey()
                .equals(biomeKey)) return true;
        return false;
    }

    /**
     * The keys of a raw biome volume that may become this cave biome: its own, and for Regions Unexplored's caves the
     * Origen cave they replace where a solid roof closes it (HostedCaveHabitats).
     */
    public static Set<String> caveSources(String biomeKey) {
        Set<String> sources = new LinkedHashSet<>();
        sources.add(biomeKey);
        for (RuBiomeDefinition cave : RuBiomeCatalogue.caves()) if (cave.getBiomeKey()
            .equals(biomeKey))
            sources.add(
                VerticalBiomeCatalogue.getOrigenCave(cave.getTerrainProfile())
                    .getKey());
        return sources;
    }
}
