package forge.ai;

import java.util.HashSet;
import java.util.Set;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityRandomTargets;
import forge.game.zone.ZoneType;

/**
 * High Troller (CMB1 #54), checked against Scryfall and its 7 rulings 2026-10-13: targeted spells and abilities cost
 * {2} less (no matter how many targets) and their targets are chosen at random from among legal ones, the number of
 * targets staying the controller's choice.
 */
public class HighTrollerTest extends AITest {

    private static int manaToPay(SpellAbility sa) {
        ManaCostBeingPaid cost = new ManaCostBeingPaid(sa.getPayCosts().getCostMana().getManaCostFor(sa));
        forge.game.cost.CostAdjustment.adjust(cost, sa, sa.getActivatingPlayer(), null, true, false);
        return cost.getConvertedManaCost();
    }

    @Test
    public void targetedSpellsCostTwoLess() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("High Troller", opp); // anyone's Troller affects everyone
        Card bears = addCard("Grizzly Bears", opp);
        Card warp = addCardToZone("Chaos Warp", ai, ZoneType.Hand);
        Card pyroclasm = addCardToZone("Pyroclasm", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility warpSa = warp.getFirstSpellAbility();
        warpSa.setActivatingPlayer(ai);
        warpSa.getTargets().add(bears);
        AssertJUnit.assertEquals("{2}{R} becomes {R}", 1, manaToPay(warpSa));

        SpellAbility pyroSa = pyroclasm.getFirstSpellAbility();
        pyroSa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("untargeted: no reduction", 2, manaToPay(pyroSa));
    }

    @Test
    public void targetsAreRepickedAtRandomKeepingTheCount() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("High Troller", ai);
        addCard("Grizzly Bears", opp);
        Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = bolt.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(StaticAbilityRandomTargets.applies(sa));
        Set<GameObject> seen = new HashSet<>();
        for (int i = 0; i < 60; i++) {
            sa.resetTargets();
            sa.getTargets().add(opp);
            StaticAbilityRandomTargets.randomizeTargets(sa);
            AssertJUnit.assertEquals(1, sa.getTargets().size());
            seen.addAll(sa.getTargets());
        }
        // both players, the Bears, and the Troller itself are all legal
        AssertJUnit.assertEquals(4, seen.size());
    }
}
