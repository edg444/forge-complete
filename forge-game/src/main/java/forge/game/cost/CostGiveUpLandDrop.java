package forge.game.cost;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Boulder Jockey's {D}: give up one of your remaining land drops this turn. It's an additional cost, not mana; you
 * can't give up a land drop you don't have, nor pay it on another player's turn (rulings).
 */
public class CostGiveUpLandDrop extends CostPart {
    private static final long serialVersionUID = 1L;

    public CostGiveUpLandDrop(final String amount) {
        this.setAmount(amount);
    }

    @Override
    public int paymentOrder() { return 7; }

    @Override
    public final String toString() {
        final int n = Integer.parseInt(getAmount());
        return n == 1 ? "Give up a land drop" : "Give up " + n + " land drops";
    }

    @Override
    public final void refund(final Card source) {
        source.getController().addLandDropsGivenUp(-Integer.parseInt(getAmount()));
    }

    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        return payer.getGame().getPhaseHandler().isPlayerTurn(payer)
                && payer.getMaxLandPlays() - payer.getLandsPlayedThisTurn() - payer.getLandDropsGivenUp() >= getAbilityAmount(ability);
    }

    @Override
    public boolean payAsDecided(final Player payer, final PaymentDecision decision, final SpellAbility ability, final boolean effect) {
        payer.addLandDropsGivenUp(decision.c);
        return true;
    }

    public <T> T accept(final ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
