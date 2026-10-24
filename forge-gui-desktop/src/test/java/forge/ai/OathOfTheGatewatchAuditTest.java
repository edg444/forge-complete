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

/** Oath of the Gatewatch R&D's Secret Lair printed text (2026-10-23). */
public class OathOfTheGatewatchAuditTest extends AITest {

    private SpellAbility dwellersTrigger(final boolean lair, final String set) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card dwellers = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Goblin Dark-Dwellers", set), p);
        p.getZone(ZoneType.Battlefield).add(dwellers);
        game.getAction().checkStateEffects(true);
        SpellAbility found = null;
        for (Trigger t : dwellers.getTriggers()) {
            if (t.requirementsCheck(game)) {
                AssertJUnit.assertNull("only one ETB trigger should be live", found);
                found = t.ensureAbility();
            }
        }
        return found;
    }

    @Test
    public void testOracleExilesOnlyTheSpell() {
        SpellAbility sa = dwellersTrigger(false, "OGW");
        AssertJUnit.assertTrue(sa.hasParam("ReplaceGraveyard"));
        AssertJUnit.assertNull(sa.getSubAbility());
    }

    @Test
    public void testPrintedExilesTheCardAllTurn() {
        SpellAbility sa = dwellersTrigger(true, "OGW");
        AssertJUnit.assertFalse(sa.hasParam("ReplaceGraveyard"));
        AssertJUnit.assertEquals(ApiType.Effect, sa.getSubAbility().getApi());
    }

    @Test
    public void testCorrectedPrintingStaysOracle() {
        SpellAbility sa = dwellersTrigger(true, "LTC");
        AssertJUnit.assertTrue(sa.hasParam("ReplaceGraveyard"));
    }
}
