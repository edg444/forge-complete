package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

/**
 * Claire D'Loon, Joy Sculptor: tokens (ValidCard$) become cards that are still tokens. They go on existing after they
 * leave the battlefield, in whatever zone they go to (rulings), so neither the state-based action that takes tokens out
 * of the game (CR 704.5d) nor the rule that a token can't move again once it has left the battlefield (CR 111.8) applies.
 */
public class StaticAbilityTokensAreCards {

    public static boolean applies(final Card token) {
        final Game game = token.getGame();
        if (game == null || !token.isToken()) {
            return false;
        }
        for (final Card source : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : source.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.TokensAreCards) && stAb.matchesValidParam("ValidCard", token)) {
                    return true;
                }
            }
        }
        return false;
    }
}
