package forge.deck.generation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.gamemodes.limited.LimitedPoolType;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.item.generation.IUnOpenedProduct;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/** The Exclude The List preference: Full card pool packs and generated decks use no PLST/ULST printings. */
public class ExcludeTheListTest extends AITest {

    private static int listCardsIn(IUnOpenedProduct product, int packs) {
        int n = 0;
        for (int i = 0; i < packs; i++) {
            for (PaperCard pc : product.get()) {
                if (!PaperCardPredicates.NOT_ON_THE_LIST.test(pc)) {
                    n++;
                }
            }
        }
        return n;
    }

    @Test
    public void testFullCardpoolPacks() {
        initAndCreateGame();
        final boolean was = FModel.getPreferences().getPrefBoolean(FPref.EXCLUDE_THE_LIST);
        try {
            FModel.getPreferences().setPref(FPref.EXCLUDE_THE_LIST, false);
            AssertJUnit.assertTrue("List printings show up in Full card pool packs by default",
                    listCardsIn(LimitedPoolType.fullCardpoolBooster(), 60) > 0);
            FModel.getPreferences().setPref(FPref.EXCLUDE_THE_LIST, true);
            AssertJUnit.assertEquals(0, listCardsIn(LimitedPoolType.fullCardpoolBooster(), 60));
        } finally {
            FModel.getPreferences().setPref(FPref.EXCLUDE_THE_LIST, was);
        }
    }

    @Test
    public void testGeneratedDeckGetsAnotherPrinting() {
        initAndCreateGame();
        final PaperCard listBolt = FModel.getMagicDb().getCommonCards().getCard("Lightning Bolt", "PLST");
        AssertJUnit.assertNotNull("Lightning Bolt is on The List", listBolt);
        final boolean was = FModel.getPreferences().getPrefBoolean(FPref.EXCLUDE_THE_LIST);
        try {
            Deck deck = new Deck("test");
            deck.getMain().add(listBolt, 4);
            FModel.getPreferences().setPref(FPref.EXCLUDE_THE_LIST, false);
            DeckgenUtil.dropTheListPrintings(deck);
            AssertJUnit.assertEquals(4, deck.getMain().count(listBolt));

            FModel.getPreferences().setPref(FPref.EXCLUDE_THE_LIST, true);
            DeckgenUtil.dropTheListPrintings(deck);
            AssertJUnit.assertEquals(4, deck.getMain().countAll());
            for (PaperCard pc : deck.getMain().toFlatList()) {
                AssertJUnit.assertEquals("Lightning Bolt", pc.getName());
                AssertJUnit.assertTrue(pc.getEdition(), PaperCardPredicates.NOT_ON_THE_LIST.test(pc));
            }
        } finally {
            FModel.getPreferences().setPref(FPref.EXCLUDE_THE_LIST, was);
        }
    }
}
