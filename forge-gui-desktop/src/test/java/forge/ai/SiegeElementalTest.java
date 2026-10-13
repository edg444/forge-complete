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
 * Siege Elemental (CMB1 #64), checked against Scryfall and its 2 rulings 2026-10-13: only tapped creatures can block,
 * and being tapped is the only thing it overlooks (a tapped creature still can't block a flier without reach).
 */
public class SiegeElementalTest extends AITest {

    @Test
    public void onlyTappedCreaturesBlock() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Siege Elemental", opp);
        Card bears = addCard("Grizzly Bears", ai);
        bears.setSickness(false);
        Card flier = addCard("Serra Angel", ai);
        flier.setSickness(false);
        Card untapped = addCard("Hill Giant", opp);
        Card tapped = addCard("Hill Giant", opp);
        tapped.setTapped(true);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai);

        Combat combat = new Combat(ai);
        game.getPhaseHandler().setCombat(combat);
        combat.addAttacker(bears, opp);
        combat.addAttacker(flier, opp);
        AssertJUnit.assertFalse(CombatUtil.canBlock(bears, untapped, combat));
        AssertJUnit.assertTrue(CombatUtil.canBlock(bears, tapped, combat));
        AssertJUnit.assertFalse("only tapped status is ignored", CombatUtil.canBlock(flier, tapped, combat));
    }
}
