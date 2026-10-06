package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

/**
 * The Grand Calcutron: a card that would be put into the hand of a player whose hand is a program goes wherever in
 * the program that player chooses instead (see GameAction.programPosition).
 */
public class StaticAbilityPlaceInProgram {

    public static boolean anyActive(final Game game) {
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.PlaceInProgram)) {
                    return true;
                }
            }
        }
        return false;
    }
}
