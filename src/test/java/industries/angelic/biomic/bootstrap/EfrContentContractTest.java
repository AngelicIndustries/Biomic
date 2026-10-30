package industries.angelic.biomic.bootstrap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;

import net.minecraftforge.common.config.Configuration;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import cpw.mods.fml.relauncher.FMLInjectionData;

public class EfrContentContractTest {

    private static Field minecraftHomeField;
    private static Object previousMinecraftHome;

    @BeforeClass
    public static void provideFmlConfigurationHome() throws ReflectiveOperationException {
        minecraftHomeField = FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHomeField.setAccessible(true);
        previousMinecraftHome = minecraftHomeField.get(null);
        minecraftHomeField.set(null, new File(System.getProperty("java.io.tmpdir")));
    }

    @AfterClass
    public static void restoreFmlConfigurationHome() throws IllegalAccessException {
        minecraftHomeField.set(null, previousMinecraftHome);
    }

    @Test
    public void enforcesAndPersistsRequiredBooleanValues() throws IOException {
        File file = Files.createTempFile("biomic-efr-contract", ".cfg")
            .toFile();
        try {
            Configuration configuration = new Configuration(file);
            configuration.get("content", "requiredEnabled", false);
            configuration.get("compatibility", "requiredDisabled", true);
            configuration.save();

            int changes = EfrContentContract.enforceAll(
                configuration,
                EfrContentContract.required("requiredEnabled", true),
                EfrContentContract.required("requiredDisabled", false));

            Assert.assertEquals(2, changes);
            Assert.assertTrue(
                configuration.getCategory("content")
                    .get("requiredEnabled")
                    .getBoolean());
            Assert.assertFalse(
                configuration.getCategory("compatibility")
                    .get("requiredDisabled")
                    .getBoolean());

            Configuration reloaded = new Configuration(file);
            reloaded.load();
            Assert.assertTrue(
                reloaded.getCategory("content")
                    .get("requiredEnabled")
                    .getBoolean());
            Assert.assertFalse(
                reloaded.getCategory("compatibility")
                    .get("requiredDisabled")
                    .getBoolean());
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    public void reportsAnEfrContractDriftInsteadOfSilentlyCreatingAReplacementProperty() throws IOException {
        File file = Files.createTempFile("biomic-efr-contract-missing", ".cfg")
            .toFile();
        try {
            Configuration configuration = new Configuration(file);
            configuration.get("content", "existingOption", true);

            IllegalStateException failure = Assert.assertThrows(
                IllegalStateException.class,
                () -> EfrContentContract.enforceAll(configuration, EfrContentContract.required("renamedOption", true)));

            Assert.assertTrue(
                failure.getMessage()
                    .contains("renamedOption"));
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    public void experimentContractPersistsBothSourceNetherPlantFamilies() throws IOException {
        File file = Files.createTempFile("biomic-efr-shroomlight", ".cfg")
            .toFile();
        try {
            Configuration configuration = new Configuration(file);
            for (String key : new String[] { "enableDripstone", "enableMangroveBlocks", "enableMossAzalea",
                "enableWarpedBlocks", "enableCrimsonBlocks", "enableSculk" }) {
                configuration.get("experiments", key, false);
            }
            Assert.assertEquals(6, EfrContentContract.enforceExperiments(configuration));
            Assert.assertEquals(0, EfrContentContract.enforceExperiments(configuration));
            Configuration reloaded = new Configuration(file);
            reloaded.load();
            Assert.assertTrue(
                reloaded.getCategory("experiments")
                    .get("enableWarpedBlocks")
                    .getBoolean());
            Assert.assertTrue(
                reloaded.getCategory("experiments")
                    .get("enableCrimsonBlocks")
                    .getBoolean());
            Assert.assertTrue(
                reloaded.getCategory("experiments")
                    .get("enableSculk")
                    .getBoolean());
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }
}
