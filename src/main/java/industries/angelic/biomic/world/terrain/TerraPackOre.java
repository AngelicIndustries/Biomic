/*
 * Adapted Terra VanillaOre/VanillaOreUtils, revision 3745c3e. Copyright (c) 2020-2023
 * Polyhedral Development; MIT, see THIRD_PARTY_NOTICES.md. Detached string-state platform adapter.
 */
package industries.angelic.biomic.world.terrain;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.dfsek.terra.api.util.MathUtil;
import com.google.gson.JsonObject;

final class TerraPackOre {

    interface Access {

        int minY();

        int maxY();

        String get(int x, int y, int z);

        void put(int x, int y, int z, String state, boolean physics);
    }

    private final double size, exposed;
    private final String material;
    private final Set<String> replace = new HashSet<>();
    private final Map<String, String> overrides = new HashMap<>();
    private final boolean physics;
    private final boolean scattered;
    private final int spread;

    TerraPackOre(JsonObject data, OrigenTerrainRuntime.Compiler compiler) {
        size = compiler.numeric(data.get("size"));
        exposed = compiler.numeric(data.get("exposed"));
        if (!Double.isFinite(size) || size < 1 || size > 128 || !Double.isFinite(exposed) || exposed < 0 || exposed > 1)
            throw new IllegalArgumentException("Unsupported ore dimensions/exposure");
        material = data.get("material")
            .getAsString();
        physics = data.get("physics")
            .getAsBoolean();
        String kind = data.has("kind") ? data.get("kind")
            .getAsString() : "ORE";
        if (!kind.equals("ORE") && !kind.equals("SCATTERED_ORE"))
            throw new IllegalArgumentException("Unsupported ore kind");
        scattered = kind.equals("SCATTERED_ORE");
        spread = scattered ? (int) compiler.numeric(data.get("spread")) : 0;
        if (spread < 0 || spread > 128) throw new IllegalArgumentException("Unsupported ore spread");
        for (var value : data.getAsJsonArray("replace")) replace.add(base(value.getAsString()));
        for (var entry : data.getAsJsonObject("material_overrides")
            .entrySet())
            overrides.put(
                base(entry.getKey()),
                entry.getValue()
                    .getAsString());
    }

    boolean generate(Access world, int ox, int oy, int oz, Random random) {
        if (scattered) return scattered(world, ox, oy, oz, random);
        float angle = random.nextFloat() * (float) Math.PI;
        double eighth = size / 8F;
        double sx = ox + MathUtil.sin(angle) * eighth, ex = ox - MathUtil.sin(angle) * eighth;
        double sz = oz + MathUtil.cos(angle) * eighth, ez = oz - MathUtil.cos(angle) * eighth;
        double sy = oy + random.nextInt(3) - 2, ey = oy + random.nextInt(3) - 2;
        int count = (int) size;
        double[] points = new double[count * 4];
        for (int i = 0; i < count; i++) {
            float t = (float) i / count;
            points[i * 4] = MathUtil.lerp(t, sx, ex);
            points[i * 4 + 1] = MathUtil.lerp(t, sy, ey);
            points[i * 4 + 2] = MathUtil.lerp(t, sz, ez);
            double roll = random.nextDouble() * size / 16;
            points[i * 4 + 3] = ((MathUtil.sin((float) Math.PI * t) + 1F) * roll + 1) / 2;
        }
        for (int a = 0; a < count - 1; a++) {
            double ra = points[a * 4 + 3];
            if (ra <= 0) continue;
            for (int b = a + 1; b < count; b++) {
                double rb = points[b * 4 + 3];
                if (rb <= 0) continue;
                double dx = points[a * 4] - points[b * 4], dy = points[a * 4 + 1] - points[b * 4 + 1],
                    dz = points[a * 4 + 2] - points[b * 4 + 2], dr = ra - rb;
                if (dr * dr > dx * dx + dy * dy + dz * dz) points[(dr > 0 ? b : a) * 4 + 3] = -1;
            }
        }
        int outset = (int) Math.ceil((size / 16F * 2F + 1F) / 2F), minX = (int) (ox - Math.ceil(eighth) - outset),
            minY = oy - 2 - outset, minZ = (int) (oz - Math.ceil(eighth) - outset);
        int horizontal = (int) (2 * (Math.ceil(eighth) + outset)), vertical = 2 * (2 + outset), placed = 0;
        var visited = new BitSet(horizontal * horizontal * vertical);
        for (int i = 0; i < count; i++) {
            double radius = points[i * 4 + 3];
            if (radius <= 0) continue;
            double cx = points[i * 4], cy = points[i * 4 + 1], cz = points[i * 4 + 2];
            int lx = (int) Math.max(Math.floor(cx - radius), minX), ux = (int) Math.max(Math.floor(cx + radius), lx);
            int ly = (int) Math.max(Math.floor(cy - radius), minY), uy = (int) Math.max(Math.floor(cy + radius), ly);
            int lz = (int) Math.max(Math.floor(cz - radius), minZ), uz = (int) Math.max(Math.floor(cz + radius), lz);
            for (int x = lx; x <= ux; x++) {
                double dx = (x + .5 - cx) / radius;
                if (dx * dx >= 1) continue;
                for (int y = ly; y <= uy; y++) {
                    double dy = (y + .5 - cy) / radius;
                    if (dx * dx + dy * dy >= 1) continue;
                    for (int z = lz; z <= uz; z++) {
                        double dz = (z + .5 - cz) / radius;
                        if (dx * dx + dy * dy + dz * dz >= 1 || y < world.minY() || y >= world.maxY()) continue;
                        int index = x - minX + (y - minY) * horizontal + (z - minZ) * horizontal * vertical;
                        if (visited.get(index)) continue;
                        visited.set(index);
                        String block = base(world.get(x, y, z));
                        if (!replace.contains(block)) continue;
                        if (exposed < 1 && !(exposed > 0 && random.nextFloat() < exposed)
                            && (air(world.get(x, y, z - 1)) || air(world.get(x, y, z + 1))
                                || air(world.get(x, y - 1, z))
                                || air(world.get(x, y + 1, z))
                                || air(world.get(x - 1, y, z))
                                || air(world.get(x + 1, y, z))))
                            continue;
                        world.put(x, y, z, overrides.getOrDefault(block, material), physics);
                        placed++;
                    }
                }
            }
        }
        return placed > 0;
    }

    private boolean scattered(Access world, int ox, int oy, int oz, Random random) {
        int count = random.nextInt((int) (size + 1));
        for (int j = 0; j < count; j++) {
            int radius = Math.min(j, spread);
            int x = ox + Math.round((random.nextFloat() - random.nextFloat()) * (float) radius);
            int y = oy + Math.round((random.nextFloat() - random.nextFloat()) * (float) radius);
            int z = oz + Math.round((random.nextFloat() - random.nextFloat()) * (float) radius);
            String block = base(world.get(x, y, z));
            if (!replace.contains(block)) continue;
            if (exposed < 1 && !(exposed > 0 && random.nextFloat() < exposed)
                && (air(world.get(x, y, z - 1)) || air(world.get(x, y, z + 1))
                    || air(world.get(x, y - 1, z))
                    || air(world.get(x, y + 1, z))
                    || air(world.get(x - 1, y, z))
                    || air(world.get(x + 1, y, z))))
                continue;
            world.put(x, y, z, overrides.getOrDefault(block, material), physics);
        }
        return true;
    }

    private static String base(String state) {
        int bracket = state.indexOf('[');
        return bracket < 0 ? state : state.substring(0, bracket);
    }

    private static boolean air(String state) {
        return Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air")
            .contains(base(state));
    }
}
