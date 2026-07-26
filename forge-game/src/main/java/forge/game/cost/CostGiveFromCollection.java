package forge.game.cost;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Give an opponent a card from your real-world collection (Collector Protector).
 * <p>
 * Unlike a wish effect, this reaches past the sideboard to the whole card pool - the card being
 * handed over is one you own in real life, not one you registered for the match. The chosen card is
 * created in the receiving opponent's sideboard, i.e. it becomes a card they own outside the game
 * and could later wish for, which is as close as Forge gets to it changing hands for real.
 */
public class CostGiveFromCollection extends CostPart {
    private static final long serialVersionUID = 1L;

    public CostGiveFromCollection(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    @Override
    public int paymentOrder() {
        return 15;
    }

    @Override
    public boolean isReusable() {
        return true;
    }

    @Override
    public boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        // a collection isn't a game zone - there's always another card in the shoebox
        return !payer.getOpponents().isEmpty();
    }

    @Override
    public boolean payAsDecided(final Player payer, final PaymentDecision decision, final SpellAbility ability,
            final boolean effect) {
        if (decision == null || decision.cards == null) {
            return true;
        }
        for (final Card c : decision.cards) {
            payer.getGame().getAction().moveTo(ZoneType.Sideboard, c, ability, null);
        }
        return true;
    }

    @Override
    public String toString() {
        return "Give an opponent a " + getTypeDescription() + " you own from outside the game";
    }

    @Override
    public <T> T accept(final ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
