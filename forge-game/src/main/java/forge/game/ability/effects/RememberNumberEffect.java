package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * Adds a computed number to the remembered set of the defined cards.
 * <p>
 * The remembered set is a Set, so repeats collapse on their own - which is what a card tracking
 * "which values have been marked" wants, and it keeps the mark off the counter system entirely
 * (B-I-N-G-O's chip counters sit on its printed tracker, so proliferate can't add to them and
 * nothing can remove them).
 */
public class RememberNumberEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        if (!sa.hasParam("Number")) {
            return;
        }
        final int number = AbilityUtils.calculateAmount(host, sa.getParam("Number"), sa);
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            // the game card, not a stale copy, or the mark is lost on the next state check
            final Card gameCard = host.getGame().getCardState(c, null);
            if (gameCard != null) {
                gameCard.addRemembered(number);
            }
        }
    }
}
