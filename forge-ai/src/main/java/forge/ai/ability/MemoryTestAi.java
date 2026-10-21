package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Memory Test: its enters trigger targets the opponent with the most cards in their library; naming is always tried.
 */
public class MemoryTestAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (sa.usesTargeting()) {
            sa.resetTargets();
            Player best = null;
            for (final Player opp : ai.getOpponents()) {
                if (sa.canTarget(opp) && (best == null
                        || opp.getCardsIn(forge.game.zone.ZoneType.Library).size() > best.getCardsIn(forge.game.zone.ZoneType.Library).size())) {
                    best = opp;
                }
            }
            if (best == null) {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
            sa.getTargets().add(best);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final AiAbilityDecision decision = checkApiLogic(ai, sa);
        return decision.willingToPlay() || !mandatory ? decision : new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
