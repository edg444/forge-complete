package forge.ai.ability;


import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;

/**
 * Time Stop, Sundial of the Infinite, Obeka: ending the turn exiles everything on the stack and skips what's left of
 * the turn, so it's worth it when their spells and abilities on the stack add up (and none of its own are there), or
 * on their turn when the combat it's in would kill it.
 */
public class EndTurnAi extends SpellAbilityAi  {

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
    }

    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) { return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi); }

    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        if (sa.getRestrictions() != null && !sa.getRestrictions().canPlay(sa.getHostCard(), sa)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlaySa);
        }
        // Obeka: the player whose turn it is makes the call, so only on its own turn
        if (sa.hasParam("Optional") && !aiPlayer.getGame().getPhaseHandler().isPlayerTurn(aiPlayer)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        return worthEnding(aiPlayer, sa) ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
    }

    private static boolean worthEnding(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        int theirs = 0;
        for (final SpellAbilityStackInstance si : game.getStack()) {
            final SpellAbility s = si.getSpellAbility();
            if (s.getHostCard() != null && s.getHostCard().equals(sa.getHostCard())) {
                continue;
            }
            if (!s.getActivatingPlayer().isOpponentOf(ai)) {
                return false;
            }
            theirs += s.isSpell() ? 40 + s.getHostCard().getCMC() * 20 : 30;
        }
        if (theirs >= 100) {
            return true;
        }
        final Combat combat = game.getCombat();
        return combat != null && !game.getPhaseHandler().isPlayerTurn(ai)
                && game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS) && ComputerUtilCombat.lifeInDanger(ai, combat);
    }

    // Obeka activated by someone else on its turn: it ends the turn only when that's good for it
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return worthEnding(player, sa);
    }
}
