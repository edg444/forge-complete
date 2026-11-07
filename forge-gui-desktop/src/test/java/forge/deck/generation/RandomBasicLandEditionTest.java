package forge.deck.generation;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.StaticData;
import forge.ai.AITest;
import forge.card.CardDb;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.MyRandom;

/**
 * Live games, 2026-11-07: with Latest Art and Exclude The List, one of the two random color decks nearly always had
 * its basics from Marvel: Darkhold Destiny. The card art pass put about half of all decks' basics on The List, and
 * dropping List printings then took the newest other printing, MDD (dated 2027). The basics now come from a random
 * edition that prints all five, chosen with the deck's printing filter and kept by the card art pass.
 */
public class RandomBasicLandEditionTest extends AITest {

    @Test
    public void basicsComeFromARandomEdition() {
        check(false);
    }

    @Test
    public void basicsComeFromARandomEditionWithoutTheList() {
        check(true);
    }

    private void check(boolean excludeTheList) {
        initAndCreateGame();
        final StaticData data = StaticData.instance();
        final CardDb.CardArtPreference oldPref = data.getCardArtPreference();
        final boolean oldSmart = data.isEnabledCardArtSmartSelection();
        final boolean oldExclude = FModel.getPreferences().getPrefBoolean(FPref.EXCLUDE_THE_LIST);
        final Random oldRandom = MyRandom.getRandom();
        data.setCardArtPreference(true, true);
        data.setEnableSmartCardArtSelection(true);
        FModel.getPreferences().setPref(FPref.EXCLUDE_THE_LIST, excludeTheList);
        MyRandom.setRandom(new Random(20261107L));
        try {
            final Set<String> editions = new HashSet<>();
            int fromNewest = 0;
            for (int i = 0; i < 12; i++) {
                final Deck deck = DeckgenUtil.buildColorDeck(Lists.newArrayList("red", "green"), null, true);
                final Set<String> deckEditions = new HashSet<>();
                int basics = 0;
                for (Map.Entry<PaperCard, Integer> e : deck.getMain()) {
                    if (e.getKey().getRules().getType().isBasicLand()) {
                        deckEditions.add(e.getKey().getEdition());
                        basics += e.getValue();
                    }
                }
                AssertJUnit.assertTrue(deck.getMain().toString(), basics >= 15);
                AssertJUnit.assertEquals("one edition per deck: " + deckEditions, 1, deckEditions.size());
                final String code = deckEditions.iterator().next();
                AssertJUnit.assertTrue(code + " is not Core/Expansion/Reprint",
                        data.getCardArtPreference().accept(data.getCardEdition(code)));
                if (excludeTheList) {
                    AssertJUnit.assertFalse(PaperCardPredicates.THE_LIST_SETS.contains(code));
                }
                editions.add(code);
                if ("MDD".equals(code)) {
                    fromNewest++;
                }
            }
            AssertJUnit.assertTrue("only " + editions, editions.size() >= 6);
            AssertJUnit.assertTrue("MDD " + fromNewest + " times", fromNewest <= 2);
        } finally {
            data.setCardArtPreference(oldPref.latestFirst, oldPref.filterSets, oldPref.blackBorderOnly);
            data.setEnableSmartCardArtSelection(oldSmart);
            FModel.getPreferences().setPref(FPref.EXCLUDE_THE_LIST, oldExclude);
            MyRandom.setRandom(oldRandom);
        }
    }
}
