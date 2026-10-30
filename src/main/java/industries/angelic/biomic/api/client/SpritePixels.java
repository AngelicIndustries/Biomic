package industries.angelic.biomic.api.client;

import java.awt.Color;

/**
 * Pixels for PaintedSprite: 16x16 ARGB images read from installed textures, and the colour arithmetic Biomic paints
 * its own sprites with. Nothing here touches client classes.
 */
public final class SpritePixels {

    public static final int SIZE = 16;

    /** Reads an installed texture as 16x16 ARGB pixels, or null when it is not there. */
    @FunctionalInterface
    public interface Source {

        int[] read(String domain, String path);
    }

    private SpritePixels() {}

    public static double luminance(int argb) {
        return 0.299 * (argb >> 16 & 255) + 0.587 * (argb >> 8 & 255) + 0.114 * (argb & 255);
    }

    /** The RGB scaled by {@code factor}, opaque. */
    public static int scale(int rgb, double factor) {
        int r = (int) clamp((rgb >> 16 & 255) * factor, 0, 255), g = (int) clamp((rgb >> 8 & 255) * factor, 0, 255),
            b = (int) clamp((rgb & 255) * factor, 0, 255);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** The RGB multiplied by a tint, as a biome colour tints grass (alpha dropped). */
    public static int multiply(int argb, int tint) {
        return ((argb >> 16 & 255) * (tint >> 16 & 255) / 255) << 16
            | ((argb >> 8 & 255) * (tint >> 8 & 255) / 255) << 8
            | (argb & 255) * (tint & 255) / 255;
    }

    /** From one RGB to another by {@code t} (0 to 1; alpha dropped). */
    public static int mix(int from, int to, double t) {
        int r = (int) ((from >> 16 & 255) * (1 - t) + (to >> 16 & 255) * t);
        int g = (int) ((from >> 8 & 255) * (1 - t) + (to >> 8 & 255) * t);
        int b = (int) ((from & 255) * (1 - t) + (to & 255) * t);
        return r << 16 | g << 8 | b;
    }

    public static float[] hsb(int rgb) {
        return Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, null);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
