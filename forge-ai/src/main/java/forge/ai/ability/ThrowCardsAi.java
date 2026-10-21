package forge.ai.ability;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Knife and Death: worth throwing only with something of the opponents' to aim at. Each throw aims at the best opposing
 * creature or planeswalker small enough to fall to a few hits, else the best one there is.
 */
public class ThrowCardsAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final List<Card> aimable = CardLists.getValidCards(ai.getGame().getCardsIn(ZoneType.Battlefield),
                sa.getParam("Aim"), ai, sa.getHostCard(), sa);
        return aimable.isEmpty() ? new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards)
                : new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public <T extends GameEntity> T chooseSingleEntity(final Player ai, final SpellAbility sa, final Collection<T> options,
            final boolean isOptional, final Player targetedPlayer, final Map<String, Object> params) {
        final List<Card> cards = Lists.newArrayList();
        for (final T o : options) {
            if (o instanceof Card c) {
                cards.add(c);
            }
        }
        final List<Card> killable = CardLists.filter(cards, c -> c.isPlaneswalker() ? c.getCurrentLoyalty() <= 4
                : c.getNetToughness() - c.getDamage() <= 4);
        final Card best = ComputerUtilCard.getBestAI(killable.isEmpty() ? cards : killable);
        @SuppressWarnings("unchecked")
        final T chosen = (T) best;
        return chosen;
    }
}
