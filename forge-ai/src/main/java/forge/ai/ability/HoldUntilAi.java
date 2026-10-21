package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.PausableUntil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Grand Marshal Macie: worth tapping her in the end step when the AI has an effect of its own about to end.
 */
public class HoldUntilAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!ai.getGame().getPhaseHandler().is(PhaseType.END_OF_TURN)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        for (final PausableUntil e : ai.getGame().getPausableUntils()) {
            if (ai.equals(e.getController()) && !e.isCurse()) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
    }
}
