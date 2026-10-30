package industries.angelic.biomic.biome;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.biome.BiomeGenBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Pinned visual biome effects for the three selected Alex's Caves habitats; not numeric biome registrations. */
public final class AlexCavesBiomeVisuals {

    public record Profile(String key, float temperature, float downfall, int grassColor, int foliageColor,
        int waterColor, int skyColor) {}

    private static final Map<String, Profile> PROFILES = profiles();

    private AlexCavesBiomeVisuals() {}

    public static Profile profile(String key) {
        return PROFILES.get(key);
    }

    /** Color-only facade used by Y-aware render snapshots. It is deliberately absent from the numeric registry. */
    public static BiomeGenBase colorBiome(String key) {
        return ColorBiomes.BY_KEY.get(key);
    }

    private static Map<String, Profile> profiles() {
        Map<String, Profile> out = new LinkedHashMap<>();
        out.put(
            "alexscaves:forlorn_hollows",
            new Profile("alexscaves:forlorn_hollows", .7F, .2F, -1, -1, 0x5D6A84, 0x28211A));
        out.put(
            "alexscaves:primordial_caves",
            new Profile("alexscaves:primordial_caves", .95F, .4F, 0xB3D600, 0xA1C442, 0x05A599, 0xF1C973));
        out.put(
            "alexscaves:abyssal_chasm",
            new Profile("alexscaves:abyssal_chasm", .5F, .5F, -1, -1, 0x00154B, 0x7BA4FF));
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, BiomeGenBase> colorBiomes() {
        Map<String, BiomeGenBase> out = new LinkedHashMap<>();
        for (Profile profile : PROFILES.values()) out.put(profile.key(), new ColorBiome(profile));
        return Collections.unmodifiableMap(out);
    }

    private static final class ColorBiomes {

        private static final Map<String, BiomeGenBase> BY_KEY = colorBiomes();
    }

    private static final class ColorBiome extends BiomeGenBase {

        private final Profile profile;

        ColorBiome(Profile profile) {
            super(0, false);
            this.profile = profile;
            setBiomeName("Biomic visual " + profile.key());
            setTemperatureRainfall(profile.temperature(), profile.downfall());
            waterColorMultiplier = profile.waterColor();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getBiomeGrassColor(int x, int y, int z) {
            return profile.grassColor() < 0 ? super.getBiomeGrassColor(x, y, z)
                : getModdedBiomeGrassColor(profile.grassColor());
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getBiomeFoliageColor(int x, int y, int z) {
            return profile.foliageColor() < 0 ? super.getBiomeFoliageColor(x, y, z)
                : getModdedBiomeFoliageColor(profile.foliageColor());
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getSkyColorByTemp(float ignored) {
            return profile.skyColor();
        }
    }
}
