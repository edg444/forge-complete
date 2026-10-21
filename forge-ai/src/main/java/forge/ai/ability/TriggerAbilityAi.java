package forge.ai.ability;

import java.util.List;
import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.ability.effects.TriggerAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;

/**
 * Trigger Happy: retrigger one of its own permanents' enters-the-battlefield abilities - the kind that does something
 * on its own, wherever it is - choosing the best such permanent; otherwise it waits.
 */
public class TriggerAbilityAi extends SpellAbilityAi {

    private static boolean hasEtb(final Card c) {
        for (final Trigger t : TriggerAbilityEffect.triggersOf(c)) {
            if (t.getMode() == TriggerType.ChangesZone && "Battlefield".equals(t.getParam("Destination"))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        sa.resetTargets();
        final CardCollection mine = CardLists.filter(CardLists.getTargetableCards(ai.getCardsIn(forge.game.zone.ZoneType.Battlefield), sa),
                TriggerAbilityAi::hasEtb);
        final Card best = ComputerUtilCard.getBestAI(mine);
        if (best == null) {
            return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
        }
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public SpellAbility chooseSingleSpellAbility(final Player player, final SpellAbility sa, final List<SpellAbility> spells,
            final Map<String, Object> params) {
        for (final SpellAbility s : spells) {
            if (s.getTrigger() != null && s.getTrigger().getMode() == TriggerType.ChangesZone) {
                return s;
            }
        }
        return spells.get(0);
    }
}
