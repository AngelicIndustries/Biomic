package industries.angelic.biomic.compat.biome;

import static org.junit.Assert.*;

import org.junit.Test;

import industries.angelic.biomic.world.terrain.AuthoredVerticalProjections;

public class SaltMarshHeightTest {

    @Test
    public void canonicalSeaBoundariesAreExplicit() {
        assertEquals(
            63,
            SaltMarshCompat.shallowPondSeaBoundary(
                SaltMarshHeight.forProjection(AuthoredVerticalProjections.origenCompressed256())));
        assertEquals(
            191,
            SaltMarshCompat
                .shallowPondSeaBoundary(SaltMarshHeight.forProjection(AuthoredVerticalProjections.shifted512())));
    }
}
