package industries.angelic.biomic.testterra.pipelinelegacy.fixture;

/** Only the mutable coordinate-holder operations used by the original BiomeHolderImpl. */
public final class Vector2 {

    public static final class Mutable {

        private double x, z;

        public Mutable(double x, double z) {
            this.x = x;
            this.z = z;
        }

        public double getX() {
            return x;
        }

        public double getZ() {
            return z;
        }

        public Mutable setX(double value) {
            x = value;
            return this;
        }

        public Mutable setZ(double value) {
            z = value;
            return this;
        }
    }
}
