/*
 * Narrow relocation of FastNoise.java from YUNG's Better Caves 2.0.4,
 * commit e71f869808fc139773b75125919a65925a983a77. Only the three-dimensional
 * SimplexFractal/RigidMulti path used by floored caverns is retained.
 * MIT License
 * Copyright (c) 2017 Jordan Peck
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package industries.angelic.biomic.internal.yungbettercaves;

/** Source-order-preserving FastNoise Simplex RigidMulti slice. */
public final class YungFastNoiseRigidMulti {

    private static final int X_PRIME = 1619;
    private static final int Y_PRIME = 31337;
    private static final int Z_PRIME = 6971;
    private static final float F3 = (float) (1.0 / 3.0);
    private static final float G3 = (float) (1.0 / 6.0);
    private static final float G33 = G3 * 3 - 1;
    private static final Gradient[] GRAD_3D = { new Gradient(1, 1, 0), new Gradient(-1, 1, 0), new Gradient(1, -1, 0),
        new Gradient(-1, -1, 0), new Gradient(1, 0, 1), new Gradient(-1, 0, 1), new Gradient(1, 0, -1),
        new Gradient(-1, 0, -1), new Gradient(0, 1, 1), new Gradient(0, -1, 1), new Gradient(0, 1, -1),
        new Gradient(0, -1, -1), new Gradient(1, 1, 0), new Gradient(0, -1, 1), new Gradient(-1, 1, 0),
        new Gradient(0, -1, -1) };

    private final int seed;
    private final int octaves;
    private final float gain;
    private final float frequency;

    public YungFastNoiseRigidMulti(int seed, int octaves, float gain, float frequency) {
        if (octaves < 1 || octaves > 16) throw new IllegalArgumentException("octaves must be in [1,16]");
        if (!Float.isFinite(gain) || gain < 0.0f || gain > 1.0f)
            throw new IllegalArgumentException("gain must be finite and in [0,1]");
        if (!Float.isFinite(frequency) || frequency <= 0.0f || frequency > 1.0f)
            throw new IllegalArgumentException("frequency must be finite and in (0,1]");
        this.seed = seed;
        this.octaves = octaves;
        this.gain = gain;
        this.frequency = frequency;
    }

    public float sample(float x, float y, float z) {
        x *= frequency;
        y *= frequency;
        z *= frequency;
        int octaveSeed = seed;
        float sum = 1 - Math.abs(simplex(octaveSeed, x, y, z));
        float amplitude = 1;
        for (int i = 1; i < octaves; i++) {
            x *= 2.0f;
            y *= 2.0f;
            z *= 2.0f;
            amplitude *= gain;
            sum -= (1 - Math.abs(simplex(++octaveSeed, x, y, z))) * amplitude;
        }
        return sum;
    }

    private static float simplex(int seed, float x, float y, float z) {
        float t = (x + y + z) * F3;
        int i = fastFloor(x + t), j = fastFloor(y + t), k = fastFloor(z + t);
        t = (i + j + k) * G3;
        float x0 = x - (i - t), y0 = y - (j - t), z0 = z - (k - t);
        int i1, j1, k1, i2, j2, k2;
        if (x0 >= y0) {
            if (y0 >= z0) {
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 1;
                k2 = 0;
            } else if (x0 >= z0) {
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            } else {
                i1 = 0;
                j1 = 0;
                k1 = 1;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            }
        } else {
            if (y0 < z0) {
                i1 = 0;
                j1 = 0;
                k1 = 1;
                i2 = 0;
                j2 = 1;
                k2 = 1;
            } else if (x0 < z0) {
                i1 = 0;
                j1 = 1;
                k1 = 0;
                i2 = 0;
                j2 = 1;
                k2 = 1;
            } else {
                i1 = 0;
                j1 = 1;
                k1 = 0;
                i2 = 1;
                j2 = 1;
                k2 = 0;
            }
        }
        float x1 = x0 - i1 + G3, y1 = y0 - j1 + G3, z1 = z0 - k1 + G3;
        float x2 = x0 - i2 + F3, y2 = y0 - j2 + F3, z2 = z0 - k2 + F3;
        float x3 = x0 + G33, y3 = y0 + G33, z3 = z0 + G33;
        float n0 = contribution(seed, i, j, k, x0, y0, z0);
        float n1 = contribution(seed, i + i1, j + j1, k + k1, x1, y1, z1);
        float n2 = contribution(seed, i + i2, j + j2, k + k2, x2, y2, z2);
        float n3 = contribution(seed, i + 1, j + 1, k + 1, x3, y3, z3);
        return 32 * (n0 + n1 + n2 + n3);
    }

    private static float contribution(int seed, int x, int y, int z, float xd, float yd, float zd) {
        float t = (float) 0.6 - xd * xd - yd * yd - zd * zd;
        if (t < 0) return 0;
        t *= t;
        return t * t * gradient(seed, x, y, z, xd, yd, zd);
    }

    private static float gradient(int seed, int x, int y, int z, float xd, float yd, float zd) {
        int hash = seed;
        hash ^= X_PRIME * x;
        hash ^= Y_PRIME * y;
        hash ^= Z_PRIME * z;
        hash = hash * hash * hash * 60493;
        hash = (hash >> 13) ^ hash;
        Gradient g = GRAD_3D[hash & 15];
        return xd * g.x + yd * g.y + zd * g.z;
    }

    // Deliberately preserves upstream's behavior at exact negative integers.
    private static int fastFloor(float value) {
        return value >= 0 ? (int) value : (int) value - 1;
    }

    private record Gradient(float x, float y, float z) {}
}
