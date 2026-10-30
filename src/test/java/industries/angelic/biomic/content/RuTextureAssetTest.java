package industries.angelic.biomic.content;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.biomic.client.VanillaTextures;

public class RuTextureAssetTest {

    private static final String REVISION = "f5dfe4eea916cb7ccb4990faebc952459dad5f8b";

    @Test
    public void generatedModernRuTexturePalettesAreCompleteAndByteExact() throws IOException, NoSuchAlgorithmException {
        // Includes source cave faces, oriented icicles and animated brimwood transition artwork.
        assertPalette("assets/biomic/ru_reference/decoration-palette.json", "biomic-ru-decoration-palette-v1", 226);
        assertSquare("assets/biomic/textures/blocks/ru_decor/alpha_grass_block_top.png");
        assertSquare("assets/biomic/textures/blocks/ru_decor/alpha_grass_side.png");
        assertSquare("assets/biomic/textures/blocks/ru_decor/alpha_grass_side_snow.png");
    }

    @Test
    public void legacyAtlasSpritesAreSquare() throws IOException {
        assertSquare("assets/biomic/textures/blocks/joshua_leaves_bottom.png");
        assertSquare("assets/biomic/textures/blocks/joshua_leaves_top.png");
        assertSquare("assets/biomic/textures/blocks/joshua_leaves_bottom_right.png");
        assertSquare("assets/biomic/textures/blocks/joshua_leaves_top_right.png");
        assertSquare("assets/biomic/textures/blocks/chalk_grass_side.png");
        assertSquare("assets/biomic/textures/blocks/chalk_grass_snow.png");
        assertSquare("assets/biomic/textures/blocks/ru_grass_block_side_overlay.png");
        assertSquare("assets/biomic/textures/blocks/stripped_pine_log.png");
        assertSquare("assets/biomic/textures/blocks/stripped_pine_log_top.png");
        assertSquare("assets/biomic/textures/blocks/pine_log_transition.png");
        assertSquare("assets/biomic/textures/blocks/palm_log.png");
        assertSquare("assets/biomic/textures/blocks/palm_log_top.png");
        assertSquare("assets/biomic/textures/blocks/palm_leaves_side.png");
        assertSquare("assets/biomic/textures/blocks/palm_leaves_top.png");
        assertSquare("assets/biomic/textures/blocks/palm_beard.png");
        assertSquare("assets/biomic/textures/blocks/eucalyptus_branch.png");
    }

    @Test
    public void alphaGrassSpritesAreRenderingOverridesRatherThanRegisteredBlocks() throws IOException {
        JsonObject manifest = readJson("assets/biomic/ru_reference/decoration-palette.json");
        JsonArray entries = manifest.getAsJsonArray("entries");
        for (JsonElement element : entries) {
            Assert.assertNotEquals(
                "regions_unexplored:alpha_grass_block",
                element.getAsJsonObject()
                    .get("id")
                    .getAsString());
        }
    }

    @Test
    public void splitJoshuaCrownSpritesReconstructThePinnedRuArtwork() throws IOException, NoSuchAlgorithmException {
        assertJoinedSpriteHash(
            "joshua_leaves_bottom",
            "9dcf2f87cb686860458a5e2b94a9302bb7f2b00350ae7d851f617d81aa026e30");
        assertJoinedSpriteHash("joshua_leaves_top", "4fd131b117bda10726f1abe93e01903071386443b906e9c9abd84fd6d432279a");
    }

