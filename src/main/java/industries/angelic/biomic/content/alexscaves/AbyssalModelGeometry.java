package industries.angelic.biomic.content.alexscaves;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure, source-space descriptions of the pinned Alex's Caves Abyssal models. Coordinates and UVs are in the
 * modern model JSON's 0..16 domain. Keeping this independent of client/Forge classes makes shape drift testable.
 */
public final class AbyssalModelGeometry {

    public enum Axis {
        NONE,
        X,
        Y,
        Z
    }

    public enum Face {
        DOWN,
        UP,
        NORTH,
        SOUTH,
        WEST,
        EAST
    }

    public record Surface(Face face, int texture, double minimumU, double minimumV, double maximumU, double maximumV,
        int uvRotation, boolean shaded, Face cullFace) {

        public Surface(Face face, int texture, double minimumU, double minimumV, double maximumU, double maximumV,
            int uvRotation, boolean shaded) {
            this(face, texture, minimumU, minimumV, maximumU, maximumV, uvRotation, shaded, null);
        }
    }

    public record Element(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, Axis axis,
        double angle, double originX, double originY, double originZ, int modelX, int modelY, Surface[] surfaces) {

        public Element {
            surfaces = surfaces.clone();
        }

        @Override
        public Surface[] surfaces() {
            return surfaces.clone();
        }
    }

    public record Model(Element[] elements, boolean crossed, int crossTexture, boolean waterlogged) {

        public Model {
            elements = elements.clone();
        }

        @Override
        public Element[] elements() {
            return elements.clone();
        }
    }

    private static final double[][][] MUSSELS = { { { 6, 0, 8, 10, 6, 8, 0 } },
        { { 2, 0, 4, 6, 6, 4, 1 }, { 10, 0, 12, 14, 6, 12, 0 } },
        { { 7, 0, 4, 11, 6, 4, 0 }, { 1, 0, 9, 5, 6, 9, 1 }, { 10, 0, 12, 14, 6, 12, 0 } },
        { { 1, 0, 3, 5, 6, 3, 0 }, { 3, 0, 11, 7, 6, 11, 1 }, { 11, 0, 14, 15, 6, 14, 0 }, { 9, 0, 5, 13, 6, 5, 1 } },
        { { 2, 0, 3, 6, 6, 3, 0 }, { 1, 0, 11, 5, 6, 11, 0 }, { 9, 0, 14, 13, 6, 14, 0 }, { 11, 0, 4, 15, 6, 4, 0 },
            { 6, 0, 8, 10, 6, 8, 1 } } };

    private AbyssalModelGeometry() {}

    public static Model model(BlockAbyssalAquatic.Kind kind, int metadata, int musselCount, int alternate) {
        return switch (kind) {
            case VENT_FULL -> new Model(new Element[] { cube(0, 0, 0, 16, 16, 16, ventFullFaces()) }, false, 0, false);
            case VENT_MEDIUM -> new Model(
                new Element[] { cube(2, 0, 2, 14, 16, 14, ventMediumFaces()) },
                false,
                0,
                (metadata & 8) != 0);
            case VENT_THIN -> new Model(
                new Element[] { cube(4, 0, 4, 12, 16, 12, ventThinFaces()) },
                false,
                0,
                (metadata & 8) != 0);
            case TUBE_WORM -> tube(metadata);
            case MUSSEL -> mussels(musselCount, metadata, alternate);
            case SPONGE -> new Model(new Element[0], true, (metadata & 1) == 0 ? 0 : 1, true);
            case DUSK, TWILIGHT, MIDNIGHT -> anemone();
        };
    }

    public static double[] selectionBounds(BlockAbyssalAquatic.Kind kind, int metadata) {
        return switch (kind) {
            case VENT_FULL -> new double[] { 0, 0, 0, 16, 16, 16 };
            case VENT_MEDIUM -> new double[] { 2, 0, 2, 14, 16, 14 };
            case VENT_THIN -> new double[] { 4, 0, 4, 12, 16, 12 };
            case TUBE_WORM -> tubeBounds(metadata);
            case MUSSEL -> musselBounds(metadata);
            case SPONGE -> (metadata & 1) == 0 ? new double[] { 6, 0, 6, 10, 16, 10 }
                : new double[] { 3, 0, 3, 13, 16, 13 };
            case DUSK, TWILIGHT, MIDNIGHT -> new double[] { 1, 0, 1, 15, 12, 15 };
        };
    }

