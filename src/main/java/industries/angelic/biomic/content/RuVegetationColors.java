package industries.angelic.biomic.content;

import java.awt.Color;

import net.minecraft.util.MathHelper;

/** Pinned RU RuColors.getAspenColor/getEnchantedAspenColor, including float/table-sine semantics. */
public final class RuVegetationColors {

    private RuVegetationColors() {}

    public static int aspen(int x, int z, boolean enchanted) {
        float wave = MathHelper.sin((float) x / 10 + MathHelper.sin(((float) z + (float) x) / 50) * 3);
        float hue = enchanted ? wave / 50 + 0.58F : wave / 75 + 0.15F;
        return Color.HSBtoRGB(hue, enchanted ? 0.8F : 0.75F, enchanted ? 1.0F : 0.8F) & 0xFFFFFF;
    }
}
