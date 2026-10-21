package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Truss, Chief Engineer: before combat on its turn, the AI adds its hack counters to the power of its best attacker, or
 * failing that takes them off the power of an opponent's best creature (AdjustNumberEffect makes that choice).
 */
public class AdjustNumberAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!ai.getGame().getPhaseHandler().is(PhaseType.MAIN1, ai)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        if (ComputerUtilCost.setMaxXValue(sa, ai, false) < 1) {
            return new AiAbilityDecision(0, AiPlayDecision.CantAffordX);
        }
        sa.resetTargets();
        final CardCollection attackers = CardLists.filter(ai.getCreaturesInPlay(), c -> sa.canTarget(c) && CombatUtil.canAttack(c));
        if (!attackers.isEmpty()) {
            sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(attackers));
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        final CardCollection theirs = CardLists.filter(ai.getGame().getCardsIn(ZoneType.Battlefield),
                c -> c.isCreature() && c.getController().isOpponentOf(ai) && sa.canTarget(c));
        if (theirs.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(theirs));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
