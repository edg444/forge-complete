package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/** Theros Beyond Death review and R&D's Secret Lair printed text (2026-10-23). */
public class TherosBeyondDeathAuditTest extends AITest {

    @Test
    public void testRageScarredBerserkerTargetsYourCreature() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Rage-Scarred Berserker", p);
        Trigger t = c.getTriggers().iterator().next();
        AssertJUnit.assertEquals("Creature.YouCtrl",
                t.getOverridingAbility().getTargetRestrictions().getValidTgts()[0]);
    }

    @Test
    public void testUroAttackTriggerIsMandatory() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Uro, Titan of Nature's Wrath", p);
        for (Trigger t : c.getTriggers()) {
            if (t.getMode() == TriggerType.Attacks) {
                AssertJUnit.assertFalse(t.hasParam("OptionalDecider"));
                return;
            }
        }
        AssertJUnit.fail("no attack trigger");
    }

    /** Eat to Extinction's castable spell: the one whose restrictions pass. */
    private SpellAbility eatToExtinction(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = addCardToZone("Eat to Extinction", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        SpellAbility found = null;
        for (SpellAbility sa : c.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if (sa.getRestrictions().checkOtherRestrictions(c, sa, p)) {
                AssertJUnit.assertNull("only one version should be active", found);
                found = sa;
            }
        }
        return found;
    }

    @Test
    public void testEatToExtinctionPrintedLookIsNotASurveil() {
        AssertJUnit.assertEquals("DBSurveil", eatToExtinction(false).getParam("SubAbility"));
        AssertJUnit.assertEquals("DBLook", eatToExtinction(true).getParam("SubAbility"));
    }
}
