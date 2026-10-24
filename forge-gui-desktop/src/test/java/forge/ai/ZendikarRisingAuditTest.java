package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;

/** Ikoria through Zendikar Rising review (2026-10-23). */
public class ZendikarRisingAuditTest extends AITest {

    @Test
    public void testWardenOfTheWoodsDrawIsOptional() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Warden of the Woods", p);
        for (Trigger t : c.getTriggers()) {
            if (t.getMode() == TriggerType.BecomesTarget) {
                AssertJUnit.assertEquals("You", t.getParam("OptionalDecider"));
                return;
            }
        }
        AssertJUnit.fail("no becomes-target trigger");
    }

    @Test
    public void testBarrinCantReturnItselfAsAPlaneswalker() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Barrin, Tolarian Archmage", p);
        Trigger etb = c.getTriggers().iterator().next();
        String[] valid = etb.getOverridingAbility().getTargetRestrictions().getValidTgts();
        AssertJUnit.assertEquals("Planeswalker.Other", valid[1]);
    }

    @Test
    public void testKickerCostTextSaysXCantBeZero() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Thieving Skydiver", p);
        String text = c.getAbilityText(c.getCurrentState());
        AssertJUnit.assertTrue(text, text.contains("Kicker {X}. X can't be 0. (You may pay"));
    }

    @Test
    public void testDifferentNamesCostsReadNaturally() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card ormos = addCard("Ormos, Archive Keeper", p);
        String text = ormos.getAbilityText(ormos.getCurrentState());
        AssertJUnit.assertTrue(text, text.contains("Discard three cards with different names:"));
        Card font = addCard("Transmutation Font", p);
        text = font.getAbilityText(font.getCurrentState());
        AssertJUnit.assertTrue(text, text.contains("Sacrifice three artifact tokens with different names:"));
    }
}
