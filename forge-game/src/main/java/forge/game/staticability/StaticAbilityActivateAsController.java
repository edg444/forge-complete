package forge.game.staticability;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The Fearsome Flock: "You may activate abilities of artifacts your opponents control as though you control them."
 * Read by {@code SpellAbilityRestriction.checkActivatorRestrictions}, so it lifts only who may activate them.
 */
public class StaticAbilityActivateAsController {

    public static boolean canActivate(final Card card, final Player activator) {
        for (final Card ca : card.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.ActivateAsController)
                        && stAb.matchesValidParam("ValidCard", card) && stAb.matchesValidParam("Activator", activator)) {
                    return true;
                }
            }
        }
        return false;
    }
}
