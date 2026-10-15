package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * Bucket List: put a counter over each type on the host's tracker list that the defined cards have and that's still
 * showing (its ruling: a spell with more than one of them covers each). The tracker list isn't the object, so these
 * aren't counters on anything - nothing can proliferate or remove them.
 */
public class TrackerCoverEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getDescription();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            for (final String type : host.getTrackerTypes()) {
                if (c.getType().hasStringType(type)) {
                    host.coverTrackerType(type);
                }
            }
        }
    }
}
