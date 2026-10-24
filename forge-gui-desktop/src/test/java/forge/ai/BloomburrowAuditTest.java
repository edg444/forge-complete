package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.zone.ZoneType;

/** Bloomburrow and Mystery Booster 2 set review (2026-10-24). */
public class BloomburrowAuditTest extends AITest {

    private String text(final String name) {
        Game game = initAndCreateGame();
        return addCardToZone(name, game.getPlayers().get(1), ZoneType.Hand).getAbilityText();
    }

    @Test
    public void testSacrificeAnyNumberUsesTheDescription() {
        String t = text("Rottenmouth Viper");
        AssertJUnit.assertTrue(t, t.contains("you may sacrifice any number of nonland permanents."));
    }

    @Test
    public void testSpliceOnAPermanent() {
        String t = text("Lifening Elemental");
        AssertJUnit.assertTrue(t, t.contains("Splice onto instant or sorcery {1}{B} ("));
    }
}
