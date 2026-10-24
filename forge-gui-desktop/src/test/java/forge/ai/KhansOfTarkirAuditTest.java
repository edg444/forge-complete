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

/** Khans of Tarkir R&D's Secret Lair printed text (2026-10-23). */
public class KhansOfTarkirAuditTest extends AITest {

    private ApiType schemingApi(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card scheming = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Taigam's Scheming", "KTK"), p);
        p.getZone(ZoneType.Hand).add(scheming);
        game.getAction().checkStateEffects(true);
        for (SpellAbility sa : scheming.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if (sa.getRestrictions().checkOtherRestrictions(scheming, sa, p)) {
                return sa.getApi();
            }
        }
        return null;
    }

    private ApiType ascendancyApi(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card ascendancy = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Sultai Ascendancy", "KTK"), p);
        p.getZone(ZoneType.Battlefield).add(ascendancy);
        game.getAction().checkStateEffects(true);
        for (Trigger t : ascendancy.getTriggers()) {
            if (t.requirementsCheck(game)) {
                return t.ensureAbility().getApi();
            }
        }
        return null;
    }

    @Test
    public void testOracleSurveils() {
        AssertJUnit.assertEquals(ApiType.Surveil, schemingApi(false));
        AssertJUnit.assertEquals(ApiType.Surveil, ascendancyApi(false));
    }

    @Test
    public void testPrintedTextIsntASurveil() {
        AssertJUnit.assertEquals(ApiType.Dig, schemingApi(true));
        AssertJUnit.assertEquals(ApiType.Dig, ascendancyApi(true));
    }
}
