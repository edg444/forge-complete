package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/** Kaldheim and Kaldheim Commander review (2026-10-23). */
public class KaldheimAuditTest extends AITest {

    @Test
    public void testSpectralSteelCanReturnAnAura() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card steel = addCardToZone("Spectral Steel", p, ZoneType.Graveyard);
        Card aura = addCardToZone("Pacifism", p, ZoneType.Graveyard);
        for (SpellAbility sa : steel.getSpellAbilities()) {
            if (sa.getApi() == ApiType.ChangeZone) {
                sa.setActivatingPlayer(p);
                AssertJUnit.assertTrue(sa.canTarget(aura));
                AssertJUnit.assertFalse(sa.canTarget(steel));
                return;
            }
        }
        AssertJUnit.fail("no return ability");
    }

    @Test
    public void testCinderheartGiantChoosesWithoutTargeting() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Cinderheart Giant", p);
        SpellAbility dies = c.getTriggers().iterator().next().ensureAbility();
        AssertJUnit.assertEquals(ApiType.ChooseCard, dies.getApi());
        AssertJUnit.assertFalse(dies.usesTargeting());
    }

    @Test
    public void testRampageOfTheValkyriesHitsEachOtherPlayer() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Rampage of the Valkyries", p);
        AssertJUnit.assertEquals("Player.Other", c.getSVar("TrigSac").replaceAll(".*Defined\\$ ([^ |]+).*", "$1"));
    }

    private Trigger activeHandTrigger(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = addCard("Hero of Bretagard", p);
        game.getAction().checkStateEffects(true);
        Trigger found = null;
        for (Trigger t : c.getTriggers()) {
            if ("Hand".equals(t.getParam("Origin")) && t.requirementsCheck(game)) {
                AssertJUnit.assertNull("only one version should be active", found);
                found = t;
            }
        }
        AssertJUnit.assertNotNull(found);
        return found;
    }

    @Test
    public void testHeroOfBretagardPrintedCountsOnlyYourExiles() {
        AssertJUnit.assertFalse(activeHandTrigger(false).hasParam("ValidCause"));
        AssertJUnit.assertEquals("SpellAbility.YouCtrl", activeHandTrigger(true).getParam("ValidCause"));
    }
}
