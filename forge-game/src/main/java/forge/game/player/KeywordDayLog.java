package forge.game.player;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The keywords a person has chosen for a card named Modular Monstrosity today - a real calendar day, across every game
 * and every Monstrosity they play with that day (rulings). Kept in memory unless the GUI layer plugs in a store that
 * lasts between sessions (FModel writes it to the user folder).
 */
public final class KeywordDayLog {
    private KeywordDayLog() { }

    public interface Store {
        Set<String> chosen(String person, String day);
        void add(String person, String day, String keyword);
    }

    private static Store store = new Store() {
        private final Map<String, Set<String>> byPersonDay = new HashMap<>();
        @Override
        public Set<String> chosen(final String person, final String day) {
            return new HashSet<>(byPersonDay.getOrDefault(person + "|" + day, Set.of()));
        }
        @Override
        public void add(final String person, final String day, final String keyword) {
            byPersonDay.computeIfAbsent(person + "|" + day, k -> new HashSet<>()).add(keyword);
        }
    };

    public static void setStore(final Store store0) {
        store = store0;
    }

    public static String today() {
        return LocalDate.now().toString();
    }

    /** The person making this player's choices - who "you" is for "you haven't chosen ... today". */
    private static String person(final Player p) {
        return p.getController().getLobbyPlayer().getName();
    }

    public static Set<String> chosenToday(final Player p) {
        return store.chosen(person(p), today());
    }

    public static void record(final Player p, final String keyword) {
        store.add(person(p), today(), keyword);
    }
}
