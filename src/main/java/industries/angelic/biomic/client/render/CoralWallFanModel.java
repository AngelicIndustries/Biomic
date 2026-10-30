package industries.angelic.biomic.client.render;

/** Pure vertices in block units, for source-derived geometry tests without initializing GL. */
public final class CoralWallFanModel {

    private CoralWallFanModel() {}

    public static double[] vertex(int facing, int plane, int vertex) {
        if (facing < 0 || facing > 3 || plane < 0 || plane > 1 || vertex < 0 || vertex > 3)
            throw new IllegalArgumentException("Invalid model index");
        double x = (vertex == 0 || vertex == 3) ? 0 : 16, z = vertex < 2 ? 0 : 16;
        double y = 8 + (z - 14) * Math.tan(Math.toRadians(plane == 0 ? -22.5 : 22.5));
        // Minecraft blockstate clockwise Y rotation around block center.
        for (int i = 0; i < facing; i++) {
            double old = x;
            x = 16 - z;
            z = old;
        }
        return new double[] { x / 16, y / 16, z / 16 };
    }
}
