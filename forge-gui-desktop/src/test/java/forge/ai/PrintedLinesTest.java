package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Lines of text as printed on each printing (PrintingTraits, read off Scryfall's card images by _tools/rules-lines),
 * which replaced the old Oracle-text estimate for Lexivore and Frazzled Editor. Every count below was checked by eye
 * against the card image.
 */
public class PrintedLinesTest extends AITest {

    private Card printing(String name, String set, String cn, Player p) {
        PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set, cn);
        AssertJUnit.assertNotNull(name + " " + set + " " + cn, pc);
        Card c = Card.fromPaperCard(pc, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(ZoneType.Battlefield).add(c);
        return c;
    }

    @Test
    public void testCountsFollowThePrinting() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        // seven lines of rules text, no flavor
        Card tolsimir = printing("Tolsimir Wolfblood", "RAV", "236", me);
        // three lines of rules text and three of flavor
        Card servant = printing("Air Servant", "W16", "4", me);
        // a Saga's chapters aren't read
        Card benalia = printing("History of Benalia", "DOM", "21", me);

        AssertJUnit.assertEquals(7, tolsimir.getPrintedTextLines());
        AssertJUnit.assertTrue(tolsimir.isWordy());
        AssertJUnit.assertEquals(6, servant.getPrintedTextLines());
        AssertJUnit.assertFalse(servant.isWordy());
        AssertJUnit.assertEquals(-1, benalia.getPrintedTextLines());
        AssertJUnit.assertFalse(benalia.isWordy());
    }

    @Test
    public void testLexivoreComparesPrintedLines() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card lexivore = addCard("Lexivore", me);
        Card tolsimir = printing("Tolsimir Wolfblood", "RAV", "236", opp);
        Card servant = printing("Air Servant", "W16", "4", opp);
        Card benalia = printing("History of Benalia", "DOM", "21", opp);

        AssertJUnit.assertTrue(tolsimir.isValid("Permanent.Other+mostLinesOfText", me, lexivore, null));
        AssertJUnit.assertFalse(servant.isValid("Permanent.Other+mostLinesOfText", me, lexivore, null));
        // unread, so the players decide whether it has the most
        AssertJUnit.assertTrue(benalia.isValid("Permanent.Other+mostLinesOfText", me, lexivore, null));
        // the AI goes by known counts only
        CardCollection pool = new CardCollection(game.getCardsIn(ZoneType.Battlefield));
        pool.remove(lexivore);
        AssertJUnit.assertEquals(new CardCollection(tolsimir), CardLists.getCardsWithMostTextBoxLines(pool));
    }

    @Test
    public void testFrazzledEditorIsProtectedFromWordy() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card editor = addCard("Frazzled Editor", me);
        Card tolsimir = printing("Tolsimir Wolfblood", "RAV", "236", opp);
        Card servant = printing("Air Servant", "W16", "4", opp);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse(CombatUtil.canBlock(editor, tolsimir));
        AssertJUnit.assertTrue(CombatUtil.canBlock(editor, servant));
    }
}
