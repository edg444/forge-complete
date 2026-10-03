package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.card.CardStateName;
import forge.card.CardType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Grusilda, Monster Masher: the two best creature cards in any graveyards. A card with augment is only worth taking
 * with a host - with anything else the combined creature goes straight to the graveyard (ruling).
 */
public class CombineAi extends SpellAbilityAi {

    private static boolean isHost(final Card c) {
        return c.getOriginalState(CardStateName.Original).getType().hasSupertype(CardType.Supertype.Host);
    }

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final CardCollection candidates = CardLists.getTargetableCards(
                ai.getGame().getCardsIn(ZoneType.Graveyard), sa);
        final CardCollection augments = CardLists.filter(candidates, Card::isAugmentCard);
        final CardCollection others = CardLists.filter(candidates, c -> !c.isAugmentCard());
        final CardCollection hosts = CardLists.filter(others, CombineAi::isHost);

        sa.resetTargets();
        if (!augments.isEmpty() && !hosts.isEmpty()) {
            sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(hosts));
            sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(augments));
        } else if (others.size() >= 2) {
            final Card first = ComputerUtilCard.getBestCreatureAI(others);
            sa.getTargets().add(first);
            others.remove(first);
            sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(others));
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final AiAbilityDecision d = checkApiLogic(ai, sa);
        return mandatory && !d.willingToPlay() ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : d;
    }
}
