package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.GameEntity;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class JoinAttackAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /** It attacks someone the AI is attacking anyway: whoever has the least life, planeswalkers last. */
    @Override
    public <T extends GameEntity> T chooseSingleEntity(Player ai, SpellAbility sa, java.util.Collection<T> options,
            boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        T best = null;
        for (final T e : options) {
            if (best == null || (e instanceof Player p && (!(best instanceof Player b) || p.getLife() < b.getLife()))) {
                best = e;
            }
        }
        return best;
    }
}
