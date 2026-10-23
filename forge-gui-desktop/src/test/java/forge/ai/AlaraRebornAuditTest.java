package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/** Alara Reborn script review (2026-10-23). */
public class AlaraRebornAuditTest extends AITest {

    @Test
    public void testBlitzHellionShufflesAwayOnOpponentsEndStep() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Island", p, ZoneType.Library);
            addCardToZone("Island", opp, ZoneType.Library);
        }
        Card hellion = addCard("Blitz Hellion", p);
        game.getAction().checkStateEffects(true);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, opp);
        playUntilPhase(game, PhaseType.CLEANUP);
        AssertJUnit.assertTrue(p.getZone(ZoneType.Library).contains(c -> c.getName().equals("Blitz Hellion")));
        AssertJUnit.assertFalse(p.getZone(ZoneType.Battlefield).contains(hellion));
    }
}
