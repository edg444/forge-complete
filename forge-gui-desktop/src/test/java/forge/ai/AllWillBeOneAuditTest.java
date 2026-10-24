package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/** Jumpstart 2022 and Phyrexia: All Will Be One set review (2026-10-24). */
public class AllWillBeOneAuditTest extends AITest {

    private Card card(final String name) {
        Game game = initAndCreateGame();
        return addCardToZone(name, game.getPlayers().get(1), ZoneType.Hand);
    }

    @Test
    public void testNornsChoirmasterWatchesCommandersYouControl() {
        for (Trigger t : card("Norn's Choirmaster").getTriggers()) {
            AssertJUnit.assertEquals("Card.IsCommander+YouCtrl", t.getParam("ValidCard"));
        }
    }

    @Test
    public void testTexts() {
        String t = card("Oil-Gorger Troll").getAbilityText();
        AssertJUnit.assertTrue(t, t.contains("Then if you control a permanent with an oil counter on it, draw a card."));
        t = card("Synthesis Pod").getAbilityText();
        AssertJUnit.assertTrue(t, t.contains("{T}, Exile a spell you control: Target opponent"));
        t = card("Gleeful Demolition").getAbilityText();
        AssertJUnit.assertFalse(t, t.contains("creature three"));
    }
}
