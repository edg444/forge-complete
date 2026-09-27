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
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * Clocknapper's phase stealing, following its Unstable rulings: during the victim's next turn the stolen phase
 * happens as though it's the thief's turn.
 */
public class StealPhaseTest extends AITest {

    private Game setUp() {
        Game game = initAndCreateGame();
        for (Player p : game.getPlayers()) {
            fillLibrary(p, 10);
        }
        return game;
    }

    /** Steps the game until {@code phase} of {@code whoseTurn}'s actual turn, stolen or not. */
    private static void stepUntil(Game game, PhaseType phase, Player whoseTurn) {
        PhaseHandler ph = game.getPhaseHandler();
        int guard = 0;
        do {
            ph.mainLoopStep();
            AssertJUnit.assertTrue("ran away", ++guard < 2000);
        } while (!game.isGameOver() && !(ph.is(phase) && ph.getActualTurnPlayer().equals(whoseTurn)));
    }

    @Test
    public void testStolenBeginningPhase() {
        Game game = setUp();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card mine = addCard("Grizzly Bears", me);
        Card theirs = addCard("Grizzly Bears", opp);
        mine.setTapped(true);
        theirs.setTapped(true);
        moveToMain2(game, me);

        game.getPhaseHandler().stealPhase(me, opp, "Beginning");

        stepUntil(game, PhaseType.UPKEEP, opp);
        AssertJUnit.assertEquals(me, game.getPhaseHandler().getPlayerTurn());
        AssertJUnit.assertTrue(game.getPhaseHandler().isPhaseStolen());

        stepUntil(game, PhaseType.MAIN1, opp);
        // "You untap permanents you control and your opponent doesn't ... You draw a card ... your opponent doesn't."
        AssertJUnit.assertEquals(opp, game.getPhaseHandler().getPlayerTurn());
        AssertJUnit.assertFalse(game.getPhaseHandler().isPhaseStolen());
        AssertJUnit.assertFalse(mine.isTapped());
        AssertJUnit.assertTrue(theirs.isTapped());
        AssertJUnit.assertEquals(1, me.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(0, opp.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testStolenCombatPhase() {
        Game game = setUp();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", me);
        bears.setSickness(false);
        moveToMain2(game, me);

        game.getPhaseHandler().stealPhase(me, opp, "Combat");

        stepUntil(game, PhaseType.COMBAT_BEGIN, opp);
        AssertJUnit.assertEquals(me, game.getPhaseHandler().getPlayerTurn());

        stepUntil(game, PhaseType.MAIN2, opp);
        // the thief attacks the player it stole the phase from, then the turn goes back to them
        AssertJUnit.assertEquals(18, opp.getLife());
        AssertJUnit.assertEquals(opp, game.getPhaseHandler().getPlayerTurn());
        AssertJUnit.assertFalse(game.getPhaseHandler().isPhaseStolen());
    }

    @Test
    public void testStolenEndingPhaseKeepsTurnOrder() {
        Game game = setUp();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        moveToMain2(game, me);
        for (int i = 0; i < 9; i++) {
            addCardToZone("Runeclaw Bear", opp, ZoneType.Hand);
        }
        game.getPhaseHandler().stealPhase(me, opp, "Ending");

        stepUntil(game, PhaseType.MAIN2, opp);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Runeclaw Bear", me, ZoneType.Hand);
        }
        final int oppHand = opp.getCardsIn(ZoneType.Hand).size();
        final int myTurns = me.getTurn();

        stepUntil(game, PhaseType.UPKEEP, me);
        // the thief's cleanup: it discards to hand size, the victim doesn't, and the next turn is still the thief's
        AssertJUnit.assertEquals(7, me.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(oppHand, opp.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(me, game.getPhaseHandler().getPlayerTurn());
        AssertJUnit.assertEquals(myTurns + 1, me.getTurn());
    }

    @Test
    public void testOnlyTheVictimsNextTurn() {
        Game game = setUp();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        moveToMain2(game, me);
        game.getPhaseHandler().stealPhase(me, opp, "Main1");

        stepUntil(game, PhaseType.MAIN1, opp);
        AssertJUnit.assertEquals(me, game.getPhaseHandler().getPlayerTurn());
        final int stolenTurn = opp.getTurn();
        do {
            stepUntil(game, PhaseType.MAIN1, opp);
        } while (opp.getTurn() == stolenTurn);
        AssertJUnit.assertEquals(opp, game.getPhaseHandler().getPlayerTurn());
    }

    private static SpellAbility clocknapperTrigger(Card c) {
        for (Trigger t : c.getTriggers()) {
            if (t.getMode() == TriggerType.ChangesZone) {
                return t.ensureAbility();
            }
        }
        return null;
    }

    private static void resolveAsAi(Player ai, Card clocknapper) {
        SpellAbility sa = clocknapperTrigger(clocknapper);
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(sa).doTrigger(ai, sa, true));
        AbilityUtils.resolve(sa);
    }

    @Test
    public void testAiStealsBeginningByDefault() {
        Game game = setUp();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        moveToMain2(game, me);
        resolveAsAi(me, addCard("Clocknapper", me));

        stepUntil(game, PhaseType.MAIN1, opp);
        AssertJUnit.assertEquals(1, me.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(0, opp.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testAiStealsCombatForLethal() {
        Game game = setUp();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Grizzly Bears", me).setSickness(false);
        opp.setLife(2, null);
        moveToMain2(game, me);
        resolveAsAi(me, addCard("Clocknapper", me));

        stepUntil(game, PhaseType.MAIN2, opp);
        AssertJUnit.assertTrue(opp.hasLost());
    }
}
