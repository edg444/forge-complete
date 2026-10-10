package forge.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.card.WinningDeck;
import forge.localinstance.properties.ForgeConstants;

/**
 * Keeps Metagamer's winning deck current: the main deck that won the latest Pro Tour (user, 2026-10-10: Pro Tours
 * only, kept up to date). Reads magic.gg - the event archive lists events newest first with each one's players in
 * finishing order, the event page links its Top 8 deck page, and that page carries each player's deck. The result is
 * cached in the user folder and refreshed at most once a day in the background; offline, the cache or the shipped
 * res/lists copy is used.
 */
public final class MetagamerDeckLoader {
    private MetagamerDeckLoader() {
    }

    private static final String ARCHIVE_URL = "https://magic.gg/events/event-archive";
    private static final String SITE = "https://magic.gg";
    // methods rather than constants, so the parsing below works without Forge's folders set up
    private static File shippedFile() {
        return new File(ForgeConstants.LISTS_DIR + "MetagamerWinningDeck.txt");
    }
    private static File cacheFile() {
        return new File(ForgeConstants.USER_DIR, "metagamer-winning-deck.txt");
    }

    /** A winning deck as stored in the shipped list and the cache. */
    static final class Deck {
        String event = "";
        String winner = "";
        String source = "";
        String fetched = "";
        final Set<String> names = new LinkedHashSet<>();
    }

    /** Loads the cached (or shipped) deck now, then refreshes it from magic.gg in the background if it's stale. */
    public static void init() {
        Deck deck = read(cacheFile());
        if (deck == null) {
            deck = read(shippedFile());
        }
        if (deck != null) {
            WinningDeck.set(deck.event, deck.winner, deck.names);
        }
        final String today = LocalDate.now().toString();
        if (deck != null && today.equals(deck.fetched)) {
            return;
        }
        final Thread refresh = new Thread(() -> {
            try {
                final Deck fresh = fetchLatest();
                if (fresh != null && !fresh.names.isEmpty()) {
                    fresh.fetched = today;
                    write(cacheFile(), fresh);
                    WinningDeck.set(fresh.event, fresh.winner, fresh.names);
                }
            } catch (final Exception e) {
                // offline or the site changed: keep what we have
                System.err.println("Metagamer: couldn't refresh the latest Pro Tour winner (" + e + ")");
            }
        }, "Metagamer winning deck");
        refresh.setDaemon(true);
        refreshing = refresh;
        refresh.start();
    }

    private static volatile Thread refreshing;

