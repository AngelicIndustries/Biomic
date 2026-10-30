package industries.angelic.biomic.client.render;

import java.awt.image.BufferedImage;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.IIcon;

/** Lossless atlas boundary for RU's static 2:1 branch sprites. Original files remain unmodified. */
public final class RuBranchSprite extends TextureAtlasSprite {

    private RuBranchSprite(String name) {
        super(name);
    }

    public static IIcon register(IIconRegister register, String name) {
        if (register instanceof TextureMap) {
            TextureMap map = (TextureMap) register;
            map.setTextureEntry(name, new RuBranchSprite(name));
            return map.getTextureExtry(name);
        }
        return register.registerIcon(name);
    }

    @Override
    public void loadSprite(BufferedImage[] images, AnimationMetadataSection animation, boolean anisotropic) {
        if (animation != null) throw new IllegalArgumentException("RU branch sprites are static: " + getIconName());
        BufferedImage[] padded = new BufferedImage[images.length];
        for (int level = 0; level < images.length; level++) {
            if (images[level] != null) padded[level] = pad(images[level]);
        }
        try {
            super.loadSprite(padded, null, anisotropic);
        } finally {
            // lwjgl3ify closes the images passed to super, not the originals replaced by padding.
            for (int level = 0; level < images.length; level++) {
                if (images[level] != padded[level] && images[level] instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable) images[level]).close();
                    } catch (Exception failure) {
                        throw new IllegalStateException("Closing RU branch source", failure);
                    }
                }
            }
        }
    }

    /** Model UVs use only the upper half of this square, just like the existing eucalyptus renderer. */
    static BufferedImage pad(BufferedImage source) {
        int width = source.getWidth(), height = source.getHeight();
        if (width == height) return source; // Already padded resource-pack replacement.
        if (width != height * 2) throw new IllegalArgumentException("Expected square or 2:1 RU branch sprite");
        BufferedImage result = new BufferedImage(width, width, BufferedImage.TYPE_INT_ARGB);
        // lwjgl3ify's NativeBackedImage requires a supplied pixel array (unlike BufferedImage).
        int[] pixels = new int[width * height];
        source.getRGB(0, 0, width, height, pixels, 0, width);
        result.setRGB(0, 0, width, height, pixels, 0, width);
        return result;
    }
}
