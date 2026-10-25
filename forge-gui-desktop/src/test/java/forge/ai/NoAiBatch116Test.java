package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 116: AlphaClearAi (Sudden Disappearance, Goblin War Cry, Eunuchs' Intrigues, Sudden Spoiling). */
public class NoAiBatch116Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private void board(Player ai, Player opp, int life) {
        for (int i = 0; i < 3; i++) {
            addCard("Grizzly Bears", ai).setSickness(false);
        }
        addCard("Hill Giant", opp);
        addCard("Hill Giant", opp);
        opp.setLife(life, null);
    }

    // Sudden Disappearance: three Bears past two Giants is 2 damage, with the Giants gone it's 6, lethal at 6
    @Test
    public void testSuddenDisappearanceForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        board(ai, opp, 6);
        SpellAbility sa = addCardToZone("Sudden Disappearance", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());

        opp.setLife(7, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Goblin War Cry: the Giant they keep blocks one Bears, so 4 gets through, lethal at 4 and not at 6
    @Test
    public void testGoblinWarCryLeavesOneBlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        board(ai, opp, 4);
        SpellAbility sa = addCardToZone("Goblin War Cry", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));

        opp.setLife(6, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Sudden Spoiling: its Bears blocked by a Hill Giant live, and the 0/2 Giant dies
    @Test
    public void testSuddenSpoilingWinsTheBlock() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Sudden Spoiling", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(bears, opp);
        combat.addBlocker(bears, giant);
        combat.setBlocked(bears, true);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
    }
}
