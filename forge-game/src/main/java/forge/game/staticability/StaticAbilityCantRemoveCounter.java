package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Quest Compleated Beast: counters of CounterType$ can't be removed from ValidPlayer$ ("can't remove poison counters").
 */
public class StaticAbilityCantRemoveCounter {

    public static boolean applies(final Player player, final CounterType type) {
        final Game game = player.getGame();
        for (final Card source : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : source.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CantRemoveCounter) || !stAb.matchesValidParam("ValidPlayer", player)) {
                    continue;
                }
                if (!stAb.hasParam("CounterType") || CounterType.getType(stAb.getParam("CounterType")).equals(type)) {
                    return true;
                }
            }
        }
        return false;
    }
}