    /** Waits (a while at most) for a background refresh to finish, so nothing replaces the deck afterward. */
    public static void awaitRefresh() {
        final Thread t = refreshing;
        if (t != null) {
            try {
                t.join(30000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static Deck read(final File file) {
        if (!file.exists()) {
            return null;
        }
        final Deck deck = new Deck();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("Event=")) {
                    deck.event = line.substring(6);
                } else if (line.startsWith("Winner=")) {
                    deck.winner = line.substring(7);
                } else if (line.startsWith("Source=")) {
                    deck.source = line.substring(7);
                } else if (line.startsWith("Fetched=")) {
                    deck.fetched = line.substring(8);
                } else {
                    deck.names.add(line);
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
        return deck.names.isEmpty() ? null : deck;
    }

    static void write(final File file, final Deck deck) throws IOException {
        try (Writer out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            out.write("# Metagamer: the main deck that won the latest Pro Tour, from magic.gg\n");
            out.write("Event=" + deck.event + "\n");
            out.write("Winner=" + deck.winner + "\n");
            out.write("Source=" + deck.source + "\n");
            out.write("Fetched=" + deck.fetched + "\n");
            for (final String name : deck.names) {
                out.write(name + "\n");
            }
        }
    }

    static Deck fetchLatest() throws IOException {
        final String archive = get(ARCHIVE_URL);
        final String[] latest = latestProTour(archive);
        if (latest == null) {
            return null;
        }
        final Deck deck = new Deck();
        deck.event = latest[0];
        deck.winner = latest[1];
        final String eventPage = get(latest[2]);
        for (final String link : deckPageLinks(eventPage)) {
            final List<String> names = mainDeck(get(SITE + link), deck.winner);
            if (!names.isEmpty()) {
                deck.source = SITE + link;
                deck.names.addAll(names);
                return deck;
            }
        }
        return null;
    }

    private static final Pattern ITEM_INDEX = Pattern.compile("\\.items\\.(\\d+)\\.");

    /**
     * The newest finished Pro Tour in the archive's data as {title (dates), winner, event url}, or null. Items are
     * ordered by start date, newest first; a finished event lists its players in finishing order.
     */
    static String[] latestProTour(final String archive) {
        int best = Integer.MAX_VALUE;
        String[] result = null;
        int at = archive.indexOf("title:\"");
        while (at >= 0) {
            final int next = archive.indexOf("title:\"", at + 7);
            final String chunk = archive.substring(at, next < 0 ? archive.length() : next);
            at = next;
            final String title = field(chunk, "title");
            if (title == null || !title.startsWith("Pro Tour") || title.contains("Qualifier")) {
                continue;
            }
            final String players = field(chunk, "players");
            final String url = field(chunk, "url");
            final Matcher index = ITEM_INDEX.matcher(chunk);
            if (players == null || players.isBlank() || url == null || !index.find()) {
                continue;
            }
            final int i = Integer.parseInt(index.group(1));
            if (i < best) {
                best = i;
                final String dates = field(chunk, "dateRange");
                result = new String[] {
                        dates == null ? title : title + " (" + dates + ")",
                        players.split("\\s*\\|\\s*")[0].trim(),
                        url
                };
            }
        }
        return result;
    }

    /** A string-valued field of the serialized page data, unescaped; null when absent or not a literal string. */
    static String field(final String chunk, final String name) {
        final int start = chunk.indexOf(name + ":\"");
        if (start < 0) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = start + name.length() + 2; i < chunk.length(); i++) {
            final char c = chunk.charAt(i);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\' && i + 1 < chunk.length()) {
                final char e = chunk.charAt(++i);
                if (e == 'u' && i + 4 < chunk.length()) {
                    sb.append((char) Integer.parseInt(chunk.substring(i + 1, i + 5), 16));
                    i += 4;
                } else {
                    sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
        return null;
    }

    private static final Pattern TOP8_LINK = Pattern.compile("href=\"(/(?:decklists|news)/[^\"]*top-8[^\"]*)\"");
    private static final Pattern DECK_LINK = Pattern.compile("href=\"(/decklists/[^\"]*)\"");

    /** The event's Top 8 deck pages first (the finals' format), then its other decklist pages. */
    static List<String> deckPageLinks(final String eventPage) {
        final Set<String> links = new LinkedHashSet<>();
        Matcher m = TOP8_LINK.matcher(eventPage);
        while (m.find()) {
            links.add(m.group(1));
        }
        m = DECK_LINK.matcher(eventPage);
        while (m.find()) {
            links.add(m.group(1));
        }
        return new ArrayList<>(links);
    }

    /** The card names in a player's main deck on a magic.gg decklist page, or empty. */
    static List<String> mainDeck(final String page, final String player) {
        final List<String> names = new ArrayList<>();
        final int deck = page.indexOf("deck-title=\"" + player + "\"");
        if (deck < 0) {
            return names;
        }
        final int start = page.indexOf("<main-deck>", deck);
        final int end = page.indexOf("</main-deck>", start);
        if (start < 0 || end < 0) {
            return names;
        }
        for (String line : page.substring(start + 11, end).replaceAll("<[^>]+>", "\n").split("\n")) {
            line = unescapeHtml(line.trim());
            // "4 Lightning Bolt", sometimes with a trailing "[id]"
            final Matcher m = Pattern.compile("^\\d+\\s+(.+?)(?:\\s*\\[[^\\]]*\\])?$").matcher(line);
            if (m.find()) {
                names.add(m.group(1));
            }
        }
        return names;
    }

    private static String unescapeHtml(final String s) {
        return s.replace("&amp;", "&").replace("&#39;", "'").replace("&#x27;", "'").replace("&quot;", "\"")
                .replace("&lt;", "<").replace("&gt;", ">");
    }

    private static String get(final String url) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Forge)");
        // a plain read loop rather than readAllBytes, which older Android versions lack
        try (InputStream in = conn.getInputStream()) {
            final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            final byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        } finally {
            conn.disconnect();
        }
    }
}
