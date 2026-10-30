package industries.angelic.biomic.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;

/**
 * Small, allocation-light bridge for modern JSON block-model quads. Coordinates and UVs use the JSON model's
 * 0..16 space. Vertex winding, per-face UV rotation, element rotation and blockstate Y rotation follow FaceBakery.
 */
public final class LegacyBlockModelRenderer {

    private static final double DEGREES_TO_RADIANS = Math.PI / 180.0D;

    private LegacyBlockModelRenderer() {}

    public static void face(Tessellator tessellator, IIcon icon, double worldX, double worldY, double worldZ,
        double fromX, double fromY, double fromZ, double toX, double toY, double toZ, Face face, double minimumU,
        double minimumV, double maximumU, double maximumV, int uvRotation) {
        face(
            tessellator,
            icon,
            worldX,
            worldY,
            worldZ,
            fromX,
            fromY,
            fromZ,
            toX,
            toY,
            toZ,
            face,
            minimumU,
            minimumV,
            maximumU,
            maximumV,
            uvRotation,
            Axis.NONE,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            false,
            0);
    }

    public static void face(Tessellator tessellator, IIcon icon, double worldX, double worldY, double worldZ,
        double fromX, double fromY, double fromZ, double toX, double toY, double toZ, Face face, double minimumU,
        double minimumV, double maximumU, double maximumV, int uvRotation, Axis rotationAxis, double rotationAngle,
        double rotationOriginX, double rotationOriginY, double rotationOriginZ, boolean rescale, int modelYRotation) {
        double[][] vertices = vertices(
            fromX,
            fromY,
            fromZ,
            toX,
            toY,
            toZ,
            face,
            rotationAxis,
            rotationAngle,
            rotationOriginX,
            rotationOriginY,
            rotationOriginZ,
            rescale,
            modelYRotation);
        double[][] uvs = uvs(minimumU, minimumV, maximumU, maximumV, uvRotation);
        for (int index = 0; index < 4; index++) {
            tessellator.addVertexWithUV(
                worldX + vertices[index][0] / 16.0D,
                worldY + vertices[index][1] / 16.0D,
                worldZ + vertices[index][2] / 16.0D,
                icon.getInterpolatedU(uvs[index][0]),
                icon.getInterpolatedV(uvs[index][1]));
        }
    }

    public static double[][] vertices(double fromX, double fromY, double fromZ, double toX, double toY, double toZ,
        Face face, Axis rotationAxis, double rotationAngle, double rotationOriginX, double rotationOriginY,
        double rotationOriginZ, boolean rescale, int modelYRotation) {
        return vertices(
            fromX,
            fromY,
            fromZ,
            toX,
            toY,
            toZ,
            face,
            rotationAxis,
            rotationAngle,
            rotationOriginX,
            rotationOriginY,
            rotationOriginZ,
            rescale,
            0,
            modelYRotation);
    }

    public static double[][] vertices(double fromX, double fromY, double fromZ, double toX, double toY, double toZ,
        Face face, Axis rotationAxis, double rotationAngle, double rotationOriginX, double rotationOriginY,
        double rotationOriginZ, boolean rescale, int modelXRotation, int modelYRotation) {
        double[][] points;
        switch (face) {
            case DOWN:
                points = new double[][] { { fromX, fromY, toZ }, { fromX, fromY, fromZ }, { toX, fromY, fromZ },
                    { toX, fromY, toZ } };
                break;
            case UP:
                points = new double[][] { { fromX, toY, fromZ }, { fromX, toY, toZ }, { toX, toY, toZ },
                    { toX, toY, fromZ } };
                break;
            case NORTH:
                points = new double[][] { { toX, toY, fromZ }, { toX, fromY, fromZ }, { fromX, fromY, fromZ },
                    { fromX, toY, fromZ } };
                break;
            case SOUTH:
                points = new double[][] { { fromX, toY, toZ }, { fromX, fromY, toZ }, { toX, fromY, toZ },
                    { toX, toY, toZ } };
                break;
            case WEST:
                points = new double[][] { { fromX, toY, fromZ }, { fromX, fromY, fromZ }, { fromX, fromY, toZ },
                    { fromX, toY, toZ } };
                break;
            case EAST:
                points = new double[][] { { toX, toY, toZ }, { toX, fromY, toZ }, { toX, fromY, fromZ },
                    { toX, toY, fromZ } };
                break;
            default:
                throw new AssertionError(face);
        }
        for (double[] point : points) {
            rotateElement(
                point,
                rotationAxis,
                rotationAngle,
                rotationOriginX,
                rotationOriginY,
                rotationOriginZ,
                rescale);
            // Block-model variant rotations use the opposite sign and rotate about the block center.
            rotate(point, Axis.X, -modelXRotation, 8.0D, 8.0D, 8.0D);
            rotate(point, Axis.Y, -modelYRotation, 8.0D, 8.0D, 8.0D);
        }
        return points;
    }

