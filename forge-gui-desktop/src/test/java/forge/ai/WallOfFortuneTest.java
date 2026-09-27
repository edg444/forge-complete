package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.effects.RollDiceEffect;
import forge.game.card.Card;
import forge.game.player.Player;

/** Wall of Fortune: tap an untapped Wall you control to have a player reroll a die they rolled. */
public class WallOfFortuneTest extends AITest {

    @Test
    public void testWallsRerollAnOpponentsHighRolls() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        // it isn't an activated ability, so a Wall that just arrived can be tapped (Unstable ruling)
        Card fortune = addCard("Wall of Fortune", me);
        fortune.setSickness(true);
        Card wood = addCard("Wall of Wood", me);
        Card bears = addCard("Grizzly Bears", me);

        for (int i = 0; i < 40; i++) {
            RollDiceEffect.rollDiceForPlayer(null, opp, 1, 6);
        }
        // a 4 or better is all but certain in 40 rolls, and each is rerolled with a Wall while one is untapped
        AssertJUnit.assertTrue(fortune.isTapped() || wood.isTapped());
        AssertJUnit.assertTrue(fortune.isTapped() && wood.isTapped());
        // only Walls can be tapped for it
        AssertJUnit.assertFalse(bears.isTapped());
    }

    @Test
    public void testNoWallsNoRerolls() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card wood = addCard("Wall of Wood", me);
        // without Wall of Fortune, a Wall is just a Wall
        for (int i = 0; i < 20; i++) {
            RollDiceEffect.rollDiceForPlayer(null, opp, 1, 6);
        }
        AssertJUnit.assertFalse(wood.isTapped());
    }
}
