package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Largepox (CMB1 #44, upstream script): each player picks one permanent per listed type at once, never the same one
 * twice, and has to sacrifice as many as it can (Catch // Release's ruling). Upstream sacrificed type by type, so an
 * artifact creature offered as the artifact could spare a second artifact.
 */
public class LargepoxTest extends AITest {

    @Test
    public void anArtifactCreatureCantSpareTheOtherArtifact() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCards("Swamp", 4, ai);
        Card thopter = addCard("Ornithopter", opp);
        Card stone = addCard("Mind Stone", opp);
        addCardToZone("Island", opp, ZoneType.Hand);
        fillLibrary(opp, 3);
        fillLibrary(ai, 3);
        Card pox = addCardToZone("Largepox", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = pox.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());

        AssertJUnit.assertFalse(game.getCardState(thopter).isInZone(ZoneType.Battlefield));
        AssertJUnit.assertFalse(game.getCardState(stone).isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(1, opp.getPoisonCounters());
    }
}
