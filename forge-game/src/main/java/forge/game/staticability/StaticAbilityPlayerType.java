package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Enroll in the Coalition's "You are a Flagbearer.": {@code Mode$ PlayerType | Affected$ <players> | Type$ <subtype>}.
 * The player has that subtype and nothing else - not the card type that goes with it (the card's ruling: "you're
 * not a creature").
 */
public class StaticAbilityPlayerType {

    public static boolean hasType(final Player player, final String type) {
        final Game game = player.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.PlayerType) || !type.equals(stAb.getParam("Type"))) {
                    continue;
                }
                if (stAb.matchesValidParam("Affected", player)) {
                    return true;
                }
            }
        }
        return false;
    }
}
