package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

/**
 * Uh, Squen &amp; Cragg Made a Card: +1/+1 counters give -1/-1 and -1/-1 counters give +1/+1 on ValidCard$ (any
 * permanent by default). More than one doesn't turn them back around - they all say the same thing.
 */
public class StaticAbilityInvertPTCounters {

    public static boolean inverted(final Card card) {
        final Game game = card.getGame();
        if (game == null) {
            return false;
        }
        for (final Card source : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : source.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.InvertPTCounters) && stAb.matchesValidParam("ValidCard", card)) {
                    return true;
                }
            }
        }
        return false;
    }
}
