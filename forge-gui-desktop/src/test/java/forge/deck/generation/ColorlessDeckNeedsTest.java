package forge.deck.generation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.deck.DeckFormat;
import forge.item.PaperCard;
import forge.model.FModel;

/** Random color decks took any colorless card - Kefnet's Monument (DeckNeeds:Color$Blue) ended up in red/black. */
public class ColorlessDeckNeedsTest extends AITest {

    private static boolean inPool(String clr1, String clr2, String name) {
        DeckGenerator2Color gen = new DeckGenerator2Color(FModel.getMagicDb().getCommonCards(), DeckFormat.Constructed, clr1, clr2);
        for (PaperCard pc : gen.selectCardsOfMatchingColorForPlayer(true)) {
            if (pc.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testColorlessCardNeedsOneOfItsColors() {
        initAndCreateGame();
        AssertJUnit.assertFalse(inPool("red", "black", "Kefnet's Monument"));
        AssertJUnit.assertTrue(inPool("blue", "red", "Kefnet's Monument"));
        AssertJUnit.assertTrue(inPool("red", "black", "Mind Stone"));
    }
}
