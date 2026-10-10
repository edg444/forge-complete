package forge.card;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Metagamer's "the winning deck(s) of the latest Mythic Championship": per its errata ruling and the user (2026-10-10),
 * the main deck that won the latest Pro Tour - the deck played in the finals, so a draft deck when the Top 8 was a
 * draft. The GUI layer fills it (see forge.model.MetagamerDeckLoader); empty until then.
 */
public final class WinningDeck {
    private WinningDeck() {
    }

    private static volatile Set<String> names = Set.of();
    private static volatile String event = "";
    private static volatile String winner = "";

    public static void set(final String event0, final String winner0, final Collection<String> cardNames) {
        names = cardNames.stream().map(WinningDeck::key).collect(Collectors.toUnmodifiableSet());
        event = event0 == null ? "" : event0;
        winner = winner0 == null ? "" : winner0;
    }

    public static String getEvent() {
        return event;
    }

    public static String getWinner() {
        return winner;
    }

    public static Set<String> getNames() {
        return names;
    }

    /** Whether a card shares a name with a card in the deck; a split or double-faced card's faces each count. */
    public static boolean contains(final String cardName) {
        if (cardName == null || names.isEmpty()) {
            return false;
        }
        if (names.contains(key(cardName))) {
            return true;
        }
        for (final String face : cardName.split("\\s*//\\s*")) {
            if (names.contains(key(face))) {
                return true;
            }
        }
        return false;
    }

    private static String key(final String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
