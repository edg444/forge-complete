package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Odyssey R&D's Secret Lair printed text (2026-10-23). */
public class OdysseyAuditTest extends AITest {

    private int spybugCountersAfterUpkeep(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Island", p, ZoneType.Library);
            addCardToZone("Island", opp, ZoneType.Library);
        }
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card tank = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Think Tank", "ODY"), p);
        p.getZone(ZoneType.Battlefield).add(tank);
        Card spybug = addCard("Dimir Spybug", p);
        game.getAction().checkStateEffects(true);
        game.getPhaseHandler().devModeSet(PhaseType.CLEANUP, opp);
        playUntilPhase(game, PhaseType.DRAW);
        return spybug.getCounters(CounterEnumType.P1P1);
    }

    @Test
    public void testThinkTankSurveilsByOracle() {
        AssertJUnit.assertEquals(1, spybugCountersAfterUpkeep(false));
    }

    @Test
    public void testThinkTankDoesNotSurveilAsPrinted() {
        AssertJUnit.assertEquals(0, spybugCountersAfterUpkeep(true));
    }
}
