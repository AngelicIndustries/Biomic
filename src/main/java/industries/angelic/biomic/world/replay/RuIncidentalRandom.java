package industries.angelic.biomic.world.replay;

import java.util.Random;

/** Replaces RU's wall-clock-seeded cosmetic decisions without consuming its authored feature RNG stream. */
public final class RuIncidentalRandom {

    public static final long TOP = 0x361ADF759C28B4E0L;
    public static final long BRANCH = 0x572EB3894F16ACD0L;
    public static final long ROOT = 0x71C5A4E923BD608FL;
    public static final long VINE = 0x62B4C13DA95708EFL;
    public static final long BASE = 0x49371CD8E250A6BFL;
    public static final long BEEHIVE = 0x34A7E98CD61502BFL;
    public static final long LEAF_BLOB = 0x1193E8BC7D02465AL;
    public static final long JOSHUA_CARDINAL = 0x2C73491FA58DE6B0L;
    public static final long JOSHUA_DIAGONAL = 0x4D8260AE97BC135FL;
    public static final long CYPRESS_LOWER = 0x6A10391BC44DA327L;
    public static final long CYPRESS_UPPER = 0x7518BE3D20649CAFL;
    public static final long CYPRESS_MOSS = 0x280D6E43AB175C99L;
    public static final long ICICLE = 0x4739FA861CD258E0L;
    public static final long ROCK_BLOB = 0x764283CA91BD502EL;
    public static final long WETLAND_BLOB = 0x1573BDE904C862AFL;
    public static final long PRISMARITE_PILLAR = 0x3BA570FD9126CE84L;
    public static final long MARSH_BLOCK = 0x417FBDC390E8A62DL;

    private RuIncidentalRandom() {}

    public static int sample(long seed, int x, int y, int z, long purpose, int bound) {
        return random(seed, x, y, z, purpose).nextInt(bound);
    }

    public static Random random(long seed, int x, int y, int z, long purpose) {
        long coordinate = mix(((long) x << 32) ^ (z & 0xffffffffL)) ^ mix(y);
        return new Random(mix(seed ^ coordinate ^ purpose));
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
