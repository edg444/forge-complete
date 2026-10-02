package forge.util;

/**
 * Infinite quantities (Infinity Elemental's power, the life its lifelink gains, the damage it deals). The engine
 * counts in ints everywhere, so infinity is a reserved value rather than a flag on every quantity: {@link #VALUE}, and
 * anything at or past {@link #THRESHOLD} reads as infinite. VALUE is small enough that sums of a hundred infinities,
 * or an AI evaluation multiplying one by a few hundred, can't overflow - and big enough that no finite game reaches
 * THRESHOLD. Arithmetic that can reach it goes through these helpers so the result is VALUE again, not VALUE + 3:
 * Infinity Elemental ties with another Infinity Elemental (rulings).
 */
public final class Infinity {
    private Infinity() { }

    public static final int VALUE = 1 << 24;
    public static final int THRESHOLD = 1 << 20;

    public static boolean isInfinite(final int n) {
        return n >= THRESHOLD;
    }

    public static boolean isNegativeInfinite(final int n) {
        return n <= -THRESHOLD;
    }

    public static boolean isEitherInfinite(final int n) {
        return isInfinite(n) || isNegativeInfinite(n);
    }

    /** VALUE or -VALUE for anything past the threshold, n otherwise. */
    public static int normalize(final int n) {
        if (isInfinite(n)) {
            return VALUE;
        }
        if (isNegativeInfinite(n)) {
            return -VALUE;
        }
        return n;
    }

    /** a + b, where an infinite term wins (∞ + -∞ is left as the first term's: nothing in Magic asks for it). */
    public static int add(final int a, final int b) {
        if (isEitherInfinite(a)) {
            return normalize(a);
        }
        if (isEitherInfinite(b)) {
            return normalize(b);
        }
        return normalize((int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, (long) a + b)));
    }

    public static int subtract(final int a, final int b) {
        return add(a, isEitherInfinite(b) ? -normalize(b) : -b);
    }

    /** How a quantity is shown: "∞", "-∞", or the number. */
    public static String format(final int n) {
        if (isInfinite(n)) {
            return "∞";
        }
        if (isNegativeInfinite(n)) {
            return "-∞";
        }
        return String.valueOf(n);
    }

    /** Reads "∞" (or "Infinity"/"INF") as VALUE, anything else as an int. */
    public static int parse(final String s) {
        final String t = s.trim();
        if (t.equals("∞") || t.equalsIgnoreCase("Infinity") || t.equalsIgnoreCase("INF")) {
            return VALUE;
        }
        if (t.equals("-∞")) {
            return -VALUE;
        }
        return Integer.parseInt(t);
    }

    public static boolean isInfinityText(final String s) {
        final String t = s.trim();
        return t.equals("∞") || t.equalsIgnoreCase("Infinity") || t.equalsIgnoreCase("INF");
    }
}
