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

/** Amonkhet review (2026-10-23). */
public class AmonkhetAuditTest extends AITest {

    @Test
    public void testWatchersOfTheDeadSparesYourGraveyard() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card watchers = addCard("Watchers of the Dead", p);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Grizzly Bears", p, ZoneType.Graveyard);
            addCardToZone("Grizzly Bears", opp, ZoneType.Graveyard);
        }
        game.getAction().checkStateEffects(true);
        SpellAbility sa = null;
        for (SpellAbility s : watchers.getSpellAbilities()) {
            if (s.getApi() == ApiType.RepeatEach) {
                sa = s;
            }
        }
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(3, p.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertEquals(2, opp.getCardsIn(ZoneType.Graveyard).size());
    }

    private ApiType nagaApi(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card naga = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Naga Oracle", "AKH"), p);
        p.getZone(ZoneType.Battlefield).add(naga);
        game.getAction().checkStateEffects(true);
        for (Trigger t : naga.getTriggers()) {
            if (t.requirementsCheck(game)) {
                return t.ensureAbility().getApi();
            }
        }
        return null;
    }

    @Test
    public void testNagaOracleOracleSurveils() {
        AssertJUnit.assertEquals(ApiType.Surveil, nagaApi(false));
    }

    @Test
    public void testNagaOraclePrintedTextIsntASurveil() {
        AssertJUnit.assertEquals(ApiType.Dig, nagaApi(true));
    }
}
