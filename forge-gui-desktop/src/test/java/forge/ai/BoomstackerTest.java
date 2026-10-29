package forge.ai;

import java.util.Random;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.DiceStack;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * Boomstacker (UND #49), checked against Scryfall and its 5 rulings 2026-10-20. User ruling: the dice stack is
 * simulated.
 */
public class BoomstackerTest extends AITest {

    private static final class Fixed extends Random {
        private final double roll;
        Fixed(final double roll) { this.roll = roll; }
        @Override public double nextDouble() { return roll; }
        @Override public int nextInt(final int bound) { return 0; }
    }

    private final Random before = MyRandom.getRandom();

    @AfterMethod
    public void restoreRandom() {
        MyRandom.setRandom(before);
    }

    private Card enter(final Game game, final Player p) {
        game.getAction().checkStateEffects(true);
        Card c = addCardToZone("Boomstacker", p, ZoneType.Hand);
        c = game.getAction().moveToPlay(c, p, null, null);
        game.getAction().checkStateEffects(true);
        return game.getCardState(c);
    }

    @Test
    public void odds() {
        AssertJUnit.assertEquals(0.024, DiceStack.placeChance(2), 1e-9);
        AssertJUnit.assertEquals(0.04, DiceStack.turnChance(2), 1e-9);
        AssertJUnit.assertEquals(0.64, DiceStack.turnChance(8), 1e-9);
        AssertJUnit.assertEquals(0.95, DiceStack.turnChance(20), 1e-9);
    }

    @Test
    public void entersWithTwoDiceAndGrowsWhenAttacking() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        MyRandom.setRandom(new Fixed(0.99));
        Card b = enter(game, ai);
        AssertJUnit.assertTrue(b.isInPlay());
        AssertJUnit.assertEquals(2, b.getStackedDice());
        AssertJUnit.assertEquals(2, b.getNetPower());
        AssertJUnit.assertEquals(2, b.getNetToughness());

        b.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        playUntilPhase(game, PhaseType.COMBAT_DECLARE_BLOCKERS);
        AssertJUnit.assertTrue("attacks each combat if able", b.isTapped());
        playUntilStackClear(game);
        AssertJUnit.assertEquals(4, b.getStackedDice());
        AssertJUnit.assertEquals(4, b.getNetPower());
    }

    @Test
    public void tappingCanTopplesIt() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        MyRandom.setRandom(new Fixed(0.99));
        Card b = enter(game, ai);
        MyRandom.setRandom(new Fixed(0.0));
        b.tap(null, ai);
        AssertJUnit.assertEquals(0, b.getStackedDice());
        playUntilStackClear(game);
        AssertJUnit.assertTrue(game.getCardState(b).isInZone(ZoneType.Graveyard));
    }
}
