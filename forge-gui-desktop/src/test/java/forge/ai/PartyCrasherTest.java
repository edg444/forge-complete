package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.util.collect.FCollectionView;

/**
 * Party Crasher (Unstable): "Haste / You can attack with this creature once each combat during each opponent's turn."
 * - Oracle text, both rulings and the Unstable FAQ entry checked 2026-10-02. It's declared after the active player's
 * attackers, the player it attacks blocks after the others, an untapped attacking creature can block it (and it can
 * block back), and two such creatures deal combat damage to each other only once. In multiplayer it can attack any of
 * its controller's opponents (user, 2026-10-02).
 */
public class PartyCrasherTest extends AITest {

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Card crasher = addCard("Party Crasher", game.getPlayers().get(0));
        AssertJUnit.assertEquals("Haste | You can attack with this creature once each combat during each opponent's turn.",
                String.join(" | ", crasher.getAbilityText().trim().split("[\\r\\n]+")));
    }

    @Test
    public void testCrashesTheActivePlayersCombat() {
        Game game = initAndCreateGame();
        Player active = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card crasher = addCard("Party Crasher", opp);
        // "Whenever you attack, target attacking Goblin you control gets +1/+0" - its controller attacks too
        addCard("Boggart Prankster", opp);
        game.getAction().checkStateEffects(true);

        playUntilPhase(game, PhaseType.COMBAT_DECLARE_BLOCKERS);
        Combat combat = game.getCombat();
        AssertJUnit.assertNotNull(combat);
        AssertJUnit.assertEquals(active, combat.getAttackingPlayer());
        AssertJUnit.assertTrue(combat.isAttacking(crasher, active));
        AssertJUnit.assertTrue(crasher.isTapped());

        playUntilPhase(game, PhaseType.MAIN2);
        AssertJUnit.assertEquals(16, active.getLife());
        AssertJUnit.assertEquals(1, opp.getCreaturesAttackedThisTurn().size());

        // that attack was this turn's, not one of the controller's own turn
        playUntilNextTurn(game);
        AssertJUnit.assertEquals(opp, game.getPhaseHandler().getPlayerTurn());
        AssertJUnit.assertTrue(opp.getCreaturesAttackedThisTurn().isEmpty());
    }

    @Test
    public void testAttacksAnyOpponentInMultiplayer() {
        Game game = initAndCreateThreePlayerGame();
        Player crashersOwner = game.getPlayers().get(0);
        Player active = game.getPlayers().get(1);
        Player third = game.getPlayers().get(2);
        Card crasher = addCard("Party Crasher", crashersOwner);
        third.setLife(5, null);
        game.getAction().checkStateEffects(true);

        playUntilPhase(game, PhaseType.COMBAT_DECLARE_BLOCKERS);
        // the AI goes after the opponent with the least life, here not the one whose turn it is
        AssertJUnit.assertTrue(game.getCombat().isAttacking(crasher, third));
        playUntilPhase(game, PhaseType.MAIN2);
        AssertJUnit.assertEquals(2, third.getLife());
        AssertJUnit.assertEquals(20, active.getLife());
    }

    @Test
    public void testAttackersBlockingEachOtherDealDamageOnce() {
        Game game = initAndCreateGame();
        Player active = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        // 4/4 vigilance, no flying: untapped while attacking, and the Crasher can block it
        Card spider = addCard("Sentinel Spider", active);
        Card crasher = addCard("Party Crasher", opp);
        game.getAction().checkStateEffects(true);

        Combat combat = new Combat(active);
        game.getPhaseHandler().setCombat(combat);
        combat.addAttacker(spider, opp);
        combat.addDefender(active);
        combat.addAttacker(crasher, active);

        // the untapped attacking Spider can block the Crasher, and the untapped Crasher can block back
        AssertJUnit.assertTrue(CombatUtil.canBlock(crasher, spider, combat));
        combat.addBlocker(crasher, spider);
        AssertJUnit.assertTrue(CombatUtil.canBlock(spider, crasher, combat));
        combat.addBlocker(spider, crasher);
        AssertJUnit.assertTrue(combat.isAttacking(spider) && combat.isBlocking(spider));

        combat.orderBlockersForDamageAssignment();
        combat.orderAttackersForDamageAssignment();
        combat.assignCombatDamage(false);
        combat.dealAssignedDamage();
        game.getAction().checkStateEffects(true);

        // 3 to the Spider, not 6; 4 to the Crasher
        AssertJUnit.assertEquals(3, spider.getDamage());
        AssertJUnit.assertTrue(spider.isInPlay());
        AssertJUnit.assertFalse(crasher.isInPlay());
    }

    @Test
    public void testAiStaysHomeWhenItShouldBlock() {
        Game game = initAndCreateGame();
        Player active = game.getPlayers().get(1);
        Player ai = game.getPlayers().get(0);
        Card crasher = addCard("Party Crasher", ai);
        Card bears = addCard("Grizzly Bears", active);
        Card angel = addCard("Serra Angel", active);
        Card wurm = addCard("Craw Wurm", active);
        Card drake = addCard("Wind Drake", active);
        game.getAction().checkStateEffects(true);
        FCollectionView<GameEntity> defenders = CombatUtil.getAllPossibleDefenders(ai);

        // Grizzly Bears attacking: the Crasher would rather block and kill it
        Combat combat = new Combat(active);
        game.getPhaseHandler().setCombat(combat);
        combat.addAttacker(bears, ai);
        bears.setTapped(true);
        AssertJUnit.assertNull(AiAttackController.chooseAttackDuringOpponentsTurn(ai, crasher, defenders, combat));

        // a flier it can't block, and nothing left to block it: it attacks
        combat = new Combat(active);
        game.getPhaseHandler().setCombat(combat);
        combat.addAttacker(drake, ai);
        drake.setTapped(true);
        wurm.setTapped(true);
        angel.setTapped(true);
        AssertJUnit.assertEquals(active, AiAttackController.chooseAttackDuringOpponentsTurn(ai, crasher, defenders, combat));

        // the vigilant Angel stays untapped and would block and kill it
        combat = new Combat(active);
        game.getPhaseHandler().setCombat(combat);
        combat.addAttacker(wurm, ai);
        angel.setTapped(false);
        AssertJUnit.assertNull(AiAttackController.chooseAttackDuringOpponentsTurn(ai, crasher, defenders, combat));

        // facing lethal, it stays home
        angel.setTapped(true);
        ai.setLife(5, null);
        AssertJUnit.assertNull(AiAttackController.chooseAttackDuringOpponentsTurn(ai, crasher, defenders, combat));
    }
}
