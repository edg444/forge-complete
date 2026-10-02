package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.player.Player;

/**
 * Text-box readers (Pygmy Giant, Tainted Monkey). Stored Oracle text separates
 * paragraphs with the script's literal backslash-n, not a real newline, so these check that the readers see
 * paragraphs rather than one long run with "\n" glued into it.
 */
public class TextBoxTest extends AITest {

    @Test
    public void testOracleTextKeepsLiteralParagraphSeparator() {
        Game game = initAndCreateGame();
        Card shivan = addCard("Shivan Dragon", game.getPlayers().get(1));

        AssertJUnit.assertTrue(shivan.getOracleText().contains("\\n"));
        AssertJUnit.assertFalse(shivan.getOracleText().contains("\n"));
    }

    @Test
    public void testTextBoxContentsSplitsParagraphs() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card disposal = addCard("Ruthless Disposal", me);

        AssertJUnit.assertFalse(disposal.getTextBoxContents().contains("\\n"));
        AssertJUnit.assertTrue(disposal.getTextBoxContents().contains("creature.\nTwo target"));
        // "Two" opens its paragraph, so glued to the separator it read as "ntwo" and Pygmy Giant missed it
        AssertJUnit.assertTrue(CardFactoryUtil.getTextBoxNumbers(disposal).contains(2));
        AssertJUnit.assertTrue(CardFactoryUtil.getTextBoxNumbers(disposal).contains(13));
    }
}
