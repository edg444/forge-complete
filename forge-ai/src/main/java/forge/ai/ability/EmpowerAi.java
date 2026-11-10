package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Empower Jace only ever adds loyalty to a Jace token of your own (making one first), so it's always worth doing.
 * Upstream gave the API no AI class, which fell back to CannotPlayAi: Remnants of Beleren's {T}: Empower Jace 1 was
 * never activated. Fork-only.
 */
public class EmpowerAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /** Remnants of Beleren's "instead": worth it when a real Jace planeswalker can take the counters. */
    @Override
    public boolean confirmAction(Player ai, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        final String type = sa.getParamOrDefault("Type", "Jace");
        return ai.getCardsIn(ZoneType.Battlefield).anyMatch(c -> c.isPlaneswalker() && !c.isToken() && c.getType().hasSubtype(type));
    }

    /** Which Jace gets the counters: a real planeswalker before a token, then the one closest to its ultimate. */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends GameEntity> T chooseSingleEntity(Player ai, SpellAbility sa, java.util.Collection<T> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        T best = null;
        int bestScore = Integer.MIN_VALUE;
        for (final T o : options) {
            if (!(o instanceof Card c)) {
                continue;
            }
            final int score = (c.isPlaneswalker() && !c.isToken() ? 1000 : 0) + c.getCounters(CounterEnumType.LOYALTY);
            if (score > bestScore) {
                bestScore = score;
                best = o;
            }
        }
        return best != null ? best : (T) super.chooseSingleEntity(ai, sa, options, isOptional, targetedPlayer, params);
    }
}
