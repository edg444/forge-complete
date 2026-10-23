package forge.game.cost;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Planeswalkerificate's loyalty costs: the enchanted creature's toughness is its loyalty, so [+N] raises it and [-N]
 * lowers it, for good. A cost that would take it below 0 can't be paid (rulings).
 */
public class CostChangeToughness extends CostPart {
    private static final long serialVersionUID = 1L;

    private final boolean raise;

    public CostChangeToughness(final String amount, final boolean raise) {
        this.setAmount(amount);
        this.raise = raise;
    }

    public boolean isRaise() {
        return raise;
    }

    @Override
    public int paymentOrder() { return 7; }

    @Override
    public Integer getMaxAmountX(final SpellAbility ability, final Player payer, final boolean effect) {
        return raise ? null : Math.max(0, ability.getHostCard().getNetToughness());
    }

    @Override
    public final String toString() {
        return (raise ? "+" : "−") + getAmount();
    }

    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        return raise || ability.getHostCard().getNetToughness() >= getAbilityAmount(ability);
    }

    @Override
    public boolean payAsDecided(final Player payer, final PaymentDecision decision, final SpellAbility ability, final boolean effect) {
        final Card host = ability.getHostCard();
        host.addPTBoost(0, raise ? decision.c : -decision.c, host.getGame().getNextTimestamp(), 0);
        host.updatePTforView();
        return true;
    }

    public <T> T accept(final ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
