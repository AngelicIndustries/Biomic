/*
 * Narrow source-order relocation of FastNoise.java (YUNG Better Caves 2.0.4,
 * e71f869808fc139773b75125919a65925a983a77), 2D default Simplex only.
 * MIT License, Copyright (c) 2017 Jordan Peck.
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

public final class YungFastNoiseSimplex2D {

    // This pinned fork uses .5/.25, NOT the usual sqrt(3) simplex constants.
    private static final float F2 = .5f;
    private static final float G2 = .25f;
    private static final int[][] GRAD = { { -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 }, { 0, -1 }, { -1, 0 }, { 0, 1 },
        { 1, 0 } };
    private final int seed;
    private final float frequency;

    public YungFastNoiseSimplex2D(int seed, float frequency) {
        if (!Float.isFinite(frequency) || frequency <= 0 || frequency > 1)
            throw new IllegalArgumentException("Invalid simplex frequency");
        this.seed = seed;
        this.frequency = frequency;
    }

    public float sample(float x, float y) {
        x *= frequency;
        y *= frequency;
        float t = (x + y) * F2;
        int i = floor(x + t), j = floor(y + t);
        t = (i + j) * G2;
        float x0 = x - (i - t), y0 = y - (j - t);
        int i1 = x0 > y0 ? 1 : 0, j1 = x0 > y0 ? 0 : 1;
        float x1 = x0 - i1 + G2, y1 = y0 - j1 + G2, x2 = x0 - 1 + F2, y2 = y0 - 1 + F2;
        float n0 = contribution(seed, i, j, x0, y0), n1 = contribution(seed, i + i1, j + j1, x1, y1),
            n2 = contribution(seed, i + 1, j + 1, x2, y2);
        return 50 * (n0 + n1 + n2);
    }

    private static float contribution(int seed, int x, int y, float xd, float yd) {
        float t = .5f - xd * xd - yd * yd;
        if (t < 0) return 0;
        t *= t;
        int hash = seed;
        hash ^= 1619 * x;
        hash ^= 31337 * y;
        hash = hash * hash * hash * 60493;
        hash = (hash >> 13) ^ hash;
        int[] g = GRAD[hash & 7];
        return t * t * (xd * g[0] + yd * g[1]);
    }

    // Deliberately preserves pinned FastFloor at exact negative integers.
    private static int floor(float f) {
        return f >= 0 ? (int) f : (int) f - 1;
    }
}
