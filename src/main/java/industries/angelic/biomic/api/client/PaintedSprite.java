package industries.angelic.biomic.api.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import industries.angelic.biomic.Biomic;

/**
 * A sprite painted when the block atlas is stitched, from textures installed with other mods (for Biomic's food: PMP's
 * berry bush, HarvestCraft's fruit, vanilla's vine), so a mod ships none of their art. Client only. The painter gets a
 * SpritePixels.Source and returns 16x16 ARGB pixels, or null for the missing-texture pattern; larger sources are
 * sampled down to their first square frame.
 */
public final class PaintedSprite extends TextureAtlasSprite {

    private static final int SIZE = SpritePixels.SIZE;

    private final Function<SpritePixels.Source, int[]> painter;

    private PaintedSprite(String name, Function<SpritePixels.Source, int[]> painter) {
        super(name);
        this.painter = painter;
    }

    /** Registers the sprite {@code name} (domain:path) with the block atlas, painted by {@code painter}. */
    public static IIcon register(IIconRegister register, String name, Function<SpritePixels.Source, int[]> painter) {
        if (register instanceof TextureMap map) {
            map.setTextureEntry(name, new PaintedSprite(name, painter));
            return map.getTextureExtry(name);
        }
        return register.registerIcon(name);
    }

    @Override
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return true;
    }

    /** False means loaded (TextureMap stitches the sprite only then). */
    @Override
    public boolean load(IResourceManager manager, ResourceLocation location) {
        int[] pixels;
        try {
            pixels = painter.apply((domain, path) -> read(manager, domain, path));
        } catch (RuntimeException failure) {
            Biomic.LOG.warn("Painted sprite {} could not be painted: {}", getIconName(), failure.toString());
            pixels = null;
        }
        if (pixels == null) pixels = missing();
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, SIZE, SIZE, pixels, 0, SIZE);
        loadSprite(new BufferedImage[] { image }, null, false);
        return false;
    }

    private static int[] read(IResourceManager manager, String domain, String path) {
        BufferedImage image;
        try (InputStream in = manager.getResource(new ResourceLocation(domain, path))
            .getInputStream()) {
            image = ImageIO.read(in);
        } catch (IOException | RuntimeException absent) {
            return null;
        }
        if (image == null) return null;
        try {
            int width = image.getWidth(), height = image.getHeight();
            int[] source = new int[width * height];
            image.getRGB(0, 0, width, height, source, 0, width);
            int[] pixels = new int[SIZE * SIZE];
            // Square frame only (animated strips use their first frame); nearest sampling down to 16.
            int frame = Math.min(width, height);
            for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++)
                pixels[y * SIZE + x] = source[(y * frame / SIZE) * width + x * frame / SIZE];
            return pixels;
        } finally {
            if (image instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private static int[] missing() {
        int[] pixels = new int[SIZE * SIZE];
        for (int i = 0; i < pixels.length; i++) pixels[i] = ((i / SIZE + i) & 8) == 0 ? 0xFFF800F8 : 0xFF000000;
        return pixels;
    }
}
