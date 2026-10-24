package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.zone.ZoneType;

/** Murders at Karlov Manor set review (2026-10-24). */
public class KarlovManorAuditTest extends AITest {

    private String text(final String name) {
        Game game = initAndCreateGame();
        return addCardToZone(name, game.getPlayers().get(1), ZoneType.Hand).getAbilityText();
    }

    @Test
    public void testDeckLimitLineOnASorcery() {
        String t = text("Slime Against Humanity");
        AssertJUnit.assertTrue(t, t.contains("A deck can have any number of cards named Slime Against Humanity."));
    }

    @Test
    public void testDisguiseCostReductionText() {
        String t = text("Fugitive Codebreaker");
        AssertJUnit.assertTrue(t, t.contains("Disguise {5}{R}. This cost is reduced by {1} for each instant and sorcery card in your graveyard."));
    }

    @Test
    public void testSolvedAbilityWord() {
        String t = text("Case of the Burning Masks");
        AssertJUnit.assertTrue(t, t.contains("Solved — Sacrifice this Case:"));
    }
}
