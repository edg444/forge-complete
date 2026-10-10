package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameStage;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

/**
 * Ruff, Underdog Champ (Mystery Booster playtest card), Oracle text and rulings checked against Scryfall 2026-10-10:
 * "All Hounds are Dogs" is already true (the Hound type was folded into Dog, and Forge has no Hounds left), and a drawn
 * game has no loser, even when both players lost at once.
 */
public class RuffUnderdogChampTest extends AITest {

    /** Ends the first game with the given player (index) losing, or as a draw when null, then starts game two. */
    private Game secondGame(Integer loser) {
        Game first = initAndCreateGame();
        Match match = first.getMatch();
        if (loser == null) {
            for (Player p : first.getPlayers()) {
                p.intentionalDraw();
            }
            first.setGameOver(forge.game.GameEndReason.Draw);
        } else {
            first.getPlayers().get(loser).concede();
            first.getAction().checkGameOverCondition();
        }
        match.addGamePlayed(first);
        Game second = new Game(match.getPlayers(), first.getRules(), match);
        second.setAge(GameStage.Play);
        second.getPhaseHandler().devModeSet(PhaseType.MAIN1, second.getPlayers().get(1));
        second.getPhaseHandler().onStackResolved();
        return second;
    }

    private Card[] ruffAndDog(Game game) {
        Player p = game.getPlayers().get(1);
        Card ruff = addCard("Ruff, Underdog Champ", p);
        Card dog = addCard("Alpine Watchdog", p);
        Card theirs = addCard("Alpine Watchdog", game.getPlayers().get(0));
        game.getAction().checkStateEffects(true);
        return new Card[] {ruff, dog, theirs};
    }

    @Test
    public void testAfterALoss() {
        Card[] c = ruffAndDog(secondGame(1));
        AssertJUnit.assertEquals(4, c[0].getNetPower());
        AssertJUnit.assertEquals(3, c[0].getNetToughness());
        AssertJUnit.assertEquals(3, c[1].getNetPower());
        AssertJUnit.assertEquals(2, c[2].getNetPower());
    }

    @Test
    public void testAfterAWinOrADraw() {
        Card[] won = ruffAndDog(secondGame(0));
        AssertJUnit.assertEquals(3, won[0].getNetPower());
        AssertJUnit.assertEquals(2, won[1].getNetPower());
        Card[] drew = ruffAndDog(secondGame(null));
        AssertJUnit.assertEquals(3, drew[0].getNetPower());
    }

    @Test
    public void testFirstGame() {
        Game game = initAndCreateGame();
        Card[] c = ruffAndDog(game);
        AssertJUnit.assertEquals(3, c[0].getNetPower());
        AssertJUnit.assertTrue(c[0].getType().hasCreatureType("Dog"));
    }
}
