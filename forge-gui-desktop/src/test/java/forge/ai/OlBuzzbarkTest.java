package forge.ai;

import java.util.List;
import java.util.Random;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.DiceLanding;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * Ol' Buzzbark (Unstable) - Oracle text, the six Scryfall rulings and the Unstable FAQ entry checked 2026-10-06.
 */
public class OlBuzzbarkTest extends AITest {

    /** Every die comes to rest squarely on the creature it was aimed at; the dice results stay random. */
    private static class DeadCenter extends Random {
        @Override
        public double nextDouble() {
            return 0.5;
        }
        @Override
        public synchronized double nextGaussian() {
            return 0;
        }
    }

    private void enter(Game game, Player me, Card buzzbark, int x) {
        game.getAction().checkStateEffects(true);
        Random before = MyRandom.getRandom();
        MyRandom.setRandom(new DeadCenter());
        try {
            for (Trigger t : buzzbark.getTriggers()) {
                if (t.getMode() == TriggerType.ChangesZone) {
                    SpellAbility sa = t.ensureAbility();
                    sa.setActivatingPlayer(me);
                    sa.setSVar("X", "Number$" + x);
                    AbilityUtils.resolve(sa);
                }
            }
        } finally {
            MyRandom.setRandom(before);
        }
        game.getAction().checkStateEffects(true);
    }

    @Test
    public void testDiceOnAnOpponentsCreatureDealDamage() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card wurm = addCard("Craw Wurm", opp);
        Card buzzbark = addCard("Ol' Buzzbark", me);
        enter(game, me, buzzbark, 4);
        // four dice, at least 1 each, on a 6/4
        AssertJUnit.assertTrue(wurm.isInZone(ZoneType.Graveyard) || game.getCardState(wurm).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals(0, buzzbark.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(4, game.getGameLog().getLogEntries(null).stream()
                .filter(e -> e.message().contains("comes to rest touching Craw Wurm")).count());
    }

    @Test
    public void testDiceOnYourOwnCreatureGiveCounters() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card buzzbark = addCard("Ol' Buzzbark", me);
        enter(game, me, buzzbark, 3);
        final int counters = buzzbark.getCounters(CounterEnumType.P1P1);
        AssertJUnit.assertTrue(counters >= 3 && counters <= 18);
        AssertJUnit.assertEquals(0, buzzbark.getDamage());
    }

    @Test
    public void testXOfZeroRollsNothing() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card buzzbark = addCard("Ol' Buzzbark", me);
        enter(game, me, buzzbark, 0);
        AssertJUnit.assertEquals(0, buzzbark.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testHigherDropsScatterMore() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        List<Card> row = addCards("Grizzly Bears", 5, opp);
        Card middle = row.get(2);
        Random rnd = new Random(20261006);
        int lowOnAim = 0, highOnAim = 0, lowTouching = 0, highTouching = 0, twoAtOnce = 0;
        for (int trial = 0; trial < 2000; trial++) {
            for (CardCollection die : DiceLanding.land(middle, 1, 1, rnd)) {
                lowTouching += die.isEmpty() ? 0 : 1;
                lowOnAim += die.contains(middle) ? 1 : 0;
                twoAtOnce += die.size() == 2 ? 1 : 0;
            }
            for (CardCollection die : DiceLanding.land(middle, 1, 12, rnd)) {
                highTouching += die.isEmpty() ? 0 : 1;
                highOnAim += die.contains(middle) ? 1 : 0;
            }
        }
        AssertJUnit.assertTrue(lowTouching > highTouching);
        AssertJUnit.assertTrue(lowOnAim > 2 * highOnAim);
        // a die on the edge between two neighbors touches both
        AssertJUnit.assertTrue(twoAtOnce > 0);
    }
}
