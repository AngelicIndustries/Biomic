package industries.angelic.biomic.content;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.common.registry.GameRegistry;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.biome.ru.RuRuntimeManifest;
import industries.angelic.biomic.content.block.BlockRuDoublePlant;
import industries.angelic.biomic.content.block.BlockRuFlatPlant;
import industries.angelic.biomic.content.block.BlockRuLeaves;
import industries.angelic.biomic.content.block.BlockRuLog;
import industries.angelic.biomic.content.block.BlockRuPlant;
import industries.angelic.biomic.content.block.BlockRuSolid;
import industries.angelic.biomic.content.block.BlockRuSurfaceGrass;

/** Generated exact-texture palette for RU configured-feature states. */
public final class RuImportedDecorationBlocks {

    public static final String RESOURCE = "/assets/biomic/ru_reference/decoration-palette.json";

    private static final Map<String, Block> matureHangingHeads = new LinkedHashMap<>();
    private static final Map<String, Block[]> multifaceBlocks = new LinkedHashMap<>();
    private static final Map<String, Entry> ENTRIES = load();
    private static boolean registered;

    private RuImportedDecorationBlocks() {}

    public static Block get(String modernId) {
        Entry entry = ENTRIES.get(modernId);
        return entry == null ? null : entry.block;
    }

    public static Block matureHangingHead(String modernId) {
        return matureHangingHeads.get(modernId);
    }

    public static Block multiface(String modernId, int mask, boolean water) {
        return multifaceBlocks.get(modernId)[(mask >> 4) + (water ? 4 : 0)];
    }

    public static Map<String, Block> blocks() {
        Map<String, Block> blocks = new LinkedHashMap<>();
        for (Map.Entry<String, Entry> entry : ENTRIES.entrySet()) {
            blocks.put(entry.getKey(), entry.getValue().block);
        }
        for (var entry : matureHangingHeads.entrySet())
            blocks.put("regions_unexplored:mature_" + RuCaveModelData.name(entry.getKey()), entry.getValue());
        for (var entry : multifaceBlocks.entrySet())
            for (int i = 1; i < 8; i++) blocks.put(entry.getKey() + "_attachments_" + i, entry.getValue()[i]);
        return Collections.unmodifiableMap(blocks);
    }

    public static synchronized void register() {
        if (registered) return;
        for (Entry entry : ENTRIES.values()) {
            if (entry.block instanceof BlockRuDoublePlant) {
                GameRegistry.registerBlock(
                    entry.block,
                    industries.angelic.biomic.content.item.ItemBlockRuPlant.class,
                    entry.registryName);
            } else GameRegistry.registerBlock(entry.block, entry.registryName);
            String id = "regions_unexplored:" + entry.registryName.substring("ru_decor_".length());
            boolean netherFireproof = industries.angelic.biomic.world.modern.ModernGroundCoverSupport.isNetherPlant(id)
                || id.startsWith("regions_unexplored:brimwood_")
                || id.startsWith("regions_unexplored:cobalt_")
                || id.startsWith("regions_unexplored:hanging_earlight")
                || id.startsWith("regions_unexplored:glistering_ivy");
            if (!netherFireproof && entry.block.getMaterial() == Material.plants)
                Blocks.fire.setFireInfo(entry.block, 30, 100);
            else if (!netherFireproof && entry.block.getMaterial() == Material.wood)
                Blocks.fire.setFireInfo(entry.block, 5, 5);
        }
        registered = true;
        for (var entry : matureHangingHeads.entrySet())
            GameRegistry.registerBlock(entry.getValue(), "ru_decor_mature_" + RuCaveModelData.name(entry.getKey()));
        for (var entry : multifaceBlocks.entrySet()) for (int i = 1; i < 8; i++) GameRegistry.registerBlock(
            entry.getValue()[i],
            "ru_decor_" + RuCaveModelData.name(entry.getKey()) + "_attachments_" + i);
        Biomic.LOG.info("Registered {} imported RU configured-feature blocks", ENTRIES.size());
    }

