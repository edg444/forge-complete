package forge.deck.generation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.card.CardDb;
import forge.deck.ArchetypeDeckGenerator;
import forge.deck.CardArchetypeLDAGenerator;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckProxy;
import forge.deck.DeckType;
import forge.deck.DeckgenUtil;
import forge.deck.FormatChoices;
import forge.deck.RandomDeckGenerator;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.gamemodes.match.LocalLobby;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Random decks with a format picked in the lobby, 2026-11-01: they draw only on the format's cards, so the deck passes
 * the check the lobby runs at the start of a game. Vintage restricts cards the generator would take four of, and
 * Canadian Highlander wants 100 singleton cards within a points cap.
 */
public class FormatRandomDeckTest extends AITest {
    private static final String[] FORMATS = {"Standard", "Pauper", "Pioneer", "Modern", "Legacy", "Vintage", "Premodern", "Canadian Highlander"};

    private static List<DeckType> archetypes() {
        return List.of(DeckType.STANDARD_CARDGEN_DECK, DeckType.PIONEER_CARDGEN_DECK,
                DeckType.HISTORIC_CARDGEN_DECK, DeckType.MODERN_CARDGEN_DECK, DeckType.LEGACY_CARDGEN_DECK,
                DeckType.VINTAGE_CARDGEN_DECK, DeckType.PAUPER_CARDGEN_DECK);
    }

    private static void assertLegal(final GameFormat format, final Deck deck) {
        AssertJUnit.assertEquals(format.getName(), Math.max(60, format.getDeckRules().minMainDeckSize()), deck.getMain().countAll());
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
        for (final GameFormat format : List.of(FModel.getFormats().getStandard(), FModel.getFormats().get("Canadian Highlander"))) {
            for (final String color : List.of("White", "Blue", "Black", "Red", "Green")) {
                assertLegal(format, DeckgenUtil.buildFormatColorDeck(List.of(color), format, true));
            }
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

    @Test
    public void archetypeDecksAreLegalInTheirFormat() {
        initAndCreateGame();
        AssertJUnit.assertTrue(CardArchetypeLDAGenerator.initialize());
        for (final DeckType type : archetypes()) {
            final GameFormat format = type.getArchetypeFormat();
            final List<DeckProxy> decks = ArchetypeDeckGenerator.getMatrixDecks(format, true);
            final int step = Math.max(1, decks.size() / 12);
            for (int i = 0; i < decks.size(); i += step) {
                final Deck deck = decks.get(i).getDeck();
                final String problem = format.getDeckConformanceProblem(deck);
                AssertJUnit.assertNull(type + " " + decks.get(i).getName() + ": " + problem, problem);
            }
        }
    }

    /** Sign in Blood is both its own card and the back of Scheming Silvertongue, a rare that isn't legal in Pauper. */
    @Test
    public void aSpellNeverTurnsIntoTheCardWhoseBackCopiesIt() {
        initAndCreateGame();
        final CardDb db = FModel.getMagicDb().getCommonCards();
        Date lastPrint = new Date(0);
        for (final PaperCard print : db.getAllCardsNoAlt("Sign in Blood")) {
            final Date date = FModel.getMagicDb().getEditions().get(print.getEdition()).getDate();
            lastPrint = date.after(lastPrint) ? date : lastPrint;
        }
        AssertJUnit.assertNull(db.getCardFromEditionsReleasedAfter("Sign in Blood", db.getCardArtPreference(), 1, lastPrint, null));
    }

    @Test
    public void anArchetypeListForcesItsFormat() {
        initAndCreateGame();
        final List<Object> choices = FormatChoices.get(true);
        for (final DeckType type : archetypes()) {
            AssertJUnit.assertTrue(type.name(), choices.contains(type.getArchetypeFormat()));
            final LocalLobby lobby = new LocalLobby();
            lobby.applyFormatOf(type);
            AssertJUnit.assertSame(type.getArchetypeFormat(), lobby.getFormat());
            AssertJUnit.assertSame(type.getArchetypeFormat(), lobby.getFormatChoice());
        }
    }

    @Test
    public void otherDeckTypesLeaveTheFormatAlone() {
        initAndCreateGame();
        final LocalLobby lobby = new LocalLobby();
        lobby.setFormatChoice(FModel.getFormats().getStandard());
        lobby.applyFormatOf(DeckType.COLOR_DECK);
        lobby.applyFormatOf(DeckType.THEME_DECK);
        AssertJUnit.assertSame(FModel.getFormats().getStandard(), lobby.getFormat());
    }

    @Test
    public void aFormatGameTypeKeepsItsDecks() {
        initAndCreateGame();
        final LocalLobby lobby = new LocalLobby();
        lobby.setFormatChoice(GameType.MomirBasic);
        lobby.applyFormatOf(DeckType.MODERN_CARDGEN_DECK);
        AssertJUnit.assertSame(GameType.MomirBasic, lobby.getFormatChoice());
    }

    @Test
    public void listedThemesBuildLegalDecks() {
        initAndCreateGame();
        final int all = DeckProxy.getAllThemeDecks().size();
        for (final String name : FORMATS) {
            final GameFormat format = FModel.getFormats().get(name);
            final List<DeckProxy> themes = DeckProxy.getAllThemeDecks(format);
            System.out.println("Themes legal in " + name + ": " + themes.size() + " of " + all);
            for (final DeckProxy theme : themes) {
                for (int i = 0; i < 3; i++) {
                    assertLegal(format, theme.getDeck());
                }
            }
        }
    }

    @Test
    public void everyThemeIsListedWithoutAFormat() {
        initAndCreateGame();
        AssertJUnit.assertEquals(DeckProxy.getAllThemeDecks().size(), DeckProxy.getAllThemeDecks(null).size());
        AssertJUnit.assertFalse(DeckProxy.getAllThemeDecks().isEmpty());
    }

    @Test
    public void anArchetypeListGivesWayToAnotherFormat() {
        initAndCreateGame();
        AssertJUnit.assertTrue(CardArchetypeLDAGenerator.initialize());
        final GameFormat premodern = FModel.getFormats().get("Premodern");
        AssertJUnit.assertEquals(DeckType.COLOR_DECK, DeckType.archetypeListFor(premodern));
    }
}
