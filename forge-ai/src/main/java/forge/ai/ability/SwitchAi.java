package forge.ai.ability;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Mary O'Kill: the switched-in card keeps the old one's place, so the trade is the difference in what the two cards are
 * worth on the battlefield - upgrading our own Killbot to Mary, or handing an opponent our weakest Killbot for their
 * Mary (whose card then sits in our hand). Done at instant speed, after blockers on our turn or at an opponent's end
 * step, when the surprise matters least to get wrong.
 */
public class SwitchAi extends SpellAbilityAi {

    private static final int MIN_GAIN = 30;

    private static int value(final Card c) {
        return c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ComputerUtilCard.evaluatePermanentList(new CardCollection(c));
    }

    /** The best (hand card, permanent) pair and how much the switch gains us, or null if nothing is worth it. */
    private static Pair<Card, Card> bestSwitch(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final CardCollection inHand = CardLists.getValidCards(ai.getCardsIn(ZoneType.Hand), sa.getParam("HandValid"), ai, host, sa);
        final CardCollection inPlay = CardLists.getValidCards(ai.getGame().getCardsIn(ZoneType.Battlefield), sa.getParam("BattlefieldValid"), ai, host, sa);
        Pair<Card, Card> best = null;
        int bestGain = MIN_GAIN;
        for (final Card h : inHand) {
            for (final Card b : inPlay) {
                final int gain = ai.isOpponentOf(b.getController()) ? value(b) - value(h) : value(h) - value(b);
                if (gain > bestGain) {
                    bestGain = gain;
                    best = Pair.of(h, b);
                }
            }
        }
        return best;
    }

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final boolean goodTime = ph.isPlayerTurn(ai) ? ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS) : ph.is(PhaseType.END_OF_TURN);
        if (!goodTime) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        if (bestSwitch(ai, sa) == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected Card chooseSingleCard(final Player ai, final SpellAbility sa, final Iterable<Card> options, final boolean isOptional,
            final Player targetedPlayer, final Map<String, Object> params) {
        final Pair<Card, Card> best = bestSwitch(ai, sa);
        Card first = null;
        for (final Card c : options) {
            if (first == null) {
                first = c;
            }
            if (best != null && (c == best.getLeft() || c == best.getRight())) {
                return c;
            }
        }
        return first;
    }
}
