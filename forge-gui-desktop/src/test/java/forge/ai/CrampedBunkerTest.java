package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Cramped Bunker (Unstable) - Oracle text, the two Scryfall rulings and the Unstable FAQ entry checked 2026-10-03.
 * Room for 8 permanents is the user's call (one per side and one per corner).
 */
public class CrampedBunkerTest extends AITest {

    /** Runs the opponent's upkeep, where the Bunker triggers. */
    private void opponentsUpkeep(Game game, Player opp) {
        game.getPhaseHandler().devModeSet(PhaseType.UNTAP, opp);
        playUntilPhase(game, PhaseType.DRAW);
        game.getAction().checkStateEffects(true);
    }

    @Test
    public void testOpponentMovesItsBestPermanentIn() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bunker = addCard("Cramped Bunker", me);
        Card bears = addCard("Grizzly Bears", opp);
        Card dragon = addCard("Shivan Dragon", opp);
        opponentsUpkeep(game, opp);
        AssertJUnit.assertTrue(bunker.isRemembered(dragon));
        AssertJUnit.assertFalse(bunker.isRemembered(bears));
        AssertJUnit.assertTrue(dragon.getAbilityText(), dragon.getAbilityText().contains("This permanent is touching Cramped Bunker."));
        AssertJUnit.assertTrue(bunker.isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testFullBunkerWipesEverythingOutside() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bunker = addCard("Cramped Bunker", me);
        Card inside = null;
        for (int i = 0; i < 8; i++) {
            inside = addCard("Runeclaw Bear", opp);
            bunker.addRemembered(inside);
        }
        Card outside = addCard("Shivan Dragon", opp);
        Card mine = addCard("Grizzly Bears", me);
        opponentsUpkeep(game, opp);
        AssertJUnit.assertTrue(game.getCardState(outside).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(inside.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(game.getCardState(bunker).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(mine.isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testNothingLeftOutsideIsCant() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bunker = addCard("Cramped Bunker", me);
        Card bears = addCard("Grizzly Bears", opp);
        bunker.addRemembered(bears);
        opponentsUpkeep(game, opp);
        AssertJUnit.assertTrue(bears.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(game.getCardState(bunker).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testLeavingTheBattlefieldLeavesTheBunker() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bunker = addCard("Cramped Bunker", me);
        Card bears = addCard("Grizzly Bears", opp);
        bunker.addRemembered(bears);
        game.getAction().checkStateEffects(true);
        game.getAction().destroy(bears, null, true, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(bunker.isRemembered(bears));
    }
}
