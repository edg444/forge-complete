package forge.game.staticability;

import forge.card.GamePieceType;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

/**
 * Intangible Vibes: "All creatures are tokens" (ValidCard$). A matching card on the battlefield is a token while the
 * effect lasts, kept in step after the continuous effects. One that leaves the battlefield as a token goes to its new
 * zone - so "dies" still triggers - and then ceases to exist like any token outside the battlefield (rulings).
 */
public class StaticAbilityAreTokens {

    public static void sync(final Game game) {
        boolean any = false;
        for (final Card source : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : source.getStaticAbilities()) {
                any |= stAb.checkConditions(StaticAbilityMode.AreTokens);
            }
        }
        for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
            final boolean wanted = any && isTokenByEffect(c);
            if (wanted && c.getGamePieceType() == GamePieceType.CARD) {
                c.setGamePieceType(GamePieceType.TOKEN);
                c.setTokenByEffect(true);
            } else if (!wanted && c.isTokenByEffect()) {
                c.setGamePieceType(GamePieceType.CARD);
                c.setTokenByEffect(false);
            }
        }
    }

    private static boolean isTokenByEffect(final Card c) {
        for (final Card source : c.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : source.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.AreTokens) && stAb.matchesValidParam("ValidCard", c)) {
                    return true;
                }
            }
        }
        return false;
    }
}
