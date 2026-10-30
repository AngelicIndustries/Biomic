package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.Biomic;

/**
 * {@code -Dbiomic.traceCell=x,y,z[;x,y,z...]}: logs everything population does to these cells (journal writes, how a
 * journal's write is admitted or refused, queued writes, other writes), to find why a cell depends on generation order.
 */
public final class CellTrace {

    private static final long[] CELLS = parse(System.getProperty("biomic.traceCell", ""));
    public static final boolean ON = CELLS.length > 0;

    private CellTrace() {}

    private static long[] parse(String value) {
        if (value.isEmpty()) return new long[0];
        String[] cells = value.split(";");
        long[] out = new long[cells.length];
        for (int i = 0; i < cells.length; i++) {
            String[] p = cells[i].trim()
                .split(",");
            out[i] = key(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()));
        }
        return out;
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (y & 0xFFFL);
    }

    public static boolean traced(int x, int y, int z) {
        if (!ON) return false;
        long key = key(x, y, z);
        for (long cell : CELLS) if (cell == key) return true;
        return false;
    }

    public static void log(int x, int y, int z, String format, Object... args) {
        if (traced(x, y, z)) Biomic.LOG.info("[cell " + x + " " + y + " " + z + "] " + format, args);
    }

    /** A short stack of the caller, for writes that come from outside the journal. */
    public static String caller() {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        StringBuilder out = new StringBuilder();
        for (int i = 2; i < Math.min(stack.length, 14); i++) {
            String c = stack[i].getClassName();
            if (c.startsWith("java.") || c.startsWith("sun.")) continue;
            out.append(c.substring(c.lastIndexOf('.') + 1))
                .append('.')
                .append(stack[i].getMethodName())
                .append(':')
                .append(stack[i].getLineNumber())
                .append(' ');
        }
        return out.toString();
    }
}
