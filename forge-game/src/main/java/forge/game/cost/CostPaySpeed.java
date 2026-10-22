package forge.game.cost;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Chicago Loop: "Pay X speed" - its payer's speed goes down by that much (to as low as no speed at all).
 */
public class CostPaySpeed extends CostPart {
    private static final long serialVersionUID = 1L;

    int paidAmount = 0;

    public CostPaySpeed(final String amount) {
        this.setAmount(amount);
    }

    @Override
    public int paymentOrder() { return 7; }

    @Override
    public Integer getMaxAmountX(final SpellAbility ability, final Player payer, final boolean effect) {
        return payer.getSpeed();
    }

    @Override
    public final String toString() {
        return "Pay " + getAmount() + " speed";
    }

    @Override
    public final void refund(final Card source) {
        source.getController().paySpeed(-paidAmount);
    }

    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        return payer.getSpeed() >= this.getAbilityAmount(ability);
    }

    @Override
    public boolean payAsDecided(Player payer, PaymentDecision decision, SpellAbility ability, final boolean effect) {
        paidAmount = decision.c;
        payer.paySpeed(paidAmount);
        return true;
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
