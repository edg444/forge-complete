package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Inhumaniac (Unstable): "At the beginning of your upkeep, roll a six-sided die. On a 3 or 4, put a +1/+1 counter on
 * this creature. On a 5 or higher, put two +1/+1 counters on it. On a 1, remove all +1/+1 counters from this
 * creature." - Oracle text checked against Scryfall 2026-10-03 (no rulings). "5 or higher" has to include results
 * past the die's 6.
 */
public class InhumaniacTest extends AITest {

    @Test
    public void testFiveOrHigherIncludesResultsAboveSix() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        // +4 to each roll: every result is 5 to 10
        addCard("Squirrel-Powered Scheme", me);
        addCard("Squirrel-Powered Scheme", me);
        Card inhumaniac = addCard("Inhumaniac", me);
        game.getAction().checkStateEffects(true);
        SpellAbility roll = inhumaniac.getTriggers().getFirst().ensureAbility().copy(inhumaniac, me, false);
        roll.setActivatingPlayer(me);
        for (int i = 1; i <= 30; i++) {
            AbilityUtils.resolve(roll);
            AssertJUnit.assertEquals(2 * i, inhumaniac.getCounters(CounterEnumType.P1P1));
        }
    }
}
