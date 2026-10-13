package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Problematic Volcano (CMB1 #60), checked against Scryfall and its 5 rulings 2026-10-13: creatures are assigned and
 * enter to the left or right of that one volcano, can't block across it, and a creature with no side blocks and is
 * blocked by either side.
 */
public class ProblematicVolcanoTest extends AITest {

    private void resolveAll(final Game game) {
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());
    }

    @Test
    public void everyCreatureGetsASideAndCantBlockAcross() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card giant = addCard("Hill Giant", ai);
        Card bears = addCard("Grizzly Bears", opp);
        Card ox = addCard("Colossal Dreadmaw", opp); // survives the 4 damage
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card volcano = game.getAction().moveToPlay(addCardToZone("Problematic Volcano", ai, ZoneType.Hand), null, null);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        resolveAll(game);

        for (Card c : new Card[] {giant, bears, ox}) {
            Card now = game.getCardState(c);
            if (now.isInPlay()) {
                AssertJUnit.assertNotNull(now + " got a side", now.getVolcanoSide(volcano));
            }
        }

        // sides decide blocking, either way round
        bears = game.getCardState(bears);
        ox = game.getCardState(ox);
        giant = game.getCardState(giant);
        AssertJUnit.assertTrue(giant.isInPlay() && ox.isInPlay());
        giant.setVolcanoSide(volcano, true);
        ox.setVolcanoSide(volcano, false);
        AssertJUnit.assertFalse(CombatUtil.canBlock(ox, giant));
        AssertJUnit.assertFalse(CombatUtil.canBlock(giant, ox));
        ox.setVolcanoSide(volcano, true);
        AssertJUnit.assertTrue(CombatUtil.canBlock(ox, giant));
        AssertJUnit.assertTrue(giant.getVolcanoSideLabel().startsWith("left of"));

        // no side: either way
        ox.setVolcanoSide(volcano, null);
        giant.setVolcanoSide(volcano, false);
        AssertJUnit.assertTrue(CombatUtil.canBlock(ox, giant));
        AssertJUnit.assertTrue(CombatUtil.canBlock(giant, ox));
    }

    @Test
    public void creaturesEnterOnASideAndItStopsMatteringWhenItLeaves() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card volcano = addCard("Problematic Volcano", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        Card bears = game.getAction().moveToPlay(addCardToZone("Grizzly Bears", opp, ZoneType.Hand), null, null);
        AssertJUnit.assertNotNull("entered on a side", bears.getVolcanoSide(volcano));
        Card giant = game.getAction().moveToPlay(addCardToZone("Hill Giant", ai, ZoneType.Hand), null, null);
        AssertJUnit.assertNotNull(giant.getVolcanoSide(volcano));
        // nothing on the battlefield yet: the AI takes the left
        AssertJUnit.assertTrue(bears.getVolcanoSide(volcano));

        giant.setVolcanoSide(volcano, !bears.getVolcanoSide(volcano));
        AssertJUnit.assertFalse(CombatUtil.canBlock(bears, giant));
        game.getAction().destroy(volcano, null, false, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(CombatUtil.canBlock(bears, giant));
        AssertJUnit.assertNull(giant.getVolcanoSideLabel());
    }
}
