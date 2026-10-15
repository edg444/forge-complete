package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

public class RevealUntilStopAi extends RevealAiBase {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!revealHandTargetAI(ai, sa, false)) {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (revealHandTargetAI(ai, sa, mandatory)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    /**
     * Whether to stop on the card just revealed. The hand's order is hidden, so this is the secretary problem:
     * let the first n/e cards go by, then stop on the first nonland better than all of them.
     */
    @Override
    public boolean confirmAction(Player ai, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        if (params == null || !(params.get("Revealed") instanceof CardCollection revealed)) {
            return true;
        }
        final int handSize = (Integer) params.get("HandSize");
        final int skip = (int) Math.floor(handSize / Math.E);
        if (revealed.size() <= skip) {
            return false;
        }
        final Card current = revealed.getLast();
        if (current.isLand()) {
            return false;
        }
        final int value = ComputerUtilCard.evaluateCardImpact(current);
        for (int i = 0; i < revealed.size() - 1; i++) {
            if (ComputerUtilCard.evaluateCardImpact(revealed.get(i)) >= value) {
                return false;
            }
        }
        return true;
    }
}
