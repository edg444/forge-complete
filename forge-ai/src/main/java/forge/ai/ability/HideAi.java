package forge.ai.ability;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.GameEntity;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;

/**
 * Entirely Normal Armchair: hiding it costs nothing and being found only sends it back to hand, so the AI sneaks it out
 * on its own turn and picks the spot at random - a choice it can't make smarter without knowing the opponent's mind.
 */
public class HideAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!ai.getGame().getPhaseHandler().is(PhaseType.MAIN2)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
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
