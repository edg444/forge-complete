package forge.ai.ability;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.GameEntity;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;

/**
 * Looking for a hidden Entirely Normal Armchair. The AI can read the whole game, so it must not use that here: it points
 * at a spot chosen uniformly at random, once each turn (the ability's limit), as a person who hadn't seen it would.
 */
public class SeekHiddenAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public <T extends GameEntity> T chooseSingleEntity(final Player ai, final SpellAbility sa, final java.util.Collection<T> options,
            final boolean isOptional, final Player targetedPlayer, final Map<String, Object> params) {
        final List<T> spots = Lists.newArrayList(options);
        return spots.isEmpty() ? null : Aggregates.random(spots);
    }

    @Override
    public boolean confirmAction(final Player ai, final SpellAbility sa, final PlayerActionConfirmMode mode, final String message,
            final Map<String, Object> params) {
        // found it: back to their hand it goes
        return true;
    }
}
