package forge.ai.ability;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.GameEntity;
import forge.game.ability.effects.HidePrintedTokenEffect;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;

/**
 * Surprise Party's hidden printed tokens: the AI hides one at a random spot in its second main phase, a few in all
 * (user, 2026-10-21: it hides and seeks). Where they are is a guess for the opponent, so random is the fair choice.
 */
public class HidePrintedTokenAi extends SpellAbilityAi {

    private static final int MAX_HIDDEN = 3;

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!ai.getGame().getPhaseHandler().is(PhaseType.MAIN2, ai)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        if (ai.getHiddenPrintedTokens().size() >= MAX_HIDDEN || HidePrintedTokenEffect.freeSpots(ai).isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public <T extends GameEntity> T chooseSingleEntity(final Player ai, final SpellAbility sa, final java.util.Collection<T> options,
            final boolean isOptional, final Player targetedPlayer, final Map<String, Object> params) {
        final List<T> spots = Lists.newArrayList(options);
        return spots.isEmpty() ? null : Aggregates.random(spots);
    }
}
