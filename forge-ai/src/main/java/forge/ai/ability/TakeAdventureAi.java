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
 * Adventurer Beguiler: take the best creature card an opponent has on an adventure, else the best of its own.
 */
public class TakeAdventureAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!sa.usesTargeting()) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        sa.resetTargets();
        final CardCollection options = CardLists.getTargetableCards(ai.getGame().getCardsIn(ZoneType.Exile), sa);
        Card best = ComputerUtilCard.getBestCreatureAI(CardLists.filter(options, c -> !c.getOwner().equals(ai)));
        if (best == null) {
            best = ComputerUtilCard.getBestCreatureAI(options);
        }
        if (best == null) {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final AiAbilityDecision decision = checkApiLogic(ai, sa);
        return decision.willingToPlay() || !mandatory ? decision : new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
    }
}
