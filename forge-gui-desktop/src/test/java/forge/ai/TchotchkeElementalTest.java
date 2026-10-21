package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Tchotchke Elemental (UNF #158), checked against Scryfall and its rulings 2026-10-21.
 */
public class TchotchkeElementalTest extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void testKnickknacks() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card elemental = addCard("Tchotchke Elemental", p);
        final Card bears = addCard("Grizzly Bears", p);
        game.getAction().checkStateEffects(true);
        game.getTriggerHandler().resetActiveTriggers();

        final Card growth = addCardToZone("Battlegrowth", p, ZoneType.Hand);
        final SpellAbility counter = growth.getFirstSpellAbility();
        counter.setActivatingPlayer(p);
        counter.getTargets().add(bears);
        AbilityUtils.resolve(counter);
        resolveAll(game);
        final CounterType knickknack = CounterType.getType("KNICKKNACK");
        AssertJUnit.assertEquals("a counter on another permanent", 1, elemental.getCounters(knickknack));

        final Card raise = addCardToZone("Raise the Alarm", p, ZoneType.Hand);
        final SpellAbility tokens = raise.getFirstSpellAbility();
        tokens.setActivatingPlayer(p);
        AbilityUtils.resolve(tokens);
        resolveAll(game);
        AssertJUnit.assertEquals("one for each token", 3, elemental.getCounters(knickknack));
        AssertJUnit.assertEquals(4, elemental.getNetPower());
    }
}
