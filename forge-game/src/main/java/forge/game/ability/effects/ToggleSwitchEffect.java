package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * Togglodyte's ON/OFF switch. Kept as a flag on the card rather than a counter so the detail panel
 * can say which way it is set - a counter can only show that it is on, never that it is off.
 */
public class ToggleSwitchEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Toggle " + sa.getHostCard() + "'s ON/OFF switch.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            final Card gameCard = host.getGame().getCardState(c, null);
            if (gameCard == null) {
                continue;
            }
            if (sa.hasParam("On")) {
                gameCard.setSwitchedOn("True".equalsIgnoreCase(sa.getParam("On")));
            } else {
                gameCard.setSwitchedOn(!gameCard.isSwitchedOn());
            }
        }
    }
}
