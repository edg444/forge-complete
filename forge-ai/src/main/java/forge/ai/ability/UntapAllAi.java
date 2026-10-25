package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class UntapAllAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        final Card source = sa.getHostCard();

        final AbilitySub abSub = sa.getSubAbility();
        final PhaseHandler ph = source.getGame().getPhaseHandler();
        if (abSub != null && ApiType.AddPhase == abSub.getApi() && ph.getPhase().isBefore(PhaseType.COMBAT_END)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        CardCollectionView list = CardLists.filter(aiPlayer.getGame().getCardsIn(ZoneType.Battlefield), CardPredicates.TAPPED);
        final String valid = sa.getParamOrDefault("ValidCards", "");
        list = CardLists.getValidCards(list, valid, source.getController(), source, sa);
        // don't untap if only opponent benefits
        final CardCollectionView mine = CardLists.filter(list, CardPredicates.isControlledByAnyOf(aiPlayer.getYourTeam()));
        if (mine.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if (abSub != null && ApiType.AddPhase == abSub.getApi()) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        if (mine.allMatch(Card::isCreature)) {
            // Battle Cry: untapping creatures pays off after they've attacked, as blockers for the opponent's turn
            if (!ph.isPlayerTurn(aiPlayer) || !(ph.is(PhaseType.MAIN2) || ph.is(PhaseType.COMBAT_END))) {
                return new AiAbilityDecision(0, AiPlayDecision.WaitForMain2);
            }
            return mine.size() >= 2 || abSub != null ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                    : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return abSub != null ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        Card source = sa.getHostCard();

        if (sa.hasParam("ValidCards")) {
            String valid = sa.getParam("ValidCards");
            CardCollectionView list = CardLists.filter(aiPlayer.getGame().getCardsIn(ZoneType.Battlefield), CardPredicates.TAPPED);
            list = CardLists.getValidCards(list, valid, source.getController(), source, sa);
            return (mandatory || !list.isEmpty()) ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        return mandatory ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
}
