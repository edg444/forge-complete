package forge.util;

import forge.localinstance.properties.ForgeConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps a printing (Scryfall set code + collector number + face) to a direct image URL on Scryfall's
 * image CDN, so downloading art doesn't depend on Scryfall's API being up.
 * <p>
 * The API host answers image requests with a redirect to the CDN, which makes it a single point of
 * failure for every image in the app: when it is rate limiting, challenging non-browser clients, or
 * down for maintenance, no art downloads at all. The CDN itself stays up through all of that, but
 * addressing it needs each printing's Scryfall id - which MTGJSON publishes per set. So this fetches
 * the (small) MTGJSON file for a set once, keeps a set code + collector number -> id map on disk,
 * and builds CDN URLs locally from then on.
 * <p>
 * Resolution is deliberately lazy and off the EDT: {@link #placeholderUrl} is what goes into the
 * download list, and the fetcher's worker thread turns it into a real URL via {@link #resolve}.
 */
public final class ScryfallImageIndex {
    public static final String PLACEHOLDER_PREFIX = "SCRYFALLID:";

    private static final Map<String, Map<String, String>> SET_INDEXES = new ConcurrentHashMap<>();
    // sets whose MTGJSON file could not be read; retrying them every image would stall every fetch
    private static final Set<String> FAILED_SETS = ConcurrentHashMap.newKeySet();
    // sets already re-fetched this session, so one unknown card can't cause repeated downloads
    private static final Set<String> REFRESHED_SETS = ConcurrentHashMap.newKeySet();
    // below this age a miss is treated as "no such printing" rather than "index out of date"
    private static final long REFRESH_AFTER_MS = 12 * 60 * 60 * 1000L;

    private ScryfallImageIndex() {
    }

    public static String placeholderUrl(ImageUtil.ScryfallCardRef ref, boolean useArtCrop) {
        return PLACEHOLDER_PREFIX + ref.editionCode + "|" + ref.collectorNumber + "|"
                + (ref.isBackFace() ? "back" : "front") + "|" + (useArtCrop ? "art_crop" : "normal");
    }

    public static String placeholderTokenUrl(String setCode, String collectorNumber, String face) {
        return PLACEHOLDER_PREFIX + setCode + "|" + collectorNumber + "|"
                + ("back".equals(face) ? "back" : "front") + "|normal";
    }

    /**
     * Turns a placeholder into a real CDN URL, downloading the set's id map if this is the first
     * time it's been needed. Blocking - call from a download worker, never the UI thread. Returns
     * null when the printing has no known id, letting the caller fall through to its other URLs.
     */
    public static String resolve(String placeholder) {
        String[] parts = placeholder.substring(PLACEHOLDER_PREFIX.length()).split("\\|", -1);
        if (parts.length != 4) {
            return null;
        }
        String setCode = parts[0];
        String collectorNumber = parts[1];
        String face = parts[2];
        String version = parts[3];

        Map<String, String> index = getSetIndex(setCode);
        if (index == null) {
            return null;
        }
        String id = index.get(collectorNumber.toLowerCase());
        if (id == null) {
            // A miss can mean the printing genuinely has no Scryfall id, or that the cached index
            // predates it - sets gain cards all through spoiler season. Refreshing only on a miss
            // (and only once per set per session) keeps that from costing anything in the normal
            // case, where every card asked for is already in the index.
            index = refreshIfStale(setCode);
            if (index != null) {
                id = index.get(collectorNumber.toLowerCase());
            }
        }
        if (id == null || id.length() < 2) {
            return null;
        }
        // CDN paths shard by the first two characters of the id
        return ForgeConstants.URL_PIC_SCRYFALL_CDN + version + "/" + face + "/"
                + id.charAt(0) + "/" + id.charAt(1) + "/" + id + ".jpg";
    }

    public static boolean isPlaceholder(String url) {
        return url != null && url.startsWith(PLACEHOLDER_PREFIX);
    }

    private static Map<String, String> getSetIndex(String setCode) {
        String key = setCode.toUpperCase();
        Map<String, String> cached = SET_INDEXES.get(key);
        if (cached != null) {
            return cached;
        }
        if (FAILED_SETS.contains(key)) {
            return null;
        }

        synchronized (ScryfallImageIndex.class) {
            cached = SET_INDEXES.get(key);
            if (cached != null) {
                return cached;
            }

            Map<String, String> index = readFromDisk(key);
            if (index == null) {
                index = downloadSetIndex(key);
                if (index != null) {
                    writeToDisk(key, index);
                }
            }
            if (index == null) {
                FAILED_SETS.add(key);
                return null;
            }
            SET_INDEXES.put(key, index);
            return index;
        }
    }

    /**
     * Re-downloads a set's index if the cached copy is old enough to plausibly be missing a card.
     * Returns the current index either way, or null if there isn't one.
     */
    private static Map<String, String> refreshIfStale(String setCode) {
        String key = setCode.toUpperCase();
        synchronized (ScryfallImageIndex.class) {
            if (!REFRESHED_SETS.add(key)) {
                return SET_INDEXES.get(key);
            }
            File file = indexFile(key);
            if (file.exists() && System.currentTimeMillis() - file.lastModified() < REFRESH_AFTER_MS) {
                return SET_INDEXES.get(key);
            }
            Map<String, String> fresh = downloadSetIndex(key);
            if (fresh == null) {
                return SET_INDEXES.get(key);
            }
            writeToDisk(key, fresh);
            SET_INDEXES.put(key, fresh);
            return fresh;
        }
    }

    private static File indexFile(String setCode) {
        return new File(ForgeConstants.CACHE_SCRYFALL_ID_DIR, setCode + ".txt");
    }

    private static Map<String, String> readFromDisk(String setCode) {
        File file = indexFile(setCode);
        if (!file.exists()) {
            return null;
        }
        Map<String, String> index = new HashMap<>();
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                int tab = line.indexOf('\t');
                if (tab > 0) {
                    index.put(line.substring(0, tab), line.substring(tab + 1));
                }
            }
        } catch (IOException e) {
            return null;
        }
        return index;
    }

    private static void writeToDisk(String setCode, Map<String, String> index) {
        try {
            Files.createDirectories(Paths.get(ForgeConstants.CACHE_SCRYFALL_ID_DIR));
            List<String> lines = new ArrayList<>(index.size());
            for (Map.Entry<String, String> e : index.entrySet()) {
                lines.add(e.getKey() + "\t" + e.getValue());
            }
            Files.write(indexFile(setCode).toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not cache Scryfall id index for " + setCode + ": " + e.getMessage());
        }
    }

    private static Map<String, String> downloadSetIndex(String setCode) {
        String url = ForgeConstants.URL_MTGJSON_SET + setCode + ".json";
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", BuildInfo.getUserAgent());
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                conn.disconnect();
                return null;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buf = new char[1 << 16];
                int read;
                while ((read = in.read(buf)) > 0) {
                    sb.append(buf, 0, read);
                }
            }
            conn.disconnect();
            return parseSetIndex(sb.toString());
        } catch (Exception e) {
            System.err.println("Could not fetch Scryfall ids for " + setCode + " from MTGJSON: " + e.getMessage());
            return null;
        }
    }

    private static Map<String, String> parseSetIndex(String json) {
        Map<String, Object> root = MiniJson.asMap(MiniJson.parse(json));
        if (root == null) {
            return null;
        }
        Map<String, Object> data = MiniJson.asMap(root.get("data"));
        if (data == null) {
            return null;
        }
        Map<String, String> index = new HashMap<>();
        collectIds(MiniJson.asList(data.get("cards")), index);
        collectIds(MiniJson.asList(data.get("tokens")), index);
        return index.isEmpty() ? null : index;
    }

    private static void collectIds(List<Object> cards, Map<String, String> index) {
        if (cards == null) {
            return;
        }
        for (Object entry : cards) {
            Map<String, Object> card = MiniJson.asMap(entry);
            if (card == null) {
                continue;
            }
            Map<String, Object> identifiers = MiniJson.asMap(card.get("identifiers"));
            Object number = card.get("number");
            if (identifiers == null || !(number instanceof String)) {
                continue;
            }
            Object id = identifiers.get("scryfallId");
            if (id instanceof String) {
                // one printing per collector number; the first entry wins so a set's own ordering,
                // not map iteration, decides ties (relevant for the double-listed funny printings)
                index.putIfAbsent(((String) number).toLowerCase(), (String) id);
            }
        }
    }

    /** Forces a re-download next time the set is needed; used when a CDN URL turns out to be stale. */
    public static void invalidate(String setCode) {
        String key = setCode.toUpperCase();
        SET_INDEXES.remove(key);
        FAILED_SETS.remove(key);
        File file = indexFile(key);
        if (file.exists() && !file.delete()) {
            System.err.println("Could not clear cached Scryfall id index for " + key);
        }
    }

    /** Set codes with an id map already on disk - exposed for diagnostics. */
    public static Set<String> cachedSets() {
        Set<String> sets = new HashSet<>();
        File dir = new File(ForgeConstants.CACHE_SCRYFALL_ID_DIR);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                if (name.endsWith(".txt")) {
                    sets.add(name.substring(0, name.length() - 4));
                }
            }
        }
        return sets;
    }
}
