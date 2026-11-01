package forge.deck.generation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckProxy;
import forge.deck.DeckType;
import forge.deck.DeckgenUtil;
import forge.deck.RandomDeckGenerator;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.model.FModel;

/**
 * Random decks with a format picked in the lobby, 2026-11-01: they draw only on the format's cards, so the deck passes
 * the check the lobby runs at the start of a game. Vintage restricts cards the generator would take four of.
 */
public class FormatRandomDeckTest extends AITest {
    private static final String[] FORMATS = {"Standard", "Pauper", "Pioneer", "Modern", "Legacy", "Vintage", "Premodern"};

    private static void assertLegal(final GameFormat format, final Deck deck) {
        AssertJUnit.assertEquals(format.getName(), 60, deck.getMain().countAll());
        AssertJUnit.assertNull(format.getName() + ": " + deck.getMain(), format.getDeckConformanceProblem(deck));
    }

    @Test
    public void colorDecksAreLegalInTheFormat() {
        initAndCreateGame();
        for (final String name : FORMATS) {
            final GameFormat format = FModel.getFormats().get(name);
            final List<String> colors = new ArrayList<>();
            for (int count = 1; count <= 3; count++) {
                colors.add("Random " + count);
                for (int i = 0; i < 8; i++) {
                    assertLegal(format, DeckgenUtil.buildFormatColorDeck(colors, format, i % 2 == 0));
                }
            }
        }
    }

    @Test
    public void namedColorsStayLegalInTheFormat() {
        initAndCreateGame();
        final GameFormat format = FModel.getFormats().getStandard();
        for (final String color : List.of("White", "Blue", "Black", "Red", "Green")) {
            assertLegal(format, DeckgenUtil.buildFormatColorDeck(List.of(color), format, true));
        }
    }

    @Test
    public void randomGeneratedDecksAreLegalInTheFormat() {
        initAndCreateGame();
        for (final String name : FORMATS) {
            final GameFormat format = FModel.getFormats().get(name);
            final List<DeckProxy> decks = RandomDeckGenerator.getRandomDecks(() -> GameType.Constructed, () -> format, true);
            for (int i = 0; i < 8; i++) {
                assertLegal(format, decks.get(0).getDeck());
            }
        }
    }

    @Test
    public void aRandomUserDeckIsLegalInTheFormat() {
        initAndCreateGame();
        final GameFormat format = FModel.getFormats().getStandard();
        final List<DeckProxy> decks = RandomDeckGenerator.getRandomDecks(() -> GameType.Constructed, () -> format, true);
        AssertJUnit.assertNull(format.getDeckConformanceProblem(decks.get(1).getDeck()));
    }

    @Test
    public void aRestrictedCardGetsOneCopy() {
        initAndCreateGame();
        final DeckGeneratorMonoColor gen = new DeckGeneratorMonoColor(FModel.getMagicDb().getCommonCards(), DeckFormat.Constructed, "Black");
        gen.setRestrictedCards(List.of("Dark Ritual"));
        gen.cardCounts.put("Dark Ritual", 0);
        gen.cardCounts.put("Duress", 0);
        gen.addSome(5, new ArrayList<>(Arrays.asList(
                FModel.getMagicDb().getCommonCards().getCard("Dark Ritual"),
                FModel.getMagicDb().getCommonCards().getCard("Duress"))));
        AssertJUnit.assertEquals(gen.tDeck.toString(), 1, gen.tDeck.countByName("Dark Ritual"));
        AssertJUnit.assertEquals(gen.tDeck.toString(), 4, gen.tDeck.countByName("Duress"));
    }

    @Test
    public void theFormatColorDecksLeftTheMenu() {
        initAndCreateGame();
        final List<DeckType> menu = List.of(DeckType.ConstructedOptions);
        for (final DeckType removed : List.of(DeckType.STANDARD_COLOR_DECK, DeckType.MODERN_COLOR_DECK, DeckType.PAUPER_COLOR_DECK)) {
            AssertJUnit.assertFalse(removed.name(), menu.contains(removed));
            AssertJUnit.assertEquals(DeckType.COLOR_DECK, removed.forDeckChooser());
        }
        AssertJUnit.assertEquals(DeckType.THEME_DECK, DeckType.THEME_DECK.forDeckChooser());
    }
}
