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

/** Eldritch Moon R&D's Secret Lair printed text (2026-10-23). */
public class EldritchMoonAuditTest extends AITest {

    private ApiType planApi(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card plan = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Contingency Plan", "EMN"), p);
        p.getZone(ZoneType.Hand).add(plan);
        game.getAction().checkStateEffects(true);
        for (SpellAbility sa : plan.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if (sa.getRestrictions().checkOtherRestrictions(plan, sa, p)) {
                return sa.getApi();
            }
        }
        return null;
    }

    private ApiType flayerApi(final boolean lair, final String set) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card flayer = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Grim Flayer", set), p);
        p.getZone(ZoneType.Battlefield).add(flayer);
        game.getAction().checkStateEffects(true);
        for (Trigger t : flayer.getTriggers()) {
            if (t.requirementsCheck(game)) {
                return t.ensureAbility().getApi();
            }
        }
        return null;
    }

    @Test
    public void testOracleSurveils() {
        AssertJUnit.assertEquals(ApiType.Surveil, planApi(false));
        AssertJUnit.assertEquals(ApiType.Surveil, flayerApi(false, "EMN"));
    }

    @Test
    public void testPrintedTextIsntASurveil() {
        AssertJUnit.assertEquals(ApiType.Dig, planApi(true));
        AssertJUnit.assertEquals(ApiType.Dig, flayerApi(true, "EMN"));
        AssertJUnit.assertEquals(ApiType.Dig, flayerApi(true, "2X2"));
    }

    @Test
    public void testDuskmournCommanderFlayerSurveils() {
        AssertJUnit.assertEquals(ApiType.Surveil, flayerApi(true, "DSC"));
    }
}
