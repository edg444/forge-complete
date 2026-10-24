package forge.deck.generation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.deck.DeckFormat;
import forge.item.PaperCardPredicates;
import forge.model.FModel;

/**
 * Random color decks, 2026-10-24: a mono-black or mono-green deck sometimes came out with no lands at all (the AI got
 * a deck of colorless artifacts and Orb of Origin). The basics came from the first card's edition that had a Plains,
 * which might have no Swamp or Forest; singleton decks also stopped short of 60 cards.
 */
public class ColorDeckLandsTest extends AITest {

    private static DeckGeneratorMonoColor black() {
        DeckGeneratorMonoColor gen = new DeckGeneratorMonoColor(FModel.getMagicDb().getCommonCards(), DeckFormat.Constructed, "Black");
        gen.tDeck.add(FModel.getMagicDb().getCommonCards().getCard("Dark Ritual"));
        gen.cardCounts.put("Dark Ritual", 1);
        return gen;
    }

    @Test
    public void anEditionWithoutASwampDoesNotSupplyTheBasics() {
        initAndCreateGame();
        DeckGeneratorMonoColor gen = black();
        // Commander Legends prints a Plains but no Swamp
        AssertJUnit.assertFalse(FModel.getMagicDb().getCommonCards().getAllCards(
                PaperCardPredicates.printedInSet("CMR").and(PaperCardPredicates.name("Plains"))).isEmpty());
        AssertJUnit.assertTrue(FModel.getMagicDb().getCommonCards().getAllCards(
                PaperCardPredicates.printedInSet("CMR").and(PaperCardPredicates.name("Swamp"))).isEmpty());
        if (gen.setBasicLandPool("CMR")) {
            gen.basicLandEdition = "CMR";
        }
        gen.addBasicLand(24);
        AssertJUnit.assertEquals(gen.tDeck.toString(), 24, gen.tDeck.countAll(PaperCardPredicates.IS_LAND));
    }

    @Test
    public void aShortSingletonDeckIsFilledWithBasics() {
        initAndCreateGame();
        DeckGeneratorMonoColor gen = black();
        gen.setSingleton(true);
        gen.adjustDeckSize(60);
        AssertJUnit.assertEquals(gen.tDeck.toString(), 60, gen.tDeck.countAll());
        AssertJUnit.assertEquals(59, gen.tDeck.countAll(PaperCardPredicates.IS_LAND));
    }
}
