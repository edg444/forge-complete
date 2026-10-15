package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class UpgradeAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /** The covered artifact's own text is ignored, so give up the least: a token if there is one, else the weakest. */
    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional,
            Player targetedPlayer, Map<String, Object> params) {
        Card worst = null;
        int worstValue = Integer.MAX_VALUE;
        for (final Card c : options) {
            final int value = (c.isToken() ? 0 : 1000) + ComputerUtilCard.evaluateCardImpact(c);
            if (value < worstValue) {
                worstValue = value;
                worst = c;
            }
        }
        return worst;
    }

    /** Whether a card with upgrade would cover something rather than be exiled. */
    public static boolean hasArtifactToCover(Player ai, Card upgrading) {
        return CardLists.count(ai.getCardsIn(ZoneType.Battlefield), c -> c.isArtifact() && !c.equals(upgrading)) > 0;
    }
}
