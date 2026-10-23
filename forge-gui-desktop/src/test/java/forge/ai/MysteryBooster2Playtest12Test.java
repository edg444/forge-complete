package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Mystery Booster 2 playtest cards (MB2), part 12: Runed Terror, checked against Scryfall 2026-10-23 with its rulings.
 */
public class MysteryBooster2Playtest12Test extends AITest {

    private static void stepUntil(final Game game, final PhaseType phase, final Player active) {
        final PhaseHandler ph = game.getPhaseHandler();
        int guard = 0;
        do {
            ph.mainLoopStep();
            AssertJUnit.assertTrue("ran away", ++guard < 2000);
        } while (!game.isGameOver() && !(ph.is(phase) && ph.getPlayerTurn().equals(active)));
    }

    /** The phases (by their first step) and their active players, in order, for the next n phase changes. */
    private static List<String> phaseOrder(final Game game, final int n) {
        final PhaseHandler ph = game.getPhaseHandler();
        final List<String> seen = Lists.newArrayList();
        String last = ph.getPlayerTurn().getName() + " " + ph.getPhase();
        int guard = 0;
        while (seen.size() < n && !game.isGameOver()) {
            ph.mainLoopStep();
            AssertJUnit.assertTrue("ran away", ++guard < 4000);
            final PhaseType p = ph.getPhase();
            if (p != PhaseType.UNTAP && p != PhaseType.MAIN1 && p != PhaseType.COMBAT_BEGIN && p != PhaseType.MAIN2
                    && p != PhaseType.END_OF_TURN) {
                continue;
            }
            final String now = ph.getPlayerTurn().getName() + " " + p;
            if (!now.equals(last)) {
                seen.add(now);
                last = now;
            }
        }
        return seen;
    }

    @Test
    public void testRunedTerrorPhasesInSequence() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        fillLibrary(opp, 20);
        fillLibrary(me, 20);
        addCard("Runed Terror", me);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, me);
        game.getAction().checkStateEffects(true);
        final int myTurns = me.getTurn();
        final int oppTurns = opp.getTurn();

        final String m = me.getName() + " ";
        final String o = opp.getName() + " ";
        AssertJUnit.assertEquals(List.of(o + "MAIN1", m + "COMBAT_BEGIN", o + "COMBAT_BEGIN", m + "MAIN2", o + "MAIN2",
                m + "END_OF_TURN", o + "END_OF_TURN", m + "UNTAP", o + "UNTAP", m + "MAIN1", o + "MAIN1"), phaseOrder(game, 11));
        AssertJUnit.assertTrue(game.getPhaseHandler().isSequentialPhases());
        AssertJUnit.assertEquals("their turn began when they became active, and again after their ending phase", oppTurns + 2, opp.getTurn());
        AssertJUnit.assertEquals(myTurns + 1, me.getTurn());
    }

    @Test
    public void testRunedTerrorEndOfTurnEffectsAndLeaving() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        fillLibrary(opp, 20);
        fillLibrary(me, 20);
        final Card terror = addCard("Runed Terror", me);
        final Card bears = addCard("Grizzly Bears", me);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, me);
        game.getAction().checkStateEffects(true);

        // a +3/+3 begun while they're the active player lasts until the end of their turn, not mine
        stepUntil(game, PhaseType.MAIN2, opp);
        final SpellAbility pump = AbilityFactory.getAbility("SP$ Pump | Defined$ Self | NumAtt$ +3 | NumDef$ +3", bears);
        pump.setActivatingPlayer(opp);
        AbilityUtils.resolve(pump);
        AssertJUnit.assertEquals(5, bears.getNetPower());
        stepUntil(game, PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertEquals("my cleanup came and went", 5, bears.getNetPower());
        stepUntil(game, PhaseType.UNTAP, me);
        AssertJUnit.assertEquals("gone in their cleanup", 2, bears.getNetPower());

        // gone in my draw step: I keep going with my turn, then they take a turn as normal
        stepUntil(game, PhaseType.DRAW, me);
        game.getAction().exile(terror, null, null);
        stepUntil(game, PhaseType.MAIN1, me);
        stepUntil(game, PhaseType.END_OF_TURN, me);
        stepUntil(game, PhaseType.UNTAP, opp);
        AssertJUnit.assertFalse(game.getPhaseHandler().isSequentialPhases());
        final List<String> after = phaseOrder(game, 3);
        AssertJUnit.assertEquals(List.of(opp.getName() + " MAIN1", opp.getName() + " COMBAT_BEGIN", opp.getName() + " MAIN2"), after);
    }
}
