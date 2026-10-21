package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Icing Manipulator (UNF #142), checked against Scryfall and its rulings 2026-10-21: each +1/+1 counter on a creature
 * you control is also a Food token, which stays on that creature; sacrificing the Food takes its counter with it.
 */
public class IcingManipulatorTest extends AITest {

    private CardCollection foods(final Player p) {
        return CardLists.filter(p.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype("Food"));
    }

    @Test
    public void testCountersAreFood() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card icing = addCard("Icing Manipulator", p);
        final Card bears = addCard("Grizzly Bears", p);
        final Card theirs = addCard("Grizzly Bears", opp);
        bears.addCounterInternal(CounterEnumType.P1P1, 2, p, false, null, null);
        theirs.addCounterInternal(CounterEnumType.P1P1, 3, opp, false, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, foods(p).size());
        AssertJUnit.assertEquals("not on creatures you don't control", 0, foods(opp).size());
        AssertJUnit.assertEquals(bears, foods(p).get(0).getCounterTokenHost());
        AssertJUnit.assertEquals("the counters stay put", 4, bears.getNetPower());

        bears.subtractCounter(CounterEnumType.P1P1, 1, p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, foods(p).size());

        final java.util.Map<forge.game.ability.AbilityKey, Object> params = forge.game.ability.AbilityKey.newMap();
        params.put(forge.game.ability.AbilityKey.LastStateBattlefield, game.copyLastStateBattlefield());
        params.put(forge.game.ability.AbilityKey.LastStateGraveyard, game.copyLastStateGraveyard());
        game.getAction().sacrifice(new CardCollection(foods(p).get(0)), null, true, params);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("its counter went with it", 0, bears.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(0, foods(p).size());

        bears.addCounterInternal(CounterEnumType.P1P1, 2, p, false, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, foods(p).size());
        game.getAction().exile(icing, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("just counters again", 0, foods(p).size());
        AssertJUnit.assertEquals(2, bears.getCounters(CounterEnumType.P1P1));
    }
}
