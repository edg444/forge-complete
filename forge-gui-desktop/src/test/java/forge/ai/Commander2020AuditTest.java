package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/** Unsanctioned and Commander 2020 review (2026-10-23). */
public class Commander2020AuditTest extends AITest {

    @Test
    public void testSelectiveAdaptationLooksForDoubleStrike() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCardToZone("Selective Adaptation", p, ZoneType.Hand);
        SpellAbility sa = c.getFirstSpellAbility();
        AssertJUnit.assertTrue(sa.getParam("ChangeValid").contains("Card.withDouble Strike"));
    }

    @Test
    public void testSpellpyrePhoenixReturnIsOptional() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Spellpyre Phoenix", p);
        Trigger etb = c.getTriggers().iterator().next();
        AssertJUnit.assertEquals("You", etb.getParam("OptionalDecider"));
    }

    @Test
    public void testPutCounterCostNamesTheCounterInLowercase() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Netherborn Altar", p);
        String text = c.getAbilityText(c.getCurrentState());
        AssertJUnit.assertTrue(text, text.contains("{T}, Put a soul counter on this artifact:"));
    }
}
