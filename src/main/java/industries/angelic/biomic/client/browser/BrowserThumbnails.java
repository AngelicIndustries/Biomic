package industries.angelic.biomic.client.browser;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;

/**
 * The biome browser's thumbnails: 384x216 PNGs, the user's own captures under config/biomic/browser/thumbnails, else a
 * pack's defaults at {@code assets/<any domain>/biomic/thumbnails/<namespace>/<biome>.png}, a biome key
 * {@code namespace:path} naming {@code <namespace>/<path>.png}. Loaded lazily as textures: every small one (for the
 * list), the large ones only for the last few biomes shown.
 */
@SideOnly(Side.CLIENT)
final class BrowserThumbnails {

    static final int WIDTH = 384, HEIGHT = 216, SMALL_WIDTH = 64, SMALL_HEIGHT = 36;
    private static final int LARGE_KEPT = 6;

    /** Every biome's small texture once asked for; null when it has no thumbnail. */
    private static final Map<String, ResourceLocation> SMALL = new HashMap<>();
    private static final Map<String, ResourceLocation> LARGE = new LinkedHashMap<>(16, .75F, true);
    private static int serial;

    private BrowserThumbnails() {}

    /** The small texture of a biome's thumbnail, or null. */
    static ResourceLocation small(String key) {
        if (SMALL.containsKey(key)) return SMALL.get(key);
        BufferedImage image = read(key);
        ResourceLocation small = image == null ? null : register(fit(image, SMALL_WIDTH, SMALL_HEIGHT));
        SMALL.put(key, small);
        return small;
    }

    /** The full-size texture of a biome's thumbnail, or null. */
    static ResourceLocation large(String key) {
        if (small(key) == null) return null;
        ResourceLocation known = LARGE.get(key);
        if (known != null) return known;
        BufferedImage image = read(key);
        if (image == null) return null;
        ResourceLocation made = register(fit(image, WIDTH, HEIGHT));
        LARGE.put(key, made);
        if (LARGE.size() > LARGE_KEPT) {
            String eldest = LARGE.keySet()
                .iterator()
                .next();
            Minecraft.getMinecraft()
                .getTextureManager()
                .deleteTexture(LARGE.remove(eldest));
        }
        return made;
    }

    static boolean has(String key) {
        return small(key) != null;
    }

    /** Whether the thumbnail is the user's own capture (which can be removed), not a pack's default. */
    static boolean captured(String key) {
        return file(key).isFile();
    }

    /** Saves a capture as the biome's thumbnail: the frame's middle 16:9, scaled down. Returns the file. */
    static File save(String key, BufferedImage frame) throws IOException {
        File file = file(key);
        File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
        ImageIO.write(fit(frame, WIDTH, HEIGHT), "png", file);
        forget(key);
        return file;
    }

    /** Removes the user's capture; a pack's default shows again, if there is one. */
    static void remove(String key) {
        File file = file(key);
        if (file.isFile() && !file.delete()) Biomic.LOG.warn("Could not remove the thumbnail {}", file);
        forget(key);
    }

    private static void forget(String key) {
        ResourceLocation small = SMALL.remove(key);
        if (small != null) Minecraft.getMinecraft()
            .getTextureManager()
            .deleteTexture(small);
        ResourceLocation large = LARGE.remove(key);
        if (large != null) Minecraft.getMinecraft()
            .getTextureManager()
            .deleteTexture(large);
    }

    /** The user's capture, else a pack's default, else null. */
    private static BufferedImage read(String key) {
        try {
            File file = file(key);
            BufferedImage image = file.isFile() ? ImageIO.read(file) : null;
            return image != null ? image : packDefault(key);
        } catch (IOException | RuntimeException unreadable) {
            Biomic.LOG.warn("Could not read the thumbnail of {}: {}", key, unreadable.toString());
            return null;
        }
    }

    private static BufferedImage packDefault(String key) throws IOException {
        String path = "biomic/thumbnails/" + relative(key);
        for (Object domain : Minecraft.getMinecraft()
            .getResourceManager()
            .getResourceDomains()) {
            try (InputStream in = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(new ResourceLocation((String) domain, path))
                .getInputStream()) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) return image;
            } catch (java.io.FileNotFoundException absent) {
                // Not in this domain.
            }
        }
        return null;
    }

    private static ResourceLocation register(BufferedImage image) {
        return Minecraft.getMinecraft()
            .getTextureManager()
            .getDynamicTextureLocation("biomic_thumbnail_" + serial++, new DynamicTexture(image));
    }

    private static File file(String key) {
        return new File(new File(BrowserState.directory(), "thumbnails"), relative(key));
    }

    /** {@code namespace:path} as {@code namespace/path.png}, in safe characters only. */
    static String relative(String key) {
        String path = key.toLowerCase(Locale.ROOT)
            .replace(':', '/')
            .replaceAll("[^a-z0-9_./-]", "_");
        path = path.replaceAll("(^|/)\\.+(?=/|$)", "$1_");
        return path + ".png";
    }

    /** The image's middle at the width-to-height ratio of the size, scaled to it with area averaging. */
    static BufferedImage fit(BufferedImage image, int width, int height) {
        int w = image.getWidth(), h = image.getHeight();
        int cropW = Math.min(w, h * width / height), cropH = Math.min(h, w * height / width);
        BufferedImage crop = image.getSubimage((w - cropW) / 2, (h - cropH) / 2, cropW, cropH);
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = out.createGraphics();
        graphics.drawImage(crop.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING), 0, 0, null);
        graphics.dispose();
        return out;
    }

    /** The frame just rendered, as vanilla's screenshot reads it (ScreenShotHelper). */
    static BufferedImage grabFrame() {
        Minecraft mc = Minecraft.getMinecraft();
        Framebuffer framebuffer = mc.getFramebuffer();
        boolean fbo = OpenGlHelper.isFramebufferEnabled();
        int width = fbo ? framebuffer.framebufferTextureWidth : mc.displayWidth;
        int height = fbo ? framebuffer.framebufferTextureHeight : mc.displayHeight;
        IntBuffer buffer = BufferUtils.createIntBuffer(width * height);
        int[] pixels = new int[width * height];
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        if (fbo) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, framebuffer.framebufferTexture);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
        } else {
            GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
        }
        buffer.get(pixels);
        TextureUtil.func_147953_a(pixels, width, height);
        if (!fbo) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, width, height, pixels, 0, width);
            return image;
        }
        BufferedImage image = new BufferedImage(
            framebuffer.framebufferWidth,
            framebuffer.framebufferHeight,
            BufferedImage.TYPE_INT_RGB);
        int skip = framebuffer.framebufferTextureHeight - framebuffer.framebufferHeight;
        for (int y = skip; y < framebuffer.framebufferTextureHeight; y++)
            for (int x = 0; x < framebuffer.framebufferWidth; x++)
                image.setRGB(x, y - skip, pixels[y * framebuffer.framebufferTextureWidth + x]);
        return image;
    }
}
