package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

/**
 * Staying Power: "until end of turn" and "this turn" effects don't end.
 * <p>
 * Forge ends those effects by running the commands registered for the end of the turn, so the
 * effect is implemented by simply not running them - everything registered stays registered and
 * will be executed on the first turn after Staying Power has left, which is the same thing that
 * happens in paper when the enchantment goes away.
 */
public class StaticAbilityStayingPower {

    public static boolean anyStayingPower(final Game game) {
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.StayingPower)) {
                    return true;
                }
            }
        }
        return false;
    }
}