    public static boolean hasCollision(BlockAbyssalAquatic.Kind kind) {
        return kind == BlockAbyssalAquatic.Kind.VENT_FULL || kind == BlockAbyssalAquatic.Kind.VENT_MEDIUM
            || kind == BlockAbyssalAquatic.Kind.VENT_THIN
            || kind == BlockAbyssalAquatic.Kind.TUBE_WORM;
    }

    /** Modern {@code OffsetType.XZ} position offset used by the source sponge and ocean flora. */
    public static double[] xzOffset(int x, int z, double maximum) {
        if (maximum < 0 || maximum > .5D) throw new IllegalArgumentException("Invalid maximum offset: " + maximum);
        long seed = (long) (x * 3129871) ^ (long) z * 116129781L;
        seed = (seed * seed * 42317861L + seed * 11L) >> 16;
        double xOffset = ((double) ((seed >> 16) & 15) / 15D - .5D) * .5D,
            zOffset = ((double) ((seed >> 24) & 15) / 15D - .5D) * .5D;
        return new double[] { Math.max(-maximum, Math.min(maximum, xOffset)),
            Math.max(-maximum, Math.min(maximum, zOffset)) };
    }

    private static Model tube(int metadata) {
        int type = Math.min(2, Math.max(0, metadata / 4)), turn = (metadata & 3) * 90;
        if (type == 0)
            return new Model(new Element[] { cube(4, 0, 4, 12, 16, 12, tubeStraightFaces()) }, false, 0, true);
        Element bend = element(
            4,
            0,
            0,
            12,
            8,
            12,
            Axis.NONE,
            0,
            8,
            0,
            8,
            0,
            turn,
            type == 1 ? tubeTurnFaces() : tubeElbowBendFaces());
        if (type == 1) return new Model(new Element[] { bend }, false, 0, true);
        Element upright = element(4, 8, 4, 12, 16, 12, Axis.NONE, 0, 0, 0, 0, 0, turn, tubeElbowUprightFaces());
        return new Model(new Element[] { bend, upright }, false, 0, true);
    }

    private static Model mussels(int count, int facing, int alternate) {
        if (count < 1 || count > 5) throw new IllegalArgumentException("Mussel count must be 1..5: " + count);
        int modelX = switch (facing & 7) {
            case 0 -> 180;
            case 2, 4, 5 -> 90;
            case 3 -> 270;
            default -> 0;
        };
        int modelY = switch (facing & 7) {
            case 4 -> 270;
            case 5 -> 90;
            default -> 0;
        };
        if (((facing & 7) == 0 || (facing & 7) == 1) && (alternate & 1) != 0) modelY = 90;
        List<Element> out = new ArrayList<>();
        for (double[] b : MUSSELS[count - 1]) {
            // Only north/south have non-zero area in the source plane; the other JSON faces are degenerate.
            boolean mirrored = b[6] != 0;
            Surface[] positive = mirrored
                ? new Surface[] { surface(Face.NORTH, 0, 4, 0, 8, 6), surface(Face.SOUTH, 0, 4, 0, 0, 6) }
                : new Surface[] { surface(Face.NORTH, 0, 8, 0, 4, 6), surface(Face.SOUTH, 0, 0, 0, 4, 6) };
            Surface[] negative = mirrored
                ? new Surface[] { surface(Face.NORTH, 0, 0, 0, 4, 6), surface(Face.SOUTH, 0, 8, 0, 4, 6) }
                : new Surface[] { surface(Face.NORTH, 0, 4, 0, 0, 6), surface(Face.SOUTH, 0, 4, 0, 8, 6) };
            out.add(
                element(
                    b[0],
                    b[1],
                    b[2],
                    b[3],
                    b[4],
                    b[5],
                    Axis.X,
                    22.5,
                    b[0] + .5,
                    0,
                    b[2],
                    modelX,
                    modelY,
                    positive));
            out.add(
                element(
                    b[0],
                    b[1],
                    b[2],
                    b[3],
                    b[4],
                    b[5],
                    Axis.X,
                    -22.5,
                    b[0] + .5,
                    0,
                    b[2],
                    modelX,
                    modelY,
                    negative));
        }
        return new Model(out.toArray(new Element[0]), false, 0, (facing & 8) == 0);
    }