    @Test
    public void paddedEucalyptusBranchPreservesVisibleRuPixelsAndHasATransparentLowerHalf()
        throws IOException, NoSuchAlgorithmException {
        BufferedImage image = read("assets/biomic/textures/blocks/eucalyptus_branch.png");
        Assert.assertEquals(32, image.getWidth());
        Assert.assertEquals(32, image.getHeight());

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int y = 0; y < 16; y++) {
            updateNormalizedRow(digest, image, y);
        }
        Assert.assertEquals("b386d8785560fe73e17c5d97473d71fad6c5b86992794975fa8c43fd2f477ef0", hex(digest.digest()));
        for (int y = 16; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                Assert.assertEquals("padding must be transparent", 0, image.getRGB(x, y) >>> 24);
            }
        }
    }

    private static void assertSquare(String path) throws IOException {
        try (InputStream stream = RuTextureAssetTest.class.getClassLoader()
            .getResourceAsStream(path)) {
            Assert.assertNotNull("missing test resource " + path, stream);
            BufferedImage image = ImageIO.read(stream);
            Assert.assertNotNull("unreadable texture " + path, image);
            Assert.assertEquals(
                "1.7 block atlas requires a square non-animated sprite",
                image.getWidth(),
                image.getHeight());
        }
    }

    private static void assertJoinedSpriteHash(String name, String expected)
        throws IOException, NoSuchAlgorithmException {
        BufferedImage left = read("assets/biomic/textures/blocks/" + name + ".png");
        BufferedImage right = read("assets/biomic/textures/blocks/" + name + "_right.png");
        Assert.assertEquals(left.getWidth(), right.getWidth());
        Assert.assertEquals(left.getHeight(), right.getHeight());

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int y = 0; y < left.getHeight(); y++) {
            updateRow(digest, left, y);
            updateRow(digest, right, y);
        }
        StringBuilder actual = new StringBuilder();
        for (byte value : digest.digest()) {
            actual.append(String.format("%02x", value & 0xFF));
        }
        Assert.assertEquals(name, expected, actual.toString());
    }

    private static BufferedImage read(String path) throws IOException {
        try (InputStream stream = RuTextureAssetTest.class.getClassLoader()
            .getResourceAsStream(path)) {
            Assert.assertNotNull("missing test resource " + path, stream);
            BufferedImage image = ImageIO.read(stream);
            Assert.assertNotNull("unreadable texture " + path, image);
            return image;
        }
    }

    private static void assertPalette(String manifestPath, String format, int expectedFiles)
        throws IOException, NoSuchAlgorithmException {
        JsonObject manifest = readJson(manifestPath);
        Assert.assertEquals(
            format,
            manifest.get("format")
                .getAsString());
        Assert.assertEquals(
            REVISION,
            manifest.get("source_revision")
                .getAsString());
        Assert.assertEquals(
            expectedFiles,
            manifest.getAsJsonArray("files")
                .size());
        for (JsonElement element : manifest.getAsJsonArray("files")) {
            JsonObject file = element.getAsJsonObject();
            String path = file.get("path")
                .getAsString();
            String resource = path.startsWith("src/main/resources/") ? path.substring("src/main/resources/".length())
                : path;
            if (VanillaTextures.downloads(resource.substring("assets/biomic/".length()))) {
                // Minecraft's own art, downloaded rather than shipped.
                Assert.assertNull(
                    resource,
                    RuTextureAssetTest.class.getClassLoader()
                        .getResource(resource));
                continue;
            }
            try (InputStream stream = RuTextureAssetTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
                Assert.assertNotNull("missing pinned texture " + resource, stream);
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] buffer = new byte[8192];
                int read;
                while ((read = stream.read(buffer)) >= 0) digest.update(buffer, 0, read);
                Assert.assertEquals(
                    resource,
                    file.get("sha256")
                        .getAsString(),
                    hex(digest.digest()));
            }
        }
    }

    private static JsonObject readJson(String path) throws IOException {
        try (InputStream stream = RuTextureAssetTest.class.getClassLoader()
            .getResourceAsStream(path)) {
            Assert.assertNotNull("missing palette " + path, stream);
            return new JsonParser().parse(new InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonObject();
        }
    }

    private static void updateRow(MessageDigest digest, BufferedImage image, int y) {
        for (int x = 0; x < image.getWidth(); x++) {
            int argb = image.getRGB(x, y);
            digest.update((byte) (argb >>> 24));
            digest.update((byte) (argb >>> 16));
            digest.update((byte) (argb >>> 8));
            digest.update((byte) argb);
        }
    }

    private static void updateNormalizedRow(MessageDigest digest, BufferedImage image, int y) {
        for (int x = 0; x < image.getWidth(); x++) {
            int argb = image.getRGB(x, y);
            if ((argb >>> 24) == 0) argb = 0;
            digest.update((byte) (argb >>> 24));
            digest.update((byte) (argb >>> 16));
            digest.update((byte) (argb >>> 8));
            digest.update((byte) argb);
        }
    }

    private static String hex(byte[] digest) {
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte value : digest) result.append(String.format("%02x", value & 0xFF));
        return result.toString();
    }
}
