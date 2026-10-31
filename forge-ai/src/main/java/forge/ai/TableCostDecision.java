package forge.ai;

import java.util.function.Predicate;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostExile;
import forge.game.cost.CostPartWithList;
import forge.game.cost.CostPutCardToLib;
import forge.game.cost.CostReturn;
import forge.game.cost.CostSacrifice;
import forge.game.cost.CostTapType;
import forge.game.cost.PaymentDecision;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Paying as the table shows it was paid: the cards the player actually moved or tapped. The rest goes as the AI would. */
final class TableCostDecision extends AiCostDecision {
    private final TableJudge link;
    private final Player payer;
    private final SpellAbility ability;

    TableCostDecision(final Player payer0, final SpellAbility sa, final boolean effect, final TableJudge link0) {
        super(payer0, sa, effect);
        payer = payer0;
        ability = sa;
        link = link0;
    }

    /** The cards of that zone the player paid with, or null if the table doesn't show enough of them to go by. */
    private PaymentDecision paidWith(final CostPartWithList cost, final Iterable<Card> from, final Predicate<Card> paid) {
        final int amount = cost.getAbilityAmount(ability);
        final CardCollection cards = new CardCollection();
        for (final Card c : from) {
            if (paid.test(c)) {
                cards.add(c);
            }
        }
        if (cards.size() != amount) {
            return null;
        }
        final String[] valid = cost.getType().split(";");
        for (final Card c : cards) {
            if (!c.isValid(valid, payer, source, ability)) {
                link.judge().illegalChoice(payer, ability, c);
            }
        }
        return PaymentDecision.card(cards);
    }

    @Override
    public PaymentDecision visit(final CostSacrifice cost) {
        if (cost.payCostFromSource()) {
            return super.visit(cost);
        }
        final PaymentDecision paid = paidWith(cost, payer.getCardsIn(ZoneType.Battlefield), c -> link.zoneOnTable(c) != ZoneType.Battlefield);
        return paid != null ? paid : super.visit(cost);
    }

    @Override
    public PaymentDecision visit(final CostDiscard cost) {
        if (cost.payCostFromSource() || "Hand".equals(cost.getType()) || "Random".equals(cost.getType())) {
            return super.visit(cost);
        }
        final PaymentDecision paid = paidWith(cost, payer.getCardsIn(ZoneType.Hand), c -> link.zoneOnTable(c) != ZoneType.Hand);
        return paid != null ? paid : super.visit(cost);
    }

    @Override
    public PaymentDecision visit(final CostExile cost) {
        if (cost.payCostFromSource()) {
            return super.visit(cost);
        }
        final PaymentDecision paid = paidWith(cost, payer.getCardsIn(cost.getFrom().get(0)), c -> link.zoneOnTable(c) == ZoneType.Exile);
        return paid != null ? paid : super.visit(cost);
    }

    @Override
    public PaymentDecision visit(final CostReturn cost) {
        if (cost.payCostFromSource()) {
            return super.visit(cost);
        }
        final PaymentDecision paid = paidWith(cost, payer.getCardsIn(ZoneType.Battlefield), c -> link.zoneOnTable(c) == ZoneType.Hand);
        return paid != null ? paid : super.visit(cost);
    }

    @Override
    public PaymentDecision visit(final CostPutCardToLib cost) {
        if (cost.payCostFromSource()) {
            return super.visit(cost);
        }
        final PaymentDecision paid = paidWith(cost, payer.getCardsIn(cost.getFrom()), c -> link.zoneOnTable(c) == ZoneType.Library);
        return paid != null ? paid : super.visit(cost);
    }

    @Override
    public PaymentDecision visit(final CostTapType cost) {
        final CardCollection tapped = link.touched(payer.getCardsIn(ZoneType.Battlefield));
        tapped.removeIf(Card::isTapped);
        final PaymentDecision paid = paidWith(cost, tapped, c -> true);
        return paid != null ? paid : super.visit(cost);
    }
}
