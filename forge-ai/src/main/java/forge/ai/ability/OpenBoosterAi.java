package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

/**
 * Opening booster packs. The AI opens A Container of Booster Packs' container in its second main phase and keeps the
 * best card; other cards that open packs it still leaves alone, as before.
 */
public class OpenBoosterAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!sa.hasParam("Container")) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if (!ai.getGame().getPhaseHandler().is(PhaseType.MAIN2, ai)) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForMain2);
        }
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
        return ComputerUtilCard.getBestAI(new CardCollection(options));
    }
}
