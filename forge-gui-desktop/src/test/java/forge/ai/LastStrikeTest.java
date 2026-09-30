package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Extremely Slow Zombie (Unstable): "Last strike (This creature deals combat damage after creatures without last
 * strike.)" - Oracle text checked against Scryfall 2026-09-30. Per the Unstable FAQ it deals its damage only if
 * it survives first-strike and regular damage, first strike + last strike deals damage twice, and double strike
 * + last strike three times.
 */
public class LastStrikeTest extends AITest {

    private Combat attack(Game game, Player attacker, Player defender, Card... attackers) {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, attacker, false);
        Combat combat = new Combat(attacker);
        for (Card c : attackers) {
            combat.addAttacker(c, defender);
            combat.setBlocked(c, false);
        }
        game.getPhaseHandler().setCombat(combat);
        return combat;
    }

    private void block(Combat combat, Card attacker, Card blocker) {
        combat.addBlocker(attacker, blocker);
        combat.setBlocked(attacker, true);
        combat.orderBlockersForDamageAssignment();
        combat.orderAttackersForDamageAssignment();
    }

    /** Plays the combat out, returning the defender's life total each time it changed. */
    private List<Integer> finishCombat(Game game, Player defender) {
        PhaseHandler ph = game.getPhaseHandler();
        List<Integer> lifeTotals = new ArrayList<>();
        int life = defender.getLife();
        int guard = 0;
        while (!game.isGameOver() && !ph.is(PhaseType.MAIN2)) {
            ph.mainLoopStep();
            if (defender.getLife() != life) {
                life = defender.getLife();
                lifeTotals.add(life);
            }
            AssertJUnit.assertTrue("ran away", ++guard < 500);
        }
        return lifeTotals;
    }

    @Test
    public void testDealsDamageInAStepOfItsOwn() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card zombie = addCard("Extremely Slow Zombie", me);
        Card bears = addCard("Grizzly Bears", me);
        AssertJUnit.assertTrue(zombie.hasLastStrike());
        AssertJUnit.assertEquals("Last strike", zombie.getAbilityText().trim());

        attack(game, me, opp, zombie, bears);
        // the Bears' 2 in the regular step, then the Zombie's 3
        AssertJUnit.assertEquals(List.of(18, 15), finishCombat(game, opp));
    }

    @Test
    public void testAloneStillDealsDamage() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        attack(game, me, opp, addCard("Extremely Slow Zombie", me));
        AssertJUnit.assertEquals(List.of(17), finishCombat(game, opp));
    }

    @Test
    public void testDiesBeforeItStrikes() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card zombie = addCard("Extremely Slow Zombie", me);
        Card giant = addCard("Hill Giant", opp);

        block(attack(game, me, opp, zombie), zombie, giant);
        finishCombat(game, opp);
        AssertJUnit.assertTrue(game.getCardState(zombie).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(giant.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(0, giant.getDamage());
    }

    @Test
    public void testStrikesBackIfItSurvives() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card zombie = addCard("Extremely Slow Zombie", opp);
        Card bears = addCard("Grizzly Bears", me);

        // blocking works the same way
        block(attack(game, me, opp, bears), bears, zombie);
        finishCombat(game, opp);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(zombie.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(2, zombie.getDamage());
    }

    @Test
    public void testWithFirstStrikeAndDoubleStrike() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card zombie = addCard("Extremely Slow Zombie", me);
        // "Creatures you control have first strike."
        Card knighthood = addCard("Knighthood", me);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(zombie.hasFirstStrike());

        // first-strike damage and last-strike damage, not regular damage
        attack(game, me, opp, zombie);
        AssertJUnit.assertEquals(List.of(17, 14), finishCombat(game, opp));

        game.getAction().moveToGraveyard(knighthood, null);
        // "Creatures you control have double strike and lifelink."
        addCard("True Conviction", me);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(zombie.hasDoubleStrike());
        AssertJUnit.assertFalse(zombie.hasFirstStrike());

        attack(game, me, opp, zombie);
        AssertJUnit.assertEquals(List.of(11, 8, 5), finishCombat(game, opp));
    }

    @Test
    public void testNoExtraStepWithoutLastStrike() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        attack(game, me, opp, addCard("Grizzly Bears", me));
        AssertJUnit.assertEquals(List.of(18), finishCombat(game, opp));
    }

    @Test
    public void testAiKnowsItStrikesLast() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card zombie = addCard("Extremely Slow Zombie", ai);
        Card giant = addCard("Hill Giant", opp);
        Card bears = addCard("Grizzly Bears", opp);
        Card ownGiant = addCard("Hill Giant", ai);

        // a 3/3 kills the Zombie before it deals damage, whichever of them attacks
        AssertJUnit.assertFalse(ComputerUtilCombat.canDestroyBlocker(ai, giant, zombie, null, true));
        AssertJUnit.assertTrue(ComputerUtilCombat.canDestroyAttacker(ai, zombie, giant, null, true));
        AssertJUnit.assertFalse(ComputerUtilCombat.canDestroyAttacker(ai, giant, zombie, null, true));
        AssertJUnit.assertTrue(ComputerUtilCombat.canDestroyBlocker(ai, zombie, giant, null, true));
        // two ordinary 3/3s trade
        AssertJUnit.assertTrue(ComputerUtilCombat.canDestroyBlocker(ai, giant, ownGiant, null, true));
        // a 2/2 doesn't kill it, so it strikes back
        AssertJUnit.assertTrue(ComputerUtilCombat.canDestroyBlocker(ai, bears, zombie, null, true));
        AssertJUnit.assertTrue(ComputerUtilCombat.canDestroyAttacker(ai, bears, zombie, null, true));
        // and it's worth less than a 3/3 with no drawback, mana cost aside
        AssertJUnit.assertTrue(ComputerUtilCard.evaluateCreature(zombie, true, false) < ComputerUtilCard.evaluateCreature(ownGiant, true, false));
    }
}
