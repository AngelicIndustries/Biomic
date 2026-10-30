package industries.angelic.biomic.client;

import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import industries.angelic.biomic.api.BiomeColorAccess;
import industries.angelic.biomic.api.BiomicBiomeApi;
import industries.angelic.biomic.world.WorldTypeBiomicBlend;

/** Uses Y-aware biome snapshots where available, with unchanged 2D fallback for other worlds/renderers. */
public final class HostedBiomeColors {

    private HostedBiomeColors() {}

    public static boolean enabled(World world) {
        if (world == null || world.getWorldInfo()
            .getTerrainType() == null) return false;
        String type = world.getWorldInfo()
            .getTerrainType()
            .getWorldTypeName();
        return enabledType(type);
    }

    static boolean enabledType(String type) {
        return WorldTypeBiomicBlend.ID.equals(type);
    }

    public static BiomeGenBase biome(IBlockAccess access, int x, int y, int z) {
        if (access instanceof BiomeColorAccess) {
            BiomeGenBase biome = ((BiomeColorAccess) access).biomic$colorBiome(x, y, z);
            if (biome != null) return biome;
        } else if (access instanceof World && enabled((World) access)) {
            return BiomicBiomeApi.getColorBiome((World) access, x, y, z);
        }
        return access.getBiomeGenForCoords(x, z);
    }
}
