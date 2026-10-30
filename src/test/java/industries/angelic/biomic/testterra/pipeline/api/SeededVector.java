// Generated test-only Terra 3745c3e oracle; MIT, see adjacent LICENSE.
package industries.angelic.biomic.testterra.pipeline.api;

public record SeededVector(long seed, int x, int z) {

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SeededVector that) {
            return this.z == that.z && this.x == that.x && this.seed == that.seed;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int code = x;
        code = 31 * code + z;
        return 31 * code + ((int) (seed ^ (seed >>> 32)));
    }
}
