package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import industries.angelic.biomic.biome.VerticalBiomeCatalogue;
import industries.angelic.biomic.biome.ru.RuBiomeCatalogue;
import industries.angelic.biomic.biome.ru.RuBiomeDefinition;
import industries.angelic.biomic.storage.BiomeVolume;

/** RU cave identities replace cave ecology, never choose the solid/air geometry or claim an exterior roof. */
final class HostedCaveHabitats {

    private static final Map<String, String> SOURCE_TO_RU = new HashMap<>();
    static {
        for (RuBiomeDefinition cave : RuBiomeCatalogue.caves()) {
            SOURCE_TO_RU.put(
                VerticalBiomeCatalogue.getOrigenCave(cave.getTerrainProfile())
                    .getKey(),
                cave.getBiomeKey()
                    .replace("biomic:", "regions_unexplored:"));
            SOURCE_TO_RU.put(
                cave.getBiomeKey(),
                cave.getBiomeKey()
                    .replace("biomic:", "regions_unexplored:"));
        }
    }
    private final String[] habitats;
    private final industries.angelic.biomic.api.ChunkVerticalLayout layout;

    /** The volume and physical materials share one height: 256 (schema 1) or 512 (shifted, schema 2). */
    HostedCaveHabitats(BiomeVolume original, byte[] materials) {
        int quarts = original.getQuartHeight();
        layout = industries.angelic.biomic.api.ChunkVerticalLayout.forHeight(original.getBlockHeight());
        if (materials.length != layout.blockCount())
            throw new IllegalArgumentException("Cave habitats need matching heights");
        habitats = new String[16 * quarts];
        for (int qz = 0; qz < 4; qz++) for (int qx = 0; qx < 4; qx++) {
            int x = qx * 4 + 2, z = qz * 4 + 2;
            // Only consider terrain connected to the existing subterranean biome field. The
            // original volume has explicit floating/surface entries, which cannot become caves.
            for (int qy = 1; qy < quarts; qy++) {
                String habitat = SOURCE_TO_RU.get(original.getBiomeKey(qx, qy, qz));
                if (habitat == null) continue;
                int y = qy * 4 + 2;
                boolean enclosed = false;
                for (int roof = y + 1; roof < layout.height() - 3; roof++) {
                    if (solid(materials, x, roof, z) && solid(materials, x, roof + 1, z)
                        && solid(materials, x, roof + 2, z)) {
                        enclosed = true;
                        break;
                    }
                }
                if (enclosed) habitats[(qy * 4 + qz) * 4 + qx] = habitat;
            }
        }
    }

    private boolean solid(byte[] materials, int x, int y, int z) {
        byte block = materials[layout.index(x, y, z)];
        return block != BiomicTerrainSampler.AIR && block != BiomicTerrainSampler.WATER
            && block != BiomicTerrainSampler.LAVA;
    }

    String biome(int x, int y, int z) {
        return y < 4 || y >= layout.height() ? null : habitats[((y >> 2) * 4 + ((z & 15) >> 2)) * 4 + ((x & 15) >> 2)];
    }

    java.util.Set<String> biomes() {
        java.util.Set<String> result = new java.util.LinkedHashSet<>();
        for (String habitat : habitats) if (habitat != null) result.add(habitat);
        return result;
    }

    BiomeVolume project(BiomeVolume original) {
        ArrayList<String> palette = new ArrayList<>(original.getPalette());
        short[] cells = original.copyPaletteIndices();
        for (int i = 0; i < habitats.length; i++) {
            if (habitats[i] == null) continue;
            String key = habitats[i].replace("regions_unexplored:", "biomic:");
            int index = palette.indexOf(key);
            if (index < 0) {
                index = palette.size();
                palette.add(key);
            }
            cells[i] = (short) index;
        }
        return BiomeVolume.create(original.getGeneratorVersion(), original.getBlockHeight(), palette, cells);
    }
}
