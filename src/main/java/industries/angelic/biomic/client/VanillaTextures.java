package industries.angelic.biomic.client;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

import com.google.common.collect.ImmutableSet;

import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.compat.efr.VanillaArtwork;

/**
 * Mojang's textures under the names Biomic's blocks and particles use, read from the client jar that Asset Director
 * downloads ({@link VanillaArtwork}) rather than shipped. Before the first download (offline), they show as missing
 * textures. A resource pack can still replace them under Biomic's names.
 */
@SideOnly(Side.CLIENT)
public final class VanillaTextures implements IResourcePack {

    private static final String DOMAIN = "minecraft_" + VanillaArtwork.VERSION;

    /** Biomic's texture path to Minecraft's, both under {@code textures/}, Minecraft's named the 1.7.10 way. */
    private static final Map<String, String> TEXTURES = new HashMap<>();

    static {
        for (String name : new String[] { "big_dripleaf_side", "big_dripleaf_stem", "big_dripleaf_tip",
            "big_dripleaf_top", "brain_coral", "brain_coral_block", "brain_coral_fan", "bubble_coral",
            "bubble_coral_block", "bubble_coral_fan", "dead_brain_coral", "dead_brain_coral_block",
            "dead_brain_coral_fan", "dead_bubble_coral", "dead_bubble_coral_block", "dead_bubble_coral_fan",
            "dead_fire_coral", "dead_fire_coral_block", "dead_fire_coral_fan", "dead_horn_coral",
            "dead_horn_coral_block", "dead_horn_coral_fan", "dead_tube_coral", "dead_tube_coral_block",
            "dead_tube_coral_fan", "fire_coral", "fire_coral_block", "fire_coral_fan", "hanging_roots", "horn_coral",
            "horn_coral_block", "horn_coral_fan", "kelp", "kelp_plant", "mushroom_stem", "ochre_froglight_side",
            "ochre_froglight_top", "powder_snow", "rooted_dirt", "sea_pickle", "seagrass", "small_dripleaf_side",
            "small_dripleaf_stem_bottom", "small_dripleaf_stem_top", "small_dripleaf_top", "spore_blossom",
            "spore_blossom_base", "tall_seagrass_bottom", "tall_seagrass_top", "tube_coral", "tube_coral_block",
            "tube_coral_fan" }) put("blocks/" + name, "blocks/" + name);
        put("blocks/seagrass_item", "items/seagrass");
        put("blocks/clover_stem", "blocks/pink_petals_stem");
        put("blocks/ru_grass_block_top", "blocks/grass_block_top");
        put("blocks/ru/bamboo_log_leaves", "blocks/bamboo_large_leaves");
        put("blocks/ru_decor/clover_stem", "blocks/pink_petals_stem");
        put("blocks/ru_decor/ru_grass_block_top", "blocks/grass_block_top");
        put("blocks/ru_decor/ru_grass_block_side_overlay_short", "blocks/grass_block_side_overlay");
        for (String name : new String[] { "chain", "iron_trapdoor", "lightning_rod", "oxidized_cut_copper", "stone",
            "weathered_cut_copper" }) put("blocks/alexscaves/" + name, "blocks/" + name);
        for (int frame = 0; frame < 12; frame++) {
            put("blocks/gardens/whistle_smoke_" + frame, "particle/big_smoke_" + frame);
            put("particles/alexscaves/vent/big_smoke_" + frame, "particle/big_smoke_" + frame);
        }
    }

    private static void put(String biomic, String minecraft) {
        TEXTURES.put("textures/" + biomic + ".png", "textures/" + minecraft + ".png");
    }

    private final List<IResourcePack> packs;
    private IResourcePack downloaded;

    private VanillaTextures(List<IResourcePack> packs) {
        this.packs = packs;
    }

    /** Adds the pack to the game's built-in ones; from the client's pre-init. */
    public static void inject() {
        List<IResourcePack> packs = ReflectionHelper
            .getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "defaultResourcePacks", "field_110449_ao");
        VanillaTextures pack = new VanillaTextures(packs);
        packs.add(pack);
        IResourceManager manager = Minecraft.getMinecraft()
            .getResourceManager();
        if (manager instanceof SimpleReloadableResourceManager reloadable) reloadable.reloadResourcePack(pack);
    }

    /** Whether a Biomic resource path (e.g. {@code textures/blocks/kelp.png}) is one of Minecraft's, downloaded. */
    public static boolean downloads(String path) {
        return target(new ResourceLocation(Biomic.MODID, path)) != null;
    }

    /** The Minecraft resource a Biomic one stands for (an animated texture's .mcmeta too), or null. */
    private static ResourceLocation target(ResourceLocation location) {
        String path = location.getResourcePath(), suffix = path.endsWith(".mcmeta") ? ".mcmeta" : "";
        String minecraft = TEXTURES.get(path.substring(0, path.length() - suffix.length()));
        return minecraft == null ? null : new ResourceLocation(DOMAIN, minecraft + suffix);
    }

    /** Asset Director's pack over the downloaded jar, once it is there. */
    private IResourcePack downloaded() {
        if (downloaded == null) for (IResourcePack pack : packs) if (pack.getResourceDomains()
            .contains(DOMAIN)) downloaded = pack;
        return downloaded;
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        ResourceLocation target = target(location);
        IResourcePack source = downloaded();
        if (target == null || source == null || !source.resourceExists(target))
            throw new FileNotFoundException(location.toString());
        return source.getInputStream(target);
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        ResourceLocation target = target(location);
        IResourcePack source = target == null ? null : downloaded();
        return source != null && source.resourceExists(target);
    }

    @Override
    public Set<String> getResourceDomains() {
        return ImmutableSet.of(Biomic.MODID);
    }

    @Override
    public IMetadataSection getPackMetadata(IMetadataSerializer serializer, String section) {
        return null;
    }

    @Override
    public BufferedImage getPackImage() {
        return null;
    }

    @Override
    public String getPackName() {
        return "Biomic: Minecraft " + VanillaArtwork.VERSION + " textures";
    }
}
