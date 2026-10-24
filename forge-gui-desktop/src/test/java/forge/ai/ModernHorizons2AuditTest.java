package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;

/** Modern Horizons 2 review (2026-10-23). */
public class ModernHorizons2AuditTest extends AITest {

    @Test
    public void testMagusOfTheBridgeCountsCreaturesYouOwn() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Magus of the Bridge", p);
        int checked = 0;
        for (Trigger t : c.getTriggers()) {
            AssertJUnit.assertEquals("Battlefield", t.getParam("TriggerZones"));
            if (t.getParam("ValidCard").contains("!token")) {
                AssertJUnit.assertTrue(t.getParam("ValidCard").contains("YouOwn"));
            }
            checked++;
        }
        AssertJUnit.assertEquals(2, checked);
    }

    @Test
    public void testSacrificeXCostsRead() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        String chatterfang = addCard("Chatterfang, Squirrel General", p).getAbilityText();
        AssertJUnit.assertTrue(chatterfang, chatterfang.contains("{B}, Sacrifice X Squirrels:"));
        String lonis = addCard("Lonis, Cryptozoologist", p).getAbilityText();
        AssertJUnit.assertTrue(lonis, lonis.contains("{T}, Sacrifice X Clues:"));
    }

    @Test
    public void testSmellFearDescribesTheFightOnce() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        SpellAbility sa = addCardToZone("Smell Fear", p, forge.game.zone.ZoneType.Hand).getFirstSpellAbility();
        String desc = sa.getDescription() + sa.getSubAbility().getDescription();
        AssertJUnit.assertEquals(desc, desc.indexOf("fights"), desc.lastIndexOf("fights"));
    }
}
