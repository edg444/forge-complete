package forge.game.staticability;

import forge.game.card.Card;
import forge.game.zone.ZoneType;

/** The Kami Knight: "Creatures you control can't be goaded." Read by {@code Card.addGoad}. */
public class StaticAbilityCantBeGoaded {

    public static boolean cantBeGoaded(final Card card) {
        for (final Card ca : card.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.CantBeGoaded) && stAb.matchesValidParam("ValidCard", card)) {
                    return true;
                }
            }
        }
        return false;
    }
}
