package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.zone.ZoneType;

/** Marvel Super Heroes set review (2026-10-24). */
public class MarvelSuperHeroesAuditTest extends AITest {

    private String text(final String name) {
        Game game = initAndCreateGame();
        return addCardToZone(name, game.getPlayers().get(1), ZoneType.Hand).getAbilityText();
    }

    @Test
    public void testTeamworkShowsOnSpellsAndPermanents() {
        String t = text("Atlantis Attacks");
        AssertJUnit.assertTrue(t, t.contains("Teamwork 4 (As an additional cost"));
        AssertJUnit.assertTrue(t, t.contains("Choose one. If this spell was cast using teamwork, choose both instead."));
        t = text("Quantum Reduction");
        AssertJUnit.assertTrue(t, t.contains("Teamwork 2 ("));
        AssertJUnit.assertFalse(t, t.contains("Teamwork:2"));
    }
}
