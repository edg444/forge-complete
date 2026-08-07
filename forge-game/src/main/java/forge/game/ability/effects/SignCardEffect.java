package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * Letter Bomb - physically signing the card. The mark rides on the card rather than on a state or a
 * counter, so it survives being shuffled into somebody else's library and stays visible on the card
 * that comes back out.
 */
public class SignCardEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getHostCard() + " is signed.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            final Card gameCard = host.getGame().getCardState(c, null);
            if (gameCard != null) {
                gameCard.setSigned(true);
            }
        }
    }
}
