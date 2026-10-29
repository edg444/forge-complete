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

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

import java.util.Map;

/**
 * The Class CostTap.
 */
public class CostTap extends CostPart {

    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new cost tap.
     */
    public CostTap() {
    }

    public int paymentOrder() { return -1; }

    @Override
    public boolean isUndoable() { return true; }

    @Override
    public boolean isReusable() { return true; }

    @Override
    public boolean isRenewable() { return true; }

    @Override
    public String toString() {
        return "{T}";
    }

    /** A payment decision to spend one of the payer's {T}s (Sole Performer) rather than tap the permanent. */
    public static final int SPEND_TAP_SYMBOL = 2;

    private transient Player spentFrom = null;

    @Override
    public void refund(final Card source) {
        if (spentFrom != null) {
            spentFrom.addTapSymbols(1);
            spentFrom = null;
            return;
        }
        source.setTapped(false);
    }

    @Override
    public boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        final Card source = ability.getHostCard();
        // a {T} from the pool pays for a permanent that's tapped already, but never for a summoning-sick one (rulings)
        return (source.canTap() || payer.getTapSymbols() > 0) && !source.isAbilitySick();
    }

    @Override
    public boolean payAsDecided(Player payer, PaymentDecision decision, SpellAbility ability, final boolean effect) {
        Card hostCard = ability.getHostCard();
        spentFrom = null;
        if ((decision != null && decision.c == SPEND_TAP_SYMBOL || !hostCard.canTap()) && payer.spendTapSymbol()) {
            spentFrom = payer;
            payer.getGame().getGameLog().add(forge.game.GameLogEntryType.MANA, payer + " spends {T} for " + hostCard
                    + " rather than tapping it.");
            return true;
        }
        if (hostCard.tap(ability, payer)) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Cards, new CardCollection(hostCard));
            payer.getGame().getTriggerHandler().runTrigger(TriggerType.TapAll, runParams, false);
        }
        return true;
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
