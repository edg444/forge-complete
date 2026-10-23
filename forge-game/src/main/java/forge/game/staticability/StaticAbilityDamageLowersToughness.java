package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

/**
 * Planeswalkerificate: "Damage lowers toughness. Toughness doesn't heal at end of turn." (ValidCard$) Damage is still
 * dealt; whenever state-based actions are checked, what's marked on the card becomes a lasting toughness loss.
 */
public class StaticAbilityDamageLowersToughness {

    public static boolean applies(final Card card) {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.DamageLowersToughness) && stAb.matchesValidParam("ValidCard", card)) {
                    return true;
                }
            }
        }
        return false;
    }
}
