package industries.angelic.biomic.content.alexscaves;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AbyssalVentAssetsTest {

    @Test
    public void allBubblingAudioMatchesPinnedBytes() throws Exception {
        try (InputStream stream = getClass()
            .getResourceAsStream("/assets/biomic/alexscaves/natural-vent-source-manifest.json")) {
            assertNotNull(stream);
            JsonObject manifest = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject();
            assertFalse(manifest.has("sprites"));
            assertEquals(
                7,
                manifest.getAsJsonObject("sounds")
                    .entrySet()
                    .size());
            for (String group : new String[] { "sounds" }) {
                String prefix = manifest.get("sound_target_prefix")
                    .getAsString();
                for (var entry : manifest.getAsJsonObject(group)
                    .entrySet())
                    try (InputStream asset = getClass().getResourceAsStream("/" + prefix + entry.getKey())) {
                        assertNotNull(entry.getKey(), asset);
                        byte[] digest = MessageDigest.getInstance("SHA-256")
                            .digest(asset.readAllBytes());
                        StringBuilder hex = new StringBuilder();
                        for (byte b : digest) hex.append(String.format("%02x", b & 255));
                        assertEquals(
                            entry.getKey(),
                            entry.getValue()
                                .getAsString(),
                            hex.toString());
                    }
            }
        }
    }
}
