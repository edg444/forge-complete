package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Mobile Clone: a photocopy of the best creature it can target - anyone's, since the copy is its own either way.
 */
public class PhotocopyAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        sa.resetTargets();
        final CardCollection options = CardLists.getTargetableCards(
                CardLists.filter(ai.getGame().getCardsIn(ZoneType.Battlefield), Card::isCreature), sa);
        final Card best = ComputerUtilCard.getBestCreatureAI(options);
        if (best == null) {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
