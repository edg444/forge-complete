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

/** Second pass, batch 119: AILogic$ CombatTrick (Fog Patch, Feint, Angelic Favor). */
public class NoAiBatch119Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static Combat attack(Game game, Player attacker, Card att, Player defender, Card blocker, PhaseType phase) {
        game.getPhaseHandler().devModeSet(phase, attacker, false, 1);
        Combat combat = new Combat(attacker);
        combat.addAttacker(att, defender);
        if (blocker != null) {
            combat.addBlocker(att, blocker);
        }
        combat.setBlocked(att, blocker != null);
        game.getPhaseHandler().setCombat(combat);
        return combat;
    }

    // Fog Patch: their unblocked Craw Wurm's 6 damage gets blocked away
    @Test
    public void testFogPatchStopsTheUnblocked() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Fog Patch", ai, ZoneType.Hand).getFirstSpellAbility();
        attack(game, opp, wurm, ai, null, PhaseType.COMBAT_DECLARE_BLOCKERS);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Feint: its Bears chump-blocking a Craw Wurm live; a Serra Angel trading with it is left to trade
    @Test
    public void testFeintSavesItsBlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", ai);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Feint", ai, ZoneType.Hand).getFirstSpellAbility();
        attack(game, opp, wurm, ai, bears, PhaseType.COMBAT_DECLARE_BLOCKERS);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getTargetCard());

        Card angel = addCard("Serra Angel", ai);
        attack(game, opp, wurm, ai, angel, PhaseType.COMBAT_DECLARE_BLOCKERS);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Angelic Favor: a 4/4 flier to block and kill their attacking Hill Giant, cast before blocks
    @Test
    public void testAngelicFavorMakesABlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Plains", ai);
        }
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Angelic Favor", ai, ZoneType.Hand).getFirstSpellAbility();
        attack(game, opp, giant, ai, null, PhaseType.COMBAT_DECLARE_ATTACKERS);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
