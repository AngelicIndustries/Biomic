package industries.angelic.biomic.content.alexscaves;

import static org.junit.Assert.*;

import org.junit.Test;

import industries.angelic.biomic.content.alexscaves.AbyssalModelGeometry.Element;
import industries.angelic.biomic.content.alexscaves.AbyssalModelGeometry.Model;

public class AbyssalModelGeometryTest {

    @Test
    public void ventsRetainSourceWidthsAndActualMetadataWaterState() {
        Model full = model(BlockAbyssalAquatic.Kind.VENT_FULL, 0, 1, 0),
            medium = model(BlockAbyssalAquatic.Kind.VENT_MEDIUM, 8, 1, 0),
            thin = model(BlockAbyssalAquatic.Kind.VENT_THIN, 8, 1, 0);
        assertFalse(full.waterlogged());
        assertTrue(medium.waterlogged());
        assertTrue(thin.waterlogged());
        assertBounds(full.elements()[0], 0, 0, 0, 16, 16, 16);
        assertBounds(medium.elements()[0], 2, 0, 2, 14, 16, 14);
        assertBounds(thin.elements()[0], 4, 0, 4, 12, 16, 12);
        assertTrue(AbyssalModelGeometry.hasCollision(BlockAbyssalAquatic.Kind.VENT_FULL));
        assertTrue(AbyssalModelGeometry.hasCollision(BlockAbyssalAquatic.Kind.VENT_MEDIUM));
        for (var kind : new BlockAbyssalAquatic.Kind[] { BlockAbyssalAquatic.Kind.VENT_MEDIUM,
            BlockAbyssalAquatic.Kind.VENT_THIN })
            for (int meta = 0; meta < 16; meta++) assertEquals((meta & 8) != 0, model(kind, meta, 1, 0).waterlogged());
    }

    @Test
    public void tubeShapesAndDirectionalBoundsMatchPinnedModels() {
        Model straight = model(BlockAbyssalAquatic.Kind.TUBE_WORM, 0, 1, 0),
            turnNorth = model(BlockAbyssalAquatic.Kind.TUBE_WORM, 4, 1, 0),
            elbowEast = model(BlockAbyssalAquatic.Kind.TUBE_WORM, 9, 1, 0);
        assertEquals(1, straight.elements().length);
        assertEquals(1, turnNorth.elements().length);
        assertEquals(2, elbowEast.elements().length);
        assertBounds(straight.elements()[0], 4, 0, 4, 12, 16, 12);
        assertBounds(turnNorth.elements()[0], 4, 0, 0, 12, 8, 12);
        assertEquals(90, elbowEast.elements()[0].modelY());
        assertArrayEquals(
            new double[] { 4, 0, 4, 16, 16, 12 },
            AbyssalModelGeometry.selectionBounds(BlockAbyssalAquatic.Kind.TUBE_WORM, 9),
            0D);
        assertTrue(AbyssalModelGeometry.hasCollision(BlockAbyssalAquatic.Kind.TUBE_WORM));
    }

    @Test
    public void everyMusselCountUsesPairedTiltedShellsAndAllAttachments() {
        for (int count = 1; count <= 5; count++) for (int facing = 0; facing < 6; facing++) {
            Model source = model(BlockAbyssalAquatic.Kind.MUSSEL, facing, count, 0);
            assertTrue(source.waterlogged());
            assertFalse(source.crossed());
            assertEquals(count * 2, source.elements().length);
            for (int shell = 0; shell < source.elements().length; shell += 2) {
                assertEquals(22.5, source.elements()[shell].angle(), 0D);
                assertEquals(-22.5, source.elements()[shell + 1].angle(), 0D);
            }
        }
        assertEquals(90, model(BlockAbyssalAquatic.Kind.MUSSEL, 1, 5, 1).elements()[0].modelY());
        assertArrayEquals(
            new double[] { 1, 12, 1, 15, 16, 15 },
            AbyssalModelGeometry.selectionBounds(BlockAbyssalAquatic.Kind.MUSSEL, 0),
            0D);
        assertArrayEquals(
            new double[] { 0, 1, 1, 4, 15, 15 },
            AbyssalModelGeometry.selectionBounds(BlockAbyssalAquatic.Kind.MUSSEL, 5),
            0D);
        assertFalse(AbyssalModelGeometry.hasCollision(BlockAbyssalAquatic.Kind.MUSSEL));
    }

