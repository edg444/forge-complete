package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.zone.ZoneType;

/** Commander Masters and Wilds of Eldraine set review (2026-10-24). */
public class WildsOfEldraineAuditTest extends AITest {

    private String text(final String name) {
        Game game = initAndCreateGame();
        return addCardToZone(name, game.getPlayers().get(1), ZoneType.Hand).getAbilityText();
    }

    @Test
    public void testReplicateOnAPermanent() {
        String t = text("Hatchery Sliver");
        AssertJUnit.assertTrue(t, t.contains("Replicate {1}{G} (As an additional cost"));
    }

    @Test
    public void testNonManaFlashCostWordsItsOwnText() {
        String t = text("Tegwyll's Scouring");
        AssertJUnit.assertTrue(t, t.contains("as though it had flash by tapping three untapped creatures you control with flying"));
        t = text("Molten Exhale");
        AssertJUnit.assertTrue(t, t.contains("if you behold a Dragon as an additional cost to cast it."));
    }
}
