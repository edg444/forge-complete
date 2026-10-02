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
 * Three-Headed Goblin (Unstable): "Triple strike (This creature deals first-strike, regular, and last-strike combat
 * damage.)" - Oracle text, the ruling and the Unstable FAQ entry checked 2026-10-02: it has to survive each step to
 * deal damage in the next; a 3/3 first striker and it take each other out in the first-strike step, and unblocked it
 * deals 9.
 */
public class ThreeHeadedGoblinTest extends AITest {

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
    public void testUnblockedItDealsNine() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card goblin = addCard("Three-Headed Goblin", me);
        AssertJUnit.assertEquals("Triple strike (This creature deals first-strike, regular, and last-strike combat damage.)",
                goblin.getAbilityText().trim());
        AssertJUnit.assertTrue(goblin.hasDoubleStrike() && goblin.hasLastStrike() && !goblin.hasFirstStrike());

        attack(game, me, opp, goblin);
        AssertJUnit.assertEquals(List.of(17, 14, 11), finishCombat(game, opp));
    }

    @Test
    public void testAFirstStrikerTakesItOutFirst() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card goblin = addCard("Three-Headed Goblin", me);
        Card giant = addCard("Hill Giant", opp);
        // "Creatures you control have first strike."
        addCard("Knighthood", opp);
        game.getAction().checkStateEffects(true);

        block(attack(game, me, opp, goblin), goblin, giant);
        finishCombat(game, opp);
        AssertJUnit.assertTrue(game.getCardState(goblin).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(game.getCardState(giant).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testThreeHitsOnABlocker() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card goblin = addCard("Three-Headed Goblin", me);
        // 0/8: 3 + 3 isn't enough, the last strike is
        Card wall = addCard("Wall of Denial", opp);

        block(attack(game, me, opp, goblin), goblin, wall);
        finishCombat(game, opp);
        AssertJUnit.assertTrue(game.getCardState(wall).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(goblin.isInPlay());
        AssertJUnit.assertEquals(20, opp.getLife());
    }

    @Test
    public void testAiCountsThreeHits() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card goblin = addCard("Three-Headed Goblin", ai);
        Card giant = addCard("Hill Giant", ai);
        AssertJUnit.assertEquals(3, goblin.getCombatDamageStrikes());
        AssertJUnit.assertEquals(9, ComputerUtilCombat.damageIfUnblocked(goblin, opp, null, true));
        // a 3/3 blocker dies in the first-strike step before it can hit back
        AssertJUnit.assertTrue(ComputerUtilCombat.canDestroyBlocker(ai, giant, goblin, null, true));
        AssertJUnit.assertFalse(ComputerUtilCombat.canDestroyAttacker(ai, goblin, giant, null, true));
        AssertJUnit.assertTrue(ComputerUtilCard.evaluateCreature(goblin) > ComputerUtilCard.evaluateCreature(giant));
    }
}
