/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.cost;

import org.apache.commons.lang3.StringUtils;

import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantGainLosePayLife;

/**
 * The Class CostPayLife.
 */
public class CostPayLife extends CostPart {
    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new cost pay life.
     *
     * @param amount
     *            the amount
     */
    /** When true the amount counts halves, so 1 means half a life (Necro-Impotence). */
    private final boolean halves;

    public CostPayLife(final String amount, final String description) {
        this(amount, description, false);
    }

    public CostPayLife(final String amount, final String description, final boolean halves) {
        super(amount, "card", description);
        this.halves = halves;
    }

    public boolean isHalves() {
        return halves;
    }

    /** Renders an amount counted in halves the way the card prints it: 1 becomes "½", 3 becomes "1½". */
    public static String halvesLabel(final int amount) {
        final int whole = amount / 2;
        if (amount % 2 == 0) {
            return String.valueOf(whole);
        }
        return whole == 0 ? "½" : whole + "½";
    }

    @Override
    public int paymentOrder() { return 7; }

    /*
     * (non-Javadoc)
     *
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Pay ");
        String desc = this.getTypeDescription();
        if (desc != null) {
            sb.append(desc);
        } else if (halves && StringUtils.isNumeric(this.getAmount())) {
            sb.append(halvesLabel(Integer.parseInt(this.getAmount()))).append(" life");
        } else {
            sb.append(this.getAmount()).append(" life");
        }
        return sb.toString();
    }

    @Override
    public Integer getMaxAmountX(SpellAbility ability, Player payer, final boolean effect) {
        if (!payer.canPayLife(1, effect, ability)) {
            return 0;
        }
        return halves ? payer.getLife() * 2 + payer.getHalfLife() : payer.getLife();
    }

    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        if (halves) {
            final int need = this.getAbilityAmount(ability);
            if (need <= 0) {
                return true;
            }
            // half a life is still life, so a player at 1/2 can pay it and a player at 0 cannot
            if (payer.getLife() * 2 + payer.getHalfLife() < need) {
                return false;
            }
            return !StaticAbilityCantGainLosePayLife.anyCantPayLife(payer, effect, ability);
        }
        if (!payer.canPayLife(this.getAbilityAmount(ability), effect, ability)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean payAsDecided(Player ai, PaymentDecision decision, SpellAbility ability, final boolean effect) {
        if (halves) {
            return ai.changeLifeByHalves(-decision.c, ability.getHostCard(), ability);
        }
        return ai.payLife(decision.c, ability, effect);
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
