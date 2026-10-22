package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Backsies: every counter becoming a -1/-1 counter hurts whoever has more of them, so the AI casts it when its
 * opponents' creatures and planeswalkers have more counters than its own.
 */
public class TreatCountersAsAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        int mine = 0, theirs = 0;
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (!c.isCreature() && !c.isPlaneswalker()) {
                continue;
            }
            if (c.getController().equals(ai)) {
                mine += c.getNumAllCounters();
            } else if (ai.isOpponentOf(c.getController())) {
                theirs += c.getNumAllCounters();
            }
        }
        return theirs > mine ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
}
