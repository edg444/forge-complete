package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.zone.ZoneType;

/** Transformers and The Brothers' War set review (2026-10-24). */
public class BrothersWarAuditTest extends AITest {

    private String text(final String name) {
        Game game = initAndCreateGame();
        return addCardToZone(name, game.getPlayers().get(1), ZoneType.Hand).getAbilityText();
    }

    @Test
    public void testPluralTapCostKeepsItsYouControl() {
        String t = text("The Archimandrite");
        AssertJUnit.assertTrue(t, t.contains("Tap three untapped Advisors, Artificers, and/or Monks you control: Draw a card."));
    }

    @Test
    public void testCostTexts() {
        String t = text("Arcee, Sharpshooter");
        AssertJUnit.assertTrue(t, t.contains("{1}, Remove one or more +1/+1 counters from Arcee: It deals"));
        t = text("Hajar, Loyal Bodyguard");
        AssertJUnit.assertTrue(t, t.contains("Sacrifice Hajar:"));
    }
}
