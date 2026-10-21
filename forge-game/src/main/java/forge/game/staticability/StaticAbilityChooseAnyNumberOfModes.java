package forge.game.staticability;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Far Out: rather than choose the indicated number of modes for spells and abilities they control, a ValidPlayer$ may
 * choose one or more different modes. Read by CharmEffect when the modes are chosen.
 */
public class StaticAbilityChooseAnyNumberOfModes {

    public static boolean applies(final Player controller) {
        for (final Card ca : controller.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.ChooseAnyNumberOfModes)
                        && stAb.matchesValidParam("ValidPlayer", controller)) {
                    return true;
                }
            }
        }
        return false;
    }
}
