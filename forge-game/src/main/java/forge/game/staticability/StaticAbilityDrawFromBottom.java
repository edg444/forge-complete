package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class StaticAbilityDrawFromBottom {

    public static boolean drawsFromBottom(final Player player) {
        return applies(player, StaticAbilityMode.DrawFromBottom);
    }

    /** Heart of a Duelist: "You may draw cards from anywhere in your library." */
    public static boolean drawsFromAnywhere(final Player player) {
        return applies(player, StaticAbilityMode.DrawFromAnywhere);
    }

    private static boolean applies(final Player player, final StaticAbilityMode mode) {
        final Game game = player.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(mode)) {
                    continue;
                }
                if (stAb.matchesValidParam("ValidPlayer", player)) {
                    return true;
                }
            }
        }
        return false;
    }
}
