package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Mysterious Hero: the AI removes the card above it only when the hero underneath is the better creature.
 */
public class SecretIdentityAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        if (host.getPaperCard() == null || !host.isCreature()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final Card hero = Card.fromPaperCard(host.getPaperCard(), ai);
        if (ComputerUtilCard.evaluateCreature(hero) > ComputerUtilCard.evaluateCreature(host)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
}
