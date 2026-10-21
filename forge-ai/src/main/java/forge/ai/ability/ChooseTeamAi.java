package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.ability.effects.ChooseTeamEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Unknown Event's team spells. With AILogic$ Pump (Unclaimed Blessing) the target depends on the team: a Mirran helps
 * its own best creature, a Phyrexian shrinks an opponent's. An AI that hasn't joined a team yet joins one now - the
 * same random pick it would make on resolution - so the target can fit it.
 */
public class ChooseTeamAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!sa.usesTargeting()) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        final String team = ChooseTeamEffect.chooseIfNeeded(ai, sa);
        sa.resetTargets();
        final boolean mirran = ChooseTeamEffect.MIRRAN.equals(team);
        final CardCollection options = CardLists.getTargetableCards(
                mirran ? ai.getCreaturesInPlay() : ai.getOpponents().getCreaturesInPlay(), sa);
        if (options.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        Card best = ComputerUtilCard.getBestCreatureAI(options);
        if (!mirran) {
            // a -1/-1 counter kills a 1-toughness creature outright; otherwise deathtouch on their best isn't worth it
            final CardCollection dies = CardLists.filter(options, c -> c.getNetToughness() <= 1);
            if (dies.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            best = ComputerUtilCard.getBestCreatureAI(dies);
        }
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final AiAbilityDecision d = checkApiLogic(ai, sa);
        if (d.willingToPlay() || !mandatory || !sa.usesTargeting()) {
            return d;
        }
        final CardCollection any = CardLists.getTargetableCards(ai.getGame().getCardsIn(ZoneType.Battlefield), sa);
        if (any.isEmpty()) {
            return d;
        }
        sa.getTargets().add(any.getFirst());
        return new AiAbilityDecision(50, AiPlayDecision.WillPlay);
    }
}