    private static Map<String, Entry> load() {
        InputStream stream = RuImportedDecorationBlocks.class.getResourceAsStream(RESOURCE);
        if (stream == null) throw new IllegalStateException("Missing generated RU decoration palette " + RESOURCE);
        try (InputStream input = stream;
            InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader)
                .getAsJsonObject();
            require(
                "biomic-ru-decoration-palette-v1".equals(
                    root.get("format")
                        .getAsString()),
                "unexpected format");
            require(
                RuRuntimeManifest.SOURCE_REVISION.equals(
                    root.get("source_revision")
                        .getAsString()),
                "source revision drift");
            Map<String, Entry> result = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("entries")) {
                JsonObject object = element.getAsJsonObject();
                String id = string(object, "id");
                String registryName = string(object, "registry_name");
                String localName = registryName.substring("ru_decor_".length());
                String kind = string(object, "kind");
                int light = integer(object, "light", 0);
                Block block;
                if (RuCaveModelData.handles(id) && !RuCaveModelData.legacyDoublePlant(id)) {
                    String particle = object.has("texture") ? string(object, "texture")
                        : object.has("lower") ? string(object, "lower") : string(object, "top");
                    block = new industries.angelic.biomic.content.block.BlockRuCaveModel(
                        localName,
                        particle,
                        light,
                        RuCaveModelData.alwaysWaterFilled(id));
                    if (RuCaveModelData.multiface(id)) {
                        Block[] variants = new Block[8];
                        variants[0] = block;
                        for (int bank = 1; bank < 8; bank++)
                            variants[bank] = new industries.angelic.biomic.content.block.BlockRuCaveModel(
                                localName,
                                particle,
                                light,
                                bank >= 4,
                                (bank & 3) * 16);
                        multifaceBlocks.put(id, variants);
                    }
                    if (RuCaveModelData.hangingHead(id)) {
                        Block mature = new industries.angelic.biomic.content.block.BlockRuCaveModel(
                            localName,
                            particle,
                            light,
                            false,
                            16);
                        matureHangingHeads.put(id, mature);
                    }
                } else if (id.equals("regions_unexplored:ashen_dirt")) {
                    block = new industries.angelic.biomic.content.block.BlockRuAshenDirt();
                } else if (id.equals("regions_unexplored:ash_vent")) {
                    block = new industries.angelic.biomic.content.block.BlockRuAshVent(
                        "imported_" + localName,
                        string(object, "side"),
                        string(object, "top"));
                } else if (id.equals("regions_unexplored:ashen_grass")) {
                    block = new industries.angelic.biomic.content.block.BlockRuAshenGrass(
                        "imported_" + localName,
                        string(object, "texture"));
                } else if (id.endsWith("_leaf_litter")) {
                    block = new industries.angelic.biomic.content.block.BlockRuGroundCover(
                        "imported_" + localName,
                        string(object, "texture"));
                } else if ("double".equals(kind)) {
                    block = new BlockRuDoublePlant(
                        "imported_" + localName,
                        new String[] { localName },
                        new String[] { string(object, "lower") },
                        new String[] { string(object, "upper") },
                        doubleTint(string(object, "tint")));
                    if (light > 0) block.setLightLevel(light / 15.0F);
                } else if ("flat".equals(kind)) {
                    block = new BlockRuFlatPlant(
                        "imported_" + localName,
                        string(object, "texture"),
                        "BIOME_FOLIAGE".equals(string(object, "tint")),
                        light);
                } else if ("surface_grass".equals(kind)) {
                    block = new BlockRuSurfaceGrass(
                        "imported_" + localName,
                        string(object, "bottom"),
                        string(object, "top"),
                        string(object, "side"),
                        string(object, "overlay"),
                        string(object, "snowy"));
                } else if ("solid".equals(kind)) {
                    Material material = "WOOD".equals(string(object, "material")) ? Material.wood
                        : "GROUND".equals(string(object, "material")) ? Material.ground : Material.rock;
                    block = new BlockRuSolid(
                        "imported_" + localName,
                        string(object, "side"),
                        string(object, "top"),
                        object.has("bottom") ? string(object, "bottom") : string(object, "top"),
                        material,
                        light,
                        object.has("plant_soil") && object.get("plant_soil")
                            .getAsBoolean());
                } else if ("log".equals(kind)) {
                    block = object.has("side_transition")
                        ? new BlockRuLog(
                            "imported_" + localName,
                            new String[] { string(object, "side"), string(object, "side_transition") },
                            new String[] { string(object, "top"), string(object, "top") })
                        : new BlockRuLog("imported_" + localName, string(object, "side"), string(object, "top"));
                    if (light > 0) block.setLightLevel(light / 15.0F);
                } else if ("leaves".equals(kind)) {
                    block = new BlockRuLeaves(
                        "imported_" + localName,
                        string(object, "texture"),
                        BlockRuLeaves.TintMode.CONSTANT,
                        0xFFFFFF);
                } else if ("coneflower".equals(kind)) {
                    block = new industries.angelic.biomic.content.block.BlockRuConeflower(
                        "imported_" + localName,
                        string(object, "texture"),
                        string(object, "stem"));
                } else if ("plant".equals(kind)) {
                    block = new BlockRuPlant(
                        "imported_" + localName,
                        string(object, "texture"),
                        null,
                        plantTint(string(object, "tint")),
                        0xFFFFFF,
                        light);
                } else {
                    throw new IllegalStateException("Unknown generated RU decoration block kind " + kind);
                }
                require(result.put(id, new Entry(registryName, block)) == null, "duplicate block " + id);
            }
            require(result.size() >= 92, "active palette lost configured-feature blocks, got " + result.size());
            return Collections.unmodifiableMap(result);
        } catch (Exception failure) {
            throw new IllegalStateException("Could not load generated RU decoration palette " + RESOURCE, failure);
        }
    }

    private static BlockRuPlant.TintMode plantTint(String value) {
        if ("BIOME_GRASS".equals(value)) return BlockRuPlant.TintMode.BIOME_GRASS;
        if ("BIOME_FOLIAGE".equals(value)) return BlockRuPlant.TintMode.BIOME_FOLIAGE;
        return BlockRuPlant.TintMode.CONSTANT;
    }

    private static BlockRuDoublePlant.TintMode doubleTint(String value) {
        if ("BIOME_GRASS".equals(value)) return BlockRuDoublePlant.TintMode.BIOME_GRASS;
        if ("BIOME_FOLIAGE".equals(value)) return BlockRuDoublePlant.TintMode.BIOME_FOLIAGE;
        return BlockRuDoublePlant.TintMode.CONSTANT;
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static int integer(JsonObject object, String key, int fallback) {
        return object.has(key) ? object.get(key)
            .getAsInt() : fallback;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Invalid RU decoration palette: " + message);
    }

    private static final class Entry {

        private final String registryName;
        private final Block block;

        private Entry(String registryName, Block block) {
            this.registryName = registryName;
            this.block = block;
        }
    }
}
