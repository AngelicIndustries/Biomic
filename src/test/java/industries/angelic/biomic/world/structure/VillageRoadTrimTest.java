package industries.angelic.biomic.world.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import org.junit.Test;

public class VillageRoadTrimTest {

    private static final class Village extends StructureStart {

        @SuppressWarnings("unchecked")
        StructureComponent add(StructureComponent piece) {
            components.add(piece);
            return piece;
        }
    }

    private static StructureComponent road(int x0, int z0, int x1, int z1) {
        return new StructureVillagePieces.Path(
            null,
            0,
            new Random(0),
            new StructureBoundingBox(x0, 64, z0, x1, 66, z1),
            0);
    }

    private static StructureComponent house(int x0, int z0, int x1, int z1) {
        return new StructureVillagePieces.House4Garden(
            null,
            0,
            new Random(0),
            new StructureBoundingBox(x0, 64, z0, x1, 70, z1),
            0);
    }

    @Test
    public void aRoadOnlyLeavingTheWellGoes() {
        Village village = new Village();
        village.add(house(0, 0, 8, 8)); // the well
        StructureComponent stub = village.add(road(-15, 3, -1, 5));
        BiomicVillagePlacement.trimRoads(village);
        assertFalse(
            village.getComponents()
                .contains(stub));
    }

    @Test
    public void aRoadEndsAtItsLastHouseAndKeepsTheWellEnd() {
        Village village = new Village();
        village.add(house(0, 0, 8, 8));
        StructureComponent road = village.add(road(9, 3, 40, 5));
        village.add(house(12, 6, 18, 12));
        BiomicVillagePlacement.trimRoads(village);
        StructureBoundingBox box = road.getBoundingBox();
        assertTrue(
            village.getComponents()
                .contains(road));
        assertEquals(9, box.minX);
        assertEquals(18, box.maxX);
    }

    @Test
    public void aBranchLeftServingNothingGoesWithItsRoadTrimmedAway() {
        Village village = new Village();
        village.add(house(0, 0, 8, 8));
        StructureComponent main = village.add(road(9, 3, 40, 5));
        village.add(house(12, 6, 18, 12));
        StructureComponent branch = village.add(road(30, 6, 32, 30));
        BiomicVillagePlacement.trimRoads(village);
        assertFalse(
            village.getComponents()
                .contains(branch));
        assertEquals(18, main.getBoundingBox().maxX);
    }

    @Test
    public void aRoadBetweenTwoRoadsStays() {
        Village village = new Village();
        village.add(house(0, 0, 8, 8));
        StructureComponent first = village.add(road(9, 3, 20, 5));
        village.add(house(12, 6, 18, 12));
        StructureComponent link = village.add(road(21, 3, 30, 5));
        StructureComponent last = village.add(road(31, 3, 40, 5));
        village.add(house(33, 6, 38, 12));
        BiomicVillagePlacement.trimRoads(village);
        assertTrue(
            village.getComponents()
                .contains(link));
        assertEquals(30, link.getBoundingBox().maxX);
        assertEquals(31, last.getBoundingBox().minX);
        assertEquals(38, last.getBoundingBox().maxX);
        assertEquals(20, first.getBoundingBox().maxX);
    }
}
