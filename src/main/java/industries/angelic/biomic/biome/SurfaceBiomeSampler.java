package industries.angelic.biomic.biome;

import net.minecraft.world.biome.BiomeGenBase;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.biome.placement.SurfaceLandform;
import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.terrain.MacroTerrainSample;
import industries.angelic.biomic.world.terrain.MacroTerrainSampler;
import industries.angelic.biomic.world.terrain.OrigenVerticalProfileSampler;
import industries.angelic.biomic.world.terrain.SurfaceTopology;

/** Materializes stable biome identities only after shared climate and landform sampling. */
public final class SurfaceBiomeSampler {

    private final MacroTerrainSampler macroSampler;
    private final OrigenVerticalProfileSampler origenProfiles;
    private final industries.angelic.biomic.world.terrain.RegionalHabitatSampler habitats;

    public SurfaceBiomeSampler(WorldgenContext context, MacroTerrainSampler macroSampler) {
        if (context == null) {
            throw new IllegalArgumentException("Worldgen context cannot be null");
        }
        if (macroSampler == null) {
            throw new IllegalArgumentException("Macro terrain sampler cannot be null");
        }
        this.macroSampler = macroSampler;
        this.origenProfiles = new OrigenVerticalProfileSampler(context);
        habitats = new industries.angelic.biomic.world.terrain.RegionalHabitatSampler(context);
    }

    public BiomeGenBase sample(int blockX, int blockZ) {
        // Marine landforms retain their ocean/coastal 2D fallback. Their actual dry caps receive
        // RU identity in the physical 3D volume, never a forest label across the surrounding water.
        if (!industries.angelic.biomic.world.terrain.RegionalHabitatSampler
            .isMarineFoundation(origenProfiles.sampleSurface(blockX, blockZ))) {
            String habitat = habitats.sample(blockX, blockZ);
            // A province is not proof of deep water. Its actual admitted seabed receives Hyacinth identity in
            // the 3D volume; rejected shelves must fall back to the underlying Origen coast, not to Hyacinth.
            if (habitat != null
                && industries.angelic.biomic.world.terrain.HostedVegetationPolicy.minimumWaterDepth(habitat) == 0)
                return industries.angelic.biomic.world.modern.ModernBiomeCompatibility.biome(habitat);
        }
        OrigenBiomeProfile profile = origenProfiles.sampleSurface(blockX, blockZ);
        BiomeGenBase surface = BiomicBiomes.getOrigenBiome(profile);
        if (surface != null && profile.isSurface()) return surface;
        // A malformed or future cave-only surface profile still gets the established safe fallback below.

        MacroTerrainSample macro = macroSampler.sample(blockX, blockZ);
        OrigenBiomeProfile origenProfile = macro.getOrigenProvince()
            .getPrimary();

        BiomeGenBase origen = BiomicBiomes.getOrigenBiome(origenProfile);
        if (origen != null && origenProfile.isSurface()) {
            return origen;
        }

        if (macro.getTopology() != SurfaceTopology.CONTINENT) {
            return BiomicBiomes.getTemperateArchipelago();
        }
        SurfaceLandform landform = macro.getLandform();
        if (landform == SurfaceLandform.SLOPE || landform == SurfaceLandform.PEAK
            || landform == SurfaceLandform.CLIFF) {
            return BiomicBiomes.getWindsweptHighlands();
        }
        return BiomicBiomes.getTemperateLowlands();
    }
}
