package industries.angelic.biomic.content.gardens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class GardensAssetContractTest {

    @Test
    public void intrinsicTexturesAndWhistlesRemainPinnedUpstreamBytes() throws Exception {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put(
            "textures/blocks/gardens/blightwart_block.png",
            "eb335c0fa7e967fac713457f0019ff5a81897ab564f36f56688c554c13efa61c");
        expected.put(
            "textures/blocks/gardens/blistercrown.png",
            "6fd8e114ffefc52fc66671f2cac5ba56609ad6086325e93dd6403770745072d0");
        expected.put(
            "textures/blocks/gardens/glowing_soul_spore.png",
            "cf06f065e6de4f0c41690f18f38e4f879eb434fdd2bca6cadc2eeeac4306fe9c");
        expected.put(
            "textures/blocks/gardens/soul_spore.png",
            "8908adf2f58093b7efe7f3e9daaa7963385233d63565e2cf79f52a100cafe317");
        expected.put(
            "textures/blocks/gardens/soul_spore_top.png",
            "e2d6f2835c48cc0c75cce00865c25f20a600153225bb51e55ed0d0e55ced9db9");
        expected.put(
            "textures/blocks/gardens/soulblight_fungus.png",
            "44f8fc24469368b3364070871f75429897c274fe8d2bf8720700e0ca1e35d8ee");
        expected.put(
            "textures/blocks/gardens/soulblight_sprouts.png",
            "655305657e50bf967ef9a075c97cb8b60a71b8c91f72659c040318a8896f1c9b");
        expected.put(
            "textures/blocks/gardens/soulblight_stem.png",
            "e8d267a189ccb28e2d8549cb18b3e304871fef24b271a6c656f2bffad779655f");
        expected.put(
            "textures/blocks/gardens/soulblight_stem_top.png",
            "0fa4d9abbc9a9071f6d07930f194880fdd55de64597d41032f961fc527b44086");
        expected.put(
            "textures/blocks/gardens/tall_blistercrown_bottom.png",
            "e49a7f88c7f33c736070be069570d54bd873ecc660401c45e1dbeedbd7aadfda");
        expected.put(
            "textures/blocks/gardens/tall_blistercrown_top.png",
            "20ea093c9e4b96e633d2ded5e009b6c74f61824a4784d23360f2eca6dd175df8");
        expected.put(
            "textures/blocks/gardens/whistlecane0.png",
            "eaf27abff58b94c7ea04efb85b93cfa2d334400f94b071a4ed505867278b2c3d");
        expected.put(
            "textures/blocks/gardens/whistlecane1.png",
            "3365ae4e467b341d9adcd655c7afef7ebf84037e147c638e6c1801e5d83179e5");
        expected.put(
            "textures/blocks/gardens/whistlecane_leaves0.png",
            "96c5a5495d12970f3053bbe95d84c1e1309d9cf1e0ff6252cf6d3e216ff61a27");
        expected.put(
            "textures/blocks/gardens/whistlecane_leaves1.png",
            "df173efcbef8c261a8427cc2c6d55533eb77ba2bfad51d67ebcd0d55c8e50a50");
        expected.put(
            "textures/blocks/gardens/whistlecane_item.png",
            "51bead8941b08939da06c6cd137a9a6c81c8d20f671d2ef323dc14758957d623");
        expected.put("sounds/gardens/whistle1.ogg", "6e38f596b2c59ddfdb3a879f4c96f0bfc84a0025a0258eaeead1a59786ba6fcc");
        expected.put("sounds/gardens/whistle2.ogg", "77dcb7d5ba401430b0e9107568b6ec21a59fa4708275e3e2f41a66a9f68978cf");
        expected.put(
            "gardens_reference/models/whistlecane0.json",
            "624138b7ffc1c06e2b414ab61b1ecfc6c7becfdd96d748365133c6f7bf1d78f2");
        expected.put(
            "gardens_reference/models/whistlecane1.json",
            "eeb1f2b955ce7727c21736868fa8a57be86beb679346f3258617c436c395883f");
        expected.put(
            "gardens_reference/models/whistlecane2.json",
            "4050f61ebeaa9c32edfabd8b8ce3c02889c2d41ce2a5e7ad0ffea1baade7d2a6");
        for (var entry : expected.entrySet()) {
            var stream = getClass().getResourceAsStream("/assets/biomic/" + entry.getKey());
            assertNotNull("Missing Gardens source asset " + entry.getKey(), stream);
            try (stream) {
                assertEquals(
                    "Gardens source asset drift " + entry.getKey(),
                    entry.getValue(),
                    HexFormat.of()
                        .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                .digest(stream.readAllBytes())));
            }
        }
    }

    @Test
    public void whistleSoundTableKeepsSourceWeightingWithoutClaimingMusic() throws Exception {
        var stream = getClass().getResourceAsStream("/assets/biomic/sounds.json");
        assertNotNull(stream);
        String json;
        try (stream) {
            json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(json.contains("\"gardens_whistlecane\""));
        assertTrue(json.contains("\"biomic:gardens/whistle1\""));
        assertTrue(json.contains("\"biomic:gardens/whistle2\""));
        assertTrue(json.contains("\"weight\": 2"));
    }
}
