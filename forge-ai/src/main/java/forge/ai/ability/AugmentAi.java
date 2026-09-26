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

import java.util.Map;

public class AugmentAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        // the combined creature is controlled by the host's controller, so only ever augment our own hosts
        CardCollection hosts = CardLists.filter(CardLists.getTargetableCards(ai.getCardsIn(ZoneType.Battlefield), sa),
                c -> c.getController().equals(ai));
        if (hosts.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        // Teacher's Pet: the sacrifice is wasted if the search can't find anything
        if (sa.hasParam("ChangeType") && CardLists.getValidCards(ai.getCardsIn(ZoneType.Library),
                sa.getParam("ChangeType"), ai, sa.getHostCard(), sa).isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final Card best = ComputerUtilCard.getBestCreatureAI(hosts);
        sa.resetTargets();
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // Teacher's Pet's library search: always take one - the host is already chosen and the cost paid
    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        return ComputerUtilCard.getBestAI(options);
    }
}
