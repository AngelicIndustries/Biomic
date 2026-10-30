package industries.angelic.biomic.compat.flora;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class NaturaMixinVersionsTest {

    private static List<String> selected() {
        return new ArrayList<>(
            List.of(
                "minecraft.Other",
                "natura.MixinNaturaBerryOwnership",
                "natura.MixinNaturaSaplingGrowthHeight",
                "natura.MixinNaturaBushGrowthHeight",
                "growthcraft.Other"));
    }

    @Test
    public void exactAuditedVersionsForEitherSingleAliasRetainLiteralSelection() {
        for (String alias : List.of("Natura", "natura")) for (String version : List.of("2.8.19", "2.8.24")) {
            var names = selected();
            NaturaMixinVersions.filter(names, Set.of(alias), version);
            assertEquals(selected(), names);
        }
    }

    @Test
    public void absentUnknownMissingAndAmbiguousProvidersInstallNoNaturaHooks() {
        for (var ids : List.of(Set.<String>of(), Set.of("Natura"), Set.of("natura"), Set.of("Natura", "natura")))
            for (String version : new String[] { null, "", "2.8.25", "2.8.24-dev", "2.8.19.1" }) {
                var names = selected();
                NaturaMixinVersions.filter(names, ids, version);
                assertEquals(List.of("minecraft.Other", "growthcraft.Other"), names);
            }
        for (var ids : List.of(Set.<String>of(), Set.of("Natura", "natura"))) {
            var names = selected();
            NaturaMixinVersions.filter(names, ids, "2.8.24");
            assertEquals(List.of("minecraft.Other", "growthcraft.Other"), names);
        }
    }
}
