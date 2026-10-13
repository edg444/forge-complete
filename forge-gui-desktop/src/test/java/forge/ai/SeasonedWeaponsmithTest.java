package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

/**
 * Seasoned Weaponsmith (CMB1 #63), checked against Scryfall and its 3 rulings 2026-10-13: tasty creatures can be
 * attacked; an attacked one can block only creatures attacking it; an unblocked attacker and the creature it attacks
 * deal combat damage to each other as attacker and blocker, and trample can't go past it to the player.
 */
public class SeasonedWeaponsmithTest extends AITest {

    private static void damage(final Game game, final Combat combat) {
        combat.orderBlockersForDamageAssignment();
        combat.orderAttackersForDamageAssignment();
        combat.assignCombatDamage(false);
        combat.dealAssignedDamage();
        game.getAction().checkStateEffects(true);
    }

    @Test
    public void attackedDirectlyTheyTradeDamage() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card smith = addCard("Seasoned Weaponsmith", opp);
        Card giant = addCard("Hill Giant", ai);
        giant.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai);

        Combat combat = new Combat(ai);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertTrue(combat.getDefenders().contains(smith));
        AssertJUnit.assertTrue(CombatUtil.canAttack(giant, smith));
        combat.addAttacker(giant, smith);
        combat.setBlocked(giant, false);
        damage(game, combat);

        AssertJUnit.assertFalse("3 damage kills the 5/2", smith.isInPlay());
        AssertJUnit.assertFalse("and it deals 5 back", giant.isInPlay());
        AssertJUnit.assertEquals(20, opp.getLife());
    }

    @Test
    public void itBlocksOnlyWhatAttacksIt() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card smith = addCard("Seasoned Weaponsmith", opp);
        Card wall = addCard("Wall of Stone", opp);
        Card giant = addCard("Hill Giant", ai);
        giant.setSickness(false);
        Card bears = addCard("Grizzly Bears", ai);
        bears.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai);

        Combat combat = new Combat(ai);
        game.getPhaseHandler().setCombat(combat);
        combat.addAttacker(giant, smith);
        combat.addAttacker(bears, opp);
        AssertJUnit.assertTrue(CombatUtil.canBlock(giant, smith, combat));
        AssertJUnit.assertFalse(CombatUtil.canBlock(bears, smith, combat));
        AssertJUnit.assertTrue("others can block for it", CombatUtil.canBlock(giant, wall, combat));
    }

    @Test
    public void trampleStopsAtTheCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card smith = addCard("Seasoned Weaponsmith", opp);
        Card dreadmaw = addCard("Colossal Dreadmaw", ai);
        dreadmaw.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai);

        Combat combat = new Combat(ai);
        game.getPhaseHandler().setCombat(combat);
        combat.addAttacker(dreadmaw, smith);
        combat.setBlocked(dreadmaw, false);
        damage(game, combat);

        AssertJUnit.assertFalse(smith.isInPlay());
        AssertJUnit.assertEquals(20, opp.getLife());
        AssertJUnit.assertEquals(5, dreadmaw.getDamage());
    }

    @Test
    public void aiAttacksWithoutConfusingItForAPlaneswalker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Seasoned Weaponsmith", opp);
        addCard("Colossal Dreadmaw", ai).setSickness(false);
        addCard("Hill Giant", ai).setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai);

        Combat combat = new Combat(ai);
        game.getPhaseHandler().setCombat(combat);
        new AiAttackController(ai).declareAttackers(combat);
        for (Card a : combat.getAttackers()) {
            AssertJUnit.assertTrue(CombatUtil.canAttack(a, combat.getDefenderByAttacker(a)));
        }
    }
}
