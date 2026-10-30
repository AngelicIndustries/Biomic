package industries.angelic.biomic.content.alexscaves;

import static org.junit.Assert.*;

import org.junit.Test;

import industries.angelic.biomic.content.alexscaves.AbyssalVentBehavior.Fluid;

public class AbyssalVentBehaviorTest {

    @Test
    public void sourceSmokeInheritsLowerVentBeforeFluidAndPreservesWetBit() {
        assertEquals(14, AbyssalVentBehavior.refreshedMetadata(8, 2, Fluid.WATER, false, false));
        assertEquals(13, AbyssalVentBehavior.refreshedMetadata(8, null, Fluid.WATER, false, false));
        assertEquals(6, AbyssalVentBehavior.refreshedMetadata(0, null, Fluid.LAVA, false, false));
        assertEquals(4, AbyssalVentBehavior.refreshedMetadata(0, null, Fluid.NONE, false, false));
        assertEquals(15, AbyssalVentBehavior.refreshedMetadata(15, null, Fluid.LAVA, false, true));
        assertFalse(AbyssalVentBehavior.emits(15)); // Legacy acid state is neither changed nor rendered.
    }

    @Test
    public void everyVentTierOccludesLowerPlumeEvenIfNativeWaterMaterialDoesNotBlockMotion() {
        for (var kind : new BlockAbyssalAquatic.Kind[] { BlockAbyssalAquatic.Kind.VENT_FULL,
            BlockAbyssalAquatic.Kind.VENT_MEDIUM, BlockAbyssalAquatic.Kind.VENT_THIN }) {
            assertTrue(AbyssalVentBehavior.vent(kind));
            assertEquals(10, AbyssalVentBehavior.refreshedMetadata(14, 2, Fluid.NONE, true, false));
            assertFalse(AbyssalVentBehavior.emits(10));
        }
        assertEquals(2, AbyssalVentBehavior.refreshedMetadata(6, 2, Fluid.NONE, false, true));
        assertTrue(AbyssalVentBehavior.emits(6));
        assertTrue(AbyssalVentBehavior.emits(13));
        assertFalse(AbyssalVentBehavior.emits(4));
        assertFalse(AbyssalVentBehavior.emits(2));
    }

    @Test
    public void exactSourceGrowthFrictionFadeAndFortyToFiftyNineLifetime() {
        assertEquals(40, AbyssalVentBehavior.lifetime(0));
        assertEquals(59, AbyssalVentBehavior.lifetime(.999999));
        assertEquals(0, AbyssalVentBehavior.growth(0, 0, 40), 0);
        assertEquals(.5F, AbyssalVentBehavior.growth(5, 0, 40), 0);
        assertEquals(1, AbyssalVentBehavior.growth(30, .5F, 40), 0);
        assertEquals(1, AbyssalVentBehavior.alpha(20, 41), 0);
        assertEquals(1 - 1 / 41F, AbyssalVentBehavior.alpha(21, 41), 0);
        assertEquals(.5F, AbyssalVentBehavior.alpha(40, 40), 0); // Upstream does not fade fully before removal.
    }
}
