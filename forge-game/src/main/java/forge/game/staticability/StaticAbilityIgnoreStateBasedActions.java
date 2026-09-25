package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class StaticAbilityIgnoreStateBasedActions {

    public static boolean ignoresStateBasedActions(final Card card) {
        return anyApplies(card.getGame(), "ValidCard", card);
    }

    public static boolean ignoresStateBasedActions(final Player player) {
        return anyApplies(player.getGame(), "ValidPlayer", player);
    }

    private static boolean anyApplies(final Game game, final String param, final Object affected) {
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.IgnoreStateBasedActions)) {
                    continue;
                }
                // matchesValidParam treats a missing param as "matches everything", so a card-only
                // ability must not silently protect players too (and vice versa)
                if (stAb.hasParam(param) && stAb.matchesValidParam(param, affected)) {
                    return true;
                }
            }
        }
        return false;
    }
}
