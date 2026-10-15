package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

/**
 * Bear with Set's Mechanic (CMB1 #71), checked against Scryfall and its 3 rulings 2026-10-14: aggressive gives one
 * more combat phase as your first ends, if you control a creature with it then, and only those creatures attack in it.
 */
public class AggressiveTest extends AITest {

    /** Runs the AI's turn from main 1 to main 2, returning how many combat phases began. */
    private static int combatsUntilMain2(final Game game, final Player ai, final Runnable inAggressiveCombat) {
        PhaseHandler ph = game.getPhaseHandler();
        int combats = 0;
        PhaseType last = null;
        for (int i = 0; i < 400 && !game.isGameOver(); i++) {
            ph.mainLoopStep();
            if (ph.is(PhaseType.COMBAT_BEGIN) && last != PhaseType.COMBAT_BEGIN) {
                combats++;
                if (combats == 2) {
                    AssertJUnit.assertTrue(ph.isAggressiveCombat());
                    inAggressiveCombat.run();
                }
            }
            last = ph.getPhase();
            if (ph.is(PhaseType.MAIN2) && ph.getPlayerTurn() == ai) {
                break;
            }
        }
        AssertJUnit.assertFalse(ph.isAggressiveCombat());
        return combats;
    }

    @Test
    public void onlyAggressiveCreaturesAttackInTheExtraCombat() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bear = addCard("Bear with Set's Mechanic", ai);
        bear.setSickness(false);
        Card angel = addCard("Serra Angel", ai);
        angel.setSickness(false);
        fillLibrary(opp, 5);
        fillLibrary(ai, 5);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        int combats = combatsUntilMain2(game, ai, () -> {
            AssertJUnit.assertTrue(CombatUtil.canAttack(bear));
            AssertJUnit.assertFalse("vigilance, but no aggressive", CombatUtil.canAttack(angel));
        });
        AssertJUnit.assertEquals(2, combats);
        AssertJUnit.assertEquals("6 in the first combat, 2 in the aggressive one", 12, opp.getLife());
    }

    @Test
    public void oneExtraCombatHoweverManyHaveIt() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Bear with Set's Mechanic", ai).setSickness(false);
        addCard("Bear with Set's Mechanic", ai).setSickness(false);
        fillLibrary(opp, 5);
        fillLibrary(ai, 5);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(2, combatsUntilMain2(game, ai, () -> { }));
        AssertJUnit.assertEquals(12, opp.getLife());
    }

    @Test
    public void noAggressiveCreatureNoExtraCombat() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Grizzly Bears", ai).setSickness(false);
        fillLibrary(opp, 5);
        fillLibrary(ai, 5);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(1, combatsUntilMain2(game, ai, () -> { }));
        AssertJUnit.assertEquals(18, opp.getLife());
    }
}
