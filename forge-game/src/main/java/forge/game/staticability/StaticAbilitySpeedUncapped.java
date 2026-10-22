package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Gomif, Fast Racer: ValidPlayer$'s speed can increase beyond 4. They're still at max speed at 4 or greater.
 */
public class StaticAbilitySpeedUncapped {

    public static boolean uncapped(final Player player) {
        final Game game = player.getGame();
        for (final Card source : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : source.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.SpeedUncapped) && stAb.matchesValidParam("ValidPlayer", player)) {
                    return true;
                }
            }
        }
        return false;
    }
}
