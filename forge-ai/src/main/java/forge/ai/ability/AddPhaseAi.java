package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class AddPhaseAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        // Untap, Upkeep, Draw: an extra draw step is a card, an extra untap step is worth it with things to untap
        if ("ExtraStep".equals(sa.getParam("AILogic"))) {
            final PhaseType extra = PhaseType.smartValueOf(sa.getParam("ExtraPhase"));
            if (extra == PhaseType.DRAW && aiPlayer.getCardsIn(ZoneType.Library).size() > 3) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            if (extra == PhaseType.UNTAP && CardLists.count(aiPlayer.getCardsIn(ZoneType.Battlefield), CardPredicates.TAPPED) >= 2) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

}
