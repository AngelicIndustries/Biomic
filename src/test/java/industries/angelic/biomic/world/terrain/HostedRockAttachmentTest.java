package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;

public class HostedRockAttachmentTest {

    private Set<Pos> points(Pos... points) {
        return new HashSet<>(Arrays.asList(points));
    }

    @Test
    public void rejectsTheTwoReportedAirSurroundedSingletons() {
        // Read-only on-disk inspection of native-caves-v5: the reported player positions
        // were one above these mossy blocks. All six adjacent blocks were air.
        assertFalse(HostedRockAttachment.attached(points(new Pos(249, 131, -4156)), p -> false));
        assertFalse(HostedRockAttachment.attached(points(new Pos(255, 130, -4162)), p -> false));
    }

    @Test
    public void attachedFirstBlobDoesNotExcuseAnOrphanLaterBlob() {
        Set<Pos> rock = points(new Pos(0, 65, 0), new Pos(-1, 64, -1));
        assertFalse(HostedRockAttachment.attached(rock, p -> p.equals(new Pos(0, 64, 0))));
        assertEquals(2, rock.size()); // Validation never edits the caller's transaction.
    }

    @Test
    public void sideContactAndEmbeddedSoilBothAnchorRocks() {
        Set<Pos> rock = points(new Pos(0, 65, 0), new Pos(1, 65, 0), new Pos(1, 66, 0));
        assertTrue(HostedRockAttachment.attached(rock, p -> p.equals(new Pos(-1, 65, 0))));
        assertTrue(HostedRockAttachment.attached(rock, p -> p.equals(new Pos(0, 65, 0))));
    }

    @Test
    public void separateGroundedBlobsAreFineButDiagonalContactIsNot() {
        Set<Pos> rock = points(new Pos(0, 65, 0), new Pos(2, 65, 0));
        assertTrue(HostedRockAttachment.attached(rock, p -> p.y == 64));
        assertFalse(HostedRockAttachment.attached(points(new Pos(0, 65, 0)), p -> p.equals(new Pos(1, 64, 0))));
    }

    @Test
    public void fluidsAndPowderSnowCannotSupportRocks() {
        for (byte material : new byte[] { BiomicTerrainSampler.AIR, BiomicTerrainSampler.WATER,
            BiomicTerrainSampler.LAVA, BiomicTerrainSampler.POWDER_SNOW })
            assertFalse(HostedRockAttachment.solidTerrain(material));
        for (byte material : new byte[] { BiomicTerrainSampler.STONE, BiomicTerrainSampler.DIRT,
            BiomicTerrainSampler.MOSS, BiomicTerrainSampler.DEEPSLATE })
            assertTrue(HostedRockAttachment.solidTerrain(material));
    }
}
