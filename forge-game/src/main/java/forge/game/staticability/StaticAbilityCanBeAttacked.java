package forge.game.staticability;

import forge.game.card.Card;
import forge.game.zone.ZoneType;

/**
 * Creatures that can be attacked directly, like a planeswalker: the Hero's Path Heads, Revelers and Xenagos Ascended
 * ("You can attack Heads directly with your creatures"). Unlike Tasty, the attacked creature deals no combat damage
 * back to the creatures attacking it (user ruling 2026-10-30).
 */
public class StaticAbilityCanBeAttacked {

    public static boolean canBeAttacked(final Card c) {
        if (c == null || c.getGame() == null || !c.isCreature() || !c.isInPlay()) {
            return false;
        }
        for (final Card ca : c.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.CanBeAttacked) && stAb.matchesValidParam("ValidCard", c)) {
                    return true;
                }
            }
        }
        return false;
    }
}
