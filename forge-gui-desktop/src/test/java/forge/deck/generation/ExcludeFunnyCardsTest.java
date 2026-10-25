package forge.deck.generation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.ai.AITest;
import forge.deck.DeckFormat;
import forge.deck.DeckgenUtil;
import forge.gamemodes.limited.LimitedPoolType;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.item.generation.IUnOpenedProduct;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/** The Exclude Funny Cards preference: Full card pool packs and generated decks use no Un-cards. */
public class ExcludeFunnyCardsTest extends AITest {

    private static int funnyCardsIn(IUnOpenedProduct product, int packs) {
        int n = 0;
        for (int i = 0; i < packs; i++) {
            for (PaperCard pc : product.get()) {
                if (!PaperCardPredicates.NOT_FUNNY.test(pc)) {
                    n++;
                }
            }
        }
        return n;
    }

    @Test
    public void testWhichCardsAreFunny() {
        initAndCreateGame();
        final StaticData data = StaticData.instance();
        AssertJUnit.assertTrue("Unglued", data.isFunnyCard("Mox Lotus"));
        AssertJUnit.assertTrue("Unfinity, acorn", data.isFunnyCard("Standard Procedure"));
        AssertJUnit.assertTrue("Heroes of the Realm", data.isFunnyCard("Kharis & The Beholder"));
        AssertJUnit.assertTrue("Ponies: The Galloping", data.isFunnyCard("Rarity"));
        AssertJUnit.assertFalse("Unfinity, eternal", data.isFunnyCard("Aerialephant"));
        AssertJUnit.assertFalse("Unstable, eternal", data.isFunnyCard("Steamflogger Boss"));
        AssertJUnit.assertFalse("printed in Unglued too", data.isFunnyCard("Plains"));
        AssertJUnit.assertFalse("reprinted in Mystery Booster 2", data.isFunnyCard("Counterspell"));
    }

    @Test
    public void testFullCardpoolPacks() {
        initAndCreateGame();
        final boolean was = FModel.getPreferences().getPrefBoolean(FPref.EXCLUDE_FUNNY_CARDS);
        // no check that they show up with it off: packs draw from StaticData.instance(), which the game simulation
        // tests replace with a database that leaves Un-cards out
        try {
            FModel.getPreferences().setPref(FPref.EXCLUDE_FUNNY_CARDS, true);
            AssertJUnit.assertEquals(0, funnyCardsIn(LimitedPoolType.fullCardpoolBooster(), 60));
        } finally {
            FModel.getPreferences().setPref(FPref.EXCLUDE_FUNNY_CARDS, was);
        }
    }

    @Test
    public void testGeneratorPool() {
        initAndCreateGame();
        final boolean was = FModel.getPreferences().getPrefBoolean(FPref.EXCLUDE_FUNNY_CARDS);
        try {
            FModel.getPreferences().setPref(FPref.EXCLUDE_FUNNY_CARDS, false);
            DeckGeneratorBase gen = new DeckGenerator5Color(FModel.getMagicDb().getCommonCards(), DeckFormat.Constructed);
            DeckgenUtil.applyPrintingPreferences(gen);
            AssertJUnit.assertTrue(gen.pool.contains("Mox Lotus"));

            FModel.getPreferences().setPref(FPref.EXCLUDE_FUNNY_CARDS, true);
            gen = new DeckGenerator5Color(FModel.getMagicDb().getCommonCards(), DeckFormat.Constructed);
            DeckgenUtil.applyPrintingPreferences(gen);
            AssertJUnit.assertFalse(gen.pool.contains("Mox Lotus"));
            AssertJUnit.assertTrue(gen.pool.contains("Aerialephant"));
            for (PaperCard pc : gen.getDeck(60, true).toFlatList()) {
                AssertJUnit.assertTrue(pc.getName(), PaperCardPredicates.NOT_FUNNY.test(pc));
            }
        } finally {
            FModel.getPreferences().setPref(FPref.EXCLUDE_FUNNY_CARDS, was);
        }
    }
}
