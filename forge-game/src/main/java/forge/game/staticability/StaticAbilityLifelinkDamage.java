package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

/**
 * Syr Cadian's knightlifelink: damage dealt by a ValidSource$ also causes its controller to gain that much life, as
 * lifelink would, without the source having lifelink (rulings: the same as granting lifelink, so a source that
 * already has it gains no extra life).
 */
public class StaticAbilityLifelinkDamage {

    public static boolean isLifelinkDamage(final Card source) {
        final Game game = source.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.LifelinkDamage)
                        && stAb.matchesValidParam("ValidSource", source)) {
                    return true;
                }
            }
        }
        return false;
    }
}
