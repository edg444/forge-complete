package forge.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * The words legacy (Inspirational Antelope) lets you write in a word blank: every keyword ability and ability word in
 * the Comprehensive Rules (702, 207.2c), from {@code res/lists/LegacyWords.txt}. The engine adds the keywords it knows
 * that the rules don't list (silver-bordered and playtest ones, legacy itself among them).
 */
public final class LegacyWords {
    private LegacyWords() { }

    private static Supplier<List<String>> source = null;
    private static List<String> keywordAbilities = null;
    private static List<String> abilityWords = null;
    private static List<String> keywordActions = null;

    public static synchronized void setSource(final Supplier<List<String>> source0) {
        source = source0;
        keywordAbilities = null;
        abilityWords = null;
        keywordActions = null;
    }

    private static synchronized void load() {
        if (keywordAbilities != null) {
            return;
        }
        final List<String> keywords = new ArrayList<>();
        final List<String> words = new ArrayList<>();
        final List<String> actions = new ArrayList<>();
        List<String> section = null;
        for (final String raw : source == null ? List.<String>of() : source.get()) {
            final String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.equals("[KeywordAbilities]")) {
                section = keywords;
            } else if (line.equals("[AbilityWords]")) {
                section = words;
            } else if (line.equals("[KeywordActions]")) {
                section = actions;
            } else if (section != null) {
                section.add(line);
            }
        }
        keywordAbilities = Collections.unmodifiableList(keywords);
        abilityWords = Collections.unmodifiableList(words);
        keywordActions = Collections.unmodifiableList(actions);
    }

    public static List<String> getKeywordAbilities() {
        load();
        return keywordAbilities;
    }

    public static List<String> getAbilityWords() {
        load();
        return abilityWords;
    }

    /** The Horizon Seeker: the keyword actions (701), less the everyday ones (user ruling 2026-10-21). Not for legacy. */
    public static List<String> getKeywordActions() {
        load();
        return keywordActions;
    }
}
