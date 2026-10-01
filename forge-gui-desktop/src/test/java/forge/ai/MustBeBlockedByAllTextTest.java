package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;

/** MustBeBlockedByAll:<valid>:<description> printed the whole keyword, valid string and all, as card text. */
public class MustBeBlockedByAllTextTest extends AITest {

    @Test
    public void testOnlyTheDescriptionIsPrinted() {
        Game game = initAndCreateGame();
        Card priest = addCard("Marble Priest", game.getPlayers().get(1));
        String text = priest.getAbilityText();
        AssertJUnit.assertFalse(text, text.contains("MustBeBlockedByAll"));
        AssertJUnit.assertTrue(text, text.contains("All Walls able to block this creature do so."));
        AssertJUnit.assertTrue(addCard("Talruum Piper", game.getPlayers().get(1)).getAbilityText()
                .contains("All creatures with flying able to block this creature do so."));
    }
}
