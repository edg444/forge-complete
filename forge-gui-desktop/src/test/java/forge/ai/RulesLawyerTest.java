package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class RulesLawyerTest extends AITest {

    @Test
    public void testStateBasedActionsSkipProtectedPlayerAndPermanents() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card lawyer = addCard("Rules Lawyer", me);
        Card damaged = addCard("Grizzly Bears", me);
        damaged.setDamage(5);
        Card shrunk = addCard("Grizzly Bears", me);
        shrunk.setCounters(CounterEnumType.M1M1, 3);
        Card annihilate = addCard("Grizzly Bears", me);
        annihilate.setCounters(CounterEnumType.P1P1, 2);
        annihilate.setCounters(CounterEnumType.M1M1, 1);
        Card legend1 = addCard("Isamaru, Hound of Konda", me);
        Card legend2 = addCard("Isamaru, Hound of Konda", me);
        Card walker = addCard("Jace Beleren", me);
        walker.setCounters(CounterEnumType.LOYALTY, 0);
        me.setLife(-5, null);
        me.setCounters(CounterEnumType.POISON, 12, null, false);

        Card oppDamaged = addCard("Grizzly Bears", opp);
        oppDamaged.setDamage(5);
        Card oppLegend1 = addCard("Isamaru, Hound of Konda", opp);
        Card oppLegend2 = addCard("Isamaru, Hound of Konda", opp);

        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse(game.isGameOver());
        AssertJUnit.assertFalse(me.hasLost());
        for (Card c : new Card[] {lawyer, damaged, shrunk, annihilate, legend1, legend2, walker}) {
            AssertJUnit.assertTrue(c + " should have stayed", c.isInZone(ZoneType.Battlefield));
        }
        AssertJUnit.assertEquals(2, annihilate.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(1, annihilate.getCounters(CounterEnumType.M1M1));

        AssertJUnit.assertTrue(game.getCardState(oppDamaged).isInZone(ZoneType.Graveyard));
        int oppLegendsLeft = (game.getCardState(oppLegend1).isInZone(ZoneType.Battlefield) ? 1 : 0)
                + (game.getCardState(oppLegend2).isInZone(ZoneType.Battlefield) ? 1 : 0);
        AssertJUnit.assertEquals(1, oppLegendsLeft);
    }

    @Test
    public void testLawyerItselfIsNotProtectedAndProtectionEnds() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);

        Card lawyer = addCard("Rules Lawyer", me);
        Card damaged = addCard("Grizzly Bears", me);
        damaged.setDamage(5);
        me.setLife(-5, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(damaged.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertFalse(me.hasLost());

        lawyer.setDamage(1);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(game.getCardState(lawyer).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(game.getCardState(damaged).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(me.hasLost());
    }
}
