package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Patient Turtle (CMB1 #82), checked against Scryfall 2026-10-15 (no rulings). */
public class PatientTurtleTest extends AITest {

    private static Card cast(final Game game, final Player p, final Card inHand) {
        Card onStack = game.getAction().moveToStack(inHand, null);
        SpellAbility sa = onStack.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        game.getStack().add(sa);
        game.getStack().resolveStack();
        return game.getCardState(onStack);
    }

    @Test
    public void countersOnlyIfYouDidntGoFirst() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        game.setStartingPlayer(opp);
        Card second = cast(game, ai, addCardToZone("Patient Turtle", ai, ZoneType.Hand));
        AssertJUnit.assertEquals(2, second.getCounters(CounterEnumType.P1P1));

        game.setStartingPlayer(ai);
        Card first = cast(game, ai, addCardToZone("Patient Turtle", ai, ZoneType.Hand));
        AssertJUnit.assertEquals(0, first.getCounters(CounterEnumType.P1P1));
    }
}
