package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;

/** Ravnica Allegiance and War of the Spark review (2026-10-23). */
public class WarOfTheSparkAuditTest extends AITest {

    @Test
    public void testOrzhovLocketTapsToDraw() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Orzhov Locket", p);
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Draw) {
                AssertJUnit.assertTrue(sa.getPayCosts().hasTapCost());
                return;
            }
        }
        AssertJUnit.fail("no draw ability");
    }

    @Test
    public void testTolsimirFightsACreatureYouDontControl() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Tolsimir, Friend to Wolves", p);
        for (Trigger t : c.getTriggers()) {
            SpellAbility fight = t.ensureAbility().getSubAbility();
            if (fight != null && fight.getApi() == ApiType.Fight) {
                AssertJUnit.assertEquals("Creature.YouDontCtrl", fight.getTargetRestrictions().getValidTgts()[0]);
                return;
            }
        }
        AssertJUnit.fail("no fight trigger");
    }
}
