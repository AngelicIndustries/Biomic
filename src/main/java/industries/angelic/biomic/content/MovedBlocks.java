package industries.angelic.biomic.content;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Blocks Biomic once registered that another mod now owns (the pack's content, moved to Adventura Arcana Core): worlds
 * made before keep them. FML sends a missing name only to the mod it belongs to, so Biomic remaps its old names to the
 * new owner's blocks and items, when that mod registered them.
 */
public final class MovedBlocks {

    private static final String AAC = "adventuraarcanacore";
    private static final Map<String, String> MOVED = Map.ofEntries(
        Map.entry("wild_bush", AAC),
        Map.entry("coffeebean_pod", AAC),
        Map.entry("curryleaf_pod", AAC),
        Map.entry("spiceleaf_pod", AAC),
        Map.entry("tealeaf_pod", AAC),
        Map.entry("grape_vine", AAC),
        Map.entry("grape_vine_bare", AAC),
        Map.entry("kiwi_vine", AAC),
        Map.entry("kiwi_vine_bare", AAC),
        Map.entry("bamboo_shoot", AAC),
        Map.entry("bamboo_shoot_asper", AAC),
        Map.entry("bamboo_shoot_fargesia_robusta", AAC),
        Map.entry("bamboo_shoot_giant_timber", AAC),
        Map.entry("bamboo_shoot_golden", AAC),
        Map.entry("bamboo_shoot_moso", AAC),
        Map.entry("bamboo_shoot_short_tassled", AAC),
        Map.entry("bamboo_shoot_timor_black", AAC),
        Map.entry("bamboo_shoot_tropical_blue", AAC),
        Map.entry("bamboo_shoot_wet_forest", AAC),
        Map.entry("bamboo_shoot_madake", AAC));

    private MovedBlocks() {}

    public static void remap(FMLMissingMappingsEvent event) {
        for (FMLMissingMappingsEvent.MissingMapping mapping : event.get()) {
            String name = mapping.name.substring(mapping.name.indexOf(':') + 1);
            String owner = MOVED.get(name);
            if (owner == null) continue;
            if (mapping.type == GameRegistry.Type.BLOCK) {
                Block block = GameRegistry.findBlock(owner, name);
                if (block != null) mapping.remap(block);
            } else {
                Item item = GameRegistry.findItem(owner, name);
                if (item != null) mapping.remap(item);
            }
        }
    }
}
