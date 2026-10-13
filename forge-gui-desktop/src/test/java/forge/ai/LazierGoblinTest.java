package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Lazier Goblin (CMB1 #56), checked against Scryfall and its 2 rulings 2026-10-13: motivate is a sorcery-speed special
 * action; once paid, the creature stays motivated for that player while it remains on the battlefield, and a new
 * controller has to motivate it before it can attack or block.
 */
public class LazierGoblinTest extends AITest {

    private static SpellAbility motivate(final Card c, final Player p) {
        for (final SpellAbility sa : c.getAllPossibleAbilities(p, true)) {
            if (sa.getApi() == ApiType.Motivate) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void motivatingLetsItAttackAndBlock() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCards("Mountain", 4, ai);
        Card goblin = addCard("Lazier Goblin", ai);
        goblin.setSickness(false);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(goblin.getAbilityText(), goblin.getAbilityText().contains("Motivate {3}{R}"));
        AssertJUnit.assertFalse(CombatUtil.canAttack(goblin));
        AssertJUnit.assertFalse(CombatUtil.canBlock(goblin));

        SpellAbility pay = motivate(goblin, ai);
        AssertJUnit.assertNotNull(pay);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, pay, null));
        AssertJUnit.assertTrue("special action: no stack", game.getStack().isEmpty());
        AssertJUnit.assertTrue(CombatUtil.canAttack(goblin));
        AssertJUnit.assertTrue(CombatUtil.canBlock(goblin));
        AssertJUnit.assertNull("only paid once", motivate(goblin, ai));

        // a new controller has to motivate it; getting it back, it's still motivated for you
        goblin.setController(opp, game.getNextTimestamp());
        AssertJUnit.assertFalse(CombatUtil.canBlock(goblin));
        goblin.setController(ai, game.getNextTimestamp());
        AssertJUnit.assertTrue(CombatUtil.canBlock(goblin));

        // a new object is unmotivated
        Card back = game.getAction().moveToPlay(game.getAction().moveToHand(goblin, null), null, null);
        back.setSickness(false);
        AssertJUnit.assertFalse(CombatUtil.canBlock(back));
    }

    @Test
    public void onlyAsASorcery() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCards("Mountain", 4, ai);
        Card goblin = addCard("Lazier Goblin", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertNull("not on an opponent's turn", motivate(goblin, ai));
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_BEGIN, ai);
        AssertJUnit.assertNull("not outside a main phase", motivate(goblin, ai));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotNull(motivate(goblin, ai));
    }

    @Test
    public void aiMotivatesBeforeCombatWhenItCanAttack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCards("Mountain", 4, ai);
        Card goblin = addCard("Lazier Goblin", ai);
        goblin.setSickness(false);
        SpellAbility pay = motivate(goblin, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay,
                ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(pay));
    }
}
