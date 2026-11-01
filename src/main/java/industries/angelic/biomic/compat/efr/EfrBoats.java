package industries.angelic.biomic.compat.efr;

import net.minecraft.item.Item;

import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.items.ItemNewBoat;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;
import industries.angelic.biomic.content.BiomicWoods;

/**
 * Et Futurum Requiem's modern boats for Biomic's woods, where EFR's new boats are on: the woods' boats and chest
 * boats are EFR's, registered with it as its own Biomes O' Plenty boat is, so they row, carry two and break like the
 * pack's other boats. EFR crafts them (planks, then a chest on a boat), tags them boatWood and boatChestWood, and draws
 * them with the art at biomic:textures/entity/boat/. Loaded only with EFR installed.
 */
public final class EfrBoats {

    private EfrBoats() {}

    public static boolean enabled() {
        return ConfigBlocksItems.enableNewBoats;
    }

    public static Item boat(BiomicWoods.Wood wood, boolean chest) {
        String name = wood.id + (chest ? "_chest_boat" : "_boat");
        ItemNewBoat boat = new ItemNewBoat(
            Biomic.MODID,
            wood.id,
            () -> Item.getItemFromBlock(wood.planks()),
            0,
            chest,
            false);
        boat.setUnlocalizedName(Biomic.MODID + "." + name);
        boat.setTextureName(Biomic.MODID + ":ru/" + name);
        boat.setCreativeTab(BiomicBlocks.CREATIVE_TAB);
        return boat;
    }
}
