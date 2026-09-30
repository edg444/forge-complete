package forge.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Ordinary English words (res/lists/Words.txt), for the cards that make a player come up with a word - Unstable's
 * Hangman. One word per line under a [common] or [more] heading; '#' starts a comment. The AI notes its words from
 * the common ones and guesses against all of them; a person may note any word at all.
 */
public final class WordList {
    private WordList() { }

    private static Supplier<List<String>> source = null;
    private static Set<String> common = null;
    private static Set<String> all = null;

    /** Set by the GUI layer at startup; the file isn't read until something asks for it. */
    public static synchronized void setSource(final Supplier<List<String>> lineSource) {
        source = lineSource;
        common = null;
        all = null;
    }

    private static synchronized void load() {
        if (all != null) {
            return;
        }
        final Set<String> commonWords = new LinkedHashSet<>();
        final Set<String> allWords = new LinkedHashSet<>();
        if (source != null) {
            boolean inCommon = true;
            for (final String raw : source.get()) {
                final String line = raw.trim();
                if (line.startsWith("[")) {
                    inCommon = line.equalsIgnoreCase("[common]");
                    continue;
                }
                final String word = line.toUpperCase();
                if (!isPlainWord(word)) {
                    continue;
                }
                allWords.add(word);
                if (inCommon) {
                    commonWords.add(word);
                }
            }
        }
        common = commonWords;
        all = allWords;
    }

    /** Only the letters A to Z, and at least one of them. */
    public static boolean isPlainWord(final String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        for (int i = 0; i < word.length(); i++) {
            final char c = word.charAt(i);
            if (!(c >= 'A' && c <= 'Z') && !(c >= 'a' && c <= 'z')) {
                return false;
            }
        }
        return true;
    }

    private static List<String> ofLength(final Set<String> words, final int minLetters, final int maxLetters) {
        final List<String> result = new ArrayList<>();
        for (final String word : words) {
            if (word.length() >= minLetters && word.length() <= maxLetters) {
                result.add(word);
            }
        }
        return result;
    }

    /** Every word with minLetters to maxLetters letters, in upper case, the common ones first. */
    public static List<String> get(final int minLetters, final int maxLetters) {
        load();
        final List<String> result = ofLength(common, minLetters, maxLetters);
        for (final String word : ofLength(all, minLetters, maxLetters)) {
            if (!common.contains(word)) {
                result.add(word);
            }
        }
        return result;
    }

    /** The everyday words with minLetters to maxLetters letters, in upper case. */
    public static List<String> getCommon(final int minLetters, final int maxLetters) {
        load();
        return ofLength(common, minLetters, maxLetters);
    }

    public static boolean isCommon(final String word) {
        load();
        return word != null && common.contains(word.toUpperCase());
    }
}
