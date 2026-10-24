package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Core Set 2019, 2017 Heroes of the Realm and Commander 2018 review (2026-10-23). */
public class CoreSet2019AuditTest extends AITest {

    @Test
    public void testUrsineChampionBecomesABearBerserker() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Ursine Champion", p);
        game.getAction().checkStateEffects(true);
        SpellAbility pump = null;
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Pump) {
                pump = sa;
            }
        }
        pump.setActivatingPlayer(p);
        AbilityUtils.resolve(pump);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(c.getType().hasCreatureType("Berserker"));
        AssertJUnit.assertTrue(c.getType().hasCreatureType("Bear"));
        AssertJUnit.assertFalse(c.getType().hasCreatureType("Human"));
    }

    @Test
    public void testInzervaLooksAtEachOtherPlayersLibrary() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Inzerva, Master of Insights"), p);
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.RepeatEach) {
                AssertJUnit.assertEquals("Player.Other", sa.getParam("RepeatPlayers"));
                AssertJUnit.assertEquals(ApiType.Scry, sa.getSubAbility().getApi());
                return;
            }
        }
        AssertJUnit.fail("no -2 ability");
    }

    private String yennettDraw(final String set, final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Yennett, Cryptic Sovereign", set), p);
        p.getZone(ZoneType.Battlefield).add(c);
        game.getAction().checkStateEffects(true);
        String found = null;
        for (Trigger t : c.getTriggers()) {
            if (t.requirementsCheck(game)) {
                AssertJUnit.assertNull("only one version should be active", found);
                found = t.ensureAbility().getSubAbility().getSubAbility().getParam("ConditionPresent");
            }
        }
        return found;
    }

    @Test
    public void testYennettPrintedTextDrawsOnlyForAnEvenCard() {
        AssertJUnit.assertEquals("Card", yennettDraw("C18", false));
        AssertJUnit.assertEquals("Card.cmcEven", yennettDraw("C18", true));
        AssertJUnit.assertEquals("Card", yennettDraw("CMM", true));
        AssertJUnit.assertEquals("Card", yennettDraw("SLD", true));
    }
}
