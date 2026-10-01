package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.effects.RollDiceEffect;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Snickering Squirrel (Unstable): "You may tap this creature to increase the result of a die any player rolled by
 * 1." - Oracle text, rulings and the Unstable FAQ entry checked 2026-10-01.
 */
public class SnickeringSquirrelTest extends AITest {

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Card squirrel = addCard("Snickering Squirrel", game.getPlayers().get(1));
        AssertJUnit.assertEquals("You may tap this creature to increase the result of a die any player rolled by 1.",
                squirrel.getAbilityText().trim());
    }

    @Test
    public void testSeveralOnOneDie() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card first = addCard("Snickering Squirrel", ai);
        Card second = addCard("Snickering Squirrel", ai);
        // just arrived: it isn't a {T} ability, so summoning sickness doesn't matter (ruling)
        first.setSickness(true);
        second.setSickness(true);

        // the opponent's roll: the AI won't help it
        AssertJUnit.assertEquals(0, RollDiceEffect.tapToIncrease(opp, 3, 6));
        AssertJUnit.assertTrue(first.isUntapped() && second.isUntapped());

        // its own 6: both Squirrels, for an 8 (ruling: results that aren't normally possible)
        AssertJUnit.assertEquals(2, RollDiceEffect.tapToIncrease(ai, 6, 6));
        AssertJUnit.assertTrue(first.isTapped() && second.isTapped());
        AssertJUnit.assertEquals(0, RollDiceEffect.tapToIncrease(ai, 4, 6));
    }

    @Test
    public void testOnARealRoll() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card squirrel = addCard("Snickering Squirrel", ai);
        // summoning sick, so the only way it ends up tapped is by raising the roll
        squirrel.setSickness(true);
        // Sly Spy F: "roll a six-sided die. That player loses life equal to the result."
        PaperCard pc = FModel.getMagicDb().getCommonCards().getCard("Sly Spy", "UST", "67f");
        Card spy = Card.fromPaperCard(pc, ai);
        spy.setGameTimestamp(game.getNextTimestamp());
        ai.getZone(ZoneType.Battlefield).add(spy);
        spy.setSickness(false);
        game.getAction().checkStateEffects(true);
        playUntilPhase(game, PhaseType.MAIN2);
        AssertJUnit.assertTrue(squirrel.isTapped());
        // 2 combat damage, then the roll plus 1: 4 to 9 in all
        AssertJUnit.assertTrue("life " + opp.getLife(), opp.getLife() >= 11 && opp.getLife() <= 16);
    }
}
