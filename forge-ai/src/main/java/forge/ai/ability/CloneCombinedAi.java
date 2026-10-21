package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

/** It Came from Planet Glurg: always worth becoming the copies, and the biggest creatures make the best ones. */
public class CloneCombinedAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public boolean confirmAction(final Player player, final SpellAbility sa, final PlayerActionConfirmMode mode,
            final String message, final Map<String, Object> params) {
        return true;
    }

    @Override
    protected Card chooseSingleCard(final Player ai, final SpellAbility sa, final Iterable<Card> options, final boolean isOptional,
            final Player targetedPlayer, final Map<String, Object> params) {
        return ComputerUtilCard.getBestCreatureAI(new CardCollection(options));
    }
}