    public static Face facing(double[][] vertices) {
        double[] a = new double[3], b = new double[3];
        for (int i = 0; i < 3; i++) {
            a[i] = vertices[1][i] - vertices[0][i];
            b[i] = vertices[2][i] - vertices[0][i];
        }
        double x = a[1] * b[2] - a[2] * b[1], y = a[2] * b[0] - a[0] * b[2], z = a[0] * b[1] - a[1] * b[0];
        return Math.abs(y) >= Math.abs(x) && Math.abs(y) >= Math.abs(z) ? (y > 0 ? Face.UP : Face.DOWN)
            : Math.abs(x) >= Math.abs(z) ? (x > 0 ? Face.EAST : Face.WEST) : (z > 0 ? Face.SOUTH : Face.NORTH);
    }

    /** World-aligned UVs for the audited full-face multipart planes (not arbitrary cropped models). */
    static double[][] lockedUvs(double[][] vertices, Face face, double[] bounds, int rotation) {
        double[][] result = new double[4][2];
        for (int i = 0; i < 4; i++) {
            double x = vertices[i][0], y = vertices[i][1], z = vertices[i][2];
            double u = switch (face) {
                case NORTH -> 16 - x;
                case EAST -> 16 - z;
                case WEST -> z;
                default -> x;
            };
            double v = switch (face) {
                case UP -> z;
                case DOWN -> 16 - z;
                default -> 16 - y;
            };
            for (int turn = 0; turn < Math.floorMod(rotation / 90, 4); turn++) {
                double old = u;
                u = v;
                v = 16 - old;
            }
            result[i][0] = bounds[0] + u / 16 * (bounds[2] - bounds[0]);
            result[i][1] = bounds[1] + v / 16 * (bounds[3] - bounds[1]);
        }
        return result;
    }

    public static double[][] uvs(double minimumU, double minimumV, double maximumU, double maximumV, int rotation) {
        if (Math.floorMod(rotation, 90) != 0) {
            throw new IllegalArgumentException("UV rotation must be a multiple of 90 degrees: " + rotation);
        }
        double[][] result = new double[4][2];
        int quarterTurns = Math.floorMod(rotation / 90, 4);
        for (int vertex = 0; vertex < 4; vertex++) {
            int shifted = Math.floorMod(vertex + quarterTurns, 4);
            result[vertex][0] = shifted == 0 || shifted == 1 ? minimumU : maximumU;
            result[vertex][1] = shifted == 0 || shifted == 3 ? minimumV : maximumV;
        }
        return result;
    }

    private static void rotateElement(double[] point, Axis axis, double degrees, double originX, double originY,
        double originZ, boolean rescale) {
        if (axis == Axis.NONE || degrees == 0.0D) return;
        rotate(point, axis, degrees, originX, originY, originZ);
        if (!rescale) return;

        double factor = 1.0D / Math.cos(Math.abs(degrees) * DEGREES_TO_RADIANS);
        if (axis != Axis.X) point[0] = originX + (point[0] - originX) * factor;
        if (axis != Axis.Y) point[1] = originY + (point[1] - originY) * factor;
        if (axis != Axis.Z) point[2] = originZ + (point[2] - originZ) * factor;
    }

    private static void rotate(double[] point, Axis axis, double degrees, double originX, double originY,
        double originZ) {
        if (axis == Axis.NONE || degrees == 0.0D) return;
        double radians = degrees * DEGREES_TO_RADIANS;
        double sine = Math.sin(radians);
        double cosine = Math.cos(radians);
        double x = point[0] - originX;
        double y = point[1] - originY;
        double z = point[2] - originZ;
        if (axis == Axis.X) {
            point[1] = originY + y * cosine - z * sine;
            point[2] = originZ + y * sine + z * cosine;
        } else if (axis == Axis.Y) {
            point[0] = originX + x * cosine + z * sine;
            point[2] = originZ - x * sine + z * cosine;
        } else {
            point[0] = originX + x * cosine - y * sine;
            point[1] = originY + x * sine + y * cosine;
        }
    }

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
}
