package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatDamageOnStack;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Stack of Paperwork (Mystery Booster playtest card), Oracle text and rulings checked against Scryfall 2026-10-10:
 * assigned combat damage goes on the stack as one object, dealt as it resolves; a source that left deals damage as it
 * last existed; damage to a creature that left or stopped being a creature isn't dealt; it resolves even if Paperwork
 * left.
 */
public class StackOfPaperworkTest extends AITest {

    private Combat combat(Game game, Player attacker) {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DAMAGE, attacker);
        Combat combat = new Combat(attacker);
        game.getPhaseHandler().setCombat(combat);
        return combat;
    }

    private void damageStep(Game game, Combat combat) {
        combat.orderBlockersForDamageAssignment();
        combat.orderAttackersForDamageAssignment();
        AssertJUnit.assertTrue(combat.assignCombatDamage(false));
        Card paperwork = CombatDamageOnStack.usesStackHost(game);
        if (paperwork != null) {
            combat.putAssignedDamageOnStack(paperwork);
        } else {
            combat.dealAssignedDamage();
        }
    }

    @Test
    public void testDamageWaitsOnTheStack() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card paperwork = addCard("Stack of Paperwork", me);
        Card bears = addCard("Grizzly Bears", me);
        game.getAction().checkStateEffects(true);
        Combat combat = combat(game, me);
        combat.addAttacker(bears, opp);
        combat.setBlocked(bears, false);
        damageStep(game, combat);

        AssertJUnit.assertEquals(20, opp.getLife());
        AssertJUnit.assertEquals(1, game.getStack().size());
        AssertJUnit.assertTrue(game.getStack().peekAbility() instanceof CombatDamageOnStack);
        // it resolves even with Paperwork gone
        game.getAction().moveToGraveyard(paperwork, null, null);
        game.getStack().resolveStack();
        AssertJUnit.assertEquals(18, opp.getLife());
        AssertJUnit.assertTrue(game.getStack().isEmpty());
    }

    @Test
    public void testBlockerLeavesButStillDealsDamage() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Stack of Paperwork", me);
        Card bears = addCard("Grizzly Bears", me);
        Card giant = addCard("Hill Giant", opp);
        game.getAction().checkStateEffects(true);
        Combat combat = combat(game, me);
        combat.addAttacker(bears, opp);
        combat.addBlocker(bears, giant);
        combat.setBlocked(bears, true);
        damageStep(game, combat);
        AssertJUnit.assertEquals(0, giant.getDamage());

        // in response, the blocker goes away - the old trick
        Card gone = game.getAction().moveToHand(giant, null);
        game.getStack().resolveStack();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("the giant's damage is dealt as it last existed", game.getCardState(bears).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals(0, game.getCardState(gone).getDamage());
    }

    @Test
    public void testThroughTheRealDamageStep() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Stack of Paperwork", me);
        Card bears = addCard("Grizzly Bears", me);
        game.getAction().checkStateEffects(true);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, me, false);
        Combat combat = new Combat(me);
        combat.addAttacker(bears, opp);
        combat.setBlocked(bears, false);
        game.getPhaseHandler().setCombat(combat);

        boolean sawStack = false;
        int guard = 0;
        forge.game.phase.PhaseHandler ph = game.getPhaseHandler();
        while (!game.isGameOver() && !ph.is(PhaseType.MAIN2)) {
            ph.mainLoopStep();
            if (!game.getStack().isEmpty() && game.getStack().peekAbility() instanceof CombatDamageOnStack) {
                sawStack = true;
                AssertJUnit.assertEquals(20, opp.getLife());
            }
            AssertJUnit.assertTrue("ran away", ++guard < 500);
        }
        AssertJUnit.assertTrue(sawStack);
        AssertJUnit.assertEquals(18, opp.getLife());
    }

    @Test
    public void testWithoutPaperworkDamageIsImmediate() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", me);
        Combat combat = combat(game, me);
        combat.addAttacker(bears, opp);
        combat.setBlocked(bears, false);
        damageStep(game, combat);
        AssertJUnit.assertEquals(18, opp.getLife());
        AssertJUnit.assertTrue(game.getStack().isEmpty());
    }
}
