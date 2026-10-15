package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * A Good Thing: spells and abilities that can't destroy a permanent ({@code ValidCard$}, {@code ValidCause$}). Only
 * a destroy something causes - state-based actions destroying it aren't caused by a spell or ability.
 */
public class StaticAbilityCantDestroy {

    public static boolean cantDestroy(final Card card, final SpellAbility cause) {
        if (cause == null) {
            return false;
        }
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.CantDestroy)
                        && stAb.matchesValidParam("ValidCard", card) && stAb.matchesValidParam("ValidCause", cause)) {
                    return true;
                }
            }
        }
        return false;
    }
}
