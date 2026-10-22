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
 * Takesies: worth it when opponents' permanents have counters to take; they go onto the AI's best creature.
 */
public class MoveCounterFromEachAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final CardCollection theirs = CardLists.filter(ai.getGame().getCardsIn(ZoneType.Battlefield),
                c -> ai.isOpponentOf(c.getController()) && c.hasCounters());
        if (theirs.isEmpty() || !sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        Card best = ComputerUtilCard.getBestCreatureAI(CardLists.getTargetableCards(ai.getCreaturesInPlay(), sa));
        if (best == null) {
            best = ComputerUtilCard.getBestAI(CardLists.getTargetableCards(ai.getCardsIn(ZoneType.Battlefield), sa));
        }
        if (best == null) {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
