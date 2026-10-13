package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

/** Queue of Beetles: while one is out, the bottom object of the stack resolves instead of the top (its ruling). */
public class StaticAbilityStackFirstInFirstOut {

    public static boolean applies(final Game game) {
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.StackFirstInFirstOut)) {
                    return true;
                }
            }
        }
        return false;
    }
}
