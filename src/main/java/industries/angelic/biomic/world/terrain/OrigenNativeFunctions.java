package industries.angelic.biomic.world.terrain;

import java.lang.reflect.Method;

/** Allocation-free forms of Origen's pinned Terra helper expressions. */
public final class OrigenNativeFunctions {

    private OrigenNativeFunctions() {}

    static Method find(String name, int arguments) {
        Class<?>[] signature = new Class<?>[arguments];
        for (int index = 0; index < signature.length; index++) signature[index] = double.class;
        try {
            return OrigenNativeFunctions.class.getMethod(name, signature);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    public static double clamp(double x, double a, double b) {
        return Math.min(Math.max(x, a), b);
    }

    public static double herp(double t, double at, double a, double bt, double b) {
        if (at < bt) {
            if (t <= at) return a;
            if (t >= bt) return b;
        } else {
            if (t >= at) return a;
            if (t <= bt) return b;
        }
        double x = (t - bt) / (at - bt);
        double squared = x * x;
        double hermite = 3.0D * squared - 2.0D * squared * x;
        return hermite * (a - b) + b;
    }

    public static double lerp(double t, double at, double a, double bt, double b) {
        if (at < bt) {
            if (t <= at) return a;
            if (t >= bt) return b;
        } else {
            if (t >= at) return a;
            if (t <= bt) return b;
        }
        return a * (t - bt) / (at - bt) + b * (t - at) / (bt - at);
    }

    public static double maskSmooth(double value, double lower, double upper, double mask) {
        if (lower < upper) {
            if (mask <= lower) return 0.0D;
            if (mask >= upper) return value;
        } else {
            if (mask >= lower) return 0.0D;
            if (mask <= upper) return value;
        }
        double x = (mask - lower) / (upper - lower);
        double squared = x * x;
        return value * (3.0D * squared - 2.0D * squared * x);
    }

    public static double parabolicMap(double t, double bt, double b, double at, double a) {
        if (at < bt) {
            if (t <= at) return a;
            if (t >= bt) return b;
        } else {
            if (t >= at) return a;
            if (t <= bt) return b;
        }
        double x = (t - bt) / (at - bt);
        return x * x * (a - b) + b;
    }

    public static double terrace(double input, double scale, double offset, double gradient, double depth) {
        double x = terraceCoordinate(input, scale, offset, gradient);
        return depth * scale * (Math.abs(x - 0.5D) - 0.5D) + input;
    }

    public static double terraceParabolic(double input, double scale, double offset, double gradient, double depth) {
        double x = terraceCoordinate(input, scale, offset, gradient) - 0.5D;
        return depth * scale * (x * x - 0.25D) + input;
    }

    public static double terraceParalinear(double input, double scale, double offset, double gradient, double depth) {
        double x = terraceCoordinate(input, scale, offset, gradient);
        double profile;
        if (0.25D < x && x < 0.75D) {
            double centered = x - 0.5D;
            profile = 2.0D * centered * centered - 0.375D;
        } else {
            profile = Math.abs(x - 0.5D) - 0.5D;
        }
        return depth * scale * profile + input;
    }

    public static double terraceStrata(double input, double scale, double offset, double gradient, double depth) {
        double x = terraceCoordinate(input, scale, offset, gradient);
        double profile = x > 0.95D ? (x - 1.0D) / 0.05D : -x / 0.95D;
        return depth * scale * profile + input;
    }

    private static double terraceCoordinate(double input, double scale, double offset, double gradient) {
        double value = input / scale - offset;
        double period = 1.0D + gradient;
        double result = value - Math.floor(value / period) * period;
        return 0.0D <= result && result < 1.0D ? result : 0.0D;
    }
}
