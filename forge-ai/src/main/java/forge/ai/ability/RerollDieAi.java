package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Pippa, Duchess of Dice rerolling a Die creature: its own die when it shows less than average (more power), an
 * opponent's when it shows more. The face that gains the most on average is the one picked.
 */
public class RerollDieAi extends SpellAbilityAi {

    static Card bestDieToReroll(final Player ai, final Iterable<Card> dice) {
        Card best = null;
        double bestGain = 0;
        for (final Card c : dice) {
            if (!c.isDie()) {
                continue;
            }
            final double average = (c.getDieSides() + 1) / 2.0;
            final double gain = c.getController().isOpponentOf(ai) ? c.getDieFace() - average : average - c.getDieFace();
            if (gain > bestGain) {
                bestGain = gain;
                best = c;
            }
        }
        return best;
    }

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        return bestDieToReroll(ai, ai.getGame().getCardsIn(ZoneType.Battlefield)) != null
                ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    public Card chooseSingleCard(final Player ai, final SpellAbility sa, final Iterable<Card> options, final boolean isOptional,
            final Player targetedPlayer, final Map<String, Object> params) {
        final Card c = bestDieToReroll(ai, options);
        return c != null ? c : options.iterator().next();
    }
}
