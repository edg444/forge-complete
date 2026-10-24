package forge.ai;

import org.apache.commons.lang3.StringUtils;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.zone.ZoneType;

/** Unknown Event set review (2026-10-24). */
public class UnknownEventReviewTest extends AITest {

    private String text(final String name) {
        Game game = initAndCreateGame();
        return addCardToZone(name, game.getPlayers().get(1), ZoneType.Hand).getAbilityText();
    }

    @Test
    public void testCharmsWithTheirOwnHeaderListTheirModes() {
        String t = text("Mindslaver Toolkit");
        AssertJUnit.assertTrue(t, t.contains("Choose any number —"));
        AssertJUnit.assertTrue(t, t.contains("• You sacrifice X permanents."));
        t = text("Blustering Barnyard");
        AssertJUnit.assertTrue(t, t.contains("{P}{P}{P} — Create a 3/3 green Ox token with trample."));
        t = text("Bin Chicken");
        AssertJUnit.assertTrue(t, t.contains("• Amass Birds X"));
    }

    @Test
    public void testCommanderSuspendShowsOnce() {
        String t = text("Groaaaaag, Hungry Monster");
        AssertJUnit.assertEquals(t, 1, StringUtils.countMatches(t, "Commander Suspend 4"));
    }

    @Test
    public void testLegendsUseTheirShortName() {
        String t = text("Slobad, Actually Just Fine");
        AssertJUnit.assertTrue(t, t.contains("{T}, Remove 3 rebuilding counters from Slobad:"));
        t = text("Rafi, Retro Racer");
        AssertJUnit.assertTrue(t, t.contains("{R}, Sacrifice Rafi:"));
    }
}
