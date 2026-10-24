package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.zone.ZoneType;

/** Final Fantasy set review (2026-10-24). */
public class FinalFantasyAuditTest extends AITest {

    private String text(final String name) {
        Game game = initAndCreateGame();
        return addCardToZone(name, game.getPlayers().get(1), ZoneType.Hand).getAbilityText();
    }

    @Test
    public void testNonManaBlitzCost() {
        String t = text("Sabin, Master Monk");
        AssertJUnit.assertTrue(t, t.contains("Blitz—{2}{R}{R}, Discard a card."));
    }

    @Test
    public void testTiersStartOnTheirOwnLines() {
        String t = text("Vincent's Limit Break");
        AssertJUnit.assertTrue(t, t.contains("base power and toughness.\r\n• Galian Beast"));
    }
}
