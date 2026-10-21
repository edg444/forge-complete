package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

/**
 * Jetpack Janitor (UNF #15), checked against Scryfall 2026-10-21 (no rulings): alpha strike gives first strike while
 * it's blocking or blocked by a creature whose name comes after "Jetpack Janitor" alphabetically.
 */
public class JetpackJanitorTest extends AITest {

    private boolean firstStrikeAttacking(final String blockerName) {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card janitor = addCard("Jetpack Janitor", p);
        janitor.setSickness(false);
        final Card blocker = addCard(blockerName, opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("not in combat", janitor.hasKeyword(Keyword.FIRST_STRIKE));

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, p, false);
        final Combat combat = new Combat(p);
        combat.addAttacker(janitor, opp);
        combat.addBlocker(janitor, blocker);
        combat.setBlocked(janitor, true);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
        return janitor.hasKeyword(Keyword.FIRST_STRIKE);
    }

    @Test
    public void testBlockedByALaterName() {
        AssertJUnit.assertTrue(firstStrikeAttacking("Llanowar Elves"));
    }

    @Test
    public void testBlockedByAnEarlierName() {
        AssertJUnit.assertFalse(firstStrikeAttacking("Grizzly Bears"));
    }

    @Test
    public void testBlockingALaterName() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card janitor = addCard("Jetpack Janitor", p);
        final Card attacker = addCard("Serra Angel", opp);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false);
        final Combat combat = new Combat(opp);
        combat.addAttacker(attacker, p);
        combat.addBlocker(attacker, janitor);
        combat.setBlocked(attacker, true);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(janitor.hasKeyword(Keyword.FIRST_STRIKE));
    }
}
