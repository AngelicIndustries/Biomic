package industries.angelic.biomic.world.structure;

/** Checks the planned pieces, not merely the start: short cave crossings are fine, open caverns are not. */
final class MineshaftRockEnvelope {

    private MineshaftRockEnvelope() {}

    interface Rock {

        boolean at(int x, int y, int z);
    }

    /** {@code floorY}/{@code ceilingY}: the terrain envelope (0/256 at legacy height). */
    static boolean contains(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int floorY, int ceilingY,
        Rock rock) {
        if (minY < floorY + 2 || maxY > ceilingY - 4) return false;
        int area = (maxX - minX + 1) * (maxZ - minZ + 1);
        int allowedOpen = area / 5; // At least 80% buried roof and supported floor in EACH piece.
        int openFloor = 0;
        int openRoof = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!rock.at(x, minY - 1, z) && ++openFloor > allowedOpen) return false;
                if ((!rock.at(x, maxY + 1, z) || !rock.at(x, maxY + 2, z)) && ++openRoof > allowedOpen) return false;
            }
        }
        return true;
    }
}
