package industries.angelic.biomic.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class BootstrapValidatorTest {

    @Test
    public void acceptsPinnedAndNewerDependencyVersions() {
        Map<String, String> versions = minimumVersions();
        versions.put("angellib", "1.0.1");
        versions.put("endlessids", "1.8.0");

        BootstrapValidator.validateDependencies(versions);
    }

    @Test
    public void rejectsMissingDependency() {
        Map<String, String> versions = minimumVersions();
        versions.remove("chunkapi");

        IllegalStateException failure = Assert
            .assertThrows(IllegalStateException.class, () -> BootstrapValidator.validateDependencies(versions));

        Assert.assertTrue(
            failure.getMessage()
                .contains("required mod 'chunkapi' is not loaded"));
    }

    @Test
    public void rejectsDependencyBelowMinimumVersion() {
        Map<String, String> versions = minimumVersions();
        versions.put("etfuturum", "2.6.53-GTNH");

        IllegalStateException failure = Assert
            .assertThrows(IllegalStateException.class, () -> BootstrapValidator.validateDependencies(versions));

        Assert.assertTrue(
            failure.getMessage()
                .contains("requires 2.6.54-GTNH or newer"));
    }

    @Test
    public void rejectsDisabledEfrMossAndAzaleaContent() {
        IllegalStateException failure = Assert.assertThrows(
            IllegalStateException.class,
            () -> BootstrapValidator.validateEfrLushCaveContent(false, true));

        Assert.assertTrue(
            failure.getMessage()
                .contains("enableMossAzalea=true"));
    }

    @Test
    public void rejectsDisabledEfrCaveVines() {
        IllegalStateException failure = Assert.assertThrows(
            IllegalStateException.class,
            () -> BootstrapValidator.validateEfrLushCaveContent(true, false));

        Assert.assertTrue(
            failure.getMessage()
                .contains("enableGlowBerries=true"));
    }

    @Test
    public void rejectsDisabledEfrCopperOre() {
        IllegalStateException failure = Assert
            .assertThrows(IllegalStateException.class, () -> BootstrapValidator.validateEfrCopperContent(false));

        Assert.assertTrue(
            failure.getMessage()
                .contains("enableCopper=true"));
        Assert.assertTrue(
            failure.getMessage()
                .contains("disableCopperOreAndIngotOnly=false"));
    }

    @Test
    public void shroomlightMustBeEnabledAndActuallyRegistered() {
        BootstrapValidator.validateEfrShroomlightContent(true, true);
        for (boolean[] state : new boolean[][] { { false, false }, { true, false }, { false, true } }) {
            IllegalStateException failure = Assert.assertThrows(
                IllegalStateException.class,
                () -> BootstrapValidator.validateEfrShroomlightContent(state[0], state[1]));
            Assert.assertTrue(
                failure.getMessage()
                    .contains("enableWarpedBlocks=true"));
        }
    }

    @Test
    public void sculkAndCatalystMustBothBeExactlyRegistered() {
        BootstrapValidator.validateEfrSculkContent(true, true, true);
        for (boolean[] state : new boolean[][] { { false, true, true }, { true, false, true },
            { true, true, false } }) {
            IllegalStateException failure = Assert.assertThrows(
                IllegalStateException.class,
                () -> BootstrapValidator.validateEfrSculkContent(state[0], state[1], state[2]));
            Assert.assertTrue(
                failure.getMessage()
                    .contains("enableSculk=true"));
        }
    }

    @Test
    public void rejectsDisabledEfrTerrestrialFlora() {
        IllegalStateException failure = Assert.assertThrows(
            IllegalStateException.class,
            () -> BootstrapValidator.validateEfrTerrestrialDetailContent(false, true, true, true, true));

        Assert.assertTrue(
            failure.getMessage()
                .contains("enable beetroots, cornflowers, and lilies"));
    }

    @Test
    public void rejectsDisabledEfrTerrainDetailBlocks() {
        IllegalStateException failure = Assert.assertThrows(
            IllegalStateException.class,
            () -> BootstrapValidator.validateEfrTerrestrialDetailContent(true, true, true, true, false));

        Assert.assertTrue(
            failure.getMessage()
                .contains("enableExtraVanillaSlabs=true"));
    }

    @Test
    public void rejectsDisabledEfrMangroveContent() {
        IllegalStateException failure = Assert
            .assertThrows(IllegalStateException.class, () -> BootstrapValidator.validateEfrMangroveContent(false));

        Assert.assertTrue(
            failure.getMessage()
                .contains("enableMangroveBlocks=true"));
    }

    @Test
    public void rejectsDisabledEfrDripstoneContent() {
        IllegalStateException failure = Assert
            .assertThrows(IllegalStateException.class, () -> BootstrapValidator.validateEfrDripstoneContent(false));

        Assert.assertTrue(
            failure.getMessage()
                .contains("enableDripstone=true"));
    }

    @Test
    public void rejectsDisabledEfrVolcanicBlocks() {
        IllegalStateException failure = Assert.assertThrows(
            IllegalStateException.class,
            () -> BootstrapValidator.validateEfrVolcanicContent(false, true));

        Assert.assertTrue(
            failure.getMessage()
                .contains("enableBasalt=true"));
        Assert.assertTrue(
            failure.getMessage()
                .contains("enableMagmaBlock=true"));
    }

    @Test
    public void rejectsDisabledEfrLateUndergroundBlocks() {
        IllegalStateException failure = Assert.assertThrows(
            IllegalStateException.class,
            () -> BootstrapValidator.validateEfrLateUndergroundContent(true, false, true, true, true));

        Assert.assertTrue(
            failure.getMessage()
                .contains("enable deepslate, deepslate ores, tuff, and stones"));
    }

    @Test
    public void rejectsEfrStoneReplacementMismatch() {
        IllegalStateException failure = Assert.assertThrows(
            IllegalStateException.class,
            () -> BootstrapValidator.validateEfrLateUndergroundContent(true, true, true, true, false));

        Assert.assertTrue(
            failure.getMessage()
                .contains("deepslateReplacesStones=true"));
    }

    private static Map<String, String> minimumVersions() {
        Map<String, String> versions = new LinkedHashMap<>();
        versions.put("angellib", "1.0.0");
        versions.put("etfuturum", "2.6.54-GTNH");
        versions.put("endlessids", "1.7.4");
        versions.put("chunkapi", "0.8.4");
        versions.put("gtnhlib", "0.11.39");
        return versions;
    }
}