    @Test
    public void musselShellUvHandednessMatchesEachPinnedModelElement() {
        Model two = model(BlockAbyssalAquatic.Kind.MUSSEL, 1, 2, 0);
        assertEquals(4, two.elements()[0].surfaces()[0].minimumU(), 0D);
        assertEquals(8, two.elements()[0].surfaces()[0].maximumU(), 0D);
        assertEquals(8, two.elements()[2].surfaces()[0].minimumU(), 0D);
        assertEquals(4, two.elements()[2].surfaces()[0].maximumU(), 0D);
        Model five = model(BlockAbyssalAquatic.Kind.MUSSEL, 1, 5, 0);
        assertEquals(4, five.elements()[8].surfaces()[0].minimumU(), 0D);
        assertEquals(8, five.elements()[8].surfaces()[0].maximumU(), 0D);
    }

    @Test
    public void spongeIsTheOnlySourceCrossAndAnemoneKeepsFourTentaclePlanes() {
        Model base = model(BlockAbyssalAquatic.Kind.SPONGE, 0, 1, 0),
            top = model(BlockAbyssalAquatic.Kind.SPONGE, 1, 1, 0),
            anemone = model(BlockAbyssalAquatic.Kind.DUSK, 0, 1, 0);
        assertTrue(base.crossed());
        assertEquals(0, base.crossTexture());
        assertEquals(1, top.crossTexture());
        assertTrue(base.waterlogged());
        assertFalse(anemone.crossed());
        assertTrue(anemone.waterlogged());
        assertEquals(6, anemone.elements().length);
        assertBounds(anemone.elements()[0], 2, 0, 2, 14, 11, 14);
        assertBounds(anemone.elements()[5], 1.75, 6.5, 1.75, 14.25, 11, 14.25);
        for (int index = 1; index <= 4; index++) {
            assertEquals(2, anemone.elements()[index].surfaces().length);
            for (var surface : anemone.elements()[index].surfaces())
                assertFalse("source tentacles disable directional shade", surface.shaded());
        }
        for (var surface : anemone.elements()[0].surfaces())
            assertTrue("source base keeps directional shade", surface.shaded());
        assertArrayEquals(
            new double[] { 1, 0, 1, 15, 12, 15 },
            AbyssalModelGeometry.selectionBounds(BlockAbyssalAquatic.Kind.MIDNIGHT, 0),
            0D);
        for (BlockAbyssalAquatic.Kind kind : BlockAbyssalAquatic.Kind.values())
            if (kind != BlockAbyssalAquatic.Kind.SPONGE) assertFalse(kind.name(), model(kind, 0, 1, 0).crossed());
    }

    @Test
    public void returnedDescriptorArraysCannotMutatePinnedModels() {
        Model model = model(BlockAbyssalAquatic.Kind.VENT_MEDIUM, 0, 1, 0);
        Element[] elements = model.elements();
        elements[0] = null;
        assertNotNull(model.elements()[0]);
        var surfaces = model.elements()[0].surfaces();
        surfaces[0] = null;
        assertNotNull(model.elements()[0].surfaces()[0]);
        assertThrows(IllegalArgumentException.class, () -> model(BlockAbyssalAquatic.Kind.MUSSEL, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> model(BlockAbyssalAquatic.Kind.MUSSEL, 0, 6, 0));
    }

    @Test
    public void spongeUsesStableSourceXzOffsetWithoutVerticalDrift() {
        double[] first = AbyssalModelGeometry.xzOffset(187, -43, .4D),
            again = AbyssalModelGeometry.xzOffset(187, -43, .4D),
            neighbor = AbyssalModelGeometry.xzOffset(188, -43, .4D);
        assertArrayEquals(first, again, 0D);
        assertFalse(java.util.Arrays.equals(first, neighbor));
        for (double value : first) assertTrue(value >= -.25D && value <= .25D);
        for (double value : AbyssalModelGeometry.xzOffset(187, -43, .2D)) assertTrue(value >= -.2D && value <= .2D);
        assertThrows(IllegalArgumentException.class, () -> AbyssalModelGeometry.xzOffset(0, 0, -.1D));
        double[] base = AbyssalModelGeometry.selectionBounds(BlockAbyssalAquatic.Kind.SPONGE, 1);
        assertArrayEquals(new double[] { 3, 0, 3, 13, 16, 13 }, base, 0D);
    }

    private static Model model(BlockAbyssalAquatic.Kind kind, int metadata, int count, int alternate) {
        return AbyssalModelGeometry.model(kind, metadata, count, alternate);
    }

    private static void assertBounds(Element element, double... expected) {
        assertArrayEquals(
            expected,
            new double[] { element.fromX(), element.fromY(), element.fromZ(), element.toX(), element.toY(),
                element.toZ() },
            0D);
    }
}
