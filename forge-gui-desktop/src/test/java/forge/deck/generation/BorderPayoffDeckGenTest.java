package forge.deck.generation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.ai.AITest;
import forge.card.CardEdition;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.item.PaperCard;
import forge.model.FModel;

/** The Whiteboarder ended up in a random deck with no white-bordered cards to pump. */
public class BorderPayoffDeckGenTest extends AITest {

    private static class Gen extends DeckGenerator2Color {
        Gen() {
            super(FModel.getMagicDb().getCommonCards(), DeckFormat.Constructed, "blue", "green");
        }

        PaperCard printing(String name, CardEdition.BorderColor border) {
            for (PaperCard pc : fullCardDB.getAllCards(pc -> pc.getName().equals(name) && border(pc) == border)) {
                return pc;
            }
            return null;
        }

        PaperCard seed(String name, int count) {
            PaperCard pc = printing(name, CardEdition.BorderColor.BLACK);
            tDeck.add(pc, count);
            cardCounts.put(name, count);
            return pc;
        }
    }

    private static CardEdition.BorderColor border(PaperCard pc) {
        return StaticData.instance().getEditions().get(pc.getEdition()).getBorderColor(pc.getCollectorNumber());
    }

    @Test
    public void testWhiteboarderGetsWhiteBorderedCreatures() {
        initAndCreateGame();
        Gen gen = new Gen();
        gen.seed("The Whiteboarder", 2);
        gen.seed("Grizzly Bears", 4);
        PaperCard goyf = gen.seed("Tarmogoyf", 4);
        gen.seed("Rampant Growth", 4);
        gen.seed("Ponder", 4);
        PaperCard counterspell = gen.seed("Counterspell", 4);
        AssertJUnit.assertNull(gen.printing("Tarmogoyf", CardEdition.BorderColor.WHITE));
        AssertJUnit.assertNotNull(gen.printing("Counterspell", CardEdition.BorderColor.WHITE));
        gen.reinforceBorderPayoffs(60, true);

        int white = 0;
        int whiteboarders = 0;
        for (PaperCard pc : gen.tDeck.toFlatList()) {
            if (pc.getName().equals("Grizzly Bears")) {
                AssertJUnit.assertEquals("Grizzly Bears moved to a white-bordered printing", CardEdition.BorderColor.WHITE, border(pc));
            }
            if (pc.getName().equals("Tarmogoyf")) {
                AssertJUnit.assertEquals("never printed white-bordered, so left alone", goyf, pc);
            }
            if (pc.getName().equals("Counterspell")) {
                AssertJUnit.assertEquals("only creatures count for The Whiteboarder", counterspell, pc);
            }
            if (pc.getName().equals("The Whiteboarder")) {
                whiteboarders++;
            } else if (pc.getRules().getType().isCreature() && border(pc) == CardEdition.BorderColor.WHITE) {
                white++;
            }
        }
        AssertJUnit.assertEquals(2, whiteboarders);
        AssertJUnit.assertTrue("at least six white-bordered creatures, got " + white, white >= 6);
        AssertJUnit.assertEquals(22, gen.tDeck.countAll());
    }

    @Test
    public void testCardArtPassKeepsTheReprints() {
        initAndCreateGame();
        Gen gen = new Gen();
        gen.seed("The Whiteboarder", 2);
        gen.seed("Grizzly Bears", 4);
        gen.seed("Tarmogoyf", 4);
        gen.seed("Ponder", 4);
        gen.reinforceBorderPayoffs(60, true);
        AssertJUnit.assertTrue(gen.getPinnedPrintings().contains("Grizzly Bears"));

        Deck deck = new Deck("test");
        deck.getMain().addAll(gen.tDeck);
        boolean smart = StaticData.instance().isEnabledCardArtSmartSelection();
        StaticData.instance().setEnableSmartCardArtSelection(true);
        try {
            deck.optimiseCardArt(null, gen.getPinnedPrintings());
        } finally {
            StaticData.instance().setEnableSmartCardArtSelection(smart);
        }
        int white = 0;
        for (PaperCard pc : deck.getMain().toFlatList()) {
            if (gen.getPinnedPrintings().contains(pc.getName())) {
                AssertJUnit.assertEquals(pc.getName(), CardEdition.BorderColor.WHITE, border(pc));
                white++;
            }
        }
        AssertJUnit.assertTrue(white >= 6);
    }

    @Test
    public void testNoPayoffLeavesPrintingsAlone() {
        initAndCreateGame();
        Gen gen = new Gen();
        PaperCard bears = gen.seed("Grizzly Bears", 4);
        gen.reinforceBorderPayoffs(60, true);
        AssertJUnit.assertEquals(bears, gen.tDeck.toFlatList().get(0));
    }
}
