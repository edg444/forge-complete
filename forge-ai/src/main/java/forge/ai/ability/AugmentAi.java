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

public class AugmentAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        // the combined creature is controlled by the host's controller, so only ever augment our own hosts
        CardCollection hosts = CardLists.filter(CardLists.getTargetableCards(ai.getCardsIn(ZoneType.Battlefield), sa),
                c -> c.getController().equals(ai));
        if (hosts.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final Card best = ComputerUtilCard.getBestCreatureAI(hosts);
        sa.resetTargets();
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
