package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Yet Another Aether Vortex - the top card of a library is on the battlefield in
 * addition to being in that library.
 */
public class StaticAbilityTopLibraryOnBattlefield {

    public static boolean appliesTo(final Player owner) {
        final Game game = owner.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.TopLibraryPermanentsOnBattlefield)) {
                    continue;
                }
                if (stAb.matchesValidParam("ValidPlayer", owner)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** CR 304.4/307.4 keep instants and sorceries off the battlefield. */
    public static boolean qualifies(final Card c) {
        return !c.isInstant() && !c.isSorcery();
    }
}
