package forge.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.effects.RollDiceEffect;
import forge.game.player.Player;
import forge.util.MyRandom;

/**
 * Krark's Other Thumb (Unstable) - Oracle text, the five Scryfall rulings and the Unstable FAQ entries checked
 * 2026-10-09. Each die becomes two, the roller keeps one, and only the kept one was rolled as far as anything knows.
 */
public class KrarksOtherThumbTest extends AITest {

    /** Rolls the given faces, in order, then fails loudly if asked for more. */
    @SuppressWarnings("serial")
    private static final class Faces extends Random {
        private final Deque<Integer> faces = new ArrayDeque<>();
        Faces(final int... values) {
            for (final int v : values) {
                faces.add(v);
            }
        }
        @Override
        public int nextInt(final int bound) {
            if (faces.isEmpty()) {
                throw new IllegalStateException("rolled more dice than expected");
            }
            return faces.poll() - 1;
        }
    }

    private int roll(final Player me, final int... faces) {
        final Random before = MyRandom.getRandom();
        MyRandom.setRandom(new Faces(faces));
        try {
            return RollDiceEffect.rollDiceForPlayer(null, me, 1, 6);
        } finally {
            MyRandom.setRandom(before);
        }
    }

    @Test
    public void rollTwoKeepOne() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        addCard("Krark's Other Thumb", me);
        game.getAction().checkStateEffects(true);

        // a 2 and a 5: the AI keeps the 5, and the ignored 2 was never rolled
        AssertJUnit.assertEquals(5, roll(me, 2, 5));
        AssertJUnit.assertEquals(1, me.getNumRollsThisTurn());
    }

    @Test
    public void twoThumbsMeanTwoPairs() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        // it's legendary, so two only coexist when the legend rule doesn't apply (Mirror Gallery) - no state-based
        // actions here
        addCard("Krark's Other Thumb", me);
        addCard("Krark's Other Thumb", me);

        // the second Thumb applies to each of the first's two dice: (1, 2) keeps 2, (3, 6) keeps 6, then 6
        AssertJUnit.assertEquals(6, roll(me, 1, 2, 3, 6));
        AssertJUnit.assertEquals(1, me.getNumRollsThisTurn());
    }

    @Test
    public void onlyItsControllersDice() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        addCard("Krark's Other Thumb", me);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(3, roll(opp, 3));
    }
}
