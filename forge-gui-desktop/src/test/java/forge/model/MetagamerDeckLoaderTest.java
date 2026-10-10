package forge.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Metagamer's magic.gg reader, against fragments of the real pages saved 2026-10-10: the event archive's data (a
 * Magic Spotlight, Pro Tour Secrets of Strixhaven and Pro Tour Marvel Super Heroes, newest first by item index) and
 * the Marvel Super Heroes Top 8 draft deck page. Set -Dforge.metagamer.live=true to also fetch from magic.gg.
 */
public class MetagamerDeckLoaderTest {

    private static String resource(String name) throws IOException {
        try (InputStream in = MetagamerDeckLoaderTest.class.getResourceAsStream("/metagamer/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void testLatestProTourAndItsWinner() throws IOException {
        String[] latest = MetagamerDeckLoader.latestProTour(resource("archive-sample.txt"));
        AssertJUnit.assertNotNull(latest);
        AssertJUnit.assertTrue(latest[0], latest[0].startsWith("Pro Tour") && latest[0].contains("Marvel Super Heroes"));
        AssertJUnit.assertEquals("Yuuki Ichikawa", latest[1]);
        AssertJUnit.assertEquals("https://magic.gg/events/pro-tour-magic-the-gathering-marvel-super-heroes", latest[2]);
    }

    @Test
    public void testWinnersMainDeckOnly() throws IOException {
        List<String> names = MetagamerDeckLoader.mainDeck(resource("top8-sample.html"), "Yuuki Ichikawa");
        AssertJUnit.assertEquals(25, names.size());
        AssertJUnit.assertTrue(names.contains("HULK SMASH!"));
        AssertJUnit.assertTrue(names.contains("Bullseye, Death Dealer"));
        AssertJUnit.assertTrue(names.contains("Forest"));
        // sideboard cards aren't in the deck
        AssertJUnit.assertFalse(names.contains("Dauthi Voidwalker"));
        AssertJUnit.assertFalse(names.contains("Red Hulk"));
        AssertJUnit.assertTrue(MetagamerDeckLoader.mainDeck(resource("top8-sample.html"), "Lorenzo Gruppi").isEmpty());
    }

    @Test
    public void testLiveFetch() throws IOException {
        if (!Boolean.getBoolean("forge.metagamer.live")) {
            throw new SkipException("live magic.gg fetch only with -Dforge.metagamer.live=true");
        }
        MetagamerDeckLoader.Deck deck = MetagamerDeckLoader.fetchLatest();
        AssertJUnit.assertNotNull(deck);
        System.out.println("Metagamer live: " + deck.event + " / " + deck.winner + " / " + deck.source + " / " + deck.names);
        AssertJUnit.assertFalse(deck.names.isEmpty());
    }
}
