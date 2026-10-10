package forge.ai.ability;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;

/**
 * Simultaneous triggers of the same ability choose targets one at a time, the earlier ones already on the stack. For
 * an effect that sets rather than adds, the later ones shouldn't pile onto the same target.
 */
public class PendingTwinTargetsTest extends AITest {

    @Test
    public void twoVoicesOfTheVerminPickTwoCreatures() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card voice1 = addCard("Voice of the Vermin", ai);
        final Card voice2 = addCard("Voice of the Vermin", ai);
        addCard("Llanowar Elves", ai);
        addCard("Savannah Lions", ai);

        final SpellAbility first = attackTrigger(voice1, ai);
        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(first).doTrigger(ai, first, true));
        final Card firstTarget = first.getTargets().getFirstTargetedCard();
        AssertJUnit.assertNotNull(firstTarget);
        game.getStack().add(first);

        final SpellAbility second = attackTrigger(voice2, ai);
        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(second).doTrigger(ai, second, true));
        final Card secondTarget = second.getTargets().getFirstTargetedCard();
        AssertJUnit.assertNotNull(secondTarget);
        AssertJUnit.assertNotSame(firstTarget, secondTarget);
    }

    private static SpellAbility attackTrigger(final Card voice, final Player ai) {
        for (final Trigger t : voice.getTriggers()) {
            final SpellAbility sa = t.ensureAbility();
            if (sa != null && sa.getApi() == ApiType.Animate) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new AssertionError("no attack trigger on Voice of the Vermin");
    }
}
