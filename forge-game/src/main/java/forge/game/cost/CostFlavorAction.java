package forge.game.cost;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * A cost representing a real-world, unenforceable action described in the card's own cost text
 * (e.g. Mesa Chicken's "Stand up, Flap your arms, Cluck like a chicken"). Mechanically free - a
 * human player is asked to confirm they did the action via a custom-labeled Yes/No prompt; the
 * AI always "pays" it trivially, since it can't perform physical actions and the mechanic is
 * unenforceable anyway.
 */
public class CostFlavorAction extends CostPart {
    private static final long serialVersionUID = 1L;

    private final String yesButtonText;

    public CostFlavorAction(final String description, final String yesButtonText) {
        super("1", "FlavorAction", description);
        this.yesButtonText = yesButtonText;
    }

    public String getYesButtonText() {
        return yesButtonText;
    }

    @Override
    public boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        return true;
    }

    @Override
    public int paymentOrder() {
        return 22;
    }

    // stands for the host's chosen word (Magic Word): "the chosen word" in rules text, the word itself when paying
    private static final String CHOSEN_WORD = "CHOSENWORD";

    @Override
    public final String toString() {
        return getTypeDescription().replace(CHOSEN_WORD, "the chosen word");
    }

    /** What the payer is asked to do right now - with the actual chosen word, where the text names one. */
    public final String getPrompt(final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        if (host != null && host.hasChosenType()) {
            return getTypeDescription().replace(CHOSEN_WORD, "\"" + host.getChosenType() + "\"");
        }
        return toString();
    }

    @Override
    public boolean payAsDecided(Player payer, PaymentDecision pd, SpellAbility sa, final boolean effect) {
        return true;
    }

    @Override
    public boolean isReusable() {
        return true;
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
