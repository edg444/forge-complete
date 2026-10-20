package forge.ai;

import java.util.Random;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * Rings a Bell (UND #27), checked against Scryfall and its 3 rulings 2026-10-20. User ruling: the AI listener rings 75%
 * of the time.
 */
public class RingsABellTest extends AITest {

    private static final class Fixed extends Random {
        private final int value;
        Fixed(final int value) { this.value = value; }
        @Override public int nextInt(final int bound) { return Math.min(value, bound - 1); }
    }

    private Card enter(final Game game, final Player p) {
        game.getAction().checkStateEffects(true);
        Card bell = addCardToZone("Rings a Bell", p, ZoneType.Hand);
        // no game loop here: the AI would say the word itself at its first priority
        bell = game.getAction().moveToPlay(bell, p, null, null);
        game.getAction().checkStateEffects(true);
        return game.getCardState(bell);
    }

    private static SpellAbility say(final Card bell, final Player p) {
        SpellAbility sa = bell.getSpellAbilities().stream().filter(s -> s.getApi() == ApiType.RingBell).findFirst().orElseThrow();
        sa.setActivatingPlayer(p);
        return sa;
    }

    private int drawsAfterSaying(final int roll) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        fillLibrary(ai, 5);
        Card bell = enter(game, ai);
        AssertJUnit.assertTrue("a word with four or more letters", bell.getChosenType().replaceAll("[^A-Za-z]", "").length() >= 4);

        int hand = ai.getCardsIn(ZoneType.Hand).size();
        SpellAbility sa = say(bell, ai);
        AssertJUnit.assertTrue(sa.canPlay());
        final Random before = MyRandom.getRandom();
        MyRandom.setRandom(new Fixed(roll));
        try {
            AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        } finally {
            MyRandom.setRandom(before);
        }
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
        }
        AssertJUnit.assertFalse("only the first time each turn", say(bell, ai).canPlay());
        AssertJUnit.assertFalse(bell.hasRemembered());
        return ai.getCardsIn(ZoneType.Hand).size() - hand;
    }

    @Test
    public void noBellDraws() {
        AssertJUnit.assertEquals(1, drawsAfterSaying(99));
    }

    @Test
    public void aBellStopsIt() {
        AssertJUnit.assertEquals(0, drawsAfterSaying(0));
    }
}
