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

    /** The letter a name begins with, upper case, or -1 when it's empty or begins with something that isn't a letter. */
    public static int initial(final String name) {
        if (name == null || name.isEmpty()) {
            return -1;
        }
        final int first = name.codePointAt(0);
        return Character.isLetter(first) ? Character.toUpperCase(first) : -1;
    }

    public static boolean sameInitial(final String a, final String b) {
        final int i = initial(a);
        return i != -1 && i == initial(b);
    }
}