    private static Model anemone() {
        List<Element> out = new ArrayList<>();
        out.add(cube(2, 0, 2, 14, 11, 14, anemoneBaseFaces()));
        out.add(element(0, 11, 12.25, 16, 16, 12.25, Axis.NONE, 0, 0, 0, 0, 0, 0, planeZ(3)));
        out.add(element(0, 11, 3.75, 16, 16, 3.75, Axis.NONE, 0, 0, 0, 0, 0, 0, planeZ(3)));
        out.add(element(12.25, 11, 0, 12.25, 16, 16, Axis.NONE, 0, 0, 0, 0, 0, 0, planeX(3)));
        out.add(element(3.75, 11, 0, 3.75, 16, 16, Axis.NONE, 0, 0, 0, 0, 0, 0, planeX(3)));
        out.add(cube(1.75, 6.5, 1.75, 14.25, 11, 14.25, anemoneTopFaces()));
        return new Model(out.toArray(new Element[0]), false, 0, true);
    }

    private static Element cube(double fx, double fy, double fz, double tx, double ty, double tz, Surface[] surfaces) {
        return element(fx, fy, fz, tx, ty, tz, Axis.NONE, 0, 0, 0, 0, 0, 0, surfaces);
    }

    private static Element element(double fx, double fy, double fz, double tx, double ty, double tz, Axis axis,
        double angle, double ox, double oy, double oz, int modelX, int modelY, Surface[] surfaces) {
        return new Element(fx, fy, fz, tx, ty, tz, axis, angle, ox, oy, oz, modelX, modelY, surfaces);
    }

    private static Surface surface(Face face, int texture, double u1, double v1, double u2, double v2) {
        return new Surface(face, texture, u1, v1, u2, v2, 0, true);
    }

    private static Surface surface(Face face, int texture, double u1, double v1, double u2, double v2, int rotation) {
        return new Surface(face, texture, u1, v1, u2, v2, rotation, true);
    }

    private static Surface unshaded(Face face, int texture, double u1, double v1, double u2, double v2) {
        return new Surface(face, texture, u1, v1, u2, v2, 0, false);
    }

    private static Surface[] ventFullFaces() {
        return new Surface[] { surface(Face.DOWN, 1, 0, 0, 16, 16), surface(Face.UP, 1, 0, 0, 16, 16),
            surface(Face.NORTH, 0, 0, 0, 16, 16), surface(Face.SOUTH, 0, 0, 0, 16, 16),
            surface(Face.WEST, 0, 0, 0, 16, 16), surface(Face.EAST, 0, 0, 0, 16, 16) };
    }

    private static Surface[] ventMediumFaces() {
        return new Surface[] { surface(Face.DOWN, 0, 0, 0, 12, 12), surface(Face.UP, 1, 0, 0, 12, 12),
            surface(Face.NORTH, 0, 0, 0, 12, 16), surface(Face.SOUTH, 0, 0, 0, 12, 16),
            surface(Face.WEST, 0, 0, 0, 12, 16), surface(Face.EAST, 0, 0, 0, 12, 16) };
    }

    private static Surface[] ventThinFaces() {
        return new Surface[] { surface(Face.DOWN, 0, 8, 0, 16, 8), surface(Face.UP, 0, 8, 8, 16, 16),
            surface(Face.NORTH, 0, 0, 0, 8, 16), surface(Face.SOUTH, 0, 0, 0, 8, 16),
            surface(Face.WEST, 0, 0, 0, 8, 16), surface(Face.EAST, 0, 0, 0, 8, 16) };
    }

    private static Surface[] tubeStraightFaces() {
        return new Surface[] { surface(Face.DOWN, 0, 16, 0, 8, 8), surface(Face.UP, 0, 16, 8, 8, 0),
            surface(Face.NORTH, 0, 0, 0, 8, 16), surface(Face.SOUTH, 0, 0, 0, 8, 16),
            surface(Face.WEST, 0, 0, 0, 8, 16), surface(Face.EAST, 0, 0, 0, 8, 16) };
    }

