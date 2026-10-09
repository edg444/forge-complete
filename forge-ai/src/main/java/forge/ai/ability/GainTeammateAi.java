package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Better Than One. A second head draws a card and plays a land every turn, but each card handed over is one the AI
 * won't draw or play itself, and the team loses if either library runs out - so it's worth it with a deep library:
 * half of that goes across, with half the lands in hand so the newcomer can cast what they draw, and the permanents
 * stay put.
 */
public class GainTeammateAi extends SpellAbilityAi {

    private static final int MIN_LIBRARY = 20;

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (ai.getCardsIn(ZoneType.Library).size() < MIN_LIBRARY) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final AiAbilityDecision d = checkApiLogic(ai, sa);
        return mandatory && !d.willingToPlay() ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : d;
    }

    @Override
    public int chooseNumber(final Player ai, final SpellAbility sa, final int min, final int max, final Map<String, Object> params) {
        return Math.max(min, max / 2);
    }

    @Override
    protected Card chooseSingleCard(final Player ai, final SpellAbility sa, final Iterable<Card> options, final boolean isOptional,
            final Player targetedPlayer, final Map<String, Object> params) {
        if (params == null || params.get("Zone") != ZoneType.Hand) {
            return null;
        }
        final int landsInHand = CardLists.count(ai.getCardsIn(ZoneType.Hand), CardPredicates.LANDS);
        final int landsLeft = CardLists.count(options, CardPredicates.LANDS);
        if (landsInHand - landsLeft >= landsInHand / 2) {
            return null;
        }
        for (final Card c : options) {
            if (c.isLand()) {
                return c;
            }
        }
        return null;
    }
}
