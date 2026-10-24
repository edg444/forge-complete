package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Forgotten Realms Commander and Adventures in the Forgotten Realms review (2026-10-23). */
public class ForgottenRealmsAuditTest extends AITest {

    @Test
    public void testImmovableRodTargetsAnotherPermanent() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card rod = addCard("Immovable Rod", p);
        Card bear = addCard("Grizzly Bears", p);
        for (SpellAbility sa : rod.getSpellAbilities()) {
            if (sa.usesTargeting()) {
                sa.setActivatingPlayer(p);
                AssertJUnit.assertFalse(sa.canTarget(rod));
                AssertJUnit.assertTrue(sa.canTarget(bear));
                return;
            }
        }
        AssertJUnit.fail("no targeted ability");
    }

    @Test
    public void testCostTextReads() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        String golem = addCard("Clay Golem", p).getAbilityText();
        AssertJUnit.assertTrue(golem, golem.contains("{6}, Roll a d8:"));
        String hireling = addCard("Grim Hireling", p).getAbilityText();
        AssertJUnit.assertTrue(hireling, hireling.contains("{B}, Sacrifice X Treasures:"));
    }

    private Trigger activeTrigger(final String name, final String set, final String mode, final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), p);
        p.getZone(ZoneType.Battlefield).add(c);
        game.getAction().checkStateEffects(true);
        Trigger found = null;
        for (Trigger t : c.getTriggers()) {
            if (t.getMode().toString().equals(mode) && t.requirementsCheck(game)) {
                AssertJUnit.assertNull("only one version should be active", found);
                found = t;
            }
        }
        AssertJUnit.assertNotNull(found);
        return found;
    }

    @Test
    public void testDelinaPrintedAlwaysRollsAgain() {
        AssertJUnit.assertEquals("DBLoop", activeTrigger("Delina, Wild Mage", "AFR", "Attacks", false).getParam("Execute"));
        Trigger printed = activeTrigger("Delina, Wild Mage", "AFR", "Attacks", true);
        AssertJUnit.assertEquals("DBLoopPrinted", printed.getParam("Execute"));
        AssertJUnit.assertFalse(printed.ensureAbility().hasParam("RepeatOptional"));
    }
}
