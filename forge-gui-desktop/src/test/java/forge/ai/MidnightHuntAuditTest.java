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
import forge.model.FModel;

/** Midnight Hunt Commander and Innistrad: Midnight Hunt review (2026-10-23). */
public class MidnightHuntAuditTest extends AITest {

    @Test
    public void testCurseOfClingingWebsWatchesControlledCreatures() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card curse = addCard("Curse of Clinging Webs", p);
        Trigger t = curse.getTriggers().iterator().next();
        AssertJUnit.assertTrue(t.getParam("ValidCard"), t.getParam("ValidCard").startsWith("Creature.EnchantedPlayerCtrl+"));
    }

    @Test
    public void testSunriseCavalierTargetsYourCreature() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card cavalier = addCard("Sunrise Cavalier", p);
        Card bear = addCard("Grizzly Bears", opp);
        for (Trigger t : cavalier.getTriggers()) {
            if (t.getMode().toString().equals("DayTimeChanges")) {
                SpellAbility sa = t.ensureAbility();
                sa.setActivatingPlayer(p);
                AssertJUnit.assertFalse(sa.canTarget(bear));
                AssertJUnit.assertTrue(sa.canTarget(cavalier));
                return;
            }
        }
        AssertJUnit.fail("no day/night trigger");
    }

    @Test
    public void testGorexSaysYouMay() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        String text = addCard("Gorex, the Tombshell", p).getAbilityText();
        AssertJUnit.assertTrue(text, text.contains("you may exile any number of creature cards"));
    }

    private ApiType activeApi(final String name, final String set, final ZoneType zone, final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), p);
        p.getZone(zone).add(c);
        game.getAction().checkStateEffects(true);
        ApiType found = null;
        for (SpellAbility sa : c.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if ((sa.getApi() == ApiType.Surveil || sa.getApi() == ApiType.Dig)
                    && sa.getRestrictions().checkOtherRestrictions(c, sa, p)) {
                AssertJUnit.assertNull("only one version should be active", found);
                found = sa.getApi();
            }
        }
        AssertJUnit.assertNotNull(found);
        return found;
    }

    @Test
    public void testPrintedLookInsteadOfSurveil() {
        AssertJUnit.assertEquals(ApiType.Surveil, activeApi("Consider", "MID", ZoneType.Hand, false));
        AssertJUnit.assertEquals(ApiType.Dig, activeApi("Consider", "MID", ZoneType.Hand, true));
        AssertJUnit.assertEquals(ApiType.Surveil, activeApi("Consider", "LTC", ZoneType.Hand, true));
        AssertJUnit.assertEquals(ApiType.Dig, activeApi("Otherworldly Gaze", "MID", ZoneType.Hand, true));
        AssertJUnit.assertEquals(ApiType.Surveil, activeApi("Larder Zombie", "MID", ZoneType.Battlefield, false));
        AssertJUnit.assertEquals(ApiType.Dig, activeApi("Larder Zombie", "MID", ZoneType.Battlefield, true));
    }
}
