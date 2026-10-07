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
 * Phoebe, Head of S.N.E.A.K.: steal from the opposing creature whose text box is worth the most - the one that
 * loses the most value without its abilities - and never from our own.
 */
public class StealTextBoxAi extends SpellAbilityAi {

    private static boolean hasTextBox(final Card c) {
        return !c.isTextBoxStolen() && (!c.getKeywords().isEmpty() || !c.getTriggers().isEmpty()
                || !c.getStaticAbilities().isEmpty() || c.getSpellAbilities().anyMatch(s -> !s.isSpell()));
    }

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final CardCollection theirs = CardLists.filter(
                CardLists.getTargetableCards(ai.getGame().getCardsIn(ZoneType.Battlefield), sa),
                c -> c.getController().isOpponentOf(ai) && hasTextBox(c));
        if (theirs.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(theirs));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final AiAbilityDecision d = checkApiLogic(ai, sa);
        return mandatory && !d.willingToPlay() ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : d;
    }
}
