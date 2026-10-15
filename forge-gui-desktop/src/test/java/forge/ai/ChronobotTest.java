package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Chronobot (CMB1 #105), checked against Scryfall and its 3 rulings 2026-10-15: you and the target opponent take
 * each other's upkeep steps while it's on the battlefield; the upkeep's player is active, but it's still the other
 * player's turn, and untap and draw steps don't move.
 */
public class ChronobotTest extends AITest {

    private static void stepUntil(Game game, PhaseType phase, Player whoseTurn) {
        PhaseHandler ph = game.getPhaseHandler();
        int guard = 0;
        do {
            ph.mainLoopStep();
            AssertJUnit.assertTrue("ran away", ++guard < 2000);
        } while (!game.isGameOver() && !(ph.is(phase) && ph.getActualTurnPlayer().equals(whoseTurn)));
    }

    @Test
    public void upkeepsSwitchWhileItStays() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player me = game.getPlayers().get(1);
        fillLibrary(opp, 10);
        fillLibrary(me, 10);
        PhaseHandler ph = game.getPhaseHandler();
        Card bot = addCard("Chronobot", me);
        addCard("Phyrexian Arena", me);
        moveToMain2(game, me);

        SpellAbility etb = bot.getTriggers().iterator().next().ensureAbility();
        etb.setActivatingPlayer(me);
        etb.getTargets().add(opp);
        AbilityUtils.resolve(etb);

        stepUntil(game, PhaseType.UPKEEP, opp);
        AssertJUnit.assertEquals("my upkeep, on their turn", me, ph.getPlayerTurn());
        AssertJUnit.assertTrue("still their turn", ph.isPlayerTurn(opp));
        AssertJUnit.assertFalse(ph.isPlayerTurn(me));
        final int oppHand = opp.getCardsIn(ZoneType.Hand).size();
        stepUntil(game, PhaseType.MAIN1, opp);
        AssertJUnit.assertEquals(opp, ph.getPlayerTurn());
        AssertJUnit.assertEquals("Phyrexian Arena's upkeep trigger", 19, me.getLife());
        AssertJUnit.assertEquals("their draw step is theirs", oppHand + 1, opp.getCardsIn(ZoneType.Hand).size());

        stepUntil(game, PhaseType.UPKEEP, me);
        AssertJUnit.assertEquals("their upkeep, on my turn", opp, ph.getPlayerTurn());
        stepUntil(game, PhaseType.MAIN1, me);
        AssertJUnit.assertEquals(me, ph.getPlayerTurn());
        AssertJUnit.assertEquals("no upkeep of mine this turn", 19, me.getLife());

        game.getAction().destroy(bot, null, false, forge.game.ability.AbilityKey.newMap());
        stepUntil(game, PhaseType.UPKEEP, opp);
        AssertJUnit.assertEquals("back to normal once it's gone", opp, ph.getPlayerTurn());
        stepUntil(game, PhaseType.UPKEEP, me);
        AssertJUnit.assertEquals(me, ph.getPlayerTurn());
        stepUntil(game, PhaseType.MAIN1, me);
        AssertJUnit.assertEquals(18, me.getLife());
    }
}