    private static Surface[] tubeTurnFaces() {
        return new Surface[] { surface(Face.DOWN, 2, 0, 0, 8, 12, 180), surface(Face.UP, 2, 8, 12, 0, 0, 180),
            surface(Face.NORTH, 0, 8, 0, 16, 8), surface(Face.SOUTH, 1, 0, 8, 8, 16),
            surface(Face.WEST, 1, 0, 0, 12, 8), surface(Face.EAST, 1, 12, 0, 0, 8) };
    }

    private static Surface[] tubeElbowBendFaces() {
        return new Surface[] { surface(Face.DOWN, 2, 8, 12, 0, 0), surface(Face.UP, 2, 0, 0, 8, 12),
            surface(Face.NORTH, 0, 8, 0, 16, 8, 180), surface(Face.SOUTH, 1, 0, 8, 8, 16, 180),
            surface(Face.WEST, 1, 12, 0, 0, 8, 180), surface(Face.EAST, 1, 0, 0, 12, 8, 180) };
    }

    private static Surface[] tubeElbowUprightFaces() {
        return new Surface[] { surface(Face.DOWN, 0, 8, 0, 16, 8), surface(Face.UP, 0, 8, 0, 16, 8),
            surface(Face.NORTH, 0, 0, 0, 8, 8), surface(Face.SOUTH, 0, 0, 0, 8, 8), surface(Face.WEST, 0, 0, 0, 8, 8),
            surface(Face.EAST, 0, 0, 0, 8, 8) };
    }

    private static Surface[] anemoneBaseFaces() {
        return new Surface[] { surface(Face.DOWN, 1, 12, 0, 0, 12), surface(Face.UP, 0, 16, 4, 12, 0),
            surface(Face.NORTH, 0, 0, 0, 12, 11), surface(Face.SOUTH, 0, 0, 0, 12, 11),
            surface(Face.WEST, 0, 0, 0, 12, 11), surface(Face.EAST, 0, 0, 0, 12, 11) };
    }

    private static Surface[] anemoneTopFaces() {
        return new Surface[] { surface(Face.DOWN, 2, 16, 0, 12, 4), surface(Face.UP, 2, 12, 12, 0, 0),
            surface(Face.NORTH, 2, 0, 12, 12, 16), surface(Face.SOUTH, 2, 0, 12, 12, 16),
            surface(Face.WEST, 2, 0, 12, 12, 16), surface(Face.EAST, 2, 0, 12, 12, 16) };
    }

    // The source anemone model explicitly sets shade:false on every tentacle plane. Its solid base and top
    // retain ordinary directional diffuse shading.
    private static Surface[] planeZ(int texture) {
        return new Surface[] { unshaded(Face.NORTH, texture, 16, 5, 0, 10),
            unshaded(Face.SOUTH, texture, 0, 5, 16, 10) };
    }

    private static Surface[] planeX(int texture) {
        return new Surface[] { unshaded(Face.EAST, texture, 0, 5, 16, 10), unshaded(Face.WEST, texture, 16, 5, 0, 10) };
    }

    private static double[] tubeBounds(int metadata) {
        int type = Math.min(2, Math.max(0, metadata / 4));
        if (type == 0) return new double[] { 4, 0, 4, 12, 16, 12 };
        return switch (metadata & 3) {
            case 0 -> new double[] { 4, 0, 0, 12, type == 1 ? 8 : 16, 12 };
            case 1 -> new double[] { 4, 0, 4, 16, type == 1 ? 8 : 16, 12 };
            case 2 -> new double[] { 4, 0, 4, 12, type == 1 ? 8 : 16, 16 };
            default -> new double[] { 0, 0, 4, 12, type == 1 ? 8 : 16, 12 };
        };
    }

    private static double[] musselBounds(int metadata) {
        return switch (metadata & 7) {
            case 0 -> new double[] { 1, 12, 1, 15, 16, 15 };
            case 2 -> new double[] { 1, 1, 12, 15, 15, 16 };
            case 3 -> new double[] { 1, 1, 0, 15, 15, 4 };
            case 4 -> new double[] { 12, 1, 1, 16, 15, 15 };
            case 5 -> new double[] { 0, 1, 1, 4, 15, 15 };
            default -> new double[] { 1, 0, 1, 15, 4, 15 };
        };
    }
}
