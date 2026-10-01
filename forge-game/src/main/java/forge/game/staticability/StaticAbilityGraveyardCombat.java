package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

/**
 * Unstable's Over My Dead Bodies: creature cards in graveyards can attack and block as though they were on the
 * battlefield. While declaring, they're put onto the battlefield unnoticed and stay listed in their graveyard
 * (see GameAction.enlistGraveyardCombatants); they go back once they're out of combat.
 */
public class StaticAbilityGraveyardCombat {

    public static boolean anyActive(final Game game) {
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.GraveyardCombat)) {
                    return true;
                }
            }
        }
        return false;
    }
}
