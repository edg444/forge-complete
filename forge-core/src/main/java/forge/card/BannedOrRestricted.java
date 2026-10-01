package forge.card;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Cards that have been banned or restricted in a Constructed format (res/lists/BannedOrRestricted.txt), for Unstable's
 * Spike, Tournament Grinder: the Unstable FAQ's list as of December 2017 plus everything Wizards of the Coast bans or
 * restricts today, generated from Scryfall by _tools/spike-list/generate.js. One card name per line, '#' comments.
 */
public final class BannedOrRestricted {
    private BannedOrRestricted() { }

    private static Supplier<List<String>> source = null;
    private static Set<String> names = null;

    /** Set by the GUI layer at startup; the file isn't read until something asks for it. */
    public static synchronized void setSource(final Supplier<List<String>> lineSource) {
        source = lineSource;
        names = null;
    }

    public static synchronized boolean contains(final String name) {
        if (names == null) {
            names = new HashSet<>();
            if (source != null) {
                for (final String raw : source.get()) {
                    final String line = raw.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        names.add(line);
                    }
                }
            }
        }
        return name != null && names.contains(name);
    }
}
