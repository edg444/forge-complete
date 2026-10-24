package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.zone.ZoneType;

/** Modern Horizons 3 and Assassin's Creed set review (2026-10-24). */
public class ModernHorizons3AuditTest extends AITest {

    private String text(final String name) {
        Game game = initAndCreateGame();
        return addCardToZone(name, game.getPlayers().get(1), ZoneType.Hand).getAbilityText();
    }

    @Test
    public void testUnearthCosts() {
        String t = text("Salvation Colossus");
        AssertJUnit.assertTrue(t, t.contains("Unearth—Pay eight {E}."));
        t = text("Dregscape Zombie");
        AssertJUnit.assertTrue(t, t.contains("Unearth {B}"));
    }

    @Test
    public void testCharmModeWithoutItsOwnDescription() {
        String t = text("What Must Be Done");
        AssertJUnit.assertTrue(t, t.contains("• Release Juno — Return target historic permanent card"));
        AssertJUnit.assertFalse(t, t.contains("null"));
    }
}
