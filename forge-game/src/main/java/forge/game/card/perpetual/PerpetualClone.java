package forge.game.card.perpetual;

import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.spellability.SpellAbility;

/**
 * A card that becomes another card and stays it in every zone (Standard Procedure: "becomes that card until end of
 * turn", which its rulings say isn't a copy effect and survives being cast or shuffled away). A zone change rebuilds
 * the card from its printing, so the clone is laid on again each time; the duration that ends it removes this.
 */
public record PerpetualClone(long timestamp, Card source, SpellAbility sa) implements PerpetualInterface {
    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void applyEffect(Card c) {
        c.addCloneState(CardFactory.getCloneStates(source, c, sa), timestamp);
    }
}
