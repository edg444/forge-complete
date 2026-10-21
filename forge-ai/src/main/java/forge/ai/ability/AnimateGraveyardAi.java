package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.ability.effects.AnimateGraveyardEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Animate Graveyard: worth five mana once the graveyard makes a decent creature, and only one is needed.
 */
public class AnimateGraveyardAi extends SpellAbilityAi {

    private static final int MIN_GRAVEYARD_SIZE = 4;

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (AnimateGraveyardEffect.findGraveyardPermanent(ai) != null
                || ai.getCardsIn(ZoneType.Graveyard).size() < MIN_GRAVEYARD_SIZE) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
