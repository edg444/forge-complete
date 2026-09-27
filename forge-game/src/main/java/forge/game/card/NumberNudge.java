package forge.game.card;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * More or Less: one number printed on an object, and what it reads as until end of turn. The change is
 * applied wherever that number is read (power, toughness, mana value, an ability's parameter or cost, a
 * keyword), so the card's script is never rewritten. Traits are matched by their original parameters,
 * which every copy of an ability (on the stack, as a trigger fires) shares.
 */
public final class NumberNudge {

    public enum Kind { POWER, TOUGHNESS, MANA_COST, PARAM, COST, KEYWORD }

    private static final String[] WORDS = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight",
            "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
            "nineteen", "twenty"};
    private static final Pattern NUMBER = Pattern.compile("\\b(\\d+|" + String.join("|", WORDS) + ")\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SIGNED = Pattern.compile("([+-]?)(\\d+)");

    public final Kind kind;
    public final String label;
    /** the trait holding the parameter (PARAM, COST) */
    public final Map<String, String> paramTrait;
    public final String key;
    /** the trait whose description prints the number, and which of that number's occurrences it is */
    public final Map<String, String> descTrait;
    public final int occurrence;
    /** the keyword's text as printed (KEYWORD) */
    public final String keyword;
    public final int from;
    public final int to;
    public final long timestamp;

    NumberNudge(Kind kind, String label, Map<String, String> paramTrait, String key, Map<String, String> descTrait,
            int occurrence, String keyword, int from, int to, long timestamp) {
        this.kind = kind;
        this.label = label;
        this.paramTrait = paramTrait;
        this.key = key;
        this.descTrait = descTrait;
        this.occurrence = occurrence;
        this.keyword = keyword;
        this.from = from;
        this.to = to;
        this.timestamp = timestamp;
    }

    public NumberNudge changedTo(final int newValue, final long ts) {
        return new NumberNudge(kind, label, paramTrait, key, descTrait, occurrence, keyword, from, newValue, ts);
    }

    public int delta() {
        return to - from;
    }

    /** The numeric value of a printed number, digits or a word from zero to twenty; -1 if it isn't one. */
    static int valueOf(final String token) {
        for (int i = 0; i < WORDS.length; i++) {
            if (WORDS[i].equalsIgnoreCase(token)) {
                return i;
            }
        }
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** How many times the number is printed in the text, as digits or as a word. */
    static int countIn(final String text, final int value) {
        int n = 0;
        final Matcher m = NUMBER.matcher(text == null ? "" : text);
        while (m.find()) {
            if (valueOf(m.group(1)) == value) {
                n++;
            }
        }
        return n;
    }

    /** The text with the given occurrence of this nudge's number changed, keeping it a word if it was one. */
    public String applyToText(final String text) {
        if (text == null) {
            return null;
        }
        final Matcher m = NUMBER.matcher(text);
        int seen = 0;
        while (m.find()) {
            if (valueOf(m.group(1)) != from) {
                continue;
            }
            if (seen++ != occurrence) {
                continue;
            }
            final boolean word = !Character.isDigit(m.group(1).charAt(0));
            String replacement = word && to >= 0 && to < WORDS.length ? WORDS[to] : String.valueOf(to);
            if (word && Character.isUpperCase(m.group(1).charAt(0))) {
                replacement = Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
            }
            int start = m.start(1);
            // "+0" nudged down reads "-1", not "+-1"
            if (to < 0 && start > 0 && text.charAt(start - 1) == '+') {
                start--;
            }
            return text.substring(0, start) + replacement + text.substring(m.end(1));
        }
        return text;
    }

    /** A parameter value with this nudge's number changed: the value itself ("3", "+2") or its first bare number token. */
    public String applyToParam(final String value) {
        if (value == null) {
            return null;
        }
        final String[] tokens = value.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            final Matcher m = SIGNED.matcher(tokens[i]);
            if (!m.matches() || Integer.parseInt(m.group(2)) != from) {
                continue;
            }
            // the sign isn't part of the number: a printed -2 changed to 3 reads -3
            final int changed = "-".equals(m.group(1)) ? -to : to;
            tokens[i] = "+".equals(m.group(1)) && changed >= 0 ? "+" + changed : String.valueOf(changed);
            return String.join(" ", tokens);
        }
        return value;
    }

    /** A keyword's text ("Bushido:2", "Flashback:2 U") with this nudge's number changed. */
    public String applyToKeyword(final String kw) {
        final int colon = kw.indexOf(':');
        if (colon < 0) {
            return kw;
        }
        final String[] parts = kw.substring(colon + 1).split(":", -1);
        for (int i = 0; i < parts.length; i++) {
            final String changed = applyToParam(parts[i]);
            if (!changed.equals(parts[i])) {
                parts[i] = changed;
                return kw.substring(0, colon + 1) + String.join(":", parts);
            }
        }
        return kw;
    }
}
