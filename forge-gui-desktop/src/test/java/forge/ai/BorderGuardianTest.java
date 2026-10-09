package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Border Guardian (Unstable, scripted upstream) - Oracle text and its Scryfall ruling checked 2026-10-09. The border it
 * reads is the printed one, except that acorn cards count as silver-bordered (user, 2026-09-25).
 */
public class BorderGuardianTest extends AITest {

    private Card printing(final String name, final String set, final Player p) {
        final PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set);
        AssertJUnit.assertNotNull(name + " (" + set + ")", pc);
        return Card.fromPaperCard(pc, p);
    }

    @Test
    public void bordersAsTheRulesSeeThem() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);

        final Card acorn = printing("Killer Cosplay", "UNF", me);
        AssertJUnit.assertTrue(acorn.isValid("Card.BorderColorSilver", me, null, null));
        AssertJUnit.assertFalse(acorn.isValid("Card.BorderColorBlack", me, null, null));
        AssertJUnit.assertFalse(acorn.isValid("Card.BlackBordered", me, null, null));

        final Card silver = printing("Hot Fix", "UST", me);
        AssertJUnit.assertTrue(silver.isValid("Card.BorderColorSilver", me, null, null));

        final Card black = printing("Lightning Bolt", "M10", me);
        AssertJUnit.assertTrue(black.isValid("Card.BorderColorBlack", me, null, null));
        AssertJUnit.assertFalse(black.isValid("Card.BorderColorSilver", me, null, null));
    }
}
