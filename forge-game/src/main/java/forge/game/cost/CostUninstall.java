package forge.game.cost;

import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Socketed Sprocketer's "Uninstall a 6 from this creature": take an installed die result off the source as a cost,
 * so it can't also be used for a die roll in response.
 */
public class CostUninstall extends CostPart {
    private static final long serialVersionUID = 1L;

    private final int result;

    public CostUninstall(final String result) {
        super("1", "Card.Self", null);
        this.result = Integer.parseInt(result);
    }

    public int getResult() {
        return result;
    }

    @Override
    public boolean isUndoable() {
        return true;
    }

    @Override
    public boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        return ability.getHostCard().getInstalledResults().contains(result);
    }

    @Override
    public String toString() {
        return "Uninstall a " + result + " from this creature";
    }

    @Override
    public boolean payAsDecided(final Player payer, final PaymentDecision pd, final SpellAbility sa, final boolean effect) {
        return sa.getHostCard().uninstallResult(result);
    }

    @Override
    public void refund(final forge.game.card.Card source) {
        source.installResult(result);
    }

    @Override
    public <T> T accept(final ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
