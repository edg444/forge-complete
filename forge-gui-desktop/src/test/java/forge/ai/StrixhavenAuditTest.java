package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Commander 2021 and Strixhaven review (2026-10-23). */
public class StrixhavenAuditTest extends AITest {

    @Test
    public void testLetterOfAcceptanceDrawNeedsTap() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Letter of Acceptance", p);
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Draw) {
                AssertJUnit.assertTrue(sa.getPayCosts().hasTapCost());
                return;
            }
        }
        AssertJUnit.fail("no draw ability");
    }

    @Test
    public void testDuelingCoachCanTargetAnOpponentsCreature() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card coach = addCard("Dueling Coach", p);
        Card bear = addCard("Grizzly Bears", opp);
        SpellAbility etb = coach.getTriggers().iterator().next().ensureAbility();
        etb.setActivatingPlayer(p);
        AssertJUnit.assertTrue(etb.canTarget(bear));
    }

    @Test
    public void testLukkaExilesAtYourNextUpkeep() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Mila, Crafty Companion", p);
        AssertJUnit.assertFalse(c.getState(CardStateName.Backside).getSVar("DBPump").contains("AtEOT"));
        AssertJUnit.assertTrue(c.getState(CardStateName.Backside).getSVar("DBUpkeepExile").contains("Phase$ Upkeep"));
    }

    private Card printing(final String name, final String set, final Player p, final ZoneType zone) {
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), p);
        p.getZone(zone).add(c);
        return c;
    }

    private int activeSpells(final String name, final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = printing(name, "STX", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        int surveil = 0;
        for (SpellAbility sa : c.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if (sa.getRestrictions().checkOtherRestrictions(c, sa, p)) {
                surveil = sa.getApi() == ApiType.Surveil ? 1 : 2;
            }
        }
        return surveil;
    }

    @Test
    public void testCuratePrintedLooksInsteadOfSurveilling() {
        AssertJUnit.assertEquals(1, activeSpells("Curate", false));
        AssertJUnit.assertEquals(2, activeSpells("Curate", true));
    }

    private Trigger activeTrigger(final String name, final String set, final String mode, final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = printing(name, set, p, ZoneType.Battlefield);
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
    public void testLaeliaAndExpressionistPrintedCountOnlyYourExiles() {
        AssertJUnit.assertFalse(activeTrigger("Laelia, the Blade Reforged", "C21", "ChangesZoneAll", false).hasParam("ValidCause"));
        AssertJUnit.assertTrue(activeTrigger("Laelia, the Blade Reforged", "C21", "ChangesZoneAll", true).hasParam("ValidCause"));
        AssertJUnit.assertEquals("TrigAnimate", activeTrigger("Elemental Expressionist", "STX", "SpellCastOrCopy", false).getParam("Execute"));
        AssertJUnit.assertEquals("TrigAnimatePrinted", activeTrigger("Elemental Expressionist", "STX", "SpellCastOrCopy", true).getParam("Execute"));
    }
}
