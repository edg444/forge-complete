package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.zone.ZoneType;

/**
 * Glimpse, the Unthinkable: "can't be chosen" (ValidCard$) for spells and abilities that say "choose", and "The name ...
 * can't be chosen" (ValidName$) for choosing a card name. Game rules (attacking, blocking) aren't choices (rulings).
 */
public class StaticAbilityCantBeChosen {

    public static boolean cantBeChosen(final Card c) {
        final Game game = c.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.CantBeChosen) && stAb.hasParam("ValidCard")
                        && stAb.matchesValidParam("ValidCard", c)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean nameCantBeChosen(final Game game, final String name) {
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.CantBeChosen) && name.equals(stAb.getParam("ValidName"))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static CardCollection filter(final CardCollection choices) {
        return new CardCollection(forge.game.card.CardLists.filter(choices, c -> !cantBeChosen(c)));
    }
}
