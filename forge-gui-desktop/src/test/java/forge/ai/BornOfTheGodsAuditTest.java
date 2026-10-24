package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;

/** Born of the Gods fixes (2026-10-23). */
public class BornOfTheGodsAuditTest extends AITest {

    @Test
    public void testKrakenOfTheStraitsLetsEqualPowerBlock() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card kraken = addCard("Kraken of the Straits", p);
        addCards("Island", 3, p);
        Card two = addCard("Grizzly Bears", opp);
        Card three = addCard("Hill Giant", opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(CombatUtil.canBlock(kraken, two));
        AssertJUnit.assertTrue(CombatUtil.canBlock(kraken, three));
    }
}
