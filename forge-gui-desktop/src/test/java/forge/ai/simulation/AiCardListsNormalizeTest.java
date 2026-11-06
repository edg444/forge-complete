package forge.ai.simulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * {@link AiCardLists#normalize} dropped its regular expressions for speed on Android; it must still give exactly what
 * the two replaceAll calls gave, for every card name and for odd spacing and accents.
 */
public class AiCardListsNormalizeTest {

    private static String regexNormalize(final String name) {
        final String plain = Normalizer.normalize(name.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    @Test
    public void oddSpacingAndAccents() {
        final String[] samples = {
                "Éomer,  Marshal of Rohan", "  Lim-Dûl the Necromancer ", "Fire // Ice", "Ætherize", "Jötun Grunt",
                "a ́ b", "́ x", "x ́", "tab\there", "new\nline\r\nend", "İstanbul", "ΣΟΦΟΣ",
                "Déjà Vu", "", "   ", "Séance⃝", "xःy"
        };
        for (final String s : samples) {
            assertEquals(AiCardLists.normalize(s), regexNormalize(s), "for \"" + s + "\"");
        }
    }

    @Test
    public void everyCardName() throws IOException {
        final Path folder = Paths.get("../forge-gui/res/cardsfolder");
        final List<String> names = new ArrayList<>();
        try (Stream<Path> files = Files.walk(folder)) {
            files.filter(p -> p.toString().endsWith(".txt")).forEach(p -> {
                try {
                    for (final String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                        if (line.startsWith("Name:")) {
                            names.add(line.substring(5));
                        }
                    }
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        assertTrue(names.size() > 30000, "card names read: " + names.size());
        for (final String n : names) {
            assertEquals(AiCardLists.normalize(n), regexNormalize(n), "for \"" + n + "\"");
        }
    }
}
