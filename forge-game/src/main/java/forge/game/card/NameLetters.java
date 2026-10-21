package forge.game.card;

/**
 * Stet, Draconic Proofreader: deleting the first letter of a name, and "whose name begins with the same letter".
 * Rulings: something with no name (face down, a blank name, or one Stet fully deleted) doesn't begin with any letter.
 */
public final class NameLetters {
    private NameLetters() { }

    /** The name without its first letter; a name with no letters in it is left as it is. */
    public static String deleteFirstLetter(final String name) {
        if (name == null) {
            return "";
        }
        for (int i = 0; i < name.length(); i = name.offsetByCodePoints(i, 1)) {
            if (Character.isLetter(name.codePointAt(i))) {
                return name.substring(0, i) + name.substring(name.offsetByCodePoints(i, 1));
            }
        }
        return name;
    }

    /**
     * The letter a name begins with, upper case, or -1 when it has none or begins with a digit. Spaces and punctuation,
     * blank lines included, are skipped (Katerina of Myra's Marvels), so "_____ Goblin" begins with G and a name that's
     * only a blank begins with nothing (Stet).
     */
    public static int initial(final String name) {
        if (name == null) {
            return -1;
        }
        for (int i = 0; i < name.length(); i = name.offsetByCodePoints(i, 1)) {
            final int c = name.codePointAt(i);
            if (Character.isLetter(c)) {
                return Character.toUpperCase(c);
            }
            if (Character.isDigit(c)) {
                return -1;
            }
        }
        return -1;
    }

    /** An initial as one of A to Z: accents dropped and Æ read as A, or -1 for a letter outside that alphabet. */
    public static int latinBase(final int initial) {
        if (initial == -1) {
            return -1;
        }
        if (initial == 'Æ') {
            return 'A';
        }
        final String base = java.text.Normalizer.normalize(new String(Character.toChars(initial)), java.text.Normalizer.Form.NFD);
        final char c = base.charAt(0);
        return c >= 'A' && c <= 'Z' ? c : -1;
    }

    public static boolean sameInitial(final String a, final String b) {
        final int i = initial(a);
        return i != -1 && i == initial(b);
    }
}
