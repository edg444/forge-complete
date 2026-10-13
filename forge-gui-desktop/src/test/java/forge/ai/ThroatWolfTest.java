package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Throat Wolf (CMB1 #65), checked against Scryfall and its 7 rulings 2026-10-13: castable during an opponent's combat;
 * firstest strike deals damage in a step before first strike's; after each opponent's first combat phase its
 * controller has a combat phase (as the active player, though it's still the opponent's turn) where only it attacks.
 */
public class ThroatWolfTest extends AITest {

    private static void damageSteps(final Game game, final Combat combat) {
        combat.orderBlockersForDamageAssignment();
        combat.orderAttackersForDamageAssignment();
        if (combat.hasFirstestStrikers()) {
            combat.assignFirstestStrikeCombatDamage();
            combat.dealAssignedDamage();
            game.getAction().checkStateEffects(true);
        }
        combat.removeAbsentCombatants();
        if (combat.assignCombatDamage(true)) {
            combat.dealAssignedDamage();
            game.getAction().checkStateEffects(true);
        }
        combat.removeAbsentCombatants();
        combat.assignCombatDamage(false);
        combat.dealAssignedDamage();
        game.getAction().checkStateEffects(true);
    }

    @Test
    public void firstestStrikeBeatsFirstStrike() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card knight = addCard("White Knight", opp);
        knight.setSickness(false);
        Card wolf = addCard("Throat Wolf", ai);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp);

        Combat combat = new Combat(opp);
        game.getPhaseHandler().setCombat(combat);
        combat.addAttacker(knight, ai);
        combat.addBlocker(knight, wolf);
        damageSteps(game, combat);

        AssertJUnit.assertFalse(knight.isInPlay());
        AssertJUnit.assertTrue(wolf.isInPlay());
    }

    @Test
    public void firstestStrikeDealsDamageOnlyOnce() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card wurm = addCard("Craw Wurm", opp);
        wurm.setSickness(false);
        Card wolf = addCard("Throat Wolf", ai);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp);

        Combat combat = new Combat(opp);
        game.getPhaseHandler().setCombat(combat);
        combat.addAttacker(wurm, ai);
        combat.addBlocker(wurm, wolf);
        damageSteps(game, combat);

        AssertJUnit.assertEquals("3 in the firstest step, none later", 3, wurm.getDamage());
        AssertJUnit.assertFalse(wolf.isInPlay());
    }

    @Test
    public void itsControllerGetsACombatAfterTheOpponentsFirst() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card wolf = addCard("Throat Wolf", ai);
        wolf.setSickness(false);
        Card bears = addCard("Grizzly Bears", ai);
        bears.setSickness(false);
        fillLibrary(opp, 5);
        fillLibrary(ai, 5);
        PhaseHandler ph = game.getPhaseHandler();
        ph.devModeSet(PhaseType.MAIN1, opp);

        boolean sawOwnCombat = false;
        int combatsBegun = 0;
        PhaseType last = null;
        Player lastTurn = null;
        for (int i = 0; i < 400 && !game.isGameOver(); i++) {
            ph.mainLoopStep();
            if (ph.is(PhaseType.COMBAT_BEGIN) && (last != PhaseType.COMBAT_BEGIN || lastTurn != ph.getPlayerTurn())) {
                combatsBegun++;
            }
            if (ph.is(PhaseType.COMBAT_BEGIN) && ph.getPlayerTurn() == ai) {
                sawOwnCombat = true;
                AssertJUnit.assertEquals("still the opponent's turn", opp, ph.getActualTurnPlayer());
                AssertJUnit.assertTrue(CombatUtil.canAttack(wolf));
                AssertJUnit.assertFalse("only Throat Wolf attacks", CombatUtil.canAttack(bears));
            }
            if (ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS) && ph.getPlayerTurn() == ai) {
                AssertJUnit.assertTrue("the AI attacks with it", ph.getCombat().isAttacking(wolf));
                AssertJUnit.assertFalse(ph.getCombat().isAttacking(bears));
            }
            last = ph.getPhase();
            lastTurn = ph.getPlayerTurn();
            if (ph.is(PhaseType.MAIN2) && ph.getPlayerTurn() == opp) {
                break;
            }
        }
        AssertJUnit.assertTrue(sawOwnCombat);
        AssertJUnit.assertEquals("the opponent's and then the Wolf's", 2, combatsBegun);
        AssertJUnit.assertEquals(opp, ph.getPlayerTurn());
        AssertJUnit.assertNull(ph.getOnlyAttacker());
    }

    @Test
    public void castableDuringAnOpponentsCombat() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCards("Mountain", 3, ai);
        Card wolf = addCardToZone("Throat Wolf", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        wolf.getFirstSpellAbility().setActivatingPlayer(ai);
        AssertJUnit.assertFalse(wolf.getFirstSpellAbility().canCastTiming(ai));
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opp);
        AssertJUnit.assertTrue(wolf.getFirstSpellAbility().canCastTiming(ai));
    }
}
