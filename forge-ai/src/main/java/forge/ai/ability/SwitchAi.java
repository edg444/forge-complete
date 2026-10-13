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
        if (c.isInZone(ZoneType.Stack) && !c.isCreature()) {
            return c.getCMC() * 30;
        }
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

    /**
     * Red Herring: the most valuable permanent or spell of ours an opponent's spell or ability is targeting; to be worth
     * activating, worth more than Red Herring itself.
     */
    private static Card herringTarget(final Player ai, final SpellAbility sa, final boolean worthIt) {
        final Card host = sa.getHostCard();
        final CardCollection ours = CardLists.getValidCards(ai.getGame().getCardsIn(ZoneType.Battlefield), sa.getParam("BattlefieldValid"), ai, host, sa);
        if (sa.hasParam("StackValid")) {
            ours.addAll(CardLists.getValidCards(ai.getGame().getCardsIn(ZoneType.Stack), sa.getParam("StackValid"), ai, host, sa));
        }
        Card best = null;
        int bestValue = worthIt ? value(host) : Integer.MIN_VALUE;
        for (final forge.game.spellability.SpellAbilityStackInstance si : ai.getGame().getStack()) {
            if (!ai.isOpponentOf(si.getActivatingPlayer())) {
                continue;
            }
            for (SpellAbility part = si.getSpellAbility(); part != null; part = part.getSubAbility()) {
                for (final forge.game.GameObject o : part.getTargets()) {
                    final Card c = o instanceof Card card ? card : o instanceof SpellAbility tsa && tsa.isSpell() ? tsa.getHostCard() : null;
                    if (c != null && ours.contains(c) && value(c) > bestValue) {
                        best = c;
                        bestValue = value(c);
                    }
                }
            }
        }
        return best;
    }

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if ("RedHerring".equals(sa.getParam("AILogic"))) {
            return herringTarget(ai, sa, true) != null ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                    : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
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
        if ("RedHerring".equals(sa.getParam("AILogic"))) {
            final Card target = herringTarget(ai, sa, false);
            for (final Card c : options) {
                if (c == target || c == sa.getHostCard()) {
                    return c;
                }
            }
        }
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
