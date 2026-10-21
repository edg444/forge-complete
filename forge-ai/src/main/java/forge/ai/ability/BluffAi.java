package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Blufferfish: a coin flip's chance at +2/+0 and evasion, worth trying once before attacking with it - in its first
 * main phase, if it can attack this turn.
 */
public class BluffAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final Card fish = sa.getHostCard();
        if (!ai.getGame().getPhaseHandler().is(PhaseType.MAIN1, ai) || sa.getActivationsThisTurn() > 0) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        if (!CombatUtil.canAttack(fish)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
