package forge.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Ordinary English words (res/lists/Words.txt, one per line, '#' starts a comment), for the cards that make a
 * player come up with a word - Unstable's Hangman. The AI notes its words from this list and guesses against it;
 * a person may note any word at all.
 */
public final class WordList {
    private WordList() { }

    private static Supplier<List<String>> source = null;
    private static List<String> words = null;

    /** Set by the GUI layer at startup; the file isn't read until something asks for it. */
    public static synchronized void setSource(final Supplier<List<String>> lineSource) {
        source = lineSource;
        words = null;
    }

    private static synchronized List<String> words() {
        if (words != null) {
            return words;
        }
        final List<String> result = new ArrayList<>();
        if (source != null) {
            for (final String line : source.get()) {
                final String word = line.trim().toUpperCase();
                if (isPlainWord(word) && !result.contains(word)) {
                    result.add(word);
                }
            }
        }
        words = Collections.unmodifiableList(result);
        return words;
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

    /** The words with minLetters to maxLetters letters, in upper case. */
    public static List<String> get(final int minLetters, final int maxLetters) {
        final List<String> result = new ArrayList<>();
        for (final String word : words()) {
            if (word.length() >= minLetters && word.length() <= maxLetters) {
                result.add(word);
            }
        }
        return result;
    }
}
