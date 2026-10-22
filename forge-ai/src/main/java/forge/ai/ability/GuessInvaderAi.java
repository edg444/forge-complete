package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Syx, Robot Overlord: the AI calls the creature it most wants gone an invader, as long as losing 3 life for a wrong
 * guess doesn't put it in danger.
 */
public class GuessInvaderAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public AiAbilityDecision chkDrawback(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected Card chooseSingleCard(final Player ai, final SpellAbility sa, final Iterable<Card> options,
            final boolean isOptional, final Player targetedPlayer, final Map<String, Object> params) {
        if (ai.getLife() <= 6 || !ai.canLoseLife()) {
            return null;
        }
        return ComputerUtilCard.getBestCreatureAI(new CardCollection(options));
    }
}
