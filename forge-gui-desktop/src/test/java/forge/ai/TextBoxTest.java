package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.player.Player;

/**
 * Text-box readers (Lexivore, Frazzled Editor, Pygmy Giant, Tainted Monkey). Stored Oracle text separates
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
    public void testLineCountMeasuresEachParagraph() {
        Game game = initAndCreateGame();
        Card shivan = addCard("Shivan Dragon", game.getPlayers().get(1));

        // 81 characters wrap to 3 lines and 48 to 2; measured as one 131-character run it would be 4
        AssertJUnit.assertEquals("Flying (This creature can't be blocked except by creatures with flying or reach.)"
                + "\\n{R}: Shivan Dragon gets +1/+0 until end of turn.", shivan.getOracleText());
        AssertJUnit.assertEquals(5, CardFactoryUtil.getTextBoxLineCount(shivan));
    }

    @Test
    public void testLineCountGivesShortParagraphsALineEach() {
        Game game = initAndCreateGame();
        Card bears = addCard("Grizzly Bears", game.getPlayers().get(1));
        bears.setOracleText("Flying\\nVigilance\\nTrample\\nHaste");

        AssertJUnit.assertEquals(4, CardFactoryUtil.getTextBoxLineCount(bears));
    }

    @Test
    public void testLineCountAcceptsRealNewlines() {
        Game game = initAndCreateGame();
        Card bears = addCard("Grizzly Bears", game.getPlayers().get(1));
        bears.setOracleText("Flying\r\nVigilance\nTrample");

        AssertJUnit.assertEquals(3, CardFactoryUtil.getTextBoxLineCount(bears));
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
