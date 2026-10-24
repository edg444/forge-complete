package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Crimson Vow through Kamigawa: Neon Dynasty review (2026-10-23). */
public class CrimsonVowNeonDynastyAuditTest extends AITest {

    @Test
    public void testBoardedWindowCountsDamageNotLifeLoss() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card window = addCard("Boarded Window", p);
        AssertJUnit.assertEquals("Count$DamageThisTurn Card,Emblem You", window.getSVar("X"));
    }

    @Test
    public void testKaitoPhasesOutOnlyAtYourEndStep() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card kaito = addCard("Kaito Shizuki", p);
        for (Trigger t : kaito.getTriggers()) {
            if (t.getMode().toString().equals("Phase")) {
                AssertJUnit.assertEquals("You", t.getParam("ValidPlayer"));
                return;
            }
        }
        AssertJUnit.fail("no end step trigger");
    }

    @Test
    public void testOwnershipAndControlChecks() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card satsuki = addCard("Satsuki, the Living Lore", p);
        AssertJUnit.assertTrue(satsuki.getSVar("GraveSaga").contains("ValidTgts$ Saga.YouOwn"));
        Card storyweave = addCardToZone("Storyweave", p, ZoneType.Hand);
        AssertJUnit.assertTrue(storyweave.getSVar("ReplaceETB").contains("Creature.Enchantment+YouCtrl"));
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
    public void testPrintedVersions() {
        AssertJUnit.assertEquals("TrigSurveil", activeTrigger("Cruel Witness", "VOW", "SpellCast", false).getParam("Execute"));
        AssertJUnit.assertEquals("TrigLookPrinted", activeTrigger("Cruel Witness", "VOW", "SpellCast", true).getParam("Execute"));
        AssertJUnit.assertEquals("TrigSurveil", activeTrigger("Cruel Witness", "J25", "SpellCast", true).getParam("Execute"));
        AssertJUnit.assertEquals("TrigPump", activeTrigger("Bloodvial Purveyor", "VOW", "Attacks", false).getParam("Execute"));
        AssertJUnit.assertEquals("TrigPumpPrinted", activeTrigger("Bloodvial Purveyor", "VOW", "Attacks", true).getParam("Execute"));
    }
}
