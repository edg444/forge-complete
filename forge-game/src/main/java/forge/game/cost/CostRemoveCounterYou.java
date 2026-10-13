package forge.game.cost;

import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Removing counters from yourself as a cost: Blood Poet's spark abilities ("−3:" spends three of your spark counters).
 * Energy has its own cost (CostPayEnergy), which the AI's energy handling is built around.
 */
public class CostRemoveCounterYou extends CostPart {

    private static final long serialVersionUID = 1L;

    private final CounterType type;

    public CostRemoveCounterYou(final String amount, final CounterType type) {
        this.setAmount(amount);
        this.type = type;
    }

    @Override
    public int paymentOrder() { return 7; }

    public CounterType getCounter() {
        return type;
    }

    @Override
    public Integer getMaxAmountX(final SpellAbility ability, final Player payer, final boolean effect) {
        return payer.getCounters(type);
    }

    @Override
    public final String toString() {
        return "Remove " + Cost.convertAmountTypeToWords(convertAmount(), getAmount(), type.getName() + " counter") + " from yourself";
    }

    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        return payer.canRemoveCounters(type) && payer.getCounters(type) >= getAbilityAmount(ability);
    }

    @Override
    public boolean payAsDecided(Player payer, PaymentDecision decision, SpellAbility sa, final boolean effect) {
        payer.subtractCounter(type, decision.c, payer);
        return true;
    }

    @Override
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
